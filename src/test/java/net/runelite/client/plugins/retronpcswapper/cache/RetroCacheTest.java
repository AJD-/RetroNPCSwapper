package net.runelite.client.plugins.retronpcswapper.cache;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.retronpcswapper.RetroNpcData;
import net.runelite.client.plugins.retronpcswapper.RetroNpcMapping;
import net.runelite.client.plugins.retronpcswapper.RetroNpcSwapperPlugin;
import org.junit.Test;
import static org.junit.Assert.*;

public class RetroCacheTest
{
	public static void main(String[] args) throws Exception
	{
		// Plugin is automatically discovered and loaded from runelite-plugin.properties on the classpath.
		RuneLite.main(args);
	}

	@Test
	public void test317NpcDecoder() throws Exception
	{
		File cacheDir = new File("retrocache/2005cache");
		if (!cacheDir.exists()) return;

		RetroCacheReader reader = new RetroCacheReader(cacheDir);
		assertTrue(reader.init());

		byte[] archiveData = reader.readFile(0, 2);
		assertNotNull(archiveData);

		Map<String, byte[]> files = reader.readArchive(archiveData);
		byte[] npcDat = files.get(String.valueOf(RetroCacheReader.hashFileName("npc.dat")));
		byte[] npcIdx = files.get(String.valueOf(RetroCacheReader.hashFileName("npc.idx")));
		assertNotNull(npcDat);
		assertNotNull(npcIdx);

		Map<Integer, RetroNpcDefinition> defs = RetroNpcDecoder.decodeAll(npcDat, npcIdx);
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

		reader.close();

		// Load mappings and verify skeleton and imp animations
		RetroNpcMapping.loadFrom2005Cache(defs);
		RetroNpcData skelData = RetroNpcMapping.get(90, "Skeleton");
		assertNotNull(skelData);
		assertEquals(262, skelData.getIdleAnimationId());
		assertEquals(259, skelData.getWalkAnimationId());
		assertEquals(260, skelData.getAttackAnimationId());
		assertEquals(261, skelData.getDefendAnimationId());
		assertEquals(263, skelData.getDeathAnimationId());

		RetroNpcData impData = RetroNpcMapping.get(708, "Imp");
		assertNotNull(impData);
		assertEquals(171, impData.getIdleAnimationId());
		assertEquals(168, impData.getWalkAnimationId());
		assertEquals(169, impData.getAttackAnimationId());
		assertEquals(170, impData.getDefendAnimationId());
		assertEquals(172, impData.getDeathAnimationId());
	}
}
