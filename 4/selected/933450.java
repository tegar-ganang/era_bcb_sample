package org.oboedit.launcher;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;
import org.bbop.expression.ExpressionException;
import org.bbop.expression.ExpressionUtil;
import org.bbop.expression.JexlContext;
import org.bbop.io.IOUtil;
import org.obo.dataadapter.GOStyleAnnotationFileAdapter;
import org.obo.dataadapter.OBDSQLDatabaseAdapter;
import org.obo.dataadapter.OBOAdapter;
import org.obo.dataadapter.OBOFileAdapter;
import org.obo.dataadapter.OBOSerializationEngine;
import org.obo.dataadapter.OBDSQLDatabaseAdapter.OBDSQLDatabaseAdapterConfiguration;
import org.obo.dataadapter.OBOFileAdapter.OBOAdapterConfiguration;
import org.obo.datamodel.OBOSession;
import org.obo.filters.Filter;
import org.obo.util.FilterUtil;
import org.oboedit.controller.ExpressionManager;
import org.oboedit.gui.Preferences;

public class Database2OBO {

    protected static final Logger logger = Logger.getLogger(Database2OBO.class);

    protected static class DanglingWrapper {

        protected String id;

        protected String text;

        public DanglingWrapper(String id, String text) {
            this.id = id;
            this.text = text;
        }

        public String getID() {
            return id;
        }

        public String getName() {
            return text;
        }
    }

    protected static class ScriptWrapper {

        protected String script;

        protected LinkedList<String> args = new LinkedList<String>();

        public LinkedList<String> getArgs() {
            return args;
        }

        public void setArgs(LinkedList<String> args) {
            this.args = args;
        }

        public String getScript() {
            return script;
        }

        public void setScript(String script) {
            this.script = script;
        }
    }

    public static void convertFiles(OBDSQLDatabaseAdapter.OBDSQLDatabaseAdapterConfiguration readConfig, OBOFileAdapter.OBOAdapterConfiguration writeConfig, List<ScriptWrapper> scripts, OBOAdapter writer) throws Exception {
        OBDSQLDatabaseAdapter adapter = new OBDSQLDatabaseAdapter();
        OBOSession session = adapter.doOperation(OBOAdapter.READ_ONTOLOGY, readConfig, null);
        Iterator<ScriptWrapper> it = scripts.iterator();
        while (it.hasNext()) {
            ScriptWrapper wrapper = it.next();
            runScript(session, wrapper.getScript(), wrapper.getArgs());
        }
        logger.info("About to write files..., session object count = " + session.getObjects().size());
        logger.info("writePath = " + writeConfig.getWritePath());
        logger.info("savePath = " + writeConfig.getSaveRecords());
        writer.doOperation(OBOAdapter.WRITE_ONTOLOGY, writeConfig, session);
    }

    public static void runScript(OBOSession session, String script, List<String> args) throws ExpressionException {
        JexlContext context = ExpressionManager.getManager().getContext();
        context.setGlobalVariable("session", session, false);
        context.setLocalVariable("args", args, true);
        ExpressionUtil.exec(script, context);
    }

