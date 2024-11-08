package org.dita.dost.module;

import static org.dita.dost.util.Constants.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import org.dita.dost.exception.DITAOTException;
import org.dita.dost.log.DITAOTLogger;
import org.dita.dost.log.MessageBean;
import org.dita.dost.log.MessageUtils;
import org.dita.dost.pipeline.AbstractPipelineInput;
import org.dita.dost.pipeline.AbstractPipelineOutput;
import org.dita.dost.reader.DitaValReader;
import org.dita.dost.reader.GenListModuleReader;
import org.dita.dost.reader.GrammarPoolManager;
import org.dita.dost.util.DelayConrefUtils;
import org.dita.dost.util.FileUtils;
import org.dita.dost.util.FilterUtils;
import org.dita.dost.util.OutputUtils;
import org.dita.dost.util.StringUtils;
import org.dita.dost.util.TimingUtils;
import org.dita.dost.util.XMLSerializer;
import org.dita.dost.writer.PropertiesWriter;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * This class extends AbstractPipelineModule, used to generate map and topic
 * list by parsing all the refered dita files.
 * 
 * @version 1.0 2004-11-25
 * 
 * @author Wu, Zhi Qiang
 */
final class GenMapAndTopicListModule implements AbstractPipelineModule {

    /** Set of all dita files */
    private final Set<String> ditaSet;

    /** Set of all topic files */
    private final Set<String> fullTopicSet;

    /** Set of all map files */
    private final Set<String> fullMapSet;

    /** Set of topic files containing href */
    private final Set<String> hrefTopicSet;

    /** Set of href topic files with anchor ID */
    private final Set<String> hrefWithIDSet;

    /** Set of chunk topic with anchor ID */
    private final Set<String> chunkTopicSet;

    /** Set of map files containing href */
    private final Set<String> hrefMapSet;

    /** Set of dita files containing conref */
    private final Set<String> conrefSet;

    /** Set of topic files containing coderef */
    private final Set<String> coderefSet;

    /** Set of all images */
    private final Set<String> imageSet;

    /** Set of all images used for flagging */
    private final Set<String> flagImageSet;

    /** Set of all html files */
    private final Set<String> htmlSet;

    /** Set of all the href targets */
    private final Set<String> hrefTargetSet;

    /** Set of all the conref targets */
    private Set<String> conrefTargetSet;

    /** Set of all the copy-to sources */
    private Set<String> copytoSourceSet;

    /** Set of all the non-conref targets */
    private final Set<String> nonConrefCopytoTargetSet;

    /** Set of sources of those copy-to that were ignored */
    private final Set<String> ignoredCopytoSourceSet;

    /** Set of subsidiary files */
    private final Set<String> subsidiarySet;

    /** Set of relative flag image files */
    private final Set<String> relFlagImagesSet;

    /** Map of all copy-to (target,source) */
    private Map<String, String> copytoMap;

    /** List of files waiting for parsing */
    private final List<String> waitList;

    /** List of parsed files */
    private final List<String> doneList;

    /** Set of outer dita files */
    private final Set<String> outDitaFilesSet;

    /** Set of sources of conacion */
    private final Set<String> conrefpushSet;

    /** Set of files containing keyref */
    private final Set<String> keyrefSet;

    /** Set of files with "@processing-role=resource-only" */
    private final Set<String> resourceOnlySet;

    /** Map of all key definitions */
    private final Map<String, String> keysDefMap;

    /** Basedir for processing */
    private String baseInputDir;

    /** Tempdir for processing */
    private String tempDir;

    /** ditadir for processing */
    private String ditaDir;

    private String inputFile;

    private String ditavalFile;

    private int uplevels = 0;

    private String prefix = "";

    private DITAOTLogger logger;

    private GenListModuleReader reader;

    private boolean xmlValidate = true;

    private String relativeValue;

    private String formatRelativeValue;

    private String rootFile;

    private XMLSerializer keydef;

    private XMLSerializer schemekeydef;

    /** Export file */
    private OutputStreamWriter export;

    private final Set<String> schemeSet;

    private final Map<String, Set<String>> schemeDictionary;

    private String transtype;

    private final Map<String, String> exKeyDefMap;

    private static final String moduleStartMsg = "GenMapAndTopicListModule.execute(): Starting...";

    private static final String moduleEndMsg = "GenMapAndTopicListModule.execute(): Execution time: ";

    /** use grammar pool cache */
    private boolean gramcache = true;

    private boolean setSystemid = true;

