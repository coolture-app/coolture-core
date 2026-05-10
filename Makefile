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
	echo "KEYCLOAK_COOLTURE_SWAGGER_CLIENT_ID=coolture-swagger"; \
	echo "KEYCLOAK_COOLTURE_GATEWAY_CLIENT_ID=coolture-gateway"; \
	echo "KEYCLOAK_COOLTURE_SWAGGER_CLIENT_SECRET=74in9eNuLAKHEIowc8LheU4CQv3pPx5x"; \
	echo "KEYCLOAK_COOLTURE_GATEWAY_CLIENT_SECRET=YsiygIl2YKRzEyTW7UDnio05PpC8yQdJ"; \
	echo "KEYCLOAK_ADMIN=admin"; \
	echo "KEYCLOAK_ADMIN_PASSWORD=admin"; \
	echo ""; \
	echo "# Postgres"; \
	echo "POSTGRES_DB=coolture_db"; \
	echo "POSTGRES_USER=admin"; \
	echo "POSTGRES_PASSWORD=admin"; \
	} > .env
	@echo 	"[env] Generated .env  ( \
ENV=$(ENV), \
HOST_NAME=$(HOST_NAME), \
GATEWAY_PORT=$(H_GATEWAY_PORT), \
API_PORT=$(H_REST_API_PORT) \
)"