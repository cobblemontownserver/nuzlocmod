# Soulink Nuzlocke (Cobblemon 1.7.x / Minecraft 1.21.1 / Fabric)

Nuzlocke rules for Cobbleverse and any Cobblemon server.

## Rules
- **One catch per biome.** The first Pokemon you catch in a biome is the only one you get there. Later throws in that biome bounce off.
- **Soul-linked.** Every Pokemon you own (including your starter) is bound to you.
- **Faint = released.** If a soul-linked Pokemon faints in battle, it is released when the battle ends and goes to your graveyard.
- **Shiny clause.** Shinies can always be caught and don't use up the biome.
- **Dupes clause.** You can't catch a species you already own; the biome stays open.

## Install
Drop the jar in the `mods` folder. On a server, it only needs to be on the server. In singleplayer, put it in your Cobbleverse instance's `mods` folder.

## Commands
- `/nuzlocke` or `/nuzlocke status` - your run summary and whether the biome you're standing in is used.
- `/nuzlocke graveyard` - your fallen Pokemon.
- `/nuzlocke biomes` - biomes you've used.
- Admin: `/nuzlocke admin reset <player>`, `/nuzlocke admin toggle <player>`, `/nuzlocke admin unlockbiome <player> <biome id>`, `/nuzlocke admin reload`.

## Config
`config/soulink-nuzlocke.json` is created on first launch. Every rule above is a true/false switch there.