    /**
     * Create a new instance and do the initialization.
     * 
     * @throws ParserConfigurationException never throw such exception
     * @throws SAXException never throw such exception
     */
    public GenMapAndTopicListModule() throws SAXException, ParserConfigurationException {
        ditaSet = new HashSet<String>(INT_128);
        fullTopicSet = new HashSet<String>(INT_128);
        fullMapSet = new HashSet<String>(INT_128);
        hrefTopicSet = new HashSet<String>(INT_128);
        hrefWithIDSet = new HashSet<String>(INT_128);
        chunkTopicSet = new HashSet<String>(INT_128);
        schemeSet = new HashSet<String>(INT_128);
        hrefMapSet = new HashSet<String>(INT_128);
        conrefSet = new HashSet<String>(INT_128);
        imageSet = new HashSet<String>(INT_128);
        flagImageSet = new LinkedHashSet<String>(INT_128);
        htmlSet = new HashSet<String>(INT_128);
        hrefTargetSet = new HashSet<String>(INT_128);
        subsidiarySet = new HashSet<String>(INT_16);
        waitList = new LinkedList<String>();
        doneList = new LinkedList<String>();
        conrefTargetSet = new HashSet<String>(INT_128);
        nonConrefCopytoTargetSet = new HashSet<String>(INT_128);
        copytoMap = new HashMap<String, String>();
        copytoSourceSet = new HashSet<String>(INT_128);
        ignoredCopytoSourceSet = new HashSet<String>(INT_128);
        outDitaFilesSet = new HashSet<String>(INT_128);
        relFlagImagesSet = new LinkedHashSet<String>(INT_128);
        conrefpushSet = new HashSet<String>(INT_128);
        keysDefMap = new HashMap<String, String>();
        exKeyDefMap = new HashMap<String, String>();
        keyrefSet = new HashSet<String>(INT_128);
        coderefSet = new HashSet<String>(INT_128);
        this.schemeDictionary = new HashMap<String, Set<String>>();
        resourceOnlySet = new HashSet<String>(INT_128);
    }

    public void setLogger(final DITAOTLogger logger) {
        this.logger = logger;
    }

    public AbstractPipelineOutput execute(final AbstractPipelineInput input) throws DITAOTException {
        if (logger == null) {
            throw new IllegalStateException("Logger not set");
        }
        final Date startTime = TimingUtils.getNowTime();
        try {
            logger.logInfo(moduleStartMsg);
            parseInputParameters(input);
            GrammarPoolManager.setGramCache(gramcache);
            reader = new GenListModuleReader();
            reader.setLogger(logger);
            reader.initXMLReader(ditaDir, xmlValidate, rootFile, setSystemid);
            parseFilterFile();
            addToWaitList(inputFile);
            processWaitList();
            updateBaseDirectory();
            refactoringResult();
            outputResult();
            keydef.writeEndDocument();
            keydef.close();
            schemekeydef.writeEndDocument();
            schemekeydef.close();
            export.write("</stub>");
            export.close();
        } catch (final DITAOTException e) {
            throw e;
        } catch (final SAXException e) {
            throw new DITAOTException(e.getMessage(), e);
        } catch (final Exception e) {
            throw new DITAOTException(e.getMessage(), e);
        } finally {
            logger.logInfo(moduleEndMsg + TimingUtils.reportElapsedTime(startTime));
        }
        return null;
    }

