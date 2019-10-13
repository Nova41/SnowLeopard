package com.nova41.bukkitdev.slr.util;

import org.apache.commons.io.FileUtils;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

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
    public static String checkSeparator(String url) {
        return url.replace("\\", File.separator);
    }

    // Mkdir with ease
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void createDirectoryIfAbsent(File dataFolder, String directoryName) {
        new File(dataFolder, checkSeparator(directoryName)).mkdirs();
    }

    // Save resource file to destination
    public static void saveResourceIfAbsent(
            Plugin plugin, String fileName, String releasePath) throws IOException {
        File toBeReleased = new File(plugin.getDataFolder(), checkSeparator(releasePath));
        if (!toBeReleased.exists())
            FileUtils.copyInputStreamToFile(plugin.getResource(fileName), toBeReleased);
    }

}
