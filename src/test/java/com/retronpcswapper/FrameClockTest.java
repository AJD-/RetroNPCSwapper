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

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class FrameClockTest
{
	@Test
	public void testFirstSightIsZero()
	{
		assertEquals(0, new FrameClock().elapsed(5, 2, 100, 4));
	}

	@Test
	public void testElapsedGrowsWithCycles()
	{
		FrameClock clock = new FrameClock();
		clock.elapsed(5, 2, 100, 4);
		assertEquals(2, clock.elapsed(5, 2, 102, 4));
	}

	@Test
	public void testSameCycleIsStable()
	{
		FrameClock clock = new FrameClock();
		clock.elapsed(5, 2, 100, 4);
		assertEquals(1, clock.elapsed(5, 2, 101, 4));
		assertEquals(1, clock.elapsed(5, 2, 101, 4));
	}

	@Test
	public void testNeverReachesLength()
	{
		FrameClock clock = new FrameClock();
		clock.elapsed(5, 2, 100, 4);
		assertEquals(3, clock.elapsed(5, 2, 104, 4));
	}

	@Test
	public void testFrameChangeResets()
	{
		FrameClock clock = new FrameClock();
		clock.elapsed(5, 2, 100, 4);
		clock.elapsed(5, 2, 102, 4);
		assertEquals(0, clock.elapsed(5, 3, 103, 4));
	}

	@Test
	public void testAnimationChangeResets()
	{
		FrameClock clock = new FrameClock();
		clock.elapsed(5, 2, 100, 4);
		clock.elapsed(5, 2, 102, 4);
		assertEquals(0, clock.elapsed(6, 2, 103, 4));
	}

	@Test
	public void testGapBeyondLengthRebaselines()
	{
		FrameClock clock = new FrameClock();
		clock.elapsed(5, 2, 100, 4);
		assertEquals(0, clock.elapsed(5, 2, 110, 4));
	}

	@Test
	public void testCycleGoingBackwardsRebaselines()
	{
		FrameClock clock = new FrameClock();
		clock.elapsed(5, 2, 100, 4);
		assertEquals(0, clock.elapsed(5, 2, 90, 4));
	}

	@Test
	public void testDegenerateLengthIsZero()
	{
		FrameClock clock = new FrameClock();
		clock.elapsed(5, 2, 100, 0);
		assertEquals(0, clock.elapsed(5, 2, 101, 0));
		clock.elapsed(5, 2, 100, 1);
		assertEquals(0, clock.elapsed(5, 2, 101, 1));
	}

	@Test
	public void testResetActsLikeFirstSight()
	{
		FrameClock clock = new FrameClock();
		clock.elapsed(5, 2, 100, 4);
		clock.elapsed(5, 2, 102, 4);
		clock.reset();
		assertEquals(0, clock.elapsed(5, 2, 103, 4));
		assertEquals(1, clock.elapsed(5, 2, 104, 4));
	}
}
