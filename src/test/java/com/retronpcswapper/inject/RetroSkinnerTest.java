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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Hand-checkable cases for the skinner. Agreement with the client over a real animation is measured
 * in game against {@code applyTransformations}; these pin the arithmetic that would otherwise only
 * be visible as a subtly wrong pose.
 */
public class RetroSkinnerTest
{
	private static final float EPSILON = 0.01f;

	/** Two vertices on the X axis, both in group 0. */
	private static RetroMesh mesh()
	{
		return new RetroMesh(1, 0,
			new float[]{0f, 100f},
			new float[]{0f, 0f},
			new float[]{0f, 0f},
			new int[]{0}, new int[]{1}, new int[]{0},
			new short[]{0}, null, null, null, null,
			null, null, null, null,
			new int[][]{{0, 1}});
	}

	/** Transform 0 sets the pivot, transform 1 is the op under test. */
	private static RetroRig rig(int opType)
	{
		return new RetroRig(1, new int[]{0, opType}, new int[][]{{0}, {0}});
	}

	private static RetroClip clip(int dx, int dy, int dz)
	{
		return new RetroClip(1, 1,
			new int[][]{{0, 1}},
			new int[][]{{0, dx}},
			new int[][]{{0, dy}},
			new int[][]{{0, dz}});
	}

	private static float[][] pose(RetroRig rig, RetroClip clip)
	{
		RetroMesh mesh = mesh();
		float[] x = new float[mesh.getVerticesCount()];
		float[] y = new float[mesh.getVerticesCount()];
		float[] z = new float[mesh.getVerticesCount()];

		assertTrue(new RetroSkinner().pose(mesh, rig, clip, 0, x, y, z));
		return new float[][]{x, y, z};
	}

	/** One op as {transform, dx, dy, dz}. */
	private static int[] op(int transform, int dx, int dy, int dz)
	{
		return new int[]{transform, dx, dy, dz};
	}

	/** Frame 0 and frame 1, each a list of ops. */
	private static RetroClip clip2(int[][] frame0, int[][] frame1)
	{
		int[][][] frames = {frame0, frame1};
		int[][] transforms = new int[2][];
		int[][] dx = new int[2][];
		int[][] dy = new int[2][];
		int[][] dz = new int[2][];
		for (int f = 0; f < 2; f++)
		{
			int ops = frames[f].length;
			transforms[f] = new int[ops];
			dx[f] = new int[ops];
			dy[f] = new int[ops];
			dz[f] = new int[ops];
			for (int i = 0; i < ops; i++)
			{
				transforms[f][i] = frames[f][i][0];
				dx[f][i] = frames[f][i][1];
				dy[f][i] = frames[f][i][2];
				dz[f][i] = frames[f][i][3];
			}
		}
		return new RetroClip(1, 1, transforms, dx, dy, dz);
	}

	/** Pivot then transform 1, translating by fromDx in frame 0 and toDx in frame 1. */
	private static RetroClip translateClip(int fromDx, int toDx)
	{
		return clip2(
			new int[][]{op(0, 0, 0, 0), op(1, fromDx, 0, 0)},
			new int[][]{op(0, 0, 0, 0), op(1, toDx, 0, 0)});
	}

	/** Frame 0 blended toward next. */
	private static float[][] blend(RetroRig rig, RetroClip clip, int next, float t, int interval)
	{
		RetroMesh mesh = mesh();
		float[] x = new float[mesh.getVerticesCount()];
		float[] y = new float[mesh.getVerticesCount()];
		float[] z = new float[mesh.getVerticesCount()];

		assertTrue(new RetroSkinner().pose(mesh, rig, clip, 0, next, t, interval, x, y, z));
		return new float[][]{x, y, z};
	}

	private static void assertPosesEqual(float[][] expected, float[][] actual)
	{
		for (int axis = 0; axis < 3; axis++)
		{
			assertArrayEquals(expected[axis], actual[axis], EPSILON);
		}
	}

	/**
	 * A quarter turn about Y. Rotations are stored in 8 bits and scaled by 8 into the 2048-entry
	 * table, so 64 is a quarter of a circle - not 512, which is the trap if you assume the stored
	 * value indexes the table directly.
	 */
	@Test
	public void testQuarterTurnAboutYSwingsIntoZ()
	{
		float[][] posed = pose(rig(2), clip(0, 64, 0));

		// The pivot is the mean of the group, so (50, 0, 0); the two vertices sit 50 either side of
		// it on X and swing to 50 either side on Z
		assertEquals(50f, posed[0][0], EPSILON);
		assertEquals(0f, posed[1][0], EPSILON);
		assertEquals(50f, posed[2][0], EPSILON);

		assertEquals(50f, posed[0][1], EPSILON);
		assertEquals(0f, posed[1][1], EPSILON);
		assertEquals(-50f, posed[2][1], EPSILON);
	}

