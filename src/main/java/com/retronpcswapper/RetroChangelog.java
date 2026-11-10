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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Release notes, announced once per profile after an update. */
final class RetroChangelog
{
	/** Raw config key, deliberately not a {@code @ConfigItem}: it must read as null until first saved. */
	static final String LAST_SEEN_VERSION_KEY = "lastSeenVersion";

	/** Newest last. One short line each; only the newest is ever announced. */
	private static final List<Release> RELEASES = List.of(
			new Release("2.8.0",
					"Added retro fishing spot animations, Custom Servants compatibility, and a variety of bugfixes"),
			new Release("2.8.1", "Added 2DScape compatibility, Region Locker GPU Plugin compatibility")
	);

	private final Release latest;
	private boolean pending;

	RetroChangelog()
	{
		this(RELEASES);
	}

	RetroChangelog(List<Release> releases)
	{
		List<Release> copy = new ArrayList<>(releases);
		if (copy.isEmpty())
		{
			throw new IllegalArgumentException("Changelog needs at least one release");
		}
		for (int i = 1; i < copy.size(); i++)
		{
			if (compareVersions(copy.get(i).version, copy.get(i - 1).version) <= 0)
			{
				throw new IllegalArgumentException("Changelog releases must increase in version order");
			}
		}
		latest = copy.get(copy.size() - 1);
	}

	String latestVersion()
	{
		return latest.version;
	}

	void requestCheck()
	{
		pending = true;
	}

	boolean isPending()
	{
		return pending;
	}

	/**
	 * @param announce false keeps the saved version moving without sending anything
	 */
	void check(boolean announce, Supplier<String> readVersion, Consumer<String> sendMessage,
		Consumer<String> saveVersion)
	{
		if (!pending)
		{
			return;
		}

		String seen = readVersion.get();
		if (!isVersion(seen))
		{
			// First run: nothing was updated as far as this profile is concerned
			saveVersion.accept(latest.version);
		}
		else if (compareVersions(seen, latest.version) < 0)
		{
			if (announce)
			{
				sendMessage.accept("Retro NPC Swapper v" + latest.version + ": " + latest.notes);
			}
			// After the send, so a failed send is retried
			saveVersion.accept(latest.version);
		}
		pending = false;
	}

	static int compareVersions(String left, String right)
	{
		int[] leftParts = parseVersion(left);
		int[] rightParts = parseVersion(right);
		for (int i = 0; i < leftParts.length; i++)
		{
			int comparison = Integer.compare(leftParts[i], rightParts[i]);
			if (comparison != 0)
			{
				return comparison;
			}
		}
		return 0;
	}

	private static boolean isVersion(String version)
	{
		return version != null && version.matches("[0-9]{1,9}\\.[0-9]{1,9}\\.[0-9]{1,9}");
	}

	private static int[] parseVersion(String version)
	{
		if (!isVersion(version))
		{
			throw new IllegalArgumentException("Expected a major.minor.patch version: " + version);
		}
		String[] parts = version.split("\\.");
		int[] numbers = new int[parts.length];
		for (int i = 0; i < parts.length; i++)
		{
			numbers[i] = Integer.parseInt(parts[i]);
		}
		return numbers;
	}

	static final class Release
	{
		final String version;
		final String notes;

		Release(String version, String notes)
		{
			parseVersion(version);
			if (notes == null || notes.trim().isEmpty() || notes.indexOf('\n') >= 0 || notes.indexOf('\r') >= 0)
			{
				throw new IllegalArgumentException("Release notes must be one non-empty line");
			}
			this.version = version;
			this.notes = notes.trim();
		}
	}
}
