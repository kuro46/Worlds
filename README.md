# Worlds

A simple world manager for Bukkit 1.12.x ~ 1.15.x

## Getting started

All of commands registered by Worlds are starts with `/world`.
`/world help` shows help message for all commands.

## For multiverse-core users

### Migrate worlds

This plugin does not conflict with Multiverse-Core.  
**So you can use this plugin and Multiverse-Core together.**  
But it is possible to move ownership of world.

**Example: Move ownership of world `foo` into Worlds**

First, remove ownership of `foo` from Multiverse
```
/mv remove foo
```
Second, import `foo` to Worlds
```
/world import foo
```

### Commands

It is recommended to add aliases like below.

```yml
aliases:
  mvtp
  - "world tp $$1"
  mvs
  - "world spawn"
```

## For developer

### Add to dependency

#### Maven

For Maven users, please add repository first.
```xml
<repository>
    <id>shirokuro-repo</id>
    <url>https://maven.shirokuro.xyz/repos/releases/</url>
</repository>
```
And then, add to dependency.
```xml
<dependency>
    <groupId>xyz.shirokuro</groupId>
    <artifactId>worlds</artifactId>
    <version>0.2.0</version>
    <scope>provided</scope>
</dependency>
```

#### Gradle

For Gradle(Groovy DSL) users, please add repository first.
```groovy
maven { url 'https://maven.shirokuro.xyz/repos/releases/' }
```
And then, add to dependency.
```groovy
compileOnly 'xyz.shirokuro:worlds:0.2.0'
```

### Examples

#### Get spawn location of the world

```java
final World world = Bukkit.getWorld("hoge");
final WorldConfigList worldConfigList = WorldsPlugin.getInstance().getWorldConfigList();
// WorldConfig has all of configuration configured by Worlds
// null if Worlds doesn't have ownership of "hoge"
final WorldConfig worldConfig = worldConfigList.get(world).orElse(null);
// Coord only have data about coordinate(X, Y, Z, Pitch, Yaw).
// It doesn't have any world info.
final Coord coord = worldConfig.getSpawn();
// Create Location instance with specifie world
final Location location = coord.withWorld(wordl);
```
