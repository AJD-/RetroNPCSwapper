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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import lombok.extern.slf4j.Slf4j;

/**
 * Decodes 2005 cache index 2, which holds the animation frames the dragon and demon meshes were
 * authored against. The live sequences those NPCs still name were re-authored for the modern rigs,
 * so the frames behind them drive the 2005 meshes as garbage; these are the frames that fit.
 *
 * <h2>Container</h2>
 * Each of the 411 files is gzip wrapped and, inflated, is a frame <em>group</em>: a directory, four
 * data sections, and an 8-byte trailer of section lengths.
 *
 * <pre>
 * u16  frameCount
 * frameCount x { u16 frameId ; u8 transformCount }     // = dirLength
 * sum(transformCount) x u8 axisMask                    // bit0 x, bit1 y, bit2 z
 * valuesLength bytes                                   // one signed smart per set bit
 * frameCount x u8                                      // per-frame duration, unused here
 * framemap: u8 length ; length x u8 type ;
 *           length x { u8 groupCount ; groupCount x u8 group }
 * trailer: u16 dirLength ; u16 sum(transformCount) ; u16 valuesLength ; u16 frameCount
 * </pre>
 *
 * <p>Three things differ from the modern format:
 * <ul>
 *   <li>frame ids are <b>flat 16-bit and globally unique</b> - 12,597 of them running 0..12596 with
 *       no gaps and no duplicates - so the directories are themselves the frame-to-file map;
 *   <li>a frame's mask run is its own {@code transformCount}, not the framemap length. Trailing
 *       transforms are simply omitted: 39 of 41 for the skeleton, 65 of 77 for the dragon;
 *   <li>the framemap group lists are interleaved rather than counts-first, see
 *       {@link RetroFramemapDefinition}.
 * </ul>
 *
 * <p>Everything else - the mask semantics, the pivot back-fill, the type-3 default of 128 and the
 * signed smart encoding - is identical to {@code FrameLoader.load} in {@code net.runelite:cache},
 * which {@link #decodeFrame} is a port of.
 *
 * <p>Test sourceSet only. The shipped plugin reads the pre-baked bundle, never a cache.
 */
@Slf4j
public class RetroFrameDecoder
{
	/** Index 2 of the RS2 cache holds animation frames. */
	public static final int RETRO_FRAME_INDEX = 2;

	/**
	 * An embedded 2005 framemap has no id, so one is synthesised from its file id. Offset far enough
	 * to never collide with a live framemap id, which the same bundle also carries.
	 */
	public static final int RETRO_RIG_ID_BASE = 100_000;

	private static final int TRAILER_BYTES = 8;

	public static int rigId(int fileId)
	{
		return RETRO_RIG_ID_BASE + fileId;
	}

	/**
	 * Decodes every file in index 2. Done wholesale because the alternative is not available: a
	 * frame cannot be located without reading the directories, and at 411 files and 900KB inflated
	 * there is nothing to gain by being lazy.
	 */
	public static RetroFrameIndex decodeAll(RetroCacheReader retro)
	{
		Map<Integer, RetroFrameGroup> groups = new LinkedHashMap<>();
		Map<Integer, Integer> frameToFile = new HashMap<>();

		for (int fileId = 0; ; fileId++)
		{
			byte[] raw = retro.readFile(RETRO_FRAME_INDEX, fileId);
			if (raw == null)
			{
				break;
			}

			RetroFrameGroup group;
			try
			{
				group = decode(fileId, gunzipIfNeeded(raw));
			}
			catch (RuntimeException e)
			{
				// Not survivable the way an unknown sequence opcode is: a misread group yields
				// plausible-looking transforms that animate as noise, so drop it and say so
				log.warn("2005 frame group {} did not decode", fileId, e);
				continue;
			}

			groups.put(fileId, group);
			for (RetroFrameDefinition frame : group.getFrames())
			{
				Integer previous = frameToFile.put(frame.getFrameId(), fileId);
				if (previous != null)
				{
					log.warn("2005 frame {} is in both file {} and file {}",
						frame.getFrameId(), previous, fileId);
				}
			}
		}

		return new RetroFrameIndex(groups, frameToFile);
	}

