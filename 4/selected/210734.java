package nl.huub.van.amelsvoort.qcommon;

import nl.huub.van.amelsvoort.Defines;
import nl.huub.van.amelsvoort.Globals;
import nl.huub.van.amelsvoort.game.Cmd;
import nl.huub.van.amelsvoort.game.cvar_t;
import nl.huub.van.amelsvoort.sys.Sys;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.*;
import nl.huub.van.amelsvoort.HuubBestand;
import nl.huub.van.amelsvoort.HuubHelp;

/**
 * FS
 * 
 * @author cwei
 */
public final class FS extends Globals {

    public static class packfile_t {

        static final int SIZE = 64;

        static final int NAME_SIZE = 56;

        String name;

        int filepos, filelen;

        public String toString() {
            return name + " [ length: " + filelen + " pos: " + filepos + " ]";
        }
    }

    public static class pack_t {

        String filename;

        RandomAccessFile handle;

        ByteBuffer backbuffer;

        int numfiles;

        Hashtable files;
    }

    public static String fs_gamedir;

    private static String fs_userdir;

    public static cvar_t fs_basedir;

    public static cvar_t fs_cddir;

    public static cvar_t fs_gamedirvar;

    public static class filelink_t {

        String from;

        int fromlength;

        String to;
    }

    public static List fs_links = new LinkedList();

    public static class searchpath_t {

        public String filename;

        public pack_t pack;

        public searchpath_t next;
    }

    public static searchpath_t fs_searchpaths;

    public static searchpath_t fs_base_searchpaths;

    public static void CreatePath(String path) {
        int index = path.lastIndexOf('/');
        if (index > 0) {
            File f = new File(path.substring(0, index));
            if (!f.mkdirs() && !f.isDirectory()) {
                Com.Printf("can't create path \"" + path + '"' + "\n");
            }
        }
    }

    public static void FCloseFile(RandomAccessFile file) throws IOException {
        file.close();
    }

    public static void FCloseFile(InputStream stream) throws IOException {
        stream.close();
    }

    public static int FileLength(String filename) {
        searchpath_t search;
        String netpath;
        pack_t pak;
        filelink_t link;
        file_from_pak = 0;
        for (Iterator it = fs_links.iterator(); it.hasNext(); ) {
            link = (filelink_t) it.next();
            if (filename.regionMatches(0, link.from, 0, link.fromlength)) {
                netpath = link.to + filename.substring(link.fromlength);
                File file = new File(netpath);
                if (file.canRead()) {
                    Com.DPrintf("link file: " + netpath + '\n');
                    return (int) file.length();
                }
                Com.Printf("Huub de link file niet gevonden op schijf: " + link.to + '\n');
                return -1;
            }
        }
        for (search = fs_searchpaths; search != null; search = search.next) {
            if (search.pack != null) {
                pak = search.pack;
                filename = filename.toLowerCase();
                packfile_t entry = (packfile_t) pak.files.get(filename);
                if (entry != null) {
                    Com.Printf("Huub file hier gevonden.... " + filename);
                    file_from_pak = 1;
                    Com.DPrintf("PackFile: " + pak.filename + " : " + filename + '\n');
                    File file = new File(pak.filename);
                    if (!file.canRead()) {
                        Com.Error(Defines.ERR_FATAL, "Couldn't reopen " + pak.filename);
                    }
                    return entry.filelen;
                }
            } else {
                filename = HuubHelp.vertalen(filename);
                netpath = search.filename + '/' + filename;
                File file = new File(netpath);
                if (file.exists()) {
                    if (HuubHelp.isLog) HuubHelp.log("File gevonden : " + file.getAbsolutePath(), HuubHelp.NIET_BELANGRIJK);
                }
                if (!(file.canRead())) {
                    HuubBestand huubBestand = new HuubBestand(file);
                    file = huubBestand.lezen(netpath);
                    if (huubBestand.getBytes() == null) {
                        continue;
                    }
                }
                Com.DPrintf("FindFile: " + netpath + " lengte : " + (int) file.length() + '\n');
                return (int) file.length();
            }
        }
        Com.DPrintf("Kan het bestand niet vinden " + filename + " of het alternatief obj\n");
        return -1;
    }

    public static int file_from_pak = 0;

