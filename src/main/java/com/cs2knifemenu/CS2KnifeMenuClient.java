package com.cs2knifemenu;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class CS2KnifeMenuClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KnifeRegistry.load();
        Config c = Config.get();
        if (KnifeRegistry.get(c.knife) == null) c.knife = KnifeRegistry.first().id;
        ClientTickEvents.END_CLIENT_TICK.register(KnifeController.I::tick);
    }
}
