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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import net.runelite.api.Animation;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;
import org.junit.Test;

public class SequenceClockTest
{
	private static final int[] TICKS = {1, 1, 3, 7, 1, 2, 1, 5, 1, 1};

	private static void assertAgrees(int frameStep)
	{
		Animation animation = mock(Animation.class);
		when(animation.isMayaAnim()).thenReturn(false);
		when(animation.getFrameLengths()).thenReturn(new int[]{3, 2, 4});
		when(animation.getDuration()).thenReturn(3);
		when(animation.getFrameStep()).thenReturn(frameStep);

		AnimationController real = new AnimationController(mock(Client.class), animation);
		SequenceClock clock = new SequenceClock(1, new int[]{3, 2, 4}, frameStep);
		for (int ticks : TICKS)
		{
			real.tick(ticks);
			clock.tick(ticks);
			assertEquals(real.getFrame(), clock.getFrame());
		}
	}

	@Test
	public void testMatchesAnimationControllerWithNoStep()
	{
		assertAgrees(-1);
	}

	@Test
	public void testMatchesAnimationControllerWithStep()
	{
		assertAgrees(3);
	}

	@Test
	public void testElapsedIsZeroBased()
	{
		SequenceClock clock = new SequenceClock(1, new int[]{3, 2, 4}, -1);
		assertEquals(0, clock.getElapsed());
		clock.tick(1);
		assertEquals(0, clock.getElapsed());
		clock.tick(2);
		assertEquals(0, clock.getFrame());
		assertEquals(2, clock.getElapsed());
		clock.tick(1);
		assertEquals(1, clock.getFrame());
		assertEquals(0, clock.getElapsed());
		assertEquals(2, clock.getLength());
	}

	@Test
	public void testFrameStepWrapsBack()
	{
		SequenceClock clock = new SequenceClock(1, new int[]{1, 1, 1, 1}, 2);
		clock.setFrame(3);
		clock.tick(2);
		assertEquals(2, clock.getFrame());
	}

	@Test
	public void testNullLengthsStandStill()
	{
		SequenceClock clock = new SequenceClock(1, null, -1);
		clock.tick(10);
		assertEquals(0, clock.getFrame());
		assertEquals(0, clock.getElapsed());
		assertEquals(0, clock.getLength());
		assertEquals(0, clock.getFrameCount());
	}

	@Test
	public void testAllZeroLengthsDoNotSpin()
	{
		SequenceClock clock = new SequenceClock(1, new int[]{0, 0, 0}, -1);
		clock.tick(100);
		assertTrue(clock.getFrame() >= 0 && clock.getFrame() < 3);
	}

	@Test
	public void testSetFrameResetsElapsed()
	{
		SequenceClock clock = new SequenceClock(1, new int[]{3, 2, 4}, -1);
		clock.tick(2);
		clock.setFrame(2);
		assertEquals(2, clock.getFrame());
		assertEquals(0, clock.getElapsed());
	}
}
