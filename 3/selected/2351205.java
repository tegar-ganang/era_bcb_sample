package com.limegroup.bittorrent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.limewire.service.ErrorService;
import org.limewire.util.BEncoder;
import org.limewire.util.CommonUtils;
import org.limewire.util.StringUtils;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.security.SHA1;

/**
 * A struct-like class which contains typesafe representations
 * of everything we understand in in a .torrent file.
 * 
 * This will throw a ValueException if the data is malformed or
 * not what we expect it to be.  UTF-8 versions of Strings are
 * preferred over ASCII versions, wherever possible.
 */
public class BTDataImpl implements BTData {

    private final String announce;

    /** All the pieces as one big array.  Non-final 'cause it's big & we want to clear it. */
    private byte[] pieces;

    /** The length of a single piece. */
    private final Long pieceLength;

    /** The SHA1 of the info object. */
    private byte[] infoHash;

    /** The name of the torrent file (if one file) or parent folder (if multiple files). */
    private final String name;

    /** The length of the torrent if one file.  null if multiple. */
    private final Long length;

    /** A list of subfiles of this torrent is multiple files.  null if a single file. */
    private List<BTData.BTFileData> files;

    /** A list of all subfolders this torrent uses.  null if a single file. */
    private final Set<String> folders;

    /** Whether the private flag is set */
    private final boolean isPrivate;

    /** Constructs a new BTData for BT File's Manager. */
    public BTDataImpl(List<com.limegroup.bittorrent.BTData.BTFileData> files, Set<String> folders, BTData torrentData) throws com.limegroup.bittorrent.ValueException {
        Object tmp;
        announce = torrentData.getAnnounce();
        infoHash = torrentData.getInfoHash();
        isPrivate = torrentData.isPrivate();
        pieces = torrentData.getPieces();
        pieceLength = torrentData.getPieceLength();
        name = torrentData.getName();
        length = torrentData.getLength();
        this.files = files;
        this.folders = folders;
    }

    /** Constructs a new BTData out of the map of properties. */
    public BTDataImpl(Map<?, ?> torrentFileMap) throws ValueException {
        Object tmp;
        tmp = torrentFileMap.get("announce");
        if (tmp instanceof byte[]) announce = StringUtils.getASCIIString((byte[]) tmp); else throw new ValueException("announce missing or invalid!");
        tmp = torrentFileMap.get("info");
        if (tmp == null || !(tmp instanceof Map)) throw new ValueException("info missing or invalid!");
        Map infoMap = (Map) tmp;
        infoHash = calculateInfoHash(infoMap);
        tmp = infoMap.get("private");
        if (tmp instanceof Long) {
            isPrivate = ((Long) tmp).intValue() == 1;
        } else isPrivate = false;
        tmp = infoMap.get("pieces");
        if (tmp instanceof byte[]) pieces = (byte[]) tmp; else throw new ValueException("info->piece missing!");
        tmp = infoMap.get("piece length");
        if (tmp instanceof Long) pieceLength = (Long) tmp; else throw new ValueException("info->'piece length' missing!");
        tmp = infoMap.get("name.utf-8");
        name = getPreferredString(infoMap, "name");
        if (name == null || name.length() == 0) throw new ValueException("no valid name!");
        if (infoMap.containsKey("length") == infoMap.containsKey("files")) throw new ValueException("info->length & info.files can't both exist or not exist!");
        tmp = infoMap.get("length");
        if (tmp instanceof Long) {
            length = (Long) tmp;
            if (length < 0) throw new ValueException("invalid length value");
        } else if (tmp != null) throw new ValueException("info->length is non-null, but not a Long!"); else length = null;
        tmp = infoMap.get("files");
        if (tmp instanceof List) {
            List<?> fileData = (List) tmp;
            if (fileData.isEmpty()) throw new ValueException("empty file list");
            files = new ArrayList<BTData.BTFileData>(fileData.size());
            folders = new HashSet<String>();
            for (Object o : fileData) {
                if (!(o instanceof Map)) throw new ValueException("info->files[x] not a Map!");
                Map<?, ?> fileMap = (Map) o;
                tmp = fileMap.get("length");
                if (!(tmp instanceof Long)) throw new ValueException("info->files[x].length not a Long!");
                Long ln = (Long) tmp;
                if (ln < 0) throw new ValueException("invalid length");
                boolean doASCII = true;
                try {
                    parseFiles(fileMap, ln, files, folders, true);
                    doASCII = false;
                } catch (ValueException ignored) {
                }
                if (doASCII) parseFiles(fileMap, ln, files, folders, false);
            }
        } else if (tmp != null) {
            throw new ValueException("info->files is non-null, but not a list!");
        } else {
            files = null;
            folders = null;
        }
        if (files == null) {
            files = new ArrayList<com.limegroup.bittorrent.BTData.BTFileData>(1);
            files.add(new com.limegroup.bittorrent.BTData.BTFileData(length, name));
        }
    }