	@Test
	public void testTranslateMovesEveryVertexInTheGroup()
	{
		float[][] posed = pose(rig(1), clip(5, -7, 11));

		assertEquals(5f, posed[0][0], EPSILON);
		assertEquals(-7f, posed[1][0], EPSILON);
		assertEquals(11f, posed[2][0], EPSILON);

		assertEquals(105f, posed[0][1], EPSILON);
		assertEquals(-7f, posed[1][1], EPSILON);
		assertEquals(11f, posed[2][1], EPSILON);
	}

	/** Scale is in 128ths and works about the pivot, so the group spreads around its own centre. */
	@Test
	public void testScaleWorksAboutThePivot()
	{
		float[][] posed = pose(rig(3), clip(256, 128, 128));

		// Doubling X about a pivot of 50 sends 0 to -50 and 100 to 150
		assertEquals(-50f, posed[0][0], EPSILON);
		assertEquals(150f, posed[0][1], EPSILON);
	}

	@Test
	public void testUnknownOpTypeLeavesTheMeshAlone()
	{
		// Type 5 animates transparency, which this does not carry - it must be a no-op rather than
		// falling through into a transform
		float[][] posed = pose(rig(5), clip(64, 64, 64));

		assertEquals(0f, posed[0][0], EPSILON);
		assertEquals(100f, posed[0][1], EPSILON);
	}

	@Test
	public void testPosingIsNotCumulativeAcrossCalls()
	{
		RetroMesh mesh = mesh();
		RetroSkinner skinner = new RetroSkinner();
		float[] x = new float[2];
		float[] y = new float[2];
		float[] z = new float[2];

		skinner.pose(mesh, rig(1), clip(10, 0, 0), 0, x, y, z);
		skinner.pose(mesh, rig(1), clip(10, 0, 0), 0, x, y, z);

		assertEquals("a second pose must start from the rest pose, not the last result",
			10f, x[0], EPSILON);
	}

	@Test
	public void testUnriggedMeshReportsFailureAndKeepsTheRestPose()
	{
		RetroMesh unrigged = new RetroMesh(1, 0,
			new float[]{7f}, new float[]{8f}, new float[]{9f},
			new int[]{0}, new int[]{0}, new int[]{0},
			new short[]{0}, null, null, null, null,
			null, null, null, null,
			null);

		float[] x = new float[1];
		float[] y = new float[1];
		float[] z = new float[1];

		assertFalse(new RetroSkinner().pose(unrigged, rig(1), clip(50, 50, 50), 0, x, y, z));
		assertEquals("the buffers must still hold usable geometry after a refusal", 7f, x[0], EPSILON);
		assertEquals(8f, y[0], EPSILON);
		assertEquals(9f, z[0], EPSILON);
	}

	@Test
	public void testFrameOutOfRangeIsRefusedRatherThanThrowing()
	{
		RetroMesh mesh = mesh();
		float[] x = new float[2];
		float[] y = new float[2];
		float[] z = new float[2];

		// The frame index comes from the client, driven by the live sequence, so it can outrun a
		// clip that decoded short
		assertFalse(new RetroSkinner().pose(mesh, rig(1), clip(5, 5, 5), 99, x, y, z));
	}

	@Test
	public void testBlendTranslateMidpoint()
	{
		float[][] posed = blend(rig(1), translateClip(0, 10), 1, 2f, 4);

		assertArrayEquals(new float[]{5f, 105f}, posed[0], EPSILON);
	}

	@Test
	public void testBlendJustShortOfNextFrame()
	{
		float[][] posed = blend(rig(1), translateClip(0, 10), 1, 3f, 4);

		assertArrayEquals(new float[]{7.5f, 107.5f}, posed[0], EPSILON);
	}

	@Test
	public void testBlendAtTZeroMatchesPlainPose()
	{
		RetroClip clip = translateClip(0, 10);

		assertPosesEqual(pose(rig(1), clip), blend(rig(1), clip, 1, 0f, 4));
	}

	@Test
	public void testNoNextFrameMatchesPlainPose()
	{
		RetroClip clip = translateClip(3, 10);
		float[][] plain = pose(rig(1), clip);

		assertPosesEqual(plain, blend(rig(1), clip, -1, 2f, 4));
		assertPosesEqual(plain, blend(rig(1), clip, 99, 2f, 4));
	}

