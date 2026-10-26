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

import lombok.Getter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Encapsulates retro visual model and animation data extracted from the 2004/2005 cache,
 * along with category-scoped sets of modern animation IDs to intercept.
 */
public class RetroNpcData
{
	@Getter
	private final RetroNpcCategory category;

	/**
	 * Model IDs from the 2004/2005 RuneScape cache, as looked up in the <em>live</em> cache.
	 */
	private final int[] retroModelIds;

	/**
	 * Model IDs to assemble from the injected asset bundle, when they differ from
	 * {@link #retroModelIds}. Null means the two are the same, which is the usual case.
	 *
	 * <p>The two paths diverge when a 2005 part no longer resolves in the live cache. Hill giants
	 * are the case that forced this: the real 2005 head 2862 is gone, so the cache-backed path
	 * substitutes a Jogre head (2866) that still exists, while the bundle can carry the real one.
	 * One shared list would either put a dead id in front of {@code loadModelData} or deny the
	 * injected path the correct head.
	 */
	private final int[] injectedModelIds;

	/**
	 * Idle (standing) animation sequence ID.
	 * Set to -1 if preserving default NPC idle animation.
	 */
	@Getter
	private final int idleAnimationId;

	/**
	 * Whether the NPC stood unanimated in 2005, so an idle of -1 means "no idle" rather than
	 * "keep the modern one".
	 */
	private final boolean staticIdle;

	/**
	 * Walking animation sequence ID.
	 * Set to -1 if preserving default NPC walk animation.
	 */
	@Getter
	private final int walkAnimationId;

	/**
	 * Attack animation sequence ID.
	 * Set to -1 if preserving default NPC attack animation.
	 */
	@Getter
	private final int attackAnimationId;

	/**
	 * Defend / block animation sequence ID.
	 * Set to -1 if preserving default NPC block animation.
	 */
	@Getter
	private final int defendAnimationId;

	/**
	 * Death animation sequence ID.
	 * Set to -1 if preserving default NPC death animation.
	 */
	@Getter
	private final int deathAnimationId;

	/**
	 * Stand-in for modern animations that have no 2005 counterpart at all.
	 * Set to -1 if there are none, which is every category but cows.
	 *
	 * <p>The other four slots each replace a 2005 animation with its modern successor. This one
	 * exists because some NPCs gained behaviour they never had: a modern cow grazes, chews and
	 * gives milk, and February 2005 holds 1670 sequences, so 1735 - the first graze - is past the
	 * end of the cache. There is nothing to swap those for, and leaving them alone is not an
	 * option either, because they play on the modern framemap and the retro mesh is not rigged to
	 * it. Redirecting them to the retro idle is both the fix and what 2005 actually looked like.
	 */
	@Getter
	private final int miscAnimationId;

	/**
	 * Horizontal resize of the retro model, in 1/128ths.
	 *
	 * <p>Seeded from the 2005 definition, then corrected per category where the 2005 and modern
	 * meshes are simply different sizes. The modern composition's own widthScale/heightScale is
	 * deliberately not used: it was authored for the modern mesh and means nothing applied to
	 * retro geometry (the live Chicken composition, for instance, is 80 - it shrinks its model).
	 */
	@Getter
	private final int scaleXZ;

	/**
	 * Vertical resize of the retro model, in 1/128ths. See {@link #scaleXZ}.
	 */
	@Getter
	private final int scaleY;

	/**
	 * Model-space shift along Z applied after the resize, in live model units. Zero for every NPC
	 * whose live model is centred on its footprint, which is nearly all of them.
	 * <p>
	 * Exists because the live client has no placement of its own to borrow: where an NPC stands
	 * within its footprint is baked into its model's vertices. The live lair Kalphite Soldier's
	 * model is its POH twin moved 71 units back, so it stands half a tile off center, and a centred
	 * 2005 mesh drawn there looks misplaced beside it. Being model-space, the shift turns with the
	 * NPC, exactly as the baked one does.
	 */
	@Getter
	private final int offsetZ;

