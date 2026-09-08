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

import java.util.ArrayList;
import java.util.List;

/**
 * Merges the parts of a multi-model NPC into one mesh, offsetting face indices and vertex-group
 * members so the parts share a coordinate and rig space.
 *
 * <p>This used to happen in the generator, with the merged result stored under the first part's
 * model id. That could not express the giant family, where five NPCs are one shared body mesh plus
 * a variant head: every one of them keys on 2870, so the last spec written would win, and they would
 * all wear the same head. The bundle now stores each part under its own real model id - keeping the
 * "keyed by the ids the source caches use" invariant, and storing a shared body once - and the
 * merge happens here instead, once per NPC id at spawn.
 *
 * <p>Parts are concatenated in the same coordinate space; there is no per-part translation, because
 * 2005 parts are authored to sit together already.
 */
public final class RetroMeshMerger
{
	private RetroMeshMerger()
	{
	}

	/**
	 * Merges parts into a single mesh under {@code id}, conventionally the first part's model id so
	 * that logging still names something recognizable.
	 *
	 * <p>A single part is returned as-is rather than copied. {@link RetroMesh} is immutable and every
	 * consumer that needs to change one builds a derived copy first, so sharing the instance is safe
	 * and makes this a provable no-op for the categories that were never multi-part.
	 */
	public static RetroMesh merge(int id, List<RetroMesh> parts)
	{
		if (parts.size() == 1)
		{
			return parts.get(0);
		}

		int totalVertices = 0;
		int totalFaces = 0;
		int groupCount = 0;
		boolean anyRenderTypes = false;
		boolean anyTransparencies = false;
		boolean anyPriorities = false;
		boolean anyTextures = false;

		for (RetroMesh part : parts)
		{
			totalVertices += part.getVerticesCount();
			totalFaces += part.getFaceCount();
			int[][] groups = part.getVertexGroups();
			if (groups != null)
			{
				groupCount = Math.max(groupCount, groups.length);
			}
			anyRenderTypes |= part.getFaceRenderTypes() != null;
			anyTransparencies |= part.getFaceTransparencies() != null;
			anyPriorities |= part.getFaceRenderPriorities() != null;
			anyTextures |= part.getFaceTextures() != null;
		}

		float[] vx = new float[totalVertices];
		float[] vy = new float[totalVertices];
		float[] vz = new float[totalVertices];
		int[] i1 = new int[totalFaces];
		int[] i2 = new int[totalFaces];
		int[] i3 = new int[totalFaces];
		short[] colors = new short[totalFaces];

		// Null is not the same as empty here: a null array is what keeps a model off the sorted
		// upload path, so an all-null column stays null rather than becoming all-zero.
		byte[] renderTypes = anyRenderTypes ? new byte[totalFaces] : null;
		byte[] transparencies = anyTransparencies ? new byte[totalFaces] : null;
		byte[] priorities = anyPriorities ? new byte[totalFaces] : null;
		short[] textures = anyTextures ? new short[totalFaces] : null;

		List<List<Integer>> groups = new ArrayList<>();
		for (int i = 0; i < groupCount; i++)
		{
			groups.add(new ArrayList<>());
		}

		int vertexBase = 0;
		int faceBase = 0;
		for (RetroMesh part : parts)
		{
			int partVertices = part.getVerticesCount();
			System.arraycopy(part.getVerticesX(), 0, vx, vertexBase, partVertices);
			System.arraycopy(part.getVerticesY(), 0, vy, vertexBase, partVertices);
			System.arraycopy(part.getVerticesZ(), 0, vz, vertexBase, partVertices);

			int[] partI1 = part.getFaceIndices1();
			int[] partI2 = part.getFaceIndices2();
			int[] partI3 = part.getFaceIndices3();
			short[] partColors = part.getFaceColors();
			byte[] partRenderTypes = part.getFaceRenderTypes();
			byte[] partTransparencies = part.getFaceTransparencies();
			byte[] partPriorities = part.getFaceRenderPriorities();
			short[] partTextures = part.getFaceTextures();

			for (int f = 0; f < part.getFaceCount(); f++)
			{
				int face = faceBase + f;
				i1[face] = partI1[f] + vertexBase;
				i2[face] = partI2[f] + vertexBase;
				i3[face] = partI3[f] + vertexBase;
				colors[face] = partColors[f];

				if (renderTypes != null)
				{
					renderTypes[face] = partRenderTypes == null ? 0 : partRenderTypes[f];
				}
				if (transparencies != null)
				{
					transparencies[face] = partTransparencies == null ? 0 : partTransparencies[f];
				}
				if (priorities != null)
				{
					// The model-level priority is the fallback the client uses for a part with no
					// per-face array, which is why RetroMesh carries it at all
					priorities[face] = partPriorities == null
						? (byte) part.getPriority() : partPriorities[f];
				}
				if (textures != null)
				{
					textures[face] = partTextures == null ? -1 : partTextures[f];
				}
			}

			int[][] partGroups = part.getVertexGroups();
			if (partGroups != null)
			{
				for (int group = 0; group < partGroups.length; group++)
				{
					if (partGroups[group] == null)
					{
						continue;
					}
					for (int vertex : partGroups[group])
					{
						groups.get(group).add(vertex + vertexBase);
					}
				}
			}

			vertexBase += partVertices;
			faceBase += part.getFaceCount();
		}

		int[][] vertexGroups = new int[groupCount][];
		for (int group = 0; group < groupCount; group++)
		{
			List<Integer> members = groups.get(group);
			int[] packed = new int[members.size()];
			for (int i = 0; i < packed.length; i++)
			{
				packed[i] = members.get(i);
			}
			vertexGroups[group] = packed;
		}

		return new RetroMesh(id, parts.get(0).getPriority(), vx, vy, vz, i1, i2, i3,
			colors, renderTypes, transparencies, priorities, textures, vertexGroups);
	}
}
