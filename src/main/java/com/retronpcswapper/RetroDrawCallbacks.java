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

import java.util.Set;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.api.Model;
import net.runelite.api.Projection;
import net.runelite.api.Renderable;
import net.runelite.api.Scene;
import net.runelite.api.SceneTileModel;
import net.runelite.api.SceneTilePaint;
import net.runelite.api.Texture;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.hooks.DrawCallbacks;

/**
 * Decorates the renderer currently holding {@code Client.setDrawCallbacks} (the bundled GPU plugin,
 * or 117 HD's zone renderer) so retro geometry can be substituted for an NPC, a player or a placed
 * carrier at draw time.
 *
 * <p>Only {@link #drawTemp} and {@link #drawDynamic} do anything other than forward. Temporary
 * entities (NPCs, players, projectiles, spotanims) are drawn through the first, and dynamic objects
 * (animated scenery, ground items) through the second. Either way the {@code Model} arrives as a
 * parameter, so handing the delegate a different one is enough to change what is rendered. The
 * delegate keeps doing all the actual upload work.
 *
 * <p>Every other method forwards verbatim. This is deliberate and load bearing: the methods on
 * {@link DrawCallbacks} are {@code default} no-ops, so any method left un-overridden here would
 * silently drop that part of rendering rather than fail loudly - omitting
 * {@link #drawScenePaint} alone would make terrain disappear.
 *
 * <p>Note the clickbox is unaffected by substitution. Under the ZBUF path the client resolves the
 * model, culls clickboxes and registers the hit target before invoking these callbacks, so the
 * clickbox continues to describe the original model. That addresses the "modifying, moving, or
 * resizing the clickboxes of in-game elements is strictly prohibited" rule.
 *
 * <p>The same split is why anything that outlines an NPC through the API - {@code Actor#getModel()}
 * is read-only and never routes through here - traces the original silhouette rather than the one
 * on screen. {@link com.retronpcswapper.compatibility.RetroInteractHighlightOverlay} redraws
 * those outlines around the substituted geometry, so the highlight follows what is rendered while
 * the clickbox still follows the original model, and the two can disagree at the edges.
 */
@Slf4j
public class RetroDrawCallbacks implements DrawCallbacks
{
	/**
	 * Supplies replacement geometry for a temporary entity - an NPC, a player, or a carrier this
	 * plugin placed - or {@code null} to leave it alone.
	 * <p>
	 * Called for every temporary entity drawn, projectiles and spotanims included, so it has to
	 * turn away what it does not recognize cheaply.
	 */
	@FunctionalInterface
	public interface ModelSubstitutor
	{
		Model substitute(Renderable renderable, Model vanilla);
	}

	/**
	 * Decides whether a temporary entity should be left out of the frame entirely.
	 * <p>
	 * The renderer culls static scenery on upper floors by level and roof, but leaves temporary
	 * entities to the client - which culls its own, and not the stand-ins this plugin places for
	 * static scenery. This is where those get the same treatment the scenery they replace gets.
	 */
	@FunctionalInterface
	public interface TempFilter
	{
		boolean isHidden(Scene scene, GameObject gameObject, Model model, SceneLevels levels);
	}

	/**
	 * The levels the client asked the renderer to draw this frame, as handed to
	 * {@code preSceneDraw}: everything from {@code minLevel} to {@code maxLevel}, except that above
	 * {@code level} the roofs named in {@code hideRoofIds} are left out.
	 */
	@Value
	public static class SceneLevels
	{
		int minLevel;
		int level;
		int maxLevel;
		Set<Integer> hideRoofIds;
	}

	@Getter
	private final DrawCallbacks delegate;

	private final ModelSubstitutor substitutor;

	/**
	 * For dynamic objects - animated scenery and ground items. Unlike {@link #substitutor} this is
	 * called from the renderer's own threads, possibly several at once, so it must be thread safe.
	 */
	private final ModelSubstitutor dynamicSubstitutor;

	private final TempFilter tempFilter;

	/** This frame's levels, or null before the first frame. */
	private SceneLevels levels;

	public RetroDrawCallbacks(DrawCallbacks delegate, ModelSubstitutor substitutor,
		ModelSubstitutor dynamicSubstitutor, TempFilter tempFilter)
	{
		this.delegate = delegate;
		this.substitutor = substitutor;
		this.dynamicSubstitutor = dynamicSubstitutor;
		this.tempFilter = tempFilter;
	}

	@Override
	public void drawTemp(Projection worldProjection, Scene scene, GameObject gameObject, Model m, int orient, int x, int y, int z)
	{
		Model substitute;

		// Never allow a substitution failure to take the renderer down with it
		try
		{
			if (levels != null && tempFilter.isHidden(scene, gameObject, m, levels))
			{
				return;
			}
			substitute = substitutor.substitute(gameObject.getRenderable(), m);
		}
		catch (Exception ex)
		{
			substitute = null;
			log.debug("Retro model substitution failed, drawing the vanilla model", ex);
		}

		delegate.drawTemp(worldProjection, scene, gameObject, substitute != null ? substitute : m, orient, x, y, z);
	}

	// --- Everything below forwards verbatim -------------------------------------------------

