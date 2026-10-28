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

/**
 * An animation's place in its frames, advanced as {@link net.runelite.api.AnimationController}
 * does, but with the cycles on the current frame readable for blending.
 */
final class SequenceClock
{
	private final int animationId;
	private final int[] frameLengths;
	private final int frameStep;
	private int frame;
	private int elapsedTicks;

	/** Null or empty lengths leave the clock standing at frame 0. */
	SequenceClock(int animationId, int[] frameLengths, int frameStep)
	{
		this.animationId = animationId;
		this.frameLengths = frameLengths;
		this.frameStep = frameStep;
	}

	void setFrame(int frame)
	{
		this.frame = frame;
		elapsedTicks = 0;
	}

	void tick(int ticks)
	{
		if (getFrameCount() == 0)
		{
			return;
		}

		elapsedTicks += ticks;
		// A zero length counts as one, so an all-zero array cannot spin
		while (elapsedTicks > Math.max(frameLengths[frame], 1))
		{
			elapsedTicks -= Math.max(frameLengths[frame], 1);
			frame++;
			if (frame >= frameLengths.length)
			{
				frame -= frameStep;
				if (frame < 0 || frame >= frameLengths.length)
				{
					frame = 0;
				}
			}
		}
	}

	int getFrame()
	{
		return frame;
	}

	/** Cycles spent on the current frame, 0-based as the client's actor path counts them. */
	int getElapsed()
	{
		return Math.max(elapsedTicks - 1, 0);
	}

	int getLength()
	{
		return getFrameCount() == 0 ? 0 : frameLengths[frame];
	}

	int[] getLengths()
	{
		return frameLengths;
	}

	int getFrameCount()
	{
		return frameLengths == null ? 0 : frameLengths.length;
	}

	int getFrameStep()
	{
		return frameStep;
	}

	int getAnimationId()
	{
		return animationId;
	}
}
