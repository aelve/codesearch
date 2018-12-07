# Note

We use several volumes for storing packages, indices and so on. In the
commands below we use the working directory for storing those volumes.

# Build Docker images

From the repository root:

```
make build
make docker-core docker-web-server
```

# Start the database

```
docker run -d -p 5432:5432 \
  -v $(pwd)/postgres:/var/lib/postgresql \
  -e POSTGRES_DB='sourcesdb' \
  -e POSTGRES_USER='postgres' \
  -e POSTGRES_PASSWORD='postgres' \
  --name codesearch-db postgres
```

# Create tables

```
docker run -d \
  --link codesearch-db:postgres \
  quay.io/aelve/codesearch-core make tables
```

# Download packages meta-information (e.g. Haskell)

```
docker run -d \
  -v $(pwd)/data:/data \
  --link codesearch-db:postgres \
  quay.io/aelve/codesearch-core make download-haskell
```

# Download packages (e.g. Haskell)

```
docker run -d \
  -v $(pwd)/data:/data \
  -v $(pwd)/index:/index \
  --link codesearch-db:postgres \
  quay.io/aelve/codesearch-core make update-haskell
```
# Index packages (e.g. Haskell)

```
docker run -d \
  -v $(pwd)/data:/data \
  -v $(pwd)/index:/index \
  --link codesearch-db:postgres \
  quay.io/aelve/codesearch-core make index-haskell
```

# Start the server

```
docker run -p 9000:80 -d \
  -v $(pwd)/data:/data \
  -v $(pwd)/index:/index \
  --link codesearch-db:postgres \
  --name codesearch-web-server \
  quay.io/aelve/codesearch-web-server make serve
```
Note: If you starting the web-server in production, you should start the server with parameter -e APPLICATION_SECRET={code}. 
Where in {code} you should type code generated from command sbt playGenerateSecret | grep -i 'generated'. 
cloud.digitalocean.com in dplay.filters.hosts.allowed need for loadbalancer healthcheck.
And port 80 -> 80, because load balancer forwading 80 -> 80.
It should look like this:
```
docker run -p 80:80 -d \
  -v $(pwd)/data:/data \
  -v $(pwd)/index:/index \
  --link codesearch-db:postgres \
  --name codesearch-web-server \
  -e APPLICATION_SECRET=f:D5ROEXEDLBI24hcbi9PuHdE \
  -e JAVA_OPTS="-Dplay.filters.hosts.allowed.1=codesearch.aelve.com -Dplay.filters.hosts.allowed.2=cloud.digitalocean.com"\
  quay.io/aelve/codesearch-web-server make serve
```
