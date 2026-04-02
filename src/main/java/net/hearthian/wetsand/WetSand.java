package net.hearthian.wetsand;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;   // ADD
import net.hearthian.wetsand.compat.BoPCompat;  // ADD
import net.hearthian.wetsand.events.Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.hearthian.wetsand.utils.initializer.initBlockItems;
import static net.hearthian.wetsand.utils.initializer.initCreativePlacement;

public class WetSand implements ModInitializer {
    public static final String MOD_ID = "wet-sand";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing...");
        initBlockItems();
        initCreativePlacement();

        if (FabricLoader.getInstance().isModLoaded("biomesoplenty")) {
            BoPCompat.init();
        }

        Events.registerDry();
    }
}