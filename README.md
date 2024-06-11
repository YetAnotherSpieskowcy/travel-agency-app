# Microservices-based travel agency app

Implementation of a system for servicing customers interested in purchasing tourist offers using a microservices architecture.

## Running services

To run all services, simply use the `dev-run` make target:
```console
make dev-run
```

### How to deploy to production?

In order to do that, you'll first need to make sure that [you have configured authentication for GitHub Packages registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry#authenticating-to-the-container-registry).
Once you do, you can build and push the images to the registry with following targets (done on dev machine):
```console
make build
make push
```
and then deploy the stack by using the `prod-run` make target (done on cluster master node):
```console
make prod-run
```

Site will be available at port 18452.

> [!NOTE]
> You can modify the default (weak) DB credentials by defining `production.env` file
> and using the `--env-file production.env` flag with `docker compose`.
> You can find the names of the env vars that you may want to override in `default.env`.

## See also

- [travel-agency-tools](https://github.com/YetAnotherSpieskowcy/travel-agency-tools) - Additional tooling for the target application such as the Tour Operator Scraper
- [travel-agency-docs](https://github.com/YetAnotherSpieskowcy/travel-agency-docs) - Formal documentation required by the project requirements
