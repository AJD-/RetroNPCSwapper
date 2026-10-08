/*
 * Copyright (c) 2026, AJD
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.retronpcswapper;

import com.retronpcswapper.inject.RetroAssetBundle;
import com.retronpcswapper.inject.RetroLighter;
import com.retronpcswapper.inject.RetroMesh;
import com.retronpcswapper.inject.RetroModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.Point;
import net.runelite.api.RuneLiteObjectController;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.hooks.DrawCallbacks;
import net.runelite.client.callback.RenderCallback;

/**
 * Puts 2005 scenery back: the pre-2006 pentagram on the Mystical wall charts, the old well, and
 * whatever else {@link RetroScenery} lists.
 * <p>
 * The live objects and their model ids mostly survive, but the 2005 meshes behind those ids were
 * overwritten and are nowhere else in the live cache, so the 2005 geometry comes from the bundle.
 * <p>
 * Scenery is static, uploaded into the renderer's zone geometry when the scene loads, so there
 * is no per-frame draw to substitute into the way there is for an NPC. Instead:
 * <ul>
 *   <li>The live object is left out of the upload through {@link RenderCallback#drawObject},
 *       which the GPU plugin and 117 HD both consult as they build each zone. Only the drawing is
 *       skipped - the object, its clickbox and its menu are untouched.</li>
 *   <li>A stand-in takes its place carrying a client model - a {@link RetroDecorController} on a
 *       wall, a {@link RetroGameObjectController} on the ground - and the draw callback swaps the
 *       2005 geometry onto it at {@code drawTemp}.</li>
 * </ul>
 * <p>
 * Everything but {@link #drawObject} runs on the client thread.
 */
@Slf4j
@Singleton
public class RetroScenerySwapper implements RenderCallback
{
	/** Scene tiles per renderer zone, as a shift. */
	private static final int ZONE_SHIFT = 3;

	/** Tiles between the extended scene's edge and the scene's first tile. */
	private static final int SCENE_OFFSET = (Constants.EXTENDED_SCENE_SIZE - Constants.SCENE_SIZE) / 2;

	/** Zone index of the scene's first tile - zones are counted from the extended scene's edge. */
	private static final int ZONE_OFFSET = SCENE_OFFSET >> ZONE_SHIFT;

	/** Local units per tile, as a shift. */
	private static final int TILE_SHIFT = 7;

	/** The quarter turns a scenery object can be placed at. */
	private static final int QUARTER_TURNS = 4;

	/** The 2005 client's contrast step: each unit of a definition's opcode 39 is five of the client's. */
	private static final int CONTRAST_STEP = 5;

	@Inject
	private Client client;

	/**
	 * The scenery being hidden and drawn as 2005. Replaced whole, never modified in place.
	 * <p>
	 * Volatile because {@link #drawObject} reads it on the map loader thread.
	 */
	private volatile Set<RetroScenery> active = Collections.emptySet();

	/** Every restored object in the loaded scene, with the stand-ins placed for it - two for a wall chart on both faces of a wall. */
	private final Map<TileObject, List<RuneLiteObjectController>> placed = new HashMap<>();

	/**
	 * The 2005 models, lit and bound, for each scenery the bundle brought - one for each quarter turn
	 * an object can be placed at, indexed by it.
	 *
	 * <p>One per turn because the client turns a scenery model to its placement before it lights it,
	 * so the light falls the same way on every copy whichever way it faces. Lighting the model once
	 * and turning the stand-in instead turns the shading with it: an object placed half a turn round
	 * is lit from behind, and a face the light should reach goes black.
	 */
	private final Map<RetroScenery, RetroModel[]> replacements = new EnumMap<>(RetroScenery.class);

	/** The client models each scenery's stand-ins carry, one per quarter turn, loaded as needed. */
	private final Map<RetroScenery, Model[]> carriers = new EnumMap<>(RetroScenery.class);

	/** The same carriers the other way round, recognized at draw time by identity alone. */
	private final Map<Model, Carried> carried = new IdentityHashMap<>();

