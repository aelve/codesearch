FROM anapsix/alpine-java:8 AS build
COPY codesearch-core.jar / 
COPY Makefile /
WORKDIR "/"
RUN apk add make 
ENTRYPOINT ["make"]
#CMD ["java", "-jar", "codesearch-core.jar", "-i"]
