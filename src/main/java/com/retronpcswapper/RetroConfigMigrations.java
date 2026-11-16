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

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/**
 * Carries saved values across from config keys that have been replaced. Add a migration to
 * {@link #run}.
 */
@Slf4j
final class RetroConfigMigrations
{
	private RetroConfigMigrations()
	{
	}

	static void run(ConfigManager configManager)
	{
		migrateLegacyToggle(configManager, RetroNpcConfig.LEGACY_HILL_GIANTS,
			true, RetroNpcConfig.SWAP_GIANTS);
		migrateLegacyToggleToChoice(configManager, RetroNpcConfig.LEGACY_SWAP_GOBLINS,
			RetroNpcConfig.GOBLIN_ERA, GoblinEra.MODERN.name(), GoblinEra.RETRO_2005.name());
		migrateLegacyToggle(configManager, RetroNpcConfig.LEGACY_SWAP_SPIDERS,
			true, RetroNpcConfig.SWAP_SMALL_SPIDERS, RetroNpcConfig.SWAP_GIANT_SPIDERS);
	}

	/**
	 * Hands a retired key's saved value to the toggles that replaced it.
	 *
	 * <p>Renaming a config key silently resets whatever the user had chosen, so the old key is read
	 * once and its value handed on. Only ever writes a key that has no value of its own or still holds
	 * {@code newKeyDefault}, which RuneLite saves before startUp, so a user who has already set the
	 * new toggles is never overwritten - which also makes this safe to run
	 * on every start. The old key is cleared afterward, so the migration happens once and leaves no
	 * orphan behind.
	 */
	static void migrateLegacyToggle(ConfigManager configManager, String legacyKey, boolean newKeyDefault,
		String... newKeys)
	{
		String legacy = configManager.getConfiguration(RetroNpcConfig.GROUP, legacyKey);

		if (legacy == null)
		{
			return;
		}

		for (String key : newKeys)
		{
			if (isUnchosen(configManager, key, Boolean.toString(newKeyDefault)))
			{
				configManager.setConfiguration(RetroNpcConfig.GROUP, key, Boolean.parseBoolean(legacy));
			}
		}

		configManager.unsetConfiguration(RetroNpcConfig.GROUP, legacyKey);
		log.debug("Migrated {}={} to {}", legacyKey, legacy, String.join(", ", newKeys));
	}

	/**
	 * Turns a retired boolean toggle into an enum choice. Same contract as
	 * {@link #migrateLegacyToggle}; values are enum names because that is how RuneLite stores them.
	 * {@code onValue} is the new key's default.
	 */
	static void migrateLegacyToggleToChoice(ConfigManager configManager, String legacyKey, String newKey,
		String offValue, String onValue)
	{
		String legacy = configManager.getConfiguration(RetroNpcConfig.GROUP, legacyKey);

		if (legacy == null)
		{
			return;
		}

		if (isUnchosen(configManager, newKey, onValue))
		{
			configManager.setConfiguration(RetroNpcConfig.GROUP, newKey,
				Boolean.parseBoolean(legacy) ? onValue : offValue);
		}

		configManager.unsetConfiguration(RetroNpcConfig.GROUP, legacyKey);
		log.debug("Migrated {}={} to {}", legacyKey, legacy, newKey);
	}

	/** Unset, or still the default RuneLite saves before startUp. */
	private static boolean isUnchosen(ConfigManager configManager, String key, String savedDefault)
	{
		String current = configManager.getConfiguration(RetroNpcConfig.GROUP, key);
		return current == null || current.equals(savedDefault);
	}
}
