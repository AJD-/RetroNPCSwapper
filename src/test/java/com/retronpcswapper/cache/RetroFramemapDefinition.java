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
package com.retronpcswapper.cache;

import lombok.Data;

/**
 * One 2005 framemap - a skeleton, in the engine's own terms: an ordered list of transforms, each
 * naming the vertex groups it moves.
 *
 * <p>Equivalent to the modern {@code FramemapDefinition}, with two differences. The 2005 framemap
 * is <b>embedded at the tail of its index 2 frame group and carries no id of its own</b>, so callers
 * assign one. And its group lists are stored <b>interleaved</b>, {@code {count, members}} per
 * transform, where the modern loader writes every count first and then every member; the two
 * layouts occupy the same number of bytes, so only the decoded values tell them apart.
 */
@Data
public class RetroFramemapDefinition
{
	/** Assigned by the caller - see {@link RetroFrameDecoder#rigId(int)}. */
	private final int id;

	/** Transform type per index: 0 pivot, 1 translate, 2 rotate, 3 scale, 5 alpha. */
	private final int[] types;

	/** Vertex groups each transform moves. */
	private final int[][] groups;

	public int getLength()
	{
		return types.length;
	}
}
