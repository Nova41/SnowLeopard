package com.nova41.bukkitdev.slr.util;

import org.apache.commons.io.FileUtils;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Helper functions for handling files.
 */
public final class SLFiles {

    private SLFiles() {}

    /**
     * Replace '\' in url with separator of local system.
     * Avoids path issue when running on non-Windows systems.
     *
     * @param url base URL
     * @return modified URL
     */
    public static String getSeparatedString(String url) {
        return url.replace("\\", File.separator);
    }

    public static Path getSeparatedPath(String url) {
        return Paths.get(URI.create(getSeparatedString(url)));
    }

    // Mkdir with ease
    public static void createDirectoryIfAbsent(String path, String directoryName) throws IOException {
        Files.createDirectory(getSeparatedPath(path + "//" + directoryName));
    }

    // Save resource file to destination
    public static void saveResourceIfAbsent(
            Plugin plugin,
            String fileName,
            String releasePath) throws IOException {
        File toBeReleased = new File(plugin.getDataFolder(), getSeparatedString(releasePath));
        if (!toBeReleased.exists())
            FileUtils.copyInputStreamToFile(plugin.getResource(fileName), toBeReleased);
    }

}
