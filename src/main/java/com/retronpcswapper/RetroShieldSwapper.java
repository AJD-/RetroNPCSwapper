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

import com.retronpcswapper.inject.RetroAttachment;
import com.retronpcswapper.inject.RetroDecals;
import com.retronpcswapper.inject.RetroModel;
import java.util.Arrays;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.Renderable;
import net.runelite.api.TileItem;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.kit.KitType;

/**
 * Restores the anti-dragon shield as it looked from 2004 until its June 2007 graphical update: the
 * inventory icon (and with it the model on the ground), and the shield players wear.
 * <p>
 * Both 2005 models are still in the live cache, orphaned - no live item names either. Measured
 * against the 2005 cache rather than assumed:
 * <ul>
 *   <li>{@link #RETRO_INVENTORY_MODEL}: all 28 vertices identical to 2005, plus four faces. The live
 *       item's zoom, angles and offsets are already the 2005 ones, so the model is all that differs
 *       and the icon can be swapped through the item definition alone.</li>
 *   <li>{@link #RETRO_WORN_MODEL}: the same 44 vertices and 78 faces as 2005, but every vertex turned
 *       180 degrees about X. The live skeleton still holds shields in 2005 coordinates - the
 *       kiteshield's worn model 486 sits where it did in 2005 to within a few units - so the turn is
 *       undone before use.</li>
 * </ul>
 * <p>
 * There is no API to change an item's worn model, so the worn shield is swapped at draw time,
 * and placed by the client's own pose of the shield it replaces. The live worn shield,
 * {@link #LIVE_WORN_MODEL}, is bound wholly to the hand, so however the player is animated it moves
 * by a single affine map. {@link RetroAttachment} finds it in the posed player model and recovers
 * that map; the live shield's faces are then hidden and the 2005 one drawn by the same map. Posing
 * the 2005 shield with the client's animation system instead puts it in the wrong place: the pivots
 * an animation turns about are worked out from the whole player model, and a shield posed by itself
 * turns about its own middle.
 * <p>
 * Nothing here changes the player's appearance, what the server knows or what any click lands
 * on - the clickbox is registered from the vanilla model before the draw callback runs.
 * <p>
 * Both 2005 models paint their detail on - the sword on the face of the shield - as faces at a
 * higher render priority, which a depth-buffered renderer ignores. {@link RetroDecals} brings that
 * detail back to the front wherever the model is drawn in the scene. The icon needs nothing: it is
 * drawn by the client's software renderer, which honors priority.
 * <p>
 * Everything here runs on the client thread, except {@link #substituteGroundItem}.
 */
@Slf4j
@Singleton
public class RetroShieldSwapper
{
	/** The 2005 inventory model. There is no gameval for model ids. */
	static final int RETRO_INVENTORY_MODEL = 2572;

	/** The 2005 worn model, male and female alike. */
	static final int RETRO_WORN_MODEL = 526;

	/** The live worn model - the one in the posed player that the 2005 shield takes the place of. */
	static final int LIVE_WORN_MODEL = 26424;

	/**
	 * Undoes the half turn about X the live copy of {@link #RETRO_WORN_MODEL} was given:
	 * {@code y = FLIP_Y - y', z = FLIP_Z - z'}. Measured from every one of its 44 vertices against
	 * the 2005 cache.
	 */
	static final int FLIP_Y = -203;
	static final int FLIP_Z = 3;

	/** {@code ModelData.scale}'s unit: 128 leaves an axis as it is. */
	private static final int UNIT_SCALE = 128;

	/**
	 * The 2005 worn model's bounds, min and max per axis, as measured in the 2005 cache - what the
	 * turned-back model should come to. Checked once, so a turn that did not take shows in the log.
	 */
	private static final int[] RETRO_WORN_BOUNDS = {24, 48, -140, -66, -49, 57};

	/** {@code faceColors3} value the renderers skip a face for. */
	private static final int HIDDEN_FACE = -2;

	// The client's lighting for player equipment
	private static final int PLAYER_AMBIENT = 64;
	private static final int PLAYER_CONTRAST = 850;
	private static final int PLAYER_LIGHT_X = -30;
	private static final int PLAYER_LIGHT_Y = -50;
	private static final int PLAYER_LIGHT_Z = -30;

	@Inject
	private Client client;

	/**
	 * Whether the icon swap is applied, as of the last item cache reset.
	 *
	 * <p>Volatile because ground items are drawn from the renderer's own threads.
	 */
	private volatile boolean iconActive;

	/** Whether the worn swap is applied. */
	private boolean wornActive;

	/** The posed player and shield together. Shared between players, consumed as each is drawn. */
	private final RetroModel composed = new RetroModel();

	/**
	 * The 2005 shield at rest: lit, and with its decals already lifted. Appended to each player as
	 * it is, then moved into place there.
	 */
	private RetroModel shieldRest;

	/** The live shield at rest, for finding it in the posed player. */
	private RetroAttachment.Part liveRest;

