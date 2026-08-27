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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.runelite.client.plugins.retronpcswapper.cache.RetroNpcDefinition;

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

	public static final Set<Integer> IMP_MODERN_ATTACKS = Set.of(169, 422, 423, 5385, 5485, 5486);
	public static final Set<Integer> IMP_MODERN_DEFENDS = Set.of(170, 424, 5388, 5488, 5489, 5490);
	public static final Set<Integer> IMP_MODERN_DEATHS = Set.of(172, 173, 836, 5389, 5390, 5491, 5492);

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
		5568, 5571, 5573, 5576, 5577, 5578, 5581, 5583, 5585, 5590, 5593, 422, 423, 412, 299
	);
	public static final Set<Integer> ZOMBIE_MODERN_DEFENDS = buildZombieDefends();
	public static final Set<Integer> ZOMBIE_MODERN_DEATHS = Set.of(
		5575, 5580, 5587, 5588, 5591, 5594, 836, 2304, 302
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

	private static Set<Integer> buildZombieDefends()
	{
		Set<Integer> set = new HashSet<>(Arrays.asList(424, 425, 1156, 2303, 300, 301, 303));
		for (int a = 5567; a <= 5595; a++)
		{
			if (a != 5568 && a != 5571 && a != 5573 && a != 5576 && a != 5577 && a != 5578 && a != 5581 && a != 5583 && a != 5585 && a != 5590 && a != 5593 && a != 5575 && a != 5580 && a != 5587 && a != 5588 && a != 5591 && a != 5594)
			{
				set.add(a);
			}
		}
		return Collections.unmodifiableSet(set);
	}

	/**
	 * Populates mappings dynamically from decoded 2005 cache NPC definitions.
	 */
	public static void loadFrom2005Cache(Map<Integer, RetroNpcDefinition> retroDefs)
	{
		ID_MAPPINGS.clear();
		NAME_MAPPINGS.clear();

		if (retroDefs == null || retroDefs.isEmpty())
		{
			return;
		}

		for (Map.Entry<Integer, RetroNpcDefinition> entry : retroDefs.entrySet())
		{
			RetroNpcDefinition def = entry.getValue();
			if (def == null || def.getName() == null || def.getName().isEmpty())
			{
				continue;
			}

			String nameLower = def.getName().toLowerCase(Locale.ROOT).trim();
			RetroNpcCategory category = null;

			if (nameLower.contains("demon"))
			{
				category = RetroNpcCategory.DEMONS;
			}
			else if (nameLower.contains("dragon"))
			{
				category = RetroNpcCategory.DRAGONS;
			}
			else if (nameLower.contains("goblin"))
			{
				category = RetroNpcCategory.GOBLINS;
			}
			else if (nameLower.equals("guard"))
			{
				category = RetroNpcCategory.GUARDS;
			}
			else if (nameLower.contains("imp"))
			{
				category = RetroNpcCategory.IMPS;
			}
			else if (nameLower.contains("skeleton"))
			{
				category = RetroNpcCategory.SKELETONS;
			}
			else if (nameLower.contains("zombie"))
			{
				category = RetroNpcCategory.ZOMBIES;
			}
			else if (nameLower.contains("ghost"))
			{
				category = RetroNpcCategory.GHOSTS;
			}
			else if (nameLower.contains("giant"))
			{
				category = RetroNpcCategory.GIANTS;
			}
			else if (nameLower.contains("chicken"))
			{
				category = RetroNpcCategory.CHICKENS;
			}

			if (category != null && def.getModels() != null && def.getModels().length > 0)
			{
				int attackAnim = -1;
				int defendAnim = -1;
				int deathAnim = -1;
				int widthScale = -1;
				int heightScale = -1;
				int[] models = def.getModels();
				int stanceAnim = def.getStanceAnimation();
				int walkAnim = def.getWalkAnimation();

				Set<Integer> modernAttacks = Collections.emptySet();
				Set<Integer> modernDefends = Collections.emptySet();
				Set<Integer> modernDeaths = Collections.emptySet();

				if (category == RetroNpcCategory.DEMONS)
				{
					attackAnim = 64;
					defendAnim = 65;
					deathAnim = 67;
					modernAttacks = DEMON_MODERN_ATTACKS;
					modernDefends = DEMON_MODERN_DEFENDS;
					modernDeaths = DEMON_MODERN_DEATHS;
				}
				else if (category == RetroNpcCategory.DRAGONS)
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
					defendAnim = 303;
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
					widthScale = -1;
					heightScale = -1;
					modernAttacks = CHICKEN_MODERN_ATTACKS;
					modernDefends = CHICKEN_MODERN_DEFENDS;
					modernDeaths = CHICKEN_MODERN_DEATHS;
				}
				else if (category == RetroNpcCategory.GIANTS)
				{
					// Giant models in OSRS are already authentic retro models; preserve models and override animations
					models = null;
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
					// Models resolved from classic OSRS guard definition (NPC 3269) for Varrock guards
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

				RetroNpcData data = RetroNpcData.builder()
					.category(category)
					.retroModelIds(models)
					.idleAnimationId(stanceAnim)
					.walkAnimationId(walkAnim)
					.attackAnimationId(attackAnim)
					.defendAnimationId(defendAnim)
					.deathAnimationId(deathAnim)
					.widthScale(widthScale)
					.heightScale(heightScale)
					.modernAttackAnims(modernAttacks)
					.modernDefendAnims(modernDefends)
					.modernDeathAnims(modernDeaths)
					.build();

				// Register by name for instant matching against any NPC variant
				NAME_MAPPINGS.putIfAbsent(nameLower, data);

				// Explicit custom overrides
				if (nameLower.equals("imp"))
				{
					int[] impModels = new int[]{2887};
					int impStance = def.getStanceAnimation() != -1 ? def.getStanceAnimation() : 171;
					int impWalk = def.getWalkAnimation() != -1 ? def.getWalkAnimation() : 168;
					RetroNpcData impData = RetroNpcData.builder()
						.category(RetroNpcCategory.IMPS)
						.retroModelIds(impModels)
						.idleAnimationId(impStance)
						.walkAnimationId(impWalk)
						.attackAnimationId(169)
						.defendAnimationId(170)
						.deathAnimationId(172)
						.modernAttackAnims(IMP_MODERN_ATTACKS)
						.modernDefendAnims(IMP_MODERN_DEFENDS)
						.modernDeathAnims(IMP_MODERN_DEATHS)
						.build();

					NAME_MAPPINGS.put("imp", impData);
					int[] impIds = {708, 709, 3080, 3081, 3082, 3083, 3084, 3085, 5007, 5008, 7067, 7068, 7069, 7070, 7071, 7072, 7924};
					for (int id : impIds)
					{
						ID_MAPPINGS.put(id, impData);
					}
				}
				else if (nameLower.contains("skeleton"))
				{
					// Unarmed Skeletons (NPC 70, 71, 73, 90, 91, 459, 1126)
					RetroNpcData unarmedSkel = RetroNpcData.builder()
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

					// Armed Skeletons with Sword & Shield (NPC 72, 92, 93, 750, 1127, 1128)
					RetroNpcData armedSkel = RetroNpcData.builder()
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

					RetroNpcData skelData = (nameLower.contains("sword") || nameLower.contains("shield") || nameLower.contains("armed")) ? armedSkel : unarmedSkel;
					NAME_MAPPINGS.put(nameLower, skelData);

					int[] unarmedIds = {70, 71, 73, 90, 91, 459, 1126};
					for (int id : unarmedIds)
					{
						ID_MAPPINGS.put(id, unarmedSkel);
					}

					int[] armedIds = {72, 92, 93, 750, 1127, 1128};
					for (int id : armedIds)
					{
						ID_MAPPINGS.put(id, armedSkel);
					}
				}
				else if (nameLower.equals("guard"))
				{
					RetroNpcData guardData = RetroNpcData.builder()
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

					NAME_MAPPINGS.put("guard", guardData);
					int[] guardIds = {
						3244, 3245, 3246, 3247, 3248, 3249, 3250,
						11903, 11904, 11905, 11911, 11912, 11913, 11914, 11915, 11916, 11917,
						11922, 11923, 11924, 11937, 11938, 11939, 11942, 11943, 11944, 11945, 11946, 11947
					};
					for (int id : guardIds)
					{
						ID_MAPPINGS.put(id, guardData);
					}
				}
				else if (nameLower.contains("hill giant"))
				{
					RetroNpcData hillGiantData = RetroNpcData.builder()
						.category(RetroNpcCategory.GIANTS)
						.retroModelIds(null)
						.idleAnimationId(130)
						.walkAnimationId(127)
						.attackAnimationId(128)
						.defendAnimationId(129)
						.deathAnimationId(131)
						.modernAttackAnims(GIANT_MODERN_ATTACKS)
						.modernDefendAnims(GIANT_MODERN_DEFENDS)
						.modernDeathAnims(GIANT_MODERN_DEATHS)
						.build();

					NAME_MAPPINGS.put("hill giant", hillGiantData);
					int[] hillGiantIds = {117, 2098, 2099, 2100, 2101, 2102, 2103, 3144, 7261, 7262};
					for (int id : hillGiantIds)
					{
						ID_MAPPINGS.put(id, hillGiantData);
					}
				}
			}
		}
	}

	public static RetroNpcData get(int npcId, String npcName)
	{
		RetroNpcData data = ID_MAPPINGS.get(npcId);
		if (data != null)
		{
			return data;
		}

		if (npcName != null)
		{
			return NAME_MAPPINGS.get(npcName.toLowerCase(Locale.ROOT).trim());
		}

		return null;
	}

	public static boolean contains(int npcId, String npcName)
	{
		return get(npcId, npcName) != null;
	}
}
