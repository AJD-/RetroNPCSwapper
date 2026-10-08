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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Arrays;
import java.util.HashSet;
import net.runelite.api.Constants;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

public class RetroScenerySwapperTest
{
	/** Config bits as the client packs them: placement type in the low five, orientation at six. */
	private static int config(int type, int orientation)
	{
		return orientation << 6 | type;
	}

	private static DecorativeObject decoration(int id, int config)
	{
		DecorativeObject decoration = mock(DecorativeObject.class);
		when(decoration.getId()).thenReturn(id);
		when(decoration.getConfig()).thenReturn(config);
		return decoration;
	}

	@Test
	public void testTheWallChartIsHiddenWhileActive()
	{
		assertTrue(RetroScenerySwapper.shouldHide(decoration(ObjectID.WITCHESWALLCHART, config(4, 1)), true));
		assertTrue(RetroScenerySwapper.shouldHide(decoration(ObjectID.WITCHESWALLCHART, config(5, 3)), true));
	}

	@Test
	public void testNothingIsHiddenWhileInactive()
	{
		assertFalse(RetroScenerySwapper.shouldHide(decoration(ObjectID.WITCHESWALLCHART, config(4, 0)), false));
	}

	@Test
	public void testOtherDecorationsAreLeftAlone()
	{
		assertFalse(RetroScenerySwapper.shouldHide(decoration(ObjectID.WITCHESWALLCHART + 1, config(4, 0)), true));
	}

	/** Every wall decoration placement is handled now, the diagonal ones included. */
	@Test
	public void testDiagonalPlacementsAreHidden()
	{
		assertTrue(RetroScenerySwapper.shouldHide(decoration(ObjectID.WITCHESWALLCHART, config(6, 0)), true));
		assertTrue(RetroScenerySwapper.shouldHide(decoration(ObjectID.WITCHESWALLCHART, config(7, 2)), true));
		assertTrue(RetroScenerySwapper.shouldHide(decoration(ObjectID.WITCHESWALLCHART, config(8, 0)), true));
	}

	/**
	 * Anything that is not a wall decoration placement gets no stand-in, so it must not be hidden
	 * either - or it would simply vanish.
	 */
	@Test
	public void testNonWallDecorationPlacementsAreLeftAlone()
	{
		assertFalse(RetroScenerySwapper.shouldHide(decoration(ObjectID.WITCHESWALLCHART, config(3, 0)), true));
		assertFalse(RetroScenerySwapper.shouldHide(decoration(ObjectID.WITCHESWALLCHART, config(10, 0)), true));
	}

	/**
	 * The Wizards' Guild charts are type 7 - on a diagonal wall, with no offset - which the client
	 * draws on the far face, half a turn round from the orientation it was given.
	 */
	@Test
	public void testDiagonalQuarterTurns()
	{
		assertEquals(1, RetroDecorController.quarterTurns(4, 1, false));
		assertEquals(3, RetroDecorController.quarterTurns(6, 3, false));
		assertEquals(0, RetroDecorController.quarterTurns(7, 2, false));
		assertEquals(3, RetroDecorController.quarterTurns(7, 1, false));

		// Drawn on both faces: once as given, once half a turn round
		assertEquals(1, RetroDecorController.quarterTurns(8, 1, false));
		assertEquals(3, RetroDecorController.quarterTurns(8, 1, true));
		assertTrue(RetroDecorController.isDrawnTwice(8));
		assertFalse(RetroDecorController.isDrawnTwice(7));
	}

	/**
	 * Quarter turns of an offset must match the renderer's own rotation,
	 * {@code x' = z sin + x cos, z' = z cos - x sin}, or the diagonal shift lands off the wall.
	 */
	@Test
	public void testRotateMatchesTheRenderer()
	{
		for (int quarters = 0; quarters < 4; quarters++)
		{
			double angle = quarters * Math.PI / 2;
			long sin = Math.round(Math.sin(angle));
			long cos = Math.round(Math.cos(angle));
			int[] turned = RetroDecorController.rotate(45, -45, quarters);
			assertEquals(-45 * sin + 45 * cos, turned[0]);
			assertEquals(-45 * cos - 45 * sin, turned[1]);
		}
	}

