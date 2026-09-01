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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;

/**
 * Registry class that maps Modern NPC IDs and Names to Retro (2004/2005) model & animation data,
 * with Category-Scoped modern animation sets.
 */
public class RetroNpcMapping
{
	private static final Map<Integer, RetroNpcData> ID_MAPPINGS = new HashMap<>();
	private static final Map<String, RetroNpcData> NAME_MAPPINGS = new HashMap<>();

	// Category-Scoped Modern Animation Sets. Values are gameval AnimationID constants where the
	// modern cache has them; the retro 2005 sequence IDs used elsewhere in this class have no
	// gameval names and stay numeric.
	//
	// Membership is curated against the gameval names as the source of truth: the role word in
	// the name (attack/defend/block/death) decides which set an ID belongs to, and IDs whose
	// names carry no combat role (walks, readys, idles, chatheads) were dropped - they would
	// never correctly trigger a combat-animation swap.
	public static final Set<Integer> DEMON_MODERN_ATTACKS = Set.of(
		AnimationID.DEMON_ATTACK, AnimationID.DEMON_ATTACK_GREATER,
		AnimationID.DEMON_UPDATE_ATTACK, AnimationID.DEMON_UPDATE_ATTACK_GREATER,
		AnimationID.DEMON_UPDATE_ATTACK_LESSER, AnimationID.DEMONS_ATTACK
	);
	public static final Set<Integer> DEMON_MODERN_DEFENDS = Set.of(
		AnimationID.DEMON_UPDATE_DEFEND,
		AnimationID.DEMON_BLOCK
	);
	public static final Set<Integer> DEMON_MODERN_DEATHS = Set.of(
		AnimationID.DEMON_DEATH, AnimationID.DEMON_DEATH_GREATER,
		AnimationID.DEMONS_DEATH
	);

	public static final Set<Integer> DRAGON_MODERN_ATTACKS = Set.of(
		AnimationID.DRAGON_ATTACK,
		AnimationID.DRAGON_RANGED_ATTACKS, AnimationID.DRAGON_HEAD_ATTACK,
		// I'm assuming these are KBD animations
		AnimationID.DRAGON_FIREBREATH_ALL_ATTACK, AnimationID.DRAGON_FIREBREATH_LEFT_ATTACK,
		AnimationID.DRAGON_FIREBREATH_RIGHT_ATTACK
	);
	public static final Set<Integer> DRAGON_MODERN_DEFENDS = Set.of(
		AnimationID.DRAGON_BLOCK, AnimationID.DRAGON_BLOCK_KBD
	);
	public static final Set<Integer> DRAGON_MODERN_DEATHS = Set.of(
		AnimationID.DRAGON_DEATH
	);

	public static final Set<Integer> GOBLIN_MODERN_ATTACKS = Set.of(
		AnimationID.SLICE_SURFACE_GOBLIN_SQUAT_UNARMED_ATTACK, AnimationID.SLICE_SURFACE_GOBLIN_ARMED_ATTACK,
			AnimationID.SLICE_SURFACE_GOBLIN_SQUAT_ATTACK_SPEAR, AnimationID.SLICE_SURFACE_GOBLIN_SERGENT_ATTACK,
			AnimationID.GOBLIN_ATTACK_UNARMED, AnimationID.GOBLIN_ATTACK_ARMED
	);
	public static final Set<Integer> GOBLIN_MODERN_DEFENDS = Set.of(
		AnimationID.SLICE_SURFACE_GOBLIN_DEFEND, AnimationID.GOBLIN_BLOCK,
			AnimationID.SLICE_SURFACE_GOBLIN_DEFEND_SPEAR, AnimationID.SLICE_SURFACE_GOBLIN_SERGENT_DEFEND
	);
	public static final Set<Integer> GOBLIN_MODERN_DEATHS = Set.of(
		AnimationID.SLICE_SURFACE_GOBLIN_DEATH, AnimationID.SLICE_SURFACE_GOBLIN_DEATH_SPEAR,
			AnimationID.SLICE_ARROW_DEATH, AnimationID.GOBLIN_DEATH, AnimationID.SLICE_SURFACE_GOBLIN_SERGENT_DEATH
	);

