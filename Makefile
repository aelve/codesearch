.RECIPEPREFIX +=

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
.PHONY: build
build:
  sbt core/assembly
  sbt web-server/assembly

# Download packages
download-%:
  java -jar codesearch-core.jar -d -u -l "$*"

# Index Hackage packages
index-hackage:
  CSEARCHINDEX=data/.hackage_csearch_index cindex data/packages/

# Run the server
.PHONY: serve
serve:
  sbt web-server/run
