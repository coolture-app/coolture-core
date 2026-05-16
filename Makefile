# .env Generator

# ENV controls which set of CONNECT_* values gets written
# Accepted values: local | dev | deploy
ENV ?= local

# HOST_NAME is written to the HOST_NAME variable in .env
# For local/dev it stays localhost; for deploy set it to your server IP (e.g. 10.0.0.1) or domain.
# Rememeber that HOSTNAME is system variable that is why it is named HOST_NAME in .env
HOST_NAME ?= localhost

# Secrets - overwritten with system env vars if exist (e.g. from GH secrets during CD)
GARAGE_RPC_SECRET        ?= $(shell openssl rand -hex 32)
GARAGE_ADMIN_TOKEN       ?= $(shell openssl rand -hex 32)
GARAGE_METRICS_TOKEN     ?= $(shell openssl rand -hex 32)
# Garage access key IDs must start with "GK" do not change the prefix.
GARAGE_ACCESS_KEY_ID     ?= GK$(shell openssl rand -hex 12)
GARAGE_ACCESS_KEY_SECRET ?= $(shell openssl rand -hex 32)

# Keycloak Database
# the same pattern as for GarageFS i.e. load from system env if available
KEYCLOAK_DB_USER ?= keycloak
KEYCLOAK_DB_PASSWORD ?= keycloak
KEYCLOAK_DB_NAME ?= keycloak

# Keycloak Realms
KEYCLOAK_REALM := coolture-dev
KEYCLOAK_TEST_USERS_PASSWORD := admin # TODO: set it to GH secret later

# Internal (Docker) hostnames
# These match the service names defined in docker-composes
# They are used for container-to-container communication
# not need to change unless you need to rename a service in docker-compose
I_REST_API_HOST  := rest-api
I_GATEWAY_HOST   := gateway
I_KEYCLOAK_HOST  := keycloak
I_GARAGE_HOST    := s3garage
I_GARAGE_UI_HOST := s3garage-ui
I_POSTGRES_HOST  := postgres
I_SCRAPPER_HOST  := scrapper

# Internal (Docker) ports
# These are the ports on which services listen on inside the Docker network
# GARAGE_*_INTERNAL_PORT values must match garage.toml
# POSTGRES and GARAGE_UI ports are fixed by the official images
I_GARAGE_API_PORT    := 3900
I_GARAGE_ADMIN_PORT  := 3910
I_GARAGE_UI_PORT     := 3909   # garage-ui always runs on 3909, do not change
I_POSTGRES_PORT      := 5432   # postgres always runs on 5432, do not change
I_GATEWAY_PORT       := 8080
I_REST_API_PORT      := 8081
I_KEYCLOAK_PORT      := 8180
I_KEYCLOAK_MGMT_PORT := 8181

# Host ports
# These are the ports exposed on the host
# Change these if you have port conflicts
# When a Spring service runs locally (not containerized), its HOST_PORT must
# match the port it actually binds to in .yml (now is set the same as INTERNAL)
H_GARAGE_API_PORT    := 3900
H_GARAGE_ADMIN_PORT  := 3910
H_GARAGE_UI_PORT     := 3920
H_FRONTEND_PORT      := 4200
H_POSTGRES_PORT      := 5430   # not 5432 to avoid conflicts
H_GATEWAY_PORT       ?= 8080
H_REST_API_PORT      ?= 8081
H_KEYCLOAK_PORT      := 8180
H_KEYCLOAK_MGMT_PORT := 8181

# Keycloak bot service-account secret.
# Substituted into keycloak/import/realm.json at realm import time AND used by
# the scrapper for client_credentials grant — both sides must see the same value.
# Default below is a static dev secret; override from environment in deploy
# (e.g. GitHub Actions: pass `secrets.KEYCLOAK_COOLTURE_BOT_CLIENT_SECRET` via
# the workflow's `env:` block before calling `make env`).
KEYCLOAK_COOLTURE_BOT_CLIENT_SECRET ?= HgT7kLmNpQrSvWxYzA3dBcEfJuK1o8i2

