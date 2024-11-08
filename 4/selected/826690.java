package org.vardb.admin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.vardb.CVardbException;
import org.vardb.blast.IBlastService;
import org.vardb.converters.CPfamLoader;
import org.vardb.counts.ICountService;
import org.vardb.resources.IResourceService;
import org.vardb.users.IUserService;
import org.vardb.util.CDatabaseHelper;
import org.vardb.util.CFileHelper;
import org.vardb.util.CJarHelper;
import org.vardb.util.CMessageWriter;
import org.vardb.util.CSpringHelper;
import org.vardb.util.CStringHelper;

public class CSetup {

    public static final String DEFAULT_SETUP_PROPERTIES_PATH = "classpath:defaultsetup.properties";

    public static final String ACTION_DELIMITER = ",";

    public static void main(String[] argv) {
        Args args = new Args(argv);
        if (args.actions.size() == 0 || args.actions.get(0) == Action.help) {
            System.out.println(Args.getHelp());
            return;
        }
        Params params = args.loadParams();
        CSetup setup = new CSetup(params);
        setup.execute(args.actions);
    }

    private Params params;

    private CMessageWriter writer = new CMessageWriter();

    private CLoadTableParams loadparams = new CLoadTableParams();

    private GenericApplicationContext applicationContext = null;

    public CSetup(Params params) {
        this.params = params;
    }

    public void execute(List<Action> actions) {
        for (Action action : actions) {
            execute(action);
        }
    }

    public boolean execute(Action action) {
        System.out.println("starting action: " + action);
        switch(action) {
            case all:
                return all();
            case test:
                return test();
            case initdb:
                return initdb();
            case loaddata:
                return loaddata();
            case annotate:
                return annotate();
            case dropdb:
                return dropdb();
            case createdb:
                return createdb();
            case createtables:
                return createtables();
            case xml:
                return xml();
            case drugs:
                return drugs();
            case loadusers:
                return loadusers();
            case importusers:
                return importusers();
            case sequences:
                return sequences();
            case domains:
                return domains();
            case othersequences:
                return othersequences();
            case alignments:
                return alignments();
            case bundles:
                return bundles();
            case pfam:
                return pfam();
            case taxonomy:
                return taxonomy();
            case refs:
                return refs();
            case counts:
                return counts();
            case pdb:
                return pdb();
            case blast:
                return blast();
            case jars:
                return jars();
            case ds:
                return ds();
            case vacuum:
                return vacuum();
            case help:
                return doNothing();
            default:
                writer.message("no handler for action: " + action);
                return false;
        }
    }

    public boolean all() {
        initdb();
        loaddata();
        annotate();
        importusers();
        jars();
        ds();
        return true;
    }

    public boolean test() {
        return true;
    }

    public boolean reload() {
        return true;
    }

    public boolean initdb() {
        createdb();
        createtables();
        loadusers();
        return true;
    }

    public boolean loaddata() {
        xml();
        sequences();
        othersequences();
        domains();
        bundles();
        blast();
        pdb();
        return true;
    }

    public boolean annotate() {
        pfam();
        drugs();
        taxonomy();
        refs();
        counts();
        return true;
    }

    public boolean dropdb() {
        if (!CFileHelper.confirm("Delete database " + params.db.getName() + "? (press y to confirm drop or any other key to skip)")) {
            System.out.println("Skipping dropdb");
            return true;
        }
        CDatabaseHelper.dropDatabase(params.db);
        return true;
    }

    public boolean createdb() {
        if (CDatabaseHelper.databaseExists(params.db)) {
            if (!params.dropdbifexists) {
                writer.message("database already exists and dropdbifexists is false - quitting");
                return false;
            } else dropdb();
        }
        CDatabaseHelper.createDatabase(params.db);
        return true;
    }

    public boolean createtables() {
        String sql = CDatabaseHelper.concatenateScripts(params.sqlDir);
        CFileHelper.writeFile(params.tempDir + "setup.sql", sql);
        CDatabaseHelper.executeSql(params.db, sql);
        return true;
    }

