package com.retronpcswapper.inject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.retronpcswapper.RetroNpcSwapperPlugin;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * The chathead counterpart of {@link RetroClipReachTest}: every bundled head against every emote
 * clip in its key range.
 *
 * <p>Measured 2026-10-17. The human heads are 24-25 groups on the 2005 head rigs (100165-100167 Feb,
 * 200165-200167 Nov), and every emote lands 42-50% of its ops on them, Feb and Nov alike, with
 * coverage from 4-9% (the blink-only skull and bored loops) to 56-59% (the laughs and the anger).
 * The rest of each emote's ops address groups the head rig carries for beards and hats a given head
 * does not have, which is why the floor sits far below the body clips' 85%: like the guards' partial
 * kit, reach here is a tripwire against a rig swap, not a validation, and only the in-game look is.
 * The goblin heads are 18 groups on a rig of their own, 100168, and land 85% with 44% coverage.
 *
 * <p>Three heads are not rigged for faces at all. The Restless ghost (28 with 78) and the unnamed
 * ghost 5204 are bound whole to group 5, which some emotes nod and most leave alone; the Gardener
 * ghost's 5179 is bound to nothing and only ever draws at rest. They are bundled anyway, since a
 * still 2005 head is what the 2005 client showed, and are left out of the reach floor.
 */
public class RetroChatheadReachTest
{
	private static final int NOV = RetroAssetBundle.NOV_HUMAN_KEY_BASE;
	private static final int GOBLIN = RetroAssetBundle.GOBLIN_CHATHEAD_KEY_BASE;

	/** The old model format's "no bone" group, which no 2005 rig names. */
	private static final int NO_BONE = 255;

	private static final int HUMAN_FLOOR = 40;
	// Tristram's head 56 (Sir Prysin's too) binds no groups 1-4, 6, 18 or 20, so it moves less: 34%
	private static final int SPARSE_HEAD = 56;
	private static final int SPARSE_HEAD_FLOOR = 30;
	private static final int GOBLIN_FLOOR = 80;
	// 2006 accessories bind few groups, so alone they reach 21-24%; the rows draw them on 142
	private static final int GOBLIN_ACCESSORY_A = 15550;
	private static final int GOBLIN_ACCESSORY_B = 15552;
	private static final int GOBLIN_ACCESSORY_FLOOR = 20;

	/** Each face-rigged head as the overlay merges it: key base, then its parts. */
	private static final int[][] HEADS = {
		{0, 4011}, {0, 4012}, {0, 4013}, {0, 113}, {0, 60}, {0, 10}, {0, 5203}, {0, 5203, 5202}, {0, 5205},
		{0, 53, 84, 43}, {0, 46}, {0, 65, 80}, {0, 49, 81}, {0, 56}, {0, 65, 83}, {0, 66, 80}, {0, 57, 80},
		{0, 65}, {0, 65, 35}, {0, 59, 81}, {0, 52, 82, 44}, {0, 55, 85, 34},
		{0, 103}, {0, 53, 43}, {0, 53, 35}, {0, 52, 81},
		{NOV, 4011}, {NOV, 4012}, {NOV, 4013}, {NOV, 8257, 41}, {NOV, 8258}, {NOV, 8259}, {NOV, 8260},
		{NOV, 8261},
		{GOBLIN, 142}, {GOBLIN, 144}, {GOBLIN, 15550}, {GOBLIN, 15551}, {GOBLIN, 15552},
		{GOBLIN, 142, 15550}, {GOBLIN, 142, 15552}};

	/** The heads that move whole or not at all, which only have to be in the bundle. */
	private static final int[][] STILL_HEADS = {{0, 28, 78}, {0, 5204}, {0, 5179}};

	private static final int[] HUMAN_EMOTES = {554, 555, 556, 557, 562, 563, 564, 565,
		567, 568, 569, 570, 571, 572, 573, 574, 575, 576, 577, 578, 584, 585, 586, 587, 588, 589, 590, 591,
		592, 593, 594, 595, 596, 597, 598, 599, 600, 601, 602, 603, 605, 606, 607, 608, 610, 611, 612, 613,
		614, 615, 616, 617};

	private static final int[] GOBLIN_EMOTES = {580, 581, 582, 583};

	@Test
	public void testEveryEmoteReachesItsHead() throws Exception
	{
		RetroAssetBundle bundle = loadBundle();
		for (int[] head : HEADS)
		{
			RetroMesh mesh = merge(bundle, head);
			Set<Integer> meshGroups = groupsUsedBy(mesh);
			int floor = head[0] == GOBLIN && head.length == 2
				&& (head[1] == GOBLIN_ACCESSORY_A || head[1] == GOBLIN_ACCESSORY_B) ? GOBLIN_ACCESSORY_FLOOR
				: head[0] == GOBLIN ? GOBLIN_FLOOR
				: head[0] == 0 && head[1] == SPARSE_HEAD ? SPARSE_HEAD_FLOOR : HUMAN_FLOOR;

			for (int emote : emotesFor(head))
			{
				RetroClip clip = clip(bundle, head[0] + emote);
				RetroRig rig = bundle.getRig(clip.getRigId());

				int total = 0;
				int landed = 0;
				for (int frame = 0; frame < clip.getFrameCount(); frame++)
				{
					for (int op = 0; op < clip.getOpCount(frame); op++)
					{
						total++;
						for (int group : rig.getGroups(clip.getTransform(frame, op)))
						{
							if (meshGroups.contains(group))
							{
								landed++;
								break;
							}
						}
					}
				}

				assertTrue("clip " + (head[0] + emote) + " has no ops", total > 0);
				int reach = landed * 100 / total;
				assertTrue("emote " + (head[0] + emote) + " reaches only " + reach + "% of head "
					+ (head[0] + head[1]) + ", below its " + floor + "% floor", reach >= floor);
			}
		}
	}

