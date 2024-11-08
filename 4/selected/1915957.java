package com.finchsync.mork;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

/**
 * This class parses a mork-database file and returns a MorkDatabase object as
 * result.
 * 
 * $Author: $
 * <p>
 * $Revision: $
 */
public class MorkParser {

    /**
	 * The whole mork file as char-array.
	 */
    private char[] data;

    /**
	 * The current parsing-position.
	 */
    private int pos;

    /**
	 * The mork database to read.
	 */
    private MorkDatabase morkdb = new MorkDatabase();

    /**
	 * The id of the group, which is currently evaluated. Is '-1' if the parser
	 * is outside a group.
	 */
    private int groupid = -1;

    /**
	 * Creates a new instance of MorkParser.
	 * 
	 * @param data
	 *            the complete mork-file as byte-array.
	 */
    public MorkParser(char[] data) {
        if (data == null || data.length == 0) throw new IllegalArgumentException();
        this.data = data;
        this.pos = 0;
    }

    /**
	 * Creates a MorkParser, reading the mork-file from the specified reader.
	 * The Reader is NOT closed after reading is complete. This must be done by
	 * the caller.
	 * 
	 * @param r
	 *            Reader to read the mork-file from.
	 * @throws IOException
	 *             if an IO problem occurs.
	 */
    public MorkParser(Reader r) throws IOException {
        if (r == null) throw new IllegalArgumentException();
        CharArrayWriter caw = new CharArrayWriter();
        int c;
        while ((c = r.read()) != -1) caw.write(c);
        caw.close();
        data = caw.toCharArray();
        pos = 0;
        if (data == null || data.length == 0) throw new IOException("File is empty.");
    }

    /**
	 * Parses the Mork-file. The mork file is parsed in two phases. The first
	 * phase will scan all dictionaries, the second phase will scan all data
	 * (tables). This is necessary, because the data-entries sometimes referes
	 * to dictionary keys, which may be defined later in the file. This may lead
	 * to unresolved references.
	 * 
	 * @return a MorkDatabase object.
	 * @throws MorkException
	 *             when a parsing error occurs.
	 */
    public MorkDatabase parseMorkFile() throws MorkException {
        String magic = readLine();
        if (!MorkDatabase.morkMagic.equals(magic)) throw new MorkParseException("Not a mork-database.");
        for (int phase = 0; phase < 2; phase++) {
            pos = 0;
            while (pos < data.length) {
                readNextElement(phase);
            }
        }
        return morkdb;
    }

    /**
	 * Reads the next Element from the file.<br>
	 * Parsing of groups is transparanet. The contents of aborted groups is
	 * ignored, commited groups are evaluated as regular part of the file and
	 * their elements returned by this method.<br>
	 * Returned objects are already feed or removed ('-' update) to or from the
	 * MorkDatabase object, so no further action must be taken with the returned
	 * objects.
	 * 
	 * @param phase
	 *            current parsing phase: 0= dictionaries, 1= data (tables &
	 *            rows)
	 * @return the next read element. May be null.
	 * @throws MorkException
	 *             when a parsing error occurs.
	 */
    private Object readNextElement(int phase) throws MorkException {
        while (true) {
            skipS();
            if (pos >= data.length) return null;
            if (data[pos] == '@' && data[pos + 1] == '$' && data[pos + 2] == '$' && data[pos + 3] == '}') {
                pos += 4;
                int id = readHexValue(-1);
                if (groupid != id) throw new MorkParseException("Error parsing group: Unexpected end of group found at pos " + pos + "'");
                if (data[pos] != '}' || data[pos + 1] != '@') throw new MorkParseException("Error parsing group: '}@' expected at pos " + pos + "'");
                pos += 2;
                groupid = -1;
                continue;
            }
            if (data[pos] == '@' && data[pos + 1] == '$' && data[pos + 2] == '$' && data[pos + 3] == '{') {
                pos += 4;
                int id = readHexValue(-1);
                if (data[pos] != '{' || data[pos + 1] != '@') throw new MorkParseException("Error parsing group:'{@' expected at pos " + pos + "'");
                pos += 2;
                int oldpos = pos;
                while (data[pos] != '@' || data[pos + 1] != '$' || data[pos + 2] != '$' || data[pos + 3] != '}') pos++;
                pos += 4;
                if (data[pos] == '~' && data[pos + 1] == '~') {
                    pos += 4;
                } else {
                    int id2 = readHexValue(-1);
                    if (id != id2) throw new MorkParseException("Error parsing group: End ID '" + id2 + "' does not match Start-ID '" + id + "'  at pos " + pos + "'");
                    if (data[pos] != '}' || data[pos + 1] != '@') throw new MorkParseException("Error parsing group: '}@' expected at pos " + pos + "'");
                    pos = oldpos;
                    groupid = id;
                }
                continue;
            }
            if (data[pos] == '<') {
                if (phase != 0) {
                    skipDictionary();
                    continue;
                }
                return readDictionary();
            }
            if (data[pos] == '{' || (data[pos] == '-' && data[pos + 1] == '{')) {
                if (phase != 1) {
                    skipTable();
                    continue;
                }
                return readTable();
            }
            if (data[pos] == '[' || (data[pos] == '-' && data[pos + 1] == '[')) {
                if (phase != 1) {
                    skipRow();
                    continue;
                }
                return readRow(null);
            }
            throw new MorkParseException("Unknown Element at pos '" + pos + "'");
        }
    }

