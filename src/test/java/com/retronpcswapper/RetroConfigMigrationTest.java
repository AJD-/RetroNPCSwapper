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

import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Covers the config-key migrations.
 *
 * <p>Worth testing precisely because the failure is silent: renaming a key without carrying its
 * value across does not error, it just resets whatever the user had chosen back to the default, and
 * a toggle that defaults to on would look like nothing happened at all.
 */
public class RetroConfigMigrationTest
{
	private static final String GROUP = RetroNpcConfig.GROUP;

	@Test
	public void testASavedValueReachesTheKeyThatReplacedIt()
	{
		ConfigManager configManager = mock(ConfigManager.class);
		when(configManager.getConfiguration(GROUP, RetroNpcConfig.LEGACY_HILL_GIANTS))
			.thenReturn("false");
		when(configManager.getConfiguration(GROUP, RetroNpcConfig.SWAP_GIANTS)).thenReturn(null);

		RetroNpcSwapperPlugin.migrateLegacyToggle(configManager,
			RetroNpcConfig.LEGACY_HILL_GIANTS, RetroNpcConfig.SWAP_GIANTS);

		// A user who had turned hill giants off must not find giants back on
		verify(configManager).setConfiguration(GROUP, RetroNpcConfig.SWAP_GIANTS, false);
		verify(configManager).unsetConfiguration(GROUP, RetroNpcConfig.LEGACY_HILL_GIANTS);
	}

	@Test
	public void testOneLegacyKeyCanFeedSeveralNewOnes()
	{
		// Stand-in key names: no live migration splits one key across several any more, but the
		// helper still supports it and the next toggle to be split will need it
		ConfigManager configManager = mock(ConfigManager.class);
		when(configManager.getConfiguration(GROUP, "legacyThing")).thenReturn("true");
		when(configManager.getConfiguration(GROUP, "newA")).thenReturn(null);
		when(configManager.getConfiguration(GROUP, "newB")).thenReturn(null);

		RetroNpcSwapperPlugin.migrateLegacyToggle(configManager, "legacyThing", "newA", "newB");

		verify(configManager).setConfiguration(GROUP, "newA", true);
		verify(configManager).setConfiguration(GROUP, "newB", true);
		verify(configManager).unsetConfiguration(GROUP, "legacyThing");
	}

	@Test
	public void testAChoiceAlreadyMadeOnTheNewKeyIsNotOverwritten()
	{
		ConfigManager configManager = mock(ConfigManager.class);
		when(configManager.getConfiguration(GROUP, RetroNpcConfig.LEGACY_HILL_GIANTS))
			.thenReturn("false");
		// The user has since set the new toggle themselves
		when(configManager.getConfiguration(GROUP, RetroNpcConfig.SWAP_GIANTS)).thenReturn("true");

		RetroNpcSwapperPlugin.migrateLegacyToggle(configManager,
			RetroNpcConfig.LEGACY_HILL_GIANTS, RetroNpcConfig.SWAP_GIANTS);

		verify(configManager, never()).setConfiguration(anyString(), anyString(), any());

		// The orphan still goes, so the migration does not keep reconsidering it
		verify(configManager).unsetConfiguration(GROUP, RetroNpcConfig.LEGACY_HILL_GIANTS);
	}

	@Test
	public void testNothingHappensWithoutASavedLegacyValue()
	{
		ConfigManager configManager = mock(ConfigManager.class);
		when(configManager.getConfiguration(GROUP, RetroNpcConfig.LEGACY_HILL_GIANTS))
			.thenReturn(null);

		RetroNpcSwapperPlugin.migrateLegacyToggle(configManager,
			RetroNpcConfig.LEGACY_HILL_GIANTS, RetroNpcConfig.SWAP_GIANTS);

		// This runs on every start, so the ordinary case has to be inert - it must not write a
		// default over a key the user has never touched, nor unset anything
		verify(configManager).getConfiguration(GROUP, RetroNpcConfig.LEGACY_HILL_GIANTS);
		verifyNoMoreInteractions(configManager);
	}

	@Test
	public void testRunningTwiceIsHarmless()
	{
		ConfigManager configManager = mock(ConfigManager.class);
		when(configManager.getConfiguration(GROUP, RetroNpcConfig.LEGACY_HILL_GIANTS))
			.thenReturn("false", (String) null);
		when(configManager.getConfiguration(GROUP, RetroNpcConfig.SWAP_GIANTS))
			.thenReturn(null, "false");

		RetroNpcSwapperPlugin.migrateLegacyToggle(configManager,
			RetroNpcConfig.LEGACY_HILL_GIANTS, RetroNpcConfig.SWAP_GIANTS);
		RetroNpcSwapperPlugin.migrateLegacyToggle(configManager,
			RetroNpcConfig.LEGACY_HILL_GIANTS, RetroNpcConfig.SWAP_GIANTS);

		// The second start finds the legacy key gone and does nothing, so the value written by the
		// first is never disturbed
		verify(configManager).setConfiguration(GROUP, RetroNpcConfig.SWAP_GIANTS, false);
		verify(configManager).unsetConfiguration(eq(GROUP), eq(RetroNpcConfig.LEGACY_HILL_GIANTS));
	}
}
