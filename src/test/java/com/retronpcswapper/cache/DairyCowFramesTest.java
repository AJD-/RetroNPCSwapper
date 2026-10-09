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

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.runelite.cache.definitions.FrameDefinition;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.SequenceDefinition;
import net.runelite.cache.fs.Store;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Whether the dairy cow's chewing clip, sequence 2303, is still the November 2005 animation in
 * the live cache - which decides whether its 2005 mesh can play the live clip.
 *
 * <p>Equal transform lists would be too strict a test: the cow rig gained transforms after 2005.
 * What matters is that every transform the 2005 frames use still moves the same parts of the 2005
 * mesh by the same amounts about the same pivots, and that whatever live added leaves it alone.
 *
 * <p>Measured: all 61 frames carry identical values; the live rig adds groups 41-44 (absent from
 * the 2005 mesh) to transforms 1 and 6, and group 0 to transform 1. That last is the one
 * difference the 2005 mesh sees - three of 8237's 243 vertices - and transform 1 is a constant
 * one-unit lift, so under the live clip they sit 1/128 of a tile higher than in 2005.
 */
public class DairyCowFramesTest
{
	private static final File NOV_CACHE_DIR = new File("retrocache/nov2005cache");

	private static final int CHEWS_GRASS = 2303;
	private static final int[] DAIRY_COW_MESHES = {8237, 8239};

	/** The transform types: an origin, then the ops that move vertices relative to it. */
	private static final int ORIGIN = 0;
	private static final int TRANSLATE = 1;

	@Test
	public void testChewingClipIsTheNovember2005One() throws Exception
	{
		File liveDir = RetroAssetGenerator.resolveLiveCacheDir();
		assumeTrue("live cache not present", liveDir != null);
		assumeTrue("Nov-2005 cache not present at " + NOV_CACHE_DIR, NOV_CACHE_DIR.exists());

		RetroCacheReader nov = new RetroCacheReader(NOV_CACHE_DIR);
		assertTrue("could not open the Nov-2005 cache", nov.init());

		try (Store store = new Store(liveDir))
		{
			store.load();

			Set<Integer> meshGroups = new HashSet<>();
			for (int modelId : DAIRY_COW_MESHES)
			{
				ModelDefinition model = RetroAssetGenerator.decodeRetroModel(nov, modelId);
				assertNotNull("Nov-2005 model " + modelId + " did not decode", model);
				model.computeAnimationTables();
				int[][] groups = model.getVertexGroups();
				for (int group = 0; groups != null && group < groups.length; group++)
				{
					if (groups[group] != null && groups[group].length > 0)
					{
						meshGroups.add(group);
					}
				}
			}

			RetroFrameIndex index = RetroFrameDecoder.decodeAll(nov);
			Map<Integer, RetroSeqDefinition> sequences = RetroFrameDecoderTest.decode2005Sequences(nov);
			RetroSeqDefinition novSeq = sequences.get(CHEWS_GRASS);
			assertNotNull("Nov-2005 sequence " + CHEWS_GRASS + " did not decode", novSeq);

			SequenceDefinition liveSeq = RetroAssetGenerator.loadSequence(store, CHEWS_GRASS);
			assertNotNull("live sequence " + CHEWS_GRASS + " is missing", liveSeq);
			assertEquals("frame count", liveSeq.frameIDs.length, novSeq.getFrameCount());

			// Which 2005 mesh groups the live rig moves that the 2005 rig did not, by transform
			Map<Integer, Set<Integer>> gained = new TreeMap<>();
			for (int i = 0; i < novSeq.getFrameCount(); i++)
			{
				int novFrameId = novSeq.getFrameIds()[i];
				RetroFrameDefinition novFrame = index.getFrame(novFrameId);
				RetroFramemapDefinition novMap = index.getFramemapForFrame(novFrameId);
				assertNotNull("Nov-2005 frame " + novFrameId + " is missing", novFrame);

				FrameDefinition liveFrame = RetroFrameDecoderTest.loadLiveFrame(store, liveSeq.frameIDs[i]);
				assertNotNull("live frame " + i + " is missing", liveFrame);
				int[] liveTypes = liveFrame.framemap.types;
				int[][] liveGroups = liveFrame.framemap.frameMaps;

				String where = "frame " + i;
				int[] novOps = novFrame.getIndexFrameIds();
				int[] liveOps = liveFrame.indexFrameIds;
				for (int op = 0; op < novOps.length; op++)
				{
					int transform = novOps[op];
					String what = where + ", transform " + transform;
					assertTrue(what + " is past the live framemap", transform < liveTypes.length);
					assertEquals(what + ": type", novMap.getTypes()[transform], liveTypes[transform]);

					Set<Integer> novMoves = groupsOf(novMap.getGroups()[transform]);
					Set<Integer> liveMoves = groupsOf(liveGroups[transform]);
					assertTrue(what + ": live drops groups", liveMoves.containsAll(novMoves));
					for (int group : liveMoves)
					{
						if (!novMoves.contains(group) && meshGroups.contains(group))
						{
							gained.computeIfAbsent(transform, t -> new TreeSet<>()).add(group);
						}
					}

					int liveOp = indexOf(liveOps, transform);
					assertTrue(what + ": live frame drops it", liveOp >= 0);
					assertEquals(what + ": x", novFrame.getTranslatorX()[op], liveFrame.translator_x[liveOp]);
					assertEquals(what + ": y", novFrame.getTranslatorY()[op], liveFrame.translator_y[liveOp]);
					assertEquals(what + ": z", novFrame.getTranslatorZ()[op], liveFrame.translator_z[liveOp]);

					// A rotation or scale turns about the latest origin; a live-only origin slipped in
					// ahead of it would move the pivot
					if (novMap.getTypes()[transform] > TRANSLATE)
					{
						assertEquals(what + ": pivot", lastOrigin(novOps, op, novMap.getTypes()),
							lastOrigin(liveOps, liveOp, liveTypes));
					}
				}

				for (int transform : liveOps)
				{
					if (indexOf(novOps, transform) < 0 && liveTypes[transform] != ORIGIN)
					{
						for (int group : liveGroups[transform])
						{
							assertFalse(where + ": live-only transform " + transform + " moves 2005 group " + group,
								meshGroups.contains(group));
						}
					}
				}

				// Transform 1, the one that gained a 2005 group, is the same lift on every frame
				int lift = indexOf(novOps, 1);
				assertTrue(where + ": transform 1", lift >= 0);
				assertEquals(where + ": transform 1 x", 0, novFrame.getTranslatorX()[lift]);
				assertEquals(where + ": transform 1 y", 1, novFrame.getTranslatorY()[lift]);
				assertEquals(where + ": transform 1 z", 0, novFrame.getTranslatorZ()[lift]);
			}

			assertEquals("2005 groups only the live rig moves", Map.of(1, Set.of(0)), gained);
		}
		finally
		{
			nov.close();
		}
	}

	private static Set<Integer> groupsOf(int[] groups)
	{
		Set<Integer> set = new HashSet<>();
		for (int group : groups)
		{
			set.add(group);
		}
		return set;
	}

	/** The origin op before {@code op}, by transform index, or -1. */
	private static int lastOrigin(int[] ops, int op, int[] types)
	{
		for (int i = op - 1; i >= 0; i--)
		{
			if (types[ops[i]] == ORIGIN)
			{
				return ops[i];
			}
		}
		return -1;
	}

	private static int indexOf(int[] values, int value)
	{
		for (int i = 0; i < values.length; i++)
		{
			if (values[i] == value)
			{
				return i;
			}
		}
		return -1;
	}
}