    public static void main(String[] args) throws Exception {
        IOUtil.setUpLogging();
        logger.info("version = " + Preferences.getVersion());
        if (args.length == 0) printUsage(1);
        OBDSQLDatabaseAdapterConfiguration readConfig = new OBDSQLDatabaseAdapter.OBDSQLDatabaseAdapterConfiguration();
        readConfig.setBasicSave(false);
        OBOAdapterConfiguration writeConfig = new OBOFileAdapter.OBOAdapterConfiguration();
        writeConfig.setBasicSave(false);
        LinkedList<ScriptWrapper> scripts = new LinkedList<ScriptWrapper>();
        OBOAdapter writer = new OBOFileAdapter();
        String formatVersion = "OBO_1_2";
        for (int i = 0; i < args.length; i++) logger.info("args[" + i + "] = |" + args[i] + "|");
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-formatversion")) {
                if (i >= args.length - 1) printUsage(1);
                i++;
                formatVersion = args[i];
                if (!(formatVersion.equals("OBO_1_2") || formatVersion.equals("OBO_1_0"))) printUsage(1);
            } else if (args[i].equals("-assoc")) {
                writer = new GOStyleAnnotationFileAdapter();
            } else if (args[i].equals("-allowdangling")) {
                readConfig.setAllowDangling(true);
            } else if (args[i].equals("-runscript")) {
                if (i >= args.length - 1) printUsage(1);
                i++;
                String scriptFile = args[i];
                String script = IOUtil.readFile(scriptFile);
                ScriptWrapper wrapper = new ScriptWrapper();
                wrapper.setScript(script);
                for (i = i + 1; i < args.length; i++) {
                    if (args[i].equals(";")) {
                        break;
                    }
                    wrapper.getArgs().add(args[i]);
                }
                scripts.add(wrapper);
            } else if (args[i].equals("-o")) {
                if (i >= args.length - 1) printUsage(1);
                i++;
                OBOSerializationEngine.FilteredPath path = new OBOSerializationEngine.FilteredPath();
                path.setUseSessionReasoner(false);
                for (; i < args.length; i++) {
                    if (args[i].equals("-f")) {
                        if (i >= args.length - 1) printUsage(1);
                        i++;
                        String filterFile = args[i];
                        Filter filter = FilterUtil.loadFilter(filterFile);
                        path.setDoFilter(filter != null);
                        path.setObjectFilter(filter);
                    } else if (args[i].equals("-allowdangling")) {
                        path.setAllowDangling(true);
                    } else if (args[i].equals("-strictrootdetection")) {
                        path.setRootAlgorithm("STRICT");
                    } else if (args[i].equals("-saveimpliedlinks")) {
                        path.setSaveImplied(true);
                        path.setImpliedType(OBOSerializationEngine.SAVE_TRIMMED_LINKS);
                    } else if (args[i].equals("-saveallimpliedlinks")) {
                        path.setSaveImplied(true);
                        path.setImpliedType(OBOSerializationEngine.SAVE_ALL);
                    } else if (args[i].equals("-realizeimpliedlinks")) {
                        path.setAssertImpliedLinks(true);
                    } else if (args[i].equals("-p")) {
                        if (i >= args.length - 1) printUsage(1);
                        i++;
                        String prefilterProperty = args[i];
                        path.setPrefilterProperty(prefilterProperty);
                    } else {
                        path.setPath(args[i]);
                        break;
                    }
                }
                logger.info("Allowdangling = " + path.getAllowDangling());
                if (path.getPath() == null) printUsage(1); else writeConfig.getSaveRecords().add(path);
            } else if (args[i].equals("-?")) {
                printUsage(0);
            } else {
                readConfig.setReadPath(args[i]);
            }
        }
        if (readConfig.getReadPath() == null) {
            logger.info("You must specify a file to load.");
            printUsage(1);
        }
        if (writeConfig.getSaveRecords().size() < 1) {
            if (scripts.size() == 0) {
                System.err.println("You must specify at least one file to save.");
                printUsage(1);
            }
        }
        writeConfig.setSerializer(formatVersion);
        convertFiles(readConfig, writeConfig, scripts, writer);
    }

    protected static void printUsage(int exitCode) {
        System.err.println("database2obo [-?]  <jdbcpath 1> ... <jdbcpath N> \\\n" + "    [-parsecomments] [-writecomments] \\\n" + "     [-script <scriptname> [arg1 arg2 ... argN] \\;] \\\n" + "   [-o [-f <filterfile1.xml>] <file1>] ... \\\n" + "   [-o [-f <filterfileN.xml>] <fileN>]");
        System.err.println("  -?                         - Writes this page to stderr and exits.");
        System.err.println("  -formatversion <versionid> - The version of OBO to write. Allowed values are\n" + "                               OBO_1_0 and OBO_1_2. The default is OBO_1_2.\n" + "                               Optional.");
        System.err.println("  -script <scriptname> <args> \\; - Runs an OSL script on the ontology. A script tag's " + "                                             arguments MUST be followed by a \\; sequence.");
        System.err.println("  <jdbcpathN>                - A JDBC location");
        System.err.println("  <filenameN>                - An obo file to load. Any number of OBO files may\n" + "                               be loaded");
        System.err.println("  -o [-f <filterfile.xml>] [-allowdangling] [-p <prefilter property id>] [-strictrootdetection] [-saveimpliedlinks|-saveallimpliedlinks] [-realizeimpliedlinks] obofilepath - \n" + "        An output file to write. The optional -f flag may be used to specify a\n" + "        filter file to apply to the output file before writing. If the \n" + "        -allowdangling flag is specified, dangling links will not be written.\n" + "        The optional -p flag specifies the ID of a property to use for \n" + "        reasoner pre-filtering. The optional -strict-root-detection flag\n" + "        applies filters using strict root detection.");
        System.exit(exitCode);
    }
}
