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
package com.retronpcswapper;

import com.retronpcswapper.inject.RetroAssetBundle;
import com.retronpcswapper.inject.RetroClip;
import com.retronpcswapper.inject.RetroLighter;
import com.retronpcswapper.inject.RetroMesh;
import com.retronpcswapper.inject.RetroMeshMerger;
import com.retronpcswapper.inject.RetroModel;
import com.retronpcswapper.inject.RetroRig;
import com.retronpcswapper.inject.RetroSkinner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Animation;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.NPC;

/**
 * Holds the retro replacement geometry, built once per NPC id and reused every frame.
 *
 * <p>The draw callback runs per entity per frame, so it must do a map lookup and nothing else.
 * Everything expensive - decoding model data, merging and lighting - happens here, driven from
 * NPC spawn and transform events rather than from the render path.
 */
@Singleton
@Slf4j
public class RetroModelCache
{
	@Inject
	private Client client;

	/** Unposed, lit retro models keyed by NPC id. */
	private final Map<Integer, Model> baseModels = new HashMap<>();

	/** Animations keyed by sequence id - loadAnimation per NPC per frame would be wasteful. */
	private final Map<Integer, Animation> animations = new HashMap<>();

	/** NPC ids whose retro models could not be built, so spawns stop retrying them. */
	private final Set<Integer> unbuildable = new HashSet<>();

	/** NPC id and animation pairs already reported by {@link #reportAction}, so each is said once. */
	private final Set<Long> reportedActions = new HashSet<>();

	/**
	 * Scratch geometry this plugin owns, used when the injection pipeline is on. One instance,
	 * reused every frame and every NPC, because it is consumed before anything else can run - the
	 * same contract {@link #pose(NPC)} already documents for the client's own shared model.
	 */
	private final RetroModel injected = new RetroModel();

	/**
	 * Whether to route posed geometry through {@link RetroModel} rather than hand the client's own
	 * model straight to the renderer.
	 *
	 * <p>While the geometry still comes from the cache this is a no-op by construction - the copy
	 * is faithful, so anything that looks different on screen with it on is a defect in the
	 * injection path. That is what makes it worth a toggle: it separates "can the renderer accept
	 * geometry we own" from "is our geometry correct", which are the two questions the injection
	 * work has to answer one at a time.
	 */
	private boolean useInjectionPipeline;

	/** One-shot guard for the lighting comparison; it only needs to run over one mesh. */
	private boolean lightingVerified;

	/** Injected geometry, rigs and clips. Empty until the bundle finishes loading, or if there is none. */
	private RetroAssetBundle bundle = RetroAssetBundle.empty();

	/** The bundle mesh id backing each substituted NPC id, when one exists. */
	private final Map<Integer, Integer> meshIds = new HashMap<>();

	/** Fully prepared injected geometry per NPC id, for NPCs the live cache can no longer supply. */
	private final Map<Integer, InjectedModel> injectedModels = new HashMap<>();

	private final RetroSkinner skinner = new RetroSkinner();

	/** Scratch pose buffers for the skinner comparison, grown to whatever mesh needs them. */
	private float[] skinnedX = new float[0];
	private float[] skinnedY = new float[0];
	private float[] skinnedZ = new float[0];

	/** One-shot guard for the skinner comparison. */
	private boolean skinningVerified;

	/**
	 * NPC ids currently eligible for substitution. The eligibility decision itself stays in the
	 * plugin, which weighs mappings, config toggles and safety settings; this is only the memo of
	 * it, so the render path is a lookup and nothing more.
	 */
	private final Set<Integer> substituted = new HashSet<>();

	/**
	 * Marks an NPC id as being substituted, so {@link #pose(NPC)} will supply geometry for it.
	 */
	public void setSubstituted(int npcId)
	{
		substituted.add(npcId);
	}

	public void clearSubstituted(int npcId)
	{
		substituted.remove(npcId);
	}

	public boolean isSubstituted(int npcId)
	{
		return substituted.contains(npcId);
	}

	/**
	 * Returns the cached retro model for an NPC id, or null if none has been built.
	 * Safe to call from the render path.
	 */
	public Model get(int npcId)
	{
		return baseModels.get(npcId);
	}

