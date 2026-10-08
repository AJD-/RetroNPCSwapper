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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.retronpcswapper.inject.RetroAssetBundle;
import com.retronpcswapper.inject.RetroAssetCodec;
import com.retronpcswapper.inject.RetroMesh;
import java.io.InputStream;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

public class RetroSceneryTest
{
	/**
	 * A scenery whose mesh is missing from the bundle stays live in silence, so the shipped bundle
	 * is checked for every one - and for the mesh being static, since a stand-in never animates.
	 */
	@Test
	public void testTheShippedBundleCarriesEveryScenerysMesh() throws Exception
	{
		RetroAssetBundle bundle;
		try (InputStream in = RetroNpcSwapperPlugin.class.getResourceAsStream("retro-assets.dat"))
		{
			assertNotNull("retro-assets.dat is missing - run ./gradlew generateRetroAssets", in);
			bundle = RetroAssetCodec.read(in);
		}

		for (RetroScenery scenery : RetroScenery.values())
		{
			RetroMesh mesh = bundle.getMesh(scenery.meshId);
			assertNotNull(scenery + " mesh " + scenery.meshId + " is not in the bundle", mesh);
			assertFalse(scenery + " mesh " + scenery.meshId + " is rigged", mesh.isRigged());
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
