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
package com.retronpcswapper.cache;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.runelite.cache.NpcManager;
import net.runelite.cache.definitions.NpcDefinition;
import net.runelite.cache.fs.Store;

/**
 * Dev-only tool that prints NPC definitions from the <em>live</em> OSRS cache, the counterpart to
 * {@link NpcMappingGenerator} which reads the 2005 cache.
 *
 * <p>Exists because sizing a substituted model needs to be compared against what the modern client
 * actually does. The live Chicken composition, for instance, is {@code widthScale=80} - it shrinks
 * its own model - which is why the retro chicken could not be sized from it and needed a
 * hand-matched value instead. Answering that sort of question previously meant launching the
 * client and reading values through the plugin.
 *
 * <p>Decoding is delegated to {@code net.runelite:cache}, a first-party library pinned to the same
 * version as the client. Hand-rolling the container and opcode formats was tried and abandoned:
 * it decoded barely 15% of definitions cleanly, and a tool that is quietly wrong about scale
 * values is worse than no tool.
 *
 * <p>Run with {@code ./gradlew dumpNpcDefinitions -Pnpc=1173} (ids, or a name substring), and
 * optionally {@code -PcacheDir=...} to point at a cache other than the RuneLite one. This class
 * lives in the test sourceSet and is never shipped, so console output and reading files outside
 * {@code .runelite} are fine here.
 */
public class ModernNpcDumper
{
	private static final String CACHE_DIR_PROPERTY = "retronpcswapper.cacheDir";

	private static final String[] DEFAULT_CACHE_DIRS = {
		".runelite/jagexcache/oldschool/LIVE",
		"jagexcache/oldschool/LIVE"
	};

	/** Guard against a broad name search printing thousands of definitions. */
	private static final int MAX_NAME_MATCHES = 40;

	public static void main(String[] args) throws IOException
	{
		File cacheDir = resolveCacheDir();
		if (cacheDir == null)
		{
			System.err.println("Could not find an OSRS cache. Looked for:");
			for (String candidate : DEFAULT_CACHE_DIRS)
			{
				System.err.println("  " + new File(System.getProperty("user.home"), candidate));
			}
			System.err.println("Pass one with -PcacheDir=<path>");
			System.exit(1);
			return;
		}

		try (Store store = new Store(cacheDir))
		{
			store.load();

			NpcManager npcManager = new NpcManager(store);
			npcManager.load();

			System.out.println("Cache: " + cacheDir);
			System.out.println("NPC definitions: " + npcManager.getNpcs().size());
			System.out.println();

			if (args.length == 0)
			{
				System.out.println("Usage: ./gradlew dumpNpcDefinitions -Pnpc=1173,1174");
				System.out.println("       ./gradlew dumpNpcDefinitions -Pnpc=chicken");
				return;
			}

			for (String arg : args)
			{
				dump(npcManager, arg.trim());
			}
		}
	}

	/**
	 * Resolves the cache directory from the system property, else the first default that exists.
	 * Returns null when nothing is found.
	 */
	private static File resolveCacheDir()
	{
		String configured = System.getProperty(CACHE_DIR_PROPERTY);
		if (configured != null && !configured.isEmpty())
		{
			File dir = new File(configured);
			return dir.isDirectory() ? dir : null;
		}

		File home = new File(System.getProperty("user.home"));
		for (String candidate : DEFAULT_CACHE_DIRS)
		{
			File dir = new File(home, candidate);
			if (dir.isDirectory())
			{
				return dir;
			}
		}
		return null;
	}

	/**
	 * Prints every definition matching a numeric id or a case-insensitive name substring.
	 */
	private static void dump(NpcManager npcManager, String query)
	{
		if (query.isEmpty())
		{
			return;
		}

		List<NpcDefinition> matches = new ArrayList<>();
		if (query.chars().allMatch(Character::isDigit))
		{
			NpcDefinition def = npcManager.get(Integer.parseInt(query));
			if (def == null)
			{
				System.out.println("No NPC with id " + query);
				return;
			}
			matches.add(def);
		}
		else
		{
			String needle = query.toLowerCase(Locale.ROOT);
			for (NpcDefinition def : npcManager.getNpcs())
			{
				if (def.name != null && def.name.toLowerCase(Locale.ROOT).contains(needle))
				{
					matches.add(def);
				}
			}

			if (matches.isEmpty())
			{
				System.out.println("No NPC name contains '" + query + "'");
				return;
			}
		}

		int shown = Math.min(matches.size(), MAX_NAME_MATCHES);
		for (int i = 0; i < shown; i++)
		{
			print(matches.get(i));
		}

		if (matches.size() > shown)
		{
			System.out.println("... and " + (matches.size() - shown) + " more matches for '" + query + "'");
			System.out.println();
		}
	}

	private static void print(NpcDefinition def)
	{
		System.out.println(def.name + " (id " + def.id + ")");
		System.out.println("  models        " + Arrays.toString(def.models));
		// The two values that matter when sizing substituted geometry - both 1/128ths
		System.out.println("  widthScale    " + def.widthScale);
		System.out.println("  heightScale   " + def.heightScale);
		System.out.println("  size          " + def.size);
		System.out.println("  combatLevel   " + def.combatLevel);
		System.out.println("  standingAnim  " + def.standingAnimation);
		System.out.println("  walkingAnim   " + def.walkingAnimation);
		if (def.recolorToFind != null)
		{
			System.out.println("  recolor       " + Arrays.toString(def.recolorToFind)
				+ " -> " + Arrays.toString(def.recolorToReplace));
		}
		if (def.configs != null)
		{
			System.out.println("  transforms    " + Arrays.toString(def.configs));
		}
		System.out.println();
	}
}
