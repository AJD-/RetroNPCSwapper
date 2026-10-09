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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.retronpcswapper.inject.RetroAssetBundle;
import com.retronpcswapper.inject.RetroAssetCodec;
import com.retronpcswapper.inject.RetroClip;
import com.retronpcswapper.inject.RetroMesh;
import com.retronpcswapper.inject.RetroMeshMerger;
import com.retronpcswapper.inject.RetroModel;
import java.io.InputStream;
import java.util.Arrays;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

public class RetroSceneryTest
{
	/**
	 * A scenery whose mesh is missing from the bundle stays live in silence, so the shipped bundle
	 * is checked for every part of every one. A static scenery's parts must be unrigged, so nothing
	 * could ever move them; an animated one's must be rigged, and its clip and the rig the clip
	 * names carried with it.
	 */
	@Test
	public void testTheShippedBundleCarriesEveryScenerysMesh() throws Exception
	{
		RetroAssetBundle bundle = shippedBundle();

		for (RetroScenery scenery : RetroScenery.values())
		{
			if (scenery.source != RetroScenery.Source.BUNDLE)
			{
				continue;
			}

			for (int meshId : scenery.getMeshIds())
			{
				RetroMesh mesh = bundle.getMesh(meshId);
				assertNotNull(scenery + " mesh " + meshId + " is not in the bundle", mesh);
				assertEquals(scenery + " mesh " + meshId + " rigged", scenery.isAnimated(), mesh.isRigged());
			}

			if (scenery.isAnimated())
			{
				RetroClip clip = bundle.getClip(scenery.animationId);
				assertNotNull(scenery + " clip " + scenery.animationId + " is not in the bundle", clip);
				assertNotNull(scenery + " rig " + clip.getRigId() + " is not in the bundle", bundle.getRig(clip.getRigId()));
			}
		}
	}

	/**
	 * The November 2005 dairy cow: body and stool joined as the client joins a definition's models,
	 * welded where they touch, and its two dark greys recoloured as the definition asks - so neither
	 * is left anywhere on it.
	 */
	@Test
	public void testTheDairyCowIsTheNovember2005Mesh() throws Exception
	{
		RetroAssetBundle bundle = shippedBundle();
		RetroMesh body = bundle.getMesh(8237);
		RetroMesh stool = bundle.getMesh(8239);
		assertEquals(243, body.getVerticesCount());
		assertEquals(450, body.getFaceCount());
		assertEquals(42, stool.getVerticesCount());
		assertEquals(54, stool.getFaceCount());

		RetroMesh cow = RetroScenerySwapper.recolor(
			RetroMeshMerger.merge(8237, Arrays.asList(body, stool)),
			RetroScenery.DAIRY_COW.getRecolorFind(), RetroScenery.DAIRY_COW.getRecolorReplace());
		assertEquals(504, cow.getFaceCount());

		int recoloured = 0;
		for (short color : cow.getFaceColors())
		{
			assertTrue("colour " + color + " survived the recolour", color != 26 && color != 30);
			if (color == 142)
			{
				recoloured++;
			}
		}
		assertTrue("nothing was recoloured", recoloured > 0);

		// Zanaris's cow carries its own pairs over too, of which only the feet's brown lands
		RetroMesh fairy = RetroScenerySwapper.recolor(
			RetroMeshMerger.merge(8237, Arrays.asList(body, stool)),
			RetroScenery.FAIRY_DAIRY_COW.getRecolorFind(), RetroScenery.FAIRY_DAIRY_COW.getRecolorReplace());
		int changed = 0;
		for (int face = 0; face < cow.getFaceCount(); face++)
		{
			short plain = cow.getFaceColors()[face];
			short dark = fairy.getFaceColors()[face];
			if (plain != dark)
			{
				assertEquals("only the brown changes", 7566, plain);
				assertEquals(142, dark);
				changed++;
			}
		}
		assertEquals("the feet's faces", 40, changed);
	}

