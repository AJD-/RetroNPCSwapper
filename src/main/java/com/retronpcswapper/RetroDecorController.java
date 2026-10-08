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

import net.runelite.api.DecorativeObject;
import net.runelite.api.Model;
import net.runelite.api.RuneLiteObjectController;
import net.runelite.api.coords.LocalPoint;

/**
 * Stands in for one wall decoration, so there is something in the scene for the draw callback to
 * swap retro geometry onto.
 * <p>
 * It hands the client a real client model - the carrier - rather than the retro one. The client
 * casts whatever this returns to its own model class, and a {@code RetroModel} there is caught and
 * silently draws nothing; the retro geometry is swapped in afterwards, at {@code drawTemp}, where
 * the renderer reads the interface alone. If that swap ever misses, the carrier is what shows.
 */
class RetroDecorController extends RuneLiteObjectController
{
	/** Jagex angle units in the eighth turn a diagonal adds. */
	private static final int EIGHTH_TURN = 256;

	/**
	 * Where the client moves a diagonal decoration once it has turned it the extra eighth: across
	 * to the diagonal through the tile's center. In model space, before the quarter turns.
	 */
	private static final int DIAGONAL_SHIFT_X = 45;
	private static final int DIAGONAL_SHIFT_Z = -45;

	// Placement types, as the client numbers them
	static final int TYPE_STRAIGHT = 4;
	static final int TYPE_STRAIGHT_OFFSET = 5;
	static final int TYPE_DIAGONAL_OFFSET = 6;
	static final int TYPE_DIAGONAL = 7;
	static final int TYPE_DIAGONAL_BOTH = 8;

	private final Model carrier;

	/**
	 * @param carrier the carrier for this stand-in's quarter turns, {@link #quarterTurns} - the
	 *                model comes already turned, as the client turns it before lighting it
	 * @param second  for a decoration drawn twice - one on each face of a diagonal wall - the second
	 *                of the two
	 */
	RetroDecorController(DecorativeObject decoration, Model carrier, boolean second)
	{
		this.carrier = carrier;

		int type = type(decoration.getConfig());
		int quarters = quarterTurns(type, orientation(decoration.getConfig()), second);
		boolean diagonal = type >= TYPE_DIAGONAL_OFFSET;

		// The tile, pushed off the wall by the offset a displaced decoration carries
		int x = decoration.getX() + (second ? decoration.getXOffset2() : decoration.getXOffset());
		int y = decoration.getY() + (second ? decoration.getYOffset2() : decoration.getYOffset());

		if (diagonal)
		{
			// The client turns a diagonal decoration an eighth, shifts it onto the diagonal, and only
			// then applies the quarter turns - so the shift turns with it. The renderer turns the
			// model about its origin, which leaves the shift to be applied here instead.
			int[] shift = rotate(DIAGONAL_SHIFT_X, DIAGONAL_SHIFT_Z, quarters);
			x += shift[0];
			y += shift[1];
		}

		setLocation(new LocalPoint(x, y, decoration.getWorldView()), decoration.getPlane());
		setZ(decoration.getZ());
		// The quarter turns are in the model already; only a diagonal's eighth is left to the renderer
		setOrientation(diagonal ? EIGHTH_TURN : 0);
	}

	@Override
	public Model getModel()
	{
		return carrier;
	}

	/** The decoration's placement type, from its config bits. */
	static int type(int config)
	{
		return config & 0x1F;
	}

	/** The decoration's quarter-turn orientation, 0-3, from its config bits. */
	static int orientation(int config)
	{
		return config >>> 6 & 3;
	}

	/** Whether a stand-in can be placed for a decoration of this type: every wall decoration. */
	static boolean isSupportedType(int type)
	{
		return type >= TYPE_STRAIGHT && type <= TYPE_DIAGONAL_BOTH;
	}

	/** Whether the client draws a decoration of this type twice, once on each face of the wall. */
	static boolean isDrawnTwice(int type)
	{
		return type == TYPE_DIAGONAL_BOTH;
	}

	/**
	 * How many quarter turns the client gives the model. A diagonal decoration with no offset is
	 * drawn on the far face of its wall, half a turn round, and one drawn on both faces is drawn
	 * once each way.
	 */
	static int quarterTurns(int type, int orientation, boolean second)
	{
		if (type == TYPE_DIAGONAL || (type == TYPE_DIAGONAL_BOTH && second))
		{
			return orientation + 2 & 3;
		}
		return orientation;
	}

	/**
	 * Turns a model-space offset by whole quarter turns, the way the renderer turns the model:
	 * {@code x' = z sin + x cos, z' = z cos - x sin}.
	 */
	static int[] rotate(int x, int z, int quarters)
	{
		switch (quarters & 3)
		{
			case 1:
				return new int[]{z, -x};
			case 2:
				return new int[]{-x, -z};
			case 3:
				return new int[]{-z, x};
			default:
				return new int[]{x, z};
		}
	}
}
