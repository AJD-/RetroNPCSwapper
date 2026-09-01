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
package com.retronpcswapper.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import net.runelite.cache.IndexType;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.loaders.ModelLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.Index;
import net.runelite.cache.fs.Storage;
import net.runelite.cache.fs.Store;

/**
 * Dev-only tool that answers the question every disabled category turns on: <em>is the model at
 * this id still the 2005 asset, or was the id reused for something else?</em>
 *
 * <p>Model ids were append-only across the RS2 lineage, so every 2005 id still resolves in the live
 * cache - but resolving is not the same as being the same mesh. This decodes the geometry from both
 * caches and compares vertex count, face count and the palette.
 *
 * <p>The comparison is exact, so read a REPLACED verdict by its magnitude rather than as a yes/no.
 * Some shipped, working categories were touched up in place and report REPLACED on a hair: the
 * chicken (2849) is 87 verts either way but 134 faces against 135, and the hill giant torso (2870)
 * is 177 verts against 355 faces vs 347. A genuinely reused id looks nothing like that - model 2943
 * went from 428 verts to 1000, and 2942 from 479 to 68. Equal vertex counts with a near-equal face
 * count and a superset palette mean preserved-and-touched-up; a different vertex count means the id
 * was reused. The skeleton (2944) is the exact-match reference.
 *
 * <p>Byte comparison cannot answer this. Jagex re-encoded every model when the format gained v2/v3
 * markers, so the preserved chicken differs byte-for-byte from its 2005 self while being the same
 * mesh.
 *
 * <p>When a model was replaced, {@code -Pfindmoved} rescans the whole live model index for the 2005
 * geometry, to tell a mesh that moved to a new id from one that is simply gone.
 *
 * <p>Run with {@code ./gradlew compareRetroModels -Pmodels=2853,2854}, optionally
 * {@code -Pfindmoved} and {@code -PcacheDir=...}. Reads the untracked {@code retrocache/} 2005
 * cache alongside the live one. This class lives in the test sourceSet and is never shipped, so
 * console output and reading files outside {@code .runelite} are fine here.
 */
public class RetroModelComparator
{
	private static final String CACHE_DIR_PROPERTY = "retronpcswapper.cacheDir";
	private static final String FIND_MOVED_PROPERTY = "retronpcswapper.findmoved";
	private static final String RETRO_CACHE_DIR = "retrocache/2005cache";

	/** Index 1 of the RS2 cache holds models. */
	private static final int RETRO_MODEL_INDEX = 1;

	public static void main(String[] args) throws IOException
	{
		if (args.length == 0)
		{
			System.out.println("Usage: ./gradlew compareRetroModels -Pmodels=2853,2854 [-Pfindmoved]");
			return;
		}

		File liveDir = resolveLiveCacheDir();
		if (liveDir == null)
		{
			System.err.println("Could not find the live OSRS cache; pass one with -PcacheDir=<path>");
			System.exit(1);
			return;
		}

		File retroDir = new File(RETRO_CACHE_DIR);
		RetroCacheReader retro = new RetroCacheReader(retroDir);
		if (!retro.init())
		{
			System.err.println("Could not read the 2005 cache at " + retroDir.getAbsolutePath()
				+ " - it is untracked, so copy it in before running this.");
			System.exit(1);
			return;
		}

		List<Geometry> replaced = new ArrayList<>();

		try (Store store = new Store(liveDir))
		{
			store.load();

			System.out.println("Live cache:  " + liveDir);
			System.out.println("2005 cache:  " + retroDir.getAbsolutePath());
			System.out.println();

			for (String arg : args)
			{
				String trimmed = arg.trim();
				if (trimmed.isEmpty())
				{
					continue;
				}

				Geometry lost = compare(store, retro, Integer.parseInt(trimmed));
				if (lost != null)
				{
					replaced.add(lost);
				}
			}

			if (replaced.isEmpty())
			{
				return;
			}

			if (Boolean.getBoolean(FIND_MOVED_PROPERTY))
			{
				findMoved(store, replaced);
			}
			else
			{
				System.out.println("Re-run with -Pfindmoved to scan the live index for the replaced meshes.");
			}
		}
		finally
		{
			retro.close();
		}
	}

	/**
	 * Prints both versions of one model id. Returns the 2005 geometry when the asset was replaced,
	 * so the caller can go looking for it elsewhere, else null.
	 */
	private static Geometry compare(Store store, RetroCacheReader retro, int modelId) throws IOException
	{
		Geometry live = decode(modelId, loadLive(store, modelId));
		Geometry old = decode(modelId, gunzipIfNeeded(retro.readFile(RETRO_MODEL_INDEX, modelId)));

		System.out.println("model " + modelId);
		System.out.println("  2005  " + describe(old));
		System.out.println("  live  " + describe(live));
		System.out.println("  ->    " + verdict(old, live));
		System.out.println();

		return old != null && live != null && !old.sameShapeAs(live) ? old : null;
	}

