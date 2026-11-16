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
package com.retronpcswapper.chathead;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.AnimationID;

/**
 * The dialogue emotes. Each comes in four lengths, one per line of text, and a goblin has a
 * single emote of its own in the same four.
 */
final class ChatEmotes
{
	private static final int[] GOBLIN = {
		AnimationID.CHATGOBLIN1, AnimationID.CHATGOBLIN2, AnimationID.CHATGOBLIN3, AnimationID.CHATGOBLIN4};

	/** The human emotes, each shortest first. */
	private static final int[][] HUMAN = {
		{AnimationID.CHATQUIZ1, AnimationID.CHATQUIZ2, AnimationID.CHATQUIZ3, AnimationID.CHATQUIZ4},
		{AnimationID.CHATBORED1, AnimationID.CHATBORED2, AnimationID.CHATBORED3, AnimationID.CHATBORED4},
		{AnimationID.CHATHAP1, AnimationID.CHATHAP2, AnimationID.CHATHAP3, AnimationID.CHATHAP4},
		{AnimationID.CHATSHOCK1, AnimationID.CHATSHOCK2, AnimationID.CHATSHOCK3, AnimationID.CHATSHOCK4},
		{AnimationID.CHATCON1, AnimationID.CHATCON2, AnimationID.CHATCON3, AnimationID.CHATCON4},
		{AnimationID.CHATENT1, AnimationID.CHATENT2, AnimationID.CHATENT3, AnimationID.CHATENT4},
		{AnimationID.CHATNEU1, AnimationID.CHATNEU2, AnimationID.CHATNEU3, AnimationID.CHATNEU4},
		{AnimationID.CHATSHIFTY1, AnimationID.CHATSHIFTY2, AnimationID.CHATSHIFTY3, AnimationID.CHATSHIFTY4},
		{AnimationID.CHATSCARED1, AnimationID.CHATSCARED2, AnimationID.CHATSCARED3, AnimationID.CHATSCARED4},
		{AnimationID.CHATDRUNK1, AnimationID.CHATDRUNK2, AnimationID.CHATDRUNK3, AnimationID.CHATDRUNK4},
		{AnimationID.CHATLAUGH1, AnimationID.CHATLAUGH2, AnimationID.CHATLAUGH3, AnimationID.CHATLAUGH4},
		{AnimationID.CHATSAD1, AnimationID.CHATSAD2, AnimationID.CHATSAD3, AnimationID.CHATSAD4},
		{AnimationID.CHATANG1, AnimationID.CHATANG2, AnimationID.CHATANG3, AnimationID.CHATANG4}};

	private static final Map<Integer, Integer> GOBLIN_FOR_HUMAN = new HashMap<>();

	static
	{
		for (int[] emote : HUMAN)
		{
			for (int length = 0; length < emote.length; length++)
			{
				GOBLIN_FOR_HUMAN.put(emote[length], GOBLIN[length]);
			}
		}
	}

	private ChatEmotes()
	{
	}

	/** The goblin emote as long as a human one. Any other emote is returned as it is. */
	static int goblinFor(int emote)
	{
		return GOBLIN_FOR_HUMAN.getOrDefault(emote, emote);
	}
}
