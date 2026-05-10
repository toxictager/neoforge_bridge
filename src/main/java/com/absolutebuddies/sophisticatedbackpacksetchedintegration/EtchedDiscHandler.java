package com.absolutebuddies.sophisticatedbackpacksetchedintegration;

import gg.moonflower.etched.common.network.play.ClientboundPlayBlockMusicPacket;
import gg.moonflower.etched.common.network.play.ClientboundPlayEntityMusicPacket;
import gg.moonflower.etched.core.registry.EtchedItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
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
        return JukeboxSong.fromStack(level.registryAccess(), stack);
    }

    @Override
    public void playDisc(ServerLevel level, BlockPos pos, UUID storageUuid, ItemStack stack, Runnable onFinished) {
        System.out.println("[SBEI] playDisc block!");
        SophisticatedBackpacksEtchedIntegrationDataBase.ETCHED_STREAMS_CACHE.put(storageUuid, EtchedStreamInfo.forBlock(pos));
        SophisticatedBackpacksEtchedIntegrationDataBase.DISC_DURATION = getLengthInTicks(stack);

        // NeoForge 1.21.1 packet sending
        PacketDistributor.sendToPlayersNear(level, null, pos.getX(), pos.getY(), pos.getZ(), 64, 
                new ClientboundPlayBlockMusicPacket(stack.copy(), pos));

        getSongInfo(stack, level).ifPresent(song ->
            ServerStorageSoundHandler.startPlayingDisc(level, pos, storageUuid, song, onFinished)
        );
    }

    @Override
    public void playDisc(ServerLevel level, Vec3 pos, UUID storageUuid, ItemStack stack, int entityId, Runnable onFinished) {
        System.out.println("[SBEI] playDisc entity!");
        SophisticatedBackpacksEtchedIntegrationDataBase.ETCHED_STREAMS_CACHE.put(storageUuid, EtchedStreamInfo.forEntity(entityId));
        SophisticatedBackpacksEtchedIntegrationDataBase.DISC_DURATION = getLengthInTicks(stack);

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
        return Optional.of(getLengthInTicks(stack));
    }

    @Override
    public boolean supports(ItemStack stack) {
        return stack.has(gg.moonflower.etched.core.registry.EtchedComponents.MUSIC.get());
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
        // In 1.21.1, NBT is accessed via DataComponents.CUSTOM_DATA
        // Etched stores duration in custom data tag "Music" -> "Duration"
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            return customData.copyTag().getCompound("Music").getInt("Duration");
        }
        return 0;
    }
}
