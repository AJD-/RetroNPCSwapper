package net.runelite.client.plugins.retronpcswapper.cache;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.runelite.client.RuneLite;
import net.runelite.client.plugins.retronpcswapper.RetroNpcCategory;
import net.runelite.client.plugins.retronpcswapper.RetroNpcData;
import net.runelite.client.plugins.retronpcswapper.RetroNpcMapping;
import org.junit.Test;
import static org.junit.Assert.*;

public class RetroCacheTest
{
	public static void main(String[] args) throws Exception
	{
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
		assertEquals(net.runelite.client.plugins.retronpcswapper.RetroNpcCategory.SKELETONS, skelData.getCategory());
		assertEquals(262, skelData.getIdleAnimationId());
		assertEquals(259, skelData.getWalkAnimationId());
		assertEquals(260, skelData.getAttackAnimationId());
		assertEquals(261, skelData.getDefendAnimationId());
		assertEquals(263, skelData.getDeathAnimationId());

		RetroNpcData impData = RetroNpcMapping.get(708, "Imp");
		assertNotNull(impData);
		assertEquals(net.runelite.client.plugins.retronpcswapper.RetroNpcCategory.IMPS, impData.getCategory());
		assertEquals(171, impData.getIdleAnimationId());
		assertEquals(168, impData.getWalkAnimationId());
		assertEquals(169, impData.getAttackAnimationId());
		assertEquals(170, impData.getDefendAnimationId());
		assertEquals(172, impData.getDeathAnimationId());

		RetroNpcData goblinData = RetroNpcMapping.get(0, "Goblin");
		assertNotNull(goblinData);
		assertEquals(net.runelite.client.plugins.retronpcswapper.RetroNpcCategory.GOBLINS, goblinData.getCategory());
		assertEquals(311, goblinData.getIdleAnimationId());
		assertEquals(308, goblinData.getWalkAnimationId());
		assertEquals(309, goblinData.getAttackAnimationId());
		assertEquals(312, goblinData.getDefendAnimationId());
		assertEquals(313, goblinData.getDeathAnimationId());

		RetroNpcData guardData = RetroNpcMapping.get(0, "Guard");
		assertNotNull(guardData);
		assertEquals(net.runelite.client.plugins.retronpcswapper.RetroNpcCategory.GUARDS, guardData.getCategory());
		assertArrayEquals(new int[0], guardData.getRetroModelIds());
		assertEquals(808, guardData.getIdleAnimationId());
		assertEquals(819, guardData.getWalkAnimationId());
		assertEquals(422, guardData.getAttackAnimationId());
		assertEquals(424, guardData.getDefendAnimationId());
		assertEquals(836, guardData.getDeathAnimationId());

		RetroNpcData zombieData = RetroNpcMapping.get(0, "Zombie");
		assertNotNull(zombieData);
		assertEquals(net.runelite.client.plugins.retronpcswapper.RetroNpcCategory.ZOMBIES, zombieData.getCategory());
		assertEquals(301, zombieData.getIdleAnimationId());
		assertEquals(298, zombieData.getWalkAnimationId());
		assertEquals(299, zombieData.getAttackAnimationId());
		assertEquals(300, zombieData.getDefendAnimationId());
		assertEquals(302, zombieData.getDeathAnimationId());

		RetroNpcData ghostData = RetroNpcMapping.get(0, "Ghost");
		assertNotNull(ghostData);
		assertEquals(net.runelite.client.plugins.retronpcswapper.RetroNpcCategory.GHOSTS, ghostData.getCategory());

		RetroNpcData hillGiantData = RetroNpcMapping.get(0, "Hill giant");
		assertNotNull(hillGiantData);
		assertEquals(RetroNpcCategory.HILL_GIANTS, hillGiantData.getCategory());
		assertArrayEquals(new int[0], hillGiantData.getRetroModelIds());
		assertEquals(130, hillGiantData.getIdleAnimationId());
		assertEquals(127, hillGiantData.getWalkAnimationId());
		assertEquals(128, hillGiantData.getAttackAnimationId());
		assertEquals(129, hillGiantData.getDefendAnimationId());
		assertEquals(131, hillGiantData.getDeathAnimationId());

		RetroNpcData chickenData = RetroNpcMapping.get(0, "Chicken");
		assertNotNull(chickenData);
		assertEquals(net.runelite.client.plugins.retronpcswapper.RetroNpcCategory.CHICKENS, chickenData.getCategory());
		assertArrayEquals(new int[]{2849}, chickenData.getRetroModelIds());
		assertEquals(54, chickenData.getIdleAnimationId());
		assertEquals(53, chickenData.getWalkAnimationId());
		assertEquals(55, chickenData.getAttackAnimationId());
		assertEquals(56, chickenData.getDefendAnimationId());
		assertEquals(57, chickenData.getDeathAnimationId());
		assertEquals(-1, chickenData.getWidthScale());
		assertEquals(-1, chickenData.getHeightScale());

		// Verify Varrock Guard IDs map to GUARDS category
		assertNotNull(RetroNpcMapping.get(11903, "Guard"));
		assertEquals(net.runelite.client.plugins.retronpcswapper.RetroNpcCategory.GUARDS, Objects.requireNonNull(RetroNpcMapping.get(11903, "Guard")).getCategory());
		assertNotNull(RetroNpcMapping.get(3244, "Guard"));
		assertEquals(net.runelite.client.plugins.retronpcswapper.RetroNpcCategory.GUARDS, Objects.requireNonNull(RetroNpcMapping.get(3244, "Guard")).getCategory());

		// Verify non-guard NPCs are excluded from GUARDS category
		assertNull(RetroNpcMapping.get(0, "Guard dog"));
		assertNull(RetroNpcMapping.get(0, "Ogre guard"));
		assertNull(RetroNpcMapping.get(0, "Khazard Guard"));
		assertNull(RetroNpcMapping.get(0, "Border Guard"));
	}
}
