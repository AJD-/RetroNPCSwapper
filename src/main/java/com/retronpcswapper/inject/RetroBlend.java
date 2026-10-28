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

/** Where a frame sits inside its run of identical frames, and the frame the run blends toward. Reused per draw. */
public final class RetroBlend
{
	/** Frame the blend lands on, -1 when not blending. */
	public int next = -1;

	/** Cycles into the interval. */
	public float t;

	/** Cycles the whole run lasts. */
	public int interval;

	/** True when a blend applies; false leaves next at -1 so callers pose the frame plainly. */
	public boolean compute(RetroClip clip, int frame, int elapsed, int[] lengths, int frameStep, boolean loop)
	{
		next = -1;
		if (clip == null || lengths == null || clip.getFrameCount() != lengths.length || !clip.hasFrame(frame)
			|| clip.getRunEnd(frame) >= lengths.length)
		{
			return false;
		}
		int rs = clip.getRunStart(frame);
		int re = clip.getRunEnd(frame);
		int total = 0;
		int before = elapsed;
		for (int f = rs; f <= re; f++)
		{
			total += lengths[f];
			if (f < frame)
			{
				before += lengths[f];
			}
		}
		int n = RetroFrameTiming.nextFrame(re, clip.getFrameCount(), frameStep, loop);
		if (total <= 0 || n < 0 || before <= 0)
		{
			return false;
		}
		interval = total;
		t = before;
		next = n;
		return true;
	}
}
