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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Reads and writes {@link RetroAssetBundle} as a compact binary blob.
 *
 * <p>Hand-rolled rather than serialized: Java serialization is off the table for a plugin, and a
 * text format would be several times the size for data that is almost entirely numeric arrays. The
 * payload is gzipped, which matters because vertex and index arrays compress well.
 *
 * <p>Every structure is length-prefixed and the whole thing starts with a magic number and a
 * version, so a bundle produced by an older generator is rejected outright instead of being
 * misread into plausible-looking geometry.
 */
public final class RetroAssetCodec
{
	/** "RTRO" - guards against being handed an unrelated file. */
	private static final int MAGIC = 0x5254524F;

	/** Bump on any layout change; readers refuse anything they were not written for. */
	static final int VERSION = 3;

	/** Sanity ceilings, so a corrupt length cannot make the reader allocate wildly. */
	private static final int MAX_ENTRIES = 100_000;
	private static final int MAX_ARRAY = 10_000_000;

	private RetroAssetCodec()
	{
	}

	public static void write(RetroAssetBundle bundle, OutputStream out) throws IOException
	{
		try (DataOutputStream data = new DataOutputStream(new GZIPOutputStream(out)))
		{
			data.writeInt(MAGIC);
			data.writeInt(VERSION);

			data.writeInt(bundle.getMeshes().size());
			for (RetroMesh mesh : bundle.getMeshes().values())
			{
				writeMesh(data, mesh);
			}

			data.writeInt(bundle.getRigs().size());
			for (RetroRig rig : bundle.getRigs().values())
			{
				writeRig(data, rig);
			}

			data.writeInt(bundle.getClips().size());
			for (RetroClip clip : bundle.getClips().values())
			{
				writeClip(data, clip);
			}
		}
	}

	public static RetroAssetBundle read(InputStream in) throws IOException
	{
		try (DataInputStream data = new DataInputStream(new GZIPInputStream(in)))
		{
			int magic = data.readInt();
			if (magic != MAGIC)
			{
				throw new IOException("Not a retro asset bundle (magic " + Integer.toHexString(magic) + ")");
			}

			int version = data.readInt();
			if (version != VERSION)
			{
				throw new IOException("Retro asset bundle is version " + version
					+ ", this build reads version " + VERSION + " - regenerate it");
			}

			Map<Integer, RetroMesh> meshes = new LinkedHashMap<>();
			int meshCount = readCount(data, MAX_ENTRIES);
			for (int i = 0; i < meshCount; i++)
			{
				RetroMesh mesh = readMesh(data);
				meshes.put(mesh.getId(), mesh);
			}

			Map<Integer, RetroRig> rigs = new LinkedHashMap<>();
			int rigCount = readCount(data, MAX_ENTRIES);
			for (int i = 0; i < rigCount; i++)
			{
				RetroRig rig = readRig(data);
				rigs.put(rig.getId(), rig);
			}

			Map<Integer, RetroClip> clips = new LinkedHashMap<>();
			int clipCount = readCount(data, MAX_ENTRIES);
			for (int i = 0; i < clipCount; i++)
			{
				RetroClip clip = readClip(data);
				clips.put(clip.getSequenceId(), clip);
			}

			return new RetroAssetBundle(meshes, rigs, clips);
		}
	}

	private static void writeMesh(DataOutputStream data, RetroMesh mesh) throws IOException
	{
		data.writeInt(mesh.getId());
		data.writeByte(mesh.getPriority());

		writeFloats(data, mesh.getVerticesX());
		writeFloats(data, mesh.getVerticesY());
		writeFloats(data, mesh.getVerticesZ());

		writeInts(data, mesh.getFaceIndices1());
		writeInts(data, mesh.getFaceIndices2());
		writeInts(data, mesh.getFaceIndices3());

		writeShorts(data, mesh.getFaceColors());
		writeBytes(data, mesh.getFaceRenderTypes());
		writeBytes(data, mesh.getFaceTransparencies());
		writeBytes(data, mesh.getFaceRenderPriorities());
		writeShorts(data, mesh.getFaceTextures());

		writeBytes(data, mesh.getTextureCoords());
		writeInts(data, mesh.getTexIndices1());
		writeInts(data, mesh.getTexIndices2());
		writeInts(data, mesh.getTexIndices3());

		writeIntMatrix(data, mesh.getVertexGroups());
	}

