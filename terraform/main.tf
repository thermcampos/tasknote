terraform {
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = ">= 2.0.0"
    }
  }

  backend "s3" {
    bucket                      = "tasknote"
    key                         = "kubernetes/terraform.tfstate"
    region                      = "auto"
    endpoints                   = { s3 = "https://d17eb09b6bce2f90e16e800bb2a6baf9.r2.cloudflarestorage.com" }
    skip_credentials_validation = true
    skip_region_validation      = true
    skip_requesting_account_id  = true
    skip_metadata_api_check     = true
    skip_s3_checksum            = true
  }
}

provider "kubernetes" {
  config_path = "~/.kube/config"
}

variable "db_user" {
  type      = string
  sensitive = true
}

variable "db_password" {
  type      = string
  sensitive = true
}

variable "db_name" {
  type      = string
  sensitive = true
}

variable "security_key" {
  type      = string
  sensitive = true
}

variable "mailgun_apikey" {
  type      = string
  sensitive = true
}

variable "r2_access_key" {
  type      = string
  sensitive = true
}

variable "r2_secret_key" {
  type      = string
  sensitive = true
}

variable "r2_bucket_name" {
  type    = string
  default = "tasknote-backups"
}

variable "r2_endpoint" {
  type    = string
  default = "https://d17eb09b6bce2f90e16e800bb2a6baf9.r2.cloudflarestorage.com"
}

variable "cors_allowed_origins" {
  type    = string
  default = "https://tasknote.darkroasted.vps-kinghost.net,https://tasknote.cc,https://www.tasknote.cc"
}

variable "root_log_level" {
  type    = string
  default = "INFO"
}

variable "backend_image" {
  type    = string
  default = "rmcampos/tasknote-api:latest"
}

variable "frontend_image" {
  type    = string
  default = "rmcampos/tasknote-app:latest"
}

resource "kubernetes_namespace_v1" "tasknote" {
  metadata {
    name = "tasknote"
  }
}

resource "kubernetes_secret_v1" "tasknote_secrets" {
  metadata {
    name      = "tasknote-secrets"
    namespace = kubernetes_namespace_v1.tasknote.metadata[0].name
  }

  data = {
    postgres_user     = var.db_user
    postgres_password = var.db_password
    postgres_db       = var.db_name
    security_key      = var.security_key
    mailgun_apikey    = var.mailgun_apikey
  }
}

resource "kubernetes_persistent_volume_claim_v1" "tasknote_db_data" {
  metadata {
    name      = "postgres-data-pvc"
    namespace = kubernetes_namespace_v1.tasknote.metadata[0].name
  }
  spec {
    access_modes = ["ReadWriteOnce"]
    resources {
      requests = {
        storage = "1Gi"
      }
    }
  }
}

resource "kubernetes_deployment_v1" "tasknote_db" {
  metadata {
    name      = "tasknote-db"
    namespace = kubernetes_namespace_v1.tasknote.metadata[0].name
  }
  spec {
    replicas = 1
    selector { match_labels = { app = "tasknote-db" } }
    template {
      metadata { labels = { app = "tasknote-db" } }
      spec {
        container {
          image = "postgres:15.8-bookworm"
          name  = "postgres"
          volume_mount {
            name       = "postgres-storage"
            mount_path = "/var/lib/postgresql/data"
          }
          env {
            name = "POSTGRES_USER"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.tasknote_secrets.metadata[0].name
                key  = "postgres_user"
              }
            }
          }
          env {
            name = "POSTGRES_PASSWORD"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.tasknote_secrets.metadata[0].name
                key  = "postgres_password"
              }
            }
          }
          env {
            name = "POSTGRES_DB"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.tasknote_secrets.metadata[0].name
                key  = "postgres_db"
              }
            }
          }
          port { container_port = 5432 }
        }
        volume {
          name = "postgres-storage"
          persistent_volume_claim {
            claim_name = kubernetes_persistent_volume_claim_v1.tasknote_db_data.metadata[0].name
          }
        }
      }
    }
  }
}

resource "kubernetes_service_v1" "tasknote_db_svc" {
  metadata {
    name      = "tasknote-db-svc"
    namespace = kubernetes_namespace_v1.tasknote.metadata[0].name
  }
  spec {
    selector = { app = "tasknote-db" }
    port { port = 5432 }
    type = "ClusterIP"
  }
}

