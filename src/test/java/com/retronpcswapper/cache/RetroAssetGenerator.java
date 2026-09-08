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

import com.retronpcswapper.inject.RetroAssetBundle;
import com.retronpcswapper.inject.RetroAssetCodec;
import com.retronpcswapper.inject.RetroClip;
import com.retronpcswapper.inject.RetroMesh;
import com.retronpcswapper.inject.RetroRig;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import net.runelite.cache.ConfigType;
import net.runelite.cache.IndexType;
import net.runelite.cache.definitions.FrameDefinition;
import net.runelite.cache.definitions.FramemapDefinition;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.SequenceDefinition;
import net.runelite.cache.definitions.loaders.FrameLoader;
import net.runelite.cache.definitions.loaders.FramemapLoader;
import net.runelite.cache.definitions.loaders.ModelLoader;
import net.runelite.cache.definitions.loaders.SequenceLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.ArchiveFiles;
import net.runelite.cache.fs.FSFile;
import net.runelite.cache.fs.Store;

/**
 * Builds the shipped asset bundle: geometry the plugin injects, plus the rigs and clips that
 * animate it.
 *
 * <p>Meshes come from whichever cache still has them - the 2005 one for assets the live cache
 * overwrote, the live one otherwise. Clips are sourced the same way, and for the same reason: the
 * live sequences the dragons and demons name survived by id but had their frames re-authored for
 * the modern rigs, so only the 2005 frames fit those meshes. The skeleton stays on live clips
 * deliberately - it exists in both caches, which is what lets {@code RetroModelCache.verifySkinning}
 * check the skinner against the client's own {@code applyTransformations}.
 *
 * <p>The frame index the plugin is handed at draw time is an index into the <em>live</em> sequence,
 * because the client plays live sequence N even when the plugin sets it as a retro id. A 2005 clip
 * has its own, usually smaller, frame count, so {@link #resample} maps one onto the other here
 * rather than at runtime - which is why nothing in {@code src/main} has to know where a clip came
 * from.
 *
 * <p>Run with {@code ./gradlew generateRetroAssets}. Dev tooling: reads caches outside
 * {@code .runelite} and prints to the console, neither of which the shipped plugin does.
 */
public class RetroAssetGenerator
{
	private static final String CACHE_DIR_PROPERTY = "retronpcswapper.cacheDir";
	private static final String RETRO_DIR_PROPERTY = "retronpcswapper.retroDir";
	private static final String RETRO_CACHE_DIR = "retrocache/2005cache";

	/** Index 1 of the RS2 cache holds models. */
	private static final int RETRO_MODEL_INDEX = 1;

	private static final Path OUTPUT_PATH =
		Paths.get("src/main/resources/com/retronpcswapper/retro-assets.dat");

	/**
	 * What to bundle. Model ids are merged into a single mesh when an NPC is built from parts, and
	 * stored under the first part's id.
	 */
	private static final List<Spec> SPECS = Arrays.asList(
		// Not shipped for its own sake - the skeleton exists in both caches and the client can
		// animate it, which makes it the only mesh the skinner can be checked against in game.
		// Its clips stay live for the same reason.
		new Spec("Skeleton", Source.LIVE, Source.LIVE, new int[]{2944}, new int[]{262, 259}),
		new Spec("Adult dragons", Source.RETRO, Source.RETRO, new int[]{2853, 2854}, new int[]{79, 80, 89, 90, 91, 92}),
		new Spec("Lesser demons", Source.RETRO, Source.RETRO, new int[]{2943}, new int[]{63, 64, 65, 66, 67, 69}),
		new Spec("Greater and black demons", Source.RETRO, Source.RETRO, new int[]{2942}, new int[]{63, 64, 65, 66, 67, 68, 69}),
		// The imp mesh survived the 2006 update untouched - only its frames were re-authored. The
		// geometry still comes from 2005, because identical geometry is no guarantee of an identical
		// rig, and it is the vertex groups that have to match the 2005 framemap.
		new Spec("Imps", Source.RETRO, Source.RETRO, new int[]{2887}, new int[]{168, 169, 170, 171, 172}),
		// Mesh 2998 drifted rather than being replaced - 250 verts became 252 - so compareRetroModels
		// calls it REPLACED on a magnitude it should not. The 2005 copy is taken anyway, and it also
		// drops the live-only colour indices 53 and 70, which no 2005 recolour pair names and which
		// would otherwise stay grey on the body.
		new Spec("Baby dragons", Source.RETRO, Source.RETRO, new int[]{2998}, new int[]{21, 25, 26, 27, 28})
	);

