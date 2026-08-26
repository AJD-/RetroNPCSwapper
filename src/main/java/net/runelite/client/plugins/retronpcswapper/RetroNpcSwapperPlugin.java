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
	private final Map<Integer, Integer> originalWidthScales = new HashMap<>();
	private final Map<Integer, Integer> originalHeightScales = new HashMap<>();

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

		// Match modern death animations
		if (data.getDeathAnimationId() != -1 && (
			anim == 5491 || anim == 5492 || anim == 5493 || anim == 5509 || anim == 5512 || anim == 8004 || anim == 8005
			|| anim == 1589 || anim == 1828 || anim == 313 || anim == 302 || anim == 836 || anim == 172 || anim == 173 || anim == 5390
			|| anim == 6182 // Goblin death
			|| (data.getCategory() == RetroNpcCategory.ZOMBIES && (
				anim == 5575 || anim == 5580 || anim == 5587 || anim == 5588 || anim == 5591 || anim == 5594 || anim == 836 || anim == 2304 || anim == 302
			))
			|| anim == 4653 || anim == 4659 || anim == 4667 || anim == 4673 || anim == 7004 // Giant death
			|| anim == 5389 // Chicken death
			|| anim == 7044 // Modern guard death
		))
		{
			log.debug("SWAPPING DEATH ANIMATION for NPC '{}' (ID: {}): {} -> {}", npc.getName(), npc.getId(), anim, data.getDeathAnimationId());
			npc.setAnimation(data.getDeathAnimationId());
		}
		// Match modern defend/take hit animations
		else if (data.getDefendAnimationId() != -1 && (
			anim == 5490 || anim == 5489 || anim == 5488 || anim == 5508 || anim == 5511 || anim == 8003 || anim == 1588
			|| anim == 1827 || anim == 312 || anim == 310 || anim == 300 || anim == 424 || anim == 170 || anim == 5388
			|| anim == 6183 // Goblin defend
			|| (data.getCategory() == RetroNpcCategory.ZOMBIES && (
				(anim >= 5567 && anim <= 5595 && anim != 5568 && anim != 5571 && anim != 5573 && anim != 5576 && anim != 5577 && anim != 5578 && anim != 5581 && anim != 5583 && anim != 5585 && anim != 5590 && anim != 5593 && anim != 5575 && anim != 5580 && anim != 5587 && anim != 5588 && anim != 5591 && anim != 5594)
				|| anim == 424 || anim == 425 || anim == 1156 || anim == 2303 || anim == 300 || anim == 301 || anim == 303
			))
			|| anim == 4651 || anim == 4657 || anim == 4665 || anim == 4671 || anim == 7001 // Giant defend
			|| anim == 7043 // Modern guard defend
		))
		{
			log.debug("SWAPPING DEFEND ANIMATION for NPC '{}' (ID: {}): {} -> {}", npc.getName(), npc.getId(), anim, data.getDefendAnimationId());
			npc.setAnimation(data.getDefendAnimationId());
		}
		// Match modern attack animations
		else if (data.getAttackAnimationId() != -1 && (
			anim == 5485 || anim == 5486 || anim == 5487 || anim == 5507 || anim == 5510 || anim == 8002 || anim == 309
			|| anim == 240 || anim == 169 || anim == 5385
			|| anim == 6184 || anim == 6185 || anim == 6186 || anim == 6188 || anim == 6190 || anim == 6191 // Goblin attack
			|| (data.getCategory() == RetroNpcCategory.ZOMBIES && (
				anim == 5568 || anim == 5571 || anim == 5573 || anim == 5576 || anim == 5577 || anim == 5578 || anim == 5581 || anim == 5583 || anim == 5585 || anim == 5590 || anim == 5593 || anim == 422 || anim == 423 || anim == 412 || anim == 299
			))
			|| anim == 4652 || anim == 4658 || anim == 4666 || anim == 4672 || anim == 7002 || anim == 7003 // Giant attack
			|| anim == 5387 // Chicken attack
			|| anim == 422 || anim == 423 || anim == 451 || anim == 7041 || anim == 7042 // Guard attack
		))
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

		int[] retroModels = data.getRetroModelIds();
		// Swap models for Guards (exact name "Guard") using classic guard models (from NPC 3269 / 3010), excluding ranged guards (3274, 3273)
		if (data.getCategory() == RetroNpcCategory.GUARDS && (retroModels == null || retroModels.length == 0))
		{
			if (npcId != 3274 && npcId != 3273 && npc.getName() != null && npc.getName().equalsIgnoreCase("Guard"))
			{
				NPCComposition classicGuard = client.getNpcDefinition(3269);
				if (classicGuard == null || classicGuard.getModels() == null)
				{
					classicGuard = client.getNpcDefinition(3010);
				}
				if (classicGuard == null || classicGuard.getModels() == null)
				{
					classicGuard = client.getNpcDefinition(3270);
				}
				if (classicGuard != null && classicGuard.getModels() != null)
				{
					retroModels = classicGuard.getModels().clone();
				}
			}
		}

		// Swap model IDs array on NPCComposition
		if (compModels != null && retroModels != null && retroModels.length > 0)
		{
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

			if (compModels.length >= retroModels.length)
			{
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
			else
			{
				setCompModels(comp, retroModels.clone());
			}
		}

		// Apply scale overrides if specified
		if (data.getWidthScale() != -1)
		{
			if (!originalWidthScales.containsKey(compId))
			{
				originalWidthScales.put(compId, comp.getWidthScale());
			}
			if (!originalWidthScales.containsKey(npcId))
			{
				originalWidthScales.put(npcId, comp.getWidthScale());
			}
			setCompWidthScale(comp, data.getWidthScale());
		}

		if (data.getHeightScale() != -1)
		{
			if (!originalHeightScales.containsKey(compId))
			{
				originalHeightScales.put(compId, comp.getHeightScale());
			}
			if (!originalHeightScales.containsKey(npcId))
			{
				originalHeightScales.put(npcId, comp.getHeightScale());
			}
			setCompHeightScale(comp, data.getHeightScale());
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
				if (compModels.length == origModels.length)
				{
					for (int i = 0; i < compModels.length; i++)
					{
						compModels[i] = origModels[i];
					}
				}
				else
				{
					setCompModels(comp, origModels.clone());
				}
			}

			Integer origWidth = originalWidthScales.remove(compId);
			if (origWidth == null)
			{
				origWidth = originalWidthScales.remove(npcId);
			}
			if (origWidth != null && comp != null)
			{
				setCompWidthScale(comp, origWidth);
			}

			Integer origHeight = originalHeightScales.remove(compId);
			if (origHeight == null)
			{
				origHeight = originalHeightScales.remove(npcId);
			}
			if (origHeight != null && comp != null)
			{
				setCompHeightScale(comp, origHeight);
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
			case GOBLINS:
				return config.swapGoblins();
			case GUARDS:
				return config.swapGuards();
			case IMPS:
				return config.swapImps();
			case SKELETONS:
				return config.swapSkeletons();
			case ZOMBIES:
				return config.swapZombies();
			case GHOSTS:
				return config.swapGhosts();
			case GIANTS:
				return config.swapGiants();
			case CHICKENS:
				return config.swapChickens();
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
				if (compModels.length == origModels.length)
				{
					for (int i = 0; i < compModels.length; i++)
					{
						compModels[i] = origModels[i];
					}
				}
				else
				{
					setCompModels(comp, origModels.clone());
				}
			}
		}

		for (Map.Entry<Integer, Integer> entry : originalWidthScales.entrySet())
		{
			NPCComposition comp = client.getNpcDefinition(entry.getKey());
			if (comp != null)
			{
				setCompWidthScale(comp, entry.getValue());
			}
		}

		for (Map.Entry<Integer, Integer> entry : originalHeightScales.entrySet())
		{
			NPCComposition comp = client.getNpcDefinition(entry.getKey());
			if (comp != null)
			{
				setCompHeightScale(comp, entry.getValue());
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
		originalWidthScales.clear();
		originalHeightScales.clear();

		resetNpcModelCache();
	}

	private void setCompModels(NPCComposition comp, int[] newModels)
	{
		if (comp == null || newModels == null)
		{
			return;
		}

		try
		{
			for (Field field : comp.getClass().getDeclaredFields())
			{
				if (!Modifier.isStatic(field.getModifiers()) && field.getType() == int[].class)
				{
					field.setAccessible(true);
					int[] arr = (int[]) field.get(comp);
					if (arr == comp.getModels())
					{
						field.set(comp, newModels);
						break;
					}
				}
			}
		}
		catch (Throwable t)
		{
			log.debug("Failed setting models array on NPCComposition: {}", t.getMessage());
		}
	}

	private void setCompWidthScale(NPCComposition comp, int widthScale)
	{
		if (comp == null)
		{
			return;
		}

		try
		{
			for (Field field : comp.getClass().getDeclaredFields())
			{
				if (!Modifier.isStatic(field.getModifiers()) && field.getType() == int.class)
				{
					field.setAccessible(true);
					int oldVal = field.getInt(comp);
					field.setInt(comp, 99999);
					if (comp.getWidthScale() == 99999)
					{
						field.setInt(comp, widthScale);
						break;
					}
					field.setInt(comp, oldVal);
				}
			}
		}
		catch (Throwable t)
		{
			log.debug("Failed setting widthScale on NPCComposition: {}", t.getMessage());
		}
	}

	private void setCompHeightScale(NPCComposition comp, int heightScale)
	{
		if (comp == null)
		{
			return;
		}

		try
		{
			for (Field field : comp.getClass().getDeclaredFields())
			{
				if (!Modifier.isStatic(field.getModifiers()) && field.getType() == int.class)
				{
					field.setAccessible(true);
					int oldVal = field.getInt(comp);
					field.setInt(comp, 99999);
					if (comp.getHeightScale() == 99999)
					{
						field.setInt(comp, heightScale);
						break;
					}
					field.setInt(comp, oldVal);
				}
			}
		}
		catch (Throwable t)
		{
			log.debug("Failed setting heightScale on NPCComposition: {}", t.getMessage());
		}
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
