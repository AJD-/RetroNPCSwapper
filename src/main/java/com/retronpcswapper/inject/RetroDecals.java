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
package com.retronpcswapper.inject;

import java.util.Arrays;

/**
 * Brings painted-on detail back in front of the surface it was painted onto.
 * <p>
 * 2005 models draw detail - the sword on the anti-dragon shield, for one - as faces lying on, or
 * even slightly under, the surface they decorate, and rely on render priority to paint them last.
 * The software renderer honors that, which is why the inventory icon looks right. A depth-buffered
 * renderer does not: the GPU plugin and 117 HD draw opaque models by depth alone, so the surface
 * wins and the detail is lost.
 * <p>
 * This moves each such decal forward along its own normal until it clears the surface behind it.
 * A decal is a connected group of faces drawn at a higher priority than the faces around them, and
 * it moves as one piece, by the largest distance any of its vertices needs, so it stays flat. Only
 * vertices the decal does not share with the surface - the faces at the lowest priority - move, so
 * the surface is never deformed: a decal welded to its surface keeps those corners where they are.
 */
public final class RetroDecals
{
	/** How far in front of the surface a decal ends up. */
	static final float CLEARANCE = 1f;

	/**
	 * How far behind a decal the surface it decorates may be found. Further than this is another
	 * part. Measured: the deepest decal found sits 2.7 behind its surface (the shield icon's sword);
	 * the nearest part that is not a decal, 7.6 (an open wardrobe's door, off the cabinet behind it).
	 */
	static final float REACH = 4f;

	/** Slack for a surface the decal lies exactly in, which a ray can find a hair behind its start. */
	private static final float COPLANAR = 0.5f;

	private RetroDecals()
	{
	}

	/**
	 * Lifts the decals among faces {@code [firstFace, faceCount)}, in place.
	 *
	 * @return how many vertices moved
	 */
	public static int lift(float[] x, float[] y, float[] z,
		int[] faces1, int[] faces2, int[] faces3, byte[] priorities,
		int firstFace, int faceCount)
	{
		if (priorities == null || firstFace >= faceCount)
		{
			return 0;
		}

		int lowest = Integer.MAX_VALUE;
		int highest = Integer.MIN_VALUE;
		int vertexEnd = 0;
		for (int f = firstFace; f < faceCount; f++)
		{
			lowest = Math.min(lowest, priorities[f]);
			highest = Math.max(highest, priorities[f]);
			vertexEnd = Math.max(vertexEnd, Math.max(faces1[f], Math.max(faces2[f], faces3[f])) + 1);
		}
		if (lowest == highest)
		{
			return 0;
		}

		// A vertex the surface uses - any face at the lowest priority - stays put. One shared only
		// between decal faces is the decal's own, though they are at different priorities: a door
		// painted as a fill and a border round it moves as one.
		boolean[] anchored = new boolean[vertexEnd];
		for (int f = firstFace; f < faceCount; f++)
		{
			if (priorities[f] == lowest)
			{
				anchored[faces1[f]] = true;
				anchored[faces2[f]] = true;
				anchored[faces3[f]] = true;
			}
		}

		// Group the decal faces - those above the lowest priority - into connected pieces
		int[] parent = new int[faceCount];
		for (int f = 0; f < faceCount; f++)
		{
			parent[f] = f;
		}
		int[] owner = new int[vertexEnd];
		Arrays.fill(owner, -1);
		for (int f = firstFace; f < faceCount; f++)
		{
			if (priorities[f] == lowest)
			{
				continue;
			}
			for (int v : new int[]{faces1[f], faces2[f], faces3[f]})
			{
				if (owner[v] == -1)
				{
					owner[v] = f;
				}
				else
				{
					union(parent, owner[v], f);
				}
			}
		}

		float[] normalX = new float[faceCount];
		float[] normalY = new float[faceCount];
		float[] normalZ = new float[faceCount];
		float[] liftBy = new float[faceCount];
		boolean[] found = new boolean[faceCount];
		for (int f = firstFace; f < faceCount; f++)
		{
			if (priorities[f] == lowest)
			{
				continue;
			}
			int root = find(parent, f);
			float[] n = normal(x, y, z, faces1[f], faces2[f], faces3[f]);
			normalX[root] += n[0];
			normalY[root] += n[1];
			normalZ[root] += n[2];
		}

		// For each piece, the furthest any of its movable vertices sits behind a lower face
		for (int f = firstFace; f < faceCount; f++)
		{
			if (priorities[f] == lowest)
			{
				continue;
			}
			int root = find(parent, f);
			float length = (float) Math.sqrt(normalX[root] * normalX[root]
				+ normalY[root] * normalY[root] + normalZ[root] * normalZ[root]);
			if (length == 0)
			{
				continue;
			}
			float nx = normalX[root] / length;
			float ny = normalY[root] / length;
			float nz = normalZ[root] / length;

			for (int v : new int[]{faces1[f], faces2[f], faces3[f]})
			{
				if (anchored[v])
				{
					// Shared with the surface - an anchor, not something to move
					continue;
				}

				for (int g = firstFace; g < faceCount; g++)
				{
					// The surface behind a piece, never the piece's own lower layer
					if (priorities[g] >= priorities[f] || priorities[g] != lowest && find(parent, g) == root)
					{
						continue;
					}
					float t = intersect(x[v], y[v], z[v], nx, ny, nz,
						x, y, z, faces1[g], faces2[g], faces3[g]);
					if (t >= -COPLANAR && t <= REACH && (!found[root] || t > liftBy[root]))
					{
						liftBy[root] = t;
						found[root] = true;
					}
				}
			}
		}

		// Move each piece's own vertices, once each
		boolean[] moved = new boolean[vertexEnd];
		int count = 0;
		for (int f = firstFace; f < faceCount; f++)
		{
			if (priorities[f] == lowest)
			{
				continue;
			}
			int root = find(parent, f);
			if (!found[root])
			{
				continue;
			}
			float length = (float) Math.sqrt(normalX[root] * normalX[root]
				+ normalY[root] * normalY[root] + normalZ[root] * normalZ[root]);
			float distance = Math.max(liftBy[root], 0f) + CLEARANCE;

			for (int v : new int[]{faces1[f], faces2[f], faces3[f]})
			{
				if (moved[v] || anchored[v])
				{
					continue;
				}
				moved[v] = true;
				x[v] += normalX[root] / length * distance;
				y[v] += normalY[root] / length * distance;
				z[v] += normalZ[root] / length * distance;
				count++;
			}
		}
		return count;
	}

