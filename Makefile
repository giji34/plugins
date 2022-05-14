.PHONY: all
all: shared spigot velocity

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

# jabba use $(cat .java-version)
