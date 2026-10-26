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
package com.retronpcswapper.inject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Everything needed to draw and animate injected geometry: meshes, the rigs they are bound to, and
 * the clips that drive them.
 * <p>
 * Keyed by the ids the source caches use - model id, framemap id, sequence id - so a bundle can
 * be read against the same identifiers the rest of the plugin already speaks, and regenerating it
 * produces stable keys.
 * <p>
 * The exception is a second copy of an id from another source cache. The November 2005 build
 * re-authored the human rig, so its elf meshes and the clips that animate them differ from the
 * February ones stored under the same model and sequence ids. They are stored at
 * {@link #NOV_HUMAN_KEY_BASE} plus the cache id, and an NPC built from them carries that base
 * (see {@code RetroNpcData.getAssetKeyBase}) so every lookup it makes stays inside the namespace.
 */
public final class RetroAssetBundle
{
	/**
	 * Key offset for Nov-2005 human kit: meshes at base + model id, clips at base + sequence id.
	 * Live model ids sit below 70k and sequence ids below 15k, so nothing shares this range.
	 */
	public static final int NOV_HUMAN_KEY_BASE = 1_000_000;

	/**
	 * Key offset for the Feb-2005 goblin chatheads: head meshes at base + model id, emote clips at
	 * base + sequence id. Their head rig is not the human heads', so a human emote played on a
	 * goblin head - which the shared key range would hand it - drives the wrong vertices; kept apart,
	 * an emote the goblin has no clip of its own for leaves the head at rest instead.
	 */
	public static final int GOBLIN_CHATHEAD_KEY_BASE = 2_000_000;

	private final Map<Integer, RetroMesh> meshes;
	private final Map<Integer, RetroRig> rigs;
	private final Map<Integer, RetroClip> clips;

	public RetroAssetBundle(Map<Integer, RetroMesh> meshes, Map<Integer, RetroRig> rigs,
		Map<Integer, RetroClip> clips)
	{
		this.meshes = Collections.unmodifiableMap(new LinkedHashMap<>(meshes));
		this.rigs = Collections.unmodifiableMap(new LinkedHashMap<>(rigs));
		this.clips = Collections.unmodifiableMap(new LinkedHashMap<>(clips));
	}

	public static RetroAssetBundle empty()
	{
		return new RetroAssetBundle(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
	}

	public RetroMesh getMesh(int modelId)
	{
		return meshes.get(modelId);
	}

	public RetroRig getRig(int rigId)
	{
		return rigs.get(rigId);
	}

	public RetroClip getClip(int sequenceId)
	{
		return clips.get(sequenceId);
	}

	public Map<Integer, RetroMesh> getMeshes()
	{
		return meshes;
	}

	public Map<Integer, RetroRig> getRigs()
	{
		return rigs;
	}

	public Map<Integer, RetroClip> getClips()
	{
		return clips;
	}

	public boolean isEmpty()
	{
		return meshes.isEmpty() && rigs.isEmpty() && clips.isEmpty();
	}

	@Override
	public String toString()
	{
		return "RetroAssetBundle{meshes=" + meshes.size()
			+ ", rigs=" + rigs.size()
			+ ", clips=" + clips.size() + "}";
	}
}
