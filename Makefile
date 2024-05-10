DB_SCRIPTS_IMAGE_NAME = 10.40.71.55:5000/rsww_184529_db_scripts

# ran on a dev machine
.PHONY: dev-up
dev-up: build-dev
	# doing a weird set -m / fg dance to allow to
	# still stop the whole stack easily during dev by Ctrl+C
	set -m ; \
	$(MAKE) dev-up-only & \
	$(MAKE) dev-fill-db \
		|| { exit_code=$$?; kill \%1; fg; exit "$$exit_code"; } ; \
	fg

# ran on a dev machine
.PHONY: dev-up-only
dev-up-only:
	docker compose -f compose.yaml -f compose.dev.yaml up

# ran on a dev machine
.PHONY: dev-up-only-daemon
dev-up-only-daemon: build-dev
	docker compose -f compose.yaml -f compose.dev.yaml up -d

# ran on a dev machine
.PHONY: dev-fill-db
dev-fill-db:
	docker run --rm \
		--network host \
		--env-file default.env \
		--env-file dev.env \
		$(DB_SCRIPTS_IMAGE_NAME)

# ran on a dev machine - builds images that are later to be pushed to registry
.PHONY: build
build:
	docker compose build
	# build db_scripts image
	docker build -t $(DB_SCRIPTS_IMAGE_NAME) db_scripts/

# ran on a dev machine
.PHONY: build-dev
build-dev:
	docker compose -f compose.yaml -f compose.dev.yaml build
	# build db_scripts image
	docker build -t $(DB_SCRIPTS_IMAGE_NAME) db_scripts/

# ran on a dev machine - pushes images to the registry
.PHONY: push
push:
	docker compose push
	docker push $(DB_SCRIPTS_IMAGE_NAME)

# ran on the cluster
.PHONY: prod-up
prod-up:
	# fill database (image already in the registry like the rest)
	docker run --rm \
		--env-file default.env \
		$(DB_SCRIPTS_IMAGE_NAME)
	# deploy stack
	docker stack deploy --compose-file compose.yaml rsww_184529
