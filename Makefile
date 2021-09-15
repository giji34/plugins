build/libs/giji34-*.jar: build.gradle $(shell find ./src/main -type f -print) third_party/dynmap/target/dynmap-api-3.2-SNAPSHOT.jar
	./gradlew assemble
	touch $@

third_party/dynmap/target/dynmap-api-3.2-SNAPSHOT.jar: ./third_party/dynmap/build.gradle $(shell find ./third_party/dynmap -name '*.java' -type f -print)
	(cd third_party/dynmap && ./gradlew build)

.PHONY: deploy
deploy:
	$(MAKE) clean
	$(MAKE) build/libs/giji34-*.jar
	scp build/libs/giji34-*.jar main.giji34:jar/giji34/

.PHONY: clean
clean:
	(cd third_party/dynmap && ./gradlew clean)
	rm -f build/libs/giji34-*.jar

.PHONY: run
run:
	./gradlew assemble
	(cd minecraft/plugins; rm -f giji34.jar; ln -s "$$(ls -1 ../../build/libs/*.jar)" ./giji34.jar; cd ..; java -jar server.jar nogui)
