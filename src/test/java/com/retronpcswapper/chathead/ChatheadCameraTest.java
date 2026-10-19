package com.retronpcswapper.chathead;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ChatheadCameraTest
{
	private static double[] project(ChatheadCamera camera, double x, double y, double z)
	{
		double[] out = new double[3];
		camera.project(x, y, z, out);
		return out;
	}

	/** Looking straight on, a head's origin is the center of its widget, zoom away. */
	@Test
	public void testStraightOnTheOriginIsTheCenter()
	{
		double[] p = project(new ChatheadCamera(0, 0, 0, 800), 0, 0, 0);

		assertEquals(0, p[0], 1e-9);
		assertEquals(0, p[1], 1e-9);
		assertEquals(800, p[2], 1e-9);
	}

	/** A quarter turn of yaw (512 of 2048) swings a point on +x round onto the view axis. */
	@Test
	public void testYawTurnsAboutTheVerticalAxis()
	{
		double[] p = project(new ChatheadCamera(0, 0, 512, 800), 100, 0, 0);

		assertEquals(0, p[0], 1e-6);
		assertEquals(700, p[2], 1e-6);
	}

	/** Farther points are drawn smaller. */
	@Test
	public void testPerspective()
	{
		double[] near = project(new ChatheadCamera(0, 0, 0, 400), 50, 0, 0);
		double[] far = project(new ChatheadCamera(0, 0, 0, 800), 50, 0, 0);

		assertEquals(near[0] / 2, far[0], 1e-9);
	}
}
