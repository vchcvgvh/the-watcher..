package com.thewatcher.client;

import com.thewatcher.TheWatcher;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Random;

/**
 * The Watcher leaves a note on your desktop and opens it (Notepad on Windows, TextEdit on macOS).
 * Only ever writes new .txt files with the mod's own text: never touches, changes or deletes anything else.
 * Turn off with pcNotes=false in config/thewatcher.properties.
 */
@Environment(EnvType.CLIENT)
public final class PcNotes {
    private PcNotes() {}

    private static final int MAX_PER_SESSION = 5;
    private static final long MIN_GAP_MS = 6 * 60_000L;
    private static int written = 0;
    private static long last = 0;

    public static void leaveNote(Random r) {
        if (!WatcherConfig.pcNotes || written >= MAX_PER_SESSION) return;
        long now = System.currentTimeMillis();
        if (now - last < MIN_GAP_MS) return;
        last = now;
        written++;
        String[] note = ScareLines.NOTES[r.nextInt(ScareLines.NOTES.length)];
        String name = ScareLines.fill(note[0]).replaceAll("[^A-Za-z0-9 _.\\-()]", "_");
        String text = ScareLines.fill(note[1]);
        Thread t = new Thread(() -> write(name, text), "the-watcher-note");
        t.setDaemon(true);
        t.start();
    }

    private static void write(String name, String text) {
        try {
            Path dir = desktop();
            Path file = dir.resolve(name);
            String base = name.endsWith(".txt") ? name.substring(0, name.length() - 4) : name;
            for (int i = 2; Files.exists(file); i++) file = dir.resolve(base + " (" + i + ").txt");
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            Files.writeString(file, os.contains("win") ? text.replace("\n", "\r\n") : text);
            open(file, os);
        } catch (Exception e) {
            TheWatcher.LOGGER.warn("The Watcher couldn't leave its note", e);
        }
    }

    private static Path desktop() throws Exception {
        Path home = Path.of(System.getProperty("user.home"));
        for (Path p : new Path[]{home.resolve("OneDrive").resolve("Desktop"), home.resolve("Desktop")}) {
            if (Files.isDirectory(p)) return p;
        }
        Path fallback = FabricLoader.getInstance().getGameDir().resolve("the_watcher");
        Files.createDirectories(fallback);
        return fallback;
    }

    private static void open(Path file, String os) throws Exception {
        ProcessBuilder pb;
        if (os.contains("win")) pb = new ProcessBuilder("notepad.exe", file.toString());
        else if (os.contains("mac")) pb = new ProcessBuilder("open", "-e", file.toString());
        else pb = new ProcessBuilder("xdg-open", file.toString());
        pb.start();
    }
}
