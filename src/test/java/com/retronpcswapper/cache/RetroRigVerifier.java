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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
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
 * Dev-only gate for injecting 2005 geometry that no longer exists at any live id.
 *
 * <p>Adult dragons and demons lost their meshes to the Aug-2006 graphical update but kept their
 * sequences, which are still keyed to the 2005 framemaps - that is <em>why</em> those sequences
 * survive. So the injection plan is 2005 geometry plus live frames, and the whole plan rests on one
 * assumption that can be measured offline: <em>do the vertex groups baked into the 2005 mesh line
 * up with the framemap the live sequence names?</em>
 *
 * <p>If they do, the mesh can be rigged by the sequences the server is already driving and no
 * animation authoring is needed. If they do not, the geometry is only a static prop and the plan
 * needs rethinking before any client work happens.
 *
 * <p>Read the rig from {@code ModelDefinition.getVertexGroups()}. The packed per-vertex array is a
 * trap: {@code ModelLoader.load} finishes by calling {@code computeAnimationTables}, which unpacks
 * it and then nulls it, so {@code packedVertexGroups} is null for every model ever loaded whether it
 * is rigged or not. Testing that field measures nothing.
 *
 * <p>Finally it dumps the 2005 opcode 40 recolour pairs for the same NPCs. Retro dragon meshes are
 * greyscale ramps - the 2005 client tinted same-mesh variants from these pairs - so without them
 * all four dragon colours render as identical grey lumps.
 *
 * <p>Run with {@code ./gradlew verifyRetroRigs}, or aim it by hand with
 * {@code -Pmodels=2853,2854 -Pseqs=79,90}. Reads the untracked {@code retrocache/} 2005 cache
 * alongside the live one. Test sourceSet, never shipped, so console output and reading files
 * outside {@code .runelite} are fine here.
 */
public class RetroRigVerifier
{
	private static final String CACHE_DIR_PROPERTY = "retronpcswapper.cacheDir";
	private static final String RETRO_DIR_PROPERTY = "retronpcswapper.retroDir";
	private static final String RETRO_CACHE_DIR = "retrocache/2005cache";

	/** Index 1 of the RS2 cache holds models. */
	private static final int RETRO_MODEL_INDEX = 1;


	/**
	 * The blocked categories, with the live sequences their NPC definitions still name. Sequence
	 * ids are the ones surviving under gameval {@code DRAGON_*} and {@code DEMON_*} names.
	 */
	private static final List<Target> DEFAULT_TARGETS = new ArrayList<>();

	static
	{
		DEFAULT_TARGETS.add(new Target("Adult dragons",
			new int[]{2853, 2854},
			new int[]{79, 80, 89, 90, 91, 92},
			"dragon"));
		DEFAULT_TARGETS.add(new Target("Lesser demons",
			new int[]{2943},
			new int[]{63, 64, 65, 66, 67, 69},
			"demon"));
		DEFAULT_TARGETS.add(new Target("Greater / black demons",
			new int[]{2942},
			new int[]{63, 64, 65, 66, 67, 68, 69},
			"demon"));
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

		if (Boolean.getBoolean("retronpcswapper.probeframes"))
		{
			probe2005Frames(retro);
			retro.close();
			return;
		}

		int findClips = Integer.getInteger("retronpcswapper.findclips", 0);
		if (findClips > 0)
		{
			try (Store store = new Store(liveDir))
			{
				store.load();
				findClipsFor(store, retro, findClips);
			}
			retro.close();
			return;
		}

		int scanRig = Integer.getInteger("retronpcswapper.scanrig", 0);
		if (scanRig > 0)
		{
			scanForRigs(liveDir, scanRig);
			retro.close();
			return;
		}

		List<Target> targets = resolveTargets();

		System.out.println("Live cache:  " + liveDir);
		System.out.println("2005 cache:  " + retroDir.getAbsolutePath());
		System.out.println();

		boolean allCompatible = true;
		try (Store store = new Store(liveDir))
		{
			store.load();

			Map<Integer, FramemapDefinition> framemapCache = new LinkedHashMap<>();
			for (Target target : targets)
			{
				allCompatible &= verify(store, retro, framemapCache, target);
			}

			dumpRecolours(retro, targets);
		}
		finally
		{
			retro.close();
		}

		System.out.println(allCompatible
			? "All target meshes are addressed by the framemaps their live sequences name."
			: "At least one mesh is NOT riggable by its live sequences - see above before building on this.");

		if (!allCompatible)
		{
			System.exit(2);
		}
	}

