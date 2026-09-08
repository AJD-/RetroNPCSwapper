package com.retronpcswapper.cache;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.retronpcswapper.inject.RetroClip;
import com.retronpcswapper.inject.RetroMesh;
import com.retronpcswapper.inject.RetroMeshMerger;
import com.retronpcswapper.inject.RetroRig;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.fs.Store;
import org.junit.Test;
import static org.junit.Assert.*;

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
	 * {@link RetroMeshMerger} joins them at spawn. This is the equivalence that makes the move safe:
	 * the same parts, through the old code and the new, have to produce the same mesh.
	 */
	@Test
	public void testTheRuntimeMergeMatchesTheGeneratorsOwn() throws Exception
	{
		if (!RETRO_CACHE_DIR.exists())
		{
			return;
		}

		RetroCacheReader retro = new RetroCacheReader(RETRO_CACHE_DIR);
		if (!retro.init())
		{
			return;
		}

		try
		{
			// The adult dragon, which shipped pre-merged, and the four-part fire giant, which is the
			// first NPC with more parts than the old path ever had to join
			assertMergesAgree(retro, 2853, 2854);
			assertMergesAgree(retro, 2870, 2864, 4991, 4990);
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
		assertEquals(where + " vertex count", expected.getVerticesCount(), actual.getVerticesCount());
		assertEquals(where + " face count", expected.getFaceCount(), actual.getFaceCount());
		assertEquals(where + " priority", expected.getPriority(), actual.getPriority());

		assertArrayEquals(where + " x", expected.getVerticesX(), actual.getVerticesX(), 0f);
		assertArrayEquals(where + " y", expected.getVerticesY(), actual.getVerticesY(), 0f);
		assertArrayEquals(where + " z", expected.getVerticesZ(), actual.getVerticesZ(), 0f);

		assertArrayEquals(where + " i1", expected.getFaceIndices1(), actual.getFaceIndices1());
		assertArrayEquals(where + " i2", expected.getFaceIndices2(), actual.getFaceIndices2());
		assertArrayEquals(where + " i3", expected.getFaceIndices3(), actual.getFaceIndices3());

		assertArrayEquals(where + " colors", expected.getFaceColors(), actual.getFaceColors());
		assertArrayEquals(where + " render types",
			expected.getFaceRenderTypes(), actual.getFaceRenderTypes());
		assertArrayEquals(where + " transparencies",
			expected.getFaceTransparencies(), actual.getFaceTransparencies());
		assertArrayEquals(where + " priorities",
			expected.getFaceRenderPriorities(), actual.getFaceRenderPriorities());
		assertArrayEquals(where + " textures", expected.getFaceTextures(), actual.getFaceTextures());

		int[][] expectedGroups = expected.getVertexGroups();
		int[][] actualGroups = actual.getVertexGroups();
		assertEquals(where + " group count", expectedGroups.length, actualGroups.length);
		for (int group = 0; group < expectedGroups.length; group++)
		{
			assertArrayEquals(where + " group " + group, expectedGroups[group], actualGroups[group]);
		}
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

	@Test
	public void testPartialDurationsFallBackToProportionalMapping()
	{
		// Same shape, but the 2005 clip is shorter than the live one it plays over
		int[] liveLengths = new int[36];
		Arrays.fill(liveLengths, 4);
		liveLengths[35] = 20000;
		int[] retroLengths = new int[9];
		retroLengths[8] = 20000;

		int[] mapping = RetroAssetGenerator.resample(liveLengths, retroLengths, 36, 9);

		assertEquals("the first live frame must start at the first 2005 frame", 0, mapping[0]);
		assertEquals("the last live frame must reach the last 2005 frame", 8, mapping[35]);

		// Monotonic and actually spread, rather than parked on one frame
		for (int i = 1; i < mapping.length; i++)
		{
			assertTrue("mapping must not go backwards", mapping[i] >= mapping[i - 1]);
		}
		assertTrue("the clip must visit more than its final frame",
			Arrays.stream(mapping).distinct().count() > 1);
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
		if (liveDir == null || !RETRO_CACHE_DIR.exists())
		{
			return;
		}

		RetroCacheReader retro = new RetroCacheReader(RETRO_CACHE_DIR);
		if (!retro.init())
		{
			return;
		}

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