    public static RandomAccessFile FOpenFile(String filename) throws IOException {
        searchpath_t search;
        String netpath;
        pack_t pak;
        filelink_t link;
        File file = null;
        file_from_pak = 0;
        for (Iterator it = fs_links.iterator(); it.hasNext(); ) {
            link = (filelink_t) it.next();
            if (filename.regionMatches(0, link.from, 0, link.fromlength)) {
                netpath = link.to + filename.substring(link.fromlength);
                file = new File(netpath);
                if (file.canRead()) {
                    return new RandomAccessFile(file, "r");
                }
                return null;
            }
        }
        for (search = fs_searchpaths; search != null; search = search.next) {
            if (search.pack != null) {
                pak = search.pack;
                filename = filename.toLowerCase();
                packfile_t entry = (packfile_t) pak.files.get(filename);
                if (entry != null) {
                    file_from_pak = 1;
                    file = new File(pak.filename);
                    if (!file.canRead()) {
                        Com.Error(Defines.ERR_FATAL, "Couldn't reopen " + pak.filename);
                    }
                    if (pak.handle == null || !pak.handle.getFD().valid()) {
                        pak.handle = new RandomAccessFile(pak.filename, "r");
                    }
                    RandomAccessFile raf = new RandomAccessFile(file, "r");
                    raf.seek(entry.filepos);
                    return raf;
                }
            } else {
                netpath = search.filename + '/' + filename;
                file = new File(netpath);
                if (!file.canRead()) {
                    HuubBestand huubBestand = new HuubBestand(file);
                    file = huubBestand.lezen(netpath);
                    if (huubBestand.getBytes() == null) {
                        continue;
                    }
                }
                return new RandomAccessFile(file, "r");
            }
        }
        return null;
    }

    public static final int MAX_READ = 0x10000;

    /**
   * Read
   *
   * Properly handles partial reads
   */
    public static void Read(byte[] buffer, int len, RandomAccessFile f) {
        int offset = 0;
        int read = 0;
        int remaining = len;
        int block;
        while (remaining != 0) {
            block = Math.min(remaining, MAX_READ);
            try {
                read = f.read(buffer, offset, block);
            } catch (IOException e) {
                Com.Error(Defines.ERR_FATAL, e.toString());
            }
            if (read == 0) {
                Com.Error(Defines.ERR_FATAL, "FS_Read: 0 bytes read");
            } else if (read == -1) {
                Com.Error(Defines.ERR_FATAL, "FS_Read: -1 bytes read");
            }
            remaining -= read;
            offset += read;
        }
    }

    public static byte[] LoadFile(String path) {
        path = HuubHelp.vertalen(path);
        RandomAccessFile file;
        byte[] buf = null;
        int len = 0;
        int index = path.indexOf('\0');
        if (index != -1) {
            path = path.substring(0, index);
        }
        len = FileLength(path);
        if (len < 1) {
            if (HuubHelp.isLog) HuubHelp.log("\t\t!! Onbekend bestand of 0 lengte retoer null : " + path, HuubHelp.MOMENTEEL_BELANGRIJK);
            return null;
        }
        try {
            file = FOpenFile(path);
            buf = new byte[len];
            file.readFully(buf);
            file.close();
        } catch (IOException e) {
            if (HuubHelp.isLog) HuubHelp.log("IOException op : " + path + e.getMessage(), HuubHelp.MOMENTEEL_BELANGRIJK);
            Com.Error(Defines.ERR_FATAL, e.toString());
        } catch (Exception e) {
            if (HuubHelp.isLog) HuubHelp.log("Null pointer op : " + path + e.getMessage(), HuubHelp.MOMENTEEL_BELANGRIJK);
        }
        return buf;
    }

