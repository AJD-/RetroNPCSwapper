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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RetroFrameTimingTest
{
	@Test
	public void testNextFrameAdvances()
	{
		assertEquals(3, RetroFrameTiming.nextFrame(2, 5, -1, false));
	}

	@Test
	public void testNextFrameStepBeatsLoop()
	{
		assertEquals(3, RetroFrameTiming.nextFrame(4, 5, 2, false));
		assertEquals(3, RetroFrameTiming.nextFrame(4, 5, 2, true));
	}

	@Test
	public void testNextFrameLoopsToStart()
	{
		assertEquals(0, RetroFrameTiming.nextFrame(4, 5, -1, true));
		assertEquals(0, RetroFrameTiming.nextFrame(4, 5, 0, true));
	}

	@Test
	public void testNextFrameEndsWithoutLoop()
	{
		assertEquals(-1, RetroFrameTiming.nextFrame(4, 5, -1, false));
		assertEquals(-1, RetroFrameTiming.nextFrame(4, 5, 9, false));
	}

	@Test
	public void testNextFrameOutOfRange()
	{
		assertEquals(-1, RetroFrameTiming.nextFrame(7, 5, -1, true));
		assertEquals(-1, RetroFrameTiming.nextFrame(-1, 5, -1, true));
	}

	@Test
	public void testPackFields()
	{
		assertEquals(Integer.MIN_VALUE | (2 << 16) | 3, RetroFrameTiming.pack(3, 2, false));
		assertTrue((RetroFrameTiming.pack(3, 2, true) & RetroFrameTiming.LOOPING) != 0);
	}

	@Test
	public void testPackClampsElapsed()
	{
		int packed = RetroFrameTiming.pack(0, 0x7FFF, false);
		assertEquals(0, packed & RetroFrameTiming.LOOPING);
		assertEquals(0x3FFF, (packed >> 16) & 0x3FFF);
		assertEquals(0, (RetroFrameTiming.pack(0, -5, false) >> 16) & 0x3FFF);
	}

	@Test
	public void testRotationTakesShortestArc()
	{
		assertEquals(256, RetroFrameTiming.rotation(0, 64, 1, 2));
		assertEquals(0, RetroFrameTiming.rotation(250, 6, 1, 2));
		assertEquals(0, RetroFrameTiming.rotation(6, 250, 1, 2));
		assertEquals(1536, RetroFrameTiming.rotation(0, 128, 1, 2));
	}

	@Test
	public void testLerp()
	{
		assertEquals(10f, RetroFrameTiming.lerp(10, 20, 0, 4), 0f);
		assertEquals(5f, RetroFrameTiming.lerp(0, 10, 2, 4), 0f);
	}
}
