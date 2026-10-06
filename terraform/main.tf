data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd*/ubuntu-noble-24.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
  }
}

data "aws_iam_policy_document" "ec2_assume" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ec2" {
  name_prefix        = "salohi-hrms-"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume.json
}

resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "ec2" {
  name_prefix = "salohi-hrms-"
  role        = aws_iam_role.ec2.name
}

resource "aws_vpc" "main" {
  cidr_block           = "10.40.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "salohi-hrms"
  }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "salohi-hrms"
  }
}

resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.40.1.0/24"
  availability_zone       = data.aws_availability_zones.available.names[0]
  map_public_ip_on_launch = true

  tags = {
    Name = "salohi-hrms-public"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "salohi-hrms-public"
  }
}

resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

resource "aws_security_group" "hrms" {
  name_prefix = "salohi-hrms-"
  description = "Salohi IT HRMS, Jenkins, and SonarQube"
  vpc_id      = aws_vpc.main.id

  tags = {
    Name = "salohi-hrms"
  }
}

resource "aws_vpc_security_group_ingress_rule" "ssh" {
  security_group_id = aws_security_group.hrms.id
  description       = "SSH"
  ip_protocol       = "tcp"
  from_port         = 22
  to_port           = 22
  cidr_ipv4         = var.allowed_cidr
}

resource "aws_vpc_security_group_ingress_rule" "jenkins" {
  security_group_id = aws_security_group.hrms.id
  description       = "Jenkins"
  ip_protocol       = "tcp"
  from_port         = 8080
  to_port           = 8080
  cidr_ipv4         = var.allowed_cidr
}

resource "aws_vpc_security_group_ingress_rule" "hrms" {
  security_group_id = aws_security_group.hrms.id
  description       = "HRMS portal"
  ip_protocol       = "tcp"
  from_port         = 8081
  to_port           = 8081
  cidr_ipv4         = var.allowed_cidr
}

resource "aws_vpc_security_group_ingress_rule" "sonar" {
  security_group_id = aws_security_group.hrms.id
  description       = "SonarQube"
  ip_protocol       = "tcp"
  from_port         = 9000
  to_port           = 9000
  cidr_ipv4         = var.allowed_cidr
}

resource "aws_vpc_security_group_egress_rule" "all" {
  security_group_id = aws_security_group.hrms.id
  description       = "Package installs, container image pulls, and updates"
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"
}

resource "aws_key_pair" "hrms" {
  count           = var.ssh_public_key == "" ? 0 : 1
  key_name_prefix = "salohi-hrms-"
  public_key      = var.ssh_public_key
}

resource "aws_instance" "hrms" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = var.instance_type
  subnet_id              = aws_subnet.public.id
  vpc_security_group_ids = [aws_security_group.hrms.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2.name
  key_name               = one(aws_key_pair.hrms[*].key_name)
  ebs_optimized          = true

  metadata_options {
    http_endpoint = "enabled"
    http_tokens   = "required"
  }

  root_block_device {
    volume_type = "gp3"
    volume_size = var.root_volume_gb
    encrypted   = true
  }

  user_data = templatefile("${path.module}/user_data.sh.tpl", {
    db_password = var.db_password
  })

  lifecycle {
    ignore_changes = [ami, user_data]
  }

  tags = {
    Name = "salohi-hrms"
  }
}

resource "aws_ebs_volume" "data" {
  availability_zone = aws_subnet.public.availability_zone
  size              = var.data_volume_gb
  type              = "gp3"
  encrypted         = true

  tags = {
    Name = "salohi-hrms-data"
  }
}

resource "aws_volume_attachment" "data" {
  device_name = "/dev/sdf"
  volume_id   = aws_ebs_volume.data.id
  instance_id = aws_instance.hrms.id
}

resource "aws_eip" "hrms" {
  domain   = "vpc"
  instance = aws_instance.hrms.id

  tags = {
    Name = "salohi-hrms"
  }
}