    private void parseInputParameters(final AbstractPipelineInput input) {
        final String basedir = input.getAttribute(ANT_INVOKER_PARAM_BASEDIR);
        final String ditaInput = input.getAttribute(ANT_INVOKER_PARAM_INPUTMAP);
        tempDir = input.getAttribute(ANT_INVOKER_PARAM_TEMPDIR);
        ditaDir = input.getAttribute(ANT_INVOKER_EXT_PARAM_DITADIR);
        ditavalFile = input.getAttribute(ANT_INVOKER_PARAM_DITAVAL);
        xmlValidate = Boolean.valueOf(input.getAttribute(ANT_INVOKER_EXT_PARAM_VALIDATE));
        transtype = input.getAttribute(ANT_INVOKER_EXT_PARAM_TRANSTYPE);
        gramcache = "yes".equalsIgnoreCase(input.getAttribute(ANT_INVOKER_EXT_PARAM_GRAMCACHE));
        setSystemid = "yes".equalsIgnoreCase(input.getAttribute(ANT_INVOKER_EXT_PARAN_SETSYSTEMID));
        OutputUtils.setGeneratecopyouter(input.getAttribute(ANT_INVOKER_EXT_PARAM_GENERATECOPYOUTTER));
        OutputUtils.setOutterControl(input.getAttribute(ANT_INVOKER_EXT_PARAM_OUTTERCONTROL));
        OutputUtils.setOnlyTopicInMap(input.getAttribute(ANT_INVOKER_EXT_PARAM_ONLYTOPICINMAP));
        final File path = new File(input.getAttribute(ANT_INVOKER_EXT_PARAM_OUTPUTDIR));
        if (path.isAbsolute()) {
            OutputUtils.setOutputDir(input.getAttribute(ANT_INVOKER_EXT_PARAM_OUTPUTDIR));
        } else {
            final StringBuffer buff = new StringBuffer(input.getAttribute(ANT_INVOKER_PARAM_BASEDIR)).append(File.separator).append(path);
            OutputUtils.setOutputDir(buff.toString());
        }
        File inFile = new File(ditaInput);
        if (!inFile.isAbsolute()) {
            inFile = new File(basedir, ditaInput);
        }
        try {
            inFile = inFile.getCanonicalFile();
        } catch (IOException e1) {
            logger.logException(e1);
        }
        if (!new File(tempDir).isAbsolute()) {
            tempDir = new File(basedir, tempDir).getAbsolutePath();
        } else {
            tempDir = FileUtils.removeRedundantNames(tempDir);
        }
        if (!new File(ditaDir).isAbsolute()) {
            ditaDir = new File(basedir, ditaDir).getAbsolutePath();
        } else {
            ditaDir = FileUtils.removeRedundantNames(ditaDir);
        }
        if (ditavalFile != null && !new File(ditavalFile).isAbsolute()) {
            ditavalFile = new File(basedir, ditavalFile).getAbsolutePath();
        }
        baseInputDir = new File(inFile.getAbsolutePath()).getParent();
        baseInputDir = FileUtils.removeRedundantNames(baseInputDir);
        rootFile = inFile.getAbsolutePath();
        rootFile = FileUtils.removeRedundantNames(rootFile);
        inputFile = inFile.getName();
        try {
            keydef = XMLSerializer.newInstance(new FileOutputStream(new File(tempDir, "keydef.xml")));
            keydef.writeStartDocument();
            keydef.writeStartElement("stub");
            schemekeydef = XMLSerializer.newInstance(new FileOutputStream(new File(tempDir, "schemekeydef.xml")));
            schemekeydef.writeStartDocument();
            schemekeydef.writeStartElement("stub");
            export = new OutputStreamWriter(new FileOutputStream(new File(tempDir, FILE_NAME_EXPORT_XML)));
            export.write(XML_HEAD);
            export.write("<stub>");
        } catch (final FileNotFoundException e) {
            logger.logException(e);
        } catch (final IOException e) {
            logger.logException(e);
        } catch (final SAXException e) {
            logger.logException(e);
        }
        OutputUtils.setInputMapPathName(inFile.getAbsolutePath());
    }

    private void processWaitList() throws DITAOTException {
        reader.setTranstype(transtype);
        if (FileUtils.isDITAMapFile(inputFile)) {
            reader.setPrimaryDitamap(inputFile);
        }
        while (!waitList.isEmpty()) {
            processFile(waitList.remove(0));
        }
    }

