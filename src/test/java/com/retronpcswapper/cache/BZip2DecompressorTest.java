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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Covers the one property no cache-gated test can: that a bzip2 stream of more than one block comes
 * back whole.
 *
 * <p>This is not hypothetical. {@code seq.dat} (141,873 bytes), {@code npc.dat} (161,282),
 * {@code obj.dat} and {@code loc.dat} in the 2005 {@code config.jag} are all multi-block, and a
 * decoder that stopped after the first block returned a buffer that was correct for the first
 * ~100,000 bytes and garbage afterwards - silently, because nothing compared the output length
 * against the declared one. It cost 545 of the 1670 2005 sequences, including the guard's shield
 * block, and was misread for a long time as unfinished opcode coverage.
 *
 * <p>A cache-gated test cannot catch this: it passes vacuously wherever the caches are absent. The
 * fixture is therefore committed. It is 150,000 bytes of {@code (i * 7 + i / 256) % 251} - chosen
 * because it has no run of four equal bytes, so the initial run-length pass cannot shrink it under
 * the 100,000-byte block size - compressed with block size 1 and with the four-byte {@code BZh1}
 * header stripped, exactly as the RS2 archive format stores it.
 */
public class BZip2DecompressorTest
{
	private static final String FIXTURE = "multiblock-150k.bz2";
	private static final int FIXTURE_LENGTH = 150_000;

	private static byte[] expected()
	{
		byte[] data = new byte[FIXTURE_LENGTH];
		for (int i = 0; i < data.length; i++)
		{
			data[i] = (byte) ((i * 7 + i / 256) % 251);
		}
		return data;
	}

	private static byte[] fixture() throws IOException
	{
		try (InputStream in = BZip2DecompressorTest.class.getResourceAsStream(FIXTURE))
		{
			assertNotNull("missing test fixture " + FIXTURE, in);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[4096];
			int read;
			while ((read = in.read(buf)) != -1)
			{
				out.write(buf, 0, read);
			}
			return out.toByteArray();
		}
	}

	@Test
	public void testEveryBlockOfAMultiBlockStreamIsDecoded() throws Exception
	{
		byte[] decompressed = BZip2Decompressor.decompress(fixture(), FIXTURE_LENGTH);

		// Compare the whole array rather than a checksum: the failure being guarded against
		// produces a buffer whose first 100,000 bytes are right, so any check that stops early or
		// samples the head would pass
		assertArrayEquals(expected(), decompressed);
	}

	/**
	 * The head being correct is exactly what made the one-block decode so hard to spot, so pin it
	 * separately - it tells a genuine regression apart from a fixture that is simply wrong.
	 */
	@Test
	public void testTheFirstBlockWasNeverTheProblem() throws Exception
	{
		byte[] decompressed = BZip2Decompressor.decompress(fixture(), FIXTURE_LENGTH);

		assertArrayEquals(Arrays.copyOf(expected(), 99_000), Arrays.copyOf(decompressed, 99_000));
	}

	@Test
	public void testATruncatedStreamIsRefusedRatherThanReturnedShort() throws Exception
	{
		byte[] truncated = Arrays.copyOf(fixture(), 1200);

		try
		{
			BZip2Decompressor.decompress(truncated, FIXTURE_LENGTH);
			fail("a stream that cannot produce the declared length must not return quietly");
		}
		catch (IllegalStateException e)
		{
			// expected - returning a short buffer in silence is the bug this guards
		}
	}

	@Test
	public void testGzipPayloadsStillTakeTheGzipPath() throws Exception
	{
		// Index 2 is gzip rather than bzip2, and the same entry point serves both
		byte[] raw = "the quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);
		ByteArrayOutputStream gz = new ByteArrayOutputStream();
		try (java.util.zip.GZIPOutputStream out = new java.util.zip.GZIPOutputStream(gz))
		{
			out.write(raw);
		}

		assertArrayEquals(raw, BZip2Decompressor.decompress(gz.toByteArray(), raw.length));
	}

	@Test
	public void testEmptyPayloadDecompressesToNothing() throws Exception
	{
		assertEquals(0, BZip2Decompressor.decompress(new byte[0], 0).length);
	}
}
