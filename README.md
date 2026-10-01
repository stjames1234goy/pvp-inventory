# PvP Drops (Fabric 1.21.1)
Keep-inventory stays on for every death EXCEPT when you're killed by another player: then you drop your whole inventory (armor + offhand included; Curse of Vanishing items are destroyed like vanilla). XP is still kept.

## Build on GitHub (no local setup)
1. Create a new GitHub repo and upload ALL files in this folder (including the hidden `.github` folder).
2. Open the repo's **Actions** tab -> "Build mod" run -> download the `pvp-drops-jar` artifact (zip containing the jar).
3. Put the jar in your server's `mods/` folder along with Fabric API (0.116.x for 1.21.1).

## Build locally
JDK 21, then `./gradlew build` -> `build/libs/pvp-drops-1.0.0.jar`.
