package com.ggvaidya.TaxonDNA.DNA.formats;

import java.io.*;
import java.util.*;
import com.ggvaidya.TaxonDNA.Common.*;
import com.ggvaidya.TaxonDNA.DNA.*;

public class TNTFile extends BaseFormatHandler {

    private static final int MAX_TAXON_LENGTH = 31;

    private static final int INTERLEAVE_AT = 80;

    private static final int GROUP_CHARSET = 1;

    private static final int GROUP_TAXONSET = 2;

    /** Returns the extension. We'll go with '.fas' as our semi-official DOS-compat extension */
    public String getExtension() {
        return "tnt";
    }

    /**
	 * Returns a valid OTU (Operation Taxonomic Unit); that is, a taxon name.
	 */
    public String getTNTName(String name, int len) {
        char first = name.charAt(0);
        if ((first >= 'A' && first <= 'Z') || (first >= 'a' && first <= 'z') || (first >= '0' && first <= '9') || (first == '_')) {
        } else {
            name = "_" + name;
        }
        name = name.replaceAll("[^a-zA-Z0-9\\-\\+\\.\\_\\*\\:\\(\\)\\|\\\\\\/]", "_");
        name = name.replace(' ', '_');
        int size = name.length();
        if (size <= len) return name; else return name.substring(0, len);
    }

    /**
	 * Returns the short name of this file format.
	 */
    public String getShortName() {
        return "TNT";
    }

    /**
	 * Returns the full name of this file format handler. E.g. "Nexus file format v2 and below".
	 * You ought to put in something about what versions of the software you support.
	 * But not too long: think about whether you could display it in a list.
	 */
    public String getFullName() {
        return "TNT/Hennig86 support";
    }

    /**
	 * Read this file into the specified SequenceList. This will read all the files straight into
	 * this sequence list, in the correct order.
	 * 
	 * @throws IOException if there was an error doing I/O
	 * @throws SequenceException if a Sequence is malformed - incorrect bases, etc.
	 * @throws FormatException if there was an error in the format of the file.
	 * @throws DelayAbortedException if the DelayCallback was aborted by the user.
	 */
    public SequenceList readFile(File file, DelayCallback delay) throws IOException, SequenceException, FormatException, DelayAbortedException {
        SequenceList sl = new SequenceList();
        sl.lock();
        appendFromFile(sl, file, delay);
        sl.unlock();
        return sl;
    }

