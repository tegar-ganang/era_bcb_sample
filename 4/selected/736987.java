package org.aha.mf4j;

/**
 * <p>
 *   Utility methods for working with permissions.
 * </p>
 * @author Arne Halvorsen (aha42)
 */
public final class Permissions {

    private Permissions() {
    }

    /**
   * <p>
   *   Convert from 
   *   {@code String} form to {@code int} form.
   * </p>
   * @param p Permissions in
   *          {@code String} form.
   * @return Permissions in {@code int} form.
   * @throws IllegalArgumentException If {@code p} not represents a permissions.
   */
    public static int toInt(String p) {
        if (p == null) {
            throw new NullPointerException("p");
        }
        if (p.equals("none")) return 0;
        if (p.equals("read")) return 1;
        if (p.equals("write")) return 2;
        if (p.equals("delete")) return 3;
        throw new IllegalArgumentException("not a permission : " + p);
    }

    /**
   * <p>
   *   Convert from {@code int} form to 
   *   {@code String} form.
   * </p>
   * @param p Permissions in {@code int} form.
   * @return Permissions in 
   *         {@code String} form.
   * @throws IllegalArgumentException If {@code p} not represents a permissions.
   */
    public static String toString(int p) {
        switch(p) {
            case 0:
                return "none";
            case 1:
                return "read";
            case 2:
                return "write";
            case 3:
                return "delete";
            default:
                throw new IllegalArgumentException("not a permission : " + p);
        }
    }

    /**
   * <p>
   *   Check if permissions in {@code int} form is valid.
   * </p>
   * @param p Permissions.
   * @throws IllegalArgumentException If {@code p} not represents a permissions. 
   */
    public static void checkPermission(String p) {
        if (p.equals("none") || p.equals("read") || p.equals("write") || p.equals("delete")) return;
        throw new IllegalArgumentException("not a permission : " + p);
    }

    /**
   * <p>
   *   Check if permissions in {@code int} form is valid.
   * </p>
   * @param p Permissions.
   * @throws IllegalArgumentException If {@code p} not represents a permissions. 
   */
    public static void checkPermission(int p) {
        if (p < 0 || p > 3) {
            throw new IllegalArgumentException("not a permission : " + p);
        }
    }

    /**
   * <p>
   *   Tells if got needed permissions.
   * </p>
   * @param need Permissions we need.
   * @param has  Permissions we has.
   * @return {@code true} if we got permissions we need, {@code false} if not.
   * @throws IllegalArgumentException If {@code need} or {@code has} not 
   *         permissions. 
   */
    public static boolean hasPermission(String need, String has) {
        if (need == null) {
            throw new NullPointerException("need");
        }
        return toInt(need) < toInt(has);
    }

    /**
   * <p>
   *   Tells if got needed permissions.
   * </p>
   * @param need Permissions we need.
   * @param has  Permissions we has.
   * @return {@code true} if we got permissions we need, {@code false} if not.
   * @throws IllegalArgumentException If {@code need} or {@code has} not 
   *         permissions. 
   */
    public static boolean hasPermission(int need, String has) {
        if (has == null) {
            throw new NullPointerException("has");
        }
        checkPermission(need);
        return need <= toInt(has);
    }

    /**
   * <p>
   *   Tells if got needed permissions.
   * </p>
   * @param need Permissions we need.
   * @param has  Permissions we has.
   * @return {@code true} if we got permissions we need, {@code false} if not.
   * @throws IllegalArgumentException If {@code need} or {@code has} not 
   *         permissions. 
   */
    public static boolean hasPermission(String need, int has) {
        if (need == null) {
            throw new NullPointerException("need");
        }
        checkPermission(has);
        return toInt(need) <= has;
    }

    /**
   * <p>
   *   Tells if got needed permissions.
   * </p>
   * @param need Permissions we need.
   * @param has  Permissions we has.
   * @return {@code true} if we got permissions we need, {@code false} if not.
   * @throws IllegalArgumentException If {@code need} or {@code has} not 
   *         permissions.
   */
    public static boolean hasPermission(int need, int has) {
        checkPermission(need);
        checkPermission(has);
        return need <= has;
    }
}
