package dev.twme.fawekit.service;

import com.sk89q.worldedit.util.Location;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PinService {
    private final Map<UUID, Location> locations = new ConcurrentHashMap<>();

    public void pin(UUID playerId, Location location) {
        locations.put(playerId, location);
    }

    public boolean unpin(UUID playerId) {
        return locations.remove(playerId) != null;
    }

    public Optional<Location> get(UUID playerId) {
        return Optional.ofNullable(locations.get(playerId));
    }
}
