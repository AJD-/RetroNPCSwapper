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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** When the update note is announced, and when the saved version moves. */
public class RetroChangelogTest
{
	private static final List<RetroChangelog.Release> HISTORY = Arrays.asList(
		new RetroChangelog.Release("1.0.9", "Ninth patch"),
		new RetroChangelog.Release("1.0.10", "Tenth patch"),
		new RetroChangelog.Release("2.0.0", "Second major"));

	private static final String LATEST_MESSAGE = "Retro NPC Swapper v2.0.0: Second major";

	/** A new install has not been updated, so it is told nothing. */
	@Test
	public void testNoSavedVersionIsSilentAndSavesTheLatest()
	{
		Memory memory = new Memory(null);
		memory.check(pending(), true);

		assertEquals(Arrays.asList("read", "save:2.0.0"), memory.events);
	}

	/** A corrupt marker must not throw on the client thread every login. */
	@Test
	public void testUnparseableSavedVersionIsTreatedAsFirstRun()
	{
		for (String saved : new String[]{"", "2.0", "v2.0.0", "2.0.0-beta", "99999999999.0.0"})
		{
			Memory memory = new Memory(saved);
			memory.check(pending(), true);

			assertEquals(saved, Arrays.asList("read", "save:2.0.0"), memory.events);
		}
	}

	/** However many releases were missed, chat gets one line. */
	@Test
	public void testOlderVersionAnnouncesOnlyTheLatestReleaseThenSaves()
	{
		for (String saved : new String[]{"0.0.1", "1.0.9", "1.0.10"})
		{
			Memory memory = new Memory(saved);
			memory.check(pending(), true);

			assertEquals(saved, Arrays.asList("read", "send:" + LATEST_MESSAGE, "save:2.0.0"), memory.events);
		}
	}

	/** Running an older build must not rewind the marker and replay notes later. */
	@Test
	public void testCurrentOrNewerVersionDoesNothing()
	{
		for (String saved : new String[]{"2.0.0", "2.0.1", "3.0.0"})
		{
			Memory memory = new Memory(saved);
			memory.check(pending(), true);

			assertEquals(saved, Collections.singletonList("read"), memory.events);
		}
	}

	/** Turning the toggle back on must not replay notes missed while it was off. */
	@Test
	public void testAnnounceOffStillSavesTheVersion()
	{
		Memory memory = new Memory("1.0.9");
		memory.check(pending(), false);

		assertEquals(Arrays.asList("read", "save:2.0.0"), memory.events);
	}

	/** Saving first would lose the note for good. */
	@Test
	public void testAFailedSendLeavesTheVersionAndIsRetried()
	{
		RetroChangelog changelog = pending();
		Memory memory = new Memory("1.0.9");

		assertThrows(IllegalStateException.class, () -> changelog.check(true, () -> memory.version,
			message ->
			{
				throw new IllegalStateException("chat unavailable");
			}, memory::save));
		assertEquals("1.0.9", memory.version);
		assertTrue(changelog.isPending());

		memory.check(changelog, true);
		assertEquals(Arrays.asList("read", "send:" + LATEST_MESSAGE, "save:2.0.0"), memory.events);
	}

	/** The plugin ticks every 0.6s; only a request may cause a config read. */
	@Test
	public void testNothingHappensUntilACheckIsRequested()
	{
		RetroChangelog changelog = new RetroChangelog(HISTORY);
		Memory memory = new Memory("1.0.9");

		memory.check(changelog, true);
		assertTrue(memory.events.isEmpty());

		changelog.requestCheck();
		memory.check(changelog, true);
		assertFalse(changelog.isPending());
		int events = memory.events.size();

		memory.check(changelog, true);
		assertEquals(events, memory.events.size());
	}

	/** String order would put 1.0.10 before 1.0.9. */
	@Test
	public void testVersionsCompareNumerically()
	{
		assertTrue(RetroChangelog.compareVersions("1.0.10", "1.0.9") > 0);
		assertTrue(RetroChangelog.compareVersions("2.0.0", "1.99.99") > 0);
		assertEquals(0, RetroChangelog.compareVersions("2.8.0", "2.8.0"));
	}

	/** A mistake in the list should fail the build, not misfire in game. */
	@Test
	public void testMalformedHistoryIsRejected()
	{
		assertThrows(IllegalArgumentException.class, () -> new RetroChangelog(Collections.emptyList()));
		assertThrows(IllegalArgumentException.class, () -> new RetroChangelog(Arrays.asList(
			new RetroChangelog.Release("1.0.1", "b"), new RetroChangelog.Release("1.0.1", "c"))));
		assertThrows(IllegalArgumentException.class, () -> new RetroChangelog(Arrays.asList(
			new RetroChangelog.Release("1.1.0", "b"), new RetroChangelog.Release("1.0.1", "c"))));
		assertThrows(IllegalArgumentException.class, () -> new RetroChangelog.Release("v1.0.1", "a"));
		assertThrows(IllegalArgumentException.class, () -> new RetroChangelog.Release("1.0.1", " "));
		assertThrows(IllegalArgumentException.class, () -> new RetroChangelog.Release("1.0.1", "a\nb"));
	}

	/**
	 * A note for a version that has not shipped would be saved as seen and then never shown.
	 * Not equality: a release without a note bumps the version alone.
	 */
	@Test
	public void testLatestNoteIsNotAheadOfThePluginVersion() throws Exception
	{
		Properties properties = new Properties();
		try (InputStream in = Files.newInputStream(Paths.get("runelite-plugin.properties")))
		{
			properties.load(in);
		}

		String latest = new RetroChangelog().latestVersion();
		assertTrue(latest + " is ahead of runelite-plugin.properties",
			RetroChangelog.compareVersions(latest, properties.getProperty("version")) <= 0);
	}

	/** JUnit 4.12 has no assertThrows. */
	private static void assertThrows(Class<? extends Throwable> expected, Runnable code)
	{
		try
		{
			code.run();
		}
		catch (Throwable thrown)
		{
			assertTrue("Threw " + thrown, expected.isInstance(thrown));
			return;
		}
		fail("Expected " + expected.getSimpleName());
	}

	private static RetroChangelog pending()
	{
		RetroChangelog changelog = new RetroChangelog(HISTORY);
		changelog.requestCheck();
		return changelog;
	}

	/** Stands in for the config store and the chat box, recording the order they are touched in. */
	private static final class Memory
	{
		private final List<String> events = new ArrayList<>();
		private String version;

		private Memory(String version)
		{
			this.version = version;
		}

		private void save(String saved)
		{
			events.add("save:" + saved);
			version = saved;
		}

		private void check(RetroChangelog changelog, boolean announce)
		{
			changelog.check(announce,
				() ->
				{
					events.add("read");
					return version;
				},
				message -> events.add("send:" + message),
				this::save);
		}
	}
}
