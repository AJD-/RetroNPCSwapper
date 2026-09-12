package com.retronpcswapper.inject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.retronpcswapper.RetroNpcSwapperPlugin;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Measures the shipped bundle rather than the caches it was built from: for every clip, the share
 * of its transform ops that land on a vertex group the mesh actually has.
 *
 * <p>This is the metric that caught the problem in the first place. Live rig 1080 scored 47-54% on
 * the 2005 dragon mesh and live 1078 scored 57-68% on the demons, against 96-97% for the skeleton
 * on its own rig - the frames behind those surviving sequence ids had been re-authored for the
 * modern skeletons. With the 2005 frames the dragons and demons should now score like the skeleton.
 *
 * <p>Two traps this deliberately does not fall into. Reach alone is trivially 100% for a clip on a
 * small framemap, since its few low-numbered groups exist in any mesh - so coverage, the share of
 * the mesh's own groups the clip ever moves, is measured too. And group ids carry no semantics, so
 * neither number can prove a rig is <em>right</em>; they only rule out gross mismatch. The in-game
 * look is still the only pass.
 */
public class RetroClipReachTest
{
	/**
	 * Well below the skeleton's 96-97% so honest variation between clips does not fail the build,
	 * and far above the 47-68% a re-authored rig scored.
	 */
	private static final int MINIMUM_PERCENT = 85;

	/**
	 * Clips the default floor does not fit, and why. Each of these is a structural property of the
	 * subject rather than a weak result, so the number is a regression tripwire - it says "this got
	 * worse" - and never a validation.
	 *
	 * <p>Giant sequence 130 is four frames and twelve ops in total, so two ops landing elsewhere
	 * reads as 83%. That is noise on a tiny sample, not the 47-68% signature of a rig mismatch.
	 *
	 * <p>The guard sequences are the harder case. Their rig is the 2005 <em>player</em> rig, which
	 * addresses every equipment slot a player can wear - hair, beard, cape, weapon, shield - while a
	 * guard wears seven or nine parts using about 35 groups. A large share of the ops in any player animation
	 * therefore targets slots this NPC simply does not have, and no correct pairing of a partial kit
	 * with a full player animation can approach the skeleton's 96%. Measured against the modern rig
	 * these same meshes score 48-63%, against the 2005 rig 65-80%, so the 2005 clips are the better
	 * fit on every sequence - but reach cannot prove the pairing is right here, only an in-game look
	 * can. Walk (819) has the thinnest margin, 65% against 60%.
	 *
	 * <p>All twenty-six measured. Pose and death: 808 79%, 819 65%, 836 70%. Shield block 1156 70%.
	 * Sword 386 75%, 387 74%, 388 75%, 389 76%, 390 70%, 391 71%, 392 75%. Axe 393 77%, 394 79%,
	 * 395 78%, 396 75%, 397 80%, 398 80%, 399 81%. Blunt 400 75%, 401 77%, 402 75%, 403 80%,
	 * 404 75%. Unarmed 422 80%, 423 70%, 424 76%. All four weapon sets sit in the same band, which
	 * is the result wanted, and 819 is still the thinnest margin of the lot. The Ardougne kit scores
	 * identically to the town guard on every clip they share, despite sharing only one mesh with it
	 * - both are 2005 human kit covering the same joints, which is why one floor serves all of them.
	 */
	private static final Map<Integer, Integer> CLIP_FLOORS = new HashMap<>();

	static
	{
		CLIP_FLOORS.put(130, 80);
		for (int guardClip : new int[]{808, 819, 836, 1156,
			386, 387, 388, 389, 390, 391, 392,
			393, 394, 395, 396, 397, 398, 399,
			400, 401, 402, 403, 404,
			422, 423, 424})
		{
			CLIP_FLOORS.put(guardClip, 60);
		}
	}

