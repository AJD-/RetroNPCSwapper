# Retro NPC Swapper

Swaps modern NPC models and animations back to their 2004/2005 look, using the retro assets that
still live in the Old School RuneScape cache.

## What gets swapped

Each category can be toggled individually in the plugin config:

- **Chickens**
- **Goblins**
- **Skeletons** (armed and unarmed)
- **Zombies** (armed and unarmed)
- **Hill Giants** (albeit with a Jogre head)
- **Ghosts**

Other retro-era NPCs are not supported. Every 2005 model ID still resolves in the live cache, but
in every case some aspect of the 2005 asset is incompatible, and the reason differs per NPC:

- **Dragons and demons** — the retro meshes have been removed from the OSRS cache. The IDs were 
  reused for unrelated geometry (statues, skulls, and other environment assets) An asset-injection
  API could bring these back, but utilizing something like that would require the plugin to ship
  Jagex copyrighted assets.
- **Imps** — the retro mesh still exists, but the 2005 animation frames were modified in place, 
  with no 2005-era sequence left to swap to.

 `./gradlew compareRetroModels -Pmodels=<ids>
-Pfindmoved` is the tool that settles which case an NPC falls into.

## Requirements

**The `GPU plugin` must be enabled.** Models are substituted while the scene is drawn, so
nothing changes while GPU rendering is off, or while another renderer such as 117 HD is in use.
The plugin detects this and simply stands down until the GPU plugin holds the renderer slot again.

## How it works

- The plugin wraps the GPU plugin's draw callbacks and hands the renderer a prebuilt retro model
  whenever an eligible NPC is drawn. Retro pose and combat animations are applied through the
  standard `Actor` animation setters.
- **Clickboxes are untouched.** The client resolves clickboxes from the original model before the
  draw callback runs, so interaction hitboxes stay exactly vanilla.
- `Interact Highlight` plugin compatibility: the **Compatibility** section provides a `Fix Interact 
  Highlight outlines` checkbox to resolve inconsistent outline draws on retro models. Because the 
  swap happens at draw time, anything outlining an NPC through the API outlines the modern mesh, 
  which no longer matches what is on screen. With the `Interact Highlight` plugin enabled, 
  this plugin _turns off_ that plugin's own NPC hover and interact outlines and draws them around
  the retro model instead, using its colors and border settings; its object, ground item and player
  highlights are untouched, and both settings are restored when this plugin stops. Note this means 
  the highlight traces the rendered model while the clickbox still traces the vanilla model, so they can
  disagree at the edges.
- **No assets are bundled or downloaded.** The plugin ships only a table of numeric model and
  animation IDs; every asset it displays already exists in your own game cache. All 2005-era model
  IDs still resolve at the same IDs in the live cache — though resolving is not the same as
  still being the 2005 asset, which is why only some categories are supported (see above).
- Safety settings (on by default) disable all swapping on PvP worlds and in the Wilderness.

There is currently no sanctioned RuneLite API for overriding NPC models, which is why the plugin
utilizes the GPU plugin's draw callbacks.

## Development

- `./gradlew run` starts a development client with the plugin loaded.
- `./gradlew generateNpcMappings` regenerates `npc-mappings.json` from a local 2005 cache
  (original source: https://archive.openrs2.org/caches/runescape/2572) placed in
  `retrocache/2005cache` (the cache itself is never committed).
- `./gradlew compareRetroModels -Pmodels=2942,2943 -Pfindmoved` decodes a model from both caches
  and compares vertex count, face count and palette, which settles whether an ID still holds its
  2005 asset. Byte comparison cannot: Jagex re-encoded every model for the v2/v3 format markers.
  `-Pfindmoved` rescans all 61,874 live models to separate "the mesh moved to a new ID" from "the
  mesh is gone". The match is exact, so read a `REPLACED` verdict by its magnitude — the chicken
  drifted by one face and reports `REPLACED` while rendering perfectly.
- `./gradlew dumpNpcDefinitions -Pnpc=1173` prints live-cache NPC definitions (IDs or a name
  substring) — models, scales and pose animations, for comparing against the retro definition.
