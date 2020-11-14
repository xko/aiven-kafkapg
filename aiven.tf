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

variable "aiven_api_token" { }
variable "aiven_project_name" { }
variable "aiven_cloud_region" { default = "google-europe-west3" }
variable "aiven_osmetrics_topic" { default = "os_metrics"}
variable "aiven_connect_error_tolerance" { default = "none" }

provider "aiven" {
  api_token = var.aiven_api_token
}

resource "aiven_project" "xko-aiven-kafkapg" {
    project = var.aiven_project_name
}

resource "aiven_service" "pg-sink" {
  project = aiven_project.xko-aiven-kafkapg.project
  cloud_name = var.aiven_cloud_region
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

resource "aiven_database" "db-os-metrics" {
  project = aiven_project.xko-aiven-kafkapg.project
  service_name = aiven_service.pg-sink.service_name
  database_name = "os-metrics-sink"
}

resource "aiven_service" "kafka" {
  project = aiven_project.xko-aiven-kafkapg.project
  cloud_name = var.aiven_cloud_region
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

resource "aiven_service" "kafka_connect" {
  project = aiven_project.xko-aiven-kafkapg.project
  cloud_name = var.aiven_cloud_region
  plan = "startup-4"
  service_name = "kafka-connect"
  service_type = "kafka_connect"
  maintenance_window_dow = "monday"
  maintenance_window_time = "10:00:00"
  kafka_connect_user_config {
    kafka_connect {
      consumer_isolation_level = "read_committed"
    }
    public_access {
      kafka_connect = true
    }
  }
}

resource "aiven_service_integration" "kafka_connect" {
  project = aiven_project.xko-aiven-kafkapg.project
  integration_type = "kafka_connect"
  source_service_name = aiven_service.kafka.service_name
  destination_service_name = aiven_service.kafka_connect.service_name
  kafka_connect_user_config {
    kafka_connect {
      group_id = "connect"
      status_storage_topic = "__connect_status"
      offset_storage_topic = "__connect_offsets"
    }
  }
}

resource "aiven_kafka_topic" "topic-os-metrics" {
  depends_on = [aiven_service.kafka]
  project = aiven_project.xko-aiven-kafkapg.project
  service_name = aiven_service.kafka.service_name
  topic_name = var.aiven_osmetrics_topic
  partitions = 3
  replication = 2

}

//noinspection HILUnresolvedReference
resource "aiven_kafka_connector" "kafka-pg-sink-con" {
  project = aiven_project.xko-aiven-kafkapg.project
  service_name = aiven_service.kafka_connect.service_name
  connector_name = "kafka-pg-sink-con"
  config = {
    name = "kafka-pg-sink-con"
    "topics" = aiven_kafka_topic.topic-os-metrics.topic_name
    "connector.class" = "io.aiven.connect.jdbc.JdbcSinkConnector"
    "value.converter" = "org.apache.kafka.connect.json.JsonConverter"
    "topics" = aiven_kafka_topic.topic-os-metrics.topic_name
    "connection.url" = "jdbc:postgresql://${aiven_service.pg-sink.service_host}:${aiven_service.pg-sink.service_port}/${aiven_database.db-os-metrics.database_name}"
    "connection.user" = aiven_service.pg-sink.service_username
    "connection.password" = aiven_service.pg-sink.service_password
    "auto.create" = "true"
    "auto.evolve" = "true"
    "value.converter.schemas.enable"=true
    "errors.deadletterqueue.topic.name"= "os-metrics-log-dead"
    "errors.deadletterqueue.topic.replication.factor"= 2
    "errors.tolerance" = var.aiven_connect_error_tolerance
  }
}