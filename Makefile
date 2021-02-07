build/libs/giji34-*.jar: build.gradle $(shell find ./src/main -type f -print)
	./gradlew assemble
	touch $@

.PHONY: deploy
deploy:
	$(MAKE) clean
	$(MAKE) build/libs/giji34-*.jar
	scp build/libs/giji34-*.jar main.giji34:jar/giji34/

.PHONY: clean
clean:
	rm -f build/libs/giji34-*.jar