	public static final Set<Integer> GUARD_MODERN_ATTACKS = Set.of(
		AnimationID.HUMAN_UNARMEDPUNCH, AnimationID.HUMAN_UNARMEDKICK
	);
	public static final Set<Integer> GUARD_MODERN_DEFENDS = Set.of(
		AnimationID.HUMAN_UNARMEDBLOCK
	);
	public static final Set<Integer> GUARD_MODERN_DEATHS = Set.of(
		AnimationID.HUMAN_DEATH
	);

	public static final Set<Integer> IMP_MODERN_ATTACKS = Set.of(
		AnimationID.IMP_ATTACK
	);
	public static final Set<Integer> IMP_MODERN_DEFENDS = Set.of(
		AnimationID.IMP_BLOCK
	);
	public static final Set<Integer> IMP_MODERN_DEATHS = Set.of(
		AnimationID.IMP_DEATH
	);

	public static final Set<Integer> SKELETON_MODERN_ATTACKS = Set.of(
		AnimationID.SKELETON_ATTACK, AnimationID.HUMAN_SWORD_SLASH, AnimationID.HUMAN_BLUNT_SPIKE,
		AnimationID.HUMAN_BLUNT_POUND, AnimationID.HUMAN_STAFF_SPIKE, AnimationID.HUMAN_STAFF_PUMMEL,
		AnimationID.HUMAN_UNARMEDPUNCH, AnimationID.HUMAN_UNARMEDKICK, AnimationID.HUMAN_BOW,
		AnimationID.HUMAN_SPEAR_SPIKE, AnimationID.HUMAN_SCYTHE_SWEEP,
		AnimationID.SKELETON_UPDATE_ATTACK_WEAPON, AnimationID.SKELETON_UPDATE_ATTACK_WEAPON_TRANSPARENT,
		AnimationID.SKELETON_UPDATE_ATTACK_SWORD, AnimationID.SKELETON_UPDATE_ATTACK_SWORD_TRANSPARENT,
		AnimationID.SKELETON_UPDATE_GIANT_VARY3_ATTACK, AnimationID.SKELETON_UPDATE_CHAMPION_ATTACK
	);
	public static final Set<Integer> SKELETON_MODERN_DEFENDS = Set.of(
		AnimationID.SKELETON_BLOCK,
		AnimationID.HUMAN_UNARMEDBLOCK, AnimationID.HUMAN_UNARMED_DEF, AnimationID.HUMAN_SHIELD_DEFENCE,
		AnimationID.SKELETON_UPDATE_DEFEND, AnimationID.SKELETON_UPDATE_DEFEND_TRANSPARENT,
		AnimationID.SKELETON_UPDATE_GIANT_VARY3_DEFEND
	);
	public static final Set<Integer> SKELETON_MODERN_DEATHS = Set.of(
		 AnimationID.SKELETON_DEATH, AnimationID.HUMAN_DEATH,
		AnimationID.SKELETON_UPDATE_DEATH, AnimationID.SKELETON_UPDATE_DEATH_TRANSPARENT,
		AnimationID.SKELETON_UPDATE_GIANT_VARY3_DEATH,
		AnimationID.GODWARS_GOBLIN_UPDATE_BANNER_DEATH
	);

	public static final Set<Integer> ZOMBIE_MODERN_ATTACKS = Set.of(
		AnimationID.ZOMBIE_UPDATE_ATTACK_NORMAL, AnimationID.ZOMBIE_UPDATE_ATTACK_WEAPON,
		AnimationID.ZOMBIE_UPDATE_ATTACK_DRAGGING, AnimationID.ZOMBIE_UPDATE_ATTACK_CHAMPION,
		AnimationID.HUMAN_UNARMEDPUNCH, AnimationID.HUMAN_UNARMEDKICK, AnimationID.HUMAN_STAFF_SPIKE,
		AnimationID.SKELETON_UPDATE_CHAMPION_ATTACK, AnimationID.ZOMBIE_ATTACK
	);
	public static final Set<Integer> ZOMBIE_MODERN_DEFENDS = Set.of(
		AnimationID.ZOMBIE_UPDATE_DEFEND_NORMAL, AnimationID.ZOMBIE_UPDATE_DEFEND_WEAPON,
		AnimationID.ZOMBIE_UPDATE_DEFEND_DRAGGING,
		AnimationID.HUMAN_UNARMEDBLOCK, AnimationID.HUMAN_UNARMED_DEF, AnimationID.HUMAN_SHIELD_DEFENCE,
		AnimationID.ZOMBIE_BLOCK
	);
	public static final Set<Integer> ZOMBIE_MODERN_DEATHS = Set.of(
		AnimationID.ZOMBIE_UPDATE_DEATH_NORMAL, AnimationID.ZOMBIE_UPDATE_DEATH_WEAPON,
		AnimationID.ZOMBIE_UPDATE_DEATH_DRAGGING, AnimationID.ZOMBIE_UPDATE_DESPAWN,
		AnimationID.HUMAN_DEATH, AnimationID.ZOMBIE_DEATH
	);