	/**
	 * Recolor pairs applied to the retro mesh, parallel arrays of palette values.
	 * <p>
	 * The 2005 client colored same-mesh NPC variants here rather than with separate models -
	 * every dragon shares one mesh and recolors palette index 61 to its own color - so without
	 * these a baby blue dragon and a baby red dragon would render identically.
	 */
	private final short[] originalColors;

	/**
	 * Replacement palette values, parallel to {@link #originalColors}.
	 */
	private final short[] replacementColors;

	/**
	 * Modern attack animation IDs to intercept and swap for this NPC category.
	 */
	@Getter
	private final Set<Integer> modernAttackAnims;

	/**
	 * Modern defend / hit animation IDs to intercept and swap for this NPC category.
	 */
	@Getter
	private final Set<Integer> modernDefendAnims;

	/**
	 * Modern death animation IDs to intercept and swap for this NPC category.
	 */
	@Getter
	private final Set<Integer> modernDeathAnims;

	/**
	 * Modern animation IDs with no 2005 counterpart, redirected to {@link #miscAnimationId}.
	 */
	@Getter
	private final Set<Integer> modernMiscAnims;

	/**
	 * Modern attack animations that each swap to a 2005 attack of their own, rather than to
	 * {@link #attackAnimationId}.
	 *
	 * <p>For NPCs that fight in more than one style. The live skeleton mage alternates a melee
	 * swing with a spell cast, and 2005 had a separate sequence for each, so collapsing both onto
	 * one slot would have it cast every punch. Checked ahead of {@link #modernAttackAnims}, and an
	 * animation named here counts as an attack whether or not that set names it too.
	 */
	private final Map<Integer, Integer> modernAttackOverrides;

	/**
	 * Added to every bundle lookup the injected path makes for this NPC: each part's mesh, and the
	 * clip for whatever animation it plays. Zero for everything but Nov-2005 human kit, which the
	 * bundle stores at {@link com.retronpcswapper.inject.RetroAssetBundle#NOV_HUMAN_KEY_BASE} because
	 * the same model and sequence ids already hold the Feb-2005 copies, bound to a different rig.
	 * <p>
	 * The part lists and animation ids stay as cache ids; only the lookup is offset. The server
	 * still sends 808, so nothing upstream of the draw has to know which copy is in use.
	 */
	@Getter
	private final int assetKeyBase;

	/**
	 * The parts of the 2005 dialogue head, npc.dat opcode 60, or null when the definition had none -
	 * in which case the modern chathead is left alone. Drawn by the chathead overlay from the
	 * bundle, recolored with {@link #originalColors} as the 2005 client recolored heads too, but
	 * never resized: that client ignored opcodes 97 and 98 on a head.
	 */
	private final int[] chatheadModelIds;

	/**
	 * Added to every bundle lookup for the chathead: its parts and the clips for its emotes. The
	 * same as {@link #assetKeyBase} except for a head on a rig of its own whose emotes would otherwise
	 * share ids with the human heads' - see
	 * {@link com.retronpcswapper.inject.RetroAssetBundle#GOBLIN_CHATHEAD_KEY_BASE}.
	 */
	@Getter
	private final int chatheadKeyBase;

	/**
	 * Pairs of injected parts, lower then upper, that 2005 drew one over the other by priority alone -
	 * which a depth buffer does not. See {@link com.retronpcswapper.inject.RetroDecals#paintOver}.
	 */
	private final int[] paintedOver;

	public RetroNpcData(
		RetroNpcCategory category,
		int[] retroModelIds,
		int idleAnimationId,
		int walkAnimationId,
		int attackAnimationId,
		int defendAnimationId,
		int deathAnimationId,
		int miscAnimationId,
		int scaleXZ,
		int scaleY,
		short[] originalColors,
		short[] replacementColors,
		Set<Integer> modernAttackAnims,
		Set<Integer> modernDefendAnims,
		Set<Integer> modernDeathAnims,
		Set<Integer> modernMiscAnims
	)
	{
		this(category, retroModelIds, null, idleAnimationId, walkAnimationId, attackAnimationId,
			defendAnimationId, deathAnimationId, miscAnimationId, scaleXZ, scaleY, originalColors,
			replacementColors, modernAttackAnims, modernDefendAnims, modernDeathAnims, modernMiscAnims,
			null, false, 0, 0, null, 0, null);
	}

