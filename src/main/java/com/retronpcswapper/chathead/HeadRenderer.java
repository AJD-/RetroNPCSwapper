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

import com.retronpcswapper.inject.RetroLighter;
import com.retronpcswapper.inject.RetroMesh;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Draws a posed chathead into an image the size of the dialogue head widget, clear wherever the
 * head is not: z-buffered and Gouraud shaded, opaque faces first, then see-through ones farthest
 * first, as the client orders them. Ported from Custom NPC Models' chathead renderer.
 * <p>
 * Textured faces draw in the mid grey {@link RetroLighter} shades them from; none of the 2005 heads
 * bundled has a texture, which the generator reports per spec.
 * <p>
 * The image and depth buffer are reused while the size holds, and each face's colors are worked out
 * once per head and brightness, as they never change in between. Client thread only.
 */
public class HeadRenderer
{
	/** The lighter's corner sentinels. */
	private static final int FLAT_SHADED = RetroLighter.FLAT_SHADED;
	private static final int HIDDEN = RetroLighter.HIDDEN;

	/** A face's transparency byte at which nothing of it is drawn: alpha is 1 - t / 255. */
	private static final int FULLY_TRANSPARENT = 255;

	private static final int OPAQUE = 0xFF000000;
	private static final double NEAR = 1;

	private BufferedImage image;
	private float[] depth = new float[0];
	private final double[] projected = new double[3];

	// Each face's three corner colors as RGB, for the head and brightness they were worked out for
	private RetroHead coloredHead;
	private double coloredBrightness = Double.NaN;
	private int[] rgb1 = new int[0];
	private int[] rgb2 = new int[0];
	private int[] rgb3 = new int[0];
	private final int[] corners = new int[3];

	/** Drops the buffers the last render kept, so a stopped plugin holds none of them. */
	public void release()
	{
		image = null;
		depth = new float[0];
		coloredHead = null;
		coloredBrightness = Double.NaN;
		rgb1 = new int[0];
		rgb2 = new int[0];
		rgb3 = new int[0];
	}

	/** {@link #render(RetroHead, float[], float[], float[], ChatheadCamera, int, int, double, double)}, centered. */
	public BufferedImage render(RetroHead head, float[] x, float[] y, float[] z, ChatheadCamera camera,
		int width, int height)
	{
		return render(head, x, y, z, camera, width, height, width / 2.0, height / 2.0);
	}

	/** {@link #render(RetroHead, float[], float[], float[], ChatheadCamera, int, int, double, double, double)} at full brightness. */
	public BufferedImage render(RetroHead head, float[] x, float[] y, float[] z, ChatheadCamera camera,
		int width, int height, double centerX, double centerY)
	{
		return render(head, x, y, z, camera, width, height, centerX, centerY, 1);
	}

	/**
	 * Draws the head into a {@code width} by {@code height} image, centered on {@code centerX},
	 * {@code centerY}: the head widget's center within the area it may be drawn over.
	 *
	 * @param brightness the player's brightness setting, as the client's palette uses it
	 */
	public BufferedImage render(RetroHead head, float[] x, float[] y, float[] z, ChatheadCamera camera,
		int width, int height, double centerX, double centerY, double brightness)
	{
		if (image == null || image.getWidth() != width || image.getHeight() != height)
		{
			image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			depth = new float[width * height];
		}
		int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
		Arrays.fill(pixels, 0);
		Arrays.fill(depth, 0f);
		color(head, brightness);

		RetroMesh mesh = head.getMesh();
		byte[] transparencies = mesh.getFaceTransparencies();
		List<double[][]> seeThrough = new ArrayList<>();
		List<Integer> seeThroughFaces = new ArrayList<>();
		for (int face = 0; face < mesh.getFaceCount(); face++)
		{
			int transparency = transparencies == null ? 0 : transparencies[face] & 0xFF;
			if (head.getLit3()[face] == HIDDEN || transparency == FULLY_TRANSPARENT)
			{
				continue;
			}
			double[][] p = projectFace(mesh, face, x, y, z, camera, centerX, centerY);
			if (p == null || backFacing(p))
			{
				continue;
			}
			if (transparency != 0)
			{
				seeThrough.add(p);
				seeThroughFaces.add(face);
				continue;
			}
			fillTriangle(pixels, width, height, p, cornerColors(face), 1);
		}

		Integer[] order = new Integer[seeThrough.size()];
		for (int i = 0; i < order.length; i++)
		{
			order[i] = i;
		}
		Arrays.sort(order, Comparator.comparingDouble(i -> -meanDepth(seeThrough.get(i))));
		for (int i : order)
		{
			int face = seeThroughFaces.get(i);
			double alpha = (FULLY_TRANSPARENT - (transparencies[face] & 0xFF)) / (double) FULLY_TRANSPARENT;
			fillTriangle(pixels, width, height, seeThrough.get(i), cornerColors(face), alpha);
		}
		return image;
	}