	public static final Set<Integer> GHOST_MODERN_ATTACKS = Set.of(
		AnimationID.GHOST_ATTACK, AnimationID.GHOST_UPDATE_NORMAL_ATTACK,
			AnimationID.BOSSGHOST_ATTACK
	);
	public static final Set<Integer> GHOST_MODERN_DEFENDS = Set.of(
		AnimationID.GHOST_BLOCK, AnimationID.GHOST_UPDATE_NORMAL_DEFEND,
			AnimationID.BOSSGHOST_FADE_OUT
	);
	public static final Set<Integer> GHOST_MODERN_DEATHS = Set.of(
		AnimationID.GHOST_DEATH, AnimationID.GHOST_UPDATE_NORMAL_DEATH,
			AnimationID.BOSSGHOST_DEATH
	);

	public static final Set<Integer> GIANT_MODERN_ATTACKS = Set.of(
		AnimationID.GIANT_UPDATE_BASIC_ATTACK, AnimationID.GIANT_UPDATE_MOSS_ATTACK,
		AnimationID.GIANT_UPDATE_FIRE_ATTACK, AnimationID.GIANT_UPDATE_FIRE_SWORD_ATTACK,
		AnimationID.GIANT_UPDATE_ICE_ATTACK, AnimationID.GIANT_ATTACK
	);
	public static final Set<Integer> GIANT_MODERN_DEFENDS = Set.of(
		AnimationID.GIANT_UPDATE_BASIC_DEFEND, AnimationID.GIANT_UPDATE_MOSS_DEFEND,
		AnimationID.GIANT_UPDATE_FIRE_SWORD_DEFEND, AnimationID.GIANT_UPDATE_ICE_DEFEND,
		AnimationID.GIANT_BLOCK
	);
	public static final Set<Integer> GIANT_MODERN_DEATHS = Set.of(
		AnimationID.GIANT_UPDATE_BASIC_DEATH, AnimationID.GIANT_UPDATE_MOSS_DEATH,
		AnimationID.GIANT_UPDATE_FIRE_DEATH, AnimationID.GIANT_UPDATE_ICE_DEATH,
		AnimationID.GIANT_DEATH
	);

	public static final Set<Integer> CHICKEN_MODERN_ATTACKS = Set.of(
		AnimationID.LORE_CHICKEN_ATTACK, AnimationID.CHICKEN_ATTACK
	);
	public static final Set<Integer> CHICKEN_MODERN_DEFENDS = Set.of(
		AnimationID.LORE_CHICKEN_DEFEND, AnimationID.CHICKEN_BLOCK
	);
	public static final Set<Integer> CHICKEN_MODERN_DEATHS = Set.of(
		AnimationID.LORE_CHICKEN_DEATH, AnimationID.CHICKEN_DEATH
	);

	// Pre-instantiated immutable archetypes.
	//
	// Archetypes for categories outside RetroNpcSwapperPlugin.isCategoryEnabled (demons, imps,
	// guards, dragons, ghosts) are currently inert: their mappings resolve but processNpc never
	// activates them. They are kept, along with their JSON entries, as staged data for when the
	// blocking issues resolve (missing 2005 animations, incomplete multi-part models).
	public static final RetroNpcData LESSER_DEMON_DEFAULT = RetroNpcData.builder()
		.category(RetroNpcCategory.LESSER_DEMONS)
		.retroModelIds(new int[]{2943})
		.idleAnimationId(66)
		.walkAnimationId(63)
		.attackAnimationId(64)
		.defendAnimationId(65)
		.deathAnimationId(67)
		.modernAttackAnims(DEMON_MODERN_ATTACKS)
		.modernDefendAnims(DEMON_MODERN_DEFENDS)
		.modernDeathAnims(DEMON_MODERN_DEATHS)
		.build();

