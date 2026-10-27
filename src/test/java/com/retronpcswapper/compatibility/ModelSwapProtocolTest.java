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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.runelite.api.Model;
import net.runelite.api.NPC;
import net.runelite.client.events.PluginMessage;
import org.junit.Test;

public class ModelSwapProtocolTest
{
	private static final String CUSTOM = ModelSwapProtocol.SOURCE_CUSTOM_NPC_MODELS;
	private static final String SERVANTS = ModelSwapProtocol.SOURCE_CUSTOM_SERVANTS;

	@Test
	public void testClaimsRoundTrip()
	{
		Set<Integer> ids = new HashSet<>(Arrays.asList(5779, 5780));
		assertEquals(ids, ModelSwapProtocol.readClaims(ModelSwapProtocol.claimsMessage(ids), CUSTOM));
		assertEquals(Collections.emptySet(),
			ModelSwapProtocol.readClaims(ModelSwapProtocol.claimsMessage(Collections.emptySet()), CUSTOM));
	}

	@Test
	public void testClaimsAreAcceptedFromBothPartners()
	{
		Set<Integer> ids = new HashSet<>(Arrays.asList(5779, 5780));

		PluginMessage custom = ModelSwapProtocol.claimsMessage(CUSTOM, ids);
		assertEquals(CUSTOM, ModelSwapProtocol.claimsSource(custom));
		assertEquals(ids, ModelSwapProtocol.readClaims(custom, CUSTOM));

		PluginMessage servants = ModelSwapProtocol.claimsMessage(SERVANTS, ids);
		assertEquals(SERVANTS, ModelSwapProtocol.claimsSource(servants));
		assertEquals(ids, ModelSwapProtocol.readClaims(servants, SERVANTS));
		assertEquals(Collections.emptySet(),
			ModelSwapProtocol.readClaims(ModelSwapProtocol.claimsMessage(SERVANTS, Collections.emptySet()), SERVANTS));
	}

	@Test
	public void testTheOldClaimsMessageIsCustomNpcModels()
	{
		assertEquals(CUSTOM, ModelSwapProtocol.claimsSource(ModelSwapProtocol.claimsMessage(Collections.singleton(5779))));
	}

	@Test
	public void testClaimsAreReadOnlyAsTheirOwnSource()
	{
		Set<Integer> ids = Collections.singleton(5779);
		assertNull(ModelSwapProtocol.readClaims(ModelSwapProtocol.claimsMessage(SERVANTS, ids), CUSTOM));
		assertNull(ModelSwapProtocol.readClaims(ModelSwapProtocol.claimsMessage(CUSTOM, ids), SERVANTS));
	}

	@Test
	public void testClaimsFromThisPluginOrAnUnknownSourceAreRejected()
	{
		Set<Integer> ids = Collections.singleton(5779);
		for (String source : new String[]{ModelSwapProtocol.SOURCE_RETRO_NPC_SWAPPER, "some-plugin"})
		{
			PluginMessage message = ModelSwapProtocol.claimsMessage(source, ids);
			assertNull(source, ModelSwapProtocol.claimsSource(message));
			assertNull(source, ModelSwapProtocol.readClaims(message, source));
		}

		Map<String, Object> data = new HashMap<>(ModelSwapProtocol.claimsMessage(ids).getData());
		data.remove(ModelSwapProtocol.KEY_SOURCE);
		PluginMessage sourceless = new PluginMessage(ModelSwapProtocol.NAMESPACE, ModelSwapProtocol.CLAIMS, data);
		assertNull(ModelSwapProtocol.claimsSource(sourceless));
		assertNull(ModelSwapProtocol.readClaims(sourceless, null));
	}

	@Test
	public void testClaimsOfAnotherVersionAreRejectedFromEitherPartner()
	{
		for (String source : new String[]{CUSTOM, SERVANTS})
		{
			Map<String, Object> data = new HashMap<>(ModelSwapProtocol.claimsMessage(source, Collections.singleton(5779)).getData());
			data.put(ModelSwapProtocol.KEY_VERSION, ModelSwapProtocol.VERSION + 1);
			PluginMessage message = new PluginMessage(ModelSwapProtocol.NAMESPACE, ModelSwapProtocol.CLAIMS, data);
			assertNull(source, ModelSwapProtocol.claimsSource(message));
			assertNull(source, ModelSwapProtocol.readClaims(message, source));
		}
	}