	private static RetroMesh readMesh(DataInputStream data) throws IOException
	{
		int id = data.readInt();
		int priority = data.readByte();

		float[] vx = readFloats(data);
		float[] vy = readFloats(data);
		float[] vz = readFloats(data);

		int[] i1 = readInts(data);
		int[] i2 = readInts(data);
		int[] i3 = readInts(data);

		short[] colors = readShorts(data);
		byte[] renderTypes = readBytes(data);
		byte[] transparencies = readBytes(data);
		byte[] priorities = readBytes(data);
		short[] textures = readShorts(data);

		byte[] textureCoords = readBytes(data);
		int[] texIndices1 = readInts(data);
		int[] texIndices2 = readInts(data);
		int[] texIndices3 = readInts(data);
		checkTextureTriangles(id, vx.length, i1.length, textureCoords,
			texIndices1, texIndices2, texIndices3);

		int[][] vertexGroups = readIntMatrix(data);

		return new RetroMesh(id, priority, vx, vy, vz, i1, i2, i3,
			colors, renderTypes, transparencies, priorities, textures,
			textureCoords, texIndices1, texIndices2, texIndices3, vertexGroups);
	}

	/**
	 * Refuses a mesh whose texture mapping cannot be drawn.
	 *
	 * <p>The per-face triangle index and the triangles themselves are separate blocks, so nothing
	 * else pairs them: an index past the end of the triangle table reads cleanly here and throws
	 * inside {@code ModelUploader.computeUv} on the first frame that draws the face. Same contract
	 * as the magic and the version - a bundle that is wrong must not load.
	 */
	private static void checkTextureTriangles(int id, int verticesCount, int faceCount,
		byte[] textureCoords, int[] texIndices1, int[] texIndices2, int[] texIndices3)
		throws IOException
	{
		boolean anyNull = texIndices1 == null || texIndices2 == null || texIndices3 == null;
		boolean allNull = texIndices1 == null && texIndices2 == null && texIndices3 == null;
		if (anyNull && !allNull)
		{
			throw new IOException("Retro asset mesh " + id
				+ " has a partial texture triangle table; regenerate the bundle");
		}

		if (!allNull && (texIndices1.length != texIndices2.length
			|| texIndices1.length != texIndices3.length))
		{
			throw new IOException("Retro asset mesh " + id + " names "
				+ texIndices1.length + ", " + texIndices2.length + " and " + texIndices3.length
				+ " texture triangle corners; regenerate the bundle");
		}

		if (!allNull)
		{
			// A corner is a vertex index, and the merge shifts it without re-checking. One past the
			// end reads out of the vertex arrays inside computeUv, a frame after the bundle loaded
			for (int triangle = 0; triangle < texIndices1.length; triangle++)
			{
				checkCorner(id, triangle, texIndices1[triangle], verticesCount);
				checkCorner(id, triangle, texIndices2[triangle], verticesCount);
				checkCorner(id, triangle, texIndices3[triangle], verticesCount);
			}
		}

		if (textureCoords == null)
		{
			return;
		}

		if (textureCoords.length != faceCount)
		{
			throw new IOException("Retro asset mesh " + id + " maps " + textureCoords.length
				+ " faces to texture triangles but has " + faceCount
				+ " faces; regenerate the bundle");
		}

		int triangles = allNull ? 0 : texIndices1.length;
		for (byte coord : textureCoords)
		{
			// -1 is the renderer's own face-as-UV projection and names no triangle
			if (coord != -1 && (coord & 0xFF) >= triangles)
			{
				throw new IOException("Retro asset mesh " + id + " maps a face to texture triangle "
					+ (coord & 0xFF) + " of " + triangles + "; regenerate the bundle");
			}
		}
	}

	private static void checkCorner(int id, int triangle, int vertex, int verticesCount)
		throws IOException
	{
		if (vertex < 0 || vertex >= verticesCount)
		{
			throw new IOException("Retro asset mesh " + id + " texture triangle " + triangle
				+ " names vertex " + vertex + " of " + verticesCount + "; regenerate the bundle");
		}
	}

	private static void writeRig(DataOutputStream data, RetroRig rig) throws IOException
	{
		data.writeInt(rig.getId());
		writeInts(data, rig.getTypes());
		writeIntMatrix(data, rig.getAllGroups());
	}

	private static RetroRig readRig(DataInputStream data) throws IOException
	{
		int id = data.readInt();
		int[] types = readInts(data);
		int[][] groups = readIntMatrix(data);

		// The two are one table read as two blocks, and nothing downstream re-checks them: the
		// skinner bounds its loop on the type count and then indexes the groups with it, so a short
		// groups block reads cleanly here and throws inside the render path instead. Refusing it is
		// the same contract the magic and version guard - a bundle that is wrong must not load.
		if (types == null || groups == null || types.length != groups.length)
		{
			throw new IOException("Retro asset rig " + id + " names "
				+ (types == null ? "no" : String.valueOf(types.length)) + " transforms but "
				+ (groups == null ? "no" : String.valueOf(groups.length))
				+ " group sets; regenerate the bundle");
		}

		return new RetroRig(id, types, groups);
	}

