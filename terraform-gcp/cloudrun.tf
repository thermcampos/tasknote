resource "google_cloud_run_v2_service" "backend" {
  name     = "tasknote-api"
  location = var.region
  ingress  = "INGRESS_TRAFFIC_ALL"

  depends_on = [google_project_service.run]

  template {
    service_account = google_service_account.cloudrun_sa.email
    vpc_access {
      connector = google_vpc_access_connector.connector.id
      egress    = "PRIVATE_RANGES_ONLY"
    }

    containers {
      image = var.backend_image
      ports {
        container_port = 8585
      }
      env {
        name  = "POSTGRES_DB"
        value = var.db_name
      }
      env {
        name  = "POSTGRES_HOST"
        value = google_sql_database_instance.instance.private_ip_address
      }
      env {
        name = "POSTGRES_USER"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.db_user.secret_id
            version = google_secret_manager_secret_version.db_user_version.version
          }
        }
      }
      env {
        name = "POSTGRES_PASSWORD"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.db_password.secret_id
            version = google_secret_manager_secret_version.db_password_version.version
          }
        }
      }
      env {
        name  = "POSTGRES_PORT"
        value = "5432"
      }
      env {
        name  = "CORS_ALLOWED_ORIGINS"
        value = var.cors_allowed_origins
      }
      env {
        name = "SERVER_SERVLET_CONTEXT_PATH"
        value = "/"
      }
      env {
        name  = "TARGET_ENV"
        value = "production"
      }
      env {
        name = "SECURITY_KEY"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.security_key.secret_id
            version = google_secret_manager_secret_version.security_key_version.version
          }
        }
      }
      env {
        name = "MAILGUN_APIKEY"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.mailgun_apikey.secret_id
            version = google_secret_manager_secret_version.mailgun_apikey_version.version
          }
        }
      }
    }
  }
}

resource "google_cloud_run_v2_service" "frontend" {
  name     = "tasknote-app"
  location = var.region
  ingress  = "INGRESS_TRAFFIC_ALL"

  depends_on = [google_project_service.run]

  template {
    service_account = google_service_account.cloudrun_sa.email

    containers {
      image = var.frontend_image
      ports {
        container_port = 5000
      }
      env {
        name  = "VITE_BACKEND_SERVER"
        value = google_cloud_run_v2_service.backend.uri
      }
    }
  }
}

# Allow unauthenticated access to both services
resource "google_cloud_run_service_iam_member" "backend_public" {
  location = google_cloud_run_v2_service.backend.location
  service  = google_cloud_run_v2_service.backend.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}

resource "google_cloud_run_service_iam_member" "frontend_public" {
  location = google_cloud_run_v2_service.frontend.location
  service  = google_cloud_run_v2_service.frontend.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}

# Grant Cloud SQL Client role to the service account
resource "google_project_iam_member" "cloudsql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.cloudrun_sa.email}"
}
