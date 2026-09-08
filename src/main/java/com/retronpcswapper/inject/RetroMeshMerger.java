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
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public final class RetroMeshMerger
{
	/**
	 * The highest texture triangle a merged mesh can address. The renderer reads the per-face index
	 * as {@code textureFaces[face] & 0xff}, so 255 is indistinguishable from the -1 that means "no
	 * triangle" - which leaves 0..254 usable. The generator refuses to bundle a set that would
	 * exceed this, so reaching it here means the bundle and this code disagree.
	 */
	private static final int MAX_TEXTURE_TRIANGLES = 0xFF;

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
		int totalTriangles = 0;

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
			totalTriangles += part.getTextureTriangleCount();
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

		// A face with no triangle is -1 rather than 0, which is a real triangle. Allocated off the
		// triangle count rather than off any part's textureCoords, so a part whose faces all use
		// the face-as-UV projection contributes nothing but its -1s.
		byte[] textureCoords = totalTriangles > 0 ? new byte[totalFaces] : null;
		int[] texIndices1 = totalTriangles > 0 ? new int[totalTriangles] : null;
		int[] texIndices2 = totalTriangles > 0 ? new int[totalTriangles] : null;
		int[] texIndices3 = totalTriangles > 0 ? new int[totalTriangles] : null;

		List<List<Integer>> groups = new ArrayList<>();
		for (int i = 0; i < groupCount; i++)
		{
			groups.add(new ArrayList<>());
		}

		int vertexBase = 0;
		int faceBase = 0;
		int triangleBase = 0;
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
			byte[] partCoords = part.getTextureCoords();

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
				if (textureCoords != null)
				{
					textureCoords[face] = mergedCoord(id, partCoords, f, triangleBase);
				}
			}

			// A texture triangle names this part's own vertices, so it shifts exactly as a face
			// index does. The triangle table itself concatenates, which is what the per-face
			// index above is shifted by.
			for (int t = 0; t < part.getTextureTriangleCount(); t++)
			{
				texIndices1[triangleBase + t] = part.getTexIndices1()[t] + vertexBase;
				texIndices2[triangleBase + t] = part.getTexIndices2()[t] + vertexBase;
				texIndices3[triangleBase + t] = part.getTexIndices3()[t] + vertexBase;
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
			triangleBase += part.getTextureTriangleCount();
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
			colors, renderTypes, transparencies, priorities, textures,
			textureCoords, texIndices1, texIndices2, texIndices3, vertexGroups);
	}

	/**
	 * The merged per-face triangle index: -1 where the part named no triangle, and the part's own
	 * index shifted into the concatenated table where it did.
	 *
	 * <p>A part can decline a triangle two ways - a null array, meaning no face on it names one, or
	 * a -1 entry, meaning that one face uses the renderer's projection - and both mean -1 here.
	 */
	private static byte mergedCoord(int id, byte[] partCoords, int face, int triangleBase)
	{
		if (partCoords == null || partCoords[face] == -1)
		{
			return -1;
		}

		int merged = (partCoords[face] & 0xFF) + triangleBase;
		if (merged >= MAX_TEXTURE_TRIANGLES)
		{
			// Unreachable from a bundle the generator produced - it refuses a set that would get
			// here - so say so loudly rather than wrapping the byte and mapping the face onto some
			// unrelated triangle
			log.warn("Merged mesh {} needs texture triangle {}, past the {} the renderer can "
				+ "address; that face falls back to the face-as-UV projection",
				id, merged, MAX_TEXTURE_TRIANGLES);
			return -1;
		}

		return (byte) merged;
	}
}
