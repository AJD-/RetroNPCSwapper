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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.util.function.Function;
import net.runelite.api.Model;
import net.runelite.api.NPC;
import org.junit.Before;
import org.junit.Test;

/**
 * What each partner said about Interact Highlight's NPC outlines, and whose poser draws an NPC.
 */
public class PartnerOutlinesTest
{
	private static final String SERVANTS = ModelSwapProtocol.SOURCE_CUSTOM_SERVANTS;
	private static final String CNM = ModelSwapProtocol.SOURCE_CUSTOM_NPC_MODELS;

	private PartnerOutlines outlines;
	private Function<NPC, Model> servantsPoser;
	private Function<NPC, Model> cnmPoser;
	private NPC npc;
	private Model servantsModel;
	private Model cnmModel;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp()
	{
		outlines = new PartnerOutlines();
		servantsPoser = mock(Function.class);
		cnmPoser = mock(Function.class);
		npc = mock(NPC.class);
		servantsModel = mock(Model.class);
		cnmModel = mock(Model.class);
	}

	private static ModelSwapProtocol.Outlines state(boolean owning, Function<NPC, Model> poser)
	{
		return new ModelSwapProtocol.Outlines(owning, poser);
	}

	@Test
	public void testNobodyOwnsAtFirst()
	{
		assertFalse(outlines.isAnyOwning());
		assertFalse(outlines.isOwning(SERVANTS));
		assertFalse(outlines.isOwning(CNM));
		assertNull(outlines.pose(npc));
	}

	@Test
	public void testAnyOwningWhenCustomServantsOwns()
	{
		outlines.accept(SERVANTS, state(true, null));

		assertTrue(outlines.isAnyOwning());
		assertTrue(outlines.isOwning(SERVANTS));
		assertFalse(outlines.isOwning(CNM));
	}

	@Test
	public void testAnyOwningWhenCustomNpcModelsOwns()
	{
		outlines.accept(CNM, state(true, null));

		assertTrue(outlines.isAnyOwning());
		assertTrue(outlines.isOwning(CNM));
		assertFalse(outlines.isOwning(SERVANTS));
	}

	@Test
	public void testNotAnyOwningWhenNeitherOwns()
	{
		outlines.accept(SERVANTS, state(false, servantsPoser));
		outlines.accept(CNM, state(false, cnmPoser));

		assertFalse(outlines.isAnyOwning());
	}

	@Test
	public void testForgettingCustomServantsLeavesCustomNpcModelsOwning()
	{
		outlines.accept(SERVANTS, state(true, servantsPoser));
		outlines.accept(CNM, state(true, cnmPoser));
		when(cnmPoser.apply(npc)).thenReturn(cnmModel);

		outlines.forget(SERVANTS);

		assertFalse(outlines.isOwning(SERVANTS));
		assertTrue(outlines.isOwning(CNM));
		assertTrue(outlines.isAnyOwning());
		assertNull(outlines.pose(SERVANTS, npc));
		assertSame(cnmModel, outlines.pose(npc));
		verifyNoInteractions(servantsPoser);
	}

	@Test
	public void testAPoserWithoutOwningNeverFlipsOwning()
	{
		outlines.accept(SERVANTS, state(false, servantsPoser));
		when(servantsPoser.apply(npc)).thenReturn(servantsModel);

		assertFalse(outlines.isOwning(SERVANTS));
		assertFalse(outlines.isAnyOwning());
		assertSame(servantsModel, outlines.pose(SERVANTS, npc));
	}

	@Test
	public void testALaterMessageReplacesTheEarlierOne()
	{
		outlines.accept(SERVANTS, state(true, servantsPoser));
		outlines.accept(SERVANTS, state(false, servantsPoser));

		assertFalse(outlines.isOwning(SERVANTS));
	}

	@Test
	public void testANullPoserWithdrawsTheEarlierOne()
	{
		outlines.accept(SERVANTS, state(true, servantsPoser));
		when(servantsPoser.apply(npc)).thenReturn(servantsModel);
		assertSame(servantsModel, outlines.pose(SERVANTS, npc));

		outlines.accept(SERVANTS, state(true, null));

		assertNull(outlines.pose(SERVANTS, npc));
		assertNull(outlines.pose(npc));
		assertTrue(outlines.isOwning(SERVANTS));
	}

