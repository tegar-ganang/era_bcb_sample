package passreminder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import passreminder.model.Group;
import passreminder.model.GroupList;
import passreminder.model.Item;
import passreminder.model.ItemList;
import passreminder.model.SyncFile;

public class DBManager {

    private static final String VERSION = "0.1";

    public static final int STX = 2;

    public static final int ETX = 3;

    public static final int GS = 29;

    private static long uniqueId = 1024;

    public File dbFile = null;

    public Map infMain;

    public Map infSecond;

    public ArrayList lastSearches = new ArrayList();

    public ArrayList syncFilenames = new ArrayList();

    public boolean isMetadataSaved = true;

    public boolean needUIRefresh = false;

    public ItemList iListMain;

    public ItemList iListSecond;

    public GroupList gListMain;

    public GroupList gListSecond;

    private String password = null;

    private SimpleDateFormat sdf = null;

    private SimpleDateFormat humanSdf = null;

    private static DBManager me;

    static {
        me = new DBManager();
    }

    public static DBManager getInstance() {
        return me;
    }

    public DBManager() {
        sdf = new SimpleDateFormat("yyyyMMdd HHmm");
        humanSdf = new SimpleDateFormat(Messages.getString("datetime_format"));
        infMain = new HashMap();
        gListMain = new GroupList();
        iListMain = new ItemList();
        infSecond = new HashMap();
        gListSecond = new GroupList();
        iListSecond = new ItemList();
    }

    public String format(Object dt) {
        return humanSdf.format(dt);
    }

    public static long nextId() {
        return ++uniqueId;
    }

    public String getPassword() {
        return password;
    }

    public void close() {
        password = null;
        dbFile = null;
        clear();
    }

    private void clear() {
        infMain.clear();
        infSecond.clear();
        iListMain.clear();
        ModelManager.groupData.children.clear();
        ModelManager.groupTrash.children.clear();
        ModelManager.groupSearch.children.clear();
    }

    private void clearSecond() {
        infSecond.clear();
        iListSecond.clear();
        ModelManager.groupDataSecond.children.clear();
    }

    public void setPassword(String password) {
        this.password = password;
        Cryptography.KEY = password;
    }

    public boolean isDBOpened() {
        return dbFile != null;
    }

