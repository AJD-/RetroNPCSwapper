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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(RetroNpcConfig.GROUP)
public interface RetroNpcConfig extends Config
{
	String GROUP = "retronpcswapper";

	/**
	 * Retired in favour of {@link #swapDragons()} and {@link #swapDemons()}. Kept only so
	 * {@code migrateDragonsAndDemons} can carry a saved value across; nothing reads it as a setting.
	 */
	String LEGACY_DRAGONS_AND_DEMONS = "swapDragonsAndDemons";

	/**
	 * Retired in favour of {@link #swapGiants()}, which covers the whole family rather than one
	 * member of it. Kept only so a saved value can be carried across; nothing reads it as a setting.
	 */
	String LEGACY_HILL_GIANTS = "swapHillGiants";

	/** Written by the migration as well as the user, so both are named rather than repeated. */
	String SWAP_DRAGONS = "swapDragons";
	String SWAP_DEMONS = "swapDemons";
	String SWAP_GIANTS = "swapGiants";

	/** Also written programmatically, so the key is named rather than repeated as a literal. */
	String OVERRIDE_INTERACT_HIGHLIGHT = "overrideInteractHighlight";
	String USE_INJECTION_PIPELINE = "useInjectionPipeline";

	/**
	 * Read-only notice for users, not a setting.
	 */
	@ConfigItem(
		keyName = "gpuRequiredNotice",
		name = "<html><body style='width:170px'>This plugin requires the <b>GPU</b> plugin to be"
			+ " enabled. Models are swapped as the scene is drawn, so nothing changes while GPU"
			+ " rendering is off, or while another renderer such as 117 HD is in use.</body></html>",
		description = "Retro models are substituted while the GPU plugin renders the scene, so it must be enabled.",
		position = 0
	)
	default void gpuRequiredNotice()
	{
	}

	@ConfigSection(
		name = "Misc NPCs",
		description = "Retro models and animations for miscellaneous NPCs",
		position = 1,
		closedByDefault = false
	)
	String miscSection = "miscSection";

	@ConfigItem(
		keyName = "swapChickens",
		name = "Chickens",
		description = "Swap modern Chicken models and animations to their 2004/2005 retro variants.",
		section = miscSection,
		position = 1
	)
	default boolean swapChickens()
	{
		return true;
	}

	@ConfigItem(
		keyName = "swapGoblins",
		name = "Goblins",
		description = "Swap modern Goblin models and animations to their 2004/2005 retro variants.",
		section = miscSection,
		position = 2
	)
	default boolean swapGoblins()
	{
		return true;
	}

	@ConfigItem(
		keyName = "swapSkeletons",
		name = "Skeletons",
		description = "Swap modern Skeleton models and animations to their 2004/2005 retro variants.",
		section = miscSection,
		position = 3
	)
	default boolean swapSkeletons()
	{
		return true;
	}

	@ConfigItem(
		keyName = "swapZombies",
		name = "Zombies",
		description = "Swap modern Zombie models and animations to their 2004/2005 retro variants.",
		section = miscSection,
		position = 4
	)
	default boolean swapZombies()
	{
		return true;
	}

	@ConfigItem(
		keyName = SWAP_GIANTS,
		name = "Giants",
		description = "<html><body style='width:170px'>Swap modern Hill, Fire, Ice and Moss Giant "
			+ "models and animations to their 2004/2005 retro variants.<br><br>Hill giants work on "
			+ "their own. The other three need <b>Use the injection pipeline</b>, because their 2005 "
			+ "heads no longer exist in the game cache at all.</body></html>",
		section = miscSection,
		position = 5
	)
	default boolean swapGiants()
	{
		return true;
	}

	@ConfigItem(
		keyName = "swapGhosts",
		name = "Ghosts",
		description = "Swap modern Ghost models and animations to their 2004/2005 retro variants."
			+ " The Restless ghost swaps its model and idle/walk poses only.",
		section = miscSection,
		position = 6
	)
	default boolean swapGhosts()
	{
		return true;
	}

	@ConfigSection(
		name = "Safety",
		description = "Safety settings to disable NPC swapping in dangerous areas or worlds",
		position = 2,
		closedByDefault = true
	)
	String safetySection = "safetySection";

	@ConfigItem(
		keyName = "disablePvpWorld",
		name = "Disable on PvP worlds",
		description = "Disable retro NPC swapping for all NPCs when on a PvP world.",
		section = safetySection,
		position = 1
	)
	default boolean disablePvpWorld()
	{
		return true;
	}

