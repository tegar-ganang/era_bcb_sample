package gov.nist.javax.sip;

import java.security.MessageDigest;
import java.util.HashSet;

/**
 * A few utilities that are used in various places by the stack. This is used to
 * convert byte arrays to hex strings etc. Generate tags and branch identifiers
 * and odds and ends.
 * 
 * @author mranga
 * @version 1.2 $Revision: 1.17 $ $Date: 2009/05/11 18:52:36 $
 */
public class Utils implements UtilsExt {

    private static MessageDigest digester;

    static {
        try {
            digester = MessageDigest.getInstance("MD5");
        } catch (Exception ex) {
            throw new RuntimeException("Could not intialize Digester ", ex);
        }
    }

    private static java.util.Random rand = new java.util.Random();

    private static long counter = 0;

    private static int callIDCounter;

    private static Utils instance = new Utils();

    public static Utils getInstance() {
        return instance;
    }

    /**
	 * to hex converter
	 */
    private static final char[] toHex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
	 * convert an array of bytes to an hexadecimal string
	 * 
	 * @return a string
	 * @param b
	 *            bytes array to convert to a hexadecimal string
	 */
    public static String toHexString(byte b[]) {
        int pos = 0;
        char[] c = new char[b.length * 2];
        for (int i = 0; i < b.length; i++) {
            c[pos++] = toHex[(b[i] >> 4) & 0x0F];
            c[pos++] = toHex[b[i] & 0x0f];
        }
        return new String(c);
    }

    /**
	 * Put quotes around a string and return it.
	 * Any " characters appearing in str are escaped
	 * 
	 * @return a quoted string
	 * @param str
	 *            string to be quoted
	 */
    public static String getQuotedString(String str) {
        return '"' + str.replace("\"", "\\\"") + '"';
    }

    /**
	 * Squeeze out all white space from a string and return the reduced string.
	 * 
	 * @param input
	 *            input string to sqeeze.
	 * @return String a reduced string.
	 */
    protected static String reduceString(String input) {
        String newString = input.toLowerCase();
        int len = newString.length();
        String retval = "";
        for (int i = 0; i < len; i++) {
            if (newString.charAt(i) == ' ' || newString.charAt(i) == '\t') continue; else retval += newString.charAt(i);
        }
        return retval;
    }

    /**
	 * Generate a call identifier. This is useful when we want to generate a
	 * call identifier in advance of generating a message.
	 */
    public synchronized String generateCallIdentifier(String address) {
        String date = new Long(System.currentTimeMillis() + callIDCounter++ + rand.nextLong()).toString();
        byte cid[] = digester.digest(date.getBytes());
        String cidString = Utils.toHexString(cid);
        return cidString + "@" + address;
    }

    /**
	 * Generate a tag for a FROM header or TO header. Just return a random 4
	 * digit integer (should be enough to avoid any clashes!) Tags only need to
	 * be unique within a call.
	 * 
	 * @return a string that can be used as a tag parameter.
	 * 
	 * synchronized: needed for access to 'rand', else risk to generate same tag
	 * twice
	 */
    public synchronized String generateTag() {
        return Integer.toHexString(rand.nextInt());
    }

    /**
	 * Generate a cryptographically random identifier that can be used to
	 * generate a branch identifier.
	 * 
	 * @return a cryptographically random gloablly unique string that can be
	 *         used as a branch identifier.
	 */
    public synchronized String generateBranchId() {
        long num = rand.nextLong() + Utils.counter++ + System.currentTimeMillis();
        byte bid[] = digester.digest(Long.toString(num).getBytes());
        return SIPConstants.BRANCH_MAGIC_COOKIE + Utils.toHexString(bid);
    }

    public static void main(String[] args) {
        HashSet branchIds = new HashSet();
        for (int b = 0; b < 100000; b++) {
            String bid = Utils.getInstance().generateBranchId();
            if (branchIds.contains(bid)) {
                throw new RuntimeException("Duplicate Branch ID");
            } else {
                branchIds.add(bid);
            }
        }
        System.out.println("Done!!");
    }
}
