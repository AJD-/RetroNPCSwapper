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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Registry class that maps Modern NPC IDs and Names to Retro (2004/2005) model & animation data,
 * with Category-Scoped modern animation sets.
 */
public class RetroNpcMapping
{
	private static final Map<Integer, RetroNpcData> ID_MAPPINGS = new HashMap<>();
	private static final Map<String, RetroNpcData> NAME_MAPPINGS = new HashMap<>();

	// Category-Scoped Modern Animation Sets
	public static final Set<Integer> DEMON_MODERN_ATTACKS = Set.of(5485, 5486, 5487, 5507, 5510, 8002);
	public static final Set<Integer> DEMON_MODERN_DEFENDS = Set.of(5488, 5489, 5490, 5508, 5511, 8003);
	public static final Set<Integer> DEMON_MODERN_DEATHS = Set.of(5491, 5492, 5493, 5509, 5512, 8004, 8005);

	public static final Set<Integer> DRAGON_MODERN_ATTACKS = Set.of(240, 5485, 5486, 5487, 5507, 5510, 8002);
	public static final Set<Integer> DRAGON_MODERN_DEFENDS = Set.of(1827, 5488, 5489, 5490, 5508, 5511, 8003);
	public static final Set<Integer> DRAGON_MODERN_DEATHS = Set.of(1828, 5491, 5492, 5493, 5509, 5512, 8004, 8005);

	public static final Set<Integer> GOBLIN_MODERN_ATTACKS = Set.of(6184, 6185, 6186, 6188, 6190, 6191, 309);
	public static final Set<Integer> GOBLIN_MODERN_DEFENDS = Set.of(6183, 312, 310);
	public static final Set<Integer> GOBLIN_MODERN_DEATHS = Set.of(6182, 313);

	public static final Set<Integer> GUARD_MODERN_ATTACKS = Set.of(422, 423, 451, 7041, 7042);
	public static final Set<Integer> GUARD_MODERN_DEFENDS = Set.of(424, 7043);
	public static final Set<Integer> GUARD_MODERN_DEATHS = Set.of(836, 7044);

	public static final Set<Integer> IMP_MODERN_ATTACKS = Set.of(169, 422, 423, 451, 5385, 5485, 5486, 7041, 7042);
	public static final Set<Integer> IMP_MODERN_DEFENDS = Set.of(170, 424, 425, 5388, 5488, 5489, 5490, 7043);
	public static final Set<Integer> IMP_MODERN_DEATHS = Set.of(172, 173, 836, 5389, 5390, 5491, 5492, 5493, 5509, 5512, 7044);

	public static final Set<Integer> SKELETON_MODERN_ATTACKS = Set.of(
		260, 390, 400, 401, 412, 414, 422, 423, 426, 428, 440, 451, 711, 1162, 1167,
		240, 309, 169, 5385, 5485, 5486, 5487, 5507, 5510, 7041, 7042, 8002
	);
	public static final Set<Integer> SKELETON_MODERN_DEFENDS = Set.of(
		170, 261, 300, 310, 312, 424, 425, 1156, 1588, 1827, 2303, 5388,
		5488, 5489, 5490, 5508, 5511, 7043, 8003
	);
	public static final Set<Integer> SKELETON_MODERN_DEATHS = Set.of(
		172, 173, 263, 302, 313, 836, 1589, 1828, 2304, 5389, 5390,
		5491, 5492, 5493, 5509, 5512, 7044, 8004, 8005
	);

	public static final Set<Integer> ZOMBIE_MODERN_ATTACKS = Set.of(
		5568, 5571, 5573, 5576, 5577, 5578, 5581, 5583, 5585, 5590, 5593, 422, 423, 412, 451, 299
	);
	public static final Set<Integer> ZOMBIE_MODERN_DEFENDS = Set.of(
		5567, 5570, 5574, 5579, 5582, 5584, 5586, 5589, 5592, 424, 425, 1156, 2303, 300
	);
	public static final Set<Integer> ZOMBIE_MODERN_DEATHS = Set.of(
		5569, 5572, 5575, 5580, 5587, 5588, 5591, 5594, 5595, 836, 2304, 302,
		5389, 5390, 5491, 5492, 5493, 5509, 5512, 7044
	);

