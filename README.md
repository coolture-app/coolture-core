# coolture

### Generating Environment Variables

`.env` is not versioned. **Generate it before first run.**
Secrets are regenerated on every `make env` call, so Keycloak & GarageFS should be restarted after it.

`CONNECT_*` which are used in Spring apps (to avoid many configurations depending on environment)
variables are set automatically based on `ENV`:

| `ENV` | When to use | `*_CONNECT_HOSTNAME` | `*_CONNECT_PORT` |
| --- | --- | --- | --- |
| `local` | Spring on host, base services in Docker | `localhost` | host ports |
| `dev` | Running in a dev container | `localhost` for Spring services, internal for base | internal ports |
| `deploy` | Homelab / all in docker-compose | internal hostnames | internal ports |

```
make help # for more info, also lot of descriptions in Makefile
```

### Using `.env` Variables in Spring Apps During Local Runs

One way is to source them before running services. Other way is to use IntelliJ Idea
option to add environment variables. To do this you need to:
1. Open: **Run** -> **Edit Configurations...**
2. Select your app run config or create new one (as Spring Boot app)
3. Click: **Modify options**
4. Check option: **Environment variables**
5. Back in app run config there is new box. Provide `.env` file in it.
6. Apply changes (do the same for every run config you use: Gateway, REST API, etc.)
IntelliJ will automatically load all variables from `.env` while starting the app.

### Keycloak Realm Export with User Profiles

Export the `coolture-dev` realm with users to `./keycloak/export/`.
Most of the times you should not version exports.
To prepare exported realm for import in next Keycloak service 
all of the env variables should be provided as in last `realm.json` that was used.

Run these commands from project root folder.

```
docker-compose stop keycloak

docker-compose run --rm \
  -v $(pwd)/keycloak/export:/tmp/export \
  keycloak \
  export \
    --dir /tmp/export \
    --realm coolture-dev \
    --users realm_file
```

### Keycloak Test Users

| Username | Password |
| --- | --- |
| coolture_admin | admin |
| coolture_user | admin |

### Managing Keycloak Through Admin Console

URL: `<hostname>:<keycloak_port>`
login: `admin`
password: `admin`

### Relational Database Design
In `./db/` there is `schema.dbml` file that is version controlled. It is universal file format for storing information about database model. It has a role of an documentation and source of truth for database design. 

Additionally ERD and whole db spec can be generated from this file using `dbdocs` tool. If you want to make changes to the ERD firstly install it: 
`npm install -g dbdocs`

Then use:  
`dbdocs login` and provide credentials in browser which admin will provide on question.  

When changes to `.dbml` file will be made they can be published with:  
`dbdocs <path-to-dbml-file> --project coolture --public --versionName=<changes-made>`

[dbdocs documentation](https://docs.dbdocs.io/)  
[dmbl documentation](https://dbml.dbdiagram.io/home/)

### REST API Contract
In `./api/` there is `contract.yml` file that is version controlled source of truth for API. It contains OpenAPI 3.1 spec describing REST API contract between front and back service to enable parallel development of these and to avoid miscommunications.

Can edit it and preview it easily using Swagger Editor in browser or with `42crunch.vscode-openapi` extension to VS Code.

After full implementation this contract can be abandoned, bcs Spring will provide it's own OpenAPI docs.

### 3miasto Scrapper Service

`scrapper` is now part of `docker-compose.app.yml` and runs on the same `devnet` as other backend services.

- It scrapes from `SCRAPPER_TARGET_URL`.
- Optional JSON dump writing is disabled by default (`SCRAPPER_WRITE_DUMP=false`).
- If enabled, dumps are written inside container filesystem to `SCRAPPER_OUTPUT_DIR` (default `/tmp/scrapper`).
- It can ingest scraped events into DB through authenticated REST API calls:
  - gets OAuth2 access token from Keycloak (`client_credentials`)
  - sends `POST /api/posts` with `Authorization: Bearer <token>`
- It supports:
  - `SCRAPPER_MODE=once` for one-shot runs
  - `SCRAPPER_MODE=scheduler` for continuous daily scheduling

The scheduler picks a different run time every day within:

- `SCRAPPER_SCHEDULE_WINDOW_START_HOUR`
- `SCRAPPER_SCHEDULE_WINDOW_END_HOUR`

Set `SCRAPPER_RUN_ON_START=true` if you want an immediate run on container startup.

For Keycloak/API integration configure:

- `SCRAPPER_INGEST_ENABLED=true`
- `SCRAPPER_API_BASE_URL=http://rest-api:8081/api`
- `SCRAPPER_KEYCLOAK_BASE_URL=http://keycloak:8180`
- `SCRAPPER_KEYCLOAK_REALM=coolture-dev`
- `SCRAPPER_KEYCLOAK_GRANT_TYPE=password` or `client_credentials`
- for `password`:
  - `SCRAPPER_KEYCLOAK_CLIENT_ID=admin-cli`
  - `SCRAPPER_KEYCLOAK_USERNAME=coolture_admin`
  - `SCRAPPER_KEYCLOAK_PASSWORD=admin`
- for `client_credentials`:
  - `SCRAPPER_KEYCLOAK_CLIENT_ID=<service-client-id>`
  - `SCRAPPER_KEYCLOAK_CLIENT_SECRET=<service-client-secret>`
- `SCRAPPER_EVENT_CATEGORY_NAME=<optional-existing-category-name>`
- `SCRAPPER_FALLBACK_CATEGORY_NAME=other` (auto-created when no category exists)