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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.function.IntPredicate;
import net.runelite.api.Animation;
import net.runelite.api.Client;
import org.junit.Before;
import org.junit.Test;

public class RetroAnimationSmoothingTest
{
	private Client client;
	private RetroAnimationSmoothing smoothing;

	@Before
	public void setUp()
	{
		client = mock(Client.class);
		smoothing = new RetroAnimationSmoothing(client);
		smoothing.setEnabled(true);
		when(client.getAnimationInterpolationFilter()).thenReturn(id -> true);
	}

	private static Animation animation(int id, boolean maya, int[] lengths)
	{
		Animation a = mock(Animation.class);
		when(a.getId()).thenReturn(id);
		when(a.isMayaAnim()).thenReturn(maya);
		when(a.getFrameLengths()).thenReturn(lengths);
		return a;
	}

	@Test
	public void testOffWhenDisabled()
	{
		smoothing.setEnabled(false);
		assertFalse(smoothing.blends(10));
	}

	@Test
	public void testOffWithoutFilter()
	{
		when(client.getAnimationInterpolationFilter()).thenReturn(null);
		assertFalse(smoothing.blends(10));
	}

	@Test
	public void testOffWhenFilterRejects()
	{
		when(client.getAnimationInterpolationFilter()).thenReturn((IntPredicate) id -> id != 10);
		assertFalse(smoothing.blends(10));
		assertTrue(smoothing.blends(11));
	}

	@Test
	public void testOnWhenAllAgree()
	{
		assertTrue(smoothing.blends(10));
		assertTrue(smoothing.blends(animation(10, false, new int[]{1, 2})));
	}

	@Test
	public void testNoAnimationNeverBlends()
	{
		assertFalse(smoothing.blends(-1));
		assertFalse(smoothing.blends((Animation) null));
	}

	@Test
	public void testMayaNeverBlends()
	{
		assertFalse(smoothing.blends(animation(10, true, new int[]{1, 2})));
	}

	@Test
	public void testNullFrameLengthsNeverBlend()
	{
		assertFalse(smoothing.blends(animation(10, false, null)));
	}

	@Test
	public void testFilterReadOnEveryCall()
	{
		when(client.getAnimationInterpolationFilter()).thenReturn(null, (IntPredicate) id -> true);
		assertFalse(smoothing.blends(10));
		assertTrue(smoothing.blends(10));
	}
}