resource "kubernetes_deployment_v1" "tasknote_backend" {
  metadata {
    name      = "tasknote-backend"
    namespace = kubernetes_namespace_v1.tasknote.metadata[0].name
  }
  spec {
    replicas = 1
    selector { match_labels = { app = "tasknote-backend" } }
    template {
      metadata { labels = { app = "tasknote-backend" } }
      spec {
        container {
          image = var.backend_image
          name  = "backend"
          env {
            name = "POSTGRES_DB"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.tasknote_secrets.metadata[0].name
                key  = "postgres_db"
              }
            }
          }
          env {
            name  = "POSTGRES_HOST"
            value = "tasknote-db-svc"
          }
          env {
            name = "POSTGRES_USER"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.tasknote_secrets.metadata[0].name
                key  = "postgres_user"
              }
            }
          }
          env {
            name = "POSTGRES_PASSWORD"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.tasknote_secrets.metadata[0].name
                key  = "postgres_password"
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
            name  = "SERVER_SERVLET_CONTEXT_PATH"
            value = "/"
          }
          env {
            name  = "ROOT_LOG_LEVEL"
            value = var.root_log_level
          }
          env {
            name = "SECURITY_KEY"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.tasknote_secrets.metadata[0].name
                key  = "security_key"
              }
            }
          }
          env {
            name  = "TARGET_ENV"
            value = "production"
          }
          env {
            name = "MAILGUN_APIKEY"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.tasknote_secrets.metadata[0].name
                key  = "mailgun_apikey"
              }
            }
          }
          resources {
            limits   = { memory = "256Mi", cpu = "500m" }
            requests = { memory = "256Mi", cpu = "250m" }
          }
        }
      }
    }
  }
}

resource "kubernetes_service_v1" "tasknote_backend_svc" {
  metadata {
    name      = "tasknote-backend-svc"
    namespace = kubernetes_namespace_v1.tasknote.metadata[0].name
  }
  spec {
    selector = { app = "tasknote-backend" }
    port {
      port        = 8585
      target_port = 8585
    }
  }
}

resource "kubernetes_deployment_v1" "tasknote_frontend" {
  metadata {
    name      = "tasknote-frontend"
    namespace = kubernetes_namespace_v1.tasknote.metadata[0].name
  }
  spec {
    replicas = 1
    selector { match_labels = { app = "tasknote-app" } }
    template {
      metadata { labels = { app = "tasknote-app" } }
      spec {
        container {
          image = var.frontend_image
          name  = "frontend"
          port { container_port = 5000 }
          env {
            name  = "VITE_BACKEND_SERVER"
            value = "https://tasknoteapi.darkroasted.vps-kinghost.net"
          }
        }
      }
    }
  }
}

resource "kubernetes_service_v1" "tasknote_frontend_svc" {
  metadata {
    name      = "tasknote-frontend-svc"
    namespace = kubernetes_namespace_v1.tasknote.metadata[0].name
  }
  spec {
    selector = { app = "tasknote-app" }
    port {
      port        = 5000
      target_port = 5000
    }
    type = "ClusterIP"
  }
}

