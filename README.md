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