	/**
	 * Brings the decals among a model's faces from {@code firstFace} on to the front: lifts the ones
	 * that can move, and biases every one of them toward the camera for the ones that cannot.
	 *
	 * <p>The bias is the renderers' own answer to coplanar detail - both the GPU plugin and 117 HD
	 * push a face's depth toward the camera by it - but it only settles a tie. A decal sitting
	 * behind its surface, like the shield's sword, needs the lift as well.
	 *
	 * @return how many vertices moved
	 */
	public static int lift(RetroModel model, int firstFace)
	{
		byte[] priorities = model.getFaceRenderPriorities();
		int faceCount = model.getFaceCount();
		if (priorities == null || firstFace >= faceCount)
		{
			return 0;
		}

		int moved = lift(model.getVerticesX(), model.getVerticesY(), model.getVerticesZ(),
			model.getFaceIndices1(), model.getFaceIndices2(), model.getFaceIndices3(),
			priorities, firstFace, faceCount);
		if (moved > 0)
		{
			model.calculateBoundsCylinder();
		}

		bias(priorities, firstFace, faceCount, model);
		return moved;
	}

	/**
	 * How far a lower part may poke out through the part painted over it.
	 */
	static final float TUCK_REACH = 8f;

	/**
	 * Settles, in place, one part that 2005 painted over another by priority: the lower part's
	 * vertices poking out through the upper are tucked behind it, then the upper is lifted clear of
	 * the lower as a decal. Opt-in per pair, since priority is structural elsewhere in a kit.
	 *
	 * @return how many vertices moved
	 */
	public static int paintOver(float[] x, float[] y, float[] z,
		int[] faces1, int[] faces2, int[] faces3,
		int lowerStart, int lowerEnd, int upperStart, int upperEnd)
	{
		int moved = tuck(x, y, z, faces1, faces2, faces3, lowerStart, lowerEnd, upperStart, upperEnd);

		// Just the pair, lower as the surface and upper as the decal, whatever their own priorities
		int lowerCount = lowerEnd - lowerStart;
		int count = lowerCount + upperEnd - upperStart;
		int[] pair1 = new int[count];
		int[] pair2 = new int[count];
		int[] pair3 = new int[count];
		byte[] layer = new byte[count];
		for (int i = 0; i < count; i++)
		{
			int f = i < lowerCount ? lowerStart + i : upperStart + i - lowerCount;
			pair1[i] = faces1[f];
			pair2[i] = faces2[f];
			pair3[i] = faces3[f];
			layer[i] = (byte) (i < lowerCount ? 0 : 1);
		}
		return moved + lift(x, y, z, pair1, pair2, pair3, layer, 0, count);
	}