	public RetroNpcData(
		RetroNpcCategory category,
		int[] retroModelIds,
		int[] injectedModelIds,
		int idleAnimationId,
		int walkAnimationId,
		int attackAnimationId,
		int defendAnimationId,
		int deathAnimationId,
		int miscAnimationId,
		int scaleXZ,
		int scaleY,
		short[] originalColors,
		short[] replacementColors,
		Set<Integer> modernAttackAnims,
		Set<Integer> modernDefendAnims,
		Set<Integer> modernDeathAnims,
		Set<Integer> modernMiscAnims,
		Map<Integer, Integer> modernAttackOverrides,
		boolean staticIdle,
		int assetKeyBase,
		int offsetZ,
		int[] chatheadModelIds,
		int chatheadKeyBase,
		int[] paintedOver
	)
	{
		this.category = category;
		this.retroModelIds = retroModelIds != null ? retroModelIds.clone() : new int[0];
		this.injectedModelIds = injectedModelIds != null ? injectedModelIds.clone() : null;
		this.idleAnimationId = idleAnimationId;
		this.staticIdle = staticIdle;
		this.walkAnimationId = walkAnimationId;
		this.attackAnimationId = attackAnimationId;
		this.defendAnimationId = defendAnimationId;
		this.deathAnimationId = deathAnimationId;
		this.miscAnimationId = miscAnimationId;
		this.scaleXZ = scaleXZ;
		this.scaleY = scaleY;
		// Only keep the pairs when both sides are present and agree - a half-populated recolor
		// would throw at draw time, which is the worst place to find out
		boolean recolorUsable = originalColors != null && replacementColors != null
			&& originalColors.length > 0 && originalColors.length == replacementColors.length;
		this.originalColors = recolorUsable ? originalColors.clone() : new short[0];
		this.replacementColors = recolorUsable ? replacementColors.clone() : new short[0];
		this.modernAttackAnims = modernAttackAnims != null
			? Set.copyOf(modernAttackAnims)
			: Collections.emptySet();
		this.modernDefendAnims = modernDefendAnims != null
			? Set.copyOf(modernDefendAnims)
			: Collections.emptySet();
		this.modernDeathAnims = modernDeathAnims != null
			? Set.copyOf(modernDeathAnims)
			: Collections.emptySet();
		this.modernMiscAnims = modernMiscAnims != null
			? Set.copyOf(modernMiscAnims)
			: Collections.emptySet();
		this.modernAttackOverrides = modernAttackOverrides != null
			? Map.copyOf(modernAttackOverrides)
			: Collections.emptyMap();
		this.assetKeyBase = assetKeyBase;
		this.offsetZ = offsetZ;
		this.chatheadModelIds = chatheadModelIds != null && chatheadModelIds.length > 0
			? chatheadModelIds.clone() : null;
		this.chatheadKeyBase = chatheadKeyBase;
		this.paintedOver = paintedOver != null ? paintedOver.clone() : new int[0];
	}

