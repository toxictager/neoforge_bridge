package com.absolutebuddies.sophisticatedbackpacksetchedintegration;

import net.minecraft.core.BlockPos;

public class EtchedStreamInfo {

    public final int entityId;
    public final BlockPos blockPos;

    private EtchedStreamInfo(int entityId, BlockPos blockPos) {
        this.entityId = entityId;
        this.blockPos = blockPos;
    }

    public boolean isEntity() {
        return entityId != -1;
    }

    public static EtchedStreamInfo forEntity(int id) {
        return new EtchedStreamInfo(id, null);
    }

    public static EtchedStreamInfo forBlock(BlockPos pos) {
        return new EtchedStreamInfo(-1, pos.immutable());
    }
}