    /**
	 * Append this file to the specified SequenceList. This will read in all the sequences from
	 * the file and append them directly onto the end of this SequenceList.
	 *
	 * @throws IOException if there was an error doing I/O
	 * @throws SequenceException if a Sequence is malformed - incorrect bases, etc.
	 * @throws FormatException if there was an error in the format of the file.
	 * @throws DelayAbortedException if the DelayCallback was aborted by the user.
	 */
    public void appendFromFile(SequenceList appendTo, File fileFrom, DelayCallback delay) throws IOException, SequenceException, FormatException, DelayAbortedException {
        FormatHandlerEvent evt = new FormatHandlerEvent(fileFrom, this, appendTo);
        if (delay != null) delay.begin();
        appendTo.lock();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileFrom));
            int count_lines = 0;
            int count_data = 0;
            long file_length = fileFrom.length();
            while (reader.ready()) {
                if (delay != null) delay.delay((int) ((float) count_data / file_length * 100), 1000);
                count_data += reader.readLine().length();
                count_lines++;
            }
            reader = new BufferedReader(new FileReader(fileFrom));
            StreamTokenizer tok = new StreamTokenizer(reader);
            tok.ordinaryChar('/');
            tok.wordChars('@', '@');
            tok.ordinaryChar('.');
            tok.ordinaryChar('-');
            tok.ordinaryChars('0', '9');
            tok.wordChars('.', '.');
            tok.wordChars('-', '-');
            tok.wordChars('0', '9');
            tok.wordChars('|', '|');
            tok.wordChars('_', '_');
            tok.ordinaryChar('\'');
            tok.ordinaryChar(';');
            int commentLevel = 0;
            boolean newCommand = true;
            while (true) {
                if (delay != null) delay.delay(tok.lineno(), count_lines);
                int type = tok.nextToken();
                if (type == StreamTokenizer.TT_EOF) break;
                if (type == '\'') {
                    if (commentLevel == 0) commentLevel = 1; else commentLevel = 0;
                    continue;
                }
                if (commentLevel > 0) continue;
                if (type == ';') {
                    newCommand = true;
                    continue;
                }
                if (newCommand && type == StreamTokenizer.TT_WORD) {
                    String str = tok.sval;
                    if (str.equalsIgnoreCase("nstates")) {
                        int token = tok.nextToken();
                        if ((token == StreamTokenizer.TT_WORD) && (tok.sval.equalsIgnoreCase("dna"))) {
                        } else {
                            if (tok.sval.equalsIgnoreCase("cont")) throw formatException(tok, "TaxonDNA can currently only load files which contain discrete sequences. This file does not (it contains continuous data, as indicated by 'nstates " + tok.sval + "').");
                        }
                    } else if (str.equalsIgnoreCase("xread")) {
                        xreadBlock(appendTo, tok, evt, delay, count_lines);
                        newCommand = true;
                        continue;
                    } else if (str.equalsIgnoreCase("xgroup")) {
                        groupCommand(GROUP_CHARSET, appendTo, tok, evt, delay, count_lines);
                        newCommand = true;
                        continue;
                    } else if (str.equalsIgnoreCase("agroup")) {
                        groupCommand(GROUP_TAXONSET, appendTo, tok, evt, delay, count_lines);
                        newCommand = true;
                        continue;
                    } else {
                    }
                } else {
                }
                newCommand = false;
            }
        } finally {
            if (delay != null) delay.end();
            appendTo.unlock();
        }
        appendTo.setFile(fileFrom);
        appendTo.setFormatHandler(this);
    }

    public FormatException formatException(StreamTokenizer tok, String message) {
        return new FormatException("Error on line " + tok.lineno() + ": " + message);
    }

    /**
	 * Parses an 'xread' command. This is going to be a pretty simple interpretation, all 
	 * things considered: we'll ignore amperstands,
	 * and we'll barf quickly and early if we think we're going in over our head. Pretty
	 * much, we are just targeting trying to be able to open files we spit out. Can't be
	 * _that_ hard, can it?
	 *
	 * Implementation note: the string '[]' in the sequence will be converted into a single '-' 
	 */
    public void xreadBlock(SequenceList appendTo, StreamTokenizer tok, FormatHandlerEvent evt, DelayCallback delay, int count_lines) throws FormatException, DelayAbortedException, IOException {
        Interleaver interleaver = new Interleaver();
        int seq_names_count = 0;
        int begin_at = tok.lineno();
        char missingChar = '?';
        char gapChar = '-';
        tok.wordChars(gapChar, gapChar);
        tok.wordChars(missingChar, missingChar);
        tok.wordChars('[', '[');
        tok.wordChars(']', ']');
        Hashtable hash_names = new Hashtable();
        String name = null;
        if (tok.ttype == StreamTokenizer.TT_WORD && tok.sval.equalsIgnoreCase("xread")) ; else tok.nextToken();
        tok.nextToken();
        StringBuffer title = null;
        if (tok.ttype == '\'') {
            title = new StringBuffer();
            while (true) {
                if (delay != null) delay.delay(tok.lineno(), count_lines);
                int type = tok.nextToken();
                if (type == '\'') break;
                if (type == StreamTokenizer.TT_WORD) {
                    if (tok.sval.length() > 0 && tok.sval.charAt(0) == '@') {
                        if (tok.sval.equalsIgnoreCase("@xgroup")) {
                            groupCommand(GROUP_CHARSET, appendTo, tok, evt, delay, count_lines);
                        } else if (tok.sval.equalsIgnoreCase("@agroup")) {
                            groupCommand(GROUP_TAXONSET, appendTo, tok, evt, delay, count_lines);
                        } else {
                        }
                    } else {
                        title.append(tok.sval);
                    }
                } else title.append(type);
                if (type == StreamTokenizer.TT_EOF) throw formatException(tok, "The title doesn't seem to have been closed properly. Are you sure the final quote character is present?");
            }
        } else {
            tok.pushBack();
        }
        int nChars = 0;
        tok.nextToken();
        if (tok.ttype != StreamTokenizer.TT_WORD) throw formatException(tok, "Couldn't find the number of characters. I found '" + (char) tok.ttype + "' instead!");
        try {
            nChars = Integer.parseInt(tok.sval);
        } catch (NumberFormatException e) {
            throw formatException(tok, "Couldn't convert this file's character count (which is \"" + tok.sval + "\") into a number. Are you sure it's really a number?");
        }
        int nTax = 0;
        tok.nextToken();
        if (tok.ttype != StreamTokenizer.TT_WORD) throw formatException(tok, "Couldn't find the number of taxa. I found '" + (char) tok.ttype + "' instead!");
        try {
            nTax = Integer.parseInt(tok.sval);
        } catch (NumberFormatException e) {
            throw formatException(tok, "Couldn't convert this file's taxon count (which is \"" + tok.sval + "\") into a number. Are you sure it's really a number?");
        }
        int lineno = tok.lineno();
        while (true) {
            int type = tok.nextToken();
            if (delay != null) delay.delay(lineno, count_lines);
            if (type == StreamTokenizer.TT_EOF) {
                throw formatException(tok, "I've reached the end of the file, but the 'xread' beginning at line " + begin_at + " was never terminated.");
            }
            if (type == ';') break;
            if (type == StreamTokenizer.TT_WORD) {
                String word = tok.sval;
                if (word.equalsIgnoreCase("[dna]") || word.equalsIgnoreCase("[prot]") || word.equalsIgnoreCase("[num]")) {
                    continue;
                }
                if (word.equalsIgnoreCase("[cont]")) {
                    throw formatException(tok, "TaxonDNA can currently only load files which contain discrete sequences. This file does not (it contains continuous data, as indicated by '[cont]').");
                }
                if (word.matches("^\\[.*\\]$")) {
                    throw formatException(tok, "Unrecognized data type: " + word);
                }
                String seq_name = new String(word);
                seq_name = seq_name.replace('_', ' ');
                int tmp_type = tok.nextToken();
                if (tmp_type != StreamTokenizer.TT_WORD) {
                    throw formatException(tok, "I recognize sequence name '" + seq_name + "', but instead of the sequence, I find '" + (char) tok.ttype + "'. What's going on?");
                }
                String sequence = tok.sval;
                seq_names_count++;
                try {
                    interleaver.appendSequence(seq_name, sequence);
                } catch (SequenceException e) {
                    throw formatException(tok, "Sequence '" + name + "' contains invalid characters. The exact error encountered was: " + e);
                }
            } else if (type == '&') {
            } else {
                throw formatException(tok, "I found '" + (char) type + "' rather unexpectedly in the xread block! Are you sure it's supposed to be here?");
            }
        }
        Iterator i = interleaver.getSequenceNamesIterator();
        int count = 0;
        while (i.hasNext()) {
            if (delay != null) delay.delay(count, seq_names_count);
            count++;
            String seqName = (String) i.next();
            Sequence seq = interleaver.getSequence(seqName);
            if (seq.getLength() != nChars) {
                throw new FormatException("The number of characters specified in the file (" + nChars + ") do not match with the number of characters is sequence '" + seqName + "' (" + seq.getLength() + ").");
            }
            appendTo.add(seq);
        }
        if (count != nTax) throw new FormatException("The number of sequences specified in the file (" + nTax + ") does not match the number of sequences present in the file (" + count + ").");
        tok.ordinaryChar(gapChar);
        tok.ordinaryChar(missingChar);
    }

    /**
	 * Parses a 'group' command. Group commands are relatively easy to work with; they go like this:
	 * 	(=\d+ (\(\w+\))* (\d+)*)*
	 * 	  ^       ^        ^
	 * 	  |	 |	  \----------	one or more char/taxon numbers
	 * 	  |	 \-------------------	the name of this group (if any)
	 * 	  \--------------------------	the number of this group (not important to us, except that /=\d+/ 
	 * 	  				indicates a new taxongroup starting
	 * In this function, I'll use ?group or _group to indicate, err, well, /[ax]group/.
	 */
    public void groupCommand(int which_group, SequenceList appendTo, StreamTokenizer tok, FormatHandlerEvent evt, DelayCallback delay, int count_lines) throws FormatException, DelayAbortedException, IOException {
        int begin_at = tok.lineno();
        String current_command_name = "";
        if (which_group == GROUP_CHARSET) current_command_name = "xgroup"; else current_command_name = "agroup";
        System.err.println("Beginning: " + current_command_name);
        tok.wordChars('.', '.');
        Hashtable hash_group_ids = new Hashtable();
        String currentName = "";
        int sequence_begin = -1;
        int sequence_end = -1;
        while (true) {
            int type = tok.nextToken();
            if (delay != null) delay.delay(tok.lineno(), count_lines);
            if (type == StreamTokenizer.TT_EOF) {
                throw formatException(tok, "I've reached the end of the file, but the '" + current_command_name + "' beginning at line " + begin_at + " was never terminated.");
            }
            if (type == ';') break;
            if (type == '=') {
                if (sequence_begin == -1 || sequence_end == -1) {
                } else {
                    if (which_group == GROUP_CHARSET) {
                        fireEvent(evt.makeCharacterSetFoundEvent(currentName, sequence_begin + 1, sequence_end + 1));
                        System.err.println("New single-character sequence: " + currentName + " from " + sequence_begin + " to " + sequence_end);
                    }
                }
                sequence_begin = -1;
                sequence_end = -1;
                if (tok.nextToken() != StreamTokenizer.TT_WORD) {
                    throw formatException(tok, "Expecting the group id, but found '" + (char) tok.ttype + "' instead!");
                } else {
                    if (hash_group_ids.get(tok.sval) != null) throw formatException(tok, "Duplicate group id '" + tok.sval + "' found!");
                    hash_group_ids.put(tok.sval, new Integer(0));
                    currentName = "Group #" + tok.sval;
                }
                continue;
            } else if (type == '(') {
                StringBuffer buff_name = new StringBuffer();
                int title_began = tok.lineno();
                while (tok.nextToken() != ')') {
                    if (delay != null) delay.delay(tok.lineno(), count_lines);
                    if (tok.ttype == StreamTokenizer.TT_EOF) throw formatException(tok, "The title which began in " + current_command_name + " on line " + title_began + " is not terminated! (I can't find the ')' which would end it)."); else if (tok.ttype == StreamTokenizer.TT_WORD) buff_name.append(tok.sval); else buff_name.append((char) tok.ttype);
                }
                currentName = buff_name.toString();
                continue;
            } else if (type == StreamTokenizer.TT_WORD) {
                String word = tok.sval;
                if (word.indexOf('.') == -1) {
                    int locus = atoi(word, tok);
                    if (sequence_begin == -1) {
                        sequence_begin = locus;
                    }
                    if (sequence_end == -1 || sequence_end == locus - 1) {
                        sequence_end = locus;
                    } else {
                        if (which_group == GROUP_CHARSET) {
                            fireEvent(evt.makeCharacterSetFoundEvent(currentName, sequence_begin + 1, sequence_end + 1));
                            System.err.println("New single-character sequence: " + currentName + " from " + sequence_begin + " to " + sequence_end);
                        }
                        sequence_begin = locus;
                        sequence_end = locus;
                    }
                    continue;
                } else {
                    if (which_group == GROUP_CHARSET) {
                        fireEvent(evt.makeCharacterSetFoundEvent(currentName, sequence_begin + 1, sequence_end + 1));
                        System.err.println("New single-character sequence: " + currentName + " from " + sequence_begin + " to " + sequence_end);
                    }
                    sequence_begin = -1;
                    sequence_end = -1;
                    int from = 0;
                    int to = 0;
                    if (word.charAt(0) == '.') {
                        if (word.length() == 1) {
                            from = 0;
                            to = appendTo.getMaxLength();
                        } else {
                            from = 0;
                            to = atoi(word.substring(1), tok);
                        }
                    } else if (word.charAt(word.length() - 1) == '.') {
                        from = atoi(word.substring(0, word.length() - 1), tok);
                        to = appendTo.getMaxLength();
                    } else {
                        int indexOf = word.indexOf('.');
                        from = atoi(word.substring(0, indexOf - 1), tok);
                        to = atoi(word.substring(indexOf + 1), tok);
                    }
                    if (which_group == GROUP_CHARSET) {
                        from++;
                        to++;
                        fireEvent(evt.makeCharacterSetFoundEvent(currentName, from, to));
                        System.err.println("New multi-character block: " + currentName + " from " + from + " to " + to);
                    }
                    continue;
                }
            } else {
                throw formatException(tok, "I found '" + (char) type + "' rather unexpectedly in the " + current_command_name + " command beginning on line " + begin_at + "! Are you sure it's supposed to be here?");
            }
        }
        if (sequence_begin != -1 && sequence_end != -1) {
            if (which_group == GROUP_CHARSET) {
                fireEvent(evt.makeCharacterSetFoundEvent(currentName, sequence_begin + 1, sequence_end + 1));
                System.err.println("New single-character sequence: " + currentName + " from " + sequence_begin + " to " + sequence_end);
            }
        }
        tok.ordinaryChar('.');
    }

    private int atoi(String word, StreamTokenizer tok) throws FormatException {
        try {
            return Integer.parseInt(word);
        } catch (NumberFormatException e) {
            throw formatException(tok, "Could not convert word '" + word + "' to a number: " + e);
        }
    }

    /**
	 * Writes the content of this sequence list into a file. The file is
	 * overwritten. The order of the sequences written into the file is
	 * guaranteed to be the same as in the list.
	 *
	 * @throws IOException if there was a problem creating/writing to the file.
	 * @throws DelayAbortedException if the DelayCallback was aborted by the user.
	 */
    public void writeFile(File file, SequenceList set, DelayCallback delay) throws IOException, DelayAbortedException {
        writeTNTFile(file, set, 0, "", delay);
    }

    /**
	 * A species TNTFile-only method to have a bit more control over how
	 * the Nexus file gets written.
	 *
	 * @param interleaveAt Specifies where you want to interleave. Note that TNTFile.INTERLEAVE_AT will be entirely ignored here, and that if the sequence is less than interleaveAt, it will not be interleaved at all. '-1' turns off all interleaving (flatline), although you can obviously use a huge value (999999) to get basically the same thing.
	 * @param otherBlocks We put this into the file at the very bottom. It should be one or more proper 'BLOCK's, unless you really know what you're doing.
	 */
    public void writeTNTFile(File file, SequenceList set, int interleaveAt, String otherBlocks, DelayCallback delay) throws IOException, DelayAbortedException {
        boolean interleaved = false;
        if (interleaveAt > 0 && interleaveAt < set.getMaxLength()) interleaved = true;
        set.lock();
        if (delay != null) delay.begin();
        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
        writer.println("nstates 32;");
        writer.println("xread");
        writer.println("'Written by TaxonDNA " + Versions.getTaxonDNA() + " on " + new Date() + "'");
        writer.println(set.getMaxLength() + " " + set.count());
        writer.println("");
        Hashtable names = new Hashtable();
        Vector vec_names = new Vector();
        Iterator i = set.iterator();
        while (i.hasNext()) {
            Sequence seq = (Sequence) i.next();
            String name = getTNTName(seq.getFullName(MAX_TAXON_LENGTH), MAX_TAXON_LENGTH);
            name = name.replaceAll("\'", "\'\'");
            name = name.replace(' ', '_');
            int no = 2;
            while (names.get(name) != null) {
                int digits = 5;
                if (no > 0 && no < 10) digits = 1;
                if (no >= 10 && no < 100) digits = 2;
                if (no >= 100 && no < 1000) digits = 3;
                if (no >= 1000 && no < 10000) digits = 4;
                name = getTNTName(seq.getFullName(MAX_TAXON_LENGTH - digits - 1), MAX_TAXON_LENGTH - digits - 1);
                name = name.replaceAll("\'", "\'\'");
                name = name.replace(' ', '_');
                name += "_" + no;
                no++;
                if (no == 10000) {
                    throw new IOException("There are 9999 sequences named '" + seq.getFullName(MAX_TAXON_LENGTH) + "', which is the most I can handle. Sorry. This is an arbitary limit: please let us know if you think we set it too low.");
                }
            }
            names.put(name, seq);
            vec_names.add(name);
        }
        if (!interleaved) {
            Iterator i_names = vec_names.iterator();
            int x = 0;
            while (i_names.hasNext()) {
                if (delay != null) {
                    try {
                        delay.delay(x, vec_names.size());
                    } catch (DelayAbortedException e) {
                        writer.close();
                        set.unlock();
                        throw e;
                    }
                }
                String name = (String) i_names.next();
                Sequence seq = (Sequence) names.get(name);
                writer.println(pad_string(name, MAX_TAXON_LENGTH) + " " + seq.getSequence());
                x++;
            }
        } else {
            for (int x = 0; x < set.getMaxLength(); x += interleaveAt) {
                Iterator i_names = vec_names.iterator();
                if (delay != null) try {
                    delay.delay(x, set.getMaxLength());
                } catch (DelayAbortedException e) {
                    writer.close();
                    set.unlock();
                    throw e;
                }
                writer.println("&");
                while (i_names.hasNext()) {
                    String name = (String) i_names.next();
                    Sequence seq = (Sequence) names.get(name);
                    Sequence subseq = null;
                    int until = 0;
                    try {
                        until = x + interleaveAt;
                        if (until > seq.getLength()) {
                            until = seq.getLength();
                        }
                        subseq = seq.getSubsequence(x + 1, until);
                    } catch (SequenceException e) {
                        delay.end();
                        throw new IOException("Could not get subsequence (" + (x + 1) + ", " + until + ") from sequence " + seq + ". This is most likely a programming error.");
                    }
                    if (subseq.getSequence().indexOf('Z') != -1 || subseq.getSequence().indexOf('z') != -1) delay.addWarning("Sequence '" + subseq.getFullName() + "' contains the letter 'Z'. This letter might not work in TNT.");
                    writer.println(pad_string(name, MAX_TAXON_LENGTH) + " " + subseq.getSequence());
                }
            }
        }
        writer.println(";");
        if (otherBlocks != null) writer.println(otherBlocks);
        writer.close();
        if (delay != null) delay.end();
        set.unlock();
    }

    private String pad_string(String x, int size) {
        StringBuffer buff = new StringBuffer();
        if (x.length() < size) {
            buff.append(x);
            for (int c = 0; c < (size - x.length()); c++) buff.append(' ');
        } else if (x.length() == size) return x; else return x.substring(x.length() - 3) + "___";
        return buff.toString();
    }

    /**
	 * Checks to see if this file *might* be of this format. Good for internal loops.
	 * 
	 * No exceptions: implementors, please swallow them all up. If the file does not
	 * exist, it's not very likely to be of this format, is it? 
	 */
    public boolean mightBe(File file) {
        try {
            BufferedReader buff = new BufferedReader(new FileReader(file));
            while (buff.ready()) {
                String str = buff.readLine().trim();
                if (str.equals("")) continue;
                if (str.toLowerCase().indexOf("xread") != -1) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private String fixColumnName(String columnName) {
        columnName = columnName.replaceAll("\\.nex", "");
        columnName = columnName.replaceAll("\\.tnt", "");
        columnName = columnName.replace('.', '_');
        columnName = columnName.replace(' ', '_');
        columnName = columnName.replace('-', '_');
        columnName = columnName.replace('\\', '_');
        columnName = columnName.replace('/', '_');
        return columnName;
    }

    /**
	 * Export a SequenceGrid as a TNT file. 
	 */
    public void writeFile(File file, SequenceGrid grid, DelayCallback delay) throws IOException, DelayAbortedException {
        writeTNTFile(file, grid, delay);
    }

    /**
	 * Export a SequenceGrid as a TNT file.
	 */
    public void writeTNTFile(File f, SequenceGrid grid, DelayCallback delay) throws IOException, DelayAbortedException {
        StringBuffer buff_title = new StringBuffer();
        Set cols = grid.getColumns();
        if (cols.size() >= 32) {
            delay.addWarning("TOO MANY CHARACTER SETS: According to the manual, TNT can only handle 32 character sets. You have " + cols.size() + " character sets. I will write out the remaining character sets into the file title, from where you can copy it into the correct position in the file as needed.");
        }
        StringBuffer buff_sets = new StringBuffer();
        buff_sets.append("xgroup\n");
        Iterator i = cols.iterator();
        int at = 0;
        int colid = 0;
        while (i.hasNext()) {
            String colName = (String) i.next();
            if (colid == 32) buff_title.append("@xgroup\n");
            if (colid <= 31) buff_sets.append("=" + colid + " (" + fixColumnName(colName) + ")\t"); else buff_title.append("=" + colid + " (" + fixColumnName(colName) + ")\t");
            for (int x = 0; x < grid.getColumnLength(colName); x++) {
                if (colid <= 31) buff_sets.append(at + " "); else buff_title.append(at + " ");
                at++;
            }
            if (colid <= 31) buff_sets.append("\n"); else buff_title.append("\n");
            colid++;
        }
        buff_sets.append("\n;\n\n");
        if (colid > 31) buff_title.append("\n;");
        if (delay != null) delay.begin();
        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(f)));
        writer.println("nstates dna;");
        writer.println("xread\n'Exported by TaxonDNA " + Versions.getTaxonDNA() + " on " + new Date() + ".");
        if (buff_title.length() > 0) {
            writer.println("Additional taxonsets and character sets will be placed below this line.");
            writer.println(buff_title.toString());
            writer.println("Additional taxonsets and character sets end here.");
        }
        writer.println("'");
        writer.println(grid.getCompleteSequenceLength() + " " + grid.getSequencesCount());
        Iterator i_rows = grid.getSequences().iterator();
        int count_rows = 0;
        while (i_rows.hasNext()) {
            if (delay != null) delay.delay(count_rows, grid.getSequencesCount());
            count_rows++;
            String seqName = (String) i_rows.next();
            Sequence seq_interleaved = null;
            int length = 0;
            writer.print(getTNTName(seqName, MAX_TAXON_LENGTH) + " ");
            Iterator i_cols = cols.iterator();
            while (i_cols.hasNext()) {
                String colName = (String) i_cols.next();
                Sequence seq = grid.getSequence(colName, seqName);
                if (seq == null) seq = Sequence.makeEmptySequence(colName, grid.getColumnLength(colName));
                length += seq.getLength();
                writer.print(seq.getSequence());
            }
            writer.println();
        }
        writer.println(";\n");
        writer.println(buff_sets);
        writer.flush();
        writer.close();
        if (delay != null) delay.end();
    }
}
