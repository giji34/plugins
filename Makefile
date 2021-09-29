.PHONY: all
all: shared spigot velocity

.PHONY: dynmap
dynmap:
	(cd third_party/dynmap && ./gradlew build)

.PHONY: spigot
spigot: shared
	./gradlew :spigot:build

.PHONY: velocity
velocity: shared
	./gradlew :velocity:build

.PHONY: clean
clean:
	./gradlew clean

.PHONY: shared
shared:
	./gradlew :shared:build