	/**
	 * Every group a head binds is addressed by the rig of every emote it plays, or that slice of the
	 * face would hold still while the rest moves. See
	 * {@link RetroClipReachTest#testNoMeshGroupIsUnaddressedByItsRig}.
	 */
	@Test
	public void testNoHeadGroupIsUnaddressedByItsRigs() throws Exception
	{
		RetroAssetBundle bundle = loadBundle();
		List<String> orphans = new ArrayList<>();
		for (int[] head : HEADS)
		{
			RetroMesh mesh = merge(bundle, head);
			for (int emote : emotesFor(head))
			{
				RetroRig rig = bundle.getRig(clip(bundle, head[0] + emote).getRigId());
				Set<Integer> missing = new HashSet<>(groupsUsedBy(mesh));
				missing.removeAll(addressedBy(rig));
				missing.remove(NO_BONE);
				if (!missing.isEmpty())
				{
					orphans.add("head " + (head[0] + head[1]) + " under " + (head[0] + emote) + ": " + missing);
				}
			}
		}
		assertTrue("head groups no rig addresses: " + orphans, orphans.isEmpty());
	}

	/**
	 * No face of a head joins a vertex its emotes never move to one they do. See
	 * {@link RetroClipReachTest#testNoFaceJoinsAStaticVertexToAMovingOne}.
	 */
	@Test
	public void testNoHeadFaceJoinsAStaticVertexToAMovingOne() throws Exception
	{
		RetroAssetBundle bundle = loadBundle();
		List<String> tears = new ArrayList<>();
		for (int[] head : HEADS)
		{
			RetroMesh mesh = merge(bundle, head);
			for (int emote : emotesFor(head))
			{
				Set<Integer> addressed = addressedBy(bundle.getRig(clip(bundle, head[0] + emote).getRigId()));
				boolean[] moves = new boolean[mesh.getVerticesCount()];
				int[][] vertexGroups = mesh.getVertexGroups();
				for (int group = 0; vertexGroups != null && group < vertexGroups.length; group++)
				{
					if (vertexGroups[group] != null && addressed.contains(group))
					{
						for (int vertex : vertexGroups[group])
						{
							moves[vertex] = true;
						}
					}
				}

				int torn = 0;
				for (int face = 0; face < mesh.getFaceCount(); face++)
				{
					int moving = (moves[mesh.getFaceIndices1()[face]] ? 1 : 0)
						+ (moves[mesh.getFaceIndices2()[face]] ? 1 : 0)
						+ (moves[mesh.getFaceIndices3()[face]] ? 1 : 0);
					if (moving != 0 && moving != 3)
					{
						torn++;
					}
				}
				if (torn > 0)
				{
					tears.add("head " + (head[0] + head[1]) + " under " + (head[0] + emote) + ": " + torn + " faces");
				}
			}
		}
		assertTrue("head faces that tear when animated: " + tears, tears.isEmpty());
	}

	@Test
	public void testStillHeadsAreBundled() throws Exception
	{
		RetroAssetBundle bundle = loadBundle();
		for (int[] head : STILL_HEADS)
		{
			merge(bundle, head);
		}
	}

	private static int[] emotesFor(int[] head)
	{
		return head[0] == GOBLIN ? GOBLIN_EMOTES : HUMAN_EMOTES;
	}

	private static RetroClip clip(RetroAssetBundle bundle, int key)
	{
		RetroClip clip = bundle.getClip(key);
		assertNotNull("clip " + key + " is missing from the bundle", clip);
		assertNotNull("rig " + clip.getRigId() + " is missing from the bundle", bundle.getRig(clip.getRigId()));
		return clip;
	}

	private static RetroMesh merge(RetroAssetBundle bundle, int[] head)
	{
		List<RetroMesh> parts = new ArrayList<>();
		for (int part = 1; part < head.length; part++)
		{
			RetroMesh mesh = bundle.getMesh(head[0] + head[part]);
			assertNotNull("head part " + (head[0] + head[part]) + " is missing from the bundle", mesh);
			parts.add(mesh);
		}
		return RetroMeshMerger.merge(head[0] + head[1], parts);
	}

	private static Set<Integer> addressedBy(RetroRig rig)
	{
		Set<Integer> addressed = new HashSet<>();
		for (int transform = 0; transform < rig.getTransformCount(); transform++)
		{
			for (int group : rig.getGroups(transform))
			{
				addressed.add(group);
			}
		}
		return addressed;
	}

	private static Set<Integer> groupsUsedBy(RetroMesh mesh)
	{
		Set<Integer> used = new HashSet<>();
		int[][] vertexGroups = mesh.getVertexGroups();
		for (int group = 0; vertexGroups != null && group < vertexGroups.length; group++)
		{
			if (vertexGroups[group] != null && vertexGroups[group].length > 0)
			{
				used.add(group);
			}
		}
		return used;
	}

	private static RetroAssetBundle loadBundle() throws Exception
	{
		try (InputStream in = RetroNpcSwapperPlugin.class.getResourceAsStream("retro-assets.dat"))
		{
			assertNotNull("retro-assets.dat is missing - run ./gradlew generateRetroAssets", in);
			return RetroAssetCodec.read(in);
		}
	}
}
