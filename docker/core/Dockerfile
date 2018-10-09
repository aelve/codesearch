FROM anapsix/alpine-java:8 AS build
COPY codesearch-core.jar /
COPY docker/core/Makefile /
COPY scripts/update_index.rb /scripts/
COPY scripts/update_npm_index.js /scripts/
WORKDIR "/"
RUN apk update && apk upgrade
RUN apk add make \
  git \
  nodejs \
  nodejs-npm \
  go \
  --no-cache musl-dev \
  curl \
  wget \
  ruby \
  ruby-json
RUN go get github.com/google/codesearch/cmd/...
RUN rm -rf /var/cache/apk/*
ENV PATH /root/go/bin:$PATH
CMD ["make", "tables"]
