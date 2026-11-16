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

import com.retronpcswapper.RetroNpcSwapperPlugin;
import com.retronpcswapper.inject.RetroAssetBundle;
import com.retronpcswapper.inject.RetroAssetCodec;
import com.retronpcswapper.inject.RetroClip;
import java.io.InputStream;
import net.runelite.api.gameval.AnimationID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RetroHeadTest
{
	private static final int GOBLIN = RetroAssetBundle.GOBLIN_CHATHEAD_KEY_BASE;

	/** The first of each human emote's four lengths. */
	private static final int[] HUMAN_EMOTES = {
		AnimationID.CHATQUIZ1, AnimationID.CHATBORED1, AnimationID.CHATHAP1, AnimationID.CHATSHOCK1,
		AnimationID.CHATCON1, AnimationID.CHATENT1, AnimationID.CHATNEU1, AnimationID.CHATSHIFTY1,
		AnimationID.CHATSCARED1, AnimationID.CHATDRUNK1, AnimationID.CHATLAUGH1, AnimationID.CHATSAD1,
		AnimationID.CHATANG1};

	private static RetroHead head(int keyBase, RetroAssetBundle bundle)
	{
		return new RetroHead(TestHeads.triangle().getMesh(), keyBase, bundle, null, null, null);
	}

	@Test
	public void testAGoblinHeadAnswersAHumanEmoteWithItsOwnOfTheSameLength()
	{
		RetroHead goblin = head(GOBLIN, RetroAssetBundle.empty());

		// The live game asks goblins for the human emotes, which their head rig has no clips for
		assertEquals(AnimationID.CHATGOBLIN1, goblin.emoteFor(AnimationID.CHATNEU1));
		assertEquals(AnimationID.CHATGOBLIN2, goblin.emoteFor(AnimationID.CHATCON2));
		assertEquals(AnimationID.CHATGOBLIN3, goblin.emoteFor(AnimationID.CHATHAP3));
		assertEquals(AnimationID.CHATGOBLIN4, goblin.emoteFor(AnimationID.CHATANG4));
	}

	@Test
	public void testAGoblinHeadKeepsItsOwnAndUnknownEmotes()
	{
		RetroHead goblin = head(GOBLIN, RetroAssetBundle.empty());

		assertEquals(AnimationID.CHATGOBLIN3, goblin.emoteFor(AnimationID.CHATGOBLIN3));
		// A modern idle emote has no 2005 counterpart, so the head still rests through it
		assertEquals(13949, goblin.emoteFor(13949));
		assertEquals(-1, goblin.emoteFor(-1));
	}

	@Test
	public void testAHumanHeadPlaysTheEmoteItIsAsked()
	{
		assertEquals(AnimationID.CHATNEU1, head(0, RetroAssetBundle.empty()).emoteFor(AnimationID.CHATNEU1));
		assertEquals(AnimationID.CHATHAP3,
			head(RetroAssetBundle.NOV_HUMAN_KEY_BASE, RetroAssetBundle.empty()).emoteFor(AnimationID.CHATHAP3));
	}

	@Test
	public void testEveryHumanEmoteFindsAGoblinClipInTheBundle() throws Exception
	{
		RetroAssetBundle bundle;
		try (InputStream in = RetroNpcSwapperPlugin.class.getResourceAsStream("retro-assets.dat"))
		{
			assertNotNull("retro-assets.dat is missing - run ./gradlew generateRetroAssets", in);
			bundle = RetroAssetCodec.read(in);
		}
		RetroHead goblin = head(GOBLIN, bundle);

		for (int first : HUMAN_EMOTES)
		{
			for (int length = 0; length < 4; length++)
			{
				int emote = first + length;
				RetroClip clip = goblin.clip(goblin.emoteFor(emote));
				assertNotNull("goblin clip for human emote " + emote, clip);
				// Same length variant, so the mouth stops with the text. A clip is named by its bundle key
				assertEquals("emote " + emote, GOBLIN + AnimationID.CHATGOBLIN1 + length, clip.getSequenceId());
			}
		}
	}
}
