import java.awt.*;
import java.io.*;
import java.util.StringTokenizer;

public class XFile {

    public XFile() {
        aFile = null;
        diStream = null;
        creator = 0x43584d4c;
        type = 0x54455854;
        tokenStream = null;
        dataWriter = null;
        fPf = null;
        Delimiter = 44;
        xMode = 0;
        NFields = 0;
        isOpen = false;
        c = new char[1];
        xMode = -1;
    }

    public XFile(File someFile) {
        aFile = null;
        diStream = null;
        creator = 0x43584d4c;
        type = 0x54455854;
        tokenStream = null;
        dataWriter = null;
        fPf = null;
        Delimiter = 44;
        xMode = 0;
        NFields = 0;
        isOpen = false;
        c = new char[1];
        aFile = someFile;
        xMode = 0;
    }

    public XFile(String someFile) {
        aFile = null;
        diStream = null;
        creator = 0x43584d4c;
        type = 0x54455854;
        tokenStream = null;
        dataWriter = null;
        fPf = null;
        Delimiter = 44;
        xMode = 0;
        NFields = 0;
        isOpen = false;
        c = new char[1];
        aFile = new File(someFile);
        xMode = 0;
    }

    public boolean Open(String sstream) {
        xMode = 0;
        diStream = new BufferedReader(new StringReader(sstream));
        isOpen = true;
        return true;
    }

    public boolean Open() {
        if (xMode == -1) {
            errMessage = "Must set mode using setMode(read=0,write=1)";
            return false;
        } else {
            return Open(xMode);
        }
    }

    public boolean Open(int omode) {
        if (isOpen || diStream != null) return omode == xMode;
        xMode = omode;
        if (aFile == null) Choose(omode);
        if (xMode == 0) {
            FileInputStream theStream;
            if (aFile.canRead()) {
                try {
                    theStream = new FileInputStream(aFile);
                } catch (Exception e) {
                    errMessage = "File is not Available";
                    return false;
                }
                try {
                    diStream = new BufferedReader(new InputStreamReader(theStream));
                } catch (Exception e) {
                    errMessage = "Stream is not Available";
                    return false;
                }
                isOpen = true;
                return true;
            } else {
                return false;
            }
        } else {
            FileOutputStream theStream;
            try {
                if (aFile.exists()) theStream = new FileOutputStream(aFile); else theStream = new FileOutputStream(aFile.getPath());
            } catch (Exception e) {
                errMessage = "Write File is not Available";
                return false;
            }
            try {
                dataWriter = new BufferedWriter(new OutputStreamWriter(theStream));
            } catch (Exception e) {
                errMessage = "Stream is not Available";
                return false;
            }
            isOpen = true;
            return true;
        }
    }

    public boolean OpenPrint() {
        xMode = 1;
        FileOutputStream theStream;
        try {
            if (aFile.exists()) theStream = new FileOutputStream(aFile); else theStream = new FileOutputStream(aFile.getPath());
        } catch (Exception e) {
            errMessage = "Write File is not Available";
            return false;
        }
        try {
            dataWriter = new PrintWriter(theStream);
        } catch (Exception e) {
            errMessage = "Stream is not Available";
            return false;
        }
        isOpen = true;
        return true;
    }

    public int CountFields(String aLine) {
        int q = 0;
        int tokenIndex = 0;
        do {
            int p = aLine.indexOf(Delimiter, tokenIndex);
            q++;
            if (p <= 0) return q;
            tokenIndex = p + 2;
        } while (tokenIndex <= aLine.length());
        return q;
    }

    public int BreakFields(String aLine) {
        int ntok = 0;
        for (StringTokenizer st = new StringTokenizer(aLine); st.hasMoreTokens(); ) ;
        return 0;
    }

    public String[] ReadFormat(int format[]) {
        String aLine = ReadLineNN();
        return MakeFields(aLine, format);
    }

    public String[] MakeFields(String aLine) {
        return MakeFields(aLine, null);
    }

    public String[] MakeFields(String aLine, int format[]) {
        if (aLine == null) {
            errMessage = "EOF";
            return null;
        }
        StringTokenizer st = new StringTokenizer(aLine);
        int nf = st.countTokens();
        String retStrs[] = new String[nf];
        for (int i = 0; i < nf; i++) retStrs[i] = st.nextToken();
        if (format == null || format[0] == -1) return retStrs;
        String xretStrs[] = new String[format.length];
        for (int i = 0; i < format.length; i++) if (format[i] < nf) {
            xretStrs[i] = retStrs[format[i]];
        } else {
            errMessage = "Variable out of bounds";
            xretStrs[i] = "";
        }
        return xretStrs;
    }

    public String ReadLine() {
        if (diStream != null) {
            try {
                return diStream.readLine();
            } catch (Exception e) {
                errMessage = "Probably at End of File";
            }
            return null;
        } else {
            errMessage = "Stream not open";
            return null;
        }
    }

    public String ReadLineNN() {
        String aLine;
        do aLine = ReadLine(); while ((aLine == null || aLine.equals("")) && aLine != null);
        return aLine;
    }

