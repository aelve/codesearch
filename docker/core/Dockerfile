FROM anapsix/alpine-java:8 AS build
COPY codesearch-core.jar / 
COPY Makefile /
COPY scripts /scripts/
VOLUME /data/rust/crates.io-index
WORKDIR /data/rust/crates.io-index
RUN git clone https://github.com/rust-lang/crates.io-index
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
