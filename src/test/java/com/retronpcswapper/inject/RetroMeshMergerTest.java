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

	private static RetroAssetBundle loadBundle() throws Exception
	{
		try (InputStream in = RetroNpcSwapperPlugin.class.getResourceAsStream("retro-assets.dat"))
		{
			assertNotNull("retro-assets.dat is missing - run ./gradlew generateRetroAssets", in);
			return RetroAssetCodec.read(in);
		}
	}
}
