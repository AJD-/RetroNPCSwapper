package com.retronpcswapper.inject;

import java.io.InputStream;
import java.util.HashSet;
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

	/** Which mesh each clip animates. A clip is only meaningful against the mesh it was built for. */
	private static final int[][] CLIP_MESHES = {
		{262, 2944}, {259, 2944},
		{79, 2853}, {80, 2853}, {89, 2853}, {90, 2853}, {91, 2853}, {92, 2853},
		{63, 2943}, {64, 2943}, {65, 2943}, {66, 2943}, {67, 2943}, {69, 2943},
		{68, 2942},
		{168, 2887}, {169, 2887}, {170, 2887}, {171, 2887}, {172, 2887},
		{21, 2998}, {25, 2998}, {26, 2998}, {27, 2998}, {28, 2998}
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
			RetroMesh mesh = bundle.getMesh(meshId);
			assertNotNull("clip " + sequenceId + " is missing from the bundle", clip);
			assertNotNull("mesh " + meshId + " is missing from the bundle", mesh);

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

			assertTrue("clip " + sequenceId + " reaches only " + reach + "% of mesh " + meshId,
				reach >= MINIMUM_PERCENT);
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