    public int CountLines(boolean doEmpty) {
        boolean wasOpen = isOpen;
        int count = 0;
        if (!wasOpen && !Open()) return -1;
        String x;
        if (doEmpty) do {
            x = ReadLine();
            count++;
        } while (x != null); else do {
            x = ReadLineNN();
            count++;
        } while (x != null);
        count--;
        if (!wasOpen) Close();
        return count;
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
        if (aMessage.equals("")) if (xMode == 1) aMessage = "Write to file ..."; else aMessage = "Read from file ...";
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

    public boolean Close() {
        isOpen = false;
        if (xMode == 0) try {
            diStream.close();
            diStream = null;
        } catch (Exception e) {
            errMessage = "Can not Close Read Stream";
            return false;
        } else try {
            dataWriter.close();
            dataWriter = null;
        } catch (Exception e) {
            errMessage = "Can not Close Write Stream";
            return false;
        }
        return true;
    }

    public boolean WriteLine(String line) {
        try {
            dataWriter.write(line);
            WriteString(Eol);
            return true;
        } catch (Exception e) {
            errMessage = "Can not Write Line";
        }
        return false;
    }

    public boolean WriteString(String line) {
        try {
            dataWriter.write(line);
            return true;
        } catch (Exception e) {
            errMessage = "Can not Write Line";
        }
        return false;
    }

    public boolean WriteBytes(String line) {
        try {
            dataWriter.write(line);
            return true;
        } catch (Exception e) {
            errMessage = "Can not write bytes";
        }
        return false;
    }

    public boolean WriteByte(int bite) {
        try {
            dataWriter.write(bite);
            return true;
        } catch (Exception e) {
            errMessage = "Can not Write Byte";
        }
        return false;
    }

    public boolean WriteNumber(Number num) {
        try {
            dataWriter.write(num.toString());
            return true;
        } catch (Exception e) {
            errMessage = "Can not Write Number";
        }
        return false;
    }

    public StreamTokenizer makeTokenizer() {
        tokenStream = null;
        if (xMode == 0 && diStream != null) tokenStream = new StreamTokenizer(diStream);
        return tokenStream;
    }

    public String getStringToken() {
        if (tokenStream == null) return null;
        try {
            tokenStream.nextToken();
            String k = null;
            switch(tokenStream.ttype) {
                case -3:
                    k = tokenStream.sval;
                    break;
                case -2:
                    k = String.valueOf(tokenStream.nval);
                    break;
                case 10:
                    k = Eol;
                    break;
                case -1:
                    k = null;
                    break;
                default:
                    c[0] = (char) tokenStream.ttype;
                    k = new String(c);
                    break;
            }
            return k;
        } catch (Exception e) {
            return null;
        }
    }

    public Long getLong() {
        Double p = getDouble();
        if (p == null) return null; else return new Long(p.longValue());
    }

    public Double getDouble() {
        if (tokenStream == null) return null;
        do try {
            tokenStream.nextToken();
            String k = null;
            switch(tokenStream.ttype) {
                case -3:
                    k = tokenStream.sval;
                    break;
                case -2:
                    return new Double(tokenStream.nval);
                case 10:
                    k = Eol;
                    break;
                case -1:
                    k = null;
                    break;
                default:
                    c[0] = (char) tokenStream.ttype;
                    String s = new String(c);
                    break;
            }
        } catch (Exception e) {
            return null;
        } while (true);
    }

    public Token getToken() {
        if (tokenStream == null) return null;
        try {
            tokenStream.nextToken();
            String k = null;
            switch(tokenStream.ttype) {
                case -3:
                    k = tokenStream.sval;
                    break;
                case -2:
                    k = String.valueOf(tokenStream.nval);
                    break;
                case 10:
                    k = Eol;
                    break;
                case -1:
                    k = null;
                    break;
                default:
                    c[0] = (char) tokenStream.ttype;
                    k = new String(c);
                    break;
            }
            return new Token(k, tokenStream.ttype);
        } catch (Exception e) {
            return null;
        }
    }

    public String[] readTag() {
        return readTag(false);
    }

    public String[] readTag(boolean start) {
        StringBuffer s = new StringBuffer(100);
        StringBuffer a = new StringBuffer(100);
        String r[] = null;
        try {
            if (!start) while (diStream.read() != 60) ;
            char c;
            while ((c = (char) diStream.read()) != '>' && c != ' ') s.append(c);
            if (c == '>') {
                r = new String[1];
                r[0] = s.toString();
            } else {
                while ((c = (char) diStream.read()) != '>') a.append(c);
                String k[] = MakeFields(a.toString());
                r = new String[k.length + 1];
                r[0] = s.toString();
                for (int i = 0; i < k.length; i++) r[i + 1] = k[i];
            }
        } catch (IOException e) {
            errMessage = "EOF before tag found or finished";
            return null;
        }
        return r;
    }

    public static final String Eol = System.getProperty("line.separator");

    public File aFile;

    BufferedReader diStream;

    int creator;

    int type;

    public StreamTokenizer tokenStream;

    public Writer dataWriter;

    public static final int READ = 0;

    public static final int WRITE = 1;

    PrintFormat fPf;

    public String errMessage;

    public int Delimiter;

    int xMode;

    int NFields;

    int theFields[];

    boolean isOpen;

    private char c[];
}