	private static boolean verify(Store store, RetroCacheReader retro,
		Map<Integer, FramemapDefinition> framemapCache, Target target) throws IOException
	{
		System.out.println("=== " + target.label + " ===");

		// Which transform groups does the 2005 geometry actually carry?
		Map<Integer, SortedSet<Integer>> meshGroups = new LinkedHashMap<>();
		for (int modelId : target.modelIds)
		{
			ModelDefinition model = decodeRetroModel(retro, modelId);
			if (model == null)
			{
				System.out.println("  2005 mesh " + modelId + "  COULD NOT DECODE");
				meshGroups.put(modelId, null);
				continue;
			}

			SortedSet<Integer> groups = vertexGroupsOf(model);
			meshGroups.put(modelId, groups);

			System.out.println("  2005 mesh " + modelId
				+ "  verts=" + model.vertexCount
				+ " faces=" + model.faceCount
				+ "  vertexGroups=" + describe(groups)
				+ describeGroupArray("alphaFaceGroups", model));

			// The same id from the live cache, as a control: for a preserved mesh the two rigs
			// should agree, which is the sanity check that the rig is being read correctly at all
			ModelDefinition live = decodeLiveModel(store, modelId);
			if (live != null)
			{
				System.out.println("       live " + modelId
					+ "  verts=" + live.vertexCount
					+ " faces=" + live.faceCount
					+ "  vertexGroups=" + describe(vertexGroupsOf(live))
					+ describeGroupArray("alphaFaceGroups", live));
			}
		}

		// Which groups do the live sequences address?
		SortedSet<Integer> addressed = new TreeSet<>();
		for (int seqId : target.sequenceIds)
		{
			SequenceDefinition sequence = loadSequence(store, seqId);
			if (sequence == null || sequence.frameIDs == null || sequence.frameIDs.length == 0)
			{
				System.out.println("  live seq " + seqId + "  ABSENT or frameless");
				continue;
			}

			SortedSet<Integer> framemapIds = new TreeSet<>();
			SortedSet<Integer> seqGroups = new TreeSet<>();
			int transforms = 0;
			for (int packed : sequence.frameIDs)
			{
				FramemapDefinition framemap = framemapFor(store, framemapCache, packed);
				if (framemap == null)
				{
					continue;
				}

				framemapIds.add(framemap.id);
				transforms = Math.max(transforms, framemap.length);
				collectFramemapGroups(framemap, seqGroups);
			}

			addressed.addAll(seqGroups);
			System.out.println("  live seq " + seqId
				+ "  frames=" + sequence.frameIDs.length
				+ " framemaps=" + framemapIds
				+ " transforms=" + transforms
				+ " groups=" + describe(seqGroups)
				+ reachOf(store, framemapCache, sequence, meshGroups));
		}

		// The verdict: every group the mesh uses has to be reachable from a framemap transform,
		// otherwise those vertices simply never move.
		boolean compatible = true;
		for (Map.Entry<Integer, SortedSet<Integer>> entry : meshGroups.entrySet())
		{
			SortedSet<Integer> groups = entry.getValue();
			if (groups == null)
			{
				compatible = false;
				continue;
			}

			if (groups.isEmpty())
			{
				// An empty group set is the absence of evidence, not evidence of compatibility -
				// the frames would have nothing to bind to, so this is a hard stop, not a pass
				compatible = false;
				System.out.println("  -> " + entry.getKey()
					+ " NO RIG DATA - the mesh carries no skin array, so live frames cannot move it");
				continue;
			}

			SortedSet<Integer> missing = new TreeSet<>(groups);
			missing.removeAll(addressed);

			if (missing.isEmpty())
			{
				System.out.println("  -> " + entry.getKey() + " COMPATIBLE - all "
					+ groups.size() + " mesh groups are addressed");
			}
			else
			{
				compatible = false;
				System.out.println("  -> " + entry.getKey() + " INCOMPATIBLE - "
					+ missing.size() + " mesh group(s) never addressed: " + missing);
			}
		}

		System.out.println();
		return compatible;
	}