	private boolean shieldModelsFailed;

	/** Where the live shield was found in the last player drawn, tried first next time. */
	private int placementHint = -1;

	/** Whether a failure to find the live shield has been logged, so it is said once. */
	private boolean placementMissLogged;

	/** The client's ground model the lifted copy was made from, and the copy. Guarded by this. */
	private Model groundSource;
	private RetroModel groundLifted;

	/**
	 * Whether {@code itemId} is one of the anti-dragon shields this restores.
	 *
	 * <p>The noted shield and its bank placeholder are deliberately not here: their own inventory
	 * model is the note or placeholder template, and the shield is drawn onto it from the base
	 * item, so they follow the base item rather than needing a change of their own.
	 */
	static boolean isShieldItem(int itemId)
	{
		return itemId == ItemID.ANTIDRAGONBREATHSHIELD || itemId == ItemID.NZONE_ANTIDRAGONSHIELD;
	}

	/**
	 * Whether a player equipment id is one of the shields.
	 */
	static boolean isShieldEquipment(int equipmentId)
	{
		// Items sit at or above ITEM_OFFSET, kits between KIT_OFFSET and it
		return equipmentId >= PlayerComposition.ITEM_OFFSET
			&& isShieldItem(equipmentId - PlayerComposition.ITEM_OFFSET);
	}

	/**
	 * Points a freshly built shield definition at the 2005 inventory model.
	 *
	 * <p>The ground item is drawn from the same model, so it changes with the icon.
	 */
	public void onPostItemComposition(ItemComposition composition)
	{
		if (iconActive && isShieldItem(composition.getId()))
		{
			composition.setInventoryModel(RETRO_INVENTORY_MODEL);
		}
	}

	/**
	 * Brings both swaps in line with what is wanted, doing nothing for one that already is.
	 *
	 * @param iconWanted whether the icon should be the 2005 one
	 * @param wornWanted whether players should wear the 2005 shield
	 */
	public void refresh(boolean iconWanted, boolean wornWanted)
	{
		if (iconWanted != iconActive)
		{
			iconActive = iconWanted;
			// Only on a real change - a reset makes the client rebuild every item icon it shows
			resetItemCaches();
		}

		// Decided per draw from here, so there is nothing to put on or take off
		wornActive = wornWanted;
	}

	/**
	 * Undoes both swaps. Called as the plugin stops.
	 */
	public void shutDown()
	{
		refresh(false, false);
		shieldRest = null;
		liveRest = null;
		shieldModelsFailed = false;
		placementHint = -1;
		placementMissLogged = false;
		synchronized (this)
		{
			groundSource = null;
			groundLifted = null;
		}
	}

	/**
	 * Draws a player with the 2005 shield in place of the live one, or returns null to let the
	 * vanilla model through - for a player without the shield, or one the live shield cannot be
	 * found on.
	 *
	 * <p>Reads {@code vanilla} and copies out of it before anything else; it is a shared client
	 * buffer, valid only until the client poses another model.
	 */
	public Model substitute(Player player, Model vanilla)
	{
		if (!wornActive || !wearsShield(player) || !ensureShieldModels())
		{
			return null;
		}

		RetroAttachment.Placement placement = RetroAttachment.locate(liveRest,
			vanilla.getVerticesX(), vanilla.getVerticesY(), vanilla.getVerticesZ(), vanilla.getVerticesCount(),
			placementHint);
		if (placement == null)
		{
			if (!placementMissLogged)
			{
				placementMissLogged = true;
				log.debug("Could not find the live anti-dragon shield in {}'s model; drawing it as it is",
					player.getName());
			}
			return null;
		}
		placementHint = placement.start;

		composed.copyFrom(vanilla);
		hideFaces(composed, placement.start, placement.start + liveRest.getCount());

		// Appended at rest, decals and bias included, then moved into place where it now sits. The
		// map is affine, so a decal lifted in front of its surface at rest is still in front of it.
		int shieldStart = composed.getVerticesCount();
		composed.appendFrom(shieldRest);
		RetroAttachment.transform(placement.map, shieldRest.getVerticesX(), shieldRest.getVerticesY(),
			shieldRest.getVerticesZ(), shieldRest.getVerticesCount(),
			composed.getVerticesX(), composed.getVerticesY(), composed.getVerticesZ(), shieldStart);
		composed.calculateBoundsCylinder();

		return composed;
	}

	/**
	 * Draws a dropped shield with its sword in front of the face it is painted on, or returns null
	 * for anything else.
	 *
	 * <p>Called from the renderer's own threads, possibly several at once - so the lifted copy is
	 * built once per client model under a lock, and never written again after it is handed out. The
	 * client keeps one ground model per item, so that is a single build until its caches turn over.
	 */
	public Model substituteGroundItem(Renderable renderable, Model vanilla)
	{
		if (!iconActive || vanilla == null || !(renderable instanceof TileItem)
			|| !isShieldItem(((TileItem) renderable).getId()))
		{
			return null;
		}

		synchronized (this)
		{
			if (vanilla != groundSource)
			{
				RetroModel lifted = new RetroModel();
				lifted.copyFrom(vanilla);
				RetroDecals.lift(lifted, 0);
				groundLifted = lifted;
				groundSource = vanilla;
			}
			return groundLifted;
		}
	}