	private static RetroAssetBundle shippedBundle() throws Exception
	{
		try (InputStream in = RetroNpcSwapperPlugin.class.getResourceAsStream("retro-assets.dat"))
		{
			assertNotNull("retro-assets.dat is missing - run ./gradlew generateRetroAssets", in);
			return RetroAssetCodec.read(in);
		}
	}

	/** The 2005 well, as the bundle carries it: 132 vertices, 210 faces. */
	@Test
	public void testTheWellIsThe2005Mesh() throws Exception
	{
		try (InputStream in = RetroNpcSwapperPlugin.class.getResourceAsStream("retro-assets.dat"))
		{
			RetroMesh well = RetroAssetCodec.read(in).getMesh(RetroScenery.WELL.meshId);
			assertEquals(132, well.getVerticesCount());
			assertEquals(210, well.getFaceCount());
		}
	}

	/**
	 * The closed wardrobe's doors lie on a slanted plane that crosses the slanted cabinet front
	 * behind them: the tops sit just in front of it, the bottoms nearly a unit behind. The 2005
	 * client drew them over the front by priority; a depth-buffered renderer hides their bottom
	 * halves unless they are lifted clear.
	 */
	@Test
	public void testTheWardrobeDoorsAreDrawnInFrontOfTheCabinet() throws Exception
	{
		RetroMesh mesh;
		try (InputStream in = RetroNpcSwapperPlugin.class.getResourceAsStream("retro-assets.dat"))
		{
			mesh = RetroAssetCodec.read(in).getMesh(RetroScenery.WARDROBE.meshId);
		}

		RetroModel model = RetroScenerySwapper.light(mesh, RetroScenery.WARDROBE);

		// The cabinet front runs from x = 4 at its top, y = -176, to x = -4 on the floor
		float[] x = mesh.getVerticesX();
		float[] y = mesh.getVerticesY();
		int[][] faces = {mesh.getFaceIndices1(), mesh.getFaceIndices2(), mesh.getFaceIndices3()};
		byte[] priorities = mesh.getFaceRenderPriorities();
		int doorFaces = 0;
		for (int f = 0; f < mesh.getFaceCount(); f++)
		{
			if (priorities[f] == 0 || !onCabinetFront(x, y, faces, f))
			{
				continue;
			}

			doorFaces++;
			for (int[] corner : faces)
			{
				int v = corner[f];
				assertTrue("face " + f + " vertex " + v + " is behind the cabinet front",
					inFrontOfCabinet(model.getVerticesX()[v], model.getVerticesY()[v]) > 0);
			}
		}
		assertTrue(doorFaces > 0);
	}

	/** Signed distance along x from the wardrobe's cabinet front: positive is in front of it. */
	private static float inFrontOfCabinet(float x, float y)
	{
		return x + y / 22 + 4;
	}

	/** Whether a face lies on the cabinet front, which stands from the floor to y = -176. */
	private static boolean onCabinetFront(float[] x, float[] y, int[][] faces, int f)
	{
		for (int[] corner : faces)
		{
			int v = corner[f];
			if (y[v] < -176 || Math.abs(inFrontOfCabinet(x[v], y[v])) >= 2)
			{
				return false;
			}
		}
		return true;
	}

	@Test
	public void testEveryScenerySpeaksForItsObjects()
	{
		for (RetroScenery scenery : RetroScenery.values())
		{
			assertTrue(scenery + " restores no objects", scenery.getObjectIds().length > 0);
			for (int objectId : scenery.getObjectIds())
			{
				assertEquals(scenery, RetroScenery.forObject(objectId));
			}
		}
	}

	@Test
	public void testObjectsAreLookedUpByLiveId()
	{
		assertEquals(RetroScenery.WELL, RetroScenery.forObject(ObjectID.WELL));
		assertEquals(RetroScenery.MYSTICAL_WALL_CHART, RetroScenery.forObject(ObjectID.WITCHESWALLCHART));
		assertNull(RetroScenery.forObject(ObjectID.GNOME_WELL));
	}
}