	/**
	 * Prints the opcode 40 pairs for the 2005 definitions behind these categories, which is the
	 * only cache-derived source of colour for a greyscale retro mesh.
	 */
	private static void dumpRecolours(RetroCacheReader retro, List<Target> targets) throws IOException
	{
		byte[] archiveData = retro.readFile(0, 2); // Archive 0 file 2 (config.jag)
		Map<String, byte[]> files = retro.readArchive(archiveData);
		byte[] npcDat = files.get(String.valueOf(RetroCacheReader.hashFileName("npc.dat")));
		byte[] npcIdx = files.get(String.valueOf(RetroCacheReader.hashFileName("npc.idx")));
		if (npcDat == null || npcIdx == null)
		{
			System.out.println("=== 2005 recolours === npc.dat/npc.idx missing, skipped");
			return;
		}

		SortedSet<String> keywords = new TreeSet<>();
		for (Target target : targets)
		{
			if (target.nameKeyword != null)
			{
				keywords.add(target.nameKeyword);
			}
		}

		System.out.println("=== 2005 opcode 40 recolours ===");
		Map<Integer, RetroNpcDefinition> defs = RetroNpcDecoder.decodeAll(npcDat, npcIdx);
		for (Map.Entry<Integer, RetroNpcDefinition> entry : new java.util.TreeMap<>(defs).entrySet())
		{
			RetroNpcDefinition def = entry.getValue();
			String name = def.getName();
			short[] find = def.getOriginalColors();
			short[] replace = def.getReplacementColors();
			if (name == null || find == null || replace == null)
			{
				continue;
			}

			String lower = name.toLowerCase(java.util.Locale.ROOT);
			boolean wanted = false;
			for (String keyword : keywords)
			{
				wanted |= lower.contains(keyword);
			}
			if (!wanted)
			{
				continue;
			}

			StringBuilder pairs = new StringBuilder();
			for (int i = 0; i < find.length && i < replace.length; i++)
			{
				pairs.append(i == 0 ? "" : ", ").append(find[i]).append(" -> ").append(replace[i]);
			}
			System.out.println("  npc " + entry.getKey() + " '" + name + "'  " + pairs);
		}
		System.out.println();
	}

	/**
	 * The transform group ids the mesh actually uses.
	 *
	 * <p>Read {@code getVertexGroups()}, never {@code packedVertexGroups}. {@code ModelLoader.load}
	 * finishes by calling {@code computeAnimationTables}, which unpacks the per-vertex values into
	 * this group-indexed table and then <em>nulls the packed array</em>. So the packed field is null
	 * for every model ever loaded, rigged or not, and testing it measures nothing - a trap this tool
	 * fell into on 2026-09-07 and reported confident verdicts from.
	 *
	 * <p>An empty slot in the table is a group id no vertex is bound to.
	 */
	private static SortedSet<Integer> vertexGroupsOf(ModelDefinition model)
	{
		SortedSet<Integer> groups = new TreeSet<>();

		int[][] table = model.getVertexGroups();
		if (table == null)
		{
			return groups;
		}

		for (int group = 0; group < table.length; group++)
		{
			if (table[group] != null && table[group].length > 0)
			{
				groups.add(group);
			}
		}

		return groups;
	}