	@ConfigItem(
		keyName = "disableWilderness",
		name = "Disable in Wilderness",
		description = "Disable retro NPC swapping for all NPCs while in the Wilderness.",
		section = safetySection,
		position = 2
	)
	default boolean disableWilderness()
	{
		return true;
	}

	@ConfigSection(
		name = "Compatibility",
		description = "Settings for working alongside other plugins",
		position = 3,
		closedByDefault = true
	)
	String compatibilitySection = "compatibilitySection";

	@ConfigItem(
		keyName = OVERRIDE_INTERACT_HIGHLIGHT,
		name = "Fix Interact Highlight outlines",
		description = "<html><body style='width:170px'>Draw the Interact Highlight plugin's NPC "
			+ "outlines around the retro model instead of the modern one.<br><br>While this is on "
			+ "and models are being swapped, Interact Highlight's own <b>NPCs: Show on hover</b> "
			+ "and <b>Show on interact</b> are turned off and this plugin draws those outlines in "
			+ "their place, using that plugin's own colours and border settings.  Both are turned back "
			+ "on when this plugin stops.</body></html>",
		section = compatibilitySection,
		position = 1
	)
	default boolean overrideInteractHighlight()
	{
		return false;
	}

	@ConfigSection(
		name = "Experimental",
		description = "Unfinished work, off by default",
		position = 4,
		closedByDefault = true
	)
	String experimentalSection = "experimentalSection";

	@ConfigItem(
		keyName = USE_INJECTION_PIPELINE,
		name = "Use the injection pipeline",
		description = "<html><body style='width:170px'>Draw swapped NPCs through geometry this "
			+ "plugin owns rather than handing the client's own model to the renderer.<br><br>"
			+ "This is what restores meshes that no longer exist anywhere in the game cache, so it "
			+ "gates the categories below - and it also gives the giants their real 2005 heads in "
			+ "place of the stand-ins the cache-backed path has to use. Everything else should look "
			+ "the same with this on; if it does not, that is a bug worth reporting.</body></html>",
		section = experimentalSection,
		position = 1
	)
	default boolean useInjectionPipeline()
	{
		return false;
	}

	@ConfigItem(
		keyName = SWAP_DRAGONS,
		name = "Dragons",
		description = "<html><body style='width:170px'>Restore the 2005 dragons - adult and baby - "
			+ "using injected geometry and 2005 animation.<br><br>The adult mesh cannot be swapped "
			+ "by ID at all: the August 2006 graphical update overwrote it. The baby mesh survived, "
			+ "but both had the frames behind their sequences re-authored for the modern "
			+ "skeletons.<br><br>Requires <b>Use the injection pipeline</b> above.</body></html>",
		section = experimentalSection,
		position = 2
	)
	default boolean swapDragons()
	{
		return false;
	}

	@ConfigItem(
		keyName = SWAP_DEMONS,
		name = "Demons",
		description = "<html><body style='width:170px'>Restore the 2005 lesser, greater and black "
			+ "demons using injected geometry and 2005 animation. These cannot be swapped by ID at "
			+ "all - the August 2006 graphical update overwrote their meshes, and the sequences "
			+ "they play survived by ID but had their frames re-authored for the modern "
			+ "skeletons.<br><br>Requires <b>Use the injection pipeline</b> above.</body></html>",
		section = experimentalSection,
		position = 3
	)
	default boolean swapDemons()
	{
		return false;
	}

	@ConfigItem(
		keyName = "swapImps",
		name = "Imps",
		description = "<html><body style='width:170px'>Restore the 2005 imp animation. The imp "
			+ "<b>mesh</b> survived the 2006 update untouched, but the frames behind the sequences "
			+ "it plays were re-authored for the modern rig, so the 2005 animation can only be "
			+ "applied through injected geometry.<br><br>Requires <b>Use the injection pipeline</b> "
			+ "above.</body></html>",
		section = experimentalSection,
		position = 4
	)
	default boolean swapImps()
	{
		return false;
	}

	@ConfigItem(
		keyName = "swapCyclops",
		name = "Cyclopes",
		description = "<html><body style='width:170px'>Restore the 2005 cyclops. It is built on the "
			+ "same body as the giants, but its 2005 head no longer exists in the game cache, so it "
			+ "can only be drawn from injected geometry.<br><br>Requires <b>Use the injection "
			+ "pipeline</b> above.</body></html>",
		section = experimentalSection,
		position = 5
	)
	default boolean swapCyclops()
	{
		return false;
	}
}
