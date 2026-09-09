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
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import lombok.extern.slf4j.Slf4j;

/**
 * Pure Java BZip2 & GZip decompressor for RS2 2005 cache blocks.
 * Matched to standard 317 RS2 client BZip2Decompressor logic.
 *
 * <p><b>The larger subfiles in config.jag span more than one bzip2 block</b> - seq.dat (141,873 B)
 * and npc.dat (161,282 B) take two at this cache's 100,000-byte block size, obj.dat and loc.dat
 * three. Decoding only the first block is not a partial answer that fails loudly: the BWT chain is
 * a permutation cycle, so a walk that runs past the end of a block wraps around and re-emits that
 * block's own bytes over the rest of the buffer. The result reads as correct data followed by
 * plausible-looking garbage. That cost 545 of the 1,670 sequences and 661 of the 1,709 NPC
 * definitions, and was misread for a long time as unfinished opcode coverage in the decoders
 * downstream. Hence, the block loop, the per-block symbol bound on the output walk, and the refusal
 * to return a buffer whose length does not match the one declared.
 */
@Slf4j
public class BZip2Decompressor
{
	/** 48-bit '1AY&SY' marker that opens each compressed block. */
	private static final long BLOCK_MAGIC = 0x314159265359L;

	/** 48-bit sqrt(pi) marker that closes the stream. */
	private static final long STREAM_END_MAGIC = 0x177245385090L;

	private static final BZip2State STATE = new BZip2State();

	public static synchronized byte[] decompress(byte[] compressed, int decompressedLen) throws Exception
	{
		if (compressed == null || compressed.length == 0)
		{
			return new byte[0];
		}

		// Check for GZIP magic number 0x1F8B
		if (compressed.length > 2 && (compressed[0] & 0xFF) == 0x1F && (compressed[1] & 0xFF) == 0x8B)
		{
			byte[] output = new byte[decompressedLen];
			try (GZIPInputStream gzin = new GZIPInputStream(new ByteArrayInputStream(compressed)))
			{
				int read = 0;
				while (read < decompressedLen)
				{
					int r = gzin.read(output, read, decompressedLen - read);
					if (r == -1) break;
					read += r;
				}
			}
			return output;
		}

		byte[] output = new byte[decompressedLen];
		BZip2State s = new BZip2State();
		s.compressed = compressed;
		s.compressedOffset = 0;
		s.decompressed = output;
		s.decompressedOffset = 0;
		s.decompressedLength = decompressedLen;
		s.bsBuff = 0;
		s.bsLive = 0;
		s.blockNo = 0;
		s.cftabCount = 0;
		s.eof = false;

		// A bzip2 stream is a sequence of blocks, and seq.dat, npc.dat, obj.dat and loc.dat in the
		// 2005 config.jag all use more than one. Decoding only the first returns a buffer that is
		// correct up to the block boundary and garbage after it, with nothing to say so.
		while (true)
		{
			long magic = ((long) s.getBits(24) << 24) | s.getBits(24);
			if (s.eof)
			{
				throw new IllegalStateException(
					"BZip2 stream ended without an end-of-stream marker");
			}

			if (magic == BLOCK_MAGIC)
			{
				decompressState(s);
			}
			else if (magic == STREAM_END_MAGIC)
			{
				break;
			}
			else
			{
				throw new IllegalStateException(
					String.format("Unexpected BZip2 block magic %012x after %d of %d bytes",
						magic, s.decompressedOffset, decompressedLen));
			}
		}

		// A short buffer returned in silence is how a one-block decode went unnoticed for so long
		if (s.decompressedOffset != decompressedLen)
		{
			throw new IllegalStateException(String.format(
				"BZip2 produced %d bytes but %d were declared", s.decompressedOffset, decompressedLen));
		}

		return output;
	}

	private static final class BZip2State
	{
		byte[] compressed;
		int compressedOffset;
		byte[] decompressed;
		int decompressedOffset;
		int decompressedLength;
		int bsBuff;
		int bsLive;
		int blockNo;
		int blockSize100k;
		int origPtr;
		int tState;
		int k0;
		int[] tt;
		final int[] unRLE = new int[256];
		int cftabCount;
		final int[] cftab = new int[257];
		final int[] inUse = new int[256];
		final boolean[] inUse16 = new boolean[16];
		final byte[] seqToUnseq = new byte[256];
		final byte[] selector = new byte[180002];
		final byte[] selectorMtf = new byte[180002];
		final byte[][] len = new byte[16][258];
		final int[][] limit = new int[16][258];
		final int[][] base = new int[16][258];
		final int[][] perm = new int[16][258];
		final int[] minLens = new int[16];
		boolean eof;