	private static boolean wearsShield(Player player)
	{
		PlayerComposition composition = player.getPlayerComposition();
		if (composition == null || composition.getTransformedNpcId() != -1)
		{
			return false;
		}

		int[] equipment = composition.getEquipmentIds();
		int slot = KitType.SHIELD.getIndex();
		return equipment != null && slot < equipment.length && isShieldEquipment(equipment[slot]);
	}

	/**
	 * Hides every face drawn entirely from vertices {@code [from, to)} - the live shield's faces,
	 * which the client appended along with its vertices.
	 */
	static void hideFaces(RetroModel model, int from, int to)
	{
		int[] faces1 = model.getFaceIndices1();
		int[] faces2 = model.getFaceIndices2();
		int[] faces3 = model.getFaceIndices3();
		int[] colors3 = model.getFaceColors3();
		for (int f = 0; f < model.getFaceCount(); f++)
		{
			if (faces1[f] >= from && faces1[f] < to
				&& faces2[f] >= from && faces2[f] < to
				&& faces3[f] >= from && faces3[f] < to)
			{
				colors3[f] = HIDDEN_FACE;
			}
		}
	}

	/**
	 * Builds what placing the shield needs, once: the live shield's rest vertices, and the 2005
	 * shield turned back, lit and with its decals lifted.
	 */
	private boolean ensureShieldModels()
	{
		if (shieldRest != null)
		{
			return true;
		}
		if (shieldModelsFailed)
		{
			return false;
		}

		ModelData live = client.loadModelData(LIVE_WORN_MODEL);
		ModelData retro = client.loadModelData(RETRO_WORN_MODEL);
		if (live == null || retro == null)
		{
			log.debug("Anti-dragon shield worn models {} or {} are missing from the cache",
				LIVE_WORN_MODEL, RETRO_WORN_MODEL);
			shieldModelsFailed = true;
			return false;
		}

		int liveCount = live.getVerticesCount();
		liveRest = new RetroAttachment.Part(Arrays.copyOf(live.getVerticesX(), liveCount),
			Arrays.copyOf(live.getVerticesY(), liveCount), Arrays.copyOf(live.getVerticesZ(), liveCount), liveCount);

		// Negating Y and Z together is the half turn about X - a rotation, not a mirror, so the faces
		// keep their winding. Turned before lighting, so the light falls the way it did in 2005.
		Model lit = retro.cloneVertices()
			.scale(UNIT_SCALE, -UNIT_SCALE, -UNIT_SCALE)
			.translate(0, FLIP_Y, FLIP_Z)
			.light(PLAYER_AMBIENT, PLAYER_CONTRAST, PLAYER_LIGHT_X, PLAYER_LIGHT_Y, PLAYER_LIGHT_Z);

		RetroModel rest = new RetroModel();
		rest.copyFrom(lit);
		// Before the lift, which moves the sword further than the check allows for
		checkBounds(rest);
		// Once, here, rather than on every player drawn: the shield is placed by an affine map, which
		// keeps a lifted decal on the near side of the surface behind it
		RetroDecals.lift(rest, 0);
		shieldRest = rest;
		return true;
	}

	/**
	 * Says, once, if the turned-back shield did not come out where the 2005 one sat - the one part
	 * of placing it that rests on the client's model transforms rather than on measurement.
	 */
	private static void checkBounds(RetroModel model)
	{
		float[][] axes = {model.getVerticesX(), model.getVerticesY(), model.getVerticesZ()};
		int[] bounds = new int[6];
		for (int axis = 0; axis < 3; axis++)
		{
			float min = Float.MAX_VALUE;
			float max = -Float.MAX_VALUE;
			for (int v = 0; v < model.getVerticesCount(); v++)
			{
				min = Math.min(min, axes[axis][v]);
				max = Math.max(max, axes[axis][v]);
			}
			bounds[axis * 2] = Math.round(min);
			bounds[axis * 2 + 1] = Math.round(max);
		}

		for (int i = 0; i < bounds.length; i++)
		{
			if (Math.abs(bounds[i] - RETRO_WORN_BOUNDS[i]) > 2)
			{
				log.debug("2005 anti-dragon shield bounds {} are not the 2005 ones {}; it will be misplaced",
					Arrays.toString(bounds), Arrays.toString(RETRO_WORN_BOUNDS));
				return;
			}
		}
	}

	/**
	 * Makes the client rebuild item definitions, models and icons, so a change to the icon swap
	 * shows at once rather than as items happen to be evicted.
	 */
	private void resetItemCaches()
	{
		client.getItemCompositionCache().reset();
		client.getItemModelCache().reset();
		client.getItemSpriteCache().reset();
	}
}