    public boolean xml() {
        getAdminService().validateFolder(params.xmlDir, writer);
        getAdminService().loadXmlFromFolder(params.xmlDir, writer);
        return true;
    }

    public boolean drugs() {
        getResourceService().updateDrugs(writer);
        return true;
    }

    public boolean loadusers() {
        if (CStringHelper.hasContent(params.userfile) && CFileHelper.exists(params.userfile)) getAdminService().loadXmlFromFile(params.userfile, writer);
        return true;
    }

    public boolean importusers() {
        if (!CStringHelper.hasContent(params.importdb)) return true;
        if (!CFileHelper.confirm("Import users from database " + params.importdb + "? (press y to confirm import or any other key to skip)")) {
            System.out.println("Skipping import user step");
            return true;
        }
        DataSource datasource = CDatabaseHelper.createDataSource(params.db, params.importdb);
        CDatabaseImporter importer = new CDatabaseImporter(datasource, writer);
        getUserService().addUsers(importer.getUsers());
        return true;
    }

    public boolean sequences() {
        getBatchService().loadTablesFromFolder(params.sequenceDir, loadparams);
        return true;
    }

    public boolean domains() {
        getBatchService().loadTablesFromFolder(params.domainDir, loadparams);
        return true;
    }

    public boolean othersequences() {
        getBatchService().loadTablesFromFolder(params.othersequenceDir, loadparams);
        return true;
    }

    public boolean alignments() {
        getAdminService().loadAlignmentsFromFolder(params.alignmentDir, writer);
        return true;
    }

    public boolean bundles() {
        String dir = params.bundleDir;
        IBatchService batchService = getBatchService();
        batchService.loadXmlFromFolder(dir, writer);
        batchService.loadBundlesFromFolder(dir, writer);
        batchService.loadJoinsFromFolder(dir, writer);
        return true;
    }

