# Create tables
.PHONY: tables
tables:
	java -Dconfig.resource=production.conf -jar codesearch-core.jar -i

# Download package index. Acceptable values: {haskell, rust, ruby, javascript}
download-%:
	java -Dconfig.resource=production.conf -jar codesearch-core.jar -d -l "$*"

# Download missing or outdated packages. Acceptable values: same as for download-%
update-%:
	java -Dconfig.resource=production.conf -jar codesearch-core.jar -u -l "$*"

# Index package sources. Acceptable values: same as for download-%
index-%:
	java -Dconfig.resource=production.conf -jar codesearch-core.jar -b -l "$*"
