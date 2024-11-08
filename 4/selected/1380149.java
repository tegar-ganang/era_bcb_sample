package org.mpn.contacts.importer;

import org.apache.log4j.Logger;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.EOFException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.nio.charset.Charset;

/**
 * todo [!] Create javadocs for org.mpn.contacts.ui.ImportMirandaDb here
 *
 * @author <a href="mailto:pmoukhataev@jnetx.ru">Pavel Moukhataev</a>
 * @version $Revision$
 */
public class ImportMirandaDb extends ImportMiranda {

    static final Logger log = Logger.getLogger(ImportMirandaDb.class);

    Map<Integer, DBModuleName> moduleNames = new HashMap<Integer, DBModuleName>();

    private final class DBHeader {

        private static final int SIGNATURE_LENGTH = 16;

        byte[] signature;

        int version;

        int ofsFileEnd;

        int slackSpace;

        int contactCount;

        int ofsFirstContact;

        int ofsUser;

        int ofsFirstModuleName;

        public void write() throws IOException {
            writeBytes(signature);
            writeDWord(version);
            writeDWord(ofsFileEnd);
            writeDWord(slackSpace);
            writeDWord(contactCount);
            writeDWord(ofsFirstContact);
            writeDWord(ofsUser);
            writeDWord(ofsFirstModuleName);
        }

        public void read() throws IOException {
            signature = readBytes(SIGNATURE_LENGTH);
            version = readDWord();
            ofsFileEnd = readDWord();
            slackSpace = readDWord();
            contactCount = readDWord();
            ofsFirstContact = readDWord();
            ofsUser = readDWord();
            ofsFirstModuleName = readDWord();
        }
    }

    private static final int DBCONTACT_SIGNATURE = 0x43DECADE;

    private final class DBContact {

        int signature;

        int ofsNext;

        int ofsFirstSettings;

        int eventCount;

        int ofsFirstEvent, ofsLastEvent;

        int ofsFirstUnreadEvent;

        int timestampFirstUnread;

        public void write() throws IOException {
            writeDWord(signature);
            writeDWord(ofsNext);
            writeDWord(ofsFirstSettings);
            writeDWord(eventCount);
            writeDWord(ofsFirstEvent);
            writeDWord(ofsLastEvent);
            writeDWord(ofsFirstUnreadEvent);
            writeDWord(timestampFirstUnread);
        }

        public void read() throws IOException {
            signature = readDWord();
            if (signature != DBCONTACT_SIGNATURE) {
                log.error("DBContact Signature is incorrect : " + signature);
            }
            ofsNext = readDWord();
            ofsFirstSettings = readDWord();
            eventCount = readDWord();
            ofsFirstEvent = readDWord();
            ofsLastEvent = readDWord();
            ofsFirstUnreadEvent = readDWord();
            timestampFirstUnread = readDWord();
        }
    }

    private static final int DBMODULENAME_SIGNATURE = 0x4DDECADE;

    private final class DBModuleName {

        int signature;

        int ofsNext;

        String name;

        public void write() throws IOException {
            writeDWord(signature);
            writeDWord(ofsNext);
            writeAscii(name);
        }

        public void read() throws IOException {
            signature = readDWord();
            if (signature != DBMODULENAME_SIGNATURE) {
                log.error("DBModuleName Signature is incorrect : " + signature);
            }
            ofsNext = readDWord();
            name = readAscii();
        }
    }

    private static final int DBVT_DELETED = 0;

    private static final int DBVT_BYTE = 1;

    private static final int DBVT_WORD = 2;

    private static final int DBVT_DWORD = 4;

    private static final int DBVT_ASCIIZ = 255;

    private static final int DBVT_BLOB = 254;

    private static final int DBVT_UTF8 = 253;

    private static final int DBVT_WCHAR = 252;

    private static final int DBVTF_VARIABLELENGTH = 0x80;

    private static final int DBVTF_DENYUNICODE = 0x10000;

    private static final int DBCONTACTSETTINGS_SIGNATURE = 0x53DECADE;

    private final class DBSetting {

        String szName;

        int dataType;

        public void write() throws IOException {
            byte[] nameBytes = szName.getBytes();
            writeByte(nameBytes.length);
            writeBytes(nameBytes);
            writeByte(dataType);
        }

        public void read(String settingName, PropertiesGroup propertiesGroup) throws IOException {
            szName = settingName;
            dataType = readByte();
            int value;
            switch(dataType) {
                case DBVT_DELETED:
                    break;
                case DBVT_BYTE:
                    value = readByte();
                    propertiesGroup.addInteger(settingName, value);
                    break;
                case DBVT_WORD:
                    value = readWord();
                    propertiesGroup.addInteger(settingName, value);
                    break;
                case DBVT_DWORD:
                    value = readDWord();
                    propertiesGroup.addInteger(settingName, value);
                    break;
                case DBVT_BLOB:
                    int blobLength = readWord();
                    skip(blobLength);
                    break;
                case DBVT_ASCIIZ:
                    String asciiString = readString(null);
                    propertiesGroup.addString(settingName, asciiString);
                    break;
                case DBVT_UTF8:
                    String utf8string = readString(UTF8_CHARSET);
                    propertiesGroup.addString(settingName, utf8string);
                    break;
                default:
                    log.error("Unknown data type " + dataType + " for field '" + szName + "'");
                    break;
            }
        }
    }

    private final class DBContactSettings {

        int signature;

        int ofsNext;

        int ofsModuleName;

        int cbBlob;

        DBSetting[] dbSettings;

        DBModuleName moduleName;

        public void write() throws IOException {
            writeDWord(signature);
            writeDWord(ofsNext);
            writeDWord(ofsModuleName);
            writeDWord(cbBlob);
            for (DBSetting dbSetting : dbSettings) {
                dbSetting.write();
            }
            writeByte(0);
        }

