package commons;

import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.text.ParseException;
import org.makagiga.commons.Property;
import org.makagiga.commons.PropertyAccess;
import org.makagiga.commons.SecureProperty;
import org.makagiga.test.Test;
import org.makagiga.test.Tester;

public final class TestSecureProperty {

    @Test
    public void test_constructor() throws ParseException {
        initSecurityManager_noReadWrite();
        Property p;
        PropertyAccess sp;
        try {
            new SecureProperty<Object>(null);
            assert false : "NullPointerException expected";
        } catch (NullPointerException exception) {
        }
        sp = new Property<String>().getSecure();
        Tester.testSerializable(sp);
        sp = new Property<String>("foo").getSecure();
        Tester.testSerializable(sp);
        shutdownSecurityManager();
    }

    @Test
    public void test_check() {
        SecureProperty<String> sp = (SecureProperty<String>) new Property<String>("foo").getSecure();
        shutdownSecurityManager();
        sp.checkRead();
        sp.checkWrite();
        initSecurityManager_noReadWrite();
        try {
            sp.checkRead();
            assert false : "SecurityException expected";
        } catch (SecurityException exception) {
        }
        try {
            sp.checkWrite();
            assert false : "SecurityException expected";
        } catch (SecurityException exception) {
        }
        initSecurityManager_readWrite();
        sp.checkRead();
        sp.checkWrite();
        shutdownSecurityManager();
    }

    @Test
    public void test_clone() {
        shutdownSecurityManager();
        TestProperty.testCloneable(new Property<String>().getSecure());
        TestProperty.testCloneable(new Property<String>("foo").getSecure());
    }

    @Test
    public void test_compareTo() {
        initSecurityManager_noReadWrite();
        assert new Property<String>("foo").getSecure().compareTo(new Property<String>("foo")) == 0;
        assert new Property<String>("A").getSecure().compareTo(new Property<String>("B")) < 0;
        assert new Property<String>("B").getSecure().compareTo(new Property<String>("A")) > 0;
        shutdownSecurityManager();
    }

    @Test
    public void test_equals() {
        initSecurityManager_noReadWrite();
        Property<String> p = new Property<String>("foo");
        PropertyAccess<String> sp = p.getSecure();
        assert sp.equals(sp);
        assert sp.equals(p);
        assert !sp.equals(null);
        assert !sp.equals("foo");
        shutdownSecurityManager();
    }

    @Test
    public void test_getProperty() {
        Property<String> p = new Property<String>("foo");
        SecureProperty<String> sp = (SecureProperty<String>) p.getSecure();
        initSecurityManager_noRead();
        try {
            sp.getProperty();
            assert false : "SecurityException expected";
        } catch (SecurityException exception) {
        }
        shutdownSecurityManager();
        assert sp.getProperty() != null;
        assert sp.getProperty() == p;
    }

    @Test
    public void test_hashCode() {
        Property<String> p = new Property<String>("foo");
        SecureProperty<String> sp = (SecureProperty<String>) p.getSecure();
        assert p.hashCode() != 0;
        assert p.hashCode() == p.get().hashCode();
        assert p.hashCode() == sp.hashCode();
    }

    @Test
    public void test_parse() throws ParseException {
    }

    private void initSecurityManager_noRead() {
        Policy.setPolicy(new Policy() {

            @Override
            public boolean implies(final ProtectionDomain pd, final Permission p) {
                if (p instanceof SecureProperty.Permission) {
                    if (p.getName().equals("read")) return false;
                }
                return true;
            }
        });
        SecurityManager sm = new SecurityManager();
        System.setSecurityManager(sm);
        assert System.getSecurityManager() == sm;
    }

    private void initSecurityManager_noReadWrite() {
        Policy.setPolicy(new Policy() {

            @Override
            public boolean implies(final ProtectionDomain pd, final Permission p) {
                if (p instanceof SecureProperty.Permission) {
                    if (p.getName().equals("read") || p.getName().equals("write")) return false;
                    assert false : "\"read\" or \"write\" permission expected";
                }
                return true;
            }
        });
        SecurityManager sm = new SecurityManager();
        System.setSecurityManager(sm);
        assert System.getSecurityManager() == sm;
    }

    private void initSecurityManager_noWrite() {
        Policy.setPolicy(new Policy() {

            @Override
            public boolean implies(final ProtectionDomain pd, final Permission p) {
                if (p instanceof SecureProperty.Permission) {
                    if (p.getName().equals("write")) return false;
                }
                return true;
            }
        });
        SecurityManager sm = new SecurityManager();
        System.setSecurityManager(sm);
        assert System.getSecurityManager() == sm;
    }

    private void initSecurityManager_readWrite() {
        Policy.setPolicy(new Policy() {

            @Override
            public boolean implies(final ProtectionDomain pd, final Permission p) {
                if (p instanceof SecureProperty.Permission) return true;
                return true;
            }
        });
        SecurityManager sm = new SecurityManager();
        System.setSecurityManager(sm);
        assert System.getSecurityManager() == sm;
    }

    private void shutdownSecurityManager() {
        System.setSecurityManager(null);
        assert System.getSecurityManager() == null;
    }
}
