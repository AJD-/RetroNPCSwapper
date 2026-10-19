package com.retronpcswapper.chathead;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import com.retronpcswapper.chathead.ChatheadSwapper.Head;
import com.retronpcswapper.chathead.ChatheadSwapper.Swap;
import com.retronpcswapper.chathead.ChatheadSwapper.Target;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.widgets.WidgetModelType;
import org.junit.Test;

/** Ported from Custom NPC Models, less the borrowed-head cases this plugin has no use for. */
public class ChatheadSwapperTest
{
	private static final int ARIANWYN = NpcID.MOURNING_ARIANWYN_VIS;
	private static final int ARIANWYN_INDEX = 42;

	private static final Target DRAWN = Target.DRAWN;

	private static final IntUnaryOperator SINGLE = npcId -> npcId;
	private static final IntUnaryOperator ARIANWYN_AT_INDEX = index -> index == ARIANWYN_INDEX ? ARIANWYN : -1;
	private static final IntFunction<Target> NOTHING = npcId -> null;
	private static final IntFunction<Target> ARIANWYN_DRAWN = npcId -> npcId == ARIANWYN ? DRAWN : null;

	private static void assertShows(Head head, int type, int id)
	{
		assertEquals(type, head.getType());
		assertEquals(id, head.getId());
	}

	/** A head the plugin draws itself blanks the widget, so only the drawn head shows. */
	@Test
	public void testAnNpcsOwnHeadIsBlankedForADrawnOne()
	{
		Head head = ChatheadSwapper.decide(WidgetModelType.NPC_CHATHEAD, ARIANWYN, null, ARIANWYN_AT_INDEX, SINGLE,
			ARIANWYN_DRAWN);

		assertShows(head, WidgetModelType.NULL, -1);
		assertEquals(new Swap(WidgetModelType.NPC_CHATHEAD, ARIANWYN, ARIANWYN, DRAWN), head.getShown());
	}

	@Test
	public void testAHeadByNpcIndexIsSwapped()
	{
		Head head = ChatheadSwapper.decide(WidgetModelType.NPC_INDEX_CHATHEAD, ARIANWYN_INDEX, null,
			ARIANWYN_AT_INDEX, SINGLE, ARIANWYN_DRAWN);

		assertShows(head, WidgetModelType.NULL, -1);
		assertEquals(new Swap(WidgetModelType.NPC_INDEX_CHATHEAD, ARIANWYN_INDEX, ARIANWYN, DRAWN), head.getShown());
	}

	/** Elena's prison multiloc speaks as the prisoner it shows, which is what the mapping names. */
	@Test
	public void testAMultiNpcIsSwappedAsTheNpcItShows()
	{
		int prisoner = NpcID.SOTE_ELENA_PRISON;
		int multi = NpcID.ELENA_PRISON;
		Head head = ChatheadSwapper.decide(WidgetModelType.NPC_CHATHEAD, multi, null, ARIANWYN_AT_INDEX,
			npcId -> npcId == multi ? prisoner : npcId, npcId -> npcId == prisoner ? DRAWN : null);

		assertShows(head, WidgetModelType.NULL, -1);
		assertEquals(new Swap(WidgetModelType.NPC_CHATHEAD, multi, prisoner, DRAWN), head.getShown());
	}

	@Test
	public void testAMultiNpcShowingNothingIsLeftAlone()
	{
		Head head = ChatheadSwapper.decide(WidgetModelType.NPC_CHATHEAD, ARIANWYN, null, ARIANWYN_AT_INDEX,
			npcId -> -1, ARIANWYN_DRAWN);

		assertShows(head, WidgetModelType.NPC_CHATHEAD, ARIANWYN);
		assertNull(head.getShown());
	}

	/** A headless 2005 definition, an NPC that is not swapped, or one whose toggle is off. */
	@Test
	public void testAnNpcWithoutARetroHeadKeepsItsOwn()
	{
		Head head = ChatheadSwapper.decide(WidgetModelType.NPC_CHATHEAD, ARIANWYN, null, ARIANWYN_AT_INDEX, SINGLE,
			NOTHING);

		assertShows(head, WidgetModelType.NPC_CHATHEAD, ARIANWYN);
		assertNull(head.getShown());
	}

