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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Covers the opcode loop and the boundary check with hand-built byte arrays.
 *
 * <p>Synthetic on purpose. {@code decodeAll} takes two plain {@code byte[]}s, so none of this needs
 * a cache - which matters, because every other test that touches this decoder returns early when
 * the 2005 cache is absent and therefore passes vacuously on any machine that does not have it.
 */
public class RetroSeqDecoderTest
{
	/**
	 * Builds a {@code seq.dat}/{@code seq.idx} pair around the given entry bodies, each of which is
	 * the opcode stream without its terminating zero.
	 */
	private static Map<Integer, RetroSeqDefinition> decode(byte[]... entries) throws Exception
	{
		ByteArrayOutputStream dat = new ByteArrayOutputStream();
		ByteArrayOutputStream idx = new ByteArrayOutputStream();

		// Both files open with the entry count
		dat.write(entries.length >> 8);
		dat.write(entries.length & 0xFF);
		idx.write(entries.length >> 8);
		idx.write(entries.length & 0xFF);

		for (byte[] entry : entries)
		{
			dat.write(entry);
			int length = entry.length;
			idx.write(length >> 8);
			idx.write(length & 0xFF);
		}

		return RetroSeqDecoder.decodeAll(dat.toByteArray(), idx.toByteArray());
	}

	private static byte[] bytes(int... values)
	{
		byte[] out = new byte[values.length];
		for (int i = 0; i < values.length; i++)
		{
			out[i] = (byte) values[i];
		}
		return out;
	}

	/** An opcode 1 frame list plus the terminator, in the 2005 layout. */
	private static byte[] frames(int... frameIds)
	{
		List<Integer> out = new ArrayList<>();
		out.add(1);
		out.add(frameIds.length);
		for (int frameId : frameIds)
		{
			out.add(frameId >> 8);
			out.add(frameId & 0xFF);
			// The chat frame, -1 in almost every real sequence
			out.add(0xFF);
			out.add(0xFF);
			// The frame length, 0 meaning "use the default"
			out.add(0);
			out.add(0);
		}
		out.add(0);

		byte[] bytes = new byte[out.size()];
		for (int i = 0; i < bytes.length; i++)
		{
			bytes[i] = (byte) (int) out.get(i);
		}
		return bytes;
	}

	@Test
	public void testFrameListIsReadAsAByteCountAndInterleavedTriples() throws Exception
	{
		Map<Integer, RetroSeqDefinition> defs = decode(frames(4192, 4193, 4200));

		RetroSeqDefinition def = defs.get(0);
		assertNotNull("the entry should have decoded", def);
		assertArrayEquals(new int[]{4192, 4193, 4200}, def.getFrameIds());
		assertArrayEquals(new int[]{0xFFFF, 0xFFFF, 0xFFFF}, def.getChatFrameIds());
		assertArrayEquals(new int[]{0, 0, 0}, def.getFrameLengths());
	}

	/**
	 * Every opcode has to consume exactly the right number of bytes, or the entry will not land on
	 * its declared end. Each case here is one opcode followed by a frame list, so a wrong width
	 * shows up as a rejected entry rather than as a wrong field.
	 */
	@Test
	public void testEveryOpcodeConsumesItsDeclaredWidth() throws Exception
	{
		byte[][] payloads = {
			bytes(2, 0x01, 0x02),                   // loop offset, short
			bytes(3, 0x02, 0x09, 0x0A),             // interleave order, byte count then bytes
			bytes(4),                               // stretches, no payload
			bytes(5, 0x07),                         // forced priority, byte
			bytes(6, 0x04, 0xD2),                   // left hand item, short
			bytes(7, 0x04, 0xD3),                   // right hand item, short
			bytes(8, 0x63),                         // max loops, byte
			bytes(9, 0x01),                         // precedence animating, byte
			bytes(10, 0x05),                        // priority, byte
			bytes(11, 0x02),                        // reply mode, byte
			bytes(12, 0x00, 0x00, 0x01, 0x00),      // discarded int
		};

		for (byte[] payload : payloads)
		{
			ByteArrayOutputStream entry = new ByteArrayOutputStream();
			entry.write(payload);
			entry.write(frames(7419, 7428));

			Map<Integer, RetroSeqDefinition> defs = decode(entry.toByteArray());
			assertNotNull("opcode " + payload[0] + " did not land on its boundary", defs.get(0));
			assertArrayEquals(new int[]{7419, 7428}, defs.get(0).getFrameIds());
		}
	}

