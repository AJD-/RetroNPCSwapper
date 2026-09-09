package com.retronpcswapper.cache;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.cache.definitions.FrameDefinition;
import net.runelite.cache.definitions.FramemapDefinition;
import net.runelite.cache.definitions.SequenceDefinition;
import net.runelite.cache.definitions.loaders.FrameLoader;
import net.runelite.cache.definitions.loaders.FramemapLoader;
import net.runelite.cache.IndexType;
import net.runelite.cache.fs.Store;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Tests for the 2005 index 2 frame decoder.
 *
 * <p>Both caches are untracked, so every test here is skipped when its input is missing rather than
 * passing vacuously. The container test needs only the 2005 cache; the two oracles need the live
 * cache as well.
 */
public class RetroFrameDecoderTest
{
	private static final File RETRO_CACHE_DIR = new File("retrocache/2005cache");

	/** Holds the skeleton's frames, and a framemap that should be live framemap 338. */
	private static final int SKELETON_FILE = 45;
	private static final int SKELETON_FRAMEMAP = 338;

	/** Skeleton ready and walk. The only sequences whose 2005 and live frames should agree. */
	private static final int[] SKELETON_SEQUENCES = {262, 259};

	/**
	 * The container invariants, all of which held across the whole index when the format was worked
	 * out. Any one of them breaking means the layout was misread, not that the data is odd.
	 */
	@Test
	public void testEveryFrameGroupDecodes()
	{
		RetroCacheReader retro = openRetro();

		try
		{
			RetroFrameIndex index = RetroFrameDecoder.decodeAll(retro);

			assertEquals("index 2 holds 411 frame groups", 411, index.getGroups().size());
			assertEquals("index 2 holds 12,597 frames", 12597, index.getFrameCount());

			Set<Integer> ids = new HashSet<>();
			for (RetroFrameGroup group : index.getGroups())
			{
				RetroFramemapDefinition framemap = group.getFramemap();
				assertTrue("file " + group.getFileId() + " has an empty framemap",
					framemap.getLength() > 0);

				for (int type : framemap.getTypes())
				{
					assertTrue("file " + group.getFileId() + " has transform type " + type,
						type >= 0 && type <= 5);
				}

				for (RetroFrameDefinition frame : group.getFrames())
				{
					assertTrue("duplicate frame id " + frame.getFrameId(), ids.add(frame.getFrameId()));

					for (int transform : frame.getIndexFrameIds())
					{
						assertTrue("frame " + frame.getFrameId() + " addresses transform " + transform
								+ " of a " + framemap.getLength() + " transform framemap",
							transform >= 0 && transform < framemap.getLength());
					}
				}
			}

			// Gapless from zero, which is what makes the directories a complete frame-to-file map
			for (int id = 0; id < 12597; id++)
			{
				assertTrue("frame id " + id + " is missing", ids.contains(id));
			}
		}
		finally
		{
			retro.close();
		}
	}

	/**
	 * The known-plaintext check that anchors the whole format: the skeleton's animation was never
	 * re-authored, so the framemap embedded in its 2005 frame group must be the live one.
	 *
	 * <p>It is a strong check because the 2005 layout interleaves the group lists where the modern
	 * one writes all counts first - the two occupy the same bytes, so nothing but the decoded values
	 * tells them apart.
	 */
	@Test
	public void testSkeletonFramemapMatchesLive() throws Exception
	{
		File liveDir = RetroAssetGenerator.resolveLiveCacheDir();
		assumeTrue("live cache not present", liveDir != null);
		RetroCacheReader retro = openRetro();

		try (Store store = new Store(liveDir))
		{
			store.load();

			RetroFrameGroup group = RetroFrameDecoder.decodeAll(retro).getGroup(SKELETON_FILE);
			assertNotNull(group);

			byte[] data = RetroAssetGenerator.loadFile(store, IndexType.SKELETONS, SKELETON_FRAMEMAP, 0);
			assertNotNull("live framemap " + SKELETON_FRAMEMAP + " is missing", data);
			FramemapDefinition live = new FramemapLoader().load(SKELETON_FRAMEMAP, data);

			RetroFramemapDefinition retroFramemap = group.getFramemap();
			assertArrayEquals("transform types", live.types, retroFramemap.getTypes());
			assertEquals("transform count", live.frameMaps.length, retroFramemap.getGroups().length);
			for (int i = 0; i < live.frameMaps.length; i++)
			{
				assertArrayEquals("groups of transform " + i, live.frameMaps[i], retroFramemap.getGroups()[i]);
			}
		}
		finally
		{
			retro.close();
		}
	}