	@Test
	public void testOtherKindsOfHeadAreLeftAlone()
	{
		for (int type : new int[]{WidgetModelType.LOCAL_PLAYER_CHATHEAD, WidgetModelType.MODEL, WidgetModelType.ITEM})
		{
			Head head = ChatheadSwapper.decide(type, ARIANWYN, null, ARIANWYN_AT_INDEX, SINGLE, ARIANWYN_DRAWN);

			assertShows(head, type, ARIANWYN);
			assertNull(head.getShown());
		}
	}

	@Test
	public void testADrawnHeadIsKeptWhileTheSpeakerStillHasOne()
	{
		Swap swap = new Swap(WidgetModelType.NPC_CHATHEAD, ARIANWYN, ARIANWYN, DRAWN);

		Head head = ChatheadSwapper.decide(WidgetModelType.NULL, -1, swap, ARIANWYN_AT_INDEX, SINGLE, ARIANWYN_DRAWN);

		assertShows(head, WidgetModelType.NULL, -1);
		assertSame("kept, so the emote clock is not restarted", swap, head.getShown());
	}

	/** The toggle turned off, the assets toggle turned off, or a safety setting stepping in. */
	@Test
	public void testADrawnHeadGoesBackWhenTheSpeakerNoLongerHasOne()
	{
		Swap swap = new Swap(WidgetModelType.NPC_CHATHEAD, ARIANWYN, ARIANWYN, DRAWN);

		Head head = ChatheadSwapper.decide(WidgetModelType.NULL, -1, swap, ARIANWYN_AT_INDEX, SINGLE, NOTHING);

		assertShows(head, WidgetModelType.NPC_CHATHEAD, ARIANWYN);
		assertNull(head.getShown());
	}

	/** A head shown by scene index goes back to the index, not to an NPC id. */
	@Test
	public void testAnIndexHeadGoesBackToItsIndex()
	{
		Swap swap = new Swap(WidgetModelType.NPC_INDEX_CHATHEAD, ARIANWYN_INDEX, ARIANWYN, DRAWN);

		Head head = ChatheadSwapper.decide(WidgetModelType.NULL, -1, swap, ARIANWYN_AT_INDEX, SINGLE, NOTHING);

		assertShows(head, WidgetModelType.NPC_INDEX_CHATHEAD, ARIANWYN_INDEX);
		assertNull(head.getShown());
	}

	/** Each new page sets the NPC's own head again: a new swap, so the drawn emote starts over. */
	@Test
	public void testANewPageIsANewSwap()
	{
		Swap swap = new Swap(WidgetModelType.NPC_CHATHEAD, ARIANWYN, ARIANWYN, DRAWN);

		Head head = ChatheadSwapper.decide(WidgetModelType.NPC_CHATHEAD, ARIANWYN, swap, ARIANWYN_AT_INDEX, SINGLE,
			ARIANWYN_DRAWN);

		assertShows(head, WidgetModelType.NULL, -1);
		assertEquals(swap, head.getShown());
		assertNotSame("a fresh swap", swap, head.getShown());
	}

	/** The dialogue moves on to another speaker without closing: that head is not touched. */
	@Test
	public void testAnotherSpeakersHeadIsLeftAlone()
	{
		Swap swap = new Swap(WidgetModelType.NPC_CHATHEAD, ARIANWYN, ARIANWYN, DRAWN);
		int other = NpcID.ELENA2_VIS;

		Head head = ChatheadSwapper.decide(WidgetModelType.NPC_CHATHEAD, other, swap, ARIANWYN_AT_INDEX, SINGLE,
			ARIANWYN_DRAWN);

		assertShows(head, WidgetModelType.NPC_CHATHEAD, other);
		assertNull(head.getShown());
	}
}
