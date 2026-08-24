package com.tarkovsearch.client;

import com.tarkovsearch.network.ChestRevealPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class TarkovSearchClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ChestRevealPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        RevealManager.startReveal(payload.pos(), payload.slots())
                )
        );
    }
}
