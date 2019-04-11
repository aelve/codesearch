CREATE TABLE package
(
    id          SERIAL        NOT NULL PRIMARY KEY,
    name        VARCHAR(128)  NOT NULL CHECK ( name <> '' ),
    version     VARCHAR(16)   NOT NULL CHECK ( version <> '' ),
    repository  VARCHAR(16)   NOT NULL CHECK ( repository <> '' ),
    maintainers JSONB,
    extra       JSONB,
    created_at  TIMESTAMP     NOT NULL DEFAULT now(),
    latest      BOOLEAN       NOT NULL DEFAULT TRUE,
    keywords    VARCHAR(16)[] NOT NULL DEFAULT []
);

CREATE UNIQUE INDEX idx_unique_package ON package (name, version, repository);

CREATE INDEX idx_keywords_package ON package (keywords);

CREATE INDEX idx_maintainers_package ON package (maintainers);