        public void read(Map<String, PropertiesGroup> contactProperties) throws IOException {
            signature = readDWord();
            if (signature != DBCONTACTSETTINGS_SIGNATURE) {
                log.error("DBContactSettings Signature is incorrect : " + signature);
            }
            ofsNext = readDWord();
            ofsModuleName = readDWord();
            cbBlob = readDWord();
            PropertiesGroup propertiesGroup = new PropertiesGroup();
            List<DBSetting> dbSettingsList = new ArrayList<DBSetting>();
            while (true) {
                DBSetting dbSetting = new DBSetting();
                String settingName = readAscii();
                if (settingName.length() == 0) break;
                dbSetting.read(settingName, propertiesGroup);
                dbSettingsList.add(dbSetting);
            }
            dbSettings = dbSettingsList.toArray(new DBSetting[dbSettingsList.size()]);
            moduleName = moduleNames.get(ofsModuleName);
            if (moduleName == null) {
                moduleName = new DBModuleName();
                seek(ofsModuleName);
                moduleName.read();
                moduleNames.put(ofsModuleName, moduleName);
            }
            contactProperties.put(moduleName.name, propertiesGroup);
        }
    }

    private static final int DBEVENT_SIGNATURE = 0x45DECADE;

    private final class DBEvent {

        int signature;

        int ofsPrev, ofsNext;

        int ofsModuleName;

        int timestamp;

        int flags;

        int eventType;

        byte[] blob;

        public void writeExternal(DataOutput out) throws IOException {
            writeDWord(signature);
            writeDWord(ofsPrev);
            writeDWord(ofsNext);
            writeDWord(ofsModuleName);
            writeDWord(timestamp);
            writeDWord(flags);
            out.writeShort(eventType);
            out.write(blob.length);
            out.write(blob);
        }

        public void readExternal(DataInput in) throws IOException {
            signature = readDWord();
            if (signature != DBEVENT_SIGNATURE) {
                log.error("DBEvent Signature is incorrect : " + signature);
            }
            ofsPrev = readDWord();
            ofsNext = readDWord();
            ofsModuleName = readDWord();
            timestamp = readDWord();
            flags = readDWord();
            eventType = in.readUnsignedShort();
            int cbBlob = readDWord();
            blob = new byte[cbBlob];
            in.readFully(blob);
        }
    }

    private RandomAccessFile in;

    public void skip(int length) throws IOException {
        in.skipBytes(length);
    }

    public void seek(long pos) throws IOException {
        in.seek(pos);
    }

    public byte[] readBytes(int length) throws IOException {
        byte[] array = new byte[length];
        in.readFully(array);
        return array;
    }

    public int readDWord() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
        return ((ch1) + (ch2 << 8) + (ch3 << 16) + (ch4 << 24));
    }

    public int readWord() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0) throw new EOFException();
        return (ch1) + (ch2 << 8);
    }

    public int readByte() throws IOException {
        int ch = in.read();
        if (ch < 0) throw new EOFException();
        return ch;
    }

    public String readAscii() throws IOException {
        int length = readByte();
        byte[] stringBytes = readBytes(length);
        String string = new String(stringBytes);
        return string;
    }

    public String readString(Charset charset) throws IOException {
        int length = readWord();
        byte[] stringBytes = readBytes(length);
        String string = new String(stringBytes, charset == null ? determineLineCharset(stringBytes, 0, stringBytes.length) : charset);
        return string;
    }

    public void writeAscii(String string) throws IOException {
        writeByte(string.length());
        writeBytes(string.getBytes());
    }

    public void writeBytes(byte[] array) throws IOException {
        in.write(array);
    }

    public void writeDWord(int v) throws IOException {
        in.write((v) & 0xFF);
        in.write((v >>> 8) & 0xFF);
        in.write((v >>> 18) & 0xFF);
        in.write((v >>> 24) & 0xFF);
    }

    public final void writeWord(int v) throws IOException {
        in.write((v) & 0xFF);
        in.write((v >>> 8) & 0xFF);
    }

    public final void writeByte(int v) throws IOException {
        in.write(v);
    }

    public void doImport(File contactFile, boolean importGroups) throws IOException {
        setCreateGroup(importGroups);
        in = new RandomAccessFile(contactFile, "r");
        DBHeader dbHeader = new DBHeader();
        dbHeader.read();
        DBContact[] dbContacts = new DBContact[dbHeader.contactCount];
        long contactPos = dbHeader.ofsFirstContact;
        for (int i = 0; i < dbContacts.length; i++) {
            in.seek(contactPos);
            DBContact dbContact = readContact();
            dbContacts[i] = dbContact;
            contactPos = dbContact.ofsNext;
            if (contactPos == 0) {
                log.debug("Last contact or user contact. i = " + i + ", dbContacts.length: " + dbContacts.length);
            }
        }
        in.close();
    }

    private DBContact readContact() throws IOException {
        DBContact dbContact = new DBContact();
        dbContact.read();
        Map<String, PropertiesGroup> contactProperties = new HashMap<String, PropertiesGroup>();
        long settingPos = dbContact.ofsFirstSettings;
        while (true) {
            DBContactSettings dbContactSettings = new DBContactSettings();
            in.seek(settingPos);
            dbContactSettings.read(contactProperties);
            settingPos = dbContactSettings.ofsNext;
            if (settingPos == 0) {
                break;
            }
        }
        parseContact(contactProperties);
        return dbContact;
    }

    public static void main(String[] args) throws Exception {
        File contactFile = new File("C:\\Projects\\jContacts\\.data\\test\\mirandaDbImport\\pavelmoukhataev.dat");
        new ImportMirandaDb().doImport(contactFile, false);
    }
}
