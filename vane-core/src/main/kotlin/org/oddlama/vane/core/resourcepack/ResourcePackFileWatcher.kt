package org.oddlama.vane.core.resourcepack

import java.io.File
import java.io.IOException
import java.nio.file.*
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask

/**
 * Watches vane module language files and refreshes the resource pack when they change.
 *
 * @param resourcePackDistributor distributor used to regenerate and broadcast the pack.
 * @param file the generated resource pack zip file.
 */
class ResourcePackFileWatcher(
    private val resourcePackDistributor: ResourcePackDistributor,
    private val file: File,
) {
    /** Active watch service used to receive filesystem events. */
    private var eyes: WatchService? = null

    /** Background Bukkit task polling filesystem events. */
    private var watchTask: BukkitTask? = null

    /**
     * Starts watching vane module directories for language file changes.
     *
     * @throws IOException if the watch service cannot be initialized.
     */
    @Throws(IOException::class)
    fun watchForChanges() {
        val plugin = resourcePackDistributor.module as? Plugin ?: return
        val watcher = FileSystems.getDefault().newWatchService()
        eyes = watcher
        val langFileMatch = FileSystems.getDefault().getPathMatcher("glob:**/lang-*.yml")
        registerDirectories(Paths.get("plugins"), watcher, ::isVaneModuleFolder)
        watchTask =
            watchAsync(watcher, langFileMatch) { updateAndSendResourcePack() }
                .runTaskAsynchronously(plugin)
    }

    /** Stops background watching and releases resources. */
    fun stop() {
        watchTask?.cancel()
        watchTask = null
        try {
            eyes?.close()
        } catch (_: IOException) {}
        eyes = null
    }

    /** Regenerates the resource pack and redistributes it to online players. */
    private fun updateAndSendResourcePack() {
        val mod = resourcePackDistributor.module ?: return
        resourcePackDistributor.counter++
        mod.generateResourcePack()
        resourcePackDistributor.updateSha1(file)
        Bukkit.getOnlinePlayers().forEach { resourcePackDistributor.sendResourcePack(it) }
    }

    /** Returns whether the path belongs to a vane module directory. */
    private fun isVaneModuleFolder(p: Path): Boolean = p.fileName.toString().startsWith("vane-")

    /**
     * Runnable wrapper that tracks start and completion state.
     *
     * @param r the wrapped runnable.
     */
    private class TrackRunned(val r: Runnable) : BukkitRunnable() {
        /** Indicates whether the runnable has completed. */
        var hasRun = false

        /** Indicates whether the runnable has started. */
        var hasStarted = false

        /** Executes the wrapped runnable and updates state flags. */
        override fun run() {
            hasStarted = true
            r.run()
            hasRun = true
        }
    }

    /**
     * Creates a background watcher task and schedules debounced updates on matching changes.
     *
     * @param eyes the active watch service.
     * @param matchLang matcher for language file paths.
     * @param onHit action to run when matching changes are detected.
     * @return a Bukkit runnable that polls watch events.
     */
    private fun watchAsync(
        eyes: WatchService,
        matchLang: PathMatcher,
        onHit: Runnable,
    ): BukkitRunnable {
        val plugin = resourcePackDistributor.module as? Plugin
        return object : BukkitRunnable() {
            override fun run() {
                var shouldSchedule = false
                var runner: TrackRunned? = null
                while (true) {
                    val key =
                        try {
                            eyes.take()
                        } catch (_: ClosedWatchServiceException) {
                            return
                        } catch (_: InterruptedException) {
                            Thread.currentThread().interrupt()
                            return
                        }
                    key.pollEvents()
                        .filterIsInstance<WatchEvent<*>>()
                        .filter { it.kind() !== StandardWatchEventKinds.OVERFLOW }
                        .forEach { ev ->
                            val dir = key.watchable() as? Path ?: return@forEach
                            val filename = ev.context() as? Path ?: return@forEach
                            if (matchLang.matches(dir.resolve(filename))) shouldSchedule = true
                        }
                    if (shouldSchedule) {
                        if (runner?.hasStarted == false) runner.cancel()
                        runner =
                            TrackRunned(onHit).also {
                                if (plugin != null) it.runTaskLater(plugin, 20L)
                            }
                        shouldSchedule = false
                    }
                    if (!key.reset()) return
                }
            }
        }
    }

    /**
     * Registers all matching subdirectories of [root] with the watch service.
     *
     * @param root root path to walk.
     * @param watcher the watch service.
     * @param pathMatch predicate that determines which directories should be watched.
     * @throws IOException if directory traversal fails.
     */
    @Throws(IOException::class)
    private fun registerDirectories(
        root: Path,
        watcher: WatchService,
        pathMatch: (Path) -> Boolean,
    ) {
        Files.walk(root)
            .filter { Files.isDirectory(it) && pathMatch(it) }
            .forEach { p ->
                p.register(
                    watcher,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                )
            }
    }
}