    private void write(File file, ItemList iList, GroupList gList) throws PRException {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(file);
            PrintStream ps = new PrintStream(fout);
            try {
                String cs = RandomStringUtils.randomNumeric(10);
                ps.println(Cryptography.encryptBase64(cs));
                ps.print(PassReminder.NAME);
                ps.print((char) GS);
                ps.print(PassReminder.VERSION);
                ps.print((char) GS);
                ps.print(PassReminder.DATE);
                ps.print((char) GS);
                ps.print(PassReminder.PLATFORM);
                ps.print((char) GS);
                ps.println(VERSION);
                for (int i = 0; i < syncFilenames.size(); i++) {
                    SyncFile sfile = ((SyncFile) syncFilenames.get(i));
                    ps.print(sfile.filename);
                    ps.print((char) GS);
                    ps.print(sfile.hostname);
                    if (i < syncFilenames.size() - 1) ps.print((char) GS);
                }
                ps.println();
                for (int i = 0; i < lastSearches.size(); i++) {
                    String word = (String) lastSearches.get(i);
                    try {
                        if (word.trim().length() != 0) {
                            ps.print(Cryptography.encryptBase64(word));
                            if (i < lastSearches.size() - 1) ps.print((char) GS);
                        }
                    } catch (Exception e) {
                    }
                }
                ps.println();
            } catch (Exception e) {
                e.printStackTrace();
                throw new PRException("Error during the item list writing.", e);
            }
            Group[] groupArray = ModelManager.getInstance().flatGroupList(gList);
            try {
                writeSection(ps, "Gr");
                for (int i = 0; i < groupArray.length; i++) {
                    Group group = groupArray[i];
                    if (group.isUser()) {
                        ps.println((char) STX);
                        ps.println(group.timestamp);
                        ps.println(group.id);
                        ps.println(group.parent.id);
                        if (group.columnsSize != null && group.columnsSize.length == 5) {
                            for (int j = 0; j < group.columnsSize.length; j++) {
                                ps.print(group.columnsSize[j]);
                                if (j != (group.columnsSize.length - 1)) ps.print(",");
                            }
                        }
                        ps.println();
                        ps.println(Cryptography.encryptBase64(group.name));
                        ps.println(Cryptography.encryptBase64(group.icon));
                        ps.println((char) ETX);
                    }
                }
                ps.println("-");
            } catch (Exception e) {
                e.printStackTrace();
                throw new PRException("Error during the item list writing.", e);
            }
            try {
                writeSection(ps, "It");
                for (int i = 0; i < iList.size(); i++) {
                    Item item = iList.get(i);
                    ps.println((char) STX);
                    ps.println(item.timestamp);
                    ps.println(item.id);
                    ps.println(item.groupId);
                    ps.println(Cryptography.encryptBase64(item.name));
                    ps.println(Cryptography.encryptBase64(item.icon));
                    ps.println(Cryptography.encryptBase64(item.user));
                    ps.println(Cryptography.encryptBase64(item.password));
                    ps.println(Cryptography.encryptBase64(item.mail));
                    ps.println(Cryptography.encryptBase64(item.command));
                    ps.println(Cryptography.encryptBase64(item.free));
                    ps.println(item.isDeleted);
                    ps.println(Cryptography.encryptBase64(item.dateAccountCreation == null ? "" : sdf.format(item.dateAccountCreation)));
                    ps.println(Cryptography.encryptBase64(item.dateExpiration == null ? "" : sdf.format(item.dateExpiration)));
                    ps.println(Cryptography.encryptBase64(item.dateCreation == null ? "" : sdf.format(item.dateCreation)));
                    ps.println(Cryptography.encryptBase64(item.dateModification == null ? "" : sdf.format(item.dateModification)));
                    ps.println(Cryptography.encryptBase64(item.dateLastAccess == null ? "" : sdf.format(item.dateLastAccess)));
                    ps.println(Cryptography.encryptBase64(item.description));
                    ps.println((char) GS);
                    ps.println((char) ETX);
                    if (item.sync.synced) {
                        needUIRefresh = true;
                    }
                }
                ps.println("-");
            } catch (Exception e) {
                e.printStackTrace();
                throw new PRException("Error during the item list writing.", e);
            }
            try {
                writeSection(ps, "Ui");
                for (int i = 0; i < 0; i++) {
                    ps.println((char) STX);
                    ps.println(ModelManager.getInstance().folderTree2String());
                    ps.println((char) ETX);
                }
                ps.println("-");
            } catch (Exception e) {
                e.printStackTrace();
                throw new PRException("Error during the item list writing.", e);
            }
            try {
                writeSection(ps, "Fi");
                for (int i = 0; i < 0; i++) {
                    ps.println((char) STX);
                    ps.println((1034 + i));
                    ps.println("....");
                    ps.println("....");
                    ps.println("....");
                    ps.println((char) ETX);
                }
                ps.println("-");
            } catch (Exception e) {
                e.printStackTrace();
                throw new PRException("Error during the item list writing.", e);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new PRException("Error during the file storing.", e);
        } finally {
            if (fout != null) try {
                fout.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        isMetadataSaved = true;
    }

    private void writeSection(PrintStream ps, String title) {
        ps.print((char) STX);
        ps.print(title);
        ps.println((char) ETX);
    }

    private Map readHead(BufferedReader fin, Map inf, String password, boolean second) throws PRException, PRCryptoException {
        String cs = null;
        try {
            cs = Cryptography.decryptBase64(fin.readLine(), password);
        } catch (Exception e) {
            e.printStackTrace();
            throw new PRException("Error at the start file reading.", e);
        }
        try {
            Long.parseLong(cs);
        } catch (Exception e) {
            throw new PRCryptoException(Messages.getString("error.decrypt.password"), e);
        }
        try {
            String line = fin.readLine();
            StringBuffer separator = new StringBuffer();
            separator.append((char) GS);
            String el[] = line.split(separator.toString());
            inf.put("passreminder.name", el[0]);
            inf.put("passreminder.version", el[1]);
            inf.put("passreminder.date", el[2]);
            inf.put("passreminder.platform", el[3]);
            inf.put("passreminder.db_version", el[4]);
            line = fin.readLine();
            if (!second) {
                try {
                    syncFilenames.clear();
                    el = line.split(separator.toString());
                    for (int i = 0; i < el.length; i += 2) {
                        syncFilenames.add(new SyncFile(el[i], el[i + 1]));
                    }
                } catch (Exception e) {
                }
            }
            String search = fin.readLine();
            String[] searches = search.split(separator.toString());
            this.lastSearches.clear();
            for (int i = 0; i < searches.length; i++) {
                String sWord = Cryptography.decryptBase64(searches[i], password);
                if (sWord != null) this.lastSearches.add(sWord);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new PRException("Error during the file reading.", e);
        }
        return inf;
    }

    private void readGroupList(BufferedReader fin, GroupList list, String password, boolean second) throws PRException {
        int groupLine = 0;
        try {
            fin.readLine();
            StringBuffer sb = new StringBuffer();
            sb.append((char) GS);
            String line = fin.readLine();
            while (!line.equals("-")) {
                groupLine++;
                Group group = new Group();
                group.timestamp = new Long(fin.readLine()).longValue();
                group.id = new Long(fin.readLine()).longValue();
                long idp = new Long(fin.readLine()).longValue();
                Group parent = ModelManager.getInstance().toGroup(list, idp, second ? ModelManager.groupDataSecond : ModelManager.groupData);
                line = fin.readLine();
                String[] colSize = line.split(",");
                if (colSize.length == 5) {
                    group.columnsSize = new int[colSize.length];
                    for (int j = 0; j < group.columnsSize.length; j++) {
                        group.columnsSize[j] = new Integer(colSize[j]).intValue();
                    }
                }
                group.name = Cryptography.decryptBase64(fin.readLine(), password);
                group.icon = Cryptography.decryptBase64(fin.readLine(), password);
                if (group.id > uniqueId) uniqueId = group.id;
                ModelManager.getInstance().addGroup(parent, group);
                fin.readLine();
                line = fin.readLine();
                if (group.id > uniqueId) uniqueId = group.id;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new PRException(Messages.getString("error.read.group_section", groupLine + ""), e);
        }
    }

    private void readItemList(BufferedReader fin, ItemList list, String password) throws PRException {
        int itemLine = 0;
        try {
            fin.readLine();
            StringBuffer sb = new StringBuffer();
            sb.append((char) GS);
            String line = fin.readLine();
            while (!line.equals("-")) {
                itemLine++;
                Item item = new Item();
                item.timestamp = new Long(fin.readLine()).longValue();
                item.id = new Long(fin.readLine()).longValue();
                item.groupId = new Long(fin.readLine()).longValue();
                item.name = Cryptography.decryptBase64(fin.readLine(), password);
                item.icon = Cryptography.decryptBase64(fin.readLine(), password);
                item.user = Cryptography.decryptBase64(fin.readLine(), password);
                item.password = Cryptography.decryptBase64(fin.readLine(), password);
                item.mail = Cryptography.decryptBase64(fin.readLine(), password);
                item.command = Cryptography.decryptBase64(fin.readLine(), password);
                item.free = Cryptography.decryptBase64(fin.readLine(), password);
                item.isDeleted = Boolean.parseBoolean(fin.readLine());
                try {
                    item.dateAccountCreation = sdf.parse(Cryptography.decryptBase64(fin.readLine(), password));
                } catch (Exception e) {
                }
                try {
                    item.dateExpiration = sdf.parse(Cryptography.decryptBase64(fin.readLine(), password));
                } catch (Exception e) {
                }
                try {
                    item.dateCreation = sdf.parse(Cryptography.decryptBase64(fin.readLine(), password));
                } catch (Exception e) {
                }
                try {
                    item.dateModification = sdf.parse(Cryptography.decryptBase64(fin.readLine(), password));
                } catch (Exception e) {
                }
                try {
                    item.dateLastAccess = sdf.parse(Cryptography.decryptBase64(fin.readLine(), password));
                } catch (Exception e) {
                }
                item.description = Cryptography.decryptBase64(fin.readLine(), password);
                while (true) {
                    line = fin.readLine();
                    if (line.length() == 1 && line.charAt(0) == (char) GS) {
                        line = fin.readLine();
                        if (line.length() == 1 && line.charAt(0) == (char) ETX) break;
                    }
                }
                item.id = nextId();
                if (item.id > uniqueId) uniqueId = item.id;
                list.add(item);
                line = fin.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new PRException(Messages.getString("error.read.item_section", itemLine + ""), e);
        }
    }

    public void readSecond(File dbFile, String password) throws PRException, PRCryptoException {
        clearSecond();
        read(dbFile, password, gListSecond, iListSecond, false);
    }

    public long readMain(File dbFile) throws PRException, PRCryptoException {
        long startT = new Date().getTime();
        read(dbFile, password, gListMain, iListMain, true);
        return new Date().getTime() - startT;
    }

    private void read(File dbFile, String password, GroupList gList, ItemList iList, boolean backup) throws PRException, PRCryptoException {
        BufferedReader fin = null;
        if (backup) this.dbFile = null;
        try {
            if (dbFile == null) throw new PRException("The file cannot be empty.", new NullPointerException());
            fin = new BufferedReader(new FileReader(dbFile));
            readHead(fin, infMain, password, !backup);
            readGroupList(fin, gList, password, !backup);
            readItemList(fin, iList, password);
            if (backup) this.dbFile = dbFile;
            if (backup) copy(new FileInputStream(dbFile), new FileOutputStream(dbFile.getAbsolutePath() + "#"));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fin != null) try {
                fin.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveMetadata() throws PRException, FileNotFoundException, IOException {
        if (!isMetadataSaved) write();
    }

    private void write(File file) throws PRException {
        write(file, iListMain, gListMain);
    }

    public void write() throws PRException, FileNotFoundException, IOException {
        File tmpFile = new File(dbFile.getAbsolutePath() + "~");
        if (!tmpFile.exists()) tmpFile.createNewFile();
        write(tmpFile);
        copy(new FileInputStream(tmpFile), new FileOutputStream(dbFile));
        System.out.println("Save passreminder file.");
    }

    public static void copy(FileInputStream source, FileOutputStream dest) throws IOException {
        FileChannel in = null, out = null;
        try {
            in = source.getChannel();
            out = dest.getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
            out.write(buf);
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }
}
