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
