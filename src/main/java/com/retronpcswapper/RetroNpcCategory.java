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
import java.util.Set;
import java.util.function.Predicate;

/**
 * NPC classification category for Retro NPC Swapper config toggles. Each names the toggle that
 * owns it, where its 2005 geometry comes from, and any {@link Trait}s.
 */
public enum RetroNpcCategory
{
	LESSER_DEMONS(RetroNpcConfig::swapDemons, Geometry.BUNDLE_ONLY, Trait.DEFINITION_RECOLORS),
	GREATER_DEMONS(RetroNpcConfig::swapDemons, Geometry.BUNDLE_ONLY, Trait.DEFINITION_RECOLORS),
	BLACK_DEMONS(RetroNpcConfig::swapDemons, Geometry.BUNDLE_ONLY, Trait.DEFINITION_RECOLORS),
	ADULT_DRAGONS(RetroNpcConfig::swapDragons, Geometry.BUNDLE_ONLY, Trait.DEFINITION_RECOLORS),
	BABY_DRAGONS(RetroNpcConfig::swapDragons, Geometry.BUNDLE_ONLY, Trait.DEFINITION_RECOLORS),
	GOBLINS(config -> config.goblinEra() != GoblinEra.MODERN, Geometry.CACHE),
	// Shares the Goblins dropdown; the same rows in both eras
	CAVE_GOBLINS(config -> config.goblinEra() != GoblinEra.MODERN, Geometry.BUNDLE_ONLY, Trait.ID_ONLY),
	GUARDS(RetroNpcConfig::swapGuards, Geometry.BUNDLE_ONLY, Trait.DEFINITION_RECOLORS, Trait.ID_ONLY),
	IMPS(RetroNpcConfig::swapImps, Geometry.BUNDLE_ONLY),
	SKELETONS(RetroNpcConfig::swapSkeletons, Geometry.CACHE),
	// Shares the Skeletons toggle
	SKELETON_MAGES(RetroNpcConfig::swapSkeletons, Geometry.BUNDLE_ONLY, Trait.DEFINITION_RECOLORS),
	ZOMBIES(RetroNpcConfig::swapZombies, Geometry.CACHE),
	GHOSTS(RetroNpcConfig::swapGhosts, Geometry.CACHE),
	HILL_GIANTS(RetroNpcConfig::swapGiants, Geometry.BUNDLE_PREFERRED),
	FIRE_GIANTS(RetroNpcConfig::swapGiants, Geometry.BUNDLE_ONLY, Trait.DEFINITION_RECOLORS),
	ICE_GIANTS(RetroNpcConfig::swapGiants, Geometry.BUNDLE_ONLY, Trait.DEFINITION_RECOLORS),
	MOSS_GIANTS(RetroNpcConfig::swapGiants, Geometry.BUNDLE_ONLY, Trait.DEFINITION_RECOLORS),
	CYCLOPS(RetroNpcConfig::swapCyclops, Geometry.BUNDLE_ONLY),
	CHICKENS(RetroNpcConfig::swapChickens, Geometry.CACHE, Trait.DEFINITION_RECOLORS),
	COWS(RetroNpcConfig::swapCows, Geometry.BUNDLE_ONLY),
	HELLHOUNDS(RetroNpcConfig::swapHellhounds, Geometry.CACHE),
	GIANT_RATS(RetroNpcConfig::swapGiantRats, Geometry.CACHE),
	SCORPIONS(RetroNpcConfig::swapScorpions, Geometry.BUNDLE_ONLY),
	// Shares the Scorpions toggle
	SMALL_SCORPIONS(RetroNpcConfig::swapScorpions, Geometry.CACHE),
	ELVES(RetroNpcConfig::swapElves, Geometry.BUNDLE_ONLY, Trait.ID_ONLY),
	SMALL_SPIDERS(RetroNpcConfig::swapSmallSpiders, Geometry.CACHE),
	GIANT_SPIDERS(RetroNpcConfig::swapGiantSpiders, Geometry.CACHE),
	BEARS(RetroNpcConfig::swapBears, Geometry.CACHE),
	KALPHITES(RetroNpcConfig::swapKalphites, Geometry.BUNDLE_ONLY),
	ROUND_TABLE(RetroNpcConfig::swapRoundTable, Geometry.BUNDLE_ONLY, Trait.ID_ONLY),
	VARROCK(RetroNpcConfig::swapVarrockNpcs, Geometry.BUNDLE_ONLY, Trait.ID_ONLY),
	BARBARIANS(RetroNpcConfig::swapBarbarians, Geometry.BUNDLE_ONLY, Trait.ID_ONLY),
	SHEEP(RetroNpcConfig::swapSheep, Geometry.BUNDLE_ONLY, Trait.ID_ONLY),
	UNICORNS(RetroNpcConfig::swapUnicorns, Geometry.BUNDLE_ONLY, Trait.ID_ONLY),
	// Shares the Unicorns toggle
	ANGRY_UNICORNS(RetroNpcConfig::swapUnicorns, Geometry.CACHE, Trait.ID_ONLY),
	MISC_BIRDS(RetroNpcConfig::swapMiscBirds, Geometry.CACHE, Trait.ID_ONLY),
	// Shares the Misc Birds toggle
	MISC_BIRD_DUCKS(RetroNpcConfig::swapMiscBirds, Geometry.BUNDLE_ONLY, Trait.ID_ONLY),
	FISHING_SPOTS(RetroNpcConfig::swapFishingSpots, Geometry.CACHE);

	/** Where a category's 2005 geometry and clips come from. */
	enum Geometry
	{
		/** The live cache still holds them. */
		CACHE,
		/** The live cache can draw it, the bundle draws it better. */
		BUNDLE_PREFERRED,
		/** Only the bundle: the live ids hold unrelated or re-rigged assets. */
		BUNDLE_ONLY
	}

	enum Trait
	{
		/** One mesh serves several NPCs, told apart by their 2005 definition's recolor pairs. */
		DEFINITION_RECOLORS,
		/** Matched by registered id only, never by name. */
		ID_ONLY
	}

	private final Predicate<RetroNpcConfig> toggle;
	private final Geometry geometry;
	private final Set<Trait> traits;

	RetroNpcCategory(Predicate<RetroNpcConfig> toggle, Geometry geometry, Trait... traits)
	{
		this.toggle = toggle;
		this.geometry = geometry;
		this.traits = traits.length == 0 ? EnumSet.noneOf(Trait.class) : EnumSet.copyOf(Arrays.asList(traits));
	}

	/** Whether the config asks for this category, and has the converted assets on if it needs them. */
	public boolean isEnabled(RetroNpcConfig config)
	{
		return toggle.test(config) && (!requiresInjectedGeometry() || config.useInjectionPipeline());
	}

	/** Whether only the bundle can draw it, so there is no cache-backed fallback. */
	public boolean requiresInjectedGeometry()
	{
		return geometry == Geometry.BUNDLE_ONLY;
	}

	/** Whether it is drawn from the bundle when the pipeline is on. */
	public boolean usesInjectedGeometry()
	{
		return geometry != Geometry.CACHE;
	}

	public boolean usesDefinitionRecolors()
	{
		return traits.contains(Trait.DEFINITION_RECOLORS);
	}

	public boolean isIdOnly()
	{
		return traits.contains(Trait.ID_ONLY);
	}
}
