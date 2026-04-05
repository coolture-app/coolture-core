# coolture

### zapis realma z userami:

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

### uruchomienie na homelab

```
docker-compose -f docker-compose.yml -f docker-compose.homelab.yml up
```

### testy endpointów

adres: `10.0.0.1:8090/api/swagger-ui.html`

### uzytkownicy testowi

`coolture_admin`
`coolture_user`

do obu pass: `admin`

### zarzadzanie keycloakiem

adres `10.0.0.1:8180`
login: `admin`
hasło: `admin`

### dostepne profile spring

`local`, `dev`, `homelab`