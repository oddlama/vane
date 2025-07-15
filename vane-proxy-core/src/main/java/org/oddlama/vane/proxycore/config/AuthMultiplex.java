package org.oddlama.vane.proxycore.config;

import java.util.*;
import java.util.stream.Collectors;

public class AuthMultiplex {

    public Integer port;
    private final List<UUID> allowed_uuids;

    public AuthMultiplex(Integer port, List<String> allowed_uuids) {
        this.port = port;

        if (allowed_uuids == null || allowed_uuids.isEmpty()) {
            this.allowed_uuids = List.of();
        } else {
            this.allowed_uuids = allowed_uuids
                .stream()
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .collect(Collectors.toList());
        }
    }

    public boolean uuid_is_allowed(UUID uuid) {
        return allowed_uuids.isEmpty() || allowed_uuids.contains(uuid);
    }
}
