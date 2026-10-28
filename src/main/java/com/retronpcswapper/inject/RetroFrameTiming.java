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

/** Frame stepping, packed-frame encoding and keyframe interpolation maths. */
public final class RetroFrameTiming
{
	/** Packed-frame flag marking a looping animation. */
	public static final int LOOPING = 0x40000000;

	private RetroFrameTiming()
	{
	}

	/** The frame after {@code frame}, or -1 when the animation ends there. */
	public static int nextFrame(int frame, int frameCount, int frameStep, boolean loop)
	{
		if (frame < 0 || frame >= frameCount)
		{
			return -1;
		}
		int n = frame + 1;
		if (n < frameCount)
		{
			return n;
		}
		if (frameStep > 0)
		{
			n -= frameStep;
			return n >= 0 && n < frameCount ? n : -1;
		}
		return loop ? 0 : -1;
	}

	/** Packs frame, elapsed cycles and loop flag into one int. */
	public static int pack(int frame, int elapsed, boolean loop)
	{
		int e = Math.max(0, Math.min(elapsed, 0x3FFF));
		return Integer.MIN_VALUE | (loop ? LOOPING : 0) | (e << 16) | (frame & 0xFFFF);
	}

	/** Linear blend of a translation or scale delta. */
	public static float lerp(int cur, int next, float t, int interval)
	{
		return cur + (next - cur) * t / interval;
	}

	/** Shortest-arc blend of a rotation delta, as an index into Perspective.SINE/COSINE. */
	public static int rotation(int cur, int next, float t, int interval)
	{
		int c = cur & 0xFF;
		int d = (next - cur) & 0xFF;
		if (d >= 128)
		{
			d -= 256;
		}
		return Math.round((c + d * t / interval) * 8) & 0x7FF;
	}
}
