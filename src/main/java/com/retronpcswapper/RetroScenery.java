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

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.TileObject;
import net.runelite.api.gameval.ObjectID;

/**
 * The scenery restored to its 2005 model, one constant per config toggle's worth.
 * <p>
 * Each names its 2005 mesh, carried in the bundle under its 2005 model id, and the live objects
 * it stands in for. Those are keyed by live id, never by the 2005 definition's: many 2005 object
 * ids now belong to something else entirely. The lighting is the 2005 definition's own (loc
 * opcodes 29 and 39), from {@code ./gradlew dumpRetroObjectDefinitions}.
 */
enum RetroScenery
{
	/** 2005 def 908. The live object and model id survive, but the mesh behind the id is the Magical symbol. */
	MYSTICAL_WALL_CHART(Placement.WALL_DECORATION, 2086, 50, 0, ObjectID.WITCHESWALLCHART),

	/**
	 * 2005 def 884, mesh 1410. The live id holds a rebuilt well, scaled 90/100/90 by its definition
	 * to fit; the 2005 mesh needs no scale. Varrock's wells were re-placed by the 2007 rework as
	 * their own object, 3646 on a new model 25044 - in 2005 that id was an unrelated ground
	 * decoration - but it is the same plain well at the same size, so it takes the 2005 one too.
	 * Later wells drawn from the live mesh - Keldagrim, Rellekka, Falador, the gnome well - are
	 * left alone: they never had a 2005 look, and their recolours target colours the 2005 mesh
	 * does not use.
	 */
	WELL(Placement.GAME_OBJECT, 1410, 0, 0, ObjectID.WELL, ObjectID.FAI_VARROCK_WELL),

	/**
	 * 2005 def 388, mesh 1951 - the Draynor Manor wardrobe. The live id holds a rebuilt wardrobe
	 * that carries its texture-26 panel as a second model, 5531; the 2005 mesh has the panel
	 * built in, so hiding the object hides both. Fenkenstrain's broom cupboard was the same mesh
	 * on a 1x2 footprint in 2005 (def 5156), and the untinted Dragon Slayer and broom cupboard
	 * copies follow it. The recoloured copies - Draynor village, Sithik, the Grim witch's house,
	 * Kourend, Mistmyst - are left alone, for the same reason as the later wells.
	 */
	WARDROBE(Placement.GAME_OBJECT, 1951, 0, 0, ObjectID.SPOOKYWARDROBE,
		ObjectID.DRAGONSLAYER_SPOOKYWARDROBE, ObjectID.FENK_BROOMCUPBOARD, ObjectID.DEAL_BROOMCUPBOARD),

	/** 2005 def 389, mesh 1953: the same wardrobe opened, and its copies as for {@link #WARDROBE}. */
	WARDROBE_OPEN(Placement.GAME_OBJECT, 1953, 0, 0, ObjectID.SPOOKYWARDROBE_OPEN,
		ObjectID.DRAGONSLAYER_SPOOKYWARDROBE_OPEN, ObjectID.FENK_BROOMCUPBOARD_OPEN,
		ObjectID.DEAL_BROOMCUPBOARD_OPEN),

	/** 2005 def 390, mesh 1949: opened, with the skeleton inside. Its only copy is recoloured. */
	WARDROBE_OPEN_SKELETON(Placement.GAME_OBJECT, 1949, 0, 0, ObjectID.SPOOKYWARDROBE_OPEN_SKELETON),

	/**
	 * 2005 def 1281, mesh 1571. The live id is the same oak touched up - 61% of the 2005 vertex
	 * positions, eight fewer faces - and the exact 2005 copy at 8288 is shrunk to 80/128, so the
	 * mesh comes from the bundle. The live oak's trunk recolour is the live repaint, not the 2005
	 * look, so it is not carried. The untinted copies - Tutorial Island's, the group ironman oak -
	 * and the copies sharing the live oak's own recolour follow it; the 4x4 Avium oak, drawn at
	 * 160/190, and the snowy Christmas oak are left alone.
	 */
	OAK_TREE(Placement.GAME_OBJECT, 1571, 0, 0, ObjectID.OAKTREE, ObjectID.NEWBIEOAKTREE,
		ObjectID.TUT2_OAK, ObjectID.TUT2_OAK_NOOP, ObjectID.TREE_OAK_DEFAULT01, ObjectID.OAKTREE_NOOP,
		ObjectID.GIM_OAKTREE),

