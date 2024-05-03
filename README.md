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

When running on production, `local` profile should not be used:
```bash
docker compose up
```

Site will be available at port 8080.

> [!NOTE]
> You can modify the default (weak) DB credentials by defining `production.env` file.
> You can find the names of the env vars that you may want to override in `default.env`.

## See also

- [travel-agency-tools](https://github.com/YetAnotherSpieskowcy/travel-agency-tools) - Additional tooling for the target application such as the Tour Operator Scraper
- [travel-agency-docs](https://github.com/YetAnotherSpieskowcy/travel-agency-docs) - Formal documentation required by the project requirements
