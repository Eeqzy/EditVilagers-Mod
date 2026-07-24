# Build

Need JDK 21 (for 1.21.x) and JDK 25 (for 26.x). Gradle comes with the wrapper.

Supported:
- 1.21.1 / 1.21.10 / 1.21.11 — Fabric + NeoForge (`src/`)
- 26.1 — Fabric + NeoForge (`src-26/`)
- 26.2 — Fabric only (`src-26/`)

## Build everything

```bat
.\gradlew.bat chiseledBuild --configure-on-demand
```

One version:

```bat
.\gradlew.bat :1.21.10-fabric:build --configure-on-demand
.\gradlew.bat :26.2-fabric:build --configure-on-demand
```

Jars end up like `editvillager-fabric-1.2.0+26.2.jar`.

## Clean

```bat
.\gradlew.bat clean --configure-on-demand
```

## In-game deps

Architectury API + Fabric API

Better to test with a normal launcher, not `runClient`
