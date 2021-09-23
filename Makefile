.PHONY: all
all: shared spigot velocity

.PHONY: dynmap
dynmap:
	(cd third_party/dynmap && ./gradlew build)

.PHONY: spigot
spigot:
	./gradlew :spigot:build

.PHONY: velocity
velocity:
	./gradlew :velocity:build

.PHONY: shared
shared:
	./gradlew :shared:build
