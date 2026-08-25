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

/**
 * Encapsulates retro visual model and animation data extracted from the 2004/2005 cache.
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

	public RetroNpcData(RetroNpcCategory category, int[] retroModelIds, int idleAnimationId, int walkAnimationId, int attackAnimationId, int defendAnimationId, int deathAnimationId)
	{
		this.category = category;
		this.retroModelIds = retroModelIds != null ? retroModelIds.clone() : new int[0];
		this.idleAnimationId = idleAnimationId;
		this.walkAnimationId = walkAnimationId;
		this.attackAnimationId = attackAnimationId;
		this.defendAnimationId = defendAnimationId;
		this.deathAnimationId = deathAnimationId;
	}

	public RetroNpcData(RetroNpcCategory category, int[] retroModelIds, int idleAnimationId, int walkAnimationId, int deathAnimationId)
	{
		this(category, retroModelIds, idleAnimationId, walkAnimationId, -1, -1, deathAnimationId);
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
			Arrays.equals(retroModelIds, that.retroModelIds);
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
		return result;
	}
}
