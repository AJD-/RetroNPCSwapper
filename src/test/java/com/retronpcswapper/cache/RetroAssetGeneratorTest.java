package com.retronpcswapper.cache;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.retronpcswapper.inject.*;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.fs.Store;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Tests the clip-building half of the generator, which the decoder oracles in
 * {@link RetroFrameDecoderTest} do not reach.
 *
 * <p>{@link RetroClipReachTest} shows that a clip's ops land on groups the mesh has, but it is
 * indifferent to <em>which</em> frame ends up in <em>which</em> row - so a resample or row-ordering
 * bug would score 100% there and still animate wrongly in game, looking exactly like a decode
 * failure. The skeleton closes that gap: it is the one subject present in both caches with equal
 * frame counts, so the two build paths must agree row for row.
 */
public class RetroAssetGeneratorTest
{
	private static final File RETRO_CACHE_DIR = new File("retrocache/2005cache");

	/** Skeleton ready and walk - 2 and 8 frames in both caches. */
	private static final int[] SKELETON_SEQUENCES = {262, 259};

	/**
	 * Checks the runtime merge against the generator's own, over real cache geometry.
	 *
	 * <p>The merge used to run here, storing the result under the first part's model id. That could
	 * not express an NPC family sharing a body mesh, so the bundle now stores parts individually and
	 * {@link RetroMeshMerger} joins them at spawn.
	 *
	 * <p>The two are no longer identical, deliberately: the old merge concatenated the parts, and the
	 * runtime one welds coincident vertices the way the client does, so seams stretch rather than
	 * part. The concatenation stays as the reference the weld is checked against: the same faces
	 * with the same attributes, every corner at the same position, and exactly one vertex per
	 * distinct position the faces reach.
	 */
	@Test
	public void testTheRuntimeMergeMatchesTheGeneratorsOwn() throws Exception
	{
		assumeTrue("2005 cache not present at " + RETRO_CACHE_DIR, RETRO_CACHE_DIR.exists());

		RetroCacheReader retro = new RetroCacheReader(RETRO_CACHE_DIR);
		assertTrue("could not open the 2005 cache", retro.init());

		try
		{
			// The adult dragon, which shipped pre-merged, and the four-part fire giant, which is the
			// first NPC with more parts than the old path ever had to join
			assertMergesAgree(retro, 2853, 2854);
			assertMergesAgree(retro, 2870, 2864, 4991, 4990);
			// The metal dragon, whose third part carries no rig binding of its own
			assertMergesAgree(retro, 4986, 5022, 4987);
			// The King Black Dragon, which shares its body with the chromatic pair
			assertMergesAgree(retro, 2853, 2855);
		}
		finally
		{
			retro.close();
		}
	}

	private static void assertMergesAgree(RetroCacheReader retro, int... modelIds) throws Exception
	{
		List<ModelDefinition> definitions = new ArrayList<>();
		List<RetroMesh> parts = new ArrayList<>();
		for (int modelId : modelIds)
		{
			ModelDefinition definition = RetroAssetGenerator.decodeRetroModel(retro, modelId);
			assertNotNull("2005 model " + modelId, definition);
			definitions.add(definition);
			parts.add(RetroAssetGenerator.toMeshForTest(modelId, definition));
		}

		RetroMesh expected = RetroAssetGenerator.legacyToMesh(modelIds[0], definitions);
		RetroMesh actual = RetroMeshMerger.merge(modelIds[0], parts);

		String where = "merge of " + Arrays.toString(modelIds);
		assertEquals(where + " face count", expected.getFaceCount(), actual.getFaceCount());
		assertEquals(where + " priority", expected.getPriority(), actual.getPriority());

		assertArrayEquals(where + " colors", expected.getFaceColors(), actual.getFaceColors());
		assertArrayEquals(where + " render types",
			expected.getFaceRenderTypes(), actual.getFaceRenderTypes());
		assertArrayEquals(where + " transparencies",
			expected.getFaceTransparencies(), actual.getFaceTransparencies());
		assertArrayEquals(where + " priorities",
			expected.getFaceRenderPriorities(), actual.getFaceRenderPriorities());
		assertArrayEquals(where + " textures", expected.getFaceTextures(), actual.getFaceTextures());

		// Every corner of every face has to land where the concatenation put it, whichever merged
		// vertex now carries that position. And the vertex there has to carry the bone of the corner
		// that reached the position first - the body's, at a seam - which is the whole reason for
		// welding: a limb's faces stretch back to the body instead of parting from it.
		int[] expectedGroupOf = groupOfVertex(expected);
		int[] actualGroupOf = groupOfVertex(actual);
		Map<List<Float>, Integer> firstGroupAt = new HashMap<>();
		Set<List<Float>> positions = new HashSet<>();
		for (int face = 0; face < expected.getFaceCount(); face++)
		{
			int[][] corners = {
				{expected.getFaceIndices1()[face], actual.getFaceIndices1()[face]},
				{expected.getFaceIndices2()[face], actual.getFaceIndices2()[face]},
				{expected.getFaceIndices3()[face], actual.getFaceIndices3()[face]},
			};
			for (int corner = 0; corner < corners.length; corner++)
			{
				List<Float> want = position(expected, corners[corner][0]);
				assertEquals(where + " face " + face + " corner " + (corner + 1),
					want, position(actual, corners[corner][1]));
				positions.add(want);

				firstGroupAt.putIfAbsent(want, expectedGroupOf[corners[corner][0]]);
				assertEquals(where + " face " + face + " corner " + (corner + 1) + " group",
					(int) firstGroupAt.get(want), actualGroupOf[corners[corner][1]]);
			}
		}

		// One vertex per distinct position: nothing left unwelded, and nothing kept that no face uses
		assertEquals(where + " welded vertex count", positions.size(), actual.getVerticesCount());
		assertTrue(where + " should weld at least one seam",
			actual.getVerticesCount() < expected.getVerticesCount());
		assertEquals(where + " group count",
			expected.getVertexGroups().length, actual.getVertexGroups().length);
	}

