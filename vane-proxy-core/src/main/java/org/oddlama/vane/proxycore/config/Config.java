package org.oddlama.vane.proxycore.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

public class Config {

    // multiplexer_id, { Integer port, List<UUID> allowed_uuids }
    public LinkedHashMap<Integer, AuthMultiplex> auth_multiplex;
    public LinkedHashMap<String, ManagedServer> managed_servers;

    public Config(File file) throws IOException {
        CommentedFileConfig config = CommentedFileConfig.builder(file)
            .autosave()
            .preserveInsertionOrder()
            .sync()
            .build();

        config.load();

        LinkedHashMap<Integer, AuthMultiplex> auth_multiplex = new LinkedHashMap<>();
        LinkedHashMap<String, ManagedServer> managed_servers = new LinkedHashMap<>();

        Set<Integer> registered_ports = new HashSet<>();
        CommentedConfig multiplexers_config = config.get("auth_multiplex");
        for (final var multiplexer_conf : multiplexers_config.entrySet()) {
            final var key_string = multiplexer_conf.getKey();
            int key;
            try {
                key = Integer.parseInt(key_string);
            } catch (Exception ignored) {
                throw new IllegalArgumentException("Multiplexer ID '" + key_string + "' is not an integer!");
            }

            final var value = multiplexer_conf.getValue();
            if (!(value instanceof final CommentedConfig multiplexer_config)) throw new IllegalArgumentException(
                "Multiplexer '" + key + "' has an invalid configuration!"
            );

            final var port = multiplexer_config.getInt("port");
            if (registered_ports.contains(port)) throw new IllegalArgumentException(
                "Multiplexer ID '" + key_string + "' uses an already registered port!"
            );

            final var multiplexer = new AuthMultiplex(port, multiplexer_config.get("allowed_uuids"));

            registered_ports.add(multiplexer.port);
            auth_multiplex.put(key, multiplexer);
        }

        this.auth_multiplex = auth_multiplex;

        CommentedConfig servers_config = config.get("managed_servers");
        for (final var server_conf : servers_config.entrySet()) {
            final var key = server_conf.getKey();

            final var value = server_conf.getValue();
            if (!(value instanceof final CommentedConfig managed_server_config)) throw new IllegalArgumentException(
                "Managed server '" + key + "' has an invalid configuration!"
            );

            final var managed_server = new ManagedServer(
                key,
                managed_server_config.get("display_name"),
                managed_server_config.get("online"),
                managed_server_config.get("offline"),
                managed_server_config.get("start")
            );

            managed_servers.put(key, managed_server);
        }

        this.managed_servers = managed_servers;
    }
}