	private static String verdict(Geometry old, Geometry live)
	{
		if (old == null)
		{
			return "NOT IN THE 2005 CACHE";
		}
		if (live == null)
		{
			return "ABSENT FROM THE LIVE CACHE";
		}
		if (old.sameShapeAs(live))
		{
			return "PRESERVED - same mesh, safe to substitute";
		}
		return "REPLACED - the live id holds different geometry, not the retro asset";
	}

	/**
	 * Decodes every model in the live index looking for the replaced geometry, to tell a mesh that
	 * moved to a new id from one that is simply gone.
	 */
	private static void findMoved(Store store, List<Geometry> replaced) throws IOException
	{
		Index index = store.getIndex(IndexType.MODELS);
		Storage storage = store.getStorage();
		ModelLoader loader = new ModelLoader();

		System.out.println("Scanning " + index.getArchives().size() + " live models for "
			+ replaced.size() + " replaced mesh(es)...");

		boolean[] found = new boolean[replaced.size()];
		for (Archive archive : index.getArchives())
		{
			int id = archive.getArchiveId();
			Geometry candidate;
			try
			{
				candidate = Geometry.of(loader.load(id, archive.decompress(storage.loadArchive(archive))));
			}
			catch (RuntimeException e)
			{
				continue;
			}

			for (int i = 0; i < replaced.size(); i++)
			{
				if (replaced.get(i).sameShapeAs(candidate))
				{
					System.out.println("  " + replaced.get(i).sourceId
						+ " -> the 2005 mesh is at live id " + id);
					found[i] = true;
				}
			}
		}

		for (int i = 0; i < replaced.size(); i++)
		{
			if (!found[i])
			{
				System.out.println("  " + replaced.get(i).sourceId
					+ " -> not present anywhere in the live cache");
			}
		}
		System.out.println();
	}

	private static byte[] loadLive(Store store, int modelId) throws IOException
	{
		Archive archive = store.getIndex(IndexType.MODELS).getArchive(modelId);
		if (archive == null)
		{
			return null;
		}
		return archive.decompress(store.getStorage().loadArchive(archive));
	}

	/**
	 * Some RS2 index entries are gzip wrapped and some are stored raw, so sniff rather than guess.
	 */
	private static byte[] gunzipIfNeeded(byte[] data)
	{
		if (data == null || data.length < 2 || (data[0] & 0xFF) != 0x1F || (data[1] & 0xFF) != 0x8B)
		{
			return data;
		}

		try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(data)))
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int read;
			while ((read = in.read(buffer)) > 0)
			{
				out.write(buffer, 0, read);
			}
			return out.toByteArray();
		}
		catch (IOException e)
		{
			return data;
		}
	}

	private static Geometry decode(int modelId, byte[] data)
	{
		if (data == null)
		{
			return null;
		}

		try
		{
			Geometry geometry = Geometry.of(new ModelLoader().load(modelId, data));
			geometry.byteLength = data.length;
			return geometry;
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	private static String describe(Geometry geometry)
	{
		if (geometry == null)
		{
			return "absent";
		}
		return geometry.byteLength + "B  verts=" + geometry.vertexCount
			+ " faces=" + geometry.faceCount
			+ " colors=" + Arrays.toString(geometry.colors);
	}

	private static File resolveLiveCacheDir()
	{
		String configured = System.getProperty(CACHE_DIR_PROPERTY);
		if (configured != null && !configured.isEmpty())
		{
			File dir = new File(configured);
			return dir.isDirectory() ? dir : null;
		}

		File dir = new File(System.getProperty("user.home"), ".runelite/jagexcache/oldschool/LIVE");
		return dir.isDirectory() ? dir : null;
	}

	/**
	 * The parts of a mesh that survive re-encoding. Vertex and face counts plus the palette
	 * identify an asset; the encoded bytes do not.
	 */
	private static final class Geometry
	{
		private int sourceId;
		private int byteLength;
		private int vertexCount;
		private int faceCount;
		private short[] colors;

		private static Geometry of(ModelDefinition model)
		{
			Geometry geometry = new Geometry();
			geometry.sourceId = model.id;
			geometry.vertexCount = model.vertexCount;
			geometry.faceCount = model.faceCount;

			TreeSet<Short> distinct = new TreeSet<>();
			if (model.faceColors != null)
			{
				for (short color : model.faceColors)
				{
					distinct.add(color);
				}
			}

			geometry.colors = new short[distinct.size()];
			int i = 0;
			for (short color : distinct)
			{
				geometry.colors[i++] = color;
			}
			return geometry;
		}

		private boolean sameShapeAs(Geometry other)
		{
			return other != null
				&& vertexCount == other.vertexCount
				&& faceCount == other.faceCount
				&& Arrays.equals(colors, other.colors);
		}
	}
}
