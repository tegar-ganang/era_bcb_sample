package org.pointrel.pointrel20110330.archives;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class ResourceFileSupport {

    public static final String ResourceFilePrefix = "PCE_SHA256_";

    public static final String VariableResourceFileSuffix = ".pointrel-variable.json";

    public static String wildcardMatchStringForAnyResourceFileWithSuffix(String suffix) {
        return ResourceFilePrefix + "*" + suffix;
    }

    public static String wildcardMatchStringForAnyResourceFile() {
        return ResourceFilePrefix + "*";
    }

    public static boolean isValidResourceFileName(String name) {
        int lengthOfHexEncodedSHA256Hash = 64;
        return isValidResourceFileName(name, ResourceFilePrefix, lengthOfHexEncodedSHA256Hash);
    }

    public static boolean isValidResourceFileName(String name, String prefix, int hashLength) {
        if (!name.startsWith(prefix)) return false;
        int positionOfSizeUnderscore = prefix.length() + hashLength;
        if (name.length() <= positionOfSizeUnderscore + 1) return false;
        String hexEncodedHash = name.substring(prefix.length(), positionOfSizeUnderscore);
        if (!hexEncodedHash.matches("[0123456789abcdef]{" + hashLength + "}")) return false;
        if (name.charAt(positionOfSizeUnderscore) != '_') return false;
        int firstDotPosition = name.indexOf(".");
        String sizeField = null;
        if (firstDotPosition == -1) {
            sizeField = name.substring(positionOfSizeUnderscore + 1);
        } else {
            sizeField = name.substring(positionOfSizeUnderscore + 1, firstDotPosition);
        }
        int lengthOfSizeField = sizeField.length();
        if (!sizeField.matches("[0123456789]{" + lengthOfSizeField + "}")) return false;
        return true;
    }

    public static boolean isValidResourceFileNameWithSuffix(String name, String suffix) {
        if (suffix == null) return isValidResourceFileName(name);
        return isValidResourceFileName(name) && name.endsWith(suffix);
    }

    public static ArrayList<String> getAllResourceFiles(String directoryPath) {
        return getAllResourceFilesWithSuffix(directoryPath, null);
    }

    public static ArrayList<String> getAllResourceFilesWithSuffix(String directoryPath, String suffix) {
        File folder = new File(directoryPath);
        ArrayList<String> result = new ArrayList<String>();
        getAllResourceFilesWithSuffix(folder, suffix, result);
        return result;
    }

    public static ArrayList<String> getAllResourceFilesWithSuffix(File directoryPath, String suffix, ArrayList<String> result) {
        File[] listOfFiles = directoryPath.listFiles();
        for (File file : listOfFiles) {
            String name = file.getName();
            if (isValidResourceFileNameWithSuffix(name, suffix)) {
                result.add(file.getName());
            } else {
                if (!name.startsWith(".") && file.isDirectory()) {
                    getAllResourceFilesWithSuffix(file, suffix, result);
                }
            }
        }
        return result;
    }

    public static void copyInputStreamToOutputStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead = 0;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
    }

    public static void copyFromFileToFile(File inputFile, File outputFile) throws IOException {
        FileInputStream inputStream = new FileInputStream(inputFile);
        FileOutputStream outputStream = new FileOutputStream(outputFile);
        try {
            byte[] buffer = new byte[4096];
            int bytesRead = 0;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } finally {
            inputStream.close();
            outputStream.close();
        }
    }

    public static void copyFromFileToFileUsingNIO(File inputFile, File outputFile) throws FileNotFoundException, IOException {
        FileChannel inputChannel = new FileInputStream(inputFile).getChannel();
        FileChannel outputChannel = new FileOutputStream(outputFile).getChannel();
        try {
            inputChannel.transferTo(0, inputChannel.size(), outputChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inputChannel != null) inputChannel.close();
            if (outputChannel != null) outputChannel.close();
        }
    }

    public static String getExtensionWithDotOrEmptyString(String filename, boolean allExtensions) {
        String extension = "";
        if (filename.contains(".")) {
            if (allExtensions) {
                extension = filename.substring(filename.indexOf("."));
            } else {
                extension = filename.substring(filename.lastIndexOf("."));
            }
        }
        return extension;
    }

    public static boolean isValidHashFromFileAndName(File file) {
        String resourceReference = Standards.getResourceReferenceWithSHA256HashAsHexEncodedString(file);
        boolean matches = resourceReference.equals(file.getName());
        return matches;
    }

    public static boolean isValidResourceFileExtension(String extension) {
        if (extension == null) return true;
        if (extension.equals("")) return true;
        if (extension.contains("/")) return false;
        if (extension.contains("\\")) return false;
        if (extension.contains(":")) return false;
        if (extension.contains("\n")) return false;
        if (extension.contains("\r")) return false;
        if (extension.contains("..")) return false;
        if (extension.charAt(0) != '.') return false;
        return true;
    }

    public static String getPrefix(String resourceReference) {
        int firstUnderscorePosition = resourceReference.indexOf("_");
        if (firstUnderscorePosition == -1) return null;
        int secondUnderscorePosition = resourceReference.indexOf("_", firstUnderscorePosition + 1);
        if (firstUnderscorePosition == -1) return null;
        return resourceReference.substring(0, secondUnderscorePosition + 1);
    }

    public static String jsonValue(String fieldName, String value) {
        String valueRepresentation;
        if (value == null) {
            valueRepresentation = "null";
        } else {
            valueRepresentation = "\"" + value + "\"";
        }
        return "\"" + fieldName + "\":" + valueRepresentation;
    }
}