# Unified Ingress for App and API
resource "kubernetes_ingress_v1" "tasknote_ingress" {
  metadata {
    name      = "tasknote-ingress"
    namespace = kubernetes_namespace_v1.tasknote.metadata[0].name
    annotations = {
      "kubernetes.io/ingress.class"    = "traefik"
      "cert-manager.io/cluster-issuer" = "letsencrypt-prod"
    }
  }
  spec {
    tls {
      hosts = [
        "tasknote.darkroasted.vps-kinghost.net",
        "tasknoteapi.darkroasted.vps-kinghost.net",
        "tasknote.cc",
        "www.tasknote.cc"
      ]
      secret_name = "tasknote-tls-certs"
    }
    rule {
      host = "tasknote.darkroasted.vps-kinghost.net"
      http {
        path {
          path      = "/"
          path_type = "Prefix"
          backend {
            service {
              name = kubernetes_service_v1.tasknote_frontend_svc.metadata[0].name
              port { number = 5000 }
            }
          }
        }
      }
    }
    rule {
      host = "tasknoteapi.darkroasted.vps-kinghost.net"
      http {
        path {
          path      = "/"
          path_type = "Prefix"
          backend {
            service {
              name = kubernetes_service_v1.tasknote_backend_svc.metadata[0].name
              port { number = 8585 }
            }
          }
        }
      }
    }
    rule {
      host = "tasknote.cc"
      http {
        path {
          path      = "/"
          path_type = "Prefix"
          backend {
            service {
              name = kubernetes_service_v1.tasknote_frontend_svc.metadata[0].name
              port { number = 5000 }
            }
          }
        }
      }
    }
    rule {
      host = "www.tasknote.cc"
      http {
        path {
          path      = "/"
          path_type = "Prefix"
          backend {
            service {
              name = kubernetes_service_v1.tasknote_frontend_svc.metadata[0].name
              port { number = 5000 }
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_secret_v1" "r2_backup_secrets" {
  metadata {
    name      = "r2-backup-secrets"
    namespace = kubernetes_namespace_v1.tasknote.metadata[0].name
  }

  data = {
    access_key = var.r2_access_key
    secret_key = var.r2_secret_key
  }
}

resource "kubernetes_cron_job_v1" "tasknote_db_backup" {
  metadata {
    name      = "tasknote-db-backup"
    namespace = kubernetes_namespace_v1.tasknote.metadata[0].name
  }
  spec {
    schedule = "0 0,12 * * *"
    job_template {
      metadata {
        labels = {
          app = "taknote-db-backup"
        }
      }
      spec {
        template {
          metadata {
            labels = {
              app = "tasknote-db-backup"
            }
          }
          spec {
            container {
              name    = "backup"
              image   = "postgres:15.8-alpine"
              command = ["/bin/sh", "-c"]
              args = [
                <<-EOT
                apk add --no-cache aws-cli
                export PGPASSWORD=$POSTGRES_PASSWORD
                FILENAME="backup-$(date +%Y%m%d%H%M%S).sql.gz"
                echo "Starting backup of $POSTGRES_DB to $FILENAME..."
                pg_dump -h $DB_HOST -U $POSTGRES_USER $POSTGRES_DB | gzip > /tmp/$FILENAME
                echo "Uploading to R2..."
                AWS_ACCESS_KEY_ID=$R2_ACCESS_KEY AWS_SECRET_ACCESS_KEY=$R2_SECRET_KEY \
                aws s3 cp /tmp/$FILENAME s3://$R2_BUCKET/ --endpoint-url $R2_ENDPOINT
                echo "Backup completed successfully."
                EOT
              ]
              env {
                name  = "DB_HOST"
                value = "tasknote-db-svc"
              }
              env {
                name = "POSTGRES_USER"
                value_from {
                  secret_key_ref {
                    name = kubernetes_secret_v1.tasknote_secrets.metadata[0].name
                    key  = "postgres_user"
                  }
                }
              }
              env {
                name = "POSTGRES_PASSWORD"
                value_from {
                  secret_key_ref {
                    name = kubernetes_secret_v1.tasknote_secrets.metadata[0].name
                    key  = "postgres_password"
                  }
                }
              }
              env {
                name = "POSTGRES_DB"
                value_from {
                  secret_key_ref {
                    name = kubernetes_secret_v1.tasknote_secrets.metadata[0].name
                    key  = "postgres_db"
                  }
                }
              }
              env {
                name = "R2_ACCESS_KEY"
                value_from {
                  secret_key_ref {
                    name = kubernetes_secret_v1.r2_backup_secrets.metadata[0].name
                    key  = "access_key"
                  }
                }
              }
              env {
                name = "R2_SECRET_KEY"
                value_from {
                  secret_key_ref {
                    name = kubernetes_secret_v1.r2_backup_secrets.metadata[0].name
                    key  = "secret_key"
                  }
                }
              }
              env {
                name  = "R2_BUCKET"
                value = var.r2_bucket_name
              }
              env {
                name  = "R2_ENDPOINT"
                value = var.r2_endpoint
              }
            }
            restart_policy = "OnFailure"
          }
        }
      }
    }
  }
}

