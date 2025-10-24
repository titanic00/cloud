package com.titanic00.cloud.util;

import io.minio.Result;
import io.minio.messages.Item;

import java.io.ByteArrayOutputStream;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MinioObjectUtil {

    // path must end with a "/" or a file name with an extension
    // symbols "< > : " / \ | ? *" are not allowed
    public static final String rgx = "^(?:[A-Za-z0-9._\\- ()]+/)*(?:[A-Za-z0-9._\\- ()]+)?$";

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

    public static boolean validatePath(String path) {
        return path.matches(rgx);
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

    // return path to parent directory without including root directory
    public static String getParentDirectoryPath(String fullPath) {
        int lastButOneIdx;

        for (int i = fullPath.lastIndexOf("/") - 1; ; i--) {
            if (fullPath.toCharArray()[i] == '/') {
                lastButOneIdx = i;
                break;
            }
        }

        return fullPath.substring(lastButOneIdx + 1, fullPath.lastIndexOf("/") + 1);
    }

    // return full path to the resource directory including root directory
    public static String getResourceFullPath(String fullPath) {
        return fullPath.substring(0, fullPath.lastIndexOf("/") + 1);
    }

    // return last nested directory and subdirectories without including root directory
    public static String formatSourceDirectory(String fullPath, String parentFolder) {
        return fullPath.substring(fullPath.indexOf(parentFolder));

    }

    public static byte[] toZip(Iterable<Result<Item>> items, String parentFolder) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {

            for (Result<Item> item : items) {
                ZipEntry entry = new ZipEntry(formatSourceDirectory(item.get().objectName(), parentFolder));
                zipOut.putNextEntry(entry);
            }

            zipOut.closeEntry();
        }

        return baos.toByteArray();
    }
}