    public boolean pfam() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(CDatabaseHelper.createDataSource(params.db, params.db.getName()));
        CPfamLoader loader = new CPfamLoader(jdbcTemplate);
        loader.loadFiles(params.tempDir);
        getAdminService().updatePfamDomains(writer);
        return true;
    }

    public boolean taxonomy() {
        getResourceService().updateTaxonomy(writer);
        return true;
    }

    public boolean refs() {
        getResourceService().updateReferences(writer);
        return true;
    }

    public boolean counts() {
        getCountService().updateCounts(writer);
        return true;
    }

    public boolean pdb() {
        getAdminService().loadStructuresFromFolder(params.pdbDir, writer);
        return true;
    }

    public boolean blast() {
        getBlastService().updateBlastDatabase(writer);
        return true;
    }

    public boolean jars() {
        CJarHelper.checkJarFiles(params.jboss.libDir, params.jarfile);
        return true;
    }

    public boolean ds() {
        CDatabaseHelper.writeDataSourceFile(params.db, params.jboss.deployDir);
        return true;
    }

    public boolean vacuum() {
        CDatabaseHelper.vacuum(params.db);
        return true;
    }

    public boolean doNothing() {
        return true;
    }

    private GenericApplicationContext getApplicationContext() {
        if (applicationContext == null) loadApplicationContext();
        return applicationContext;
    }

    private void loadApplicationContext() {
        if (!CDatabaseHelper.databaseExists(params.db)) throw new CVardbException("can't create application context because database not created yet: " + params.db.getName());
        applicationContext = new GenericApplicationContext();
        CSpringHelper.registerDataSource(applicationContext, "dataSource", params.db);
        CSpringHelper.loadXmlBeanDefinitions(applicationContext, "spring-services.xml");
        applicationContext.refresh();
    }

    private IAdminService getAdminService() {
        return (IAdminService) getApplicationContext().getBean("adminService");
    }

    private IBatchService getBatchService() {
        return (IBatchService) getApplicationContext().getBean("batchService");
    }

    private ICountService getCountService() {
        return (ICountService) getApplicationContext().getBean("countService");
    }

    private IResourceService getResourceService() {
        return (IResourceService) getApplicationContext().getBean("resourceService");
    }

    private IBlastService getBlastService() {
        return (IBlastService) getApplicationContext().getBean("blastService");
    }

    private IUserService getUserService() {
        return (IUserService) getApplicationContext().getBean("userService");
    }

    public static class Params {

        protected String vardbDir;

        protected String tempDir;

        protected String pfamRelease;

        protected String sqlDir;

        protected String xmlDir;

        protected String sequenceDir;

        protected String domainDir;

        protected String othersequenceDir;

        protected String alignmentDir;

        protected String pdbDir;

        protected String bundleDir;

        protected String userfile;

        protected String jarfile;

        protected Boolean dropdbifexists = false;

        protected String importdb;

        protected String rootid;

        protected String rootusername;

        protected String rootpassword;

        protected DbParams db = new DbParams();

        protected JbossParams jboss = new JbossParams();

        public static class DbParams extends CDatabaseHelper.Params {
        }

        public static class JbossParams {

            protected String libDir;

            protected String deployDir;

            public String getLibDir() {
                return this.libDir;
            }

            public void setLibDir(final String libDir) {
                this.libDir = libDir;
            }

            public String getDeployDir() {
                return this.deployDir;
            }

            public void setDeployDir(final String deployDir) {
                this.deployDir = deployDir;
            }

            public void validate() {
                libDir = CFileHelper.normalizeDirectory(CStringHelper.checkResolvedProperty("libDir", libDir));
                deployDir = CFileHelper.normalizeDirectory(CStringHelper.checkResolvedProperty("deployDir", deployDir));
            }

            @Override
            public String toString() {
                return CStringHelper.toString(this);
            }
        }

        public Params() {
        }

        public String getVardbDir() {
            return this.vardbDir;
        }

        public void setVardbDir(final String vardbDir) {
            this.vardbDir = vardbDir;
        }

        public DbParams getDb() {
            return this.db;
        }

        public void setDb(final DbParams db) {
            this.db = db;
        }

        public String getUserfile() {
            return this.userfile;
        }

        public void setUserfile(final String userfile) {
            this.userfile = userfile;
        }

        public String getTempDir() {
            return this.tempDir;
        }

        public void setTempDir(final String tempDir) {
            this.tempDir = tempDir;
        }

        public String getPfamRelease() {
            return this.pfamRelease;
        }

        public void setPfamRelease(final String pfamRelease) {
            this.pfamRelease = pfamRelease;
        }

        public String getSqlDir() {
            return this.sqlDir;
        }

        public void setSqlDir(final String sqlDir) {
            this.sqlDir = sqlDir;
        }

        public String getXmlDir() {
            return this.xmlDir;
        }

        public void setXmlDir(final String xmlDir) {
            this.xmlDir = xmlDir;
        }

        public String getSequenceDir() {
            return this.sequenceDir;
        }

        public void setSequenceDir(final String sequenceDir) {
            this.sequenceDir = sequenceDir;
        }

        public String getDomainDir() {
            return this.domainDir;
        }

        public void setDomainDir(final String domainDir) {
            this.domainDir = domainDir;
        }

        public String getOthersequenceDir() {
            return this.othersequenceDir;
        }

        public void setOthersequenceDir(final String othersequenceDir) {
            this.othersequenceDir = othersequenceDir;
        }

        public String getAlignmentDir() {
            return this.alignmentDir;
        }

        public void setAlignmentDir(final String alignmentDir) {
            this.alignmentDir = alignmentDir;
        }

        public String getPdbDir() {
            return this.pdbDir;
        }

        public void setPdbDir(final String pdbDir) {
            this.pdbDir = pdbDir;
        }

        public String getBundleDir() {
            return this.bundleDir;
        }

        public void setBundleDir(final String bundleDir) {
            this.bundleDir = bundleDir;
        }

        public Boolean getDropdbifexists() {
            return this.dropdbifexists;
        }

        public void setDropdbifexists(final Boolean dropdbifexists) {
            this.dropdbifexists = dropdbifexists;
        }

        public String getImportdb() {
            return this.importdb;
        }

        public void setImportdb(final String importdb) {
            this.importdb = importdb;
        }

        public String getRootid() {
            return this.rootid;
        }

        public void setRootid(final String rootid) {
            this.rootid = rootid;
        }

        public String getRootusername() {
            return this.rootusername;
        }

        public void setRootusername(final String rootusername) {
            this.rootusername = rootusername;
        }

        public String getRootpassword() {
            return this.rootpassword;
        }

        public void setRootpassword(final String rootpassword) {
            this.rootpassword = rootpassword;
        }

        public String getJarfile() {
            return this.jarfile;
        }

        public void setJarfile(final String jarfile) {
            this.jarfile = jarfile;
        }

        public JbossParams getJboss() {
            return this.jboss;
        }

        public void setJboss(final JbossParams jboss) {
            this.jboss = jboss;
        }

        public void validate() {
            vardbDir = CFileHelper.normalizeDirectory(CStringHelper.checkResolvedProperty("vardbDir", vardbDir));
            tempDir = CFileHelper.normalizeDirectory(CStringHelper.checkResolvedProperty("tempDir", tempDir));
            pfamRelease = CFileHelper.normalizeDirectory(CStringHelper.checkResolvedProperty("pfamRelease", pfamRelease));
            sqlDir = CFileHelper.normalizeDirectory(CStringHelper.checkResolvedProperty("sqlDir", sqlDir));
            xmlDir = CFileHelper.normalizeDirectory(CStringHelper.checkResolvedProperty("xmlDir", xmlDir));
            sequenceDir = CFileHelper.normalizeDirectory(CStringHelper.checkResolvedProperty("sequenceDir", sequenceDir));
            domainDir = CFileHelper.normalizeDirectory(CStringHelper.checkResolvedProperty("domainDir", domainDir));
            othersequenceDir = CFileHelper.normalizeDirectory(CStringHelper.checkResolvedProperty("othersequenceDir", othersequenceDir));
            alignmentDir = CFileHelper.normalizeDirectory(CStringHelper.checkResolvedProperty("alignmentDir", alignmentDir));
            pdbDir = CFileHelper.normalizeDirectory(CStringHelper.checkResolvedProperty("pdbDir", pdbDir));
            bundleDir = CFileHelper.normalizeDirectory(CStringHelper.checkResolvedProperty("bundleDir", bundleDir));
            userfile = CFileHelper.normalize(userfile);
            jarfile = CFileHelper.normalize(CStringHelper.checkResolvedProperty("jarfile", jarfile));
            db.validate();
            jboss.validate();
        }

        public static Map<String, Object> getPropertyMap() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            mapProperty(map, "vardbDir", "vardb.dir");
            mapProperty(map, "userfile", "users.filename");
            mapProperty(map, "tempDir", "temp.dir");
            mapProperty(map, "pfamRelease", "pfam.release");
            mapProperty(map, "sqlDir", "sql.dir");
            mapProperty(map, "xmlDir", "xml.dir");
            mapProperty(map, "sequenceDir", "sequence.dir");
            mapProperty(map, "domainDir", "domain.dir");
            mapProperty(map, "othersequenceDir", "othersequence.dir");
            mapProperty(map, "alignmentDir", "alignment.dir");
            mapProperty(map, "pdbDir", "pdb.dir");
            mapProperty(map, "bundleDir", "bundle.dir");
            mapProperty(map, "jarfile", "jars.filename");
            mapProperty(map, "importdb", "importdb");
            mapProperty(map, "rootid", "root.id");
            mapProperty(map, "rootusername", "root.username");
            mapProperty(map, "rootpassword", "root.password");
            mapProperty(map, "importdb", "importdb");
            mapProperty(map, "jboss.", "libDir", "lib.dir");
            mapProperty(map, "jboss.", "deployDir", "deploy.dir");
            mapProperty(map, "db.", "driver", "driver");
            mapProperty(map, "db.", "name", "name");
            mapProperty(map, "db.", "driver", "driver");
            mapProperty(map, "db.", "jndi", "jndi");
            mapProperty(map, "db.", "template", "template");
            mapProperty(map, "db.", "host", "host");
            mapProperty(map, "db.", "port", "port");
            mapProperty(map, "db.", "username", "username");
            mapProperty(map, "db.", "password", "password");
            mapProperty(map, "db.", "basedb", "basedb");
            return map;
        }

        private static void mapProperty(Map<String, Object> map, String name, String value) {
            mapProperty(map, "", name, value);
        }

        private static void mapProperty(Map<String, Object> map, String prefix, String name, String value) {
            map.put(prefix + name, "${" + prefix + value + "}");
        }

        public Properties getProperties() {
            Properties properties = new Properties();
            properties.setProperty("db.driver", db.getDriver());
            properties.setProperty("db.name", db.getName());
            properties.setProperty("db.template", db.getTemplate());
            properties.setProperty("db.host", db.getHost());
            properties.setProperty("db.port", db.getPort());
            properties.setProperty("db.username", db.getUsername());
            properties.setProperty("db.password", db.getPassword());
            return properties;
        }

        @Override
        public String toString() {
            return CStringHelper.toString(this);
        }
    }

    public static class Args {

        protected String filename;

        protected List<Action> actions = new ArrayList<Action>();

        private static final String FILENAME = "filename";

        private static final String ACTION = "action";

        public Args(String[] args) {
            Options options = new Options();
            options.addOption(FILENAME, true, "properties files");
            options.addOption(ACTION, true, "operation to perform");
            CommandLine line = CFileHelper.parseCommandLine(args, options);
            if (line.hasOption(FILENAME)) {
                this.filename = CFileHelper.normalize(line.getOptionValue(FILENAME));
                if (!CFileHelper.exists(this.filename)) throw new CVardbException("can't find setup file: " + this.filename);
            }
            if (line.hasOption(ACTION)) this.actions = parseActions(line.getOptionValue(ACTION));
        }

        private List<Action> parseActions(String values) {
            List<Action> actions = new ArrayList<Action>();
            for (String value : CStringHelper.split(values, ACTION_DELIMITER)) {
                actions.add(Action.find(value));
            }
            return actions;
        }

        public static String getHelp() {
            StringBuilder buffer = new StringBuilder();
            buffer.append("usage: ant setup -Dfilename=setup.properties -Daction=all\n");
            buffer.append("available actions:\n");
            for (Action action : Action.values()) {
                buffer.append(action.name() + ": " + action.getDescription() + "\n");
            }
            return buffer.toString();
        }

        public Params loadParams() {
            GenericApplicationContext context = new GenericApplicationContext();
            CSpringHelper.registerPropertyPlaceholderConfigurer(context, DEFAULT_SETUP_PROPERTIES_PATH, CFileHelper.getFilenameAsUrl(filename));
            Map<String, Object> map = Params.getPropertyMap();
            CSpringHelper.registerBean(context, "params", Params.class, map);
            context.refresh();
            Params params = (Params) context.getBean("params");
            params.validate();
            System.out.println("setup params=" + params.toString());
            return params;
        }
    }

    public enum Action {

        all("Perform all steps"), test("To test the setup tool (it does nothing, only feedback on variable values"), initdb("Perform database initialization: createdb, createtables and loadusers"), loaddata("Load information, sequences, etc. into the database"), annotate("Obtain information from external resources like Pfam, NCBI taxonomy and PubMed."), dropdb("Drop database if exists"), createdb("Create databas"), createtables("load setup SQL commands"), xml("load XML files in data directory"), drugs("update drug information from KEGG drug"), loadusers("load users from XML user file"), importusers("import users from another database"), sequences("load pipeline-detected sequences"), domains("load Pfam domain information"), othersequences("load non-pipeline sequences"), alignments("protein alignments"), bundles("load clinical data bundles/tags"), pfam("update Pfam information"), taxonomy("update NCBI taxonomy data"), refs("update PubMed references"), pdb("load PDB structures"), counts("update sequence counts"), blast("update blast database"), jars("check required jars"), ds("create a datasource file in the JBoss deploy directory"), vacuum("run vacuum analyze on the database to refresh query statistics"), help("show usage and action types");

        private String description;

        Action(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public static Action find(String value) {
            try {
                return Action.valueOf(value.trim());
            } catch (IllegalArgumentException e) {
                throw new CVardbException("cannot find action type: " + value);
            }
        }
    }
}
