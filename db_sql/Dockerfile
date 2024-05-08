FROM postgres:13

USER postgres

# set fixed credentials for testing
ENV POSTGRES_DB=rsww_184529
ENV POSTGRES_PASSWORD=student

HEALTHCHECK CMD pg_isready || exit 1