    private void processFile(String currentFile) throws DITAOTException {
        File fileToParse;
        final File file = new File(currentFile);
        if (file.isAbsolute()) {
            fileToParse = file;
            currentFile = FileUtils.getRelativePathFromMap(rootFile, currentFile);
        } else {
            fileToParse = new File(baseInputDir, currentFile);
        }
        try {
            fileToParse = fileToParse.getCanonicalFile();
        } catch (IOException e1) {
            logger.logError(e1.toString());
        }
        logger.logInfo("Processing " + fileToParse.getAbsolutePath());
        String msg = null;
        final Properties params = new Properties();
        params.put("%1", currentFile);
        if (!fileToParse.exists()) {
            logger.logError(MessageUtils.getMessage("DOTX008E", params).toString());
            return;
        }
        try {
            if (FileUtils.isValidTarget(currentFile.toLowerCase())) {
                reader.setTranstype(transtype);
                reader.setCurrentDir(new File(currentFile).getParent());
                reader.parse(fileToParse);
            } else {
                final Properties prop = new Properties();
                prop.put("%1", fileToParse);
                logger.logWarn(MessageUtils.getMessage("DOTJ053W", params).toString());
            }
            if (reader.isValidInput()) {
                processParseResult(currentFile);
                categorizeCurrentFile(currentFile);
            } else if (!currentFile.equals(inputFile)) {
                logger.logWarn(MessageUtils.getMessage("DOTJ021W", params).toString());
            }
        } catch (final SAXParseException sax) {
            final Exception inner = sax.getException();
            if (inner != null && inner instanceof DITAOTException) {
                logger.logInfo(inner.getMessage());
                throw (DITAOTException) inner;
            }
            if (currentFile.equals(inputFile)) {
                final MessageBean msgBean = MessageUtils.getMessage("DOTJ012F", params);
                msg = MessageUtils.getMessage("DOTJ012F", params).toString();
                msg = new StringBuffer(msg).append(":").append(sax.getMessage()).toString();
                throw new DITAOTException(msgBean, sax, msg);
            }
            final StringBuffer buff = new StringBuffer();
            msg = MessageUtils.getMessage("DOTJ013E", params).toString();
            buff.append(msg).append(LINE_SEPARATOR).append(sax.getMessage());
            logger.logError(buff.toString());
        } catch (final Exception e) {
            if (currentFile.equals(inputFile)) {
                final MessageBean msgBean = MessageUtils.getMessage("DOTJ012F", params);
                msg = MessageUtils.getMessage("DOTJ012F", params).toString();
                msg = new StringBuffer(msg).append(":").append(e.getMessage()).toString();
                throw new DITAOTException(msgBean, e, msg);
            }
            final StringBuffer buff = new StringBuffer();
            msg = MessageUtils.getMessage("DOTJ013E", params).toString();
            buff.append(msg).append(LINE_SEPARATOR).append(e.getMessage());
            logger.logError(buff.toString());
        }
        if (!reader.isValidInput() && currentFile.equals(inputFile)) {
            if (xmlValidate == true) {
                msg = MessageUtils.getMessage("DOTJ022F", params).toString();
                throw new DITAOTException(msg);
            } else {
                msg = MessageUtils.getMessage("DOTJ034F", params).toString();
                throw new DITAOTException(msg);
            }
        }
        doneList.add(currentFile);
        reader.reset();
    }

