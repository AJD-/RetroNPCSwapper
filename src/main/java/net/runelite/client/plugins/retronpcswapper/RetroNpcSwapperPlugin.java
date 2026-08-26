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
package net.runelite.client.plugins.retronpcswapper;

import com.google.inject.Provides;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.NodeCache;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.retronpcswapper.cache.RetroCacheReader;
import net.runelite.client.plugins.retronpcswapper.cache.RetroNpcDecoder;
import net.runelite.client.plugins.retronpcswapper.cache.RetroNpcDefinition;

@PluginDescriptor(
	name = "Retro NPC Swapper",
	description = "Swaps modern NPC models and animations to their retro 2004/2005 variants from the 2005 cache.",
	tags = {"npc", "retro", "swapper", "model", "animation", "cache"}
)
public class RetroNpcSwapperPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(RetroNpcSwapperPlugin.class);

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private RetroNpcConfig config;

	private final Set<Integer> modifiedNpcIndexes = new HashSet<>();
	private final Map<Integer, Integer> originalIdleAnims = new HashMap<>();
	private final Map<Integer, Integer> originalPoseAnims = new HashMap<>();
	private final Map<Integer, Integer> originalIdleRotateLeftAnims = new HashMap<>();
	private final Map<Integer, Integer> originalIdleRotateRightAnims = new HashMap<>();
	private final Map<Integer, Integer> originalWalkAnims = new HashMap<>();
	private final Map<Integer, Integer> originalWalkRotate180Anims = new HashMap<>();
	private final Map<Integer, Integer> originalWalkRotateLeftAnims = new HashMap<>();
	private final Map<Integer, Integer> originalWalkRotateRightAnims = new HashMap<>();
	private final Map<Integer, Integer> originalRunAnims = new HashMap<>();
	private final Map<Integer, int[]> originalModelsMap = new HashMap<>();

	@Override
	protected void startUp() throws Exception
	{
		log.info("Retro NPC Swapper started");
		initRetroCache();
		clientThread.invoke(this::recheckLoadedNpcs);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Retro NPC Swapper stopped");
		clientThread.invoke(this::resetAllModifiedNpcs);
	}

	private void initRetroCache()
	{
		File cacheDir = new File("retrocache/2005cache");
		if (!cacheDir.exists())
		{
			cacheDir = new File(RuneLite.RUNELITE_DIR, "retro-npc-swapper/2005cache");
		}

		if (cacheDir.exists())
		{
			RetroCacheReader reader = new RetroCacheReader(cacheDir);
			if (reader.init())
			{
				byte[] archiveData = reader.readFile(0, 2); // Archive 0 file 2 (config.jag)
				if (archiveData != null)
				{
					Map<String, byte[]> files = reader.readArchive(archiveData);
					byte[] npcDat = files.get(String.valueOf(RetroCacheReader.hashFileName("npc.dat")));
					byte[] npcIdx = files.get(String.valueOf(RetroCacheReader.hashFileName("npc.idx")));
					if (npcDat != null && npcIdx != null)
					{
						Map<Integer, RetroNpcDefinition> defs = RetroNpcDecoder.decodeAll(npcDat, npcIdx);
						RetroNpcMapping.loadFrom2005Cache(defs);
						log.info("Successfully loaded {} 2005 cache NPC definitions into RetroNpcMapping", defs.size());
					}
				}
				reader.close();
			}
		}
		else
		{
			log.warn("2005 retro cache directory not found at: {}", cacheDir.getAbsolutePath());
		}
	}

	@Provides
	RetroNpcConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RetroNpcConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!"retronpcswapper".equals(event.getGroup()))
		{
			return;
		}

		// Refresh active NPC visual overrides when configuration options change
		clientThread.invoke(this::recheckLoadedNpcs);
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		processNpc(event.getNpc());
		resetNpcModelCache();
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		processNpc(event.getNpc());
		resetNpcModelCache();
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		if (npc != null)
		{
			int npcIdx = npc.getIndex();
			modifiedNpcIndexes.remove(npcIdx);
			originalIdleAnims.remove(npcIdx);
			originalPoseAnims.remove(npcIdx);
			originalIdleRotateLeftAnims.remove(npcIdx);
			originalIdleRotateRightAnims.remove(npcIdx);
			originalWalkAnims.remove(npcIdx);
			originalWalkRotate180Anims.remove(npcIdx);
			originalWalkRotateLeftAnims.remove(npcIdx);
			originalWalkRotateRightAnims.remove(npcIdx);
			originalRunAnims.remove(npcIdx);
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		Actor actor = event.getActor();
		if (!(actor instanceof NPC))
		{
			return;
		}

		NPC npc = (NPC) actor;
		RetroNpcData data = RetroNpcMapping.get(npc.getId(), npc.getName());
		if (data == null || !isCategoryEnabled(data.getCategory()))
		{
			return;
		}

		int anim = npc.getAnimation();

		if (anim == -1)
		{
			// Animation finished - re-enforce retro pose animations (idle / walk)
			applyRetroSwap(npc, data);
			return;
		}

		// Guard against re-triggering if current animation is already the retro target animation
		if (anim == data.getAttackAnimationId() || anim == data.getDefendAnimationId() || anim == data.getDeathAnimationId())
		{
			return;
		}

		// Match modern death animations (5491, 5492, 5493, 5509, 5512, 8004, 8005, 1589, 1828, 313, 302, 836, 172, 173, 5390)
		if (data.getDeathAnimationId() != -1 && (anim == 5491 || anim == 5492 || anim == 5493 || anim == 5509 || anim == 5512 || anim == 8004 || anim == 8005 || anim == 1589 || anim == 1828 || anim == 313 || anim == 302 || anim == 836 || anim == 172 || anim == 173 || anim == 5390))
		{
			log.debug("SWAPPING DEATH ANIMATION for NPC '{}' (ID: {}): {} -> {}", npc.getName(), npc.getId(), anim, data.getDeathAnimationId());
			npc.setAnimation(data.getDeathAnimationId());
		}
		// Match modern defend/take hit animations (5490, 5489, 5488, 5508, 5511, 8003, 1588, 1827, 312, 310, 300, 424, 170, 5388)
		else if (data.getDefendAnimationId() != -1 && (anim == 5490 || anim == 5489 || anim == 5488 || anim == 5508 || anim == 5511 || anim == 8003 || anim == 1588 || anim == 1827 || anim == 312 || anim == 310 || anim == 300 || anim == 424 || anim == 170 || anim == 5388))
		{
			log.debug("SWAPPING DEFEND ANIMATION for NPC '{}' (ID: {}): {} -> {}", npc.getName(), npc.getId(), anim, data.getDefendAnimationId());
			npc.setAnimation(data.getDefendAnimationId());
		}
		// Match modern attack animations (5485, 5486, 5487, 5507, 5510, 8002, 309, 240, 169, 5385)
		else if (data.getAttackAnimationId() != -1 && (anim == 5485 || anim == 5486 || anim == 5487 || anim == 5507 || anim == 5510 || anim == 8002 || anim == 309 || anim == 240 || anim == 169 || anim == 5385))
		{
			log.debug("SWAPPING ATTACK ANIMATION for NPC '{}' (ID: {}): {} -> {}", npc.getName(), npc.getId(), anim, data.getAttackAnimationId());
			npc.setAnimation(data.getAttackAnimationId());
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGING_IN
			|| gameStateChanged.getGameState() == GameState.HOPPING)
		{
			modifiedNpcIndexes.clear();
			originalIdleAnims.clear();
			originalPoseAnims.clear();
			originalIdleRotateLeftAnims.clear();
			originalIdleRotateRightAnims.clear();
			originalWalkAnims.clear();
			originalWalkRotate180Anims.clear();
			originalWalkRotateLeftAnims.clear();
			originalWalkRotateRightAnims.clear();
			originalRunAnims.clear();
		}
	}

	/**
	 * Evaluates an NPC to check if it should be swapped to its retro variant.
	 */
	private void processNpc(NPC npc)
	{
		if (npc == null)
		{
			return;
		}

		RetroNpcData data = RetroNpcMapping.get(npc.getId(), npc.getName());
		if (data == null)
		{
			resetNpc(npc);
			return;
		}

		// Verify if category toggle is enabled in configuration
		if (isCategoryEnabled(data.getCategory()))
		{
			applyRetroSwap(npc, data);
		}
		else
		{
			resetNpc(npc);
		}
	}

	/**
	 * Applies retro model and animation overrides to the given NPC.
	 */
	private void applyRetroSwap(NPC npc, RetroNpcData data)
	{
		NPCComposition comp = npc.getTransformedComposition();
		if (comp == null)
		{
			comp = npc.getComposition();
		}
		if (comp == null)
		{
			comp = client.getNpcDefinition(npc.getId());
		}

		if (comp == null)
		{
			return;
		}

		int npcIdx = npc.getIndex();
		int npcId = npc.getId();
		int compId = comp.getId();

		// Save original animation states before overriding
		if (!modifiedNpcIndexes.contains(npcIdx))
		{
			originalIdleAnims.put(npcIdx, npc.getIdlePoseAnimation());
			originalPoseAnims.put(npcIdx, npc.getPoseAnimation());
			originalIdleRotateLeftAnims.put(npcIdx, npc.getIdleRotateLeft());
			originalIdleRotateRightAnims.put(npcIdx, npc.getIdleRotateRight());
			originalWalkAnims.put(npcIdx, npc.getWalkAnimation());
			originalWalkRotate180Anims.put(npcIdx, npc.getWalkRotate180());
			originalWalkRotateLeftAnims.put(npcIdx, npc.getWalkRotateLeft());
			originalWalkRotateRightAnims.put(npcIdx, npc.getWalkRotateRight());
			originalRunAnims.put(npcIdx, npc.getRunAnimation());
			modifiedNpcIndexes.add(npcIdx);
		}

		int[] compModels = comp.getModels();
		log.debug("INTERCEPTED NPC: name='{}', id={}, compId={}, index={}, category={}, origModels={}",
			npc.getName(), npc.getId(), compId, npcIdx, data.getCategory(), Arrays.toString(compModels));

		// Swap model IDs array on NPCComposition
		if (compModels != null && data.getRetroModelIds() != null && data.getRetroModelIds().length > 0)
		{
			int[] retroModels = data.getRetroModelIds();
			if (!originalModelsMap.containsKey(compId))
			{
				originalModelsMap.put(compId, compModels.clone());
			}
			if (!originalModelsMap.containsKey(npcId))
			{
				originalModelsMap.put(npcId, compModels.clone());
			}

			log.debug("SWAPPING MODELS for NPC '{}' (ID: {}, CompID: {}): original={} -> retro={}",
				npc.getName(), npc.getId(), compId, Arrays.toString(compModels), Arrays.toString(retroModels));

			for (int i = 0; i < compModels.length; i++)
			{
				if (i < retroModels.length)
				{
					compModels[i] = retroModels[i];
				}
				else
				{
					compModels[i] = -1;
				}
			}
		}

		// Apply idle animation override from 2005 cache definition
		if (data.getIdleAnimationId() != -1)
		{
			log.debug("SWAPPING IDLE ANIMATION for NPC '{}': orig={} -> retro={}",
				npc.getName(), npc.getIdlePoseAnimation(), data.getIdleAnimationId());
			npc.setIdlePoseAnimation(data.getIdleAnimationId());
			npc.setPoseAnimation(data.getIdleAnimationId());
			npc.setIdleRotateLeft(data.getIdleAnimationId());
			npc.setIdleRotateRight(data.getIdleAnimationId());
		}

		// Apply walking animation override from 2005 cache definition
		if (data.getWalkAnimationId() != -1)
		{
			log.debug("SWAPPING WALK ANIMATION for NPC '{}': orig={} -> retro={}",
				npc.getName(), npc.getWalkAnimation(), data.getWalkAnimationId());
			npc.setWalkAnimation(data.getWalkAnimationId());
			npc.setWalkRotate180(data.getWalkAnimationId());
			npc.setWalkRotateLeft(data.getWalkAnimationId());
			npc.setWalkRotateRight(data.getWalkAnimationId());
			npc.setRunAnimation(data.getWalkAnimationId());
		}
	}

	/**
	 * Resets an NPC back to its original visual state.
	 */
	private void resetNpc(NPC npc)
	{
		int npcIdx = npc.getIndex();
		int npcId = npc.getId();

		if (modifiedNpcIndexes.remove(npcIdx))
		{
			log.debug("Resetting NPC visuals for: {} (ID: {})", npc.getName(), npc.getId());

			NPCComposition comp = npc.getTransformedComposition();
			if (comp == null)
			{
				comp = npc.getComposition();
			}
			if (comp == null)
			{
				comp = client.getNpcDefinition(npcId);
			}

			int compId = comp != null ? comp.getId() : npcId;
			int[] origModels = originalModelsMap.get(compId);
			if (origModels == null)
			{
				origModels = originalModelsMap.get(npcId);
			}

			if (origModels != null && comp != null && comp.getModels() != null)
			{
				int[] compModels = comp.getModels();
				for (int i = 0; i < compModels.length; i++)
				{
					if (i < origModels.length)
					{
						compModels[i] = origModels[i];
					}
					else
					{
						compModels[i] = -1;
					}
				}
			}

			Integer origIdle = originalIdleAnims.remove(npcIdx);
			if (origIdle != null && origIdle != -1)
			{
				npc.setIdlePoseAnimation(origIdle);
			}

			Integer origPose = originalPoseAnims.remove(npcIdx);
			if (origPose != null && origPose != -1)
			{
				npc.setPoseAnimation(origPose);
			}

			Integer origIdleRotL = originalIdleRotateLeftAnims.remove(npcIdx);
			if (origIdleRotL != null && origIdleRotL != -1)
			{
				npc.setIdleRotateLeft(origIdleRotL);
			}

			Integer origIdleRotR = originalIdleRotateRightAnims.remove(npcIdx);
			if (origIdleRotR != null && origIdleRotR != -1)
			{
				npc.setIdleRotateRight(origIdleRotR);
			}

			Integer origWalk = originalWalkAnims.remove(npcIdx);
			if (origWalk != null && origWalk != -1)
			{
				npc.setWalkAnimation(origWalk);
			}

			Integer origWalk180 = originalWalkRotate180Anims.remove(npcIdx);
			if (origWalk180 != null && origWalk180 != -1)
			{
				npc.setWalkRotate180(origWalk180);
			}

			Integer origWalkRotL = originalWalkRotateLeftAnims.remove(npcIdx);
			if (origWalkRotL != null && origWalkRotL != -1)
			{
				npc.setWalkRotateLeft(origWalkRotL);
			}

			Integer origWalkRotR = originalWalkRotateRightAnims.remove(npcIdx);
			if (origWalkRotR != null && origWalkRotR != -1)
			{
				npc.setWalkRotateRight(origWalkRotR);
			}

			Integer origRun = originalRunAnims.remove(npcIdx);
			if (origRun != null && origRun != -1)
			{
				npc.setRunAnimation(origRun);
			}
		}
	}

	/**
	 * Checks if a specific NPC category toggle is enabled in config.
	 */
	private boolean isCategoryEnabled(RetroNpcCategory category)
	{
		if (category == null)
		{
			return false;
		}

		switch (category)
		{
			case DEMONS:
				return config.swapDemons();
			case DRAGONS:
				return config.swapDragons();
			case MISC:
				return config.swapMisc();
			default:
				return false;
		}
	}

	/**
	 * Re-evaluates all currently loaded scene NPCs against active configuration toggles.
	 */
	private void recheckLoadedNpcs()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		for (NPC npc : client.getNpcs())
		{
			if (npc != null)
			{
				processNpc(npc);
			}
		}

		resetNpcModelCache();
	}

	/**
	 * Resets all modified NPCs in the scene back to default visuals upon plugin shutdown.
	 */
	private void resetAllModifiedNpcs()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			for (NPC npc : client.getNpcs())
			{
				if (npc != null && modifiedNpcIndexes.contains(npc.getIndex()))
				{
					resetNpc(npc);
				}
			}
		}

		// Also restore all cached NPC compositions that were modified, ensuring no stale models remain
		for (Map.Entry<Integer, int[]> entry : originalModelsMap.entrySet())
		{
			int id = entry.getKey();
			int[] origModels = entry.getValue();
			NPCComposition comp = client.getNpcDefinition(id);
			if (comp != null && comp.getModels() != null && origModels != null)
			{
				int[] compModels = comp.getModels();
				for (int i = 0; i < compModels.length; i++)
				{
					if (i < origModels.length)
					{
						compModels[i] = origModels[i];
					}
					else
					{
						compModels[i] = -1;
					}
				}
			}
		}

		modifiedNpcIndexes.clear();
		originalIdleAnims.clear();
		originalPoseAnims.clear();
		originalIdleRotateLeftAnims.clear();
		originalIdleRotateRightAnims.clear();
		originalWalkAnims.clear();
		originalWalkRotate180Anims.clear();
		originalWalkRotateLeftAnims.clear();
		originalWalkRotateRightAnims.clear();
		originalRunAnims.clear();
		originalModelsMap.clear();

		resetNpcModelCache();
	}

	/**
	 * Resets the client's internal NPCComposition and NPC model caches so that model ID
	 * changes to NPCComposition.getModels() take effect immediately on screen.
	 */
	private void resetNpcModelCache()
	{
		try
		{
			NPCComposition sampleComp = client.getNpcDefinition(0);
			if (sampleComp != null)
			{
				Class<?> compClass = sampleComp.getClass();
				for (Field field : compClass.getDeclaredFields())
				{
					if (Modifier.isStatic(field.getModifiers()) && NodeCache.class.isAssignableFrom(field.getType()))
					{
						field.setAccessible(true);
						NodeCache cache = (NodeCache) field.get(null);
						if (cache != null)
						{
							cache.reset();
						}
					}
				}
			}
		}
		catch (Throwable t)
		{
			log.debug("Failed to reset NPC model cache: {}", t.getMessage());
		}
	}
}
