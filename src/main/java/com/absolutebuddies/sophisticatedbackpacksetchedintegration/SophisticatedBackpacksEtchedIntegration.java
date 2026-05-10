package com.absolutebuddies.sophisticatedbackpacksetchedintegration;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.DiscHandlerRegistry;

@Mod("sophisticatedbackpacksetchedintegration")
public class SophisticatedBackpacksEtchedIntegration {

    public SophisticatedBackpacksEtchedIntegration(IEventBus modEventBus) {
        modEventBus.addListener(this::setup);
        NeoForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            DiscHandlerRegistry.registerHandler(new EtchedDiscHandler());
        });
    }
}