	/** Per vertex, the group it is bound to, or -1 for none. */
	private static int[] groupOfVertex(RetroMesh mesh)
	{
		int[] groupOf = new int[mesh.getVerticesCount()];
		Arrays.fill(groupOf, -1);
		int[][] groups = mesh.getVertexGroups();
		for (int group = 0; group < groups.length; group++)
		{
			for (int vertex : groups[group])
			{
				groupOf[vertex] = group;
			}
		}
		return groupOf;
	}

	private static List<Float> position(RetroMesh mesh, int vertex)
	{
		return Arrays.asList(mesh.getVerticesX()[vertex], mesh.getVerticesY()[vertex],
			mesh.getVerticesZ()[vertex]);
	}

	/**
	 * A 2005 death sequence declares no duration for the frames that do the dying and an enormous
	 * hold on the corpse. Weighting by duration over that maps every live frame to the final 2005
	 * frame, so the NPC snaps straight to a corpse instead of falling - which is what shipped, for
	 * every death animation in the bundle, until the guards made it obvious.
	 */
	@Test
	public void testADeathSequenceDoesNotCollapseOntoItsFinalFrame()
	{
		int[] liveLengths = {8, 4, 4, 4, 14, 4, 3, 3, 3, 20000};
		int[] retroLengths = {0, 0, 0, 0, 0, 0, 0, 0, 0, 20000};

		int[] mapping = RetroAssetGenerator.resample(liveLengths, retroLengths, 10, 10);

		assertArrayEquals("equal frame counts must map straight through",
			new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, mapping);
	}

	/**
	 * A death plays once and then leaves a corpse, so a 2005 clip shorter than the modern animation
	 * it replaces should run at its own pace and hold - not stretch. The baby dragon is the case
	 * that showed it: nine 2005 frames spread over thirty-six live ones held every pose about
	 * sixteen ticks, against the five of the guard death that looks right.
	 */
	@Test
	public void testAShortDeathPlaysOnceAndThenHolds()
	{
		int[] liveLengths = new int[36];
		Arrays.fill(liveLengths, 4);
		liveLengths[35] = 200;
		int[] retroLengths = new int[9];
		retroLengths[8] = 20000;

		int[] mapping = RetroAssetGenerator.resample(liveLengths, retroLengths, 36, 9);

		assertArrayEquals("the 2005 frames must run a frame at a time",
			new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8}, Arrays.copyOf(mapping, 9));

