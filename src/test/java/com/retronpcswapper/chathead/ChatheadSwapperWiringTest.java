package com.retronpcswapper.chathead;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetModelType;
import org.junit.Before;
import org.junit.Test;

/** ChatheadSwapper frame to frame: what it leaves the dialogue head widget showing. Ported from Custom NPC Models. */
public class ChatheadSwapperWiringTest
{
	private static final int ARIANWYN = NpcID.MOURNING_ARIANWYN_VIS;

	/** The widget's model type and id, which the client and the swapper both set. */
	private final int[] shows = {WidgetModelType.NPC_CHATHEAD, ARIANWYN};
	private final boolean[] hidden = {false};

	/** Whether the plugin would hand over a head right now: the toggles and safety settings, in one. */
	private final boolean[] swapping = {true};

	private final RetroHead head = TestHeads.triangle();
	private ChatheadSwapper swapper;

	@Before
	public void setUp()
	{
		Widget widget = mock(Widget.class);
		when(widget.getModelType()).thenAnswer(call -> shows[0]);
		when(widget.getModelId()).thenAnswer(call -> shows[1]);
		when(widget.isHidden()).thenAnswer(call -> hidden[0]);
		when(widget.setModelType(anyInt())).thenAnswer(call ->
		{
			shows[0] = call.getArgument(0);
			return widget;
		});
		when(widget.setModelId(anyInt())).thenAnswer(call ->
		{
			shows[1] = call.getArgument(0);
			return widget;
		});

		Client client = mock(Client.class);
		when(client.getWidget(InterfaceID.ChatLeft.HEAD)).thenReturn(widget);

		swapper = new ChatheadSwapper(client, new ChatheadOverlay(client));
	}

	private RetroHead headFor(int npcId)
	{
		return swapping[0] && npcId == ARIANWYN ? head : null;
	}

	private void assertShows(int type, int id)
	{
		assertEquals("model type", type, shows[0]);
		assertEquals("model id", id, shows[1]);
	}

	/**
	 * The head hidden and shown again on the same page (the chatbox hidden and shown) still shows the
	 * swap, so it is still the swapper's to put back.
	 */
	@Test
	public void testAHeadHiddenAndShownAgainKeepsItsSwap()
	{
		swapper.start(this::headFor);
		swapper.onBeforeRender();
		assertShows(WidgetModelType.NULL, -1);

		hidden[0] = true;
		swapper.onBeforeRender();
		hidden[0] = false;
		swapper.onBeforeRender();
		swapping[0] = false;
		swapper.onBeforeRender();

		assertShows(WidgetModelType.NPC_CHATHEAD, ARIANWYN);
	}

	/** Stopping mid-dialogue puts the NPC's own head back. */
	@Test
	public void testStopPutsTheOwnHeadBack()
	{
		swapper.start(this::headFor);
		swapper.onBeforeRender();
		assertShows(WidgetModelType.NULL, -1);

		swapper.stop();

		assertShows(WidgetModelType.NPC_CHATHEAD, ARIANWYN);
	}

	/**
	 * A plugin switched off and straight back on: its stop runs on the client thread after the new
	 * start, and must not switch swapping off behind it.
	 */
	@Test
	public void testAStopQueuedBehindARestartLeavesItSwapping()
	{
		swapper.start(this::headFor);
		swapper.start(this::headFor);
		swapper.stop();

		swapper.onBeforeRender();

		assertShows(WidgetModelType.NULL, -1);
	}
}