    /**
	 * Reads a table from the current position. The table is added to the
	 * database.<br>
	 * A Table may be a 'cut'-update, which means the table will be removed from
	 * the Database. In this case, 'null' is returned. So 'null' is not a
	 * parsing error. Parsing-problems will result in a MorkParseException.
	 * 
	 * @return the read table,or null, if the table was marked with '-' (update
	 *         cut table).
	 * @throws MorkException
	 *             when a parsing error occurs.
	 */
    private MorkTable readTable() throws MorkException {
        boolean deletetable = false;
        boolean cleartable = false;
        String tnamespace;
        if (data[pos] == '-') {
            deletetable = true;
            pos++;
        }
        if (data[pos] != '{') throw new MorkParseException("Error parsing table:'{' expected at pos " + pos + "'");
        pos++;
        skipS();
        if (data[pos] == '-') {
            cleartable = true;
            pos++;
        }
        MorkOID oid = this.readOID(null);
        tnamespace = oid.getNamespace();
        MorkTable mt = morkdb.getTable(oid);
        if (mt == null) mt = new MorkTable(oid, morkdb);
        if (cleartable) mt.clearAllRows();
        while (true) {
            skipS();
            if (data[pos] == '}') break;
            if (data[pos] == '{') {
                MorkCell[] c = readMetaInfo('}');
                if (c != null) mt.addMetaInfo(c);
                continue;
            } else if (data[pos] == '[' || data[pos] == '-') {
                MorkRow mr = readRow(mt);
                if (mr != null) mt.addRow(mr);
            } else if (Character.digit(data[pos], 16) >= 0) {
                MorkOID rid = readOID(tnamespace);
                MorkRow mr = morkdb.getRow(rid);
                if (mr == null) mr = new MorkRow(rid, morkdb);
                mt.addRow(mr);
            } else throw new MorkParseException("Error parsing table: Unknown char '" + data[pos] + "' at pos " + pos + "'");
        }
        pos++;
        if (deletetable == true) morkdb.removeTable(mt.getOID());
        return mt;
    }

    /**
	 * Skips the next table.
	 * 
	 * @throws MorkException
	 *             when a parsing error occurs.
	 */
    private void skipTable() throws MorkException {
        String tnamespace;
        if (data[pos] == '-') {
            pos++;
        }
        if (data[pos] != '{') throw new MorkParseException("Error parsing table:'{' expected at pos " + pos + "'");
        pos++;
        skipS();
        if (data[pos] == '-') {
            pos++;
        }
        MorkOID oid = this.readOID("dummy");
        tnamespace = oid.getNamespace();
        while (true) {
            skipS();
            if (data[pos] == '}') break;
            if (data[pos] == '{') {
                skipMetaInfo('}');
                continue;
            } else if (data[pos] == '[' || data[pos] == '-') {
                skipRow();
            } else if (Character.digit(data[pos], 16) >= 0) {
                @SuppressWarnings("unused") MorkOID rid = readOID(tnamespace);
            } else throw new MorkParseException("Error parsing table: Unknown char '" + data[pos] + "' at pos " + pos + "'");
        }
        pos++;
    }

