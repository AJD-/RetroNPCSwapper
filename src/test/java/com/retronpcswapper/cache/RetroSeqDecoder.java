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

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Decodes the 2005 {@code seq.dat} / {@code seq.idx} pair into sequence definitions.
 *
 * <p>Needed because the live sequences these NPCs name have been re-authored against modern
 * skeletons - the models were replaced in 2006 and the animations followed. The 2005 sequences are
 * the only ones authored against the 2005 meshes, so they are the only ones that can drive them.
 *
 * <p>The container is the same shape as {@code npc.dat}/{@code npc.idx}: a count, then a per-entry
 * length table, then concatenated opcode streams. Only opcode 1 carries anything this needs - the
 * frame list - but the rest are still walked, because an unrecognised opcode means the stream
 * position is lost and everything after it decodes as garbage.
 *
 * <h2>Two ways this differs from the modern format</h2>
 *
 * The frame count is a <b>byte</b>, not a short, and the per-frame fields are <b>interleaved</b>
 * rather than written as three separate passes. Both were found by checking against sequences whose
 * frame counts are known from the live cache: skeleton 262 and 259 must come out at 2 and 8.
 *
 * <p>Frame ids are also flat 16-bit values here - the high half that names an animation file in the
 * modern format is always zero - so a sequence says nothing about which of the 411 index 2 files
 * holds its frames. {@link RetroFrameIndex} resolves that, by reading every file's directory.
 *
 * <h2>Status</h2>
 *
 * Decodes 1162 of the 1670 declared sequences, including every one this project needs. The
 * shortfall is unfinished opcode coverage rather than a container problem: an unknown opcode stops
 * that sequence and leaves the rest untouched. Worth finishing before relying on the whole table.
 */
@Slf4j
public class RetroSeqDecoder
{
	public static Map<Integer, RetroSeqDefinition> decodeAll(byte[] seqDat, byte[] seqIdx)
	{
		Map<Integer, RetroSeqDefinition> defs = new HashMap<>();
		if (seqDat == null || seqIdx == null)
		{
			log.warn("seq.dat or seq.idx payload is null");
			return defs;
		}

		try
		{
			Buffer idxBuffer = new Buffer(seqIdx);
			int total = idxBuffer.readUnsignedShort();
			int[] streamIndices = new int[total];
			int offset = 2;
			for (int i = 0; i < total; i++)
			{
				streamIndices[i] = offset;
				if (idxBuffer.getOffset() < seqIdx.length)
				{
					offset += idxBuffer.readUnsignedShort();
				}
			}

			Buffer datBuffer = new Buffer(seqDat);
			for (int i = 0; i < total; i++)
			{
				int seqOffset = streamIndices[i];
				if (seqOffset <= 0 || seqOffset >= seqDat.length)
				{
					continue;
				}

				datBuffer.setOffset(seqOffset);
				RetroSeqDefinition def = decodeSequence(i, datBuffer);
				if (def != null && def.getFrameIds() != null)
				{
					defs.put(i, def);
				}
			}

			log.info("Successfully decoded {} 2005 sequence definitions of {} declared",
				defs.size(), total);
		}
		catch (Exception e)
		{
			log.error("Failed decoding 2005 sequence definitions", e);
		}

		return defs;
	}

	private static RetroSeqDefinition decodeSequence(int id, Buffer stream)
	{
		RetroSeqDefinition def = new RetroSeqDefinition();
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
					int frameCount = stream.readUnsignedByte();
					int[] frameIds = new int[frameCount];
					int[] frameLengths = new int[frameCount];

					// Interleaved per frame, unlike the modern format which splits the same fields
					// into three separate passes. The giveaway in a raw dump is a repeating triple
					// with a constant -1 in the middle - the chat frame, which almost no sequence
					// uses.
					int[] chatFrameIds = new int[frameCount];
					for (int i = 0; i < frameCount; i++)
					{
						frameIds[i] = stream.readUnsignedShort();
						chatFrameIds[i] = stream.readUnsignedShort();
						frameLengths[i] = stream.readUnsignedShort();
					}

					def.setChatFrameIds(chatFrameIds);

					def.setFrameIds(frameIds);
					def.setFrameLengths(frameLengths);
				}
				else if (opcode == 2)
				{
					def.setLoopOffset(stream.readUnsignedShort());
				}
				else if (opcode == 3)
				{
					// Which transforms come from the second animation when two are layered. Read
					// past it rather than skipped blindly, so the stream stays aligned.
					int count = stream.readUnsignedByte();
					int[] interleave = new int[count];
					for (int i = 0; i < count; i++)
					{
						interleave[i] = stream.readUnsignedByte();
					}
					def.setInterleaveOrder(interleave);
				}
				else if (opcode == 4)
				{
					def.setStretches(true);
				}
				else if (opcode == 5)
				{
					def.setForcedPriority(stream.readUnsignedByte());
				}
				else if (opcode == 6)
				{
					def.setLeftHandItem(stream.readUnsignedShort());
				}
				else if (opcode == 7)
				{
					def.setRightHandItem(stream.readUnsignedShort());
				}
				else if (opcode == 8)
				{
					def.setMaxLoops(stream.readUnsignedByte());
				}
				else if (opcode == 9)
				{
					def.setPrecedenceAnimating(stream.readUnsignedByte());
				}
				else if (opcode == 10)
				{
					def.setPriority(stream.readUnsignedByte());
				}
				else if (opcode == 11)
				{
					def.setReplyMode(stream.readUnsignedByte());
				}
				else if (opcode == 12)
				{
					stream.readInt();
				}
				else
				{
					// Everything after an unknown opcode is garbage, so stop rather than produce a
					// definition that looks decoded but is not
					log.debug("Unknown opcode {} in 2005 sequence {}", opcode, id);
					return def.getFrameIds() == null ? null : def;
				}
			}
		}
		catch (RuntimeException e)
		{
			log.debug("Ran off the end of 2005 sequence {}", id);
			return def.getFrameIds() == null ? null : def;
		}

		return def;
	}
}
