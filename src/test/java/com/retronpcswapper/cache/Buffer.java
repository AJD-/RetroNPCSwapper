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

import java.nio.charset.StandardCharsets;

public class Buffer
{
	private final byte[] array;
	private int offset;

	public Buffer(byte[] array)
	{
		this.array = array;
		this.offset = 0;
	}

	public int getOffset()
	{
		return offset;
	}

	public void setOffset(int offset)
	{
		this.offset = offset;
	}

	public int readUnsignedByte()
	{
		return array[offset++] & 0xFF;
	}

	public byte readByte()
	{
		return array[offset++];
	}

	public int readUnsignedShort()
	{
		offset += 2;
		return ((array[offset - 2] & 0xFF) << 8) + (array[offset - 1] & 0xFF);
	}

	public int readShort()
	{
		offset += 2;
		int val = ((array[offset - 2] & 0xFF) << 8) + (array[offset - 1] & 0xFF);
		if (val > 32767)
		{
			val -= 65536;
		}
		return val;
	}

	public int read24BitInt()
	{
		offset += 3;
		return ((array[offset - 3] & 0xFF) << 16) + ((array[offset - 2] & 0xFF) << 8) + (array[offset - 1] & 0xFF);
	}

	public int readInt()
	{
		offset += 4;
		return ((array[offset - 4] & 0xFF) << 24) + ((array[offset - 3] & 0xFF) << 16) + ((array[offset - 2] & 0xFF) << 8) + (array[offset - 1] & 0xFF);
	}

	public String readString()
	{
		int start = offset;
		while (offset < array.length && array[offset] != 10 && array[offset] != 0)
		{
			offset++;
		}
		String s = new String(array, start, offset - start, StandardCharsets.UTF_8);
		if (offset < array.length)
		{
			offset++; // Skip terminator
		}
		return s;
	}

	public int readSmart()
	{
		int value = array[offset] & 0xFF;
		if (value < 128)
		{
			return readUnsignedByte();
		}
		return readUnsignedShort() - 32768;
	}

	public int readSignedSmart()
	{
		int value = array[offset] & 0xFF;
		if (value < 128)
		{
			return readUnsignedByte() - 64;
		}
		return readUnsignedShort() - 49152;
	}

	public void readBytes(byte[] buffer, int off, int len)
	{
		System.arraycopy(array, offset, buffer, off, len);
		offset += len;
	}
}
