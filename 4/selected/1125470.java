package tests.support;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.BasicPermission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.ProtectionDomain;

public class Support_PermissionCollection {

    public static void main(String[] args) throws Exception {
        System.setSecurityManager(new SecurityManager());
        java.net.URL[] urls = new java.net.URL[1];
        Class<?> c = null;
        java.net.URLClassLoader ucl = null;
        try {
            URL url = new URL(args[0]);
            urls[0] = url;
            ucl = URLClassLoader.newInstance(urls, null);
            c = Class.forName("coucou.FileAccess", true, ucl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ProtectionDomain pd = c.getProtectionDomain();
        PermissionCollection coll = Policy.getPolicy().getPermissions(pd);
        Class<?> myPermission = Class.forName("mypackage.MyPermission");
        BasicPermission myperm = (BasicPermission) myPermission.newInstance();
        System.out.println(coll.implies(new java.io.FilePermission("test1.txt", "write")) + "," + coll.implies(myperm) + "," + coll.implies(new java.io.FilePermission("test2.txt", "write")) + "," + coll.implies(new java.io.FilePermission("test3.txt", "read")) + ",");
    }
}