# Scrapper
SCRAPPER_TIMEZONE := Europe/Warsaw
SCRAPPER_INGEST_ENABLED := true
SCRAPPER_WRITE_DUMP := false
SCRAPPER_OUTPUT_DIR := /tmp/scrapper
SCRAPPER_API_BASE_URL := http://rest-api:8081/api
SCRAPPER_KEYCLOAK_BASE_URL := http://keycloak:8180
SCRAPPER_KEYCLOAK_REALM := coolture-dev
SCRAPPER_KEYCLOAK_GRANT_TYPE := client_credentials
SCRAPPER_KEYCLOAK_CLIENT_ID := coolture-bot
SCRAPPER_KEYCLOAK_CLIENT_SECRET := $(KEYCLOAK_COOLTURE_BOT_CLIENT_SECRET)
SCRAPPER_KEYCLOAK_USERNAME := coolture_admin
SCRAPPER_KEYCLOAK_PASSWORD := admin
SCRAPPER_EVENT_CATEGORY_NAME :=
SCRAPPER_FALLBACK_CATEGORY_NAME := other
SCRAPPER_RUN_ON_START := true
SCRAPPER_SCHEDULE_WINDOW_START_HOUR := 6
SCRAPPER_SCHEDULE_WINDOW_END_HOUR := 22
SCRAPPER_SCHEDULE_SALT := coolture-scrapper

# CONNECT variables (environment specific used in developed services)
# CONNECT_* tell Spring where to reach each backing service.
# The correct values depend on where Spring itself is running:
# local: 	Spring runs on the host, base services run in Docker, so using HOSTNAME & HOST_PORT
# dev:		Everything runs inside a dev container (Spring apps are run by hand during development)
#			Using internal hostnames and ports for base services, but Spring services see each other by localhost
# deploy:  	HOSTNAME is set to ip/domain; every service on backend see each other by internal hostname & port

# If you add a new service, add its CONNECT variables in both branches below.
ifeq ($(ENV),local)
	# Spring is on the host; services in Docker
	C_REST_API_HOST := localhost
	C_KEYCLOAK_HOST := localhost
	C_GARAGE_HOST   := localhost
	C_POSTGRES_HOST := localhost

	C_KEYCLOAK_PORT := $(H_KEYCLOAK_PORT)
	C_POSTGRES_PORT := $(H_POSTGRES_PORT)
	C_GARAGE_PORT   := $(H_GARAGE_API_PORT)
else ifeq ($(ENV),dev)
	# running in dev container
	C_REST_API_HOST := localhost			# developed services see each other on locahost
	C_KEYCLOAK_HOST := $(I_KEYCLOAK_HOST)
	C_GARAGE_HOST   := $(I_GARAGE_HOST)
	C_POSTGRES_HOST := $(I_POSTGRES_HOST)

	C_KEYCLOAK_PORT := $(I_KEYCLOAK_PORT)
	C_POSTGRES_PORT := $(I_POSTGRES_PORT)
	C_GARAGE_PORT   := $(I_GARAGE_API_PORT)
else
	# Spring is inside Docker with all services
	C_REST_API_HOST := $(I_REST_API_HOST)
	C_KEYCLOAK_HOST := $(I_KEYCLOAK_HOST)
	C_GARAGE_HOST   := $(I_GARAGE_HOST)
	C_POSTGRES_HOST := $(I_POSTGRES_HOST)

	C_KEYCLOAK_PORT := $(I_KEYCLOAK_PORT)
	C_POSTGRES_PORT := $(I_POSTGRES_PORT)
	C_GARAGE_PORT   := $(I_GARAGE_API_PORT)
endif

.PHONY: help env

help:
	@echo ""
	@echo "Usage: make <command> [ENV=local|dev|deploy] [HOSTNAME=<host>] [H_REST_API_PORT=<port>] [H_GATEWAY_PORT=<port>]"
	@echo ""
	@echo "defaults:"
	@echo "  ENV      = local"
	@echo "  HOSTNAME = localhost"
	@echo "  H_GATEWAY_PORT   = 8080"
	@echo "  H_REST_API_PORT  = 8081"
	@echo ""
	@echo "commands:"
	@echo "  env	Generate .env from template. Secrets are rotated on every run."
	@echo "  help	Show this message."
	@echo ""
	@echo "examples:"
	@echo "  make env                           		# local Spring + containerized base services env"
	@echo "  make env ENV=dev                   		# dev container env"
	@echo "  make env ENV=deploy HOSTNAME=10.0.0.1"

