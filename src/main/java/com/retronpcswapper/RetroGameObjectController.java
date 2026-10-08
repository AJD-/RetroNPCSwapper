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

import net.runelite.api.GameObject;
import net.runelite.api.Model;
import net.runelite.api.RuneLiteObjectController;
import net.runelite.api.coords.LocalPoint;

/**
 * Stands in for one scenery object standing on its tiles - a well, a tree - the way
 * {@link RetroDecorController} does for a wall decoration, and carrying a client model for the
 * same reason.
 */
class RetroGameObjectController extends RuneLiteObjectController
{
	/** Jagex angle units in a quarter turn, and in the eighth turn a diagonal adds. */
	private static final int QUARTER_TURN = 512;
	private static final int EIGHTH_TURN = 256;

	/** Half a tile, less the margin the controller's default radius of 60 leaves a one-tile model. */
	private static final int RADIUS_PER_TILE = 64;
	private static final int RADIUS_MARGIN = 4;

	private final Model carrier;

	RetroGameObjectController(GameObject object, Model carrier)
	{
		this.carrier = carrier;

		// The object's own coordinates are the center of its footprint, where the client draws it
		setLocation(new LocalPoint(object.getX(), object.getY(), object.getWorldView()), object.getPlane());
		setZ(object.getZ());
		setOrientation(orientation(object.getConfig()));
		setRadius(radius(object.sizeX(), object.sizeY()));
	}

	@Override
	public Model getModel()
	{
		return carrier;
	}

	/**
	 * The turn the client gives the model: its quarter turns, and an extra eighth for a diagonal
	 * placement, which the client draws as an ordinary one turned onto the diagonal.
	 */
	static int orientation(int config)
	{
		int turn = RetroDecorController.orientation(config) * QUARTER_TURN;
		if (RetroDecorController.type(config) == RetroScenery.TYPE_DIAGONAL_CENTREPIECE)
		{
			turn += EIGHTH_TURN;
		}
		return turn;
	}

	/**
	 * How far the stand-in reaches from its center, which decides the tiles drawn ahead of it:
	 * just inside its footprint, as the default does for a single tile, so it sorts over the
	 * ground it stands on and no further.
	 */
	static int radius(int sizeX, int sizeY)
	{
		return Math.max(sizeX, sizeY) * RADIUS_PER_TILE - RADIUS_MARGIN;
	}
}
