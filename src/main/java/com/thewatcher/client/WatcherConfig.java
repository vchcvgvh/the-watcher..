package com.thewatcher.client;

import com.thewatcher.TheWatcher;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** .minecraft/config/thewatcher.properties: switch off the screen jumpscares or the notes on your PC. */
@Environment(EnvType.CLIENT)
public final class WatcherConfig {
    private WatcherConfig() {}

    public static boolean jumpscares = true;
    public static boolean popupText = true;
    public static boolean pcNotes = true;

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("thewatcher.properties");
        Properties p = new Properties();
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file)) { p.load(r); }
            }
            jumpscares = Boolean.parseBoolean(p.getProperty("jumpscares", "true"));
            popupText = Boolean.parseBoolean(p.getProperty("popupText", "true"));
            pcNotes = Boolean.parseBoolean(p.getProperty("pcNotes", "true"));
            p.setProperty("jumpscares", String.valueOf(jumpscares));
            p.setProperty("popupText", String.valueOf(popupText));
            p.setProperty("pcNotes", String.valueOf(pcNotes));
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                p.store(w, " The Watcher\n jumpscares = faces filling the screen\n popupText = messages popping up on screen\n"
                        + " pcNotes = the Watcher leaves text files on your desktop and opens them in Notepad");
            }
        } catch (Exception e) {
            TheWatcher.LOGGER.warn("Couldn't read/write thewatcher.properties", e);
        }
    }
}
