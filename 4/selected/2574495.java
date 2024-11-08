package sourcedoc;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import com.osisc.Assert;
import com.osisc.ReflexionException;
import com.osisc.Request;
import com.osisc.RequestQueue;
import com.osisc.autodoc.DocWriter;
import com.osisc.autodoc.SourceWriter;
import com.osisc.autodoc.internal.DocCoordinator;
import com.osisc.autodoc.internal.DocList;
import com.osisc.autodoc.internal.DocWorker;
import com.osisc.autodoc.internal.ParseController;
import com.osisc.io.DirResolutionException;
import com.osisc.io.FileInputException;
import com.osisc.util.PropertiesFilter;

/**
 * The main class of SourceDoc.
 */
public class Main {

    /** The index of all docs */
    private static final Map docMap = new HashMap();

    /** The context for of all number docs */
    private static final Map contextMap = new HashMap();

    /** The map of all types */
    private static final Map typeMap = new HashMap();

    /** The map of all preprocessor constants */
    private static final Map defineMap = new HashMap();

    /** The map of all preprocessor macros */
    private static final Map macroMap = new HashMap();

    /** The list of input files */
    private static final List fileList = new ArrayList();

    /** The list of output doc files */
    private static final List extractList = new ArrayList();

    /** The list of output source files */
    private static final List createList = new ArrayList();

    /** The list of <...> include directories */
    private static final List sysDirList = new ArrayList();

    /** The list of "..." include directories */
    private static final List srcDirList = new ArrayList();

    /** The list of worker threads */
    private static final List workerList = new ArrayList();

    /** The default maximum number of worker threads */
    private static final int DEFAULT_NUM_WORKERS = 20;

    /** The maximum number of worker threads */
    private static int numWorkers = DEFAULT_NUM_WORKERS;

    /** The verification options */
    private static final PropertiesFilter verifyProps = DocList.setVerifyProps();

    /** The creation options */
    private static final PropertiesFilter createProps = DocList.setCreateProps();

    /** The extraction options */
    private static final PropertiesFilter extractProps = DocList.setExtractProps();

    private static final Coordinator coordinator = new Coordinator();

    /** The request queue */
    private static final RequestQueue in = new RequestQueue(Thread.currentThread());

    /** The current working directory */
    private static final File presentWorkingDirectory;

    /** The name of the default DocWriter */
    private static final String DEFAULT_DOC_WRITER = "com.osisc.autodoc.HTMLDocWriter";

    /** The name of the default DocWriter */
    private static final String DEFAULT_SOURCE_WRITER = "com.osisc.autodoc.DefaultSourceWriter";

    /** The name of the DocWriter to use */
    private static String docWriter = DEFAULT_DOC_WRITER;

    /** The name of the SourceWriter to use */
    private static String sourceWriter = DEFAULT_SOURCE_WRITER;

    static {
        File pwd = null;
        try {
            pwd = (new File(System.getProperty("user.dir"))).getCanonicalFile();
        } catch (IOException ioe) {
            dirResolutionException(new DirResolutionException(ioe.getMessage(), System.getProperty("user.dir")));
            System.exit(-1);
        }
        presentWorkingDirectory = pwd;
    }

    /** The default root of the output tree */
    private static File outPath = presentWorkingDirectory;

    /** The default root of the output source tree */
    private static File soutPath = presentWorkingDirectory;

    /** The default root of the input source tree */
    private static File sinPath = presentWorkingDirectory;

    /** No constructor. All static */
    private Main() {
        Assert.fail();
    }

    private static class Coordinator implements DocCoordinator {

        /** @see com.osisc.autodoc.internal.DocCoordinator#createProps() */
        public PropertiesFilter createProps() {
            return createProps;
        }

        /** @see com.osisc.autodoc.internal.DocCoordinator#extractProps() */
        public PropertiesFilter extractProps() {
            return extractProps;
        }

        /** @see com.osisc.autodoc.internal.DocCoordinator#verifyProps() */
        public PropertiesFilter verifyProps() {
            return verifyProps;
        }

        /** @see com.osisc.autodoc.internal.DocCoordinator#emitDocReply(boolean) */
        public void emitDocReply(boolean success) {
            in.enqueue(new EmitDocReply(success));
        }