	/** Decodes one inflated index 2 file. */
	public static RetroFrameGroup decode(int fileId, byte[] data)
	{
		if (data == null || data.length < TRAILER_BYTES + 2)
		{
			throw new IllegalArgumentException("Frame group " + fileId + " is too short for a trailer");
		}

		int trailerOffset = data.length - TRAILER_BYTES;

		Buffer trailer = new Buffer(data);
		trailer.setOffset(trailerOffset);
		int dirLength = trailer.readUnsignedShort();
		int totalTransforms = trailer.readUnsignedShort();
		int valuesLength = trailer.readUnsignedShort();
		int frameCount = trailer.readUnsignedShort();

		Buffer directory = new Buffer(data);
		int declaredCount = directory.readUnsignedShort();
		if (declaredCount != frameCount || dirLength != frameCount * 3)
		{
			throw new IllegalStateException("Frame group " + fileId + " header says " + declaredCount
				+ " frames, trailer says " + frameCount + " over " + dirLength + " directory bytes");
		}

		int[] frameIds = new int[frameCount];
		int[] transformCounts = new int[frameCount];
		int transformSum = 0;
		for (int i = 0; i < frameCount; i++)
		{
			frameIds[i] = directory.readUnsignedShort();
			transformCounts[i] = directory.readUnsignedByte();
			transformSum += transformCounts[i];
		}

		if (transformSum != totalTransforms)
		{
			throw new IllegalStateException("Frame group " + fileId + " directory totals "
				+ transformSum + " transforms, trailer says " + totalTransforms);
		}

		int masksOffset = 2 + dirLength;
		int valuesOffset = masksOffset + totalTransforms;
		int durationsOffset = valuesOffset + valuesLength;
		int framemapOffset = durationsOffset + frameCount;

		RetroFramemapDefinition framemap = decodeFramemap(fileId, data, framemapOffset, trailerOffset);

		Buffer masks = new Buffer(data);
		masks.setOffset(masksOffset);
		Buffer values = new Buffer(data);
		values.setOffset(valuesOffset);

		List<RetroFrameDefinition> frames = new ArrayList<>(frameCount);
		for (int i = 0; i < frameCount; i++)
		{
			frames.add(decodeFrame(frameIds[i], transformCounts[i], framemap.getTypes(), masks, values));
		}

		// Both streams landing exactly on the next section is what makes a decode trustworthy - the
		// same reason FrameLoader throws when its value stream does not end at the end of the file
		if (masks.getOffset() != valuesOffset || values.getOffset() != durationsOffset)
		{
			throw new IllegalStateException("Frame group " + fileId + " left slack: masks ended at "
				+ masks.getOffset() + " (expected " + valuesOffset + "), values at "
				+ values.getOffset() + " (expected " + durationsOffset + ")");
		}

		return new RetroFrameGroup(fileId, framemap, frames);
	}

	private static RetroFramemapDefinition decodeFramemap(int fileId, byte[] data, int offset, int end)
	{
		Buffer buffer = new Buffer(data);
		buffer.setOffset(offset);

		int length = buffer.readUnsignedByte();
		int[] types = new int[length];
		for (int i = 0; i < length; i++)
		{
			types[i] = buffer.readUnsignedByte();
		}

		int[][] groups = new int[length][];
		for (int i = 0; i < length; i++)
		{
			groups[i] = new int[buffer.readUnsignedByte()];
			for (int j = 0; j < groups[i].length; j++)
			{
				groups[i][j] = buffer.readUnsignedByte();
			}
		}

		if (buffer.getOffset() != end)
		{
			throw new IllegalStateException("Frame group " + fileId + " framemap ended at "
				+ buffer.getOffset() + ", expected " + end);
		}

		return new RetroFramemapDefinition(rigId(fileId), types, groups);
	}

	/**
	 * Port of {@code FrameLoader.load} from {@code net.runelite:cache}, reading the mask and value
	 * streams of one frame. The only change is the loop bound: a 2005 frame declares its own
	 * transform count and omits trailing transforms, where the modern format always runs the full
	 * framemap.
	 */
	private static RetroFrameDefinition decodeFrame(int frameId, int transformCount, int[] types,
		Buffer masks, Buffer values)
	{
		// The pivot back-fill can emit one extra op per transform, so twice is the ceiling
		int[] indexFrameIds = new int[transformCount * 2];
		int[] translatorX = new int[transformCount * 2];
		int[] translatorY = new int[transformCount * 2];
		int[] translatorZ = new int[transformCount * 2];

		int lastI = -1;
		int index = 0;

		for (int i = 0; i < transformCount; i++)
		{
			int mask = masks.readUnsignedByte();
			if (mask <= 0)
			{
				continue;
			}

			if (types[i] != 0)
			{
				// Reset the pivot this transform turns about, rather than inheriting whichever one
				// the last emitted op left behind
				for (int j = i - 1; j > lastI; j--)
				{
					if (types[j] == 0)
					{
						indexFrameIds[index] = j;
						translatorX[index] = 0;
						translatorY[index] = 0;
						translatorZ[index] = 0;
						index++;
						break;
					}
				}
			}

			indexFrameIds[index] = i;
			int unset = types[i] == 3 ? 128 : 0;
			translatorX[index] = (mask & 1) != 0 ? values.readSignedSmart() : unset;
			translatorY[index] = (mask & 2) != 0 ? values.readSignedSmart() : unset;
			translatorZ[index] = (mask & 4) != 0 ? values.readSignedSmart() : unset;

			lastI = i;
			index++;
		}

		return new RetroFrameDefinition(frameId,
			Arrays.copyOf(indexFrameIds, index),
			Arrays.copyOf(translatorX, index),
			Arrays.copyOf(translatorY, index),
			Arrays.copyOf(translatorZ, index));
	}

	/** Some RS2 index entries are gzip wrapped and some are stored raw, so sniff rather than guess. */
	static byte[] gunzipIfNeeded(byte[] data)
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
