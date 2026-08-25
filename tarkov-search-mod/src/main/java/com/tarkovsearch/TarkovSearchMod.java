package com.tarkovsearch;

import com.tarkovsearch.network.ChestRevealPayload;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TarkovSearchMod implements ModInitializer {

    public static final String MOD_ID = "tarkovsearch";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ChestRevealPayload.register();
        LOGGER.info("[TarkovSearch] initialized (network payload registered)");
    }
}
