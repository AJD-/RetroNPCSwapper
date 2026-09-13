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
import net.runelite.cache.ObjectManager;
import net.runelite.cache.definitions.ObjectDefinition;
import net.runelite.cache.fs.Store;

/**
 * Dev-only tool that prints <em>object</em> (scenery) definitions from the live OSRS cache, the
 * counterpart to {@link ModernNpcDumper}.
 *
 * <p>Exists because not everything that looks like an animal is an NPC. The dairy cow is scenery -
 * you right-click it to milk it, it never walks - so it has no NPC definition at all, does not
 * reach {@code onNpcSpawned}, and is drawn through a callback this plugin only forwards. Searching
 * the NPC index for it returns nothing, which reads like the NPC is missing rather than like the
 * thing is not an NPC. This tool is what tells those two apart.
 *
 * <p>Run with {@code ./gradlew dumpObjectDefinitions -Pobj=dairy} (ids, or a name substring), and
 * optionally {@code -PcacheDir=...}. Lives in the test sourceSet and is never shipped.
 */
public class ModernObjectDumper
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

			ObjectManager objectManager = new ObjectManager(store);
			objectManager.load();

			System.out.println("Cache: " + cacheDir);
			System.out.println("Object definitions: " + objectManager.getObjects().size());
			System.out.println();

			if (args.length == 0)
			{
				System.out.println("Usage: ./gradlew dumpObjectDefinitions -Pobj=1234,5678");
				System.out.println("       ./gradlew dumpObjectDefinitions -Pobj=dairy");
				return;
			}

			for (String arg : args)
			{
				dump(objectManager, arg.trim());
			}
		}
	}

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
	private static void dump(ObjectManager objectManager, String query)
	{
		if (query.isEmpty())
		{
			return;
		}

		List<ObjectDefinition> matches = new ArrayList<>();
		try
		{
			ObjectDefinition byId = objectManager.getObject(Integer.parseInt(query));
			if (byId != null)
			{
				matches.add(byId);
			}
		}
		catch (NumberFormatException e)
		{
			String needle = query.toLowerCase(Locale.ROOT);
			for (ObjectDefinition object : objectManager.getObjects())
			{
				String name = object.getName();
				if (name != null && name.toLowerCase(Locale.ROOT).contains(needle))
				{
					matches.add(object);
					if (matches.size() >= MAX_NAME_MATCHES)
					{
						break;
					}
				}
			}
		}

		if (matches.isEmpty())
		{
			System.out.println("No object matches '" + query + "'");
			return;
		}

		for (ObjectDefinition object : matches)
		{
			print(object);
		}
	}

	private static void print(ObjectDefinition object)
	{
		System.out.println(object.getName() + " (id " + object.getId() + ")");
		System.out.println("  models        " + Arrays.toString(object.getObjectModels()));
		System.out.println("  modelSize     " + object.getModelSizeX() + "/" + object.getModelSizeHeight()
			+ "/" + object.getModelSizeY());
		System.out.println("  size          " + object.getSizeX() + "x" + object.getSizeY());
		System.out.println("  animationID   " + object.getAnimationID());
		if (object.getRecolorToFind() != null)
		{
			System.out.println("  recolor       " + Arrays.toString(object.getRecolorToFind())
				+ " -> " + Arrays.toString(object.getRecolorToReplace()));
		}
		System.out.println();
	}
}
