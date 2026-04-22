# coolture

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

URL: `<host_address>:<keycloak_port>`
login: `admin`
password: `admin`

### Available Spring Profiles

`local`, `dev`, `homelab`

More than one profile can be set.
Profiles can be selected through `.env` file 
or during local development in IntelliJ Idea build configuration.