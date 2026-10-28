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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.retronpcswapper.inject.RetroAssetBundle;
import com.retronpcswapper.inject.RetroClip;
import com.retronpcswapper.inject.RetroFrameTiming;
import com.retronpcswapper.inject.RetroMesh;
import com.retronpcswapper.inject.RetroRig;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.IntPredicate;
import net.runelite.api.Animation;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.NPC;
import org.junit.Before;
import org.junit.Test;

public class RetroModelCacheSmoothingTest
{
	private static final int NPC_ID = 99990;
	private static final int MODEL_ID = 100;
	private static final int ACTION_ID = 6184;
	private static final int POSE_ID = 6182;

	private Client client;
	private RetroModelCache cache;
	private RetroAnimationSmoothing smoothing;
	private Model base;
	private Animation action;
	private Animation pose;

	private static Animation animation(int id, int frames, int length)
	{
		int[] lengths = new int[frames];
		Arrays.fill(lengths, length);
		Animation animation = mock(Animation.class);
		when(animation.getId()).thenReturn(id);
		when(animation.getNumFrames()).thenReturn(frames);
		when(animation.getFrameLengths()).thenReturn(lengths);
		return animation;
	}

	private static NPC npc(int animation, int frame, int poseAnimation, int poseFrame)
	{
		NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(NPC_ID);
		when(npc.getAnimation()).thenReturn(animation);
		when(npc.getAnimationFrame()).thenReturn(frame);
		when(npc.getPoseAnimation()).thenReturn(poseAnimation);
		when(npc.getPoseAnimationFrame()).thenReturn(poseFrame);
		return npc;
	}

	@Before
	public void setUp()
	{
		client = mock(Client.class);
		Injector injector = Guice.createInjector(binder -> binder.bind(Client.class).toInstance(client));
		cache = injector.getInstance(RetroModelCache.class);
		smoothing = injector.getInstance(RetroAnimationSmoothing.class);

		base = mock(Model.class);
		ModelData data = mock(ModelData.class);
		when(data.light()).thenReturn(base);
		when(client.loadModelData(MODEL_ID)).thenReturn(data);

		action = animation(ACTION_ID, 5, 6);
		pose = animation(POSE_ID, 8, 4);
		when(client.loadAnimation(ACTION_ID)).thenReturn(action);
		when(client.loadAnimation(POSE_ID)).thenReturn(pose);
		when(client.getAnimationInterpolationFilter()).thenReturn((IntPredicate) id -> true);

		RetroNpcData npcData = RetroNpcData.builder()
			.category(RetroNpcCategory.GOBLINS)
			.retroModelIds(new int[]{MODEL_ID})
			.build();
		assertTrue(cache.ensureBuilt(NPC_ID, npcData));
		cache.setSubstituted(NPC_ID);
	}

	@Test
	public void testActingNeedsAnActionFrameInRange()
	{
		Animation five = animation(1, 5, 1);
		assertFalse(RetroModelCache.acting(null, 0));
		assertFalse(RetroModelCache.acting(five, -1));
		assertFalse(RetroModelCache.acting(five, 5));
		assertTrue(RetroModelCache.acting(five, 0));
		assertTrue(RetroModelCache.acting(five, 4));
	}

	@Test
	public void testBlendingActionPassesTheActionAlone()
	{
		smoothing.setEnabled(true);
		when(client.getGameCycle()).thenReturn(100, 103);
		NPC npc = npc(ACTION_ID, 2, POSE_ID, 7);

		cache.pose(npc);
		cache.pose(npc);

		verify(client).applyTransformations(base, action, RetroFrameTiming.pack(2, 0, false), null, 0);
		verify(client).applyTransformations(base, action, RetroFrameTiming.pack(2, 3, false), null, 0);
		verify(client, never()).applyTransformations(any(), any(), anyInt(), any(Animation.class), anyInt());
	}

	@Test
	public void testClocksAreKeyedByNpcInstance()
	{
		smoothing.setEnabled(true);
		when(client.getGameCycle()).thenReturn(100, 103, 103);
		NPC first = npc(ACTION_ID, 2, POSE_ID, 7);
		NPC second = npc(ACTION_ID, 2, POSE_ID, 7);

		cache.pose(first);
		cache.pose(first);
		cache.pose(second);

		verify(client).applyTransformations(base, action, RetroFrameTiming.pack(2, 3, false), null, 0);
		verify(client, times(2)).applyTransformations(base, action, RetroFrameTiming.pack(2, 0, false), null, 0);
	}

