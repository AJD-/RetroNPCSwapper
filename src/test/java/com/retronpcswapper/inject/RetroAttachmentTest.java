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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.util.Random;
import org.junit.Test;

public class RetroAttachmentTest
{
	private static final int PART = 40;

	/** A lumpy, unmistakably three-dimensional point cloud standing in for a shield at rest. */
	private static float[][] part(long seed)
	{
		Random random = new Random(seed);
		float[][] points = new float[3][PART];
		for (int v = 0; v < PART; v++)
		{
			points[0][v] = 24 + random.nextInt(25);
			points[1][v] = -140 + random.nextInt(75);
			points[2][v] = -49 + random.nextInt(107);
		}
		return points;
	}

	/** Turns points about Y and then X, then moves them - the kind of map a posed arm makes. */
	private static float[][] posed(float[][] rest, double yaw, double pitch, float tx, float ty, float tz)
	{
		float[][] out = new float[3][rest[0].length];
		for (int v = 0; v < rest[0].length; v++)
		{
			double x = rest[0][v];
			double y = rest[1][v];
			double z = rest[2][v];

			double x1 = x * Math.cos(yaw) + z * Math.sin(yaw);
			double z1 = z * Math.cos(yaw) - x * Math.sin(yaw);
			double y2 = y * Math.cos(pitch) - z1 * Math.sin(pitch);
			double z2 = y * Math.sin(pitch) + z1 * Math.cos(pitch);

			// The client holds posed vertices as whole units
			out[0][v] = Math.round(x1 + tx);
			out[1][v] = Math.round(y2 + ty);
			out[2][v] = Math.round(z2 + tz);
		}
		return out;
	}

	/** A player model in miniature: noise, the posed part, more noise. */
	private static float[][] model(float[][] posedPart, int before, int after)
	{
		Random random = new Random(99);
		int count = before + PART + after;
		float[][] out = new float[3][count];
		for (int axis = 0; axis < 3; axis++)
		{
			for (int v = 0; v < count; v++)
			{
				out[axis][v] = random.nextInt(400) - 200;
			}
			System.arraycopy(posedPart[axis], 0, out[axis], before, PART);
		}
		return out;
	}

	@Test
	public void testFitRecoversATurnAndAMove()
	{
		float[][] rest = part(1);
		float[][] posed = posed(rest, 0.7, -0.4, 30, -12, 55);

		double[] map = RetroAttachment.fit(rest[0], rest[1], rest[2], PART, posed[0], posed[1], posed[2], 0);

		assertNotNull(map);
		float[] x = new float[PART];
		float[] y = new float[PART];
		float[] z = new float[PART];
		RetroAttachment.transform(map, rest[0], rest[1], rest[2], PART, x, y, z);
		for (int v = 0; v < PART; v++)
		{
			assertEquals(posed[0][v], x[v], 1.5f);
			assertEquals(posed[1][v], y[v], 1.5f);
			assertEquals(posed[2][v], z[v], 1.5f);
		}
	}

	/**
	 * The map recovered from one part is the one to place another part by - so a point not in the
	 * fit lands where the same turn and move would have put it.
	 */
	@Test
	public void testTheMapCarriesOverToAnotherPart()
	{
		float[][] rest = part(1);
		float[][] posed = posed(rest, 0.7, -0.4, 30, -12, 55);
		double[] map = RetroAttachment.fit(rest[0], rest[1], rest[2], PART, posed[0], posed[1], posed[2], 0);

		float[][] other = part(2);
		float[][] expected = posed(other, 0.7, -0.4, 30, -12, 55);
		float[] x = new float[PART];
		float[] y = new float[PART];
		float[] z = new float[PART];
		RetroAttachment.transform(map, other[0], other[1], other[2], PART, x, y, z);

		for (int v = 0; v < PART; v++)
		{
			assertEquals(expected[0][v], x[v], 2f);
			assertEquals(expected[1][v], y[v], 2f);
			assertEquals(expected[2][v], z[v], 2f);
		}
	}

	@Test
	public void testLocateFindsThePartAmongTheRest()
	{
		float[][] rest = part(1);
		float[][] model = model(posed(rest, 1.1, 0.3, -20, 5, 40), 333, 517);

		RetroAttachment.Placement placement = RetroAttachment.locate(rest[0], rest[1], rest[2], PART,
			model[0], model[1], model[2], model[0].length, -1);

		assertNotNull(placement);
		assertEquals(333, placement.start);
	}

	@Test
	public void testAWrongHintFallsBackToASearch()
	{
		float[][] rest = part(1);
		float[][] model = model(posed(rest, 1.1, 0.3, -20, 5, 40), 333, 517);

		RetroAttachment.Placement placement = RetroAttachment.locate(rest[0], rest[1], rest[2], PART,
			model[0], model[1], model[2], model[0].length, 12);

		assertNotNull(placement);
		assertEquals(333, placement.start);
	}

	/** A model without the part - say the player took the shield off - finds nothing. */
	@Test
	public void testLocateFindsNothingWhenThePartIsAbsent()
	{
		float[][] rest = part(1);
		float[][] model = model(posed(part(3), 1.1, 0.3, -20, 5, 40), 333, 517);

		assertNull(RetroAttachment.locate(rest[0], rest[1], rest[2], PART,
			model[0], model[1], model[2], model[0].length, -1));
	}

	/** Bent rather than moved whole, the part is not one rigid piece, and no single map is trusted. */
	@Test
	public void testFitRefusesAPartThatWasNotMovedWhole()
	{
		float[][] rest = part(1);
		float[][] posed = posed(rest, 0.7, -0.4, 30, -12, 55);
		for (int v = 0; v < PART / 2; v++)
		{
			posed[1][v] += 25;
		}

		assertNull(RetroAttachment.fit(rest[0], rest[1], rest[2], PART, posed[0], posed[1], posed[2], 0));
	}
}
