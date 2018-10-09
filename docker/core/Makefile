.RECIPEPREFIX +=

# Create tables
.PHONY: tables
tables:
  java -jar codesearch-core.jar -i

# Download packages. Acceptable values: {haskell, rust, ruby, javascript}
download-%:
  java -jar codesearch-core.jar -d -u -l "$*"

# Index packages. Acceptable values: same as for download-%
index-%:
  java -jar codesearch-core.jar -b -l "$*"
