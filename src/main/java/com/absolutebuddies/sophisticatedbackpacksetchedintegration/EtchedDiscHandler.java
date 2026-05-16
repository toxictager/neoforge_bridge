package com.absolutebuddies.sophisticatedbackpacksetchedintegration;

import gg.moonflower.etched.common.network.play.ClientboundPlayBlockMusicPacket;
import gg.moonflower.etched.common.network.play.ClientboundPlayEntityMusicPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongs;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.api.IDiscHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.ServerStorageSoundHandler;

import java.util.Optional;
import java.util.UUID;

public class EtchedDiscHandler implements IDiscHandler<Holder<JukeboxSong>> {

    @Override
    public Optional<Holder<JukeboxSong>> getSongInfo(ItemStack stack, Level level) {
        Optional<Holder<JukeboxSong>> song = JukeboxSong.fromStack(level.registryAccess(), stack);
        if (song.isEmpty() && supports(stack)) {
            System.err.println("[SBEI] getSongInfo: Etched disc detected, providing fallback song");
            return level.registryAccess().registryOrThrow(Registries.JUKEBOX_SONG)
                    .getHolder(JukeboxSongs.THIRTEEN)
                    .map(ref -> (Holder<JukeboxSong>) ref);
        }
        return song;
    }

    @Override
    public void playDisc(ServerLevel level, BlockPos pos, UUID storageUuid, ItemStack stack, Runnable onFinished) {
        System.err.println("[SBEI] playDisc block for " + stack);
        SophisticatedBackpacksEtchedIntegrationDataBase.ETCHED_STREAMS_CACHE.put(storageUuid, EtchedStreamInfo.forBlock(pos));
        SophisticatedBackpacksEtchedIntegrationDataBase.DISC_DURATION = getLengthInTicks(stack);
        System.err.println("[SBEI] Duration: " + SophisticatedBackpacksEtchedIntegrationDataBase.DISC_DURATION);

        // Send Etched packet to clients
        PacketDistributor.sendToPlayersNear(level, null, pos.getX(), pos.getY(), pos.getZ(), 64, 
                new ClientboundPlayBlockMusicPacket(stack.copy(), pos));

        // Start Sophisticated Core playback (needed to keep the upgrade state active)
        getSongInfo(stack, level).ifPresent(song ->
            ServerStorageSoundHandler.startPlayingDisc(level, pos, storageUuid, song, onFinished)
        );
    }

    @Override
    public void playDisc(ServerLevel level, Vec3 pos, UUID storageUuid, ItemStack stack, int entityId, Runnable onFinished) {
        System.err.println("[SBEI] playDisc entity for " + stack);
        SophisticatedBackpacksEtchedIntegrationDataBase.ETCHED_STREAMS_CACHE.put(storageUuid, EtchedStreamInfo.forEntity(entityId));
        SophisticatedBackpacksEtchedIntegrationDataBase.DISC_DURATION = getLengthInTicks(stack);
        System.err.println("[SBEI] Duration: " + SophisticatedBackpacksEtchedIntegrationDataBase.DISC_DURATION);

        Entity entity = level.getEntity(entityId);
        if (entity != null) {
            PacketDistributor.sendToPlayersTrackingEntity(entity, new ClientboundPlayEntityMusicPacket(stack.copy(), entity, false));
        }

        getSongInfo(stack, level).ifPresent(song ->
            ServerStorageSoundHandler.startPlayingDisc(level, pos, storageUuid, entityId, song, onFinished)
        );
    }

    @Override
    public Optional<Integer> getMusicLengthInTicks(ItemStack stack, Level level) {
        int length = getLengthInTicks(stack);
        System.err.println("[SBEI] getMusicLengthInTicks for " + stack + ": " + length);
        return Optional.of(length);
    }

    @Override
    public boolean supports(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        // Try direct component check
        boolean hasComponent = stack.has(gg.moonflower.etched.core.registry.EtchedComponents.MUSIC.get());
        
        // Fallback: check by string ID in case of registry mismatch
        if (!hasComponent) {
            hasComponent = stack.getComponents().stream()
                .anyMatch(c -> c.type().toString().contains("etched:music"));
        }

        if (stack.getItem().toString().contains("etched")) {
             System.err.println("[SBEI] supports check for " + stack + ": " + hasComponent);
        }
        return hasComponent;
    }

    @Override
    public Optional<ItemStack> getRandomDisc(RandomSource randomSource) {
        return Optional.empty();
    }

    @Override
    public int getMusicDiscSize() {
        return 0;
    }

    public int getLengthInTicks(ItemStack stack) {
        // Log components using System.err to ensure they appear in console
        System.err.println("[SBEI] Components for " + stack.getItem() + ": " + stack.getComponents());

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            int duration = customData.copyTag().getCompound("Music").getInt("Duration");
            if (duration > 0) return duration;
        }

        // Fallback for Etched discs if duration is not found
        return 2400; // 120 seconds default
    }
}
