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
package net.runelite.client.plugins.retronpcswapper;

import java.io.File;
import java.util.Map;
import java.util.Objects;

import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.retronpcswapper.cache.RetroCacheReader;
import net.runelite.client.plugins.retronpcswapper.cache.RetroNpcDecoder;
import net.runelite.client.plugins.retronpcswapper.cache.RetroNpcDefinition;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class RetroNpcCategoryTest
{
	@BeforeClass
	public static void setUp()
	{
		File cacheDir = new File("retrocache/2005cache");








		if (cacheDir.exists())
		{
			RetroCacheReader reader = new RetroCacheReader(cacheDir);
			if (reader.init())
			{
				byte[] archiveData = reader.readFile(0, 2);
				if (archiveData != null)
				{
					Map<String, byte[]> files = reader.readArchive(archiveData);
					byte[] npcDat = files.get(String.valueOf(RetroCacheReader.hashFileName("npc.dat")));
					byte[] npcIdx = files.get(String.valueOf(RetroCacheReader.hashFileName("npc.idx")));
					if (npcDat != null && npcIdx != null)
					{
						Map<Integer, RetroNpcDefinition> defs = RetroNpcDecoder.decodeAll(npcDat, npcIdx);
						RetroNpcMapping.loadFrom2005Cache(defs);
					}
				}
				reader.close();
			}
		}
	}

	@Test
	public void testLesserDemonsCategory()
	{
		RetroNpcData lesserDemon = RetroNpcMapping.get(82, "Lesser demon");
		assertNotNull("Lesser demon mapping must exist", lesserDemon);
		assertEquals(RetroNpcCategory.LESSER_DEMONS, lesserDemon.getCategory());
		assertArrayEquals(new int[]{2943}, lesserDemon.getRetroModelIds());
		assertEquals(66, lesserDemon.getIdleAnimationId());
		assertEquals(63, lesserDemon.getWalkAnimationId());
		assertEquals(64, lesserDemon.getAttackAnimationId());
		assertEquals(65, lesserDemon.getDefendAnimationId());
		assertEquals(67, lesserDemon.getDeathAnimationId());

		int[] lesserIds = {82, 2005, 2006, 2007, 2008, 2018, 3982, 7247, 7248, 7656, 7657, 7664, 7865, 7866, 7867, 12361, 12363, 12365, 12376, 12389};
		for (int id : lesserIds)
		{
			RetroNpcData mapped = RetroNpcMapping.get(id, "Lesser demon");
			assertNotNull("Lesser demon ID " + id + " must be mapped", mapped);
			assertEquals(RetroNpcCategory.LESSER_DEMONS, mapped.getCategory());
		}

		// Verify modern attack animations
		assertTrue(lesserDemon.isAttackAnimation(5485));
		assertTrue(lesserDemon.isAttackAnimation(5486));
		assertTrue(lesserDemon.isAttackAnimation(5487));
		assertTrue(lesserDemon.isAttackAnimation(5507));
		assertTrue(lesserDemon.isAttackAnimation(5510));
		assertTrue(lesserDemon.isAttackAnimation(8002));

		// Verify modern defend animations
		assertTrue(lesserDemon.isDefendAnimation(5488));
		assertTrue(lesserDemon.isDefendAnimation(5489));
		assertTrue(lesserDemon.isDefendAnimation(5490));
		assertTrue(lesserDemon.isDefendAnimation(5508));
		assertTrue(lesserDemon.isDefendAnimation(5511));
		assertTrue(lesserDemon.isDefendAnimation(8003));

		// Verify modern death animations
		assertTrue(lesserDemon.isDeathAnimation(5491));
		assertTrue(lesserDemon.isDeathAnimation(5492));
		assertTrue(lesserDemon.isDeathAnimation(5493));
		assertTrue(lesserDemon.isDeathAnimation(5509));
		assertTrue(lesserDemon.isDeathAnimation(5512));
		assertTrue(lesserDemon.isDeathAnimation(8004));
		assertTrue(lesserDemon.isDeathAnimation(8005));

		assertFalse(lesserDemon.isAttackAnimation(99999));
	}

	@Test
	public void testGreaterDemonsCategory()
	{
		RetroNpcData greaterDemon = RetroNpcMapping.get(83, "Greater demon");
		assertNotNull("Greater demon mapping must exist", greaterDemon);
		assertEquals(RetroNpcCategory.GREATER_DEMONS, greaterDemon.getCategory());
		assertArrayEquals(new int[]{2942}, greaterDemon.getRetroModelIds());
		assertEquals(66, greaterDemon.getIdleAnimationId());
		assertEquals(63, greaterDemon.getWalkAnimationId());
		assertEquals(64, greaterDemon.getAttackAnimationId());
		assertEquals(65, greaterDemon.getDefendAnimationId());
		assertEquals(67, greaterDemon.getDeathAnimationId());

		int[] greaterIds = {83, 2025, 2026, 2027, 2028, 2029, 2030, 2031, 2032, 7244, 7245, 7246, 7871, 7872, 7873, 12387};
		for (int id : greaterIds)
		{
			RetroNpcData mapped = RetroNpcMapping.get(id, "Greater demon");
			assertNotNull("Greater demon ID " + id + " must be mapped", mapped);
			assertEquals(RetroNpcCategory.GREATER_DEMONS, mapped.getCategory());
		}

		assertTrue(greaterDemon.isAttackAnimation(5485));
		assertTrue(greaterDemon.isDefendAnimation(5488));
		assertTrue(greaterDemon.isDeathAnimation(5491));
	}

	@Test
	public void testBlackDemonsCategory()
	{
		RetroNpcData blackDemon = RetroNpcMapping.get(84, "Black demon");
		assertNotNull("Black demon mapping must exist", blackDemon);
		assertEquals(RetroNpcCategory.BLACK_DEMONS, blackDemon.getCategory());
		assertArrayEquals(new int[]{2942}, blackDemon.getRetroModelIds());
		assertEquals(66, blackDemon.getIdleAnimationId());
		assertEquals(63, blackDemon.getWalkAnimationId());
		assertEquals(64, blackDemon.getAttackAnimationId());
		assertEquals(65, blackDemon.getDefendAnimationId());
		assertEquals(67, blackDemon.getDeathAnimationId());

		int[] blackIds = {84, 240, 1432, 2048, 2049, 2050, 2051, 2052, 5874, 5875, 5876, 5877, 6295, 6357, 7242, 7243, 7874, 7875, 7876, 12385};
		for (int id : blackIds)
		{
			RetroNpcData mapped = RetroNpcMapping.get(id, "Black demon");
			assertNotNull("Black demon ID " + id + " must be mapped", mapped);
			assertEquals(RetroNpcCategory.BLACK_DEMONS, mapped.getCategory());
		}

		assertTrue(blackDemon.isAttackAnimation(5485));
		assertTrue(blackDemon.isDefendAnimation(5488));
		assertTrue(blackDemon.isDeathAnimation(5491));
	}

	@Test
	public void testDragonsCategory()
	{
		RetroNpcData blueDragon = RetroNpcMapping.get(55, "Blue dragon");
		assertNotNull("Blue dragon mapping must exist", blueDragon);
		assertEquals(RetroNpcCategory.ADULT_DRAGONS, blueDragon.getCategory());
		assertTrue("Blue dragon should have models", blueDragon.getRetroModelIds().length > 0);

		// Verify dragon animations
		assertTrue(blueDragon.isAttackAnimation(240));
		assertTrue(blueDragon.isDefendAnimation(1827));
		assertTrue(blueDragon.isDeathAnimation(1828));
		assertFalse(blueDragon.isAttackAnimation(99999));
	}

	@Test
	public void testGoblinsCategory()
	{
		RetroNpcData goblin = RetroNpcMapping.get(0, "Goblin");
		assertNotNull("Goblin mapping must exist", goblin);
		assertEquals(RetroNpcCategory.GOBLINS, goblin.getCategory());
		assertEquals(311, goblin.getIdleAnimationId());
		assertEquals(308, goblin.getWalkAnimationId());
		assertEquals(309, goblin.getAttackAnimationId());
		assertEquals(312, goblin.getDefendAnimationId());
		assertEquals(313, goblin.getDeathAnimationId());

		// Verify modern goblin attack animations
		assertTrue(goblin.isAttackAnimation(6184));
		assertTrue(goblin.isAttackAnimation(6185));
		assertTrue(goblin.isAttackAnimation(6186));
		assertTrue(goblin.isAttackAnimation(6188));
		assertTrue(goblin.isAttackAnimation(6190));
		assertTrue(goblin.isAttackAnimation(6191));
		assertTrue(goblin.isAttackAnimation(309));

		// Verify modern goblin defend animation
		assertTrue(goblin.isDefendAnimation(6183));
		assertTrue(goblin.isDefendAnimation(312));
		assertTrue(goblin.isDefendAnimation(310));

		// Verify modern goblin death animation
		assertTrue(goblin.isDeathAnimation(6182));
		assertTrue(goblin.isDeathAnimation(313));
	}

	@Test
	public void testGuardsCategory()
	{
		RetroNpcData guardByName = RetroNpcMapping.get(0, "Guard");
		assertNotNull("Guard by name must exist", guardByName);
		assertEquals(RetroNpcCategory.GUARDS, guardByName.getCategory());
		assertEquals(808, guardByName.getIdleAnimationId());
		assertEquals(819, guardByName.getWalkAnimationId());
		assertEquals(422, guardByName.getAttackAnimationId());
		assertEquals(424, guardByName.getDefendAnimationId());
		assertEquals(836, guardByName.getDeathAnimationId());

		// Verify Varrock Guard explicit ID mappings
		int[] guardIds = {3244, 3245, 11903, 11904, 11911, 11922, 11937, 11942, 11947};
		for (int id : guardIds)
		{
			RetroNpcData guardById = RetroNpcMapping.get(id, "Guard");
			assertNotNull("Guard ID " + id + " must map to data", guardById);
			assertEquals(RetroNpcCategory.GUARDS, guardById.getCategory());
		}

		// Verify modern guard animations
		assertTrue(guardByName.isAttackAnimation(422));
		assertTrue(guardByName.isAttackAnimation(423));
		assertTrue(guardByName.isAttackAnimation(451));
		assertTrue(guardByName.isAttackAnimation(7041));
		assertTrue(guardByName.isAttackAnimation(7042));
		assertTrue(guardByName.isDefendAnimation(424));
		assertTrue(guardByName.isDefendAnimation(7043));
		assertTrue(guardByName.isDeathAnimation(836));
		assertTrue(guardByName.isDeathAnimation(7044));
	}

	@Test
	public void testImpsCategory()
	{
		RetroNpcData imp = RetroNpcMapping.get(708, "Imp");
		assertNotNull("Imp mapping must exist", imp);
		assertEquals(RetroNpcCategory.IMPS, imp.getCategory());
		assertArrayEquals(new int[]{2887}, imp.getRetroModelIds());
		assertEquals(171, imp.getIdleAnimationId());
		assertEquals(168, imp.getWalkAnimationId());
		assertEquals(169, imp.getAttackAnimationId());
		assertEquals(170, imp.getDefendAnimationId());
		assertEquals(172, imp.getDeathAnimationId());

		int[] impIds = {708, 709, 3080, 3081, 3082, 3083, 3084, 3085, 3134, 5007, 5008, 5728, 7067, 7068, 7069, 7070, 7071, 7072, 7924};
		for (int id : impIds)
		{
			RetroNpcData impById = RetroNpcMapping.get(id, "Imp");
			assertNotNull("Imp ID " + id + " must be mapped", impById);
			assertEquals(RetroNpcCategory.IMPS, impById.getCategory());
		}

		// Verify modern attack animations
		int[] impAttacks = {169, 422, 423, 451, 5385, 5485, 5486, 7041, 7042};
		for (int anim : impAttacks)
		{
			assertTrue("Imp should match attack animation " + anim, imp.isAttackAnimation(anim));
		}

		// Verify modern defend animations
		int[] impDefends = {170, 424, 425, 5388, 5488, 5489, 5490, 7043};
		for (int anim : impDefends)
		{
			assertTrue("Imp should match defend animation " + anim, imp.isDefendAnimation(anim));
		}

		// Verify modern death animations
		int[] impDeaths = {172, 173, 836, 5389, 5390, 5491, 5492, 5493, 5509, 5512, 7044};
		for (int anim : impDeaths)
		{
			assertTrue("Imp should match death animation " + anim, imp.isDeathAnimation(anim));
		}

		assertFalse(imp.isAttackAnimation(99999));
	}

	@Test
	public void testSkeletonsCategory()
	{
		// Unarmed skeleton
		RetroNpcData unarmed = RetroNpcMapping.get(90, "Skeleton");
		assertNotNull("Unarmed skeleton must exist", unarmed);
		assertEquals(RetroNpcCategory.SKELETONS, unarmed.getCategory());
		assertArrayEquals(new int[]{2944}, unarmed.getRetroModelIds());
		assertEquals(262, unarmed.getIdleAnimationId());
		assertEquals(259, unarmed.getWalkAnimationId());
		assertEquals(260, unarmed.getAttackAnimationId());
		assertEquals(261, unarmed.getDefendAnimationId());
		assertEquals(263, unarmed.getDeathAnimationId());

		// Armed skeleton
		RetroNpcData armed = RetroNpcMapping.get(92, "Skeleton");
		assertNotNull("Armed skeleton must exist", armed);
		assertEquals(RetroNpcCategory.SKELETONS, armed.getCategory());
		assertArrayEquals(new int[]{2944, 2946}, armed.getRetroModelIds());

		// Verify Draynor Sewer Skeleton ID 77 maps to SKELETONS category
		RetroNpcData draynorSkel = RetroNpcMapping.get(77, "Skeleton");
		assertNotNull("Draynor Sewer Skeleton (ID 77) must exist", draynorSkel);
		assertEquals(RetroNpcCategory.SKELETONS, draynorSkel.getCategory());
		assertArrayEquals(new int[]{2944}, draynorSkel.getRetroModelIds());

		int[] unarmedIds = {70, 71, 73, 74, 77, 78, 90, 91, 459, 1126};
		for (int id : unarmedIds)
		{
			RetroNpcData skel = RetroNpcMapping.get(id, "Skeleton");
			assertNotNull("Skeleton ID " + id + " must be mapped", skel);
			assertEquals(RetroNpcCategory.SKELETONS, skel.getCategory());
			assertEquals(1, skel.getRetroModelIds().length);
		}

		int[] armedIds = {72, 75, 76, 92, 93, 750, 1127, 1128};
		for (int id : armedIds)
		{
			RetroNpcData skel = RetroNpcMapping.get(id, "Skeleton");
			assertNotNull("Armed skeleton ID " + id + " must be mapped", skel);
			assertEquals(RetroNpcCategory.SKELETONS, skel.getCategory());
			assertEquals(2, skel.getRetroModelIds().length);
		}

		// Verify modern attack animations (unarmed, weapon slash, stab, crush, modern variants)
		int[] skeletonAttacks = {260, 422, 423, 412, 451, 390, 401, 428, 440, 7041, 7042, 5485};
		for (int anim : skeletonAttacks)
		{
			assertTrue("Skeleton should match attack anim " + anim, unarmed.isAttackAnimation(anim));
			assertTrue("Armed skeleton should match attack anim " + anim, armed.isAttackAnimation(anim));
		}

		// Verify modern defend / flinch animations (block, flinch, hit, shield)
		int[] skeletonDefends = {261, 424, 425, 1156, 7043, 5488, 5489, 5490, 300, 310, 312};
		for (int anim : skeletonDefends)
		{
			assertTrue("Skeleton should match defend anim " + anim, unarmed.isDefendAnimation(anim));
			assertTrue("Armed skeleton should match defend anim " + anim, armed.isDefendAnimation(anim));
		}

		// Verify modern death animations (collapse, guard death, generic humanoid death)
		int[] skeletonDeaths = {263, 836, 7044, 5491, 5492, 5493, 5509, 5512};
		for (int anim : skeletonDeaths)
		{
			assertTrue("Skeleton should match death anim " + anim, unarmed.isDeathAnimation(anim));
			assertTrue("Armed skeleton should match death anim " + anim, armed.isDeathAnimation(anim));
		}

		// Verify name fallback for unmapped Skeleton ID
		RetroNpcData unmappedSkel = RetroNpcMapping.get(4491, "Skeleton");
		assertNotNull(unmappedSkel);
		assertEquals(RetroNpcCategory.SKELETONS, unmappedSkel.getCategory());
		assertEquals(260, unmappedSkel.getAttackAnimationId());
		assertEquals(261, unmappedSkel.getDefendAnimationId());
		assertEquals(263, unmappedSkel.getDeathAnimationId());
		assertTrue(unmappedSkel.isAttackAnimation(422));
		assertTrue(unmappedSkel.isDefendAnimation(424));
		assertTrue(unmappedSkel.isDeathAnimation(836));
	}

	@Test
	public void testZombiesCategory()
	{
		// Default / unarmed zombie (e.g. Level 13 Zombie IDs 38, 39, 40)
		RetroNpcData zombie = RetroNpcMapping.get(0, "Zombie");
		assertNotNull("Zombie mapping must exist", zombie);
		assertEquals(RetroNpcCategory.ZOMBIES, zombie.getCategory());
		assertArrayEquals(new int[]{2931}, zombie.getRetroModelIds());
		assertEquals(301, zombie.getIdleAnimationId());
		assertEquals(298, zombie.getWalkAnimationId());
		assertEquals(299, zombie.getAttackAnimationId());
		assertEquals(300, zombie.getDefendAnimationId());
		assertEquals(302, zombie.getDeathAnimationId());

		// Armed zombie with axe (e.g. Level 24 Zombie IDs 55, 56)
		RetroNpcData armedZombie = RetroNpcMapping.get(55, "Zombie");
		assertNotNull("Armed zombie mapping must exist", armedZombie);
		assertEquals(RetroNpcCategory.ZOMBIES, armedZombie.getCategory());
		assertArrayEquals(new int[]{2931, 2932}, armedZombie.getRetroModelIds());
		assertEquals(301, armedZombie.getIdleAnimationId());
		assertEquals(298, armedZombie.getWalkAnimationId());
		assertEquals(299, armedZombie.getAttackAnimationId());
		assertEquals(300, armedZombie.getDefendAnimationId());
		assertEquals(302, armedZombie.getDeathAnimationId());

		// Verify unarmed zombie explicit IDs (including Level 13 IDs 38, 39, 40)
		int[] unarmedIds = {26, 27, 28, 29, 30, 31, 32, 34, 37, 38, 39, 40, 41, 42, 43, 44, 419, 420, 421, 422, 423, 424, 1115, 1433, 1434};
		for (int id : unarmedIds)
		{
			RetroNpcData data = RetroNpcMapping.get(id, "Zombie");
			assertNotNull("Unarmed zombie ID " + id + " must be mapped", data);
			assertEquals(RetroNpcCategory.ZOMBIES, data.getCategory());
			assertEquals(1, data.getRetroModelIds().length);
			assertEquals(2931, data.getRetroModelIds()[0]);
			assertEquals(300, data.getDefendAnimationId());
		}

		// Verify armed zombie explicit IDs (including Level 24 IDs 55, 56)
		int[] armedIds = {49, 50, 51, 52, 54, 55, 56, 57, 58, 751, 1116};
		for (int id : armedIds)
		{
			RetroNpcData data = RetroNpcMapping.get(id, "Zombie");
			assertNotNull("Armed zombie ID " + id + " must be mapped", data);
			assertEquals(RetroNpcCategory.ZOMBIES, data.getCategory());
			assertEquals(2, data.getRetroModelIds().length);
			assertArrayEquals(new int[]{2931, 2932}, data.getRetroModelIds());
			assertEquals(300, data.getDefendAnimationId());
		}

		// Test various modern zombie attack animations (unarmed punches, axe chops, weapon slashes)
		int[] attackAnims = {5568, 5571, 5573, 5576, 5577, 5578, 5581, 5583, 5585, 5590, 5593, 422, 423, 412, 451, 299};
		for (int anim : attackAnims)
		{
			assertTrue("Zombie should match attack anim " + anim, zombie.isAttackAnimation(anim));
			assertTrue("Armed zombie should match attack anim " + anim, armedZombie.isAttackAnimation(anim));
			assertFalse("Attack anim " + anim + " must not be defend", zombie.isDefendAnimation(anim));
			assertFalse("Attack anim " + anim + " must not be death", zombie.isDeathAnimation(anim));
		}

		// Test various modern zombie defend animations (flinches, blocks)
		int[] defendAnims = {5567, 5570, 5574, 5579, 5582, 5584, 5586, 5589, 5592, 424, 425, 1156, 2303, 300};
		for (int anim : defendAnims)
		{
			assertTrue("Zombie should match defend anim " + anim, zombie.isDefendAnimation(anim));
			assertTrue("Armed zombie should match defend anim " + anim, armedZombie.isDefendAnimation(anim));
			assertFalse("Defend anim " + anim + " must not be death", zombie.isDeathAnimation(anim));
		}

		// Verify idle (301) and mammoth walk (303) are not defend animations
		assertFalse(zombie.isDefendAnimation(301));
		assertFalse(zombie.isDefendAnimation(303));
		assertFalse(armedZombie.isDefendAnimation(301));
		assertFalse(armedZombie.isDefendAnimation(303));

		// Test various modern zombie death animations (including variant deaths 5569, 5572, 5575, 5580, 5587, 5588, 5591, 5594, 5595)
		int[] deathAnims = {5569, 5572, 5575, 5580, 5587, 5588, 5591, 5594, 5595, 836, 2304, 302};
		for (int anim : deathAnims)
		{
			assertTrue("Zombie should match death anim " + anim, zombie.isDeathAnimation(anim));
			assertTrue("Armed zombie should match death anim " + anim, armedZombie.isDeathAnimation(anim));
			assertFalse("Death anim " + anim + " must not be defend", zombie.isDefendAnimation(anim));
		}

		// Verify name fallback for unmapped Zombie ID
		RetroNpcData unmappedZombie = RetroNpcMapping.get(99998, "Zombie");
		assertNotNull(unmappedZombie);
		assertEquals(RetroNpcCategory.ZOMBIES, unmappedZombie.getCategory());
		assertEquals(299, unmappedZombie.getAttackAnimationId());
		assertEquals(300, unmappedZombie.getDefendAnimationId());
		assertEquals(302, unmappedZombie.getDeathAnimationId());
		assertTrue(unmappedZombie.isAttackAnimation(5568));
		assertTrue(unmappedZombie.isDefendAnimation(300));
		assertTrue(unmappedZombie.isDeathAnimation(5575));
		assertTrue(unmappedZombie.isDeathAnimation(5569));
		assertTrue(unmappedZombie.isDeathAnimation(5572));
		assertTrue(unmappedZombie.isDeathAnimation(5595));
	}

	@Test
	public void testGhostsCategory()
	{
		RetroNpcData ghost = RetroNpcMapping.get(0, "Ghost");
		assertNotNull("Ghost mapping must exist", ghost);
		assertEquals(RetroNpcCategory.GHOSTS, ghost.getCategory());
	}

	@Test
	public void testGiantsCategory()
	{
		RetroNpcData hillGiant = RetroNpcMapping.get(0, "Hill giant");
		assertNotNull("Hill giant mapping must exist", hillGiant);
		assertEquals(RetroNpcCategory.HILL_GIANTS, hillGiant.getCategory());
		assertArrayEquals(new int[]{2870, 2866}, hillGiant.getRetroModelIds());
		assertEquals(130, hillGiant.getIdleAnimationId());
		assertEquals(127, hillGiant.getWalkAnimationId());
		assertEquals(128, hillGiant.getAttackAnimationId());
		assertEquals(129, hillGiant.getDefendAnimationId());
		assertEquals(131, hillGiant.getDeathAnimationId());

		int[] hillGiantIds = {117, 2098, 2099, 2100, 2101, 2102, 2103, 3144, 7261, 7262};
		for (int id : hillGiantIds)
		{
			RetroNpcData giantById = RetroNpcMapping.get(id, "Hill giant");
			assertNotNull("Hill giant ID " + id + " must exist", giantById);
			assertEquals(RetroNpcCategory.HILL_GIANTS, giantById.getCategory());
		}

		// Test modern giant animations
		assertTrue(hillGiant.isAttackAnimation(4652));
		assertTrue(hillGiant.isAttackAnimation(4658));
		assertTrue(hillGiant.isAttackAnimation(7002));
		assertTrue(hillGiant.isDefendAnimation(4651));
		assertTrue(hillGiant.isDefendAnimation(4657));
		assertTrue(hillGiant.isDefendAnimation(7001));
		assertTrue(hillGiant.isDeathAnimation(4653));
		assertTrue(hillGiant.isDeathAnimation(4659));
		assertTrue(hillGiant.isDeathAnimation(7004));
	}

	@Test
	public void testChickensCategory()
	{
		RetroNpcData chicken = RetroNpcMapping.get(0, "Chicken");
		assertNotNull("Chicken mapping must exist", chicken);
		assertEquals(RetroNpcCategory.CHICKENS, chicken.getCategory());
		assertArrayEquals(new int[]{2849}, chicken.getRetroModelIds());
		assertEquals(54, chicken.getIdleAnimationId());
		assertEquals(53, chicken.getWalkAnimationId());
		assertEquals(55, chicken.getAttackAnimationId());
		assertEquals(56, chicken.getDefendAnimationId());
		assertEquals(57, chicken.getDeathAnimationId());

		assertTrue(chicken.isAttackAnimation(5385));
		assertTrue(chicken.isAttackAnimation(5387));
		assertTrue(chicken.isDefendAnimation(5388));
		assertTrue(chicken.isDeathAnimation(5389));
		assertTrue(chicken.isDeathAnimation(5390));
	}

	@Test
	public void testCategoryMatchingExclusions()
	{
		assertNull(RetroNpcMapping.get(0, "Guard dog"));
		assertNull(RetroNpcMapping.get(0, "Ogre guard"));
		assertNull(RetroNpcMapping.get(0, "Khazard Guard"));
		assertNull(RetroNpcMapping.get(0, "Border Guard"));
		assertNull(RetroNpcMapping.get(0, "Baby impling"));
		assertNull(RetroNpcMapping.get(0, "Dragon impling"));
		assertNull(RetroNpcMapping.get(0, "Lucky impling"));
		assertNull(RetroNpcMapping.get(0, "Impaler deer"));
	}

	@Test
	public void testDraynorSewerSkeletonRegression()
	{
		// Draynor Sewer Skeleton is ID 77 in-game
		RetroNpcData draynorSkel = RetroNpcMapping.get(77, "Skeleton");
		assertNotNull("Draynor Sewer Skeleton (ID 77) must exist in mapping", draynorSkel);
		assertEquals("Draynor Sewer Skeleton (ID 77) must be SKELETONS category", RetroNpcCategory.SKELETONS, draynorSkel.getCategory());
		assertArrayEquals("Draynor Sewer Skeleton must use unarmed skeleton retro model [2944]", new int[]{2944}, draynorSkel.getRetroModelIds());
		assertEquals("Draynor Sewer Skeleton idle animation must be 262", 262, draynorSkel.getIdleAnimationId());
		assertEquals("Draynor Sewer Skeleton walk animation must be 259", 259, draynorSkel.getWalkAnimationId());
		assertEquals("Draynor Sewer Skeleton attack animation must be 260", 260, draynorSkel.getAttackAnimationId());
		assertEquals("Draynor Sewer Skeleton defend animation must be 261", 261, draynorSkel.getDefendAnimationId());
		assertEquals("Draynor Sewer Skeleton death animation must be 263", 263, draynorSkel.getDeathAnimationId());

		// Verify entire range of sewer / wilderness skeleton IDs 70-78
		int[] skeletonRange = {70, 71, 72, 73, 74, 75, 76, 77, 78};
		for (int id : skeletonRange)
		{
			RetroNpcData skel = RetroNpcMapping.get(id, "Skeleton");
			assertNotNull("Skeleton ID " + id + " must be mapped", skel);
			assertEquals("Skeleton ID " + id + " must be SKELETONS category", RetroNpcCategory.SKELETONS, skel.getCategory());
			assertNotEquals("Skeleton ID " + id + " must not use zombie model", 2931, skel.getRetroModelIds()[0]);
		}
	}

	@Test
	public void testZombieDeathVsFlinchAnimations()
	{
		// Level 13 Zombies (IDs 38, 39, 40) - Unarmed variant
		int[] level13Ids = {38, 39, 40};
		for (int id : level13Ids)
		{
			RetroNpcData z = RetroNpcMapping.get(id, "Zombie");
			assertNotNull("Zombie ID " + id + " must be mapped", z);
			assertEquals(RetroNpcCategory.ZOMBIES, z.getCategory());
			assertArrayEquals("Level 13 Zombie " + id + " must use unarmed zombie model [2931]", new int[]{2931}, z.getRetroModelIds());
			assertEquals("Flinch animation must be sequence 300", 300, z.getDefendAnimationId());
			assertEquals("Death animation must be sequence 302", 302, z.getDeathAnimationId());
		}

		// Level 24 Zombies (IDs 55, 56) - Armed axe variant
		int[] level24Ids = {55, 56};
		for (int id : level24Ids)
		{
			RetroNpcData z = RetroNpcMapping.get(id, "Zombie");
			assertNotNull("Zombie ID " + id + " must be mapped", z);
			assertEquals(RetroNpcCategory.ZOMBIES, z.getCategory());
			assertArrayEquals("Level 24 Zombie " + id + " must use armed zombie models [2931, 2932]", new int[]{2931, 2932}, z.getRetroModelIds());
			assertEquals("Flinch animation must be sequence 300", 300, z.getDefendAnimationId());
			assertEquals("Death animation must be sequence 302", 302, z.getDeathAnimationId());
		}

		RetroNpcData sampleZombie = RetroNpcMapping.get(38, "Zombie");

		// Modern variant death animations MUST match isDeathAnimation and MUST NEVER match isDefendAnimation (which caused the flinch-on-death bug)
		int[] deathAnims = {5569, 5572, 5575, 5580, 5587, 5588, 5591, 5594, 5595, 836, 2304, 302};
		for (int death : deathAnims)
		{
			assertTrue("Sequence " + death + " must match isDeathAnimation", Objects.requireNonNull(sampleZombie).isDeathAnimation(death));
			assertFalse("Sequence " + death + " must NOT match isDefendAnimation (prevents flinch-on-death regression)", sampleZombie.isDefendAnimation(death));
			assertFalse("Sequence " + death + " must NOT match isAttackAnimation", sampleZombie.isAttackAnimation(death));
		}

		// Modern variant defend animations MUST match isDefendAnimation and MUST NEVER match isDeathAnimation
		int[] defendAnims = {5567, 5570, 5574, 5579, 5582, 5584, 5586, 5589, 5592, 424, 425, 1156, 2303, 300};
		for (int defend : defendAnims)
		{
			assertTrue("Sequence " + defend + " must match isDefendAnimation", sampleZombie.isDefendAnimation(defend));
			assertFalse("Sequence " + defend + " must NOT match isDeathAnimation", sampleZombie.isDeathAnimation(defend));
			assertFalse("Sequence " + defend + " must NOT match isAttackAnimation", sampleZombie.isAttackAnimation(defend));
		}

		// Modern variant attack animations MUST match isAttackAnimation and MUST NEVER match isDefendAnimation or isDeathAnimation
		int[] attackAnims = {5568, 5571, 5573, 5576, 5577, 5578, 5581, 5583, 5585, 5590, 5593, 422, 423, 412, 451, 299};
		for (int attack : attackAnims)
		{
			assertTrue("Sequence " + attack + " must match isAttackAnimation", sampleZombie.isAttackAnimation(attack));
			assertFalse("Sequence " + attack + " must NOT match isDefendAnimation", sampleZombie.isDefendAnimation(attack));
			assertFalse("Sequence " + attack + " must NOT match isDeathAnimation", sampleZombie.isDeathAnimation(attack));
		}

		// Idle (301) and mammoth walk (303) MUST NOT match any combat state
		assertFalse(sampleZombie.isAttackAnimation(301));
		assertFalse(sampleZombie.isDefendAnimation(301));
		assertFalse(sampleZombie.isDeathAnimation(301));
		assertFalse(sampleZombie.isAttackAnimation(303));
		assertFalse(sampleZombie.isDefendAnimation(303));
		assertFalse(sampleZombie.isDeathAnimation(303));
	}

	@Test
	public void testCategoryIntegrityAcrossSharedIds()
	{
		// ID 55 is a modern Zombie ID, but also Blue Dragon in 2005 cache
		RetroNpcData dragon = RetroNpcMapping.get(55, "Blue dragon");
		assertNotNull(dragon);
		assertEquals(RetroNpcCategory.ADULT_DRAGONS, dragon.getCategory());

		RetroNpcData zombie = RetroNpcMapping.get(55, "Zombie");
		assertNotNull(zombie);
		assertEquals(RetroNpcCategory.ZOMBIES, zombie.getCategory());

		// ID 77 is a modern Skeleton ID
		RetroNpcData skel = RetroNpcMapping.get(77, "Skeleton");
		assertNotNull(skel);
		assertEquals(RetroNpcCategory.SKELETONS, skel.getCategory());

		// Non-matching NPC names with ID collisions must not match swapper
		assertNull(RetroNpcMapping.get(55, "Guard dog"));
		assertNull(RetroNpcMapping.get(77, "Ogre guard"));
	}

	@Test
	public void testBuilderAndEquals()
	{
		RetroNpcData data1 = RetroNpcData.builder()
			.category(RetroNpcCategory.GOBLINS)
			.retroModelIds(new int[]{100, 101})
			.idleAnimationId(311)
			.walkAnimationId(308)
			.attackAnimationId(309)
			.defendAnimationId(312)
			.deathAnimationId(313)
			.modernAttackAnims(6184, 6185)
			.modernDefendAnims(6183)
			.modernDeathAnims(6182)
			.build();

		RetroNpcData data2 = RetroNpcData.builder()
			.category(RetroNpcCategory.GOBLINS)
			.retroModelIds(new int[]{100, 101})
			.idleAnimationId(311)
			.walkAnimationId(308)
			.attackAnimationId(309)
			.defendAnimationId(312)
			.deathAnimationId(313)
			.modernAttackAnims(6184, 6185)
			.modernDefendAnims(6183)
			.modernDeathAnims(6182)
			.build();

		assertEquals(data1, data2);
		assertEquals(data1.hashCode(), data2.hashCode());
		assertTrue(data1.isAttackAnimation(6184));
		assertTrue(data1.isAttackAnimation(6185));
		assertTrue(data1.isDefendAnimation(6183));
		assertTrue(data1.isDeathAnimation(6182));
		assertFalse(data1.isAttackAnimation(999));
	}

	@Test
	public void testSafetyConfigDefaultsAndAnnotations() throws Exception
	{
		// Test config defaults
		RetroNpcConfig config = new RetroNpcConfig() {};
		assertTrue("disablePvpWorld must default to true", config.disablePvpWorld());
		assertTrue("disableWilderness must default to true", config.disableWilderness());

		// Test annotations on disablePvpWorld
		var pvpMethod = RetroNpcConfig.class.getMethod("disablePvpWorld");
		var pvpItem = pvpMethod.getAnnotation(net.runelite.client.config.ConfigItem.class);
		assertNotNull(pvpItem);
		assertEquals("disablePvpWorld", pvpItem.keyName());
		assertEquals("Disable on PvP worlds", pvpItem.name());
		assertEquals("safetySection", pvpItem.section());

		// Test annotations on disableWilderness
		var wildyMethod = RetroNpcConfig.class.getMethod("disableWilderness");
		var wildyItem = wildyMethod.getAnnotation(net.runelite.client.config.ConfigItem.class);
		assertNotNull(wildyItem);
		assertEquals("disableWilderness", wildyItem.keyName());
		assertEquals("Disable in Wilderness", wildyItem.name());
		assertEquals("safetySection", wildyItem.section());

		// Test swapGuards defaults to false (disabled/unchecked)
		assertFalse("swapGuards must default to false (disabled/unchecked)", config.swapGuards());
		var guardMethod = RetroNpcConfig.class.getMethod("swapGuards");
		var guardItem = guardMethod.getAnnotation(net.runelite.client.config.ConfigItem.class);
		assertNotNull(guardItem);
		assertEquals("swapGuards", guardItem.keyName());
		assertEquals("Guards (Disabled)", guardItem.name());
		assertEquals("miscSection", guardItem.section());
		assertEquals(7, guardItem.position());
		assertTrue("swapGuards description must contain Disabled", guardItem.description().contains("Disabled"));
		assertTrue("swapGuards warning must be present", guardItem.warning().contains("disabled"));

		// Test swapImps defaults to false (disabled/unchecked) with explanation tooltip and warning
		assertFalse("swapImps must default to false (disabled/unchecked)", config.swapImps());
		var impMethod = RetroNpcConfig.class.getMethod("swapImps");
		var impItem = impMethod.getAnnotation(net.runelite.client.config.ConfigItem.class);
		assertNotNull(impItem);
		assertEquals("swapImps", impItem.keyName());
		assertEquals("Imps (Disabled)", impItem.name());
		assertEquals("miscSection", impItem.section());
		assertEquals(8, impItem.position());
		assertTrue("swapImps description must contain explanation", impItem.description().contains("Disabled"));
		assertTrue("swapImps description must mention sequence IDs 168-172", impItem.description().contains("168-172"));
		assertTrue("swapImps warning must be present", impItem.warning().contains("disabled"));

		// Test Demon config items (Lesser, Greater, Black) - all disabled by default
		assertFalse("swapLesserDemons must default to false (disabled/unchecked)", config.swapLesserDemons());
		var lesserMethod = RetroNpcConfig.class.getMethod("swapLesserDemons");
		var lesserItem = lesserMethod.getAnnotation(net.runelite.client.config.ConfigItem.class);
		assertNotNull(lesserItem);
		assertEquals("swapLesserDemons", lesserItem.keyName());
		assertEquals("Lesser Demons (Disabled)", lesserItem.name());
		assertEquals("demonSection", lesserItem.section());
		assertEquals(1, lesserItem.position());
		assertTrue("swapLesserDemons description must mention Disabled", lesserItem.description().contains("Disabled"));
		assertTrue("swapLesserDemons warning must be present", lesserItem.warning().contains("disabled"));

		assertFalse("swapGreaterDemons must default to false (disabled/unchecked)", config.swapGreaterDemons());
		var greaterMethod = RetroNpcConfig.class.getMethod("swapGreaterDemons");
		var greaterItem = greaterMethod.getAnnotation(net.runelite.client.config.ConfigItem.class);
		assertNotNull(greaterItem);
		assertEquals("swapGreaterDemons", greaterItem.keyName());
		assertEquals("Greater Demons (Disabled)", greaterItem.name());
		assertEquals("demonSection", greaterItem.section());
		assertEquals(2, greaterItem.position());
		assertTrue("swapGreaterDemons description must mention Disabled", greaterItem.description().contains("Disabled"));
		assertTrue("swapGreaterDemons warning must be present", greaterItem.warning().contains("disabled"));

		assertFalse("swapBlackDemons must default to false (disabled/unchecked)", config.swapBlackDemons());
		var blackMethod = RetroNpcConfig.class.getMethod("swapBlackDemons");
		var blackItem = blackMethod.getAnnotation(net.runelite.client.config.ConfigItem.class);
		assertNotNull(blackItem);
		assertEquals("swapBlackDemons", blackItem.keyName());
		assertEquals("Black Demons (Disabled)", blackItem.name());
		assertEquals("demonSection", blackItem.section());
		assertEquals(3, blackItem.position());
		assertTrue("swapBlackDemons description must mention Disabled", blackItem.description().contains("Disabled"));
		assertTrue("swapBlackDemons warning must be present", blackItem.warning().contains("disabled"));

		// Test Dragon config items (Adult Dragons, Baby Dragons) - disabled by default
		assertFalse("swapAdultDragons must default to false (disabled/unchecked)", config.swapAdultDragons());
		var dragonMethod = RetroNpcConfig.class.getMethod("swapAdultDragons");
		var dragonItem = dragonMethod.getAnnotation(net.runelite.client.config.ConfigItem.class);
		assertNotNull(dragonItem);
		assertEquals("swapAdultDragons", dragonItem.keyName());
		assertEquals("Adult Dragons (Disabled)", dragonItem.name());
		assertEquals("dragonSection", dragonItem.section());
		assertEquals(1, dragonItem.position());
		assertTrue("swapAdultDragons description must mention Disabled", dragonItem.description().contains("Disabled"));
		assertTrue("swapAdultDragons warning must be present", dragonItem.warning().contains("disabled"));

		assertFalse("swapBabyDragons must default to false (disabled/unchecked)", config.swapBabyDragons());
		var babyDragonMethod = RetroNpcConfig.class.getMethod("swapBabyDragons");
		var babyDragonItem = babyDragonMethod.getAnnotation(net.runelite.client.config.ConfigItem.class);
		assertNotNull(babyDragonItem);
		assertEquals("swapBabyDragons", babyDragonItem.keyName());
		assertEquals("Baby Dragons (Disabled)", babyDragonItem.name());
		assertEquals("dragonSection", babyDragonItem.section());
		assertEquals(2, babyDragonItem.position());
		assertTrue("swapBabyDragons description must mention Disabled", babyDragonItem.description().contains("Disabled"));
		assertTrue("swapBabyDragons warning must be present", babyDragonItem.warning().contains("disabled"));

		// Test Moss Giants - disabled by default
		assertFalse("swapMossGiants must default to false (disabled/unchecked)", config.swapMossGiants());
		var mossMethod = RetroNpcConfig.class.getMethod("swapMossGiants");
		var mossItem = mossMethod.getAnnotation(net.runelite.client.config.ConfigItem.class);
		assertNotNull(mossItem);
		assertEquals("swapMossGiants", mossItem.keyName());
		assertEquals("Moss Giants (Disabled)", mossItem.name());
		assertEquals("miscSection", mossItem.section());
		assertEquals(9, mossItem.position());
		assertTrue("swapMossGiants description must mention Disabled", mossItem.description().contains("Disabled"));
		assertTrue("swapMossGiants warning must be present", mossItem.warning().contains("disabled"));

		// Test Fire Giants - disabled by default
		assertFalse("swapFireGiants must default to false (disabled/unchecked)", config.swapFireGiants());
		var fireMethod = RetroNpcConfig.class.getMethod("swapFireGiants");
		var fireItem = fireMethod.getAnnotation(net.runelite.client.config.ConfigItem.class);
		assertNotNull(fireItem);
		assertEquals("swapFireGiants", fireItem.keyName());
		assertEquals("Fire Giants (Disabled)", fireItem.name());
		assertEquals("miscSection", fireItem.section());
		assertEquals(10, fireItem.position());
		assertTrue("swapFireGiants description must mention Disabled", fireItem.description().contains("Disabled"));
		assertTrue("swapFireGiants warning must be present", fireItem.warning().contains("disabled"));

		// Test Hill Giants - enabled by default
		assertTrue("swapHillGiants must default to true", config.swapHillGiants());
		var hillGiantMethod = RetroNpcConfig.class.getMethod("swapHillGiants");
		var hillGiantItem = hillGiantMethod.getAnnotation(net.runelite.client.config.ConfigItem.class);
		assertNotNull(hillGiantItem);
		assertEquals("swapHillGiants", hillGiantItem.keyName());
		assertEquals("Hill Giants", hillGiantItem.name());
		assertEquals(5, hillGiantItem.position());

		// Test Ghosts - disabled by default
		assertFalse("swapGhosts must default to false (disabled/unchecked)", config.swapGhosts());
		var ghostMethod = RetroNpcConfig.class.getMethod("swapGhosts");
		var ghostItem = ghostMethod.getAnnotation(net.runelite.client.config.ConfigItem.class);
		assertNotNull(ghostItem);
		assertEquals("swapGhosts", ghostItem.keyName());
		assertEquals("Ghosts (Disabled)", ghostItem.name());
		assertEquals("miscSection", ghostItem.section());
		assertEquals(6, ghostItem.position());
		assertTrue("swapGhosts description must mention Disabled", ghostItem.description().contains("Disabled"));
		assertTrue("swapGhosts warning must be present", ghostItem.warning().contains("disabled"));

		// Test Section positions
		var miscSectionField = RetroNpcConfig.class.getField("miscSection");
		var miscSectionAnn = miscSectionField.getAnnotation(net.runelite.client.config.ConfigSection.class);
		assertNotNull(miscSectionAnn);
		assertEquals(0, miscSectionAnn.position());

		var dragonSectionField = RetroNpcConfig.class.getField("dragonSection");
		var dragonSectionAnn = dragonSectionField.getAnnotation(net.runelite.client.config.ConfigSection.class);
		assertNotNull(dragonSectionAnn);
		assertEquals(1, dragonSectionAnn.position());

		var demonSectionField = RetroNpcConfig.class.getField("demonSection");
		var demonSectionAnn = demonSectionField.getAnnotation(net.runelite.client.config.ConfigSection.class);
		assertNotNull(demonSectionAnn);
		assertEquals(2, demonSectionAnn.position());

		var safetySectionField = RetroNpcConfig.class.getField("safetySection");
		var safetySectionAnn = safetySectionField.getAnnotation(net.runelite.client.config.ConfigSection.class);
		assertNotNull(safetySectionAnn);
		assertEquals(3, safetySectionAnn.position());
	}

	@Test
	public void testSafetyPvpAndWildernessDetection()
	{
		// Verify WorldType.isPvpWorld detection for various world types
		assertTrue(net.runelite.api.WorldType.isPvpWorld(java.util.EnumSet.of(net.runelite.api.WorldType.PVP)));
		assertTrue(net.runelite.api.WorldType.isPvpWorld(java.util.EnumSet.of(net.runelite.api.WorldType.DEADMAN)));
		assertFalse(net.runelite.api.WorldType.isPvpWorld(java.util.EnumSet.of(net.runelite.api.WorldType.MEMBERS)));
		assertFalse(net.runelite.api.WorldType.isPvpWorld(java.util.EnumSet.noneOf(net.runelite.api.WorldType.class)));

		// Verify Wilderness Varbit constant
		assertEquals(5963, VarbitID.INSIDE_WILDERNESS);
	}
}
