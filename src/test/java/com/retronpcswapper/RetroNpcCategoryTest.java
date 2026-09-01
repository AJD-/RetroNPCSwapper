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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarbitID;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class RetroNpcCategoryTest
{
	@BeforeClass
	public static void setUp() throws Exception
	{
		// Load mappings from the bundled JSON resource, exactly as the plugin does
		// at startup - no local 2005 cache needed to run this suite.
		try (InputStream in = RetroNpcSwapperPlugin.class.getResourceAsStream("npc-mappings.json"))
		{
			assertNotNull("npc-mappings.json resource missing - run ./gradlew generateNpcMappings", in);
			List<RetroNpcMappingEntry> entries = new Gson().fromJson(
				new InputStreamReader(in, StandardCharsets.UTF_8),
				new TypeToken<List<RetroNpcMappingEntry>>() {}.getType());
			RetroNpcMapping.load(entries);
		}
	}

	@Test
	public void testLesserDemonsCategory()
	{
		RetroNpcData lesserDemon = RetroNpcMapping.get(NpcID.LESSER_DEMON, "Lesser demon");
		assertNotNull("Lesser demon mapping must exist", lesserDemon);
		assertEquals(RetroNpcCategory.LESSER_DEMONS, lesserDemon.getCategory());
		assertArrayEquals(new int[]{2943}, lesserDemon.getRetroModelIds());
		assertEquals(66, lesserDemon.getIdleAnimationId());
		assertEquals(63, lesserDemon.getWalkAnimationId());
		assertEquals(64, lesserDemon.getAttackAnimationId());
		assertEquals(65, lesserDemon.getDefendAnimationId());
		assertEquals(67, lesserDemon.getDeathAnimationId());

		int[] lesserIds = {
			NpcID.LESSER_DEMON, NpcID.LESSER_DEMON2, NpcID.LESSER_DEMON3, NpcID.LESSER_DEMON4, NpcID.LESSER_DEMON5,
			NpcID.DRAGONSLAYER_DEMON, NpcID.KOUREND_LESSER_DEMON1, NpcID.KOUREND_LESSER_DEMON2,
			NpcID.LESSER_DEMON_SLAYERCAVE_1, NpcID.LESSER_DEMON_SLAYERCAVE_2, NpcID.LESSER_DEMON_SLAYERCAVE_3,
			NpcID.WILD_CAVE_LESSER_DEMON, NpcID.WILD_CAVE_LESSER_DEMON2, NpcID.WILD_CAVE_LESSER_DEMON3,
			NpcID.DT2_SCAR_MAZE_MAGE_DEMON_NORMAL, NpcID.DT2_SCAR_MAZE_MELEE_DEMON_NORMAL,
			NpcID.DT2_SCAR_MAZE_RANGED_DEMON_NORMAL, NpcID.DT2_SCAR_LESSER_DEMON_1};
		for (int id : lesserIds)
		{
			RetroNpcData mapped = RetroNpcMapping.get(id, "Lesser demon");
			assertNotNull("Lesser demon ID " + id + " must be mapped", mapped);
			assertEquals(RetroNpcCategory.LESSER_DEMONS, mapped.getCategory());
		}

		// Verify modern attack animations (64/1528 classic, 4644 greater, 4678-4680 demon-update)
		assertTrue(lesserDemon.isAttackAnimation(64));
		assertTrue(lesserDemon.isAttackAnimation(1528));
		assertTrue(lesserDemon.isAttackAnimation(4644));
		assertTrue(lesserDemon.isAttackAnimation(4678));
		assertTrue(lesserDemon.isAttackAnimation(4679));
		assertTrue(lesserDemon.isAttackAnimation(4680));

		// Verify modern defend animations
		assertTrue(lesserDemon.isDefendAnimation(65));
		assertTrue(lesserDemon.isDefendAnimation(4676));

		// Verify modern death animations
		assertTrue(lesserDemon.isDeathAnimation(67));
		assertTrue(lesserDemon.isDeathAnimation(68));
		assertTrue(lesserDemon.isDeathAnimation(1530));

		// Walk/ready/casting sequences (63, 66, 69, 1526, 1527) must not be
		// intercepted, and skeleton-update anims (5485/5489/5491) no longer
		// bleed into the demon sets after the gameval re-curation
		assertFalse(lesserDemon.isAttackAnimation(63));
		assertFalse(lesserDemon.isAttackAnimation(66));
		assertFalse(lesserDemon.isAttackAnimation(69));
		assertFalse(lesserDemon.isAttackAnimation(1527));
		assertFalse(lesserDemon.isDefendAnimation(1526));
		assertFalse(lesserDemon.isAttackAnimation(5485));
		assertFalse(lesserDemon.isDefendAnimation(5489));
		assertFalse(lesserDemon.isDeathAnimation(5491));
		assertFalse(lesserDemon.isAttackAnimation(99999));
	}

	@Test
	public void testGreaterDemonsCategory()
	{
		RetroNpcData greaterDemon = RetroNpcMapping.get(NpcID.GREATER_DEMON, "Greater demon");
		assertNotNull("Greater demon mapping must exist", greaterDemon);
		assertEquals(RetroNpcCategory.GREATER_DEMONS, greaterDemon.getCategory());
		assertArrayEquals(new int[]{2942}, greaterDemon.getRetroModelIds());
		assertEquals(66, greaterDemon.getIdleAnimationId());
		assertEquals(63, greaterDemon.getWalkAnimationId());
		assertEquals(64, greaterDemon.getAttackAnimationId());
		assertEquals(65, greaterDemon.getDefendAnimationId());
		assertEquals(67, greaterDemon.getDeathAnimationId());

		int[] greaterIds = {
			NpcID.GREATER_DEMON, NpcID.GREATER_DEMON2, NpcID.GREATER_DEMON3, NpcID.GREATER_DEMON4, NpcID.GREATER_DEMON5,
			NpcID.GREATER_DEMON_STRONGHOLDCAVE_1, NpcID.GREATER_DEMON_STRONGHOLDCAVE_2, NpcID.GREATER_DEMON_STRONGHOLDCAVE_3,
			NpcID.KOUREND_GREATER_DEMON1, NpcID.KOUREND_GREATER_DEMON2, NpcID.KOUREND_GREATER_DEMON3,
			NpcID.WILD_CAVE_GREATER_DEMON, NpcID.WILD_CAVE_GREATER_DEMON2, NpcID.WILD_CAVE_GREATER_DEMON3,
			NpcID.DT2_SCAR_GREATER_DEMON_1};
		for (int id : greaterIds)
		{
			RetroNpcData mapped = RetroNpcMapping.get(id, "Greater demon");
			assertNotNull("Greater demon ID " + id + " must be mapped", mapped);
			assertEquals(RetroNpcCategory.GREATER_DEMONS, mapped.getCategory());
		}

		assertTrue(greaterDemon.isAttackAnimation(4679));
		assertTrue(greaterDemon.isDefendAnimation(4676));
		assertTrue(greaterDemon.isDeathAnimation(68));
	}

	@Test
	public void testBlackDemonsCategory()
	{
		RetroNpcData blackDemon = RetroNpcMapping.get(NpcID.BLACK_DEMON, "Black demon");
		assertNotNull("Black demon mapping must exist", blackDemon);
		assertEquals(RetroNpcCategory.BLACK_DEMONS, blackDemon.getCategory());
		assertArrayEquals(new int[]{2942}, blackDemon.getRetroModelIds());
		assertEquals(66, blackDemon.getIdleAnimationId());
		assertEquals(63, blackDemon.getWalkAnimationId());
		assertEquals(64, blackDemon.getAttackAnimationId());
		assertEquals(65, blackDemon.getDefendAnimationId());
		assertEquals(67, blackDemon.getDeathAnimationId());

		int[] blackIds = {
			NpcID.BLACK_DEMON, NpcID.BLACK_DEMON2, NpcID.BLACK_DEMON3, NpcID.BLACK_DEMON4, NpcID.BLACK_DEMON5,
			NpcID.BLACK_DEMON_STRONGHOLDCAVE_1, NpcID.BLACK_DEMON_STRONGHOLDCAVE_2, NpcID.BLACK_DEMON_STRONGHOLDCAVE_3,
			NpcID.BLACK_DEMON_STRONGHOLDCAVE_4, NpcID.BLACK_DEMON_STRONGHOLDCAVE_5,
			NpcID.GRANDTREE_BLACKDEMON, NpcID.NZONE_GRANDTREE_BLACKDEMON_HARD, NpcID.NZONE_GRANDTREE_BLACKDEMON_NORMAL,
			NpcID.KOUREND_BLACK_DEMON_1, NpcID.KOUREND_BLACK_DEMON_2,
			NpcID.WILD_CAVE_BLACK_DEMON, NpcID.WILD_CAVE_BLACK_DEMON2, NpcID.WILD_CAVE_BLACK_DEMON3,
			NpcID.DT2_SCAR_BLACK_DEMON_1};
		for (int id : blackIds)
		{
			RetroNpcData mapped = RetroNpcMapping.get(id, "Black demon");
			assertNotNull("Black demon ID " + id + " must be mapped", mapped);
			assertEquals(RetroNpcCategory.BLACK_DEMONS, mapped.getCategory());
		}

		assertTrue(blackDemon.isAttackAnimation(4678));
		assertTrue(blackDemon.isDefendAnimation(65));
		assertTrue(blackDemon.isDeathAnimation(67));
	}

	@Test
	public void testDragonsCategory()
	{
		RetroNpcData blueDragon = RetroNpcMapping.get(55, "Blue dragon");
		assertNotNull("Blue dragon mapping must exist", blueDragon);
		assertEquals(RetroNpcCategory.ADULT_DRAGONS, blueDragon.getCategory());
		assertTrue("Blue dragon should have models", blueDragon.getRetroModelIds().length > 0);

		// Verify dragon animations (80 melee, 91 head attack, 81-83 KBD firebreath, 1990 ranged)
		assertTrue(blueDragon.isAttackAnimation(80));
		assertTrue(blueDragon.isAttackAnimation(91));
		assertTrue(blueDragon.isAttackAnimation(81));
		assertTrue(blueDragon.isAttackAnimation(82));
		assertTrue(blueDragon.isAttackAnimation(83));
		assertTrue(blueDragon.isAttackAnimation(1990));
		assertTrue(blueDragon.isDefendAnimation(89));
		assertTrue(blueDragon.isDefendAnimation(4638));
		assertTrue(blueDragon.isDeathAnimation(92));
		// Walk (79) and ready (90) sequences are not combat anims, and the stale
		// skeleton-update IDs (5489/5491) are gone after the gameval re-curation
		assertFalse(blueDragon.isAttackAnimation(79));
		assertFalse(blueDragon.isDefendAnimation(90));
		assertFalse(blueDragon.isDefendAnimation(5489));
		assertFalse(blueDragon.isDeathAnimation(5491));
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

		// Verify modern goblin attack animations (6154 is the sergeant attack)
		assertTrue(goblin.isAttackAnimation(6184));
		assertTrue(goblin.isAttackAnimation(6185));
		assertTrue(goblin.isAttackAnimation(6188));
		assertTrue(goblin.isAttackAnimation(6154));
		assertTrue(goblin.isAttackAnimation(309));
		assertTrue(goblin.isAttackAnimation(310));

		// Verify modern goblin defend animations (6189 spear defend, 6155 sergeant defend)
		assertTrue(goblin.isDefendAnimation(6183));
		assertTrue(goblin.isDefendAnimation(6189));
		assertTrue(goblin.isDefendAnimation(6155));
		assertTrue(goblin.isDefendAnimation(312));

		// Verify modern goblin death animations (6190/6191 are spear/arrow death
		// sequences, 6156 the sergeant death)
		assertTrue(goblin.isDeathAnimation(6182));
		assertTrue(goblin.isDeathAnimation(313));
		assertTrue(goblin.isDeathAnimation(6190));
		assertTrue(goblin.isDeathAnimation(6191));
		assertTrue(goblin.isDeathAnimation(6156));

		// 6186/6153 are ready stances and death sequences must never register as
		// attacks or flinches
		assertFalse(goblin.isAttackAnimation(6186));
		assertFalse(goblin.isAttackAnimation(6153));
		assertFalse(goblin.isAttackAnimation(6190));
		assertFalse(goblin.isAttackAnimation(6191));
		assertFalse(goblin.isAttackAnimation(6156));
		assertFalse(goblin.isDefendAnimation(6156));
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

		// Verify Varrock/Falador/Ardougne Guard explicit ID mappings
		int[] guardIds = {
			NpcID.BIM_FAI_VARROCK_GUARD02, NpcID.BIM_FAI_VARROCK_GUARD02_F, NpcID.FAI_VARROCK_GUARD02,
			NpcID.GUARD1_VARIANT01, NpcID.ARDOUGNE_GUARD_VARIANT01,
			NpcID.FAI_FALADOR_GUARD1_VARIANT01, NpcID.FAI_FALADOR_GUARD4_F};
		for (int id : guardIds)
		{
			RetroNpcData guardById = RetroNpcMapping.get(id, "Guard");
			assertNotNull("Guard ID " + id + " must map to data", guardById);
			assertEquals(RetroNpcCategory.GUARDS, guardById.getCategory());
		}

		// Verify modern guard animations
		assertTrue(guardByName.isAttackAnimation(422));
		assertTrue(guardByName.isAttackAnimation(423));
		assertTrue(guardByName.isDefendAnimation(424));
		assertTrue(guardByName.isDeathAnimation(836));
		// 451 (chathead), 7041 (crawl), 7043 (run) and 7044 (turn) are not combat sequences
		assertFalse(guardByName.isAttackAnimation(451));
		assertFalse(guardByName.isAttackAnimation(7041));
		assertFalse(guardByName.isDefendAnimation(7043));
		assertFalse(guardByName.isDeathAnimation(7044));
	}

	@Test
	public void testImpsCategory()
	{
		RetroNpcData imp = RetroNpcMapping.get(NpcID.IMP, "Imp");
		assertNotNull("Imp mapping must exist", imp);
		assertEquals(RetroNpcCategory.IMPS, imp.getCategory());
		assertArrayEquals(new int[]{2887}, imp.getRetroModelIds());
		assertEquals(171, imp.getIdleAnimationId());
		assertEquals(168, imp.getWalkAnimationId());
		assertEquals(169, imp.getAttackAnimationId());
		assertEquals(170, imp.getDefendAnimationId());
		assertEquals(172, imp.getDeathAnimationId());

		int[] impIds = {NpcID.IMP, NpcID.GODWARS_ANCIENT_IMP, NpcID.CASTLEWARS_IMP};
		for (int id : impIds)
		{
			RetroNpcData impById = RetroNpcMapping.get(id, "Imp");
			assertNotNull("Imp ID " + id + " must be mapped", impById);
			assertEquals(RetroNpcCategory.IMPS, impById.getCategory());
		}

		// The gameval re-curation trimmed the imp sets to the actual imp
		// sequences - human/skeleton/chicken anims no longer belong to them
		assertTrue(imp.isAttackAnimation(169));
		assertTrue(imp.isDefendAnimation(170));
		assertTrue(imp.isDeathAnimation(172));

		assertFalse(imp.isAttackAnimation(422));
		assertFalse(imp.isAttackAnimation(5485));
		assertFalse(imp.isDefendAnimation(424));
		assertFalse(imp.isDefendAnimation(5388));
		assertFalse(imp.isDeathAnimation(836));
		assertFalse(imp.isDeathAnimation(5389));
		assertFalse(imp.isAttackAnimation(99999));
	}

	@Test
	public void testSkeletonsCategory()
	{
		// Unarmed skeleton
		RetroNpcData unarmed = RetroNpcMapping.get(NpcID.SKELETON_UNARMED, "Skeleton");
		assertNotNull("Unarmed skeleton must exist", unarmed);
		assertEquals(RetroNpcCategory.SKELETONS, unarmed.getCategory());
		assertArrayEquals(new int[]{2944}, unarmed.getRetroModelIds());
		assertEquals(262, unarmed.getIdleAnimationId());
		assertEquals(259, unarmed.getWalkAnimationId());
		assertEquals(260, unarmed.getAttackAnimationId());
		assertEquals(261, unarmed.getDefendAnimationId());
		assertEquals(263, unarmed.getDeathAnimationId());

		RetroNpcData armed = RetroNpcMapping.get(NpcID.SKELETON_ARMED, "Skeleton");
		assertNotNull("Armed skeleton must exist", armed);
		assertEquals(RetroNpcCategory.SKELETONS, armed.getCategory());
		assertArrayEquals(new int[]{2944, 2946}, armed.getRetroModelIds());

		int[] unarmedIds = {
				NpcID.SKELETON_UNARMED, NpcID.SKELETON_UNARMED2, NpcID.SKELETON_UNARMED3,
				NpcID.SKELETON_UNARMED4, NpcID.SKELETON_UNAGRESSIVE
		};
		for (int id : unarmedIds)
		{
			RetroNpcData skel = RetroNpcMapping.get(id, "Skeleton");
			assertNotNull("Skeleton ID " + id + " must be mapped", skel);
			assertEquals(RetroNpcCategory.SKELETONS, skel.getCategory());
			assertEquals(1, skel.getRetroModelIds().length);
		}

		int[] armedIds = {
				NpcID.SKELETON_ARMED, NpcID.SKELETON_ARMED2, NpcID.SKELETON_UNAGRESSIVE2,
				NpcID.SKELETON_UNAGRESSIVE3
		};
		for (int id : armedIds)
		{
			RetroNpcData skel = RetroNpcMapping.get(id, "Skeleton");
			assertNotNull("Armed skeleton ID " + id + " must be mapped", skel);
			assertEquals(RetroNpcCategory.SKELETONS, skel.getCategory());
			assertEquals(2, skel.getRetroModelIds().length);
		}

		// Verify modern attack animations (unarmed, weapon slash, stab, crush, modern variants)
		int[] skeletonAttacks = {260, 422, 423, 412, 390, 400, 401, 414, 426, 428, 440, 5485, 5486, 5487, 5488, 5507, 5512};
		for (int anim : skeletonAttacks)
		{
			assertTrue("Skeleton should match attack anim " + anim, unarmed.isAttackAnimation(anim));
			assertTrue("Armed skeleton should match attack anim " + anim, armed.isAttackAnimation(anim));
		}

		// Verify modern defend / flinch animations (block, flinch, hit, shield)
		int[] skeletonDefends = {261, 424, 425, 1156, 5489, 5490, 5508};
		for (int anim : skeletonDefends)
		{
			assertTrue("Skeleton should match defend anim " + anim, unarmed.isDefendAnimation(anim));
			assertTrue("Armed skeleton should match defend anim " + anim, armed.isDefendAnimation(anim));
		}

		// Verify modern death animations (collapse, generic humanoid death, skeleton-update deaths)
		int[] skeletonDeaths = {263, 836, 5491, 5492, 5509, 7042};
		for (int anim : skeletonDeaths)
		{
			assertTrue("Skeleton should match death anim " + anim, unarmed.isDeathAnimation(anim));
			assertTrue("Armed skeleton should match death anim " + anim, armed.isDeathAnimation(anim));
		}

		// Non-combat sequences (chathead 451, crawl 7041, run 7043, turn 7044, ready 5493)
		// must not be intercepted
		assertFalse(unarmed.isAttackAnimation(451));
		assertFalse(unarmed.isAttackAnimation(7041));
		assertFalse(unarmed.isDefendAnimation(7043));
		assertFalse(unarmed.isDeathAnimation(7044));
		assertFalse(unarmed.isDeathAnimation(5493));
		// Goblin/zombie anims (310, 300, 312, 302, 313) and the spider death 8005
		// were dropped from the skeleton sets in the gameval re-curation
		assertFalse(unarmed.isAttackAnimation(310));
		assertFalse(unarmed.isDefendAnimation(300));
		assertFalse(unarmed.isDefendAnimation(312));
		assertFalse(unarmed.isDeathAnimation(302));
		assertFalse(unarmed.isDeathAnimation(313));
		assertFalse(unarmed.isDeathAnimation(8005));

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
		RetroNpcData armedZombie = RetroNpcMapping.get(NpcID.ZOMBIE_ARMED_SEWER1, "Zombie");
		assertNotNull("Armed zombie mapping must exist", armedZombie);
		assertEquals(RetroNpcCategory.ZOMBIES, armedZombie.getCategory());
		assertArrayEquals(new int[]{2931, 2932}, armedZombie.getRetroModelIds());
		assertEquals(301, armedZombie.getIdleAnimationId());
		assertEquals(298, armedZombie.getWalkAnimationId());
		assertEquals(299, armedZombie.getAttackAnimationId());
		assertEquals(300, armedZombie.getDefendAnimationId());
		assertEquals(302, armedZombie.getDeathAnimationId());

		// Verify unarmed zombie explicit IDs (including Level 13 sewer zombies)
		int[] unarmedIds = {
			NpcID.ZOMBIE_UNARMED, NpcID.ZOMBIE_UNARMED2, NpcID.ZOMBIE_UNARMED3, NpcID.ZOMBIE_UNARMED4,
			NpcID.ZOMBIE_UNARMED5, NpcID.ZOMBIE_UNARMED6, NpcID.ZOMBIE_UNARMED_CITY1, NpcID.ZOMBIE_UNARMED_CITY3,
			NpcID.ZOMBIE_UNARMED_CITY6, NpcID.ZOMBIE_UNARMED_SEWER1, NpcID.ZOMBIE_UNARMED_SEWER2,
			NpcID.ZOMBIE_UNARMED_SEWER3, NpcID.ZOMBIE_UNARMED_SEWER4,
			NpcID.ZOMBIE2, NpcID.ZOMBIE2_B, NpcID.ZOMBIE2_C};
		for (int id : unarmedIds)
		{
			RetroNpcData data = RetroNpcMapping.get(id, "Zombie");
			assertNotNull("Unarmed zombie ID " + id + " must be mapped", data);
			assertEquals(RetroNpcCategory.ZOMBIES, data.getCategory());
			assertEquals(1, data.getRetroModelIds().length);
			assertEquals(2931, data.getRetroModelIds()[0]);
			assertEquals(300, data.getDefendAnimationId());
		}

		// Verify armed zombie explicit IDs (including Level 24 sewer zombies)
		int[] armedIds = {
			NpcID.ZOMBIE_ARMED, NpcID.ZOMBIE_ARMED2, NpcID.ZOMBIE_ARMED3, NpcID.ZOMBIE_ARMED_CITY1,
			NpcID.ZOMBIE_ARMED_CITY3, NpcID.ZOMBIE_ARMED_SEWER1, NpcID.ZOMBIE_ARMED_SEWER2,
			NpcID.ZOMBIE_ARMED_SEWER3, NpcID.ZOMBIE_ARMED_SEWER4};
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
		int[] attackAnims = {5568, 5571, 5578, 5581, 5512, 422, 423, 412, 299};
		for (int anim : attackAnims)
		{
			assertTrue("Zombie should match attack anim " + anim, zombie.isAttackAnimation(anim));
			assertTrue("Armed zombie should match attack anim " + anim, armedZombie.isAttackAnimation(anim));
			assertFalse("Attack anim " + anim + " must not be defend", zombie.isDefendAnimation(anim));
			assertFalse("Attack anim " + anim + " must not be death", zombie.isDeathAnimation(anim));
		}

		// Test various modern zombie defend animations (flinches, blocks)
		int[] defendAnims = {5567, 5574, 5579, 424, 425, 1156, 300};
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

		// Test various modern zombie death animations
		int[] deathAnims = {5569, 5575, 5580, 5587, 836, 302};
		for (int anim : deathAnims)
		{
			assertTrue("Zombie should match death anim " + anim, zombie.isDeathAnimation(anim));
			assertTrue("Armed zombie should match death anim " + anim, armedZombie.isDeathAnimation(anim));
			assertFalse("Death anim " + anim + " must not be defend", zombie.isDefendAnimation(anim));
		}

		// Ready/walk sequences (5572, 5594, 5595) are not deaths and must not be
		// intercepted; the Tarn's Lair anims (log swing 5588, pickaxe 5590-5592)
		// no longer belong to the zombie sets
		assertFalse(zombie.isDeathAnimation(5572));
		assertFalse(zombie.isDeathAnimation(5594));
		assertFalse(zombie.isDeathAnimation(5595));
		assertFalse(zombie.isAttackAnimation(5588));
		assertFalse(zombie.isDefendAnimation(5590));
		assertFalse(zombie.isAttackAnimation(5591));
		assertFalse(zombie.isDeathAnimation(5592));

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
		assertTrue(unmappedZombie.isDeathAnimation(5587));
	}

	@Test
	public void testGhostsCategory()
	{
		// The 2005 ghost sequences survive in the modern cache at their original IDs,
		// under gameval GHOST_* names (READY=125, WALK=119, ATTACK=123, BLOCK=124, DEATH=126)
		RetroNpcData ghost = RetroNpcMapping.get(0, "Ghost");
		assertNotNull("Ghost mapping must exist", ghost);
		assertEquals(RetroNpcCategory.GHOSTS, ghost.getCategory());
		assertArrayEquals(new int[]{2961, 2964, 2965}, ghost.getRetroModelIds());
		assertEquals(AnimationID.GHOST_READY, ghost.getIdleAnimationId());
		assertEquals(AnimationID.GHOST_WALK, ghost.getWalkAnimationId());
		assertEquals(AnimationID.GHOST_ATTACK, ghost.getAttackAnimationId());
		assertEquals(AnimationID.GHOST_BLOCK, ghost.getDefendAnimationId());
		assertEquals(AnimationID.GHOST_DEATH, ghost.getDeathAnimationId());

		// Modern ghost combat anims are intercepted and swapped to the retro sequences
		assertTrue(ghost.isAttackAnimation(AnimationID.GHOST_UPDATE_NORMAL_ATTACK));
		assertTrue(ghost.isAttackAnimation(AnimationID.BOSSGHOST_ATTACK));
		assertTrue(ghost.isDefendAnimation(AnimationID.GHOST_UPDATE_NORMAL_DEFEND));
		assertTrue(ghost.isDeathAnimation(AnimationID.GHOST_UPDATE_NORMAL_DEATH));
		assertTrue(ghost.isDeathAnimation(AnimationID.BOSSGHOST_DEATH));

		// The retro sequences themselves are targets, not modern anims to intercept
		assertFalse(ghost.isAttackAnimation(AnimationID.GHOST_ATTACK));
		assertFalse(ghost.isDefendAnimation(AnimationID.GHOST_BLOCK));
		assertFalse(ghost.isDeathAnimation(AnimationID.GHOST_DEATH));

		// The Restless ghost is non-combat: model + idle/walk (GHOSTHUMAN poses) only,
		// combat anims stay -1 so combat interception short-circuits
		RetroNpcData restless = RetroNpcMapping.get(0, "Restless ghost");
		assertNotNull("Restless ghost mapping must exist", restless);
		assertEquals(RetroNpcCategory.GHOSTS, restless.getCategory());
		assertEquals(AnimationID.GHOSTHUMAN_READY, restless.getIdleAnimationId());
		assertEquals(AnimationID.GHOSTHUMAN_WALK_FORWARD, restless.getWalkAnimationId());
		assertEquals(-1, restless.getAttackAnimationId());
		assertEquals(-1, restless.getDefendAnimationId());
		assertEquals(-1, restless.getDeathAnimationId());
		assertFalse(restless.isAttackAnimation(AnimationID.GHOST_UPDATE_NORMAL_ATTACK));
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

		int[] hillGiantIds = {
			NpcID.GIANT, NpcID.GIANT2, NpcID.GIANT3, NpcID.GIANT4, NpcID.GIANT5, NpcID.GIANT6,
			NpcID.KOUREND_HILLGIANT};
		for (int id : hillGiantIds)
		{
			RetroNpcData giantById = RetroNpcMapping.get(id, "Hill giant");
			assertNotNull("Hill giant ID " + id + " must exist", giantById);
			assertEquals(RetroNpcCategory.HILL_GIANTS, giantById.getCategory());
		}

		// Test modern giant animations
		assertTrue(hillGiant.isAttackAnimation(4652));
		assertTrue(hillGiant.isAttackAnimation(4658));
		assertTrue(hillGiant.isAttackAnimation(4666));
		assertTrue(hillGiant.isAttackAnimation(4667));
		assertTrue(hillGiant.isDefendAnimation(4651));
		assertTrue(hillGiant.isDefendAnimation(4657));
		assertTrue(hillGiant.isDefendAnimation(4665));
		assertTrue(hillGiant.isDeathAnimation(4653));
		assertTrue(hillGiant.isDeathAnimation(4659));
		assertTrue(hillGiant.isDeathAnimation(4668));
		// Unrelated sequences (hydra idle 7002, snowdrops 7001, godsword 7004) are not giant anims
		assertFalse(hillGiant.isAttackAnimation(7002));
		assertFalse(hillGiant.isDefendAnimation(7001));
		assertFalse(hillGiant.isDeathAnimation(7004));
		// 4667 is the fire giant sword ATTACK and must never register as a death
		assertFalse(hillGiant.isDeathAnimation(4667));
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

		assertTrue(chicken.isAttackAnimation(55));
		assertTrue(chicken.isAttackAnimation(5387));
		assertTrue(chicken.isDefendAnimation(56));
		assertTrue(chicken.isDefendAnimation(5388));
		assertTrue(chicken.isDeathAnimation(57));
		assertTrue(chicken.isDeathAnimation(5389));
		// 5385 is the chicken walk and 5390 an unrelated sequence - neither is combat
		assertFalse(chicken.isAttackAnimation(5385));
		assertFalse(chicken.isDeathAnimation(5390));
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
		int[] deathAnims = {5569, 5575, 5580, 5587, 836, 302};
		for (int death : deathAnims)
		{
			assertTrue("Sequence " + death + " must match isDeathAnimation", Objects.requireNonNull(sampleZombie).isDeathAnimation(death));
			assertFalse("Sequence " + death + " must NOT match isDefendAnimation (prevents flinch-on-death regression)", sampleZombie.isDefendAnimation(death));
			assertFalse("Sequence " + death + " must NOT match isAttackAnimation", sampleZombie.isAttackAnimation(death));
		}

		// Modern variant defend animations MUST match isDefendAnimation and MUST NEVER match isDeathAnimation
		int[] defendAnims = {5567, 5574, 5579, 424, 425, 1156, 300};
		for (int defend : defendAnims)
		{
			assertTrue("Sequence " + defend + " must match isDefendAnimation", sampleZombie.isDefendAnimation(defend));
			assertFalse("Sequence " + defend + " must NOT match isDeathAnimation", sampleZombie.isDeathAnimation(defend));
			assertFalse("Sequence " + defend + " must NOT match isAttackAnimation", sampleZombie.isAttackAnimation(defend));
		}

		// Modern variant attack animations MUST match isAttackAnimation and MUST NEVER match isDefendAnimation or isDeathAnimation
		int[] attackAnims = {5568, 5571, 5578, 5581, 5512, 422, 423, 412, 299};
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

		// An explicitly registered ID swaps even when the modern name no longer
		// matches a 2005 name (the ID fallback in RetroNpcMapping.get)
		RetroNpcData byIdOnly = RetroNpcMapping.get(55, "Guard dog");
		assertNotNull(byIdOnly);
		assertEquals(RetroNpcCategory.ZOMBIES, byIdOnly.getCategory());

		// Unregistered IDs with non-matching names still return nothing
		assertNull(RetroNpcMapping.get(99997, "Guard dog"));
		assertNull(RetroNpcMapping.get(99997, "Ogre guard"));
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
	public void testConfigDefaults()
	{
		RetroNpcConfig config = new RetroNpcConfig() {};

		// Safety toggles must default to on
		assertTrue("disablePvpWorld must default to true", config.disablePvpWorld());
		assertTrue("disableWilderness must default to true", config.disableWilderness());

		// The five live category toggles default to on
		assertTrue("swapChickens must default to true", config.swapChickens());
		assertTrue("swapGoblins must default to true", config.swapGoblins());
		assertTrue("swapSkeletons must default to true", config.swapSkeletons());
		assertTrue("swapZombies must default to true", config.swapZombies());
		assertTrue("swapHillGiants must default to true", config.swapHillGiants());
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
