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
package com.retronpcswapper;

import com.google.inject.Guice;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RetroModelCacheTest
{
	private static final int NPC_ID = 3;
	private static final int MODEL_ID = 2951;

	private Client client;
	private RetroModelCache cache;
	private RetroNpcData data;

	@Before
	public void setUp()
	{
		client = mock(Client.class);
		ModelData part = mock(ModelData.class);
		when(part.light()).thenAnswer(invocation -> mock(Model.class));
		when(client.loadModelData(MODEL_ID)).thenReturn(part);
		cache = Guice.createInjector(binder -> binder.bind(Client.class).toInstance(client))
			.getInstance(RetroModelCache.class);
		data = RetroNpcData.builder()
			.category(RetroNpcCategory.GOBLINS)
			.retroModelIds(new int[]{MODEL_ID})
			.build();
	}

	@Test
	public void testChangingTheGoblinEraRebuildsBuiltModels()
	{
		assertTrue(cache.ensureBuilt(NPC_ID, data));
		assertTrue(cache.ensureBuilt(NPC_ID, data));
		verify(client, times(1)).loadModelData(MODEL_ID);

		cache.setGoblinEra(GoblinEra.RETRO_2006);
		assertTrue(cache.ensureBuilt(NPC_ID, data));
		verify(client, times(2)).loadModelData(MODEL_ID);
	}

	@Test
	public void testSettingTheSameGoblinEraKeepsBuiltModels()
	{
		cache.setGoblinEra(GoblinEra.RETRO_2005);
		assertTrue(cache.ensureBuilt(NPC_ID, data));

		cache.setGoblinEra(GoblinEra.RETRO_2005);
		assertTrue(cache.ensureBuilt(NPC_ID, data));
		verify(client, times(1)).loadModelData(MODEL_ID);
	}
}
