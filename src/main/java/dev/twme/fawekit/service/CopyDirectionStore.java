package dev.twme.fawekit.service;

import com.sk89q.worldedit.math.BlockVector3;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CopyDirectionStore {
    private final Map<UUID, BlockVector3> directions = new ConcurrentHashMap<>();

    public void put(UUID playerId, BlockVector3 direction) {
        directions.put(playerId, direction);
    }

    public Optional<BlockVector3> get(UUID playerId) {
        return Optional.ofNullable(directions.get(playerId));
    }
}
