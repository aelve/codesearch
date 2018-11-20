.RECIPEPREFIX +=

# Create tables
.PHONY: tables
tables:
  java -Dconfig.resource=production.conf -jar codesearch-core.jar -i

# Downloading metainformation about packages in repos
metaDownload-%:
  java -Dconfig.resource=production.conf -jar codesearch-core.jar -d -l "$*"

# Downloading packages. Acceptable values: same as for download-%
packagesDownload-%:
  java -Dconfig.resource=production.conf -jar codesearch-core.jar -u -l "$*"

# Creating index from downloaded packages
index-%:
  java -Dconfig.resource=production.conf -jar codesearch-core.jar -b -l "$*"