	/**
	 * Builds and caches the retro model for an NPC id if it is not already present.
	 * Must be called on the client thread.
	 */
	public void ensureBuilt(int npcId, RetroNpcData data)
	{
		if (data == null || baseModels.containsKey(npcId) || injectedModels.containsKey(npcId)
			|| unbuildable.contains(npcId))
		{
			return;
		}

		// Injected geometry first, when the pipeline is on: for the categories this exists for, the
		// live cache no longer holds the mesh at all, so the cache-backed path has nothing to load.
		// Gating on the toggle keeps a shipping category on its known-good path by default - the
		// skeleton is in the bundle as a test subject, and should not quietly change how it renders.
		InjectedModel injectedModel = useInjectionPipeline ? buildInjected(data) : null;
		if (injectedModel != null)
		{
			injectedModels.put(npcId, injectedModel);
			log.debug("Built injected model for NPC id {} from mesh {} ({} verts, {} faces)",
				npcId, injectedModel.mesh.getId(),
				injectedModel.mesh.getVerticesCount(), injectedModel.mesh.getFaceCount());
			return;
		}

		if (RetroNpcMapping.requiresInjectedGeometry(data.getCategory()))
		{
			// The bundle is the only source for these, so there is no cache-backed fallback to try:
			// their model ids resolve in the live cache, but to unrelated geometry. Reaching here
			// means the bundle is absent or incomplete, and drawing nothing is the correct outcome.
			unbuildable.add(npcId);
			log.debug("No injected geometry for NPC id {} ({}), and it has no cache-backed fallback",
				npcId, data.getCategory());
			return;
		}

		Model model = build(data);
		if (model == null)
		{
			// Remember the failure so every subsequent spawn does not repeat the work
			unbuildable.add(npcId);
			log.debug("Could not build retro model for NPC id {}", npcId);
			return;
		}

		baseModels.put(npcId, model);

		// Remember which bundle mesh backs this NPC, so the skinning check can reach it with a lookup.
		// Only a single-part NPC qualifies: the memo holds one mesh, so for a multi-part NPC it would
		// be the body alone against the client's fully merged model, and the comparison could never
		// do anything but skip - once a frame, forever, now that giants share a bundled body.
		int[] modelIds = data.getRetroModelIds();
		RetroMesh bundleMesh = modelIds.length > 0 ? bundle.getMesh(modelIds[0]) : null;
		if (bundleMesh != null && bundleMesh.getVerticesCount() == model.getVerticesCount())
		{
			meshIds.put(npcId, modelIds[0]);
		}

		log.debug("Built retro model for NPC id {} from {} model ids", npcId, modelIds.length);
	}

	/**
	 * Publishes the injected asset bundle. Client thread only; the load itself happens off it.
	 */
	public void setBundle(RetroAssetBundle bundle)
	{
		this.bundle = bundle == null ? RetroAssetBundle.empty() : bundle;

		// The bundle arrives off-thread and can land after NPCs have already been built, whose
		// mesh lookups would then have been recorded against an empty bundle. Dropping the built
		// models makes them pick it up on the next spawn check; it costs one rebuild, once.
		baseModels.clear();
		meshIds.clear();
		injectedModels.clear();
		unbuildable.clear();

		log.debug("Retro asset bundle loaded: {}", this.bundle);
	}

	/**
	 * Resolves an animation id, caching the result. Returns null for -1 or an unknown id.
	 */
	public Animation animation(int animationId)
	{
		if (animationId == -1)
		{
			return null;
		}

		// computeIfAbsent is avoided so a null from loadAnimation is not retried every frame
		if (animations.containsKey(animationId))
		{
			return animations.get(animationId);
		}

		Animation animation = client.loadAnimation(animationId);
		animations.put(animationId, animation);
		return animation;
	}

	/**
	 * Poses the cached retro model for an NPC, or null when nothing has been built for its id.
	 *
	 * <p>The returned model is shared and is invalidated by the next applyTransformations call,
	 * including the client's own, so it has to be consumed before anything else runs. Both callers
	 * do: the draw callback hands it straight to the renderer, and the outline renderer projects
	 * and rasterizes it before returning. Must be called on the client thread.
	 */
	public Model pose(NPC npc)
	{
		int npcId = npc.getId();
		if (!substituted.contains(npcId))
		{
			return null;
		}

		InjectedModel injectedModel = injectedModels.get(npcId);
		if (injectedModel != null)
		{
			return poseInjected(npc, injectedModel);
		}

		Model base = baseModels.get(npcId);
		if (base == null)
		{
			return null;
		}

		Animation action = animation(npc.getAnimation());
		Animation pose = animation(npc.getPoseAnimation());
		Model posed = client.applyTransformations(base, action, npc.getAnimationFrame(), pose,
			npc.getPoseAnimationFrame());

		verifySkinning(npc, posed);

		if (posed == null || !useInjectionPipeline)
		{
			return posed;
		}

		// Copy into geometry we own. applyTransformations is the last point this is allowed - the
		// returned model is only valid until the next call, so the copy has to happen here rather
		// than at the consumer.
		injected.copyFrom(posed);
		return injected;
	}

