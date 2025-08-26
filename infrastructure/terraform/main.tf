terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  
  backend "s3" {
    bucket = "arb-monitor-terraform-state"
    key    = "terraform.tfstate"
    region = "us-east-1"
  }
}

provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Project     = "ArbMonitor"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

# Data sources
data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_caller_identity" "current" {}

# Local values
locals {
  name_prefix = "${var.project_name}-${var.environment}"
  azs         = slice(data.aws_availability_zones.available.names, 0, 2)
  
  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}

# VPC and Networking
module "vpc" {
  source = "./modules/vpc"
  
  name_prefix = local.name_prefix
  cidr_block  = var.vpc_cidr
  azs         = local.azs
  
  tags = local.tags
}

# Security Groups
module "security_groups" {
  source = "./modules/security"
  
  name_prefix = local.name_prefix
  vpc_id      = module.vpc.vpc_id
  
  tags = local.tags
}

# RDS Database
module "database" {
  source = "./modules/rds"
  
  name_prefix           = local.name_prefix
  vpc_id               = module.vpc.vpc_id
  private_subnet_ids   = module.vpc.private_subnet_ids
  security_group_ids   = [module.security_groups.rds_security_group_id]
  
  db_instance_class    = var.db_instance_class
  db_allocated_storage = var.db_allocated_storage
  db_name             = var.db_name
  db_username         = var.db_username
  db_password         = var.db_password
  
  backup_retention_period = var.environment == "prod" ? 30 : 7
  multi_az               = var.environment == "prod" ? true : false
  
  tags = local.tags
}

# ElastiCache Redis
module "redis" {
  source = "./modules/redis"
  
  name_prefix        = local.name_prefix
  vpc_id            = module.vpc.vpc_id
  private_subnet_ids = module.vpc.private_subnet_ids
  security_group_ids = [module.security_groups.redis_security_group_id]
  
  node_type = var.redis_node_type
  
  tags = local.tags
}

# S3 Buckets
module "s3" {
  source = "./modules/s3"
  
  name_prefix = local.name_prefix
  environment = var.environment
  
  tags = local.tags
}

# ECS Cluster
module "ecs" {
  source = "./modules/ecs"
  
  name_prefix = local.name_prefix
  vpc_id      = module.vpc.vpc_id
  
  public_subnet_ids  = module.vpc.public_subnet_ids
  private_subnet_ids = module.vpc.private_subnet_ids
  
  alb_security_group_id     = module.security_groups.alb_security_group_id
  backend_security_group_id = module.security_groups.backend_security_group_id
  
  # Database connection
  database_url      = module.database.connection_string
  database_username = var.db_username
  database_password = var.db_password
  
  # Redis connection
  redis_host = module.redis.primary_endpoint
  
  # S3 bucket
  s3_bucket_name = module.s3.media_bucket_name
  
  # Application configuration
  apify_token           = var.apify_token
  jwt_secret           = var.jwt_secret
  stripe_secret_key    = var.stripe_secret_key
  stripe_webhook_secret = var.stripe_webhook_secret
  nextauth_secret      = var.nextauth_secret
  
  # Scaling configuration
  backend_desired_count  = var.backend_desired_count
  backend_min_capacity   = var.backend_min_capacity
  backend_max_capacity   = var.backend_max_capacity
  frontend_desired_count = var.frontend_desired_count
  
  tags = local.tags
}

# CloudFront Distribution
module "cloudfront" {
  source = "./modules/cloudfront"
  
  name_prefix = local.name_prefix
  environment = var.environment
  
  alb_domain_name = module.ecs.alb_dns_name
  s3_bucket_name  = module.s3.frontend_bucket_name
  
  tags = local.tags
}

# Route 53 DNS
module "route53" {
  source = "./modules/route53"
  
  domain_name              = var.domain_name
  cloudfront_domain_name   = module.cloudfront.domain_name
  cloudfront_hosted_zone_id = module.cloudfront.hosted_zone_id
  
  tags = local.tags
}

# IAM Roles and Policies
module "iam" {
  source = "./modules/iam"
  
  name_prefix = local.name_prefix
  s3_bucket_arns = [
    module.s3.frontend_bucket_arn,
    module.s3.media_bucket_arn
  ]
  
  tags = local.tags
}
