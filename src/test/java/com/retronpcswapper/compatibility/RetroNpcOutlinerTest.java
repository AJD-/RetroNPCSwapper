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
package com.retronpcswapper.compatibility;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.google.inject.Guice;
import com.retronpcswapper.RetroModelCache;
import java.awt.Color;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import org.junit.Before;
import org.junit.Test;

/**
 * An NPC a partner plugin claims is never outlined around the retro model, even while the memo
 * still holds one.
 */
public class RetroNpcOutlinerTest
{
	private RetroModelCache modelCache;
	private PartnerClaims claims;
	private RetroNpcOutliner outliner;
	private NPC npc;

	@Before
	public void setUp()
	{
		modelCache = mock(RetroModelCache.class);
		claims = new PartnerClaims();
		outliner = Guice.createInjector(binder ->
		{
			binder.bind(Client.class).toInstance(mock(Client.class));
			binder.bind(ModelOutlineRenderer.class).toInstance(mock(ModelOutlineRenderer.class));
			binder.bind(RetroModelCache.class).toInstance(modelCache);
			binder.bind(PartnerClaims.class).toInstance(claims);
			binder.bind(PartnerOutlines.class).toInstance(new PartnerOutlines());
		}).getInstance(RetroNpcOutliner.class);

		WorldView worldView = mock(WorldView.class);
		// Built ahead of the stubbing: the constructor reads the world view, itself a mock
		LocalPoint location = new LocalPoint(0, 0, worldView);
		npc = mock(NPC.class);
		when(npc.getId()).thenReturn(5);
		when(npc.getWorldView()).thenReturn(worldView);
		when(npc.getLocalLocation()).thenReturn(location);
	}

	@Test
	public void testAClaimedNpcIsNotPosedFromTheRetroMemo()
	{
		claims.put(ModelSwapProtocol.SOURCE_CUSTOM_SERVANTS, Set.of(5));

		assertFalse(outliner.drawOutline(npc, 1, Color.RED, 0));
		verify(modelCache, never()).pose(npc);
	}
}
