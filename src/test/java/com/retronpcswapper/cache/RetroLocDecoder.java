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

/**
 * Decodes the 2005 object (scenery) definitions, {@code loc.dat}/{@code loc.idx} in config.jag.
 *
 * <p>Same container as {@code npc.dat}: the index is a u16 count followed by a u16 length per
 * definition, and the data starts two bytes in. Like {@link RetroNpcDecoder}, a definition that
 * fails to decode is dropped rather than kept half-read.
 */
@Slf4j
public class RetroLocDecoder
{
	/**
	 * Decodes all object definitions from a local 2005 cache directory.
	 */
	static Map<Integer, RetroLocDefinition> decodeDefinitions(File cacheDir) throws IOException
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
			byte[] locDat = files.get(String.valueOf(RetroCacheReader.hashFileName("loc.dat")));
			byte[] locIdx = files.get(String.valueOf(RetroCacheReader.hashFileName("loc.idx")));
			if (locDat == null || locIdx == null)
			{
				throw new IOException("config.jag does not contain loc.dat/loc.idx");
			}
			return decodeAll(locDat, locIdx);
		}
		finally
		{
			reader.close();
		}
	}

	static Map<Integer, RetroLocDefinition> decodeAll(byte[] locDat, byte[] locIdx)
	{
		Map<Integer, RetroLocDefinition> defs = new HashMap<>();

		Buffer idxBuffer = new Buffer(locIdx);
		int total = idxBuffer.readUnsignedShort();
		int offset = 2;
		int failed = 0;
		for (int id = 0; id < total; id++)
		{
			int length = idxBuffer.readUnsignedShort();
			Buffer datBuffer = new Buffer(locDat);
			datBuffer.setOffset(offset);
			RetroLocDefinition def = decodeLoc(id, datBuffer, offset + length);
			if (def == null)
			{
				failed++;
			}
			else
			{
				defs.put(id, def);
			}
			offset += length;
		}

		if (failed > 0)
		{
			log.warn("{} 2005 object definitions did not decode and were dropped", failed);
		}
		return defs;
	}

	/**
	 * Decodes one definition, which must end with opcode 0 exactly at {@code end} - a definition
	 * that stops early or runs over has been misread, whatever its fields look like.
	 */
	private static RetroLocDefinition decodeLoc(int id, Buffer stream, int end)
	{
		RetroLocDefinition def = new RetroLocDefinition();
		def.setId(id);

		try
		{
			while (true)
			{
				int opcode = stream.readUnsignedByte();
				if (opcode == 0)
				{
					break;
				}

				if (opcode == 1)
				{
					int count = stream.readUnsignedByte();
					int[] models = new int[count];
					int[] types = new int[count];
					for (int i = 0; i < count; i++)
					{
						models[i] = stream.readUnsignedShort();
						types[i] = stream.readUnsignedByte();
					}
					def.setModels(models);
					def.setModelTypes(types);
				}
				else if (opcode == 2)
				{
					def.setName(stream.readString());
				}
				else if (opcode == 3)
				{
					def.setDescription(stream.readString());
				}
				else if (opcode == 5)
				{
					int count = stream.readUnsignedByte();
					int[] models = new int[count];
					for (int i = 0; i < count; i++)
					{
						models[i] = stream.readUnsignedShort();
					}
					def.setModels(models);
					def.setModelTypes(null);
				}
				else if (opcode == 14)
				{
					def.setSizeX(stream.readUnsignedByte());
				}
				else if (opcode == 15)
				{
					def.setSizeY(stream.readUnsignedByte());
				}
				else if (opcode == 17 || opcode == 18 || opcode == 22 || opcode == 23
					|| opcode == 64 || opcode == 73 || opcode == 74)
				{
					// Collision, shading and shadow flags
				}
				else if (opcode == 21)
				{
					def.setContouredGround(true);
				}
				else if (opcode == 62)
				{
					def.setMirrored(true);
				}
				else if (opcode == 19 || opcode == 28 || opcode == 69 || opcode == 75)
				{
					stream.readUnsignedByte(); // interactive, decor offset, surroundings, support items
				}
				else if (opcode == 24)
				{
					int animation = stream.readUnsignedShort();
					def.setAnimation(animation == 0xFFFF ? -1 : animation);
				}
				else if (opcode == 29)
				{
					def.setAmbient(stream.readByte());
				}
				else if (opcode == 39)
				{
					def.setContrast(stream.readByte());
				}
				else if (opcode >= 30 && opcode < 39)
				{
					String action = stream.readString();
					def.getActions()[opcode - 30] = "hidden".equalsIgnoreCase(action) ? null : action;
				}
				else if (opcode == 40)
				{
					int colors = stream.readUnsignedByte();
					short[] original = new short[colors];
					short[] replacement = new short[colors];
					for (int c = 0; c < colors; c++)
					{
						original[c] = (short) stream.readUnsignedShort();
						replacement[c] = (short) stream.readUnsignedShort();
					}
					def.setOriginalColors(original);
					def.setReplacementColors(replacement);
				}
				else if (opcode == 60 || opcode == 68)
				{
					stream.readUnsignedShort(); // map function, map scene
				}
				else if (opcode == 65)
				{
					def.setScaleX(stream.readUnsignedShort());
				}
				else if (opcode == 66)
				{
					def.setScaleY(stream.readUnsignedShort());
				}
				else if (opcode == 67)
				{
					def.setScaleZ(stream.readUnsignedShort());
				}
				else if (opcode == 70)
				{
					def.setOffsetX(stream.readShort());
				}
				else if (opcode == 71)
				{
					def.setOffsetY(stream.readShort());
				}
				else if (opcode == 72)
				{
					def.setOffsetZ(stream.readShort());
				}
				else if (opcode == 77)
				{
					int varbit = stream.readUnsignedShort();
					int varp = stream.readUnsignedShort();
					def.setVarbit(varbit == 0xFFFF ? -1 : varbit);
					def.setVarp(varp == 0xFFFF ? -1 : varp);
					int count = stream.readUnsignedByte();
					int[] children = new int[count + 1];
					for (int i = 0; i <= count; i++)
					{
						int child = stream.readUnsignedShort();
						children[i] = child == 0xFFFF ? -1 : child;
					}
					def.setChildren(children);
				}
				else
				{
					log.debug("2005 object {} has unknown opcode {}", id, opcode);
					return null;
				}
			}
		}
		catch (Exception e)
		{
			log.debug("2005 object {} did not decode", id, e);
			return null;
		}

		if (stream.getOffset() != end)
		{
			log.debug("2005 object {} ended at {}, its index says {}", id, stream.getOffset(), end);
			return null;
		}
		return def;
	}
}
