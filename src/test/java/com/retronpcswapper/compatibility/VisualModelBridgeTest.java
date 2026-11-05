package com.retronpcswapper.compatibility;

import java.util.Map;
import java.util.function.Function;
import net.runelite.api.*;
import net.runelite.client.events.PluginMessage;
import org.junit.Test;
import static org.junit.Assert.*;

public class VisualModelBridgeTest
{
    @Test public void syncIsIndependentOfClaimsAndOutlines()
    {
        assertTrue(VisualModelBridge.isRequest(new PluginMessage("npc-model-swap", "modelsSyncReq", Map.of("version", 1, "source", "2dscape"))));
        assertFalse(VisualModelBridge.isRequest(new PluginMessage("npc-model-swap", "claims", Map.of("version", 1, "source", "2dscape"))));
        assertFalse(VisualModelBridge.isRequest(new PluginMessage("npc-model-swap", "modelsSyncReq", Map.of("version", 2, "source", "2dscape"))));
    }
    @Test public void callbacksAndWithdrawalUseOnlySharedTypes()
    {
        Function<NPC, Model> poser = npc -> null;
        assertSame(poser, VisualModelBridge.state(poser, null, null).getData().get("npcModel"));
        assertFalse(VisualModelBridge.state(null, null, null).getData().containsKey("npcModel"));
        assertEquals("retro-npc-swapper", VisualModelBridge.state(null, null, null).getData().get("source"));
    }
}
