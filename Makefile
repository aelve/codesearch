.RECIPEPREFIX +=

# Run Postgres
db:
  docker run --name codesearch-db \
    -e POSTGRES_DB=sourcesdb \
    -e POSTGRES_USER=postgres \
    -e POSTGRES_PASSWORD=postgres \
    -p 5432:5432 -d postgres -N 1000

# Create tables
tables:
  java -jar codesearch-core.jar -i

# Destroy Postgres
db-kill:
  docker stop codesearch-db
  docker rm codesearch-db

# Build the project
build:
  sbt core/assembly
  sbt web-server/assembly

# Run the indexer
index:
  sbt core/run

# Run the server
server:
  sbt web-server/run
