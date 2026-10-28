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

import com.retronpcswapper.inject.RetroAssetBundle;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RetroGoblinMapping2006Test
{
	private static RetroNpcData row(int npcId)
	{
		return RetroNpcMapping.get(npcId, "Goblin", GoblinEra.RETRO_2006);
	}

	@Test
	public void testLumbridgeGoblinIsDef2274()
	{
		RetroNpcData d = row(NpcID.GOBLIN);
		assertEquals(RetroNpcCategory.GOBLINS, d.getCategory());
		assertArrayEquals(new int[]{5536, 5539, 5544, 5547}, d.getRetroModelIds());
		assertEquals(125, d.getScaleXZ());
		assertEquals(125, d.getScaleY());
		assertEquals(AnimationID.GOBLIN_READY, d.getIdleAnimationId());
		assertEquals(AnimationID.GOBLIN_WALK, d.getWalkAnimationId());
		assertEquals(AnimationID.GOBLIN_ATTACK_UNARMED, d.getAttackAnimationId());
		assertEquals(AnimationID.GOBLIN_BLOCK, d.getDefendAnimationId());
		assertEquals(AnimationID.GOBLIN_DEATH, d.getDeathAnimationId());
		assertArrayEquals(new int[]{15551}, d.getChatheadModelIds());
		assertEquals(RetroAssetBundle.GOBLIN_CHATHEAD_KEY_BASE, d.getChatheadKeyBase());
	}

	@Test
	public void testRedSoldierIsDef4479WithDedupedRecolours()
	{
		RetroNpcData d = row(NpcID.GOBLIN_RED_SOLDIER_1);
		assertArrayEquals(new int[]{5537, 5540, 5546, 5547, 5543, 2951, 5552, 5549}, d.getRetroModelIds());
		assertArrayEquals(new short[]{14238, 2974}, d.getOriginalColors());
		assertArrayEquals(new short[]{10782, 916}, d.getReplacementColors());
		assertArrayEquals(new int[]{142, 15550}, d.getChatheadModelIds());
		assertEquals(130, d.getScaleXZ());
		assertEquals(130, d.getScaleY());
		assertSame(d, row(NpcID.GOBLIN_RED_SOLDIER_UNDERGROUND));
		assertSame(d, row(NpcID.XMAS20_GOBLIN_RED));
		assertSame(d, row(NpcID.SLICE_CUTSCENE_SCARED_GOBLIN_1));
		assertArrayEquals(d.getRetroModelIds(), row(NpcID.GOBLIN_RED_SOLDIER_8).getRetroModelIds());
	}

	@Test
	public void testSpearAndShieldSoldierStandsInSpearPose()
	{
		RetroNpcData d = row(NpcID.GOBLIN_RED_SOLDIER_2);
		assertArrayEquals(new int[]{5535, 5540, 5545, 2956, 5541, 5553}, d.getRetroModelIds());
		assertEquals(AnimationID.GOBLIN_READY_SPEAR, d.getIdleAnimationId());
		assertEquals(AnimationID.GOBLIN_WALK_SPEAR, d.getWalkAnimationId());
		assertEquals(AnimationID.GOBLIN_ATTACK_UNARMED, d.getAttackAnimationId());
		assertEquals(AnimationID.GOBLIN_BLOCK, d.getDefendAnimationId());
		assertEquals(AnimationID.GOBLIN_DEATH, d.getDeathAnimationId());
		assertArrayEquals(new short[]{14238, 2974}, d.getOriginalColors());
		assertArrayEquals(new short[]{16685, 916}, d.getReplacementColors());
	}

	@Test
	public void testGreenArmourCarriesNoDriftCorrection()
	{
		RetroNpcData d = row(NpcID.GOBLIN_GREENARMOUR);
		assertArrayEquals(new int[]{2951, 2953, 2955, 2956, 2957}, d.getRetroModelIds());
		assertArrayEquals(new short[]{916}, d.getOriginalColors());
		assertArrayEquals(new short[]{22443}, d.getReplacementColors());
		assertArrayEquals(new int[]{142}, d.getChatheadModelIds());
		assertEquals(AnimationID.GOBLIN_READY_SPEAR, d.getIdleAnimationId());
		RetroNpcData red = row(NpcID.GOBLIN_REDARMOUR);
		assertArrayEquals(new short[]{933}, red.getReplacementColors());
		assertArrayEquals(new int[]{144}, red.getChatheadModelIds());
	}

	@Test
	public void testNoRowCarriesTheWeaponDriftPair()
	{
		for (RetroNpcData d : allRows())
		{
			if (d.getOriginalColors() == null)
			{
				continue;
			}
			for (short c : d.getOriginalColors())
			{
				assertTrue(c != 70 && c != 8084);
			}
			for (short c : d.getReplacementColors())
			{
				assertTrue(c != -22417 && c != 528);
			}
		}
	}

	@Test
	public void testSosWarGoblinsPairByCombatLevel()
	{
		RetroNpcData d = row(NpcID.SOS_WAR_GOBLIN_HELMET2);
		assertArrayEquals(new int[]{2952, 2953, 2955, 2956}, d.getRetroModelIds());
		assertArrayEquals(new int[]{2951, 2953, 2955, 2956, 2957}, row(NpcID.SOS_WAR_GOBLIN_ARMED).getRetroModelIds());
		assertArrayEquals(new int[]{2952, 2953, 2955, 2956}, row(NpcID.SOS_WAR_GOBLIN_HELMET).getRetroModelIds());
		RetroNpcData green = row(NpcID.SOS_WAR_GOBLIN_GREEN_SOLDIER_1);
		assertArrayEquals(new int[]{5535, 5540, 5546, 5547, 5552, 2957}, green.getRetroModelIds());
		assertEquals(AnimationID.GOBLIN_READY_SPEAR, green.getIdleAnimationId());
		assertFalse(green.hasChathead());
	}

	@Test
	public void testNameRowsCoverGoblinAndGoblinGuard()
	{
		assertArrayEquals(row(NpcID.GOBLIN).getRetroModelIds(),
			RetroNpcMapping.get(0, "Goblin", GoblinEra.RETRO_2006).getRetroModelIds());
		RetroNpcData guard = RetroNpcMapping.get(0, "Goblin guard", GoblinEra.RETRO_2006);
		assertArrayEquals(new int[]{2951, 2953, 2955, 2956, 2957}, guard.getRetroModelIds());
		assertArrayEquals(new int[]{142}, guard.getChatheadModelIds());
		assertSame(guard, row(NpcID.GOBLIN_GUARD));
	}

	@Test
	public void testPairingTable()
	{
		assertSame(row(NpcID.GOBLIN_UNARMED_MELEE_1), row(NpcID.GOBLIN_UNARMED_MELEE_IN_1));
		assertSame(row(NpcID.GOBLIN_UNARMED_MELEE_8), row(NpcID.GOBLIN_UNARMED_MELEE_IN_8));
		assertArrayEquals(new int[]{2951, 5539, 5545, 5547, 5543}, row(NpcID.GOBLIN_UNARMED_MELEE_8).getRetroModelIds());
		assertSame(row(NpcID.GOBLIN), row(NpcID.GOBLIN_ARMED_MELEE_3));
		assertSame(row(NpcID.GOBLIN_UNARMED_MELEE_1), row(NpcID.GOBLIN_ARMED_MELEE_4));
		assertArrayEquals(new int[]{5536, 5542, 5545, 5548}, row(NpcID.GOBLIN_ARMED_MELEE_2).getRetroModelIds());
		assertSame(row(NpcID.GOBLIN_ARMED), row(NpcID.GODWARS_GOBLIN5));
		assertSame(row(NpcID.GOBLIN_ARMED), row(NpcID.EYEGLO_GOBLIN_SOLDIER_5));
		assertSame(row(NpcID.FAI_BARBARIAN_GOBLIN_ARMED_1), row(NpcID.EYEGLO_GOBLIN_SOLDIER_1));
		assertArrayEquals(new int[]{5535, 5540, 5546, 2957, 5547, 5543}, row(NpcID.FAI_BARBARIAN_GOBLIN_ARMED_1).getRetroModelIds());
		assertArrayEquals(new int[]{2952, 2953, 2955, 2956}, row(NpcID.GOBLIN_HELMET).getRetroModelIds());
		assertSame(row(NpcID.GOBLIN_ARMED_MCANNON_5), row(NpcID.MCANNON_GOBLIN_GUARD));
		assertArrayEquals(new int[]{2951, 5541, 5544, 5551, 5547, 5542}, row(NpcID.MCANNON_GOBLIN_GUARD).getRetroModelIds());
		assertArrayEquals(new int[]{2951, 2953, 2955, 5547, 5537}, row(NpcID.MCANNON_GOBLIN1).getRetroModelIds());
		assertFalse(row(NpcID.MCANNON_GOBLIN1).hasChathead());
		assertArrayEquals(new int[]{2951, 5542, 5544, 5548, 5538}, row(NpcID.OBSERVATORY_GOBLIN_PATROL2).getRetroModelIds());
		assertSame(row(NpcID.GOBLIN_GREEN_SOLDIER_1), row(NpcID.GOBLIN_GREEN_SOLDIER_8));
		assertSame(row(NpcID.GOBLIN_GREEN_SOLDIER_1), row(NpcID.SLICE_CUTSCENE_ARROW_GOBLIN));
		assertEquals(22414, row(NpcID.GOBLIN_GREEN_SOLDIER_1).getReplacementColors()[1]);
	}

	@Test
	public void testExcludedIdsStayExcluded()
	{
		assertNull(RetroNpcMapping.get(NpcID.PATTERN_GOBLIN1_DESK, "Goblin", GoblinEra.RETRO_2006));
	}

	@Test
	public void testEveryMappedLiveGoblinHasA2006Row()
	{
		int[] ids = {
			NpcID.GOBLIN_ARMED, NpcID.GOBLIN_ARMED_MELEE_1,
			NpcID.GOBLIN_UNARMED_MELEE_6, NpcID.GOBLIN_UNARMED_MELEE_7,
			NpcID.GOBLIN_UNARMED_MELEE_IN_6, NpcID.GOBLIN_UNARMED_MELEE_IN_7,
			NpcID.FAI_BARBARIAN_GOBLIN_ARMED_1, NpcID.FAI_BARBARIAN_GOBLIN_ARMED_2,
			NpcID.FAI_BARBARIAN_GOBLIN_ARMED_3, NpcID.FAI_BARBARIAN_GOBLIN_ARMED_4,
			NpcID.MCANNON_GOBLIN_GUARD,
			NpcID.GOBLIN_ARMED_MCANNON_1, NpcID.GOBLIN_ARMED_MCANNON_2, NpcID.GOBLIN_ARMED_MCANNON_3,
			NpcID.GOBLIN_ARMED_MCANNON_4, NpcID.GOBLIN_ARMED_MCANNON_5,
			NpcID.EYEGLO_GOBLIN_SOLDIER_1, NpcID.EYEGLO_GOBLIN_SOLDIER_2, NpcID.EYEGLO_GOBLIN_SOLDIER_3,
			NpcID.EYEGLO_GOBLIN_SOLDIER_4, NpcID.EYEGLO_GOBLIN_SOLDIER_5,
			NpcID.SOS_WAR_GOBLIN_ARMED, NpcID.SOS_WAR_GOBLIN_ARMED2,
			NpcID.SOS_WAR_GOBLIN_HELMET, NpcID.SOS_WAR_GOBLIN_HELMET2,
			NpcID.SOS_WAR_GOBLIN_GREEN_SOLDIER_1, NpcID.SOS_WAR_GOBLIN_GREEN_SOLDIER_2,
			NpcID.GODWARS_GOBLIN1, NpcID.GODWARS_GOBLIN2, NpcID.GODWARS_GOBLIN3,
			NpcID.GODWARS_GOBLIN4, NpcID.GODWARS_GOBLIN5,
			NpcID.GOBLIN_HELMET,
			NpcID.GOBLIN_GREEN_SOLDIER_1, NpcID.GOBLIN_GREEN_SOLDIER_2, NpcID.GOBLIN_GREEN_SOLDIER_3,
			NpcID.GOBLIN_GREEN_SOLDIER_4, NpcID.GOBLIN_GREEN_SOLDIER_5, NpcID.GOBLIN_GREEN_SOLDIER_6,
			NpcID.GOBLIN_GREEN_SOLDIER_7, NpcID.GOBLIN_GREEN_SOLDIER_8,
			NpcID.GOBLIN_GREENARMOUR, NpcID.XMAS20_GOBLIN_GREEN,
			NpcID.SLICE_CUTSCENE_FIREBOLT_GOBLIN, NpcID.SLICE_CUTSCENE_ARROW_GOBLIN,
			NpcID.GOBLIN_RED_SOLDIER_1, NpcID.GOBLIN_RED_SOLDIER_2, NpcID.GOBLIN_RED_SOLDIER_3,
			NpcID.GOBLIN_RED_SOLDIER_4, NpcID.GOBLIN_RED_SOLDIER_5, NpcID.GOBLIN_RED_SOLDIER_6,
			NpcID.GOBLIN_RED_SOLDIER_7, NpcID.GOBLIN_RED_SOLDIER_8,
			NpcID.GOBLIN_RED_SOLDIER_UNDERGROUND, NpcID.GOBLIN_REDARMOUR, NpcID.XMAS20_GOBLIN_RED,
			NpcID.SLICE_CUTSCENE_SCARED_GOBLIN_1, NpcID.GOBLIN_GUARD,
			NpcID.GOBLIN, NpcID.GOBLIN_UNARMED_MELEE_1, NpcID.GOBLIN_UNARMED_MELEE_2,
			NpcID.GOBLIN_UNARMED_MELEE_3, NpcID.GOBLIN_UNARMED_MELEE_4, NpcID.GOBLIN_UNARMED_MELEE_5,
			NpcID.GOBLIN_UNARMED_MELEE_8,
			NpcID.GOBLIN_UNARMED_MELEE_IN_1, NpcID.GOBLIN_UNARMED_MELEE_IN_2,
			NpcID.GOBLIN_UNARMED_MELEE_IN_3, NpcID.GOBLIN_UNARMED_MELEE_IN_4,
			NpcID.GOBLIN_UNARMED_MELEE_IN_5, NpcID.GOBLIN_UNARMED_MELEE_IN_8,
			NpcID.GOBLIN_ARMED_MELEE_2, NpcID.GOBLIN_ARMED_MELEE_3, NpcID.GOBLIN_ARMED_MELEE_4,
			NpcID.MCANNON_GOBLIN1, NpcID.GOBLIN_UNARMED_MCANNON_1, NpcID.GOBLIN_UNARMED_MCANNON_9,
			NpcID.OBSERVATORY_GOBLIN_PATROL1, NpcID.OBSERVATORY_GOBLIN_PATROL2,
		};
		for (int id : ids)
		{
			assertNotNull("npc " + id, row(id));
			assertEquals(RetroNpcCategory.GOBLINS, row(id).getCategory());
		}
	}

	@Test
	public void testModelIdsAreTheSpikeSet()
	{
		Set<Integer> expected = new HashSet<>();
		for (int id : new int[]{2951, 2952, 2953, 2955, 2956, 2957, 142, 144, 15550, 15551, 15552})
		{
			expected.add(id);
		}
		for (int id = 5535; id <= 5554; id++)
		{
			expected.add(id);
		}
		assertEquals(31, expected.size());
		assertEquals(expected, RetroGoblinMapping2006.modelIds());
	}

	private static Set<RetroNpcData> allRows()
	{
		Set<RetroNpcData> rows = new HashSet<>();
		for (int id = 0; id < 12000; id++)
		{
			RetroNpcData d = RetroGoblinMapping2006.get(id, null);
			if (d != null)
			{
				rows.add(d);
			}
		}
		rows.add(RetroGoblinMapping2006.get(0, "goblin"));
		rows.add(RetroGoblinMapping2006.get(0, "goblin guard"));
		return rows;
	}
}
