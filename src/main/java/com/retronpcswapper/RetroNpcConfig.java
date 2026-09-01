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

@ConfigGroup(RetroNpcSwapperPlugin.CONFIG_GROUP)
public interface RetroNpcConfig extends Config
{
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
		keyName = "swapHillGiants",
		name = "Hill Giants",
		description = "Swap modern Hill Giant models and animations to their 2004/2005 retro variants.",
		section = miscSection,
		position = 5
	)
	default boolean swapHillGiants()
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
}
