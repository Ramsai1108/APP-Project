package com.campus.emergency.util;

import java.io.File;

public class FileUtils {

    public static boolean fileExists(String path) {
        if (path == null || path.trim().isEmpty()) return false;
        File file = new File(path);
        return file.exists() && file.isFile();
    }
}
