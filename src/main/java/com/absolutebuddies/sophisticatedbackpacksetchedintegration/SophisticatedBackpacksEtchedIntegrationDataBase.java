package com.absolutebuddies.sophisticatedbackpacksetchedintegration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SophisticatedBackpacksEtchedIntegrationDataBase {

    public enum EStreamType {
        Etched,
        Vanilla
    }

    public static final Map<UUID, EtchedStreamInfo> ETCHED_STREAMS_CACHE = new ConcurrentHashMap<>();
    public static final Map<UUID, EStreamType> ACTIVE_STREAMS_CACHE = new ConcurrentHashMap<>();
    public static int DISC_DURATION = 0;
}
