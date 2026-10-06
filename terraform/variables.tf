variable "aws_region" {
  description = "Region for the HRMS stack. m7i-flex.large is available in ap-south-1."
  type        = string
  default     = "ap-south-1"
}

variable "allowed_cidr" {
  description = "Your public IP in CIDR form, for example 203.0.113.10/32. This is the only address that can reach SSH, the portal, Jenkins, and SonarQube."
  type        = string

  validation {
    condition     = can(cidrnetmask(var.allowed_cidr))
    error_message = "allowed_cidr must be a valid CIDR block such as 203.0.113.10/32."
  }
}

variable "db_password" {
  description = "Password for the PostgreSQL hrms and sonar users. Letters and digits only, 12 to 64 characters. Stored on the instance by the first boot script."
  type        = string
  sensitive   = true

  validation {
    condition     = can(regex("^[A-Za-z0-9]{12,64}$", var.db_password))
    error_message = "Use 12 to 64 letters and digits so the password is safe to place in the boot script."
  }
}

variable "ssh_public_key" {
  description = "Optional SSH public key contents (ssh-ed25519 AAAA...). Leave empty to use Systems Manager Session Manager only."
  type        = string
  default     = ""
}

variable "instance_type" {
  description = "EC2 size. Salohi IT asked for m7i-flex.large (2 vCPU, 8 GiB)."
  type        = string
  default     = "m7i-flex.large"
}

variable "root_volume_gb" {
  description = "Encrypted gp3 root volume for the OS, Jenkins, and toolchains."
  type        = number
  default     = 30
}

variable "data_volume_gb" {
  description = "Encrypted gp3 volume mounted at /data for PostgreSQL and SonarQube."
  type        = number
  default     = 50
}
