resource "google_service_account" "cloudrun_sa" {
  account_id   = "tasknote-cloudrun-sa"
  display_name = "TaskNote Cloud Run Service Account"
}

resource "google_secret_manager_secret" "db_password" {
  secret_id = "db-password"
  replication {
    user_managed {
      replicas {
        location = var.region
      }
    }
  }
  depends_on = [google_project_service.secretmanager]
}

resource "google_secret_manager_secret_version" "db_password_version" {
  secret      = google_secret_manager_secret.db_password.id
  secret_data = var.db_password
}

resource "google_secret_manager_secret" "db_user" {
  secret_id = "db-user"
  replication {
    user_managed {
      replicas {
        location = var.region
      }
    }
  }
  depends_on = [google_project_service.secretmanager]
}

resource "google_secret_manager_secret_version" "db_user_version" {
  secret      = google_secret_manager_secret.db_user.id
  secret_data = var.db_user
}

resource "google_secret_manager_secret_iam_member" "db_password_access" {
  secret_id = google_secret_manager_secret.db_password.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloudrun_sa.email}"
}

resource "google_secret_manager_secret_iam_member" "db_user_access" {
  secret_id = google_secret_manager_secret.db_user.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloudrun_sa.email}"
}

resource "google_secret_manager_secret" "security_key" {
  secret_id = "security-key"
  replication {
    user_managed {
      replicas {
        location = var.region
      }
    }
  }
}

resource "google_secret_manager_secret_version" "security_key_version" {
  secret      = google_secret_manager_secret.security_key.id
  secret_data = var.security_key
}

resource "google_secret_manager_secret" "mailgun_apikey" {
  secret_id = "mailgun-apikey"
  replication {
    user_managed {
      replicas {
        location = var.region
      }
    }
  }
  depends_on = [google_project_service.secretmanager]
}

resource "google_secret_manager_secret_version" "mailgun_apikey_version" {
  secret      = google_secret_manager_secret.mailgun_apikey.id
  secret_data = var.mailgun_apikey
}

resource "google_secret_manager_secret_iam_member" "security_key_access" {
  secret_id = google_secret_manager_secret.security_key.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloudrun_sa.email}"
}

resource "google_secret_manager_secret_iam_member" "mailgun_apikey_access" {
  secret_id = google_secret_manager_secret.mailgun_apikey.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloudrun_sa.email}"
}
