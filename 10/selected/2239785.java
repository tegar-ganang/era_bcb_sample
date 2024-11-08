package nuts.tools.sql;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import nuts.core.io.IOUtils;
import nuts.core.lang.ArrayUtils;
import nuts.core.lang.LocaleUtils;
import nuts.core.lang.StringUtils;
import nuts.core.orm.sql.SqlExecutor;
import nuts.core.orm.sql.engine.SimpleSqlExecutor;

/**
 * Convert templates to SQL scripts
 */
public class Templates2Sql extends SqlTool {

    /**
	 * Main class for Templates2Sql
	 */
    public static class Main extends SqlTool.Main {

        /**
		 * @param args arguments
		 */
        public static void main(String[] args) {
            Main cgm = new Main();
            Object cg = new Templates2Sql();
            cgm.execute(cg, args);
        }

        @SuppressWarnings("static-access")
        protected void addCommandLineOptions(Options options) throws Exception {
            super.addCommandLineOptions(options);
            options.addOption(OptionBuilder.withArgName("directory").hasArg().withDescription("template files directory").create("d"));
            options.addOption(OptionBuilder.withArgName("extension").hasArg().withDescription("template source file extension [default: " + Templates2Sql.TPL_EXT + "]").create("e"));
            options.addOption(OptionBuilder.withArgName("insert sql").hasArg().withDescription("insert sql template [e.g.: INSERT INTO TEMPLATE VALUES(#language#, #country#, #variant#, #source#) ]").create("is"));
            options.addOption(OptionBuilder.withArgName("delete sql").hasArg().withDescription("delete sql template [e.g.: DELETE FROM TEMPLATE WHERE LANGUAGE=#language# AND COUNTRY=#country#) ]").create("ds"));
            options.addOption(OptionBuilder.withArgName("update sql").hasArg().withDescription("update sql template [e.g.: UPDATE TEMPLATE SET SOURCE=#source# WHERE LANGUAGE=#language# AND COUNTRY=#country#) ]").create("us"));
            options.addOption(OptionBuilder.withArgName("charset").hasArg().withDescription("template source file charset").create("c"));
            options.addOption(OptionBuilder.withArgName("prefix").hasArg().withDescription("prefix of name").create("p"));
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
            if (cl.hasOption("us")) {
                setParameter("updateSql", cl.getOptionValue("us"));
            }
            if (cl.hasOption("c")) {
                setParameter("charset", cl.getOptionValue("c"));
            }
            if (cl.hasOption("p")) {
                setParameter("prefix", cl.getOptionValue("p"));
            }
        }
    }

    private static final String TPL_EXT = ".ftl";

    private File dir;

    private String ext = TPL_EXT;

    private String deleteSql;

    private String updateSql;

    private String insertSql;

    private String charset;

    private String prefix;

    private int cntFile = 0;

    private int cntDel = 0;

    private int cntUpd = 0;

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
	 * @return the deleteSql
	 */
    public String getDeleteSql() {
        return deleteSql;
    }

    /**
	 * @param deleteSql the deleteSql to set
	 */
    public void setDeleteSql(String deleteSql) {
        this.deleteSql = deleteSql;
    }

    /**
	 * @param updateSql the updateSql to set
	 */
    public void setUpdateSql(String updateSql) {
        this.updateSql = updateSql;
    }

    /**
	 * @param insertSql the insertSql to set
	 */
    public void setInsertSql(String insertSql) {
        this.insertSql = insertSql;
    }

    /**
	 * @param charset the charset to set
	 */
    public void setCharset(String charset) {
        this.charset = charset;
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
        System.out.println("Processing templates: " + dir.getPath());
        connect();
        try {
            connection.setAutoCommit(false);
            cntFile = 0;
            cntDel = 0;
            cntUpd = 0;
            cntIns = 0;
            processFile(dir);
            String s = cntFile + " files processed";
            if (cntDel > 0) {
                s += ", " + cntDel + " templates deleted";
            }
            if (cntUpd > 0) {
                s += ", " + cntUpd + " templates updated";
            }
            if (cntIns > 0) {
                s += ", " + cntIns + " templates inserted";
            }
            System.out.println(s + " successfully");
        } finally {
            disconnect();
        }
    }

