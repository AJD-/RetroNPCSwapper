package com.retronpcswapper.cache;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.retronpcswapper.RetroNpcMappingEntry;
import com.retronpcswapper.RetroNpcSwapperPlugin;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class RetroCacheTest
{
	private static final File CACHE_DIR = new File("retrocache/2005cache");

	/**
	 * Every sequence the 2005 {@code seq.idx} declares, and the guard's shield block among them.
	 *
	 * <p>This is the tripwire for a truncated payload. {@code seq.dat} is a two-block bzip2
	 * stream, and while only its first block was being decoded the table stopped at id 1124 and
	 * everything past it either vanished or decoded out of the re-emitted tail - which is how
	 * 1156 came to be written off as an opcode this decoder could not read. The count is the
	 * cheap half; sequence 1156 is the half that says the frames are actually right.
	 */
	@Test
	public void testEvery2005SequenceDecodes() throws Exception
	{
		assumeTrue("2005 cache not present at " + CACHE_DIR, CACHE_DIR.exists());

		RetroCacheReader reader = new RetroCacheReader(CACHE_DIR);
		assertTrue("could not open the 2005 cache", reader.init());
		Map<String, byte[]> config = reader.readArchive(reader.readFile(0, 2));
		byte[] seqDat = config.get(String.valueOf(RetroCacheReader.hashFileName("seq.dat")));
		byte[] seqIdx = config.get(String.valueOf(RetroCacheReader.hashFileName("seq.idx")));

		assertEquals("seq.dat is truncated", 141873, seqDat.length);

		Map<Integer, RetroSeqDefinition> seqs = RetroSeqDecoder.decodeAll(seqDat, seqIdx);
		assertEquals("every declared 2005 sequence should decode", 1670, seqs.size());

		for (int id : new int[]{1155, 1156, 1157})
		{
			assertNotNull("2005 sequence " + id + " is missing", seqs.get(id));
		}

		// HUMAN_SHIELD_DEFENCE: raise, hold, lower - the frame list runs out and back
		assertArrayEquals(new int[]{
				4192, 4193, 4194, 4195, 4196, 4197, 4198, 4199, 4200,
				4199, 4198, 4197, 4196, 4195, 4194, 4193, 4192},
			seqs.get(1156).getFrameIds());
	}

	@Test
	public void test317NpcDecoder() throws Exception
	{
		assumeTrue("2005 cache not present at " + CACHE_DIR, CACHE_DIR.exists());

		Map<Integer, RetroNpcDefinition> defs = NpcMappingGenerator.decodeDefinitions(CACHE_DIR);
		assertTrue(defs.size() > 1000);

		RetroNpcDefinition lesserDemon = defs.get(82);
		assertNotNull(lesserDemon);
		assertEquals("Lesser demon", lesserDemon.getName());
		assertEquals(82, lesserDemon.getCombatLevel());
		assertNotNull(lesserDemon.getModels());
		assertTrue(lesserDemon.getModels().length > 0);

		RetroNpcDefinition blueDragon = defs.get(55);
		assertNotNull(blueDragon);
		assertEquals("Blue dragon", blueDragon.getName());
		assertEquals(111, blueDragon.getCombatLevel());
		assertNotNull(blueDragon.getModels());
		assertTrue(blueDragon.getModels().length > 0);

		RetroNpcDefinition imp708 = defs.get(708);
		assertNotNull(imp708);
		assertEquals("Imp", imp708.getName());
		assertEquals(171, imp708.getStanceAnimation());
		assertEquals(168, imp708.getWalkAnimation());
	}

	/**
	 * Regenerates the mapping entries from the local 2005 cache and verifies they
	 * match the committed npc-mappings.json resource, catching a stale resource
	 * after decoder or category-matching changes. Skipped when no local cache is
	 * present (the cache is intentionally not committed).
	 */
	@Test
	public void testCommittedMappingsMatchGenerator() throws Exception
	{
		assumeTrue("2005 cache not present at " + CACHE_DIR, CACHE_DIR.exists());

		List<RetroNpcMappingEntry> generated =
			NpcMappingGenerator.buildEntries(NpcMappingGenerator.decodeDefinitions(CACHE_DIR));

		List<RetroNpcMappingEntry> committed;
		try (InputStream in = RetroNpcSwapperPlugin.class.getResourceAsStream("npc-mappings.json"))
		{
			assertNotNull("npc-mappings.json resource missing - run ./gradlew generateNpcMappings", in);
			committed = new Gson().fromJson(
				new InputStreamReader(in, StandardCharsets.UTF_8),
				new TypeToken<List<RetroNpcMappingEntry>>() {}.getType());
		}

		Gson gson = new Gson();
		assertEquals("Committed npc-mappings.json is stale - run ./gradlew generateNpcMappings",
			gson.toJson(generated), gson.toJson(committed));
	}
}
