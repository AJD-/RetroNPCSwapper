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
package com.retronpcswapper.compatibility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.google.inject.Guice;
import com.retronpcswapper.RetroNpcConfig;
import java.util.HashMap;
import java.util.Map;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.PluginManager;
import org.junit.Before;
import org.junit.Test;

public class InteractHighlightCompatTest
{
	private static final String GROUP = "interacthighlight";
	private static final String[] KEYS = {"npcShowHover", "npcShowInteract", "objectShowHover", "objectShowInteract"};

	/** Saved config, as group.key to value. */
	private final Map<String, String> saved = new HashMap<>();

	private InteractHighlightCompat compat;

	@Before
	public void setUp()
	{
		ConfigManager configManager = mock(ConfigManager.class);
		when(configManager.getConfiguration(anyString(), anyString()))
			.thenAnswer(call -> saved.get(call.getArgument(0) + "." + call.getArgument(1)));
		doAnswer(call -> saved.put(call.getArgument(0) + "." + call.getArgument(1), call.getArgument(2)))
			.when(configManager).setConfiguration(anyString(), anyString(), anyString());
		doAnswer(call -> saved.remove(call.getArgument(0) + "." + call.getArgument(1)))
			.when(configManager).unsetConfiguration(anyString(), anyString());

		PluginManager pluginManager = mock(PluginManager.class);
		compat = Guice.createInjector(binder ->
		{
			binder.bind(ConfigManager.class).toInstance(configManager);
			binder.bind(PluginManager.class).toInstance(pluginManager);
		}).getInstance(InteractHighlightCompat.class);
	}

	private String live(String key)
	{
		return saved.get(GROUP + "." + key);
	}

	private boolean stashed()
	{
		return saved.keySet().stream().anyMatch(key -> key.startsWith(RetroNpcConfig.GROUP + "."));
	}

	@Test
	public void testSuppressTurnsOffNpcAndObjectOutlines()
	{
		saved.put(GROUP + ".objectShowHover", "true");

		compat.suppress();

		for (String key : KEYS)
		{
			assertEquals(key, "false", live(key));
		}
	}

	@Test
	public void testRestorePutsBackWhatTheUserHad()
	{
		saved.put(GROUP + ".npcShowHover", "true");
		saved.put(GROUP + ".objectShowInteract", "false");

		compat.suppress();
		compat.restore();

		assertEquals("true", live("npcShowHover"));
		assertEquals("false", live("objectShowInteract"));
		// Never saved before, so left unsaved and the plugin's default stands
		assertNull(live("npcShowInteract"));
		assertNull(live("objectShowHover"));
		assertFalse("the stash is cleared once restored", stashed());
	}

	/** A session that stopped while suppressing is repaired on the next start, objects included. */
	@Test
	public void testAStaleStashIsRestored()
	{
		saved.put(GROUP + ".objectShowHover", "true");
		compat.suppress();
		compat.forget();

		compat.restoreStaleStash();

		assertEquals("true", live("objectShowHover"));
		assertFalse(stashed());
	}

	/**
	 * A stash left by a version that only suppressed the NPC keys is kept as it is, and the object
	 * keys are stashed alongside it rather than the NPC values being stashed again over it.
	 */
	@Test
	public void testAnOlderNpcOnlyStashIsKept()
	{
		saved.put(RetroNpcConfig.GROUP + ".interactHighlightStash_npcShowHover", "true");
		saved.put(GROUP + ".npcShowHover", "false");
		saved.put(GROUP + ".objectShowHover", "true");

		compat.suppress();
		compat.restore();

		assertEquals("true", live("npcShowHover"));
		assertEquals("true", live("objectShowHover"));
	}

	@Test
	public void testTurningAnObjectOutlineBackOnIsTheUsersOverride()
	{
		compat.suppress();

		assertTrue(compat.isUserOverride(new ConfigChanged()
		{
			{
				setGroup(GROUP);
				setKey("objectShowHover");
				setNewValue("true");
			}
		}));
	}

	/** The key the user just set keeps their value; the others go back to what they had. */
	@Test
	public void testOptingOutKeepsTheUsersChoice()
	{
		saved.put(GROUP + ".npcShowHover", "true");
		compat.suppress();
		saved.put(GROUP + ".objectShowHover", "true");

		compat.optOut("objectShowHover");

		assertEquals("true", live("objectShowHover"));
		assertEquals("true", live("npcShowHover"));
		assertNull(live("objectShowInteract"));
		assertFalse(stashed());
	}
}
