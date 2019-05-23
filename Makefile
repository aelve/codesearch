SHELL := /bin/bash

port := 9000

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


# Destroy Postgres
.PHONY: db-kill
db-kill:
	docker stop codesearch-db
	docker rm codesearch-db

# Build the project
build:
	sbt core/assembly web-server/assembly

# Download package index. Acceptable values: {haskell, rust, ruby, javascript}
download-%:
	java -jar codesearch-core.jar -d -l "$*"

# Download missing or outdated packages. Acceptable values: same as for download-%
update-%:
	java -jar codesearch-core.jar -u -l "$*"

# Index package sources. Acceptable values: same as for download-%
index-%:
	java -jar codesearch-core.jar -b -l "$*"

# Run the server
.PHONY: serve
serve:
	LOG_LEVEL=DEBUG java -Dhttp.port=$(port) -Dplay.http.secret.key=devsecret \
		-jar codesearch-server.jar

# Build a Docker image (the project must be built already)
build-docker-%:
	if [ "$(branch)" == "master" ] || [ "$(branch)" == "develop" ]; \
	then \
		docker build \
			-f "docker/$*/Dockerfile" \
			-t "quay.io/aelve/codesearch-$*:$(branch)" . ; \
	else \
		docker build \
			-f "docker/$*/Dockerfile" \
			-t "quay.io/aelve/codesearch-$*:latest" . ; \
	fi \

	if [ "$(branch)" == "master" ]; \
	then \
		docker tag \
			"quay.io/aelve/codesearch-$*:master" \
			"quay.io/aelve/codesearch-$*:latest"; \
	fi

# Push a Docker image to Quay
push-docker-%:
	if [ -n "$(branch)" ]; \
	then \
		if [ "$(branch)" == "master" ]; \
		then \
			docker push "quay.io/aelve/codesearch-$*:master"; \
			docker push "quay.io/aelve/codesearch-$*:latest"; \
		else \
			docker push "quay.io/aelve/codesearch-$*:$(branch)"; \
		fi \
	else \
		echo "Empty branch parameter."; \
	fi
