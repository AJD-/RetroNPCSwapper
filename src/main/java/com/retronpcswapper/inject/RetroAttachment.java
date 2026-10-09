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

/**
 * Recovers where the client has put a rigid part of an animated model, so another part can be put
 * in exactly the same place.
 * <p>
 * Every vertex of a part bound to a single vertex group goes through the same chain of
 * translations, rotations and scales as the model is posed, so the whole part moves by one affine
 * map. Find the part's block of vertices in the posed model, fit that map from its rest vertices
 * to its posed ones, and the map can be applied to any other part bound to the same group.
 */
public final class RetroAttachment
{
	/** How far a fitted vertex may land from where the client put it before the fit is refused. */
	static final double MAX_RESIDUAL = 3.0;

	/** Slack on a distance between two vertices of the part, as a share of it, and at least this. */
	private static final double DISTANCE_SLACK = 0.1;
	private static final double MIN_DISTANCE_SLACK = 2.0;

	private RetroAttachment()
	{
	}

	/**
	 * Where a part was found in a posed model, and the map that put it there.
	 */
	public static final class Placement
	{
		/** The first of the part's vertices in the posed model. */
		public final int start;

		/** The map from {@link #fit}. */
		public final double[] map;

		Placement(int start, double[] map)
		{
			this.start = start;
			this.map = map;
		}
	}

	/**
	 * A part at rest, with the spacing {@link #locate} checks each candidate run against worked out
	 * once rather than on every search.
	 */
	public static final class Part
	{
		final float[] x;
		final float[] y;
		final float[] z;
		final int count;

		/** Pairs of the part's vertices, far apart across it, and the distance between each pair. */
		final int[] pairs;
		final double[] distances;

		/**
		 * Takes the rest vertices as they are, without copying - the caller must not change them.
		 */
		public Part(float[] x, float[] y, float[] z, int count)
		{
			this.x = x;
			this.y = y;
			this.z = z;
			this.count = count;

			if (count < 4)
			{
				// Too few vertices to pin a map down; locate turns these away
				pairs = null;
				distances = null;
				return;
			}

			pairs = new int[]{0, count - 1, count / 4, count * 3 / 4, count / 3, count - 2, 1, count / 2};
			distances = new double[pairs.length / 2];
			for (int p = 0; p < distances.length; p++)
			{
				distances[p] = distance(x, y, z, pairs[2 * p], x, y, z, pairs[2 * p + 1]);
			}
		}

		/** How many vertices the part has - the length of the run {@link #locate} looks for. */
		public int getCount()
		{
			return count;
		}
	}

	/**
	 * Finds where a part's vertices sit in a posed model. See {@link #locate(Part, float[], float[], float[], int, int)}.
	 */
	public static Placement locate(float[] restX, float[] restY, float[] restZ, int restCount,
		float[] x, float[] y, float[] z, int count, int hint)
	{
		return locate(new Part(restX, restY, restZ, restCount), x, y, z, count, hint);
	}

	/**
	 * Finds where a part's vertices sit in a posed model, and how they got there: a run of
	 * {@code part.count} vertices whose spacing matches the part's at rest, confirmed by an affine
	 * map that fits every one of them.
	 *
	 * <p>The client builds a player's model by appending each part's vertices in turn, so a part is
	 * one unbroken run. Its shape survives posing - a rigid part only turns and moves - so checking
	 * a few long distances across it rejects nearly every other run cheaply, and the fit settles
	 * the few that get through.
	 *
	 * @param hint where it was found last time, tried first; -1 for none
	 * @return the placement, or null if no run is the part
	 */
	public static Placement locate(Part part, float[] x, float[] y, float[] z, int count, int hint)
	{
		int restCount = part.count;
		if (restCount < 4 || count < restCount)
		{
			return null;
		}

		if (hint >= 0 && hint <= count - restCount && matches(part.pairs, part.distances, x, y, z, hint))
		{
			double[] map = fit(part.x, part.y, part.z, restCount, x, y, z, hint);
			if (map != null)
			{
				return new Placement(hint, map);
			}
		}

		for (int start = 0; start <= count - restCount; start++)
		{
			if (start != hint && matches(part.pairs, part.distances, x, y, z, start))
			{
				double[] map = fit(part.x, part.y, part.z, restCount, x, y, z, start);
				if (map != null)
				{
					return new Placement(start, map);
				}
			}
		}
		return null;
	}

