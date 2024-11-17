package net.jmb19905.client;

import fuzs.forgeconfigapiport.fabric.api.forge.v4.ForgeConfigRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.jmb19905.Caelum;
import net.jmb19905.config.ConfigHelper;
import net.neoforged.fml.config.ModConfig;

public class CaelumClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ForgeConfigRegistry.INSTANCE.register(Caelum.MOD_ID, ModConfig.Type.CLIENT, ConfigHelper.clientConfig);
	}
}