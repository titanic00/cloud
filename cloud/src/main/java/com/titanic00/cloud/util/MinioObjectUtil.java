package com.titanic00.cloud.util;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

public class MinioObjectUtil {

    public static String buildObjectName(String path, String fileName) {
        // avoid double slash at the beginning and at the end
        if (!path.endsWith("/") && !fileName.startsWith("/")) {
            path += "/";
        }

        // remove slash at the beginning
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        return path + fileName;
    }

    public static boolean isDir(String path) {
        return path.endsWith("/");
    }

    // return path to the file without including root directory
    public static String formatObjectPath(String objectName) {
        String path = objectName.substring(objectName.indexOf("/") + 1, objectName.lastIndexOf("/") + 1);
        if (path.startsWith("/")) {
            return path.substring(1);
        }

        return path;
    }

    // return path to the directory without including root directory
    public static String formatDirectoryPath(String fullPath) {
        int lastButOneIdx;

        for (int i = fullPath.lastIndexOf("/") - 1; ; i--) {
            if (fullPath.toCharArray()[i] == '/') {
                lastButOneIdx = i;
                break;
            }
        }

        String path = fullPath.substring(fullPath.indexOf("/") + 1, lastButOneIdx + 1);

        if (path.isEmpty()) {
            return "/";
        }

        return path;
    }

    // return file name
    public static String getFileNameFromObjectName(String objectName) {
        String filename = objectName.substring(objectName.lastIndexOf("/"));
        if (filename.startsWith("/")) {
            return filename.substring(1);
        }

        return filename;
    }

    // return directory name
    public static String getDirectoryNameFromObjectName(String objectName) {
        int lastButOneIdx;

        for (int i = objectName.lastIndexOf("/") - 1; ; i--) {
            if (objectName.toCharArray()[i] == '/') {
                lastButOneIdx = i;
                break;
            }
        }

        return objectName.substring(lastButOneIdx + 1, objectName.lastIndexOf("/") + 1);
    }

    public static String humanReadableByteCountSI(Long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }
}