    /**
	 * Reads a row from the current position. The row is added to the database.
	 * A Row may be a 'cut'-update, which means the row will be removed from the
	 * Database. In this case, 'null' is returned. So 'null' is not a parsing
	 * error. Parsing-problems will result in a MorkParseException.
	 * 
	 * @param mt
	 *            reference to the enclosing table or null.
	 * @return the read row, or null, if the row was marked with '-' (update cut
	 *         row).
	 * @throws MorkException
	 *             when a parsing error occurs.
	 */
    private MorkRow readRow(MorkTable mt) throws MorkException {
        boolean deleterow = false;
        boolean clearrow = false;
        if (data[pos] == '-') {
            deleterow = true;
            pos++;
            skipS();
        }
        if (data[pos] != '[') throw new MorkParseException("Error parsing row:'[' expected at pos " + pos + "'");
        pos++;
        skipS();
        if (data[pos] == '-') {
            clearrow = true;
            pos++;
        }
        MorkOID oid;
        if (mt != null) oid = this.readOID(mt.getOID().getNamespace()); else oid = this.readOID(null);
        MorkRow mr = morkdb.getRow(oid);
        if (mr == null) {
            mr = new MorkRow(oid, morkdb);
            morkdb.addRow(mr);
        }
        if (clearrow) mr.clearAllAttributes();
        while (true) {
            skipS();
            if (data[pos] == ']') break;
            if (data[pos] == '[') {
                MorkCell[] c = readMetaInfo(']');
                if (c != null) mt.addMetaInfo(c);
                continue;
            } else if (data[pos] == '(') {
                MorkCell mc = readCell();
                if (mc != null) mr.setAttribute(mc.getColumn(), mc.getValue());
            } else throw new MorkParseException("Error parsing table: Unknown char '" + data[pos] + "' at pos " + pos + "'");
        }
        pos++;
        if (deleterow == true) {
            morkdb.removeRow(oid);
            return null;
        }
        return mr;
    }

    /**
	 * Skips the next row.
	 * 
	 * @throws MorkException
	 *             when a parsing error occurs.
	 */
    private void skipRow() throws MorkException {
        if (data[pos] == '-') {
            pos++;
            skipS();
        }
        if (data[pos] != '[') throw new MorkParseException("Error parsing row:'[' expected at pos " + pos + "'");
        pos++;
        skipS();
        if (data[pos] == '-') {
            pos++;
        }
        @SuppressWarnings("unused") MorkOID oid = this.readOID("dummy");
        while (true) {
            skipS();
            if (data[pos] == ']') break;
            if (data[pos] == '[') {
                skipMetaInfo(']');
                continue;
            } else if (data[pos] == '(') {
                skipCell();
            } else throw new MorkParseException("Error parsing table: Unknown char '" + data[pos] + "' at pos " + pos + "'");
        }
        pos++;
        return;
    }

    /**
	 * Read a MorkDictionary from the current position. The dictionary is added
	 * to the database.
	 * 
	 * @return the read dictionary.
	 * @throws MorkException
	 *             when a parsing error occurs.
	 */
    private MorkDictionary readDictionary() throws MorkException {
        if (data[pos] != '<') throw new MorkParseException("Error parsing dictionary:'<' expected at pos " + pos + "'");
        pos++;
        MorkDictionary md = new MorkDictionary();
        while (true) {
            skipS();
            if (data[pos] == '>') break;
            if (data[pos] == '<') {
                MorkCell[] c = readMetaInfo('>');
                if (c != null) md.addMetaInfo(c);
                continue;
            } else if (data[pos] == '(') {
                pos++;
                String hv = readName();
                int id = Integer.parseInt(hv, 16);
                skipS();
                if (data[pos] != '=') throw new MorkParseException("Error parsing dictionary:'=' expected at pos " + pos + "'");
                pos++;
                skipS();
                String al = this.readValueString();
                md.setAlias(id, al);
                if (data[pos] != ')') throw new MorkParseException("Error parsing dictionary:')' expected at pos " + pos + "'");
                pos++;
                continue;
            } else throw new MorkParseException("Error parsing dictionary: Unknown char '" + data[pos] + "' at pos " + pos + "'");
        }
        pos++;
        String namespace = md.getMetaInfoAttribute("a");
        if (namespace == null) namespace = "a";
        morkdb.addDictionary(namespace, md);
        return md;
    }

