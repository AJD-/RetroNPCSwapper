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
import java.util.Map;
import java.util.TreeSet;

/**
 * Dev-only tool that prints NPC definitions from the <em>2005</em> cache, the counterpart to
 * {@link ModernNpcDumper} which reads the live one.
 *
 * <p>Exists because {@code npc-mappings.json} cannot answer a question about one particular NPC.
 * {@link NpcMappingGenerator#buildEntries} keys rows by name and keeps the lowest def id per name,
 * so every 2005 NPC called "Guard" collapses into a single row - which is fine for a category that
 * is one costume, and useless the moment two towns dress their guards differently.
 *
 * <p>Decoding is the repo's own 2005 stack ({@link RetroCacheReader}, {@link RetroNpcDecoder}),
 * reached through {@link NpcMappingGenerator#decodeDefinitions} so there is one path into the
 * cache rather than two.
 *
 * <p>Run with {@code ./gradlew dumpRetroNpcDefinitions -Pnpc=guard} (ids, or a name substring),
 * and {@code -PretroDir=...} to point at a cache other than {@code retrocache/2005cache}. This
 * class lives in the test sourceSet and is never shipped, so console output and reading files
 * outside {@code .runelite} are fine here.
 */
public class RetroNpcDumper
{
	private static final String DEFAULT_CACHE_DIR = "retrocache/2005cache";
	private static final String MAX_MATCHES_PROPERTY = "retronpcswapper.maxMatches";

	/**
	 * Guard against a broad name search printing thousands of definitions. Raise it with
	 * {@code -Pmax=N} when the whole family is the point, as it is when working out which of
	 * several same-named NPCs a costume belongs to.
	 */
	private static final int DEFAULT_MAX_NAME_MATCHES = 40;

	public static void main(String[] args) throws IOException
	{
		// The retro dir arrives as the first argument, the way generateNpcMappings passes it
		List<String> queries = new ArrayList<>(Arrays.asList(args));
		String cacheArg = System.getProperty("retronpcswapper.retroDir");
		if (cacheArg == null && !queries.isEmpty() && new File(queries.get(0)).isDirectory())
		{
			cacheArg = queries.remove(0);
		}

		File cacheDir = new File(cacheArg != null ? cacheArg : DEFAULT_CACHE_DIR);
		if (!cacheDir.isDirectory())
		{
			System.err.println("2005 cache directory not found: " + cacheDir.getAbsolutePath());
			System.err.println("Run from the repo root, or pass one with -PretroDir=<path>");
			System.exit(1);
			return;
		}

		Map<Integer, RetroNpcDefinition> defs = NpcMappingGenerator.decodeDefinitions(cacheDir);
		int maxMatches = maxMatches();

		System.out.println("Cache: " + cacheDir);
		System.out.println("NPC definitions: " + defs.size());
		System.out.println();

		if (queries.isEmpty())
		{
			System.out.println("Usage: ./gradlew dumpRetroNpcDefinitions -Pnpc=guard");
			System.out.println("       ./gradlew dumpRetroNpcDefinitions -Pnpc=277,278 -PretroDir=<path>");
			return;
		}

		for (String query : queries)
		{
			dump(defs, query.trim(), maxMatches);
		}
	}

	/**
	 * The print ceiling, from {@code -Pmax}, else {@link #DEFAULT_MAX_NAME_MATCHES}.
	 */
	private static int maxMatches()
	{
		String configured = System.getProperty(MAX_MATCHES_PROPERTY);
		if (configured == null || configured.isEmpty())
		{
			return DEFAULT_MAX_NAME_MATCHES;
		}

		try
		{
			return Math.max(1, Integer.parseInt(configured.trim()));
		}
		catch (NumberFormatException ex)
		{
			System.err.println("Ignoring non-numeric -Pmax=" + configured);
			return DEFAULT_MAX_NAME_MATCHES;
		}
	}

	/**
	 * Prints every definition matching a numeric id or a case-insensitive name substring.
	 */
	private static void dump(Map<Integer, RetroNpcDefinition> defs, String query, int maxMatches)
	{
		if (query.isEmpty())
		{
			return;
		}

		List<RetroNpcDefinition> matches = new ArrayList<>();
		if (query.chars().allMatch(Character::isDigit))
		{
			RetroNpcDefinition def = defs.get(Integer.parseInt(query));
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
			// Ascending id order, the same order buildEntries visits them in, so "the row the
			// generator kept" is always the first one printed
			for (int id : new TreeSet<>(defs.keySet()))
			{
				RetroNpcDefinition def = defs.get(id);
				if (def != null && def.getName() != null
					&& def.getName().toLowerCase(Locale.ROOT).contains(needle))
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

		int shown = Math.min(matches.size(), maxMatches);
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

	private static void print(RetroNpcDefinition def)
	{
		System.out.println(def.getName() + " (id " + def.getId() + ")");
		System.out.println("  models        " + Arrays.toString(def.getModels()));
		if (def.getAdditionalModels() != null)
		{
			System.out.println("  headModels    " + Arrays.toString(def.getAdditionalModels()));
		}
		// Both 1/128ths, as the 2005 client applied them (opcodes 97 and 98)
		System.out.println("  scaleXZ       " + def.getScaleXZ());
		System.out.println("  scaleY        " + def.getScaleY());
		System.out.println("  size          " + def.getSize());
		System.out.println("  combatLevel   " + def.getCombatLevel());
		System.out.println("  standingAnim  " + def.getStanceAnimation());
		System.out.println("  walkingAnim   " + def.getWalkAnimation());
		// The opcode 40 pairs. For generic human kit these are the whole identity of a costume -
		// the meshes are shared with every other NPC wearing it
		if (def.getOriginalColors() != null)
		{
			System.out.println("  recolor       " + Arrays.toString(def.getOriginalColors())
				+ " -> " + Arrays.toString(def.getReplacementColors()));
		}
		if (def.getDescription() != null)
		{
			System.out.println("  description   " + def.getDescription());
		}
		System.out.println("  actions       " + Arrays.toString(def.getActions()));
		System.out.println();
	}
}