	/** 250 to 4 is 10 steps forward through 0, not 246 back, so halfway is 255. */
	@Test
	public void testBlendRotateAcrossSeamTakesShortArc()
	{
		RetroClip clip = clip2(
			new int[][]{op(0, 0, 0, 0), op(1, 0, 250, 0)},
			new int[][]{op(0, 0, 0, 0), op(1, 0, 4, 0)});

		assertPosesEqual(pose(rig(2), clip(0, 255, 0)), blend(rig(2), clip, 1, 1f, 2));
	}

	@Test
	public void testPivotDeltaIsInterpolated()
	{
		RetroClip clip = clip2(
			new int[][]{op(0, 0, 0, 0), op(1, 0, 64, 0)},
			new int[][]{op(0, 20, 0, 0), op(1, 0, 64, 0)});

		float[][] posed = blend(rig(2), clip, 1, 1f, 2);

		// Pivot is the group mean (50, 0, 0) plus half of 20, then a quarter turn about Y
		assertArrayEquals(new float[]{60f, 60f}, posed[0], EPSILON);
		assertArrayEquals(new float[]{60f, -40f}, posed[2], EPSILON);
	}

	@Test
	public void testOpMissingFromNextFrameBlendsToIdentity()
	{
		RetroClip clip = clip2(
			new int[][]{op(0, 0, 0, 0), op(1, 10, 0, 0)},
			new int[][]{op(0, 0, 0, 0)});

		assertArrayEquals(new float[]{5f, 105f}, blend(rig(1), clip, 1, 2f, 4)[0], EPSILON);
	}

	@Test
	public void testOpOnlyInNextFrameBlendsFromIdentity()
	{
		RetroClip clip = clip2(
			new int[][]{op(0, 0, 0, 0)},
			new int[][]{op(0, 0, 0, 0), op(1, 10, 0, 0)});

		assertArrayEquals(new float[]{5f, 105f}, blend(rig(1), clip, 1, 2f, 4)[0], EPSILON);
	}

	@Test
	public void testScaleMissingFillsWith128()
	{
		RetroClip clip = clip2(
			new int[][]{op(0, 0, 0, 0), op(1, 256, 128, 128)},
			new int[][]{op(0, 0, 0, 0)});

		// Halfway from 256 to 128 is 192, so 1.5x about the pivot of 50
		assertArrayEquals(new float[]{-25f, 125f}, blend(rig(3), clip, 1, 2f, 4)[0], EPSILON);
	}

	@Test
	public void testInterleavedTransformIndicesMerge()
	{
		RetroRig rig = new RetroRig(1, new int[]{0, 1, 1, 1}, new int[][]{{0}, {0}, {0}, {0}});
		RetroClip clip = clip2(
			new int[][]{op(0, 0, 0, 0), op(1, 8, 0, 0), op(3, 0, 0, 4)},
			new int[][]{op(0, 0, 0, 0), op(2, 0, 6, 0), op(3, 0, 0, 4)});

		float[][] posed = blend(rig, clip, 1, 1f, 2);

		assertArrayEquals(new float[]{4f, 104f}, posed[0], EPSILON);
		assertArrayEquals(new float[]{3f, 3f}, posed[1], EPSILON);
		assertArrayEquals(new float[]{4f, 4f}, posed[2], EPSILON);
	}

	@Test
	public void testTransparencyOpIsNotBlended()
	{
		RetroClip clip = clip2(
			new int[][]{op(0, 0, 0, 0), op(1, 64, 64, 64)},
			new int[][]{op(0, 0, 0, 0), op(1, 0, 0, 0)});

		assertArrayEquals(new float[]{0f, 100f}, blend(rig(5), clip, 1, 2f, 4)[0], EPSILON);
	}

	@Test
	public void testBlendIsNotCumulativeAcrossCalls()
	{
		RetroMesh mesh = mesh();
		RetroSkinner skinner = new RetroSkinner();
		RetroClip clip = translateClip(0, 10);
		float[] x = new float[2];
		float[] y = new float[2];
		float[] z = new float[2];

		skinner.pose(mesh, rig(1), clip, 0, 1, 2f, 4, x, y, z);
		skinner.pose(mesh, rig(1), clip, 0, 1, 2f, 4, x, y, z);

		assertArrayEquals(new float[]{5f, 105f}, x, EPSILON);
	}
}