    /**
	 * Skips the next dictionary.
	 * 
	 * @throws MorkException
	 *             when a parsing error occurs.
	 */
    private void skipDictionary() throws MorkException {
        if (data[pos] != '<') throw new MorkParseException("Error parsing dictionary:'<' expected at pos " + pos + "'");
        pos++;
        while (true) {
            skipS();
            if (data[pos] == '>') break;
            if (data[pos] == '<') {
                skipMetaInfo('>');
                continue;
            } else if (data[pos] == '(') {
                pos++;
                String hv = readName();
                @SuppressWarnings("unused") int id = Integer.parseInt(hv, 16);
                skipS();
                if (data[pos] != '=') throw new MorkParseException("Error parsing dictionary:'=' expected at pos " + pos + "'");
                pos++;
                skipS();
                @SuppressWarnings("unused") String al = this.readValueString();
                if (data[pos] != ')') throw new MorkParseException("Error parsing dictionary:')' expected at pos " + pos + "'");
                pos++;
                continue;
            } else throw new MorkParseException("Error parsing dictionary: Unknown char '" + data[pos] + "' at pos " + pos + "'");
        }
        pos++;
        return;
    }

    /**
	 * Reads meta-info from the current position.
	 * 
	 * @param cend
	 *            the closing brace char for this meta-info.
	 * @return all read cells.
	 * @throws MorkException
	 *             when a parsing error occurs.
	 */
    private MorkCell[] readMetaInfo(char cend) throws MorkException {
        Vector<MorkCell> v = new Vector<MorkCell>();
        pos++;
        while (data[pos] != cend) {
            skipS();
            MorkCell c = readCell();
            v.add(c);
        }
        pos++;
        if (v.size() == 0) return null;
        MorkCell[] result = new MorkCell[v.size()];
        for (int i = 0; i < result.length; i++) result[i] = v.get(i);
        return result;
    }

    /**
	 * Skips meta-info from the current position.
	 * 
	 * @throws MorkException
	 *             when a parsing error occurs.
	 */
    private void skipMetaInfo(char cend) throws MorkException {
        pos++;
        while (data[pos] != cend) {
            skipS();
            skipCell();
        }
        pos++;
        return;
    }

    /**
	 * Reads a cell from the current position.
	 * 
	 * @return the red cell.
	 * @throws MorkException
	 *             when a parsing error occurs.
	 */
    private MorkCell readCell() throws MorkException {
        if (data[pos] != '(') throw new MorkParseException("Error parsing cell: '(' expected at pos " + pos + "'");
        pos++;
        String c = readColumn();
        skipS();
        String v = readValue();
        if (data[pos] != ')') throw new MorkParseException("Error parsing cell: ')' expected at pos " + pos + "'");
        pos++;
        return new MorkCell(c, v);
    }

    /**
	 * Skips the next cell.
	 * 
	 * @throws MorkException
	 *             when a parsing error occurs.
	 */
    private void skipCell() throws MorkException {
        if (data[pos] != '(') throw new MorkParseException("Error parsing cell: '(' expected at pos " + pos + "'");
        pos++;
        skipColumn();
        skipS();
        skipValue();
        if (data[pos] != ')') throw new MorkParseException("Error parsing cell: ')' expected at pos " + pos + "'");
        pos++;
        return;
    }

    /**
	 * Reads a column-name from the actual position. The name is either a string
	 * or an OID, which is resolved to the name.<br>
	 * Example: (columnname...), (^1fe....)<br>
	 * 
	 * @return the columnname.
	 * @throws MorkException
	 *             if a parsing problem occurs.
	 */
    private String readColumn() throws MorkException {
        String col;
        if (data[pos] == '^') {
            pos++;
            MorkOID mid = readOID("c");
            String ns = mid.getNamespace();
            col = morkdb.resolveAliasID(mid.getId(), ns);
            return col;
        }
        col = readName();
        if (col == null) throw new MorkParseException("Error parsing column: Can't read name at pos " + pos + "'");
        return col;
    }

    /**
	 * Skips the next column name.
	 * 
	 * @throws MorkException
	 *             if a parsing problem occurs.
	 */
    private void skipColumn() throws MorkException {
        String col;
        if (data[pos] == '^') {
            pos++;
            @SuppressWarnings("unused") MorkOID mid = readOID("c");
            return;
        }
        col = readName();
        if (col == null) throw new MorkParseException("Error parsing column: Can't read name at pos " + pos + "'");
        return;
    }

