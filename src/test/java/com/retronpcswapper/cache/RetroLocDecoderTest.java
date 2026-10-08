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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;
import java.io.File;
import java.util.Map;
import org.junit.Test;

public class RetroLocDecoderTest
{
	private static final File CACHE_DIR = new File("retrocache/2005cache");

	/**
	 * Every definition must end exactly where loc.idx says it does, so a dropped one means an
	 * opcode was misread - and the count is what notices.
	 */
	@Test
	public void testEvery2005ObjectDecodes() throws Exception
	{
		assumeTrue("2005 cache not present at " + CACHE_DIR, CACHE_DIR.exists());

		Map<Integer, RetroLocDefinition> defs = RetroLocDecoder.decodeDefinitions(CACHE_DIR);
		assertEquals(5490, defs.size());
	}

	@Test
	public void testTheWell() throws Exception
	{
		assumeTrue("2005 cache not present at " + CACHE_DIR, CACHE_DIR.exists());

		RetroLocDefinition well = RetroLocDecoder.decodeDefinitions(CACHE_DIR).get(884);
		assertNotNull(well);
		assertEquals("Well", well.getName());
		assertArrayEquals(new int[]{1410}, well.getModels());
		assertEquals(2, well.getSizeX());
		assertEquals(2, well.getSizeY());
		assertEquals(0, well.getAmbient());
		assertEquals(0, well.getContrast());
		assertEquals(128, well.getScaleX());
	}

	/** The wall chart's own adjustment, which the swapper lights its pentagram with. */
	@Test
	public void testTheWallChartIsLitBrighter() throws Exception
	{
		assumeTrue("2005 cache not present at " + CACHE_DIR, CACHE_DIR.exists());

		RetroLocDefinition chart = RetroLocDecoder.decodeDefinitions(CACHE_DIR).get(908);
		assertNotNull(chart);
		assertArrayEquals(new int[]{2086}, chart.getModels());
		assertEquals(50, chart.getAmbient());
	}
}
