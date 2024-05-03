FROM postgres:13

USER postgres

# set fixed credentials for testing
ENV POSTGRES_DB=rsww_184529
ENV POSTGRES_PASSWORD=student

# copy all files except for `00_*.sql`
# could be handled by `COPY --exclude` but it's not stable syntax yet
#
# Ignore undefined FROM alias - hadolint can't detect additional contexts
# hadolint ignore=DL3022
COPY --from=db_scripts 0[1-9]_*.sql /docker-entrypoint-initdb.d/
# hadolint ignore=DL3022
COPY --from=db_scripts [1-9][0-9]_*.sql /docker-entrypoint-initdb.d/

# copy samples
#
# Ignore undefined FROM alias - hadolint can't detect additional contexts
# hadolint ignore=DL3022
COPY --from=db_scripts 50_sample[s]/*.sql /docker-entrypoint-initdb.d/

HEALTHCHECK CMD pg_isready || exit 1