	/** Which scenery, at which quarter turn, a carrier stands for. */
	@Value
	private static class Carried
	{
		RetroScenery scenery;
		int quarterTurns;
	}

	/**
	 * Whether a scene object is one this hides: a restored object, placed the way its stand-in
	 * reproduces, while its scenery is active.
	 */
	static boolean shouldHide(TileObject object, Set<RetroScenery> active)
	{
		if (active.isEmpty())
		{
			return false;
		}
		RetroScenery scenery = RetroScenery.forObject(object);
		return scenery != null && active.contains(scenery);
	}

	@Override
	public boolean drawObject(Scene scene, TileObject object)
	{
		return !shouldHide(object, active);
	}

	/**
	 * Takes the 2005 meshes from a freshly loaded bundle.
	 */
	public void setBundle(RetroAssetBundle bundle)
	{
		replacements.clear();
		for (RetroScenery scenery : RetroScenery.values())
		{
			RetroMesh mesh = bundle.getMesh(scenery.meshId);
			if (mesh == null)
			{
				log.debug("Bundle has no mesh {}; {} stays as it is", scenery.meshId, scenery);
				continue;
			}
			RetroModel[] turned = new RetroModel[QUARTER_TURNS];
			for (int quarters = 0; quarters < QUARTER_TURNS; quarters++)
			{
				turned[quarters] = light(rotate(mesh, quarters), scenery);
			}
			replacements.put(scenery, turned);
		}
	}

	/**
	 * Brings the swap in line with what is wanted, doing nothing for scenery already in the state
	 * asked for.
	 *
	 * @param wanted the scenery that should show as 2005 - which also needs a renderer attached to
	 *               draw it, or the live objects would simply vanish
	 */
	public void refresh(Set<RetroScenery> wanted)
	{
		Set<RetroScenery> next = EnumSet.noneOf(RetroScenery.class);
		for (RetroScenery scenery : wanted)
		{
			if (replacements.containsKey(scenery) && carrier(scenery, 0) != null)
			{
				next.add(scenery);
			}
		}

		if (next.equals(active))
		{
			return;
		}

		// What is switching either way
		Set<RetroScenery> changed = EnumSet.noneOf(RetroScenery.class);
		changed.addAll(next);
		changed.addAll(active);
		changed.removeIf(scenery -> next.contains(scenery) && active.contains(scenery));

		if (!next.isEmpty())
		{
			// An object that spawned while its carrier could not be loaded went untracked, and would
			// now be hidden with nothing in its place - so pick up any the spawns missed first
			scanLoadedScene();
		}

		active = Collections.unmodifiableSet(next);
		for (Map.Entry<TileObject, List<RuneLiteObjectController>> entry : placed.entrySet())
		{
			RetroScenery scenery = RetroScenery.forObject(entry.getKey().getId());
			if (!changed.contains(scenery))
			{
				continue;
			}

			for (RuneLiteObjectController controller : entry.getValue())
			{
				if (next.contains(scenery))
				{
					client.registerRuneLiteObject(controller);
				}
				else
				{
					client.removeRuneLiteObject(controller);
				}
			}
		}

		// The live objects already uploaded have to be uploaded again for the change to show
		invalidateZones(changed);
	}

	public void onSpawned(TileObject object)
	{
		RetroScenery scenery = RetroScenery.forObject(object);
		if (scenery == null)
		{
			if (RetroScenery.forObject(object.getId()) != null)
			{
				log.debug("Object {} at {} is placed with config {}; leaving it alone",
					object.getId(), object.getWorldLocation(), configOf(object));
			}
			return;
		}

		if (placed.containsKey(object))
		{
			return;
		}

		List<RuneLiteObjectController> controllers = new ArrayList<>(2);
		if (object instanceof DecorativeObject)
		{
			DecorativeObject decoration = (DecorativeObject) object;
			int config = decoration.getConfig();
			int type = RetroDecorController.type(config);
			for (boolean second : RetroDecorController.isDrawnTwice(type) ? new boolean[]{false, true} : new boolean[]{false})
			{
				Model model = carrier(scenery,
					RetroDecorController.quarterTurns(type, RetroDecorController.orientation(config), second));
				if (model == null)
				{
					return;
				}
				controllers.add(new RetroDecorController(decoration, model, second));
			}
		}
		else
		{
			GameObject gameObject = (GameObject) object;
			Model model = carrier(scenery, RetroDecorController.orientation(gameObject.getConfig()));
			if (model == null)
			{
				return;
			}
			controllers.add(new RetroGameObjectController(gameObject, model));
		}

		placed.put(object, controllers);
		if (active.contains(scenery))
		{
			for (RuneLiteObjectController controller : controllers)
			{
				client.registerRuneLiteObject(controller);
			}
		}
	}