	/** Culled as the client culls: wound the other way round on screen. */
	private static boolean backFacing(double[][] p)
	{
		return (p[1][0] - p[0][0]) * (p[2][1] - p[0][1]) - (p[2][0] - p[0][0]) * (p[1][1] - p[0][1]) > 0;
	}

	private static double meanDepth(double[][] p)
	{
		return (p[0][2] + p[1][2] + p[2][2]) / 3;
	}

	/** Works out every face's corner colors, unless they are already for this head and brightness. */
	private void color(RetroHead head, double brightness)
	{
		if (head == coloredHead && brightness == coloredBrightness)
		{
			return;
		}
		int faces = head.getMesh().getFaceCount();
		int[] lit1 = head.getLit1();
		int[] lit2 = head.getLit2();
		int[] lit3 = head.getLit3();
		rgb1 = new int[faces];
		rgb2 = new int[faces];
		rgb3 = new int[faces];
		for (int face = 0; face < faces; face++)
		{
			if (lit3[face] == HIDDEN)
			{
				continue;
			}
			rgb1[face] = hslToRgb(lit1[face], brightness);
			boolean flat = lit3[face] == FLAT_SHADED;
			rgb2[face] = flat ? rgb1[face] : hslToRgb(lit2[face], brightness);
			rgb3[face] = flat ? rgb1[face] : hslToRgb(lit3[face], brightness);
		}
		coloredHead = head;
		coloredBrightness = brightness;
	}

	/** A face's corner colors, in a buffer the next face reuses. */
	private int[] cornerColors(int face)
	{
		corners[0] = rgb1[face];
		corners[1] = rgb2[face];
		corners[2] = rgb3[face];
		return corners;
	}

	/** Screen x, y and depth of the face's corners, or null when any is behind the camera. */
	private double[][] projectFace(RetroMesh mesh, int face, float[] x, float[] y, float[] z, ChatheadCamera camera,
		double centerX, double centerY)
	{
		int[] corners = {mesh.getFaceIndices1()[face], mesh.getFaceIndices2()[face], mesh.getFaceIndices3()[face]};
		double[][] out = new double[3][];
		for (int k = 0; k < 3; k++)
		{
			int v = corners[k];
			camera.project(x[v], y[v], z[v], projected);
			if (projected[2] < NEAR)
			{
				return null;
			}
			out[k] = new double[]{centerX + projected[0], centerY + projected[1], projected[2]};
		}
		return out;
	}

	/**
	 * Fills a triangle, interpolating depth as 1/z so it is right under perspective. A see-through
	 * face ({@code alpha} below 1) is blended over what is drawn, or drawn at its own opacity over
	 * clear, and leaves the depth alone so what is behind still shows.
	 */
	private void fillTriangle(int[] pixels, int width, int height, double[][] p, int[] rgb, double alpha)
	{
		double x0 = p[0][0], y0 = p[0][1];
		double x1 = p[1][0], y1 = p[1][1];
		double x2 = p[2][0], y2 = p[2][1];
		double area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
		if (Math.abs(area) < 1e-9)
		{
			return;
		}

		int minX = Math.max(0, (int) Math.floor(Math.min(x0, Math.min(x1, x2))));
		int maxX = Math.min(width - 1, (int) Math.ceil(Math.max(x0, Math.max(x1, x2))));
		int minY = Math.max(0, (int) Math.floor(Math.min(y0, Math.min(y1, y2))));
		int maxY = Math.min(height - 1, (int) Math.ceil(Math.max(y0, Math.max(y1, y2))));
		double iz0 = 1 / p[0][2];
		double iz1 = 1 / p[1][2];
		double iz2 = 1 / p[2][2];

		for (int py = minY; py <= maxY; py++)
		{
			double cy = py + 0.5;
			for (int px = minX; px <= maxX; px++)
			{
				double cx = px + 0.5;
				double w0 = ((x1 - cx) * (y2 - cy) - (x2 - cx) * (y1 - cy)) / area;
				double w1 = ((x2 - cx) * (y0 - cy) - (x0 - cx) * (y2 - cy)) / area;
				double w2 = 1 - w0 - w1;
				if (w0 < 0 || w1 < 0 || w2 < 0)
				{
					continue;
				}

				float inverseZ = (float) (w0 * iz0 + w1 * iz1 + w2 * iz2);
				int at = py * width + px;
				if (inverseZ <= depth[at])
				{
					continue;
				}
				int color = blend(rgb, w0, w1, w2);
				if (alpha >= 1)
				{
					depth[at] = inverseZ;
					pixels[at] = OPAQUE | color;
				}
				else if (pixels[at] >>> 24 == 0)
				{
					pixels[at] = (int) Math.round(alpha * 255) << 24 | color;
				}
				else
				{
					pixels[at] = OPAQUE | mix(pixels[at], color, alpha);
				}
			}
		}
	}

