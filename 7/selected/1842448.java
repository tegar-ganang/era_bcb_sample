package issrg.pba.credentials;

/**
 * This is the class that represents an inseparable collection of properties: a product.
 *
 * @see Property
 */
public class Product extends DefaultCredentialBehaviour {

    protected Credential[] product;

    protected Product() {
    }

    public Product(Credential[] product) {
        this.product = normalise(product);
    }

    public boolean contains(Property p) {
        Property[] p1 = (p instanceof Product) ? ((Product) p).product : new Property[] { p };
        return contains(product, p1);
    }

    public boolean partOf(Property p) {
        return contains(new Property[] { p }, product);
    }

    private static boolean contains(Property[] p1, Property[] p2) {
        if (p2 == null) {
            return true;
        }
        if (p1 == null) {
            return false;
        }
        boolean[] r = new boolean[p2.length];
        p1_loop: for (int i = 0; i < p1.length; i++) {
            for (int j = 0; j < p2.length; j++) {
                if (!r[j] && p1[i].contains(p2[j])) {
                    r[j] = true;
                    continue p1_loop;
                }
            }
            return false;
        }
        return true;
    }

    public Credential intersect(Credential c) {
        Credential[] p1 = (c instanceof Product) ? ((Product) c).product : new Credential[] { c };
        boolean[] r = new boolean[p1.length];
        java.util.Vector result = new java.util.Vector();
        for (int i = 0; i < product.length; i++) {
            Credential b = null;
            for (int j = 0; j < p1.length; j++) {
                if (!r[j] && (b = product[i].intersect(p1[j])) != null) {
                    r[j] = true;
                    break;
                }
            }
            if (b == null) {
                b = product[i];
            }
            result.add(b);
        }
        for (int j = 0; j < p1.length; j++) {
            if (!r[j]) {
                result.add(p1[j]);
            }
        }
        p1 = new Credential[result.size()];
        System.arraycopy(result.toArray(), 0, p1, 0, p1.length);
        return new Product(p1);
    }

    /**
   * This method should be used only at construction time for updating the actual chain of credentials.
   * In fact, it is a multiplication of two credentials.
   *
   * <p>This method does not check if it already contains this credential or not. You can always do
   * that yourself.
   *
   * @param c is the credential to link to the chain
   */
    protected void link(Credential c) {
        Credential[] p1 = new Credential[product.length + 1];
        p1[0] = c;
        System.arraycopy(product, 0, p1, 1, product.length);
        product = normalise(p1);
    }

    private static Credential[] normalise(Credential[] product) {
        Credential[] p1 = new Credential[product.length];
        System.arraycopy(product, 0, p1, 0, p1.length);
        for (int i = p1.length - 1; i-- > 0; ) {
            Credential c = p1[i];
            int j;
            for (j = i; j < p1.length - 1; j++) {
                if (c.contains(p1[j + 1])) {
                    break;
                } else {
                    p1[j] = p1[j + 1];
                }
            }
            p1[j] = c;
        }
        return p1;
    }

    public String toString() {
        if (product == null) {
            return null;
        }
        StringBuffer sb = new StringBuffer("p[");
        for (int i = 0; i < product.length; i++) {
            if (i > 0) {
                sb.append(" : ");
            }
            sb.append(product[i] == null ? "null" : product[i].toString());
        }
        return sb.append("]").toString();
    }
}