	/**
	 * Which mesh each clip animates. A clip is only meaningful against the mesh it was built for,
	 * and for a multi-part NPC that means the merged mesh - measuring a head clip against a
	 * body-only part would report a mismatch that is really just the missing half.
	 */
	private static final int[][] CLIP_MESHES = {
		{262, 2944}, {259, 2944},
		{79, 2853, 2854}, {80, 2853, 2854}, {89, 2853, 2854},
		{90, 2853, 2854}, {91, 2853, 2854}, {92, 2853, 2854},
		{63, 2943}, {64, 2943}, {65, 2943}, {66, 2943}, {67, 2943}, {69, 2943},
		{68, 2942},
		{168, 2887}, {169, 2887}, {170, 2887}, {171, 2887}, {172, 2887},
		{21, 2998}, {25, 2998}, {26, 2998}, {27, 2998}, {28, 2998},
		// The giant family: one shared body, a variant head, and props on the fire and moss giants
		{127, 2870, 2862}, {128, 2870, 2862}, {129, 2870, 2862},
		{130, 2870, 2862}, {131, 2870, 2862},
		// A full 2005 guard kit against the 2005 human rig. Measured against the LIVE rig these
		// score 60-63%, which looks like a mismatch but is not: framemap 0 addresses 218 groups for
		// every equipment slot, and a nine-part kit only ever uses the ~35 named in the javadoc.
		{808, 233, 246, 294, 151, 176, 254, 185, 519, 541},
		{819, 233, 246, 294, 151, 176, 254, 185, 519, 541},
		{422, 233, 246, 294, 151, 176, 254, 185, 519, 541},
		{423, 233, 246, 294, 151, 176, 254, 185, 519, 541},
		{424, 233, 246, 294, 151, 176, 254, 185, 519, 541},
		{836, 233, 246, 294, 151, 176, 254, 185, 519, 541},
		// The sword and shield set a guard actually fights with; 1156 is the shield block.
		{386, 233, 246, 294, 151, 176, 254, 185, 519, 541},
		{389, 233, 246, 294, 151, 176, 254, 185, 519, 541},
		{390, 233, 246, 294, 151, 176, 254, 185, 519, 541},
		{1156, 233, 246, 294, 151, 176, 254, 185, 519, 541},
		// The rest of the sword family. 386, 390 and 1156 are what the town guard was seen playing;
		// these complete it for a sword guard with no shield to block behind.
		{387, 233, 246, 294, 151, 176, 254, 185, 519, 541},
		{388, 233, 246, 294, 151, 176, 254, 185, 519, 541},
		{391, 233, 246, 294, 151, 176, 254, 185, 519, 541},
		{392, 233, 246, 294, 151, 176, 254, 185, 519, 541},
		// The Ardougne guard rides on the same clips: a second 2005 costume on the same human rig,
		// sharing only the boots (185) with the kit above. Measured separately because a merge of
		// seven different meshes uses a different set of groups, and a floor that only ever saw
		// the town guard would not notice this one drifting.
		{808, 225, 301, 162, 179, 274, 185, 502},
		{819, 225, 301, 162, 179, 274, 185, 502},
		{422, 225, 301, 162, 179, 274, 185, 502},
		{423, 225, 301, 162, 179, 274, 185, 502},
		{424, 225, 301, 162, 179, 274, 185, 502},
		{836, 225, 301, 162, 179, 274, 185, 502},
		{386, 225, 301, 162, 179, 274, 185, 502},
		{389, 225, 301, 162, 179, 274, 185, 502},
		{390, 225, 301, 162, 179, 274, 185, 502},
		{1156, 225, 301, 162, 179, 274, 185, 502},
		// Weapon 502 is a mace, so this guard fights blunt where the town guard stabs. 401 is the
		// one seen in game; its family ships with it. 401 and 403 resolve to framemap 100082 rather
		// than 100083 - a clip carries its own rig id, and these are the only guard clips that use
		// the second one, so they are the reason to measure the family rather than just 401.
		{400, 225, 301, 162, 179, 274, 185, 502},
		{401, 225, 301, 162, 179, 274, 185, 502},
		{402, 225, 301, 162, 179, 274, 185, 502},
		{403, 225, 301, 162, 179, 274, 185, 502},
		{404, 225, 301, 162, 179, 274, 185, 502},
		// The axe family, against the Falador axe guard's own kit - battleaxe 550 where the rest
		// carry sword 519, which is also the only row that measures 550 at all. 395 and 397 are the
		// two seen in game. Between them, 393/395/397/398 use framemap 100082 and 394/399 use
		// 100075, so the guards now span three rigs rather than one.
		{393, 233, 246, 294, 151, 176, 254, 185, 550, 541},
		{394, 233, 246, 294, 151, 176, 254, 185, 550, 541},
		{395, 233, 246, 294, 151, 176, 254, 185, 550, 541},
		{396, 233, 246, 294, 151, 176, 254, 185, 550, 541},
		{397, 233, 246, 294, 151, 176, 254, 185, 550, 541},
		{398, 233, 246, 294, 151, 176, 254, 185, 550, 541},
		{399, 233, 246, 294, 151, 176, 254, 185, 550, 541}
	};

