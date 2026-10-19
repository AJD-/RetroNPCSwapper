package com.retronpcswapper.chathead;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.retronpcswapper.inject.RetroMesh;
import java.awt.image.BufferedImage;
import org.junit.Test;

/** Ported from Custom NPC Models. */
public class HeadRendererTest
{
	private static BufferedImage render(RetroHead head, int width, int height)
	{
		RetroMesh mesh = head.getMesh();
		return new HeadRenderer().render(head, mesh.getVerticesX(), mesh.getVerticesY(), mesh.getVerticesZ(),
			new ChatheadCamera(0, 0, 0, 600), width, height);
	}

	@Test
	public void testDrawsTheHeadOpaqueAndLeavesTheRestClear()
	{
		BufferedImage image = render(TestHeads.triangle(), 100, 100);

		assertEquals("the triangle's middle is drawn", 0xFF, image.getRGB(50, 50) >>> 24);
		assertEquals("the corners are clear", 0, image.getRGB(0, 0) >>> 24);
	}

	/** A widget resized by the chatbox or client scaling is drawn at its new size every frame. */
	@Test
	public void testRendersAtTheSizeAsked()
	{
		HeadRenderer renderer = new HeadRenderer();
		RetroHead head = TestHeads.triangle();
		RetroMesh mesh = head.getMesh();
		ChatheadCamera camera = new ChatheadCamera(0, 0, 0, 600);

		BufferedImage small = renderer.render(head, mesh.getVerticesX(), mesh.getVerticesY(), mesh.getVerticesZ(),
			camera, 60, 40);
		assertEquals(60, small.getWidth());
		assertEquals(40, small.getHeight());
		BufferedImage large = renderer.render(head, mesh.getVerticesX(), mesh.getVerticesY(), mesh.getVerticesZ(),
			camera, 120, 90);
		assertEquals(120, large.getWidth());
		assertEquals(90, large.getHeight());
		assertTrue("the larger image is drawn too", (large.getRGB(60, 45) >>> 24) == 0xFF);
	}

	/**
	 * The client draws a chathead centered on its widget but past the widget's edges, as far as the
	 * dialogue reaches, so the head is drawn into the dialogue's bounds around the widget's center.
	 */
	@Test
	public void testDrawsAroundTheCenterAsked()
	{
		RetroHead head = TestHeads.triangle();
		RetroMesh mesh = head.getMesh();

		BufferedImage image = new HeadRenderer().render(head, mesh.getVerticesX(), mesh.getVerticesY(),
			mesh.getVerticesZ(), new ChatheadCamera(0, 0, 0, 600), 300, 120, 60, 60);

		assertEquals("the triangle is drawn around the given center", 0xFF, image.getRGB(60, 60) >>> 24);
		assertEquals("and not around the image's middle", 0, image.getRGB(150, 60) >>> 24);
	}

	/** The player's brightness setting lightens the head as it lightens everything the client draws. */
	@Test
	public void testDrawsAtThePlayersBrightness()
	{
		RetroHead head = TestHeads.triangle();
		RetroMesh mesh = head.getMesh();
		ChatheadCamera camera = new ChatheadCamera(0, 0, 0, 600);

		int plain = new HeadRenderer().render(head, mesh.getVerticesX(), mesh.getVerticesY(), mesh.getVerticesZ(),
			camera, 100, 100, 50, 50, 1.0).getRGB(50, 50);
		int bright = new HeadRenderer().render(head, mesh.getVerticesX(), mesh.getVerticesY(), mesh.getVerticesZ(),
			camera, 100, 100, 50, 50, 0.6).getRGB(50, 50);

		assertTrue("brighter at 0.6: " + Integer.toHexString(plain) + " vs " + Integer.toHexString(bright),
			(bright >> 8 & 255) > (plain >> 8 & 255));
	}

	/** The palette's endpoints: luminance 0 is black under any hue, and full luminance near white. */
	@Test
	public void testThePaletteEndpoints()
	{
		assertEquals(0, HeadRenderer.hslToRgb(0, 1));
		assertEquals(0, HeadRenderer.hslToRgb(63 << 10 | 7 << 7, 1));
		int white = HeadRenderer.hslToRgb(127, 1);
		assertTrue(Integer.toHexString(white), (white >> 16 & 255) > 250 && (white & 255) > 250);
	}
}
