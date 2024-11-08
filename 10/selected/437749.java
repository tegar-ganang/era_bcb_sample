package nuts.tools.sql;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import nuts.core.io.IOUtils;
import nuts.core.lang.LocaleUtils;
import nuts.core.lang.StringUtils;
import nuts.core.orm.sql.SqlExecutor;
import nuts.core.orm.sql.engine.SimpleSqlExecutor;

/**
 * Convert properties to SQL scripts
 */
public class Properties2Sql extends SqlTool {

    /**
	 * Main class for Properties2Sql
	 */
    public static class Main extends SqlTool.Main {

        /**
		 * @param args arguments
		 */
        public static void main(String[] args) {
            Main cgm = new Main();
            Object cg = new Properties2Sql();
            cgm.execute(cg, args);
        }

        @SuppressWarnings("static-access")
        protected void addCommandLineOptions(Options options) throws Exception {
            super.addCommandLineOptions(options);
            options.addOption(OptionBuilder.withArgName("directory").hasArg().withDescription("property files directory").create("d"));
            options.addOption(OptionBuilder.withArgName("extension").hasArg().withDescription("properties file extension [default: " + Properties2Sql.PRO_EXT + "]").create("e"));
            options.addOption(OptionBuilder.withArgName("insert sql").hasArg().withDescription("insert sql template [e.g.: INSERT INTO RESOURCE VALUES(#clazz#, #language#, #country#, #variant#, #name#, #value#)").create("is"));
            options.addOption(OptionBuilder.withArgName("delete sql").hasArg().withDescription("delete sql template [e.g.: DELETE FROM RESOURCE WHERE CLAZZ=#clazz# AND LANGUAGE=#language# AND COUNTRY=#country# AND VARIANT=#variant# AND NAME=#name)").create("ds"));
            options.addOption(OptionBuilder.withArgName("prefix").hasArg().withDescription("class name prefix").create("p"));
        }

        protected void getCommandLineOptions(CommandLine cl, Options options) throws Exception {
            super.getCommandLineOptions(cl, options);
            if (cl.hasOption("d")) {
                setParameter("dir", new File(cl.getOptionValue("d")));
            } else {
                printHelp(options, "parameter [directory] is required.");
            }
            if (cl.hasOption("e")) {
                setParameter("ext", cl.getOptionValue("e"));
            }
            if (cl.hasOption("is")) {
                setParameter("insertSql", cl.getOptionValue("is"));
            } else {
                printHelp(options, "parameter [insert sql] is required.");
            }
            if (cl.hasOption("ds")) {
                setParameter("deleteSql", cl.getOptionValue("ds"));
            }
            if (cl.hasOption("p")) {
                setParameter("prefix", cl.getOptionValue("p"));
            }
        }
    }

    private static final String PRO_EXT = ".properties";

    private File dir;

    private String ext = PRO_EXT;

    private String insertSql;

    private String prefix;

    private int cntFile = 0;

    private int cntIns = 0;

    /**
	 * @param dir the dir to set
	 */
    public void setDir(File dir) {
        this.dir = dir;
    }

    /**
	 * @param ext the ext to set
	 */
    public void setExt(String ext) {
        this.ext = ext;
    }

    /**
	 * @param insertSql the insertSql to set
	 */
    public void setInsertSql(String insertSql) {
        this.insertSql = insertSql;
    }

    /**
	 * @param prefix the prefix to set
	 */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    protected void checkParameters() throws Exception {
        if (dir == null) {
            throw new IllegalArgumentException("parameter [dir] is required.");
        }
        if (!dir.exists()) {
            throw new IllegalArgumentException("[" + dir.getPath() + "] does not exists.");
        }
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("[" + dir.getPath() + "] is not a directory.");
        }
        if (StringUtils.isEmpty(ext)) {
            throw new IllegalArgumentException("parameter [ext] is required.");
        }
        if (StringUtils.isEmpty(insertSql)) {
            throw new IllegalArgumentException("parameter [insertSql] is required.");
        }
    }

    /**
	 * execute
	 * @throws Exception if an error occurs
	 */
    public void execute() throws Exception {
        checkParameters();
        System.out.println("Processing properties: " + dir.getPath());
        connect();
        try {
            connection.setAutoCommit(false);
            cntFile = 0;
            cntIns = 0;
            processFile(dir);
            System.out.println(cntIns + " records of " + cntFile + " properties fils inserted successfully");
        } finally {
            disconnect();
        }
    }

    private String getClazz(File f) {
        String c = f.getPath().substring(dir.getPath().length());
        c = c.substring(0, c.length() - ext.length());
        c = c.replace('/', '.').replace('\\', '.');
        if (c.charAt(0) == '.') {
            c = c.substring(1);
        }
        int ub = c.indexOf('_');
        if (ub > 0) {
            c = c.substring(0, ub);
        }
        return c;
    }

    private String getLocaleValue(String val) {
        return val == null ? "" : val;
    }

    private static final Locale defaultLocale = new Locale("", "", "");

    private void properties2sql(File f) throws Exception {
        Properties p = new Properties();
        FileInputStream fis = new FileInputStream(f);
        try {
            p.load(fis);
        } catch (Exception e) {
            System.err.println("Failed to process " + f.getPath());
            throw e;
        } finally {
            IOUtils.closeQuietly(fis);
        }
        SqlExecutor executor = new SimpleSqlExecutor(connection);
        cntFile++;
        try {
            for (Iterator<Entry<Object, Object>> it = p.entrySet().iterator(); it.hasNext(); ) {
                Entry<Object, Object> en = it.next();
                String k = en.getKey().toString();
                String v = en.getValue().toString();
                Map<String, String> param = new HashMap<String, String>();
                String clazz = getClazz(f);
                if (clazz.startsWith(prefix)) {
                    clazz = clazz.substring(prefix.length());
                }
                param.put("clazz", clazz);
                Locale locale = LocaleUtils.localeFromFileName(f, defaultLocale);
                if (StringUtils.isNotEmpty(locale.toString()) && !LocaleUtils.isAvailableLocale(locale)) {
                    System.out.println("Warning: " + locale + " is not a valid Locale [" + f.getName() + "]");
                }
                param.put("language", getLocaleValue(locale.getLanguage()));
                param.put("country", getLocaleValue(locale.getCountry()));
                param.put("variant", getLocaleValue(locale.getVariant()));
                param.put("name", k);
                param.put("value", v);
                cntIns += executor.executeUpdate(insertSql, param);
            }
            connection.commit();
        } catch (Exception e) {
            rollback();
            System.err.println("Failed to process " + f.getPath());
            throw e;
        }
    }

    protected void processFile(File f) throws Exception {
        if (f.isHidden()) {
            ;
        } else if (f.isDirectory()) {
            File[] sfs = f.listFiles();
            for (File sf : sfs) {
                processFile(sf);
            }
        } else if (f.isFile()) {
            if (f.getName().endsWith(ext)) {
                properties2sql(f);
            }
        }
    }
}
