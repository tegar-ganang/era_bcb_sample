package lrf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;

public class Utils {

    static Hashtable<Short, String> ent = new Hashtable<Short, String>();

    static Hashtable<String, String> replacesEnt = new Hashtable<String, String>();

    static {
        ent.put(new Short((short) 160), "nbsp");
        ent.put(new Short((short) 161), "iexcl");
        ent.put(new Short((short) 162), "cent");
        ent.put(new Short((short) 163), "pound");
        ent.put(new Short((short) 164), "curren");
        ent.put(new Short((short) 165), "yen");
        ent.put(new Short((short) 166), "brvbar");
        ent.put(new Short((short) 167), "sect");
        ent.put(new Short((short) 168), "uml");
        ent.put(new Short((short) 169), "copy");
        ent.put(new Short((short) 170), "ordf");
        ent.put(new Short((short) 171), "laquo");
        ent.put(new Short((short) 172), "not");
        ent.put(new Short((short) 173), "shy");
        ent.put(new Short((short) 174), "reg");
        ent.put(new Short((short) 175), "macr");
        ent.put(new Short((short) 176), "deg");
        ent.put(new Short((short) 177), "plusmn");
        ent.put(new Short((short) 178), "sup2");
        ent.put(new Short((short) 179), "sup3");
        ent.put(new Short((short) 180), "acute");
        ent.put(new Short((short) 181), "micro");
        ent.put(new Short((short) 182), "para");
        ent.put(new Short((short) 183), "middot");
        ent.put(new Short((short) 184), "cedil");
        ent.put(new Short((short) 185), "sup1");
        ent.put(new Short((short) 186), "ordm");
        ent.put(new Short((short) 187), "raquo");
        ent.put(new Short((short) 188), "frac14");
        ent.put(new Short((short) 189), "frac12");
        ent.put(new Short((short) 190), "frac34");
        ent.put(new Short((short) 191), "iquest");
        ent.put(new Short((short) 192), "Agrave");
        ent.put(new Short((short) 193), "Aacute");
        ent.put(new Short((short) 194), "Acirc");
        ent.put(new Short((short) 195), "Atilde");
        ent.put(new Short((short) 196), "Auml");
        ent.put(new Short((short) 197), "Aring");
        ent.put(new Short((short) 198), "AElig");
        ent.put(new Short((short) 199), "Ccedil");
        ent.put(new Short((short) 200), "Egrave");
        ent.put(new Short((short) 201), "Eacute");
        ent.put(new Short((short) 202), "Ecirc");
        ent.put(new Short((short) 203), "Euml");
        ent.put(new Short((short) 204), "Igrave");
        ent.put(new Short((short) 205), "Iacute");
        ent.put(new Short((short) 206), "Icirc");
        ent.put(new Short((short) 207), "Iuml");
        ent.put(new Short((short) 208), "ETH");
        ent.put(new Short((short) 209), "Ntilde");
        ent.put(new Short((short) 210), "Ograve");
        ent.put(new Short((short) 211), "Oacute");
        ent.put(new Short((short) 212), "Ocirc");
        ent.put(new Short((short) 213), "Otilde");
        ent.put(new Short((short) 214), "Ouml");
        ent.put(new Short((short) 215), "times");
        ent.put(new Short((short) 216), "Oslash");
        ent.put(new Short((short) 217), "Ugrave");
        ent.put(new Short((short) 218), "Uacute");
        ent.put(new Short((short) 219), "Ucirc");
        ent.put(new Short((short) 220), "Uuml");
        ent.put(new Short((short) 221), "Yacute");
        ent.put(new Short((short) 222), "THORN");
        ent.put(new Short((short) 223), "szlig");
        ent.put(new Short((short) 224), "agrave");
        ent.put(new Short((short) 225), "aacute");
        ent.put(new Short((short) 226), "acirc");
        ent.put(new Short((short) 227), "atilde");
        ent.put(new Short((short) 228), "auml");
        ent.put(new Short((short) 229), "aring");
        ent.put(new Short((short) 230), "aelig");
        ent.put(new Short((short) 231), "ccedil");
        ent.put(new Short((short) 232), "egrave");
        ent.put(new Short((short) 233), "eacute");
        ent.put(new Short((short) 234), "ecirc");
        ent.put(new Short((short) 235), "euml");
        ent.put(new Short((short) 236), "igrave");
        ent.put(new Short((short) 237), "iacute");
        ent.put(new Short((short) 238), "icirc");
        ent.put(new Short((short) 239), "iuml");
        ent.put(new Short((short) 240), "eth");
        ent.put(new Short((short) 241), "ntilde");
        ent.put(new Short((short) 242), "ograve");
        ent.put(new Short((short) 243), "oacute");
        ent.put(new Short((short) 244), "ocirc");
        ent.put(new Short((short) 245), "otilde");
        ent.put(new Short((short) 246), "ouml");
        ent.put(new Short((short) 247), "divide");
        ent.put(new Short((short) 248), "oslash");
        ent.put(new Short((short) 249), "ugrave");
        ent.put(new Short((short) 250), "uacute");
        ent.put(new Short((short) 251), "ucirc");
        ent.put(new Short((short) 252), "uuml");
        ent.put(new Short((short) 253), "yacute");
        ent.put(new Short((short) 254), "thorn");
        ent.put(new Short((short) 255), "yuml");
        ent.put(new Short((short) 402), "fnof");
        ent.put(new Short((short) 913), "Alpha");
        ent.put(new Short((short) 914), "Beta");
        ent.put(new Short((short) 915), "Gamma");
        ent.put(new Short((short) 916), "Delta");
        ent.put(new Short((short) 917), "Epsilon");
        ent.put(new Short((short) 918), "Zeta");
        ent.put(new Short((short) 919), "Eta");
        ent.put(new Short((short) 920), "Theta");
        ent.put(new Short((short) 921), "Iota");
        ent.put(new Short((short) 922), "Kappa");
        ent.put(new Short((short) 923), "Lambda");
        ent.put(new Short((short) 924), "Mu");
        ent.put(new Short((short) 925), "Nu");
        ent.put(new Short((short) 926), "Xi");
        ent.put(new Short((short) 927), "Omicron");
        ent.put(new Short((short) 928), "Pi");
        ent.put(new Short((short) 929), "Rho");
        ent.put(new Short((short) 931), "Sigma");
        ent.put(new Short((short) 932), "Tau");
        ent.put(new Short((short) 933), "Upsilon");
        ent.put(new Short((short) 934), "Phi");
        ent.put(new Short((short) 935), "Chi");
        ent.put(new Short((short) 936), "Psi");
        ent.put(new Short((short) 937), "Omega");
        ent.put(new Short((short) 945), "alpha");
        ent.put(new Short((short) 946), "beta");
        ent.put(new Short((short) 947), "gamma");
        ent.put(new Short((short) 948), "delta");
        ent.put(new Short((short) 949), "epsilon");
        ent.put(new Short((short) 950), "zeta");
        ent.put(new Short((short) 951), "eta");
        ent.put(new Short((short) 952), "theta");
        ent.put(new Short((short) 953), "iota");
        ent.put(new Short((short) 954), "kappa");
        ent.put(new Short((short) 955), "lambda");
        ent.put(new Short((short) 956), "mu");
        ent.put(new Short((short) 957), "nu");
        ent.put(new Short((short) 958), "xi");
        ent.put(new Short((short) 959), "omicron");
        ent.put(new Short((short) 960), "pi");
        ent.put(new Short((short) 961), "rho");
        ent.put(new Short((short) 962), "sigmaf");
        ent.put(new Short((short) 963), "sigma");
        ent.put(new Short((short) 964), "tau");
        ent.put(new Short((short) 965), "upsilon");
        ent.put(new Short((short) 966), "phi");
        ent.put(new Short((short) 967), "chi");
        ent.put(new Short((short) 968), "psi");
        ent.put(new Short((short) 969), "omega");
        ent.put(new Short((short) 977), "thetasym");
        ent.put(new Short((short) 978), "upsih");
        ent.put(new Short((short) 982), "piv");
        ent.put(new Short((short) 8226), "bull");
        ent.put(new Short((short) 8230), "hellip");
        ent.put(new Short((short) 8242), "prime");
        ent.put(new Short((short) 8243), "Prime");
        ent.put(new Short((short) 8254), "oline");
        ent.put(new Short((short) 8260), "frasl");
        ent.put(new Short((short) 8472), "weierp");
        ent.put(new Short((short) 8465), "image");
        ent.put(new Short((short) 8476), "real");
        ent.put(new Short((short) 8482), "trade");
        ent.put(new Short((short) 8501), "alefsym");
        ent.put(new Short((short) 8592), "larr");
        ent.put(new Short((short) 8593), "uarr");
        ent.put(new Short((short) 8594), "rarr");
        ent.put(new Short((short) 8595), "darr");
        ent.put(new Short((short) 8596), "harr");
        ent.put(new Short((short) 8629), "crarr");
        ent.put(new Short((short) 8656), "lArr");
        ent.put(new Short((short) 8657), "uArr");
        ent.put(new Short((short) 8658), "rArr");
        ent.put(new Short((short) 8659), "dArr");
        ent.put(new Short((short) 8660), "hArr");
        ent.put(new Short((short) 8704), "forall");
        ent.put(new Short((short) 8706), "part");
        ent.put(new Short((short) 8707), "exist");
        ent.put(new Short((short) 8709), "empty");
        ent.put(new Short((short) 8711), "nabla");
        ent.put(new Short((short) 8712), "isin");
        ent.put(new Short((short) 8713), "notin");
        ent.put(new Short((short) 8715), "ni");
        ent.put(new Short((short) 8719), "prod");
        ent.put(new Short((short) 8721), "sum");
        ent.put(new Short((short) 8722), "minus");
        ent.put(new Short((short) 8727), "lowast");
        ent.put(new Short((short) 8730), "radic");
        ent.put(new Short((short) 8733), "prop");
        ent.put(new Short((short) 8734), "infin");
        ent.put(new Short((short) 8736), "ang");
        ent.put(new Short((short) 8743), "and");
        ent.put(new Short((short) 8744), "or");
        ent.put(new Short((short) 8745), "cap");
        ent.put(new Short((short) 8746), "cup");
        ent.put(new Short((short) 8747), "int");
        ent.put(new Short((short) 8756), "there4");
        ent.put(new Short((short) 8764), "sim");
        ent.put(new Short((short) 8773), "cong");
        ent.put(new Short((short) 8776), "asymp");
        ent.put(new Short((short) 8800), "ne");
        ent.put(new Short((short) 8801), "equiv");
        ent.put(new Short((short) 8804), "le");
        ent.put(new Short((short) 8805), "ge");
        ent.put(new Short((short) 8834), "sub");
        ent.put(new Short((short) 8835), "sup");
        ent.put(new Short((short) 8836), "nsub");
        ent.put(new Short((short) 8838), "sube");
        ent.put(new Short((short) 8839), "supe");
        ent.put(new Short((short) 8853), "oplus");
        ent.put(new Short((short) 8855), "otimes");
        ent.put(new Short((short) 8869), "perp");
        ent.put(new Short((short) 8901), "sdot");
        ent.put(new Short((short) 8968), "lceil");
        ent.put(new Short((short) 8969), "rceil");
        ent.put(new Short((short) 8970), "lfloor");
        ent.put(new Short((short) 8971), "rfloor");
        ent.put(new Short((short) 9001), "lang");
        ent.put(new Short((short) 9002), "rang");
        ent.put(new Short((short) 9674), "loz");
        ent.put(new Short((short) 9824), "spades");
        ent.put(new Short((short) 9827), "clubs");
        ent.put(new Short((short) 9829), "hearts");
        ent.put(new Short((short) 9830), "diams");
        ent.put(new Short((short) 34), "quot");
        ent.put(new Short((short) 60), "lt");
        ent.put(new Short((short) 62), "gt");
        ent.put(new Short((short) 338), "OElig");
        ent.put(new Short((short) 339), "oelig");
        ent.put(new Short((short) 352), "Scaron");
        ent.put(new Short((short) 353), "scaron");
        ent.put(new Short((short) 376), "Yuml");
        ent.put(new Short((short) 710), "circ");
        ent.put(new Short((short) 732), "tilde");
        ent.put(new Short((short) 8194), "ensp");
        ent.put(new Short((short) 8195), "emsp");
        ent.put(new Short((short) 8201), "thinsp");
        ent.put(new Short((short) 8204), "zwnj");
        ent.put(new Short((short) 8205), "zwj");
        ent.put(new Short((short) 8206), "lrm");
        ent.put(new Short((short) 8207), "rlm");
        ent.put(new Short((short) 8211), "ndash");
        ent.put(new Short((short) 8212), "mdash");
        ent.put(new Short((short) 8216), "lsquo");
        ent.put(new Short((short) 8217), "rsquo");
        ent.put(new Short((short) 8218), "sbquo");
        ent.put(new Short((short) 8220), "ldquo");
        ent.put(new Short((short) 8221), "rdquo");
        ent.put(new Short((short) 8222), "bdquo");
        ent.put(new Short((short) 8224), "dagger");
        ent.put(new Short((short) 8225), "Dagger");
        ent.put(new Short((short) 8240), "permil");
        ent.put(new Short((short) 8249), "lsaquo");
        ent.put(new Short((short) 8250), "rsaquo");
        ent.put(new Short((short) 8364), "euro");
        for (Enumeration<Short> enu = ent.keys(); enu.hasMoreElements(); ) {
            Short k1 = enu.nextElement();
            String k2 = Character.toString((char) k1.intValue());
            String v2 = "&" + ent.get(k1) + ";";
            replacesEnt.put(k2, v2);
        }
    }