	/**
	 * 2005 def 1306, mesh 1693. Unlike the rest, the 2005 mesh survives at its id untouched - every
	 * vertex and face color - and simply went unused when the live magic tree moved to model 7978,
	 * so it is drawn straight from the live cache. The untinted copies on 7978 follow; the farming
	 * patch's growth stage on it does not, or one stage of eleven would grow into a 2005 tree.
	 * A magic tree placed as 10835 - a parent switching by varbit 4153 between this tree and its
	 * stump - stays live: the swapper knows an object by the id placed in the map, not the child
	 * the client resolves it to.
	 */
	MAGIC_TREE(Source.LIVE_CACHE, Placement.GAME_OBJECT, 1693, 0, 0, ObjectID.MAGICTREE,
		ObjectID.CRAB_MAGICTREE, ObjectID.CRAB_MAGICTREE_NOOP),

	/**
	 * 2005 def 611, mesh 1453. The live id holds a rebuilt bench (4% vertex overlap). Port
	 * Sarim's untinted copy follows; the recoloured garden, Pirate's Treasure, Falador and Clan
	 * Wars copies are left alone, as the later wells are.
	 */
	PICNIC_BENCH(Placement.GAME_OBJECT, 1453, 0, 0, ObjectID.PICNICBENCH, ObjectID.SARIM_PICNICBENCH);

	/** Where the 2005 mesh comes from. */
	enum Source
	{
		/** The bundle, under its 2005 model id - the live cache no longer has it. */
		BUNDLE,
		/** The live cache, at its 2005 model id, which still holds it unchanged. */
		LIVE_CACHE
	}

	/** How a scenery object sits in the scene, which decides how its stand-in is placed. */
	enum Placement
	{
		/** On a wall, placement types 4-8. */
		WALL_DECORATION,
		/** Standing on its tiles, placement types 10 and 11. */
		GAME_OBJECT
	}

	private static final Map<Integer, RetroScenery> BY_OBJECT_ID = new HashMap<>();

	static
	{
		for (RetroScenery scenery : values())
		{
			for (int objectId : scenery.objectIds)
			{
				RetroScenery previous = BY_OBJECT_ID.put(objectId, scenery);
				if (previous != null)
				{
					throw new IllegalStateException("Object " + objectId + " is claimed by both "
						+ previous + " and " + scenery);
				}
			}
		}
	}

	final Source source;
	final Placement placement;

	/** The 2005 model id, which the bundle or the live cache - as {@link #source} says - holds the mesh under. */
	final int meshId;

	/** The 2005 definition's lighting adjustments, on top of the client's base. */
	final int ambient;
	final int contrast;

	private final int[] objectIds;

	RetroScenery(Placement placement, int meshId, int ambient, int contrast, int... objectIds)
	{
		this(Source.BUNDLE, placement, meshId, ambient, contrast, objectIds);
	}

	RetroScenery(Source source, Placement placement, int meshId, int ambient, int contrast, int... objectIds)
	{
		this.source = source;
		this.placement = placement;
		this.meshId = meshId;
		this.ambient = ambient;
		this.contrast = contrast;
		this.objectIds = objectIds;
	}

	/**
	 * Whether the 2005 mesh paints detail onto a surface by priority, where a depth-buffered
	 * renderer would hide it, and so has its decals lifted to the front. The wardrobes' doors lie
	 * partly behind the cabinet front. Decided per mesh, from measuring each, rather than for all:
	 * the lift leaves the other meshes' vertices where they are, but would still bias their
	 * higher-priority faces toward the camera.
	 */
	boolean hasPaintedDetail()
	{
		return this == WARDROBE || this == WARDROBE_OPEN || this == WARDROBE_OPEN_SKELETON;
	}

	int[] getObjectIds()
	{
		return objectIds.clone();
	}

	/** The scenery a live object id stands for, or null. */
	static RetroScenery forObject(int objectId)
	{
		return BY_OBJECT_ID.get(objectId);
	}

	/**
	 * The scenery a scene object is placed as, or null - for an object of no restored id, or one
	 * placed in a way its stand-in cannot reproduce.
	 *
	 * <p>The placement checks are load-bearing: this is also asked about temporary entities, the
	 * stand-ins among them, and those must never match. A stand-in's own object has no
	 * centrepiece placement bits, so it never passes the game object check.
	 */
	static RetroScenery forObject(TileObject object)
	{
		RetroScenery scenery = forObject(object.getId());
		if (scenery == null)
		{
			return null;
		}

		switch (scenery.placement)
		{
			case WALL_DECORATION:
				return object instanceof DecorativeObject
					&& RetroDecorController.isSupportedType(ObjectPlacement.type(((DecorativeObject) object).getConfig()))
					? scenery : null;
			case GAME_OBJECT:
				if (!(object instanceof GameObject))
				{
					return null;
				}
				int type = ObjectPlacement.type(((GameObject) object).getConfig());
				return type == ObjectPlacement.TYPE_CENTREPIECE || type == ObjectPlacement.TYPE_DIAGONAL_CENTREPIECE
					? scenery : null;
			default:
				return null;
		}
	}
}