	public static final RetroNpcData GREATER_DEMON_DEFAULT = RetroNpcData.builder()
		.category(RetroNpcCategory.GREATER_DEMONS)
		.retroModelIds(new int[]{2942})
		.idleAnimationId(66)
		.walkAnimationId(63)
		.attackAnimationId(64)
		.defendAnimationId(65)
		.deathAnimationId(67)
		.modernAttackAnims(DEMON_MODERN_ATTACKS)
		.modernDefendAnims(DEMON_MODERN_DEFENDS)
		.modernDeathAnims(DEMON_MODERN_DEATHS)
		.build();

	public static final RetroNpcData BLACK_DEMON_DEFAULT = RetroNpcData.builder()
		.category(RetroNpcCategory.BLACK_DEMONS)
		.retroModelIds(new int[]{2942})
		.idleAnimationId(66)
		.walkAnimationId(63)
		.attackAnimationId(64)
		.defendAnimationId(65)
		.deathAnimationId(67)
		.modernAttackAnims(DEMON_MODERN_ATTACKS)
		.modernDefendAnims(DEMON_MODERN_DEFENDS)
		.modernDeathAnims(DEMON_MODERN_DEATHS)
		.build();

	public static final RetroNpcData IMP_DEFAULT = RetroNpcData.builder()
		.category(RetroNpcCategory.IMPS)
		.retroModelIds(new int[]{2887})
		.idleAnimationId(171)
		.walkAnimationId(168)
		.attackAnimationId(169)
		.defendAnimationId(170)
		.deathAnimationId(172)
		.modernAttackAnims(IMP_MODERN_ATTACKS)
		.modernDefendAnims(IMP_MODERN_DEFENDS)
		.modernDeathAnims(IMP_MODERN_DEATHS)
		.build();

	public static final RetroNpcData SKELETON_UNARMED = RetroNpcData.builder()
		.category(RetroNpcCategory.SKELETONS)
		.retroModelIds(new int[]{2944})
		.idleAnimationId(262)
		.walkAnimationId(259)
		.attackAnimationId(260)
		.defendAnimationId(261)
		.deathAnimationId(263)
		.modernAttackAnims(SKELETON_MODERN_ATTACKS)
		.modernDefendAnims(SKELETON_MODERN_DEFENDS)
		.modernDeathAnims(SKELETON_MODERN_DEATHS)
		.build();

	public static final RetroNpcData SKELETON_ARMED = RetroNpcData.builder()
		.category(RetroNpcCategory.SKELETONS)
		.retroModelIds(new int[]{2944, 2946})
		.idleAnimationId(262)
		.walkAnimationId(259)
		.attackAnimationId(260)
		.defendAnimationId(261)
		.deathAnimationId(263)
		.modernAttackAnims(SKELETON_MODERN_ATTACKS)
		.modernDefendAnims(SKELETON_MODERN_DEFENDS)
		.modernDeathAnims(SKELETON_MODERN_DEATHS)
		.build();

	public static final RetroNpcData ZOMBIE_UNARMED = RetroNpcData.builder()
		.category(RetroNpcCategory.ZOMBIES)
		.retroModelIds(new int[]{2931})
		.idleAnimationId(301)
		.walkAnimationId(298)
		.attackAnimationId(299)
		.defendAnimationId(300)
		.deathAnimationId(302)
		.modernAttackAnims(ZOMBIE_MODERN_ATTACKS)
		.modernDefendAnims(ZOMBIE_MODERN_DEFENDS)
		.modernDeathAnims(ZOMBIE_MODERN_DEATHS)
		.build();

	public static final RetroNpcData ZOMBIE_ARMED = RetroNpcData.builder()
		.category(RetroNpcCategory.ZOMBIES)
		.retroModelIds(new int[]{2931, 2932})
		.idleAnimationId(301)
		.walkAnimationId(298)
		.attackAnimationId(299)
		.defendAnimationId(300)
		.deathAnimationId(302)
		.modernAttackAnims(ZOMBIE_MODERN_ATTACKS)
		.modernDefendAnims(ZOMBIE_MODERN_DEFENDS)
		.modernDeathAnims(ZOMBIE_MODERN_DEATHS)
		.build();

	public static final RetroNpcData GUARD_DEFAULT = RetroNpcData.builder()
		.category(RetroNpcCategory.GUARDS)
		.retroModelIds(null)
		.idleAnimationId(808)
		.walkAnimationId(819)
		.attackAnimationId(422)
		.defendAnimationId(424)
		.deathAnimationId(836)
		.modernAttackAnims(GUARD_MODERN_ATTACKS)
		.modernDefendAnims(GUARD_MODERN_DEFENDS)
		.modernDeathAnims(GUARD_MODERN_DEATHS)
		.build();

