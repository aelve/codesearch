FROM anapsix/alpine-java:8 AS build
COPY codesearch-server.jar /
RUN apk add git
RUN apk add go
RUN apk add --no-cache musl-dev
RUN go get github.com/google/codesearch/cmd/...
ENV PATH /root/go/bin:$PATH
CMD ["java", "-jar", "codesearch-server.jar"]