	/**
	 * Prints the shape of the face label array, which type 5 alpha ops address. Separate from the
	 * rig - a mesh can carry one without the other.
	 */
	private static String describeGroupArray(String label, ModelDefinition model)
	{
		int[] values = model.packedTransparencyVertexGroups;
		if (values == null)
		{
			return "";
		}

		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		SortedSet<Integer> distinct = new TreeSet<>();
		for (int value : values)
		{
			min = Math.min(min, value);
			max = Math.max(max, value);
			distinct.add(value);
		}

		String sized = values.length == model.vertexCount ? "vertexCount"
			: values.length == model.faceCount ? "faceCount" : "neither";

		return "\n       " + label + " len=" + values.length + " (" + sized + ")"
			+ " range=[" + min + ".." + max + "]"
			+ " distinct=" + distinct.size();
	}

	/**
	 * How much of a clip actually lands on the mesh: the share of its transform ops whose groups
	 * intersect groups the mesh really uses.
	 *
	 * <p>This is the check the subset test above should have been. "Every mesh group is addressed
	 * somewhere in the framemap" is nearly vacuous when the framemap addresses 215 groups and the
	 * mesh uses 60 - almost any mesh passes. What matters is the converse: if the frames spend
	 * their ops on groups this mesh does not have, most of the animation lands on nothing and the
	 * model barely moves, which is what a rig authored for a different mesh looks like.
	 *
	 * <p>Read it against a known-good pair. The skeleton animates correctly in game, so whatever
	 * reach it shows is what working looks like.
	 */
	private static String reachOf(Store store, Map<Integer, FramemapDefinition> cache,
		SequenceDefinition sequence, Map<Integer, SortedSet<Integer>> meshGroups) throws IOException
	{
		SortedSet<Integer> meshUses = new TreeSet<>();
		for (SortedSet<Integer> groups : meshGroups.values())
		{
			if (groups != null)
			{
				meshUses.addAll(groups);
			}
		}

		if (meshUses.isEmpty())
		{
			return "";
		}

		int totalOps = 0;
		int landedOps = 0;

		for (int packed : sequence.frameIDs)
		{
			FramemapDefinition framemap = framemapFor(store, cache, packed);
			if (framemap == null)
			{
				continue;
			}

			byte[] frameData = loadFile(store, IndexType.ANIMATIONS, packed >> 16, packed & 0xFFFF);
			if (frameData == null)
			{
				continue;
			}

			FrameDefinition frame = new FrameLoader().load(framemap, packed & 0xFFFF, frameData);
			for (int op = 0; op < frame.translatorCount; op++)
			{
				int transform = frame.indexFrameIds[op];
				if (transform < 0 || transform >= framemap.frameMaps.length)
				{
					continue;
				}

				totalOps++;
				for (int group : framemap.frameMaps[transform])
				{
					if (meshUses.contains(group))
					{
						landedOps++;
						break;
					}
				}
			}
		}

		if (totalOps == 0)
		{
			return "  reach=n/a";
		}

		return "  reach=" + (landedOps * 100 / totalOps) + "% (" + landedOps + "/" + totalOps + " ops)";
	}

