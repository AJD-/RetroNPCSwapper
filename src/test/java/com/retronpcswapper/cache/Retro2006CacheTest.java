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
package com.retronpcswapper.cache;

import com.retronpcswapper.GoblinEra;
import com.retronpcswapper.RetroGoblinMapping2006;
import com.retronpcswapper.RetroNpcData;
import com.retronpcswapper.RetroNpcMapping;
import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.runelite.api.gameval.NpcID;
import net.runelite.cache.IndexType;
import net.runelite.cache.NpcManager;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.NpcDefinition;
import net.runelite.cache.definitions.loaders.ModelLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.Store;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class Retro2006CacheTest
{
	private Retro2006CacheReader reader;

	@Before
	public void open()
	{
		File dir = Retro2006CacheReader.resolveDir();
		assumeTrue("2006 cache not present at " + dir, dir.isDirectory());
		reader = Retro2006CacheReader.open(dir);
		assertNotNull("could not open the 2006 cache at " + dir, reader);
	}

	@After
	public void close()
	{
		if (reader != null)
		{
			reader.close();
		}
	}

	@Test
	public void testNpcDefinitionsDecode()
	{
		Map<Integer, RetroNpcDefinition> defs = reader.npcDefinitions();
		assertEquals(5193, defs.size());
		// The 5026-5045 gap is real, not a shift
		assertFalse(defs.containsKey(5030));

		RetroNpcDefinition goblin = defs.get(2274);
		assertEquals("Goblin", goblin.getName());
		assertArrayEquals(new int[]{5536, 5539, 5544, 5547}, goblin.getModels());
		assertArrayEquals(new int[]{15551}, goblin.getAdditionalModels());
		assertEquals(311, goblin.getStanceAnimation());
		assertEquals(308, goblin.getWalkAnimation());
		assertEquals(125, goblin.getScaleXZ());
		assertEquals(125, goblin.getScaleY());

		// 14238->10782 and 2974->916, in def order
		RetroNpcDefinition red = defs.get(4479);
		assertArrayEquals(new short[]{14238, 2974}, red.getOriginalColors());
		assertArrayEquals(new short[]{10782, 916}, red.getReplacementColors());
	}

	@Test
	public void testModelsDecodeInTheOldFormat()
	{
		ModelDefinition m = reader.model(2951);
		assertEquals(60, m.vertexCount);
		assertEquals(111, m.faceCount);
		// One past the last archive in index 7
		assertNull(reader.model(20506));
	}

	/** The live cache still holds every 2006 goblin mesh. */
	@Test
	public void testEvery2006GoblinMeshIsIdenticalInLive() throws Exception
	{
		File liveDir = RetroAssetGenerator.resolveLiveCacheDir();
		assumeTrue("live cache not found", liveDir != null);

		Set<Integer> ids = RetroGoblinMapping2006.modelIds();
		assertEquals(36, ids.size());
		try (Store store = new Store(liveDir))
		{
			store.load();
			for (int id : ids)
			{
				assertIdenticalInLive(store, id);
			}
		}
	}

	/** The 2006 rams are the kits live 1261-1265 still draw, so there is nothing to swap. */
	@Test
	public void testRamsAreAlreadyTheir2006Models() throws Exception
	{
		Map<Integer, RetroNpcDefinition> defs = reader.npcDefinitions();
		int[][] kits = {{20283, 20289, 20287}, {20292, 20295, 20294}, {20292, 20295, 20294},
			{20292, 20295, 20294}, {20282, 20288, 20286}};
		int[] ids = {3672, 3673, 5168, 5169, 5170};
		Set<Integer> meshes = new TreeSet<>();
		for (int i = 0; i < ids.length; i++)
		{
			RetroNpcDefinition ram = defs.get(ids[i]);
			assertEquals("Ram", ram.getName());
			assertArrayEquals(kits[i], ram.getModels());
			for (int mesh : kits[i])
			{
				meshes.add(mesh);
			}
		}
		assertEquals(9, meshes.size());

		File liveDir = RetroAssetGenerator.resolveLiveCacheDir();
		assumeTrue("live cache not found", liveDir != null);
		try (Store store = new Store(liveDir))
		{
			store.load();
			for (int id : meshes)
			{
				assertIdenticalInLive(store, id);
			}
		}
	}

	/** The Thing, def 3579: the kit live replaced at the same ids. */
	@Test
	public void testTheThingDefinition() throws Exception
	{
		RetroNpcDefinition thing = reader.npcDefinitions().get(3579);
		assertEquals("Sheep", thing.getName());
		assertArrayEquals(new int[]{14084, 14085}, thing.getModels());
		assertEquals(3569, thing.getStanceAnimation());
		assertEquals(3568, thing.getWalkAnimation());
		assertEquals(128, thing.getScaleXZ());
		assertEquals(128, thing.getScaleY());
		assertEquals(0, thing.getOriginalColors() == null ? 0 : thing.getOriginalColors().length);

		assertEquals(242, reader.model(14084).vertexCount);
		assertEquals(109, reader.model(14085).vertexCount);

		File liveDir = RetroAssetGenerator.resolveLiveCacheDir();
		assumeTrue("live cache not found", liveDir != null);
		try (Store store = new Store(liveDir))
		{
			store.load();
			for (int id : new int[]{14084, 14085})
			{
				Archive archive = store.getIndex(IndexType.MODELS).getArchive(id);
				ModelDefinition live = new ModelLoader().load(id, archive.decompress(store.getStorage().loadArchive(archive)));
				assertNotEquals("live " + id + " is a later model", reader.model(id).vertexCount, live.vertexCount);
			}
		}
	}

	/** A Soul's Bane: the goblin and unicorn kits live replaced, on meshes it still holds. */
	@Test
	public void testAngryGoblinAndUnicornDefinitions() throws Exception
	{
		Map<Integer, RetroNpcDefinition> defs = reader.npcDefinitions();
		Object[][] rows = {
			{3648, "Angry goblin", RetroNpcMapping.ANGRY_GOBLIN},
			{3663, "Angry goblin", RetroNpcMapping.ANGRY_GOBLIN},
			{3646, "Angry unicorn", RetroNpcMapping.ANGRY_UNICORN},
			{3661, "Angry unicorn", RetroNpcMapping.ANGRY_UNICORN}};
		for (Object[] row : rows)
		{
			RetroNpcDefinition def = defs.get((int) row[0]);
			RetroNpcData data = (RetroNpcData) row[2];
			String label = "def " + row[0];
			assertEquals(label, row[1], def.getName());
			assertArrayEquals(label, data.getRetroModelIds(), def.getModels());
			assertArrayEquals(label, data.getOriginalColors(), def.getOriginalColors());
			assertArrayEquals(label, data.getReplacementColors(), def.getReplacementColors());
			assertEquals(label, data.getIdleAnimationId(), def.getStanceAnimation());
			assertEquals(label, data.getWalkAnimationId(), def.getWalkAnimation());
			assertEquals(label, data.getScaleXZ(), def.getScaleXZ());
			assertEquals(label, data.getScaleY(), def.getScaleY());
		}

		File liveDir = RetroAssetGenerator.resolveLiveCacheDir();
		assumeTrue("live cache not found", liveDir != null);
		try (Store store = new Store(liveDir))
		{
			store.load();
			// The goblin parts are covered by the 2006 goblin set
			for (int id : RetroNpcMapping.ANGRY_UNICORN.getRetroModelIds())
			{
				assertIdenticalInLive(store, id);
			}
		}
	}

	/** The Hunter birds arrived in Nov-2006 as recolours of the 2005 gull, which live still holds. */
	@Test
	public void testHunterBirdDefinitions() throws Exception
	{
		Map<Integer, RetroNpcDefinition> defs = reader.npcDefinitions();
		Object[][] rows = {
			{5073, "Crimson swift", RetroNpcMapping.CRIMSON_SWIFT},
			{5075, "Golden warbler", RetroNpcMapping.GOLDEN_WARBLER},
			{5076, "Copper longtail", RetroNpcMapping.COPPER_LONGTAIL}};
		for (Object[] row : rows)
		{
			RetroNpcDefinition def = defs.get((int) row[0]);
			RetroNpcData data = (RetroNpcData) row[2];
			String label = "def " + row[0];
			assertEquals(label, row[1], def.getName());
			assertArrayEquals(label, data.getRetroModelIds(), def.getModels());
			assertArrayEquals(label, data.getOriginalColors(),
				def.getOriginalColors() == null ? new short[0] : def.getOriginalColors());
			assertArrayEquals(label, data.getReplacementColors(),
				def.getReplacementColors() == null ? new short[0] : def.getReplacementColors());
			assertEquals(label, data.getIdleAnimationId(), def.getStanceAnimation());
			assertEquals(label, data.getWalkAnimationId(), def.getWalkAnimation());
			assertEquals(label, data.getScaleXZ(), def.getScaleXZ());
		}

		File liveDir = RetroAssetGenerator.resolveLiveCacheDir();
		assumeTrue("live cache not found", liveDir != null);
		try (Store store = new Store(liveDir))
		{
			store.load();
			assertIdenticalInLive(store, 2850);
		}
	}

	/**
	 * The mounted terrorchick gnome first appears here, on meshes live still holds; the 2005
	 * terrorbird meshes it shares clips with are pinned alongside.
	 */
	@Test
	public void testTerrorchickDefinitionAndTerrorbirdMeshes() throws Exception
	{
		Map<Integer, RetroNpcDefinition> defs = reader.npcDefinitions();
		RetroNpcDefinition def = defs.get(4570);
		RetroNpcData data = RetroNpcMapping.TERRORCHICK_MOUNTED;
		assertEquals("Mounted terrorchick gnome", def.getName());
		assertArrayEquals(data.getRetroModelIds(), def.getModels());
		assertNull(def.getOriginalColors());
		assertFalse(data.hasRecolors());
		assertEquals(data.getIdleAnimationId(), def.getStanceAnimation());
		assertEquals(data.getWalkAnimationId(), def.getWalkAnimation());
		assertEquals(data.getScaleXZ(), def.getScaleXZ());
		assertEquals(data.getScaleY(), def.getScaleY());
		assertArrayEquals(new int[]{16970}, def.getAdditionalModels());

		File liveDir = RetroAssetGenerator.resolveLiveCacheDir();
		assumeTrue("live cache not found", liveDir != null);
		try (Store store = new Store(liveDir))
		{
			store.load();
			NpcManager npcs = new NpcManager(store);
			npcs.load();
			// Live already talks with the 2006 head, so the row carries none
			assertArrayEquals(def.getAdditionalModels(), npcs.get(NpcID.ALUFT_DELIVERY_MOUNTED_TERRORCHIC).chatheadModels);
			assertFalse(data.hasChathead());

			for (int id : new int[]{17161, 17160, 3011, 3012, 3013})
			{
				assertIdenticalInLive(store, id);
			}
		}
	}

	/** The generals and Grubfoot, by the live id each def is paired with. */
	@Test
	public void testGeneralAndGrubfootDefinitions()
	{
		Map<Integer, RetroNpcDefinition> defs = reader.npcDefinitions();
		Object[][] rows = {
			{4493, "General Bentnoze", NpcID.GENERAL_BENTNOZE_RED, new int[]{15551}},
			{4494, "General Wartface", NpcID.GENERAL_WARTFACE_GREEN, new int[]{144}},
			{3392, "General Bentnoze", NpcID._100_GENERAL_BENTNOZE, new int[]{15551}},
			{3391, "General Wartface", NpcID._100_GENERAL_WARTFACE, new int[]{144}},
			{4496, "Grubfoot", NpcID.CATWALK_GOBLIN_BROWN, new int[]{142, 15552}},
			{4497, "Grubfoot", NpcID.CATWALK_GOBLIN_ORANGE, new int[]{142, 15552}},
			{4498, "Grubfoot", NpcID.CATWALK_GOBLIN_BLUE, new int[]{142, 15552}}};
		for (Object[] row : rows)
		{
			RetroNpcDefinition def = defs.get((int) row[0]);
			RetroNpcData data = RetroNpcMapping.get((int) row[2], (String) row[1], GoblinEra.RETRO_2006);
			String label = "def " + row[0];
			assertNotNull(label, data);
			assertEquals(label, row[1], def.getName());
			assertArrayEquals(label, data.getRetroModelIds(), def.getModels());
			assertArrayEquals(label, data.getOriginalColors(), def.getOriginalColors());
			assertArrayEquals(label, data.getReplacementColors(), def.getReplacementColors());
			assertEquals(label, data.getIdleAnimationId(), def.getStanceAnimation());
			assertEquals(label, data.getWalkAnimationId(), def.getWalkAnimation());
			assertEquals(label, data.getScaleXZ(), def.getScaleXZ());
			assertEquals(label, data.getScaleY(), def.getScaleY());
			assertArrayEquals(label, (int[]) row[3], def.getAdditionalModels());
			assertArrayEquals(label, (int[]) row[3], data.getChatheadModelIds());
		}
	}

	/** Live still draws the Recipe for Disaster generals from defs 3407-3412, so 2006 has nothing to swap. */
	@Test
	public void testRecipeForDisasterGeneralsAreAlreadyTheir2006Definitions() throws Exception
	{
		File liveDir = RetroAssetGenerator.resolveLiveCacheDir();
		assumeTrue("live cache not found", liveDir != null);

		Map<Integer, RetroNpcDefinition> defs = reader.npcDefinitions();
		int[][] pairs = {
			{NpcID.HUNDRED_GOBLIN_1, 3407}, {NpcID.HUNDRED_GOBLIN_2, 3408},
			{NpcID.HUNDRED_GOBLIN_1_FROZEN_BASE, 3410}, {NpcID.HUNDRED_GOBLIN_2_FROZEN_BASE, 3412}};
		try (Store store = new Store(liveDir))
		{
			store.load();
			NpcManager npcs = new NpcManager(store);
			npcs.load();
			for (int[] pair : pairs)
			{
				NpcDefinition live = npcs.get(pair[0]);
				RetroNpcDefinition def = defs.get(pair[1]);
				String label = "live " + pair[0] + " / def " + pair[1];
				assertEquals(label, def.getName(), live.name);
				assertArrayEquals(label, def.getModels(), live.models);
				assertArrayEquals(label, def.getOriginalColors(), live.recolorToFind);
				assertArrayEquals(label, def.getReplacementColors(), live.recolorToReplace);
				assertEquals(label, def.getStanceAnimation(), live.standingAnimation);
				assertEquals(label, def.getScaleXZ(), live.widthScale);
				assertEquals(label, def.getScaleY(), live.heightScale);
			}
		}
	}

	/** The 2006 angry bears and giant rats are the kits live still draws, so there is nothing to swap. */
	@Test
	public void testAngryBearsAndRatsAreAlreadyTheir2006Models() throws Exception
	{
		Map<Integer, RetroNpcDefinition> defs = reader.npcDefinitions();
		int[] bear = {18882, 14288};
		int[] rat = {18891, 18889, 18894, 18887};
		int[] rat2 = {18892, 18890, 18895, 18887};
		// def, name, kit; live 1060, 4692, 1062, 4689, 4690 in order
		Object[][] rows = {
			{3645, "Angry bear", bear}, {3664, "Angry bear", bear},
			{3647, "Angry giant rat", rat}, {3662, "Angry giant rat", rat}, {4940, "Angry giant rat", rat2}};
		Set<Integer> meshes = new TreeSet<>();
		for (Object[] row : rows)
		{
			RetroNpcDefinition def = defs.get((int) row[0]);
			assertEquals(row[1], def.getName());
			assertArrayEquals((int[]) row[2], def.getModels());
			for (int mesh : (int[]) row[2])
			{
				meshes.add(mesh);
			}
		}
		assertEquals(9, meshes.size());

		File liveDir = RetroAssetGenerator.resolveLiveCacheDir();
		assumeTrue("live cache not found", liveDir != null);
		try (Store store = new Store(liveDir))
		{
			store.load();
			for (int id : meshes)
			{
				assertIdenticalInLive(store, id);
			}
		}
	}

	private void assertIdenticalInLive(Store store, int id) throws Exception
	{
		Archive archive = store.getIndex(IndexType.MODELS).getArchive(id);
		assertNotNull("live archive " + id, archive);
		ModelDefinition live = new ModelLoader().load(id, archive.decompress(store.getStorage().loadArchive(archive)));
		ModelDefinition retro = reader.model(id);
		assertNotNull("2006 model " + id, retro);
		assertEquals("vertices " + id, retro.vertexCount, live.vertexCount);
		assertEquals("faces " + id, retro.faceCount, live.faceCount);
		assertArrayEquals("colours " + id, sorted(retro.faceColors), sorted(live.faceColors));
		assertArrayEquals("x " + id, retro.vertexX, live.vertexX);
		assertArrayEquals("y " + id, retro.vertexY, live.vertexY);
		assertArrayEquals("z " + id, retro.vertexZ, live.vertexZ);
		assertArrayEquals("face a " + id, retro.faceIndices1, live.faceIndices1);
		assertArrayEquals("face b " + id, retro.faceIndices2, live.faceIndices2);
		assertArrayEquals("face c " + id, retro.faceIndices3, live.faceIndices3);
		// load() nulls packedVertexGroups, so the rig is read unpacked
		assertNotNull("rig " + id, retro.getVertexGroups());
		assertArrayEquals("rig " + id, retro.getVertexGroups(), live.getVertexGroups());
	}

	private static short[] sorted(short[] colors)
	{
		short[] copy = colors.clone();
		Arrays.sort(copy);
		return copy;
	}
}