		for (int i = 9; i < mapping.length; i++)
		{
			assertEquals("and then hold the corpse", 8, mapping[i]);
		}
	}

	/** Equal frame counts are unaffected by the rule above, which is why guard death already worked. */
	@Test
	public void testADeathOfEqualLengthStillMapsStraightThrough()
	{
		int[] retroLengths = new int[10];
		retroLengths[9] = 20000;

		int[] mapping = RetroAssetGenerator.resample(
			new int[]{8, 4, 4, 4, 14, 4, 3, 3, 3, 20000}, retroLengths, 10, 10);

		assertArrayEquals(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, mapping);
	}

	/** A cycle has no terminal hold, so it still stretches to cover the live sequence. */
	@Test
	public void testACycleStillStretches()
	{
		int[] mapping = RetroAssetGenerator.resample(
			new int[]{4, 4, 4, 4, 4, 4, 4, 4}, new int[]{0, 0}, 8, 2);

		assertArrayEquals(new int[]{0, 0, 0, 0, 1, 1, 1, 1}, mapping);
	}

	/** Real durations on both sides still weight by duration rather than by index. */
	@Test
	public void testCompleteDurationsStillWeightByDuration()
	{
		// The first 2005 frame is held three times as long as the second, so it should claim three
		// of the four live frames. Mapping by index would split them evenly instead.
		int[] mapping = RetroAssetGenerator.resample(
			new int[]{1, 1, 1, 1}, new int[]{3, 1}, 4, 2);

		assertArrayEquals(new int[]{0, 0, 0, 1}, mapping);
	}

	@Test
	public void testRetroAndLiveClipPathsAgreeOnTheSkeleton() throws Exception
	{
		File liveDir = RetroAssetGenerator.resolveLiveCacheDir();
		assumeTrue("live cache not present", liveDir != null);
		assumeTrue("2005 cache not present at " + RETRO_CACHE_DIR, RETRO_CACHE_DIR.exists());

		RetroCacheReader retro = new RetroCacheReader(RETRO_CACHE_DIR);
		assertTrue("could not open the 2005 cache", retro.init());

		try (Store store = new Store(liveDir))
		{
			store.load();

			RetroFrameIndex frames = RetroFrameDecoder.decodeAll(retro);
			Map<Integer, RetroSeqDefinition> sequences = RetroAssetGenerator.decodeRetroSequences(retro);
			Map<Integer, RetroRig> rigs = new LinkedHashMap<>();

			for (int sequenceId : SKELETON_SEQUENCES)
			{
				RetroClip live = RetroAssetGenerator.buildClip(store, sequenceId, rigs);
				RetroClip retroClip =
					RetroAssetGenerator.buildRetroClip(store, sequenceId, frames, sequences, rigs);

				assertNotNull("live clip " + sequenceId, live);
				assertNotNull("2005 clip " + sequenceId, retroClip);

				// The rig ids differ by construction - an embedded 2005 framemap has no id, so it
				// gets a synthetic one - but the transforms they address are the same rig
				assertEquals("sequence " + sequenceId + " frame count",
					live.getFrameCount(), retroClip.getFrameCount());

				for (int frame = 0; frame < live.getFrameCount(); frame++)
				{
					String where = "sequence " + sequenceId + " frame " + frame;
					assertEquals(where + " op count",
						live.getOpCount(frame), retroClip.getOpCount(frame));

					for (int op = 0; op < live.getOpCount(frame); op++)
					{
						assertEquals(where + " op " + op + " transform",
							live.getTransform(frame, op), retroClip.getTransform(frame, op));
						assertEquals(where + " op " + op + " dx",
							live.getDx(frame, op), retroClip.getDx(frame, op));
						assertEquals(where + " op " + op + " dy",
							live.getDy(frame, op), retroClip.getDy(frame, op));
						assertEquals(where + " op " + op + " dz",
							live.getDz(frame, op), retroClip.getDz(frame, op));
					}
				}

				System.out.println("sequence " + sequenceId + ": 2005 and live clip paths agree over "
					+ live.getFrameCount() + " frames");
			}
		}
		finally
		{
			retro.close();
		}
	}

	@Test
	public void testResampleIsIdentityWhenTheCountsMatch()
	{
		int[] lengths = {4, 2, 2, 6};
		assertArrayEquals(new int[]{0, 1, 2, 3},
			RetroAssetGenerator.resample(lengths, lengths, 4, 4));
	}

	@Test
	public void testResampleStretchesAShortClipOverALongSequence()
	{
		int[] live = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
		int[] retro = {1, 1, 1, 1, 1, 1, 1, 1};

		assertArrayEquals(new int[]{0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7},
			RetroAssetGenerator.resample(live, retro, 16, 8));
	}

	@Test
	public void testResampleFollowsDurationsRatherThanIndices()
	{
		// The 2005 clip spends three quarters of its cycle on frame 0, so most live frames should
		// land there - which an index-proportional mapping would not do
		int[] live = {1, 1, 1, 1};
		int[] retro = {3, 1};

		assertArrayEquals(new int[]{0, 0, 0, 1},
			RetroAssetGenerator.resample(live, retro, 4, 2));
	}

	@Test
	public void testResampleFallsBackToIndicesWithoutDurations()
	{
		assertArrayEquals(new int[]{0, 0, 1, 1, 2, 2},
			RetroAssetGenerator.resample(null, null, 6, 3));
	}
}