    /** Parses the List of Maps of file data. */
    private void parseFiles(Map<?, ?> fileMap, Long ln, List<BTData.BTFileData> fileData, Set<String> folderData, boolean utf8) throws ValueException {
        Object tmp = fileMap.get("path" + (utf8 ? ".utf-8" : ""));
        if (!(tmp instanceof List)) throw new ValueException("info->files[x].path[.utf-8] not a List!");
        Set<String> newFolders = new HashSet<String>();
        String path = parseFileList((List) tmp, newFolders, true);
        if (path == null) throw new ValueException("info->files[x].path[-utf-8] not valid!");
        folderData.addAll(newFolders);
        fileData.add(new BTData.BTFileData(ln, path));
    }

    /**
     * Parses a list of paths into a single string, adding the intermediate
     * folders into the Set of folders.  The paths are parsed either as
     * UTF or ASCII.
     */
    private String parseFileList(List<?> paths, Set<String> folders, boolean utf8) throws ValueException {
        if (paths.isEmpty()) throw new ValueException("empty paths list");
        StringBuilder sb = new StringBuilder();
        for (Iterator<?> i = paths.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (!(o instanceof byte[])) throw new ValueException("info->files[x]->path[.utf-8][x] not a byte[]!");
            String current;
            if (utf8) current = StringUtils.getUTF8String((byte[]) o); else current = StringUtils.getASCIIString((byte[]) o);
            if (current.length() == 0) throw new ValueException("empty path element");
            sb.append(File.separator);
            sb.append(CommonUtils.convertFileName(current));
            if (i.hasNext()) folders.add(sb.toString());
        }
        return sb.toString();
    }

    /** Returns either the UTF-8 version (if it exists) or the ASCII version of a String. */
    private String getPreferredString(Map<?, ?> info, String key) {
        String str = null;
        Object data = info.get(key + ".utf-8");
        if (data instanceof byte[]) {
            try {
                str = new String((byte[]) data, Constants.UTF_8_ENCODING);
            } catch (Throwable t) {
            }
        }
        if (str == null) {
            data = info.get(key);
            if (data instanceof byte[]) str = StringUtils.getASCIIString((byte[]) data);
        }
        return str;
    }

    /**
     * Calculates the infoHash of the map.  Because BT maps are stored
     * as String -> Object, and the keys are stored alphabetically, 
     * it is guaranteed that any two maps with identical keys & values
     * will have the same info hash when decoded & recoded.
     * 
     * @param infoMap
     * @return the infoHash of the infoMap
     */
    private byte[] calculateInfoHash(Map<?, ?> infoMap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            BEncoder.getEncoder(baos, true, false, "UTF-8").encodeDict(infoMap);
        } catch (IOException ioe) {
            ErrorService.error(ioe);
        }
        MessageDigest md = new SHA1();
        return md.digest(baos.toByteArray());
    }

    public String getAnnounce() {
        return announce;
    }

    public List<BTData.BTFileData> getFiles() {
        return files;
    }

    public Set<String> getFolders() {
        return folders;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    public Long getLength() {
        return length;
    }

    public String getName() {
        return name;
    }

    public Long getPieceLength() {
        return pieceLength;
    }

    public byte[] getPieces() {
        return pieces;
    }

    public void clearPieces() {
        pieces = null;
    }
}
