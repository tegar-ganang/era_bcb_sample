package java.util;

import java.security.Permission;
import java.security.PermissionCollection;

/**
 * This class provides the implementation for
 * <code>PropertyPermission.newPermissionCollection()</code>. It only accepts
 * PropertyPermissions, and correctly implements <code>implies</code>. It
 * is synchronized, as specified in the superclass.
 *
 * @author Eric Blake (ebb9@email.byu.edu)
 * @status an undocumented class, but this matches Sun's serialization
 */
class PropertyPermissionCollection extends PermissionCollection {

    /**
   * Compatible with JDK 1.4.
   */
    private static final long serialVersionUID = 7015263904581634791L;

    /**
   * The permissions.
   *
   * @serial the table of permissions in the collection
   */
    private final Hashtable permissions = new Hashtable();

    /**
   * A flag to detect if "*" is in the collection.
   *
   * @serial true if "*" is in the collection 
   */
    private boolean all_allowed;

    /**
   * Adds a PropertyPermission to this collection.
   *
   * @param permission the permission to add
   * @throws IllegalArgumentException if permission is not a PropertyPermission
   * @throws SecurityException if collection is read-only
   */
    public void add(Permission permission) {
        if (isReadOnly()) throw new SecurityException("readonly");
        if (!(permission instanceof PropertyPermission)) throw new IllegalArgumentException();
        PropertyPermission pp = (PropertyPermission) permission;
        String name = pp.getName();
        if (name.equals("*")) all_allowed = true;
        PropertyPermission old = (PropertyPermission) permissions.get(name);
        if (old != null) {
            if ((pp.actions | old.actions) == old.actions) pp = old; else if ((pp.actions | old.actions) != pp.actions) pp = new PropertyPermission(name, "read,write");
        }
        permissions.put(name, pp);
    }

    /**
   * Returns true if this collection implies the given permission. This even
   * returns true for this case:
   *
   * <pre>
   * collection.add(new PropertyPermission("a.*", "read"));
   * collection.add(new PropertyPermission("a.b.*", "write"));
   * collection.implies(new PropertyPermission("a.b.c", "read,write"));
   * </pre>
   *
   * @param permission the permission to check
   * @return true if it is implied by this
   */
    public boolean implies(Permission permission) {
        if (!(permission instanceof PropertyPermission)) return false;
        PropertyPermission toImply = (PropertyPermission) permission;
        int actions = toImply.actions;
        if (all_allowed) {
            int all_actions = ((PropertyPermission) permissions.get("*")).actions;
            actions &= ~all_actions;
            if (actions == 0) return true;
        }
        String name = toImply.getName();
        if (name.equals("*")) return false;
        int prefixLength = name.length();
        if (name.endsWith("*")) prefixLength -= 2;
        while (true) {
            PropertyPermission forName = (PropertyPermission) permissions.get(name);
            if (forName != null) {
                actions &= ~forName.actions;
                if (actions == 0) return true;
            }
            prefixLength = name.lastIndexOf('.', prefixLength - 1);
            if (prefixLength < 0) return false;
            name = name.substring(0, prefixLength + 1) + '*';
        }
    }

    /**
   * Enumerate over the collection.
   *
   * @return an enumeration of the collection contents
   */
    public Enumeration elements() {
        return permissions.elements();
    }
}
