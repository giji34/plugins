VERSION := 0.1

build/libs/giji34-$(VERSION).jar: build.gradle $(shell find ./src/main -type f -print)
	./gradlew assemble
	touch $@

.PHONY: deploy
deploy: build/libs/giji34-$(VERSION).jar
	scp $^ giji34:minecraft/plugins/
	scp $^ world06:world06/plugins/
	scp $^ world06:hololive_01/plugins/
