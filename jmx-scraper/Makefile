# https://github.com/open-telemetry/weaver/releases
# v0.18.0
WEAVER_CONTAINER=otel/weaver@sha256:5425ade81dc22ddd840902b0638b4b6a9186fb654c5b50c1d1ccd31299437390

# Next - we want to run docker as our local file user, so generated code is not
# owned by root, and we don't give unnecessary access.
#
# Determine if "docker" is actually podman
DOCKER_VERSION_OUTPUT := $(shell docker --version 2>&1)
DOCKER_IS_PODMAN := $(shell echo $(DOCKER_VERSION_OUTPUT) | grep -c podman)
ifeq ($(DOCKER_IS_PODMAN),0)
    DOCKER_COMMAND := docker
else
    DOCKER_COMMAND := podman
endif
DOCKER_RUN=$(DOCKER_COMMAND) run
DOCKER_USER=$(shell id -u):$(shell id -g)
DOCKER_USER_IS_HOST_USER_ARG=-u $(DOCKER_USER)
ifeq ($(DOCKER_COMMAND),podman)
 # On podman, additional arguments are needed to make "-u" work
 # correctly with the host user ID and host group ID.
 #
 #      Error: OCI runtime error: crun: setgroups: Invalid argument
 DOCKER_USER_IS_HOST_USER_ARG=--userns=keep-id -u $(DOCKER_USER)
endif

# Generate the rules files from the model
.PHONY: generate-rules
generate-rules:
	mkdir -p output
	$(DOCKER_RUN) \
		$(DOCKER_USER_IS_HOST_USER_ARG) \
		--mount 'type=bind,source=$(PWD)/model,target=/home/weaver/model,readonly' \
		--mount 'type=bind,source=$(PWD)/templates,target=/home/weaver/templates,readonly' \
		--mount 'type=bind,source=$(PWD)/output,target=/home/weaver/output' \
		--mount 'type=bind,source=$(PWD),target=/home/weaver' \
		${WEAVER_CONTAINER} registry generate \
		--registry /home/weaver/model \
		rules \
		--future \
		/home/weaver/output
	cp ./output/jvm.yaml ./src/main/resources/jvm.yaml

# for Debugging purposes only:  run a shell in the weaver container
.PHONY: weaver-shell
weaver-shell:
	mkdir -p output
	@echo "DOCKER_USER_IS_HOST_USER_ARG is: $(DOCKER_USER_IS_HOST_USER_ARG)"
	$(DOCKER_RUN) \
		$(DOCKER_USER_IS_HOST_USER_ARG) \
		--mount 'type=bind,source=$(PWD)/model,target=/home/weaver/model,readonly' \
		--mount 'type=bind,source=$(PWD)/templates,target=/home/weaver/templates,readonly' \
		--mount 'type=bind,source=$(PWD)/output,target=/home/weaver/output' \
		--mount 'type=bind,source=$(PWD),target=/home/weaver' \
		--entrypoint sh \
		-it ${WEAVER_CONTAINER}
