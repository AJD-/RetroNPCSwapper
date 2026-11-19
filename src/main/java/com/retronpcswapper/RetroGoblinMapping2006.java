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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.retronpcswapper.inject.RetroAssetBundle;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;

/**
 * The 2006 goblin rows. Every row is {@link RetroNpcCategory#GOBLINS}.
 */
public final class RetroGoblinMapping2006
{
	private static final Map<Integer, RetroNpcData> ID_MAPPINGS = new HashMap<>();
	private static final Map<String, RetroNpcData> NAME_MAPPINGS = new HashMap<>();

	private static RetroNpcData goblin2006(int[] parts, int scale, short[] find, short[] replace,
		int stance, int walk, int... heads)
	{
		RetroNpcData.Builder builder = RetroNpcMapping.goblinBase()
			.retroModelIds(parts)
			.scaleXZ(scale)
			.scaleY(scale)
			.recolors(find, replace)
			.idleAnimationId(stance)
			.walkAnimationId(walk);
		if (heads.length > 0)
		{
			builder.chathead(heads).chatheadKeyBase(RetroAssetBundle.GOBLIN_CHATHEAD_KEY_BASE);
		}
		return builder.build();
	}

	// Plain stance
	private static RetroNpcData stand(int[] parts, int scale, short[] find, short[] replace, int... heads)
	{
		return goblin2006(parts, scale, find, replace,
			AnimationID.GOBLIN_READY, AnimationID.GOBLIN_WALK, heads);
	}

	private static RetroNpcData stand(Kit kit, int... heads)
	{
		return stand(kit.parts, kit.scale, kit.find, kit.replace, heads);
	}

	// Spear-and-shield stance
	private static RetroNpcData spear(int[] parts, int scale, short[] find, short[] replace, int... heads)
	{
		return goblin2006(parts, scale, find, replace,
			AnimationID.GOBLIN_READY_SPEAR, AnimationID.GOBLIN_WALK_SPEAR, heads);
	}

	private static int[] p(int... ids)
	{
		return ids;
	}

	private static short[] c(int... colours)
	{
		short[] out = new short[colours.length];
		for (int i = 0; i < out.length; i++)
		{
			out[i] = (short) colours[i];
		}
		return out;
	}

	/** The look of one goblin: parts, scale and recolour pairs. */
	private static final class Kit
	{
		private final int[] parts;
		private final int scale;
		private final short[] find;
		private final short[] replace;

		private Kit(int[] parts, int scale, short[] find, short[] replace)
		{
			this.parts = parts;
			this.scale = scale;
			this.find = find;
			this.replace = replace;
		}
	}

	// The Lumbridge looks, which the Dwarf Cannon goblins repeat without heads
	private static final Kit KIT_2274 = new Kit(p(5536, 5539, 5544, 5547), 125, null, null);
	private static final Kit KIT_2275 = new Kit(p(5537, 5542, 5544, 5547, 5543, 2951), 125, c(14238), c(16685));
	private static final Kit KIT_2276 = new Kit(p(5536, 5539, 5545, 2956, 5541), 120,
		c(14238, 8086), c(14121, 6038));
	private static final Kit KIT_2277 = new Kit(p(5538, 5539, 5545, 5548, 5541, 2951), 130, c(14238), c(17839));
	private static final Kit KIT_2278 = new Kit(p(5536, 5542, 5544, 5547, 5541), 123, c(14238), c(14656));
	private static final Kit KIT_2279 = new Kit(p(5536, 5542, 5544, 2956, 5543), 122,
		c(14238, 8086), c(13093, 2213));
	private static final Kit KIT_2280 = new Kit(p(2951, 5539, 5545, 5547, 5543), 128,
		c(14238, 8086), c(11047, 6038));
	private static final Kit KIT_2281 = new Kit(p(5536, 5542, 5545, 5548), 124, c(14238), c(12973));
	private static final int[] ARMED = p(2951, 2953, 2955, 2956, 2957);
	private static final int[] HELMET = p(2952, 2953, 2955, 2956);

