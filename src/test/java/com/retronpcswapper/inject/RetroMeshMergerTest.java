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

import com.retronpcswapper.RetroNpcSwapperPlugin;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Covers the merge that lets the bundle store one part per model id.
 *
 * <p>The offsets are the whole point: a part's face indices and vertex-group members address its
 * own vertices, so both have to be shifted by the running vertex base or the merged mesh draws
 * garbage and animates against the wrong bones.
 */
public class RetroMeshMergerTest
{
	/** Two vertices, one face, one vertex group, no optional arrays. */
	private static RetroMesh part(int id, int priority, float x, int group)
	{
		return new RetroMesh(id, priority,
			new float[]{x, x + 1f},
			new float[]{0f, 1f},
			new float[]{0f, 2f},
			new int[]{0}, new int[]{1}, new int[]{0},
			new short[]{(short) id}, null, null, null, null,
			null, null, null, null,
			group < 0 ? null : groups(group));
	}

	private static int[][] groups(int group)
	{
		int[][] vertexGroups = new int[group + 1][];
		for (int i = 0; i <= group; i++)
		{
			vertexGroups[i] = new int[0];
		}
		vertexGroups[group] = new int[]{0, 1};
		return vertexGroups;
	}

	@Test
	public void testSinglePartIsReturnedUntouched()
	{
		RetroMesh only = part(2887, 0, 0f, 0);

		// Returned rather than copied - RetroMesh is immutable and every consumer that needs to
		// change one derives a copy first, so this makes the merge a provable no-op for the
		// categories that were never multi-part
		assertSame(only, RetroMeshMerger.merge(2887, Collections.singletonList(only)));
	}

	@Test
	public void testFaceIndicesAndVertexGroupsAreOffsetByTheRunningVertexBase()
	{
		RetroMesh body = part(2870, 0, 0f, 1);
		RetroMesh head = part(2862, 0, 100f, 3);

		RetroMesh merged = RetroMeshMerger.merge(2870, Arrays.asList(body, head));

		assertEquals("merged mesh keeps the first part's id", 2870, merged.getId());
		assertEquals(4, merged.getVerticesCount());
		assertEquals(2, merged.getFaceCount());

		// The head's vertices follow the body's, and its single face addresses them there
		assertArrayEquals(new float[]{0f, 1f, 100f, 101f}, merged.getVerticesX(), 0f);
		assertArrayEquals(new int[]{0, 2}, merged.getFaceIndices1());
		assertArrayEquals(new int[]{1, 3}, merged.getFaceIndices2());
		assertArrayEquals(new int[]{0, 2}, merged.getFaceIndices3());
		assertArrayEquals(new short[]{2870, 2862}, merged.getFaceColors());

		// Group membership is unioned by group index, with the same offset applied
		assertArrayEquals(new int[]{0, 1}, merged.getVertexGroup(1));
		assertArrayEquals(new int[]{2, 3}, merged.getVertexGroup(3));
		assertEquals(0, merged.getVertexGroup(0).length);
	}

	@Test
	public void testPartsSharingAGroupMoveTogether()
	{
		// A prop rides a single bone the body already has - the fire giant's 4991 is bound to group
		// 32 and nothing else, so it has to end up in the body's group 32 to animate with the arm
		RetroMesh body = part(2870, 0, 0f, 2);
		RetroMesh prop = part(4991, 0, 50f, 2);

		RetroMesh merged = RetroMeshMerger.merge(2870, Arrays.asList(body, prop));

		assertArrayEquals(new int[]{0, 1, 2, 3}, merged.getVertexGroup(2));
	}

	@Test
	public void testAllNullOptionalArraysStayNull()
	{
		RetroMesh merged = RetroMeshMerger.merge(2870,
			Arrays.asList(part(2870, 0, 0f, 0), part(2862, 0, 10f, 0)));

		// Null is not the same as empty: a null transparency array is what keeps a model on the
		// opaque upload path, so filling these in would silently move every merged model off it
		assertNull(merged.getFaceRenderTypes());
		assertNull(merged.getFaceTransparencies());
		assertNull(merged.getFaceRenderPriorities());
		assertNull(merged.getFaceTextures());
	}

	@Test
	public void testAPartWithoutPrioritiesIsFilledWithItsModelPriority()
	{
		RetroMesh withPriorities = new RetroMesh(2870, 0,
			new float[]{0f, 1f}, new float[]{0f, 0f}, new float[]{0f, 0f},
			new int[]{0}, new int[]{1}, new int[]{0},
			new short[]{0}, null, null, new byte[]{7}, null,
			null, null, null, null,
			groups(0));

		// This part carries no per-face array, so the merge has to fall back to its model-level
		// priority - which is what the client's own mergeModels does. Filling zero here would
		// change draw order on exactly the multi-part NPCs merging exists for.
		RetroMesh withoutPriorities = part(2862, 4, 10f, 0);

		RetroMesh merged = RetroMeshMerger.merge(2870,
			Arrays.asList(withPriorities, withoutPriorities));

		assertArrayEquals(new byte[]{7, 4}, merged.getFaceRenderPriorities());

		// Textures default to -1 rather than 0, which is a real texture id
		assertNull(merged.getFaceTextures());
	}

