FROM anapsix/alpine-java:8 AS build
COPY codesearch-core.jar / 
COPY Makefile /
COPY scripts /scripts/
WORKDIR "/"
RUN apk update && apk upgrade
RUN apk add make 
RUN apk add git
RUN apk add nodejs
RUN apk add nodejs-npm
RUN apk add go
RUN apk add --no-cache musl-dev
RUN apk add curl wget
RUN apk add ruby ruby-bundler
RUN go get github.com/google/codesearch/cmd/...
RUN npm install lodash
RUN rm -rf /var/cache/apk/*
ENV PATH /root/go/bin:$PATH
CMD ["make", "tables"]
