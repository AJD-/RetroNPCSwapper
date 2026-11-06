package com.retronpcswapper;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.retronpcswapper.chathead.ChatheadOverlay;
import com.retronpcswapper.chathead.ChatheadSwapper;
import com.retronpcswapper.compatibility.InteractHighlightCompat;
import com.retronpcswapper.compatibility.InteractTargetTracker;
import com.retronpcswapper.compatibility.ModelSwapProtocol;
import com.retronpcswapper.compatibility.PartnerClaims;
import com.retronpcswapper.compatibility.PartnerOutlines;
import com.retronpcswapper.compatibility.RetroInteractHighlightOverlay;
import com.retronpcswapper.compatibility.RetroNpcOutliner;
import com.retronpcswapper.compatibility.RetroSceneryOutliner;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.NPC;
import net.runelite.api.RuneLiteObjectController;
import net.runelite.api.TileObject;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Exercises the actual published callbacks across requests, live state changes and shutdown. */
public class ModelCaptureProvidersTest
{
	private final Client client = mock(Client.class);
	private final ClientThread clientThread = mock(ClientThread.class);
	private final RetroNpcConfig config = mock(RetroNpcConfig.class);
	private final ConfigManager configManager = mock(ConfigManager.class);
	private final PluginManager pluginManager = mock(PluginManager.class);
	private final RetroModelCache modelCache = mock(RetroModelCache.class);
	private final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
	private final OverlayManager overlayManager = mock(OverlayManager.class);
	private final InteractHighlightCompat interactHighlight = mock(InteractHighlightCompat.class);
	private final RetroInteractHighlightOverlay interactHighlightOverlay = mock(RetroInteractHighlightOverlay.class);
	private final InteractTargetTracker targetTracker = mock(InteractTargetTracker.class);
	private final RetroNpcOutliner outliner = mock(RetroNpcOutliner.class);
	private final EventBus eventBus = mock(EventBus.class);
	private final PartnerOutlines partnerOutlines = mock(PartnerOutlines.class);
	private final RetroSceneryOutliner sceneryOutliner = mock(RetroSceneryOutliner.class);
	private final RetroShieldSwapper shieldSwapper = mock(RetroShieldSwapper.class);
	private final RetroScenerySwapper scenerySwapper = mock(RetroScenerySwapper.class);
	private final RenderCallbackManager renderCallbackManager = mock(RenderCallbackManager.class);
	private final ChatheadSwapper chatheadSwapper = mock(ChatheadSwapper.class);
	private final ChatheadOverlay chatheadOverlay = mock(ChatheadOverlay.class);
	private final PartnerClaims partnerClaims = mock(PartnerClaims.class);
	private final RetroNpcSwapperPlugin plugin = new RetroNpcSwapperPlugin();
	private final List<PluginMessage> messages = new ArrayList<>();
	private final Queue<Runnable> clientTasks = new ArrayDeque<>();

	@Before
	public void setUp() throws Exception
	{
		Guice.createInjector(binder ->
		{
			binder.bind(Client.class).toInstance(client);
			binder.bind(ClientThread.class).toInstance(clientThread);
			binder.bind(RetroNpcConfig.class).toInstance(config);
			binder.bind(ConfigManager.class).toInstance(configManager);
			binder.bind(Gson.class).toInstance(RuneLiteAPI.GSON);
			binder.bind(PluginManager.class).toInstance(pluginManager);
			binder.bind(RetroModelCache.class).toInstance(modelCache);
			binder.bind(ScheduledExecutorService.class).toInstance(executor);
			binder.bind(OverlayManager.class).toInstance(overlayManager);
			binder.bind(InteractHighlightCompat.class).toInstance(interactHighlight);
			binder.bind(RetroInteractHighlightOverlay.class).toInstance(interactHighlightOverlay);
			binder.bind(InteractTargetTracker.class).toInstance(targetTracker);
			binder.bind(RetroNpcOutliner.class).toInstance(outliner);
			binder.bind(EventBus.class).toInstance(eventBus);
			binder.bind(PartnerOutlines.class).toInstance(partnerOutlines);
			binder.bind(RetroSceneryOutliner.class).toInstance(sceneryOutliner);
			binder.bind(RetroShieldSwapper.class).toInstance(shieldSwapper);
			binder.bind(RetroScenerySwapper.class).toInstance(scenerySwapper);
			binder.bind(RenderCallbackManager.class).toInstance(renderCallbackManager);
			binder.bind(ChatheadSwapper.class).toInstance(chatheadSwapper);
			binder.bind(ChatheadOverlay.class).toInstance(chatheadOverlay);
			binder.bind(PartnerClaims.class).toInstance(partnerClaims);
		}).injectMembers(plugin);
		doAnswer(call ->
		{
			clientTasks.add(call.getArgument(0));
			return null;
		})
			.when(clientThread).invoke(any(Runnable.class));
		doAnswer(call ->
		{
			messages.add(call.getArgument(0));
			return null;
		})
			.when(eventBus).post(any(PluginMessage.class));
		plugin.startUp();
		// Run the real startup refresh with no renderer attached.
		drainClientTasks();
	}

	private void drainClientTasks()
	{
		while (!clientTasks.isEmpty())
		{
			clientTasks.remove().run();
		}
	}