        /** @see com.osisc.autodoc.internal.DocCoordinator#emitOverviewReply(boolean) */
        public void emitOverviewReply(boolean success) {
            in.enqueue(new EmitOverviewReply(success));
        }

        /** @see com.osisc.autodoc.internal.DocCoordinator#emitSourceReply(boolean) */
        public void emitSourceReply(boolean success) {
            in.enqueue(new EmitSourceReply(success));
        }

        /** @see com.osisc.autodoc.internal.DocCoordinator#emitContentsReply(boolean) */
        public void emitContentsReply(boolean success) {
            in.enqueue(new EmitContentsReply(success));
        }

        /** @see com.osisc.autodoc.internal.DocCoordinator#emitIndexReply(boolean) */
        public void emitIndexReply(boolean success) {
            in.enqueue(new EmitIndexReply(success));
        }

        /** @see com.osisc.autodoc.internal.DocCoordinator#emitNumbersReply(boolean) */
        public void emitNumbersReply(boolean success) {
            in.enqueue(new EmitNumbersReply(success));
        }

        /** @see com.osisc.autodoc.internal.DocCoordinator#parseReply(boolean, com.osisc.autodoc.internal.DocList) */
        public void parseReply(boolean success, DocList docs) {
            in.enqueue(new ParseReply(success, docs));
        }

        /** @see com.osisc.autodoc.internal.DocCoordinator#findFile(int, java.lang.String) */
        public File findFile(int path, String name) {
            File file;
            switch(path) {
                case ParseController.SYSTEM:
                    for (Iterator i = sysDirList.iterator(); i.hasNext(); ) {
                        if ((file = new File((String) i.next(), name)).canRead()) return file;
                    }
                    break;
                case ParseController.SOURCE:
                    for (Iterator i = srcDirList.iterator(); i.hasNext(); ) {
                        if ((file = new File((String) i.next(), name)).canRead()) return file;
                    }
                    break;
                default:
                    Assert.fail();
                    break;
            }
            return null;
        }

        /** @see com.osisc.autodoc.internal.DocCoordinator#getDocPath() */
        public File getDocPath() {
            return outPath;
        }

        /** @see com.osisc.autodoc.internal.DocCoordinator#getDocWriter(java.io.File, java.io.File) */
        public DocWriter getDocWriter(File path, File file) throws IOException, ReflexionException {
            try {
                Constructor dwc = Class.forName(docWriter).getConstructor(docWriterParam);
                return (DocWriter) dwc.newInstance(new Object[] { path, file });
            } catch (InvocationTargetException ite) {
                Throwable e = ite.getTargetException();
                if (!(e instanceof IOException)) throw new ReflexionException(e.toString(), docWriter); else throw (IOException) e;
            } catch (Exception e) {
                throw new ReflexionException(e.toString(), docWriter);
            }
        }

        private static final Class[] docWriterParam = new Class[] { File.class, File.class };

        /** @see com.osisc.autodoc.internal.DocCoordinator#getPwd() */
        public File getPwd() {
            return sinPath;
        }

        /** @see com.osisc.autodoc.internal.DocCoordinator#getSourcePath() */
        public File getSourcePath() {
            return soutPath;
        }

        /** @see com.osisc.autodoc.internal.DocCoordinator#getSourceWriter(java.io.File) */
        public SourceWriter getSourceWriter(File file) throws IOException, ReflexionException {
            try {
                Constructor swc = Class.forName(sourceWriter).getConstructor(sourceWriterParam);
                return (SourceWriter) swc.newInstance(new Object[] { file });
            } catch (InvocationTargetException ite) {
                Throwable e = ite.getTargetException();
                if (!(e instanceof IOException)) throw new ReflexionException(e.toString(), sourceWriter); else throw (IOException) e;
            } catch (Exception e) {
                throw new ReflexionException(e.toString(), sourceWriter);
            }
        }

        private static final Class[] sourceWriterParam = new Class[] { File.class };

        /** @see com.osisc.autodoc.internal.DocCoordinator#isDefine(java.lang.String) */
        public boolean isDefine(String name) {
            return defineMap.containsKey(name);
        }