    /**
	 * Reads a value from the actual position. The value is either a string
	 * (starting with '=') or an OID, which is resolved to a value.<br>
	 * Example: (columnname=This is the value), (columnname^22e)<br>
	 * 
	 * @return the columnname.
	 * @throws MorkException
	 *             if a parsing problem occurs.
	 */
    private String readValue() throws MorkException {
        String val;
        if (data[pos] == '^') {
            pos++;
            MorkOID mid = readOID("a");
            String ns = mid.getNamespace();
            val = morkdb.resolveAliasID(mid.getId(), ns);
            return val;
        }
        if (data[pos] != '=') throw new MorkParseException("Error parsing value: '=' expected at pos " + pos + "'");
        pos++;
        val = readValueString();
        return val;
    }

    /**
	 * Skips the next value.
	 * 
	 * @throws MorkException
	 *             if a parsing problem occurs.
	 */
    private void skipValue() throws MorkException {
        if (data[pos] == '^') {
            pos++;
            @SuppressWarnings("unused") MorkOID mid = readOID("a");
            return;
        }
        if (data[pos] != '=') throw new MorkParseException("Error parsing value: '=' expected at pos " + pos + "'");
        pos++;
        readValueString();
        return;
    }

    /**
	 * Reads a OID, which is a int id with an optional namespace.<br>
	 * The int id is a hex-value, starting with '^'. The Namespace, if
	 * specified, starts with ':', followed by a String, or an other hex-value
	 * to lookup the namespace in column-namespace.<br>
	 * Example: ^fe, ^fe:values, ^fe:^1f<br>
	 * 
	 * @param defaultnamespace
	 *            to use, if no namespace is specified in the file.
	 * @return the read MorkOID, the namespace may be null, if not specified.
	 * @throws MorkException
	 *             if a parsing problem occurs.
	 */
    private MorkOID readOID(String defaultnamespace) throws MorkException {
        int id = readHexValue(-1);
        String namespace = null;
        if (data[pos] == ':') {
            pos++;
            if (data[pos] == '^') {
                pos++;
                int h = readHexValue(-1);
                namespace = morkdb.resolveAliasID(h, "c");
            } else namespace = readName();
        }
        if (namespace == null) namespace = defaultnamespace;
        if (namespace == null) throw new MorkParseException("Error parsing oid: No namespace found and no default set at pos " + pos + "'");
        return new MorkOID(id, namespace);
    }

    /**
	 * Reads a String from the current position until a closing brace ')' is
	 * found.<br>
	 * Escaped chars are evaluated. These are: '\\','\$', '\)' and '$xx' where
	 * xx is a hex-number specifing the ascii code.<br>
	 * 
	 * @return the read String.
	 * @throws MorkParseException
	 *             if a parsing problem occurs.
	 */
    private String readValueString() throws MorkParseException {
        StringBuffer sb = new StringBuffer();
        while (true) {
            char c = data[pos];
            if (c == ')') break;
            if (c == '\\') {
                if (data[pos + 1] == 0x0a || data[pos + 1] == 0x0d) {
                    skipS();
                    continue;
                }
                sb.append(data[pos + 1]);
                pos += 2;
                continue;
            } else if (c == '$') {
                pos++;
                int h = readHexValue(2);
                skipS();
                if (h < 0x7F || data[pos] != '$') {
                    sb.append((char) h);
                    continue;
                }
                if ((h & 0xe0) == 0xc0) {
                    skipS();
                    if (data[pos] != '$') throw new MorkParseException("Error parsing valuestring: Invalid UTF8 sequence, missing second byte at pos " + pos + "'");
                    pos++;
                    int h2 = readHexValue(2);
                    if ((h2 & 0xc0) != 0x80) throw new MorkParseException("Error parsing valuestring: Invalid UTF8 sequence, invalid second byte at pos " + pos + "'");
                    int res = (h & 0x1f) << 6;
                    res += h2 & 0x3f;
                    sb.append((char) res);
                    continue;
                }
                if ((h & 0xf0) == 0xe0) {
                    skipS();
                    if (data[pos] != '$') throw new MorkParseException("Error parsing valuestring: Invalid UTF8 sequence, missing second byte at pos " + pos + "'");
                    pos++;
                    int h2 = readHexValue(2);
                    if ((h2 & 0xc0) != 0x80) throw new MorkParseException("Error parsing valuestring: Invalid UTF8 sequence, invalid second byte at pos " + pos + "'");
                    skipS();
                    if (data[pos] != '$') throw new MorkParseException("Error parsing valuestring: Invalid UTF8 sequence, missing third byte at pos " + pos + "'");
                    pos++;
                    int h3 = readHexValue(2);
                    if ((h3 & 0xc0) != 0x80) throw new MorkParseException("Error parsing valuestring: Invalid UTF8 sequence, invalid third byte at pos " + pos + "'");
                    int res = (h & 0x0f) << 12;
                    res += (h2 & 0x3f) << 6;
                    res += (h3 & 0x3f);
                    sb.append((char) res);
                    continue;
                }
                throw new MorkParseException("Error parsing valuestring: Invalid UTF8 sequence at pos " + pos + "'");
            } else {
                sb.append(c);
                pos++;
                continue;
            }
        }
        if (sb.length() == 0) return null;
        return sb.toString();
    }

