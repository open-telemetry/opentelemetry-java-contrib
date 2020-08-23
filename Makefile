.DEFAULT_GOAL := build

.PHONY: build
build:
	./gradlew build

.PHONY: lint
lint:
	./gradlew spotlessApply

.PHONY: check
check:
	./gradlew spotlessCheck

.PHONY: clean
clean:
	./gradlew clean

.PHONY: assemble
assemble:
	./gradlew assemble

.PHONY: test
test:
	./gradlew test

.PHONY: integration-test
integration-test:
	./gradlew -Pojc.integration.test=true test
