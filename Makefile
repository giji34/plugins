.PHONY: all
all: spigot velocity

.PHONY: dynmap
dynmap:
	(cd third_party/dynmap && ./gradlew build)

.PHONY: spigot
spigot:
	./gradlew :spigot:build

.PHONY: velocity
velocity:
	./gradlew :velocity:build