	public static final RetroNpcData HILL_GIANT_DEFAULT = RetroNpcData.builder()
		.category(RetroNpcCategory.HILL_GIANTS)
		.retroModelIds(new int[]{2870, 2866})
		.idleAnimationId(130)
		.walkAnimationId(127)
		.attackAnimationId(128)
		.defendAnimationId(129)
		.deathAnimationId(131)
		.modernAttackAnims(GIANT_MODERN_ATTACKS)
		.modernDefendAnims(GIANT_MODERN_DEFENDS)
		.modernDeathAnims(GIANT_MODERN_DEATHS)
		.build();

	/**
	 * Populates mappings from the bundled npc-mappings.json entries (generated
	 * from the 2005 cache by the dev-only NpcMappingGenerator tool), while
	 * preserving explicit static archetype overrides and modern OSRS ID mappings.
	 */
	public static void load(List<RetroNpcMappingEntry> entries)
	{
		ID_MAPPINGS.clear();
		NAME_MAPPINGS.clear();

		// 1. Register base static archetype overrides and modern OSRS ID mappings
		registerStaticOverrides();

		if (entries == null)
		{
			return;
		}

		// 2. Populate mappings from the generated 2005 cache entries
		for (RetroNpcMappingEntry entry : entries)
		{
			if (entry == null || entry.getName() == null || entry.getName().isEmpty()
				|| entry.getCategory() == null
				|| entry.getModelIds() == null || entry.getModelIds().length == 0)
			{
				continue;
			}

			String nameLower = entry.getName().toLowerCase(Locale.ROOT).trim();
			NAME_MAPPINGS.putIfAbsent(nameLower, createMappingData(entry));
		}
	}

	private static void registerMapping(RetroNpcData data, int... npcIds)
	{
		for (int id : npcIds)
		{
			ID_MAPPINGS.put(id, data);
		}
	}

