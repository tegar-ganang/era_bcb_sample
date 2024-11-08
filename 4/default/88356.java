import java.awt.*;
import java.io.*;
import java.util.StringTokenizer;

public class XFile {

    public XFile() {
        creator = 0x43584d4c;
        type = 0x54455854;
        Delimiter = 44;
        xMode = 0;
        isOpen = false;
        c = new char[1];
        xMode = -1;
    }

    public XFile(File file) {
        creator = 0x43584d4c;
        type = 0x54455854;
        Delimiter = 44;
        xMode = 0;
        isOpen = false;
        c = new char[1];
        aFile = file;
        xMode = 0;
    }

    public XFile(String s) {
        creator = 0x43584d4c;
        type = 0x54455854;
        Delimiter = 44;
        xMode = 0;
        isOpen = false;
        c = new char[1];
        aFile = new File(s);
        xMode = 0;
    }

    public boolean Open(String s) {
        xMode = 0;
        diStream = new BufferedReader(new StringReader(s));
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

    public boolean Open(int i) {
        if (isOpen || diStream != null) return i == xMode;
        xMode = i;
        if (aFile == null) Choose(i);
        if (xMode == 0) if (aFile.canRead()) {
            FileInputStream fileinputstream;
            try {
                fileinputstream = new FileInputStream(aFile);
            } catch (Exception _ex) {
                errMessage = "File is not Available";
                return false;
            }
            try {
                diStream = new BufferedReader(new InputStreamReader(fileinputstream, "ISO-8859-1"));
            } catch (Exception _ex) {
                errMessage = "Stream is not Available";
                return false;
            }
            isOpen = true;
            return true;
        } else {
            return false;
        }
        FileOutputStream fileoutputstream;
        try {
            if (aFile.exists()) fileoutputstream = new FileOutputStream(aFile); else fileoutputstream = new FileOutputStream(aFile.getPath());
        } catch (Exception _ex) {
            errMessage = "Write File is not Available";
            return false;
        }
        try {
            dataWriter = new BufferedWriter(new OutputStreamWriter(fileoutputstream, "ISO-8859-1"));
        } catch (Exception _ex) {
            errMessage = "Stream is not Available";
            return false;
        }
        isOpen = true;
        return true;
    }

    public boolean OpenPrint() {
        xMode = 1;
        FileOutputStream fileoutputstream;
        try {
            if (aFile.exists()) fileoutputstream = new FileOutputStream(aFile); else fileoutputstream = new FileOutputStream(aFile.getPath());
        } catch (Exception _ex) {
            errMessage = "Write File is not Available";
            return false;
        }
        try {
            dataWriter = new PrintWriter(fileoutputstream);
        } catch (Exception _ex) {
            errMessage = "Stream is not Available";
            return false;
        }
        isOpen = true;
        return true;
    }

    public int CountFields(String s) {
        int j = 0;
        int k = 0;
        do {
            int i = s.indexOf(Delimiter, k);
            j++;
            if (i <= 0) return j;
            k = i + 2;
        } while (k <= s.length());
        return j;
    }

    public int BreakFields(String s) {
        for (StringTokenizer stringtokenizer = new StringTokenizer(s); stringtokenizer.hasMoreTokens(); ) ;
        return 0;
    }

    public String[] ReadFormat(int ai[]) {
        String s = ReadLineNN();
        return MakeFields(s, ai);
    }

    public String[] MakeFields(String s) {
        return MakeFields(s, null);
    }

    public String[] MakeFields(String s, int ai[]) {
        if (s == null) {
            errMessage = "EOF";
            return null;
        }
        StringTokenizer stringtokenizer = new StringTokenizer(s);
        int k = stringtokenizer.countTokens();
        String as[] = new String[k];
        for (int i = 0; i < k; i++) as[i] = stringtokenizer.nextToken();
        if (ai == null || ai[0] == -1) return as;
        String as1[] = new String[ai.length];
        for (int j = 0; j < ai.length; j++) if (ai[j] < k) {
            as1[j] = as[ai[j]];
        } else {
            errMessage = "Variable out of bounds";
            as1[j] = "";
        }
        return as1;
    }

    public String ReadLine() {
        if (diStream != null) {
            try {
                return diStream.readLine();
            } catch (Exception _ex) {
                errMessage = "Probably at End of File";
            }
            return null;
        } else {
            errMessage = "Stream not open";
            return null;
        }
    }

    public String ReadLineNN() {
        String s;
        do s = ReadLine(); while ((s == null || s.equals("")) && s != null);
        return s;
    }

    public int CountLines(boolean flag) {
        boolean flag1 = isOpen;
        int i = 0;
        if (!flag1 && !Open()) return -1;
        String s;
        if (flag) do {
            s = ReadLine();
            i++;
        } while (s != null); else do {
            s = ReadLineNN();
            i++;
        } while (s != null);
        i--;
        if (!flag1) Close();
        return i;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public boolean Choose(int i) {
        return Choose(i, "", "");
    }

    public boolean Choose(int i, String s) {
        return Choose(i, s, "");
    }

    public boolean Choose(int i, String s, String s1) {
        xMode = i;
        if (xMode == -1) return false;
        Frame frame = new Frame();
        if (s1.equals("")) if (xMode == 1) s1 = "Write to file ..."; else s1 = "Read from file ...";
        FileDialog filedialog = new FileDialog(frame, s1, xMode);
        filedialog.setFile(s);
        try {
            filedialog.show();
            aFile = new File(filedialog.getDirectory().substring(0, filedialog.getDirectory().length() - 1), filedialog.getFile());
        } catch (Exception _ex) {
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
        } catch (Exception _ex) {
            errMessage = "Can not Close Read Stream";
            return false;
        } else try {
            dataWriter.close();
            dataWriter = null;
        } catch (Exception _ex) {
            errMessage = "Can not Close Write Stream";
            return false;
        }
        return true;
    }

    public boolean WriteLine(String s) {
        try {
            dataWriter.write(s);
            WriteString(Eol);
            return true;
        } catch (Exception _ex) {
            errMessage = "Can not Write Line";
        }
        return false;
    }

    public boolean WriteString(String s) {
        try {
            dataWriter.write(s);
            return true;
        } catch (Exception _ex) {
            errMessage = "Can not Write Line";
        }
        return false;
    }

    public boolean WriteBytes(String s) {
        try {
            dataWriter.write(s);
            return true;
        } catch (Exception _ex) {
            errMessage = "Can not write bytes";
        }
        return false;
    }

    public boolean WriteByte(int i) {
        try {
            dataWriter.write(i);
            return true;
        } catch (Exception _ex) {
            errMessage = "Can not Write Byte";
        }
        return false;
    }

    public boolean WriteNumber(Number number) {
        try {
            dataWriter.write(number.toString());
            return true;
        } catch (Exception _ex) {
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
            String s = null;
            switch(tokenStream.ttype) {
                case -3:
                    s = tokenStream.sval;
                    break;
                case -2:
                    s = String.valueOf(tokenStream.nval);
                    break;
                case 10:
                    s = Eol;
                    break;
                case -1:
                    s = null;
                    break;
                default:
                    c[0] = (char) tokenStream.ttype;
                    s = new String(c);
                    break;
            }
            return s;
        } catch (Exception _ex) {
            return null;
        }
    }

    public Long getLong() {
        Double double1 = getDouble();
        if (double1 == null) return null; else return new Long(double1.longValue());
    }

    public Double getDouble() {
        if (tokenStream == null) return null;
        do try {
            tokenStream.nextToken();
            String s = null;
            switch(tokenStream.ttype) {
                case -3:
                    s = tokenStream.sval;
                    break;
                case -2:
                    return new Double(tokenStream.nval);
                case 10:
                    s = Eol;
                    break;
                case -1:
                    s = null;
                    break;
                default:
                    c[0] = (char) tokenStream.ttype;
                    String s1 = new String(c);
                    break;
            }
        } catch (Exception _ex) {
            return null;
        } while (true);
    }

    public Token getToken() {
        if (tokenStream == null) return null;
        try {
            tokenStream.nextToken();
            String s = null;
            switch(tokenStream.ttype) {
                case -3:
                    s = tokenStream.sval;
                    break;
                case -2:
                    s = String.valueOf(tokenStream.nval);
                    break;
                case 10:
                    s = Eol;
                    break;
                case -1:
                    s = null;
                    break;
                default:
                    c[0] = (char) tokenStream.ttype;
                    s = new String(c);
                    break;
            }
            return new Token(s, tokenStream.ttype);
        } catch (Exception _ex) {
            return null;
        }
    }

    public String[] readTag() {
        return readTag(false);
    }

    public String[] readTag(boolean flag) {
        StringBuffer stringbuffer = new StringBuffer(100);
        StringBuffer stringbuffer1 = new StringBuffer(100);
        String as[] = null;
        try {
            if (!flag) while (diStream.read() != 60) ;
            char c1;
            while ((c1 = (char) diStream.read()) != '>' && c1 != ' ') stringbuffer.append(c1);
            if (c1 == '>') {
                as = new String[1];
                as[0] = stringbuffer.toString();
            } else {
                char c2;
                while ((c2 = (char) diStream.read()) != '>') stringbuffer1.append(c2);
                String as1[] = MakeFields(stringbuffer1.toString());
                as = new String[as1.length + 1];
                as[0] = stringbuffer.toString();
                for (int i = 0; i < as1.length; i++) as[i + 1] = as1[i];
            }
        } catch (IOException _ex) {
            errMessage = "EOF before tag found or finished";
            return null;
        }
        return as;
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