    private String getName(File f) {
        String name = f.getPath().substring(dir.getPath().length()).replace('\\', '/');
        name = name.startsWith("/") ? name.substring(1) : name;
        name = IOUtils.stripFileNameExtension(name);
        String[] sa = name.split("\\_");
        if (sa.length > 3) {
            if (sa[sa.length - 3].length() == 2 && sa[sa.length - 2].length() == 2) {
                name = StringUtils.join(ArrayUtils.subarray(sa, 0, sa.length - 3), '_');
            } else if (sa[sa.length - 2].length() == 2 && sa[sa.length - 1].length() == 2) {
                name = StringUtils.join(ArrayUtils.subarray(sa, 0, sa.length - 2), '_');
            } else if (sa[sa.length - 1].length() == 2) {
                name = StringUtils.join(ArrayUtils.subarray(sa, 0, sa.length - 1), '_');
            }
        } else if (sa.length == 3) {
            if (sa[sa.length - 2].length() == 2 && sa[sa.length - 1].length() == 2) {
                name = StringUtils.join(ArrayUtils.subarray(sa, 0, sa.length - 2), '_');
            } else if (sa[sa.length - 1].length() == 2) {
                name = StringUtils.join(ArrayUtils.subarray(sa, 0, sa.length - 1), '_');
            }
        } else if (sa.length == 2) {
            if (sa[sa.length - 1].length() == 2) {
                name = StringUtils.join(ArrayUtils.subarray(sa, 0, sa.length - 1), '_');
            }
        }
        return name;
    }

    private String getLocaleValue(String val) {
        return val == null ? "" : val;
    }

    private static final Locale defaultLocale = new Locale("", "", "");

    private void template2sql(File f) throws Exception {
        FileInputStream fis = new FileInputStream(f);
        SqlExecutor executor = new SimpleSqlExecutor(connection);
        cntFile++;
        try {
            byte[] buf = new byte[fis.available()];
            fis.read(buf);
            Map<String, String> param = new HashMap<String, String>();
            String name = getName(f);
            if (StringUtils.isNotEmpty(prefix)) {
                name = prefix + name;
            }
            param.put("name", name);
            Locale locale = LocaleUtils.localeFromFileName(f, defaultLocale);
            param.put("language", getLocaleValue(locale.getLanguage()));
            param.put("country", getLocaleValue(locale.getCountry()));
            param.put("variant", getLocaleValue(locale.getVariant()));
            String source;
            if (StringUtils.isNotEmpty(charset)) {
                source = new String(buf, charset);
            } else {
                String c = LocaleUtils.charsetFromLocale(locale);
                if (StringUtils.isNotEmpty(c)) {
                    source = new String(buf, c);
                } else {
                    source = new String(buf);
                }
            }
            if (source.length() > 0 && source.charAt(0) == '﻿') {
                source = source.substring(1);
            }
            param.put("source", source);
            if (StringUtils.isNotEmpty(deleteSql)) {
                cntDel += executor.executeUpdate(deleteSql, param);
            }
            int cu = 0;
            if (StringUtils.isNotEmpty(updateSql)) {
                cu = executor.executeUpdate(updateSql, param);
                cntUpd += cu;
            }
            if (cu == 0) {
                cntIns += executor.executeUpdate(insertSql, param);
            }
            connection.commit();
        } catch (Exception e) {
            rollback();
            System.err.println("Failed to process " + f.getPath());
            throw e;
        } finally {
            IOUtils.closeQuietly(fis);
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
                template2sql(f);
            }
        }
    }
}
