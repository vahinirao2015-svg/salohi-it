param(
    [string]$HostAddress = "",
    [string]$KeyPath = "",
    [string]$InstanceId = "",
    [string]$Region = "ap-south-1"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root

function Add-PortableBuildTools {
    if (Get-Command mvn -ErrorAction SilentlyContinue) {
        return
    }

    $tools = Join-Path $env:TEMP "salohi-tools"
    $jdkRoot = Join-Path $tools "jdk"
    $mvnHome = Join-Path $tools "maven\apache-maven-3.9.9"
    New-Item -ItemType Directory -Force -Path $tools | Out-Null

    if (-not (Get-ChildItem $jdkRoot -Directory -ErrorAction SilentlyContinue)) {
        Write-Host "Downloading a portable JDK 17..."
        $jdkZip = Join-Path $tools "jdk.zip"
        Invoke-WebRequest -Uri "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk" -OutFile $jdkZip
        Expand-Archive -Path $jdkZip -DestinationPath $jdkRoot -Force
    }
    if (-not (Test-Path (Join-Path $mvnHome "bin\mvn.cmd"))) {
        Write-Host "Downloading a portable Maven..."
        $mvnZip = Join-Path $tools "maven.zip"
        Invoke-WebRequest -Uri "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip" -OutFile $mvnZip
        Expand-Archive -Path $mvnZip -DestinationPath (Join-Path $tools "maven") -Force
    }

    $jdk = Get-ChildItem $jdkRoot -Directory | Select-Object -First 1
    $env:JAVA_HOME = $jdk.FullName
    $env:Path = "$($jdk.FullName)\bin;$mvnHome\bin;" + $env:Path
}

function Get-TerraformOutput([string]$Name) {
    $value = terraform -chdir="$Root\terraform" output -raw $Name 2>$null
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($value)) {
        throw "Could not read terraform output '$Name'. Run this from the project after terraform apply."
    }
    return $value.Trim()
}

function Invoke-Ssm([string[]]$Commands) {
    $paramsFile = Join-Path $env:TEMP "salohi-ssm-params.json"
    @{ commands = $Commands } | ConvertTo-Json | Set-Content -Path $paramsFile -Encoding ascii
    $fileUri = "file://" + ($paramsFile -replace '\\', '/')
    $commandId = aws ssm send-command `
        --region $Region `
        --instance-ids $InstanceId `
        --document-name AWS-RunShellScript `
        --parameters $fileUri `
        --query "Command.CommandId" `
        --output text
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($commandId)) {
        throw "Could not send the command through Session Manager."
    }
    $commandId = $commandId.Trim()
    for ($i = 0; $i -lt 40; $i++) {
        Start-Sleep -Seconds 3
        $status = aws ssm get-command-invocation `
            --region $Region `
            --command-id $commandId `
            --instance-id $InstanceId `
            --query "Status" `
            --output text
        $status = "$status".Trim()
        if ($status -in @("Success", "Failed", "Cancelled", "TimedOut")) {
            $output = aws ssm get-command-invocation `
                --region $Region `
                --command-id $commandId `
                --instance-id $InstanceId `
                --query "StandardOutputContent" `
                --output text
            if ($status -ne "Success") {
                $errorText = aws ssm get-command-invocation `
                    --region $Region `
                    --command-id $commandId `
                    --instance-id $InstanceId `
                    --query "StandardErrorContent" `
                    --output text
                throw "Instance command $status. $errorText"
            }
            return "$output"
        }
    }
    throw "Timed out waiting for the instance command."
}

Add-PortableBuildTools

if ([string]::IsNullOrWhiteSpace($HostAddress) -or $HostAddress -eq "INSTANCE_IP") {
    $HostAddress = Get-TerraformOutput "public_ip"
}
if ([string]::IsNullOrWhiteSpace($InstanceId)) {
    $InstanceId = Get-TerraformOutput "instance_id"
}

$placeholderKey = $KeyPath -match "path\\to\\key" -or $KeyPath -eq "C:\path\to\key.pem"
if ([string]::IsNullOrWhiteSpace($KeyPath) -or $placeholderKey -or -not (Test-Path $KeyPath)) {
    $KeyPath = Join-Path $Root "terraform\salohi-deploy"
    if (-not (Test-Path $KeyPath)) {
        cmd.exe /c "ssh-keygen -q -t ed25519 -f `"$KeyPath`" -N `"`" -C salohi-hrms-deploy"
        if ($LASTEXITCODE -ne 0) { throw "Could not create an SSH key." }
    }
    $publicKey = (Get-Content "$KeyPath.pub" -Raw).Trim()
    Write-Host "Authorizing the deploy key on $InstanceId..."
    Invoke-Ssm @(
        "mkdir -p /home/ubuntu/.ssh",
        "touch /home/ubuntu/.ssh/authorized_keys",
        "grep -qxF '$publicKey' /home/ubuntu/.ssh/authorized_keys || echo '$publicKey' >> /home/ubuntu/.ssh/authorized_keys",
        "chown -R ubuntu:ubuntu /home/ubuntu/.ssh",
        "chmod 700 /home/ubuntu/.ssh",
        "chmod 600 /home/ubuntu/.ssh/authorized_keys"
    ) | Out-Null
}

Write-Host "Waiting until the instance has finished installing Java, Docker, and PostgreSQL..."
$ready = $false
for ($i = 0; $i -lt 40; $i++) {
    $state = Invoke-Ssm @("if [ -x /opt/hrms/deploy-app.sh ]; then echo READY; else echo BOOTING; fi")
    if ($state -match "READY") {
        $ready = $true
        break
    }
    Start-Sleep -Seconds 15
}
if (-not $ready) {
    throw "The instance is still starting. Wait a few minutes and run this script again."
}

Write-Host "Building the HRMS jar..."
& mvn -B -f hrms/pom.xml -DskipTests package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Copying the jar to $HostAddress..."
& scp -i $KeyPath -o StrictHostKeyChecking=accept-new "hrms\target\hrms.jar" "ubuntu@${HostAddress}:/tmp/hrms.jar"
if ($LASTEXITCODE -ne 0) { throw "scp failed." }
& ssh -i $KeyPath -o StrictHostKeyChecking=accept-new "ubuntu@${HostAddress}" "sudo /opt/hrms/deploy-app.sh /tmp/hrms.jar"
if ($LASTEXITCODE -ne 0) { throw "The instance did not start the HRMS service." }

Write-Host "Portal: http://${HostAddress}:8081"
