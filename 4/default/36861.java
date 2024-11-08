import java.io.*;
import java.awt.Frame;
import java.awt.FileDialog;
import java.lang.*;
import java.util.StringTokenizer;
import java.net.*;

public class XFile extends Object {

    public static final String Eol = System.getProperty("line.separator");

    public File aFile = null;

    BufferedReader diStream = null;

    int creator = 0x43584D4C;

    int type = 0x54455854;

    public StreamTokenizer tokenStream = null;

    public Writer dataWriter = null;

    public static final int READ = 0;

    public static final int WRITE = 1;

    PrintFormat fPf = null;

    public String errMessage;

    public int Delimiter = ',';

    int xMode = READ;

    int NFields = 0;

    int[] theFields;

    boolean isOpen = false;

    public XFile() {
        xMode = -1;
    }

    public XFile(File someFile) {
        aFile = someFile;
        xMode = READ;
    }

    public XFile(String someFile) {
        aFile = new File(someFile);
        xMode = READ;
    }

    public boolean OpenURL(URL theContext, String relURL) {
        try {
            return OpenURL(new URL(theContext, relURL));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean OpenURL(URL theURL) {
        if (theURL != null) {
            BufferedInputStream theStream;
            try {
                InputStream a = theURL.openStream();
                theStream = new BufferedInputStream(a);
            } catch (java.io.IOException e) {
                theStream = null;
                System.out.println("URL Stream is NULL!");
                return false;
            }
            try {
                diStream = new BufferedReader(new InputStreamReader(theStream));
            } catch (Exception e) {
                errMessage = "Stream is not Available";
                return (false);
            }
            isOpen = true;
            return true;
        }
        return false;
    }

    public boolean Open(String sstream) {
        xMode = READ;
        diStream = new BufferedReader(new StringReader(sstream));
        isOpen = true;
        return true;
    }

    public boolean Open() {
        if (xMode == -1) {
            errMessage = "Must set mode using setMode(read=0,write=1)";
            return false;
        }
        return (Open(xMode));
    }

    public boolean Open(int omode) {
        if (isOpen || diStream != null) {
            return omode == xMode;
        }
        xMode = omode;
        if (aFile == null) {
            Choose(omode);
        }
        if (xMode == 0) {
            FileInputStream theStream;
            if (aFile.canRead()) {
                try {
                    theStream = new FileInputStream(aFile);
                } catch (Exception e) {
                    errMessage = "File is not Available";
                    return (false);
                }
                try {
                    diStream = new BufferedReader(new InputStreamReader(theStream));
                } catch (Exception e) {
                    errMessage = "Stream is not Available";
                    return (false);
                }
                isOpen = true;
                return true;
            }
            return false;
        } else {
            FileOutputStream theStream;
            try {
                if (aFile.exists()) theStream = new FileOutputStream(aFile); else theStream = new FileOutputStream(aFile.getPath());
            } catch (Exception e) {
                errMessage = "Write File is not Available";
                return (false);
            }
            setFileTypeAndCreator();
            try {
                dataWriter = new BufferedWriter(new OutputStreamWriter(theStream));
            } catch (Exception e) {
                errMessage = "Stream is not Available";
                return (false);
            }
            isOpen = true;
            return true;
        }
    }

    public boolean OpenPrint() {
        xMode = WRITE;
        FileOutputStream theStream;
        try {
            if (aFile.exists()) theStream = new FileOutputStream(aFile); else theStream = new FileOutputStream(aFile.getPath());
        } catch (Exception e) {
            errMessage = "Write File is not Available";
            return (false);
        }
        try {
            dataWriter = new PrintWriter(theStream);
        } catch (Exception e) {
            errMessage = "Stream is not Available";
            return (false);
        }
        isOpen = true;
        return true;
    }

    public int CountFields(String aLine) {
        int p, q = 0;
        int tokenIndex;
        tokenIndex = 0;
        do {
            p = aLine.indexOf(Delimiter, tokenIndex);
            q++;
            if (p <= 0) {
                return (q);
            } else {
                tokenIndex = p + 2;
            }
        } while (tokenIndex <= aLine.length());
        return (q);
    }

    public int BreakFields(String aLine) {
        int ntok = 0;
        StringTokenizer st = new StringTokenizer(aLine);
        while (st.hasMoreTokens()) {
        }
        return 0;
    }

    public String[] ReadFormat(int format[]) {
        int i, nf;
        String aLine = this.ReadLineNN();
        return MakeFields(aLine, format);
    }

    public String[] MakeFields(String aLine) {
        return MakeFields(aLine, null);
    }

    public String[] MakeFields(String aLine, int format[]) {
        int i, nf;
        if (aLine == null) {
            errMessage = "EOF";
            return null;
        } else {
            StringTokenizer st = new StringTokenizer(aLine);
            nf = st.countTokens();
            String[] retStrs = new String[nf];
            for (i = 0; i < nf; i++) {
                retStrs[i] = st.nextToken();
            }
            if (format == null || format[0] == -1) {
                return retStrs;
            } else {
                String[] xretStrs = new String[format.length];
                for (i = 0; i < format.length; i++) {
                    if (format[i] < nf) xretStrs[i] = retStrs[format[i]]; else {
                        errMessage = "Variable out of bounds";
                        xretStrs[i] = "";
                    }
                }
                return xretStrs;
            }
        }
    }

    public String ReadLine() {
        if (diStream != null) {
            try {
                return (diStream.readLine());
            } catch (Exception e) {
                errMessage = "Probably at End of File";
                return (null);
            }
        } else {
            errMessage = "Stream not open";
            return (null);
        }
    }

    public String ReadLineNN() {
        String aLine;
        do {
            aLine = this.ReadLine();
            if (aLine != null) {
                if (!aLine.equals("")) break;
            }
        } while (aLine != null);
        return (aLine);
    }

    public int CountLines(boolean doEmpty) {
        boolean wasOpen = isOpen;
        int count = 0;
        String x;
        if (!wasOpen) {
            if (!Open()) return -1;
        }
        if (doEmpty) {
            do {
                x = ReadLine();
                count++;
            } while (x != null);
        } else {
            do {
                x = ReadLineNN();
                count++;
            } while (x != null);
        }
        count--;
        if (!wasOpen) Close();
        return (count);
    }

    public boolean isOpen() {
        return isOpen;
    }

    public boolean Choose(int omode) {
        return Choose(omode, "", "");
    }

    public boolean Choose(int omode, String filename) {
        return Choose(omode, filename, "");
    }

    public boolean Choose(int omode, String fileName, String aMessage) {
        xMode = omode;
        if (xMode == -1) return false;
        Frame aFrame = new Frame();
        if (aMessage.equals("")) {
            if (xMode == WRITE) aMessage = "Write to file ..."; else aMessage = "Read from file ...";
        }
        FileDialog aFD = new FileDialog(aFrame, aMessage, xMode);
        aFD.setFile(fileName);
        try {
            aFD.show();
            aFile = new File(aFD.getDirectory().substring(0, aFD.getDirectory().length() - 1), aFD.getFile());
        } catch (Exception e) {
            errMessage = "Cancelled or error";
            return false;
        }
        return true;
    }

    synchronized void setFileTypeAndCreator(String fn, int typ, int creat) {
    }

    synchronized void setFileTypeAndCreator() {
    }

    public boolean Close() {
        isOpen = false;
        if (xMode == 0) {
            try {
                diStream.close();
                diStream = null;
            } catch (Exception e) {
                errMessage = "Can not Close Read Stream";
                return false;
            }
        } else {
            try {
                dataWriter.close();
                dataWriter = null;
            } catch (Exception e) {
                errMessage = "Can not Close Write Stream";
                return false;
            }
        }
        setFileTypeAndCreator();
        return true;
    }

    public boolean WriteLine(String line) {
        try {
            dataWriter.write(line);
            WriteString(Eol);
            return true;
        } catch (Exception e) {
            errMessage = "Can not Write Line";
            return false;
        }
    }

    public boolean WriteString(String line) {
        try {
            dataWriter.write(line);
            return true;
        } catch (Exception e) {
            errMessage = "Can not Write Line";
            return false;
        }
    }

    public boolean WriteBytes(String line) {
        try {
            dataWriter.write(line);
            return true;
        } catch (Exception e) {
            errMessage = "Can not write bytes";
            return false;
        }
    }

    public boolean WriteByte(int bite) {
        try {
            dataWriter.write(bite);
            return true;
        } catch (Exception e) {
            errMessage = "Can not Write Byte";
            return false;
        }
    }

    public boolean WriteNumber(Number num) {
        try {
            dataWriter.write(num.toString());
            return true;
        } catch (Exception e) {
            errMessage = "Can not Write Number";
            return false;
        }
    }

    public StreamTokenizer makeTokenizer() {
        tokenStream = null;
        if (xMode == 0 && diStream != null) {
            tokenStream = new StreamTokenizer(diStream);
        }
        return tokenStream;
    }

    private char c[] = new char[1];

    public String getStringToken() {
        if (tokenStream == null) return null;
        try {
            tokenStream.nextToken();
            String k = null;
            switch(tokenStream.ttype) {
                case StreamTokenizer.TT_WORD:
                    k = tokenStream.sval;
                    break;
                case StreamTokenizer.TT_NUMBER:
                    k = String.valueOf(tokenStream.nval);
                    break;
                case StreamTokenizer.TT_EOL:
                    k = Eol;
                    break;
                case StreamTokenizer.TT_EOF:
                    k = null;
                    break;
                default:
                    c[0] = (char) tokenStream.ttype;
                    k = new String(c);
                    break;
            }
            return k;
        } catch (Exception e) {
        }
        return null;
    }

    public Long getLong() {
        Double p = getDouble();
        if (p == null) return null;
        return new Long(p.longValue());
    }

    public Double getDouble() {
        Token tok;
        if (tokenStream == null) return null;
        for (; ; ) try {
            tokenStream.nextToken();
            String k = null;
            switch(tokenStream.ttype) {
                case StreamTokenizer.TT_WORD:
                    k = tokenStream.sval;
                    break;
                case StreamTokenizer.TT_NUMBER:
                    return new Double(tokenStream.nval);
                case StreamTokenizer.TT_EOL:
                    k = Eol;
                    break;
                case StreamTokenizer.TT_EOF:
                    k = null;
                    break;
                default:
                    c[0] = (char) tokenStream.ttype;
                    k = new String(c);
                    break;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public Token getToken() {
        Token tok;
        if (tokenStream == null) return null;
        try {
            tokenStream.nextToken();
            String k = null;
            switch(tokenStream.ttype) {
                case StreamTokenizer.TT_WORD:
                    k = tokenStream.sval;
                    break;
                case StreamTokenizer.TT_NUMBER:
                    k = String.valueOf(tokenStream.nval);
                    break;
                case StreamTokenizer.TT_EOL:
                    k = Eol;
                    break;
                case StreamTokenizer.TT_EOF:
                    k = null;
                    break;
                default:
                    c[0] = (char) tokenStream.ttype;
                    k = new String(c);
                    break;
            }
            return new Token(k, tokenStream.ttype);
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Reads the next SGML style tag from the Xfile DataInputStream 
     * specified input stream with the specified buffer size. 
     *
     * @return  String[] containing the tag and any parameters
     *					returns null if EOF reached;
     * @since   CSAC
     */
    public String[][] readTag() {
        return readTag(false);
    }

    /**
     * Reads the next SGML style tag from the Xfile DataInputStream 
     * specified input stream with the specified buffer size. 
     *
     * @return  String[] containing the tag and any parameters
     *					returns null if EOF reached;
     * @param   start  have we already read the '<'
     * @since   CSAC
     */
    String[][] attributes = null;

    public String[][] readTag(boolean start) {
        java.util.Vector h = new java.util.Vector(10);
        char c;
        StringBuffer s = new StringBuffer(100);
        StringBuffer a = new StringBuffer(100);
        StringBuffer av = new StringBuffer(300);
        String[][] r = null;
        try {
            if (!start) while (diStream.read() != '<') ;
            while ((c = (char) diStream.read()) != '>' && c != ' ') s.append(c);
            if (c == '>') {
                r = new String[1][1];
                r[0][0] = s.toString();
            } else {
                while ((c = (char) diStream.read()) != '>') {
                    a.setLength(0);
                    av.setLength(0);
                    while (c == ' ') c = (char) diStream.read();
                    if (c == '>') break;
                    while (c != ' ' && c != '=' && c != '>') {
                        a.append(c);
                        c = (char) diStream.read();
                    }
                    if (c == '>') break;
                    while (c == '"' || c == '=' || c == ' ') c = (char) diStream.read();
                    while (c != '"' && c != '>') {
                        av.append(c);
                        c = (char) diStream.read();
                    }
                    if (c == '>') {
                        h.addElement(a.toString());
                        h.addElement(av.toString());
                        break;
                    }
                    h.addElement(a.toString());
                    h.addElement(av.toString());
                }
                r = new String[h.size() / 2 + 1][2];
                r[0][0] = s.toString();
                for (int i = 0; i < (h.size() / 2); i++) {
                    r[i + 1][0] = (String) h.elementAt(i * 2);
                    r[i + 1][1] = (String) h.elementAt(i * 2 + 1);
                }
            }
        } catch (IOException e) {
            errMessage = "EOF before tag found or finished";
            return null;
        }
        attributes = r;
        return r;
    }

    public boolean readUntilTag(String tag) {
        return readUntilTag(tag, false);
    }

    public boolean readUntilTag(String tag, boolean readLBracket) {
        String x[][];
        for (; ; ) {
            x = readTag(readLBracket);
            readLBracket = false;
            if (x == null) return false;
            if (x[0][0].equalsIgnoreCase(tag)) {
                return true;
            }
        }
    }

    public String readTagValue(String tag) {
        return readTagValue(tag, false);
    }

    public String readTagValue(String tag, boolean readLBracket) {
        StringBuffer a = new StringBuffer(100);
        char c;
        if (!readUntilTag(tag, readLBracket)) return null;
        try {
            while ((c = ((char) diStream.read())) != '<') a.append(c);
            while (((char) diStream.read()) != '>') ;
        } catch (Exception e) {
            errMessage = "EOF before tag found or finished";
            return null;
        }
        return a.toString().trim();
    }

    public String readThisTagValue() {
        StringBuffer a = new StringBuffer(100);
        char c;
        try {
            while ((c = ((char) diStream.read())) != '<') a.append(c);
            while (((char) diStream.read()) != '>') ;
        } catch (Exception e) {
            errMessage = "EOF before tag found or finished";
            return null;
        }
        return a.toString().trim();
    }

    public String getFileName() {
        if (aFile == null) return "";
        return aFile.getName();
    }

    public String getParentDirectory() {
        if (aFile == null) return "";
        if (aFile.getParent() == null) return "";
        return aFile.getParent();
    }
}
