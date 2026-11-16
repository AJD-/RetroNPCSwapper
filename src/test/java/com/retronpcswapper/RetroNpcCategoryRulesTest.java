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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.junit.Test;
import static com.retronpcswapper.RetroNpcCategory.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins which config toggle owns each category, and which categories need the converted assets.
 */
public class RetroNpcCategoryRulesTest
{
	private static final List<Function<RetroNpcConfig, Boolean>> TOGGLES = Arrays.asList(
		RetroNpcConfig::useInjectionPipeline,
		RetroNpcConfig::swapChickens, RetroNpcConfig::swapSkeletons, RetroNpcConfig::swapZombies,
		RetroNpcConfig::swapGiants, RetroNpcConfig::swapGhosts, RetroNpcConfig::swapDragons,
		RetroNpcConfig::swapDemons, RetroNpcConfig::swapImps, RetroNpcConfig::swapCyclops,
		RetroNpcConfig::swapGuards, RetroNpcConfig::swapCows, RetroNpcConfig::swapHellhounds,
		RetroNpcConfig::swapGiantRats, RetroNpcConfig::swapScorpions, RetroNpcConfig::swapGiantSpiders,
		RetroNpcConfig::swapSmallSpiders, RetroNpcConfig::swapElves, RetroNpcConfig::swapBears,
		RetroNpcConfig::swapKalphites, RetroNpcConfig::swapRoundTable, RetroNpcConfig::swapVarrockNpcs,
		RetroNpcConfig::swapFishingSpots);

	/** Every toggle stubbed on: a bare mock answers false, and a null era reads as not Modern. */
	private static RetroNpcConfig allOn()
	{
		RetroNpcConfig config = mock(RetroNpcConfig.class);
		for (Function<RetroNpcConfig, Boolean> toggle : TOGGLES)
		{
			when(toggle.apply(config)).thenReturn(true);
		}
		when(config.goblinEra()).thenReturn(GoblinEra.RETRO_2005);
		return config;
	}

	private static void assertOnlyOff(RetroNpcConfig config, RetroNpcCategory... off)
	{
		Set<RetroNpcCategory> expected = EnumSet.allOf(RetroNpcCategory.class);
		expected.removeAll(Arrays.asList(off));

		Set<RetroNpcCategory> enabled = EnumSet.noneOf(RetroNpcCategory.class);
		for (RetroNpcCategory category : RetroNpcCategory.values())
		{
			if (RetroNpcSwapperPlugin.isCategoryEnabled(category, config))
			{
				enabled.add(category);
			}
		}
		assertEquals(expected, enabled);
	}

	private static void assertOwns(Function<RetroNpcConfig, Boolean> toggle, RetroNpcCategory... owned)
	{
		RetroNpcConfig config = allOn();
		when(toggle.apply(config)).thenReturn(false);
		assertOnlyOff(config, owned);
	}

	@Test
	public void testEveryCategoryIsOnWithEveryToggleOn()
	{
		assertOnlyOff(allOn());
	}

	@Test
	public void testNoCategoryIsNeverEnabled()
	{
		assertFalse(RetroNpcSwapperPlugin.isCategoryEnabled(null, allOn()));
	}

