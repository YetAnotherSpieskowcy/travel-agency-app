# Microservices-based travel agency app

Implementation of a system for servicing customers interested in purchasing tourist offers using a microservices architecture.

## Running services

To run all services, simply use the `dev-up` make target:
```console
make dev-up
```

### How to run on production?

When running on production, you should first upload the images to registry (done on dev machine):
```console
make build
make push
```
and then run the stack by using the `prod-up` make target (done on cluster master node):
```console
make dev-up
```

Site will be available at port 8080.

> [!NOTE]
> You can modify the default (weak) DB credentials by defining `production.env` file
> and using the `--env-file production.env` flag with `docker compose`.
> You can find the names of the env vars that you may want to override in `default.env`.

## See also

- [travel-agency-tools](https://github.com/YetAnotherSpieskowcy/travel-agency-tools) - Additional tooling for the target application such as the Tour Operator Scraper
- [travel-agency-docs](https://github.com/YetAnotherSpieskowcy/travel-agency-docs) - Formal documentation required by the project requirements
