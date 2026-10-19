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
import static org.junit.Assume.assumeTrue;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.retronpcswapper.inject.ClasspathAssetSource;
import com.retronpcswapper.inject.RetroAssetBundle;
import com.retronpcswapper.inject.RetroMesh;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.runelite.api.gameval.NpcID;
import net.runelite.cache.IndexType;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.loaders.ModelLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.Store;
import org.junit.Test;

/**
 * Places each bundled Kalphite where the live composition draws its own model.
 * <p>
 * The live lair Soldier's model 24595 is the POH Soldier's 24593 moved 71 units along Z, so it
 * stands half a tile off its footprint's center while every other live Kalphite is centred. The
 * 2005 meshes are all centred, so without its offset the swapped Soldier stands half a tile away
 * from where the server draws it.
 */
public class KalphitePlacementTest
{
	// An eighth of a tile. The 2005 and live meshes are different shapes, so their centroids never
	// agree exactly - the centred families sit within 9 of each other, the Soldier's miss was 80
	private static final float CENTROID_TOLERANCE = 16f;
	// The share of the live model's width the 2005 one may miss by
	private static final float WIDTH_TOLERANCE = 0.1f;

	@Test
	public void testKalphitesStandWhereTheLiveModelDoes() throws Exception
	{
		File liveDir = new File(System.getProperty("user.home"), ".runelite/jagexcache/oldschool/LIVE");
		assumeTrue("live cache not present", liveDir.isDirectory());

		RetroNpcMapping.load(loadCommittedEntries());
		RetroAssetBundle bundle = new ClasspathAssetSource().load();

		// npc id, name, live model, live composition's widthScale, whether the width must match.
		// Only the Soldiers' sizes are fitted to the live models; the rest keep their 2005 sizes,
		// which the live remodels do not follow - the larva is a quarter narrower
		Object[][] kalphites = {
			{NpcID.KALPHITE_SOLDIER, "Kalphite Soldier", 24595, 128, true},
			{NpcID.KALPHITE_SOLDIER_STRONGHOLDCAVE, "Kalphite Soldier", 24595, 128, true},
			{NpcID.POH_KALPHITE_SOLDIER, "Kalphite Soldier", 24593, 172, true},
			{NpcID.KALPHITE_WORKER, "Kalphite Worker", 24596, 128, false},
			{NpcID.KALPHITE_LORD, "Kalphite Guardian", 24594, 128, false},
			{NpcID.KALPHITE_LARVA, "Kalphite Larva", 24591, 128, false}
		};

		try (Store store = new Store(liveDir))
		{
			store.load();

			for (Object[] k : kalphites)
			{
				int id = (Integer) k[0];
				RetroNpcData data = RetroNpcMapping.get(id, (String) k[1]);
				assertNotNull("Kalphite " + id + " must be mapped", data);

				RetroMesh mesh = bundle.getMesh(data.getInjectedModelIds()[0]);
				RetroMesh placed = RetroModelCache.scaled(mesh, mesh.getFaceColors(),
					data.getScaleXZ(), data.getScaleY(), data.getOffsetZ());
				float[] retro = footprint(placed.getVerticesX(), placed.getVerticesZ(), 1f);

				ModelDefinition model = loadLive(store, (Integer) k[2]);
				float[] liveX = new float[model.vertexCount];
				float[] liveZ = new float[model.vertexCount];
				for (int v = 0; v < model.vertexCount; v++)
				{
					liveX[v] = model.vertexX[v];
					liveZ[v] = model.vertexZ[v];
				}
				float[] live = footprint(liveX, liveZ, (Integer) k[3] / 128f);

				assertEquals("Kalphite " + id + " X centroid", live[0], retro[0], CENTROID_TOLERANCE);
				assertEquals("Kalphite " + id + " Z centroid", live[1], retro[1], CENTROID_TOLERANCE);
				if ((Boolean) k[4])
				{
					assertEquals("Kalphite " + id + " width", live[2], retro[2], live[2] * WIDTH_TOLERANCE);
				}
			}
		}
	}

	/**
	 * Mean X, mean Z and X extent of the vertices, resized by {@code scale}.
	 */
	private static float[] footprint(float[] x, float[] z, float scale)
	{
		double sumX = 0;
		double sumZ = 0;
		float minX = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE;
		for (int v = 0; v < x.length; v++)
		{
			sumX += x[v];
			sumZ += z[v];
			minX = Math.min(minX, x[v]);
			maxX = Math.max(maxX, x[v]);
		}
		return new float[]{
			(float) (sumX / x.length) * scale,
			(float) (sumZ / x.length) * scale,
			(maxX - minX) * scale};
	}

	private static ModelDefinition loadLive(Store store, int modelId) throws Exception
	{
		Archive archive = store.getIndex(IndexType.MODELS).getArchive(modelId);
		assertNotNull("live model " + modelId, archive);
		byte[] data = archive.decompress(store.getStorage().loadArchive(archive));
		return new ModelLoader().load(modelId, data);
	}

	private static List<RetroNpcMappingEntry> loadCommittedEntries() throws Exception
	{
		try (InputStream in = RetroNpcSwapperPlugin.class.getResourceAsStream("npc-mappings.json"))
		{
			assertNotNull("npc-mappings.json resource missing - run ./gradlew generateNpcMappings", in);
			return new Gson().fromJson(
				new InputStreamReader(in, StandardCharsets.UTF_8),
				new TypeToken<List<RetroNpcMappingEntry>>() {}.getType());
		}
	}
}
