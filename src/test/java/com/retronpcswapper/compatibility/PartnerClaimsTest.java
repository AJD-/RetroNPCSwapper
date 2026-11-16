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
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class PartnerClaimsTest
{
	private static final String CUSTOM = ModelSwapProtocol.SOURCE_CUSTOM_NPC_MODELS;
	private static final String SERVANTS = ModelSwapProtocol.SOURCE_CUSTOM_SERVANTS;

	private final PartnerClaims claims = new PartnerClaims();

	private static Set<Integer> ids(Integer... ids)
	{
		return new HashSet<>(Arrays.asList(ids));
	}

	@Test
	public void testNothingIsClaimedAtFirst()
	{
		assertEquals(Collections.emptySet(), claims.claimed());
		assertFalse(claims.isClaimed(1));
	}

	@Test
	public void testTheClaimsOfTwoSourcesAreJoined()
	{
		assertTrue(claims.put(CUSTOM, ids(1, 2)));
		assertTrue(claims.put(SERVANTS, ids(3)));

		assertEquals(ids(1, 2, 3), claims.claimed());
		assertTrue(claims.isClaimed(1));
		assertTrue(claims.isClaimed(3));
		assertFalse(claims.isClaimed(4));
	}

	@Test
	public void testIsClaimedByTellsTheSourcesApart()
	{
		claims.put(CUSTOM, ids(1));
		claims.put(SERVANTS, ids(2));

		assertTrue(claims.isClaimedBy(CUSTOM, 1));
		assertFalse(claims.isClaimedBy(SERVANTS, 1));
		assertTrue(claims.isClaimedBy(SERVANTS, 2));
		assertFalse(claims.isClaimedBy(CUSTOM, 2));
		assertFalse(claims.isClaimedBy(CUSTOM, 3));
		assertFalse(claims.isClaimedBy(null, 1));

		claims.remove(SERVANTS);
		assertFalse(claims.isClaimedBy(SERVANTS, 2));
	}

	@Test
	public void testASourceReplacesOnlyItsOwnClaims()
	{
		claims.put(CUSTOM, ids(1, 2));
		claims.put(SERVANTS, ids(3));

		assertTrue(claims.put(SERVANTS, ids(4)));
		assertEquals(ids(1, 2, 4), claims.claimed());

		assertTrue(claims.put(CUSTOM, ids(2)));
		assertEquals(ids(2, 4), claims.claimed());
	}

	@Test
	public void testRepeatingTheSameClaimsChangesNothing()
	{
		claims.put(CUSTOM, ids(1, 2));
		assertFalse(claims.put(CUSTOM, ids(2, 1)));
	}

	@Test
	public void testAnIdBothClaimStaysClaimedUntilBothLetGo()
	{
		claims.put(CUSTOM, ids(5));
		claims.put(SERVANTS, ids(5));

		// The union did not change, so there is nothing to recheck
		assertFalse(claims.put(CUSTOM, Collections.emptySet()));
		assertEquals(ids(5), claims.claimed());

		assertTrue(claims.remove(SERVANTS));
		assertEquals(Collections.emptySet(), claims.claimed());
	}

	@Test
	public void testRemovingASourceDropsOnlyItsClaims()
	{
		claims.put(CUSTOM, ids(1));
		claims.put(SERVANTS, ids(2));

		assertTrue(claims.remove(SERVANTS));
		assertEquals(ids(1), claims.claimed());

		// Already gone
		assertFalse(claims.remove(SERVANTS));
		assertFalse(claims.remove("some-plugin"));
	}

	@Test
	public void testAnEmptySetWithdrawsASourcesClaims()
	{
		assertFalse(claims.put(SERVANTS, Collections.emptySet()));

		claims.put(SERVANTS, ids(1));
		assertTrue(claims.put(SERVANTS, Collections.emptySet()));
		assertEquals(Collections.emptySet(), claims.claimed());
		assertFalse(claims.remove(SERVANTS));
	}

	@Test
	public void testClearForgetsEverySource()
	{
		claims.put(CUSTOM, ids(1));
		claims.put(SERVANTS, ids(2));

		claims.clear();
		assertEquals(Collections.emptySet(), claims.claimed());

		// Nothing left to remove, and a fresh claim counts as a change again
		assertFalse(claims.remove(CUSTOM));
		assertTrue(claims.put(CUSTOM, ids(1)));
	}

	@Test
	public void testTheUnionCannotBeModifiedOrChangedByTheCallersSet()
	{
		Set<Integer> mine = ids(1);
		claims.put(CUSTOM, mine);
		mine.add(2);
		assertEquals(ids(1), claims.claimed());

		try
		{
			claims.claimed().add(3);
		}
		catch (UnsupportedOperationException expected)
		{
			return;
		}
		throw new AssertionError("the union was modifiable");
	}
}
