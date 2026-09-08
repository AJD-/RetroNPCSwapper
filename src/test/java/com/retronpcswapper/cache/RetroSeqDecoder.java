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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 * position is lost and everything after it in that entry decodes as garbage.
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
 * <h2>The length table is an oracle, not just a seek table</h2>
 *
 * A correctly decoded entry consumes <b>exactly</b> its declared length and ends on the opcode 0
 * terminator. That makes the table self-checking: a wrong field width, an opcode this does not
 * know, and a truncated payload all show up as a residue against the declared end, on the entry
 * where they happen. Anything that fails is dropped rather than stored - a definition that looks
 * decoded but is not is far worse than a missing one. Keeping the frames read before a failure is
 * what once let fabricated sequences into the table with nothing flagging them.
 *
 * <h2>Status</h2>
 *
 * All 1670 declared sequences decode and land exactly on their boundary. If that number falls, the
 * payload is the first thing to suspect rather than this decoder: {@code seq.dat} is a multi-block
 * bzip2 stream, and a decompressor that stops after one block returns a buffer that is correct up
 * to the block boundary and garbage after it.
 */
@Slf4j
public class RetroSeqDecoder
{
	/**
	 * How many failing ids to name in the summary before trailing off. Enough to show whether the
	 * failures cluster - one contiguous run points at the payload, scattered ids at the opcodes.
	 */
	private static final int FAILURES_TO_NAME = 10;

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
			int[] starts = new int[total];
			int[] ends = new int[total];
			int offset = 2;
			for (int i = 0; i < total; i++)
			{
				starts[i] = offset;
				if (idxBuffer.getOffset() < seqIdx.length)
				{
					offset += idxBuffer.readUnsignedShort();
				}
				ends[i] = offset;
			}

			if (offset != seqDat.length)
			{
				log.warn("seq.idx accounts for {} bytes but seq.dat is {} - the payload is "
					+ "truncated, or the index is being misread", offset, seqDat.length);
			}

			Buffer datBuffer = new Buffer(seqDat);
			List<Integer> failed = new ArrayList<>();
			int frameless = 0;
			for (int i = 0; i < total; i++)
			{
				if (starts[i] <= 0 || ends[i] > seqDat.length || ends[i] < starts[i])
				{
					log.debug("2005 sequence {} spans {}..{}, outside seq.dat", i, starts[i], ends[i]);
					failed.add(i);
					continue;
				}

				datBuffer.setOffset(starts[i]);
				RetroSeqDefinition def = decodeSequence(i, datBuffer, ends[i]);
				if (def == null)
				{
					failed.add(i);
				}
				else if (def.getFrameIds() == null)
				{
					// Read cleanly, it just carries no frame list. Nothing here can use it, but it
					// is not a decode failure and must not be reported as one
					frameless++;
				}
				else
				{
					defs.put(i, def);
				}
			}

			if (failed.isEmpty())
			{
				log.info("Decoded all {} declared 2005 sequence definitions ({} carry no frames)",
					total, frameless);
			}
			else
			{
				log.warn("Decoded {} of {} declared 2005 sequence definitions - {} failed, {} carry"
						+ " no frames. First failures: {}{}",
					defs.size(), total, failed.size(), frameless,
					failed.subList(0, Math.min(FAILURES_TO_NAME, failed.size())),
					failed.size() > FAILURES_TO_NAME ? " ..." : "");
			}
		}
		catch (Exception e)
		{
			log.error("Failed decoding 2005 sequence definitions", e);
		}

		return defs;
	}

	/**
	 * Reads one entry, which must end on an opcode 0 terminator at exactly {@code end}.
	 *
	 * @return the definition, or null if the entry did not decode cleanly
	 */
	private static RetroSeqDefinition decodeSequence(int id, Buffer stream, int end)
	{
		RetroSeqDefinition def = new RetroSeqDefinition();
		def.setId(id);

		try
		{
			boolean terminated = false;
			while (stream.getOffset() < end)
			{
				int opcode = stream.readUnsignedByte();
				if (opcode == 0)
				{
					terminated = true;
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
					// Everything after an unknown opcode is garbage, so drop the entry rather than
					// keep however much of it was read before the stream position was lost
					log.debug("Unknown opcode {} in 2005 sequence {}", opcode, id);
					return null;
				}
			}

			if (!terminated)
			{
				log.debug("2005 sequence {} reached its declared end at {} with no terminator",
					id, end);
				return null;
			}

			int residue = stream.getOffset() - end;
			if (residue != 0)
			{
				log.debug("2005 sequence {} ended {} bytes {} its declared end at {}",
					id, Math.abs(residue), residue > 0 ? "past" : "short of", end);
				return null;
			}
		}
		catch (RuntimeException e)
		{
			log.debug("Ran off the end of 2005 sequence {}", id);
			return null;
		}

		return def;
	}
}