    private void processParseResult(String currentFile) {
        final Map<String, String> cpMap = reader.getCopytoMap();
        final Map<String, String> kdMap = reader.getKeysDMap();
        final Map<String, String> exKdMap = reader.getExKeysDefMap();
        exKeyDefMap.putAll(exKdMap);
        for (final String file : reader.getNonCopytoResult()) {
            categorizeResultFile(file);
            updateUplevels(file);
        }
        for (final String key : cpMap.keySet()) {
            final String value = cpMap.get(key);
            if (copytoMap.containsKey(key)) {
                final Properties prop = new Properties();
                prop.setProperty("%1", value);
                prop.setProperty("%2", key);
                logger.logWarn(MessageUtils.getMessage("DOTX065W", prop).toString());
                ignoredCopytoSourceSet.add(value);
            } else {
                updateUplevels(key);
                copytoMap.put(key, value);
            }
        }
        schemeSet.addAll(reader.getSchemeRefSet());
        for (final String key : kdMap.keySet()) {
            final String value = kdMap.get(key);
            if (keysDefMap.containsKey(key)) {
            } else {
                updateUplevels(key);
                keysDefMap.put(key, value + "(" + currentFile + ")");
            }
            if (schemeSet.contains(currentFile)) {
                try {
                    schemekeydef.writeStartElement("keydef");
                    schemekeydef.writeAttribute("keys", key);
                    schemekeydef.writeAttribute("href", value);
                    schemekeydef.writeAttribute("source", currentFile);
                    schemekeydef.writeEndElement();
                } catch (final SAXException e) {
                    logger.logException(e);
                }
            }
        }
        hrefTargetSet.addAll(reader.getHrefTargets());
        hrefWithIDSet.addAll(reader.getHrefTopicSet());
        chunkTopicSet.addAll(reader.getChunkTopicSet());
        conrefTargetSet.addAll(reader.getConrefTargets());
        nonConrefCopytoTargetSet.addAll(reader.getNonConrefCopytoTargets());
        ignoredCopytoSourceSet.addAll(reader.getIgnoredCopytoSourceSet());
        subsidiarySet.addAll(reader.getSubsidiaryTargets());
        outDitaFilesSet.addAll(reader.getOutFilesSet());
        resourceOnlySet.addAll(reader.getResourceOnlySet());
        if (reader.getSchemeSet() != null && reader.getSchemeSet().size() > 0) {
            Set<String> children = null;
            children = this.schemeDictionary.get(currentFile);
            if (children == null) {
                children = new HashSet<String>();
            }
            children.addAll(reader.getSchemeSet());
            currentFile = currentFile.replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR);
            this.schemeDictionary.put(currentFile, children);
            final Set<String> hrfSet = reader.getHrefTargets();
            for (String f : hrfSet) {
                final String filename = f.replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR);
                children = this.schemeDictionary.get(filename);
                if (children == null) {
                    children = new HashSet<String>();
                }
                children.addAll(reader.getSchemeSet());
                this.schemeDictionary.put(filename, children);
            }
        }
    }

    private void categorizeCurrentFile(final String currentFile) {
        final String lcasefn = currentFile.toLowerCase();
        ditaSet.add(currentFile);
        if (FileUtils.isTopicFile(currentFile)) {
            hrefTargetSet.add(currentFile);
        }
        if (reader.hasConaction()) {
            conrefpushSet.add(currentFile);
        }
        if (reader.hasConRef()) {
            conrefSet.add(currentFile);
        }
        if (reader.hasKeyRef()) {
            keyrefSet.add(currentFile);
        }
        if (reader.hasCodeRef()) {
            coderefSet.add(currentFile);
        }
        if (FileUtils.isDITATopicFile(lcasefn)) {
            fullTopicSet.add(currentFile);
            if (reader.hasHref()) {
                hrefTopicSet.add(currentFile);
            }
        }
        if (FileUtils.isDITAMapFile(lcasefn)) {
            fullMapSet.add(currentFile);
            if (reader.hasHref()) {
                hrefMapSet.add(currentFile);
            }
        }
    }

    private void categorizeResultFile(String file) {
        String lcasefn = null;
        String format = null;
        if (file.contains(STICK)) {
            lcasefn = file.substring(0, file.indexOf(STICK)).toLowerCase();
            format = file.substring(file.indexOf(STICK) + 1);
            file = file.substring(0, file.indexOf(STICK));
        } else {
            lcasefn = file.toLowerCase();
        }
        if (subsidiarySet.contains(lcasefn)) {
            return;
        }
        if (FileUtils.isDITAFile(lcasefn) && (format == null || ATTR_FORMAT_VALUE_DITA.equalsIgnoreCase(format) || ATTR_FORMAT_VALUE_DITAMAP.equalsIgnoreCase(format))) {
            addToWaitList(file);
        } else if (!FileUtils.isSupportedImageFile(lcasefn)) {
            htmlSet.add(file);
        }
        if (FileUtils.isSupportedImageFile(lcasefn)) {
            imageSet.add(file);
        }
        if (FileUtils.isHTMLFile(lcasefn) || FileUtils.isResourceFile(lcasefn)) {
            htmlSet.add(file);
        }
    }

    /**
     * Update uplevels if needed.
     * 
     * @param file
     */
    private void updateUplevels(String file) {
        if (file.contains(STICK)) {
            file = file.substring(0, file.indexOf(STICK));
        }
        final int lastIndex = FileUtils.removeRedundantNames(file).replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR).lastIndexOf("../");
        if (lastIndex != -1) {
            final int newUplevels = lastIndex / 3 + 1;
            uplevels = newUplevels > uplevels ? newUplevels : uplevels;
        }
    }

    /**
     * Add the given file the wait list if it has not been parsed.
     * 
     * @param file
     */
    private void addToWaitList(final String file) {
        if (doneList.contains(file) || waitList.contains(file)) {
            return;
        }
        waitList.add(file);
    }

    private void updateBaseDirectory() {
        baseInputDir = new File(baseInputDir).getAbsolutePath();
        for (int i = uplevels; i > 0; i--) {
            final File file = new File(baseInputDir);
            baseInputDir = file.getParent();
            prefix = new StringBuffer(file.getName()).append(File.separator).append(prefix).toString();
        }
    }

    private String getUpdateLevels() {
        int current = uplevels;
        final StringBuffer buff = new StringBuffer();
        while (current > 0) {
            buff.append(".." + FILE_SEPARATOR);
            current--;
        }
        return buff.toString();
    }

    /**
     * Escape regular expression special characters.
     * 
     * @param value input
     * @return input with regular expression special characters escaped
     */
    private String formatRelativeValue(final String value) {
        final StringBuffer buff = new StringBuffer();
        if (value == null || value.length() == 0) {
            return "";
        }
        int index = 0;
        while (index < value.length()) {
            final char current = value.charAt(index);
            switch(current) {
                case '.':
                    buff.append("\\.");
                    break;
                case '\\':
                    buff.append("[\\\\|/]");
                    break;
                case '(':
                    buff.append("\\(");
                    break;
                case ')':
                    buff.append("\\)");
                    break;
                case '[':
                    buff.append("\\[");
                    break;
                case ']':
                    buff.append("\\]");
                    break;
                case '{':
                    buff.append("\\{");
                    break;
                case '}':
                    buff.append("\\}");
                    break;
                case '^':
                    buff.append("\\^");
                    break;
                case '+':
                    buff.append("\\+");
                    break;
                case '$':
                    buff.append("\\$");
                    break;
                default:
                    buff.append(current);
            }
            index++;
        }
        return buff.toString();
    }

    private void parseFilterFile() {
        if (ditavalFile != null) {
            final DitaValReader ditaValReader = new DitaValReader();
            ditaValReader.setLogger(logger);
            ditaValReader.initXMLReader(setSystemid);
            ditaValReader.read(ditavalFile);
            FilterUtils.setFilterMap(ditaValReader.getFilterMap());
            flagImageSet.addAll(ditaValReader.getImageList());
            relFlagImagesSet.addAll(ditaValReader.getRelFlagImageList());
        } else {
            FilterUtils.setFilterMap(null);
        }
    }

    private void refactoringResult() {
        handleConref();
        handleCopyto();
    }

    private void handleCopyto() {
        final Map<String, String> tempMap = new HashMap<String, String>();
        final Set<String> pureCopytoSources = new HashSet<String>(INT_128);
        final Set<String> totalCopytoSources = new HashSet<String>(INT_128);
        for (final String key : copytoMap.keySet()) {
            final String value = copytoMap.get(key);
            if (new File(baseInputDir + File.separator + prefix, value).exists()) {
                tempMap.put(key, value);
                if (conrefSet.contains(value)) {
                    conrefSet.add(key);
                }
            }
        }
        copytoMap = tempMap;
        ditaSet.addAll(copytoMap.keySet());
        fullTopicSet.addAll(copytoMap.keySet());
        totalCopytoSources.addAll(copytoMap.values());
        totalCopytoSources.addAll(ignoredCopytoSourceSet);
        for (final String src : totalCopytoSources) {
            if (!nonConrefCopytoTargetSet.contains(src) && !copytoMap.keySet().contains(src)) {
                pureCopytoSources.add(src);
            }
        }
        copytoSourceSet = pureCopytoSources;
        ditaSet.removeAll(pureCopytoSources);
        fullTopicSet.removeAll(pureCopytoSources);
    }

    private void handleConref() {
        final Set<String> pureConrefTargets = new HashSet<String>(INT_128);
        for (final String target : conrefTargetSet) {
            if (!nonConrefCopytoTargetSet.contains(target)) {
                pureConrefTargets.add(target);
            }
        }
        conrefTargetSet = pureConrefTargets;
        ditaSet.removeAll(pureConrefTargets);
        fullTopicSet.removeAll(pureConrefTargets);
    }

    private void outputResult() throws DITAOTException {
        final Properties prop = new Properties();
        final PropertiesWriter writer = new PropertiesWriter();
        final Content content = new ContentImpl();
        final File outputFile = new File(tempDir, FILE_NAME_DITA_LIST);
        final File xmlDitalist = new File(tempDir, FILE_NAME_DITA_LIST_XML);
        final File dir = new File(tempDir);
        final Set<String> copytoSet = new HashSet<String>(INT_128);
        final Set<String> keysDefSet = new HashSet<String>(INT_128);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        prop.put("user.input.dir", baseInputDir);
        prop.put("user.input.file", prefix + inputFile);
        prop.put("user.input.file.listfile", "usr.input.file.list");
        final File inputfile = new File(tempDir, "usr.input.file.list");
        Writer bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(inputfile)));
            bufferedWriter.write(prefix + inputFile);
            bufferedWriter.flush();
        } catch (final FileNotFoundException e) {
            logger.logException(e);
        } catch (final IOException e) {
            logger.logException(e);
        } finally {
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (final IOException e) {
                    logger.logException(e);
                }
            }
        }
        relativeValue = prefix;
        formatRelativeValue = formatRelativeValue(relativeValue);
        prop.put("tempdirToinputmapdir.relative.value", formatRelativeValue);
        prop.put("uplevels", getUpdateLevels());
        addSetToProperties(prop, OUT_DITA_FILES_LIST, outDitaFilesSet);
        addSetToProperties(prop, FULL_DITAMAP_TOPIC_LIST, ditaSet);
        addSetToProperties(prop, FULL_DITA_TOPIC_LIST, fullTopicSet);
        addSetToProperties(prop, FULL_DITAMAP_LIST, fullMapSet);
        addSetToProperties(prop, HREF_DITA_TOPIC_LIST, hrefTopicSet);
        addSetToProperties(prop, CONREF_LIST, conrefSet);
        addSetToProperties(prop, IMAGE_LIST, imageSet);
        addSetToProperties(prop, FLAG_IMAGE_LIST, flagImageSet);
        addSetToProperties(prop, HTML_LIST, htmlSet);
        addSetToProperties(prop, HREF_TARGET_LIST, hrefTargetSet);
        addSetToProperties(prop, HREF_TOPIC_LIST, hrefWithIDSet);
        addSetToProperties(prop, CHUNK_TOPIC_LIST, chunkTopicSet);
        addSetToProperties(prop, SUBJEC_SCHEME_LIST, schemeSet);
        addSetToProperties(prop, CONREF_TARGET_LIST, conrefTargetSet);
        addSetToProperties(prop, COPYTO_SOURCE_LIST, copytoSourceSet);
        addSetToProperties(prop, SUBSIDIARY_TARGET_LIST, subsidiarySet);
        addSetToProperties(prop, CONREF_PUSH_LIST, conrefpushSet);
        addSetToProperties(prop, KEYREF_LIST, keyrefSet);
        addSetToProperties(prop, CODEREF_LIST, coderefSet);
        addSetToProperties(prop, RESOURCE_ONLY_LIST, resourceOnlySet);
        addFlagImagesSetToProperties(prop, REL_FLAGIMAGE_LIST, relFlagImagesSet);
        for (final Map.Entry<String, String> entry : copytoMap.entrySet()) {
            copytoSet.add(entry.toString());
        }
        for (final Map.Entry<String, String> entry : keysDefMap.entrySet()) {
            keysDefSet.add(entry.toString());
        }
        addSetToProperties(prop, COPYTO_TARGET_TO_SOURCE_MAP_LIST, copytoSet);
        addSetToProperties(prop, KEY_LIST, keysDefSet);
        content.setValue(prop);
        writer.setContent(content);
        writer.write(outputFile.getAbsolutePath());
        writer.writeToXML(xmlDitalist.getAbsolutePath());
        writeMapToXML(reader.getRelationshipGrap(), FILE_NAME_SUBJECT_RELATION);
        writeMapToXML(this.schemeDictionary, FILE_NAME_SUBJECT_DICTIONARY);
        if (INDEX_TYPE_ECLIPSEHELP.equals(transtype)) {
            final File pluginIdFile = new File(tempDir, FILE_NAME_PLUGIN_XML);
            DelayConrefUtils.getInstance().writeMapToXML(reader.getPluginMap(), pluginIdFile);
            final StringBuffer result = reader.getResult();
            try {
                export.write(result.toString());
            } catch (final IOException e) {
                logger.logException(e);
            }
        }
    }

    private void writeMapToXML(final Map<String, Set<String>> m, final String filename) {
        if (m == null) {
            return;
        }
        final Properties prop = new Properties();
        for (final Map.Entry<String, Set<String>> entry : m.entrySet()) {
            final String key = entry.getKey();
            final String value = StringUtils.assembleString(entry.getValue(), COMMA);
            prop.setProperty(key, value);
        }
        final File outputFile = new File(tempDir, filename);
        OutputStream os = null;
        try {
            os = new FileOutputStream(outputFile);
            prop.storeToXML(os, null);
            os.close();
        } catch (final IOException e) {
            this.logger.logException(e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (final Exception e) {
                    logger.logException(e);
                }
            }
        }
    }

    private void addSetToProperties(final Properties prop, final String key, final Set<String> set) {
        String value = null;
        final Set<String> newSet = new LinkedHashSet<String>(INT_128);
        for (final String file : set) {
            if (new File(file).isAbsolute()) {
                newSet.add(FileUtils.removeRedundantNames(file));
            } else {
                final int index = file.indexOf(EQUAL);
                if (index != -1) {
                    final String to = file.substring(0, index);
                    final String source = file.substring(index + 1);
                    if (KEY_LIST.equals(key)) {
                        final StringBuilder repStr = new StringBuilder();
                        repStr.append(FileUtils.removeRedundantNames(prefix + to).replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR));
                        repStr.append(EQUAL);
                        if (source.substring(0, 1).equals(LEFT_BRACKET)) {
                            repStr.append(FileUtils.removeRedundantNames(prefix).replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR));
                            repStr.append(LEFT_BRACKET);
                            repStr.append(FileUtils.removeRedundantNames(source.substring(1, source.length() - 1)).replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR));
                            repStr.append(RIGHT_BRACKET);
                        } else {
                            repStr.append(FileUtils.removeRedundantNames(prefix + source).replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR));
                        }
                        StringBuffer result = new StringBuffer(repStr);
                        if (!"".equals(prefix)) {
                            final String prefix1 = prefix.replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR);
                            if (repStr.indexOf(prefix1) != -1) {
                                result = new StringBuffer();
                                result.append(repStr.substring(prefix1.length()));
                                result.insert(result.lastIndexOf(LEFT_BRACKET) + 1, prefix1);
                                if (exKeyDefMap.containsKey(to)) {
                                    final int pos = result.indexOf(prefix1);
                                    result.delete(pos, pos + prefix1.length());
                                }
                                newSet.add(result.toString());
                            }
                        } else {
                            newSet.add(result.toString());
                        }
                        writeKeyDef(to, result);
                    } else {
                        newSet.add(FileUtils.removeRedundantNames(new StringBuffer(prefix).append(to).toString()).replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR) + EQUAL + FileUtils.removeRedundantNames(new StringBuffer(prefix).append(source).toString()).replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR));
                    }
                } else {
                    newSet.add(FileUtils.removeRedundantNames(new StringBuffer(prefix).append(file).toString()).replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR));
                }
            }
        }
        final String fileKey = key.substring(0, key.lastIndexOf("list")) + "file";
        prop.put(fileKey, key.substring(0, key.lastIndexOf("list")) + ".list");
        final File list = new File(tempDir, prop.getProperty(fileKey));
        Writer bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(list)));
            final Iterator<String> it = newSet.iterator();
            while (it.hasNext()) {
                bufferedWriter.write(it.next());
                if (it.hasNext()) {
                    bufferedWriter.write("\n");
                }
            }
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (final FileNotFoundException e) {
            logger.logException(e);
        } catch (final IOException e) {
            logger.logException(e);
        } finally {
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (final IOException e) {
                    logger.logException(e);
                }
            }
        }
        value = StringUtils.assembleString(newSet, COMMA);
        prop.put(key, value);
        set.clear();
        newSet.clear();
    }

    /**
     * Write keydef into keydef.xml.
     * 
     * @param keyName key name.
     * @param result keydef.
     */
    private void writeKeyDef(final String keyName, final StringBuffer result) {
        try {
            final int equalIndex = result.indexOf(EQUAL);
            final int leftBracketIndex = result.lastIndexOf(LEFT_BRACKET);
            final int rightBracketIndex = result.lastIndexOf(RIGHT_BRACKET);
            final String href = result.substring(equalIndex + 1, leftBracketIndex);
            final String sourcefile = result.substring(leftBracketIndex + 1, rightBracketIndex);
            keydef.writeStartElement("keydef");
            keydef.writeAttribute("keys", keyName);
            keydef.writeAttribute("href", href);
            keydef.writeAttribute("source", sourcefile);
            keydef.writeEndElement();
        } catch (final SAXException e) {
            logger.logException(e);
        }
    }

    /**
     * add FlagImangesSet to Properties, which needn't to change the dir level,
     * just ouput to the ouput dir.
     * 
     * @param prop
     * @param key
     * @param set
     */
    private void addFlagImagesSetToProperties(final Properties prop, final String key, final Set<String> set) {
        String value = null;
        final Set<String> newSet = new LinkedHashSet<String>(INT_128);
        for (final String file : set) {
            if (new File(file).isAbsolute()) {
                newSet.add(FileUtils.removeRedundantNames(file));
            } else {
                newSet.add(FileUtils.removeRedundantNames(new StringBuffer().append(file).toString()).replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR));
            }
        }
        final String fileKey = key.substring(0, key.lastIndexOf("list")) + "file";
        prop.put(fileKey, key.substring(0, key.lastIndexOf("list")) + ".list");
        final File list = new File(tempDir, prop.getProperty(fileKey));
        Writer bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(list)));
            final Iterator<String> it = newSet.iterator();
            while (it.hasNext()) {
                bufferedWriter.write(it.next());
                if (it.hasNext()) {
                    bufferedWriter.write("\n");
                }
            }
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (final FileNotFoundException e) {
            logger.logException(e);
        } catch (final IOException e) {
            logger.logException(e);
        } finally {
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (final IOException e) {
                    logger.logException(e);
                }
            }
        }
        value = StringUtils.assembleString(newSet, COMMA);
        prop.put(key, value);
        set.clear();
        newSet.clear();
    }
}