		int getBits(int n)
		{
			while (bsLive < n)
			{
				if (compressedOffset >= compressed.length)
				{
					eof = true;
					return 0;
				}
				bsBuff = (bsBuff << 8) | (compressed[compressedOffset++] & 0xFF);
				bsLive += 8;
			}
			int mask = (n == 32) ? -1 : ((1 << n) - 1);
			int res = (bsBuff >>> (bsLive - n)) & mask;
			bsLive -= n;
			return res;
		}

		int getBit()
		{
			return getBits(1);
		}

		byte getByte()
		{
			return (byte) getBits(8);
		}
	}

	private static void createDecodeTables(int[] limit, int[] base, int[] perm, byte[] length, int minLen, int maxLen, int alphaSize)
	{
		int pp = 0;
		for (int i = minLen; i <= maxLen; i++)
		{
			for (int j = 0; j < alphaSize; j++)
			{
				if ((length[j] & 0xFF) == i)
				{
					perm[pp++] = j;
				}
			}
		}

		for (int i = 0; i < 23; i++)
		{
			base[i] = 0;
		}
		for (int i = 0; i < alphaSize; i++)
		{
			int l = length[i] & 0xFF;
			if (l + 1 < 23)
			{
				base[l + 1]++;
			}
		}
		for (int i = 1; i < 23; i++)
		{
			base[i] += base[i - 1];
		}

		for (int i = 0; i < 23; i++)
		{
			limit[i] = 0x7FFFFFFF;
		}
		int vec = 0;
		for (int i = minLen; i <= maxLen; i++)
		{
			vec += base[i + 1] - base[i];
			limit[i] = vec - 1;
			vec <<= 1;
		}

		for (int i = minLen + 1; i <= maxLen; i++)
		{
			base[i] = ((limit[i - 1] + 1) << 1) - base[i];
		}
	}