    /**
	 * Reads a name, which is a string, containing only chars, digits and
	 * underscores.
	 * 
	 * @return the read String.
	 */
    private String readName() {
        StringBuffer sb = new StringBuffer();
        while (true) {
            char c = data[pos];
            if (Character.isLetterOrDigit(c) || c == '_') {
                sb.append(c);
                pos++;
            } else break;
        }
        if (sb.length() == 0) return null;
        return sb.toString();
    }

    /**
	 * Reads a Hex value with the specified length of digits. If len is set to
	 * '-1', digits are read until a non hex-char is found. ( This method will
	 * read up to 8 digits maximum).<br>
	 * The read position must be at the first hex-char.
	 * 
	 * @return read value.
	 * @param len
	 *            length of hex-number to read.
	 * @throws MorkParseException
	 *             if a parsing problem occurs.
	 */
    private int readHexValue(int len) throws MorkParseException {
        if (len == 0 || len < -1 || len > 8) throw new RuntimeException();
        StringBuffer b = new StringBuffer();
        if (len > 0) {
            while (len-- > 0) b.append(data[pos++]);
        } else {
            len = 0;
            while (Character.digit(data[pos], 16) >= 0) {
                b.append(data[pos++]);
                len++;
                if (len > 8) throw new MorkParseException("Error parsing HexValue: More than 8 digits read at pos " + pos + "'");
            }
        }
        return Integer.parseInt(b.toString(), 16);
    }

    /**
	 * Skips S (space).<br>
	 * zm:S ::= (#x20 | #x9 | #xA | #xD | zm:Continue | zm:Comment)+ <br>
	 * zm:Comment ::= '//' zm:NonCRLF* zm:LineEnd <br>
	 * zm:Continue ::= '\' zm:LineEnd <br>
	 */
    private void skipS() {
        while (true) {
            if (pos >= data.length) return;
            if (data[pos] == 0x20) {
                pos++;
                continue;
            }
            if (data[pos] == 0x9) {
                pos++;
                continue;
            }
            if (data[pos] == 0xa) {
                pos++;
                continue;
            }
            if (data[pos] == 0xd) {
                pos++;
                continue;
            }
            int le;
            if (data[pos] == '\\' && ((le = checkLineEnd(pos + 1)) > 0)) {
                pos += le + 1;
                continue;
            }
            if (data[pos] == '/' && data[pos + 1] == '/') {
                pos += 2;
                while ((le = checkLineEnd(pos)) == 0) pos++;
                pos += le;
                continue;
            }
            break;
        }
    }

    /**
	 * Reads all characters from the current position to 'LineEnd'.<br>
	 * The LineEnd mark is not included in the result, and the position is after
	 * the LineEnd mark after the call.<br>
	 * 
	 * @return the read line.
	 */
    private String readLine() {
        StringBuffer b = new StringBuffer();
        int le;
        while ((le = checkLineEnd(pos)) == 0) b.append(data[pos++]);
        pos += le;
        return b.toString();
    }

    /**
	 * Checks, if there is a LineEnd at the specified position.<br>
	 * zm:LineEnd ::= #xA #xD | #xD #xA | #xA | #xD<br>
	 * 
	 * @param p
	 *            position to check
	 * @return Length of the LineEnd element found ( 1 or 2 chars), or 0, if
	 *         there is no LineEnd ahead.
	 */
    private int checkLineEnd(int p) {
        if (data[p] == 0xa && data[p + 1] == 0xd) return 2;
        if (data[p] == 0xd && data[p + 1] == 0xa) return 2;
        if (data[p] == 0xa) return 1;
        if (data[p] == 0xd) return 1;
        return 0;
    }
}
