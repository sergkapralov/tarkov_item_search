package com.tarkovsearch.client;

import com.tarkovsearch.network.ChestRevealPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class TarkovSearchClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ChestRevealPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    int[] slots = payload.slots().stream().mapToInt(Integer::intValue).toArray();
                    RevealManager.startReveal(payload.pos(), slots);
                })
        );
    }
}
