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
package net.runelite.client.plugins.retronpcswapper.cache;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetroNpcDecoder
{
	private static final Logger log = LoggerFactory.getLogger(RetroNpcDecoder.class);

	public static Map<Integer, RetroNpcDefinition> decodeAll(byte[] npcDat, byte[] npcIdx)
	{
		Map<Integer, RetroNpcDefinition> defs = new HashMap<>();
		if (npcDat == null || npcIdx == null)
		{
			log.warn("npc.dat or npc.idx payload is null");
			return defs;
		}

		try
		{
			Buffer idxBuffer = new Buffer(npcIdx);
			int totalNpcs = idxBuffer.readUnsignedShort();
			int[] streamIndices = new int[totalNpcs];
			int offset = 2;
			for (int j = 0; j < totalNpcs; j++)
			{
				streamIndices[j] = offset;
				if (idxBuffer.getOffset() < npcIdx.length)
				{
					offset += idxBuffer.readUnsignedShort();
				}
			}

			Buffer datBuffer = new Buffer(npcDat);
			for (int j = 0; j < totalNpcs; j++)
			{
				int npcOffset = streamIndices[j];
				if (npcOffset > 0 && npcOffset < npcDat.length)
				{
					datBuffer.setOffset(npcOffset);
					RetroNpcDefinition def = decodeNpc(j, datBuffer, npcDat.length);
					if (def != null && def.getName() != null && !def.getName().isEmpty())
					{
						defs.put(j, def);
					}
				}
			}

			log.info("Successfully decoded {} 2005 Retro NPC definitions", defs.size());
		}
		catch (Exception e)
		{
			log.error("Failed decoding 2005 NPC definitions", e);
		}

		return defs;
	}

	private static RetroNpcDefinition decodeNpc(int id, Buffer stream, int maxLen)
	{
		RetroNpcDefinition def = new RetroNpcDefinition();
		def.setId(id);

		try
		{
			while (stream.getOffset() < maxLen)
			{
				int opcode = stream.readUnsignedByte();
				if (opcode == 0)
				{
					break;
				}

			if (opcode == 1)
			{
				int modelCount = stream.readUnsignedByte();
				int[] models = new int[modelCount];
				for (int j = 0; j < modelCount; j++)
				{
					models[j] = stream.readUnsignedShort();
				}
				def.setModels(models);
			}
			else if (opcode == 2)
			{
				def.setName(stream.readString());
			}
			else if (opcode == 3)
			{
				def.setDescription(stream.readString());
			}
			else if (opcode == 12)
			{
				def.setSize(stream.readByte());
			}
			else if (opcode == 13)
			{
				def.setStanceAnimation(stream.readUnsignedShort());
			}
			else if (opcode == 14)
			{
				def.setWalkAnimation(stream.readUnsignedShort());
			}
			else if (opcode == 17)
			{
				def.setWalkAnimation(stream.readUnsignedShort());
				stream.readUnsignedShort(); // turnAroundAnim
				stream.readUnsignedShort(); // turnRightAnim
				stream.readUnsignedShort(); // turnLeftAnim
			}
			else if (opcode >= 30 && opcode < 40)
			{
				String[] actions = def.getActions();
				int actionIdx = opcode - 30;
				String actionStr = stream.readString();
				if (actionIdx >= 0 && actionIdx < actions.length)
				{
					if ("hidden".equalsIgnoreCase(actionStr))
					{
						actionStr = null;
					}
					actions[actionIdx] = actionStr;
				}
				def.setActions(actions);
			}
			else if (opcode == 40)
			{
				int colors = stream.readUnsignedByte();
				for (int c = 0; c < colors; c++)
				{
					stream.readUnsignedShort(); // originalColor
					stream.readUnsignedShort(); // modifiedColor
				}
			}
			else if (opcode == 60)
			{
				int addModelCount = stream.readUnsignedByte();
				int[] addModels = new int[addModelCount];
				for (int j = 0; j < addModelCount; j++)
				{
					addModels[j] = stream.readUnsignedShort();
				}
				def.setAdditionalModels(addModels);
			}
			else if (opcode == 90 || opcode == 91 || opcode == 92)
			{
				stream.readUnsignedShort();
			}
			else if (opcode == 93)
			{
				// drawMapDot = false
			}
			else if (opcode == 95)
			{
				def.setCombatLevel(stream.readUnsignedShort());
			}
			else if (opcode == 97)
			{
				stream.readUnsignedShort(); // scaleXZ
			}
			else if (opcode == 98)
			{
				stream.readUnsignedShort(); // scaleY
			}
			else if (opcode == 99)
			{
				// priorityRender = true
			}
			else if (opcode == 100)
			{
				stream.readByte(); // lightModifier1
			}
			else if (opcode == 101)
			{
				stream.readByte(); // lightModifier2
			}
			else if (opcode == 102)
			{
				stream.readUnsignedShort(); // headIcon
			}
			else if (opcode == 103)
			{
				stream.readUnsignedShort(); // degreesToTurn
			}
			else if (opcode == 106)
			{
				stream.readUnsignedShort(); // varbitId
				stream.readUnsignedShort(); // varpId
				int childCount = stream.readUnsignedByte();
				for (int c = 0; c <= childCount; c++)
				{
					stream.readUnsignedShort(); // childrenIDs
				}
			}
			else if (opcode == 107)
			{
				// clickable = false
			}
		}
		}
		catch (Exception ignored)
		{
		}

		return def;
	}
}