        /** @see com.osisc.autodoc.internal.DocCoordinator#isMacro(java.lang.String) */
        public boolean isMacro(String name) {
            return macroMap.containsKey(name);
        }

        /** @see com.osisc.autodoc.internal.DocCoordinator#isType(java.lang.String) */
        public boolean isType(String name) {
            return typeMap.containsKey(name);
        }
    }

    /** Reply data carrier class */
    private static class WorkerReply extends Request {

        /** The return value */
        final boolean retVal;

        /** @param success the return value */
        WorkerReply(boolean success) {
            super(Thread.currentThread());
            retVal = success;
        }

        /** @see com.osisc.Request#execute() */
        protected void execute() {
        }
    }

    /** The data carrier request class to add a doclist */
    private static class ParseReply extends WorkerReply {

        /** The resulting doc list */
        final DocList docList;

        /**
       * @param success The return value
       * @param docs The resulting doc list
       */
        ParseReply(boolean success, DocList docs) {
            super(success);
            docList = docs;
        }
    }

    private static class EmitSourceReply extends WorkerReply {

        /** @param success The return value */
        EmitSourceReply(boolean success) {
            super(success);
        }
    }

    private static class EmitOverviewReply extends WorkerReply {

        /** @param success The return value */
        EmitOverviewReply(boolean success) {
            super(success);
        }
    }

    private static class EmitIndexReply extends WorkerReply {

        /** @param success The return value */
        EmitIndexReply(boolean success) {
            super(success);
        }
    }

    private static class EmitNumbersReply extends WorkerReply {

        /** @param success The return value */
        EmitNumbersReply(boolean success) {
            super(success);
        }
    }

    private static class EmitContentsReply extends WorkerReply {

        /** @param success The return value */
        EmitContentsReply(boolean success) {
            super(success);
        }
    }

    private static class EmitDocReply extends WorkerReply {

        /** @param success The return value */
        EmitDocReply(boolean success) {
            super(success);
        }
    }

    /** @param arg The command line to set state from */
    private static void setOptions(String[] arg) throws FileInputException, DirResolutionException {
        for (int i = 0; i < arg.length; i++) {
            String s = arg[i];
            if (s.charAt(0) == '-') {
                switch(s.charAt(1)) {
                    case 'D':
                        {
                            int k = s.indexOf('=');
                            int l = s.indexOf('(');
                            if (k == -1) {
                                if (l == -1) defineMap.put(s.substring(2), Boolean.TRUE); else macroMap.put(s.substring(2, l), Boolean.TRUE);
                            } else {
                                if ((l == -1) || (l > k)) defineMap.put(s.substring(2, k), Boolean.TRUE); else macroMap.put(s.substring(2, l), Boolean.TRUE);
                            }
                        }
                        break;
                    case 'T':
                        typeMap.put(s.substring(2), Boolean.TRUE);
                        break;
                    case 'I':
                        srcDirList.add(s.substring(2));
                        break;
                    case 'H':
                        sysDirList.add(s.substring(2));
                        break;
                    case 'E':
                        if (extractProps.get()) invalidSingletonOption("-E"); else setFilter(extractProps, s.substring(2), "extract.properties");
                        break;
                    case 'C':
                        if (createProps.get()) invalidSingletonOption("-C"); else setFilter(createProps, s.substring(2), "create.properties");
                        break;
                    case 'V':
                        if (verifyProps.get()) invalidSingletonOption("-V"); else setFilter(verifyProps, s.substring(2), "verify.properties");
                        break;
                    case 'O':
                        if (outPath != presentWorkingDirectory) {
                            invalidSingletonOption("-O");
                        } else try {
                            outPath = (new File(s.substring(2))).getCanonicalFile();
                        } catch (IOException ioe) {
                            throw new DirResolutionException(ioe.getMessage(), s.substring(2));
                        }
                        break;
                    case 'S':
                        if (soutPath != presentWorkingDirectory) {
                            invalidSingletonOption("-S");
                        } else try {
                            soutPath = (new File(s.substring(2))).getCanonicalFile();
                        } catch (IOException ioe) {
                            throw new DirResolutionException(ioe.getMessage(), s.substring(2));
                        }
                        break;
                    case 'R':
                        if (sinPath != presentWorkingDirectory) {
                            invalidSingletonOption("-R");
                        } else try {
                            sinPath = (new File(s.substring(2))).getCanonicalFile();
                        } catch (IOException ioe) {
                            throw new DirResolutionException(ioe.getMessage(), s.substring(2));
                        }
                        break;
                    case 'N':
                        if (numWorkers != DEFAULT_NUM_WORKERS) {
                            invalidSingletonOption("-N");
                        } else try {
                            numWorkers = Integer.parseInt(s.substring(2));
                        } catch (NumberFormatException nfe) {
                            invalidNumericalOption(s);
                        }
                        break;
                    case 'o':
                        if (docWriter != DEFAULT_DOC_WRITER) invalidSingletonOption("-o"); else docWriter = s.substring(2);
                        break;
                    case 's':
                        if (sourceWriter != DEFAULT_SOURCE_WRITER) invalidSingletonOption("-s"); else sourceWriter = s.substring(2);
                        break;
                    case '?':
                        invalidOption();
                        break;
                    default:
                        invalidOption(s);
                        break;
                }
            } else {
                fileList.add(s);
            }
        }
    }

