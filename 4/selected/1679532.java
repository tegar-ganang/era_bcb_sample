package com.quickwcm.jackrabbit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Properties;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.SimpleCredentials;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.apache.jackrabbit.core.jndi.RegistryHelper;

public class JCRHelper {

    /**
   * Property for the repository name (used for jndi lookup)
   */
    public static final String PROP_REPOSITORY_NAME = "org.apache.jackrabbit.repository.name";

    /**
   * Property for the repository home directory (used for repository
   * instantiation)
   */
    public static final String PROP_REPOSITORY_HOME = "org.apache.jackrabbit.repository.home";

    /**
   * Property for the jaas config path. If the system property
   * <code>java.security.auth.login.config</code> is not set this repository
   * stub will try to read this property from the environment and use the value
   * retrieved as the value for the system property.
   */
    public static final String PROP_JAAS_CONFIG = "org.apache.jackrabbit.repository.jaas.config";

    /**
   * The repository instance
   */
    private static Repository repository = null;

    public static Repository getRepository() {
        return repository;
    }

    @SuppressWarnings("unchecked")
    public static void initialize() {
        if (repository == null) {
            try {
                Properties jcrProperties = new Properties();
                InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("jcr.properties");
                if (input == null) {
                    throw new RuntimeException("Couldn't load jcr.properties");
                }
                jcrProperties.load(input);
                String repName = jcrProperties.getProperty(PROP_REPOSITORY_NAME, "repo");
                String repHome = System.getProperty("user.home") + "/" + jcrProperties.getProperty(PROP_REPOSITORY_HOME, ".quickwcm");
                String resConfig = repHome + "/repository.xml";
                if (!new File(repHome).exists()) {
                    new File(repHome).mkdir();
                }
                if (!new File(resConfig).exists()) {
                    InputStream repositoryIS = Thread.currentThread().getContextClassLoader().getResourceAsStream("org/apache/jackrabbit/core/repository.xml");
                    OutputStream os = new FileOutputStream(resConfig);
                    int read;
                    byte[] buff = new byte[102400];
                    while ((read = repositoryIS.read(buff)) > 0) {
                        os.write(buff, 0, read);
                    }
                    repositoryIS.close();
                    os.close();
                }
                Hashtable env = new Hashtable();
                env.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.jackrabbit.core.jndi.provider.DummyInitialContextFactory");
                env.put(Context.PROVIDER_URL, "localhost");
                InitialContext ctx = new InitialContext(env);
                RegistryHelper.registerRepository(ctx, repName, resConfig, repHome, true);
                repository = (Repository) ctx.lookup(repName);
            } catch (Exception e) {
                e.printStackTrace(System.out);
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    public static Credentials getSystemCredentials() {
        return new SimpleCredentials("root", "secret".toCharArray());
    }
}
