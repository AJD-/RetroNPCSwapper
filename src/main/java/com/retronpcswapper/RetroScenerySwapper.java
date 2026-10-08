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
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.hooks.DrawCallbacks;
import net.runelite.client.callback.RenderCallback;

/**
 * Puts the pre-2006 pentagram back on the Mystical wall charts.
 *
 * <p>The wall chart is object 908 - today's Magical symbol. The object and its model id 2086 both
 * survive, but the 2005 mesh behind that id was overwritten when the pentagrams were removed in
 * January 2006 and is nowhere else in the live cache, so the pentagram comes from the bundle.
 *
 * <p>A wall decoration is static scenery, uploaded into the renderer's zone geometry when the scene
 * loads, so there is no per-frame draw to substitute into the way there is for an NPC. Instead:
 * <ul>
 *   <li>The Magical symbol is left out of the upload through {@link RenderCallback#drawObject},
 *       which the GPU plugin and 117 HD both consult as they build each zone. Only the drawing is
 *       skipped - the object, and its Examine clickbox, are untouched.</li>
 *   <li>A {@link RetroDecorController} stands in its place carrying a client model, and the draw
 *       callback swaps the pentagram onto it at {@code drawTemp}.</li>
 * </ul>
 *
 * <p>Everything but {@link #drawObject} runs on the client thread.
 */
@Slf4j
@Singleton
public class RetroScenerySwapper implements RenderCallback
{
	/** The wall chart's model id, in the bundle as its 2005 mesh and in the live cache as the symbol. */
	static final int CHART_MODEL = 2086;

	/** The 2005 definition's ambient adjustment (loc opcode 29), on top of the client's base. */
	private static final int CHART_AMBIENT = ModelData.DEFAULT_AMBIENT + 50;

	/** Scene tiles per renderer zone, as a shift. */
	private static final int ZONE_SHIFT = 3;

	/** Tiles between the extended scene's edge and the scene's first tile. */
	private static final int SCENE_OFFSET = (Constants.EXTENDED_SCENE_SIZE - Constants.SCENE_SIZE) / 2;

	/** Zone index of the scene's first tile - zones are counted from the extended scene's edge. */
	private static final int ZONE_OFFSET = SCENE_OFFSET >> ZONE_SHIFT;

	/** Local units per tile, as a shift. */
	private static final int TILE_SHIFT = 7;

	@Inject
	private Client client;

	/**
	 * Whether the symbols are being hidden and the pentagrams drawn.
	 *
	 * <p>Volatile because {@link #drawObject} reads it on the map loader thread.
	 */
	private volatile boolean active;

	/** Every wall chart in the loaded scene, with the stand-ins placed for it - two on a wall it decorates both faces of. */
	private final Map<DecorativeObject, List<RetroDecorController>> charts = new HashMap<>();

	/** The pentagram, lit and bound, or null until the bundle brings one. */
	private RetroModel pentagram;

	/** The client model the stand-ins carry, recognized at draw time. */
	private Model carrier;

	/**
	 * Whether a scene object is one this hides.
	 *
	 * <p>The {@code instanceof} is load-bearing: this is also asked about temporary entities, the
	 * stand-ins among them, and those must never be hidden.
	 */
	static boolean shouldHide(TileObject object, boolean active)
	{
		return active
			&& object instanceof DecorativeObject
			&& object.getId() == ObjectID.WITCHESWALLCHART
			&& RetroDecorController.isSupportedType(RetroDecorController.type(((DecorativeObject) object).getConfig()));
	}

	@Override
	public boolean drawObject(Scene scene, TileObject object)
	{
		return !shouldHide(object, active);
	}

	/**
	 * Takes the pentagram from a freshly loaded bundle.
	 */
	public void setBundle(RetroAssetBundle bundle)
	{
		RetroMesh mesh = bundle.getMesh(CHART_MODEL);
		pentagram = mesh == null ? null : light(mesh);
		if (pentagram == null)
		{
			log.debug("Bundle has no mesh {}; the wall charts stay as they are", CHART_MODEL);
		}
	}

	/**
	 * Brings the swap in line with what is wanted, doing nothing if it already is.
	 *
	 * @param wanted whether the pentagrams should show - which also needs a renderer attached to
	 *               draw them, or the symbols would simply vanish
	 */
	public void refresh(boolean wanted)
	{
		boolean next = wanted && pentagram != null && carrier() != null;
		if (next == active)
		{
			return;
		}

		if (next)
		{
			// A chart that spawned while the carrier could not be loaded went untracked, and would
			// now be hidden with nothing in its place - so pick up any the spawns missed first
			scanLoadedScene();
		}

		active = next;
		for (List<RetroDecorController> controllers : charts.values())
		{
			for (RetroDecorController controller : controllers)
			{
				if (active)
				{
					client.registerRuneLiteObject(controller);
				}
				else
				{
					client.removeRuneLiteObject(controller);
				}
			}
		}

		// The symbols already uploaded have to be uploaded again for the change to show
		invalidateZones();
	}

