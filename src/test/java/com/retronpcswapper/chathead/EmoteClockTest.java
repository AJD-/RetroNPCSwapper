package com.retronpcswapper.chathead;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class EmoteClockTest
{
	@Test
	public void testEachFrameHoldsForItsLength()
	{
		int[] lengths = {3, 2};

		assertEquals(0, EmoteClock.frameAt(lengths, 0));
		assertEquals(0, EmoteClock.frameAt(lengths, 2));
		assertEquals(1, EmoteClock.frameAt(lengths, 3));
		assertEquals(1, EmoteClock.frameAt(lengths, 4));
	}

	/** A chathead talks until the page turns, so the emote loops. */
	@Test
	public void testLoops()
	{
		assertEquals(0, EmoteClock.frameAt(new int[]{3, 2}, 5));
		assertEquals(1, EmoteClock.frameAt(new int[]{3, 2}, 8));
	}

	/** A zero length still shows its frame for a cycle, as the client plays it. */
	@Test
	public void testAZeroLengthFrameShowsForOneCycle()
	{
		assertEquals(1, EmoteClock.frameAt(new int[]{1, 0, 1}, 1));
		assertEquals(2, EmoteClock.frameAt(new int[]{1, 0, 1}, 2));
	}

	/** No frames (a skeletal emote, or none loaded): no frame, and the head shows at rest. */
	@Test
	public void testNoFramesIsNoFrame()
	{
		assertEquals(-1, EmoteClock.frameAt(new int[0], 10));
		assertEquals(-1, EmoteClock.frameAt(null, 10));
	}

	@Test
	public void testNegativeElapsedIsTheFirstFrame()
	{
		assertEquals(0, EmoteClock.frameAt(new int[]{3, 2}, -4));
	}
}