	/**
	 * Feasibility probe for using the 2005 animation data instead of the live frames.
	 *
	 * <p>The live frames turned out to be authored against modern rigs, so the retro meshes need
	 * the retro frames. This reports whether the 2005 cache actually carries them and whether the
	 * modern loaders can read them, which decides whether that is a decoding job or a
	 * format-archaeology one.
	 */
	private static void probe2005Frames(RetroCacheReader retro) throws IOException
	{
		System.out.println("=== 2005 config.jag contents ===");
		byte[] archiveData = retro.readFile(0, 2);
		Map<String, byte[]> config = retro.readArchive(archiveData);
		for (String name : new String[]{"seq.dat", "seq.idx", "npc.dat", "npc.idx", "spotanim.dat"})
		{
			byte[] file = config.get(String.valueOf(RetroCacheReader.hashFileName(name)));
			System.out.println("  " + name + "  " + (file == null ? "absent" : file.length + " bytes"));
		}

		byte[] seqDat = config.get(String.valueOf(RetroCacheReader.hashFileName("seq.dat")));
		byte[] seqIdx = config.get(String.valueOf(RetroCacheReader.hashFileName("seq.idx")));
		Map<Integer, RetroSeqDefinition> sequences = RetroSeqDecoder.decodeAll(seqDat, seqIdx);

		System.out.println();
		System.out.println("=== 2005 sequences vs live frame counts ===");
		System.out.println("  (a matching count means the frame index the client drives lines up 1:1)");
		for (int seqId : new int[]{262, 259, 90, 79, 66, 63})
		{
			RetroSeqDefinition def = sequences.get(seqId);
			if (def == null)
			{
				System.out.println("  seq " + seqId + "  ABSENT from the 2005 cache");
				continue;
			}

			System.out.println("  seq " + seqId
				+ "  frames=" + def.getFrameCount()
				+ "  lengths=" + java.util.Arrays.toString(def.getFrameLengths())
				+ "  frameIds=" + java.util.Arrays.toString(def.getFrameIds()));
		}

		System.out.println();
		System.out.println("=== 2005 index 2 (animations) ===");

		int present = 0;
		int totalBytes = 0;
		int framemapLike = 0;
		Integer firstId = null;
		int largest = 0;
		int largestId = -1;

		for (int id = 0; id < 3000; id++)
		{
			byte[] data;
			try
			{
				// Index 2 entries are gzip wrapped, same as the models
				data = gunzipIfNeeded(retro.readFile(2, id));
			}
			catch (RuntimeException e)
			{
				continue;
			}

			if (data == null || data.length == 0)
			{
				continue;
			}

			present++;
			totalBytes += data.length;
			if (firstId == null)
			{
				firstId = id;
			}
			if (data.length > largest)
			{
				largest = data.length;
				largestId = id;
			}

			// Does it read as a framemap? A real one has a small transform count and small group
			// ids; garbage decodes into implausible numbers or throws.
			try
			{
				FramemapDefinition framemap = new FramemapLoader().load(id, data);
				if (framemap.length > 0 && framemap.length < 512 && framemap.types != null)
				{
					framemapLike++;
				}
			}
			catch (RuntimeException e)
			{
				// Not a framemap, which is itself informative
			}
		}

		// The modern loaders read a framemap as: transform count, then that many type bytes, then a
		// group list per transform. Seeing whether the leading bytes are plausible under that shape
		// says whether index 2 holds framemaps with a header in front, or something else entirely.
		System.out.println();
		System.out.println("  leading bytes of a few files:");
		for (int id : new int[]{0, 1, 2, 80, 100})
		{
			byte[] data = gunzipIfNeeded(retro.readFile(2, id));
			if (data == null || data.length < 24)
			{
				continue;
			}

			StringBuilder hex = new StringBuilder();
			for (int i = 0; i < 24; i++)
			{
				hex.append(String.format("%02x ", data[i]));
			}
			System.out.println("    id " + id + " (" + data.length + "B): " + hex);
		}
		System.out.println();

		System.out.println("  files present: " + present
			+ "  (first id " + firstId + ", largest " + largest + " bytes at id " + largestId + ")");
		System.out.println("  total bytes:   " + totalBytes);
		System.out.println("  decode as a modern framemap: " + framemapLike + " of " + present);
		System.out.println();
	}

