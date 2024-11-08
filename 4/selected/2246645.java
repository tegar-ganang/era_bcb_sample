package naming;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.OperationNotSupportedException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import tyrex.tm.RuntimeContext;
import util.VerboseStream;

/**
 * Naming test suite
 */
public class Naming extends TestSuite {

    public Naming(String name) {
        super(name);
        TestCase tc;
        tc = new MemoryContextTest();
        addTest(tc);
        tc = new EnvContextTest();
        addTest(tc);
        tc = new ReferenceableTest();
        addTest(tc);
    }

    public static InitialContext getInitialContext(String url) throws NamingException {
        Hashtable env;
        env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, tyrex.naming.MemoryContextFactory.class.getName());
        env.put(Context.URL_PKG_PREFIXES, "tyrex.naming");
        if (url != null) env.put(Context.PROVIDER_URL, url);
        return new InitialContext(env);
    }

    /**
     * Tests the in-memory service provider (MemoryContext).
     */
    public static class MemoryContextTest extends TestCase {

        public MemoryContextTest() {
            super("[TC01] In-Memory Service Provider");
        }

        /**
         * Main test method - TC01
         *  - in-memory service provider test
         *
         */
        public void runTest() {
            String value = "Just A Test";
            String name = "test";
            String sub = "sub";
            InitialContext initCtx;
            Context ctx1;
            Context ctx2;
            VerboseStream stream = new VerboseStream();
            try {
                stream.writeVerbose("Constructing same context in two different ways and comparing bound values");
                initCtx = Naming.getInitialContext("root/" + name);
                ctx1 = initCtx;
                initCtx = Naming.getInitialContext("root");
                ctx2 = (Context) initCtx.lookup(name);
                ctx1.bind(name, value);
                if (ctx2.lookup(name) != value) {
                    fail("Error: Same testValue not bound in both contexts (1)");
                }
                ctx2 = ctx2.createSubcontext(sub);
                ctx1 = (Context) ctx1.lookup(sub);
                ctx1.bind(sub + name, value);
                if (ctx2.lookup(sub + name) != value) {
                    fail("Error: Same testValue not bound in both contexts (2)");
                }
                stream.writeVerbose("Testing that shared and non-shared namespaces are different");
                ctx2 = Naming.getInitialContext(null);
                try {
                    ctx2 = (Context) ctx2.lookup(name);
                    if (ctx2.lookup(name) == value) {
                        fail("Error: Same testValue bound to not-shared contexts (1)");
                    }
                    fail("Error: NameNotFoundException not reported");
                } catch (NameNotFoundException except) {
                    ctx2.bind(name, value);
                }
                stream.writeVerbose("Testing that two non-shared namespaces are different");
                ctx2 = Naming.getInitialContext(null);
                try {
                    ctx2 = (Context) ctx2.lookup(name);
                    if (ctx2.lookup(name) == value) {
                        fail("Error: Same testValue bound to not-shared contexts (2)");
                    }
                    fail("Error: NameNotFoundException not reported");
                } catch (NameNotFoundException except) {
                    ctx2.bind(name, value);
                }
            } catch (NamingException except) {
                System.out.println(except);
                except.printStackTrace();
            }
        }
    }

    /**
     * Tests the environment naming context (EnvContext).
     */
    public static class EnvContextTest extends TestCase {

        public EnvContextTest() {
            super("[TC02] Environment Naming Context");
        }

        /**
         * Main test method - TC02
         *  - environment naming context test
         *
         */
        public void runTest() {
            String value = "testValue";
            String path = "comp/env";
            String name = "test";
            RuntimeContext runCtx;
            Context rootCtx;
            Context ctx;
            Context enc = null;
            InitialContext initCtx;
            VerboseStream stream = new VerboseStream();
            try {
                runCtx = RuntimeContext.newRuntimeContext();
                stream.writeVerbose("Constructing a context with comp/env/test as a test value");
                rootCtx = runCtx.getEnvContext();
                rootCtx.createSubcontext("comp");
                rootCtx.createSubcontext("comp/env");
                rootCtx.bind(path + "/" + name, value);
                ctx = (Context) rootCtx.lookup("");
                stream.writeVerbose("Test ability to read from the ENC in variety of ways");
                try {
                    if (rootCtx.lookup(path + "/" + name) != value) {
                        fail("Error: Failed to lookup name (1)");
                    }
                } catch (NameNotFoundException except) {
                    fail("Error: Failed to lookup name (2)");
                }
                try {
                    enc = (Context) rootCtx.lookup("comp");
                    enc = (Context) enc.lookup("env");
                    if (enc.lookup(name) != value) {
                        fail("Error: Failed to lookup name (3)");
                    }
                } catch (NameNotFoundException except) {
                    fail("Error: Failed to lookup name (4)");
                }
                stream.writeVerbose("Test updates on memory context reflecting in ENC");
                ctx.unbind(path + "/" + name);
                try {
                    enc.lookup(name);
                    fail("Error: NameNotFoundException not reported");
                } catch (NameNotFoundException except) {
                }
                stream.writeVerbose("Test the stack nature of the JNDI ENC");
                initCtx = new InitialContext();
                ctx.bind(path + "/" + name, value);
                RuntimeContext.setRuntimeContext(runCtx);
                RuntimeContext.setRuntimeContext(RuntimeContext.newRuntimeContext());
                try {
                    initCtx.lookup("java:" + path + "/" + name);
                    fail("Error: NotContextException not reported");
                } catch (NotContextException except) {
                }
                RuntimeContext.unsetRuntimeContext();
                try {
                    if (initCtx.lookup("java:" + path + "/" + name) != value) {
                        fail("Error: Failed to lookup name (5)");
                    }
                } catch (NamingException except) {
                    fail("Error: NamingException reported");
                }
                RuntimeContext.unsetRuntimeContext();
                try {
                    initCtx.lookup("java:" + path + "/" + name);
                    fail("Error: NamingException not reported");
                } catch (NamingException except) {
                }
                stream.writeVerbose("Test the serialization nature of JNDI ENC");
                RuntimeContext.setRuntimeContext(runCtx);
                stream.writeVerbose("Test that the JNDI ENC is read only");
                ctx.unbind(path + "/" + name);
                try {
                    initCtx.bind("java:" + name, value);
                    fail("Error: JNDI ENC not read-only (1)");
                } catch (OperationNotSupportedException except) {
                }
                ctx.bind(path + "/" + name, value);
                try {
                    initCtx.unbind("java:" + name);
                    fail("Error: JNDI ENC not read-only (2)");
                } catch (OperationNotSupportedException except) {
                }
                try {
                    ObjectOutputStream oos;
                    ObjectInputStream ois;
                    ByteArrayOutputStream aos;
                    enc = (Context) initCtx.lookup("java:" + path);
                    aos = new ByteArrayOutputStream();
                    oos = new ObjectOutputStream(aos);
                    oos.writeObject(enc);
                    ois = new ObjectInputStream(new ByteArrayInputStream(aos.toByteArray()));
                    enc = (Context) ois.readObject();
                } catch (Exception except) {
                    fail("Error: Failed to (de)serialize: " + except);
                }
                RuntimeContext.unsetRuntimeContext();
                try {
                    enc.lookup(name);
                    stream.writeVerbose("Error: Managed to lookup name but java:comp not bound to thread");
                } catch (NamingException except) {
                }
                {
                }
                RuntimeContext.setRuntimeContext(runCtx);
                try {
                    if (enc.lookup(name) != value) {
                        fail("Error: Failed to lookup name (6)");
                    }
                } catch (NameNotFoundException except) {
                    fail("Error: NameNotFoundException reported");
                }
            } catch (NamingException except) {
                System.out.println(except);
                except.printStackTrace();
            }
        }
    }

    /**
     * Test the handling of referencable objects.
     */
    public static class ReferenceableTest extends TestCase {

        public ReferenceableTest() {
            super("[TC03] Referenceable Object Handling");
        }

        /**
         * Main test method - TC03
         *  - handling of referencable objects test
         *
         */
        public void runTest() {
            Context ctx;
            String name = "test";
            String value = "Just A Test";
            Object object;
            RuntimeContext runCtx;
            VerboseStream stream = new VerboseStream();
            try {
                object = new TestObject(value);
                stream.writeVerbose("Test binding of Referenceable object in MemoryContext");
                runCtx = RuntimeContext.newRuntimeContext();
                ctx = runCtx.getEnvContext();
                ctx.bind(name, object);
                if (ctx.lookup(name) == object) {
                    fail("Error: Same object instance returned in both cases");
                }
                if (!ctx.lookup(name).equals(object)) {
                    fail("Error: The two objects are not identical");
                }
                stream.writeVerbose("Bound one object, reconstructed another, both pass equality test");
                stream.writeVerbose("Test looking up Referenceable object from EnvContext");
                RuntimeContext.setRuntimeContext(runCtx);
                ctx = new InitialContext();
                if (ctx.lookup("java:" + name) == object) {
                    fail("Error: Same object instance returned in both cases");
                }
                if (!ctx.lookup("java:" + name).equals(object)) {
                    fail("Error: The two objects are not identical");
                }
                RuntimeContext.unsetRuntimeContext();
                stream.writeVerbose("Bound one object, reconstructed another, both pass equality test");
            } catch (NamingException except) {
                System.out.println(except);
                except.printStackTrace();
            }
        }

        public static class TestObject implements Referenceable {

            private String _value;

            TestObject(String value) {
                _value = value;
            }

            public boolean equals(Object other) {
                if (this == other) return true;
                if (other instanceof TestObject && ((TestObject) other)._value.equals(_value)) return true;
                return false;
            }

            public Reference getReference() throws NamingException {
                Reference ref;
                ref = new Reference(getClass().getName(), TestObjectFactory.class.getName(), null);
                ref.add(new StringRefAddr("Value", _value));
                return ref;
            }
        }

        public static class TestObjectFactory implements ObjectFactory {

            public Object getObjectInstance(Object refObj, Name name, Context nameCtx, Hashtable env) throws NamingException {
                Reference ref;
                if (refObj instanceof Reference) {
                    ref = (Reference) refObj;
                    if (ref.getClassName().equals(TestObject.class.getName())) {
                        TestObject object;
                        object = new TestObject((String) ref.get("Value").getContent());
                        return object;
                    } else throw new NamingException("Not a reference");
                }
                return null;
            }
        }
    }
}