	/**
	 * Prepares injected geometry for an NPC, or null when the bundle has nothing for it.
	 *
	 * <p>This is the path for meshes the live cache no longer holds. Everything the cache-backed
	 * build gets from the client - merging, recoloring, scaling, lighting - happens here instead,
	 * once, at spawn.
	 */
	private InjectedModel buildInjected(RetroNpcData data)
	{
		int[] modelIds = data.getInjectedModelIds();
		if (modelIds == null || modelIds.length == 0)
		{
			return null;
		}

		List<RetroMesh> parts = new ArrayList<>(modelIds.length);
		for (int modelId : modelIds)
		{
			RetroMesh part = bundle.getMesh(modelId);
			if (part != null)
			{
				parts.add(part);
			}
		}

		if (parts.isEmpty())
		{
			// Not a bundled category at all - the cache-backed path owns this one
			return null;
		}

		if (parts.size() < modelIds.length)
		{
			// Unlike the cache path, which draws whatever parts it managed to load, a partial set is
			// never drawn here: the bundle is generated from a fixed spec list, so a missing part is
			// a generator bug rather than a degraded asset, and a headless giant is worse than
			// falling back to the cache path. This is also what keeps armed skeletons whole - mesh
			// 2944 is bundled as the skinner test subject but 2946, its weapon, is not.
			log.debug("Bundle has {} of {} parts for model ids {}; refusing a partial merge",
				parts.size(), modelIds.length, Arrays.toString(modelIds));
			return null;
		}

		RetroMesh mesh = RetroMeshMerger.merge(modelIds[0], parts);

		// Recolor before lighting, not after: lit colors are baked once and never recomputed, so
		// a recolor applied afterward would have nothing left to bite on. Retro dragon meshes are
		// greyscale ramps, so this is what separates a red dragon from a black one.
		short[] colors = mesh.getFaceColors().clone();
		if (data.hasRecolors())
		{
			short[] find = data.getOriginalColors();
			short[] replace = data.getReplacementColors();
			for (int face = 0; face < colors.length; face++)
			{
				for (int pair = 0; pair < find.length; pair++)
				{
					if (colors[face] == find[pair])
					{
						colors[face] = replace[pair];
						break;
					}
				}
			}
		}

		mesh = scaled(mesh, colors, data.getScaleXZ(), data.getScaleY());

		int faceCount = mesh.getFaceCount();
		int[] colors1 = new int[faceCount];
		int[] colors2 = new int[faceCount];
		int[] colors3 = new int[faceCount];

		RetroLighter.light(
			mesh.getVerticesCount(), mesh.getVerticesX(), mesh.getVerticesY(), mesh.getVerticesZ(),
			faceCount, mesh.getFaceIndices1(), mesh.getFaceIndices2(), mesh.getFaceIndices3(),
			mesh.getFaceColors(), mesh.getFaceRenderTypes(), mesh.getFaceTextures(),
			ModelData.DEFAULT_AMBIENT, ModelData.DEFAULT_CONTRAST,
			ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z,
			colors1, colors2, colors3);

		InjectedModel injectedModel = new InjectedModel(mesh);
		injectedModel.model.bind(mesh, colors1, colors2, colors3);
		return injectedModel;
	}

	/**
	 * Returns the mesh with the recolored palette, resized when the retro mesh is not the size the
	 * modern NPC expects.
	 *
	 * <p>Substituted geometry gets no resize from the client, so it has to be applied here - the
	 * same reason the cache-backed path scales its {@code ModelData}.
	 */
	private static RetroMesh scaled(RetroMesh mesh, short[] colors, int scaleXZ, int scaleY)
	{
		boolean resize = scaleXZ != 128 || scaleY != 128;

		float[] vx = mesh.getVerticesX();
		float[] vy = mesh.getVerticesY();
		float[] vz = mesh.getVerticesZ();

		if (resize)
		{
			vx = vx.clone();
			vy = vy.clone();
			vz = vz.clone();
			for (int v = 0; v < vx.length; v++)
			{
				vx[v] = vx[v] * scaleXZ / 128f;
				vy[v] = vy[v] * scaleY / 128f;
				vz[v] = vz[v] * scaleXZ / 128f;
			}
		}

		// Faces and rigging are shared with the bundle mesh - only vertices and colors differ per
		// NPC, and neither the bundle nor any other NPC sees these copies
		return new RetroMesh(mesh.getId(), mesh.getPriority(), vx, vy, vz,
			mesh.getFaceIndices1(), mesh.getFaceIndices2(), mesh.getFaceIndices3(),
			colors, mesh.getFaceRenderTypes(), mesh.getFaceTransparencies(),
			mesh.getFaceRenderPriorities(), mesh.getFaceTextures(), mesh.getVertexGroups());
	}