	@Test
	public void testTheShippedDragonStillMergesToItsKnownSize() throws Exception
	{
		// Adult dragons shipped pre-merged at 387 verts / 776 faces before the parts were split
		// apart in the bundle. Merging them back has to land on the same numbers.
		RetroAssetBundle bundle = loadBundle();

		RetroMesh body = bundle.getMesh(2853);
		RetroMesh head = bundle.getMesh(2854);
		assertNotNull("dragon body 2853 is missing from the bundle", body);
		assertNotNull("dragon head 2854 is missing from the bundle", head);

		RetroMesh merged = RetroMeshMerger.merge(2853, Arrays.asList(body, head));
		assertEquals(387, merged.getVerticesCount());
		assertEquals(776, merged.getFaceCount());
	}

	@Test
	public void testTheGiantFamilySharesOneBodyMesh() throws Exception
	{
		RetroAssetBundle bundle = loadBundle();

		// One body, five heads - the arrangement the old modelIds[0] bundle key could not express,
		// since every one of these NPCs would have collided on 2870
		assertNotNull("giant body 2870 is missing", bundle.getMesh(2870));
		for (int head : new int[]{2862, 2864, 2865, 2867, 2868})
		{
			assertNotNull("giant head " + head + " is missing", bundle.getMesh(head));
		}
		for (int prop : new int[]{4990, 4991})
		{
			assertNotNull("giant prop " + prop + " is missing", bundle.getMesh(prop));
		}

		RetroMesh hillGiant = RetroMeshMerger.merge(2870,
			Arrays.asList(bundle.getMesh(2870), bundle.getMesh(2862)));
		assertEquals(177 + 86, hillGiant.getVerticesCount());
		assertEquals(355 + 155, hillGiant.getFaceCount());
	}

	/**
	 * The guard's 2005 head is the only textured mesh in the bundle, and the only one that names
	 * texture triangles. Everything about that mapping is dropped unless the bundle carries it, and
	 * a dropped mapping does not fail - it renders, wrongly, as 34 copies of a 64x64 texture.
	 */
	@Test
	public void testOnlyTheGuardHeadCarriesATextureMapping() throws Exception
	{
		RetroAssetBundle bundle = loadBundle();

		RetroMesh head = bundle.getMesh(294);
		assertNotNull("guard head 294 is missing from the bundle", head);
		assertEquals("the 2005 head names 18 texture triangles", 18, head.getTextureTriangleCount());

		byte[] coords = head.getTextureCoords();
		assertNotNull("the per-face triangle index is what the renderer branches on", coords);
		assertEquals(head.getFaceCount(), coords.length);

		int mapped = 0;
		for (byte coord : coords)
		{
			if (coord != -1)
			{
				mapped++;
				assertTrue("face triangle " + coord + " is past the table",
					(coord & 0xFF) < head.getTextureTriangleCount());
			}
		}
		assertEquals("34 of the head's 42 faces are textured", 34, mapped);
		assertEquals(8, coords.length - mapped);

		for (int meshId : new int[]{233, 246, 151, 176, 254, 185, 519, 541, 550, 2870, 2944})
		{
			RetroMesh mesh = bundle.getMesh(meshId);
			assertNotNull("mesh " + meshId + " is missing from the bundle", mesh);
			assertEquals("mesh " + meshId + " is untextured and should name no triangles",
				0, mesh.getTextureTriangleCount());
			assertNull("mesh " + meshId + " should carry no per-face triangle index",
				mesh.getTextureCoords());
		}
	}

	/**
	 * A texture triangle names its own part's vertices, so merging has to shift it exactly as a
	 * face index is shifted - and the per-face index into the triangle table has to shift by the
	 * triangles the earlier parts contributed. The guard is the case that exercises both: its head
	 * is the third of ten parts, so its triangles land at a vertex offset with no triangle offset.
	 */
	@Test
	public void testTheGuardHeadKeepsItsMappingThroughTheMerge() throws Exception
	{
		RetroAssetBundle bundle = loadBundle();

		RetroMesh torso = bundle.getMesh(233);
		RetroMesh cape = bundle.getMesh(246);
		RetroMesh head = bundle.getMesh(294);

		RetroMesh merged = RetroMeshMerger.merge(233, Arrays.asList(torso, cape, head));

		int vertexBase = torso.getVerticesCount() + cape.getVerticesCount();
		int faceBase = torso.getFaceCount() + cape.getFaceCount();

		assertEquals(head.getTextureTriangleCount(), merged.getTextureTriangleCount());
		for (int t = 0; t < head.getTextureTriangleCount(); t++)
		{
			assertEquals("triangle " + t + " corner 1",
				head.getTexIndices1()[t] + vertexBase, merged.getTexIndices1()[t]);
			assertEquals("triangle " + t + " corner 2",
				head.getTexIndices2()[t] + vertexBase, merged.getTexIndices2()[t]);
			assertEquals("triangle " + t + " corner 3",
				head.getTexIndices3()[t] + vertexBase, merged.getTexIndices3()[t]);
		}

		byte[] mergedCoords = merged.getTextureCoords();
		assertNotNull(mergedCoords);
		assertEquals(merged.getFaceCount(), mergedCoords.length);

		// The parts in front of the head are untextured, so their faces name no triangle
		for (int face = 0; face < faceBase; face++)
		{
			assertEquals("face " + face + " belongs to an untextured part", -1, mergedCoords[face]);
		}

		// No triangles came before the head's, so its indices carry through unshifted
		assertArrayEquals(head.getTextureCoords(),
			Arrays.copyOfRange(mergedCoords, faceBase, faceBase + head.getFaceCount()));
	}

