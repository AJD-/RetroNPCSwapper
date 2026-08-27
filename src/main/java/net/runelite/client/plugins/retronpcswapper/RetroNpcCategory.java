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

/**
 * NPC classification category for Retro NPC Swapper config toggles.
 */
public enum RetroNpcCategory
{
	LESSER_DEMONS("Lesser_Demons"),
	GREATER_DEMONS("Greater_Demons"),
	BLACK_DEMONS("Black_Demons"),
	ADULT_DRAGONS("Adult_Dragons"),
	BABY_DRAGONS("Baby_Dragons"),
	GOBLINS("Goblins"),
	GUARDS("Guards"),
	IMPS("Imps"),
	SKELETONS("Skeletons"),
	ZOMBIES("Zombies"),
	GHOSTS("Ghosts"),
	HILL_GIANTS("Hill_Giants"),
	MOSS_GIANTS("Moss_Giants"),
	FIRE_GIANTS("Fire_Giants"),
	CHICKENS("Chickens");

	private final String name;

	RetroNpcCategory(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}
}
