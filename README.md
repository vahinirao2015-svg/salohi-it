# Salohi IT HRMS

Java portal for employee records, leave, attendance, and payslips. PostgreSQL keeps the data. On AWS, that database lives on an encrypted EBS volume mounted at `/data`.

## What Terraform creates

- Ubuntu 24.04 EC2 instance, type `m7i-flex.large` (2 vCPU, 8 GiB)
- Encrypted gp3 root disk and a separate encrypted gp3 data disk
- Elastic IP, locked security group, and a Systems Manager role
- First boot installs Java 17, Maven, Docker, Jenkins, and SonarQube
- PostgreSQL 16 and SonarQube run in Docker, with files on `/data`

Jenkins listens on port 8080, the portal on 8081, and SonarQube on 9000. PostgreSQL listens on the instance only.

`m7i-flex.large` has 8 GiB of memory. Jenkins, SonarQube, PostgreSQL, and the portal share that memory, so the boot script adds a 2 GiB swap file and caps SonarQube heaps. Builds will be slower than on a full-size `m7i.large`.

## Monthly cost if left running

Approximate on-demand prices, 730 hours, before tax:

| | ap-south-1 (default) | us-east-1 |
| --- | --- | --- |
| m7i-flex.large | about $74 | about $70 |
| 80 GB gp3 | about $8 | about $6 |
| Public IPv4 | about $4 | about $4 |
| **Total** | **about $86** | **about $80** |

`terraform destroy` removes the instance and the data volume.

## Create the stack

```powershell
cd terraform
copy terraform.tfvars.example terraform.tfvars
```

Set `allowed_cidr` to your public IP with `/32`, and set `db_password` to 12–64 letters and digits. Optionally paste an SSH public key into `ssh_public_key`.

```powershell
terraform init
terraform apply
```

Connect with the `session_manager` output, or with SSH if you supplied a key. Jenkins’ first admin password is in `/var/lib/jenkins/secrets/initialAdminPassword`. SonarQube’s first sign-in is `admin` / `admin`.

## Deploy the portal

From this folder, after the instance has finished its first boot (Docker and PostgreSQL are up):

```powershell
.\scripts\deploy.ps1 -HostAddress INSTANCE_IP -KeyPath C:\path\to\key.pem
```

Then open `http://INSTANCE_IP:8081`.

## Run on this computer

```powershell
docker compose up -d
mvn -B -f hrms/pom.xml spring-boot:run
```

Open http://localhost:8081.

Seeded accounts, password `Salohi@123`:

- `admin@salohi.it` — administrator
- `arjun@salohi.it` — employee
- `meera@salohi.it` — employee

Change these passwords from People after the first sign-in.

## Pay calculation

Net pay = basic + 40% house rent allowance + 10% special allowance − 12% provident fund − ₹200 professional tax − loss of pay. Approved unpaid leave in that month is the loss of pay. Casual and earned leave share the annual balance.
