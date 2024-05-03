FROM mongo:4.4.9

USER mongodb

# set fixed credentials for testing
ENV MONGO_INITDB_DATABASE=rsww_184529
ENV MONGO_INITDB_ROOT_USERNAME=root
ENV MONGO_INITDB_ROOT_PASSWORD=student

# Ignore undefined FROM alias - hadolint can't detect additional contexts
# hadolint ignore=DL3022
COPY --from=db_scripts * /docker-entrypoint-initdb.d/

HEALTHCHECK CMD \
    mongo --quiet localhost:27017/test \
    --eval 'quit(db.runCommand({ ping: 1 }).ok ? 0 : 2)' \
    || exit 1