	@Test
	public void testAPartnersOtherMessagesHaveNoClaimsSource()
	{
		assertNull(ModelSwapProtocol.claimsSource(null));
		assertNull(ModelSwapProtocol.claimsSource(ModelSwapProtocol.synMessage(SERVANTS)));
		assertNull(ModelSwapProtocol.claimsSource(ModelSwapProtocol.synMessage(CUSTOM)));
		assertNull(ModelSwapProtocol.claimsSource(ModelSwapProtocol.outlinesMessage(CUSTOM, true, null)));
		assertNull(ModelSwapProtocol.claimsSource(new PluginMessage("some-plugin", ModelSwapProtocol.CLAIMS)));
	}

	@Test
	public void testClaimsAreACopyNobodyCanModify()
	{
		Set<Integer> ids = new HashSet<>(Collections.singleton(5779));
		PluginMessage message = ModelSwapProtocol.claimsMessage(ids);
		ids.add(1);

		Set<?> sent = (Set<?>) message.getData().get(ModelSwapProtocol.KEY_NPC_IDS);
		assertEquals(Collections.singleton(5779), sent);
		try
		{
			sent.clear();
		}
		catch (UnsupportedOperationException expected)
		{
			return;
		}
		throw new AssertionError("the claimed set was modifiable");
	}

	@Test
	public void testOtherMessagesAreNotClaims()
	{
		assertNull(ModelSwapProtocol.readClaims(null, CUSTOM));
		assertNull(ModelSwapProtocol.readClaims(new PluginMessage("some-plugin", ModelSwapProtocol.CLAIMS), CUSTOM));
		assertNull(ModelSwapProtocol.readClaims(ModelSwapProtocol.synMessage(ModelSwapProtocol.SOURCE_RETRO_NPC_SWAPPER), CUSTOM));
		assertNull(ModelSwapProtocol.readClaims(ModelSwapProtocol.synMessage(SERVANTS), SERVANTS));
	}

	@Test
	public void testAnotherVersionIsIgnored()
	{
		Map<String, Object> data = new HashMap<>(ModelSwapProtocol.claimsMessage(Collections.singleton(5779)).getData());
		data.put(ModelSwapProtocol.KEY_VERSION, ModelSwapProtocol.VERSION + 1);
		assertNull(ModelSwapProtocol.readClaims(new PluginMessage(ModelSwapProtocol.NAMESPACE, ModelSwapProtocol.CLAIMS, data), CUSTOM));
	}

	@Test
	public void testClaimsFromAnotherSourceAreIgnored()
	{
		Map<String, Object> data = new HashMap<>(ModelSwapProtocol.claimsMessage(Collections.singleton(5779)).getData());
		data.put(ModelSwapProtocol.KEY_SOURCE, ModelSwapProtocol.SOURCE_RETRO_NPC_SWAPPER);
		assertNull(ModelSwapProtocol.readClaims(new PluginMessage(ModelSwapProtocol.NAMESPACE, ModelSwapProtocol.CLAIMS, data), CUSTOM));
	}

	@Test
	public void testSyncReqIsRecognisedFromThePartnerOnly()
	{
		PluginMessage syn = ModelSwapProtocol.synMessage(ModelSwapProtocol.SOURCE_RETRO_NPC_SWAPPER);
		assertTrue(ModelSwapProtocol.isSyncReq(syn, ModelSwapProtocol.SOURCE_RETRO_NPC_SWAPPER));
		assertFalse(ModelSwapProtocol.isSyncReq(syn, ModelSwapProtocol.SOURCE_CUSTOM_NPC_MODELS));
		assertFalse(ModelSwapProtocol.isSyncReq(ModelSwapProtocol.claimsMessage(Collections.emptySet()),
			ModelSwapProtocol.SOURCE_CUSTOM_NPC_MODELS));
		assertFalse(ModelSwapProtocol.isSyncReq(new PluginMessage(ModelSwapProtocol.NAMESPACE, ModelSwapProtocol.SYN),
			ModelSwapProtocol.SOURCE_RETRO_NPC_SWAPPER));
	}