	private enum Source
	{
		LIVE,
		RETRO
	}

	public static void main(String[] args) throws IOException
	{
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

		try (Store store = new Store(liveDir))
		{
			store.load();

			RetroAssetBundle bundle = build(store, retro);
			write(bundle);
		}
		finally
		{
			retro.close();
		}
	}

	static RetroAssetBundle build(Store store, RetroCacheReader retro) throws IOException
	{
		Map<Integer, RetroMesh> meshes = new LinkedHashMap<>();
		Map<Integer, RetroRig> rigs = new LinkedHashMap<>();
		Map<Integer, RetroClip> clips = new LinkedHashMap<>();

		RetroFrameIndex retroFrames = null;
		Map<Integer, RetroSeqDefinition> retroSequences = null;
		if (SPECS.stream().anyMatch(spec -> spec.clipSource == Source.RETRO))
		{
			retroFrames = RetroFrameDecoder.decodeAll(retro);
			retroSequences = decodeRetroSequences(retro);
			System.out.println("2005 animations: " + retroFrames.getFrameCount() + " frames in "
				+ retroFrames.getGroups().size() + " groups, " + retroSequences.size() + " sequences");
		}

		for (Spec spec : SPECS)
		{
			List<ModelDefinition> parts = new ArrayList<>();
			for (int modelId : spec.modelIds)
			{
				ModelDefinition part = spec.source == Source.RETRO
					? decodeRetroModel(retro, modelId)
					: decodeLiveModel(store, modelId);

				if (part == null)
				{
					System.err.println("  " + spec.label + ": model " + modelId + " could not be decoded");
					continue;
				}
				parts.add(part);
			}

			if (parts.isEmpty())
			{
				System.err.println(spec.label + ": no geometry, skipped");
				continue;
			}

			int meshId = spec.modelIds[0];
			RetroMesh mesh = toMesh(meshId, parts);
			meshes.put(meshId, mesh);

			System.out.println(spec.label + "  mesh " + meshId
				+ "  verts=" + mesh.getVerticesCount()
				+ " faces=" + mesh.getFaceCount()
				+ " rigged=" + mesh.isRigged()
				+ (parts.size() > 1 ? "  (merged from " + parts.size() + " parts)" : ""));

			for (int sequenceId : spec.sequenceIds)
			{
				RetroClip clip = spec.clipSource == Source.RETRO
					? buildRetroClip(store, sequenceId, retroFrames, retroSequences, rigs)
					: buildClip(store, sequenceId, rigs);

				if (clip == null)
				{
					System.err.println("  sequence " + sequenceId + " could not be decoded");
					continue;
				}

				clips.put(sequenceId, clip);
				System.out.println("  clip " + sequenceId + "  frames=" + clip.getFrameCount()
					+ " rig=" + clip.getRigId()
					+ (spec.clipSource == Source.RETRO ? "  (2005)" : ""));
			}
		}

		return new RetroAssetBundle(meshes, rigs, clips);
	}