# Writes .env to the project root.
env:
	@{ \
	echo "HOST_NAME=$(HOST_NAME)"; \
	echo ""; \
	echo "# CONNECT"; \
	echo "REST_API_CONNECT_HOSTNAME=$(C_REST_API_HOST)"; \
	echo "KEYCLOAK_CONNECT_HOSTNAME=$(C_KEYCLOAK_HOST)"; \
	echo "GARAGE_API_CONNECT_HOSTNAME=$(C_GARAGE_HOST)"; \
	echo "POSTGRES_CONNECT_HOSTNAME=$(C_POSTGRES_HOST)"; \
	echo "KEYCLOAK_CONNECT_PORT=$(C_KEYCLOAK_PORT)"; \
	echo "POSTGRES_CONNECT_PORT=$(C_POSTGRES_PORT)"; \
	echo "GARAGE_API_CONNECT_PORT=$(C_GARAGE_PORT)"; \
	echo ""; \
	echo "# INTERNAL"; \
	echo "REST_API_INTERNAL_HOSTNAME=$(I_REST_API_HOST)"; \
	echo "GATEWAY_INTERNAL_HOSTNAME=$(I_GATEWAY_HOST)"; \
	echo "KEYCLOAK_INTERNAL_HOSTNAME=$(I_KEYCLOAK_HOST)"; \
	echo "GARAGE_API_INTERNAL_HOSTNAME=$(I_GARAGE_HOST)"; \
	echo "GARAGE_UI_INTERNAL_HOSTNAME=$(I_GARAGE_UI_HOST)"; \
	echo "POSTGRES_INTERNAL_HOSTNAME=$(I_POSTGRES_HOST)"; \
	echo "SCRAPPER_INTERNAL_HOSTNAME=$(I_SCRAPPER_HOST)"; \
	echo ""; \
	echo "# HOST PORTS"; \
	echo "GARAGE_API_HOST_PORT=$(H_GARAGE_API_PORT)"; \
	echo "GARAGE_ADMIN_HOST_PORT=$(H_GARAGE_ADMIN_PORT)"; \
	echo "GARAGE_UI_HOST_PORT=$(H_GARAGE_UI_PORT)"; \
	echo "FRONTEND_HOST_PORT=$(H_FRONTEND_PORT)"; \
	echo "POSTGRES_HOST_PORT=$(H_POSTGRES_PORT)"; \
	echo "GATEWAY_HOST_PORT=$(H_GATEWAY_PORT)"; \
	echo "REST_API_HOST_PORT=$(H_REST_API_PORT)"; \
	echo "KEYCLOAK_HOST_PORT=$(H_KEYCLOAK_PORT)"; \
	echo "KEYCLOAK_MANAGEMENT_HOST_PORT=$(H_KEYCLOAK_MGMT_PORT)"; \
	echo ""; \
	echo "# INTERNAL PORTS"; \
	echo "GARAGE_API_INTERNAL_PORT=$(I_GARAGE_API_PORT)"; \
	echo "GARAGE_ADMIN_INTERNAL_PORT=$(I_GARAGE_ADMIN_PORT)"; \
	echo "GARAGE_UI_INTERNAL_PORT=$(I_GARAGE_UI_PORT)"; \
	echo "POSTGRES_INTERNAL_PORT=$(I_POSTGRES_PORT)"; \
	echo "GATEWAY_INTERNAL_PORT=$(I_GATEWAY_PORT)"; \
	echo "REST_API_INTERNAL_PORT=$(I_REST_API_PORT)"; \
	echo "KEYCLOAK_INTERNAL_PORT=$(I_KEYCLOAK_PORT)"; \
	echo "KEYCLOAK_MANAGEMENT_INTERNAL_PORT=$(I_KEYCLOAK_MGMT_PORT)"; \
	echo ""; \
	echo "# Garage S3"; \
	echo "GARAGE_RPC_SECRET=$(GARAGE_RPC_SECRET)"; \
	echo "GARAGE_ADMIN_TOKEN=$(GARAGE_ADMIN_TOKEN)"; \
	echo "GARAGE_METRICS_TOKEN=$(GARAGE_METRICS_TOKEN)"; \
	echo "GARAGE_ACCESS_KEY_ID=$(GARAGE_ACCESS_KEY_ID)"; \
	echo "GARAGE_ACCESS_KEY_SECRET=$(GARAGE_ACCESS_KEY_SECRET)"; \
	echo "GARAGE_REGION=garage"; \
	echo "GARAGE_BUCKET_NAME=coolture-bucket"; \
	echo "GARAGE_KEY_NAME=coolture-key"; \
	echo ""; \
	echo "# Keycloak"; \
	echo "KEYCLOAK_DB_USER=$(KEYCLOAK_DB_USER)"; \
	echo "KEYCLOAK_DB_PASSWORD=$(KEYCLOAK_DB_PASSWORD)"; \
	echo "KEYCLOAK_DB_NAME=$(KEYCLOAK_DB_NAME)"; \
	echo "KEYCLOAK_REALM=$(KEYCLOAK_REALM)"; \
	echo "KEYCLOAK_COOLTURE_SWAGGER_CLIENT_ID=coolture-swagger"; \
	echo "KEYCLOAK_COOLTURE_GATEWAY_CLIENT_ID=coolture-gateway"; \
	echo "KEYCLOAK_COOLTURE_BOT_CLIENT_ID=coolture-bot"; \
	echo "KEYCLOAK_COOLTURE_SWAGGER_CLIENT_SECRET=74in9eNuLAKHEIowc8LheU4CQv3pPx5x"; \
	echo "KEYCLOAK_COOLTURE_GATEWAY_CLIENT_SECRET=YsiygIl2YKRzEyTW7UDnio05PpC8yQdJ"; \
	echo "KEYCLOAK_COOLTURE_BOT_CLIENT_SECRET=$(KEYCLOAK_COOLTURE_BOT_CLIENT_SECRET)"; \
	echo "KEYCLOAK_ADMIN=admin"; \
	echo "KEYCLOAK_ADMIN_PASSWORD=admin"; \
	echo "KEYCLOAK_TEST_USERS_PASSWORD=$(KEYCLOAK_TEST_USERS_PASSWORD)"; \
	echo ""; \
	echo "# Postgres"; \
	echo "POSTGRES_DB=coolture_db"; \
	echo "POSTGRES_USER=admin"; \
	echo "POSTGRES_PASSWORD=admin"; \
	echo ""; \
	echo "# Scrapper"; \
	echo "SCRAPPER_MODE=scheduler"; \
	echo "SCRAPPER_TARGET_URL=https://www.trojmiasto.pl/imprezy/kalendarz-imprez/dni,30dni.html"; \
	echo "SCRAPPER_TIMEZONE=$(SCRAPPER_TIMEZONE)"; \
	echo "SCRAPPER_WRITE_DUMP=$(SCRAPPER_WRITE_DUMP)"; \
	echo "SCRAPPER_OUTPUT_DIR=$(SCRAPPER_OUTPUT_DIR)"; \
	echo "SCRAPPER_INGEST_ENABLED=$(SCRAPPER_INGEST_ENABLED)"; \
	echo "SCRAPPER_API_BASE_URL=$(SCRAPPER_API_BASE_URL)"; \
	echo "SCRAPPER_KEYCLOAK_BASE_URL=$(SCRAPPER_KEYCLOAK_BASE_URL)"; \
	echo "SCRAPPER_KEYCLOAK_REALM=$(SCRAPPER_KEYCLOAK_REALM)"; \
	echo "SCRAPPER_KEYCLOAK_GRANT_TYPE=$(SCRAPPER_KEYCLOAK_GRANT_TYPE)"; \
	echo "SCRAPPER_KEYCLOAK_CLIENT_ID=$(SCRAPPER_KEYCLOAK_CLIENT_ID)"; \
	echo "SCRAPPER_KEYCLOAK_CLIENT_SECRET=$(SCRAPPER_KEYCLOAK_CLIENT_SECRET)"; \
	echo "SCRAPPER_KEYCLOAK_USERNAME=$(SCRAPPER_KEYCLOAK_USERNAME)"; \
	echo "SCRAPPER_KEYCLOAK_PASSWORD=$(SCRAPPER_KEYCLOAK_PASSWORD)"; \
	echo "SCRAPPER_EVENT_CATEGORY_NAME=$(SCRAPPER_EVENT_CATEGORY_NAME)"; \
	echo "SCRAPPER_FALLBACK_CATEGORY_NAME=$(SCRAPPER_FALLBACK_CATEGORY_NAME)"; \
	echo "SCRAPPER_RUN_ON_START=$(SCRAPPER_RUN_ON_START)"; \
	echo "SCRAPPER_SCHEDULE_WINDOW_START_HOUR=$(SCRAPPER_SCHEDULE_WINDOW_START_HOUR)"; \
	echo "SCRAPPER_SCHEDULE_WINDOW_END_HOUR=$(SCRAPPER_SCHEDULE_WINDOW_END_HOUR)"; \
	echo "SCRAPPER_SCHEDULE_SALT=$(SCRAPPER_SCHEDULE_SALT)"; \
	} > .env
	@echo 	"[env] Generated .env  ( \
ENV=$(ENV), \
HOST_NAME=$(HOST_NAME), \
GATEWAY_PORT=$(H_GATEWAY_PORT), \
API_PORT=$(H_REST_API_PORT) \
)"