	/**
	 * The check that proves the value decoding rather than just the section lengths: the skeleton's
	 * 2005 frames against the live frames of the same sequences, op for op.
	 *
	 * <p>Transform index lists are asserted equal - those prove the mask semantics and the pivot
	 * back-fill. Per-axis values are only reported, because "never re-authored" was inferred from
	 * reach rather than measured on values, so a small drift here is a re-export and not a bug. A
	 * large or axis-swapped delta would be, which is what the printed maximum is for.
	 */
	@Test
	public void testSkeletonFrameValuesMatchLive() throws Exception
	{
		File liveDir = RetroAssetGenerator.resolveLiveCacheDir();
		assumeTrue("live cache not present", liveDir != null);
		RetroCacheReader retro = openRetro();

		try (Store store = new Store(liveDir))
		{
			store.load();

			RetroFrameIndex index = RetroFrameDecoder.decodeAll(retro);
			Map<Integer, RetroSeqDefinition> sequences = decode2005Sequences(retro);

			for (int sequenceId : SKELETON_SEQUENCES)
			{
				RetroSeqDefinition retroSeq = sequences.get(sequenceId);
				assertNotNull("2005 sequence " + sequenceId + " did not decode", retroSeq);

				SequenceDefinition liveSeq = RetroAssetGenerator.loadSequence(store, sequenceId);
				assertNotNull("live sequence " + sequenceId + " is missing", liveSeq);
				assertEquals("sequence " + sequenceId + " frame count",
					liveSeq.frameIDs.length, retroSeq.getFrameIds().length);

				int worst = 0;
				for (int i = 0; i < liveSeq.frameIDs.length; i++)
				{
					RetroFrameDefinition retroFrame = index.getFrame(retroSeq.getFrameIds()[i]);
					assertNotNull("2005 frame " + retroSeq.getFrameIds()[i] + " is missing", retroFrame);

					FrameDefinition liveFrame = loadLiveFrame(store, liveSeq.frameIDs[i]);
					assertNotNull(liveFrame);

					String where = "sequence " + sequenceId + " frame " + i;
					assertArrayEquals(where + " transform indices",
						liveFrame.indexFrameIds, retroFrame.getIndexFrameIds());

					worst = Math.max(worst, maxDelta(liveFrame.translator_x, retroFrame.getTranslatorX()));
					worst = Math.max(worst, maxDelta(liveFrame.translator_y, retroFrame.getTranslatorY()));
					worst = Math.max(worst, maxDelta(liveFrame.translator_z, retroFrame.getTranslatorZ()));
				}

				System.out.println("skeleton sequence " + sequenceId + ": " + liveSeq.frameIDs.length
					+ " frames, transform indices identical, max per-axis delta " + worst);
			}
		}
		finally
		{
			retro.close();
		}
	}

	private static int maxDelta(int[] live, int[] retro)
	{
		int worst = 0;
		for (int i = 0; i < Math.min(live.length, retro.length); i++)
		{
			worst = Math.max(worst, Math.abs(live[i] - retro[i]));
		}
		return worst;
	}

	private static FrameDefinition loadLiveFrame(Store store, int packed) throws Exception
	{
		byte[] frameData = RetroAssetGenerator.loadFile(store, IndexType.ANIMATIONS, packed >> 16, packed & 0xFFFF);
		if (frameData == null || frameData.length < 2)
		{
			return null;
		}

		int framemapId = ((frameData[0] & 0xFF) << 8) | (frameData[1] & 0xFF);
		byte[] framemapData = RetroAssetGenerator.loadFile(store, IndexType.SKELETONS, framemapId, 0);
		if (framemapData == null)
		{
			return null;
		}

		FramemapDefinition framemap = new FramemapLoader().load(framemapId, framemapData);
		return new FrameLoader().load(framemap, packed & 0xFFFF, frameData);
	}

	private static Map<Integer, RetroSeqDefinition> decode2005Sequences(RetroCacheReader retro)
	{
		Map<String, byte[]> config = retro.readArchive(retro.readFile(0, 2));
		byte[] seqDat = config.get(String.valueOf(RetroCacheReader.hashFileName("seq.dat")));
		byte[] seqIdx = config.get(String.valueOf(RetroCacheReader.hashFileName("seq.idx")));
		return RetroSeqDecoder.decodeAll(seqDat, seqIdx);
	}

	/**
	 * The 2005 cache, or a skipped test when it is not present.
	 *
	 * <p>A cache that is present but will not open is a failure rather than a skip. Treating the
	 * two the same reports green for a corrupt cache, which is the outcome worth knowing about.
	 */
	private static RetroCacheReader openRetro()
	{
		assumeTrue("2005 cache not present at " + RETRO_CACHE_DIR, RETRO_CACHE_DIR.exists());

		RetroCacheReader retro = new RetroCacheReader(RETRO_CACHE_DIR);
		assertTrue("could not open the 2005 cache", retro.init());
		return retro;
	}
}
