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
package com.retronpcswapper;

/**
 * How the client places a scenery object, as far as the stand-ins have to reproduce it: the
 * placement type and quarter turns packed into an object's config, and the turns themselves.
 */
final class ObjectPlacement
{
	// Placement types, as the client numbers them
	static final int TYPE_STRAIGHT = 4;
	static final int TYPE_STRAIGHT_OFFSET = 5;
	static final int TYPE_DIAGONAL_OFFSET = 6;
	static final int TYPE_DIAGONAL = 7;
	static final int TYPE_DIAGONAL_BOTH = 8;
	static final int TYPE_CENTREPIECE = 10;
	static final int TYPE_DIAGONAL_CENTREPIECE = 11;

	/** Jagex angle units in the eighth turn a diagonal placement adds. */
	static final int EIGHTH_TURN = 256;

	private ObjectPlacement()
	{
	}

	/** The object's placement type, from its config bits. */
	static int type(int config)
	{
		return config & 0x1F;
	}

	/** The object's quarter-turn orientation, 0-3, from its config bits. */
	static int orientation(int config)
	{
		return config >>> 6 & 3;
	}

	/**
	 * Turns the first {@code count} points in place by whole quarter turns about the vertical axis,
	 * the way the renderer turns a model by its orientation:
	 * {@code x' = z sin + x cos, z' = z cos - x sin}.
	 */
	static void turn(float[] xs, float[] zs, int count, int quarterTurns)
	{
		switch (quarterTurns & 3)
		{
			case 1:
				for (int v = 0; v < count; v++)
				{
					float x = xs[v];
					xs[v] = zs[v];
					zs[v] = -x;
				}
				break;
			case 2:
				for (int v = 0; v < count; v++)
				{
					xs[v] = -xs[v];
					zs[v] = -zs[v];
				}
				break;
			case 3:
				for (int v = 0; v < count; v++)
				{
					float x = xs[v];
					xs[v] = -zs[v];
					zs[v] = x;
				}
				break;
			default:
				break;
		}
	}
}
