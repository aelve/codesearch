# Run Postgres
.PHONY: db
db:
	docker start codesearch-db || docker run --name codesearch-db \
		-e POSTGRES_DB=sourcesdb \
		-e POSTGRES_USER=postgres \
		-e POSTGRES_PASSWORD=postgres \
		-p 5432:5432 -d postgres -N 1000

# Connect to Postgres
.PHONY: psql
psql:
	docker run -it --rm --link codesearch-db:postgres \
		-e PGPASSWORD=postgres \
		postgres psql -h postgres -U postgres sourcesdb

# Create tables
.PHONY: tables
tables:
	java -jar codesearch-core.jar -i

# Destroy Postgres
.PHONY: db-kill
db-kill:
	docker stop codesearch-db
	docker rm codesearch-db

# Build the project
build:
	sbt core/assembly web-server/assembly

# Download packages. Acceptable values: {haskell, rust, ruby, javascript}
download-%:
	java -jar codesearch-core.jar -d -u -l "$*"

# Index packages. Acceptable values: same as for download-%
index-%:
	java -jar codesearch-core.jar -b -l "$*"

# Run the server
.PHONY: serve
serve:
	java -jar codesearch-server.jar

# Build a Docker image (the project must be built already)
build-docker-%:
	docker build \
		-f "docker/$*/Dockerfile" \
		-t "quay.io/aelve/codesearch-$*:local" .
	docker tag \
		"quay.io/aelve/codesearch-$*:local" \
		"quay.io/aelve/codesearch-$*:latest"

# Push a Docker image to Quay
push-docker-%:
	docker push "quay.io/aelve/codesearch-$*:latest"
