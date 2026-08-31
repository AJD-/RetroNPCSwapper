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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import net.runelite.api.gameval.VarbitID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.NodeCache;
import net.runelite.api.WorldType;
import net.runelite.api.WorldView;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WorldChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

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

	@Inject
	private ConfigManager configManager;

	@Inject
	private Gson gson;

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
		loadMappings();
		clientThread.invoke(this::recheckLoadedNpcs);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Retro NPC Swapper stopped");
		clientThread.invoke(this::resetAllModifiedNpcs);
	}

	private void loadMappings() throws IOException
	{
		try (InputStream in = RetroNpcSwapperPlugin.class.getResourceAsStream("npc-mappings.json"))
		{
			// The resource is bundled with the plugin, so a miss is a build defect;
			// fail startup loudly rather than silently swapping nothing.
			if (in == null)
			{
				throw new IOException("npc-mappings.json resource is missing");
			}

			List<RetroNpcMappingEntry> entries = gson.fromJson(
				new InputStreamReader(in, StandardCharsets.UTF_8),
				new TypeToken<List<RetroNpcMappingEntry>>() {}.getType());
			if (entries == null || entries.isEmpty())
			{
				throw new IOException("npc-mappings.json resource is empty or malformed");
			}

			RetroNpcMapping.load(entries);
			log.info("Loaded {} retro NPC mappings", entries.size());
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

		if ("swapGhosts".equals(event.getKey()) && config.swapGhosts())
		{
			configManager.setConfiguration("retronpcswapper", "swapGhosts", false);
		}
		else if ("swapGuards".equals(event.getKey()) && config.swapGuards())
		{
			configManager.setConfiguration("retronpcswapper", "swapGuards", false);
		}
		else if ("swapImps".equals(event.getKey()) && config.swapImps())
		{
			configManager.setConfiguration("retronpcswapper", "swapImps", false);
		}
		else if ("swapLesserDemons".equals(event.getKey()) && config.swapLesserDemons())
		{
			configManager.setConfiguration("retronpcswapper", "swapLesserDemons", false);
		}
		else if ("swapGreaterDemons".equals(event.getKey()) && config.swapGreaterDemons())
		{
			configManager.setConfiguration("retronpcswapper", "swapGreaterDemons", false);
		}
		else if ("swapBlackDemons".equals(event.getKey()) && config.swapBlackDemons())
		{
			configManager.setConfiguration("retronpcswapper", "swapBlackDemons", false);
		}
		else if ("swapAdultDragons".equals(event.getKey()) && config.swapAdultDragons())
		{
			configManager.setConfiguration("retronpcswapper", "swapAdultDragons", false);
		}
		else if ("swapBabyDragons".equals(event.getKey()) && config.swapBabyDragons())
		{
			configManager.setConfiguration("retronpcswapper", "swapBabyDragons", false);
		}
		else if ("swapMossGiants".equals(event.getKey()) && config.swapMossGiants())
		{
			configManager.setConfiguration("retronpcswapper", "swapMossGiants", false);
		}
		else if ("swapFireGiants".equals(event.getKey()) && config.swapFireGiants())
		{
			configManager.setConfiguration("retronpcswapper", "swapFireGiants", false);
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
		if (isSafetyDisabled())
		{
			return;
		}

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

		// Match modern combat animations using Category-Scoped Data Sets
		if (data.isDeathAnimation(anim))
		{
			log.debug("SWAPPING DEATH ANIMATION for NPC '{}' (ID: {}): {} -> {}", npc.getName(), npc.getId(), anim, data.getDeathAnimationId());
			npc.setAnimation(data.getDeathAnimationId());
		}
		else if (data.isDefendAnimation(anim))
		{
			log.debug("SWAPPING DEFEND ANIMATION for NPC '{}' (ID: {}): {} -> {}", npc.getName(), npc.getId(), anim, data.getDefendAnimationId());
			npc.setAnimation(data.getDefendAnimationId());
		}
		else if (data.isAttackAnimation(anim))
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
		else if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invoke(this::recheckLoadedNpcs);
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() == VarbitID.INSIDE_WILDERNESS)
		{
			clientThread.invoke(this::recheckLoadedNpcs);
		}
	}

	@Subscribe
	public void onWorldChanged(WorldChanged event)
	{
		clientThread.invoke(this::recheckLoadedNpcs);
	}

	/**
	 * Checks if retro NPC swapping should be disabled due to safety settings
	 * (e.g. on a PvP world or inside the Wilderness).
	 */
	private boolean isSafetyDisabled()
	{
		if (config.disablePvpWorld() && client.getWorldType() != null && WorldType.isPvpWorld(client.getWorldType()))
		{
			return true;
		}
		if (config.disableWilderness() && client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) == 1)
		{
			return true;
		}
		return false;
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

		if (isSafetyDisabled())
		{
			resetNpc(npc);
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
				System.arraycopy(origModels, 0, compModels, 0, Math.min(origModels.length, compModels.length));
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
			case LESSER_DEMONS:
				return false;
			case GREATER_DEMONS:
				return false;
			case BLACK_DEMONS:
				return false;
			case ADULT_DRAGONS:
				return false;
			case BABY_DRAGONS:
				return false;
			case GOBLINS:
				return config.swapGoblins();
			case GUARDS:
				return false;
			case IMPS:
				return false;
			case SKELETONS:
				return config.swapSkeletons();
			case ZOMBIES:
				return config.swapZombies();
			case GHOSTS:
				return false;
			case HILL_GIANTS:
				return config.swapHillGiants();
			case MOSS_GIANTS:
				return false;
			case FIRE_GIANTS:
				return false;
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

		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return;
		}

		for (NPC npc : worldView.npcs())
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
			WorldView worldView = client.getTopLevelWorldView();
			if (worldView != null)
			{
				for (NPC npc : worldView.npcs())
				{
					if (npc != null && modifiedNpcIndexes.contains(npc.getIndex()))
					{
						resetNpc(npc);
					}
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
				System.arraycopy(origModels, 0, compModels, 0, Math.min(origModels.length, compModels.length));
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
