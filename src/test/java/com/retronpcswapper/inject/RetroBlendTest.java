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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RetroBlendTest
{
	/** Frames [A, A, B, C]: runs {0-1}, {2}, {3}. */
	private static RetroClip clip()
	{
		int[][] t = {{0}, {0}, {0}, {0}};
		int[][] x = {{1}, {1}, {2}, {3}};
		int[][] z = {{0}, {0}, {0}, {0}};
		return new RetroClip(1, 2, t, x, new int[4][1], z);
	}

	private static final int[] L = {2, 3, 4, 1};
	private static final int[] L3 = {2, 3, 4, 3};

	private final RetroBlend b = new RetroBlend();

	@Test
	public void testMidRun()
	{
		assertTrue(b.compute(clip(), 1, 1, L, -1, false));
		assertEquals(5, b.interval);
		assertEquals(3f, b.t, 0f);
		assertEquals(2, b.next);
	}

	@Test
	public void testRunStartWithElapsed()
	{
		assertTrue(b.compute(clip(), 0, 1, L, -1, false));
		assertEquals(5, b.interval);
		assertEquals(1f, b.t, 0f);
		assertEquals(2, b.next);
	}

	@Test
	public void testRunStartNoElapsedIsPlain()
	{
		assertFalse(b.compute(clip(), 0, 0, L, -1, false));
		assertEquals(-1, b.next);
	}

	@Test
	public void testSingleFrameRun()
	{
		assertTrue(b.compute(clip(), 2, 2, L, -1, false));
		assertEquals(4, b.interval);
		assertEquals(2f, b.t, 0f);
		assertEquals(3, b.next);
	}

	@Test
	public void testLoopWrap()
	{
		assertFalse(b.compute(clip(), 3, 0, L, -1, true));
		assertTrue(b.compute(clip(), 3, 1, L3, -1, true));
		assertEquals(0, b.next);
		assertEquals(3, b.interval);
		assertEquals(1f, b.t, 0f);
	}

	@Test
	public void testHoldAtEnd()
	{
		assertFalse(b.compute(clip(), 3, 1, L3, -1, false));
		assertEquals(-1, b.next);
	}

	@Test
	public void testFrameStepRewind()
	{
		assertTrue(b.compute(clip(), 3, 1, L3, 2, false));
		assertEquals(2, b.next);
	}

	@Test
	public void testShortLengths()
	{
		int[] shortLengths = {2, 3};
		assertFalse(b.compute(clip(), 1, 1, shortLengths, -1, false));
		assertFalse(b.compute(clip(), 2, 1, shortLengths, -1, false));
		assertEquals(-1, b.next);
	}

	@Test
	public void testLongLengths()
	{
		assertFalse(b.compute(clip(), 1, 1, new int[]{2, 3, 4, 1, 1}, -1, false));
		assertEquals(-1, b.next);
	}

	@Test
	public void testDegenerateInputs()
	{
		assertFalse(b.compute(clip(), 1, 1, new int[4], -1, false));
		assertFalse(b.compute(null, 1, 1, L, -1, false));
		assertFalse(b.compute(clip(), 1, 1, null, -1, false));
		assertFalse(b.compute(clip(), 4, 1, L, -1, false));
		assertFalse(b.compute(clip(), -1, 1, L, -1, false));
		assertEquals(-1, b.next);
	}
}
