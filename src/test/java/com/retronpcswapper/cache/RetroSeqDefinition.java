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

import lombok.Data;

/** One 2005 sequence definition, as decoded from seq.dat. */
@Data
public class RetroSeqDefinition
{
	private int id;

	/**
	 * Flat 16-bit ids, unlike the modern format's {@code animationFile << 16 | frameIndex}. Resolve
	 * them through {@link RetroFrameIndex}, which is built by reading every index 2 directory.
	 */
	private int[] frameIds;

	/** Secondary frame per entry, for chathead animations. 0xFFFF on almost everything. */
	private int[] chatFrameIds;

	/** Per-frame duration in client ticks. */
	private int[] frameLengths;

	private int loopOffset = -1;

	/** Which transforms come from the second animation when a pose and an action are layered. */
	private int[] interleaveOrder;

	private boolean stretches;
	private int forcedPriority = 5;
	private int leftHandItem = -1;
	private int rightHandItem = -1;
	private int maxLoops = 99;
	private int precedenceAnimating = -1;
	private int priority = -1;
	private int replyMode = 2;

	public int getFrameCount()
	{
		return frameIds == null ? 0 : frameIds.length;
	}
}
