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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import org.junit.Test;

public class RetroAssetCodecTest
{
	private static RetroMesh mesh()
	{
		return new RetroMesh(2944, 5,
			new float[]{0f, 10f, 20f},
			new float[]{0f, -30f, 5f},
			new float[]{0f, 40f, -15f},
			new int[]{0}, new int[]{1}, new int[]{2},
			new short[]{(short) 0x3A05},
			new byte[]{1},
			null,                      // deliberately null - see the round-trip test below
			new byte[]{2},
			new short[]{37},
			// one face mapped by the one texture triangle, which names the mesh's own vertices
			new byte[]{0},
			new int[]{0}, new int[]{1}, new int[]{2},
			new int[][]{{0, 1}, {}, {2}});
	}

	private static RetroRig rig()
	{
		return new RetroRig(338, new int[]{0, 2, 1}, new int[][]{{0}, {0, 1}, {2}});
	}

	private static RetroClip clip()
	{
		return new RetroClip(262, 338,
			new int[][]{{0, 1}, {1}},
			new int[][]{{5, -5}, {0}},
			new int[][]{{0, 128}, {7}},
			new int[][]{{-3, 3}, {0}});
	}

	private static RetroAssetBundle roundTrip(RetroAssetBundle bundle) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RetroAssetCodec.write(bundle, out);
		return RetroAssetCodec.read(new ByteArrayInputStream(out.toByteArray()));
	}

	private static RetroAssetBundle sample()
	{
		Map<Integer, RetroMesh> meshes = new LinkedHashMap<>();
		meshes.put(2944, mesh());
		Map<Integer, RetroRig> rigs = new LinkedHashMap<>();
		rigs.put(338, rig());
		Map<Integer, RetroClip> clips = new LinkedHashMap<>();
		clips.put(262, clip());
		return new RetroAssetBundle(meshes, rigs, clips);
	}

	@Test
	public void testMeshSurvivesRoundTrip() throws IOException
	{
		RetroMesh original = mesh();
		RetroMesh restored = roundTrip(sample()).getMesh(2944);

		assertNotNull(restored);
		assertEquals(original.getId(), restored.getId());

		// The model-level priority is the per-face fallback a merge needs when only one part carries
		// a priority array, so losing it in the round trip would change draw order on merged models
		assertEquals(5, restored.getPriority());
		assertEquals(original.getPriority(), restored.getPriority());

		assertEquals(original.getVerticesCount(), restored.getVerticesCount());
		assertEquals(original.getFaceCount(), restored.getFaceCount());
		assertArrayEquals(original.getVerticesX(), restored.getVerticesX(), 0f);
		assertArrayEquals(original.getVerticesY(), restored.getVerticesY(), 0f);
		assertArrayEquals(original.getVerticesZ(), restored.getVerticesZ(), 0f);
		assertArrayEquals(original.getFaceIndices1(), restored.getFaceIndices1());
		assertArrayEquals(original.getFaceColors(), restored.getFaceColors());
		assertArrayEquals(original.getFaceRenderTypes(), restored.getFaceRenderTypes());
		assertArrayEquals(original.getFaceRenderPriorities(), restored.getFaceRenderPriorities());
		assertArrayEquals(original.getFaceTextures(), restored.getFaceTextures());

		// Without these the renderer falls back to a hardcoded (0,0), (1,0), (0,1) per face, which
		// stretches the whole texture across every face separately
		assertArrayEquals(original.getTextureCoords(), restored.getTextureCoords());
		assertArrayEquals(original.getTexIndices1(), restored.getTexIndices1());
		assertArrayEquals(original.getTexIndices2(), restored.getTexIndices2());
		assertArrayEquals(original.getTexIndices3(), restored.getTexIndices3());
	}

	/**
	 * Null and empty are different to the renderer - a null transparency array is what puts a model
	 * on the opaque path, while an empty one is a zero-face model - so the encoding has to keep them
	 * apart rather than normalising one into the other.
	 */
	@Test
	public void testNullArraysStayNullAndEmptyStaysEmpty() throws IOException
	{
		RetroMesh restored = roundTrip(sample()).getMesh(2944);

		assertNull("a null array must not come back as empty", restored.getFaceTransparencies());
		assertEquals("an empty vertex group must not come back as null or populated",
			0, restored.getVertexGroup(1).length);
		assertArrayEquals(new int[]{0, 1}, restored.getVertexGroup(0));
	}

	@Test
	public void testRigAndClipSurviveRoundTrip() throws IOException
	{
		RetroAssetBundle restored = roundTrip(sample());

		RetroRig restoredRig = restored.getRig(338);
		assertNotNull(restoredRig);
		assertEquals(3, restoredRig.getTransformCount());
		assertEquals(2, restoredRig.getType(1));
		assertArrayEquals(new int[]{0, 1}, restoredRig.getGroups(1));

		RetroClip restoredClip = restored.getClip(262);
		assertNotNull(restoredClip);
		assertEquals(338, restoredClip.getRigId());
		assertEquals(2, restoredClip.getFrameCount());
		assertEquals(2, restoredClip.getOpCount(0));
		assertEquals(1, restoredClip.getTransform(0, 1));
		assertEquals(-5, restoredClip.getDx(0, 1));
		assertEquals(128, restoredClip.getDy(0, 1));
		assertEquals(3, restoredClip.getDz(0, 1));
	}

	@Test
	public void testOutOfRangeGroupIsEmptyRatherThanAThrow()
	{
		// Rigs are shared across a category and address more groups than any one mesh uses, so this
		// is the normal case rather than an error
		assertEquals(0, mesh().getVertexGroup(99).length);
		assertEquals(0, mesh().getVertexGroup(-1).length);
	}

	@Test
	public void testEmptyBundleRoundTrips() throws IOException
	{
		assertEquals(true, roundTrip(RetroAssetBundle.empty()).isEmpty());
	}

	@Test
	public void testRejectsForeignData()
	{
		try
		{
			RetroAssetCodec.read(new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}));
			fail("expected a refusal rather than a partial read");
		}
		catch (IOException expected)
		{
			// A bundle that is not ours must be refused outright, not decoded into plausible
			// geometry that then renders as garbage
		}
	}

	/**
	 * A bundle written by a different generator has to be refused, not read. Silently misreading a
	 * changed layout produces geometry that looks plausible and renders as garbage, which is far
	 * harder to diagnose than a startup failure.
	 */
	@Test
	public void testRejectsAnUnknownVersion() throws IOException
	{
		ByteArrayOutputStream raw = new ByteArrayOutputStream();
		try (DataOutputStream data = new DataOutputStream(new GZIPOutputStream(raw)))
		{
			data.writeInt(0x5254524F);              // correct magic
			data.writeInt(RetroAssetCodec.VERSION + 1);
			data.writeInt(0);
			data.writeInt(0);
			data.writeInt(0);
		}

		try
		{
			RetroAssetCodec.read(new ByteArrayInputStream(raw.toByteArray()));
			fail("expected a refusal for a bundle from a newer generator");
		}
		catch (IOException expected)
		{
			assertTrue("the message should say to regenerate, not just that something went wrong",
				expected.getMessage().contains("regenerate"));
		}
	}

	/**
	 * A rig is one table written as two blocks, so they can disagree without anything else noticing.
	 * {@link RetroSkinner} bounds its loop on the transform count and indexes the group sets with
	 * it, which turns a short groups block into an exception inside the render path - a frame into
	 * the fight rather than at load.
	 */
	@Test
	public void testRejectsARigWhoseTablesDisagree() throws IOException
	{
		ByteArrayOutputStream raw = new ByteArrayOutputStream();
		try (DataOutputStream data = new DataOutputStream(new GZIPOutputStream(raw)))
		{
			data.writeInt(0x5254524F);
			data.writeInt(RetroAssetCodec.VERSION);

			data.writeInt(0);                       // no meshes

			data.writeInt(1);                       // one rig
			data.writeInt(338);                     // its id
			data.writeInt(3);                       // three transform types
			data.writeInt(0);
			data.writeInt(2);
			data.writeInt(1);
			data.writeInt(2);                       // but only two group sets
			data.writeInt(1);
			data.writeInt(0);
			data.writeInt(1);
			data.writeInt(0);

			data.writeInt(0);                       // no clips
		}

		try
		{
			RetroAssetCodec.read(new ByteArrayInputStream(raw.toByteArray()));
			fail("expected a refusal for a rig whose two tables disagree");
		}
		catch (IOException expected)
		{
			assertTrue("the message should name the mismatch it found: " + expected.getMessage(),
				expected.getMessage().contains("3 transforms but 2 group sets"));
		}
	}

	@Test
	public void testBundleLookupsMissUnknownIds() throws IOException
	{
		RetroAssetBundle restored = roundTrip(new RetroAssetBundle(
			Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()));

		assertNull(restored.getMesh(1));
		assertNull(restored.getRig(1));
		assertNull(restored.getClip(1));
	}
}
