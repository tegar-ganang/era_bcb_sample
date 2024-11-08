package javax.security.auth;

import java.io.FilePermission;
import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.Enumeration;
import junit.framework.TestCase;
import org.apache.harmony.auth.tests.support.SecurityChecker;
import org.apache.harmony.auth.tests.support.TestUtils;
import tests.support.resource.Support_Resources;

/**
 * Tests Policy class
 */
@SuppressWarnings("deprecation")
public class PolicyTest extends TestCase {

    /**
     * Tests that setPolicy() is properly secured via SecurityManager.
     */
    public void testSetPolicy() {
        SecurityManager old = System.getSecurityManager();
        Policy oldPolicy = null;
        oldPolicy = Policy.getPolicy();
        try {
            SecurityChecker checker = new SecurityChecker(new AuthPermission("setPolicy"), true);
            System.setSecurityManager(checker);
            Policy custom = new TestProvider();
            Policy.setPolicy(custom);
            assertTrue(checker.checkAsserted);
            assertSame(custom, Policy.getPolicy());
            checker.reset();
            checker.enableAccess = false;
            try {
                Policy.setPolicy(new TestProvider());
                fail("SecurityException is intercepted");
            } catch (SecurityException ok) {
            }
        } finally {
            System.setSecurityManager(old);
            Policy.setPolicy(oldPolicy);
        }
    }

    /**
     * Tests that getPolicy() is properly secured via SecurityManager.
     */
    public void testGetPolicy_CheckPermission() {
        SecurityManager old = System.getSecurityManager();
        Policy oldPolicy = null;
        oldPolicy = Policy.getPolicy();
        try {
            Policy.setPolicy(new TestProvider());
            SecurityChecker checker = new SecurityChecker(new AuthPermission("getPolicy"), true);
            System.setSecurityManager(checker);
            Policy.getPolicy();
            assertTrue(checker.checkAsserted);
            checker.reset();
            checker.enableAccess = false;
            try {
                Policy.getPolicy();
                fail("SecurityException is intercepted");
            } catch (SecurityException ok) {
            }
        } finally {
            System.setSecurityManager(old);
            Policy.setPolicy(oldPolicy);
        }
    }

    public static class TestProvider extends Policy {

        @Override
        public PermissionCollection getPermissions(Subject subject, CodeSource cs) {
            return null;
        }

        @Override
        public void refresh() {
        }
    }

    public static class FakePolicy {
    }

    /**
     * Tests loading of a default provider, both valid and invalid class
     * references.
     */
    public void testGetPolicy_LoadDefaultProvider() {
        Policy oldPolicy = null;
        try {
            oldPolicy = Policy.getPolicy();
        } catch (Throwable ignore) {
        }
        String POLICY_PROVIDER = "auth.policy.provider";
        String oldProvider = Security.getProperty(POLICY_PROVIDER);
        try {
            Security.setProperty(POLICY_PROVIDER, TestProvider.class.getName());
            Policy.setPolicy(null);
            Policy p = Policy.getPolicy();
            assertNotNull(p);
            assertEquals(TestProvider.class.getName(), p.getClass().getName());
            Security.setProperty(POLICY_PROVIDER, "a.b.c.D");
            Policy.setPolicy(null);
            try {
                p = Policy.getPolicy();
                fail("No SecurityException on failed provider");
            } catch (SecurityException ok) {
            }
            Security.setProperty(POLICY_PROVIDER, FakePolicy.class.getName());
            Policy.setPolicy(null);
            try {
                p = Policy.getPolicy();
                fail("No expected SecurityException");
            } catch (SecurityException ok) {
            }
        } finally {
            TestUtils.setSystemProperty(POLICY_PROVIDER, oldProvider);
            Policy.setPolicy(oldPolicy);
        }
    }

    static String inputFile1 = Support_Resources.getAbsoluteResourcePath("auth_policy1.txt");

    static String inputFile2 = Support_Resources.getAbsoluteResourcePath("auth_policy2.txt");

    private static final String POLICY_PROP = "java.security.auth.policy";