	/** {@code over} laid on {@code under} at the given opacity; RGB only. */
	private static int mix(int under, int over, double alpha)
	{
		int r = (int) Math.round((under >> 16 & 255) * (1 - alpha) + (over >> 16 & 255) * alpha);
		int g = (int) Math.round((under >> 8 & 255) * (1 - alpha) + (over >> 8 & 255) * alpha);
		int b = (int) Math.round((under & 255) * (1 - alpha) + (over & 255) * alpha);
		return r << 16 | g << 8 | b;
	}

	private static int blend(int[] rgb, double w0, double w1, double w2)
	{
		int r = (int) ((rgb[0] >> 16 & 255) * w0 + (rgb[1] >> 16 & 255) * w1 + (rgb[2] >> 16 & 255) * w2);
		int g = (int) ((rgb[0] >> 8 & 255) * w0 + (rgb[1] >> 8 & 255) * w1 + (rgb[2] >> 8 & 255) * w2);
		int b = (int) ((rgb[0] & 255) * w0 + (rgb[1] & 255) * w1 + (rgb[2] & 255) * w2);
		return Math.min(255, r) << 16 | Math.min(255, g) << 8 | Math.min(255, b);
	}

	/**
	 * Packed HSL to 0xRRGGBB as the client draws it at a brightness setting: the client's palette
	 * arithmetic, each field sampled at the center of its bucket, then each channel raised to
	 * {@code brightness} as the client builds its palette. 1 is the plain palette; the game's
	 * settings are below it, and lighten.
	 */
	static int hslToRgb(int hsl, double brightness)
	{
		double hue = (hsl >> 10 & 63) / 64.0 + 0.0078125;
		double saturation = (hsl >> 7 & 7) / 8.0 + 0.0625;
		double luminance = (hsl & 127) / 128.0;

		double q = luminance < 0.5
			? luminance * (1 + saturation)
			: luminance + saturation - luminance * saturation;
		double p = 2 * luminance - q;

		int r = toByte(channel(p, q, wrap(hue + 1 / 3.0)));
		int g = toByte(channel(p, q, hue));
		int b = toByte(channel(p, q, wrap(hue - 1 / 3.0)));
		if (brightness != 1)
		{
			r = Math.min(255, (int) (Math.pow(r / 256.0, brightness) * 256.0));
			g = Math.min(255, (int) (Math.pow(g / 256.0, brightness) * 256.0));
			b = Math.min(255, (int) (Math.pow(b / 256.0, brightness) * 256.0));
		}
		return r << 16 | g << 8 | b;
	}

	private static double wrap(double t)
	{
		if (t < 0)
		{
			return t + 1;
		}
		return t > 1 ? t - 1 : t;
	}

	private static double channel(double p, double q, double t)
	{
		if (6 * t < 1)
		{
			return p + (q - p) * 6 * t;
		}
		if (2 * t < 1)
		{
			return q;
		}
		if (3 * t < 2)
		{
			return p + (q - p) * (2 / 3.0 - t) * 6;
		}
		return p;
	}

	private static int toByte(double value)
	{
		return Math.max(0, Math.min(255, (int) (value * 256)));
	}
}