	@Test
	public void testDecodedFieldsCarryTheirValues() throws Exception
	{
		ByteArrayOutputStream entry = new ByteArrayOutputStream();
		entry.write(bytes(2, 0x00, 0x03));
		entry.write(bytes(3, 0x02, 0x09, 0x0A));
		entry.write(bytes(4));
		entry.write(bytes(5, 0x07));
		entry.write(bytes(6, 0x04, 0xD2));
		entry.write(bytes(7, 0x04, 0xD3));
		entry.write(bytes(8, 0x63));
		entry.write(bytes(10, 0x05));
		entry.write(bytes(11, 0x02));
		entry.write(frames(1));

		RetroSeqDefinition def = decode(entry.toByteArray()).get(0);
		assertNotNull(def);
		assertEquals(3, def.getLoopOffset());
		assertArrayEquals(new int[]{9, 10}, def.getInterleaveOrder());
		assertTrue(def.isStretches());
		assertEquals(7, def.getForcedPriority());
		assertEquals(1234, def.getLeftHandItem());
		assertEquals(1235, def.getRightHandItem());
		assertEquals(99, def.getMaxLoops());
		assertEquals(5, def.getPriority());
		assertEquals(2, def.getReplyMode());
	}

	@Test
	public void testAnUnknownOpcodeDropsTheEntryRatherThanKeepingItsFrames() throws Exception
	{
		// The frame list is read before the unknown opcode is reached, so a decoder that keeps
		// whatever it read so far would store this one with plausible-looking frames
		ByteArrayOutputStream entry = new ByteArrayOutputStream();
		entry.write(bytes(1, 1, 0x10, 0x60, 0xFF, 0xFF, 0x00, 0x00));
		entry.write(bytes(13, 0x00));
		entry.write(bytes(0));

		assertNull(decode(entry.toByteArray()).get(0));
	}

	@Test
	public void testAnEntryEndingShortOfItsDeclaredLengthIsRejected() throws Exception
	{
		// A trailing byte the opcode stream never accounts for: the terminator arrives early
		byte[] entry = Arrays.copyOf(frames(4192), frames(4192).length + 1);

		assertNull(decode(entry).get(0));
	}

	@Test
	public void testAnEntryRunningPastItsDeclaredLengthIsRejected() throws Exception
	{
		// Declare one byte less than the body needs, so the frame list overruns the boundary
		byte[] body = frames(4192, 4193);
		ByteArrayOutputStream dat = new ByteArrayOutputStream();
		ByteArrayOutputStream idx = new ByteArrayOutputStream();
		dat.write(0);
		dat.write(1);
		idx.write(0);
		idx.write(1);
		dat.write(body);
		idx.write(0);
		idx.write(body.length - 1);

		assertTrue(RetroSeqDecoder.decodeAll(dat.toByteArray(), idx.toByteArray()).isEmpty());
	}

	@Test
	public void testAnEntryWithNoTerminatorIsRejected() throws Exception
	{
		byte[] body = frames(4192);

		assertNull(decode(Arrays.copyOf(body, body.length - 1)).get(0));
	}

	/**
	 * The reason each entry is re-seeked from its own index offset rather than read where the last
	 * one stopped. One malformed entry must not cost the entries after it.
	 */
	@Test
	public void testOneBadEntryDoesNotDisturbItsNeighbours() throws Exception
	{
		ByteArrayOutputStream bad = new ByteArrayOutputStream();
		bad.write(bytes(13, 0x00, 0x00));
		bad.write(bytes(0));

		Map<Integer, RetroSeqDefinition> defs =
			decode(frames(100, 101), bad.toByteArray(), frames(200));

		assertNotNull("the entry before the bad one should survive", defs.get(0));
		assertNull(defs.get(1));
		assertNotNull("the entry after the bad one should survive", defs.get(2));
		assertArrayEquals(new int[]{200}, defs.get(2).getFrameIds());
	}

	@Test
	public void testAnEntryWithNoFrameListIsNotStoredButIsNotAFailureEither() throws Exception
	{
		// Opcode 5 then the terminator: it reads cleanly, it just has nothing to animate
		Map<Integer, RetroSeqDefinition> defs = decode(bytes(5, 0x07, 0), frames(4192));

		assertFalse("a frameless entry has nothing to offer a clip", defs.containsKey(0));
		assertNotNull(defs.get(1));
	}

	@Test
	public void testNullPayloadsAreHandled()
	{
		assertTrue(RetroSeqDecoder.decodeAll(null, new byte[]{0, 0}).isEmpty());
		assertTrue(RetroSeqDecoder.decodeAll(new byte[]{0, 0}, null).isEmpty());
	}
}