	private static void registerStaticOverrides()
	{
		// Lesser Demons
		NAME_MAPPINGS.put("lesser demon", LESSER_DEMON_DEFAULT);
		registerMapping(LESSER_DEMON_DEFAULT,
			NpcID.LESSER_DEMON, NpcID.LESSER_DEMON2, NpcID.LESSER_DEMON3, NpcID.LESSER_DEMON4, NpcID.LESSER_DEMON5,
			NpcID.DRAGONSLAYER_DEMON, NpcID.KOUREND_LESSER_DEMON1, NpcID.KOUREND_LESSER_DEMON2,
			NpcID.LESSER_DEMON_SLAYERCAVE_1, NpcID.LESSER_DEMON_SLAYERCAVE_2, NpcID.LESSER_DEMON_SLAYERCAVE_3,
			NpcID.WILD_CAVE_LESSER_DEMON, NpcID.WILD_CAVE_LESSER_DEMON2, NpcID.WILD_CAVE_LESSER_DEMON3,
			NpcID.DT2_SCAR_MAZE_MAGE_DEMON_NORMAL, NpcID.DT2_SCAR_MAZE_MELEE_DEMON_NORMAL,
			NpcID.DT2_SCAR_MAZE_RANGED_DEMON_NORMAL, NpcID.DT2_SCAR_LESSER_DEMON_1
		);

		// Greater Demons
		NAME_MAPPINGS.put("greater demon", GREATER_DEMON_DEFAULT);
		registerMapping(GREATER_DEMON_DEFAULT,
			NpcID.GREATER_DEMON, NpcID.GREATER_DEMON2, NpcID.GREATER_DEMON3, NpcID.GREATER_DEMON4, NpcID.GREATER_DEMON5,
			NpcID.GREATER_DEMON_STRONGHOLDCAVE_1, NpcID.GREATER_DEMON_STRONGHOLDCAVE_2, NpcID.GREATER_DEMON_STRONGHOLDCAVE_3,
			NpcID.KOUREND_GREATER_DEMON1, NpcID.KOUREND_GREATER_DEMON2, NpcID.KOUREND_GREATER_DEMON3,
			NpcID.WILD_CAVE_GREATER_DEMON, NpcID.WILD_CAVE_GREATER_DEMON2, NpcID.WILD_CAVE_GREATER_DEMON3,
			NpcID.DT2_SCAR_GREATER_DEMON_1
		);

		// Black Demons
		NAME_MAPPINGS.put("black demon", BLACK_DEMON_DEFAULT);
		registerMapping(BLACK_DEMON_DEFAULT,
			NpcID.BLACK_DEMON, NpcID.BLACK_DEMON2, NpcID.BLACK_DEMON3, NpcID.BLACK_DEMON4, NpcID.BLACK_DEMON5,
			NpcID.BLACK_DEMON_STRONGHOLDCAVE_1, NpcID.BLACK_DEMON_STRONGHOLDCAVE_2, NpcID.BLACK_DEMON_STRONGHOLDCAVE_3,
			NpcID.BLACK_DEMON_STRONGHOLDCAVE_4, NpcID.BLACK_DEMON_STRONGHOLDCAVE_5,
			NpcID.GRANDTREE_BLACKDEMON, NpcID.NZONE_GRANDTREE_BLACKDEMON_HARD, NpcID.NZONE_GRANDTREE_BLACKDEMON_NORMAL,
			NpcID.KOUREND_BLACK_DEMON_1, NpcID.KOUREND_BLACK_DEMON_2,
			NpcID.WILD_CAVE_BLACK_DEMON, NpcID.WILD_CAVE_BLACK_DEMON2, NpcID.WILD_CAVE_BLACK_DEMON3,
			NpcID.DT2_SCAR_BLACK_DEMON_1
		);

		// Imps
		NAME_MAPPINGS.put("imp", IMP_DEFAULT);
		registerMapping(IMP_DEFAULT, NpcID.IMP, NpcID.GODWARS_ANCIENT_IMP, NpcID.CASTLEWARS_IMP);

		NAME_MAPPINGS.put("skeleton", SKELETON_UNARMED);
		registerMapping(SKELETON_UNARMED,
			NpcID.SKELETON_UNARMED, NpcID.SKELETON_UNARMED2, NpcID.SKELETON_UNARMED3,
				NpcID.SKELETON_UNARMED4, NpcID.SKELETON_UNAGRESSIVE
		);
		registerMapping(SKELETON_ARMED,
				NpcID.SKELETON_ARMED, NpcID.SKELETON_ARMED2, NpcID.SKELETON_UNAGRESSIVE2,
				NpcID.SKELETON_UNAGRESSIVE3
		);

		// Zombies
		NAME_MAPPINGS.put("zombie", ZOMBIE_UNARMED);
		registerMapping(ZOMBIE_UNARMED,
			NpcID.ZOMBIE_UNARMED, NpcID.ZOMBIE_UNARMED2, NpcID.ZOMBIE_UNARMED3, NpcID.ZOMBIE_UNARMED4,
			NpcID.ZOMBIE_UNARMED5, NpcID.ZOMBIE_UNARMED6, NpcID.ZOMBIE_UNARMED_CITY1, NpcID.ZOMBIE_UNARMED_CITY3,
			NpcID.ZOMBIE_UNARMED_CITY6, NpcID.ZOMBIE_UNARMED_SEWER1, NpcID.ZOMBIE_UNARMED_SEWER2,
			NpcID.ZOMBIE_UNARMED_SEWER3, NpcID.ZOMBIE_UNARMED_SEWER4,
			NpcID.ZOMBIE2, NpcID.ZOMBIE2_B, NpcID.ZOMBIE2_C
		);
		registerMapping(ZOMBIE_ARMED,
			NpcID.ZOMBIE_ARMED, NpcID.ZOMBIE_ARMED2, NpcID.ZOMBIE_ARMED3, NpcID.ZOMBIE_ARMED_CITY1,
			NpcID.ZOMBIE_ARMED_CITY3, NpcID.ZOMBIE_ARMED_SEWER1, NpcID.ZOMBIE_ARMED_SEWER2,
			NpcID.ZOMBIE_ARMED_SEWER3, NpcID.ZOMBIE_ARMED_SEWER4
		);

		// Guards
		NAME_MAPPINGS.put("guard", GUARD_DEFAULT);
		registerMapping(GUARD_DEFAULT,
			NpcID.BIM_FAI_VARROCK_GUARD02, NpcID.BIM_FAI_VARROCK_GUARD02_F, NpcID.BIM_FAI_VARROCK_GUARD02_VARIANT02,
			NpcID.FAI_VARROCK_GUARD02, NpcID.FAI_VARROCK_GUARD02_VARIANT01, NpcID.FAI_VARROCK_GUARD02_VARIANT02,
			NpcID.FAI_VARROCK_GUARD02_F, NpcID.FAI_VARROCK_GUARD02_F_VARIANT01, NpcID.FAI_VARROCK_GUARD02_F_VARIANT02,
			NpcID.FAI_VARROCK_GUARD_CAPTAIN02,
			NpcID.GUARD1_VARIANT01, NpcID.GUARD1_F, NpcID.GUARD1_F_VARIANT01,
			NpcID.ARDOUGNE_GUARD_VARIANT01, NpcID.ARDOUGNE_GUARD_F, NpcID.ARDOUGNE_GUARD_F_VARIANT01,
			NpcID.FAI_FALADOR_GUARD1_VARIANT01, NpcID.FAI_FALADOR_GUARD1_F, NpcID.FAI_FALADOR_GUARD1_VARIANT02,
			NpcID.FAI_FALADOR_GUARD2_F, NpcID.FAI_FALADOR_GUARD3_F, NpcID.FAI_FALADOR_GUARD4_F
		);

		// Hill Giants
		NAME_MAPPINGS.put("hill giant", HILL_GIANT_DEFAULT);
		registerMapping(HILL_GIANT_DEFAULT,
			NpcID.GIANT, NpcID.GIANT2, NpcID.GIANT3, NpcID.GIANT4, NpcID.GIANT5, NpcID.GIANT6,
			NpcID.KOUREND_HILLGIANT
		);
	}

