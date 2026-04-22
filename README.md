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
