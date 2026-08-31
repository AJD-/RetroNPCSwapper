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
import java.util.List;
import java.util.Map;
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
import net.runelite.api.events.GameTick;
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
	private Gson gson;

	// Original pose/movement animations per swapped NPC, keyed by NPC index
	private final Map<Integer, OriginalNpcState> originalNpcState = new HashMap<>();

	// Original composition model IDs, keyed by composition id
	private final Map<Integer, int[]> originalCompositionModels = new HashMap<>();

	private boolean pendingModelCacheReset;

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

		// Refresh active NPC visual overrides when configuration options change
		clientThread.invoke(this::recheckLoadedNpcs);
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		processNpc(event.getNpc());
		pendingModelCacheReset = true;
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		processNpc(event.getNpc());
		pendingModelCacheReset = true;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		// Coalesce cache resets requested by spawn/change events into at most one per tick
		if (pendingModelCacheReset)
		{
			pendingModelCacheReset = false;
			resetNpcModelCache();
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		if (npc != null)
		{
			// Only drop per-NPC bookkeeping. The composition model swap is NOT
			// reverted here - compositions are shared by every NPC of that id,
			// so reverting on one despawn would visually break live instances.
			// Composition models are restored in resetNpc (toggle-off/safety)
			// and resetAllModifiedNpcs (shutdown).
			originalNpcState.remove(npc.getIndex());
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
			originalNpcState.clear();
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
		int compId = comp.getId();

		// Save original animation states before overriding
		originalNpcState.computeIfAbsent(npcIdx, idx -> OriginalNpcState.capture(npc));

		int[] compModels = comp.getModels();
		log.debug("INTERCEPTED NPC: name='{}', id={}, compId={}, index={}, category={}, origModels={}",
			npc.getName(), npc.getId(), compId, npcIdx, data.getCategory(), Arrays.toString(compModels));

		int[] retroModels = data.getRetroModelIds();

		// Swap model IDs array on NPCComposition
		if (compModels != null && retroModels != null && retroModels.length > 0)
		{
			// Key by composition id only; the composition may already hold swapped
			// models when another NPC of the same type was processed first
			originalCompositionModels.putIfAbsent(compId, compModels.clone());

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
		OriginalNpcState state = originalNpcState.remove(npc.getIndex());
		if (state == null)
		{
			return;
		}

		log.debug("Resetting NPC visuals for: {} (ID: {})", npc.getName(), npc.getId());

		NPCComposition comp = npc.getTransformedComposition();
		if (comp == null)
		{
			comp = npc.getComposition();
		}
		if (comp == null)
		{
			comp = client.getNpcDefinition(npc.getId());
		}

		if (comp != null && comp.getModels() != null)
		{
			int[] origModels = originalCompositionModels.get(comp.getId());
			if (origModels != null)
			{
				int[] compModels = comp.getModels();
				System.arraycopy(origModels, 0, compModels, 0, Math.min(origModels.length, compModels.length));
			}
		}

		state.restore(npc);
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
			case CHICKENS:
				return config.swapChickens();
			case GOBLINS:
				return config.swapGoblins();
			case SKELETONS:
				return config.swapSkeletons();
			case ZOMBIES:
				return config.swapZombies();
			case HILL_GIANTS:
				return config.swapHillGiants();
			default:
				// All other categories are disabled: their 2005 model IDs no longer
				// resolve in the modern cache (see the git history for details)
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

		pendingModelCacheReset = false;
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
					if (npc != null && originalNpcState.containsKey(npc.getIndex()))
					{
						resetNpc(npc);
					}
				}
			}
		}

		// Also restore all cached NPC compositions that were modified, ensuring no stale models remain
		for (Map.Entry<Integer, int[]> entry : originalCompositionModels.entrySet())
		{
			int[] origModels = entry.getValue();
			NPCComposition comp = client.getNpcDefinition(entry.getKey());
			if (comp != null && comp.getModels() != null && origModels != null)
			{
				int[] compModels = comp.getModels();
				System.arraycopy(origModels, 0, compModels, 0, Math.min(origModels.length, compModels.length));
			}
		}

		originalNpcState.clear();
		originalCompositionModels.clear();

		pendingModelCacheReset = false;
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
