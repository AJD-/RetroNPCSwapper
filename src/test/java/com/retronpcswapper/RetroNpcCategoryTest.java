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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
		RetroNpcMapping.load(loadCommittedEntries());
	}

	/**
	 * Reads the shipped resource the way the plugin does at startup.
	 */
	private static List<RetroNpcMappingEntry> loadCommittedEntries() throws Exception
	{
		try (InputStream in = RetroNpcSwapperPlugin.class.getResourceAsStream("npc-mappings.json"))
		{
			assertNotNull("npc-mappings.json resource missing - run ./gradlew generateNpcMappings", in);
			return new Gson().fromJson(
				new InputStreamReader(in, StandardCharsets.UTF_8),
				new TypeToken<List<RetroNpcMappingEntry>>() {}.getType());
		}
	}

	/**
	 * Lesser demons need injected geometry, and not for the reason the code used to give. The
	 * 2005 sequences survive under DEMON_* gameval names; it is the mesh that is gone. Model 2943
	 * resolves in the live cache but holds a 1000-vertex asset where the 2005 lesser demon is 428,
	 * and a scan of all 61874 live models finds the retro mesh at no id - the 29 August 2006
	 * graphical update replaced it, before the snapshot OSRS descends from. The mapping is still
	 * asserted here because it resolves by name and must stay internally correct.
	 */
	@Test
	public void testLesserDemonsCategory()
	{
		RetroNpcData lesserDemon = RetroNpcMapping.get(NpcID.LESSER_DEMON, "Lesser demon");
		assertNotNull("Lesser demon mapping must exist", lesserDemon);
		assertEquals(RetroNpcCategory.LESSER_DEMONS, lesserDemon.getCategory());
		assertArrayEquals(new int[]{2943}, lesserDemon.getRetroModelIds());
		assertEquals(AnimationID.DEMON_READY, lesserDemon.getIdleAnimationId());
		assertEquals(AnimationID.DEMON_WALK, lesserDemon.getWalkAnimationId());
		assertEquals(AnimationID.DEMON_ATTACK, lesserDemon.getAttackAnimationId());
		assertEquals(AnimationID.DEMON_BLOCK, lesserDemon.getDefendAnimationId());
		assertEquals(AnimationID.DEMON_DEATH, lesserDemon.getDeathAnimationId());

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

		// Only the DEMON_UPDATE_* rig and the DEMONS_* set are modern. The plain Lesser demon
		// definitions (2005-2008, 2018) still carry standingAnim 66 / walkingAnim 63, so the
		// 2005 sequences are what the live game already plays; the update rig belongs to the
		// Lesser Demon Champion and its kin.
		assertTrue(lesserDemon.isAttackAnimation(1528));
		assertTrue(lesserDemon.isAttackAnimation(4644));
		assertTrue(lesserDemon.isAttackAnimation(4678));
		assertTrue(lesserDemon.isAttackAnimation(4679));
		assertTrue(lesserDemon.isAttackAnimation(4680));
		assertTrue(lesserDemon.isDefendAnimation(4676));
		assertTrue(lesserDemon.isDeathAnimation(AnimationID.DEMON_UPDATE_DEATH));
		assertTrue(lesserDemon.isDeathAnimation(1530));

		// The DT2_SCAR_MAZE mage and ranged demons register to this archetype, so their cast
		// and swipe must be intercepted too
		assertTrue(lesserDemon.isAttackAnimation(AnimationID.DEMON_UPDATE_SWIPE));
		assertTrue(lesserDemon.isAttackAnimation(AnimationID.DEMON_UPDATE_FIREBALL_CAST));

		// The retro sequences are the swap targets, never anims to intercept. Listing them made
		// the plugin re-swap its own output - the bug the ghost sets carried before PR #3.
		assertFalse(lesserDemon.isAttackAnimation(AnimationID.DEMON_ATTACK));
		assertFalse(lesserDemon.isDefendAnimation(AnimationID.DEMON_BLOCK));
		assertFalse(lesserDemon.isDeathAnimation(AnimationID.DEMON_DEATH));
		assertFalse(lesserDemon.isDeathAnimation(AnimationID.DEMON_DEATH_GREATER));

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
		assertEquals(AnimationID.DEMON_READY, greaterDemon.getIdleAnimationId());
		assertEquals(AnimationID.DEMON_WALK, greaterDemon.getWalkAnimationId());
		assertEquals(AnimationID.DEMON_ATTACK, greaterDemon.getAttackAnimationId());
		assertEquals(AnimationID.DEMON_BLOCK, greaterDemon.getDefendAnimationId());
		assertEquals(AnimationID.DEMON_DEATH, greaterDemon.getDeathAnimationId());

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
		assertTrue(greaterDemon.isDeathAnimation(AnimationID.DEMON_UPDATE_DEATH));
		// 68 is DEMON_DEATH_GREATER, a surviving 2005 sequence, not a modern one
		assertFalse(greaterDemon.isDeathAnimation(AnimationID.DEMON_DEATH_GREATER));
	}

	@Test
	public void testBlackDemonsCategory()
	{
		RetroNpcData blackDemon = RetroNpcMapping.get(NpcID.BLACK_DEMON, "Black demon");
		assertNotNull("Black demon mapping must exist", blackDemon);
		assertEquals(RetroNpcCategory.BLACK_DEMONS, blackDemon.getCategory());
		assertArrayEquals(new int[]{2942}, blackDemon.getRetroModelIds());
		assertEquals(AnimationID.DEMON_READY, blackDemon.getIdleAnimationId());
		assertEquals(AnimationID.DEMON_WALK, blackDemon.getWalkAnimationId());
		assertEquals(AnimationID.DEMON_ATTACK, blackDemon.getAttackAnimationId());
		assertEquals(AnimationID.DEMON_BLOCK, blackDemon.getDefendAnimationId());
		assertEquals(AnimationID.DEMON_DEATH, blackDemon.getDeathAnimationId());

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
		assertTrue(blackDemon.isDefendAnimation(AnimationID.DEMON_UPDATE_DEFEND));
		assertTrue(blackDemon.isDeathAnimation(AnimationID.DEMON_UPDATE_DEATH));
		assertFalse(blackDemon.isDefendAnimation(AnimationID.DEMON_BLOCK));
		assertFalse(blackDemon.isDeathAnimation(AnimationID.DEMON_DEATH));
	}

	@Test
	public void testDragonsCategory()
	{
		RetroNpcData blueDragon = RetroNpcMapping.get(55, "Blue dragon");
		assertNotNull("Blue dragon mapping must exist", blueDragon);
		assertEquals(RetroNpcCategory.ADULT_DRAGONS, blueDragon.getCategory());
		assertTrue("Blue dragon should have models", blueDragon.getRetroModelIds().length > 0);

		// The ranged attack (1990) is the one post-2005 sequence, so it is the only one swapped,
		// and it swaps to the 2005 melee attack (80)
		assertTrue(blueDragon.isAttackAnimation(1990));
		assertEquals(AnimationID.DRAGON_ATTACK, blueDragon.getAttackAnimationId());

		// Dragons kept their 2005 sequences, so these must play through untouched. Rewriting them
		// turned every melee and firebreath attack into a head attack.
		assertFalse("melee attack is already the retro sequence", blueDragon.isAttackAnimation(80));
		assertFalse("head attack is already the retro sequence", blueDragon.isAttackAnimation(91));
		assertFalse(blueDragon.isAttackAnimation(81));
		assertFalse(blueDragon.isAttackAnimation(82));
		assertFalse(blueDragon.isAttackAnimation(83));

		// Death needs no swap at all, so that slot is -1 and short-circuits
		assertEquals(-1, blueDragon.getDeathAnimationId());
		assertFalse(blueDragon.isDeathAnimation(92));

		// The defend slot is filled for KBD's sake - it is the one adult dragon
		// with a post-2005 block, DRAGON_BLOCK_KBD 4638. A chromatic dragon carries the slot but
		// never reaches the override, because it only fires on an id in DRAGON_MODERN_DEFENDS and
		// an ordinary dragon already blocks on the 2005 sequence.
		assertEquals(AnimationID.DRAGON_BLOCK, blueDragon.getDefendAnimationId());
		assertFalse("block 89 is already the retro sequence", blueDragon.isDefendAnimation(89));
		assertTrue(blueDragon.isDefendAnimation(AnimationID.DRAGON_BLOCK_KBD));

		// Walk (79) and ready (90) are pose sequences, never combat anims
		assertFalse(blueDragon.isAttackAnimation(79));
		assertFalse(blueDragon.isDefendAnimation(90));
		assertFalse(blueDragon.isAttackAnimation(99999));
	}

	/**
	 * The metal dragons are the same category on a different 2005 mesh set: three parts rather
	 * than two, and three opcode 40 pairs rather than one, because bronze, iron and steel are one
	 * greyscale mesh told apart entirely by colour. They resolve by name like the chromatics -
	 * ADULT_DRAGONS is not an id-only category and no static archetype shadows these rows.
	 */
	@Test
	public void testMetalDragonsCategory()
	{
		int[] ids = {NpcID.BRONZE_DRAGON, NpcID.IRON_DRAGON, NpcID.STEEL_DRAGON};
		String[] names = {"Bronze dragon", "Iron dragon", "Steel dragon"};

		Set<List<Short>> palettes = new HashSet<>();
		for (int i = 0; i < ids.length; i++)
		{
			RetroNpcData dragon = RetroNpcMapping.get(ids[i], names[i]);
			assertNotNull(names[i] + " mapping must exist", dragon);
			assertEquals(RetroNpcCategory.ADULT_DRAGONS, dragon.getCategory());

			assertArrayEquals(names[i] + " is a three part 2005 mesh",
				new int[]{4986, 5022, 4987}, dragon.getInjectedModelIds());

			// Without the pairs all three render as the same grey lump, so this is structural
			assertTrue(names[i] + " must carry its 2005 recolors", dragon.hasRecolors());
			assertArrayEquals(new short[]{61, 33, 41}, dragon.getOriginalColors());
			assertEquals(3, dragon.getReplacementColors().length);

			List<Short> palette = new ArrayList<>();
			for (short color : dragon.getReplacementColors())
			{
				palette.add(color);
			}
			assertTrue(names[i] + " must not share a palette with another metal",
				palettes.add(palette));

			// Same policy as the chromatics: only the post-2005 ranged attack is intercepted, and
			// the retro-native sequences - firebreath included - pass straight through
			assertEquals(AnimationID.DRAGON_READY, dragon.getIdleAnimationId());
			assertEquals(AnimationID.DRAGON_WALK, dragon.getWalkAnimationId());
			assertEquals(AnimationID.DRAGON_ATTACK, dragon.getAttackAnimationId());
			assertTrue(dragon.isAttackAnimation(AnimationID.DRAGON_RANGED_ATTACKS));
			for (int retroNative : new int[]{80, 81, 82, 83, 84, 91})
			{
				assertFalse("sequence " + retroNative + " is already the retro one",
					dragon.isAttackAnimation(retroNative));
			}
			// The defend slot is category-wide, filled for the King Black Dragon's post-2005
			// block; a metal dragon carries it but never sends 4638, so it never fires
			assertEquals(AnimationID.DRAGON_BLOCK, dragon.getDefendAnimationId());
			assertFalse(names[i] + " blocks on the retro sequence already",
				dragon.isDefendAnimation(AnimationID.DRAGON_BLOCK));
			assertEquals(-1, dragon.getDeathAnimationId());
		}
	}

	/**
	 * The King Black Dragon reaches the category the same way every other adult dragon does -
	 * by name, off the generated row, with no id registration anywhere.
	 */
	@Test
	public void testKingBlackDragonCategory()
	{
		for (int id : new int[]{
			NpcID.KING_DRAGON, NpcID.CLANCUP_KING_DRAGON,
			NpcID.TWOCATS_KBD_CUTSCENE, NpcID.DEADMAN_BREACH_KING_BLACK_DRAGON})
		{
			RetroNpcData kbd = RetroNpcMapping.get(id, "King Black Dragon");
			assertNotNull("King Black Dragon mapping must exist for id " + id, kbd);
			assertEquals(RetroNpcCategory.ADULT_DRAGONS, kbd.getCategory());

			// The chromatic body wearing the three-headed head, in place of the single head 2854
			assertArrayEquals(new int[]{2853, 2855}, kbd.getInjectedModelIds());

			// The one dragon whose 2005 definition asks to be resized, and the one carrying five
			// opcode 40 pairs rather than a single body tint
			assertEquals(160, kbd.getScaleXZ());
			assertEquals(160, kbd.getScaleY());
			assertTrue(kbd.hasRecolors());
			assertArrayEquals(new short[]{61, 41, 0, 115, 127}, kbd.getOriginalColors());
			assertEquals(5, kbd.getReplacementColors().length);

			assertEquals(AnimationID.DRAGON_READY, kbd.getIdleAnimationId());
			assertEquals(AnimationID.DRAGON_WALK, kbd.getWalkAnimationId());

			// Its block is the one post-2005 sequence in the whole category
			assertTrue(kbd.isDefendAnimation(AnimationID.DRAGON_BLOCK_KBD));
			assertEquals(AnimationID.DRAGON_BLOCK, kbd.getDefendAnimationId());

			// Everything else it plays is retro-native and passes straight through
			for (int retroNative : new int[]{80, 81, 82, 83, 84, 91})
			{
				assertFalse("sequence " + retroNative + " is already the retro one",
					kbd.isAttackAnimation(retroNative));
			}
			assertTrue(kbd.isAttackAnimation(AnimationID.DRAGON_RANGED_ATTACKS));
		}

		// The wall trophies and the pet share the theme but not the name, so nothing extra is
		// needed to keep them out - "Left head" and "Prince Black Dragon" simply never match
		assertNull(RetroNpcMapping.get(NpcID.POH_MOUNTED_KBD_LEFT, "Left head"));
		assertNull(RetroNpcMapping.get(NpcID.KBD_PET, "Prince Black Dragon"));
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
		// By id, not by name: "Guard" alone no longer resolves, see ID_ONLY_CATEGORIES
		RetroNpcData guard = RetroNpcMapping.get(NpcID.GUARD1, "Guard");
		assertNotNull("the registered town guard must resolve", guard);
		assertEquals(RetroNpcCategory.GUARDS, guard.getCategory());

		// Nine parts of 2005 human kit. Head 294, arms 151 and hands 254 no longer resolve to their
		// 2005 geometry in the live cache, which is what makes this category injection-only.
		assertArrayEquals(new int[]{233, 246, 294, 151, 176, 254, 185, 519, 541},
			guard.getRetroModelIds());
		assertArrayEquals(guard.getRetroModelIds(), guard.getInjectedModelIds());

		assertEquals(808, guard.getIdleAnimationId());
		assertEquals(819, guard.getWalkAnimationId());
		assertEquals(422, guard.getAttackAnimationId());
		assertEquals(424, guard.getDefendAnimationId());
		assertEquals(836, guard.getDeathAnimationId());

		// Verify Varrock/Falador Guard explicit ID mappings. Ardougne is a different costume and
		// has its own test - see testArdougneGuards.
		int[] guardIds = {
			NpcID.BIM_FAI_VARROCK_GUARD02, NpcID.BIM_FAI_VARROCK_GUARD02_F, NpcID.FAI_VARROCK_GUARD02,
			NpcID.GUARD1_VARIANT01,
			NpcID.FAI_FALADOR_GUARD1_VARIANT01, NpcID.FAI_FALADOR_GUARD3_F,
			// The base row of each family. The list above enumerates the _F and _VARIANT
			// derivatives of exactly these NPCs and used to skip the NPCs themselves.
			NpcID.GUARD1,
			NpcID.FAI_FALADOR_GUARD1, NpcID.FAI_FALADOR_GUARD3};
		for (int id : guardIds)
		{
			RetroNpcData guardById = RetroNpcMapping.get(id, "Guard");
			assertNotNull("Guard ID " + id + " must map to data", guardById);
			assertEquals(RetroNpcCategory.GUARDS, guardById.getCategory());
		}

		// Verify modern guard animations
		assertTrue(guard.isAttackAnimation(422));
		assertTrue(guard.isAttackAnimation(423));
		assertTrue(guard.isDefendAnimation(424));
		assertTrue(guard.isDeathAnimation(836));
		// A guard fights with a sword and shield, so it blocks with HUMAN_SHIELD_DEFENCE. That is
		// shipped as its own 2005 clip now and must pass straight through: intercepting it onto
		// the unarmed block 424 is what left a guard blocking with no shield raise, and nothing
		// used to fail if the interception came back.
		assertFalse("1156 ships as a 2005 clip and must not be rewritten onto the unarmed block",
			guard.isDefendAnimation(1156));

		// Falador's bow and crossbow guards are called "Guard" too, so the name table would hand
		// them the sword-and-shield kit and take the bow away. The tell is the weapon model, not
		// the stance - 3272, 3273 and 3274 carry bow 563 in the ordinary 808 idle.
		for (int excluded : new int[]{
			NpcID.FAI_FALADOR_GUARD2, NpcID.FAI_FALADOR_GUARD2_F, NpcID.FAI_FALADOR_GUARD4,
			NpcID.FAI_FALADOR_GUARD5, NpcID.FAI_FALADOR_GUARD6,
			NpcID.FAI_VARROCK_GUARD})
		{
			assertNull("guard " + excluded + " must not be swapped by id",
				RetroNpcMapping.get(excluded, null));
			assertNull("guard " + excluded + " must not be swapped by name either",
				RetroNpcMapping.get(excluded, "Guard"));
		}

		// The guards either side of them in the same family are untouched
		assertNotNull(RetroNpcMapping.get(NpcID.FAI_FALADOR_GUARD1, "Guard"));
		assertNotNull(RetroNpcMapping.get(NpcID.FAI_FALADOR_GUARD3, "Guard"));
		assertNotNull("the melee female guards carry 23179, not a bow",
			RetroNpcMapping.get(NpcID.FAI_FALADOR_GUARD3_F, "Guard"));

		// Falador's axe guard is the same character carrying different equipment, so it wears the
		// guard kit with 2005 battleaxe 550 where the rest carry sword 519. Derived after the
		// recolor graft, so losing the 2005 colors here is the thing to watch.
		RetroNpcData axeGuard = RetroNpcMapping.get(NpcID.FAI_FALADOR_GUARD3, "Guard");
		assertNotNull(axeGuard);
		assertArrayEquals(new int[]{233, 246, 294, 151, 176, 254, 185, 550, 541},
			axeGuard.getRetroModelIds());
		assertArrayEquals(axeGuard.getRetroModelIds(), axeGuard.getInjectedModelIds());
		assertEquals(RetroNpcCategory.GUARDS, axeGuard.getCategory());
		assertTrue("the axe guard must inherit the guard recolors", axeGuard.hasRecolors());
		assertEquals(guard.getDefendAnimationId(), axeGuard.getDefendAnimationId());

		// Female guards are recent content with no 2005 counterpart, so they wear the male kit -
		// and the female of the axe guard gets the axe, not the sword
		assertArrayEquals(new int[]{233, 246, 294, 151, 176, 254, 185, 550, 541},
			Objects.requireNonNull(RetroNpcMapping.get(NpcID.FAI_FALADOR_GUARD3_F, "Guard")).getRetroModelIds());
		assertArrayEquals(new int[]{233, 246, 294, 151, 176, 254, 185, 519, 541},
			Objects.requireNonNull(RetroNpcMapping.get(NpcID.FAI_FALADOR_GUARD1_F, "Guard")).getRetroModelIds());

		// The female bow guard is replaced by the male one, taken from the live cache: the archer
		// guard is 2006 content unchanged since, and head 9458 and arms 9450 have no 2005 original.
		// These are NPC 3272's own parts, so she must take the cache-backed path - the bundle's
		// 2005 clips would drive live-rigged meshes off the wrong joints.
		RetroNpcData bowGuard = RetroNpcMapping.get(NpcID.FAI_FALADOR_GUARD4_F, "Guard");
		assertNotNull("the female bow guard is swapped, not excluded", bowGuard);
		assertArrayEquals(new int[]{233, 250, 9458, 9450, 176, 28285, 185, 563, 215},
			bowGuard.getRetroModelIds());
		assertTrue("she must be built from the live cache, not the bundle",
			RetroNpcMapping.usesLiveGeometry(NpcID.FAI_FALADOR_GUARD4_F));
		assertFalse("the 2005 recolors belong to the 2005 meshes, not these",
			bowGuard.hasRecolors());
		assertFalse("no other guard takes the live path",
			RetroNpcMapping.usesLiveGeometry(NpcID.FAI_FALADOR_GUARD1));

		// and the sword guards keep the sword
		assertArrayEquals(new int[]{233, 246, 294, 151, 176, 254, 185, 519, 541},
			Objects.requireNonNull(RetroNpcMapping.get(NpcID.FAI_FALADOR_GUARD1, "Guard")).getRetroModelIds());

		// "Guard" is a job rather than a costume: 184 NPCs carry the name and only the town guard
		// wears this kit, so the category resolves by registered id and the name alone buys
		// nothing. Trolls, dwarves, elves, goblins and archers were all being swapped before.
		int[] notTownGuards = {
			NpcID.TROLL_SGUARD1, NpcID.DWARF_CITY_BLACK_GUARD1, NpcID.PRIF_GUARD1,
			NpcID.DORGESH_GUARD1, NpcID.LATHASTRAINER2, NpcID.DEADMAN_GUARD_FALADOR_RANGE_VIS};
		for (int id : notTownGuards)
		{
			assertNull("NPC " + id + " is named Guard but is not a town guard",
				RetroNpcMapping.get(id, "Guard"));
		}

		// The Ratcatchers guards do wear the kit and were only ever reached by name, so they are
		// registered by id now rather than lost
		assertEquals(RetroNpcCategory.GUARDS,
			Objects.requireNonNull(RetroNpcMapping.get(NpcID.RATCATCHER_CHIEFGUARD, "Guard")).getCategory());
		assertEquals(RetroNpcCategory.GUARDS,
			Objects.requireNonNull(RetroNpcMapping.get(NpcID.RATCATCHER_GUARD_LEFT_INSIDE, "Guard")).getCategory());
		// 451 (chathead), 7041 (crawl), 7043 (run) and 7044 (turn) are not combat sequences
		assertFalse(guard.isAttackAnimation(451));
		assertFalse(guard.isAttackAnimation(7041));
		assertFalse(guard.isDefendAnimation(7043));
		assertFalse(guard.isDeathAnimation(7044));
	}

	@Test
	public void testArdougneGuards()
	{
		// Ardougne's guards were registered against GUARD_DEFAULT, which put every one of them in
		// a Varrock uniform. They are a different 2005 costume, not a recolor: definition 32's
		// seven parts against definition 9's nine, sharing only the boots.
		RetroNpcData ardougne = RetroNpcMapping.get(NpcID.ARDOUGNE_GUARD, "Guard");
		assertNotNull("the Ardougne guard must resolve by id", ardougne);
		assertEquals(RetroNpcCategory.GUARDS, ardougne.getCategory());
		assertArrayEquals(new int[]{225, 301, 162, 179, 274, 185, 502}, ardougne.getRetroModelIds());
		assertArrayEquals(ardougne.getRetroModelIds(), ardougne.getInjectedModelIds());

		// Live ARDOUGNE_GUARD carries these same pairs, byte for byte, 20 years on - they are what
		// separates the two towns' kit. Held inline on the archetype rather than grafted: the one
		// "guard" name key belongs to the town guard, and applyCacheDefinitions only reaches
		// archetypes that hold one.
		assertTrue("the Ardougne colors must survive load()", ardougne.hasRecolors());
		assertArrayEquals(new short[]{25238, 8741}, ardougne.getOriginalColors());
		assertArrayEquals(new short[]{811, -21597}, ardougne.getReplacementColors());

		// Same 2005 human rig and the same clips - definition 32 names the same stance and walk
		assertEquals(808, ardougne.getIdleAnimationId());
		assertEquals(819, ardougne.getWalkAnimationId());
		assertEquals(422, ardougne.getAttackAnimationId());
		assertEquals(424, ardougne.getDefendAnimationId());
		assertEquals(836, ardougne.getDeathAnimationId());

		// The variant and both females. Female guards are recent content with no 2005 counterpart,
		// so they take the male kit, as Varrock's and Falador's do.
		for (int id : new int[]{NpcID.ARDOUGNE_GUARD_VARIANT01, NpcID.ARDOUGNE_GUARD_F,
			NpcID.ARDOUGNE_GUARD_F_VARIANT01})
		{
			RetroNpcData variant = RetroNpcMapping.get(id, "Guard");
			assertNotNull("Ardougne guard " + id + " must map to data", variant);
			assertArrayEquals("Ardougne guard " + id + " must wear the Ardougne kit",
				new int[]{225, 301, 162, 179, 274, 185, 502}, variant.getRetroModelIds());
		}

		// The Carnillean mansion guards are the town's guards posted indoors: the same kit with the
		// weapon slot dropped, matching 2005 definition 887 exactly.
		for (int id : new int[]{NpcID.SOTN_GUARD_CARNILLEAN_UPSTAIRS, NpcID.GUARD_CARNILLEAN_VIS,
			NpcID.GUARD_CARNILLEAN_CUTSCENE})
		{
			RetroNpcData carnillean = RetroNpcMapping.get(id, "Guard");
			assertNotNull("Carnillean guard " + id + " must map to data", carnillean);
			assertArrayEquals(new int[]{225, 301, 162, 179, 274, 185}, carnillean.getRetroModelIds());
			assertTrue(carnillean.hasRecolors());
		}

		// The regression this whole change exists to prevent, in both directions: Varrock and
		// Falador keep definition 9's kit and its colors, and Ardougne never acquires them.
		RetroNpcData townGuard = RetroNpcMapping.get(NpcID.GUARD1, "Guard");
		assertNotNull(townGuard);
		assertArrayEquals(new int[]{233, 246, 294, 151, 176, 254, 185, 519, 541},
			townGuard.getRetroModelIds());
		assertArrayEquals("the town guard keeps its own 2005 colors",
			new short[]{25238, 8741, 61}, townGuard.getOriginalColors());
		assertArrayEquals(new short[]{10508, 6930, 5652}, townGuard.getReplacementColors());
		assertArrayEquals(new int[]{233, 246, 294, 151, 176, 254, 185, 519, 541},
			Objects.requireNonNull(RetroNpcMapping.get(NpcID.FAI_VARROCK_GUARD02, "Guard")).getRetroModelIds());

		// Deadman guards wear this kit too and are registered for no town at all - level 1337 with
		// no stance animation. Unreachable rather than excluded, the category resolving by id.
		assertNull(RetroNpcMapping.get(NpcID.DEADMAN_GUARD_ARDOUGNE_VIS, "Guard"));
		assertNull(RetroNpcMapping.get(NpcID.DEADMAN_GUARD_ARDOUGNE_RANGE_VIS, "Guard"));
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

	/**
	 * All four baby dragon colors share retro mesh 2998, whose palette is a greyscale ramp, so the
	 * recolor pair is the only thing separating them. A variant that lost its pair would render as
	 * a gray lump rather than fail, which is why the colors are asserted individually.
	 */
	@Test
	public void testBabyDragonsCategory()
	{
		int[][] variants = {
			{NpcID.BABYBLUEDRAGON, -25049},
			{NpcID.BABYREDDRAGON, 687},
			{NpcID.BABYGREENDRAGON1, 22051},
			{NpcID.CHICKENQUEST_BABY_BLACK_DRAGON, 16},
		};

		for (int[] variant : variants)
		{
			RetroNpcData baby = RetroNpcMapping.get(variant[0], "Baby dragon");
			assertNotNull("baby dragon " + variant[0] + " must be mapped", baby);
			assertEquals(RetroNpcCategory.BABY_DRAGONS, baby.getCategory());
			assertArrayEquals(new int[]{2998}, baby.getRetroModelIds());

			assertEquals(AnimationID.BDRAG_READY, baby.getIdleAnimationId());
			assertEquals(AnimationID.BDRAG_WALK, baby.getWalkAnimationId());

			// The cache cannot say what a modern baby dragon plays in a fight, so these stay unset
			// rather than being guessed at - see the branch in createMappingData
			assertEquals(-1, baby.getAttackAnimationId());
			assertEquals(-1, baby.getDefendAnimationId());
			assertEquals(-1, baby.getDeathAnimationId());

			assertTrue("baby dragon " + variant[0] + " must carry a recolor", baby.hasRecolors());
			assertArrayEquals("every 2005 dragon recolors the same body index",
				new short[]{61}, baby.getOriginalColors());
			assertArrayEquals(new short[]{(short) variant[1]}, baby.getReplacementColors());
		}

		// Resolving by name matters as much as by id: a color added or renamed upstream falls back
		// to the name, and the four must not collapse onto one shared instance
		assertEquals((short) -25049,
			Objects.requireNonNull(RetroNpcMapping.get(-1, "Baby blue dragon")).getReplacementColors()[0]);
		assertEquals((short) 687,
			Objects.requireNonNull(RetroNpcMapping.get(-1, "Baby red dragon")).getReplacementColors()[0]);
		assertEquals((short) 22051,
			Objects.requireNonNull(RetroNpcMapping.get(-1, "Baby green dragon")).getReplacementColors()[0]);
		assertEquals((short) 16,
			Objects.requireNonNull(RetroNpcMapping.get(-1, "Baby black dragon")).getReplacementColors()[0]);
	}

	/**
	 * The 2005 giant skeleton is def 93 - "Skeleton", level 45, the armed kit at scale 170. Live
	 * GIANTSKELETON shares that name, so without its id it resolved to the normal-size unarmed row.
	 */
	@Test
	public void testGiantSkeletons()
	{
		int[] giantIds = {
			NpcID.GIANTSKELETON, NpcID.GIANTSKELETON2,
			NpcID.SWORD_SKELETON_3, NpcID.SWORD_SKELETON_3B, NpcID.LOTR_GIANT_SKELETON
		};
		for (int id : giantIds)
		{
			for (String name : new String[]{"Skeleton", "Giant skeleton"})
			{
				RetroNpcData giant = RetroNpcMapping.get(id, name);
				assertNotNull("Giant skeleton ID " + id + " as '" + name + "' must be mapped", giant);
				assertEquals(RetroNpcCategory.SKELETONS, giant.getCategory());
				assertArrayEquals(new int[]{2944, 2946}, giant.getRetroModelIds());
				assertEquals(170, giant.getScaleXZ());
				assertEquals(170, giant.getScaleY());
				assertEquals(262, giant.getIdleAnimationId());
				assertEquals(259, giant.getWalkAnimationId());
				assertEquals(260, giant.getAttackAnimationId());
				assertEquals(261, giant.getDefendAnimationId());
				assertEquals(263, giant.getDeathAnimationId());
			}
		}

		// An unregistered "Giant skeleton" still reaches the giant by name
		RetroNpcData byName = RetroNpcMapping.get(99996, "Giant skeleton");
		assertNotNull(byName);
		assertEquals(170, byName.getScaleXZ());

		// The giant's own combat sequences are intercepted
		assertTrue(byName.isAttackAnimation(AnimationID.SKELETON_UPDATE_GIANT_ATTACK));
		assertTrue(byName.isDefendAnimation(AnimationID.SKELETON_UPDATE_GIANT_DEFEND));
		assertTrue(byName.isDeathAnimation(AnimationID.SKELETON_UPDATE_GIANT_DEATH));

		// Regular skeletons keep their size
		assertEquals(128, RetroNpcMapping.get(NpcID.SKELETON_UNARMED, "Skeleton").getScaleXZ());
		assertEquals(128, RetroNpcMapping.get(NpcID.SKELETON_ARMED, "Skeleton").getScaleXZ());
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
				NpcID.SKELETON_ARMED, NpcID.SKELETON_ARMED2, NpcID.SKELETON_ARMED3,
				NpcID.SKELETON_ARMED4, NpcID.SKELETON_ARMED5,
				NpcID.SKELETON_UNAGRESSIVE2, NpcID.SKELETON_UNAGRESSIVE3
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

	/**
	 * Black and greater demons are the same mesh (2942) and differ only by the 2005 opcode 40
	 * pairs, so a black demon without them renders in greater demon colors. The pairs reach it
	 * through a static archetype, which normally shadows the generated JSON row entirely.
	 */
	@Test
	public void testBlackDemonInheritsItsRecolorsFromTheCache()
	{
		RetroNpcData blackDemon = RetroNpcMapping.get(0, "Black demon");

		assertNotNull(blackDemon);
		assertTrue("the black demon archetype must pick up the JSON row's recolor pairs",
			blackDemon.hasRecolors());
		assertEquals("pairs must stay parallel",
			blackDemon.getOriginalColors().length, blackDemon.getReplacementColors().length);
	}

	/**
	 * The bundle carries mesh 2944 so the skinner can be checked against the client's own animation
	 * of it, and only its idle and walk clips - drawing a skeleton from the bundle would cost it
	 * every combat animation it has. Which path a category takes is therefore a decision the
	 * mapping makes, not one read off the bundle's contents.
	 */
	@Test
	public void testTheSkeletonIsNotDrawnFromTheBundle()
	{
		assertFalse("the skeleton is bundled to be measured, not to be drawn",
			RetroNpcMapping.usesInjectedGeometry(RetroNpcCategory.SKELETONS));
		assertTrue("hill giants have a cache-backed render, but a better head in the bundle",
			RetroNpcMapping.usesInjectedGeometry(RetroNpcCategory.HILL_GIANTS));
		assertFalse("and only the bundle can supply that head",
			RetroNpcMapping.requiresInjectedGeometry(RetroNpcCategory.HILL_GIANTS));
	}

	/**
	 * An equipment variant replaces the cache-backed parts only. Guards keep one list for both
	 * paths, so for them the new weapon reaches both - but a mapping that declares a distinct
	 * injected list has that list for a reason, and collapsing the two would inject a hill giant
	 * wearing the Jogre head that stands in for its missing one on the cache path alone.
	 */
	@Test
	public void testAnEquipmentVariantKeepsItsInjectedParts()
	{
		RetroNpcData variant = RetroNpcMapping.HILL_GIANT_DEFAULT.withModelIds(new int[]{2870, 2866, 4990});

		assertArrayEquals(new int[]{2870, 2866, 4990}, variant.getRetroModelIds());
		assertArrayEquals("the injected parts are not the cache-backed ones",
			new int[]{2870, 2862}, variant.getInjectedModelIds());
	}

	/**
	 * The resize is grafted the same way and for the same reason: 2005 asked for a greater demon at
	 * 110/128ths, that number lives only in the generated row, and the archetype that shadows the
	 * row is built before any of it is read. Without the graft the injected path scales by 128/128,
	 * which is no resize at all.
	 */
	@Test
	public void testGreaterDemonInheritsItsResizeFromTheCache()
	{
		RetroNpcData greaterDemon = RetroNpcMapping.get(NpcID.GREATER_DEMON, "Greater demon");

		assertNotNull(greaterDemon);
		assertEquals("the greater demon archetype must pick up the JSON row's resize",
			110, greaterDemon.getScaleXZ());
		assertEquals(110, greaterDemon.getScaleY());
	}

	/**
	 * A hand-corrected size is the archetype's own opinion and must survive the graft. The chicken
	 * is the case: 2005 asked for no resize, the modern composition shrinks its model, and 204 is
	 * the value that was measured against neither.
	 */
	@Test
	public void testAHandCorrectedResizeIsNotOverwritten()
	{
		RetroNpcData chicken = RetroNpcMapping.get(0, "Chicken");

		assertNotNull(chicken);
		assertEquals(204, chicken.getScaleXZ());
		assertEquals(204, chicken.getScaleY());
	}

	/**
	 * The same graft must reach every NPC id registered against the archetype, not just the name
	 * lookup - both maps hold the same instance, so replacing one and not the other would leave
	 * most black demons uncolored.
	 */
	@Test
	public void testBlackDemonRecolorsReachTheIdMappingsToo()
	{
		RetroNpcData byId = RetroNpcMapping.get(NpcID.BLACK_DEMON, "Black demon");

		assertNotNull(byId);
		assertTrue("id-resolved black demons must carry the recolors as well", byId.hasRecolors());
	}

	@Test
	public void testNoCategoryForwardsRecolorsByDefault()
	{
		assertFalse(Objects.requireNonNull(RetroNpcMapping.get(0, "Goblin")).hasRecolors());
		assertFalse(Objects.requireNonNull(RetroNpcMapping.get(0, "Restless ghost")).hasRecolors());
	}

	/**
	 * The 2005 Skeleton Mage is human kit tinted bone, not the skeleton mesh. Half its parts are
	 * gone from the live cache, so it has to be its own injected category rather than a SKELETONS
	 * row - and it needs the recolors, which are the only thing that make the kit a skeleton.
	 */
	@Test
	public void testSkeletonMageIsTheInjected2005Kit()
	{
		int[] mageIds = {
			NpcID.SKELETONMAGE, NpcID.UNATTACKABLE_SKELETON_MAGE,
			NpcID.SWAN_SKELETON_BATTLE, NpcID.SWAN_SKELETON_UNATTACKABLE, NpcID.SWAN_SKELETON_TRAINING
		};

		List<RetroNpcData> lookups = new ArrayList<>();
		lookups.add(RetroNpcMapping.get(0, "Skeleton Mage"));
		for (int id : mageIds)
		{
			lookups.add(RetroNpcMapping.get(id, "Skeleton Mage"));
			lookups.add(RetroNpcMapping.get(id, null));
		}

		for (RetroNpcData mage : lookups)
		{
			assertNotNull(mage);
			assertEquals(RetroNpcCategory.SKELETON_MAGES, mage.getCategory());
			assertArrayEquals(new int[]{209, 251, 292, 170, 256, 325}, mage.getRetroModelIds());
			assertEquals(AnimationID.HUMAN_READY, mage.getIdleAnimationId());
			assertEquals(AnimationID.HUMAN_WALK_F, mage.getWalkAnimationId());
			assertEquals(AnimationID.HUMAN_UNARMEDPUNCH, mage.getAttackAnimationId());

			// Two attack styles, each with its own 2005 sequence: melee swings punch, spells cast
			for (int melee : new int[]{AnimationID.SKELETON_UPDATE_ATTACK_WEAPON,
				AnimationID.SKELETON_UPDATE_ATTACK_SWORD, AnimationID.SKELETON_ATTACK})
			{
				assertEquals(AnimationID.HUMAN_UNARMEDPUNCH, mage.getAttackAnimationFor(melee));
			}
			for (int cast : new int[]{AnimationID.SKELETON_UPDATE_MAGE_CASTING,
				AnimationID.SKELETON_STRIKE_CASTING, AnimationID.SKELETON_UPDATE_MAGE_CASTING_SWANSONG})
			{
				assertTrue(mage.isAttackAnimation(cast));
				assertEquals(AnimationID.HUMAN_CASTSTRIKE, mage.getAttackAnimationFor(cast));
			}

			// Both 2005 attacks are recognised as the swap landing, so neither is re-intercepted
			assertTrue(mage.isRetroAttackAnimation(AnimationID.HUMAN_UNARMEDPUNCH));
			assertTrue(mage.isRetroAttackAnimation(AnimationID.HUMAN_CASTSTRIKE));
			assertEquals(-1, mage.getAttackAnimationFor(AnimationID.HUMAN_READY));
			assertEquals(AnimationID.HUMAN_UNARMEDBLOCK, mage.getDefendAnimationId());
			assertEquals(AnimationID.HUMAN_DEATH, mage.getDeathAnimationId());
			assertArrayEquals(new short[]{25238, 8741, 6798}, mage.getOriginalColors());
			assertArrayEquals(new short[]{10508, 10508, 10508}, mage.getReplacementColors());
		}

		assertTrue(RetroNpcMapping.requiresInjectedGeometry(RetroNpcCategory.SKELETON_MAGES));

		// The overrides must survive the recolor graft, which rebuilds the archetype
		RetroNpcData byId = RetroNpcMapping.get(NpcID.SKELETONMAGE, "Skeleton Mage");
		assertNotNull(byId);
		assertTrue(byId.hasRecolors());
		assertEquals(AnimationID.HUMAN_CASTSTRIKE,
			byId.getAttackAnimationFor(AnimationID.SKELETON_UPDATE_MAGE_CASTING));

		// The plain skeleton must stay on the cache path, with its own clips
		RetroNpcData skeleton = RetroNpcMapping.get(NpcID.SKELETON_UNARMED, "Skeleton");
		assertNotNull(skeleton);
		assertEquals(RetroNpcCategory.SKELETONS, skeleton.getCategory());
		assertFalse(RetroNpcMapping.requiresInjectedGeometry(RetroNpcCategory.SKELETONS));
	}

	/**
	 * The recolors have to reach the <em>id</em> lookup, not just the name one. Both maps hold the
	 * same instance and the graft replaces it, so checking only get(0, name) would pass while every
	 * guard resolved by id rendered in the base kit's colors.
	 */
	@Test
	public void testGuardRecolorsReachTheIdMappingsToo()
	{
		RetroNpcData byId = RetroNpcMapping.get(NpcID.GUARD1, "Guard");
		assertNotNull(byId);
		assertTrue("id-resolved guards must carry the recolors as well", byId.hasRecolors());
		assertArrayEquals(new short[]{25238, 8741, 61}, byId.getOriginalColors());
		assertArrayEquals(new short[]{10508, 6930, 5652}, byId.getReplacementColors());
	}

	@Test
	public void testGuardsDoCarryTheirRecolors()
	{
		// Guards are the exception to the rule above, and deliberately so. Their parts are generic
		// 2005 human kit shared with every other NPC that wears it, so without the opcode 40 pairs
		// a guard renders in a townsperson's colors. The pairs and the parts come from the same
		// definition, which is what makes this safe where forwarding a goblin variant's would not.
		RetroNpcData guard = RetroNpcMapping.get(NpcID.GUARD1, "Guard");
		assertNotNull(guard);
		assertTrue("guards must carry their 2005 recolors", guard.hasRecolors());
		assertEquals("recolor arrays must stay parallel",
			guard.getOriginalColors().length, guard.getReplacementColors().length);
	}

	/**
	 * The generator must still carry the pairs through to the JSON even though no category consumes
	 * them - otherwise the opt-in above would have nothing to opt into.
	 */
	@Test
	public void testGeneratedMappingsCarryRecolorPairs() throws Exception
	{
		List<RetroNpcMappingEntry> entries = loadCommittedEntries();

		RetroNpcMappingEntry blueDragon = entries.stream()
			.filter(e -> "blue dragon".equals(e.getName()))
			.findFirst()
			.orElse(null);

		assertNotNull("blue dragon row missing from npc-mappings.json", blueDragon);
		assertNotNull("blue dragon must carry its opcode 40 pairs", blueDragon.getOriginalColors());
		assertNotNull(blueDragon.getReplacementColors());
		assertEquals("recolor arrays must stay parallel",
			blueDragon.getOriginalColors().length, blueDragon.getReplacementColors().length);
	}

	@Test
	public void testGiantsCategory()
	{
		RetroNpcData hillGiant = RetroNpcMapping.get(0, "Hill giant");
		assertNotNull("Hill giant mapping must exist", hillGiant);
		assertEquals(RetroNpcCategory.HILL_GIANTS, hillGiant.getCategory());
		// The two paths take different heads on purpose: the real 2005 head 2862 no longer resolves
		// in the live cache, so only the bundle can supply it and the cache path wears a Jogre head
		assertArrayEquals(new int[]{2870, 2866}, hillGiant.getRetroModelIds());
		assertArrayEquals(new int[]{2870, 2862}, hillGiant.getInjectedModelIds());
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

	/**
	 * Both chickens are mesh 2849; the 2005 client told the undead one apart with opcode 40 alone.
	 * Chickens are the only recoloring category that builds from the live cache, so this is also
	 * the check that the pairs survive the graft for a cache-backed category - and that turning
	 * them on for the category did not repaint the ordinary bird, which carries no pairs at all.
	 */
	@Test
	public void testUndeadChickenKeepsIts2005Palette()
	{
		RetroNpcData undead = RetroNpcMapping.get(NpcID.AHOY_UNDEAD_CHICKEN, "Undead chicken");
		assertNotNull("Undead chicken mapping must exist", undead);
		assertEquals(RetroNpcCategory.CHICKENS, undead.getCategory());
		assertArrayEquals(new int[]{2849}, undead.getRetroModelIds());
		assertTrue("the undead chicken is only undead by its palette", undead.hasRecolors());
		assertArrayEquals(new short[]{127, 11200, 8394, 926, 6080}, undead.getOriginalColors());
		assertArrayEquals(new short[]{12480, 10566, 12475, 4771, 8101}, undead.getReplacementColors());

		assertFalse("the living chicken must stay the mesh's own colors",
			Objects.requireNonNull(RetroNpcMapping.get(0, "Chicken")).hasRecolors());
	}

	/**
	 * No evil chicken is swapped, and that is the point of this test. Live mesh 7728 is already the
	 * model it wore in August 2005, so there is nothing retro to restore; February 2005 - the cache
	 * this plugin is built from - has no evil chicken at all, so there is nothing to copy either.
	 * Every variant has to stay unmapped, including the two the name row would otherwise catch.
	 */
	@Test
	public void testEvilChickensAreLeftAlone()
	{
		for (int id : new int[]{
			NpcID.CHICKENQUEST_EVIL_CHICKEN,
			NpcID.NZONE_CHICKENQUEST_EVIL_CHICKEN_NORMAL,
			NpcID.EVIL_CHICKEN})
		{
			assertNull("evil chicken " + id + " must keep its own model",
				RetroNpcMapping.get(id, "Evil Chicken"));
		}

		assertNull(RetroNpcMapping.get(
			NpcID.NZONE_CHICKENQUEST_EVIL_CHICKEN_HARD, "Evil Chicken (hard)"));
		assertNull(RetroNpcMapping.get(NpcID.DEADMAN_BREACH_EVIL_CHICKEN, "Big Evil Chicken"));

		// and no name row may creep back in and catch a variant added later
		assertNull(RetroNpcMapping.get(0, "Evil Chicken"));
	}

	/**
	 * The rooster is the bird the 2005 palette belongs to, and nothing reached it before: the
	 * generator only categorizes names containing "chicken", so no rooster row exists for the name
	 * lookup to find. All three live variants have to resolve through the archetype - including
	 * Ernest's, which wears the same live mesh as the evil chicken but, unlike it, has a February
	 * 2005 definition of its own to go back to.
	 */
	@Test
	public void testRoosterResolvesForEveryLiveVariant()
	{
		for (int id : new int[]{NpcID.ROOSTER, NpcID.FARM_ROOSTER, NpcID.MISC_ROOSTER})
		{
			assertRooster("rooster " + id, RetroNpcMapping.get(id, "Rooster"));
		}
		assertRooster("the name row", RetroNpcMapping.get(0, "Rooster"));
	}

	/**
	 * Ernest's rooster fights on the rooster sequences, not the chicken ones. Uncaught they would
	 * play a modern clip on 2005 geometry, so each has to land in its own slot and in no other -
	 * the mistake {@code testZombieDeathVsFlinchAnimations} exists to catch.
	 */
	@Test
	public void testRoosterInterceptsItsOwnSequences()
	{
		RetroNpcData rooster = RetroNpcMapping.get(NpcID.FARM_ROOSTER, "Rooster");
		assertNotNull(rooster);

		assertTrue("ROOSTERATTACK must become the retro peck", rooster.isAttackAnimation(2299));
		assertTrue("ROOSTERPARRY must become the retro block", rooster.isDefendAnimation(2300));
		assertTrue("ROOSTERDEATH must become the retro death", rooster.isDeathAnimation(2301));

		// No sequence may register as more than one state
		assertFalse(rooster.isDeathAnimation(2299));
		assertFalse(rooster.isDefendAnimation(2299));
		assertFalse(rooster.isAttackAnimation(2301));
		assertFalse(rooster.isDefendAnimation(2301));
		assertFalse(rooster.isAttackAnimation(2300));
		assertFalse(rooster.isDeathAnimation(2300));

		// ROOSTERWALK and ROOSTERREADY are poses, replaced outright rather than intercepted; 5385 is
		// the modern chicken walk; ROOSTERMAGIC belongs to no mapped bird - none of the four is combat
		assertFalse(rooster.isAttackAnimation(2297));
		assertFalse(rooster.isAttackAnimation(2298));
		assertFalse(rooster.isAttackAnimation(5385));
		assertFalse(rooster.isAttackAnimation(2302));
	}

	/**
	 * A 2005 rooster: the chicken mesh on the chicken sequences, wearing def 1018's palette, which
	 * is the whole of what separates it from the hen, at the size that definition asked for.
	 */
	private static void assertRooster(String name, RetroNpcData data)
	{
		assertNotNull(name + " must resolve", data);
		assertEquals(name, RetroNpcCategory.CHICKENS, data.getCategory());
		assertArrayEquals(name, new int[]{2849}, data.getRetroModelIds());
		assertEquals(name, 54, data.getIdleAnimationId());
		assertEquals(name, 53, data.getWalkAnimationId());
		assertEquals(name, 55, data.getAttackAnimationId());
		assertEquals(name, 56, data.getDefendAnimationId());
		assertEquals(name, 57, data.getDeathAnimationId());

		assertTrue(name + " is only a rooster by its palette", data.hasRecolors());
		assertArrayEquals(name, new short[]{127, 11200, 8394, 61}, data.getOriginalColors());
		assertArrayEquals(name, new short[]{3998, 6720, 1942, 1942}, data.getReplacementColors());

		// 204 * 172/128 - the ratio def 1018 asked for against the hen
		assertEquals(name, 274, data.getScaleXZ());
		assertEquals(name, 274, data.getScaleY());
	}

	/**
	 * Cows resolve two different ways and both have to end up with the same six slots. Every cow
	 * but the undead one is an id-registered archetype built by {@code cow(...)}; the undead cow is
	 * the only one that reaches {@code createMappingData}. Asserting one path would pass while the
	 * other silently carried -1 for attack, defend, death and misc.
	 */
	@Test
	public void testCowsCategory()
	{
		RetroNpcData cow = RetroNpcMapping.get(NpcID.COW, "Cow");
		assertNotNull("Cow mapping must exist", cow);
		assertEquals(RetroNpcCategory.COWS, cow.getCategory());
		assertArrayEquals(new int[]{3341, 3342}, cow.getRetroModelIds());
		assertCowAnimations(cow);

		// The undead cow is its own 2005 mesh, and the only cow built from the generated row
		RetroNpcData undead = RetroNpcMapping.get(NpcID.AHOY_UNDEAD_COW, "Undead cow");
		assertNotNull("Undead cow mapping must exist", undead);
		assertEquals(RetroNpcCategory.COWS, undead.getCategory());
		assertArrayEquals(new int[]{5237}, undead.getRetroModelIds());
		assertCowAnimations(undead);
	}

	private static void assertCowAnimations(RetroNpcData cow)
	{
		assertEquals(61, cow.getIdleAnimationId());
		assertEquals(58, cow.getWalkAnimationId());
		assertEquals(59, cow.getAttackAnimationId());
		assertEquals(60, cow.getDefendAnimationId());
		assertEquals(62, cow.getDeathAnimationId());
		assertEquals(61, cow.getMiscAnimationId());

		assertTrue(cow.isAttackAnimation(59));
		assertTrue(cow.isAttackAnimation(5849));
		assertTrue(cow.isDefendAnimation(60));
		assertTrue(cow.isDefendAnimation(5850));
		assertTrue(cow.isDeathAnimation(62));
		assertTrue(cow.isDeathAnimation(5851));

		// The modern-only animations: no 2005 counterpart, and keyed to a framemap the retro mesh
		// is not rigged to, so they are redirected to the retro idle rather than left to bend it
		assertTrue(cow.isMiscAnimation(1735));
		assertTrue(cow.isMiscAnimation(5853));
		assertTrue(cow.isMiscAnimation(5854));
		assertTrue(cow.isMiscAnimation(5855));

		// 2162, 2303 and 2312 are legacy cow animations on framemap 282 - they fit the retro mesh
		// and must keep playing
		assertFalse(cow.isMiscAnimation(2162));
		assertFalse(cow.isMiscAnimation(2303));
		assertFalse(cow.isMiscAnimation(2312));
		assertFalse(cow.isAttackAnimation(5848));
	}

	/**
	 * The three 2005 cows are one mesh in three palettes, so each live variant has to come back
	 * with its own pairs - and every one of them has to start with the drift correction, without
	 * which the 2005 pairs miss the repainted hide and the cow renders white.
	 */
	@Test
	public void testCowVariantsCarryTheirOwnPalettes()
	{
		RetroNpcData white = RetroNpcMapping.get(NpcID.COW, "Cow");
		RetroNpcData brown = RetroNpcMapping.get(NpcID.COW2, "Cow");
		RetroNpcData grey = RetroNpcMapping.get(NpcID.COW3, "Cow");

		assertNotNull(white);
		assertNotNull(brown);
		assertNotNull(grey);
		assertTrue(white.hasRecolors());
		assertTrue(brown.hasRecolors());
		assertTrue(grey.hasRecolors());

		assertNotEquals("the 2005 cow variants differ only by palette", white, brown);
		assertNotEquals("the 2005 cow variants differ only by palette", brown, grey);

		// The 2005 definition's pairs verbatim. No palette-drift correction: the bundle carries the
		// 2005 meshes, so the indices the 2005 client recolored are the ones that are there - unlike
		// the live copies, whose hide was repainted 10363 -> 10365 across 135 faces
		assertArrayEquals(new short[]{26, 10363, 30}, white.getOriginalColors());
		assertArrayEquals(new short[]{10365, 5784, 10365}, white.getReplacementColors());

		// 2005 asked for this one slightly smaller; the hand-set size must survive the graft
		assertEquals(115, brown.getScaleXZ());
		assertEquals(115, brown.getScaleY());
	}

	/**
	 * February 2005 has no calf, so it is the cow mesh scaled down - which means it must still be
	 * the cow mesh, and must still be smaller than the cow.
	 */
	@Test
	public void testCowCalvesAreTheCowMeshScaledDown()
	{
		for (int calfId : new int[]{NpcID.COW2_CALF, NpcID.COW3_CALF, NpcID.CALF})
		{
			RetroNpcData calf = RetroNpcMapping.get(calfId, "Cow calf");
			assertNotNull("calf " + calfId + " must resolve", calf);
			assertEquals(RetroNpcCategory.COWS, calf.getCategory());
			assertArrayEquals(new int[]{3341, 3342}, calf.getRetroModelIds());
			assertEquals(68, calf.getScaleXZ());
			assertEquals(68, calf.getScaleY());
			assertCowAnimations(calf);
		}
	}

	/**
	 * Cows render from the bundle, not the live cache: both 2005 meshes are still at their own ids
	 * but their vertex groups were renumbered onto another rig, so the cache-backed path would
	 * animate the right geometry off the wrong joints. requiresInjectedGeometry is what stops it
	 * falling back to that path when the bundle is missing.
	 */
	@Test
	public void testCowsAreBundleOnly()
	{
		assertTrue("cows must not fall back to live geometry",
			RetroNpcMapping.requiresInjectedGeometry(RetroNpcCategory.COWS));
		assertTrue(RetroNpcMapping.usesInjectedGeometry(RetroNpcCategory.COWS));
	}

	/**
	 * "Cow" is a name the plugin matches on, so anything else wearing it has to be kept out by id.
	 * 10598 is a mount on animations 180/229, not a cow.
	 */
	@Test
	public void testNonCowsNamedCowDoNotSwap()
	{
		assertNull(RetroNpcMapping.get(NpcID.OSB8_COW, "Cow"));
	}

	/**
	 * The Zanaris and Nightmare Zone cows share the ordinary cow's look, and "Cow (hard)" does not
	 * match the name row at all - it reaches the mapping only through its registered id.
	 */
	@Test
	public void testCowVariantsOutsideLumbridgeResolve()
	{
		assertNotNull(RetroNpcMapping.get(NpcID.FAIRY_COW, "Cow"));
		assertNotNull(RetroNpcMapping.get(NpcID.NZONE_COW_NORMAL, "Cow"));
		assertNotNull(RetroNpcMapping.get(NpcID.NZONE_COW_HARD, "Cow (hard)"));
		assertNotNull(RetroNpcMapping.get(NpcID.ANMA_COW_CUTSCENE, "Undead cow"));
	}

	/**
	 * The 2005 goblin family is six definitions under one name, so every variety has to come back
	 * with its own part list and palette while still agreeing with {@code createMappingData} on all
	 * five animation slots - the archetypes supersede the generated row for "goblin", and a slot
	 * the factory forgot would read as -1 rather than fail loudly.
	 */
	@Test
	public void testGoblinVarietiesResolveByIdAndShareTheAnimations()
	{
		RetroNpcData green = RetroNpcMapping.get(NpcID.GOBLIN_GREENARMOUR, "Goblin");
		RetroNpcData red = RetroNpcMapping.get(NpcID.GOBLIN_REDARMOUR, "Goblin");
		RetroNpcData armed = RetroNpcMapping.get(NpcID.GOBLIN_ARMED, "Goblin");
		RetroNpcData strong = RetroNpcMapping.get(NpcID.GOBLIN_HELMET, "Goblin");
		RetroNpcData plain = RetroNpcMapping.get(NpcID.GOBLIN, "Goblin");

		for (RetroNpcData variety : new RetroNpcData[]{green, red, armed, strong, plain})
		{
			assertNotNull("every goblin variety must resolve", variety);
			// One category for the whole family, or get() would stop preferring the id over the
			// name row and every variety would collapse back into the default
			assertEquals(RetroNpcCategory.GOBLINS, variety.getCategory());
			assertGoblinAnimations(variety);
		}

		// Def 100 - the plain goblin is the four-part kit with no weapon
		assertArrayEquals(new int[]{2951, 2953, 2955, 2956}, plain.getRetroModelIds());
		// Def 101 - level 5, the same kit carrying 2957
		assertArrayEquals(new int[]{2951, 2953, 2955, 2956, 2957}, armed.getRetroModelIds());
		// Def 102 - "grown strong": the only variety that swaps the body mesh rather than a color
		assertArrayEquals(new int[]{2952, 2953, 2955, 2956}, strong.getRetroModelIds());
	}

	/**
	 * Green and red are the whole of the 2005 goblin's opcode 40 data, and the live cache is what
	 * pins each one down: GOBLIN_GREENARMOUR and GOBLIN_REDARMOUR still carry defs 298 and 299's
	 * pairs verbatim over the 2005 meshes themselves.
	 */
	@Test
	public void testGoblinVariantsCarryTheirOwnPalettes()
	{
		RetroNpcData green = RetroNpcMapping.get(NpcID.GOBLIN_GREENARMOUR, "Goblin");
		RetroNpcData red = RetroNpcMapping.get(NpcID.GOBLIN_REDARMOUR, "Goblin");
		RetroNpcData plain = RetroNpcMapping.get(NpcID.GOBLIN, "Goblin");

		assertTrue(green.hasRecolors());
		assertTrue(red.hasRecolors());
		assertNotEquals("the two colored goblins differ only by palette", green, red);

		// Def 298 and def 299's pairs, each followed by the live-to-2005 correction that gives the
		// weapon mesh 2957 its 2005 colors back
		assertArrayEquals(new short[]{916, 70, 8084}, green.getOriginalColors());
		assertArrayEquals(new short[]{22443, -22417, 528}, green.getReplacementColors());
		assertArrayEquals(new short[]{916, 70, 8084}, red.getOriginalColors());
		assertArrayEquals(new short[]{933, -22417, 528}, red.getReplacementColors());

		// The plain goblin carries no weapon, so it gets no correction and no pairs at all - its
		// armour is the mesh's own 916
		assertFalse(plain.hasRecolors());
	}

	/**
	 * 147 live NPCs are called exactly "Goblin" and only a handful are listed by id, so the name
	 * row carrying the rest is not a fallback of last resort - it is the main path. This is why
	 * GOBLINS stays out of ID_ONLY_CATEGORIES.
	 */
	@Test
	public void testUnlistedGoblinsFallBackToTheNameRow()
	{
		for (int npcId : new int[]{
			NpcID.CHAMPIONS_GOBLIN, NpcID.GOBLIN_UNARMED_MELEE_1,
			NpcID.GOBLIN_ARMED_MELEE_2, NpcID.GOBLIN_UNARMED_MELEE_IN_1})
		{
			RetroNpcData data = RetroNpcMapping.get(npcId, "Goblin");
			assertNotNull("goblin " + npcId + " must still swap by name", data);
			assertEquals(RetroNpcCategory.GOBLINS, data.getCategory());
			assertArrayEquals(new int[]{2951, 2953, 2955, 2956}, data.getRetroModelIds());
		}
	}

	/**
	 * The shield-and-spear goblins are a pose family of their own: GOBLIN_RED_SOLDIER_5 and
	 * GOBLIN_GREEN_SOLDIER_4 stand on 6200 and walk on 6201, where every other live goblin uses
	 * 6181 or 6186. Their attack, 6199, was the one goblin combat animation not intercepted, so
	 * both swung a modern animation on the retro mesh while everything else about them swapped.
	 */
	@Test
	public void testShieldSpearGoblinsAttackIsIntercepted()
	{
		for (int npcId : new int[]{NpcID.GOBLIN_RED_SOLDIER_5, NpcID.GOBLIN_GREEN_SOLDIER_4})
		{
			RetroNpcData goblin = RetroNpcMapping.get(npcId, "Goblin");
			assertNotNull(goblin);
			assertTrue("the shield-and-spear attack must be rewritten to the 2005 attack",
				goblin.isAttackAnimation(AnimationID.SLICE_SURFACE_GOBLIN_SQUAT_SPEAR_ATTACK_SHIELD));
			// Its ready and walk poses are replaced outright rather than intercepted, so they must
			// not also register as combat
			assertFalse(goblin.isAttackAnimation(AnimationID.SLICE_SURFACE_GOBLIN_SQUAT_SHIELD_SPEAR_READY));
			assertFalse(goblin.isDefendAnimation(AnimationID.SLICE_SURFACE_GOBLIN_SQUAT_WALK_SHIELD_ARMED));
		}
	}

	/**
	 * Armed is read off the live kit, not off the name: 12 of the goblin family's part meshes only
	 * ever appear in slot 6 or later and 66 only ever appear before it, with nothing in both. The
	 * names disagree with that in both directions, and the kit is what the player sees.
	 */
	@Test
	public void testArmedGoblinsAreChosenByKitNotByName()
	{
		// Named unarmed, holds a weapon
		for (int npcId : new int[]{NpcID.GOBLIN_UNARMED_MELEE_6, NpcID.GOBLIN_UNARMED_MELEE_IN_7})
		{
			RetroNpcData goblin = RetroNpcMapping.get(npcId, "Goblin");
			assertNotNull(goblin);
			assertArrayEquals("a goblin holding a weapon must carry 2957",
				new int[]{2951, 2953, 2955, 2956, 2957}, goblin.getRetroModelIds());
		}

		// Named armed, holds nothing
		for (int npcId : new int[]{NpcID.GOBLIN_ARMED_MELEE_2, NpcID.GOBLIN_ARMED_MELEE_4})
		{
			RetroNpcData goblin = RetroNpcMapping.get(npcId, "Goblin");
			assertNotNull(goblin);
			assertArrayEquals("a goblin holding nothing must not be handed a weapon",
				new int[]{2951, 2953, 2955, 2956}, goblin.getRetroModelIds());
		}
	}

	/**
	 * The two cutscene goblins are registered by id and are named after how each one dies, so their
	 * scripted deaths have to be intercepted like any other.
	 */
	@Test
	public void testCutsceneGoblinDeathsAreIntercepted()
	{
		RetroNpcData arrow = RetroNpcMapping.get(NpcID.SLICE_CUTSCENE_ARROW_GOBLIN, "Goblin");
		RetroNpcData firebolt = RetroNpcMapping.get(NpcID.SLICE_CUTSCENE_FIREBOLT_GOBLIN, "Goblin");
		assertNotNull(arrow);
		assertNotNull(firebolt);
		assertTrue(arrow.isDeathAnimation(AnimationID.SLICE_SURFACE_GOBLIN_DEATH_BY_ARROW));
		assertTrue(firebolt.isDeathAnimation(AnimationID.SLICE_SURFACE_GOBLIN_DEATH_BY_FIREBOLT));
		// Wormbrain is not called "Goblin" and reaches no mapping, so its death stays out
		assertFalse(arrow.isDeathAnimation(AnimationID.SURFACE_GOBLIN_WORMBRAIN_DEATH));
	}

	/**
	 * Every NPC in Mr. Mordaut's Surprise Exam classroom is seated at a desk, and a 2005 standing
	 * kit would replace it.
	 *
	 * <p>"Goblin" and "Zombie" are names the plugin matches on, so those three are kept out by id
	 * - the desk goblins are one merged mesh, size 2, and never walk. The other four carry the
	 * names the live cache gives them (Giant, Mummy, Mr. Mordaut, Dunce), none of which is a name
	 * row, and this test keeps it that way if a row such as a bare "giant" is ever added.
	 */
	@Test
	public void testSurpriseExamClassroomDoesNotSwap()
	{
		assertNull(RetroNpcMapping.get(NpcID.PATTERN_ZOMBIE_DESK, "Zombie"));
		assertNull(RetroNpcMapping.get(NpcID.PATTERN_GOBLIN1_DESK, "Goblin"));
		assertNull(RetroNpcMapping.get(NpcID.PATTERN_GOBLIN2_DESK, "Goblin"));
		assertNull(RetroNpcMapping.get(NpcID.PATTERN_GIANT_DESK, "Giant"));
		assertNull(RetroNpcMapping.get(NpcID.PATTERN_MUMMY_DESK, "Mummy"));
		assertNull(RetroNpcMapping.get(NpcID.PATTERN_TEACHER, "Mr. Mordaut"));
		assertNull(RetroNpcMapping.get(NpcID.PATTERN_IMP_DUNCE, "Dunce"));

		// Scoped to the desk ids: an ordinary zombie still swaps by name
		assertNotNull(RetroNpcMapping.get(0, "Zombie"));
	}

	/**
	 * The goblin guard and the hobgoblin share the goblin's category but not its archetype: the
	 * guard is the armed kit, and the hobgoblin is its own mesh on its own poses, still coming from
	 * the generated row. Proves the goblin archetypes did not swallow the sibling names.
	 */
	@Test
	public void testGoblinGuardAndHobgoblinKeepTheirOwnKit()
	{
		RetroNpcData guard = RetroNpcMapping.get(NpcID.GOBLIN_GUARD, "Goblin guard");
		assertNotNull(guard);
		assertEquals(RetroNpcCategory.GOBLINS, guard.getCategory());
		assertArrayEquals(new int[]{2951, 2953, 2955, 2956, 2957}, guard.getRetroModelIds());
		assertGoblinAnimations(guard);

		RetroNpcData hobgoblin = RetroNpcMapping.get(0, "Hobgoblin");
		assertNotNull(hobgoblin);
		assertEquals(RetroNpcCategory.GOBLINS, hobgoblin.getCategory());
		assertArrayEquals(new int[]{2994}, hobgoblin.getRetroModelIds());
		// Its own 2005 poses, not the goblin's
		assertEquals(AnimationID.HOBGOBLIN_READY, hobgoblin.getIdleAnimationId());
		assertEquals(AnimationID.HOBGOBLIN_WALK, hobgoblin.getWalkAnimationId());
	}

	/**
	 * Every goblin, however it resolved, plays the same 2005 sequences - the archetypes and
	 * createMappingData have to agree slot for slot.
	 */
	private static void assertGoblinAnimations(RetroNpcData goblin)
	{
		assertEquals(AnimationID.GOBLIN_READY, goblin.getIdleAnimationId());
		assertEquals(AnimationID.GOBLIN_WALK, goblin.getWalkAnimationId());
		assertEquals(AnimationID.GOBLIN_ATTACK_UNARMED, goblin.getAttackAnimationId());
		assertEquals(AnimationID.GOBLIN_BLOCK, goblin.getDefendAnimationId());
		assertEquals(AnimationID.GOBLIN_DEATH, goblin.getDeathAnimationId());

		assertTrue(goblin.isAttackAnimation(6184));
		assertTrue(goblin.isAttackAnimation(6154));
		assertTrue(goblin.isDefendAnimation(6183));
		assertTrue(goblin.isDeathAnimation(6182));
		assertFalse(goblin.isAttackAnimation(6186));
	}

	@Test
	public void testTheRestOfTheGiantFamily()
	{
		// One shared 2005 body with a variant head - the arrangement that made a bundle keyed by the
		// first model id unworkable, since all five collided on 2870
		assertGiant("Fire giant", RetroNpcCategory.FIRE_GIANTS, new int[]{2870, 2864, 4991, 4990});
		assertGiant("Ice giant", RetroNpcCategory.ICE_GIANTS, new int[]{2870, 2868});
		assertGiant("Moss giant", RetroNpcCategory.MOSS_GIANTS, new int[]{2870, 2865, 4990});
		assertGiant("Cyclops", RetroNpcCategory.CYCLOPS, new int[]{2870, 2867});

		int[][] idsByName = {
			{NpcID.FIREGIANT, NpcID.FIREGIANT_BIG, NpcID.FIREGIANT_STRONGHOLDCAVE_1, NpcID.KOUREND_FIREGIANT1},
			{NpcID.ICEGIANT, NpcID.ICEGIANT_LOW_WANDERRANGE, NpcID.WILD_CAVE_ICEGIANT},
			{NpcID.MOSSGIANT, NpcID.ROVING_MOSSGIANT, NpcID.PRIF_MOSSGIANT, NpcID.GB_MOSSGIANT},
			{NpcID.CYCLOPS, NpcID.WARGUILD_CYCLOPS1, NpcID.WARGUILD_CYCLOPS6_HIGH, NpcID.KOUREND_CYCLOPS2}};
		RetroNpcCategory[] categories = {
			RetroNpcCategory.FIRE_GIANTS, RetroNpcCategory.ICE_GIANTS,
			RetroNpcCategory.MOSS_GIANTS, RetroNpcCategory.CYCLOPS};

		for (int i = 0; i < idsByName.length; i++)
		{
			for (int id : idsByName[i])
			{
				RetroNpcData byId = RetroNpcMapping.get(id, "unused");
				assertNotNull("NPC id " + id + " must map", byId);
				assertEquals(categories[i], byId.getCategory());
			}
		}
	}

	private static void assertGiant(String name, RetroNpcCategory category, int[] models)
	{
		RetroNpcData data = RetroNpcMapping.get(0, name);
		assertNotNull(name + " mapping must exist", data);
		assertEquals(category, data.getCategory());
		assertArrayEquals(name + " parts", models, data.getRetroModelIds());

		// Nothing but the hill giant needs a per-path split, so both lists agree
		assertArrayEquals(name + " injected parts", models, data.getInjectedModelIds());

		// The whole family animates off the same five sequences, which is why they all resolve to
		// framemap 302
		assertEquals(130, data.getIdleAnimationId());
		assertEquals(127, data.getWalkAnimationId());
		assertEquals(128, data.getAttackAnimationId());
		assertEquals(129, data.getDefendAnimationId());
		assertEquals(131, data.getDeathAnimationId());

		// The whole family reuses GIANT_MODERN_*, so the post-2006 rework animations are intercepted
		assertTrue(name + " must intercept the reworked attacks", data.isAttackAnimation(4652));
		assertTrue(name + " must intercept the reworked defends", data.isDefendAnimation(4651));
		assertTrue(name + " must intercept the reworked deaths", data.isDeathAnimation(4653));

		// Unrelated sequences are left alone
		assertFalse(name + " must not intercept an unrelated attack", data.isAttackAnimation(5385));
	}

	@Test
	public void testFireIceAndMossGiantsCarryTheir2005Recolors()
	{
		// These three are the same body mesh as the hill giant, told apart only by opcode 40. Without
		// the pairs they would all render in hill giant colors.
		for (String name : new String[]{"Fire giant", "Ice giant", "Moss giant"})
		{
			RetroNpcData data = RetroNpcMapping.get(0, name);
			assertNotNull(name, data);
			assertTrue(name + " must carry 2005 recolors", data.hasRecolors());
			assertEquals(name + " recolor arrays must stay parallel",
				data.getOriginalColors().length, data.getReplacementColors().length);
		}

		// The recolors have to reach the id lookup too - ID_MAPPINGS and NAME_MAPPINGS hold the
		// same instance, so a graft that updated only one would leave most fire giants uncolored
		RetroNpcData byId = RetroNpcMapping.get(NpcID.FIREGIANT, "Fire giant");
		assertNotNull(byId);
		assertTrue("recolors must reach the id mapping", byId.hasRecolors());
	}

	@Test
	public void testCategoryMatchingExclusions()
	{
		// A bare "giant" substring would sweep all of these into the giant family
		assertNull(RetroNpcMapping.get(0, "Giant rat"));
		assertNull(RetroNpcMapping.get(0, "Giant spider"));
		assertNull(RetroNpcMapping.get(0, "Giant frog"));
		assertNull(RetroNpcMapping.get(0, "Giant bat"));

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

		// Every category that renders without the bundle defaults to on
		assertTrue("swapChickens must default to true", config.swapChickens());
		assertTrue("swapGoblins must default to true", config.swapGoblins());
		assertTrue("swapSkeletons must default to true", config.swapSkeletons());
		assertTrue("swapZombies must default to true", config.swapZombies());
		assertTrue("swapGiants must default to true", config.swapGiants());
		assertTrue("swapGhosts must default to true", config.swapGhosts());
		assertTrue("swapHellhounds must default to true", config.swapHellhounds());

		// The pipeline toggle carries the six bundle-only categories, so its default decides
		// whether they can render at all - see isCategoryEnabled
		assertTrue("useInjectionPipeline must default to true", config.useInjectionPipeline());

		// The bundle-only categories ship on as well. They are not individually opt-in: what the
		// plugin distributes is gated once, by useInjectionPipeline above, and turning that off
		// leaves every one of these unable to render whatever its own toggle says.
		assertTrue("swapDragons must default to true", config.swapDragons());
		assertTrue("swapDemons must default to true", config.swapDemons());
		assertTrue("swapImps must default to true", config.swapImps());
		assertTrue("swapCyclops must default to true", config.swapCyclops());
		assertTrue("swapGuards must default to true", config.swapGuards());

		assertFalse("overrideInteractHighlight must default to false",
			config.overrideInteractHighlight());
	}

	/**
	 * The 2005 hellhound is def 49, mesh 2997 on sequences 157-161, all of which survive in the live
	 * cache - so it is a cache-path category, and none of the retro sequences may be intercepted.
	 */
	@Test
	public void testHellhounds()
	{
		int[] ids = {
			NpcID.HELLHOUND, NpcID.HELLHOUND_STRONGHOLDCAVE, NpcID.POH_HELLHOUND,
			NpcID.KOUREND_HELLHOUND, NpcID.WILD_CAVE_HELL_HOUND, NpcID.GODWARS_ANCIENT_HELLHOUND
		};
		for (int id : ids)
		{
			RetroNpcData hound = RetroNpcMapping.get(id, "Hellhound");
			assertNotNull("Hellhound ID " + id + " must be mapped", hound);
			assertEquals(RetroNpcCategory.HELLHOUNDS, hound.getCategory());
			assertArrayEquals(new int[]{2997}, hound.getRetroModelIds());
			assertEquals(160, hound.getIdleAnimationId());
			assertEquals(157, hound.getWalkAnimationId());
			assertEquals(158, hound.getAttackAnimationId());
			assertEquals(159, hound.getDefendAnimationId());
			assertEquals(161, hound.getDeathAnimationId());
			assertEquals(160, hound.getMiscAnimationId());

			int scale = id == NpcID.GODWARS_ANCIENT_HELLHOUND ? 90 : 128;
			assertEquals(scale, hound.getScaleXZ());
			assertEquals(scale, hound.getScaleY());
		}

		// An unregistered hellhound still reaches the archetype by name
		RetroNpcData byName = RetroNpcMapping.get(99995, "Hellhound");
		assertNotNull(byName);
		assertEquals(RetroNpcCategory.HELLHOUNDS, byName.getCategory());

		// The modern dog-rig sequences are intercepted
		assertTrue(byName.isAttackAnimation(AnimationID.DOG_UPDATE_MEDIUM_DOG_ATTACK));
		assertTrue(byName.isAttackAnimation(AnimationID.DOG_UPDATE_HELLHOUND_GODWARS_ATTACK));
		assertTrue(byName.isDefendAnimation(AnimationID.DOG_UPDATE_MEDIUM_DOG_DEFEND));
		assertTrue(byName.isDeathAnimation(AnimationID.DOG_UPDATE_MEDIUM_DOG_DEATH));
		assertTrue(byName.isMiscAnimation(AnimationID.DOG_UPDATE_HELLHOUND_GODWARS_DEATH_REVERSE));

		// The surviving 2005 sequences are the targets, never things to intercept, and the torch is
		// not on the dog rig
		for (int retro : new int[]{157, 158, 159, 160, 161})
		{
			assertFalse(byName.isAttackAnimation(retro));
			assertFalse(byName.isDefendAnimation(retro));
			assertFalse(byName.isDeathAnimation(retro));
			assertFalse(byName.isMiscAnimation(retro));
		}
		assertFalse(byName.isMiscAnimation(AnimationID.HELLHOUND_TORCH));

		// Cache path: the mesh and rig both survive
		assertFalse(RetroNpcMapping.requiresInjectedGeometry(RetroNpcCategory.HELLHOUNDS));
		assertFalse(RetroNpcMapping.usesInjectedGeometry(RetroNpcCategory.HELLHOUNDS));

		// Other hounds are left where they were
		assertNull(RetroNpcMapping.get(NpcID.WILD_CAVE_HELLHOUND, "Revenant hellhound"));
		assertNull(RetroNpcMapping.get(NpcID.ARCEUUS_REANIMATED_HELLHOUND, "Reanimated hellhound"));
	}

	/**
	 * The skeleton hellhound is 2005 def 1575 - mesh 4974 on sequences 1493-1497, both preserved -
	 * and belongs to the Hellhounds toggle, not the generated SKELETONS row it used to fall into.
	 */
	@Test
	public void testSkeletonHellhounds()
	{
		Object[][] cases = {
			{NpcID.SKELETON_HELLHOUND, "Skeleton Hellhound"},
			{NpcID.NZONE_SKELETON_HELLHOUND_NORMAL, "Skeleton Hellhound"},
			{NpcID.NZONE_SKELETON_HELLHOUND_HARD, "Skeleton Hellhound (hard)"},
			{99994, "Skeleton Hellhound"}
		};
		for (Object[] c : cases)
		{
			RetroNpcData hound = RetroNpcMapping.get((Integer) c[0], (String) c[1]);
			assertNotNull("Skeleton hellhound " + c[0] + " must be mapped", hound);
			assertEquals(RetroNpcCategory.HELLHOUNDS, hound.getCategory());
			assertArrayEquals(new int[]{4974}, hound.getRetroModelIds());
			assertEquals(1494, hound.getIdleAnimationId());
			assertEquals(1493, hound.getWalkAnimationId());
			assertEquals(1495, hound.getAttackAnimationId());
			assertEquals(1496, hound.getDefendAnimationId());
			assertEquals(1497, hound.getDeathAnimationId());
			assertEquals(256, hound.getScaleXZ());
			assertEquals(256, hound.getScaleY());
			assertArrayEquals(new short[]{10318}, hound.getOriginalColors());
			assertArrayEquals(new short[]{11714}, hound.getReplacementColors());

			assertTrue(hound.isAttackAnimation(AnimationID.DOG_UPDATE_SKELETON_HELLHOUND_ATTACK));
			assertTrue(hound.isDefendAnimation(AnimationID.DOG_UPDATE_SKELETON_HELLHOUND_DEFEND));
			assertTrue(hound.isDeathAnimation(AnimationID.DOG_UPDATE_MEDIUM_DOG_DEATH));
			for (int retro : new int[]{1493, 1494, 1495, 1496, 1497})
			{
				assertFalse(hound.isAttackAnimation(retro));
				assertFalse(hound.isDefendAnimation(retro));
				assertFalse(hound.isDeathAnimation(retro));
			}
		}

		// Vet'ion's summons share the name and are deliberately left alone
		assertNull(RetroNpcMapping.get(NpcID.VETION_HELLHOUND_JNR, "Skeleton Hellhound"));
		assertNull(RetroNpcMapping.get(NpcID.VETION_HELLHOUND_JNR_SINGLES, "Skeleton Hellhound"));
		assertNull(RetroNpcMapping.get(NpcID.VETION_HELLHOUND_SNR, "Greater Skeleton Hellhound"));
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