	/**
	 * The callback is asked about temporary entities too, the stand-ins among them. A stand-in has
	 * no id of its own to tell it apart, so the type check is the only thing keeping it visible.
	 */
	@Test
	public void testNonDecorationsAreNeverHidden()
	{
		GameObject gameObject = mock(GameObject.class);
		when(gameObject.getId()).thenReturn(ObjectID.WITCHESWALLCHART);

		assertFalse(RetroScenerySwapper.shouldHide(gameObject, true));
	}

	private static RetroDrawCallbacks.SceneLevels levels(int level, Integer... hiddenRoofs)
	{
		return new RetroDrawCallbacks.SceneLevels(0, level, 3, new HashSet<>(Arrays.asList(hiddenRoofs)));
	}

	/**
	 * The Wizards' Guild: charts on the first and second floors, a roof over them, the player on
	 * the ground floor with that roof taken off. The symbols are hidden there, so the stand-ins
	 * must be too.
	 */
	@Test
	public void testAStandInUnderAHiddenRoofAboveThePlayerIsHidden()
	{
		assertTrue(RetroScenerySwapper.isHidden(1, 7, levels(0, 7)));
		assertTrue(RetroScenerySwapper.isHidden(2, 7, levels(0, 7)));
	}

	@Test
	public void testAStandInOnThePlayersOwnLevelOrBelowIsDrawn()
	{
		assertFalse(RetroScenerySwapper.isHidden(1, 7, levels(1, 7)));
		assertFalse(RetroScenerySwapper.isHidden(0, 7, levels(1, 7)));
	}

	/** Outside, nothing is taken off, so the upper floors are drawn - and their stand-ins with them. */
	@Test
	public void testAStandInUnderARoofThatIsStillOnIsDrawn()
	{
		assertFalse(RetroScenerySwapper.isHidden(1, 7, levels(0)));
		assertFalse(RetroScenerySwapper.isHidden(1, 7, levels(0, 8)));
		assertFalse(RetroScenerySwapper.isHidden(1, 0, levels(0, 7)));
	}

	@Test
	public void testAStandInOutsideTheDrawnLevelsIsHidden()
	{
		RetroDrawCallbacks.SceneLevels upToFirst = new RetroDrawCallbacks.SceneLevels(0, 0, 1, new HashSet<>());
		assertTrue(RetroScenerySwapper.isHidden(2, 0, upToFirst));
	}

	@Test
	public void testRoofIdComesFromTheLevelBelow()
	{
		byte[][][] settings = new byte[4][2][2];
		int[][][] roofs = new int[4][2][2];
		roofs[0][1][1] = 7;
		roofs[1][1][1] = 9;

		assertEquals(0, RetroScenerySwapper.roofId(settings, roofs, 0, 1, 1));
		assertEquals(7, RetroScenerySwapper.roofId(settings, roofs, 1, 1, 1));

		// A bridge counts as a level higher
		settings[1][1][1] = Constants.TILE_FLAG_BRIDGE;
		assertEquals(9, RetroScenerySwapper.roofId(settings, roofs, 1, 1, 1));

		// And a tile meant to be seen from below is under no roof at all
		settings[1][1][1] = Constants.TILE_FLAG_VIS_BELOW;
		assertEquals(0, RetroScenerySwapper.roofId(settings, roofs, 1, 1, 1));
	}

	@Test
	public void testConfigBitsUnpack()
	{
		int config = config(5, 3) | 1 << 5;     // bit 5 sits between the two fields and belongs to neither

		assertEquals(5, RetroDecorController.type(config));
		assertEquals(3, RetroDecorController.orientation(config));
	}
}
