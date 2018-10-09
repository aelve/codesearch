.RECIPEPREFIX +=

# Create tables
.PHONY: tables
tables:
  java -jar codesearch-core.jar -i

# Download packages
download-%:
  java -jar codesearch-core.jar -d -u -l "$*"

# Index haskell packages
index-haskell:
  java -jar codesearch-core.jar -b -l "haskell"

# Index js packages
index-javascript:
  java -jar codesearch-core.jar -b -l "javascript"

# Index rust packages
index-rust:
  java -jar codesearch-core.jar -b -l "rust"

# Index ruby packages
index-ruby:
  java -jar codesearch-core.jar -b -l "ruby"
