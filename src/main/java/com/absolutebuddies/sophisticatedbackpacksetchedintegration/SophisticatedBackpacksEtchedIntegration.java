package com.absolutebuddies.sophisticatedbackpacksetchedintegration;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.DiscHandlerRegistry;

@Mod("sophisticatedbackpacksetchedintegration")
public class SophisticatedBackpacksEtchedIntegration {

    public SophisticatedBackpacksEtchedIntegration(IEventBus modEventBus) {
        System.err.println("[SBEI] Mod constructor called!");
        modEventBus.addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event) {
        System.err.println("[SBEI] Setup method called!");
        event.enqueueWork(() -> {
            System.err.println("[SBEI] Registering EtchedDiscHandler!");
            DiscHandlerRegistry.registerHandler(new EtchedDiscHandler());
        });
    }
}