    /**
    * @param p The filter to set
    * @param file The file to read from or ""
    * @param dflt The resource to use (if file == "")
    */
    private static void setFilter(PropertiesFilter p, String file, String dflt) throws FileInputException {
        InputStream is = null;
        try {
            try {
                if (file.length() > 0) is = new FileInputStream(file); else is = Main.class.getResourceAsStream(file = dflt);
                if (is == null) throw new IOException("Program distribution is incomplete.\n");
                p.load(is);
            } finally {
                if (is != null) is.close();
            }
        } catch (IOException ioe) {
            throw new FileInputException(ioe.getMessage(), file);
        }
    }

    /** @param opt The invalid numerical option */
    private static void invalidNumericalOption(String opt) {
        System.err.println("SourceDoc: WARNING: Invalid numerical option \"" + opt + "\" disregarded.");
        invalidOption();
    }

    /** @param opt The invalid option */
    private static void invalidOption(String opt) {
        System.err.println("SourceDoc: WARNING: Invalid option \"" + opt + "\" disregarded.");
        invalidOption();
    }

    /** @param opt The singleton option */
    private static void invalidSingletonOption(String opt) {
        System.err.println("SourceDoc: WARNING: Option \"" + opt + "\" can not be used more than once. " + "Additional references disregarded.");
        invalidOption();
    }

    private static void invalidOption() {
        System.err.println("Syntax: \n" + "java -jar sourcedoc.jar [<options>] <files>\n" + "java -cp sourcedoc.jar sourcedoc.Main [<options>] <files>\n\n" + "Options:\n" + "-D<macro>[=<value>] Add <macro> to the list of preprocessor macros\n" + "-T<type>            Add <type> to the list of typedef types\n" + "-I<path>            Add <path> to the list of directories for #include \"...\"\n" + "-H<path>            Add <path> to the list of directories for #include <...>\n" + "-E[<file>]          Extract documentation as specified in <file>\n" + "-C[<file>]          Create documentation as specified in <file>\n" + "-V[<file>]          Verify documentation as specified in <file>\n" + "-N<num>             Use at most <num> threads\n" + "-O<path>            Output documentation to directory <path>\n" + "-S<path>            Output documented source to directory <path>\n" + "-o<class>           Use <class> as documentation writer\n" + "-s<class>           Use <class> as source writer\n" + "-?                  Display this message\n");
    }

    /** @param foe The file output exception */
    private static void fileInputException(FileInputException fie) {
        System.err.println("SourceDoc: ERROR: File \"" + fie.fileName + "\" could not be read. I/O Exception message: " + fie.getMessage());
    }

    private static void dirResolutionException(DirResolutionException dre) {
        System.err.println("SourceDoc: ERROR: Directory \"" + dre.dirName + "\" could not be resolved to an absolute path. " + "I/O Exception message: " + dre.getMessage());
    }