	private static RetroNpcData createMappingData(RetroNpcMappingEntry entry)
	{
		RetroNpcCategory category = entry.getCategory();
		int attackAnim = -1;
		int defendAnim = -1;
		int deathAnim = -1;
		int[] models = entry.getModelIds();
		int stanceAnim = entry.getIdleAnim();
		int walkAnim = entry.getWalkAnim();
		// Seeded from the 2005 definition; categories below correct it where the retro and
		// modern meshes are different sizes
		int scaleXZ = entry.getScaleXZ();
		int scaleY = entry.getScaleY();

		Set<Integer> modernAttacks = Collections.emptySet();
		Set<Integer> modernDefends = Collections.emptySet();
		Set<Integer> modernDeaths = Collections.emptySet();

		// These guys were removed from the cache when they removed the Realm of Memories
		if (category == RetroNpcCategory.LESSER_DEMONS
			|| category == RetroNpcCategory.GREATER_DEMONS
			|| category == RetroNpcCategory.BLACK_DEMONS)
		{
			stanceAnim = stanceAnim != -1 ? stanceAnim : 66;
			walkAnim = walkAnim != -1 ? walkAnim : 63;
			attackAnim = 64;
			defendAnim = 65;
			deathAnim = 67;
			modernAttacks = DEMON_MODERN_ATTACKS;
			modernDefends = DEMON_MODERN_DEFENDS;
			modernDeaths = DEMON_MODERN_DEATHS;
		}
		else if (category == RetroNpcCategory.ADULT_DRAGONS)
		{
			attackAnim = 91;
			defendAnim = 89;
			deathAnim = 92;
			modernAttacks = DRAGON_MODERN_ATTACKS;
			modernDefends = DRAGON_MODERN_DEFENDS;
			modernDeaths = DRAGON_MODERN_DEATHS;
		}
		else if (category == RetroNpcCategory.GOBLINS)
		{
			stanceAnim = stanceAnim != -1 ? stanceAnim : 311;
			walkAnim = walkAnim != -1 ? walkAnim : 308;
			attackAnim = 309;
			defendAnim = 312;
			deathAnim = 313;
			modernAttacks = GOBLIN_MODERN_ATTACKS;
			modernDefends = GOBLIN_MODERN_DEFENDS;
			modernDeaths = GOBLIN_MODERN_DEATHS;
		}
		else if (category == RetroNpcCategory.ZOMBIES)
		{
			stanceAnim = stanceAnim != -1 ? stanceAnim : 301;
			walkAnim = walkAnim != -1 ? walkAnim : 298;
			attackAnim = 299;
			defendAnim = 300;
			deathAnim = 302;
			modernAttacks = ZOMBIE_MODERN_ATTACKS;
			modernDefends = ZOMBIE_MODERN_DEFENDS;
			modernDeaths = ZOMBIE_MODERN_DEATHS;
		}
		else if (category == RetroNpcCategory.CHICKENS)
		{
			// These are really tiny compared to modern chickens. The 2005 definition asked for no
			// resize at all (128), and the modern composition's own scale is 80, so neither
			// source gets us there - this is a hand-matched value.
			scaleXZ = 204;
			scaleY = 204;
			models = new int[]{2849};
			stanceAnim = 54;
			walkAnim = 53;
			attackAnim = 55;
			defendAnim = 56;
			deathAnim = 57;
			modernAttacks = CHICKEN_MODERN_ATTACKS;
			modernDefends = CHICKEN_MODERN_DEFENDS;
			modernDeaths = CHICKEN_MODERN_DEATHS;
		}
		else if (category == RetroNpcCategory.HILL_GIANTS)
		{
			// TODO: Maybe we can find the 'real' Hill Giant Head (currently set to a Jogre head, which looks 'OK')
			models = new int[] {2870, 2866};
			stanceAnim = 130;
			walkAnim = 127;
			attackAnim = 128;
			defendAnim = 129;
			deathAnim = 131;
			modernAttacks = GIANT_MODERN_ATTACKS;
			modernDefends = GIANT_MODERN_DEFENDS;
			modernDeaths = GIANT_MODERN_DEATHS;
		}
		else if (category == RetroNpcCategory.GUARDS)
		{
			// Currently unused, they are multi-part models and I couldn't find the correct head/arms in the cache
			models = null;
			stanceAnim = 808;
			walkAnim = 819;
			attackAnim = 422;
			defendAnim = 424;
			deathAnim = 836;
			modernAttacks = GUARD_MODERN_ATTACKS;
			modernDefends = GUARD_MODERN_DEFENDS;
			modernDeaths = GUARD_MODERN_DEATHS;
		}
		else if (category == RetroNpcCategory.IMPS)
		{
			// Animations no longer exist in the official game cache, so these are disabled
			stanceAnim = stanceAnim != -1 ? stanceAnim : 171;
			walkAnim = walkAnim != -1 ? walkAnim : 168;
			attackAnim = 169;
			defendAnim = 170;
			deathAnim = 172;
			modernAttacks = IMP_MODERN_ATTACKS;
			modernDefends = IMP_MODERN_DEFENDS;
			modernDeaths = IMP_MODERN_DEATHS;
		}
		else if (category == RetroNpcCategory.SKELETONS)
		{
			stanceAnim = stanceAnim != -1 ? stanceAnim : 262;
			walkAnim = walkAnim != -1 ? walkAnim : 259;
			attackAnim = 260;
			defendAnim = 261;
			deathAnim = 263;
			modernAttacks = SKELETON_MODERN_ATTACKS;
			modernDefends = SKELETON_MODERN_DEFENDS;
			modernDeaths = SKELETON_MODERN_DEATHS;
		}
		else if (category == RetroNpcCategory.GHOSTS)
		{
			// Had issues with animations (again, appear to have been removed), so ghosts are disabled
			attackAnim = 422;
			defendAnim = 424;
			deathAnim = 836;
			modernAttacks = GHOST_MODERN_ATTACKS;
			modernDefends = GHOST_MODERN_DEFENDS;
			modernDeaths = GHOST_MODERN_DEATHS;
		}

		return RetroNpcData.builder()
			.category(category)
			.retroModelIds(models)
			.idleAnimationId(stanceAnim)
			.walkAnimationId(walkAnim)
			.attackAnimationId(attackAnim)
			.defendAnimationId(defendAnim)
			.deathAnimationId(deathAnim)
			.scaleXZ(scaleXZ)
			.scaleY(scaleY)
			.modernAttackAnims(modernAttacks)
			.modernDefendAnims(modernDefends)
			.modernDeathAnims(modernDeaths)
			.build();
	}

	public static RetroNpcData get(int npcId, String npcName)
	{
		if (npcName == null)
		{
			return ID_MAPPINGS.get(npcId);
		}

		String nameLower = npcName.toLowerCase(Locale.ROOT).trim();
		RetroNpcData byName = NAME_MAPPINGS.get(nameLower);
		RetroNpcData byId = ID_MAPPINGS.get(npcId);

		if (byName == null)
		{
			// Modern NPCs whose names no longer match a 2005 name are still
			// swappable when their ID was explicitly registered.
			return byId;
		}

		if (byId != null && byId.getCategory() == byName.getCategory())
		{
			return byId;
		}

		return byName;
	}
}