    ;

    public static String toTOCText(String app) {
        app = toUnhandText(app);
        app = app.replace("�", "");
        app = app.replace("'", "");
        app = app.replace("�", "");
        app = app.replace("\"", "");
        return app;
    }

    public static String toXMLText(String app) {
        app = app.replace("&", "&amp;");
        for (Enumeration<String> search = replacesEnt.keys(); search.hasMoreElements(); ) {
            String s = search.nextElement();
            String r = replacesEnt.get(s);
            app = app.replace(s, r);
        }
        return app;
    }

    public static String toUnhandText(String app) {
        app = app.replace('', ' ');
        app = app.replace("�", "'");
        app = app.replace("�", "'");
        app = app.replace("\"", "'");
        app = app.replace("�", "'");
        app = app.replace("�", "'");
        app = app.replace("�", "-");
        app = app.replace("�", "...");
        app = app.replace("�", "AE");
        app = app.replace("�", "a");
        app = app.replace("�", "e");
        app = app.replace("�", "i");
        app = app.replace("�", "o");
        app = app.replace("�", "u");
        app = app.replace("�", "A");
        app = app.replace("�", "E");
        app = app.replace("�", "I");
        app = app.replace("�", "O");
        app = app.replace("�", "U");
        app = app.replace("�", "n");
        app = app.replace("�", "N");
        app = app.replace("\t", " ");
        app = app.replace("\n", " ");
        byte n[] = app.getBytes();
        for (int i = 0; i < n.length; i++) {
            if (n[i] < 0) n[i] = 32;
        }
        app = new String(n);
        return app;
    }