	@Test
	public void testEveryShippedClipReachesItsMesh() throws Exception
	{
		RetroAssetBundle bundle = loadBundle();

		for (int[] pair : CLIP_MESHES)
		{
			int sequenceId = pair[0];
			int meshId = pair[1];

			RetroClip clip = bundle.getClip(sequenceId);
			assertNotNull("clip " + sequenceId + " is missing from the bundle", clip);

			List<RetroMesh> parts = new ArrayList<>();
			for (int part = 1; part < pair.length; part++)
			{
				RetroMesh partMesh = bundle.getMesh(pair[part]);
				assertNotNull("mesh " + pair[part] + " is missing from the bundle", partMesh);
				parts.add(partMesh);
			}
			RetroMesh mesh = RetroMeshMerger.merge(meshId, parts);

			RetroRig rig = bundle.getRig(clip.getRigId());
			assertNotNull("rig " + clip.getRigId() + " is missing from the bundle", rig);

			Set<Integer> meshGroups = groupsUsedBy(mesh);
			assertFalse("mesh " + meshId + " carries no rig data", meshGroups.isEmpty());

			int totalOps = 0;
			int landedOps = 0;
			Set<Integer> moved = new HashSet<>();

			for (int frame = 0; frame < clip.getFrameCount(); frame++)
			{
				for (int op = 0; op < clip.getOpCount(frame); op++)
				{
					int transform = clip.getTransform(frame, op);
					if (transform < 0 || transform >= rig.getTransformCount())
					{
						fail("clip " + sequenceId + " addresses transform " + transform
							+ " of a " + rig.getTransformCount() + " transform rig");
					}

					totalOps++;
					for (int group : rig.getGroups(transform))
					{
						if (meshGroups.contains(group))
						{
							landedOps++;
							moved.add(group);
							break;
						}
					}
				}
			}

			assertTrue("clip " + sequenceId + " has no ops", totalOps > 0);

			int reach = landedOps * 100 / totalOps;
			int coverage = moved.size() * 100 / meshGroups.size();
			System.out.println("clip " + sequenceId + " on mesh " + meshId + " (rig " + rig.getId()
				+ "): reach=" + reach + "% (" + landedOps + "/" + totalOps + " ops)"
				+ " coverage=" + coverage + "% (" + moved.size() + "/" + meshGroups.size() + " groups)");

			int floor = CLIP_FLOORS.getOrDefault(sequenceId, MINIMUM_PERCENT);
			assertTrue("clip " + sequenceId + " reaches only " + reach + "% of mesh " + meshId
				+ ", below its " + floor + "% floor", reach >= floor);
		}
	}

	/**
	 * Every group a mesh binds must be reachable by some transform of the rig that animates it.
	 *
	 * <p>Reach cannot see this. It counts ops that land, and an op lands on the first group of its
	 * transform that the mesh has - so a group no transform names at all costs nothing and shows up
	 * nowhere, while in game that slice of the mesh holds its rest pose while the body around it
	 * moves. The Ardougne guard is what made it worth asserting: its hands (274) bind up to group
	 * 36, past the [0..34] the town guard's kit had established.
	 */
	@Test
	public void testNoMeshGroupIsUnaddressedByItsRig() throws Exception
	{
		RetroAssetBundle bundle = loadBundle();

		for (int[] pair : CLIP_MESHES)
		{
			RetroClip clip = bundle.getClip(pair[0]);
			assertNotNull("clip " + pair[0] + " is missing from the bundle", clip);
			RetroRig rig = bundle.getRig(clip.getRigId());
			assertNotNull("rig " + clip.getRigId() + " is missing from the bundle", rig);

			List<RetroMesh> parts = new ArrayList<>();
			for (int part = 1; part < pair.length; part++)
			{
				parts.add(bundle.getMesh(pair[part]));
			}
			RetroMesh mesh = RetroMeshMerger.merge(pair[1], parts);

			Set<Integer> addressed = new HashSet<>();
			for (int transform = 0; transform < rig.getTransformCount(); transform++)
			{
				for (int group : rig.getGroups(transform))
				{
					addressed.add(group);
				}
			}

			Set<Integer> orphans = new HashSet<>(groupsUsedBy(mesh));
			orphans.removeAll(addressed);
			assertTrue("mesh " + pair[1] + " binds groups " + orphans + " that rig " + rig.getId()
				+ " never addresses, so they would never move", orphans.isEmpty());
		}
	}

	private static Set<Integer> groupsUsedBy(RetroMesh mesh)
	{
		Set<Integer> used = new HashSet<>();
		int[][] vertexGroups = mesh.getVertexGroups();
		if (vertexGroups == null)
		{
			return used;
		}

		// An empty slot is a group nothing is bound to, so it is not a group the mesh has
		for (int group = 0; group < vertexGroups.length; group++)
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
			RetroAssetBundle bundle = RetroAssetCodec.read(in);

			Map<Integer, RetroClip> clips = bundle.getClips();
			assertFalse("bundle carries no clips", clips.isEmpty());
			return bundle;
		}
	}
}