	private static boolean matches(int[] pairs, double[] distances, float[] x, float[] y, float[] z, int start)
	{
		for (int p = 0; p < distances.length; p++)
		{
			double posed = distance(x, y, z, start + pairs[2 * p], x, y, z, start + pairs[2 * p + 1]);
			double slack = Math.max(MIN_DISTANCE_SLACK, distances[p] * DISTANCE_SLACK);
			if (Math.abs(posed - distances[p]) > slack)
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Fits the affine map taking a part's rest vertices onto its posed ones, starting at
	 * {@code start} in the posed arrays.
	 *
	 * @return the map as {@code {a00, a01, a02, tx, a10, a11, a12, ty, a20, a21, a22, tz}}, or null
	 *         when no affine map puts every vertex within {@link #MAX_RESIDUAL} of where it is -
	 *         which means the run is not the part after all
	 */
	public static double[] fit(float[] restX, float[] restY, float[] restZ, int restCount,
		float[] x, float[] y, float[] z, int start)
	{
		double rx = 0;
		double ry = 0;
		double rz = 0;
		double px = 0;
		double py = 0;
		double pz = 0;
		for (int v = 0; v < restCount; v++)
		{
			rx += restX[v];
			ry += restY[v];
			rz += restZ[v];
			px += x[start + v];
			py += y[start + v];
			pz += z[start + v];
		}
		rx /= restCount;
		ry /= restCount;
		rz /= restCount;
		px /= restCount;
		py /= restCount;
		pz /= restCount;

		// Least squares, centered: A = (sum P R^T)(sum R R^T)^-1
		double[][] rr = new double[3][3];
		double[][] pr = new double[3][3];
		for (int v = 0; v < restCount; v++)
		{
			double[] r = {restX[v] - rx, restY[v] - ry, restZ[v] - rz};
			double[] p = {x[start + v] - px, y[start + v] - py, z[start + v] - pz};
			for (int i = 0; i < 3; i++)
			{
				for (int j = 0; j < 3; j++)
				{
					rr[i][j] += r[i] * r[j];
					pr[i][j] += p[i] * r[j];
				}
			}
		}

		double[][] inverse = invert(rr);
		if (inverse == null)
		{
			// A flat part pins down no map at all
			return null;
		}

		double[] map = new double[12];
		for (int i = 0; i < 3; i++)
		{
			for (int j = 0; j < 3; j++)
			{
				double a = 0;
				for (int k = 0; k < 3; k++)
				{
					a += pr[i][k] * inverse[k][j];
				}
				map[i * 4 + j] = a;
			}
		}
		map[3] = px - (map[0] * rx + map[1] * ry + map[2] * rz);
		map[7] = py - (map[4] * rx + map[5] * ry + map[6] * rz);
		map[11] = pz - (map[8] * rx + map[9] * ry + map[10] * rz);

		for (int v = 0; v < restCount; v++)
		{
			double ex = apply(map, 0, restX[v], restY[v], restZ[v]) - x[start + v];
			double ey = apply(map, 4, restX[v], restY[v], restZ[v]) - y[start + v];
			double ez = apply(map, 8, restX[v], restY[v], restZ[v]) - z[start + v];
			if (ex * ex + ey * ey + ez * ez > MAX_RESIDUAL * MAX_RESIDUAL)
			{
				return null;
			}
		}
		return map;
	}

	/**
	 * Moves {@code count} vertices from rest into place by a map from {@link #fit}, writing into the
	 * destination arrays.
	 */
	public static void transform(double[] map, float[] restX, float[] restY, float[] restZ, int count,
		float[] outX, float[] outY, float[] outZ)
	{
		transform(map, restX, restY, restZ, count, outX, outY, outZ, 0);
	}

	/**
	 * As {@link #transform(double[], float[], float[], float[], int, float[], float[], float[])}, but
	 * writing from {@code outOffset} on - so a part appended to a larger model can be placed where it
	 * sits, with no copy of its own.
	 */
	public static void transform(double[] map, float[] restX, float[] restY, float[] restZ, int count,
		float[] outX, float[] outY, float[] outZ, int outOffset)
	{
		for (int v = 0; v < count; v++)
		{
			float vx = restX[v];
			float vy = restY[v];
			float vz = restZ[v];
			outX[outOffset + v] = (float) apply(map, 0, vx, vy, vz);
			outY[outOffset + v] = (float) apply(map, 4, vx, vy, vz);
			outZ[outOffset + v] = (float) apply(map, 8, vx, vy, vz);
		}
	}

	private static double apply(double[] map, int row, double x, double y, double z)
	{
		return map[row] * x + map[row + 1] * y + map[row + 2] * z + map[row + 3];
	}

	private static double distance(float[] ax, float[] ay, float[] az, int a, float[] bx, float[] by, float[] bz, int b)
	{
		double dx = ax[a] - bx[b];
		double dy = ay[a] - by[b];
		double dz = az[a] - bz[b];
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	private static double[][] invert(double[][] m)
	{
		double c00 = m[1][1] * m[2][2] - m[1][2] * m[2][1];
		double c01 = m[1][2] * m[2][0] - m[1][0] * m[2][2];
		double c02 = m[1][0] * m[2][1] - m[1][1] * m[2][0];
		double det = m[0][0] * c00 + m[0][1] * c01 + m[0][2] * c02;
		if (Math.abs(det) < 1e-6)
		{
			return null;
		}

		double inv = 1 / det;
		return new double[][]{
			{c00 * inv, (m[0][2] * m[2][1] - m[0][1] * m[2][2]) * inv, (m[0][1] * m[1][2] - m[0][2] * m[1][1]) * inv},
			{c01 * inv, (m[0][0] * m[2][2] - m[0][2] * m[2][0]) * inv, (m[0][2] * m[1][0] - m[0][0] * m[1][2]) * inv},
			{c02 * inv, (m[0][1] * m[2][0] - m[0][0] * m[2][1]) * inv, (m[0][0] * m[1][1] - m[0][1] * m[1][0]) * inv},
		};
	}
}
