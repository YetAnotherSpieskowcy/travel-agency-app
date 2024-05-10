FROM mongo:4.4.9 as mongo

RUN apt-get update \
    && apt install -y --no-install-recommends postgresql-client \
    && rm -rf /var/lib/apt/lists/*

COPY . /scripts

CMD ["/scripts/fill_with_sample_data.sh"]