	/**
	 * Poses injected geometry for the frame the client is currently showing.
	 *
	 * <p>An action animation wins over the movement pose when the bundle carries it. The client
	 * layers the two using the sequence's interleave mask; until the skinner implements that, the
	 * action replacing the pose outright is the closer of the two approximations, because an action
	 * is what the whole body is doing.
	 */
	private Model poseInjected(NPC npc, InjectedModel injectedModel)
	{
		RetroMesh mesh = injectedModel.mesh;
		RetroModel model = injectedModel.model;

		int action = npc.getAnimation();
		RetroClip clip = bundle.getClip(action);
		int frame = npc.getAnimationFrame();

		if (action != -1)
		{
			reportAction(npc.getId(), action, frame, clip);
		}

		if (clip == null || !clip.hasFrame(frame))
		{
			clip = bundle.getClip(npc.getPoseAnimation());
			frame = npc.getPoseAnimationFrame();
		}

		RetroRig rig = clip == null ? null : bundle.getRig(clip.getRigId());

		// A missing clip is not a failure - it leaves the mesh in its rest pose, which is far better
		// than not drawing the NPC at all
		skinner.pose(mesh, rig, clip, frame,
			model.getVerticesX(), model.getVerticesY(), model.getVerticesZ());
		model.calculateBoundsCylinder();

		return model;
	}

	/**
	 * Says once, per NPC id and animation, what the injected path did with an action animation.
	 *
	 * <p>An action that finds no clip leaves the NPC holding its movement pose, which on screen is
	 * indistinguishable from the animation simply not playing - so without this the only way to
	 * learn which sequence id an NPC really uses is to guess, and guessing at combat sequences is
	 * what once rewrote every dragon attack into a head butt. The frame index is reported with it
	 * because an action that starts part-way through its clip looks like the NPC snapping straight
	 * to the end.
	 *
	 * <p>Bounded by construction: one line per id and animation, not per frame.
	 */
	private void reportAction(int npcId, int action, int frame, RetroClip clip)
	{
		if (!log.isDebugEnabled() || !reportedActions.add(((long) npcId << 32) | (action & 0xFFFFFFFFL)))
		{
			return;
		}

		if (clip == null)
		{
			log.debug("NPC {} plays animation {}, which the bundle has no clip for - it will hold "
				+ "its movement pose", npcId, action);
		}
		else if (!clip.hasFrame(frame))
		{
			log.debug("NPC {} plays animation {} at frame {}, past the {} frames of its clip",
				npcId, action, frame, clip.getFrameCount());
		}
		else
		{
			log.debug("NPC {} plays animation {}, entering its clip at frame {} of {}",
				npcId, action, frame, clip.getFrameCount());
		}
	}

	/** Injected geometry for one NPC id: the mesh it was built from, and the model handed out. */
	private static final class InjectedModel
	{
		private final RetroMesh mesh;

		/**
		 * Reused across every NPC of this id and every frame. Safe for the same reason the client's
		 * own posed model is: it is consumed by the renderer before anything else can pose again.
		 * Two dragons on different frames are posed and drawn one after the other, not at once.
		 */
		private final RetroModel model = new RetroModel();

		private InjectedModel(RetroMesh mesh)
		{
			this.mesh = mesh;
		}
	}

