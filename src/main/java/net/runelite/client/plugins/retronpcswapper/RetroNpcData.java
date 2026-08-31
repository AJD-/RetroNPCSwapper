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
package net.runelite.client.plugins.retronpcswapper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Encapsulates retro visual model and animation data extracted from the 2004/2005 cache,
 * along with category-scoped sets of modern animation IDs to intercept.
 */
public class RetroNpcData
{
	private final RetroNpcCategory category;

	/**
	 * Model IDs from the 2004/2005 RuneScape cache.
	 */
	private final int[] retroModelIds;

	/**
	 * Idle (standing) animation sequence ID.
	 * Set to -1 if preserving default NPC idle animation.
	 */
	private final int idleAnimationId;

	/**
	 * Walking animation sequence ID.
	 * Set to -1 if preserving default NPC walk animation.
	 */
	private final int walkAnimationId;

	/**
	 * Attack animation sequence ID.
	 * Set to -1 if preserving default NPC attack animation.
	 */
	private final int attackAnimationId;

	/**
	 * Defend / block animation sequence ID.
	 * Set to -1 if preserving default NPC block animation.
	 */
	private final int defendAnimationId;

	/**
	 * Death animation sequence ID.
	 * Set to -1 if preserving default NPC death animation.
	 */
	private final int deathAnimationId;

	/**
	 * Modern attack animation IDs to intercept and swap for this NPC category.
	 */
	private final Set<Integer> modernAttackAnims;

	/**
	 * Modern defend / hit animation IDs to intercept and swap for this NPC category.
	 */
	private final Set<Integer> modernDefendAnims;

	/**
	 * Modern death animation IDs to intercept and swap for this NPC category.
	 */
	private final Set<Integer> modernDeathAnims;

	public RetroNpcData(
		RetroNpcCategory category,
		int[] retroModelIds,
		int idleAnimationId,
		int walkAnimationId,
		int attackAnimationId,
		int defendAnimationId,
		int deathAnimationId,
		Set<Integer> modernAttackAnims,
		Set<Integer> modernDefendAnims,
		Set<Integer> modernDeathAnims
	)
	{
		this.category = category;
		this.retroModelIds = retroModelIds != null ? retroModelIds.clone() : new int[0];
		this.idleAnimationId = idleAnimationId;
		this.walkAnimationId = walkAnimationId;
		this.attackAnimationId = attackAnimationId;
		this.defendAnimationId = defendAnimationId;
		this.deathAnimationId = deathAnimationId;
		this.modernAttackAnims = modernAttackAnims != null
			? Collections.unmodifiableSet(new HashSet<>(modernAttackAnims))
			: Collections.emptySet();
		this.modernDefendAnims = modernDefendAnims != null
			? Collections.unmodifiableSet(new HashSet<>(modernDefendAnims))
			: Collections.emptySet();
		this.modernDeathAnims = modernDeathAnims != null
			? Collections.unmodifiableSet(new HashSet<>(modernDeathAnims))
			: Collections.emptySet();
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public boolean isAttackAnimation(int animId)
	{
		return attackAnimationId != -1 && modernAttackAnims.contains(animId);
	}

	public boolean isDefendAnimation(int animId)
	{
		return defendAnimationId != -1 && modernDefendAnims.contains(animId);
	}

	public boolean isDeathAnimation(int animId)
	{
		return deathAnimationId != -1 && modernDeathAnims.contains(animId);
	}

	public RetroNpcCategory getCategory()
	{
		return category;
	}

	public int[] getRetroModelIds()
	{
		return retroModelIds.clone();
	}

	public int getIdleAnimationId()
	{
		return idleAnimationId;
	}

	public int getWalkAnimationId()
	{
		return walkAnimationId;
	}

	public int getAttackAnimationId()
	{
		return attackAnimationId;
	}

	public int getDefendAnimationId()
	{
		return defendAnimationId;
	}

	public int getDeathAnimationId()
	{
		return deathAnimationId;
	}

	public Set<Integer> getModernAttackAnims()
	{
		return modernAttackAnims;
	}

	public Set<Integer> getModernDefendAnims()
	{
		return modernDefendAnims;
	}

	public Set<Integer> getModernDeathAnims()
	{
		return modernDeathAnims;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RetroNpcData that = (RetroNpcData) o;
		return idleAnimationId == that.idleAnimationId &&
			walkAnimationId == that.walkAnimationId &&
			attackAnimationId == that.attackAnimationId &&
			defendAnimationId == that.defendAnimationId &&
			deathAnimationId == that.deathAnimationId &&
			category == that.category &&
			Arrays.equals(retroModelIds, that.retroModelIds) &&
			Objects.equals(modernAttackAnims, that.modernAttackAnims) &&
			Objects.equals(modernDefendAnims, that.modernDefendAnims) &&
			Objects.equals(modernDeathAnims, that.modernDeathAnims);
	}

	@Override
	public int hashCode()
	{
		int result = category != null ? category.hashCode() : 0;
		result = 31 * result + Arrays.hashCode(retroModelIds);
		result = 31 * result + idleAnimationId;
		result = 31 * result + walkAnimationId;
		result = 31 * result + attackAnimationId;
		result = 31 * result + defendAnimationId;
		result = 31 * result + deathAnimationId;
		result = 31 * result + (modernAttackAnims != null ? modernAttackAnims.hashCode() : 0);
		result = 31 * result + (modernDefendAnims != null ? modernDefendAnims.hashCode() : 0);
		result = 31 * result + (modernDeathAnims != null ? modernDeathAnims.hashCode() : 0);
		return result;
	}

	public static class Builder
	{
		private RetroNpcCategory category;
		private int[] retroModelIds = new int[0];
		private int idleAnimationId = -1;
		private int walkAnimationId = -1;
		private int attackAnimationId = -1;
		private int defendAnimationId = -1;
		private int deathAnimationId = -1;
		private final Set<Integer> modernAttackAnims = new HashSet<>();
		private final Set<Integer> modernDefendAnims = new HashSet<>();
		private final Set<Integer> modernDeathAnims = new HashSet<>();

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

		public RetroNpcData build()
		{
			return new RetroNpcData(
				category,
				retroModelIds,
				idleAnimationId,
				walkAnimationId,
				attackAnimationId,
				defendAnimationId,
				deathAnimationId,
				modernAttackAnims,
				modernDefendAnims,
				modernDeathAnims
			);
		}
	}
}
