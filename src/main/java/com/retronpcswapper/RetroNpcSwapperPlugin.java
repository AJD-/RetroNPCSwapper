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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.Actor;
import net.runelite.api.Animation;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.api.NPC;
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
import net.runelite.api.hooks.DrawCallbacks;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.gpu.GpuPlugin;

@Slf4j
@PluginDescriptor(
	name = "Retro NPC Swapper",
	description = "Swaps modern NPC models and animations to their retro 2004/2005 variants from the 2005 cache.",
	tags = {"npc", "retro", "swapper", "model", "animation", "cache"}
)
public class RetroNpcSwapperPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private RetroNpcConfig config;

	@Inject
	private Gson gson;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private RetroModelCache modelCache;

	// Original pose/movement animations per swapped NPC, keyed by NPC index
	private final Map<Integer, OriginalNpcState> originalNpcState = new HashMap<>();

	// NPC ids currently eligible for model substitution. Maintained by processNpc so the
	// render path never has to evaluate mappings, config toggles or safety settings.
	private final Set<Integer> substitutedNpcIds = new HashSet<>();

	// Our decorator, while it owns the client's draw callbacks slot
	private RetroDrawCallbacks wrapper;

	// Resolved once - the plugin list does not change identity, and attach() is polled per tick
	private Plugin gpuPlugin;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Retro NPC Swapper started");
		loadMappings();
		clientThread.invoke(() ->
		{
			recheckLoadedNpcs();
			attach();
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Retro NPC Swapper stopped");
		clientThread.invoke(() ->
		{
			detach();
			resetAllModifiedNpcs();
			modelCache.clear();
		});
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
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		// A transform gives the NPC a different id, which the spawn-time build never saw.
		// Without reprocessing here the substitution silently stops for that NPC.
		processNpc(event.getNpc());
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		// Cheap guard: the GPU plugin sets and clears the draw callbacks slot unconditionally,
		// so re-take it whenever we have lost it. Covers orderings PluginChanged misses.
		if (wrapper == null || client.getDrawCallbacks() != wrapper)
		{
			attach();
		}
	}

	@Subscribe
	public void onPluginChanged(PluginChanged event)
	{
		if (event.getPlugin() instanceof GpuPlugin)
		{
			// attach() declines on its own when the GPU plugin is no longer holding the slot
			clientThread.invoke(this::attach);
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		if (npc != null)
		{
			// Only per-NPC bookkeeping is dropped. substitutedNpcIds is keyed by NPC id, not
			// index, and is shared by every instance of that type, so it is left alone here.
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

		RetroNpcData data = RetroNpcMapping.get(npc.getId(), npc.getName());

		// Animation overrides are gated on substitution being live. Without the wrapper the
		// model stays vanilla, and a modern-rigged model playing a 2005 sequence - which is
		// keyed to 2005 framemaps - renders distorted rather than retro.
		//
		// Verify if category toggle is enabled in configuration
		if (wrapper == null || isSafetyDisabled() || data == null || !isCategoryEnabled(data.getCategory()))
		{
			substitutedNpcIds.remove(npc.getId());
			resetNpc(npc);
			return;
		}

		// Build the replacement geometry here, on the client thread, so the draw callback
		// only ever does a map lookup
		modelCache.ensureBuilt(npc.getId(), data);
		substitutedNpcIds.add(npc.getId());
		applyRetroSwap(npc, data);
	}

	/**
	 * Applies retro animation overrides to the given NPC.
	 *
	 * <p>Geometry is substituted per NPC at draw time by {@link RetroDrawCallbacks}
	 */
	private void applyRetroSwap(NPC npc, RetroNpcData data)
	{
		// Save original animation states before overriding
		originalNpcState.computeIfAbsent(npc.getIndex(), idx -> OriginalNpcState.capture(npc));

		log.debug("INTERCEPTED NPC: name='{}', id={}, index={}, category={}",
			npc.getName(), npc.getId(), npc.getIndex(), data.getCategory());

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

		// Only animations need restoring - the composition was never modified, and dropping the
		// NPC id from substitutedNpcIds is what reverts its models on the next frame drawn.
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

		originalNpcState.clear();
		substitutedNpcIds.clear();
	}

	/**
	 * Takes over the client's draw callbacks slot by wrapping whatever the GPU plugin registered.
	 *
	 * <p>Declines when the GPU plugin is not holding the slot - either it is disabled, or another
	 * renderer such as 117HD owns it. Must be called on the client thread.
	 */
	private void attach()
	{
		DrawCallbacks current = client.getDrawCallbacks();
		if (wrapper != null && current == wrapper)
		{
			return;
		}

		// We are not the registered callbacks anymore; drop the stale reference before
		// deciding whether we can retake the slot
		boolean wasAttached = wrapper != null;
		wrapper = null;

		Plugin gpu = findGpuPlugin();
		if (gpu != null && current == gpu)
		{
			RetroDrawCallbacks callbacks = new RetroDrawCallbacks((DrawCallbacks) gpu, this::substitute);
			client.setDrawCallbacks(callbacks);
			wrapper = callbacks;
			log.debug("Attached retro draw callbacks over the GPU plugin");
		}

		// Only on a real transition - the per-tick guard calls this repeatedly while detached,
		// and re-processing the whole scene every tick would be wasteful
		if (wasAttached == (wrapper == null))
		{
			recheckLoadedNpcs();
		}
	}

	/**
	 * Hands the draw callbacks slot back to the GPU plugin. Must be called on the client thread.
	 */
	private void detach()
	{
		if (wrapper != null && client.getDrawCallbacks() == wrapper)
		{
			// Restore the delegate, never null - nulling the slot would leave the GPU plugin
			// running with no callbacks registered
			client.setDrawCallbacks(wrapper.getDelegate());
			log.debug("Detached retro draw callbacks");
		}
		wrapper = null;
	}

	private Plugin findGpuPlugin()
	{
		if (gpuPlugin == null)
		{
			for (Plugin plugin : pluginManager.getPlugins())
			{
				if (plugin instanceof GpuPlugin)
				{
					gpuPlugin = plugin;
					break;
				}
			}
		}
		return gpuPlugin;
	}

	/**
	 * Supplies retro geometry for an NPC being drawn, or null to let the vanilla model through.
	 *
	 * <p>Runs per NPC per frame, so it does map lookups only - eligibility is decided in
	 * {@link #processNpc} and the geometry is built by {@link RetroModelCache} ahead of time.
	 */
	private Model substitute(NPC npc, Model vanilla)
	{
		int npcId = npc.getId();
		if (!substitutedNpcIds.contains(npcId))
		{
			return null;
		}

		Model base = modelCache.get(npcId);
		if (base == null)
		{
			return null;
		}

		Animation action = modelCache.animation(npc.getAnimation());
		Animation pose = modelCache.animation(npc.getPoseAnimation());

		// The returned model is shared and is invalidated by the next applyTransformations call,
		// including the client's own - it is handed straight to the delegate and uploaded before
		// anything else can run, which is what makes that safe here.
		return client.applyTransformations(base, action, npc.getAnimationFrame(), pose, npc.getPoseAnimationFrame());
	}
}
