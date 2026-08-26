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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.runelite.client.plugins.retronpcswapper.cache.RetroNpcDefinition;

/**
 * Registry class that maps Modern NPC IDs and Names to Retro (2004/2005) model & animation data.
 */
public class RetroNpcMapping
{
	private static final Map<Integer, RetroNpcData> ID_MAPPINGS = new HashMap<>();
	private static final Map<String, RetroNpcData> NAME_MAPPINGS = new HashMap<>();

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

				if (category == RetroNpcCategory.GOBLINS)
				{
					stanceAnim = stanceAnim != -1 ? stanceAnim : 311;
					walkAnim = walkAnim != -1 ? walkAnim : 308;
					attackAnim = 309;
					defendAnim = 312;
					deathAnim = 313;
				}
				else if (category == RetroNpcCategory.ZOMBIES)
				{
					stanceAnim = stanceAnim != -1 ? stanceAnim : 301;
					walkAnim = walkAnim != -1 ? walkAnim : 298;
					attackAnim = 299;
					defendAnim = 303;
					deathAnim = 302;
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
				}

				RetroNpcData data = new RetroNpcData(
					category,
					models,
					stanceAnim,
					walkAnim,
					attackAnim,
					defendAnim,
					deathAnim,
					widthScale,
					heightScale
				);

				// Register by name for instant matching against any NPC variant
				NAME_MAPPINGS.putIfAbsent(nameLower, data);

				// Explicit custom overrides
				if (nameLower.equals("imp"))
				{
					int[] impModels = new int[]{2887};
					int impStance = def.getStanceAnimation() != -1 ? def.getStanceAnimation() : 171;
					int impWalk = def.getWalkAnimation() != -1 ? def.getWalkAnimation() : 168;
					RetroNpcData impData = new RetroNpcData(RetroNpcCategory.IMPS, impModels, impStance, impWalk, 169, 170, 172);

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
					RetroNpcData unarmedSkel = new RetroNpcData(RetroNpcCategory.SKELETONS, new int[]{2944}, 262, 259, 260, 261, 263);
					// Armed Skeletons with Sword & Shield (NPC 72, 92, 93, 750, 1127, 1128)
					RetroNpcData armedSkel = new RetroNpcData(RetroNpcCategory.SKELETONS, new int[]{2944, 2946}, 262, 259, 260, 261, 263);

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
					RetroNpcData guardData = new RetroNpcData(
						RetroNpcCategory.GUARDS,
						null,
						808,
						819,
						422,
						424,
						836
					);
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
					RetroNpcData hillGiantData = new RetroNpcData(
						RetroNpcCategory.GIANTS,
						null,
						130,
						127,
						128,
						129,
						131
					);
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
