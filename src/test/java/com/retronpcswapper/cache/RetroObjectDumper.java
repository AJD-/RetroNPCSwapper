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
 * Dev-only tool that prints object (scenery) definitions from the <em>2005</em> cache, the
 * counterpart to {@link ModernObjectDumper}.
 *
 * <p>A scenery swap needs the 2005 definition's own values - its models, recolours, lighting and
 * scale - and the live definition at the same id is no guide to them: many 2005 object ids have
 * been reused for something else entirely.
 *
 * <p>Run with {@code ./gradlew dumpRetroObjectDefinitions -Pobj=wardrobe} (ids, or a name
 * substring), optionally {@code -Pmax=N} and {@code -PretroDir=...}. Lives in the test sourceSet
 * and is never shipped.
 */
public class RetroObjectDumper
{
	private static final String DEFAULT_CACHE_DIR = "retrocache/2005cache";
	private static final String MAX_MATCHES_PROPERTY = "retronpcswapper.maxMatches";
	private static final int DEFAULT_MAX_NAME_MATCHES = 40;

	public static void main(String[] args) throws IOException
	{
		String cacheArg = System.getProperty("retronpcswapper.retroDir");
		File cacheDir = new File(cacheArg != null ? cacheArg : DEFAULT_CACHE_DIR);
		if (!cacheDir.isDirectory())
		{
			System.err.println("2005 cache directory not found: " + cacheDir.getAbsolutePath());
			System.err.println("Run from the repo root, or pass one with -PretroDir=<path>");
			System.exit(1);
			return;
		}

		Map<Integer, RetroLocDefinition> defs = RetroLocDecoder.decodeDefinitions(cacheDir);
		System.out.println("Cache: " + cacheDir);
		System.out.println("Object definitions: " + defs.size());
		System.out.println();

		if (args.length == 0)
		{
			System.out.println("Usage: ./gradlew dumpRetroObjectDefinitions -Pobj=wardrobe");
			System.out.println("       ./gradlew dumpRetroObjectDefinitions -Pobj=884,1281 -PretroDir=<path>");
			return;
		}

		int maxMatches = Integer.getInteger(MAX_MATCHES_PROPERTY, DEFAULT_MAX_NAME_MATCHES);
		for (String query : args)
		{
			dump(defs, query.trim(), maxMatches);
		}
	}

	/**
	 * Prints every definition matching a numeric id or a case-insensitive name substring.
	 */
	private static void dump(Map<Integer, RetroLocDefinition> defs, String query, int maxMatches)
	{
		if (query.isEmpty())
		{
			return;
		}

		List<RetroLocDefinition> matches = new ArrayList<>();
		if (query.chars().allMatch(Character::isDigit))
		{
			RetroLocDefinition def = defs.get(Integer.parseInt(query));
			if (def == null)
			{
				System.out.println("No object with id " + query);
				return;
			}
			matches.add(def);
		}
		else
		{
			String needle = query.toLowerCase(Locale.ROOT);
			for (int id : new TreeSet<>(defs.keySet()))
			{
				RetroLocDefinition def = defs.get(id);
				if (def.getName() != null && def.getName().toLowerCase(Locale.ROOT).contains(needle))
				{
					matches.add(def);
				}
			}

			if (matches.isEmpty())
			{
				System.out.println("No object name contains '" + query + "'");
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

	private static void print(RetroLocDefinition def)
	{
		System.out.println(def.getName() + " (id " + def.getId() + ")");
		System.out.println("  models        " + Arrays.toString(def.getModels()));
		if (def.getModelTypes() != null)
		{
			System.out.println("  modelTypes    " + Arrays.toString(def.getModelTypes()));
		}
		System.out.println("  size          " + def.getSizeX() + "x" + def.getSizeY());
		System.out.println("  scale         " + def.getScaleX() + "/" + def.getScaleY() + "/" + def.getScaleZ());
		System.out.println("  offset        " + def.getOffsetX() + "/" + def.getOffsetY() + "/" + def.getOffsetZ());
		System.out.println("  ambient       " + def.getAmbient() + " contrast " + def.getContrast());
		System.out.println("  animation     " + def.getAnimation());
		System.out.println("  contoured     " + def.isContouredGround() + " mirrored " + def.isMirrored());
		if (def.getOriginalColors() != null)
		{
			System.out.println("  recolor       " + Arrays.toString(def.getOriginalColors())
				+ " -> " + Arrays.toString(def.getReplacementColors()));
		}
		if (def.getChildren() != null)
		{
			System.out.println("  children      " + Arrays.toString(def.getChildren())
				+ " (varbit " + def.getVarbit() + ", varp " + def.getVarp() + ")");
		}
		if (def.getDescription() != null)
		{
			System.out.println("  description   " + def.getDescription());
		}
		System.out.println("  actions       " + Arrays.toString(def.getActions()));
		System.out.println();
	}
}