    /**
    * @param arg Command line args
    * @throws InterruptedException
    */
    public static void main(String[] arg) throws InterruptedException {
        int numfiles;
        int numworkers;
        int exit = 0;
        try {
            setOptions(arg);
            defineMap.put("SOURCEDOC", Boolean.TRUE);
            numfiles = fileList.size();
            if (numfiles > 0) {
                {
                    int n = numfiles;
                    if (n > numWorkers) n = numWorkers;
                    while ((n--) > 0) {
                        DocWorker dw = new DocWorker(coordinator);
                        workerList.add(dw);
                    }
                }
                numworkers = workerList.size();
                for (int i = 0; i < numworkers; i++) {
                    DocWorker dw = (DocWorker) workerList.get(i);
                    for (int j = i; j < numfiles; j += numworkers) {
                        dw.parseRequest((String) fileList.get(j));
                    }
                    dw.start();
                }
                for (int i = 0; i < numfiles; i++) {
                    ParseReply pr = (ParseReply) in.dequeue(PARSE_REPLY);
                    if (!pr.retVal) exit = -1; else {
                        if (pr.docList.isSelected(extractProps)) {
                            pr.docList.setIndex(docMap);
                            pr.docList.setContextMap(contextMap);
                            extractList.add(pr.docList);
                        }
                        if (pr.docList.isSelected(createProps)) {
                            createList.add(pr.docList);
                        }
                    }
                    fileList.clear();
                }
                if (extractProps.get()) {
                    numfiles = extractList.size();
                    {
                        DocWorker dw = (DocWorker) workerList.get(0);
                        dw.emitContentsRequest(extractList);
                        dw.emitOverviewRequest(extractList);
                        dw.emitIndexRequest(docMap.values());
                        dw.emitNumbersRequest(contextMap);
                        if (!((EmitContentsReply) in.dequeue(EMIT_CONTENTS_REPLY)).retVal) exit = -1;
                        if (!((EmitOverviewReply) in.dequeue(EMIT_OVERVIEW_REPLY)).retVal) exit = -1;
                        if (!((EmitIndexReply) in.dequeue(EMIT_INDEX_REPLY)).retVal) exit = -1;
                        if (!((EmitNumbersReply) in.dequeue(EMIT_NUMBERS_REPLY)).retVal) exit = -1;
                    }
                    for (int i = 0; i < numworkers; i++) {
                        DocWorker dw = (DocWorker) workerList.get(i);
                        for (int j = i; j < numfiles; j += numworkers) {
                            dw.emitDocRequest((DocList) extractList.get(j));
                        }
                    }
                    for (int i = 0; i < numfiles; i++) {
                        if (!((EmitDocReply) in.dequeue(EMIT_DOC_REPLY)).retVal) exit = -1;
                    }
                    extractList.clear();
                }
                if (createProps.get()) {
                    numfiles = createList.size();
                    for (int i = 0; i < numworkers; i++) {
                        DocWorker dw = (DocWorker) workerList.get(i);
                        for (int j = i; j < numfiles; j += numworkers) {
                            dw.emitSourceRequest((DocList) createList.get(j));
                        }
                    }
                    for (int i = 0; i < numfiles; i++) {
                        if (!((EmitSourceReply) in.dequeue(EMIT_SOURCE_REPLY)).retVal) exit = -1;
                    }
                    createList.clear();
                }
            }
        } catch (FileInputException fie) {
            fileInputException(fie);
            exit = -1;
        } catch (DirResolutionException dre) {
            dirResolutionException(dre);
            exit = -1;
        }
        System.exit(exit);
    }

    /** ParseReply select */
    private static final Class[] PARSE_REPLY = { ParseReply.class };

    /** EmitDocReply select */
    private static final Class[] EMIT_DOC_REPLY = { EmitDocReply.class };

    /** EmitOverviewReply select */
    private static final Class[] EMIT_OVERVIEW_REPLY = { EmitOverviewReply.class };

    /** EmitIndexReply select */
    private static final Class[] EMIT_INDEX_REPLY = { EmitIndexReply.class };

    /** EmitContentsReply select */
    private static final Class[] EMIT_CONTENTS_REPLY = { EmitContentsReply.class };

    /** EmitSourceReply select */
    private static final Class[] EMIT_SOURCE_REPLY = { EmitSourceReply.class };

    /** EmitNumbersReply select */
    private static final Class[] EMIT_NUMBERS_REPLY = { EmitNumbersReply.class };
}
