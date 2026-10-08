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
import static org.junit.Assert.assertNull;
import org.junit.Test;

/**
 * The geometry here is the anti-dragon shield's in miniature: a surface facing up (-Y, since Y
 * grows downward) and a decal painted onto it at a higher priority.
 */
public class RetroDecalsTest
{
	/**
	 * A surface square at {@code surfaceY} over x and z in [-20, 20], as two priority-0 faces on
	 * vertices 0-3, and a priority-1 decal triangle at {@code decalY} on vertices 4-6. Both wound to
	 * face up.
	 */
	private static float[][] vertices(float surfaceY, float decalY)
	{
		return new float[][]{
			{-20, 20, 20, -20, 0, 5, 0},
			{surfaceY, surfaceY, surfaceY, surfaceY, decalY, decalY, decalY},
			{-20, -20, 20, 20, 0, 0, 5},
		};
	}

	// Wound (a, b, c) so (b - a) x (c - a) points along -Y
	private static final int[] FACES1 = {0, 0, 4};
	private static final int[] FACES2 = {1, 2, 5};
	private static final int[] FACES3 = {2, 3, 6};
	private static final byte[] PRIORITIES = {0, 0, 1};

	@Test
	public void testABuriedDecalIsLiftedClearOfItsSurface()
	{
		// The shield's case: the sword sits two units under the face it decorates
		float[][] v = vertices(-18, -16);

		int moved = RetroDecals.lift(v[0], v[1], v[2], FACES1, FACES2, FACES3, PRIORITIES, 0, 3);

		assertEquals(3, moved);
		for (int vertex = 4; vertex < 7; vertex++)
		{
			assertEquals(-18 - RetroDecals.CLEARANCE, v[1][vertex], 0.01f);
		}
		// The surface itself never moves
		assertEquals(-18, v[1][0], 0f);
	}

	@Test
	public void testADecalAlreadyInFrontStillGetsClearance()
	{
		float[][] v = vertices(-18, -18);

		RetroDecals.lift(v[0], v[1], v[2], FACES1, FACES2, FACES3, PRIORITIES, 0, 3);

		assertEquals(-18 - RetroDecals.CLEARANCE, v[1][4], 0.01f);
	}

	/**
	 * A decal welded to its surface cannot move without dragging the surface with it, so it stays
	 * where it is and is left to the depth bias.
	 */
	@Test
	public void testADecalSharingItsVerticesStaysPut()
	{
		float[] x = {-20, 20, 20, -20};
		float[] y = {-18, -18, -18, -18};
		float[] z = {-20, -20, 20, 20};

		// The surface is one triangle, the decal the other half of the same square
		int moved = RetroDecals.lift(x, y, z, new int[]{0, 0}, new int[]{1, 2}, new int[]{2, 3},
			new byte[]{0, 1}, 0, 2);

		assertEquals(0, moved);
		assertEquals(-18, y[3], 0f);
	}

	@Test
	public void testDecalsAreBiasedTowardTheCamera()
	{
		float[][] v = vertices(-18, -16);
		RetroModel model = bound(v);

		RetroDecals.lift(model, 0);

		byte[] bias = model.getFaceBias();
		assertEquals(0, bias[0]);
		assertEquals(0, bias[1]);
		assertEquals(RetroDecals.BIAS_STEP, bias[2]);
	}

	@Test
	public void testFacesBeforeTheFirstAreLeftAlone()
	{
		float[][] v = vertices(-18, -16);
		RetroModel model = bound(v);

		// Only the decal is in range, and on its own it has no surface and no priority above another
		RetroDecals.lift(model, 2);

		assertEquals(-16, model.getVerticesY()[4], 0f);
		assertNull(model.getFaceBias());
	}

	@Test
	public void testOneFlatPriorityIsLeftAlone()
	{
		float[][] v = vertices(-18, -16);

		int moved = RetroDecals.lift(v[0], v[1], v[2], FACES1, FACES2, FACES3, new byte[]{3, 3, 3}, 0, 3);

		assertEquals(0, moved);
		assertEquals(-16, v[1][4], 0f);
	}

	/**
	 * Why the worn shield can be lifted once at rest: lifting and then turning and moving it puts
	 * every vertex where turning and moving it and then lifting does.
	 */
	@Test
	public void testLiftingCommutesWithATurnAndAMove()
	{
		double yaw = 0.7;
		double pitch = -0.4;
		// Turn about Y, then about X, then move - as a map from RetroAttachment.fit is laid out
		double cy = Math.cos(yaw);
		double sy = Math.sin(yaw);
		double cp = Math.cos(pitch);
		double sp = Math.sin(pitch);
		double[] map = {
			cy, 0, sy, 30,
			sp * sy, cp, -sp * cy, -12,
			-cp * sy, sp, cp * cy, 55,
		};

		float[][] liftedFirst = vertices(-18, -16);
		RetroDecals.lift(liftedFirst[0], liftedFirst[1], liftedFirst[2], FACES1, FACES2, FACES3, PRIORITIES, 0, 3);
		float[][] placedAfter = new float[3][7];
		RetroAttachment.transform(map, liftedFirst[0], liftedFirst[1], liftedFirst[2], 7,
			placedAfter[0], placedAfter[1], placedAfter[2]);

		float[][] rest = vertices(-18, -16);
		float[][] placedFirst = new float[3][7];
		RetroAttachment.transform(map, rest[0], rest[1], rest[2], 7, placedFirst[0], placedFirst[1], placedFirst[2]);
		RetroDecals.lift(placedFirst[0], placedFirst[1], placedFirst[2], FACES1, FACES2, FACES3, PRIORITIES, 0, 3);

		for (int axis = 0; axis < 3; axis++)
		{
			for (int v = 0; v < 7; v++)
			{
				assertEquals(placedFirst[axis][v], placedAfter[axis][v], 0.01f);
			}
		}
	}

	private static RetroModel bound(float[][] v)
	{
		RetroMesh mesh = new RetroMesh(1, 0, v[0], v[1], v[2], FACES1, FACES2, FACES3,
			new short[]{100, 100, 200}, null, null, PRIORITIES, null, null, null, null, null, new int[0][]);
		RetroModel model = new RetroModel();
		model.bind(mesh, new int[]{1, 1, 1}, new int[]{1, 1, 1}, new int[]{-1, -1, -1});
		return model;
	}
}