	@Test
	public void testForgetRestartsTheClock()
	{
		smoothing.setEnabled(true);
		when(client.getGameCycle()).thenReturn(100, 103);
		NPC npc = npc(ACTION_ID, 2, POSE_ID, 7);

		cache.pose(npc);
		cache.forget(npc);
		cache.pose(npc);

		verify(client, times(2)).applyTransformations(base, action, RetroFrameTiming.pack(2, 0, false), null, 0);
	}

	@Test
	public void testBlendingIdlePassesThePoseAloneAsLooping()
	{
		smoothing.setEnabled(true);
		when(client.getGameCycle()).thenReturn(50);

		cache.pose(npc(-1, -1, POSE_ID, 3));

		verify(client).applyTransformations(base, pose, RetroFrameTiming.pack(3, 0, true), null, 0);
		verify(client, never()).applyTransformations(any(), any(), anyInt(), any(Animation.class), anyInt());
	}

	@Test
	public void testActionPastItsFramesFallsBackToThePose()
	{
		smoothing.setEnabled(true);

		cache.pose(npc(ACTION_ID, 5, POSE_ID, 1));

		verify(client).applyTransformations(base, pose, RetroFrameTiming.pack(1, 0, true), null, 0);
	}

	@Test
	public void testSmoothingOffKeepsTheTwoAnimationCall()
	{
		cache.pose(npc(ACTION_ID, 2, POSE_ID, 7));

		verify(client).applyTransformations(base, action, 2, pose, 7);
		verify(client, never()).applyTransformations(any(), any(), anyInt(), isNull(), anyInt());
	}

	@Test
	public void testFilteredAnimationKeepsTheTwoAnimationCall()
	{
		smoothing.setEnabled(true);
		when(client.getAnimationInterpolationFilter()).thenReturn((IntPredicate) id -> id != ACTION_ID);

		cache.pose(npc(ACTION_ID, 2, POSE_ID, 7));

		verify(client).applyTransformations(base, action, 2, pose, 7);
	}

	/** Swaps in a one-mesh bundle and builds the NPC down the injected path, idling on POSE_ID. */
	private void installInjected()
	{
		RetroMesh mesh = new RetroMesh(MODEL_ID, 0,
			new float[]{0f, 100f}, new float[]{0f, 0f}, new float[]{0f, 0f},
			new int[]{0}, new int[]{1}, new int[]{0},
			new short[]{0}, null, null, null, null,
			null, null, null, null,
			new int[][]{{0, 1}});
		RetroRig rig = new RetroRig(2, new int[]{0, 1}, new int[][]{{0}, {0}});
		RetroClip clip = new RetroClip(POSE_ID, 2,
			new int[][]{{0, 1}, {0, 1}},
			new int[][]{{0, 0}, {0, 10}},
			new int[][]{{0, 0}, {0, 0}},
			new int[][]{{0, 0}, {0, 0}});

		cache.setUseInjectionPipeline(true);
		cache.setBundle(new RetroAssetBundle(Collections.singletonMap(MODEL_ID, mesh),
			Collections.singletonMap(2, rig), Collections.singletonMap(POSE_ID, clip)));
		RetroNpcData npcData = RetroNpcData.builder()
			.category(RetroNpcCategory.COWS)
			.retroModelIds(new int[]{MODEL_ID})
			.build();
		assertTrue(cache.ensureBuilt(NPC_ID, npcData));
	}

	@Test
	public void testInjectedPoseSkipsTheAnimationLookupWhileSmoothingIsOff()
	{
		installInjected();

		assertNotNull(cache.pose(npc(-1, -1, POSE_ID, 1)));

		verify(client, never()).loadAnimation(anyInt());
	}

	@Test
	public void testInjectedPoseLooksUpTheLiveAnimationWhileSmoothing()
	{
		installInjected();
		smoothing.setEnabled(true);

		assertNotNull(cache.pose(npc(-1, -1, POSE_ID, 1)));

		verify(client).loadAnimation(POSE_ID);
	}
}
