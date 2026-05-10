package com.absolutebuddies.sophisticatedbackpacksetchedintegration.mixin;

import com.absolutebuddies.sophisticatedbackpacksetchedintegration.EtchedStreamInfo;
import com.absolutebuddies.sophisticatedbackpacksetchedintegration.SophisticatedBackpacksEtchedIntegrationDataBase;
import gg.moonflower.etched.common.network.play.ClientboundPlayBlockMusicPacket;
import gg.moonflower.etched.common.network.play.ClientboundPlayEntityMusicPacket;
import gg.moonflower.etched.core.registry.EtchedItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.PlayDiscPayload;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.ServerStorageSoundHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.StopDiscPlaybackPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(value = ServerStorageSoundHandler.class, remap = false)
public class ServerStorageSoundHandlerMixin {

    @Inject(
        method = "startPlayingDisc(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Ljava/util/UUID;Lnet/minecraft/core/Holder;Ljava/lang/Runnable;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void OnStartPlayingDiscBlock(
        ServerLevel serverLevel,
        BlockPos position,
        UUID storageUuid,
        Holder<JukeboxSong> song,
        Runnable onFinishedHandler,
        CallbackInfo ci
    ) {
        Vec3 pos = Vec3.atCenterOf(position);
        if (SyncActiveStreams(serverLevel, pos, storageUuid)) return;

        System.out.println("[SBEI] onStartPlayingDiscBlock!");

        // NeoForge 1.21.1: PacketDistributor
        PacketDistributor.sendToPlayersNear(serverLevel, null, pos.x, pos.y, pos.z, 128,
            new PlayDiscPayload(storageUuid, song, position)
        );

        ServerStorageSoundHandler.putSoundInfo(
            serverLevel,
            storageUuid,
            onFinishedHandler,
            pos,
            serverLevel.getGameTime() + SophisticatedBackpacksEtchedIntegrationDataBase.DISC_DURATION
        );

        System.out.println("[SBEI] Registered: " + SophisticatedBackpacksEtchedIntegrationDataBase.DISC_DURATION);
        ci.cancel();
    }

    @Inject(
        method = "startPlayingDisc(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;Ljava/util/UUID;ILnet/minecraft/core/Holder;Ljava/lang/Runnable;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void OnStartPlayingDiscEntity(
        ServerLevel serverLevel,
        Vec3 position,
        UUID storageUuid,
        int entityId,
        Holder<JukeboxSong> song,
        Runnable onFinishedHandler,
        CallbackInfo ci
    ) {
        if (SyncActiveStreams(serverLevel, position, storageUuid)) return;

        System.out.println("[SBEI] onStartPlayingDiscEntity!");

        PacketDistributor.sendToPlayersNear(serverLevel, null, position.x, position.y, position.z, 128,
            new PlayDiscPayload(storageUuid, song, entityId)
        );

        ServerStorageSoundHandler.putSoundInfo(
            serverLevel,
            storageUuid,
            onFinishedHandler,
            position,
            serverLevel.getGameTime() + SophisticatedBackpacksEtchedIntegrationDataBase.DISC_DURATION
        );

        System.out.println("[SBEI] Registered: " + SophisticatedBackpacksEtchedIntegrationDataBase.DISC_DURATION);
        ci.cancel();
    }

    @Inject(
        method = "sendStopMessage",
        at = @At("HEAD"),
        remap = false
    )
    private static void OnSendStopMessage(ServerLevel serverWorld, Vec3 position, UUID storageUuid, CallbackInfo ci) {
        SophisticatedBackpacksEtchedIntegrationDataBase.ACTIVE_STREAMS_CACHE.remove(storageUuid);
        EtchedStreamInfo info = (EtchedStreamInfo) SophisticatedBackpacksEtchedIntegrationDataBase.ETCHED_STREAMS_CACHE.remove(storageUuid);
        if (info != null) {
            System.out.println("[SBEI] OnSendStopMessage!");
            StopEtchedStream(serverWorld, info);
        }
    }

    @Unique
    private static void StopEtchedStream(ServerLevel serverLevel, EtchedStreamInfo info) {
        if (info.isEntity()) {
            Entity entity = serverLevel.getEntity(info.entityId);
            if (entity != null) {
                // NeoForge 1.21.1: broadcastAndSend replaces TRACKING_ENTITY_AND_SELF
                serverLevel.getChunkSource().broadcastAndSend(
                    entity,
                    new ClientboundPlayEntityMusicPacket(entity)
                );
            }
        } else {
            Vec3 center = Vec3.atCenterOf(info.blockPos);
            serverLevel.getPlayers(p -> p.distanceToSqr(center) < 64 * 64).forEach(player ->
                player.connection.send(new ClientboundPlayBlockMusicPacket(ItemStack.EMPTY, info.blockPos))
            );
        }
    }

    @Unique
    private static boolean SyncActiveStreams(ServerLevel serverLevel, Vec3 position, UUID storageUuid) {
        SophisticatedBackpacksEtchedIntegrationDataBase.EStreamType type =
            SophisticatedBackpacksEtchedIntegrationDataBase.ACTIVE_STREAMS_CACHE.get(storageUuid);

        // We check if it's an etched stream by looking at the ETCHED_STREAMS_CACHE
        boolean isEtched = SophisticatedBackpacksEtchedIntegrationDataBase.ETCHED_STREAMS_CACHE.containsKey(storageUuid);

        if (!isEtched) {
            // Vanilla disc being played
            if (type == SophisticatedBackpacksEtchedIntegrationDataBase.EStreamType.Etched) {
                EtchedStreamInfo info = SophisticatedBackpacksEtchedIntegrationDataBase.ETCHED_STREAMS_CACHE.get(storageUuid);
                if (info != null) {
                    StopEtchedStream(serverLevel, info);
                }
            }
            SophisticatedBackpacksEtchedIntegrationDataBase.ACTIVE_STREAMS_CACHE.put(
                storageUuid,
                SophisticatedBackpacksEtchedIntegrationDataBase.EStreamType.Vanilla
            );
            return true;
        } else {
            // Etched disc being played
            if (type == SophisticatedBackpacksEtchedIntegrationDataBase.EStreamType.Vanilla) {
                PacketDistributor.sendToPlayersNear(serverLevel, null, position.x, position.y, position.z, 128,
                    new StopDiscPlaybackPayload(storageUuid)
                );
            }
            SophisticatedBackpacksEtchedIntegrationDataBase.ACTIVE_STREAMS_CACHE.put(
                storageUuid,
                SophisticatedBackpacksEtchedIntegrationDataBase.EStreamType.Etched
            );
            return false;
        }
    }
}