	public void onSpawned(DecorativeObject decoration)
	{
		if (decoration.getId() != ObjectID.WITCHESWALLCHART || charts.containsKey(decoration))
		{
			return;
		}

		int type = RetroDecorController.type(decoration.getConfig());
		if (!RetroDecorController.isSupportedType(type))
		{
			log.debug("Wall chart at {} has placement type {}; leaving it alone",
				decoration.getWorldLocation(), RetroDecorController.type(decoration.getConfig()));
			return;
		}

		Model model = carrier();
		if (model == null)
		{
			return;
		}

		List<RetroDecorController> controllers = new ArrayList<>(2);
		controllers.add(new RetroDecorController(decoration, model, false));
		if (RetroDecorController.isDrawnTwice(type))
		{
			controllers.add(new RetroDecorController(decoration, model, true));
		}

		charts.put(decoration, controllers);
		if (active)
		{
			for (RetroDecorController controller : controllers)
			{
				client.registerRuneLiteObject(controller);
			}
		}
	}

	public void onDespawned(DecorativeObject decoration)
	{
		List<RetroDecorController> controllers = charts.remove(decoration);
		if (controllers != null)
		{
			for (RetroDecorController controller : controllers)
			{
				client.removeRuneLiteObject(controller);
			}
		}
	}

	/**
	 * A new scene is loading. Its decorations arrive as spawns of their own, and the old ones are not
	 * guaranteed a despawn, so the stand-ins go now.
	 */
	public void onLoading()
	{
		removeAll();
	}

	/**
	 * Picks up the wall charts in a scene that loaded before the plugin started. A one-off, run at
	 * startup - from then on the spawn events keep track.
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
					DecorativeObject decoration = tile == null ? null : tile.getDecorativeObject();
					if (decoration != null)
					{
						onSpawned(decoration);
					}
				}
			}
		}
	}

	/**
	 * Swaps the pentagram onto a stand-in's carrier as it is drawn, or returns null for anything
	 * else.
	 *
	 * <p>Recognized by the model alone. Asking the renderable for its model instead would pose an
	 * NPC or player all over again, into the shared buffer the model being drawn sits in.
	 */
	public Model substitute(Model vanilla)
	{
		return active && vanilla != null && vanilla == carrier ? pentagram : null;
	}

	/**
	 * Whether a stand-in being drawn sits where the renderer is hiding the scenery this frame - an
	 * upper floor, under a roof the client has taken off. The symbol it stands in for is hidden
	 * there, so the pentagram must be too. Anything that is not a stand-in is left to the client.
	 */
	public boolean isHidden(Scene scene, GameObject gameObject, Model model, RetroDrawCallbacks.SceneLevels levels)
	{
		if (model == null || model != carrier)
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
	 * Puts the Magical symbols back. Called as the plugin stops.
	 */
	public void shutDown()
	{
		refresh(false);
		removeAll();
		pentagram = null;
		carrier = null;
	}

	private void removeAll()
	{
		for (List<RetroDecorController> controllers : charts.values())
		{
			for (RetroDecorController controller : controllers)
			{
				client.removeRuneLiteObject(controller);
			}
		}
		charts.clear();
	}

	/**
	 * Has the renderer rebuild every zone holding a wall chart. Only those: a zone the renderer has
	 * not built yet is not safe to invalidate, and one holding a chart was built for it.
	 */
	private void invalidateZones()
	{
		DrawCallbacks drawCallbacks = client.getDrawCallbacks();
		if (drawCallbacks == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		Map<Scene, Set<Integer>> invalidated = new HashMap<>();
		for (DecorativeObject decoration : charts.keySet())
		{
			WorldView worldView = decoration.getWorldView();
			Scene scene = worldView == null ? null : worldView.getScene();
			if (scene == null)
			{
				continue;
			}

			LocalPoint location = decoration.getLocalLocation();
			int zoneX = (location.getSceneX() >> ZONE_SHIFT) + ZONE_OFFSET;
			int zoneZ = (location.getSceneY() >> ZONE_SHIFT) + ZONE_OFFSET;
			if (invalidated.computeIfAbsent(scene, s -> new HashSet<>()).add(zoneX << 16 | zoneZ))
			{
				drawCallbacks.invalidateZone(scene, zoneX, zoneZ);
			}
		}
	}

	/** The live Magical symbol, as a client model the stand-ins can carry. */
	private Model carrier()
	{
		if (carrier == null)
		{
			carrier = client.loadModel(CHART_MODEL);
		}
		return carrier;
	}

	/**
	 * Lights the 2005 mesh the way the client lights scenery: the client's base ambient plus the
	 * definition's own adjustment.
	 */
	private static RetroModel light(RetroMesh mesh)
	{
		int faceCount = mesh.getFaceCount();
		int[] colors1 = new int[faceCount];
		int[] colors2 = new int[faceCount];
		int[] colors3 = new int[faceCount];

		RetroLighter.light(
			mesh.getVerticesCount(), mesh.getVerticesX(), mesh.getVerticesY(), mesh.getVerticesZ(),
			faceCount, mesh.getFaceIndices1(), mesh.getFaceIndices2(), mesh.getFaceIndices3(),
			mesh.getFaceColors(), mesh.getFaceRenderTypes(), mesh.getFaceTextures(),
			CHART_AMBIENT, ModelData.DEFAULT_CONTRAST,
			ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z,
			colors1, colors2, colors3);

		RetroModel model = new RetroModel();
		model.bind(mesh, colors1, colors2, colors3);
		return model;
	}
}