	private PluginMessage latestModels()
	{
		return messages.stream().filter(message -> ModelSwapProtocol.MODELS.equals(message.getName()))
			.reduce((first, second) -> second).orElseThrow(AssertionError::new);
	}

	private void requestModels()
	{
		plugin.onPluginMessage(new PluginMessage(ModelSwapProtocol.NAMESPACE, ModelSwapProtocol.MODELS_SYN,
			Map.of("version", 1, "source", ModelSwapProtocol.SOURCE_2DSCAPE)));
		drainClientTasks();
	}

	@Test
	public void testRequestsPublishOnTheClientThreadWithoutOutlineRefreshNoise()
	{
		messages.clear();
		plugin.onPluginMessage(new PluginMessage(ModelSwapProtocol.NAMESPACE, ModelSwapProtocol.MODELS_SYN,
			Map.of("version", 1, "source", ModelSwapProtocol.SOURCE_2DSCAPE)));
		assertTrue(messages.isEmpty());
		assertEquals(1, clientTasks.size());
		drainClientTasks();
		assertEquals(1, messages.size());
		assertEquals(ModelSwapProtocol.MODELS, messages.get(0).getName());

		messages.clear();
		plugin.onPluginMessage(ModelSwapProtocol.synMessage(ModelSwapProtocol.SOURCE_CUSTOM_NPC_MODELS));
		drainClientTasks();
		assertEquals(1, messages.size());
		assertEquals(ModelSwapProtocol.OUTLINES, messages.get(0).getName());

		messages.clear();
		plugin.onPluginMessage(new PluginMessage(ModelSwapProtocol.NAMESPACE, ModelSwapProtocol.MODELS_SYN,
			Map.of("version", 2, "source", ModelSwapProtocol.SOURCE_2DSCAPE)));
		assertTrue(clientTasks.isEmpty());
		assertTrue(messages.isEmpty());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testProvidersUseLiveStateAndRejectStaleControllers() throws Exception
	{
		PluginMessage initial = latestModels();
		Function<NPC, Model> npcModel = (Function<NPC, Model>) initial.getData().get("npcModel");
		Function<TileObject, List<RuneLiteObjectController>> standIns =
			(Function<TileObject, List<RuneLiteObjectController>>) initial.getData().get("sceneryStandIns");
		Function<RuneLiteObjectController, Model> sceneryModel =
			(Function<RuneLiteObjectController, Model>) initial.getData().get("sceneryModel");
		NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(NpcID.MOURNING_ARIANWYN_VIS);
		Model retro = mock(Model.class);
		Model servant = mock(Model.class);
		when(modelCache.pose(npc)).thenReturn(retro);
		assertSame(retro, npcModel.apply(npc));
		when(partnerClaims.isClaimed(npc.getId())).thenReturn(true);
		assertNull(npcModel.apply(npc));
		when(partnerClaims.isClaimedBy(ModelSwapProtocol.SOURCE_CUSTOM_SERVANTS, npc.getId())).thenReturn(true);
		when(partnerOutlines.pose(ModelSwapProtocol.SOURCE_CUSTOM_SERVANTS, npc)).thenReturn(servant);
		assertSame(servant, npcModel.apply(npc));

		TileObject object = mock(TileObject.class);
		RuneLiteObjectController controller = mock(RuneLiteObjectController.class);
		List<RuneLiteObjectController> placed = new ArrayList<>(List.of(controller));
		when(scenerySwapper.getStandIns(object)).thenReturn(placed);
		List<RuneLiteObjectController> offered = standIns.apply(object);
		assertEquals(List.of(controller), offered);
		try
		{
			offered.clear();
			fail("Consumers must not mutate the swapper's stand-ins");
		}
		catch (UnsupportedOperationException expected)
		{
			assertEquals(List.of(controller), placed);
		}
		when(scenerySwapper.getDrawnModel(controller)).thenReturn(retro);
		assertNull(sceneryModel.apply(controller));
		verify(scenerySwapper, never()).getDrawnModel(controller);
		when(client.isRuneLiteObjectRegistered(controller)).thenReturn(true);
		assertSame(retro, sceneryModel.apply(controller));
		when(client.isRuneLiteObjectRegistered(controller)).thenReturn(false);
		assertNull(sceneryModel.apply(controller));
		placed.clear();
		assertTrue(standIns.apply(object).isEmpty());

		requestModels();
		assertSame(npcModel, latestModels().getData().get("npcModel"));
		assertSame(standIns, latestModels().getData().get("sceneryStandIns"));
		assertSame(sceneryModel, latestModels().getData().get("sceneryModel"));

		plugin.shutDown();
		// Old callbacks stop answering before client-thread withdrawal is dispatched.
		when(client.isRuneLiteObjectRegistered(controller)).thenReturn(true);
		assertNull(npcModel.apply(npc));
		assertTrue(standIns.apply(object).isEmpty());
		assertNull(sceneryModel.apply(controller));
		assertSame(npcModel, latestModels().getData().get("npcModel"));
		drainClientTasks();
		assertEquals(Map.of("version", 1, "source", ModelSwapProtocol.SOURCE_RETRO_NPC_SWAPPER), latestModels().getData());
	}
}
