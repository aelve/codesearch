# note

We use several volumes for storing packages, indices and so on. In the
commands below they are mounted into `vol/` in the current directory.

# start database

```
docker run -d -p 5432:5432 \
  -v $(pwd)/vol/postgres:/var/lib/postgresql \
  -e POSTGRES_DB='sourcesdb' \
  -e POSTGRES_USER='postgres' \
  -e POSTGRES_PASSWORD='postgres' \
  --name codesearch-db postgres
```

# create tables

```
docker run -d \
  --link codesearch-db:postgres \
  codesearch-core make tables
```

# download packages (e.g. Haskell)

```
docker run -d \
  -v $(pwd)/vol/packages:/data \
  -v $(pwd)/vol/npm:/index/npm \
  --link codesearch-db:postgres \
  codesearch-core make download-haskell
```

# start server

```
docker run -p 9000:80 -d \
  -v $(pwd)/vol/packages:/data \
  --link codesearch-db:postgres \
  --name codesearch-web-server codesearch-web-server
```