	private static void decompressState(BZip2State s)
	{
		int minLen = 0;
		int[] limit = null;
		int[] base = null;
		int[] perm = null;

		s.blockSize100k = 1;
		if (s.cftab == null)
		{
			s.cftabCount = 0;
		}

		for (int i = 0; i < 256; i++)
		{
			s.inUse[i] = 0;
			s.unRLE[i] = 0;
		}

		// The 48-bit block magic has already been read by the caller, which needed it to tell a
		// block from the end of the stream.

		// 4 bytes CRC/Header bits
		s.getBits(8);
		s.getBits(8);
		s.getBits(8);
		s.getBits(8);
		s.getBits(1);

		s.origPtr = s.getBits(24);

		for (int i = 0; i < 16; i++)
		{
			s.inUse16[i] = s.getBit() == 1;
		}

		for (int i = 0; i < 256; i++)
		{
			s.inUse[i] = 0;
		}

		for (int i = 0; i < 16; i++)
		{
			if (s.inUse16[i])
			{
				for (int j = 0; j < 16; j++)
				{
					if (s.getBit() == 1)
					{
						s.inUse[i * 16 + j] = 1;
					}
				}
			}
		}

		makeMaps(s);
		int alphaSize = s.cftabCount + 2;
		int numGroups = s.getBits(3);
		int numSelectors = s.getBits(15);

		for (int i = 0; i < numSelectors; i++)
		{
			int j = 0;
			while (s.getBit() == 1)
			{
				j++;
			}
			s.selectorMtf[i] = (byte) j;
		}

		byte[] pos = new byte[256];
		for (int v = 0; v < numGroups; v++)
		{
			pos[v] = (byte) v;
		}

		for (int i = 0; i < numSelectors; i++)
		{
			int v = s.selectorMtf[i] & 0xFF;
			byte tmp = pos[v];
			while (v > 0)
			{
				pos[v] = pos[v - 1];
				v--;
			}
			pos[0] = tmp;
			s.selector[i] = tmp;
		}

		for (int t = 0; t < numGroups; t++)
		{
			int curr = s.getBits(5);
			for (int i = 0; i < alphaSize; i++)
			{
				while (s.getBit() == 1)
				{
					if (s.getBit() == 0)
					{
						curr++;
					}
					else
					{
						curr--;
					}
				}
				s.len[t][i] = (byte) curr;
			}
		}

		for (int t = 0; t < numGroups; t++)
		{
			int minL = 32;
			int maxL = 0;
			for (int i = 0; i < alphaSize; i++)
			{
				int l = s.len[t][i] & 0xFF;
				if (l > maxL)
				{
					maxL = l;
				}
				if (l < minL)
				{
					minL = l;
				}
			}
			createDecodeTables(s.limit[t], s.base[t], s.perm[t], s.len[t], minL, maxL, alphaSize);
			s.minLens[t] = minL;
		}

		int groupNo = 0;
		int groupPos = 0;
		int selectorIndex = s.selector[0] & 0xFF;
		limit = s.limit[selectorIndex];
		base = s.base[selectorIndex];
		perm = s.perm[selectorIndex];
		minLen = s.minLens[selectorIndex];

		int reqTtLen = Math.max(900000, s.decompressedLength + 100000);
		if (s.tt == null || s.tt.length < reqTtLen)
		{
			s.tt = new int[reqTtLen];
		}
		int nblock = 0;

		byte[] yy = new byte[256];
		for (int i = 0; i < 256; i++)
		{
			yy[i] = (byte) i;
		}

		int nextSym;
		if (groupPos == 50)
		{
			groupPos = 0;
			groupNo++;
			if (groupNo < numSelectors && groupNo < s.selector.length)
			{
				selectorIndex = s.selector[groupNo] & 0xFF;
				limit = s.limit[selectorIndex];
				base = s.base[selectorIndex];
				perm = s.perm[selectorIndex];
				minLen = s.minLens[selectorIndex];
			}
		}
		groupPos++;

		int zn = minLen;
		int zvec = s.getBits(zn);
		while (zn < 22 && zvec > limit[zn])
		{
			zn++;
			zvec = (zvec << 1) | s.getBit();
		}
		int permIndex = zvec - base[zn];
		if (permIndex >= 0 && permIndex < perm.length)
		{
			nextSym = perm[permIndex];
		}
		else
		{
			nextSym = s.cftabCount + 1;
		}

		while (nextSym != s.cftabCount + 1 && !s.eof)
		{
			if (nextSym == 0 || nextSym == 1)
			{
				int es = -1;
				int N = 1;
				do
				{
					if (nextSym == 0)
					{
						es += N;
					}
					else if (nextSym == 1)
					{
						es += 2 * N;
					}
					N <<= 1;

					if (groupPos == 50)
					{
						groupPos = 0;
						groupNo++;
						if (groupNo < numSelectors && groupNo < s.selector.length)
						{
							selectorIndex = s.selector[groupNo] & 0xFF;
							limit = s.limit[selectorIndex];
							base = s.base[selectorIndex];
							perm = s.perm[selectorIndex];
							minLen = s.minLens[selectorIndex];
						}
					}
					groupPos++;

					zn = minLen;
					zvec = s.getBits(zn);
					while (zn < 22 && zvec > limit[zn])
					{
						zn++;
						zvec = (zvec << 1) | s.getBit();
					}
					int pIdx = zvec - base[zn];
					if (pIdx >= 0 && pIdx < perm.length)
					{
						nextSym = perm[pIdx];
					}
					else
					{
						break;
					}
				}
				while (!s.eof && (nextSym == 0 || nextSym == 1));

				es++;
				byte uc = s.seqToUnseq[yy[0] & 0xFF];
				s.unRLE[uc & 0xFF] += es;

				while (es > 0)
				{
					if (nblock >= s.tt.length)
					{
						s.tt = Arrays.copyOf(s.tt, Math.max(nblock + 100000, s.tt.length * 2));
					}
					s.tt[nblock++] = uc & 0xFF;
					es--;
				}
			}

			if (nextSym == s.cftabCount + 1)
			{
				break;
			}

			int nn = nextSym - 1;
			byte uc = yy[nn];
			System.arraycopy(yy, 0, yy, 1, nn);
			yy[0] = uc;

			byte seq = s.seqToUnseq[uc & 0xFF];
			s.unRLE[seq & 0xFF]++;
			if (nblock >= s.tt.length)
			{
				s.tt = Arrays.copyOf(s.tt, Math.max(nblock + 100000, s.tt.length * 2));
			}
			s.tt[nblock++] = seq & 0xFF;

			if (groupPos == 50)
			{
				groupPos = 0;
				groupNo++;
				if (groupNo < numSelectors && groupNo < s.selector.length)
				{
					selectorIndex = s.selector[groupNo] & 0xFF;
					limit = s.limit[selectorIndex];
					base = s.base[selectorIndex];
					perm = s.perm[selectorIndex];
					minLen = s.minLens[selectorIndex];
				}
			}
			groupPos++;

			zn = minLen;
			zvec = s.getBits(zn);
			while (zn < 22 && zvec > limit[zn])
			{
				zn++;
				zvec = (zvec << 1) | s.getBit();
			}
			int pIdx = zvec - base[zn];
			if (pIdx >= 0 && pIdx < perm.length)
			{
				nextSym = perm[pIdx];
			}
			else
			{
				break;
			}
		}

		s.cftab[0] = 0;
		System.arraycopy(s.unRLE, 0, s.cftab, 1, 256);
		for (int i = 1; i <= 256; i++)
		{
			s.cftab[i] += s.cftab[i - 1];
		}

		int[] bwtTt = new int[nblock];
		for (int i = 0; i < nblock; i++)
		{
			int ch = s.tt[i] & 0xFF;
			int targetIdx = s.cftab[ch]++;
			bwtTt[targetIdx] = (i << 8) | ch;
		}
		s.tt = bwtTt;

		if (s.origPtr < 0 || s.origPtr >= s.tt.length)
		{
			log.warn("BZip2 origPtr {} out of bounds for tt length {}, nblock={}", s.origPtr, s.tt.length, nblock);
			return;
		}

		int p = s.origPtr;
		int entry = s.tt[p];
		byte chState = (byte) (entry & 0xFF);
		p = entry >>> 8;

		s.decompressed[s.decompressedOffset++] = chState;
		int count = 1;
		int runLength = 0;
		int state = 0;

		// This walk is bounded by the block's own symbol count, not by how much output is still
		// wanted. Bounding it by the latter is what made a truncated stream invisible: the BWT
		// chain is a permutation cycle, so on reaching the end of the block it wraps to origPtr
		// and re-emits the same bytes until the buffer happens to be full. Count tt entries
		// rather than output bytes - the run-length step expands, so the two are not the same.
		int consumed = 1;

		// A run in progress still has to be flushed once the last symbol has been read, so state 1
		// keeps going after the symbol budget is spent
		while (state == 1 || (consumed < nblock && p >= 0 && p < s.tt.length))
		{
			if (s.decompressedOffset >= s.decompressedLength)
			{
				break;
			}

			if (state == 0)
			{
				entry = s.tt[p];
				consumed++;
				byte b = (byte) (entry & 0xFF);
				p = entry >>> 8;

				if (b != chState)
				{
					s.decompressed[s.decompressedOffset++] = b;
					chState = b;
					count = 1;
				}
				else
				{
					count++;
					if (count == 4)
					{
						s.decompressed[s.decompressedOffset++] = b;
						entry = s.tt[p];
						consumed++;
						byte repeat = (byte) (entry & 0xFF);
						p = entry >>> 8;

						runLength = repeat & 0xFF;
						count = 0;
						if (runLength > 0)
						{
							state = 1;
						}
					}
					else
					{
						s.decompressed[s.decompressedOffset++] = b;
					}
				}
			}
			else if (state == 1)
			{
				s.decompressed[s.decompressedOffset++] = chState;
				runLength--;
				if (runLength == 0)
				{
					state = 0;
				}
			}
		}
	}

	private static void makeMaps(BZip2State s)
	{
		s.cftabCount = 0;
		for (int i = 0; i < 256; i++)
		{
			if (s.inUse[i] != 0)
			{
				s.seqToUnseq[s.cftabCount] = (byte) i;
				s.cftabCount++;
			}
		}
	}
}
