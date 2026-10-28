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

import com.retronpcswapper.RetroGoblinMapping2006;
import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import net.runelite.cache.IndexType;
import net.runelite.cache.definitions.ModelDefinition;
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
		assertEquals(31, ids.size());
		try (Store store = new Store(liveDir))
		{
			store.load();
			for (int id : ids)
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
			}
		}
	}

	private static short[] sorted(short[] colors)
	{
		short[] copy = colors.clone();
		Arrays.sort(copy);
		return copy;
	}
}