    public void test_GetPermissions() throws Exception {
        PermissionCollection c;
        Permission per;
        Subject subject;
        CodeSource source;
        String oldProp = System.getProperty(POLICY_PROP);
        try {
            System.setProperty(POLICY_PROP, inputFile1);
            Policy p = Policy.getPolicy();
            p.refresh();
            c = p.getPermissions(null, null);
            assertFalse("Read only for empty", c.isReadOnly());
            assertFalse("Elements for empty", c.elements().hasMoreElements());
            subject = new Subject();
            c = p.getPermissions(subject, null);
            assertFalse("Elements: ", c.elements().hasMoreElements());
            subject.getPrincipals().add(new MyPrincipal("kuke"));
            c = p.getPermissions(subject, null);
            assertFalse("Elements: ", c.elements().hasMoreElements());
            subject.getPrincipals().add(new OtherPrincipal("duke"));
            c = p.getPermissions(subject, null);
            assertFalse("Elements: ", c.elements().hasMoreElements());
            subject.getPrincipals().add(new FakePrincipal("duke"));
            c = p.getPermissions(subject, null);
            assertFalse("Elements: ", c.elements().hasMoreElements());
            subject.getPrincipals().add(new MyPrincipal("duke"));
            Enumeration<Permission> e = p.getPermissions(subject, null).elements();
            per = e.nextElement();
            assertFalse("Elements: ", e.hasMoreElements());
            assertEquals("Permission: ", per, new FilePermission("/home/duke", "read, write"));
            source = new CodeSource(new URL("http://dummy.xxx"), (Certificate[]) null);
            c = p.getPermissions(subject, source);
            assertTrue("Elements: ", c.elements().hasMoreElements());
            source = new CodeSource(new URL("http://dummy.xxx"), (CodeSigner[]) null);
            c = p.getPermissions(subject, source);
            assertTrue("Elements: ", c.elements().hasMoreElements());
            source = new CodeSource(new URL("http://dummy.xxx"), (Certificate[]) null);
            subject = new Subject();
            subject.getPrincipals().add(new MyPrincipal("dummy"));
            e = p.getPermissions(subject, source).elements();
            per = e.nextElement();
            assertFalse("Elements: ", e.hasMoreElements());
            assertEquals("Permission: ", per, new RuntimePermission("createClassLoader"));
            subject = new Subject();
            c = p.getPermissions(subject, source);
            assertFalse("Elements: ", c.elements().hasMoreElements());
            subject.getPrincipals().add(new MyPrincipal("kuke"));
            c = p.getPermissions(subject, null);
            assertFalse("Elements: ", c.elements().hasMoreElements());
            subject.getPrincipals().add(new OtherPrincipal("dummy"));
            c = p.getPermissions(subject, null);
            assertFalse("Elements: ", c.elements().hasMoreElements());
            subject = new Subject();
            subject.getPrincipals().add(new MyPrincipal("my"));
            c = p.getPermissions(subject, null);
            assertFalse("Elements: ", c.elements().hasMoreElements());
            subject.getPrincipals().add(new OtherPrincipal("other"));
            e = p.getPermissions(subject, null).elements();
            per = e.nextElement();
            assertFalse("Elements: ", e.hasMoreElements());
            assertEquals("Permission: ", per, new AllPermission());
            subject = new Subject();
            subject.getPrincipals().add(new MyPrincipal("bunny"));
            e = p.getPermissions(subject, null).elements();
            Permission[] get = new Permission[2];
            get[0] = e.nextElement();
            get[1] = e.nextElement();
            assertFalse("Elements: ", e.hasMoreElements());
            Permission[] set = new Permission[2];
            set[0] = new FilePermission("/home/bunny", "read, write");
            set[1] = new RuntimePermission("stopThread");
            if (get[0].equals(set[0])) {
                assertEquals("Permission: ", set[1], get[1]);
            } else {
                assertEquals("Permission: ", set[0], get[1]);
                assertEquals("Permission: ", set[1], get[0]);
            }
        } finally {
            TestUtils.setSystemProperty(POLICY_PROP, oldProp);
        }
    }

    public void test_Refresh() {
        Permission per;
        Subject subject;
        Enumeration<?> e;
        String oldProp = System.getProperty(POLICY_PROP);
        try {
            System.setProperty(POLICY_PROP, inputFile1);
            Policy p = Policy.getPolicy();
            p.refresh();
            subject = new Subject();
            subject.getPrincipals().add(new MyPrincipal("duke"));
            e = p.getPermissions(subject, null).elements();
            per = (Permission) e.nextElement();
            assertFalse("Elements: ", e.hasMoreElements());
            assertEquals("Permission: ", per, new FilePermission("/home/duke", "read, write"));
            System.setProperty(POLICY_PROP, inputFile2);
            p.refresh();
            e = p.getPermissions(subject, null).elements();
            per = (Permission) e.nextElement();
            assertFalse("Elements: ", e.hasMoreElements());
            assertEquals("Permission: ", per, new RuntimePermission("createClassLoader"));
        } finally {
            TestUtils.setSystemProperty(POLICY_PROP, oldProp);
        }
    }
}
