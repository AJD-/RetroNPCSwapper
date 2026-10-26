package com.retronpcswapper.cache;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.retronpcswapper.RetroNpcData;
import com.retronpcswapper.RetroNpcMapping;
import com.retronpcswapper.RetroNpcMappingEntry;
import com.retronpcswapper.RetroNpcSwapperPlugin;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class RetroCacheTest
{
	private static final File CACHE_DIR = new File("retrocache/2005cache");

	private static final File NOV_CACHE_DIR = new File("retrocache/nov2005cache");

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

	/**
	 * Every archetype's dialogue head is the opcode 60 list of the 2005 definition it was built from,
	 * and a definition that had none leaves its archetype headless - which is what keeps the modern
	 * chathead for an NPC 2005 could not talk to, rather than one guessed from its kit.
	 */
	@Test
	public void testArchetypeChatheadsAreTheir2005Definitions() throws Exception
	{
		assumeTrue("2005 cache not present at " + CACHE_DIR, CACHE_DIR.exists());
		assumeTrue("Nov-2005 cache not present at " + NOV_CACHE_DIR, NOV_CACHE_DIR.exists());

		Map<Integer, RetroNpcDefinition> feb = NpcMappingGenerator.decodeDefinitions(CACHE_DIR);
		assertChathead(feb, 1183, RetroNpcMapping.ELF_ARCHER);
		assertChathead(feb, 1184, RetroNpcMapping.ELF_WARRIOR);
		assertChathead(feb, 1202, RetroNpcMapping.ARIANWYN_DEFAULT);
		assertChathead(feb, 1202, RetroNpcMapping.ARIANWYN_BOW);
		assertChathead(feb, 1680, RetroNpcMapping.ISLWYN_DEFAULT);
		assertChathead(feb, 1680, RetroNpcMapping.ISLWYN_BOW);
		assertChathead(feb, 1679, RetroNpcMapping.ELUNED_DEFAULT);
		assertChathead(feb, 335, RetroNpcMapping.ELENA_PRISONER);
		assertChathead(feb, 715, RetroNpcMapping.ELENA_DEFAULT);
		assertChathead(feb, 298, RetroNpcMapping.GOBLIN_GREEN);
		assertChathead(feb, 299, RetroNpcMapping.GOBLIN_RED);
		assertChathead(feb, 489, RetroNpcMapping.GOBLIN_GUARD_DEFAULT);
		assertChathead(feb, 100, RetroNpcMapping.GOBLIN_DEFAULT);
		assertChathead(feb, 887, RetroNpcMapping.CARNILLEAN_GUARD_DEFAULT);
		for (Object[] row : ROUND_TABLE_DEFINITIONS)
		{
			assertChathead(feb, (int) row[0], (RetroNpcData) row[1]);
		}
		assertChathead(feb, 9, RetroNpcMapping.GUARD_DEFAULT);

		Map<Integer, RetroNpcDefinition> nov = NpcMappingGenerator.decodeDefinitions(NOV_CACHE_DIR);
		assertChathead(nov, 2359, RetroNpcMapping.NOV_ELF_WARRIOR);
		assertChathead(nov, 2360, RetroNpcMapping.NOV_ELF_WARRIOR2);
		assertChathead(nov, 2361, RetroNpcMapping.NOV_ELF_ARCHER);
		assertChathead(nov, 2362, RetroNpcMapping.NOV_ELF_ARCHER2);
		assertChathead(nov, 1777, RetroNpcMapping.ILFEEN);
		assertChathead(nov, 2352, RetroNpcMapping.EUDAV);
		assertChathead(nov, 2353, RetroNpcMapping.ORONWEN);
		assertChathead(nov, 2354, RetroNpcMapping.LLETYA_BANKER_MALE);
		assertChathead(nov, 2355, RetroNpcMapping.LLETYA_BANKER_FEMALE);
		assertChathead(nov, 2356, RetroNpcMapping.DALLDAV);
		assertChathead(nov, 2357, RetroNpcMapping.GETHIN);
		assertChathead(nov, 2363, RetroNpcMapping.GOREU);
		assertChathead(nov, 2364, RetroNpcMapping.YSGAWYN);
		assertChathead(nov, 2365, RetroNpcMapping.ARVEL);
		assertChathead(nov, 2366, RetroNpcMapping.MAWRTH);
		assertChathead(nov, 2367, RetroNpcMapping.KELYN);
		assertChathead(nov, 2368, RetroNpcMapping.EOIN);
		assertChathead(nov, 2369, RetroNpcMapping.IONA);
	}

	private static final Object[][] ROUND_TABLE_DEFINITIONS = {
		{251, RetroNpcMapping.KING_ARTHUR}, {239, RetroNpcMapping.SIR_LANCELOT}, {240, RetroNpcMapping.SIR_GAWAIN},
		{241, RetroNpcMapping.SIR_KAY}, {242, RetroNpcMapping.SIR_BEDIVERE}, {243, RetroNpcMapping.SIR_TRISTRAM},
		{244, RetroNpcMapping.SIR_PELLEAS}, {245, RetroNpcMapping.SIR_LUCAN}, {246, RetroNpcMapping.SIR_PALOMEDES},
		{247, RetroNpcMapping.SIR_MORDRED}, {211, RetroNpcMapping.SIR_PERCIVAL}, {212, RetroNpcMapping.KING_PERCIVAL},
		{218, RetroNpcMapping.BROTHER_GALAHAD}, {249, RetroNpcMapping.MERLIN}, {248, RetroNpcMapping.MORGAN_LE_FAYE},
		{250, RetroNpcMapping.LADY_OF_THE_LAKE}, {606, RetroNpcMapping.SQUIRE}};

	/** Arthur's court is each character's own 2005 definition: parts, colors and stance. */
	@Test
	public void testRoundTableArchetypesAreTheir2005Definitions() throws Exception
	{
		assumeTrue("2005 cache not present at " + CACHE_DIR, CACHE_DIR.exists());

		Map<Integer, RetroNpcDefinition> feb = NpcMappingGenerator.decodeDefinitions(CACHE_DIR);
		for (Object[] row : ROUND_TABLE_DEFINITIONS)
		{
			RetroNpcDefinition def = feb.get((int) row[0]);
			RetroNpcData data = (RetroNpcData) row[1];
			String label = def.getId() + " " + def.getName();
			assertArrayEquals(label, def.getModels(), data.getRetroModelIds());
			assertArrayEquals(label, def.getOriginalColors(), data.getOriginalColors());
			assertArrayEquals(label, def.getReplacementColors(), data.getReplacementColors());
			assertEquals(label, def.getStanceAnimation(), data.getIdleAnimationId());
			assertEquals(label, def.getWalkAnimation(), data.getWalkAnimationId());
		}
	}

	private static void assertChathead(Map<Integer, RetroNpcDefinition> defs, int defId, RetroNpcData data)
	{
		RetroNpcDefinition def = defs.get(defId);
		assertNotNull("2005 definition " + defId + " is missing", def);
		int[] expected = def.getAdditionalModels() == null || def.getAdditionalModels().length == 0
			? null : def.getAdditionalModels();
		assertArrayEquals("chathead of the archetype built from definition " + defId + " (" + def.getName() + ")",
			expected, data.getChatheadModelIds());
	}
}