	// Lumbridge family
	private static final RetroNpcData DEF_2274 = stand(KIT_2274, 15551);
	private static final RetroNpcData DEF_2275 = stand(KIT_2275, 142, 15550);
	private static final RetroNpcData DEF_2276 = stand(KIT_2276, 15551);
	private static final RetroNpcData DEF_2277 = stand(KIT_2277, 142, 15552);
	private static final RetroNpcData DEF_2278 = stand(KIT_2278, 15551);
	private static final RetroNpcData DEF_2279 = stand(KIT_2279, 15551);
	private static final RetroNpcData DEF_2280 = stand(KIT_2280, 142);
	private static final RetroNpcData DEF_2281 = stand(KIT_2281, 15551);

	// Armed, strong, armored and guard goblins
	private static final RetroNpcData DEF_101 = stand(ARMED, 128, null, null, 142);
	private static final RetroNpcData DEF_102 = stand(HELMET, 128, null, null, 15551);
	private static final RetroNpcData DEF_298 = spear(ARMED, 128, c(916), c(22443), 142);
	private static final RetroNpcData DEF_299 = spear(ARMED, 128, c(916), c(933), 144);
	private static final RetroNpcData DEF_489 = stand(ARMED, 128, null, null, 142);

	// Dwarf Cannon, no heads
	private static final RetroNpcData DEF_4261 = stand(p(2951, 2953, 2955, 5547, 5537), 128, null, null);
	private static final RetroNpcData DEF_4262 = stand(p(2951, 5540, 5546, 5554, 5547, 5537), 128,
		c(14238, 7079, 2974), c(15004, 9889, -24411));
	private static final RetroNpcData DEF_4263 = stand(KIT_2274);
	private static final RetroNpcData DEF_4264 = stand(KIT_2275);
	private static final RetroNpcData DEF_4265 = stand(KIT_2276);
	private static final RetroNpcData DEF_4266 = stand(KIT_2277);
	private static final RetroNpcData DEF_4267 = stand(KIT_2278);
	private static final RetroNpcData DEF_4268 = stand(KIT_2279);
	private static final RetroNpcData DEF_4269 = stand(KIT_2280);
	private static final RetroNpcData DEF_4270 = stand(KIT_2281);
	private static final RetroNpcData DEF_4271 = stand(p(2951, 5542, 5544, 5548, 5538), 124,
		c(14238, 7079, 8086, 41), c(14753, 7690, 11152, 10283));
	private static final RetroNpcData DEF_4272 = stand(p(2951, 5540, 5546, 5547, 5538, 5543, 5553), 124,
		c(14238), c(10142));
	private static final RetroNpcData DEF_4273 = stand(p(2951, 5542, 5544, 5548, 5535, 5550), 124,
		c(14238), c(12469));
	private static final RetroNpcData DEF_4274 = stand(p(2951, 5539, 5545, 2956, 5537, 5551), 124,
		c(14238), c(14629));
	private static final RetroNpcData DEF_4275 = stand(p(2951, 5540, 2955, 5547, 5554, 5552), 124,
		c(14238), c(13493));
	private static final RetroNpcData DEF_4276 = stand(p(2951, 5541, 5544, 5551, 5547, 5542), 124,
		c(14238, 8086), c(13093, 666));

	// Barbarian village and Eye of Glouphrie, no heads
	private static final RetroNpcData DEF_3264 = stand(p(5535, 5540, 5546, 2957, 5547, 5543), 128,
		c(14238, 41, 12500, 0, 2974, 8086), c(14883, 5289, 5305, 15250, 2718, 8598));
	private static final RetroNpcData DEF_3265 = stand(p(5535, 5540, 5546, 2957, 5547, 5541), 128,
		c(14238, 41, 12500, 0, 2974, 8086), c(14878, 5285, 5305, 15248, 2842, 8598));
	private static final RetroNpcData DEF_3266 = stand(p(5535, 5540, 5546, 2957, 5547), 128,
		c(14238, 41, 12500, 0, 2974, 8086), c(14883, 7335, 5305, 15248, 3862, 8598));
	private static final RetroNpcData DEF_3267 = stand(p(5535, 5540, 5546, 2957, 5547, 5543), 128,
		c(14238, 41, 12500, 0, 2974, 8086), c(17052, 7335, 6331, 15248, 3740, 8598));

