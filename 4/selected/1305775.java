package org.databene.dbsanity;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import org.databene.commons.CollectionUtil;
import org.databene.commons.StringUtil;
import org.databene.commons.SystemInfo;
import org.databene.commons.ui.ConsoleInfoPrinter;
import org.databene.commons.version.VersionInfo;
import org.databene.commons.version.VersionNumber;
import org.databene.dbsanity.model.SanityCheckSuite;
import org.databene.jdbacl.version.ConstantVersionProvider;

/**
 * DB Sanity's main class which parses the command line arguments, 
 * prints out help and version information and 
 * invokes core functionality of DB Sanity.<br/>
 * <br/>
 * Created: 11.03.2011 11:24:31
 * @since 0.7
 * @author Volker Bergmann
 */
public class Main {

    public static void main(String[] args) throws Exception {
        DbSanity instance = parseCLAs(args);
        SanityCheckSuite suite = instance.execute();
        boolean success = (suite.getDefectCount() + suite.countErredChecks() == 0);
        System.exit(success ? 0 : -1);
    }

    static DbSanity parseCLAs(String[] args) throws IOException {
        DbSanity instance = new DbSanity();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("-h".equals(arg)) {
                printHelpAndExit(0);
            } else if ("-a".equals(arg) || "--appversion".equals(arg)) {
                if (++i == args.length) printHelpAndExit(-1);
                instance.setVersionProvider(new ConstantVersionProvider(VersionNumber.valueOf(args[i])));
            } else if ("-c".equals(arg) || "--clear".equals(arg)) {
                instance.setClearBefore(true);
            } else if ("-C".equals(arg) || "--condensed".equals(arg)) {
                instance.setCondensed(true);
            } else if ("-b".equals(arg) || "--buffer".equals(arg)) {
                instance.setMetaDataCached(true);
            } else if ("-B".equals(arg) || "--browse".equals(arg)) {
                instance.setBrowsing(true);
            } else if ("-k".equals(arg) || "--keep".equals(arg)) {
                instance.setDeleteTempAfter(false);
            } else if ("-v".equals(arg) || "--verbose".equals(arg)) {
                instance.setMode(ExecutionMode.VERBOSE);
            } else if ("-V".equals(arg) || "--version".equals(arg)) {
                System.out.println("DB Sanity " + VersionInfo.getInfo("dbsanity").getVersion());
                System.exit(0);
            } else if ("-q".equals(arg) || "--quiet".equals(arg)) {
                instance.setMode(ExecutionMode.QUIET);
            } else if ("-i".equals(arg) || "--in".equals(arg)) {
                if (++i == args.length) printHelpAndExit(-1);
                instance.setCheckDefinitionFile(new File(args[i]));
            } else if ("-o".equals(arg) || "--out".equals(arg)) {
                if (++i == args.length) printHelpAndExit(-1);
                instance.setReportFolder(new File(args[i]));
            } else if ("-t".equals(arg) || "--tables".equals(arg)) {
                if (++i == args.length) printHelpAndExit(-1);
                String[] tables = StringUtil.tokenize(args[i], ',');
                StringUtil.trimAll(tables);
                instance.setTables(tables);
            } else if ("-T".equals(arg) || "--tags".equals(arg)) {
                if (++i == args.length) printHelpAndExit(-1);
                String[] tags = StringUtil.tokenize(args[i], ',');
                StringUtil.trimAll(tags);
                instance.setTags(CollectionUtil.toSet(tags));
            } else if ("-s".equals(arg) || "--skin".equals(arg)) {
                if (++i == args.length) printHelpAndExit(-1);
                instance.setSkin(args[i]);
            } else if ("-l".equals(arg) || "--locale".equals(arg)) {
                if (++i == args.length) printHelpAndExit(-1);
                instance.setLocale(new Locale(args[i]));
            } else if ("-L".equals(arg) || "--limit".equals(arg)) {
                if (++i == args.length) printHelpAndExit(-1);
                instance.setDefectCountLimit(Integer.parseInt(args[i]));
            } else if ("-m".equals(arg) || "--maxrows".equals(arg)) {
                if (++i == args.length) printHelpAndExit(-1);
                instance.setDefectRowLimit(Integer.parseInt(args[i]));
            } else if ("-z".equals(arg) || "--zip".equals(arg)) {
                instance.setZipping(true);
            } else if (arg.startsWith("-")) {
                ConsoleInfoPrinter.printHelp("Illegal option: " + arg);
                printHelpAndExit(-1);
            } else {
                instance.setEnvironment(arg);
            }
        }
        if (instance.getConnectData() == null) {
            ConsoleInfoPrinter.printHelp("Error: missing environment name");
            System.exit(-2);
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(args[i]);
        }
        instance.setCommandLine(builder.toString());
        return instance;
    }

    private static void printHelpAndExit(int status) {
        printHelp();
        System.exit(status);
    }

    private static void printHelp() {
        ConsoleInfoPrinter.printHelp("DB Sanity " + VersionInfo.getInfo("dbsanity"));
        if (SystemInfo.isWindows()) ConsoleInfoPrinter.printHelp("Usage: dbsanity.bat [options] <environment>"); else ConsoleInfoPrinter.printHelp("Usage: dbsanity.sh [options] <environment>");
        String PS = SystemInfo.getPathSeparator();
        ConsoleInfoPrinter.printHelp("", "Options:", "-a,--appversion <arg>   apply only checks for the specified application version", "-i,--in <arg>           read the check definitions from the specified file ", "                        (default: " + DbSanity.DEFAULT_CHECK_DEFINITION_FOLDER + PS + ")", "-o,--out <arg>          write the report into the specified file or directory", "                        (default: " + DbSanity.DEFAULT_REPORT_FOLDER + PS + ")", "-b,--buffer             buffer database meta data", "-B,--browse             open report in web browser", "-c,--clear              clear report directory before generating the report", "                        (default: disabled)", "-C,--condensed          create a condensed report with only problematic checks", "                        (default: disabled)", "-h,--help               print this help", "-k,--keep               keep the temoporary files (for developers)", "-l,--locale <arg>       render the report using the specified locale", "                        (2 letter iso code)", "-L,--limit <arg>        limit the number of processed defects", "-m,--maxrows <arg>      limit the number of defect rows reported ", "                        in the defect list", "-q,--quiet              quiet mode", "-s,--skin <arg>         render the report using the specified skin", "                        -s online  requires internet access for viewing", "                        -s offline can be viewed offline", "-t,--tables <arg>       perform checks only for a list of tables", "-T,--tag <arg>          perform checks only for a list of tags", "-v,--verbose            verbose mode", "-V,--version            prints out DB Sanity's version number", "-z,--zip                write the report to a zip file", "", "The environment, eg. 'mydb', refers to a properties file, ", "e.g. 'mydb.env.properties',", "which must provide JDBC connection data in the following format:", "	db_url=jdbc:hsqldb:hsql://localhost/mydb", "	db_driver=org.hsqldb.jdbcDriver", "	db_user=customer", "	db_password=secret");
    }
}