	/**
	 * Checks {@link RetroSkinner} against the client's own animation for the same mesh and frame.
	 *
	 * <p>The skeleton is the only mesh that can settle this: it exists in the live cache, so the
	 * client will animate it, and it is also in the bundle, so we can animate it ourselves. Meshes
	 * that only exist in the bundle - which is the point of the exercise - have nothing to compare
	 * against by definition.
	 *
	 * <p>Only runs while no action animation is playing. The client layers a pose and an action
	 * using the sequence's interleave mask to decide which wins per transform, and the skinner does
	 * not implement that yet, so comparing mid-attack would measure the gap rather than the port.
	 */
	private void verifySkinning(NPC npc, Model posed)
	{
		if (skinningVerified || posed == null || npc.getAnimation() != -1)
		{
			return;
		}

		Integer meshId = meshIds.get(npc.getId());
		if (meshId == null)
		{
			return;
		}

		RetroMesh mesh = bundle.getMesh(meshId);
		RetroClip clip = bundle.getClip(npc.getPoseAnimation());
		if (mesh == null || clip == null)
		{
			return;
		}

		RetroRig rig = bundle.getRig(clip.getRigId());
		int frame = npc.getPoseAnimationFrame();
		if (rig == null || !clip.hasFrame(frame))
		{
			return;
		}

		try
		{
			// Only spend the one shot on a comparison that actually ran. A multi-part NPC memoises
			// its first part, so the client's merged model has more vertices than the bundle mesh
			// and the comparison bails - if that burned the flag, a hill giant walking past would
			// deny the skeleton, the one subject that can settle this, its turn.
			skinningVerified = compareSkinning(mesh, rig, clip, frame, posed);
		}
		catch (RuntimeException ex)
		{
			// A check must never be why a frame fails to draw
			skinningVerified = true;
			log.debug("Skinning comparison failed", ex);
		}
	}

	/** Returns whether the comparison actually ran, so a skip does not consume the one shot. */
	private boolean compareSkinning(RetroMesh mesh, RetroRig rig, RetroClip clip, int frame, Model posed)
	{
		int count = mesh.getVerticesCount();
		if (posed.getVerticesCount() != count)
		{
			log.debug("Skinning comparison skipped: bundle mesh has {} vertices, the client's has {}",
				count, posed.getVerticesCount());
			return false;
		}

		if (skinnedX.length < count)
		{
			skinnedX = new float[count];
			skinnedY = new float[count];
			skinnedZ = new float[count];
		}

		if (!skinner.pose(mesh, rig, clip, frame, skinnedX, skinnedY, skinnedZ))
		{
			log.debug("Skinning comparison skipped: frame {} of clip {} could not be applied",
				frame, clip.getSequenceId());
			return false;
		}

		float[] theirX = posed.getVerticesX();
		float[] theirY = posed.getVerticesY();
		float[] theirZ = posed.getVerticesZ();

		float maxDelta = 0f;
		int worstVertex = -1;
		for (int v = 0; v < count; v++)
		{
			float delta = Math.max(Math.abs(skinnedX[v] - theirX[v]),
				Math.max(Math.abs(skinnedY[v] - theirY[v]), Math.abs(skinnedZ[v] - theirZ[v])));
			if (delta > maxDelta)
			{
				maxDelta = delta;
				worstVertex = v;
			}
		}

		// Exact agreement is not expected: the reference this was ported from works in integer
		// vertices where the client works in floats, so sub-unit drift is the port being right
		// rather than wrong. A tile is 128 units, so anything under 1 is invisible.
		if (maxDelta < 1f)
		{
			log.debug("RetroSkinner matches the client on clip {} frame {} over {} vertices "
				+ "(max delta {})", clip.getSequenceId(), frame, count, maxDelta);
			return true;
		}

		log.debug("RetroSkinner differs from the client on clip {} frame {}: max delta {} at vertex "
				+ "{} - ours [{}, {}, {}] theirs [{}, {}, {}]",
			clip.getSequenceId(), frame, maxDelta, worstVertex,
			skinnedX[worstVertex], skinnedY[worstVertex], skinnedZ[worstVertex],
			theirX[worstVertex], theirY[worstVertex], theirZ[worstVertex]);
		return true;
	}

	/**
	 * Turns the injection pipeline on or off. Client thread only.
	 */
	public void setUseInjectionPipeline(boolean useInjectionPipeline)
	{
		if (this.useInjectionPipeline == useInjectionPipeline)
		{
			return;
		}

		this.useInjectionPipeline = useInjectionPipeline;
		log.debug("Retro injection pipeline {}", useInjectionPipeline ? "enabled" : "disabled");

		// Which path a model was built down is decided at build time, so already-built models have
		// to go for the toggle to take effect on NPCs that are already on screen
		baseModels.clear();
		injectedModels.clear();
		meshIds.clear();
		unbuildable.clear();
	}

	/** Whether injected geometry is currently in use. */
	public boolean isInjectionPipelineEnabled()
	{
		return useInjectionPipeline;
	}

