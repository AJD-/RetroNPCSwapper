package com.retronpcswapper.chathead;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import net.runelite.api.Animation;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import org.junit.Before;
import org.junit.Test;

/**
 * ChatheadOverlay draws every frame, but renders the head again only when what it shows changes.
 * Ported from Custom NPC Models.
 */
public class ChatheadOverlayTest
{
	private static final int EMOTE = 554;

	/** Counts the renders that reach the rasterizer. */
	private static final class CountingRenderer extends HeadRenderer
	{
		int renders;

		@Override
		public BufferedImage render(RetroHead head, float[] x, float[] y, float[] z, ChatheadCamera camera,
			int width, int height, double centerX, double centerY, double brightness)
		{
			renders++;
			return super.render(head, x, y, z, camera, width, height, centerX, centerY, brightness);
		}
	}

	private final int[] cycle = {0};
	private final CountingRenderer renderer = new CountingRenderer();
	private final Graphics2D graphics = mock(Graphics2D.class);
	private Widget widget;
	private ChatheadOverlay overlay;

	@Before
	public void setUp()
	{
		widget = mock(Widget.class);
		when(widget.getBounds()).thenReturn(new Rectangle(10, 20, 40, 40));
		when(widget.getAnimationId()).thenReturn(EMOTE);
		when(widget.getRotationX()).thenReturn(40);
		when(widget.getRotationZ()).thenReturn(1882);
		when(widget.getModelZoom()).thenReturn(796);

		Animation emote = mock(Animation.class);
		when(emote.getFrameLengths()).thenReturn(new int[]{5, 5});

		Client client = mock(Client.class);
		when(client.getWidget(InterfaceID.ChatLeft.HEAD)).thenReturn(widget);
		when(client.getGameCycle()).thenAnswer(call -> cycle[0]);
		when(client.loadAnimation(EMOTE)).thenReturn(emote);

		overlay = new ChatheadOverlay(client, renderer);
		overlay.show(TestHeads.triangle(), 0);
	}

	@Test
	public void testAnUnchangedFrameIsNotRenderedAgain()
	{
		overlay.render(graphics);
		overlay.render(graphics);
		cycle[0] = 2;
		overlay.render(graphics);

		assertEquals(1, renderer.renders);
	}

	@Test
	public void testTheNextEmoteFrameIsRendered()
	{
		overlay.render(graphics);
		cycle[0] = 5;
		overlay.render(graphics);

		assertEquals(2, renderer.renders);
	}

	@Test
	public void testAMovedWidgetIsRenderedAgain()
	{
		overlay.render(graphics);
		when(widget.getBounds()).thenReturn(new Rectangle(10, 20, 60, 60));
		overlay.render(graphics);

		assertEquals(2, renderer.renders);
	}

	@Test
	public void testAHiddenHeadDrawsNothing()
	{
		overlay.hide();
		overlay.render(graphics);

		assertEquals(0, renderer.renders);
	}
}
