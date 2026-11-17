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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * The retro cache a dev tool reads: the 2005 one, or the Dec-2006 one under {@code -Pcache=2006}.
 */
abstract class RetroEraCache
{
	/** "2006" selects the Dec-2006 cache. */
	private static final String CACHE_PROPERTY = "retronpcswapper.cache";

	/** Index 1 of the RS2 cache holds models. */
	private static final int RETRO_MODEL_INDEX = 1;

	static boolean is2006()
	{
		return "2006".equals(System.getProperty(CACHE_PROPERTY));
	}

	/** "2005" or "2006". */
	static String label()
	{
		return is2006() ? "2006" : "2005";
	}

	/** The gradle flag that points the selected cache somewhere else. */
	static String dirFlag()
	{
		return is2006() ? "-Pretro2006Dir" : "-PretroDir";
	}

	/** The selected cache's directory. {@code dir2005} is where the caller found the 2005 one. */
	static File dir(File dir2005)
	{
		return is2006() ? Retro2006CacheReader.resolveDir() : dir2005;
	}

	/** Opens the selected cache. Null when it does not load. */
	static RetroEraCache open(File dir2005)
	{
		if (is2006())
		{
			Retro2006CacheReader reader = Retro2006CacheReader.open(Retro2006CacheReader.resolveDir());
			return reader == null ? null : new Dec2006(reader);
		}

		RetroCacheReader reader = new RetroCacheReader(dir2005);
		return reader.init() ? new Feb2005(reader) : null;
	}

	abstract Map<Integer, RetroNpcDefinition> npcDefinitions() throws IOException;

	/** The unwrapped model bytes, null when the id holds none. */
	abstract byte[] modelData(int id);

	abstract void close();

	private static final class Feb2005 extends RetroEraCache
	{
		private final RetroCacheReader reader;

		private Feb2005(RetroCacheReader reader)
		{
			this.reader = reader;
		}

		@Override
		Map<Integer, RetroNpcDefinition> npcDefinitions() throws IOException
		{
			return NpcMappingGenerator.decodeDefinitions(reader);
		}

		@Override
		byte[] modelData(int id)
		{
			return gunzipIfNeeded(reader.readFile(RETRO_MODEL_INDEX, id));
		}

		@Override
		void close()
		{
			reader.close();
		}
	}

	private static final class Dec2006 extends RetroEraCache
	{
		private final Retro2006CacheReader reader;

		private Dec2006(Retro2006CacheReader reader)
		{
			this.reader = reader;
		}

		@Override
		Map<Integer, RetroNpcDefinition> npcDefinitions()
		{
			return reader.npcDefinitions();
		}

		@Override
		byte[] modelData(int id)
		{
			return reader.modelData(id);
		}

		@Override
		void close()
		{
			reader.close();
		}
	}

	/** Some RS2 index entries are gzip wrapped and some are stored raw, so sniff rather than guess. */
	private static byte[] gunzipIfNeeded(byte[] data)
	{
		if (data == null || data.length < 2 || (data[0] & 0xFF) != 0x1F || (data[1] & 0xFF) != 0x8B)
		{
			return data;
		}

		try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(data)))
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int read;
			while ((read = in.read(buffer)) > 0)
			{
				out.write(buffer, 0, read);
			}
			return out.toByteArray();
		}
		catch (IOException e)
		{
			return data;
		}
	}
}
