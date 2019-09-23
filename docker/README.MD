# Note

We use several volumes for storing packages, indices and so on. In the
commands below we use the working directory for storing those volumes.

# Build Docker images

From the repository root:

```
make build
make build-docker-core build-docker-web-server
```

Note: If you starting the web-server in production, you should start the server with environment parameter `SECRET_KEY=$SECRET_KEY`. 
Where in $SECRET_KEY you should type code generated from command:

`sbt playGenerateSecret | grep -i 'generated'. `

cloud.digitalocean.com in dplay.filters.hosts.allowed need for loadbalancer healthcheck.
Also set environment parameter  `SERVER_PORT=$SERVER_PORT` where `$SERVER_PORT` equals 80, because load balancer forwarding 80 -> 80.
It should look like this:
```
SECRET_KEY=$SECRET_KEY SERVER_PORT=$SERVER_PORT docker stack deploy --with-registry-auth -c docker-stack-compose.yml codesearch
```
# Start the service with all tasks (download, update, index) for all languages via docker-compose

```
docker-compose up -d
```