    public static ByteBuffer LoadMappedFile(String filename) {
        searchpath_t search;
        String netpath;
        pack_t pak;
        filelink_t link;
        File file = null;
        int fileLength = 0;
        FileChannel channel = null;
        FileInputStream input = null;
        ByteBuffer buffer = null;
        file_from_pak = 0;
        try {
            for (Iterator it = fs_links.iterator(); it.hasNext(); ) {
                link = (filelink_t) it.next();
                if (filename.regionMatches(0, link.from, 0, link.fromlength)) {
                    netpath = link.to + filename.substring(link.fromlength);
                    file = new File(netpath);
                    if (file.canRead()) {
                        input = new FileInputStream(file);
                        channel = input.getChannel();
                        fileLength = (int) channel.size();
                        buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileLength);
                        input.close();
                        return buffer;
                    }
                    return null;
                }
            }
            for (search = fs_searchpaths; search != null; search = search.next) {
                if (search.pack != null) {
                    pak = search.pack;
                    filename = filename.toLowerCase();
                    packfile_t entry = (packfile_t) pak.files.get(filename);
                    if (entry != null) {
                        file_from_pak = 1;
                        file = new File(pak.filename);
                        if (!file.canRead()) {
                            Com.Error(Defines.ERR_FATAL, "Couldn't reopen " + pak.filename);
                        }
                        if (pak.handle == null || !pak.handle.getFD().valid()) {
                            pak.handle = new RandomAccessFile(pak.filename, "r");
                        }
                        if (pak.backbuffer == null) {
                            channel = pak.handle.getChannel();
                            pak.backbuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, pak.handle.length());
                            channel.close();
                        }
                        pak.backbuffer.position(entry.filepos);
                        buffer = pak.backbuffer.slice();
                        buffer.limit(entry.filelen);
                        return buffer;
                    }
                } else {
                    netpath = search.filename + '/' + filename;
                    file = new File(netpath);
                    if (!file.canRead()) {
                        continue;
                    }
                    input = new FileInputStream(file);
                    channel = input.getChannel();
                    fileLength = (int) channel.size();
                    buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileLength);
                    input.close();
                    return buffer;
                }
            }
        } catch (Exception e) {
        }
        try {
            if (input != null) {
                input.close();
            } else if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (IOException ioe) {
        }
        return null;
    }

    public static void FreeFile(byte[] buffer) {
        buffer = null;
    }

    static final int IDPAKHEADER = (('K' << 24) + ('C' << 16) + ('A' << 8) + 'P');

    static class dpackheader_t {

        int ident;

        int dirofs;

        int dirlen;
    }

    static final int MAX_FILES_IN_PACK = 4096;

    static byte[] tmpText = new byte[packfile_t.NAME_SIZE];

    static pack_t LoadPackFile(String packfile) {
        dpackheader_t header;
        Hashtable newfiles;
        RandomAccessFile file;
        int numpackfiles = 0;
        pack_t pack = null;
        try {
            file = new RandomAccessFile(packfile, "r");
            FileChannel fc = file.getChannel();
            ByteBuffer packhandle = fc.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            packhandle.order(ByteOrder.LITTLE_ENDIAN);
            fc.close();
            if (packhandle == null || packhandle.limit() < 1) {
                return null;
            }
            header = new dpackheader_t();
            header.ident = packhandle.getInt();
            header.dirofs = packhandle.getInt();
            header.dirlen = packhandle.getInt();
            if (header.ident != IDPAKHEADER) {
                Com.Error(Defines.ERR_FATAL, packfile + " is not a packfile");
            }
            numpackfiles = header.dirlen / packfile_t.SIZE;
            if (numpackfiles > MAX_FILES_IN_PACK) {
                Com.Error(Defines.ERR_FATAL, packfile + " has " + numpackfiles + " files");
            }
            newfiles = new Hashtable(numpackfiles);
            packhandle.position(header.dirofs);
            packfile_t entry = null;
            for (int i = 0; i < numpackfiles; i++) {
                packhandle.get(tmpText);
                entry = new packfile_t();
                entry.name = new String(tmpText).trim();
                entry.filepos = packhandle.getInt();
                entry.filelen = packhandle.getInt();
                newfiles.put(entry.name.toLowerCase(), entry);
            }
        } catch (IOException e) {
            Com.DPrintf(e.getMessage() + '\n');
            return null;
        }
        pack = new pack_t();
        pack.filename = new String(packfile);
        pack.handle = file;
        pack.numfiles = numpackfiles;
        pack.files = newfiles;
        Com.Printf("Added packfile " + packfile + " (" + numpackfiles + " files)\n");
        return pack;
    }

    static void AddGameDirectory(String dir) {
        int i;
        searchpath_t search;
        pack_t pak;
        String pakfile;
        fs_gamedir = new String(dir);
        search = new searchpath_t();
        search.filename = new String(dir);
        if (fs_searchpaths != null) {
            search.next = fs_searchpaths.next;
            fs_searchpaths.next = search;
        } else {
            fs_searchpaths = search;
        }
        for (i = 0; i < 10; i++) {
            pakfile = dir + "/pak" + i + ".pak";
            if (!(new File(pakfile).canRead())) {
                continue;
            }
            pak = LoadPackFile(pakfile);
            if (pak == null) {
                continue;
            }
            search = new searchpath_t();
            search.pack = pak;
            search.filename = "";
            search.next = fs_searchpaths;
            fs_searchpaths = search;
        }
    }

    public static String Gamedir() {
        return (fs_userdir != null) ? fs_userdir : Globals.BASEDIRNAME;
    }

    public static String BaseGamedir() {
        return (fs_gamedir != null) ? fs_gamedir : Globals.BASEDIRNAME;
    }

    public static void ExecAutoexec() {
        String dir = fs_userdir;
        String name;
        if (dir != null && dir.length() > 0) {
            name = dir + "/autoexec.cfg";
        } else {
            name = fs_basedir.string + '/' + Globals.BASEDIRNAME + "/autoexec.cfg";
        }
        int canthave = Defines.SFF_SUBDIR | Defines.SFF_HIDDEN | Defines.SFF_SYSTEM;
        if (Sys.FindAll(name, 0, canthave) != null) {
            Cbuf.AddText("exec autoexec.cfg\n");
        }
    }

    public static void SetGamedir(String dir) {
        searchpath_t next;
        if (dir.indexOf("..") != -1 || dir.indexOf("/") != -1 || dir.indexOf("\\") != -1 || dir.indexOf(":") != -1) {
            Com.Printf("Gamedir should be a single filename, not a path\n");
            return;
        }
        while (fs_searchpaths != fs_base_searchpaths) {
            if (fs_searchpaths.pack != null) {
                try {
                    fs_searchpaths.pack.handle.close();
                } catch (IOException e) {
                    Com.DPrintf(e.getMessage() + '\n');
                }
                fs_searchpaths.pack.files.clear();
                fs_searchpaths.pack.files = null;
                fs_searchpaths.pack = null;
            }
            next = fs_searchpaths.next;
            fs_searchpaths = null;
            fs_searchpaths = next;
        }
        if ((Globals.dedicated != null) && (Globals.dedicated.value == 0.0f)) {
            Cbuf.AddText("vid_restart\nsnd_restart\n");
        }
        fs_gamedir = fs_basedir.string + '/' + dir;
        if (dir.equals(Globals.BASEDIRNAME) || (dir.length() == 0)) {
            Cvar.FullSet("gamedir", "", CVAR_SERVERINFO | CVAR_NOSET);
            Cvar.FullSet("game", "", CVAR_LATCH | CVAR_SERVERINFO);
        } else {
            Cvar.FullSet("gamedir", dir, CVAR_SERVERINFO | CVAR_NOSET);
            if (fs_cddir.string != null && fs_cddir.string.length() > 0) {
                AddGameDirectory(fs_cddir.string + '/' + dir);
            }
            AddGameDirectory(fs_basedir.string + '/' + dir);
        }
    }

    public static void Link_f() {
        filelink_t entry = null;
        if (Cmd.Argc() != 3) {
            Com.Printf("USAGE: link <from> <to>\n");
            return;
        }
        for (Iterator it = fs_links.iterator(); it.hasNext(); ) {
            entry = (filelink_t) it.next();
            if (entry.from.equals(Cmd.Argv(1))) {
                if (Cmd.Argv(2).length() < 1) {
                    it.remove();
                    return;
                }
                entry.to = new String(Cmd.Argv(2));
                return;
            }
        }
        if (Cmd.Argv(2).length() > 0) {
            entry = new filelink_t();
            entry.from = new String(Cmd.Argv(1));
            entry.fromlength = entry.from.length();
            entry.to = new String(Cmd.Argv(2));
            fs_links.add(entry);
        }
    }

    public static String[] ListFiles(String findname, int musthave, int canthave) {
        String[] list = null;
        File[] files = Sys.FindAll(findname, musthave, canthave);
        if (files != null) {
            list = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                list[i] = files[i].getPath();
            }
        }
        return list;
    }

    public static void Dir_f() {
        String path = null;
        String findname = null;
        String wildcard = "*.*";
        String[] dirnames;
        if (Cmd.Argc() != 1) {
            wildcard = Cmd.Argv(1);
        }
        while ((path = NextPath(path)) != null) {
            String tmp = findname;
            findname = path + '/' + wildcard;
            if (tmp != null) {
                tmp.replaceAll("\\\\", "/");
            }
            Com.Printf("Directory of " + findname + '\n');
            Com.Printf("----\n");
            dirnames = ListFiles(findname, 0, 0);
            if (dirnames != null) {
                int index = 0;
                for (int i = 0; i < dirnames.length; i++) {
                    if ((index = dirnames[i].lastIndexOf('/')) > 0) {
                        Com.Printf("FS.java : " + dirnames[i].substring(index + 1, dirnames[i].length()) + '\n');
                    } else {
                        Com.Printf("FS.java : " + dirnames[i] + '\n');
                    }
                }
            }
            Com.Printf("\n");
        }
    }

    public static void Path_f() {
        searchpath_t s;
        filelink_t link;
        Com.Printf("Current search path:\n");
        for (s = fs_searchpaths; s != null; s = s.next) {
            if (s == fs_base_searchpaths) {
                Com.Printf("----------\n");
            }
            if (s.pack != null) {
                Com.Printf("FS.java Path_f : " + s.pack.filename + " (" + s.pack.numfiles + " files)\n");
            } else {
                Com.Printf("FS.java Path_f : " + s.filename + '\n');
            }
        }
        Com.Printf("\nLinks:\n");
        for (Iterator it = fs_links.iterator(); it.hasNext(); ) {
            link = (filelink_t) it.next();
            Com.Printf(link.from + " : " + link.to + '\n');
        }
    }

    public static String NextPath(String prevpath) {
        searchpath_t s;
        String prev;
        if (prevpath == null || prevpath.length() == 0) {
            return fs_gamedir;
        }
        prev = fs_gamedir;
        for (s = fs_searchpaths; s != null; s = s.next) {
            if (s.pack != null) {
                continue;
            }
            if (prevpath == prev) {
                return s.filename;
            }
            prev = s.filename;
        }
        return null;
    }

    public static void InitFilesystem() {
        Cmd.AddCommand("path", new xcommand_t() {

            public void execute() {
                Path_f();
            }
        });
        Cmd.AddCommand("link", new xcommand_t() {

            public void execute() {
                Link_f();
            }
        });
        Cmd.AddCommand("dir", new xcommand_t() {

            public void execute() {
                Dir_f();
            }
        });
        fs_userdir = System.getProperty("user.home") + "/punthuub3d";
        Com.Printf("\n------- gebruikers directory voor de configuratie : " + fs_userdir + "\n");
        FS.CreatePath(fs_userdir + "/");
        FS.AddGameDirectory(fs_userdir);
        fs_basedir = Cvar.Get("basedir", "/home/huub/NetBeansProjects/Huub3D/data", CVAR_NOSET);
        setCDDir();
        AddGameDirectory(fs_basedir.string + '/' + Globals.BASEDIRNAME);
        markBaseSearchPaths();
        fs_gamedirvar = Cvar.Get("game", "", CVAR_LATCH | CVAR_SERVERINFO);
        if (fs_gamedirvar.string.length() > 0) {
            SetGamedir(fs_gamedirvar.string);
        }
    }

    /**
   * set data directory
   */
    public static void setCDDir() {
        fs_cddir = Cvar.Get("cddir", "", CVAR_ARCHIVE);
        if (fs_cddir.string.length() > 0) {
            AddGameDirectory(fs_cddir.string);
        }
    }

    static void markBaseSearchPaths() {
        fs_base_searchpaths = fs_searchpaths;
    }

    public static int Developer_searchpath(int who) {
        searchpath_t s;
        for (s = fs_searchpaths; s != null; s = s.next) {
            if (s.filename.indexOf("xatrix") != -1) {
                return 1;
            }
            if (s.filename.indexOf("rogue") != -1) {
                return 2;
            }
        }
        return 0;
    }
}
