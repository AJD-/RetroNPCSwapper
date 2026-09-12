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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
	private static final String RETRO_DIR_PROPERTY = "retronpcswapper.retroDir";
	private static final String FIND_MOVED_PROPERTY = "retronpcswapper.findmoved";
	private static final String FACE_DIFF_PROPERTY = "retronpcswapper.facediff";
	private static final String RETRO_CACHE_DIR = "retrocache/2005cache";

	/** Index 1 of the RS2 cache holds models. */
	private static final int RETRO_MODEL_INDEX = 1;

	public static void main(String[] args) throws IOException
	{
		if (args.length == 0)
		{
			System.out.println("Usage: ./gradlew compareRetroModels -Pmodels=2853,2854 [-Pfindmoved] [-Pfacediff]");
			return;
		}

		File liveDir = resolveLiveCacheDir();
		if (liveDir == null)
		{
			System.err.println("Could not find the live OSRS cache; pass one with -PcacheDir=<path>");
			System.exit(1);
			return;
		}

		File retroDir = resolveRetroCacheDir();
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
		byte[] liveData = loadLive(store, modelId);
		byte[] oldData = gunzipIfNeeded(retro.readFile(RETRO_MODEL_INDEX, modelId));

		ModelDefinition liveModel = decodeModel(modelId, liveData);
		ModelDefinition oldModel = decodeModel(modelId, oldData);

		Geometry live = describeOf(liveModel, liveData);
		Geometry old = describeOf(oldModel, oldData);

		System.out.println("model " + modelId);
		System.out.println("  2005  " + describe(old));
		System.out.println("  live  " + describe(live));
		System.out.println("  ->    " + verdict(old, live));

		if (Boolean.getBoolean(FACE_DIFF_PROPERTY))
		{
			printFaceDiff(oldModel, liveModel);
		}
		System.out.println();

		return old != null && live != null && !old.sameShapeAs(live) ? old : null;
	}

	/**
	 * Walks the two decodes face by face and reports every 2005 color that became a different live
	 * color. A distinct-palette diff can only say which colors left and which arrived, never which
	 * became which - and where a whole palette shifted, guessing the pairing by similarity is how a
	 * correction recolor ends up inverted. Face order survives re-encoding when the counts match,
	 * so pairing them positionally is exact.
	 */
	private static void printFaceDiff(ModelDefinition old, ModelDefinition live)
	{
		if (old == null || live == null || old.faceColors == null || live.faceColors == null)
		{
			System.out.println("  face diff: unavailable - one side did not decode");
			return;
		}

		System.out.println("  vertices: " + vertexOverlap(old, live));
		printRigDiff(old, live);

		// Keyed 2005 -> live, counting the faces that agree on the pairing. A 2005 color reaching
		// more than one live color means the faces were not matched up correctly.
		//
		// Two ways to line the faces up, cheapest first. Face order usually survives re-encoding,
		// but not always - model 3341 is re-ordered, and a mesh that gained or lost a face cannot
		// be paired by position at all - so fall back to matching each face by where it sits in
		// space, which survives both the faces and the vertices being renumbered.
		Map<Short, Map<Short, Integer>> pairs = old.faceCount == live.faceCount
			? pairByIndex(old, live)
			: null;

		if (pairs == null || isAmbiguous(pairs))
		{
			System.out.println("  face diff: " + (pairs == null
				? "face counts differ (" + old.faceCount + " vs " + live.faceCount + "), matching by position in space"
				: "face order did not survive, matching by position in space instead"));
			pairs = pairByVertices(old, live);
		}

		if (pairs.isEmpty())
		{
			System.out.println("  face diff: every face kept its 2005 color");
			return;
		}

		System.out.println("  face diff (2005 -> live, by face):");
		for (Map.Entry<Short, Map<Short, Integer>> entry : pairs.entrySet())
		{
			StringBuilder line = new StringBuilder("    " + entry.getKey() + " ->");
			for (Map.Entry<Short, Integer> target : entry.getValue().entrySet())
			{
				line.append(' ').append(target.getKey()).append(" (").append(target.getValue()).append(" faces)");
			}
			if (entry.getValue().size() > 1)
			{
				line.append("   AMBIGUOUS - faces could not be matched up");
			}
			System.out.println(line);
		}
	}

	/**
	 * The share of the 2005 mesh's vertex positions that the live mesh also has, order aside.
	 *
	 * <p>This is the check that settles what the palette cannot. A mesh that survived can still be
	 * re-encoded with a few vertices moved and its whole palette repainted, which reads as
	 * {@code REPLACED} on colors alone - the cow body 3341 is repainted across four of its six
	 * colors and still 98% the same geometry. Read it by magnitude against the calibration points:
	 * the preserved chicken 2849 scores 97%, the undead cow 5237 97%, the skeleton 2944 and the imp
	 * 2887 100%. A genuinely reused id scores nothing like that - 2942 went 479 vertices to 68.
	 *
	 * <p>Requiring an exact match is too strict and reads a surviving mesh as a lost one: three of
	 * 3341's 239 vertices moved.
	 */
	private static String vertexOverlap(ModelDefinition old, ModelDefinition live)
	{
		Map<String, Integer> liveVertices = new HashMap<>();
		for (int i = 0; i < live.vertexCount; i++)
		{
			liveVertices.merge(live.vertexX[i] + "," + live.vertexY[i] + "," + live.vertexZ[i], 1, Integer::sum);
		}

		int shared = 0;
		for (int i = 0; i < old.vertexCount; i++)
		{
			String key = old.vertexX[i] + "," + old.vertexY[i] + "," + old.vertexZ[i];
			Integer remaining = liveVertices.get(key);
			if (remaining != null && remaining > 0)
			{
				liveVertices.put(key, remaining - 1);
				shared++;
			}
		}

		int percent = old.vertexCount == 0 ? 0 : (shared * 100) / old.vertexCount;
		return shared + "/" + old.vertexCount + " of the 2005 positions are in the live mesh (" + percent + "%)";
	}

	/**
	 * Whether the two decodes bind the same geometry to the same transform groups.
	 *
	 * <p>The other half of "is this still the retro asset". A mesh can survive intact and still be
	 * useless to swap to, because its vertex groups were renumbered onto a different rig - the
	 * guard parts are byte-identical in both caches with exactly that done to them. Reach cannot
	 * see it: a renumbered group is still <em>a</em> group the framemap addresses, so
	 * verifyRetroRigs reports 100% while every joint drives the wrong vertices.
	 *
	 * <p>Vertices are matched by position, since re-encoding renumbers them.
	 */
	private static void printRigDiff(ModelDefinition old, ModelDefinition live)
	{
		int[] oldGroups = perVertexGroups(old);
		int[] liveGroups = perVertexGroups(live);
		if (oldGroups == null || liveGroups == null)
		{
			System.out.println("  rig: one side carries no vertex groups");
			return;
		}

		Map<String, Integer> liveByPosition = new HashMap<>();
		for (int i = 0; i < live.vertexCount; i++)
		{
			liveByPosition.put(vertexKey(live, i), liveGroups[i]);
		}

		Map<Integer, Map<Integer, Integer>> pairs = new TreeMap<>();
		int matched = 0;
		int same = 0;
		for (int i = 0; i < old.vertexCount; i++)
		{
			Integer liveGroup = liveByPosition.get(vertexKey(old, i));
			if (liveGroup == null)
			{
				continue;
			}
			matched++;
			if (liveGroup == oldGroups[i])
			{
				same++;
			}
			else
			{
				pairs.computeIfAbsent(oldGroups[i], k -> new TreeMap<>()).merge(liveGroup, 1, Integer::sum);
			}
		}

		if (matched == 0)
		{
			System.out.println("  rig: no shared vertices to compare");
			return;
		}

		int percent = (same * 100) / matched;
		System.out.println("  rig: " + same + "/" + matched + " shared vertices keep their 2005 group ("
			+ percent + "%)" + (percent == 100 ? "" : "  <-- RENUMBERED, the 2005 clips will drive the wrong vertices"));
		for (Map.Entry<Integer, Map<Integer, Integer>> entry : pairs.entrySet())
		{
			StringBuilder line = new StringBuilder("    group " + entry.getKey() + " ->");
			for (Map.Entry<Integer, Integer> target : entry.getValue().entrySet())
			{
				line.append(' ').append(target.getKey()).append(" (").append(target.getValue()).append(" verts)");
			}
			System.out.println(line);
		}
	}

	/**
	 * Inverts {@code getVertexGroups()} - a group-indexed table of vertex indices - into a
	 * vertex-indexed array of group ids. Never read {@code packedVertexGroups}: ModelLoader.load
	 * unpacks it into the table and nulls it, so it is null for every model ever loaded.
	 */
	private static int[] perVertexGroups(ModelDefinition model)
	{
		int[][] table = model.getVertexGroups();
		if (table == null)
		{
			return null;
		}

		int[] byVertex = new int[model.vertexCount];
		Arrays.fill(byVertex, -1);
		for (int group = 0; group < table.length; group++)
		{
			if (table[group] == null)
			{
				continue;
			}
			for (int vertex : table[group])
			{
				if (vertex >= 0 && vertex < byVertex.length)
				{
					byVertex[vertex] = group;
				}
			}
		}
		return byVertex;
	}

	private static String vertexKey(ModelDefinition model, int i)
	{
		return model.vertexX[i] + "," + model.vertexY[i] + "," + model.vertexZ[i];
	}

	private static boolean isAmbiguous(Map<Short, Map<Short, Integer>> pairs)
	{
		for (Map<Short, Integer> targets : pairs.values())
		{
			if (targets.size() > 1)
			{
				return true;
			}
		}
		return false;
	}

	private static Map<Short, Map<Short, Integer>> pairByIndex(ModelDefinition old, ModelDefinition live)
	{
		Map<Short, Map<Short, Integer>> pairs = new TreeMap<>();
		for (int i = 0; i < old.faceCount; i++)
		{
			record(pairs, old.faceColors[i], live.faceColors[i]);
		}
		return pairs;
	}

	/**
	 * Matches faces by where they sit in space rather than by position in the array. Vertex indices
	 * cannot be used directly - re-encoding renumbers the vertex array too - but a face's three
	 * corner coordinates summed per axis is the same number whichever order its corners are stored
	 * in and whichever slot the face occupies.
	 *
	 * <p>Faces whose corners moved match nothing and are counted as unmatched rather than guessed.
	 */
	private static Map<Short, Map<Short, Integer>> pairByVertices(ModelDefinition old, ModelDefinition live)
	{
		Map<String, List<Short>> byCentroid = new HashMap<>();
		for (int i = 0; i < old.faceCount; i++)
		{
			byCentroid.computeIfAbsent(centroidKey(old, i), k -> new ArrayList<>()).add(old.faceColors[i]);
		}

		Map<Short, Map<Short, Integer>> pairs = new TreeMap<>();
		int unmatched = 0;
		for (int i = 0; i < live.faceCount; i++)
		{
			List<Short> candidates = byCentroid.get(centroidKey(live, i));
			if (candidates == null || candidates.isEmpty())
			{
				unmatched++;
				continue;
			}
			record(pairs, candidates.remove(candidates.size() - 1), live.faceColors[i]);
		}

		if (unmatched > 0)
		{
			System.out.println("  face diff: " + unmatched + " live face(s) sit where no 2005 face does");
		}
		return pairs;
	}

	/**
	 * A face's three corners summed per axis - independent of both corner order and face order.
	 */
	private static String centroidKey(ModelDefinition model, int face)
	{
		int a = model.faceIndices1[face];
		int b = model.faceIndices2[face];
		int c = model.faceIndices3[face];
		return (model.vertexX[a] + model.vertexX[b] + model.vertexX[c]) + ","
			+ (model.vertexY[a] + model.vertexY[b] + model.vertexY[c]) + ","
			+ (model.vertexZ[a] + model.vertexZ[b] + model.vertexZ[c]);
	}

	private static void record(Map<Short, Map<Short, Integer>> pairs, short from, short to)
	{
		if (from != to)
		{
			pairs.computeIfAbsent(from, k -> new TreeMap<>()).merge(to, 1, Integer::sum);
		}
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
		return describeOf(decodeModel(modelId, data), data);
	}

	private static ModelDefinition decodeModel(int modelId, byte[] data)
	{
		if (data == null)
		{
			return null;
		}

		try
		{
			return new ModelLoader().load(modelId, data);
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	private static Geometry describeOf(ModelDefinition model, byte[] data)
	{
		if (model == null)
		{
			return null;
		}

		Geometry geometry = Geometry.of(model);
		geometry.byteLength = data.length;
		return geometry;
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

	private static File resolveRetroCacheDir()
	{
		String configured = System.getProperty(RETRO_DIR_PROPERTY);
		return configured == null || configured.isEmpty()
			? new File(RETRO_CACHE_DIR)
			: new File(configured);
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