	/**
	 * Searches every live sequence for one whose frames actually drive a 2005 mesh.
	 *
	 * <p>The sequences these NPCs name turned out to be re-authored against modern rigs. This asks
	 * the broader question before anyone commits to decoding the 2005 animation format: is there
	 * <em>any</em> surviving animation that fits this mesh? A high scorer would mean the original
	 * rig lives on at some other id; nothing scoring near the skeleton's 96% means it does not.
	 */
	private static void findClipsFor(Store store, RetroCacheReader retro, int modelId) throws IOException
	{
		ModelDefinition model = decodeRetroModel(retro, modelId);
		if (model == null)
		{
			System.err.println("Could not decode 2005 model " + modelId);
			return;
		}

		SortedSet<Integer> meshUses = vertexGroupsOf(model);
		System.out.println("Searching live sequences for animations that fit 2005 mesh " + modelId
			+ " (" + meshUses.size() + " groups, max " + meshUses.last() + ")");
		System.out.println();

		Map<Integer, FramemapDefinition> framemapCache = new LinkedHashMap<>();
		Map<Integer, SortedSet<Integer>> meshGroups = new LinkedHashMap<>();
		meshGroups.put(modelId, meshUses);

		Archive configArchive = store.getIndex(IndexType.CONFIGS).getArchive(ConfigType.SEQUENCE.getId());
		if (configArchive == null)
		{
			System.err.println("No sequence archive in the live cache");
			return;
		}

		ArchiveFiles files = configArchive.getFiles(store.getStorage().loadArchive(configArchive));
		SequenceLoader loader = new SequenceLoader()
			.configureForRevision(store.getIndex(IndexType.CONFIGS).getRevision());

		// Keep the best handful rather than every score; the interesting question is whether
		// anything comes close to a known-good pairing, not the full distribution
		int[] bestSeq = new int[10];
		int[] bestReach = new int[10];
		int scanned = 0;

		for (FSFile file : files.getFiles())
		{
			SequenceDefinition sequence;
			try
			{
				sequence = loader.load(file.getFileId(), file.getContents());
			}
			catch (RuntimeException e)
			{
				continue;
			}

			if (sequence.frameIDs == null || sequence.frameIDs.length == 0)
			{
				continue;
			}

			scanned++;

			// Score both directions. Op reach alone is trivially 100% for any clip built on a small
			// framemap, because its handful of low-numbered groups all happen to exist in a mesh
			// with 60 of them - a human walk cycle scores perfectly against a dragon. A real fit
			// also has to move most of the mesh, so the score is the lower of the two.
			int reach = Math.min(
				reachPercent(store, framemapCache, sequence, meshUses),
				coveragePercent(store, framemapCache, sequence, meshUses));

			for (int slot = 0; slot < bestReach.length; slot++)
			{
				if (reach > bestReach[slot])
				{
					System.arraycopy(bestReach, slot, bestReach, slot + 1, bestReach.length - slot - 1);
					System.arraycopy(bestSeq, slot, bestSeq, slot + 1, bestSeq.length - slot - 1);
					bestReach[slot] = reach;
					bestSeq[slot] = sequence.getId();
					break;
				}
			}
		}

		System.out.println("Scanned " + scanned + " live sequences. Best fits:");
		for (int slot = 0; slot < bestReach.length && bestReach[slot] > 0; slot++)
		{
			System.out.println("  seq " + bestSeq[slot] + "  reach=" + bestReach[slot] + "%");
		}
		System.out.println();
		System.out.println("For reference, skeleton mesh 2944 against its own sequences reaches 96-97%.");
	}

	/**
	 * The share of the mesh's own groups that a clip ever moves. A clip that only drives a handful
	 * of them leaves most of the model rigid, whatever its op reach says.
	 */
	private static int coveragePercent(Store store, Map<Integer, FramemapDefinition> cache,
		SequenceDefinition sequence, SortedSet<Integer> meshUses) throws IOException
	{
		SortedSet<Integer> touched = new TreeSet<>();

		for (int packed : sequence.frameIDs)
		{
			FramemapDefinition framemap = framemapFor(store, cache, packed);
			if (framemap == null)
			{
				continue;
			}

			byte[] frameData = loadFile(store, IndexType.ANIMATIONS, packed >> 16, packed & 0xFFFF);
			if (frameData == null)
			{
				continue;
			}

			FrameDefinition frame;
			try
			{
				frame = new FrameLoader().load(framemap, packed & 0xFFFF, frameData);
			}
			catch (RuntimeException e)
			{
				continue;
			}

			for (int op = 0; op < frame.translatorCount; op++)
			{
				int transform = frame.indexFrameIds[op];
				if (transform < 0 || transform >= framemap.frameMaps.length)
				{
					continue;
				}
				for (int group : framemap.frameMaps[transform])
				{
					if (meshUses.contains(group))
					{
						touched.add(group);
					}
				}
			}
		}

		return meshUses.isEmpty() ? 0 : touched.size() * 100 / meshUses.size();
	}