	@Test
	public void testCustomServantsModelWinsWhenBothPose()
	{
		outlines.accept(SERVANTS, state(false, servantsPoser));
		outlines.accept(CNM, state(true, cnmPoser));
		when(servantsPoser.apply(npc)).thenReturn(servantsModel);
		when(cnmPoser.apply(npc)).thenReturn(cnmModel);

		assertSame(servantsModel, outlines.pose(npc));
		verify(cnmPoser, never()).apply(npc);
	}

	@Test
	public void testCustomNpcModelsModelIsUsedWhenCustomServantsDrawsNone()
	{
		outlines.accept(SERVANTS, state(false, servantsPoser));
		outlines.accept(CNM, state(false, cnmPoser));
		when(servantsPoser.apply(npc)).thenReturn(null);
		when(cnmPoser.apply(npc)).thenReturn(cnmModel);

		assertSame(cnmModel, outlines.pose(npc));
		verify(servantsPoser).apply(npc);
	}

	@Test
	public void testCustomNpcModelsModelIsUsedWhenCustomServantsPoserThrows()
	{
		outlines.accept(SERVANTS, state(false, servantsPoser));
		outlines.accept(CNM, state(false, cnmPoser));
		when(servantsPoser.apply(npc)).thenThrow(new IllegalStateException("partner bug"));
		when(cnmPoser.apply(npc)).thenReturn(cnmModel);

		assertSame(cnmModel, outlines.pose(npc));
		verify(cnmPoser).apply(npc);
	}

	@Test
	public void testAThrowingPoserPosesNothing()
	{
		outlines.accept(SERVANTS, state(false, servantsPoser));
		when(servantsPoser.apply(npc)).thenThrow(new IllegalStateException("partner bug"));

		assertNull(outlines.pose(SERVANTS, npc));
		assertNull(outlines.pose(npc));
	}

	@Test
	public void testPosingOneSourceAsksOnlyThatSource()
	{
		outlines.accept(SERVANTS, state(false, servantsPoser));
		outlines.accept(CNM, state(false, cnmPoser));
		when(cnmPoser.apply(npc)).thenReturn(cnmModel);

		assertSame(cnmModel, outlines.pose(CNM, npc));
		verify(cnmPoser).apply(npc);
		verifyNoInteractions(servantsPoser);
	}

	@Test
	public void testOurOwnMessagesAreIgnored()
	{
		String self = ModelSwapProtocol.SOURCE_RETRO_NPC_SWAPPER;
		outlines.accept(self, state(true, servantsPoser));

		assertFalse(outlines.isOwning(self));
		assertFalse(outlines.isAnyOwning());
		assertNull(outlines.pose(self, npc));
		assertNull(outlines.pose(npc));
		verifyNoInteractions(servantsPoser);
	}

	@Test
	public void testUnknownSourcesAreIgnored()
	{
		outlines.accept("someone-else", state(true, servantsPoser));
		outlines.accept(null, state(true, servantsPoser));
		outlines.accept(SERVANTS, null);
		outlines.forget(null);

		assertFalse(outlines.isOwning("someone-else"));
		assertFalse(outlines.isOwning(null));
		assertFalse(outlines.isAnyOwning());
		assertNull(outlines.pose("someone-else", npc));
		assertNull(outlines.pose(null, npc));
		assertNull(outlines.pose(npc));
		verifyNoInteractions(servantsPoser);
	}

	@Test
	public void testHandshakeIsNotDoneUntilSet()
	{
		assertFalse(outlines.isHandshakeDone());

		outlines.setHandshakeDone();

		assertTrue(outlines.isHandshakeDone());
	}

	@Test
	public void testResetForgetsEverything()
	{
		outlines.accept(SERVANTS, state(true, servantsPoser));
		outlines.accept(CNM, state(true, cnmPoser));
		outlines.setHandshakeDone();

		outlines.reset();

		assertFalse(outlines.isAnyOwning());
		assertFalse(outlines.isOwning(SERVANTS));
		assertFalse(outlines.isOwning(CNM));
		assertNull(outlines.pose(npc));
		assertFalse(outlines.isHandshakeDone());
		verifyNoInteractions(servantsPoser, cnmPoser);
	}
}
