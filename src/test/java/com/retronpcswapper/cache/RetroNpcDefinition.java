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
package com.retronpcswapper.cache;

import java.util.Arrays;

public class RetroNpcDefinition
{
	private int id;
	private String name;
	private String description;
	private int size = 1;
	private int[] models;
	private int[] additionalModels;
	private int stanceAnimation = -1;
	private int walkAnimation = -1;
	private int turnAroundAnimation = -1;
	private int turnLeftAnimation = -1;
	private int turnRightAnimation = -1;
	private int combatLevel = -1;
	private String[] actions = new String[5];
	private short[] originalColors;
	private short[] replacementColors;

	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public int getSize()
	{
		return size;
	}

	public void setSize(int size)
	{
		this.size = size;
	}

	public int[] getModels()
	{
		return models != null ? models.clone() : new int[0];
	}

	public void setModels(int[] models)
	{
		this.models = models;
	}

	public int[] getAdditionalModels()
	{
		return additionalModels != null ? additionalModels.clone() : new int[0];
	}

	public void setAdditionalModels(int[] additionalModels)
	{
		this.additionalModels = additionalModels;
	}

	public int getStanceAnimation()
	{
		return stanceAnimation;
	}

	public void setStanceAnimation(int stanceAnimation)
	{
		this.stanceAnimation = stanceAnimation;
	}

	public int getWalkAnimation()
	{
		return walkAnimation;
	}

	public void setWalkAnimation(int walkAnimation)
	{
		this.walkAnimation = walkAnimation;
	}

	public int getCombatLevel()
	{
		return combatLevel;
	}

	public void setCombatLevel(int combatLevel)
	{
		this.combatLevel = combatLevel;
	}

	public String[] getActions()
	{
		return actions != null ? actions.clone() : new String[5];
	}

	public void setActions(String[] actions)
	{
		this.actions = actions;
	}

	@Override
	public String toString()
	{
		return "RetroNpcDefinition{" +
			"id=" + id +
			", name='" + name + '\'' +
			", combatLevel=" + combatLevel +
			", models=" + Arrays.toString(models) +
			", stanceAnimation=" + stanceAnimation +
			", walkAnimation=" + walkAnimation +
			'}';
	}
}