	/**
	 * Merges the parts of a multi-model NPC into one mesh, offsetting face indices and vertex groups
	 * so the parts share a coordinate and rig space.
	 *
	 * <p>Done here rather than at runtime because it is a pure transformation of cache data, and
	 * because the client's own {@code mergeModels} is not available for geometry the client never
	 * decoded.
	 */
	private static RetroMesh toMesh(int meshId, List<ModelDefinition> parts)
	{
		int totalVertices = 0;
		int totalFaces = 0;
		int groupCount = 0;
		boolean anyRenderTypes = false;
		boolean anyTransparencies = false;
		boolean anyPriorities = false;
		boolean anyTextures = false;

		for (ModelDefinition part : parts)
		{
			part.computeAnimationTables();
			totalVertices += part.vertexCount;
			totalFaces += part.faceCount;
			int[][] groups = part.getVertexGroups();
			if (groups != null)
			{
				groupCount = Math.max(groupCount, groups.length);
			}
			anyRenderTypes |= part.faceRenderTypes != null;
			anyTransparencies |= part.faceTransparencies != null;
			anyPriorities |= part.faceRenderPriorities != null;
			anyTextures |= part.faceTextures != null;
		}

		float[] vx = new float[totalVertices];
		float[] vy = new float[totalVertices];
		float[] vz = new float[totalVertices];
		int[] i1 = new int[totalFaces];
		int[] i2 = new int[totalFaces];
		int[] i3 = new int[totalFaces];
		short[] colors = new short[totalFaces];
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
		for (ModelDefinition part : parts)
		{
			for (int v = 0; v < part.vertexCount; v++)
			{
				vx[vertexBase + v] = part.vertexX[v];
				vy[vertexBase + v] = part.vertexY[v];
				vz[vertexBase + v] = part.vertexZ[v];
			}

			for (int f = 0; f < part.faceCount; f++)
			{
				int face = faceBase + f;
				i1[face] = part.faceIndices1[f] + vertexBase;
				i2[face] = part.faceIndices2[f] + vertexBase;
				i3[face] = part.faceIndices3[f] + vertexBase;
				colors[face] = part.faceColors[f];

				if (renderTypes != null)
				{
					renderTypes[face] = part.faceRenderTypes == null ? 0 : part.faceRenderTypes[f];
				}
				if (transparencies != null)
				{
					transparencies[face] = part.faceTransparencies == null ? 0 : part.faceTransparencies[f];
				}
				if (priorities != null)
				{
					priorities[face] = part.faceRenderPriorities == null
						? part.priority : part.faceRenderPriorities[f];
				}
				if (textures != null)
				{
					textures[face] = part.faceTextures == null ? -1 : part.faceTextures[f];
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

			vertexBase += part.vertexCount;
			faceBase += part.faceCount;
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

		return new RetroMesh(meshId, vx, vy, vz, i1, i2, i3,
			colors, renderTypes, transparencies, priorities, textures, vertexGroups);
	}

	/**
	 * Decodes one sequence into a clip, registering the rig it references.
	 *
	 * <p>A frame names its own framemap in its first two bytes, which is why the framemap has to be
	 * loaded before the frame can be.
	 */
	static RetroClip buildClip(Store store, int sequenceId, Map<Integer, RetroRig> rigs)
		throws IOException
	{
		SequenceDefinition sequence = loadSequence(store, sequenceId);
		if (sequence == null || sequence.frameIDs == null || sequence.frameIDs.length == 0)
		{
			return null;
		}

		int frameCount = sequence.frameIDs.length;
		int[][] transforms = new int[frameCount][];
		int[][] dx = new int[frameCount][];
		int[][] dy = new int[frameCount][];
		int[][] dz = new int[frameCount][];

		int rigId = -1;
		for (int i = 0; i < frameCount; i++)
		{
			int packed = sequence.frameIDs[i];
			byte[] frameData = loadFile(store, IndexType.ANIMATIONS, packed >> 16, packed & 0xFFFF);
			if (frameData == null || frameData.length < 2)
			{
				transforms[i] = new int[0];
				dx[i] = new int[0];
				dy[i] = new int[0];
				dz[i] = new int[0];
				continue;
			}

			int framemapId = ((frameData[0] & 0xFF) << 8) | (frameData[1] & 0xFF);
			FramemapDefinition framemap = loadFramemap(store, framemapId, rigs);
			if (framemap == null)
			{
				return null;
			}

			if (rigId == -1)
			{
				rigId = framemapId;
			}
			else if (rigId != framemapId)
			{
				// Every clip this bundle carries uses one rig throughout. A sequence that switched
				// rigs mid-animation would need a per-frame rig id, so fail loudly rather than
				// silently animate against the wrong skeleton.
				throw new IOException("Sequence " + sequenceId + " mixes framemaps "
					+ rigId + " and " + framemapId + "; the clip format assumes one per clip");
			}

			FrameDefinition frame = new FrameLoader().load(framemap, packed & 0xFFFF, frameData);
			transforms[i] = frame.indexFrameIds;
			dx[i] = frame.translator_x;
			dy[i] = frame.translator_y;
			dz[i] = frame.translator_z;
		}

		return rigId == -1 ? null : new RetroClip(sequenceId, rigId, transforms, dx, dy, dz);
	}

	/**
	 * Builds a clip from the 2005 frames, resampled onto the live sequence's frame count.
	 *
	 * <p>A 2005 sequence names its frames by a flat id that says nothing about where they live, so
	 * the file - which is also what owns the rig - comes from {@link RetroFrameIndex}.
	 */
	static RetroClip buildRetroClip(Store store, int sequenceId, RetroFrameIndex frames,
		Map<Integer, RetroSeqDefinition> sequences, Map<Integer, RetroRig> rigs) throws IOException
	{
		RetroSeqDefinition retroSeq = sequences.get(sequenceId);
		if (retroSeq == null || retroSeq.getFrameIds() == null || retroSeq.getFrameIds().length == 0)
		{
			return null;
		}

		int[] retroFrameIds = retroSeq.getFrameIds();

		// The client plays the live sequence at this id, so its frame count is what the plugin will
		// be handed at draw time. Without one there is nothing to resample onto.
		SequenceDefinition liveSequence = loadSequence(store, sequenceId);
		int liveCount = liveSequence == null || liveSequence.frameIDs == null
			? retroFrameIds.length
			: liveSequence.frameIDs.length;

		int rigId = -1;
		RetroFramemapDefinition rig = null;
		for (int retroFrameId : retroFrameIds)
		{
			RetroFramemapDefinition framemap = frames.getFramemapForFrame(retroFrameId);
			if (framemap == null)
			{
				System.err.println("  sequence " + sequenceId + ": 2005 frame " + retroFrameId
					+ " is in no index 2 file");
				return null;
			}

			if (rigId == -1)
			{
				rigId = framemap.getId();
				rig = framemap;
			}
			else if (rigId != framemap.getId() && !sameRig(rig, framemap))
			{
				// Every clip this bundle carries uses one rig throughout. Two files can hold the
				// same skeleton, which is harmless; genuinely different ones are not.
				throw new IOException("2005 sequence " + sequenceId + " mixes framemaps "
					+ rigId + " and " + framemap.getId() + "; the clip format assumes one per clip");
			}
		}

		rigs.putIfAbsent(rigId, new RetroRig(rigId, rig.getTypes(), rig.getGroups()));

		int[] mapping = resample(
			liveSequence == null ? null : liveSequence.frameLengths,
			retroSeq.getFrameLengths(),
			liveCount,
			retroFrameIds.length);

		int[][] transforms = new int[liveCount][];
		int[][] dx = new int[liveCount][];
		int[][] dy = new int[liveCount][];
		int[][] dz = new int[liveCount][];

		for (int i = 0; i < liveCount; i++)
		{
			RetroFrameDefinition frame = frames.getFrame(retroFrameIds[mapping[i]]);
			transforms[i] = frame.getIndexFrameIds();
			dx[i] = frame.getTranslatorX();
			dy[i] = frame.getTranslatorY();
			dz[i] = frame.getTranslatorZ();
		}

		return new RetroClip(sequenceId, rigId, transforms, dx, dy, dz);
	}

	private static boolean sameRig(RetroFramemapDefinition a, RetroFramemapDefinition b)
	{
		if (!Arrays.equals(a.getTypes(), b.getTypes()))
		{
			return false;
		}

		return Arrays.deepEquals(a.getGroups(), b.getGroups());
	}

	/**
	 * Maps each live frame onto the 2005 frame at the same point in the cycle, so a 2005 clip plays
	 * over the live sequence's duration however many frames it actually has.
	 *
	 * <p>Weighted by duration where both sides declare one, since neither cache holds frames of
	 * uniform length; proportional by index otherwise, which is the same answer when they are
	 * uniform.
	 */
	static int[] resample(int[] liveLengths, int[] retroLengths, int liveCount, int retroCount)
	{
		int[] mapping = new int[liveCount];

		long liveTotal = total(liveLengths, liveCount);
		long retroTotal = total(retroLengths, retroCount);

		if (liveTotal <= 0 || retroTotal <= 0)
		{
			for (int i = 0; i < liveCount; i++)
			{
				mapping[i] = (int) ((long) i * retroCount / liveCount);
			}
			return mapping;
		}

		long elapsed = 0;
		for (int i = 0; i < liveCount; i++)
		{
			// The midpoint of the live frame, so a frame is matched to the pose it spends most of
			// its time nearest rather than to whatever happens to start at its leading edge
			long midpoint = 2 * elapsed + liveLengths[i];
			long target = midpoint * retroTotal / (2 * liveTotal);

			long retroElapsed = 0;
			int chosen = retroCount - 1;
			for (int j = 0; j < retroCount; j++)
			{
				retroElapsed += retroLengths[j];
				if (target < retroElapsed)
				{
					chosen = j;
					break;
				}
			}

			mapping[i] = chosen;
			elapsed += liveLengths[i];
		}

		return mapping;
	}

	private static long total(int[] lengths, int count)
	{
		if (lengths == null || lengths.length < count)
		{
			return 0;
		}

		long sum = 0;
		for (int i = 0; i < count; i++)
		{
			if (lengths[i] < 0)
			{
				return 0;
			}
			sum += lengths[i];
		}
		return sum;
	}

	static Map<Integer, RetroSeqDefinition> decodeRetroSequences(RetroCacheReader retro)
	{
		Map<String, byte[]> config = retro.readArchive(retro.readFile(0, 2));
		byte[] seqDat = config.get(String.valueOf(RetroCacheReader.hashFileName("seq.dat")));
		byte[] seqIdx = config.get(String.valueOf(RetroCacheReader.hashFileName("seq.idx")));
		return RetroSeqDecoder.decodeAll(seqDat, seqIdx);
	}

	private static FramemapDefinition loadFramemap(Store store, int framemapId, Map<Integer, RetroRig> rigs)
		throws IOException
	{
		byte[] data = loadFile(store, IndexType.SKELETONS, framemapId, 0);
		if (data == null)
		{
			return null;
		}

		FramemapDefinition framemap = new FramemapLoader().load(framemapId, data);
		rigs.putIfAbsent(framemapId, new RetroRig(framemapId, framemap.types, framemap.frameMaps));
		return framemap;
	}

	static SequenceDefinition loadSequence(Store store, int sequenceId) throws IOException
	{
		byte[] data = loadFile(store, IndexType.CONFIGS, ConfigType.SEQUENCE.getId(), sequenceId);
		if (data == null)
		{
			return null;
		}

		return new SequenceLoader()
			.configureForRevision(store.getIndex(IndexType.CONFIGS).getRevision())
			.load(sequenceId, data);
	}

	static byte[] loadFile(Store store, IndexType indexType, int archiveId, int fileId)
		throws IOException
	{
		Archive archive = store.getIndex(indexType).getArchive(archiveId);
		if (archive == null)
		{
			return null;
		}

		byte[] container = store.getStorage().loadArchive(archive);
		if (container == null)
		{
			return null;
		}

		if (archive.getFileData() != null && archive.getFileData().length == 1)
		{
			return archive.decompress(container);
		}

		ArchiveFiles files = archive.getFiles(container);
		FSFile file = files.findFile(fileId);
		return file == null ? null : file.getContents();
	}

	private static ModelDefinition decodeLiveModel(Store store, int modelId)
	{
		try
		{
			Archive archive = store.getIndex(IndexType.MODELS).getArchive(modelId);
			if (archive == null)
			{
				return null;
			}
			byte[] data = archive.decompress(store.getStorage().loadArchive(archive));
			return data == null ? null : new ModelLoader().load(modelId, data);
		}
		catch (IOException | RuntimeException e)
		{
			return null;
		}
	}

	private static ModelDefinition decodeRetroModel(RetroCacheReader retro, int modelId)
	{
		try
		{
			byte[] data = gunzipIfNeeded(retro.readFile(RETRO_MODEL_INDEX, modelId));
			return data == null ? null : new ModelLoader().load(modelId, data);
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	/** Some RS2 index entries are gzip wrapped and some are stored raw, so sniff rather than guess. */
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

	private static void write(RetroAssetBundle bundle) throws IOException
	{
		Files.createDirectories(OUTPUT_PATH.getParent());
		try (OutputStream out = Files.newOutputStream(OUTPUT_PATH))
		{
			RetroAssetCodec.write(bundle, out);
		}

		System.out.println();
		System.out.println("Wrote " + OUTPUT_PATH.toAbsolutePath()
			+ "  (" + Files.size(OUTPUT_PATH) / 1024 + " KB, " + bundle + ")");
	}

	private static File resolveRetroCacheDir()
	{
		String configured = System.getProperty(RETRO_DIR_PROPERTY);
		return configured == null || configured.isEmpty()
			? new File(RETRO_CACHE_DIR)
			: new File(configured);
	}

	static File resolveLiveCacheDir()
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

	/** One bundled entry: where its geometry comes from, and which sequences drive it. */
	private static final class Spec
	{
		private final String label;
		private final Source source;
		private final Source clipSource;
		private final int[] modelIds;
		private final int[] sequenceIds;

		private Spec(String label, Source source, Source clipSource, int[] modelIds, int[] sequenceIds)
		{
			this.label = label;
			this.source = source;
			this.clipSource = clipSource;
			this.modelIds = modelIds;
			this.sequenceIds = sequenceIds;
		}
	}
}
