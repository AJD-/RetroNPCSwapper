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

import java.util.Arrays;
import lombok.Getter;

/**
 * One animation sequence, as a list of frames of transform operations.
 * <p>
 * Frames are addressed by the index the client is already using: {@code Actor#getAnimationFrame}
 * indexes the live sequence's frame list, so a clip decoded from the live cache lines up with what
 * the server is driving, for free. That is why clips come from the live cache even for NPCs whose
 * mesh does not.
 * <p>
 * Ops are four parallel arrays per frame rather than objects, because this is walked per NPC per
 * frame.
 */
public final class RetroClip
{
	@Getter
	private final int sequenceId;

	/** The rig every frame of this clip is expressed against. */
	@Getter
	private final int rigId;

	/** Per frame: the rig transform index each op applies to. */
	private final int[][] transforms;
	private final int[][] dx;
	private final int[][] dy;
	private final int[][] dz;

	/** Per frame: bounds of the run of identical consecutive frames it sits in. */
	private final int[] runStart;
	private final int[] runEnd;

	public RetroClip(int sequenceId, int rigId, int[][] transforms, int[][] dx, int[][] dy, int[][] dz)
	{
		this.sequenceId = sequenceId;
		this.rigId = rigId;
		this.transforms = transforms;
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;

		int n = transforms.length;
		runStart = new int[n];
		runEnd = new int[n];
		for (int f = 0; f < n; f++)
		{
			runStart[f] = f > 0 && sameFrame(f - 1, f) ? runStart[f - 1] : f;
		}
		for (int f = n - 1; f >= 0; f--)
		{
			runEnd[f] = f < n - 1 && sameFrame(f, f + 1) ? runEnd[f + 1] : f;
		}
	}

	private boolean sameFrame(int a, int b)
	{
		return Arrays.equals(transforms[a], transforms[b])
			&& Arrays.equals(dx[a], dx[b])
			&& Arrays.equals(dy[a], dy[b])
			&& Arrays.equals(dz[a], dz[b]);
	}

	/** First frame of the run of identical consecutive frames containing {@code frame}. */
	public int getRunStart(int frame)
	{
		return runStart[frame];
	}

	/** Last frame of that run. */
	public int getRunEnd(int frame)
	{
		return runEnd[frame];
	}

	public int getFrameCount()
	{
		return transforms.length;
	}

	/**
	 * Whether a frame index is one this clip actually has.
	 * <p>
	 * Worth checking rather than assuming: the frame index comes from the client, driven by the
	 * live sequence, so a clip that decoded short would otherwise index out of bounds on the render
	 * path.
	 */
	public boolean hasFrame(int frame)
	{
		return frame >= 0 && frame < transforms.length;
	}

	public int getOpCount(int frame)
	{
		return transforms[frame].length;
	}

	public int getTransform(int frame, int op)
	{
		return transforms[frame][op];
	}

	public int getDx(int frame, int op)
	{
		return dx[frame][op];
	}

	public int getDy(int frame, int op)
	{
		return dy[frame][op];
	}

	public int getDz(int frame, int op)
	{
		return dz[frame][op];
	}

	int[][] getTransforms()
	{
		return transforms;
	}

	int[][] getAllDx()
	{
		return dx;
	}

	int[][] getAllDy()
	{
		return dy;
	}

	int[][] getAllDz()
	{
		return dz;
	}
}