	/**
	 * The other direction: a part whose triangles are not the first has its per-face indices shifted
	 * by everything ahead of it. Synthetic, because the bundle has only one textured mesh and so
	 * cannot exercise a non-zero triangle base.
	 */
	@Test
	public void testTriangleIndicesShiftByTheTrianglesAhead()
	{
		RetroMesh first = textured(1, 3, new byte[]{0, 1, -1}, 2);
		RetroMesh second = textured(2, 2, new byte[]{0, 0}, 1);
		RetroMesh untextured = part(3, 0, 50f, 0);

		RetroMesh merged = RetroMeshMerger.merge(1, Arrays.asList(first, second, untextured));

		assertArrayEquals(new byte[]{0, 1, -1, 2, 2, -1}, merged.getTextureCoords());
		assertEquals(3, merged.getTextureTriangleCount());

		// first's triangles address vertices 0..2 and stay put; second's address its own 0..2 and
		// shift by first's three vertices
		assertArrayEquals(new int[]{0, 0, 3}, merged.getTexIndices1());
		assertArrayEquals(new int[]{1, 1, 4}, merged.getTexIndices2());
		assertArrayEquals(new int[]{2, 2, 5}, merged.getTexIndices3());
	}

	/**
	 * The renderer reads a per-face triangle index as {@code textureFaces[face] & 0xff}, so 255 is
	 * indistinguishable from the -1 that means "no triangle" and only 0..254 are addressable. A
	 * merge that pushes a face past that must drop it to the face-as-UV projection rather than let
	 * the byte wrap onto some unrelated triangle.
	 *
	 * <p>Three indices are needed to pin this, because two of them cannot tell the behaviours apart:
	 * 254 is the last index that still works, so it separates a correct boundary from one off by
	 * one; 255 narrows to -1 whether it wrapped or fell back, so it proves nothing on its own; 256
	 * is the one that matters, because wrapping it yields 0 and silently maps the face onto the
	 * first part's own first triangle - a corruption with nothing on screen to announce it.
	 */
	@Test
	public void testAFaceMappedPastTheAddressableTrianglesFallsBackRatherThanWrapping()
	{
		RetroMesh first = textured(1, 1, new byte[]{0}, 254);
		RetroMesh second = textured(2, 3, new byte[]{0, 1, 2}, 3);

		RetroMesh merged = RetroMeshMerger.merge(1, Arrays.asList(first, second));

		// second's triangles shift by first's 254 to 254, 255 and 256. Only the first is
		// addressable; the other two must read as "no triangle", never as 255 & 0xff or 0
		assertArrayEquals(new byte[]{0, (byte) 254, -1, -1}, merged.getTextureCoords());

		// The table itself still concatenates in full - it is the per-face index that cannot reach
		// the tail, not the triangles that go missing
		assertEquals(257, merged.getTextureTriangleCount());
	}

	/** Three vertices, {@code coords.length} faces and {@code triangles} texture triangles. */
	private static RetroMesh textured(int id, int faceCount, byte[] coords, int triangles)
	{
		int[] i1 = new int[faceCount];
		int[] i2 = new int[faceCount];
		int[] i3 = new int[faceCount];
		short[] colors = new short[faceCount];
		short[] textures = new short[faceCount];
		for (int f = 0; f < faceCount; f++)
		{
			i1[f] = 0;
			i2[f] = 1;
			i3[f] = 2;
			textures[f] = 37;
		}

		int[] t1 = new int[triangles];
		int[] t2 = new int[triangles];
		int[] t3 = new int[triangles];
		for (int t = 0; t < triangles; t++)
		{
			t1[t] = 0;
			t2[t] = 1;
			t3[t] = 2;
		}

		return new RetroMesh(id, 0,
			new float[]{0f, 1f, 2f}, new float[]{0f, 1f, 2f}, new float[]{0f, 1f, 2f},
			i1, i2, i3, colors, null, null, null, textures,
			coords, t1, t2, t3, groups(0));
	}

	private static RetroAssetBundle loadBundle() throws Exception
	{
		try (InputStream in = RetroNpcSwapperPlugin.class.getResourceAsStream("retro-assets.dat"))
		{
			assertNotNull("retro-assets.dat is missing - run ./gradlew generateRetroAssets", in);
			return RetroAssetCodec.read(in);
		}
	}
}