    public static void writeTo(InputStream is, OutputStream os) throws IOException {
        int readed;
        byte buf[] = new byte[1024 * 1024];
        while ((readed = is.read(buf, 0, buf.length)) > 0) {
            os.write(buf, 0, readed);
        }
    }

    public static byte[] getResourceAsByteArray(String rsName) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeTo("".getClass().getResourceAsStream(rsName), baos);
        return baos.toByteArray();
    }

    public static byte[] readBytesFromStream(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeTo(is, baos);
        return baos.toByteArray();
    }

    public static String surroundWithQuotes(String style, String tgt) {
        int pos = style.indexOf(tgt);
        if (pos >= 0) {
            pos += tgt.length();
            while (style.charAt(pos) != ':') pos++;
            pos++;
            while (Character.isWhitespace(style.charAt(pos))) pos++;
            int pos2 = pos;
            while (style.charAt(pos2) != ';') pos2++;
            style = style.substring(0, pos) + "\"" + style.substring(pos, pos2) + "\"" + style.substring(pos2);
        }
        return style;
    }

    public static String getValueFromStyle(String style, String tgt) {
        int pos = style.indexOf(tgt);
        if (pos >= 0) {
            pos += tgt.length();
            while (style.charAt(pos) != ':') pos++;
            pos++;
            while (Character.isWhitespace(style.charAt(pos))) pos++;
            int pos2 = pos;
            while (style.charAt(pos2) != ';') pos2++;
            return style.substring(pos, pos2);
        }
        return "";
    }

    public static String setValueFromStyle(String style, String tgt, String val) {
        int pos = style.indexOf(tgt);
        if (pos >= 0) {
            pos += tgt.length();
            while (style.charAt(pos) != ':') pos++;
            pos++;
            while (Character.isWhitespace(style.charAt(pos))) pos++;
            int pos2 = pos;
            while (style.charAt(pos2) != ';') pos2++;
            style = style.substring(0, pos) + "\"" + val + "\"" + style.substring(pos2);
        }
        return style;
    }
}
