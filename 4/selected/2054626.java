package it.freax.fpm.util;

import it.freax.fpm.util.exceptions.ExtensionDecodingException;
import it.freax.fpm.util.exceptions.ParseException;
import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Strings {

    public static final int rangeExtMin = 1;

    public static final int rangeExtMax = 4;

    public static final String extSeparator = ".";

    public Strings() {
    }

    public static Strings getOne() {
        return new Strings();
    }

    public boolean checkExtensions(String path, List<String> list) throws ExtensionDecodingException {
        boolean ret = false;
        if (!getExtension(path).isEmpty()) {
            for (String ext : list) {
                if (ret = checkExtension(path, ext)) {
                    break;
                }
            }
        }
        return ret;
    }

    public boolean checkExtension(String path, String ext) throws ExtensionDecodingException {
        return ext.equalsIgnoreCase(getExtension(path));
    }

    public String getExtension(String path) throws ExtensionDecodingException {
        String ext = "";
        ArrayList<String> candidates = new ArrayList<String>();
        String[] splitted = path.split("\\" + extSeparator);
        candidates.add(splitted[splitted.length - 1]);
        for (int i = splitted.length - 2; i > 0; i--) {
            if (!splitted[i].isEmpty() && inRangeExt(splitted[i])) {
                candidates.add(splitted[i]);
            }
        }
        if (candidates.size() > 0) {
            ext = extSeparator + FpmCollections.getOne(candidates).lastOrDefault();
        }
        if (!path.endsWith(ext) || ext.isEmpty()) {
            throw new ExtensionDecodingException("Extension decoding failed!");
        }
        return ext;
    }

    private boolean inRangeExt(String ext) {
        return (ext.length() >= rangeExtMin) && (ext.length() <= rangeExtMax);
    }

    public String removeExtension(String fileName) throws ExtensionDecodingException {
        return fileName.replace(getExtension(fileName), "");
    }

    public String replaceExtension(String fileName, String extToReplace) throws ExtensionDecodingException {
        return fileName.replace(getExtension(fileName), extToReplace);
    }

    public String trimEnd(String str) {
        str = str.trim();
        int idx = str.lastIndexOf(' ');
        if ((str != null) && !str.equalsIgnoreCase("")) {
            str = str.substring(0, idx);
        }
        return str;
    }

    public String trimStart(String str) {
        str = str.trim();
        int idx = str.lastIndexOf(' ') + 1;
        if ((str != null) && !str.equalsIgnoreCase("")) {
            str = str.substring(idx, str.length());
        }
        return str;
    }

    public String getStringInsideDelimiters(String input, String startDelimiter, String endDelimiter) {
        String ret = "";
        if ((input != null) && !input.isEmpty()) {
            ret = input.substring(input.indexOf(startDelimiter) + 1);
            if (!ret.equalsIgnoreCase(input)) {
                ret = ret.substring(0, ret.indexOf(endDelimiter));
            }
        }
        return ret;
    }

    public String getStringFromKeyValue(String input, String keyValueDelimiter, boolean getKey) {
        String ret = "";
        StringDelimiterTokenizer st = new StringDelimiterTokenizer(input, keyValueDelimiter);
        if (getKey) {
            ret = st.nextToken();
        } else {
            st.nextToken();
            if (st.hasMoreTokens()) {
                ret = st.nextToken();
            }
        }
        return ret;
    }

    public String clean(String input) {
        return input.replace("[", "").replace("]", "");
    }

    public String getRowSubstring(String input, String subset, boolean removeSubSet) {
        int beginIndex = input.indexOf(subset);
        if (removeSubSet) {
            beginIndex += subset.length();
        }
        return input.substring(beginIndex);
    }

    public List<String> getLines(String input) {
        List<String> ret = new ArrayList<String>();
        Scanner scn = new Scanner(input);
        while (scn.hasNextLine()) {
            String line = scn.nextLine();
            if (!line.isEmpty()) {
                ret.add(line);
            }
        }
        return ret;
    }

    public String getLines(Collection<String> input) {
        StringBuffer ret = new StringBuffer();
        String[] inputArr = input.toArray(new String[input.size()]);
        for (String element : inputArr) {
            ret.append(element).append(Constants.LS);
        }
        return ret.toString();
    }

    public List<String> grep(String input, String pattern, boolean caseInsensitive) {
        ArrayList<String> ret = new ArrayList<String>();
        Scanner scn = new Scanner(input);
        int Options = 0;
        if (caseInsensitive) {
            Options |= Pattern.CASE_INSENSITIVE;
        }
        Pattern patRegex = Pattern.compile(pattern, Options);
        while (scn.hasNext()) {
            String curr = scn.nextLine();
            if (patRegex.matcher(curr).matches()) {
                ret.add(curr);
            }
        }
        return ret;
    }

    public String Split(String input, String delim, int elemidx, boolean remext) throws ExtensionDecodingException {
        String ret = "";
        if (remext) {
            ret = removeExtension(input);
        }
        ret = input.split(delim)[elemidx];
        return ret;
    }

    private int getOpID(String input) {
        int opid = 0;
        char first = input.charAt(0);
        switch(first) {
            case '"':
                {
                    opid = 1;
                    break;
                }
            case '\'':
                {
                    opid = 2;
                    break;
                }
            case '<':
                {
                    opid = 3;
                    break;
                }
            case '{':
                {
                    opid = 4;
                    break;
                }
            case '[':
                {
                    opid = 5;
                    break;
                }
            case '(':
                {
                    opid = 6;
                    break;
                }
        }
        return opid;
    }

    public String KeyValue(String input, String delim) {
        String ret = "";
        ret = getStringFromKeyValue(input, delim, false);
        int opid = getOpID(ret);
        switch(opid) {
            case 0:
                {
                    break;
                }
            case 1:
                {
                    ret = getStringInsideDelimiters(ret, "\"", "\"");
                    break;
                }
            case 2:
                {
                    ret = getStringInsideDelimiters(ret, "'", "'");
                    break;
                }
            case 3:
                {
                    ret = getStringInsideDelimiters(ret, "<", ">");
                    break;
                }
            case 4:
                {
                    ret = getStringInsideDelimiters(ret, "{", "}");
                    break;
                }
            case 5:
                {
                    ret = getStringInsideDelimiters(ret, "[", "]");
                    break;
                }
            case 6:
                {
                    ret = getStringInsideDelimiters(ret, "(", ")");
                    break;
                }
        }
        return ret;
    }

    public String trimDir(String file) {
        int idx = file.lastIndexOf(Constants.FS) + 1;
        file = file.substring(idx);
        return file;
    }

    public List<String> split(String str, String delim) {
        List<String> ret = new ArrayList<String>();
        if (str != null) {
            ret = FpmCollections.<String>getOne(str.split(delim)).toList();
        }
        return ret;
    }

    public String merge(List<String> toMerge, String delimiter) {
        StringBuffer merged = new StringBuffer();
        for (int i = 0; i < toMerge.size(); i++) {
            merged.append(toMerge.get(i));
            if (i < (toMerge.size() - 1)) {
                merged.append(delimiter);
            }
        }
        return merged.toString();
    }

    public String safeConcatPaths(String... args) {
        String ret = "";
        try {
            ret = concatPaths(args);
        } catch (ExtensionDecodingException e) {
            if (args.length > 1) {
                String[] newargs = new String[] {};
                FpmCollections<String> coll = FpmCollections.getOne(args);
                newargs = coll.subarray(0, FpmCollections.LastIndex - 1, newargs);
                ret = safeConcatPaths(newargs);
            } else if (args.length > 0) {
                ret = args[0];
            } else {
                ret = null;
            }
        }
        return ret;
    }

    public String concatPaths(String... args) throws ExtensionDecodingException {
        String ret = args[0];
        for (int i = 1; i < args.length; i++) {
            if (!ret.endsWith(Constants.FS)) {
                ret += Constants.FS;
            }
            if (args[i].endsWith(Constants.FS) || ((i == (args.length - 1)) && !isNullOrEmpty(getExtension(args[i])))) {
                ret += args[i];
            } else {
                ret += args[i] + Constants.FS;
            }
        }
        return ret;
    }

    public boolean isNullOrEmpty(String s) {
        return (s == null) || s.isEmpty();
    }

    public boolean isRelativePath(String path) {
        boolean ret = false;
        File[] roots = File.listRoots();
        for (int i = 0; (i < roots.length); i++) {
            ret |= !path.startsWith(roots[i].getAbsolutePath());
        }
        return ret;
    }

    public boolean checkPathExistence(String path) {
        return new File(path).exists();
    }

    public boolean createPath(String path) {
        boolean ret = false;
        File f = new File(path);
        if (!f.exists()) {
            if (f.getParent() == null) {
                ret = f.mkdir();
            } else if (f.getParentFile().exists()) {
                ret = f.mkdir();
            } else {
                ret = createPath(f.getParent());
                if (ret) {
                    ret = f.mkdir();
                }
            }
        } else {
            ret = true;
        }
        return ret;
    }

    public LinearDictionary<String, String> getMap(String content, String kvs, String inCommTk, String outCommTk) throws ParseException {
        LinearDictionary<String, String> dict = new LinearDictionary<String, String>();
        List<String> lines = getLines(content);
        int count = 0;
        boolean keyok = false;
        String currentline = "";
        try {
            for (String line : lines) {
                currentline = line;
                keyok = false;
                if (!(line.startsWith(inCommTk) && (line.endsWith(outCommTk) || isNullOrEmpty(outCommTk)))) {
                    String key = getStringFromKeyValue(line, kvs, true);
                    keyok = true;
                    String value = getStringFromKeyValue(line, kvs, false);
                    MapEntry<String, String> me;
                    me = new MapEntry<String, String>(key, value);
                    if (!dict.entrySet().contains(me)) {
                        dict.put(key.trim(), value.trim());
                    }
                }
                count++;
            }
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Read " + count + " lines of " + lines.size()).append(Constants.LS);
            sb.append("Broken in the value getter: " + keyok);
            sb.append(" at the line (non-blank) " + ++count).append(Constants.LS);
            sb.append("Current line is:").append(Constants.LS);
            sb.append(currentline).append(Constants.LS);
            throw new ParseException(sb.toString());
        }
        return dict;
    }

    public String reverse(String input) {
        StringBuilder ret = new StringBuilder();
        char[] inArr = input.toCharArray();
        for (int i = inArr.length - 1; i > 0; i--) {
            ret.append(inArr[i]);
        }
        return ret.toString();
    }

    public StringWriter toStringWriter(String source) {
        StringWriter writer = new StringWriter();
        Scanner scanner;
        scanner = new Scanner(source);
        while (scanner.hasNextLine()) {
            writer.append(scanner.nextLine()).append(Constants.FS);
        }
        return writer;
    }
}
