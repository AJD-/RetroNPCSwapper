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

import com.retronpcswapper.RetroNpcData;
import com.retronpcswapper.RetroNpcMapping;
import com.retronpcswapper.RetroNpcSwapperPlugin;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
	 * The wardrobe's doors: a decal painted in two layers - a fill and a border round it at the
	 * next priority up, sharing its corners - both under the surface. The shared corners are the
	 * decal's own, not the surface's, so the whole door comes forward as one piece.
	 */
	@Test
	public void testADecalInTwoLayersIsLiftedWhole()
	{
		// The surface square at y = -18, then a fill triangle and a border triangle at y = -16,
		// sharing the edge between vertices 4 and 5
		float[] x = {-20, 20, 20, -20, 0, 5, 0, 5};
		float[] y = {-18, -18, -18, -18, -16, -16, -16, -16};
		float[] z = {-20, -20, 20, 20, 0, 0, 5, -5};

		int moved = RetroDecals.lift(x, y, z, new int[]{0, 0, 4, 4}, new int[]{1, 2, 5, 7},
			new int[]{2, 3, 6, 5}, new byte[]{0, 0, 1, 2}, 0, 4);

		assertEquals(4, moved);
		for (int vertex = 4; vertex < 8; vertex++)
		{
			assertEquals(-18 - RetroDecals.CLEARANCE, y[vertex], 0.01f);
		}
	}

	/**
	 * A piece is lifted off the surface behind it, not off its own lower layer: the picnic bench's
	 * legs are drawn in two priorities that meet edge to edge, with nothing painted on anything.
	 */
	@Test
	public void testAPieceIsNotLiftedOffItsOwnLowerLayer()
	{
		// Only the two layers, coplanar and sharing an edge; the surface square is far below
		float[] x = {-20, 20, 20, -20, 0, 5, 0, 5};
		float[] y = {40, 40, 40, 40, -16, -16, -16, -16};
		float[] z = {-20, -20, 20, 20, 0, 0, 5, -5};

		int moved = RetroDecals.lift(x, y, z, new int[]{0, 0, 4, 4}, new int[]{1, 2, 5, 7},
			new int[]{2, 3, 6, 5}, new byte[]{0, 0, 1, 2}, 0, 4);

		assertEquals(0, moved);
	}

	/**
	 * A surface further behind than a decal is ever painted is another part of the model - as the
	 * cabinet is, 7.6 behind an open wardrobe's door, which would otherwise be pushed out of shape.
	 */
	@Test
	public void testASurfaceBeyondReachIsLeftAlone()
	{
		float[][] v = vertices(-18, -18 + 7.6f);

		int moved = RetroDecals.lift(v[0], v[1], v[2], FACES1, FACES2, FACES3, PRIORITIES, 0, 3);

		assertEquals(0, moved);
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

	/**
	 * Merlin's hair through his hat's brim: a lower part poking out in front of the part painted
	 * over it goes back behind it, and the upper part stays where it is.
	 */
	@Test
	public void testALowerPartPokingThroughIsTuckedBehind()
	{
		// The surface square is the upper part, the triangle two units in front of it the lower
		float[][] v = vertices(-18, -20);

		int moved = RetroDecals.paintOver(v[0], v[1], v[2], FACES1, FACES2, FACES3, 2, 3, 0, 2);

		assertEquals(3, moved);
		for (int vertex = 4; vertex < 7; vertex++)
		{
			assertEquals(-18 + RetroDecals.CLEARANCE, v[1][vertex], 0.01f);
		}
		assertEquals(-18, v[1][0], 0f);
	}

	/** Merlin's robe: a panel lying on the torso is lifted off it like any decal. */
	@Test
	public void testAnUpperPartLyingOnTheLowerIsLiftedClear()
	{
		float[][] v = vertices(-18, -18);

		// The square is the lower part this time, the triangle the panel
		RetroDecals.paintOver(v[0], v[1], v[2], FACES1, FACES2, FACES3, 0, 2, 2, 3);

		assertEquals(-18 - RetroDecals.CLEARANCE, v[1][4], 0.01f);
		assertEquals(-18, v[1][0], 0f);
	}

	@Test
	public void testAPartFurtherThanTheReachIsNotTucked()
	{
		float[][] v = vertices(-18, -18 - RetroDecals.TUCK_REACH - 1);

		int moved = RetroDecals.paintOver(v[0], v[1], v[2], FACES1, FACES2, FACES3, 2, 3, 0, 2);

		assertEquals(0, moved);
	}

	/** Merlin's pairs on his bundled kit: one pass settles both, so a second has nothing left to move. */
	@Test
	public void testMerlinsPaintedOverPairsSettleInOnePass() throws Exception
	{
		assertArrayEquals(new int[]{214, 202, 292, 324}, RetroNpcMapping.MERLIN.getPaintedOver());
		// Measured: five hair vertices out through the hat; the robe panel's 28 lying on the torso
		assertSettlesInOnePass(RetroNpcMapping.MERLIN, 5, 28);
	}

	/** The mugger's mask lies on his head; Feb and Nov kits alike. */
	@Test
	public void testTheMuggersMaskSettlesInOnePass() throws Exception
	{
		assertArrayEquals(new int[]{230, 243}, RetroNpcMapping.MUGGER.getPaintedOver());
		assertArrayEquals(new int[]{230, 243}, RetroNpcMapping.JAIL_MUGGER.getPaintedOver());
		// Measured: six mask vertices lying on the head
		assertSettlesInOnePass(RetroNpcMapping.MUGGER, 6);
		assertSettlesInOnePass(RetroNpcMapping.JAIL_MUGGER, 6);
	}

	private static void assertSettlesInOnePass(RetroNpcData data, int... firstPass) throws Exception
	{
		RetroAssetBundle bundle;
		try (InputStream in = RetroNpcSwapperPlugin.class.getResourceAsStream("retro-assets.dat"))
		{
			assertNotNull("retro-assets.dat is missing - run ./gradlew generateRetroAssets", in);
			bundle = RetroAssetCodec.read(in);
		}

		int base = data.getAssetKeyBase();
		int[] ids = data.getInjectedModelIds();
		int[] pairs = data.getPaintedOver();

		List<RetroMesh> parts = new ArrayList<>();
		int[] starts = new int[ids.length + 1];
		for (int i = 0; i < ids.length; i++)
		{
			RetroMesh part = bundle.getMesh(base + ids[i]);
			assertNotNull("part " + ids[i] + " is missing from the bundle", part);
			parts.add(part);
			starts[i + 1] = starts[i] + part.getFaceCount();
		}
		RetroMesh mesh = RetroMeshMerger.merge(base + ids[0], parts);
		float[] x = mesh.getVerticesX().clone();
		float[] y = mesh.getVerticesY().clone();
		float[] z = mesh.getVerticesZ().clone();

		for (int pass = 0; pass < 2; pass++)
		{
			for (int pair = 0; pair < pairs.length; pair += 2)
			{
				int lower = indexOf(ids, pairs[pair]);
				int upper = indexOf(ids, pairs[pair + 1]);
				int moved = RetroDecals.paintOver(x, y, z,
					mesh.getFaceIndices1(), mesh.getFaceIndices2(), mesh.getFaceIndices3(),
					starts[lower], starts[lower + 1], starts[upper], starts[upper + 1]);
				assertEquals("pass " + pass + ", " + pairs[pair] + " under " + pairs[pair + 1],
					pass == 0 ? firstPass[pair / 2] : 0, moved);
			}
		}
	}

	private static int indexOf(int[] ids, int id)
	{
		for (int i = 0; i < ids.length; i++)
		{
			if (ids[i] == id)
			{
				return i;
			}
		}
		throw new AssertionError(id + " is not a part");
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