	// Shield of Arrav war goblins, no heads
	private static final RetroNpcData DEF_4407 = stand(ARMED, 128, null, null);
	private static final RetroNpcData DEF_4408 = stand(HELMET, 128, null, null);
	private static final RetroNpcData DEF_4409 = spear(p(5535, 5540, 5546, 5547, 5552, 2957), 125,
		c(14238, 2974), c(9891, 25121));

	// Goblin Village red (4499 is 4479 again)
	private static final RetroNpcData DEF_4479 = stand(p(5537, 5540, 5546, 5547, 5543, 2951, 5552, 5549), 130,
		c(14238, 2974), c(10782, 916), 142, 15550);
	private static final RetroNpcData DEF_4480 = spear(p(5535, 5540, 5545, 2956, 5541, 5553), 120,
		c(14238, 2974), c(16685, 916), 15551);
	private static final RetroNpcData DEF_4481 = stand(p(5538, 5540, 5546, 5548, 5541, 2951, 5551, 5552), 121,
		c(14238, 2974), c(10782, 916), 142, 15552);
	private static final RetroNpcData DEF_4482 = spear(p(5535, 5540, 5546, 5547, 5543, 2957, 5552), 120,
		c(14238, 2974), c(10136, 916), 15551);
	private static final RetroNpcData DEF_4483 = stand(p(5535, 5540, 5546, 2956, 5543, 5550), 124,
		c(14238, 2974), c(16685, 916), 15551);
	private static final RetroNpcData DEF_4484 = spear(p(2951, 5540, 5545, 5547, 5543, 5553), 129,
		c(14238, 2974), c(9891, 916), 142);
	private static final RetroNpcData DEF_4485 = spear(p(5535, 5540, 5546, 5548, 5553), 124,
		c(14238, 2974), c(14656, 916), 15551);

	// Goblin Village green
	private static final RetroNpcData DEF_4486 = stand(p(5537, 5540, 5546, 5547, 5543, 2951, 5552, 5550), 120,
		c(14238, 2974), c(10782, 22414), 142);
	private static final RetroNpcData DEF_4487 = spear(p(5535, 5540, 5545, 2956, 5541, 5553), 120,
		c(14238, 2974), c(16685, 22414), 15551);
	private static final RetroNpcData DEF_4488 = stand(p(5538, 5540, 5546, 5548, 5541, 2951, 5554, 5552), 125,
		c(14238, 8086, 2974), c(10782, 8732, 22414), 142, 15552);
	private static final RetroNpcData DEF_4489 = spear(p(5535, 5540, 5546, 5547, 5543, 2957, 5552), 118,
		c(14238, 2974), c(10136, 22414), 15551);
	private static final RetroNpcData DEF_4490 = stand(p(5535, 5540, 5546, 2956, 5543, 5554), 124,
		c(14238, 2974), c(16685, 22414), 15551);
	private static final RetroNpcData DEF_4491 = stand(p(2951, 5540, 5545, 5547, 5543, 5549), 130,
		c(14238, 2974), c(9891, 22414), 142);
	private static final RetroNpcData DEF_4492 = spear(p(5535, 5540, 5546, 5548, 2957), 124,
		c(14238, 8086, 2974), c(14656, 8732, 22414), 15551);

