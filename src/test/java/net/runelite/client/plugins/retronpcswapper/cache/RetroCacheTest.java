package net.runelite.client.plugins.retronpcswapper.cache;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import net.runelite.client.plugins.retronpcswapper.RetroNpcMappingEntry;
import net.runelite.client.plugins.retronpcswapper.RetroNpcSwapperPlugin;
import org.junit.Test;
import static org.junit.Assert.*;

public class RetroCacheTest
{
	private static final File CACHE_DIR = new File("retrocache/2005cache");

	@Test
	public void test317NpcDecoder() throws Exception
	{
		if (!CACHE_DIR.exists()) return;

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
		if (!CACHE_DIR.exists()) return;

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
