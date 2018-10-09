FROM anapsix/alpine-java:8 AS build
COPY ../codesearch-server.jar /
RUN apk add git \
  go \
  musl-dev
RUN go get github.com/google/codesearch/cmd/...
RUN rm -rf /var/cache/apk/*
ENV PATH /root/go/bin:$PATH
CMD ["java", "-jar", "codesearch-server.jar"]