	// The generals; 3391 and 3392 are seated at the Recipe for Disaster feast
	private static final short[] GENERAL_RED_FIND = c(14238, 8086, 2974);
	private static final short[] GENERAL_RED_REPLACE = c(10782, 8732, 916);
	private static final short[] GENERAL_GREEN_FIND = c(14238, 8086, 2974, 916);
	private static final short[] GENERAL_GREEN_REPLACE = c(10782, 8732, 25121, 22414);
	private static final RetroNpcData DEF_4493 = stand(p(5535, 16426, 16425, 5551, 5548, 15485), 148,
		GENERAL_RED_FIND, GENERAL_RED_REPLACE, 15551);
	private static final RetroNpcData DEF_4494 = stand(p(2951, 16426, 16425, 5549, 5548, 15483), 148,
		GENERAL_GREEN_FIND, GENERAL_GREEN_REPLACE, 144);
	private static final RetroNpcData DEF_3391 = goblin2006(p(2951, 16426, 16425, 5548), 132,
		GENERAL_GREEN_FIND, GENERAL_GREEN_REPLACE,
		AnimationID._100_GOBTWO_SIT, AnimationID._100_GOBTWO_SIT, 144);
	private static final RetroNpcData DEF_3392 = goblin2006(p(5535, 16426, 16425, 5547), 132,
		GENERAL_RED_FIND, GENERAL_RED_REPLACE,
		AnimationID._100_GOB_SIT, AnimationID._100_GOB_SIT, 15551);

	// Hobgoblin, no head
	private static final RetroNpcData DEF_122 = goblin2006(p(2994), 128, null, null,
		AnimationID.HOBGOBLIN_READY, AnimationID.HOBGOBLIN_WALK);

	private static void register(RetroNpcData data, int... npcIds)
	{
		for (int npcId : npcIds)
		{
			ID_MAPPINGS.put(npcId, data);
		}
	}

