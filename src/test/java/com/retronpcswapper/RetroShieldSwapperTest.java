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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.runelite.api.PlayerComposition;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class RetroShieldSwapperTest
{
	@Test
	public void testBothShieldsAreRecognized()
	{
		assertTrue(RetroShieldSwapper.isShieldItem(ItemID.ANTIDRAGONBREATHSHIELD));
		assertTrue(RetroShieldSwapper.isShieldItem(ItemID.NZONE_ANTIDRAGONSHIELD));
	}

	/**
	 * The noted shield and its placeholder take the shield's icon from the base item, so changing
	 * their own inventory model would replace the note paper with a shield.
	 */
	@Test
	public void testNotedShieldIsLeftAlone()
	{
		assertFalse(RetroShieldSwapper.isShieldItem(ItemID.Cert.ANTIDRAGONBREATHSHIELD));
	}

	@Test
	public void testOtherShieldsAreLeftAlone()
	{
		assertFalse(RetroShieldSwapper.isShieldItem(ItemID.DRAGONFIRE_SHIELD));
		assertFalse(RetroShieldSwapper.isShieldItem(ItemID.RUNE_KITESHIELD));
	}

	@Test
	public void testEquipmentIdsCarryTheItemOffset()
	{
		assertTrue(RetroShieldSwapper.isShieldEquipment(
			ItemID.ANTIDRAGONBREATHSHIELD + PlayerComposition.ITEM_OFFSET));
		assertTrue(RetroShieldSwapper.isShieldEquipment(
			ItemID.NZONE_ANTIDRAGONSHIELD + PlayerComposition.ITEM_OFFSET));

		// The bare item id is not an equipment id: below the offset it would be a kit or nothing
		assertFalse(RetroShieldSwapper.isShieldEquipment(ItemID.ANTIDRAGONBREATHSHIELD));
	}

	@Test
	public void testEmptySlotsAndKitsAreNotShields()
	{
		assertFalse(RetroShieldSwapper.isShieldEquipment(0));
		// Kit ids sit between the kit offset and the item offset
		assertFalse(RetroShieldSwapper.isShieldEquipment(PlayerComposition.KIT_OFFSET + 18));
		// Under the old 512 offset this was a shield; under the real one it is a kit
		assertFalse(RetroShieldSwapper.isShieldEquipment(ItemID.ANTIDRAGONBREATHSHIELD + 512));
		assertFalse(RetroShieldSwapper.isShieldEquipment(-1));
	}
}
