package org.jwos.plugin.file.jcr.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.commons.repository.RepositoryFactory;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.jwos.plugin.file.jcr.config.BootstrapConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

public class JackrabbitRepositoryFactory implements RepositoryFactory {

    private static final Logger log = LoggerFactory.getLogger(BootstrapConfig.class);

    /**
     * the place for the repository config template
     */
    private final String configTemplate = "/org/apache/jackrabbit/core/repository.xml";

    /**
     * the place for the bootstrap properties template
     */
    private String bootstrapTemplate = "/org/jwos/plugin/file/jcr/config/template/bootstrap.properties";

    /**
     * the repository home directory
     */
    private String repHome = null;

    /**
     * the registered repository
     */
    private Repository repository = null;

    /**
     * the jndi context; created based on configuration
     */
    private InitialContext jndiContext;

    /**
     * the bootstrap config
     */
    private BootstrapConfig bootstrapConfig;

    public JackrabbitRepositoryFactory() {
    }

    public JackrabbitRepositoryFactory(String repHome) {
        this.repHome = repHome;
    }

    public synchronized Repository getRepository() throws RepositoryException {
        if (repository == null) {
            try {
                repository = getRepositoryByJNDI();
                if (repository == null) {
                    initRepository();
                    registerJNDI();
                }
            } catch (Exception e) {
                throw new RepositoryException(e);
            }
        }
        return repository;
    }

    public Repository createRepository() throws IOException, RepositoryException {
        Repository repository = null;
        String repXml = repHome + "/repository.xml";
        File homeDir = new File(repHome);
        File configFile = new File(repXml);
        File bootstrapConfigFile = new File(repHome + "/bootstrap.properties");
        homeDir.mkdirs();
        installRepositoryConfig(configFile);
        installBootstrap(bootstrapConfigFile, repHome, repXml);
        InputSource is = new InputSource(new FileInputStream(configFile));
        repository = createRepository(is, homeDir);
        return repository;
    }

    public void registerJNDI() throws RepositoryException {
        try {
            getJNDIContext().bind(getBootstrapConfig().getJndiName(), getRepository());
        } catch (NamingException e) {
            throw new RepositoryException(e);
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

    public void unregisterJNDI() {
        try {
            getJNDIContext().unbind(getBootstrapConfig().getJndiName());
        } catch (Exception e) {
            log.info("Error while unbinding repository from JNDI: " + e);
        }
    }

    public void close() {
        if (repository == null) {
            repository = getRepositoryByJNDI();
        }
        if (repository != null) {
            if (repository instanceof JackrabbitRepository) {
                ((JackrabbitRepository) repository).shutdown();
            }
            unregisterJNDI();
            repository = null;
        }
    }

    private void initBootstrapConfig() throws IOException {
        InputStream in = new FileInputStream(repHome + "/bootstrap.properties");
        Properties bootstrapProps = new Properties();
        bootstrapProps.load(in);
        bootstrapConfig = new BootstrapConfig();
        bootstrapConfig.init(bootstrapProps);
    }

    private void initRepository() throws IOException, RepositoryException {
        File repHome = new File(bootstrapConfig.getRepositoryHome()).getCanonicalFile();
        String repConfig = bootstrapConfig.getRepositoryConfig();
        InputStream in = new FileInputStream(new File(repConfig));
        repository = createRepository(new InputSource(in), repHome);
    }

    private InitialContext getJNDIContext() throws NamingException, IOException {
        if (jndiContext == null) {
            jndiContext = new InitialContext(getBootstrapConfig().getJndiEnv());
        }
        return jndiContext;
    }

    private BootstrapConfig getBootstrapConfig() throws IOException {
        if (bootstrapConfig == null) {
            initBootstrapConfig();
        }
        return bootstrapConfig;
    }

    private Repository getRepositoryByJNDI() {
        Repository rep = null;
        try {
            rep = (Repository) getJNDIContext().lookup(getBootstrapConfig().getRepositoryName());
        } catch (Exception e) {
        }
        return rep;
    }

    private void installRepositoryConfig(File dest) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = getClass().getResourceAsStream(configTemplate);
            out = new FileOutputStream(dest);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
            }
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
            }
        }
    }

    private void installBootstrap(File dest, String repHome, String repXml) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = getClass().getResourceAsStream(bootstrapTemplate);
            Properties props = new Properties();
            props.load(in);
            props.setProperty("repository.home", repHome);
            props.setProperty("repository.config", repXml);
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }
            out = new FileOutputStream(dest);
            props.store(out, "bootstrap properties for the repository startup.");
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
            }
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
            }
        }
    }

    private Repository createRepository(InputSource is, File homedir) throws RepositoryException {
        RepositoryConfig config = RepositoryConfig.create(is, homedir.getAbsolutePath());
        return RepositoryImpl.create(config);
    }

    public String getRepHome() {
        return repHome;
    }

    public void setRepHome(String repHome) {
        this.repHome = repHome;
    }

    public String getBootstrapTemplate() {
        return bootstrapTemplate;
    }

    public void setBootstrapTemplate(String bootstrapTemplate) {
        this.bootstrapTemplate = bootstrapTemplate;
    }

    public static void main(String[] args) {
        try {
            JackrabbitRepositoryFactory factory = new JackrabbitRepositoryFactory("C:/jackrabbit");
            JackrabbitRepository repository = (JackrabbitRepository) factory.createRepository();
            repository.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
