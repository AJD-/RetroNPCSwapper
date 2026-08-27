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
	public void testDemonsCategory()
	{
		RetroNpcData lesserDemon = RetroNpcMapping.get(82, "Lesser demon");
		assertNotNull("Lesser demon mapping must exist", lesserDemon);
		assertEquals(RetroNpcCategory.DEMONS, lesserDemon.getCategory());
		assertTrue("Lesser demon should have models", lesserDemon.getRetroModelIds().length > 0);

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
	public void testDragonsCategory()
	{
		RetroNpcData blueDragon = RetroNpcMapping.get(55, "Blue dragon");
		assertNotNull("Blue dragon mapping must exist", blueDragon);
		assertEquals(RetroNpcCategory.DRAGONS, blueDragon.getCategory());
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

		int[] impIds = {708, 709, 3080, 3081, 3082, 3083, 3084, 3085, 5007, 5008, 7067, 7068, 7069, 7070, 7071, 7072, 7924};
		for (int id : impIds)
		{
			RetroNpcData impById = RetroNpcMapping.get(id, "Imp");
			assertNotNull("Imp ID " + id + " must be mapped", impById);
			assertEquals(RetroNpcCategory.IMPS, impById.getCategory());
		}

		assertTrue(imp.isAttackAnimation(169));
		assertTrue(imp.isDefendAnimation(170));
		assertTrue(imp.isDeathAnimation(172));
		assertTrue(imp.isDeathAnimation(173));
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

		int[] unarmedIds = {70, 71, 73, 90, 91, 459, 1126};
		for (int id : unarmedIds)
		{
			assertEquals(1, RetroNpcMapping.get(id, "Skeleton").getRetroModelIds().length);
		}

		int[] armedIds = {72, 92, 93, 750, 1127, 1128};
		for (int id : armedIds)
		{
			assertEquals(2, RetroNpcMapping.get(id, "Skeleton").getRetroModelIds().length);
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
		RetroNpcData zombie = RetroNpcMapping.get(0, "Zombie");
		assertNotNull("Zombie mapping must exist", zombie);
		assertEquals(RetroNpcCategory.ZOMBIES, zombie.getCategory());
		assertEquals(301, zombie.getIdleAnimationId());
		assertEquals(298, zombie.getWalkAnimationId());
		assertEquals(299, zombie.getAttackAnimationId());
		assertEquals(303, zombie.getDefendAnimationId());
		assertEquals(302, zombie.getDeathAnimationId());

		// Test various modern zombie attack animations
		int[] attackAnims = {5568, 5571, 5573, 5576, 5577, 5578, 5581, 5583, 5585, 5590, 5593, 422, 423, 412, 299};
		for (int anim : attackAnims)
		{
			assertTrue("Zombie should match attack anim " + anim, zombie.isAttackAnimation(anim));
		}

		// Test various modern zombie defend animations
		int[] defendAnims = {5567, 5569, 5570, 5572, 5574, 5579, 5582, 5584, 5586, 5589, 5592, 5595, 424, 425, 1156, 2303, 300, 301, 303};
		for (int anim : defendAnims)
		{
			assertTrue("Zombie should match defend anim " + anim, zombie.isDefendAnimation(anim));
		}

		// Test various modern zombie death animations
		int[] deathAnims = {5575, 5580, 5587, 5588, 5591, 5594, 836, 2304, 302};
		for (int anim : deathAnims)
		{
			assertTrue("Zombie should match death anim " + anim, zombie.isDeathAnimation(anim));
		}
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
		assertEquals(RetroNpcCategory.GIANTS, hillGiant.getCategory());
		assertArrayEquals(new int[0], hillGiant.getRetroModelIds());
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
			assertEquals(RetroNpcCategory.GIANTS, giantById.getCategory());
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
			.widthScale(128)
			.heightScale(128)
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
			.widthScale(128)
			.heightScale(128)
			.modernAttackAnims(6184, 6185)
			.modernDefendAnims(6183)
			.modernDeathAnims(6182)
			.build();

		assertEquals(data1, data2);
		assertEquals(data1.hashCode(), data2.hashCode());
		assertEquals(128, data1.getWidthScale());
		assertEquals(128, data1.getHeightScale());
		assertTrue(data1.isAttackAnimation(6184));
		assertTrue(data1.isAttackAnimation(6185));
		assertTrue(data1.isDefendAnimation(6183));
		assertTrue(data1.isDeathAnimation(6182));
		assertFalse(data1.isAttackAnimation(999));
	}
}