	/** Moves each lower vertex in front of an upper face to behind it. Vertices the upper uses stay. */
	static int tuck(float[] x, float[] y, float[] z, int[] faces1, int[] faces2, int[] faces3,
		int lowerStart, int lowerEnd, int upperStart, int upperEnd)
	{
		boolean[] upper = new boolean[x.length];
		for (int g = upperStart; g < upperEnd; g++)
		{
			upper[faces1[g]] = true;
			upper[faces2[g]] = true;
			upper[faces3[g]] = true;
		}

		boolean[] done = new boolean[x.length];
		int count = 0;
		for (int f = lowerStart; f < lowerEnd; f++)
		{
			for (int v : new int[]{faces1[f], faces2[f], faces3[f]})
			{
				if (done[v] || upper[v])
				{
					continue;
				}
				done[v] = true;

				float deepest = 0;
				float[] by = null;
				for (int g = upperStart; g < upperEnd; g++)
				{
					float[] n = normal(x, y, z, faces1[g], faces2[g], faces3[g]);
					float t = intersect(x[v], y[v], z[v], -n[0], -n[1], -n[2],
						x, y, z, faces1[g], faces2[g], faces3[g]);
					if (t > 0 && t <= TUCK_REACH && t > deepest)
					{
						deepest = t;
						by = n;
					}
				}
				if (by != null)
				{
					float distance = deepest + CLEARANCE;
					x[v] -= by[0] * distance;
					y[v] -= by[1] * distance;
					z[v] -= by[2] * distance;
					count++;
				}
			}
		}
		return count;
	}

	/** Biases faces {@code [start, end)} one step toward the camera, the ties a painted-over part wins. */
	public static void biasOver(RetroModel model, int start, int end)
	{
		byte[] bias = model.writableFaceBias();
		for (int f = start; f < end; f++)
		{
			bias[f] = (byte) Math.max(bias[f] & 0xFF, BIAS_STEP);
		}
	}

	/** Depth bias per priority step above the lowest. */
	static final int BIAS_STEP = 2;

	/** The most bias any face gets; the GPU plugin's own bias debug view tops out here. */
	static final int MAX_BIAS = 12;

	private static void bias(byte[] priorities, int firstFace, int faceCount, RetroModel model)
	{
		int lowest = Integer.MAX_VALUE;
		for (int f = firstFace; f < faceCount; f++)
		{
			lowest = Math.min(lowest, priorities[f]);
		}

		byte[] bias = null;
		for (int f = firstFace; f < faceCount; f++)
		{
			int step = priorities[f] - lowest;
			if (step <= 0)
			{
				continue;
			}
			if (bias == null)
			{
				bias = model.writableFaceBias();
			}
			bias[f] = (byte) Math.max(bias[f] & 0xFF, Math.min(MAX_BIAS, step * BIAS_STEP));
		}
	}

	/** The face normal, by the winding the renderer culls by, unnormalized. */
	private static float[] normal(float[] x, float[] y, float[] z, int a, int b, int c)
	{
		float ux = x[b] - x[a];
		float uy = y[b] - y[a];
		float uz = z[b] - z[a];
		float vx = x[c] - x[a];
		float vy = y[c] - y[a];
		float vz = z[c] - z[a];
		float nx = uy * vz - uz * vy;
		float ny = uz * vx - ux * vz;
		float nz = ux * vy - uy * vx;
		float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
		return length == 0 ? new float[3] : new float[]{nx / length, ny / length, nz / length};
	}

	/**
	 * Where the ray from a point along a direction meets a triangle, as a distance along the ray, or
	 * NaN when it misses. Moller-Trumbore, both faces of the triangle.
	 */
	static float intersect(float px, float py, float pz, float dx, float dy, float dz,
		float[] x, float[] y, float[] z, int a, int b, int c)
	{
		float e1x = x[b] - x[a];
		float e1y = y[b] - y[a];
		float e1z = z[b] - z[a];
		float e2x = x[c] - x[a];
		float e2y = y[c] - y[a];
		float e2z = z[c] - z[a];

		float hx = dy * e2z - dz * e2y;
		float hy = dz * e2x - dx * e2z;
		float hz = dx * e2y - dy * e2x;
		float det = e1x * hx + e1y * hy + e1z * hz;
		if (Math.abs(det) < 1e-6f)
		{
			return Float.NaN;
		}

		float inv = 1f / det;
		float sx = px - x[a];
		float sy = py - y[a];
		float sz = pz - z[a];
		float u = (sx * hx + sy * hy + sz * hz) * inv;
		if (u < 0f || u > 1f)
		{
			return Float.NaN;
		}

		float qx = sy * e1z - sz * e1y;
		float qy = sz * e1x - sx * e1z;
		float qz = sx * e1y - sy * e1x;
		float v = (dx * qx + dy * qy + dz * qz) * inv;
		if (v < 0f || u + v > 1f)
		{
			return Float.NaN;
		}

		return (e2x * qx + e2y * qy + e2z * qz) * inv;
	}

	private static int find(int[] parent, int f)
	{
		while (parent[f] != f)
		{
			parent[f] = parent[parent[f]];
			f = parent[f];
		}
		return f;
	}

	private static void union(int[] parent, int a, int b)
	{
		parent[find(parent, a)] = find(parent, b);
	}
}
