# Microservices-based travel agency app

Implementation of a system for servicing customers interested in purchasing tourist offers using a microservices architecture.

## Running services

Build all the nessesary services.
```bash
docker compose build
```

When build finishes start all containers using the following command.
```bash
docker compose --profile local up
```

After containers are running, you'll need to fill the DB with sample data:
```bash
export $(cat default.env dev.env | xargs)
db_scripts/fill_with_sample_data.sh
```

### How to run on production?

When running on production, you should first fill the database with sample data:
```bash
export $(cat default.env | xargs)
db_scripts/fill_with_sample_data.sh
```
and then run the stack without the `local` profile (as databases are already provided separately):
```bash
docker stack deploy --compose-file compose.yaml rsww_184529
```

Site will be available at port 8080.

> [!NOTE]
> You can modify the default (weak) DB credentials by defining `production.env` file
> and using the `--env-file production.env` flag with `docker compose`.
> You can find the names of the env vars that you may want to override in `default.env`.

## See also

- [travel-agency-tools](https://github.com/YetAnotherSpieskowcy/travel-agency-tools) - Additional tooling for the target application such as the Tour Operator Scraper
- [travel-agency-docs](https://github.com/YetAnotherSpieskowcy/travel-agency-docs) - Formal documentation required by the project requirements
