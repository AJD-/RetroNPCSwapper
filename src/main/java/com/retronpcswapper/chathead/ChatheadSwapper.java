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

import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.WorldView;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetModelType;

/**
 * Shows a swapped NPC's 2005 chathead in dialogue, in place of its modern one. Ported from Custom NPC
 * Models' chathead swapper, keeping only its drawn-head path.
 * <p>
 * The dialogue head is a widget the client draws from the cache, and a 2005 head is geometry the
 * cache no longer holds - nor can a plugin-built model be handed to the client's widget drawing,
 * which casts to its own model class. So the widget is blanked, and {@link ChatheadOverlay} draws
 * the head over it.
 * <p>
 * Checked every frame rather than on a script event, so a page that opens with the NPC's own head is
 * swapped before it is first drawn, and every new page, which sets the head again, is swapped too.
 * Client thread only.
 */
@Singleton
public class ChatheadSwapper
{
	private final Client client;
	private final ChatheadOverlay overlay;

	/**
	 * The 2005 head to draw for an NPC id, or null to leave its own. The plugin decides, since whether
	 * an NPC is swapped at all is its call. Set from startUp, read on the client thread.
	 */
	private volatile IntFunction<RetroHead> heads = npcId -> null;

	/** The swap the head is showing, or null while it shows what the client gave it. */
	private Swap shown;

	@Inject
	ChatheadSwapper(Client client, ChatheadOverlay overlay)
	{
		this.client = client;
		this.overlay = overlay;
	}

	/** What the head is swapped to. Only ever {@link #DRAWN}: the widget blanked, the overlay over it. */
	@Value
	static class Target
	{
		static final Target DRAWN = new Target(WidgetModelType.NULL, -1);

		int type;
		int id;
	}

	/** What the head showed before a swap, whose head it is, and what it was swapped to. */
	@Value
	static class Swap
	{
		int type;
		int id;
		int speaker;
		Target target;
	}

	/** What the head should show, and the swap that leaves it showing, if any. */
	@Value
	static class Head
	{
		int type;
		int id;
		Swap shown;
	}

	/** Swaps from now on, drawing whatever {@code heads} returns for a speaker. */
	public void start(IntFunction<RetroHead> heads)
	{
		this.heads = heads;
	}

	/**
	 * Puts back the head a dialogue still open would have shown, and forgets the swap. Runs on the
	 * client thread after shutDown, which can be after the next startUp, so it leaves {@link #heads}
	 * alone: the plugin no longer forwards frames to a stopped swapper anyway.
	 */
	public void stop()
	{
		Widget head = client.getWidget(InterfaceID.ChatLeft.HEAD);
		if (head != null && shown != null && head.getModelType() == shown.getTarget().getType()
			&& head.getModelId() == shown.getTarget().getId())
		{
			head.setModelType(shown.getType());
			head.setModelId(shown.getId());
		}
		overlay.clear();
		shown = null;
	}

	/** Called by the plugin on every BeforeRender. */
	public void onBeforeRender()
	{
		Widget head = client.getWidget(InterfaceID.ChatLeft.HEAD);
		if (head == null)
		{
			shown = null;
			overlay.hide();
			return;
		}
		if (head.isHidden())
		{
			// Hidden, not gone: it still shows the swap when it comes back, so the swap is kept
			overlay.hide();
			return;
		}

		int type = head.getModelType();
		int id = head.getModelId();
		Swap previous = shown;
		Head next = decide(type, id, shown, this::speakerAt, this::resolve, this::targetFor);
		shown = next.getShown();
		if (next.getType() != type || next.getId() != id)
		{
			head.setModelType(next.getType());
			head.setModelId(next.getId());
		}

		if (shown == null)
		{
			overlay.hide();
		}
		else if (shown != previous)
		{
			// A new swap, a new page or a new speaker: the drawn head's emote starts over
			overlay.show(heads.apply(shown.getSpeaker()), client.getGameCycle());
		}
	}

	/**
	 * What a dialogue head showing {@code type} and {@code id} should show instead.
	 * <p>
	 * A head still showing the last swap keeps it while the speaker still has a 2005 head to draw,
	 * and goes back to what it was once it doesn't (the toggle turned off, a safety setting stepping
	 * in). Anything else the head shows was set by the client, so it is looked at afresh: an NPC's own
	 * head, by id or by the index of the NPC in the scene, is swapped when the NPC has a 2005 head,
	 * and every other kind of head is left alone.
	 *
	 * @param speakerAt the NPC id at a scene index, or -1 when there is none
	 * @param resolve   the NPC a multi NPC shows as, -1 for none; any other NPC is itself
	 * @param targetFor what an NPC id's head is swapped to, or null to keep its own
	 */
	static Head decide(int type, int id, Swap shown, IntUnaryOperator speakerAt, IntUnaryOperator resolve,
		IntFunction<Target> targetFor)
	{
		if (shown != null && type == shown.getTarget().getType() && id == shown.getTarget().getId())
		{
			Target target = targetFor.apply(shown.getSpeaker());
			if (shown.getTarget().equals(target))
			{
				return new Head(type, id, shown);
			}
			if (target == null)
			{
				return new Head(shown.getType(), shown.getId(), null);
			}
			return new Head(target.getType(), target.getId(),
				new Swap(shown.getType(), shown.getId(), shown.getSpeaker(), target));
		}

		int speaker = type == WidgetModelType.NPC_CHATHEAD ? id
			: type == WidgetModelType.NPC_INDEX_CHATHEAD ? speakerAt.applyAsInt(id)
			: -1;
		// The dialogue may name a multi NPC, which is mapped by the NPC it shows as
		if (speaker >= 0)
		{
			speaker = resolve.applyAsInt(speaker);
		}
		Target target = speaker < 0 ? null : targetFor.apply(speaker);
		if (target == null || (type == target.getType() && id == target.getId()))
		{
			return new Head(type, id, null);
		}
		return new Head(target.getType(), target.getId(), new Swap(type, id, speaker, target));
	}

	/** The NPC a multi NPC shows as right now, -1 for none, or the NPC itself when it isn't one. */
	private int resolve(int npcId)
	{
		NPCComposition npc = client.getNpcDefinition(npcId);
		if (npc == null || npc.getConfigs() == null)
		{
			return npcId;
		}
		NPCComposition shows = npc.transform();
		return shows == null ? -1 : shows.getId();
	}

	private int speakerAt(int index)
	{
		WorldView worldView = client.getTopLevelWorldView();
		NPC npc = worldView == null ? null : worldView.npcs().byIndex(index);
		return npc == null ? -1 : npc.getId();
	}

	private Target targetFor(int npcId)
	{
		return heads.apply(npcId) == null ? null : Target.DRAWN;
	}
}