	static
	{
		NAME_MAPPINGS.put("goblin", DEF_2274);
		NAME_MAPPINGS.put("goblin guard", DEF_489);
		NAME_MAPPINGS.put("hobgoblin", DEF_122);

		// Lumbridge and its twins
		register(DEF_2274, NpcID.GOBLIN, NpcID.GOBLIN_ARMED_MELEE_3);
		register(DEF_2275, NpcID.GOBLIN_UNARMED_MELEE_1, NpcID.GOBLIN_UNARMED_MELEE_IN_1,
			NpcID.GOBLIN_ARMED_MELEE_4);
		register(DEF_2276, NpcID.GOBLIN_UNARMED_MELEE_2, NpcID.GOBLIN_UNARMED_MELEE_IN_2);
		register(DEF_2277, NpcID.GOBLIN_UNARMED_MELEE_3, NpcID.GOBLIN_UNARMED_MELEE_IN_3);
		register(DEF_2278, NpcID.GOBLIN_UNARMED_MELEE_4, NpcID.GOBLIN_UNARMED_MELEE_IN_4);
		register(DEF_2279, NpcID.GOBLIN_UNARMED_MELEE_5, NpcID.GOBLIN_UNARMED_MELEE_IN_5);
		register(DEF_2280, NpcID.GOBLIN_UNARMED_MELEE_8, NpcID.GOBLIN_UNARMED_MELEE_IN_8);
		register(DEF_2281, NpcID.GOBLIN_ARMED_MELEE_2);

		// Lumbridge 6/7 and armed 1, in id order over 4272-4276
		register(DEF_4272, NpcID.GOBLIN_UNARMED_MELEE_6);
		register(DEF_4273, NpcID.GOBLIN_UNARMED_MELEE_7);
		register(DEF_4274, NpcID.GOBLIN_UNARMED_MELEE_IN_6);
		register(DEF_4275, NpcID.GOBLIN_UNARMED_MELEE_IN_7);
		register(DEF_4276, NpcID.GOBLIN_ARMED_MELEE_1);

		// Dwarf Cannon
		register(DEF_4261, NpcID.MCANNON_GOBLIN1);
		register(DEF_4262, NpcID.GOBLIN_UNARMED_MCANNON_1);
		register(DEF_4263, NpcID.GOBLIN_UNARMED_MCANNON_2);
		register(DEF_4264, NpcID.GOBLIN_UNARMED_MCANNON_3);
		register(DEF_4265, NpcID.GOBLIN_UNARMED_MCANNON_4);
		register(DEF_4266, NpcID.GOBLIN_UNARMED_MCANNON_5);
		register(DEF_4267, NpcID.GOBLIN_UNARMED_MCANNON_6);
		register(DEF_4268, NpcID.GOBLIN_UNARMED_MCANNON_7);
		register(DEF_4269, NpcID.GOBLIN_UNARMED_MCANNON_8);
		register(DEF_4270, NpcID.GOBLIN_UNARMED_MCANNON_9);
		register(DEF_4271, NpcID.OBSERVATORY_GOBLIN_PATROL1, NpcID.OBSERVATORY_GOBLIN_PATROL2);
		register(DEF_4272, NpcID.GOBLIN_ARMED_MCANNON_1);
		register(DEF_4273, NpcID.GOBLIN_ARMED_MCANNON_2);
		register(DEF_4274, NpcID.GOBLIN_ARMED_MCANNON_3);
		register(DEF_4275, NpcID.GOBLIN_ARMED_MCANNON_4);
		register(DEF_4276, NpcID.GOBLIN_ARMED_MCANNON_5, NpcID.MCANNON_GOBLIN_GUARD);

		// Armed, helmeted, guard and armoured
		register(DEF_101, NpcID.GOBLIN_ARMED, NpcID.GODWARS_GOBLIN1, NpcID.GODWARS_GOBLIN2,
			NpcID.GODWARS_GOBLIN3, NpcID.GODWARS_GOBLIN4, NpcID.GODWARS_GOBLIN5,
			NpcID.EYEGLO_GOBLIN_SOLDIER_5);
		register(DEF_3264, NpcID.FAI_BARBARIAN_GOBLIN_ARMED_1, NpcID.EYEGLO_GOBLIN_SOLDIER_1);
		register(DEF_3265, NpcID.FAI_BARBARIAN_GOBLIN_ARMED_2, NpcID.EYEGLO_GOBLIN_SOLDIER_2);
		register(DEF_3266, NpcID.FAI_BARBARIAN_GOBLIN_ARMED_3, NpcID.EYEGLO_GOBLIN_SOLDIER_3);
		register(DEF_3267, NpcID.FAI_BARBARIAN_GOBLIN_ARMED_4, NpcID.EYEGLO_GOBLIN_SOLDIER_4);
		register(DEF_102, NpcID.GOBLIN_HELMET);
		register(DEF_489, NpcID.GOBLIN_GUARD);
		register(DEF_298, NpcID.GOBLIN_GREENARMOUR);
		register(DEF_299, NpcID.GOBLIN_REDARMOUR);

		// Shield of Arrav war goblins
		register(DEF_4407, NpcID.SOS_WAR_GOBLIN_ARMED, NpcID.SOS_WAR_GOBLIN_ARMED2);
		register(DEF_4408, NpcID.SOS_WAR_GOBLIN_HELMET, NpcID.SOS_WAR_GOBLIN_HELMET2);
		register(DEF_4409, NpcID.SOS_WAR_GOBLIN_GREEN_SOLDIER_1, NpcID.SOS_WAR_GOBLIN_GREEN_SOLDIER_2);

		// Goblin Village
		register(DEF_4479, NpcID.GOBLIN_RED_SOLDIER_1, NpcID.GOBLIN_RED_SOLDIER_8,
			NpcID.GOBLIN_RED_SOLDIER_UNDERGROUND, NpcID.XMAS20_GOBLIN_RED,
			NpcID.SLICE_CUTSCENE_SCARED_GOBLIN_1);
		register(DEF_4480, NpcID.GOBLIN_RED_SOLDIER_2);
		register(DEF_4481, NpcID.GOBLIN_RED_SOLDIER_3);
		register(DEF_4482, NpcID.GOBLIN_RED_SOLDIER_4);
		register(DEF_4483, NpcID.GOBLIN_RED_SOLDIER_5);
		register(DEF_4484, NpcID.GOBLIN_RED_SOLDIER_6);
		register(DEF_4485, NpcID.GOBLIN_RED_SOLDIER_7);
		register(DEF_4486, NpcID.GOBLIN_GREEN_SOLDIER_1, NpcID.GOBLIN_GREEN_SOLDIER_8,
			NpcID.XMAS20_GOBLIN_GREEN, NpcID.SLICE_CUTSCENE_FIREBOLT_GOBLIN,
			NpcID.SLICE_CUTSCENE_ARROW_GOBLIN);
		register(DEF_4487, NpcID.GOBLIN_GREEN_SOLDIER_2);
		register(DEF_4488, NpcID.GOBLIN_GREEN_SOLDIER_3);
		register(DEF_4489, NpcID.GOBLIN_GREEN_SOLDIER_4);
		register(DEF_4490, NpcID.GOBLIN_GREEN_SOLDIER_5);
		register(DEF_4491, NpcID.GOBLIN_GREEN_SOLDIER_6);
		register(DEF_4492, NpcID.GOBLIN_GREEN_SOLDIER_7);

		// A Soul's Bane, defs 3648 and 3663
		register(RetroNpcMapping.ANGRY_GOBLIN, NpcID.SOULBANE_ANGER_GOBLIN, NpcID.SOULBANE_GOBLIN);

		// The generals. Deliberately left out: the HUNDRED_GOBLIN ids, which live still draws from
		// defs 3407-3412
		register(DEF_4493, NpcID.GENERAL_BENTNOZE_RED, NpcID.SLICE_CUTSCENE_BENTNOZE,
			NpcID.GENERAL_BENTNOZE_RED_NOOP);
		register(DEF_4494, NpcID.GENERAL_WARTFACE_GREEN, NpcID.SLICE_CUTSCENE_WARTFACE,
			NpcID.GENERAL_WARTFACE_GREEN_NOOP);
		register(DEF_3391, NpcID._100_GENERAL_WARTFACE);
		register(DEF_3392, NpcID._100_GENERAL_BENTNOZE);

		// Grubfoot, defs 4496-4498
		register(RetroNpcMapping.GRUBFOOT_BROWN, NpcID.CATWALK_GOBLIN_BROWN, NpcID.CATWALK_GOBLIN_BROWN_NOOP,
			NpcID.SLICE_CUTSCENE_GRUBFOOT, NpcID.LOTG_GRUBFOOT, NpcID.LOTG_GRUBFOOT_FOLLOWER);
		register(RetroNpcMapping.GRUBFOOT_ORANGE, NpcID.CATWALK_GOBLIN_ORANGE, NpcID.CATWALK_GOBLIN_ORANGE_NOOP);
		register(RetroNpcMapping.GRUBFOOT_BLUE, NpcID.CATWALK_GOBLIN_BLUE, NpcID.CATWALK_GOBLIN_BLUE_NOOP);
	}

	private RetroGoblinMapping2006()
	{
	}

	/**
	 * @param nameLower lower-cased trimmed NPC name, or null
	 */
	static RetroNpcData get(int npcId, String nameLower)
	{
		RetroNpcData byId = ID_MAPPINGS.get(npcId);
		if (byId != null)
		{
			return byId;
		}
		return nameLower == null ? null : NAME_MAPPINGS.get(nameLower);
	}

	/**
	 * Every live model id a row references.
	 */
	public static Set<Integer> modelIds()
	{
		Set<Integer> ids = new HashSet<>();
		for (RetroNpcData data : ID_MAPPINGS.values())
		{
			addModelIds(ids, data);
		}
		for (RetroNpcData data : NAME_MAPPINGS.values())
		{
			addModelIds(ids, data);
		}
		return ids;
	}

	private static void addModelIds(Set<Integer> ids, RetroNpcData data)
	{
		for (int modelId : data.getRetroModelIds())
		{
			ids.add(modelId);
		}
		if (data.hasChathead())
		{
			for (int modelId : data.getChatheadModelIds())
			{
				ids.add(modelId);
			}
		}
	}
}