	public void onDespawned(TileObject object)
	{
		List<RuneLiteObjectController> controllers = placed.remove(object);
		if (controllers != null)
		{
			for (RuneLiteObjectController controller : controllers)
			{
				client.removeRuneLiteObject(controller);
			}
		}
	}

	/**
	 * A new scene is loading. Its objects arrive as spawns of their own, and the old ones are not
	 * guaranteed a despawn, so the stand-ins go now.
	 */
	public void onLoading()
	{
		removeAll();
	}

	/**
	 * Picks up the restored objects in a scene that loaded before they could be tracked - at
	 * startup, and as scenery is switched on. From then on the spawn events keep track.
	 */
	public void scanLoadedScene()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		WorldView worldView = client.getTopLevelWorldView();
		Scene scene = worldView == null ? null : worldView.getScene();
		if (scene == null)
		{
			return;
		}

		for (Tile[][] plane : scene.getTiles())
		{
			for (Tile[] column : plane)
			{
				for (Tile tile : column)
				{
					if (tile == null)
					{
						continue;
					}

					DecorativeObject decoration = tile.getDecorativeObject();
					if (decoration != null)
					{
						onSpawned(decoration);
					}

					// An object bigger than a tile is on every tile it covers; onSpawned skips the repeats
					for (GameObject gameObject : tile.getGameObjects())
					{
						if (gameObject != null)
						{
							onSpawned(gameObject);
						}
					}
				}
			}
		}
	}

	/**
	 * Swaps the 2005 geometry onto a stand-in's carrier as it is drawn, or returns null for
	 * anything else.
	 * <p>
	 * Recognized by the model alone. Asking the renderable for its model instead would pose an
	 * NPC or player all over again, into the shared buffer the model being drawn sits in.
	 */
	public Model substitute(Model vanilla)
	{
		Carried carrier = vanilla == null ? null : carried.get(vanilla);
		if (carrier == null || !active.contains(carrier.getScenery()))
		{
			return null;
		}

		RetroModel[] turned = replacements.get(carrier.getScenery());
		return turned == null ? null : turned[carrier.getQuarterTurns()];
	}

	/**
	 * Whether a stand-in being drawn sits where the renderer is hiding the scenery this frame - an
	 * upper floor, under a roof the client has taken off. The object it stands in for is hidden
	 * there, so the stand-in must be too. Anything that is not a stand-in is left to the client.
	 */
	public boolean isHidden(Scene scene, GameObject gameObject, Model model, RetroDrawCallbacks.SceneLevels levels)
	{
		if (model == null || !carried.containsKey(model))
		{
			return false;
		}

		int offset = scene.getWorldViewId() == WorldView.TOPLEVEL ? SCENE_OFFSET : 0;
		int x = (gameObject.getX() >> TILE_SHIFT) + offset;
		int y = (gameObject.getY() >> TILE_SHIFT) + offset;
		int level = gameObject.getPlane();

		byte[][][] settings = scene.getExtendedTileSettings();
		int[][][] roofs = scene.getRoofs();
		if (settings == null || roofs == null || x < 0 || y < 0
			|| x >= settings[0].length || y >= settings[0][0].length)
		{
			return false;
		}

		return isHidden(level, roofId(settings, roofs, level, x, y), levels);
	}

	/**
	 * The renderer's rule for which scenery it draws: the levels from the lowest to the highest
	 * asked for, except that above the player's own level, tiles under a roof the client has taken
	 * off are left out.
	 */
	static boolean isHidden(int level, int roofId, RetroDrawCallbacks.SceneLevels levels)
	{
		if (level < levels.getMinLevel() || level > levels.getMaxLevel())
		{
			return true;
		}
		return level > levels.getLevel() && roofId > 0 && levels.getHideRoofIds().contains(roofId);
	}

	/**
	 * The roof a tile is drawn under, as the renderer works it out when it builds the tile into a
	 * zone: the roof of the level beneath, counting a bridge as a level higher, and none for a tile
	 * that is meant to be seen from below.
	 */
	static int roofId(byte[][][] settings, int[][][] roofs, int level, int x, int y)
	{
		int mapLevel = level;
		if ((settings[1][x][y] & Constants.TILE_FLAG_BRIDGE) != 0)
		{
			mapLevel++;
		}

		boolean visibleBelow = mapLevel <= 3 && (settings[mapLevel][x][y] & Constants.TILE_FLAG_VIS_BELOW) != 0;
		if (visibleBelow || mapLevel == 0)
		{
			return 0;
		}
		return roofs[mapLevel - 1][x][y];
	}

	/**
	 * The renderer zones a footprint covers, from its south-west to its north-east scene tile, each
	 * packed as {@code zoneX << 16 | zoneZ}. An object bigger than a tile can straddle a zone edge;
	 * rebuilding every zone it touches is the safe side of which one the renderer filed it under.
	 */
	static Set<Integer> zones(int minSceneX, int minSceneY, int maxSceneX, int maxSceneY)
	{
		Set<Integer> zones = new HashSet<>();
		for (int zoneX = minSceneX >> ZONE_SHIFT; zoneX <= maxSceneX >> ZONE_SHIFT; zoneX++)
		{
			for (int zoneZ = minSceneY >> ZONE_SHIFT; zoneZ <= maxSceneY >> ZONE_SHIFT; zoneZ++)
			{
				zones.add((zoneX + ZONE_OFFSET) << 16 | (zoneZ + ZONE_OFFSET));
			}
		}
		return zones;
	}

	/**
	 * Puts the live scenery back. Called as the plugin stops.
	 */
	public void shutDown()
	{
		refresh(Collections.emptySet());
		removeAll();
		replacements.clear();
		carriers.clear();
		carried.clear();
	}

	private void removeAll()
	{
		for (List<RuneLiteObjectController> controllers : placed.values())
		{
			for (RuneLiteObjectController controller : controllers)
			{
				client.removeRuneLiteObject(controller);
			}
		}
		placed.clear();
	}

	/**
	 * Has the renderer rebuild every zone holding an object of the given scenery. Only those: a zone
	 * the renderer has not built yet is not safe to invalidate, and one holding a placed object was
	 * built for it.
	 */
	private void invalidateZones(Set<RetroScenery> scenery)
	{
		DrawCallbacks drawCallbacks = client.getDrawCallbacks();
		if (drawCallbacks == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		Map<Scene, Set<Integer>> invalidated = new HashMap<>();
		for (TileObject object : placed.keySet())
		{
			if (!scenery.contains(RetroScenery.forObject(object.getId())))
			{
				continue;
			}

			WorldView worldView = object.getWorldView();
			Scene scene = worldView == null ? null : worldView.getScene();
			if (scene == null)
			{
				continue;
			}

			Set<Integer> done = invalidated.computeIfAbsent(scene, s -> new HashSet<>());
			for (int zone : zonesOf(object))
			{
				if (done.add(zone))
				{
					drawCallbacks.invalidateZone(scene, zone >>> 16, zone & 0xFFFF);
				}
			}
		}
	}

	private static Set<Integer> zonesOf(TileObject object)
	{
		if (object instanceof GameObject)
		{
			GameObject gameObject = (GameObject) object;
			Point min = gameObject.getSceneMinLocation();
			Point max = gameObject.getSceneMaxLocation();
			return zones(min.getX(), min.getY(), max.getX(), max.getY());
		}

		LocalPoint location = object.getLocalLocation();
		return zones(location.getSceneX(), location.getSceneY(), location.getSceneX(), location.getSceneY());
	}

	private static int configOf(TileObject object)
	{
		if (object instanceof DecorativeObject)
		{
			return ((DecorativeObject) object).getConfig();
		}
		return object instanceof GameObject ? ((GameObject) object).getConfig() : -1;
	}

	/**
	 * The live model a scenery's stand-ins at one quarter turn carry - lit afresh, so the instance is
	 * that scenery's at that turn alone and is told apart at draw time from every other model, its
	 * own live copy included.
	 */
	private Model carrier(RetroScenery scenery, int quarterTurns)
	{
		Model[] turned = carriers.computeIfAbsent(scenery, s -> new Model[QUARTER_TURNS]);
		if (turned[quarterTurns] == null)
		{
			ModelData data = client.loadModelData(scenery.meshId);
			Model carrier = data == null ? null : data.light();
			if (carrier != null)
			{
				turned[quarterTurns] = carrier;
				carried.put(carrier, new Carried(scenery, quarterTurns));
			}
		}
		return turned[quarterTurns];
	}

	/**
	 * A mesh turned by whole quarter turns about its vertical axis, the way the client turns a
	 * scenery model to its placement before lighting it. Shares everything but the turned vertex
	 * positions with the mesh it came from.
	 */
	static RetroMesh rotate(RetroMesh mesh, int quarterTurns)
	{
		if (quarterTurns == 0)
		{
			return mesh;
		}

		int count = mesh.getVerticesCount();
		float[] x = new float[count];
		float[] z = new float[count];
		for (int v = 0; v < count; v++)
		{
			float[] turned = rotate(mesh.getVerticesX()[v], mesh.getVerticesZ()[v], quarterTurns);
			x[v] = turned[0];
			z[v] = turned[1];
		}

		return new RetroMesh(mesh.getId(), mesh.getPriority(), x, mesh.getVerticesY(), z,
			mesh.getFaceIndices1(), mesh.getFaceIndices2(), mesh.getFaceIndices3(),
			mesh.getFaceColors(), mesh.getFaceRenderTypes(), mesh.getFaceTransparencies(),
			mesh.getFaceRenderPriorities(), mesh.getFaceTextures(),
			mesh.getTextureCoords(), mesh.getTexIndices1(), mesh.getTexIndices2(), mesh.getTexIndices3(),
			mesh.getVertexGroups());
	}

	/**
	 * Turns a point by whole quarter turns, the way the renderer turns a model by its orientation:
	 * {@code x' = z sin + x cos, z' = z cos - x sin}.
	 */
	static float[] rotate(float x, float z, int quarterTurns)
	{
		switch (quarterTurns & 3)
		{
			case 1:
				return new float[]{z, -x};
			case 2:
				return new float[]{-x, -z};
			case 3:
				return new float[]{-z, x};
			default:
				return new float[]{x, z};
		}
	}

	/**
	 * Lights a 2005 mesh the way the 2005 client lit scenery: the client's base ambient and
	 * contrast plus the definition's own adjustments.
	 */
	private static RetroModel light(RetroMesh mesh, RetroScenery scenery)
	{
		int faceCount = mesh.getFaceCount();
		int[] colors1 = new int[faceCount];
		int[] colors2 = new int[faceCount];
		int[] colors3 = new int[faceCount];

		RetroLighter.light(
			mesh.getVerticesCount(), mesh.getVerticesX(), mesh.getVerticesY(), mesh.getVerticesZ(),
			faceCount, mesh.getFaceIndices1(), mesh.getFaceIndices2(), mesh.getFaceIndices3(),
			mesh.getFaceColors(), mesh.getFaceRenderTypes(), mesh.getFaceTextures(),
			ModelData.DEFAULT_AMBIENT + scenery.ambient,
			ModelData.DEFAULT_CONTRAST + scenery.contrast * CONTRAST_STEP,
			ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z,
			colors1, colors2, colors3);

		RetroModel model = new RetroModel();
		model.bind(mesh, colors1, colors2, colors3);
		return model;
	}
}
