DB_SCRIPTS_IMAGE_NAME = ghcr.io/yetanotherspieskowcy/rsww_184529_db_scripts

# [ran on a dev machine]
# Builds and runs the whole dev stack, making sure to fill the DB
.PHONY: dev-run
dev-run: build-dev
	# doing a weird set -m / fg dance to allow to
	# still stop the whole stack easily during dev by Ctrl+C
	set -m ; \
	$(MAKE) dev-up-no-build & \
	$(MAKE) dev-fill-db \
		|| { exit_code=$$?; kill \%1; fg; exit "$$exit_code"; } ; \
	fg

# [ran on a dev machine]
# Builds, runs the whole dev stack, and watches it for changes, making sure to fill the DB
.PHONY: dev-run
watch-dev-run: build-dev
	# doing a weird set -m / fg dance to allow to
	# still stop the whole stack easily during dev by Ctrl+C
	set -m ; \
	$(MAKE) watch-dev-up-no-build & \
	$(MAKE) dev-fill-db \
		|| { exit_code=$$?; kill \%1; fg; exit "$$exit_code"; } ; \
	fg

# [ran on a dev machine]
# Runs the whole dev stack (w/o building images or filling the DB)
.PHONY: dev-up-no-build
dev-up-no-build:
	docker compose -f compose.yaml -f compose.dev.yaml up

# [ran on a dev machine]
# Runs the whole dev stack and watches it for changes (w/o building images or filling the DB)
.PHONY: dev-up-no-build
watch-dev-up-no-build:
	docker compose -f compose.yaml -f compose.dev.yaml up --watch

# [ran on a dev machine]
# Builds the whole stack and then runs it in the background (daemonized)
.PHONY: dev-up-daemon
dev-up-daemon: build-dev
	docker compose -f compose.yaml -f compose.dev.yaml up -d

# [ran on a dev machine]
# Fills the DB running on the development stack
.PHONY: dev-fill-db
dev-fill-db:
	docker run --rm \
		--network host \
		--env-file default.env \
		--env-file dev.env \
		$(DB_SCRIPTS_IMAGE_NAME)

# [ran on a dev machine]
# Builds images for the production stack
.PHONY: build
build:
	docker compose build
	# build db_scripts image
	docker build -t $(DB_SCRIPTS_IMAGE_NAME) db_scripts/

# [ran on a dev machine]
# Builds images for the development stack
.PHONY: build-dev
build-dev:
	docker compose -f compose.yaml -f compose.dev.yaml build
	# build db_scripts image
	docker build -t $(DB_SCRIPTS_IMAGE_NAME) db_scripts/

# [ran on a dev machine]
# Pushes production images to the registry
.PHONY: push
push:
	docker compose push
	docker push $(DB_SCRIPTS_IMAGE_NAME)

# [ran on the cluster]
# Fills the DB and deploys the production stack to the cluster
#
# NOTE: production images need to be in the registry
.PHONY: prod-run
prod-run:
	$(MAKE) prod-fill-db
	$(MAKE) prod-up

# [ran on the cluster]
# Fills the shared DB on production
#
# NOTE: db_scripts image needs to be in the registry
.PHONY: prod-fill-db
prod-fill-db:
	docker run --rm \
		--env-file default.env \
		$(DB_SCRIPTS_IMAGE_NAME)

# [ran on the cluster]
# Deploys the production stack to the cluster (w/o filling the DB)
#
# NOTE: production stack images need to be in the registry
.PHONY: prod-up
prod-up:
	docker stack deploy --compose-file compose.yaml rsww_184529
