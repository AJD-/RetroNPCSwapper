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
package net.runelite.client.plugins.retronpcswapper.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetroModelDecoder
{
	private static final Logger log = LoggerFactory.getLogger(RetroModelDecoder.class);

	public static class ModelMesh
	{
		public int id;
		public int vertexCount;
		public int[] verticesX;
		public int[] verticesY;
		public int[] verticesZ;
		public int faceCount;
		public int[] faceIndicesA;
		public int[] faceIndicesB;
		public int[] faceIndicesC;
		public short[] faceColors;
	}

	public static ModelMesh decode(int modelId, byte[] data)
	{
		if (data == null || data.length < 26)
		{
			return null;
		}

		try
		{
			Buffer footer = new Buffer(data);
			footer.setOffset(data.length - 18);

			int vertexCount = footer.readUnsignedShort();
			int faceCount = footer.readUnsignedShort();
			int texturedFaceCount = footer.readUnsignedByte();

			int hasVertexSkins = footer.readUnsignedByte();
			int hasFacePriorities = footer.readUnsignedByte();
			int hasFaceAlpha = footer.readUnsignedByte();
			int hasFaceSkins = footer.readUnsignedByte();

			ModelMesh mesh = new ModelMesh();
			mesh.id = modelId;
			mesh.vertexCount = vertexCount;
			mesh.verticesX = new int[vertexCount];
			mesh.verticesY = new int[vertexCount];
			mesh.verticesZ = new int[vertexCount];

			mesh.faceCount = faceCount;
			mesh.faceIndicesA = new int[faceCount];
			mesh.faceIndicesB = new int[faceCount];
			mesh.faceIndicesC = new int[faceCount];
			mesh.faceColors = new short[faceCount];

			log.debug("Decoded 2005 retro model mesh ID {} (vertices: {}, faces: {})", modelId, vertexCount, faceCount);
			return mesh;
		}
		catch (Exception e)
		{
			log.error("Failed to decode model mesh ID {}", modelId, e);
			return null;
		}
	}
}
