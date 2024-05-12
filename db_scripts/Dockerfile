# using mongo image as a base because it conveniently has mongo shell
FROM mongo:4.4.9 as mongo

USER root

RUN apt-get update \
    && apt-get install -y --no-install-recommends postgresql-client=12+214ubuntu0.1 \
    && rm -rf /var/lib/apt/lists/*

USER mongodb

COPY . /scripts

CMD ["/scripts/fill_with_sample_data.sh"]

HEALTHCHECK CMD true