	@Test
	public void testOutlinesRoundTrip()
	{
		Function<NPC, Model> poser = npc -> null;
		ModelSwapProtocol.Outlines outlines = ModelSwapProtocol.readOutlines(
			ModelSwapProtocol.outlinesMessage(ModelSwapProtocol.SOURCE_RETRO_NPC_SWAPPER, true, poser),
			ModelSwapProtocol.SOURCE_RETRO_NPC_SWAPPER);
		assertNotNull(outlines);
		assertTrue(outlines.isOwning());
		assertSame(poser, outlines.getPoser());
	}

	@Test
	public void testOutlinesWithoutAPoserWithdrawIt()
	{
		PluginMessage message = ModelSwapProtocol.outlinesMessage(ModelSwapProtocol.SOURCE_CUSTOM_NPC_MODELS, false, null);
		assertFalse(message.getData().containsKey(ModelSwapProtocol.KEY_POSER));

		ModelSwapProtocol.Outlines outlines = ModelSwapProtocol.readOutlines(message, ModelSwapProtocol.SOURCE_CUSTOM_NPC_MODELS);
		assertNotNull(outlines);
		assertFalse(outlines.isOwning());
		assertNull(outlines.getPoser());
	}

	@Test
	public void testOwnOutlinesAreNotThePartners()
	{
		PluginMessage message = ModelSwapProtocol.outlinesMessage(ModelSwapProtocol.SOURCE_CUSTOM_NPC_MODELS, true, npc -> null);
		assertNull(ModelSwapProtocol.readOutlines(message, ModelSwapProtocol.SOURCE_RETRO_NPC_SWAPPER));
	}

	@Test
	public void testOtherMessagesAreNotOutlines()
	{
		assertNull(ModelSwapProtocol.readOutlines(null, ModelSwapProtocol.SOURCE_RETRO_NPC_SWAPPER));
		assertNull(ModelSwapProtocol.readOutlines(ModelSwapProtocol.synMessage(ModelSwapProtocol.SOURCE_RETRO_NPC_SWAPPER),
			ModelSwapProtocol.SOURCE_RETRO_NPC_SWAPPER));

		Map<String, Object> data = new HashMap<>(
			ModelSwapProtocol.outlinesMessage(ModelSwapProtocol.SOURCE_RETRO_NPC_SWAPPER, true, null).getData());
		data.remove(ModelSwapProtocol.KEY_OWNING);
		assertNull(ModelSwapProtocol.readOutlines(new PluginMessage(ModelSwapProtocol.NAMESPACE, ModelSwapProtocol.OUTLINES, data),
			ModelSwapProtocol.SOURCE_RETRO_NPC_SWAPPER));
	}

	@Test
	public void testOutlinesOfAnotherVersionAreIgnored()
	{
		Map<String, Object> data = new HashMap<>(
			ModelSwapProtocol.outlinesMessage(ModelSwapProtocol.SOURCE_RETRO_NPC_SWAPPER, true, null).getData());
		data.put(ModelSwapProtocol.KEY_VERSION, ModelSwapProtocol.VERSION + 1);
		assertNull(ModelSwapProtocol.readOutlines(new PluginMessage(ModelSwapProtocol.NAMESPACE, ModelSwapProtocol.OUTLINES, data),
			ModelSwapProtocol.SOURCE_RETRO_NPC_SWAPPER));
	}

	@Test
	public void testOptOutIsRecognisedFromThePartnerOnly()
	{
		PluginMessage optOut = ModelSwapProtocol.optOutMessage(ModelSwapProtocol.SOURCE_RETRO_NPC_SWAPPER);
		assertTrue(ModelSwapProtocol.isOptOut(optOut, ModelSwapProtocol.SOURCE_RETRO_NPC_SWAPPER));
		assertFalse(ModelSwapProtocol.isOptOut(optOut, ModelSwapProtocol.SOURCE_CUSTOM_NPC_MODELS));
		assertFalse(ModelSwapProtocol.isOptOut(ModelSwapProtocol.synMessage(ModelSwapProtocol.SOURCE_RETRO_NPC_SWAPPER),
			ModelSwapProtocol.SOURCE_RETRO_NPC_SWAPPER));
	}
}
