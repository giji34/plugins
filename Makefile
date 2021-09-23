.PHONY: dynmap
dynmap:
	(cd third_party/dynmap && ./gradlew build)

.PHONY: spigot
spigot:
	(cd spigot && ../gradlew build)

.PHONY: velocity
velocity:
	(cd velocity && ../gradlew build)

.PHONY: shared
shared:
	(cd shared && ../gradlew build)
