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

import com.retronpcswapper.RetroScenerySwapper;
import java.awt.Color;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.RuneLiteObjectController;
import net.runelite.api.TileObject;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/**
 * Outlines the 2005 scenery drawn in a live object's place.
 *
 * <p>{@link ModelOutlineRenderer}'s {@code TileObject} overload reads the object's own renderable,
 * which for restored scenery is the live model the renderer has been told to leave out. The object
 * is drawn by a stand-in instead, so the outline is drawn around that: the stand-in's position and
 * turn, carrying the model it shows, through the {@link RuneLiteObject} overload - the same door
 * {@link RetroNpcOutliner} uses.
 *
 * <p>The scratch object is never activated. It is a model carrier handed straight to the outline
 * renderer, never registered with the client, so it adds nothing to the scene.
 */
@Singleton
public class RetroSceneryOutliner
{
	@Inject
	private Client client;

	@Inject
	private ModelOutlineRenderer modelOutlineRenderer;

	@Inject
	private RetroScenerySwapper scenerySwapper;

	// Built lazily and reused - creating one per frame would churn for no reason
	private RuneLiteObject carrier;

	/**
	 * Outlines the 2005 model an object is drawn as.
	 *
	 * @return false when the object is drawn as it is, leaving the caller to outline it the
	 * ordinary way
	 */
	boolean drawOutline(TileObject object, int outlineWidth, Color color, int feather)
	{
		List<RuneLiteObjectController> standIns = scenerySwapper.getStandIns(object);
		if (standIns.isEmpty())
		{
			return false;
		}

		if (carrier == null)
		{
			carrier = client.createRuneLiteObject();
		}

		// A wall chart on both faces of a wall has two stand-ins, and the outline takes in both, as
		// the object's own outline takes in both of its models
		for (RuneLiteObjectController standIn : standIns)
		{
			Model model = scenerySwapper.getDrawnModel(standIn);
			if (model == null)
			{
				continue;
			}

			carrier.setModel(model);
			carrier.setX(standIn.getX());
			carrier.setY(standIn.getY());
			carrier.setZ(standIn.getZ());
			carrier.setWorldView(standIn.getWorldView());
			carrier.setLevel(standIn.getLevel());
			carrier.setOrientation(standIn.getOrientation());
			modelOutlineRenderer.drawOutline(carrier, outlineWidth, color, feather);
		}
		return true;
	}

	/**
	 * Drops the scratch object, releasing its reference to a model.
	 */
	public void clear()
	{
		carrier = null;
	}
}
