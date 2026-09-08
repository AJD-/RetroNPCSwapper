# Retro NPC Swapper

Swaps modern NPC models and animations back to their 2004/2005 look, using the retro assets that
still live in the Old School RuneScape cache.

## What gets swapped

Each category can be toggled individually in the plugin config:

- **Chickens**
- **Goblins**
- **Skeletons** (armed and unarmed)
- **Zombies** (armed and unarmed)
- **Giants** — Hill, Fire, Ice and Moss
- **Ghosts**

Under **Experimental**, behind the *Use the injection pipeline* toggle:

- **Dragons** — adult and baby, in all four colours
- **Demons** — lesser, greater and black
- **Imps**
- **Cyclopes**

These need the injection pipeline because swapping IDs is not enough for them. The adult dragon and
demon meshes were removed from the OSRS cache outright — the IDs were reused for unrelated geometry
such as statues and skulls — so there is nothing to swap to. The imp and baby dragon meshes
survived, but for all of them the animation *frames* behind the surviving sequence IDs were
re-authored for the modern skeletons, so the sequences no longer drive the retro meshes. Both the
geometry and the animation therefore come from the 2005 data instead of the live cache.

The giant family is a mixed case, which is why it sits under one toggle in the first list. All five
— the four giants and the cyclops — are the same 2005 body mesh (2870) wearing a different head, and
that body survives in the live cache. Only the fire giant's head survived with it; the hill, ice,
moss and cyclops heads were all reused for unrelated geometry. So **Hill Giants** render either way
(the cache-backed path substitutes a Jogre head for the one that is gone, while the injected path
carries the real 2005 head), and the rest need the injection pipeline to get a head at all. Their
animations were never the problem: sequences 127-131 still resolve to framemap 302 and still fit the
2005 body, so the clips come from the live cache.

**Guards** remain unsupported. They are an animation-only swap with no retro model, so there is no
geometry to inject and nowhere to hang the 2005 frames their sequences no longer carry.

`./gradlew compareRetroModels -Pmodels=<ids> -Pfindmoved` is the tool that settles whether an ID
still holds its 2005 mesh; `./gradlew verifyRetroRigs` settles whether its animation still fits.

## Requirements

**The `GPU plugin` must be enabled.** Models are substituted while the scene is drawn, so
nothing changes while GPU rendering is off, or while another renderer such as 117 HD is in use.
The plugin detects this and simply stands down until the GPU plugin holds the renderer slot again.

## How it works

- The plugin wraps the GPU plugin's draw callbacks and hands the renderer a prebuilt retro model
  whenever an eligible NPC is drawn. Retro pose and combat animations are applied through the
  standard `Actor` animation setters, and the client animates the model as usual.
- **The experimental categories take a second path.** Their geometry never existed in the live
  cache, so the client cannot animate it — `applyTransformations` only accepts the client's own
  model type. The plugin therefore skins and lights those models itself, in Java, reading the frame
  index the client is already driving so the two stay in step.
- **Clickboxes are untouched.** The client resolves clickboxes from the original model before the
  draw callback runs, so interaction hitboxes stay exactly vanilla.
- `Interact Highlight` plugin compatibility: the **Compatibility** section provides a `Fix Interact 
  Highlight outlines` checkbox to resolve inconsistent outline draws on retro models. Because the 
  swap happens at draw time, anything outlining an NPC through the API outlines the modern mesh, 
  which no longer matches what is on screen. With the `Interact Highlight` plugin enabled, and the
  compatibility checkbox enabled, this plugin _turns off_ that plugin's own NPC hover and interact
  outlines and draws them around the retro model instead, using its colors and border settings; its
  object, ground item and player highlights are untouched, and both settings are restored when this
  plugin stops. Note: This means the highlight will trace the rendered model while the clickbox still
  traces the vanilla model, so they can disagree at the edges. Turning either of those two settings
  back on yourself in the `Interact Highlight` config while retro compatibility is enabled hands the
  outlines straight back to the `Interact Highlight` plugin and unticks the compatibility checkbox in
  this plugin.
- **Nothing is downloaded.** For the categories in the first list, the plugin ships only a table of
  numeric model and animation IDs, and every asset it displays already comes from your own game
  cache. Resolving an ID is not the same as it still being the 2005 asset, which is what separates
  those categories from the experimental ones.
- **The injected categories ship their assets.** Dragons, demons, imps, the cyclops and the giant
  heads have no usable 2005 asset left in the live cache, so `retro-assets.dat` (~41 KB) is bundled
  in the jar and carries their meshes, rigs and animation clips, extracted from the February 2005
  cache. This is the one thing the plugin distributes rather than reads from your own installation,
  which is why those categories are gated behind a toggle that is off by default. Parts are stored
  individually and joined at spawn, so the body the whole giant family shares is carried once.
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
- `./gradlew generateRetroAssets` rebuilds `retro-assets.dat` from the same local 2005 cache. It
  bundles the meshes the live cache no longer has, the rigs those meshes are skinned to, and the
  2005 animation clips, resampled onto the live sequences' frame counts so the frame index the
  client drives still lines up.
- `./gradlew verifyRetroRigs` measures *reach* — the share of a clip's transform ops that land on
  vertex groups the mesh actually has. A rig authored for a different mesh scores 47-68%; a
  matching one scores 96-100%. This is what separates "the sequence ID survived" from "the frames
  behind it still fit".
