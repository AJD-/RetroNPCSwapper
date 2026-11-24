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

public class RetroClipTest
{
	/** One op per frame; each row is {transform, dx, dy, dz}. */
	private static RetroClip clip(int[]... rows)
	{
		int n = rows.length;
		int[][] t = new int[n][];
		int[][] x = new int[n][];
		int[][] y = new int[n][];
		int[][] z = new int[n][];
		for (int i = 0; i < n; i++)
		{
			t[i] = new int[]{rows[i][0]};
			x[i] = new int[]{rows[i][1]};
			y[i] = new int[]{rows[i][2]};
			z[i] = new int[]{rows[i][3]};
		}
		return new RetroClip(1, 2, t, x, y, z);
	}

	private static int[] starts(RetroClip c)
	{
		int[] r = new int[c.getFrameCount()];
		for (int i = 0; i < r.length; i++)
		{
			r[i] = c.getRunStart(i);
		}
		return r;
	}

	private static int[] ends(RetroClip c)
	{
		int[] r = new int[c.getFrameCount()];
		for (int i = 0; i < r.length; i++)
		{
			r[i] = c.getRunEnd(i);
		}
		return r;
	}

	@Test
	public void testRunsAreByContent()
	{
		RetroClip c = clip(new int[]{0, 1, 2, 3}, new int[]{0, 1, 2, 3}, new int[]{0, 1, 2, 4}, new int[]{0, 1, 2, 3});
		assertArrayEquals(new int[]{0, 0, 2, 3}, starts(c));
		assertArrayEquals(new int[]{1, 1, 2, 3}, ends(c));
	}

	@Test
	public void testTransformsDifferOnly()
	{
		RetroClip c = clip(new int[]{0, 1, 2, 3}, new int[]{5, 1, 2, 3});
		assertArrayEquals(new int[]{0, 1}, starts(c));
		assertArrayEquals(new int[]{0, 1}, ends(c));
	}

	@Test
	public void testDyDiffersOnly()
	{
		RetroClip c = clip(new int[]{0, 1, 2, 3}, new int[]{0, 1, 9, 3});
		assertArrayEquals(new int[]{0, 1}, starts(c));
		assertArrayEquals(new int[]{0, 1}, ends(c));
	}

	@Test
	public void testSingleFrame()
	{
		RetroClip c = clip(new int[]{0, 1, 2, 3});
		assertArrayEquals(new int[]{0}, starts(c));
		assertArrayEquals(new int[]{0}, ends(c));
	}

	@Test
	public void testAllIdentical()
	{
		int[] r = {0, 1, 2, 3};
		RetroClip c = clip(r, r.clone(), r.clone(), r.clone());
		assertArrayEquals(new int[]{0, 0, 0, 0}, starts(c));
		assertArrayEquals(new int[]{3, 3, 3, 3}, ends(c));
	}

	@Test
	public void testFrameBounds()
	{
		RetroClip c = clip(new int[]{0, 1, 2, 3}, new int[]{0, 1, 2, 4});
		assertEquals(2, c.getFrameCount());
		assertTrue(c.hasFrame(1));
		assertFalse(c.hasFrame(2));
		assertFalse(c.hasFrame(-1));
	}

	private static void assertArrayEquals(int[] expected, int[] actual)
	{
		org.junit.Assert.assertArrayEquals(expected, actual);
	}
}