	@Test
	public void testEachToggleOwnsItsCategories()
	{
		assertOwns(RetroNpcConfig::swapChickens, CHICKENS);
		assertOwns(RetroNpcConfig::swapSkeletons, SKELETONS, SKELETON_MAGES);
		assertOwns(RetroNpcConfig::swapZombies, ZOMBIES);
		assertOwns(RetroNpcConfig::swapGiants, HILL_GIANTS, FIRE_GIANTS, ICE_GIANTS, MOSS_GIANTS);
		assertOwns(RetroNpcConfig::swapGhosts, GHOSTS);
		assertOwns(RetroNpcConfig::swapDragons, ADULT_DRAGONS, BABY_DRAGONS);
		assertOwns(RetroNpcConfig::swapDemons, LESSER_DEMONS, GREATER_DEMONS, BLACK_DEMONS);
		assertOwns(RetroNpcConfig::swapImps, IMPS);
		assertOwns(RetroNpcConfig::swapCyclops, CYCLOPS);
		assertOwns(RetroNpcConfig::swapGuards, GUARDS);
		assertOwns(RetroNpcConfig::swapCows, COWS);
		assertOwns(RetroNpcConfig::swapHellhounds, HELLHOUNDS);
		assertOwns(RetroNpcConfig::swapGiantRats, GIANT_RATS);
		assertOwns(RetroNpcConfig::swapScorpions, SCORPIONS, SMALL_SCORPIONS);
		assertOwns(RetroNpcConfig::swapGiantSpiders, GIANT_SPIDERS);
		assertOwns(RetroNpcConfig::swapSmallSpiders, SMALL_SPIDERS);
		assertOwns(RetroNpcConfig::swapElves, ELVES);
		assertOwns(RetroNpcConfig::swapBears, BEARS);
		assertOwns(RetroNpcConfig::swapKalphites, KALPHITES);
		assertOwns(RetroNpcConfig::swapRoundTable, ROUND_TABLE);
		assertOwns(RetroNpcConfig::swapVarrockNpcs, VARROCK);
		assertOwns(RetroNpcConfig::swapFishingSpots, FISHING_SPOTS);
	}

	@Test
	public void testTheModernEraTurnsOffGoblinsAlone()
	{
		RetroNpcConfig config = allOn();
		when(config.goblinEra()).thenReturn(GoblinEra.MODERN);
		assertOnlyOff(config, GOBLINS);

		when(config.goblinEra()).thenReturn(GoblinEra.RETRO_2006);
		assertOnlyOff(config);
	}

	@Test
	public void testConvertedAssetsOffTurnsOffTheBundleOnlyCategories()
	{
		assertOwns(RetroNpcConfig::useInjectionPipeline,
			ADULT_DRAGONS, BABY_DRAGONS, LESSER_DEMONS, GREATER_DEMONS, BLACK_DEMONS, IMPS,
			FIRE_GIANTS, ICE_GIANTS, MOSS_GIANTS, CYCLOPS, GUARDS, SKELETON_MAGES, COWS, SCORPIONS,
			ELVES, KALPHITES, ROUND_TABLE, VARROCK);
	}
	private static final Set<RetroNpcCategory> BUNDLE_ONLY = EnumSet.of(
		ADULT_DRAGONS, BABY_DRAGONS, LESSER_DEMONS, GREATER_DEMONS, BLACK_DEMONS, IMPS,
		FIRE_GIANTS, ICE_GIANTS, MOSS_GIANTS, CYCLOPS, GUARDS, SKELETON_MAGES, COWS, SCORPIONS,
		ELVES, KALPHITES, ROUND_TABLE, VARROCK);

	@Test
	public void testTheCategoriesOnlyTheBundleCanDraw()
	{
		Set<RetroNpcCategory> bundlePreferred = EnumSet.copyOf(BUNDLE_ONLY);
		bundlePreferred.add(HILL_GIANTS);

		for (RetroNpcCategory category : RetroNpcCategory.values())
		{
			assertEquals(category.name(), BUNDLE_ONLY.contains(category), category.requiresInjectedGeometry());
			assertEquals(category.name(), bundlePreferred.contains(category), category.usesInjectedGeometry());
		}
	}

	@Test
	public void testTheCategoriesTakingTheirDefinitionsRecolors()
	{
		Set<RetroNpcCategory> recolored = EnumSet.of(
			ADULT_DRAGONS, BABY_DRAGONS, LESSER_DEMONS, GREATER_DEMONS, BLACK_DEMONS,
			FIRE_GIANTS, ICE_GIANTS, MOSS_GIANTS, GUARDS, SKELETON_MAGES, CHICKENS);

		for (RetroNpcCategory category : RetroNpcCategory.values())
		{
			assertEquals(category.name(), recolored.contains(category), category.usesDefinitionRecolors());
		}
	}

	@Test
	public void testTheCategoriesMatchedByIdAlone()
	{
		Set<RetroNpcCategory> idOnly = EnumSet.of(GUARDS, ELVES, ROUND_TABLE, VARROCK);

		for (RetroNpcCategory category : RetroNpcCategory.values())
		{
			assertEquals(category.name(), idOnly.contains(category), category.isIdOnly());
		}
	}
}
