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
			else if (nameLower.contains("goblin") || nameLower.contains("imp") || nameLower.contains("guard") || nameLower.contains("skeleton"))
			{
				category = RetroNpcCategory.MISC;
			}

			if (category != null && def.getModels() != null && def.getModels().length > 0)
			{
				RetroNpcData data = new RetroNpcData(
					category,
					def.getModels(),
					def.getStanceAnimation(),
					def.getWalkAnimation(),
					-1
				);

				// Register by name for instant matching against any NPC variant
				NAME_MAPPINGS.putIfAbsent(nameLower, data);

				// Shelving Demons and Dragons for now per user request.
				if (nameLower.equals("imp"))
				{
					int[] impModels = new int[]{2887};
					int stanceAnim = def.getStanceAnimation() != -1 ? def.getStanceAnimation() : 171;
					int walkAnim = def.getWalkAnimation() != -1 ? def.getWalkAnimation() : 168;
					RetroNpcData impData = new RetroNpcData(RetroNpcCategory.MISC, impModels, stanceAnim, walkAnim, 169, 170, 172);

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
					RetroNpcData unarmedSkel = new RetroNpcData(RetroNpcCategory.MISC, new int[]{2944}, 262, 259, 260, 261, 263);
					// Armed Skeletons with Sword & Shield (NPC 72, 92, 93, 750, 1127, 1128)
					RetroNpcData armedSkel = new RetroNpcData(RetroNpcCategory.MISC, new int[]{2944, 2946}, 262, 259, 260, 261, 263);

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
