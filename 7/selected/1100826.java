package org.nightlabs.util.windows;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.apache.commons.io.EndianUtils;

/**
 * @author Marc Klinger - marc[at]nightlabs[dot]de
 */
public class Shortcut {

    private File shortcutFile;

    private static int supportedHeader = 'L';

    private static byte[] supportedGuid = new byte[] { 1, 20, 2, 0, 0, 0, 0, 0, -64, 0, 0, 0, 0, 0, 0, 70 };

    private int header;

    private byte[] guid;

    private long flags;

    private int fileAttributes;

    private long creationTime;

    private long modificationTime;

    private long lastAccessTime;

    private long fileLength;

    private long iconNumber;

    private long showWndValue;

    private long hotKey;

    public static final int FLAG_HAS_SHELL_ITEM_ID = 1;

    public static final int FLAG_IS_FILE_OR_DIRECTORY = 2;

    public static final int FLAG_HAS_DESCRIPTION = 4;

    public static final int FLAG_HAS_RELATIVE_PATH = 8;

    public static final int FLAG_HAS_WORKING_DIRECTORY = 16;

    public static final int FLAG_HAS_COMMAND_LINE_ARGUMENTS = 32;

    public static final int FLAG_HAS_CUSTOM_ICON = 64;

    public static final int TARGET_READ_ONLY = 1;

    public static final int TARGET_HIDDEN = 2;

    public static final int TARGET_SYSTEM_FILE = 4;

    public static final int TARGET_VOLUME_LABEL = 8;

    public static final int TARGET_DIRECTORY = 16;

    public static final int TARGET_MOD_SINCE_BACKUP = 32;

    public static final int TARGET_ENCRYPTED = 64;

    public static final int TARGET_NORMAL = 128;

    public static final int TARGET_TEMPORARY = 256;

    public static final int TARGET_SPARSE = 512;

    public static final int TARGET_REPARSE_POINT_DATA = 1024;

    public static final int TARGET_COMPRESSED = 2048;

    public static final int TARGET_OFFLINE = 4096;

    public Shortcut(File shortcutFile) {
        this.shortcutFile = shortcutFile;
    }

    public void load() throws IOException {
        InputStream input = new FileInputStream(shortcutFile);
        try {
            header = EndianUtils.readSwappedInteger(input);
            if (header != supportedHeader) throw new IOException("Invalid header (not a lnk shortcut file)");
            guid = new byte[16];
            int len = input.read(guid);
            if (len != 16) throw new IOException("Invalid guid (unexepected end of file)");
            if (!Arrays.equals(guid, supportedGuid)) throw new IOException("Invalid guid (unsupported version)");
            flags = EndianUtils.readSwappedUnsignedInteger(input);
            fileAttributes = EndianUtils.readSwappedInteger(input);
            creationTime = EndianUtils.readSwappedLong(input);
            modificationTime = EndianUtils.readSwappedLong(input);
            lastAccessTime = EndianUtils.readSwappedLong(input);
            fileLength = EndianUtils.readSwappedUnsignedInteger(input);
            iconNumber = EndianUtils.readSwappedUnsignedInteger(input);
            showWndValue = EndianUtils.readSwappedUnsignedInteger(input);
            hotKey = EndianUtils.readSwappedUnsignedInteger(input);
            EndianUtils.readSwappedInteger(input);
            EndianUtils.readSwappedInteger(input);
            for (int i = 0, flag = 1; i < 32; i++, flag *= 2) {
                System.out.print((flags & flag) > 0 ? "1" : "0");
            }
            System.out.println();
            for (int i = 0, flag = 1; i < 32; i++, flag *= 2) {
                if ((flags & flag) > 0) {
                    switch(flag) {
                        case FLAG_HAS_SHELL_ITEM_ID:
                            System.out.print("FLAG_HAS_SHELL_ITEM_ID ");
                            break;
                        case FLAG_IS_FILE_OR_DIRECTORY:
                            System.out.print("FLAG_IS_FILE_OR_DIRECTORY ");
                            break;
                        case FLAG_HAS_DESCRIPTION:
                            System.out.print("FLAG_HAS_DESCRIPTION ");
                            break;
                        case FLAG_HAS_RELATIVE_PATH:
                            System.out.print("FLAG_HAS_RELATIVE_PATH ");
                            break;
                        case FLAG_HAS_WORKING_DIRECTORY:
                            System.out.print("FLAG_HAS_WORKING_DIRECTORY ");
                            break;
                        case FLAG_HAS_COMMAND_LINE_ARGUMENTS:
                            System.out.print("FLAG_HAS_COMMAND_LINE_ARGUMENTS ");
                            break;
                        case FLAG_HAS_CUSTOM_ICON:
                            System.out.print("FLAG_HAS_CUSTOM_ICON ");
                            break;
                        default:
                            System.out.print("UNKNOWN(" + i + ")");
                    }
                }
            }
            System.out.println();
            if ((flags & FLAG_HAS_SHELL_ITEM_ID) > 0) {
                int shellItemIdListLength = EndianUtils.readSwappedUnsignedShort(input);
                if (shellItemIdListLength > 0) input.skip(shellItemIdListLength);
            }
            if ((flags & FLAG_IS_FILE_OR_DIRECTORY) > 0) {
                long fileLocationInfoLength = EndianUtils.readSwappedUnsignedInteger(input);
                if (fileLocationInfoLength < 4) throw new IllegalStateException();
                input.skip(fileLocationInfoLength - 4);
            }
            if ((flags & FLAG_HAS_DESCRIPTION) > 0) {
                String description = readString(input);
                System.out.println("description: " + description);
            }
            if ((flags & FLAG_HAS_RELATIVE_PATH) > 0) {
                String relativePath = readString(input);
                System.out.println("relativePath: " + relativePath);
            }
            if ((flags & FLAG_HAS_WORKING_DIRECTORY) > 0) {
                String workingDirectory = readString(input);
                System.out.println("workingDirectory: " + workingDirectory);
            }
            if ((flags & FLAG_HAS_COMMAND_LINE_ARGUMENTS) > 0) {
                String commandLineArguments = readString(input);
                System.out.println("commandLineArguments: " + commandLineArguments);
            }
            if ((flags & FLAG_HAS_CUSTOM_ICON) > 0) {
                String iconFile = readString(input);
                System.out.println("iconFile: " + iconFile);
            }
            long ending = EndianUtils.readSwappedUnsignedInteger(input);
            System.out.println("ending: " + ending);
        } finally {
            input.close();
        }
    }

    private String readString(InputStream input) throws IOException, EOFException {
        int length = EndianUtils.readSwappedUnsignedShort(input);
        length *= 2;
        byte[] data = new byte[length];
        int read = input.read(data);
        if (read != length) throw new EOFException("Unexpected end of file");
        for (int i = 0; i < length; i += 2) {
            byte x = data[i];
            data[i] = data[i + 1];
            data[i + 1] = x;
        }
        return new String(data, "UTF-16");
    }

    public long getFlags() {
        return flags;
    }

    public void setFlags(long flags) {
        this.flags = flags;
    }
}
