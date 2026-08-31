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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import com.retronpcswapper.RetroNpcCategory;
import com.retronpcswapper.RetroNpcMappingEntry;

/**
 * Dev-only tool that regenerates the bundled npc-mappings.json resource from a
 * local copy of the 2005 cache. Run with {@code ./gradlew generateNpcMappings}
 * from the repo root (the cache folder itself is not committed).
 *
 * This class lives in the test sourceSet and is never shipped with the plugin,
 * so a plain GsonBuilder and direct file writes outside .runelite are fine here;
 * the @Inject Gson / .runelite-only rules apply to src/main runtime code.
 */
public class NpcMappingGenerator
{
	private static final Path OUTPUT_PATH =
		Paths.get("src/main/resources/com/retronpcswapper/npc-mappings.json");

	public static void main(String[] args) throws IOException
	{
		File cacheDir = new File(args.length > 0 ? args[0] : "retrocache/2005cache");
		if (!cacheDir.isDirectory())
		{
			System.err.println("2005 cache directory not found: " + cacheDir.getAbsolutePath());
			System.err.println("Run from the repo root, or pass the cache directory as the first argument.");
			System.exit(1);
		}

		Map<Integer, RetroNpcDefinition> defs = decodeDefinitions(cacheDir);
		List<RetroNpcMappingEntry> entries = buildEntries(defs);

		Files.createDirectories(OUTPUT_PATH.getParent());
		Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		try (Writer out = Files.newBufferedWriter(OUTPUT_PATH, StandardCharsets.UTF_8))
		{
			gson.toJson(entries, out);
			out.write('\n');
		}

		Map<RetroNpcCategory, Integer> counts = new TreeMap<>();
		for (RetroNpcMappingEntry entry : entries)
		{
			counts.merge(entry.getCategory(), 1, Integer::sum);
		}
		System.out.println("Wrote " + entries.size() + " entries from " + defs.size() + " defs to " + OUTPUT_PATH);
		counts.forEach((category, count) -> System.out.println("  " + category + ": " + count));
	}

	/**
	 * Decodes all NPC definitions from a local 2005 cache directory.
	 */
	static Map<Integer, RetroNpcDefinition> decodeDefinitions(File cacheDir) throws IOException
	{
		RetroCacheReader reader = new RetroCacheReader(cacheDir);
		if (!reader.init())
		{
			throw new IOException("Failed to initialize cache reader for " + cacheDir.getAbsolutePath());
		}

		try
		{
			byte[] archiveData = reader.readFile(0, 2); // Archive 0 file 2 (config.jag)
			if (archiveData == null)
			{
				throw new IOException("Failed to read config.jag from the 2005 cache");
			}
			Map<String, byte[]> files = reader.readArchive(archiveData);
			byte[] npcDat = files.get(String.valueOf(RetroCacheReader.hashFileName("npc.dat")));
			byte[] npcIdx = files.get(String.valueOf(RetroCacheReader.hashFileName("npc.idx")));
			if (npcDat == null || npcIdx == null)
			{
				throw new IOException("config.jag does not contain npc.dat/npc.idx");
			}
			return RetroNpcDecoder.decodeAll(npcDat, npcIdx);
		}
		finally
		{
			reader.close();
		}
	}

	/**
	 * Extracts the mapping rows from decoded 2005 NPC definitions. Definitions
	 * are visited in ascending def-id order so that when several share a name,
	 * the lowest id deterministically wins (the runtime keys mappings by name).
	 */
	static List<RetroNpcMappingEntry> buildEntries(Map<Integer, RetroNpcDefinition> defs)
	{
		Map<String, RetroNpcMappingEntry> byName = new LinkedHashMap<>();
		for (int defId : new TreeSet<>(defs.keySet()))
		{
			RetroNpcDefinition def = defs.get(defId);
			if (def == null || def.getName() == null || def.getName().isEmpty()
				|| def.getModels() == null || def.getModels().length == 0)
			{
				continue;
			}

			String nameLower = def.getName().toLowerCase(Locale.ROOT).trim();
			RetroNpcCategory category = getNpcCategory(nameLower);
			if (category == null)
			{
				continue;
			}

			byName.putIfAbsent(nameLower, new RetroNpcMappingEntry(
				nameLower, category, def.getModels(), def.getStanceAnimation(), def.getWalkAnimation()));
		}

		return new ArrayList<>(new TreeMap<>(byName).values());
	}

	/**
	 * Name-based category matching for 2005 NPC names. This lives in the
	 * generator (not the runtime) because the generated JSON already carries
	 * each entry's category.
	 */
	static RetroNpcCategory getNpcCategory(String nameLower)
	{
		if (nameLower.contains("lesser demon"))
		{
			return RetroNpcCategory.LESSER_DEMONS;
		}
		if (nameLower.contains("greater demon"))
		{
			return RetroNpcCategory.GREATER_DEMONS;
		}
		if (nameLower.contains("black demon"))
		{
			return RetroNpcCategory.BLACK_DEMONS;
		}
		if (nameLower.contains("dragon") && !nameLower.contains("baby") && !nameLower.contains("king"))
		{
			return RetroNpcCategory.ADULT_DRAGONS;
		}
		if (nameLower.contains("baby") && nameLower.contains("dragon"))
		{
			return RetroNpcCategory.BABY_DRAGONS;
		}
		if (nameLower.contains("goblin"))
		{
			return RetroNpcCategory.GOBLINS;
		}
		if (nameLower.equals("guard"))
		{
			return RetroNpcCategory.GUARDS;
		}
		if (nameLower.contains("imp") && !nameLower.contains("impling") && !nameLower.contains("impaler"))
		{
			return RetroNpcCategory.IMPS;
		}
		if (nameLower.contains("skeleton"))
		{
			return RetroNpcCategory.SKELETONS;
		}
		if (nameLower.contains("zombie"))
		{
			return RetroNpcCategory.ZOMBIES;
		}
		if (nameLower.contains("ghost"))
		{
			return RetroNpcCategory.GHOSTS;
		}
		if (nameLower.contains("hill giant"))
		{
			return RetroNpcCategory.HILL_GIANTS;
		}
		if (nameLower.contains("chicken"))
		{
			return RetroNpcCategory.CHICKENS;
		}
		return null;
	}
}
