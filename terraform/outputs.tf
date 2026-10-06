output "hrms_url" {
  description = "Salohi IT HRMS portal. Available after the application jar is deployed."
  value       = "http://${aws_eip.hrms.public_ip}:8081"
}

output "jenkins_url" {
  description = "Jenkins UI."
  value       = "http://${aws_eip.hrms.public_ip}:8080"
}

output "sonarqube_url" {
  description = "SonarQube UI. First sign-in is admin / admin and SonarQube will ask for a new password."
  value       = "http://${aws_eip.hrms.public_ip}:9000"
}

output "public_ip" {
  description = "Elastic IP of the HRMS instance."
  value       = aws_eip.hrms.public_ip
}

output "instance_id" {
  description = "EC2 instance id, used with Session Manager."
  value       = aws_instance.hrms.id
}

output "session_manager" {
  description = "Connect without SSH."
  value       = "aws ssm start-session --target ${aws_instance.hrms.id} --region ${var.aws_region}"
}

output "data_volume_id" {
  description = "Persistent data volume (PostgreSQL and SonarQube)."
  value       = aws_ebs_volume.data.id
}
