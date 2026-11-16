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
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.runelite.cache.IndexType;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.loaders.ModelLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.FSFile;
import net.runelite.cache.fs.Store;

/**
 * The Dec-2006 cache (build 443, OpenRS2 233). JS5 like the live cache, so RuneLite's Store reads
 * it; NPC definitions keep the 2005 opcodes.
 */
@Slf4j
public class Retro2006CacheReader
{
	static final String DEFAULT_DIR = "retrocache/dec2006cache";
	private static final String DIR_PROPERTY = "retronpcswapper.retro2006Dir";

	/** Index 2 archive 9, file id = npc id. */
	private static final int NPC_ARCHIVE = 9;

	private final Store store;

	private Retro2006CacheReader(Store store)
	{
		this.store = store;
	}

	/**
	 * Null when the cache does not load.
	 */
	static Retro2006CacheReader open(File dir)
	{
		Store store = null;
		try
		{
			store = new Store(dir);
			store.load();
			return new Retro2006CacheReader(store);
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Could not load the 2006 cache at {}", dir, e);
			if (store != null)
			{
				try
				{
					store.close();
				}
				catch (IOException ignored)
				{
				}
			}
			return null;
		}
	}

	void close()
	{
		try
		{
			store.close();
		}
		catch (IOException e)
		{
			log.debug("Closing the 2006 cache", e);
		}
	}

	/**
	 * -Dretronpcswapper.retro2006Dir, else {@link #DEFAULT_DIR}.
	 */
	static File resolveDir()
	{
		String configured = System.getProperty(DIR_PROPERTY);
		return new File(configured == null || configured.isEmpty() ? DEFAULT_DIR : configured);
	}

	/**
	 * Every definition by npc id. The ids have gaps, so this walks the archive's own file ids.
	 */
	Map<Integer, RetroNpcDefinition> npcDefinitions()
	{
		Map<Integer, RetroNpcDefinition> defs = new HashMap<>();
		Archive archive = store.getIndex(IndexType.CONFIGS).getArchive(NPC_ARCHIVE);
		if (archive == null)
		{
			return defs;
		}

		int failed = 0;
		try
		{
			for (FSFile file : archive.getFiles(store.getStorage().loadArchive(archive)).getFiles())
			{
				RetroNpcDefinition def = RetroNpcDecoder.decodeNpc(file.getFileId(), file.getContents());
				if (def == null)
				{
					failed++;
				}
				else
				{
					defs.put(file.getFileId(), def);
				}
			}
		}
		catch (IOException e)
		{
			throw new IllegalStateException("2006 NPC archive did not load", e);
		}

		if (failed > 0)
		{
			log.warn("{} 2006 NPC definitions did not decode and were dropped", failed);
		}
		return defs;
	}

	/**
	 * Null when the id has no archive.
	 */
	ModelDefinition model(int id)
	{
		byte[] data = modelData(id);
		return data == null ? null : new ModelLoader().load(id, data);
	}

	/**
	 * The unwrapped model bytes, null when the id has no archive.
	 */
	byte[] modelData(int id)
	{
		Archive archive = store.getIndex(IndexType.MODELS).getArchive(id);
		if (archive == null)
		{
			return null;
		}

		try
		{
			return archive.decompress(store.getStorage().loadArchive(archive));
		}
		catch (IOException e)
		{
			throw new IllegalStateException("2006 model " + id + " did not load", e);
		}
	}
}
