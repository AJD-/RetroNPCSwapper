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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("retronpcswapper")
public interface RetroNpcConfig extends Config
{
	@ConfigSection(
		name = "Misc NPCs",
		description = "Retro models and animations for miscellaneous NPCs",
		position = 0,
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

	@ConfigItem(
		keyName = "swapGhosts",
		name = "Ghosts (Disabled)",
		description = "Disabled: In modern OSRS, 2005 ghost models were re-indexed/reworked and cannot be cleanly mapped without custom client model injection.",
		warning = "Ghost swapping is disabled: Modern OSRS cache does not preserve the 2005 ghost models.",
		section = miscSection,
		position = 6
	)
	default boolean swapGhosts()
	{
		return false;
	}

	@ConfigItem(
		keyName = "swapGuards",
		name = "Guards (Disabled)",
		description = "Disabled: In modern OSRS, Guard models are multi-part modular meshes and cannot be cleanly mapped without custom client model injection.",
		warning = "Guard swapping is disabled: Modern OSRS guard models are multi-part modular meshes.",
		section = miscSection,
		position = 7
	)
	default boolean swapGuards()
	{
		return false;
	}

	@ConfigItem(
		keyName = "swapImps",
		name = "Imps (Disabled)",
		description = "Disabled: In modern OSRS, Imp animation sequence IDs (168-172) are identical to the 2005 cache, but their internal skeletal frames were directly modified in-place by Jagex with no preserved legacy sequence IDs to swap to.",
		warning = "Imp swapping is disabled: Modern OSRS Imp animations cannot be reverted to 2005 variants because Jagex modified the sequence frames in-place with no legacy sequences preserved.",
		section = miscSection,
		position = 8
	)
	default boolean swapImps()
	{
		return false;
	}

	@ConfigItem(
		keyName = "swapMossGiants",
		name = "Moss Giants (Disabled)",
		description = "Disabled: In modern OSRS, 2005 moss giant models were re-indexed/reworked with multi-part meshes and cannot be swapped without custom client model injection.",
		warning = "Moss Giant swapping is disabled: Modern OSRS cache does not preserve the 2005 single-mesh moss giant models.",
		section = miscSection,
		position = 9
	)
	default boolean swapMossGiants()
	{
		return false;
	}

	@ConfigItem(
		keyName = "swapFireGiants",
		name = "Fire Giants (Disabled)",
		description = "Disabled: In modern OSRS, 2005 fire giant models were re-indexed/reworked with multi-part meshes and cannot be swapped without custom client model injection.",
		warning = "Fire Giant swapping is disabled: Modern OSRS cache does not preserve the 2005 single-mesh fire giant models.",
		section = miscSection,
		position = 10
	)
	default boolean swapFireGiants()
	{
		return false;
	}

	@ConfigSection(
		name = "Dragons (Disabled)",
		description = "Retro models and animations for Dragon NPCs",
		position = 1,
		closedByDefault = true
	)
	String dragonSection = "dragonSection";

	@ConfigItem(
		keyName = "swapAdultDragons",
		name = "Adult Dragons (Disabled)",
		description = "Disabled: In modern OSRS, 2005 dragon models (2853, 2854) were re-indexed to scenery objects and cannot be swapped without custom client model injection.",
		warning = "Adult Dragon swapping is disabled: Modern OSRS cache re-indexed models 2853/2854 to scenery objects and the 2005 dragon meshes are not preserved in the modern cache.",
		section = dragonSection,
		position = 1
	)
	default boolean swapAdultDragons()
	{
		return false;
	}

	@ConfigItem(
		keyName = "swapBabyDragons",
		name = "Baby Dragons (Disabled)",
		description = "Disabled: In modern OSRS, 2005 baby dragon models were re-indexed/reworked with multi-part meshes and cannot be swapped without custom client model injection.",
		warning = "Baby Dragon swapping is disabled: Modern OSRS cache does not preserve the 2005 baby dragon models.",
		section = dragonSection,
		position = 2
	)
	default boolean swapBabyDragons()
	{
		return false;
	}

	@ConfigSection(
		name = "Demons (Disabled)",
		description = "Retro models and animations for Demon NPCs",
		position = 2,
		closedByDefault = true
	)
	String demonSection = "demonSection";

	@ConfigItem(
		keyName = "swapLesserDemons",
		name = "Lesser Demons (Disabled)",
		description = "Disabled: In modern OSRS, 2005 demon models (2943) were re-indexed to scenery objects and cannot be swapped without custom client model injection.",
		warning = "Lesser Demon swapping is disabled: Modern OSRS cache re-indexed model 2943 to scenery objects and the 2005 demon mesh is not preserved in the modern cache.",
		section = demonSection,
		position = 1
	)
	default boolean swapLesserDemons()
	{
		return false;
	}

	@ConfigItem(
		keyName = "swapGreaterDemons",
		name = "Greater Demons (Disabled)",
		description = "Disabled: In modern OSRS, 2005 demon models (2942) were re-indexed to scenery objects and cannot be swapped without custom client model injection.",
		warning = "Greater Demon swapping is disabled: Modern OSRS cache re-indexed model 2942 to scenery objects and the 2005 demon mesh is not preserved in the modern cache.",
		section = demonSection,
		position = 2
	)
	default boolean swapGreaterDemons()
	{
		return false;
	}

	@ConfigItem(
		keyName = "swapBlackDemons",
		name = "Black Demons (Disabled)",
		description = "Disabled: In modern OSRS, 2005 demon models (2942) were re-indexed to scenery objects and cannot be swapped without custom client model injection.",
		warning = "Black Demon swapping is disabled: Modern OSRS cache re-indexed model 2942 to scenery objects and the 2005 demon mesh is not preserved in the modern cache.",
		section = demonSection,
		position = 3
	)
	default boolean swapBlackDemons()
	{
		return false;
	}

	@ConfigSection(
		name = "Safety",
		description = "Safety settings to disable NPC swapping in dangerous areas or worlds",
		position = 3,
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
