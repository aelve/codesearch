.RECIPEPREFIX +=

# Create tables
.PHONY: tables
tables:
  java -Dconfig.resource=production.conf -jar codesearch-core.jar -i

# Download packages meta-information. Acceptable values: {haskell, rust, ruby, javascript}
download-%:
  java -Dconfig.resource=production.conf -jar codesearch-core.jar -d -l "$*"

# Update packages. Acceptable values: {haskell, rust, ruby, javascript}
update-%:
  java -Dconfig.resource=production.conf -jar codesearch-core.jar -u -l "$*"

# Index packages. Acceptable values: same as for download-%
index-%:
  java -Dconfig.resource=production.conf -jar codesearch-core.jar -b -l "$*"
