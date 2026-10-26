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
package com.retronpcswapper.chathead;

import com.retronpcswapper.inject.RetroAssetBundle;
import com.retronpcswapper.inject.RetroClip;
import com.retronpcswapper.inject.RetroMesh;
import com.retronpcswapper.inject.RetroRig;
import lombok.Getter;

/**
 * A 2005 dialogue head, built for drawing: its merged and recolored mesh, its colors lit once at
 * rest as the client lights an interface model, and where in the bundle its emotes are keyed.
 */
@Getter
public final class RetroHead
{
	private final RetroMesh mesh;

	/** Added to an emote's sequence id to find its clip, see {@code RetroNpcData.getChatheadKeyBase}. */
	private final int keyBase;
	private final RetroAssetBundle source;
	private final int[] lit1;
	private final int[] lit2;
	private final int[] lit3;

	public RetroHead(RetroMesh mesh, int keyBase, RetroAssetBundle source, int[] lit1, int[] lit2, int[] lit3)
	{
		this.mesh = mesh;
		this.keyBase = keyBase;
		this.source = source;
		this.lit1 = lit1;
		this.lit2 = lit2;
		this.lit3 = lit3;
	}

	/**
	 * The head's clip for an emote, or null when it has none. Looked up only inside the head's own
	 * key range, so a Nov head never plays a Feb clip bound to the other rig.
	 */
	public RetroClip clip(int sequenceId)
	{
		return sequenceId < 0 ? null : source.getClip(keyBase + sequenceId);
	}

	/** The rig a clip of this head's moves, or null for no clip. */
	public RetroRig rig(RetroClip clip)
	{
		return clip == null ? null : source.getRig(clip.getRigId());
	}
}