	public static final Set<Integer> GHOST_MODERN_ATTACKS = Set.of(422, 423, 5485, 5486);
	public static final Set<Integer> GHOST_MODERN_DEFENDS = Set.of(424, 5488, 5489, 5490);
	public static final Set<Integer> GHOST_MODERN_DEATHS = Set.of(836, 5491, 5492, 5493);

	public static final Set<Integer> GIANT_MODERN_ATTACKS = Set.of(4652, 4658, 4666, 4672, 7002, 7003, 128);
	public static final Set<Integer> GIANT_MODERN_DEFENDS = Set.of(4651, 4657, 4665, 4671, 7001, 129);
	public static final Set<Integer> GIANT_MODERN_DEATHS = Set.of(4653, 4659, 4667, 4673, 7004, 131);

	public static final Set<Integer> CHICKEN_MODERN_ATTACKS = Set.of(5385, 5387, 55);
	public static final Set<Integer> CHICKEN_MODERN_DEFENDS = Set.of(5388, 56);
	public static final Set<Integer> CHICKEN_MODERN_DEATHS = Set.of(5389, 5390, 57);

	// Pre-instantiated immutable archetypes
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
			82, 2005, 2006, 2007, 2008, 2018, 3982, 7247, 7248, 7656, 7657, 7664, 7865, 7866, 7867,
			12361, 12363, 12365, 12376, 12389
		);

		// Greater Demons
		NAME_MAPPINGS.put("greater demon", GREATER_DEMON_DEFAULT);
		registerMapping(GREATER_DEMON_DEFAULT,
			83, 2025, 2026, 2027, 2028, 2029, 2030, 2031, 2032, 7244, 7245, 7246, 7871, 7872, 7873,
			12387
		);

		// Black Demons
		NAME_MAPPINGS.put("black demon", BLACK_DEMON_DEFAULT);
		registerMapping(BLACK_DEMON_DEFAULT,
			84, 240, 1432, 2048, 2049, 2050, 2051, 2052, 5874, 5875, 5876, 5877, 6295, 6357,
			7242, 7243, 7874, 7875, 7876, 12385
		);

		// Imps
		NAME_MAPPINGS.put("imp", IMP_DEFAULT);
		registerMapping(IMP_DEFAULT, 708, 709, 3080, 3081, 3082, 3083, 3084, 3085, 3134, 5007, 5008, 5728, 7067, 7068, 7069, 7070, 7071, 7072, 7924);

		// Skeletons
		NAME_MAPPINGS.put("skeleton", SKELETON_UNARMED);
		registerMapping(SKELETON_UNARMED, 70, 71, 73, 74, 77, 78, 90, 91, 459, 1126);
		registerMapping(SKELETON_ARMED, 72, 75, 76, 92, 93, 750, 1127, 1128);

		// Zombies
		NAME_MAPPINGS.put("zombie", ZOMBIE_UNARMED);
		registerMapping(ZOMBIE_UNARMED,
			26, 27, 28, 29, 30, 31, 32, 34, 37, 38, 39, 40, 41, 42, 43, 44,
			419, 420, 421, 422, 423, 424, 1115, 1433, 1434
		);
		registerMapping(ZOMBIE_ARMED,
			49, 50, 51, 52, 54, 55, 56, 57, 58, 751, 1116
		);

		// Guards
		NAME_MAPPINGS.put("guard", GUARD_DEFAULT);
		registerMapping(GUARD_DEFAULT,
			3244, 3245, 3246, 3247, 3248, 3249, 3250,
			11903, 11904, 11905, 11911, 11912, 11913, 11914, 11915, 11916, 11917,
			11922, 11923, 11924, 11937, 11938, 11939, 11942, 11943, 11944, 11945, 11946, 11947
		);

		// Hill Giants
		NAME_MAPPINGS.put("hill giant", HILL_GIANT_DEFAULT);
		registerMapping(HILL_GIANT_DEFAULT, 117, 2098, 2099, 2100, 2101, 2102, 2103, 3144, 7261, 7262);
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

		Set<Integer> modernAttacks = Collections.emptySet();
		Set<Integer> modernDefends = Collections.emptySet();
		Set<Integer> modernDeaths = Collections.emptySet();

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
			// TODO: Find the 'real' Hill Giant Head (currently set to a Jogre head)
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
			// TODO: Replace retro models for guards (they are multi-part models)
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