	public void clear()
	{
		substituted.clear();
		reportedActions.clear();
		baseModels.clear();
		meshIds.clear();
		injectedModels.clear();
		animations.clear();
		unbuildable.clear();
	}

	private Model build(RetroNpcData data)
	{
		int[] modelIds = data.getRetroModelIds();
		if (modelIds == null || modelIds.length == 0)
		{
			return null;
		}

		ModelData[] parts = new ModelData[modelIds.length];
		int found = 0;
		for (int modelId : modelIds)
		{
			ModelData part = client.loadModelData(modelId);
			if (part != null)
			{
				parts[found++] = part;
			}
		}

		if (found == 0)
		{
			return null;
		}

		ModelData merged = found == 1 ? parts[0] : client.mergeModels(parts, found);
		if (merged == null)
		{
			return null;
		}

		if (data.hasRecolors())
		{
			// cloneColors first, for the same reason as cloneVertices below - loadModelData hands
			// back the client's shared arrays, and recoloring in place would repaint the cached
			// model for everything else using it
			short[] originalColors = data.getOriginalColors();
			short[] replacementColors = data.getReplacementColors();
			merged = merged.cloneColors();
			for (int i = 0; i < originalColors.length; i++)
			{
				merged.recolor(originalColors[i], replacementColors[i]);
			}
		}

		int scaleXZ = data.getScaleXZ();
		int scaleY = data.getScaleY();
		if (scaleXZ != 128 || scaleY != 128)
		{
			// cloneVertices first - loadModelData hands back the client's shared arrays, and
			// scaling in place would resize the cached model for everything else using it
			merged = merged.cloneVertices().scale(scaleXZ, scaleY, scaleXZ);
		}

		Model lit = merged.light();
		verifyLighting(merged, lit);
		return lit;
	}

	/**
	 * Checks {@link RetroLighter} against the client's own {@code light()} for the same mesh.
	 *
	 * <p>Injected geometry cannot be lit by the client - {@code ModelData} only exists for models it
	 * decoded itself - so the lighting has to be reimplemented, and the only trustworthy way to know
	 * it is right is to run both over geometry the client <em>can</em> light and compare. Purely a
	 * check: the model returned to the renderer is still the client's.
	 *
	 * <p>Runs once per session rather than per model, since a mismatch is a property of the
	 * algorithm, not of any one mesh.
	 */
	private void verifyLighting(ModelData source, Model lit)
	{
		if (lightingVerified || lit == null)
		{
			return;
		}
		lightingVerified = true;

		try
		{
			compareLighting(source, lit);
		}
		catch (RuntimeException ex)
		{
			// This is a check, not a feature - it must never be the reason a model fails to build
			log.debug("Lighting comparison failed", ex);
		}
	}

	private void compareLighting(ModelData source, Model lit)
	{
		int faceCount = source.getFaceCount();
		int[] ours1 = new int[faceCount];
		int[] ours2 = new int[faceCount];
		int[] ours3 = new int[faceCount];

		RetroLighter.light(
			source.getVerticesCount(), source.getVerticesX(), source.getVerticesY(), source.getVerticesZ(),
			faceCount, source.getFaceIndices1(), source.getFaceIndices2(), source.getFaceIndices3(),
			source.getFaceColors(), null, source.getFaceTextures(),
			ModelData.DEFAULT_AMBIENT, ModelData.DEFAULT_CONTRAST,
			ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z,
			ours1, ours2, ours3);

		int[] theirs1 = lit.getFaceColors1();
		int[] theirs2 = lit.getFaceColors2();
		int[] theirs3 = lit.getFaceColors3();

		int mismatches = 0;
		int firstMismatch = -1;
		for (int face = 0; face < faceCount; face++)
		{
			if (ours1[face] != theirs1[face] || ours2[face] != theirs2[face] || ours3[face] != theirs3[face])
			{
				if (firstMismatch == -1)
				{
					firstMismatch = face;
				}
				mismatches++;
			}
		}

		if (mismatches == 0)
		{
			log.debug("RetroLighter matches the client exactly over {} faces", faceCount);
			return;
		}

		log.debug("RetroLighter differs from the client on {} of {} faces; first at {} - "
				+ "ours [{}, {}, {}] theirs [{}, {}, {}]",
			mismatches, faceCount, firstMismatch,
			ours1[firstMismatch], ours2[firstMismatch], ours3[firstMismatch],
			theirs1[firstMismatch], theirs2[firstMismatch], theirs3[firstMismatch]);
	}
}
