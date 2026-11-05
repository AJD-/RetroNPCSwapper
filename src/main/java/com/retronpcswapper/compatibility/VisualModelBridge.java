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
package com.retronpcswapper.compatibility;

import java.util.*;
import java.util.function.Function;
import net.runelite.api.*;
import net.runelite.client.events.PluginMessage;

/** Optional capture consumers; independent of claims and outline ownership. */
public final class VisualModelBridge
{
    private VisualModelBridge() {}
    public static boolean isRequest(PluginMessage event)
    {
        return "npc-model-swap".equals(event.getNamespace()) && "modelsSyncReq".equals(event.getName())
            && event.getData() != null && Integer.valueOf(1).equals(event.getData().get("version"))
            && "2dscape".equals(event.getData().get("source"));
    }
    public static PluginMessage state(Function<NPC, Model> npc,
        Function<TileObject, List<RuneLiteObjectController>> scenery,
        Function<RuneLiteObjectController, Model> model)
    {
        Map<String, Object> data = new HashMap<>();
        data.put("version", 1);
        data.put("source", "retro-npc-swapper");
        if (npc != null) data.put("npcModel", npc);
        if (scenery != null) data.put("sceneryStandIns", scenery);
        if (model != null) data.put("sceneryModel", model);
        return new PluginMessage("npc-model-swap", "models", data);
    }
}
