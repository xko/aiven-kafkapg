//Based on:
// https://help.aiven.io/en/articles/4187903-kafka-connect-terraform-integration
// https://registry.terraform.io/providers/aiven/aiven/latest/docs/guides/getting-started

terraform {
  required_providers {
    aiven = {
      source = "aiven/aiven"
      version = "2.0.11"
    }
  }
}

variable "aiven_api_token" {}
variable "aiven_project_name" {}
variable "cloud_region" {
  default = "google-europe-west3"
}

provider "aiven" {
  api_token = var.aiven_api_token
}

resource "aiven_project" "xko-aiven-kafkapg" {
    project = var.aiven_project_name
}

resource "aiven_service" "pg-sink" {
  project = aiven_project.xko-aiven-kafkapg.project
  cloud_name = var.cloud_region
  plan = "startup-4"
  service_name = "pg-sink"
  service_type = "pg"
  maintenance_window_dow = "monday"
  maintenance_window_time = "12:00:00"
  pg_user_config {
    pg {
      idle_in_transaction_session_timeout = 900
    }
    pg_version = "10"
  }
}

resource "aiven_service" "kafka" {
  project = aiven_project.xko-aiven-kafkapg.project
  cloud_name = var.cloud_region
  plan = "startup-2"
  service_name = "kafka"
  service_type = "kafka"
  maintenance_window_dow = "monday"
  maintenance_window_time = "10:00:00"
  kafka_user_config {
    kafka_version = "2.6"
    schema_registry = true
    kafka_rest = true
  }
}

resource "aiven_kafka_topic" "kafka-topic-os-metrics-log" {
  depends_on = [aiven_service.kafka]
  project = aiven_project.xko-aiven-kafkapg.project
  service_name = aiven_service.kafka.service_name
  topic_name = "os-metrics-log"
  partitions = 3
  replication = 2
  timeouts {
    create = "5m"
  }
}