# Retro NPC Swapper

Swaps modern NPC models and animations back to their 2004/2005 look, using the retro assets that
still live in the Old School RuneScape cache.

## What gets swapped

Each category can be toggled individually in the plugin config:

- **Chickens**
- **Goblins**
- **Skeletons** (armed and unarmed)
- **Zombies** (armed and unarmed)
- **Hill Giants (albeit with a Jogre head)**
- **Ghosts** (the Restless ghost swaps its model and idle/walk poses only, as its retro combat
  animations no longer exist — it never fights anyway)

Other retro-era NPCs (dragons, demons, imps, guards) are not yet supported: their 2005
model IDs still resolve in the live cache, but the matching animations were removed or the
multi-part models are missing pieces, so the result no longer looks right. The groundwork for them
is staged in the code and may be enabled if that changes.

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
- **No assets are bundled or downloaded.** The plugin ships only a table of numeric model and
  animation IDs; every asset it displays already exists in your own game cache. All 2005-era model
  IDs still resolve at the same IDs in the live cache.
- Safety settings (on by default) disable all swapping on PvP worlds and in the Wilderness.

There is currently no sanctioned RuneLite API for overriding NPC models, which is why the plugin
utilizes the GPU plugin's draw callbacks.

## Development

- `./gradlew run` starts a development client with the plugin loaded.
- `./gradlew generateNpcMappings` regenerates `npc-mappings.json` from a local 2005 cache
  (original source: https://archive.openrs2.org/caches/runescape/2572) placed in
  `retrocache/2005cache` (the cache itself is never committed).