	private static int reachPercent(Store store, Map<Integer, FramemapDefinition> cache,
		SequenceDefinition sequence, SortedSet<Integer> meshUses) throws IOException
	{
		int totalOps = 0;
		int landedOps = 0;

		for (int packed : sequence.frameIDs)
		{
			FramemapDefinition framemap = framemapFor(store, cache, packed);
			if (framemap == null)
			{
				continue;
			}

			byte[] frameData = loadFile(store, IndexType.ANIMATIONS, packed >> 16, packed & 0xFFFF);
			if (frameData == null)
			{
				continue;
			}

			FrameDefinition frame;
			try
			{
				frame = new FrameLoader().load(framemap, packed & 0xFFFF, frameData);
			}
			catch (RuntimeException e)
			{
				continue;
			}

			for (int op = 0; op < frame.translatorCount; op++)
			{
				int transform = frame.indexFrameIds[op];
				if (transform < 0 || transform >= framemap.frameMaps.length)
				{
					continue;
				}

				totalOps++;
				for (int group : framemap.frameMaps[transform])
				{
					if (meshUses.contains(group))
					{
						landedOps++;
						break;
					}
				}
			}
		}

		return totalOps == 0 ? 0 : landedOps * 100 / totalOps;
	}

	private static void collectFramemapGroups(FramemapDefinition framemap, SortedSet<Integer> into)
	{
		int[][] maps = framemap.frameMaps;
		if (maps == null)
		{
			return;
		}

		for (int[] map : maps)
		{
			if (map == null)
			{
				continue;
			}
			for (int group : map)
			{
				into.add(group);
			}
		}
	}

	/**
	 * A frame names its own framemap in its first two bytes - {@link net.runelite.cache.definitions.loaders.FrameLoader}
	 * takes the framemap as an argument, so the caller has to read it first.
	 */
	private static FramemapDefinition framemapFor(Store store,
		Map<Integer, FramemapDefinition> cache, int packedFrameId) throws IOException
	{
		int archiveId = packedFrameId >> 16;
		int fileId = packedFrameId & 0xFFFF;

		byte[] frame = loadFile(store, IndexType.ANIMATIONS, archiveId, fileId);
		if (frame == null || frame.length < 2)
		{
			return null;
		}

		int framemapId = ((frame[0] & 0xFF) << 8) | (frame[1] & 0xFF);
		if (cache.containsKey(framemapId))
		{
			return cache.get(framemapId);
		}

		FramemapDefinition framemap = null;
		byte[] data = loadFile(store, IndexType.SKELETONS, framemapId, 0);
		if (data != null)
		{
			framemap = new FramemapLoader().load(framemapId, data);
		}

		cache.put(framemapId, framemap);
		return framemap;
	}

	private static SequenceDefinition loadSequence(Store store, int seqId) throws IOException
	{
		byte[] data = loadFile(store, IndexType.CONFIGS, ConfigType.SEQUENCE.getId(), seqId);
		if (data == null)
		{
			return null;
		}

		SequenceLoader loader = new SequenceLoader()
			.configureForRevision(store.getIndex(IndexType.CONFIGS).getRevision());
		return loader.load(seqId, data);
	}

	private static byte[] loadFile(Store store, IndexType indexType, int archiveId, int fileId)
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

		// Single-file archives carry the payload directly rather than behind a file table
		if (archive.getFileData() != null && archive.getFileData().length == 1)
		{
			return archive.decompress(container);
		}

