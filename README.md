# Retro NPC Swapper

Swaps modern NPC models and animations back to their 2004/2005 look — using the retro assets that
still live in your own Old School RuneScape cache where they survived, and a small bundled 2005 set
where they did not.

## What gets swapped

Each category can be toggled individually under **NPC Toggles** in the plugin config:

- **Chickens**
- **Goblins**
- **Skeletons** (armed and unarmed)
- **Zombies** (armed and unarmed)
- **Giants** — Hill only
- **Ghosts**

Also in **NPC Toggles**, gated behind *Use Converted 2005 Assets* (on by default):

- **Giants** — Fire, Ice and Moss, under the same Giants toggle as Hill
- **Dragons** — adult and baby
- **Demons** — lesser, greater and black
- **Imps**
- **Cyclopes**
- **Guards** — Varrock, Falador and Ardougne, each in its own town's 2005 kit

These need converted 2005 assets because swapping IDs is not enough for them, and they fail in two
different ways. Some lost the mesh outright: the adult dragon and demon meshes were removed from the
OSRS cache and their IDs reused for unrelated geometry such as statues and skulls, and the fire, ice
and moss giant heads and the cyclops head went the same way, so there is nothing to swap to. Others
kept the mesh but lost the rig: the imp and baby dragon meshes survived, but the animation *frames*
behind their surviving sequence IDs were re-authored for the modern skeletons, and the guard's parts
are byte-identical in both caches with their vertex groups renumbered onto a different rig. Either
way the geometry, the animation, or both have to come from the 2005 data instead of the live cache.

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
- **The converted-2005-asset categories take a second path.** Their geometry is not something the
  client decoded, so the client cannot animate it — `applyTransformations` only accepts the client's
  own model type. The plugin therefore skins and lights those models itself, in Java, reading the
  frame index the client is already driving so the two stay in step.
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
  those categories from the injected ones.
- **The injected categories ship their assets.** Dragons, demons, imps, guards, the cyclops and the
  fire, ice and moss giant heads have no usable 2005 asset left in the live cache, so
  `retro-assets.dat` (~52 KB) is bundled in the jar and carries their meshes, rigs and animation
  clips, extracted from the February 2005 cache. This is the one thing the plugin distributes rather
  than reads from your own installation, which is why it is all gated behind a single toggle you can
  switch off. Parts are stored individually and joined at spawn, so the body the whole giant family
  shares is carried once.
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
  `-Pfindmoved` rescans the whole live model index to separate "the mesh moved to a new ID" from "the
  mesh is gone". The match is exact, so read a `REPLACED` verdict by its magnitude — the chicken
  drifted by one face and reports `REPLACED` while rendering perfectly.
- `./gradlew dumpNpcDefinitions -Pnpc=1173` prints live-cache NPC definitions (IDs or a name
  substring) — models, scales and pose animations, for comparing against the retro definition.
- `./gradlew generateRetroAssets` rebuilds `retro-assets.dat` from the same local 2005 cache. It
  bundles the meshes the live cache no longer has, the rigs those meshes are skinned to, and the
  2005 animation clips, resampled onto the live sequences' frame counts so the frame index the
  client drives still lines up.
- `./gradlew verifyRetroRigs` measures *reach* — the share of a clip's transform ops that land on
  vertex groups the mesh actually has. A rig authored for a different mesh scores 47-68%, against
  96-100% for a mesh animated by its own rig. This is what separates "the sequence ID survived" from
  "the frames behind it still fit". Read it by magnitude rather than as a threshold: a correct
  pairing of a *partial* kit with a full player animation scores lower by construction — the 2005
  guard clips reach 65-80% and are right — so reach rules out a gross mismatch rather than proving
  a fit. `RetroClipReachTest` enforces a per-clip floor against the shipped bundle.
