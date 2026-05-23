package org.oddlama.vane.core.resourcepack;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.*;
import java.nio.file.*;
import java.util.Iterator;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class ResourcePackFileWatcher {

    private final ResourcePackDistributor resource_pack_distributor;
    private final File file;
    private WatchService eyes;
    private org.bukkit.scheduler.BukkitTask watchTask;

    public ResourcePackFileWatcher(ResourcePackDistributor resource_pack_distributor, File file)
        throws IOException, InterruptedException {
        this.resource_pack_distributor = resource_pack_distributor;
        this.file = file;
    }

    public void watch_for_changes() throws IOException {
        var eyes = FileSystems.getDefault().newWatchService();
        var lang_file_match = FileSystems.getDefault().getPathMatcher("glob:**/lang-*.yml");
        register_directories(Paths.get("plugins"), eyes, this::is_vane_module_folder);

        watchTask = watch_async(eyes, lang_file_match, this::update_and_send_resource_pack).runTaskAsynchronously(
            resource_pack_distributor.get_module()
        );
    }

    public void stop() {
        if (watchTask != null) {
            watchTask.cancel();
            watchTask = null;
        }
        if (eyes != null) {
            try {
                eyes.close();
            } catch (IOException ignored) {}
            eyes = null;
        }
    }

    private void update_and_send_resource_pack() {
        resource_pack_distributor.counter++;
        resource_pack_distributor.get_module().generate_resource_pack();
        resource_pack_distributor.update_sha1(file);
        for (Player player : Bukkit.getOnlinePlayers()) {
            resource_pack_distributor.send_resource_pack(player);
        }
    }

    private boolean is_vane_module_folder(Path p) {
        return p.getFileName().toString().startsWith("vane-");
    }

    private static class TrackRunned extends BukkitRunnable {

        final Runnable r;
        boolean has_run = false;
        boolean has_started = false;

        public TrackRunned(Runnable r) {
            this.r = r;
        }

        @Override
        public void run() {
            has_started = true;
            r.run();
            has_run = true;
        }
    }

    private @NotNull BukkitRunnable watch_async(WatchService eyes, PathMatcher match_lang, Runnable on_hit) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                boolean should_schedule = false;
                TrackRunned runner = null;
                for (;;) {
                    final WatchKey key;
                    try {
                        key = eyes.take();
                    } catch (java.nio.file.ClosedWatchServiceException e) {
                        return;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    // process events
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == OVERFLOW) continue;

                        // This generic is always Path for WatchEvent kinds other than OVERFLOW
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path dir = (Path) key.watchable();
                        Path filename = ev.context();
                        if (!match_lang.matches(dir.resolve(filename))) continue;
                        should_schedule = true;
                    }

                    if (should_schedule) {
                        if (runner != null) {
                            if (!runner.has_started) runner.cancel();
                        }
                        runner = new TrackRunned(on_hit);
                        runner.runTaskLater(resource_pack_distributor.get_module().core, 20L);
                        should_schedule = false;
                    }

                    // reset the key
                    boolean valid = key.reset();
                    if (!valid) {
                        return;
                    }
                }
            }
        };
    }

    private void register_directories(Path root, WatchService watcher, Predicate<Path> path_match) throws IOException {
        // register vane sub-folders.
        final Iterator<Path> interesting_paths = Files.walk(root)
            .filter(Files::isDirectory)
            .filter(path_match)
            .iterator();
        // quirky, but checked exceptions inside streams suck.
        while (interesting_paths.hasNext()) {
            Path p = interesting_paths.next();
            p.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        }
    }
}
