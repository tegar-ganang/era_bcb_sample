package news_rack.database;

import java.io.UnsupportedEncodingException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.misc.BASE64Encoder;
import sun.misc.CharacterEncoder;
import news_rack.util.Misc;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Date;
import news_rack.GlobalConstants;
import news_rack.util.Tuple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Adapted from http://www.devbistro.com/articles/Java/Password-Encryption **/
public final class PasswordService {

    private static final int RESET_KEY_VALIDITY = 30;

    private static Log _log = LogFactory.getLog("news_rack.database.PasswordService.class");

    private static int _resetKeyValidityTime = 0;

    private static PasswordService _instance;

    private static Map<String, Tuple> _passwordResetKeys;

    public static void Init() {
        _instance = new PasswordService();
        _passwordResetKeys = new HashMap<String, Tuple>();
        String rkvString = GlobalConstants.GetProperty("password.resetkey.timevalidity");
        try {
            if (rkvString != null) _resetKeyValidityTime = Integer.parseInt(rkvString); else _resetKeyValidityTime = RESET_KEY_VALIDITY;
        } catch (Exception e) {
            _log.error("Error parsing password.resetkey.timevalidity string " + rkvString);
            _resetKeyValidityTime = RESET_KEY_VALIDITY;
        }
    }

    public static synchronized PasswordService getInstance() {
        if (_instance == null) Init();
        return _instance;
    }

    private PasswordService() {
    }

    public static synchronized String encrypt(String plaintext) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(plaintext.getBytes("UTF-8"));
        byte raw[] = md.digest();
        return (new BASE64Encoder()).encode(raw);
    }

    public static String GetRandomKey() {
        Random r1 = new Random();
        String t1 = Long.toString(Math.abs(r1.nextLong()), 36);
        Random r2 = new Random();
        String t2 = Long.toString(Math.abs(r2.nextLong()), 36);
        return t1 + t2;
    }

    public static String GetPasswordResetKey(String uid) {
        String pwResetKey = GetRandomKey();
        _passwordResetKeys.put(uid, new Tuple(pwResetKey, new Date()));
        return pwResetKey;
    }

    public static boolean IsAValidPasswordResetKey(String uid, String resetKey) {
        Tuple t = _passwordResetKeys.get(uid);
        if ((t == null) || !resetKey.equals(t._a)) {
            _log.error("Invalid password reset key " + resetKey + " for " + uid);
            return false;
        }
        long timeDiff = (new Date()).getTime() - ((Date) t._b).getTime();
        if (timeDiff > (1000 * 60 * _resetKeyValidityTime)) {
            _log.error("Expired password reset key for " + uid + ". Generated " + timeDiff / 1000 + " seconds back.  Max validity is for " + _resetKeyValidityTime + " minutes!");
            return false;
        }
        return true;
    }

    public static void InvalidatePasswordResetKey(String uid) {
        if (uid != null) _passwordResetKeys.remove(uid);
    }

    public static void FixUserTable() throws Exception {
        PrintWriter pw = Misc.GetUTF8Writer("user.table.xml.NEW");
        BufferedReader br = new BufferedReader(new FileReader("user.table.xml"));
        String line;
        while ((line = br.readLine()) != null) {
            int x = line.indexOf("password=\"");
            if (x == -1) {
                pw.println(line);
            } else {
                x += 10;
                int y = line.indexOf("\"", x);
                String a = line.substring(0, x);
                String pwd = line.substring(x, y);
                String b = getInstance().encrypt(pwd);
                String c = line.substring(y);
                pw.println(a + b + c);
                pw.println("\t\t\tpassword-plain=\"" + pwd + c);
                System.out.println("a        = '" + a + "'");
                System.out.println("password = '" + pwd + "'");
                System.out.println("b        = '" + b + "'");
                System.out.println("c        = '" + c + "'");
                System.out.println("ORIG     = '" + a + pwd + c + "'");
                System.out.println("a+b+c    = '" + a + b + c + "'");
            }
        }
        br.close();
        pw.close();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Got key " + GetRandomKey());
    }
}