	@Override
	public void draw(Projection projection, Scene scene, Renderable renderable, int orientation, int x, int y, int z, long hash)
	{
		delegate.draw(projection, scene, renderable, orientation, x, y, z, hash);
	}

	@Override
	public void drawScenePaint(Scene scene, SceneTilePaint paint, int plane, int tileX, int tileZ)
	{
		delegate.drawScenePaint(scene, paint, plane, tileX, tileZ);
	}

	@Override
	public void drawSceneTileModel(Scene scene, SceneTileModel model, int tileX, int tileZ)
	{
		delegate.drawSceneTileModel(scene, model, tileX, tileZ);
	}

	@Override
	public void draw(int overlayColor)
	{
		delegate.draw(overlayColor);
	}

	@Override
	public void drawScene(double cameraX, double cameraY, double cameraZ, double cameraPitch, double cameraYaw, int plane)
	{
		delegate.drawScene(cameraX, cameraY, cameraZ, cameraPitch, cameraYaw, plane);
	}

	@Override
	public void postDrawScene()
	{
		delegate.postDrawScene();
	}

	@Override
	public void animate(Texture texture, int diff)
	{
		delegate.animate(texture, diff);
	}

	@Override
	public void loadScene(Scene scene)
	{
		delegate.loadScene(scene);
	}

	@Override
	public void swapScene(Scene scene)
	{
		delegate.swapScene(scene);
	}

	@Override
	public boolean tileInFrustum(Scene scene, float pitchSin, float pitchCos, float yawSin, float yawCos,
		int cameraX, int cameraY, int cameraZ, int plane, int msx, int msy)
	{
		return delegate.tileInFrustum(scene, pitchSin, pitchCos, yawSin, yawCos, cameraX, cameraY, cameraZ, plane, msx, msy);
	}

	@Override
	public boolean zoneInFrustum(int zoneX, int zoneZ, int maxY, int minY)
	{
		return delegate.zoneInFrustum(zoneX, zoneZ, maxY, minY);
	}

	@Override
	public void loadScene(WorldView worldView, Scene scene)
	{
		delegate.loadScene(worldView, scene);
	}

	@Override
	public void despawnWorldView(WorldView worldView)
	{
		delegate.despawnWorldView(worldView);
	}

	@Override
	public void preSceneDraw(Scene scene, Projection entityProjection,
		float cameraX, float cameraY, float cameraZ, float cameraPitch, float cameraYaw,
		int minLevel, int level, int maxLevel, Set<Integer> hideRoofIds)
	{
		levels = new SceneLevels(minLevel, level, maxLevel, hideRoofIds);
		delegate.preSceneDraw(scene, entityProjection, cameraX, cameraY, cameraZ, cameraPitch, cameraYaw,
			minLevel, level, maxLevel, hideRoofIds);
	}

	@Override
	@Deprecated
	public void preSceneDraw(Scene scene,
		float cameraX, float cameraY, float cameraZ, float cameraPitch, float cameraYaw,
		int minLevel, int level, int maxLevel, Set<Integer> hideRoofIds)
	{
		levels = new SceneLevels(minLevel, level, maxLevel, hideRoofIds);
		delegate.preSceneDraw(scene, cameraX, cameraY, cameraZ, cameraPitch, cameraYaw,
			minLevel, level, maxLevel, hideRoofIds);
	}

	@Override
	public void postSceneDraw(Scene scene)
	{
		delegate.postSceneDraw(scene);
	}

	@Override
	public void drawPass(Projection entityProjection, Scene scene, int pass)
	{
		delegate.drawPass(entityProjection, scene, pass);
	}

	@Override
	public void drawZoneOpaque(Projection entityProjection, Scene scene, int zx, int zz)
	{
		delegate.drawZoneOpaque(entityProjection, scene, zx, zz);
	}

	@Override
	public void drawZoneAlpha(Projection entityProjection, Scene scene, int level, int zx, int zz)
	{
		delegate.drawZoneAlpha(entityProjection, scene, level, zx, zz);
	}

	@Override
	public void drawDynamic(Projection worldProjection, Scene scene, TileObject tileObject, Renderable r, Model m,
		int orient, int x, int y, int z)
	{
		delegate.drawDynamic(worldProjection, scene, tileObject, r, substituteDynamic(r, m), orient, x, y, z);
	}

	@Override
	public void drawDynamic(int renderThreadId, Projection worldProjection, Scene scene, TileObject tileObject,
		Renderable r, Model m, int orient, int x, int y, int z)
	{
		delegate.drawDynamic(renderThreadId, worldProjection, scene, tileObject, r, substituteDynamic(r, m),
			orient, x, y, z);
	}

	/**
	 * The dynamic counterpart of the substitution in {@link #drawTemp}, with the same guarantee:
	 * a failure draws the vanilla model rather than nothing.
	 */
	private Model substituteDynamic(Renderable r, Model m)
	{
		try
		{
			Model substitute = dynamicSubstitutor.substitute(r, m);
			return substitute != null ? substitute : m;
		}
		catch (Exception ex)
		{
			log.debug("Retro dynamic model substitution failed, drawing the vanilla model", ex);
			return m;
		}
	}

	@Override
	public void invalidateZone(Scene scene, int zx, int zz)
	{
		delegate.invalidateZone(scene, zx, zz);
	}
}
