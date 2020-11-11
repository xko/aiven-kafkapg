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


provider "aiven" {
  api_token = var.aiven_api_token
}

resource "aiven_project" "xko-aiven-kafkapg" {
  project = var.aiven_project_name
}

resource "aiven_service" "pg-sink" {
  project = aiven_project.xko-aiven-kafkapg.project
  cloud_name = "google-europe-west3"
  plan = "startup-4"
  service_name = "pg-sink"
  service_type = "pg"
  maintenance_window_dow = "monday"
  maintenance_window_time = "12:00:00"
  //noinspection HCLUnknownBlockType
  pg_user_config {
    //noinspection HCLUnknownBlockType
    pg {
      idle_in_transaction_session_timeout = 900
    }
    pg_version = "10"
  }
}