		ArchiveFiles files = archive.getFiles(container);
		FSFile file = files.findFile(fileId);
		return file == null ? null : file.getContents();
	}

	/**
	 * Diagnostic: how many live models carry classic rig data at all. A zero here means the loader
	 * is not surfacing it rather than the assets lacking it, which is a very different problem.
	 */
	private static void scanForRigs(File liveDir, int limit) throws IOException
	{
		try (Store store = new Store(liveDir))
		{
			store.load();

			int scanned = 0;
			int withVertexGroups = 0;
			int withAnimaya = 0;
			int withAlphaGroups = 0;
			Integer firstRigged = null;

			for (Archive archive : store.getIndex(IndexType.MODELS).getArchives())
			{
				if (scanned >= limit)
				{
					break;
				}

				ModelDefinition model = decodeLiveModel(store, archive.getArchiveId());
				if (model == null)
				{
					continue;
				}

				scanned++;
				if (!vertexGroupsOf(model).isEmpty())
				{
					withVertexGroups++;
					if (firstRigged == null)
					{
						firstRigged = archive.getArchiveId();
					}
				}
				if (model.animayaGroups != null)
				{
					withAnimaya++;
				}
				if (model.packedTransparencyVertexGroups != null)
				{
					withAlphaGroups++;
				}
			}

			System.out.println("Format markers on known ids (last two bytes decide the decode path):");
			for (int id : new int[]{2849, 2944, 2961, 2853, 2942})
			{
				Archive archive = store.getIndex(IndexType.MODELS).getArchive(id);
				if (archive == null)
				{
					continue;
				}
				byte[] data = archive.decompress(store.getStorage().loadArchive(archive));
				if (data == null || data.length < 2)
				{
					continue;
				}
				int a = data[data.length - 2];
				int b = data[data.length - 1];
				String path = (a == -1 && b == -3) ? "decodeType3"
					: (a == -1 && b == -2) ? "decodeType2"
					: (a == -1 && b == -1) ? "decodeType1" : "decodeOldFormat";
				System.out.println("  live " + id + "  tail=[" + a + "," + b + "] -> " + path);
			}
			System.out.println();

			System.out.println("Scanned " + scanned + " live models:");
			System.out.println("  rigged (getVertexGroups non-empty):      " + withVertexGroups
				+ (firstRigged == null ? "" : "  (first at id " + firstRigged + ")"));
			System.out.println("  animayaGroups present:                   " + withAnimaya);
			System.out.println("  packedTransparencyVertexGroups present:  " + withAlphaGroups);
		}
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

	private static String describe(SortedSet<Integer> groups)
	{
		if (groups == null || groups.isEmpty())
		{
			return "none";
		}
		return groups.size() + " [" + groups.first() + ".." + groups.last() + "]";
	}

	private static List<Target> resolveTargets()
	{
		int[] models = parseIds(System.getProperty("retronpcswapper.models"));
		int[] seqs = parseIds(System.getProperty("retronpcswapper.seqs"));
		if (models == null || seqs == null)
		{
			return DEFAULT_TARGETS;
		}

		List<Target> targets = new ArrayList<>();
		targets.add(new Target("Ad-hoc", models, seqs, null));
		return targets;
	}

	private static int[] parseIds(String value)
	{
		if (value == null || value.trim().isEmpty())
		{
			return null;
		}

		String[] parts = value.split(",");
		int[] ids = new int[parts.length];
		for (int i = 0; i < parts.length; i++)
		{
			ids[i] = Integer.parseInt(parts[i].trim());
		}
		return ids;
	}

	/**
	 * The 2005 cache is untracked, so it lives in whichever checkout it was unpacked into rather
	 * than in every worktree. {@code -PretroDir=} points at it without duplicating 9.7MB.
	 */
	private static File resolveRetroCacheDir()
	{
		String configured = System.getProperty(RETRO_DIR_PROPERTY);
		if (configured != null && !configured.isEmpty())
		{
			return new File(configured);
		}

		return new File(RETRO_CACHE_DIR);
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

	/** One blocked category: the 2005 meshes to inject and the live sequences that must rig them. */
	private static final class Target
	{
		private final String label;
		private final int[] modelIds;
		private final int[] sequenceIds;
		private final String nameKeyword;

		private Target(String label, int[] modelIds, int[] sequenceIds, String nameKeyword)
		{
			this.label = label;
			this.modelIds = modelIds;
			this.sequenceIds = sequenceIds;
			this.nameKeyword = nameKeyword;
		}
	}
}