	private static void writeClip(DataOutputStream data, RetroClip clip) throws IOException
	{
		data.writeInt(clip.getSequenceId());
		data.writeInt(clip.getRigId());
		writeIntMatrix(data, clip.getTransforms());
		writeIntMatrix(data, clip.getAllDx());
		writeIntMatrix(data, clip.getAllDy());
		writeIntMatrix(data, clip.getAllDz());
	}

	private static RetroClip readClip(DataInputStream data) throws IOException
	{
		int sequenceId = data.readInt();
		int rigId = data.readInt();
		int[][] transforms = readIntMatrix(data);
		int[][] dx = readIntMatrix(data);
		int[][] dy = readIntMatrix(data);
		int[][] dz = readIntMatrix(data);
		return new RetroClip(sequenceId, rigId, transforms, dx, dy, dz);
	}

	// A length of -1 encodes null, which is distinct from an empty array: a null transparency array
	// is what puts a model on the opaque render path, so the difference has to survive a round trip.

	private static void writeFloats(DataOutputStream data, float[] values) throws IOException
	{
		if (values == null)
		{
			data.writeInt(-1);
			return;
		}
		data.writeInt(values.length);
		for (float value : values)
		{
			data.writeFloat(value);
		}
	}

	private static float[] readFloats(DataInputStream data) throws IOException
	{
		int length = data.readInt();
		if (length < 0)
		{
			return null;
		}
		checkLength(length);
		float[] values = new float[length];
		for (int i = 0; i < length; i++)
		{
			values[i] = data.readFloat();
		}
		return values;
	}

	private static void writeInts(DataOutputStream data, int[] values) throws IOException
	{
		if (values == null)
		{
			data.writeInt(-1);
			return;
		}
		data.writeInt(values.length);
		for (int value : values)
		{
			data.writeInt(value);
		}
	}

	private static int[] readInts(DataInputStream data) throws IOException
	{
		int length = data.readInt();
		if (length < 0)
		{
			return null;
		}
		checkLength(length);
		int[] values = new int[length];
		for (int i = 0; i < length; i++)
		{
			values[i] = data.readInt();
		}
		return values;
	}

	private static void writeShorts(DataOutputStream data, short[] values) throws IOException
	{
		if (values == null)
		{
			data.writeInt(-1);
			return;
		}
		data.writeInt(values.length);
		for (short value : values)
		{
			data.writeShort(value);
		}
	}

	private static short[] readShorts(DataInputStream data) throws IOException
	{
		int length = data.readInt();
		if (length < 0)
		{
			return null;
		}
		checkLength(length);
		short[] values = new short[length];
		for (int i = 0; i < length; i++)
		{
			values[i] = data.readShort();
		}
		return values;
	}

	private static void writeBytes(DataOutputStream data, byte[] values) throws IOException
	{
		if (values == null)
		{
			data.writeInt(-1);
			return;
		}
		data.writeInt(values.length);
		data.write(values);
	}

	private static byte[] readBytes(DataInputStream data) throws IOException
	{
		int length = data.readInt();
		if (length < 0)
		{
			return null;
		}
		checkLength(length);
		byte[] values = new byte[length];
		data.readFully(values);
		return values;
	}

	private static void writeIntMatrix(DataOutputStream data, int[][] values) throws IOException
	{
		if (values == null)
		{
			data.writeInt(-1);
			return;
		}
		data.writeInt(values.length);
		for (int[] row : values)
		{
			writeInts(data, row);
		}
	}

	private static int[][] readIntMatrix(DataInputStream data) throws IOException
	{
		int length = data.readInt();
		if (length < 0)
		{
			return null;
		}
		checkLength(length);
		int[][] values = new int[length][];
		for (int i = 0; i < length; i++)
		{
			values[i] = readInts(data);
		}
		return values;
	}

	private static int readCount(DataInputStream data, int max) throws IOException
	{
		int count = data.readInt();
		if (count < 0 || count > max)
		{
			throw new IOException("Implausible entry count " + count + " in retro asset bundle");
		}
		return count;
	}

	private static void checkLength(int length) throws IOException
	{
		if (length > MAX_ARRAY)
		{
			throw new IOException("Implausible array length " + length + " in retro asset bundle");
		}
	}
}