	public boolean hasStaticIdle()
	{
		return staticIdle;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public boolean isAttackAnimation(int animId)
	{
		return modernAttackOverrides.containsKey(animId)
			|| (attackAnimationId != -1 && modernAttackAnims.contains(animId));
	}

	/**
	 * The 2005 attack to play in place of this modern one, or -1 if it is not an attack this
	 * mapping swaps.
	 */
	public int getAttackAnimationFor(int animId)
	{
		Integer override = modernAttackOverrides.get(animId);
		if (override != null)
		{
			return override;
		}
		return isAttackAnimation(animId) ? attackAnimationId : -1;
	}

	/**
	 * Whether this is one of the 2005 attacks this mapping plays, so seeing it again is the swap
	 * landing rather than a new action to intercept.
	 */
	public boolean isRetroAttackAnimation(int animId)
	{
		return animId != -1
			&& (animId == attackAnimationId || modernAttackOverrides.containsValue(animId));
	}

	public boolean isDefendAnimation(int animId)
	{
		return defendAnimationId != -1 && modernDefendAnims.contains(animId);
	}

	public boolean isDeathAnimation(int animId)
	{
		return deathAnimationId != -1 && modernDeathAnims.contains(animId);
	}

	public boolean isMiscAnimation(int animId)
	{
		return miscAnimationId != -1 && modernMiscAnims.contains(animId);
	}

	public int[] getRetroModelIds()
	{
		return retroModelIds.clone();
	}

	/**
	 * The model IDs the injected path assembles, falling back to {@link #getRetroModelIds()} when
	 * no separate list was given.
	 */
	public int[] getInjectedModelIds()
	{
		return injectedModelIds != null ? injectedModelIds.clone() : retroModelIds.clone();
	}

	public short[] getOriginalColors()
	{
		return originalColors.clone();
	}

	public short[] getReplacementColors()
	{
		return replacementColors.clone();
	}

	public boolean hasRecolors()
	{
		return originalColors.length > 0;
	}

	/**
	 * Returns a copy carrying these recolor pairs, leaving everything else alone.
	 *
	 * <p>Exists for the static archetypes. They take precedence over the generated JSON row, but
	 * that row is the only source of the 2005 opcode 40 pairs, so the two have to be recombined
	 * after the fact rather than at construction - the archetypes are built before any cache data
	 * is read.
	 */
	public RetroNpcData withRecolors(short[] originalColors, short[] replacementColors)
	{
		return new RetroNpcData(
			category,
			retroModelIds,
			injectedModelIds,
			idleAnimationId,
			walkAnimationId,
			attackAnimationId,
			defendAnimationId,
			deathAnimationId,
			miscAnimationId,
			scaleXZ,
			scaleY,
			originalColors,
			replacementColors,
			modernAttackAnims,
			modernDefendAnims,
			modernDeathAnims,
			modernMiscAnims,
			modernAttackOverrides,
			staticIdle,
			assetKeyBase,
			offsetZ,
			chatheadModelIds,
			chatheadKeyBase,
			paintedOver);
	}

	/**
	 * Returns a copy resized to these 1/128ths, leaving everything else alone.
	 *
	 * <p>Exists for the same reason {@link #withRecolors} does: the resize is the 2005 definition's,
	 * it reaches the plugin only through the generated JSON row, and an archetype is built before
	 * any of that is read.
	 */
	public RetroNpcData withScale(int scaleXZ, int scaleY)
	{
		return new RetroNpcData(
			category,
			retroModelIds,
			injectedModelIds,
			idleAnimationId,
			walkAnimationId,
			attackAnimationId,
			defendAnimationId,
			deathAnimationId,
			miscAnimationId,
			scaleXZ,
			scaleY,
			originalColors,
			replacementColors,
			modernAttackAnims,
			modernDefendAnims,
			modernDeathAnims,
			modernMiscAnims,
			modernAttackOverrides,
			staticIdle,
			assetKeyBase,
			offsetZ,
			chatheadModelIds,
			chatheadKeyBase,
			paintedOver);
	}

	/**
	 * Returns a copy wearing these parts, leaving everything else alone.
	 *
	 * <p>For NPCs that are the same character carrying different equipment - one guard family
	 * holds a sword, another a battleaxe. Everything that makes them a guard is shared, so the
	 * variant is derived from the archetype rather than declared beside it, which also means it
	 * inherits the recolor pairs already grafted on.
	 *
	 * <p>Only the cache-backed list is replaced. A mapping that keeps one list for both paths -
	 * every guard - therefore wears the new parts on both, which is what a change of weapon means.
	 * A mapping that declares a distinct injected list keeps it, rather than silently collapsing
	 * onto the parts passed here: a hill giant variant derived this way would otherwise be injected
	 * wearing the Jogre head that only the cache-backed list is meant to carry.
	 */
	public RetroNpcData withModelIds(int[] modelIds)
	{
		return new RetroNpcData(
			category,
			modelIds,
			injectedModelIds,
			idleAnimationId,
			walkAnimationId,
			attackAnimationId,
			defendAnimationId,
			deathAnimationId,
			miscAnimationId,
			scaleXZ,
			scaleY,
			originalColors,
			replacementColors,
			modernAttackAnims,
			modernDefendAnims,
			modernDeathAnims,
			modernMiscAnims,
			modernAttackOverrides,
			staticIdle,
			assetKeyBase,
			offsetZ,
			chatheadModelIds,
			chatheadKeyBase,
			paintedOver);
	}

	/** Lower and upper part ids, flattened in pairs. See {@link #paintedOver}. */
	public int[] getPaintedOver()
	{
		return paintedOver.clone();
	}

	/** Whether the 2005 definition had a dialogue head of its own. */
	public boolean hasChathead()
	{
		return chatheadModelIds != null;
	}

	/** The dialogue head's parts, or null when there is none. See {@link #chatheadModelIds}. */
	public int[] getChatheadModelIds()
	{
		return chatheadModelIds != null ? chatheadModelIds.clone() : null;
	}

	/**
	 * Returns a copy with this dialogue head, in the same key range, leaving everything else
	 * alone.
	 *
	 * <p>For the same reason {@link #withRecolors} exists - the head reaches an archetype from its
	 * JSON row after construction - and for a variant that changes the head the body wears.
	 */
	public RetroNpcData withChathead(int[] chatheadModelIds)
	{
		return new RetroNpcData(
			category,
			retroModelIds,
			injectedModelIds,
			idleAnimationId,
			walkAnimationId,
			attackAnimationId,
			defendAnimationId,
			deathAnimationId,
			miscAnimationId,
			scaleXZ,
			scaleY,
			originalColors,
			replacementColors,
			modernAttackAnims,
			modernDefendAnims,
			modernDeathAnims,
			modernMiscAnims,
			modernAttackOverrides,
			staticIdle,
			assetKeyBase,
			offsetZ,
			chatheadModelIds,
			chatheadKeyBase,
			paintedOver);
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RetroNpcData that = (RetroNpcData) o;
		return idleAnimationId == that.idleAnimationId &&
			staticIdle == that.staticIdle &&
			walkAnimationId == that.walkAnimationId &&
			attackAnimationId == that.attackAnimationId &&
			defendAnimationId == that.defendAnimationId &&
			deathAnimationId == that.deathAnimationId &&
			miscAnimationId == that.miscAnimationId &&
			scaleXZ == that.scaleXZ &&
			scaleY == that.scaleY &&
			assetKeyBase == that.assetKeyBase &&
			offsetZ == that.offsetZ &&
			chatheadKeyBase == that.chatheadKeyBase &&
			category == that.category &&
			Arrays.equals(retroModelIds, that.retroModelIds) &&
			Arrays.equals(injectedModelIds, that.injectedModelIds) &&
			Arrays.equals(originalColors, that.originalColors) &&
			Arrays.equals(replacementColors, that.replacementColors) &&
			Arrays.equals(chatheadModelIds, that.chatheadModelIds) &&
			Arrays.equals(paintedOver, that.paintedOver) &&
			Objects.equals(modernAttackAnims, that.modernAttackAnims) &&
			Objects.equals(modernDefendAnims, that.modernDefendAnims) &&
			Objects.equals(modernDeathAnims, that.modernDeathAnims) &&
			Objects.equals(modernMiscAnims, that.modernMiscAnims) &&
			Objects.equals(modernAttackOverrides, that.modernAttackOverrides);
	}

	@Override
	public int hashCode()
	{
		int result = category != null ? category.hashCode() : 0;
		result = 31 * result + Arrays.hashCode(retroModelIds);
		result = 31 * result + Arrays.hashCode(injectedModelIds);
		result = 31 * result + idleAnimationId;
		result = 31 * result + (staticIdle ? 1 : 0);
		result = 31 * result + walkAnimationId;
		result = 31 * result + attackAnimationId;
		result = 31 * result + defendAnimationId;
		result = 31 * result + deathAnimationId;
		result = 31 * result + miscAnimationId;
		result = 31 * result + scaleXZ;
		result = 31 * result + scaleY;
		result = 31 * result + assetKeyBase;
		result = 31 * result + offsetZ;
		result = 31 * result + Arrays.hashCode(chatheadModelIds);
		result = 31 * result + chatheadKeyBase;
		result = 31 * result + Arrays.hashCode(paintedOver);
		result = 31 * result + Arrays.hashCode(originalColors);
		result = 31 * result + Arrays.hashCode(replacementColors);
		result = 31 * result + (modernAttackAnims != null ? modernAttackAnims.hashCode() : 0);
		result = 31 * result + (modernDefendAnims != null ? modernDefendAnims.hashCode() : 0);
		result = 31 * result + (modernDeathAnims != null ? modernDeathAnims.hashCode() : 0);
		result = 31 * result + (modernMiscAnims != null ? modernMiscAnims.hashCode() : 0);
		result = 31 * result + modernAttackOverrides.hashCode();
		return result;
	}

	public static class Builder
	{
		private RetroNpcCategory category;
		private int[] retroModelIds = new int[0];
		private int[] injectedModelIds;
		private int idleAnimationId = -1;
		private boolean staticIdle;
		private int walkAnimationId = -1;
		private int attackAnimationId = -1;
		private int defendAnimationId = -1;
		private int deathAnimationId = -1;
		private int miscAnimationId = -1;
		private int scaleXZ = 128;
		private int scaleY = 128;
		private short[] originalColors;
		private short[] replacementColors;
		private final Set<Integer> modernAttackAnims = new HashSet<>();
		private final Set<Integer> modernDefendAnims = new HashSet<>();
		private final Set<Integer> modernDeathAnims = new HashSet<>();
		private final Set<Integer> modernMiscAnims = new HashSet<>();
		private final Map<Integer, Integer> modernAttackOverrides = new HashMap<>();
		private int assetKeyBase;
		private int offsetZ;
		private int[] chatheadModelIds;
		private Integer chatheadKeyBase;
		private int[] paintedOver = new int[0];

		public Builder category(RetroNpcCategory category)
		{
			this.category = category;
			return this;
		}

		public Builder retroModelIds(int[] retroModelIds)
		{
			this.retroModelIds = retroModelIds != null ? retroModelIds.clone() : new int[0];
			return this;
		}

		public Builder idleAnimationId(int idleAnimationId)
		{
			this.idleAnimationId = idleAnimationId;
			return this;
		}

		/**
		 * Clears the idle pose instead of leaving the modern one playing, for an NPC that stood
		 * unanimated in 2005. See {@link RetroNpcData#hasStaticIdle}.
		 */
		public Builder staticIdle()
		{
			this.staticIdle = true;
			return this;
		}

		public Builder walkAnimationId(int walkAnimationId)
		{
			this.walkAnimationId = walkAnimationId;
			return this;
		}

		public Builder attackAnimationId(int attackAnimationId)
		{
			this.attackAnimationId = attackAnimationId;
			return this;
		}

		public Builder defendAnimationId(int defendAnimationId)
		{
			this.defendAnimationId = defendAnimationId;
			return this;
		}

		public Builder deathAnimationId(int deathAnimationId)
		{
			this.deathAnimationId = deathAnimationId;
			return this;
		}

		public Builder scaleXZ(int scaleXZ)
		{
			this.scaleXZ = scaleXZ;
			return this;
		}

		public Builder scaleY(int scaleY)
		{
			this.scaleY = scaleY;
			return this;
		}

		/**
		 * Recolor pairs as parallel arrays, in npc.dat opcode 40 order.
		 */
		public Builder recolors(short[] originalColors, short[] replacementColors)
		{
			this.originalColors = originalColors;
			this.replacementColors = replacementColors;
			return this;
		}

		/**
		 * Convenience for the common single-pair case, where one palette index carries the whole
		 * color of the variant.
		 */
		public Builder recolor(int originalColor, int replacementColor)
		{
			this.originalColors = new short[]{(short) originalColor};
			this.replacementColors = new short[]{(short) replacementColor};
			return this;
		}

		public Builder modernAttackAnims(Collection<Integer> anims)
		{
			if (anims != null)
			{
				this.modernAttackAnims.addAll(anims);
			}
			return this;
		}

		public Builder modernAttackAnims(int... anims)
		{
			if (anims != null)
			{
				for (int a : anims)
				{
					this.modernAttackAnims.add(a);
				}
			}
			return this;
		}

		/**
		 * Swaps each of these modern attacks for this one 2005 attack instead of
		 * {@link #attackAnimationId}. See {@link RetroNpcData#modernAttackOverrides}.
		 */
		public Builder attackAnimationOverride(int retroAnim, int... modernAnims)
		{
			for (int modern : modernAnims)
			{
				this.modernAttackOverrides.put(modern, retroAnim);
			}
			return this;
		}

		public Builder modernDefendAnims(Collection<Integer> anims)
		{
			if (anims != null)
			{
				this.modernDefendAnims.addAll(anims);
			}
			return this;
		}

		public Builder modernDefendAnims(int... anims)
		{
			if (anims != null)
			{
				for (int a : anims)
				{
					this.modernDefendAnims.add(a);
				}
			}
			return this;
		}

		public Builder modernDeathAnims(Collection<Integer> anims)
		{
			if (anims != null)
			{
				this.modernDeathAnims.addAll(anims);
			}
			return this;
		}

		public Builder modernDeathAnims(int... anims)
		{
			if (anims != null)
			{
				for (int a : anims)
				{
					this.modernDeathAnims.add(a);
				}
			}
			return this;
		}

		public Builder miscAnimationId(int miscAnimationId)
		{
			this.miscAnimationId = miscAnimationId;
			return this;
		}

		public Builder modernMiscAnims(Collection<Integer> anims)
		{
			if (anims != null)
			{
				this.modernMiscAnims.addAll(anims);
			}
			return this;
		}

		public Builder modernMiscAnims(int... anims)
		{
			if (anims != null)
			{
				for (int a : anims)
				{
					this.modernMiscAnims.add(a);
				}
			}
			return this;
		}

		/**
		 * Overrides the model IDs the injected path assembles, for an NPC whose 2005 parts no
		 * longer all resolve in the live cache.
		 */
		public Builder injectedModelIds(int[] injectedModelIds)
		{
			this.injectedModelIds = injectedModelIds;
			return this;
		}

		/** See {@link RetroNpcData#assetKeyBase}. */
		public Builder assetKeyBase(int assetKeyBase)
		{
			this.assetKeyBase = assetKeyBase;
			return this;
		}

		/** See {@link RetroNpcData#offsetZ}. */
		public Builder offsetZ(int offsetZ)
		{
			this.offsetZ = offsetZ;
			return this;
		}

		/** The 2005 dialogue head's parts, see {@link RetroNpcData#chatheadModelIds}. */
		public Builder chathead(int... chatheadModelIds)
		{
			this.chatheadModelIds = chatheadModelIds;
			return this;
		}

		/**
		 * Keys the dialogue head apart from the body, see {@link RetroNpcData#chatheadKeyBase}.
		 * Unset, it follows {@link #assetKeyBase}.
		 */
		public Builder chatheadKeyBase(int chatheadKeyBase)
		{
			this.chatheadKeyBase = chatheadKeyBase;
			return this;
		}

		/** The upper part was drawn over the lower by priority, see {@link RetroNpcData#paintedOver}. */
		public Builder paintedOver(int lowerPart, int upperPart)
		{
			paintedOver = Arrays.copyOf(paintedOver, paintedOver.length + 2);
			paintedOver[paintedOver.length - 2] = lowerPart;
			paintedOver[paintedOver.length - 1] = upperPart;
			return this;
		}

		public RetroNpcData build()
		{
			return new RetroNpcData(
				category,
				retroModelIds,
				injectedModelIds,
				idleAnimationId,
				walkAnimationId,
				attackAnimationId,
				defendAnimationId,
				deathAnimationId,
				miscAnimationId,
				scaleXZ,
				scaleY,
				originalColors,
				replacementColors,
				modernAttackAnims,
				modernDefendAnims,
				modernDeathAnims,
				modernMiscAnims,
				modernAttackOverrides,
				staticIdle,
				assetKeyBase,
				offsetZ,
				chatheadModelIds,
				chatheadKeyBase != null ? chatheadKeyBase : assetKeyBase,
				paintedOver
			);
		}
	}
}
