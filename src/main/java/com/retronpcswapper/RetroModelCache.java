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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Animation;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.NPC;

/**
 * Holds the retro replacement geometry, built once per NPC id and reused every frame.
 *
 * <p>The draw callback runs per entity per frame, so it must do a map lookup and nothing else.
 * Everything expensive - decoding model data, merging and lighting - happens here, driven from
 * NPC spawn and transform events rather than from the render path.
 */
@Singleton
@Slf4j
public class RetroModelCache
{
	@Inject
	private Client client;

	/** Unposed, lit retro models keyed by NPC id. */
	private final Map<Integer, Model> baseModels = new HashMap<>();

	/** Animations keyed by sequence id - loadAnimation per NPC per frame would be wasteful. */
	private final Map<Integer, Animation> animations = new HashMap<>();

	/** NPC ids whose retro models could not be built, so spawns stop retrying them. */
	private final Set<Integer> unbuildable = new HashSet<>();

	/**
	 * Returns the cached retro model for an NPC id, or null if none has been built.
	 * Safe to call from the render path.
	 */
	public Model get(int npcId)
	{
		return baseModels.get(npcId);
	}

	/**
	 * Builds and caches the retro model for an NPC id if it is not already present.
	 * Must be called on the client thread.
	 */
	public void ensureBuilt(int npcId, RetroNpcData data)
	{
		if (data == null || baseModels.containsKey(npcId) || unbuildable.contains(npcId))
		{
			return;
		}

		Model model = build(data);
		if (model == null)
		{
			// Remember the failure so every subsequent spawn does not repeat the work
			unbuildable.add(npcId);
			log.debug("Could not build retro model for NPC id {}", npcId);
			return;
		}

		baseModels.put(npcId, model);
		log.debug("Built retro model for NPC id {} from {} model ids", npcId, data.getRetroModelIds().length);
	}

	/**
	 * Resolves an animation id, caching the result. Returns null for -1 or an unknown id.
	 */
	public Animation animation(int animationId)
	{
		if (animationId == -1)
		{
			return null;
		}

		// computeIfAbsent is avoided so a null from loadAnimation is not retried every frame
		if (animations.containsKey(animationId))
		{
			return animations.get(animationId);
		}

		Animation animation = client.loadAnimation(animationId);
		animations.put(animationId, animation);
		return animation;
	}

	/**
	 * Poses the cached retro model for an NPC, or null when nothing has been built for its id.
	 *
	 * <p>The returned model is shared and is invalidated by the next applyTransformations call,
	 * including the client's own, so it has to be consumed before anything else runs. Both callers
	 * do: the draw callback hands it straight to the renderer, and the outline renderer projects
	 * and rasterizes it before returning. Must be called on the client thread.
	 */
	public Model pose(NPC npc)
	{
		Model base = baseModels.get(npc.getId());
		if (base == null)
		{
			return null;
		}

		Animation action = animation(npc.getAnimation());
		Animation pose = animation(npc.getPoseAnimation());
		return client.applyTransformations(base, action, npc.getAnimationFrame(), pose, npc.getPoseAnimationFrame());
	}

	public void clear()
	{
		baseModels.clear();
		animations.clear();
		unbuildable.clear();
	}

	private Model build(RetroNpcData data)
	{
		int[] modelIds = data.getRetroModelIds();
		if (modelIds == null || modelIds.length == 0)
		{
			return null;
		}

		ModelData[] parts = new ModelData[modelIds.length];
		int found = 0;
		for (int modelId : modelIds)
		{
			ModelData part = client.loadModelData(modelId);
			if (part != null)
			{
				parts[found++] = part;
			}
		}

		if (found == 0)
		{
			return null;
		}

		ModelData merged = found == 1 ? parts[0] : client.mergeModels(parts, found);
		if (merged == null)
		{
			return null;
		}

		int scaleXZ = data.getScaleXZ();
		int scaleY = data.getScaleY();
		if (scaleXZ != 128 || scaleY != 128)
		{
			// cloneVertices first - loadModelData hands back the client's shared arrays, and
			// scaling in place would resize the cached model for everything else using it
			merged = merged.cloneVertices().scale(scaleXZ, scaleY, scaleXZ);
		}

		return merged.light();
	}
}
