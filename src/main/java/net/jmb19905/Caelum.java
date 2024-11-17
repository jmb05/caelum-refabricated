package net.jmb19905;

import fuzs.forgeconfigapiport.fabric.api.forge.v4.ForgeConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.jmb19905.config.ConfigHelper;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Caelum implements ModInitializer {

    public static final String MOD_ID = "caelum";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ForgeConfigRegistry.INSTANCE.register(Caelum.MOD_ID, ModConfig.Type.COMMON, ConfigHelper.commonConfig);
    }
}
