package com.retronpcswapper.cache;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import com.retronpcswapper.inject.RetroClip;
import com.retronpcswapper.inject.RetroRig;
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
