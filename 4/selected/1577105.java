package freedbimporter;

import freedbimporter.data.CD;
import freedbimporter.data.adaption.AnnotatingAdapter;
import freedbimporter.data.adaption.CDAdapter;
import freedbimporter.data.adaption.ValidatingAdapter;
import freedbimporter.data.adaption.ValidatingAnnotatingAdapter;
import freedbimporter.data.adaption.langanno.CharProfileComparator;
import freedbimporter.data.adaption.langanno.StringProfile;
import freedbimporter.data.adaption.validation.BlackListingValidator;
import freedbimporter.data.adaption.validation.CascadedValidator;
import freedbimporter.data.adaption.validation.DefaultDiscValidator;
import freedbimporter.data.adaption.validation.ValidationException;
import freedbimporter.data.adaption.validation.Validator;
import freedbimporter.data.adaption.validation.WhiteListingValidator;
import freedbimporter.extraction.FileCDLoader;
import freedbimporter.extraction.ProfilingExtractor;
import freedbimporter.extraction.SpoolingExtractor;
import freedbimporter.gui.MainGUI;
import freedbimporter.retrieval.CDEnumeration;
import freedbimporter.retrieval.NumericalDisciminator;
import freedbimporter.retrieval.TextualDisciminator;
import freedbimporter.spooling.FixedLengthDummy;
import freedbimporter.spooling.MySQLConnector;
import freedbimporter.spooling.MySQLStatementGenerator;
import freedbimporter.spooling.OptimalLengthCalculator;
import freedbimporter.spooling.VarcharFieldLengthAdvisor;
import freedbimporter.util.ConsoleTextLogger;
import freedbimporter.util.ExceptionReporter;
import freedbimporter.util.Logger;
import freedbimporter.util.RawTextLogger;
import freedbimporter.util.RawTextReader;
import freedbimporter.util.EncodingDeterminator;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Configures extractor / spooler from commandline-parameters and starts them.
 * <p>
 * 
 * @version      9.1 by 10.3.2010
 * @author       Copyright 2004 <a href="MAILTO:freedb2mysql@freedb2mysql.de">Christian Kruggel</a> - freedbimporter and all it&acute;s parts are free software and destributed under <a href="http://www.gnu.org/licenses/gpl-2.0.txt" target="_blank">GNU General Public License</a>
 */
public class Starter {

    public static void main(String[] args) {
        if (!containsSwitch(NO_GUI_COMMAND_LINE_SWITCH, args)) try {
            MainGUI.main(args);
        } catch (Exception e) {
            System.err.println(e);
        }
        if (containsSwitch(MERGE_COMMAND_LINE_SWITCH, args)) {
            String fileName = getStringValueFor(MERGE_SOURCE_DIR_PREFIX, args);
            if (fileName == null) System.err.println(MERGE_SOURCE_DIR_PREFIX + " not specified"); else {
                int targetFileSize = getIntValueFor(MERGE_TARGET_FILE_SIZE_PREFIX, args);
                if (targetFileSize < 1000) targetFileSize = 5000000;
                File freeDBDir = new File(fileName);
                File targetDir = new File(freeDBDir.getAbsolutePath() + MERGE_TARGET_FILE_EXTENSION);
                String targetDirName = getStringValueFor(MERGE_TARGET_DIR_PREFIX, args);
                if (targetDirName != null) targetDir = new File(targetDirName);
                if (freeDBDir.exists() && freeDBDir.canRead() && targetDir.mkdirs() || (targetDir.exists() && targetDir.isDirectory() && targetDir.canWrite())) {
                    File targetSubDir;
                    File[] sourceSubDirs = freeDBDir.listFiles();
                    if ((sourceSubDirs != null) && (sourceSubDirs.length > 0)) {
                        boolean quiet = containsSwitch(QUIET_COMMAND_LINE_SWITCH, args);
                        if (!quiet) Arrays.sort(sourceSubDirs);
                        long startMergeGMT = System.currentTimeMillis();
                        for (int i = 0; i < sourceSubDirs.length; i++) {
                            if (sourceSubDirs[i].exists() && sourceSubDirs[i].isDirectory() && sourceSubDirs[i].canRead()) try {
                                targetSubDir = new File(targetDir.getAbsolutePath() + File.separatorChar + sourceSubDirs[i].getName());
                                if (!quiet) System.out.println("Reading from " + sourceSubDirs[i].getAbsolutePath() + ", writing to " + targetSubDir.getAbsolutePath());
                                if (mergeFreeDBSubDir(sourceSubDirs[i], targetSubDir, targetFileSize, quiet) != 0) System.err.println("Failed to copy " + targetDir.getAbsolutePath() + " completely");
                            } catch (IOException ioException) {
                                System.err.println("Failed to merge " + freeDBDir.getAbsolutePath() + " into " + targetDir.getAbsolutePath());
                                System.err.println(ioException.getMessage());
                            }
                        }
                        RawTextReader reader;
                        RawTextLogger writer;
                        try {
                            reader = new RawTextReader(new File(freeDBDir.getAbsolutePath() + File.separatorChar + "COPYING"));
                            writer = new RawTextLogger(new File(targetDir.getAbsolutePath() + File.separatorChar + "COPYING"));
                            while (reader.hasMoreElements()) writer.println(reader.nextElement());
                            writer.close();
                            reader.close();
                            reader = new RawTextReader(new File(freeDBDir.getAbsolutePath() + File.separatorChar + "README"));
                            writer = new RawTextLogger(new File(targetDir.getAbsolutePath() + File.separatorChar + "README"));
                            while (reader.hasMoreElements()) writer.println(reader.nextElement());
                            writer.close();
                            reader.close();
                            if (!quiet) System.out.println();
                            System.out.println("Merge took " + StringProfile.getTextualDuration(System.currentTimeMillis() - startMergeGMT));
                            System.out.println();
                            System.out.println(mergeFilesBytesRead + "\t\t bytes from\t\t " + mergeFilesRead + "\t files in\t\t " + freeDBDir.getAbsolutePath());
                            System.out.println(mergeFilesBytesWritten + "\t\t bytes to\t\t " + mergeFilesWritten + "\t\t files in\t\t " + targetDir.getAbsolutePath());
                            System.out.println();
                            if (containsSwitch(MERGE_VERIFY_COMAND_LINE_SWITCH, args)) {
                                long sum = 0;
                                FileCDLoader fcl = new FileCDLoader(freeDBDir);
                                long startValidation = System.currentTimeMillis();
                                while (fcl.hasMoreCDs()) {
                                    sum++;
                                    fcl.nextCD();
                                }
                                System.out.println(sum + "\t\t discs parsed from " + freeDBDir.getAbsolutePath() + "\t\t " + StringProfile.getTextualDuration(System.currentTimeMillis() - startValidation));
                                sum = 0;
                                fcl = new FileCDLoader(targetDir);
                                startValidation = System.currentTimeMillis();
                                while (fcl.hasMoreCDs()) {
                                    sum++;
                                    fcl.nextCD();
                                }
                                System.out.println(sum + "\t\t discs parsed from " + targetDir.getAbsolutePath() + "\t " + StringProfile.getTextualDuration(System.currentTimeMillis() - startValidation));
                                System.out.println();
                            }
                        } catch (IOException ioException) {
                            System.err.println("Failed to copy GNU General Public License");
                            System.err.println(ioException.getMessage());
                        }
                    }
                } else System.err.println("Failed to create targetdirectory " + targetDir.getAbsolutePath());
            }
        }
        MySQLConnector.setUpLogging(args);
        MySQLStatementGenerator statementGenerator = getMySQLStatementGeneratorConfiguredFromCommandLine(args);
        CharProfileComparator comparator = null;
        if (containsSwitch(SKIP_LANG_ANNO_COMMAND_LINE_SWITCH, args)) comparator = CharProfileComparator.getEmptyComparator(); else comparator = CharProfileComparator.getDefaultEuroCentricComparator();
        Validator validator = getValidatorFromArgs(args);
        if (containsSwitch(DUMP_DB_SCHEMA_COMMAND_LINE_SWITCH, args)) {
            int numberOfTables = MySQLConnector.FOUR_TABLES;
            if (containsSwitch(CREATE_TWO_TABLES_SWITCH_COMMAND_LINE_SWITCH, args)) numberOfTables = MySQLConnector.TWO_TABLES;
            if (numberOfTables == MySQLConnector.FOUR_TABLES) {
                System.out.print(statementGenerator.getDataBaseInitializeStatementsForFourTabeles());
                System.out.println();
                System.out.print(statementGenerator.getDataBaseIndexPurgeStatementsForFourTabeles());
                System.out.print(statementGenerator.getDataBaseIndexInitializeStatementsForFourTabeles());
            }
            if (numberOfTables == MySQLConnector.TWO_TABLES) {
                System.out.print(statementGenerator.getDataBaseInitializeStatementsForTwoTabeles());
                System.out.println();
                System.out.print(statementGenerator.getDataBaseIndexPurgeStatementsForTwoTabeles());
                System.out.print(statementGenerator.getDataBaseIndexInitializeStatementsForTwoTabeles());
            }
        }
        File sourceToExtractFrom = null;
        if (containsSwitch(EXTRACTION_SOURCE_FILE_PREFIX, args)) sourceToExtractFrom = new File(getStringValueFor(EXTRACTION_SOURCE_FILE_PREFIX, args));
        if (sourceToExtractFrom != null) {
            if (!sourceToExtractFrom.exists()) System.err.println(EXTRACTION_SOURCE_FILE_PREFIX + ' ' + sourceToExtractFrom.getName() + " does not exist"); else if (!sourceToExtractFrom.canRead()) System.err.println(EXTRACTION_SOURCE_FILE_PREFIX + ' ' + sourceToExtractFrom.getName() + " is not readable"); else {
                Logger extractionLogger = new ExceptionReporter();
                try {
                    if (containsSwitch(EXTRACTION_REPORT_COMMAND_LINE_SWITCH, args)) extractionLogger = new ConsoleTextLogger();
                    if (containsSwitch(EXTRACTION_REPORT_FILE_PREFIX, args)) extractionLogger = new RawTextLogger(new File("Extracting from " + sourceToExtractFrom.getName() + ".txt"));
                } catch (IOException iE) {
                    System.err.println(iE.getMessage());
                }
                ExceptionReporter extractionReporter = new ExceptionReporter(extractionLogger);
                ProfilingExtractor profiler = null;
                String fileName = sourceToExtractFrom.getAbsoluteFile() + "." + validator.getClass().getSimpleName() + ProfilingExtractor.PROFILER_EXTENSION;
                try {
                    profiler = ProfilingExtractor.getProfiler(fileName);
                    System.out.println(fileName + " loaded");
                } catch (IOException iE) {
                } catch (ClassNotFoundException cE) {
                }
                if ((profiler == null) || containsSwitch(EVALUATE_FILES_COMMAND_LINE_SWITCH, args)) {
                    try {
                        if (containsSwitch(SPOOL_COMMAND_LINE_SWITCH, args)) profiler = new ProfilingExtractor(sourceToExtractFrom, containsSwitch(EXTRACT_EASY_COMMAND_LINE_SWITCH, args), containsSwitch(EXTRACT_NO_DISC_WITHOUT_RELEASE_YEAR_SET_COMMAND_LINE_SWITCH, args), containsSwitch(EXTRACT_DUPLICATE_DISC_DEFINITIONS, args), new ValidatingAdapter(validator, extractionReporter), extractionReporter); else profiler = new ProfilingExtractor(sourceToExtractFrom, containsSwitch(EXTRACT_EASY_COMMAND_LINE_SWITCH, args), containsSwitch(EXTRACT_NO_DISC_WITHOUT_RELEASE_YEAR_SET_COMMAND_LINE_SWITCH, args), containsSwitch(EXTRACT_DUPLICATE_DISC_DEFINITIONS, args), new ValidatingAnnotatingAdapter(validator, comparator, extractionReporter), extractionReporter);
                    } catch (IOException iE) {
                        System.err.println("Failed to profile " + sourceToExtractFrom.getAbsolutePath());
                        System.err.println(iE.getMessage());
                    }
                    fileName = sourceToExtractFrom.getAbsoluteFile() + "." + validator.getClass().getSimpleName() + ProfilingExtractor.PROFILER_EXTENSION;
                    try {
                        ProfilingExtractor.saveProfiler(profiler, fileName);
                        System.out.println(fileName + " saved");
                    } catch (IOException iE) {
                        System.err.println("Failed to save Profiler to " + fileName);
                        System.err.println(iE.getMessage());
                    }
                    if (!containsSwitch(SPOOL_COMMAND_LINE_SWITCH, args)) {
                        System.out.println();
                        System.out.println(profiler.getOperationSummary());
                        System.out.println(profiler.getExceptionSummary());
                    }
                }
                VarcharFieldLengthAdvisor advisor = null;
                fileName = sourceToExtractFrom.getAbsoluteFile() + "." + validator.getClass().getSimpleName() + OptimalLengthCalculator.OPTIMIZER_EXTENSION;
                try {
                    advisor = OptimalLengthCalculator.getOptimzer(fileName);
                    System.out.println(fileName + " loaded");
                    System.out.println();
                } catch (IOException iE) {
                } catch (ClassNotFoundException cE) {
                }
                if ((advisor == null) && (containsSwitch(SPOOL_COMMAND_LINE_SWITCH, args))) {
                    if (containsSwitch(SKIP_COLUMNS_WIDTHS_CALC_COMMAND_LINE_SWITCH, args) || containsSwitch(SPOOLER_CONTINUE_TABLES_COMMAND_LINE_SWITCH, args)) {
                        advisor = new FixedLengthDummy();
                        System.out.println();
                    } else {
                        advisor = new OptimalLengthCalculator(profiler.getStringProfiles(), 496);
                        advisor.getLengthCommendationForStringsInField("initialise");
                        fileName = sourceToExtractFrom.getAbsoluteFile() + "." + validator.getClass().getSimpleName() + OptimalLengthCalculator.OPTIMIZER_EXTENSION;
                        try {
                            OptimalLengthCalculator.saveOptimizer((OptimalLengthCalculator) advisor, fileName);
                            System.out.println(fileName + " saved");
                        } catch (IOException iE) {
                            System.err.println("Failed to save Optimizer to " + fileName);
                            System.err.println(iE.getMessage());
                        } finally {
                            System.out.println();
                        }
                        System.out.println(advisor.getOperationSummary());
                    }
                }
                System.out.flush();
                if (containsSwitch(SPOOL_COMMAND_LINE_SWITCH, args)) {
                    MySQLConnector.setUpLogging(args);
                    int numberOfTables = MySQLConnector.FOUR_TABLES;
                    if (containsSwitch(CREATE_TWO_TABLES_SWITCH_COMMAND_LINE_SWITCH, args)) numberOfTables = MySQLConnector.TWO_TABLES;
                    MySQLConnector mySQLCDSpooler = null;
                    statementGenerator = new MySQLStatementGenerator(getDBNameFromCommandLine(args));
                    Logger spoolingLogger = new ExceptionReporter();
                    try {
                        if (containsSwitch(SPOOLER_REPORT_COMMAND_LINE_SWITCH, args)) spoolingLogger = new ConsoleTextLogger();
                        if (containsSwitch(SPOOLER_REPORT_FILE_PREFIX, args)) spoolingLogger = new RawTextLogger(new File("Spooling to " + getDBNameFromCommandLine(args) + ".txt"));
                    } catch (IOException iE) {
                        System.err.println(iE.getMessage());
                    }
                    ExceptionReporter spoolingReporter = new ExceptionReporter(spoolingLogger);
                    if (containsSwitch(ANNOTATE_WITHOUT_VALIDATION, args)) mySQLCDSpooler = getMySQLConnectorConfiguredFromCommandLine(args, numberOfTables, new AnnotatingAdapter(), spoolingReporter); else mySQLCDSpooler = getMySQLConnectorConfiguredFromCommandLine(args, numberOfTables, new ValidatingAnnotatingAdapter(validator, comparator, spoolingReporter), spoolingReporter);
                    if (mySQLCDSpooler == null) {
                        if (!containsSwitch(SPOOLER_REPORT_COMMAND_LINE_SWITCH, args)) System.out.println("Failed to connect MySQL-CddbServer, use switch " + SPOOLER_REPORT_COMMAND_LINE_SWITCH + " to get an error-message");
                    } else try {
                        if (!containsSwitch(SPOOLER_CONTINUE_TABLES_COMMAND_LINE_SWITCH, args)) {
                            mySQLCDSpooler.initializeDataBase();
                            extractionReporter.flush();
                        }
                        if (containsSwitch(SPOOLER_LIMIT_ARTISTS_CACHE_PREFIX, args)) mySQLCDSpooler.setSpoolersArtistCacheSize(getIntValueFor(SPOOLER_LIMIT_ARTISTS_CACHE_PREFIX, args));
                        if (containsSwitch(SPOOLER_LIMIT_GENRES_CACHE_PREFIX, args)) mySQLCDSpooler.setSpoolersGenreCacheSize(getIntValueFor(SPOOLER_LIMIT_GENRES_CACHE_PREFIX, args));
                        SpoolingExtractor extractor = new SpoolingExtractor(sourceToExtractFrom, RawTextReader.UTF8, containsSwitch(EXTRACT_EASY_COMMAND_LINE_SWITCH, args), containsSwitch(EXTRACT_NO_DISC_WITHOUT_RELEASE_YEAR_SET_COMMAND_LINE_SWITCH, args), containsSwitch(EXTRACT_DUPLICATE_DISC_DEFINITIONS, args), mySQLCDSpooler);
                        extractor.startExtraction();
                        if (!containsSwitch(SKIP_INDEXING_DB_COMMAND_LINE_SWITCH, args)) {
                            extractionReporter.flush();
                            mySQLCDSpooler.createIndices();
                        }
                        System.out.println(extractor.getOperationSummary());
                        System.out.println(mySQLCDSpooler.getOperationSummary());
                        mySQLCDSpooler.close();
                        extractor.close();
                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                    }
                }
            }
        }
        long discsFound = 0;
        MySQLConnector connector = null;
        boolean showTracks = containsSwitch(DB_QUERY_SHOW_TRACKS_COMMAND_LINE_SWITCH, args);
        if (containsSwitch(DB_QUERY_DUMP_SERVER_COMPLETELY_COMMAND_LINE_SWITCH, args)) {
            connector = getMySQLConnectorConfiguredFromCommandLine(args, MySQLConnector.TABLES_COUNT_UNSET, new CDAdapter(), new ExceptionReporter());
            if (connector != null) {
                System.out.print("Dumping ");
                System.out.print(statementGenerator.getDataBaseName());
                System.out.print(" containing ");
                System.out.print(connector.getNumberOfDiscsInDataBase());
                System.out.println(" entries");
                System.out.println();
                CDEnumeration e = connector.getCompactDiscs();
                while (e.hasMoreCDs()) {
                    dumpDisc(e.nextCD(), showTracks);
                    discsFound++;
                }
                System.out.println();
                System.out.println(discsFound + " discs found");
            }
        }
        if (containsSwitch(DB_QUERY_ARTIST_NAME_PREFIX, args)) {
            connector = getMySQLConnectorConfiguredFromCommandLine(args, MySQLConnector.TABLES_COUNT_UNSET, new CDAdapter(), new ExceptionReporter());
            String artist = getStringValueFor(DB_QUERY_ARTIST_NAME_PREFIX, args);
            if ((connector != null) && (artist != null)) {
                System.out.print("Querying ");
                System.out.print(statementGenerator.getDataBaseName());
                System.out.print(" (");
                System.out.print(connector.getNumberOfDiscsInDataBase());
                System.out.print(" entries) for discs by artists named ");
                System.out.println(artist);
                System.out.println();
                CDEnumeration e = connector.getCompactDiscsByArtist(new TextualDisciminator(MySQLStatementGenerator.ARTISTS_NAME_COLUMN_NAME, artist));
                while (e.hasMoreCDs()) {
                    dumpDisc(e.nextCD(), showTracks);
                    discsFound++;
                }
                System.out.println();
                System.out.println(discsFound + " discs found");
            }
        }
        if (containsSwitch(DB_QUERY_DISC_TITLE_PREFIX, args)) {
            connector = getMySQLConnectorConfiguredFromCommandLine(args, MySQLConnector.TABLES_COUNT_UNSET, new CDAdapter(), new ExceptionReporter());
            String title = getStringValueFor(DB_QUERY_DISC_TITLE_PREFIX, args);
            if ((connector != null) && (title != null)) {
                System.out.print("Querying ");
                System.out.print(statementGenerator.getDataBaseName());
                System.out.print(" (");
                System.out.print(connector.getNumberOfDiscsInDataBase());
                System.out.print(" entries) for discs titeled ");
                System.out.println(title);
                System.out.println();
                CDEnumeration e = connector.getCompactDiscsByTitle(new TextualDisciminator(MySQLStatementGenerator.DISCS_TITLE_COLUMN_NAME, title));
                while (e.hasMoreCDs()) {
                    dumpDisc(e.nextCD(), showTracks);
                    discsFound++;
                }
                System.out.println();
                System.out.println(discsFound + " discs found");
            }
        }
        if (containsSwitch(DB_QUERY_FREEDB_DISCID_PREFIX, args)) {
            connector = getMySQLConnectorConfiguredFromCommandLine(args, MySQLConnector.TABLES_COUNT_UNSET, new CDAdapter(), new ExceptionReporter());
            String freeDBDiscId = getStringValueFor(DB_QUERY_FREEDB_DISCID_PREFIX, args);
            if ((connector != null) && (freeDBDiscId != null)) {
                System.out.print("Querying ");
                System.out.print(statementGenerator.getDataBaseName());
                System.out.print(" (");
                System.out.print(connector.getNumberOfDiscsInDataBase());
                System.out.print(" entries) for id ");
                System.out.println(freeDBDiscId);
                System.out.println();
                try {
                    Long discId = new Long(Long.parseLong(freeDBDiscId, 16));
                    CDEnumeration e = connector.getCompactDiscsByFreeDBId(new NumericalDisciminator(MySQLStatementGenerator.DISCS_FREEDB_DISCID_COLUMN_NAME, discId.longValue(), discId.longValue()));
                    while (e.hasMoreCDs()) {
                        dumpDisc(e.nextCD(), showTracks);
                        discsFound++;
                    }
                    System.out.println();
                    System.out.println(discsFound + " discs found");
                } catch (NumberFormatException e) {
                }
            }
        }
    }

    protected static void dumpDisc(CD disc, boolean showTracks) {
        if (showTracks) {
            System.out.println(disc.getAsString());
            System.out.println();
        } else System.out.println(disc);
    }

    static long mergeFilesRead = 0;

    static long mergeFilesBytesRead = 0;

    static long mergeFilesWritten = 0;

    static long mergeFilesBytesWritten = 0;

    protected static int mergeFreeDBSubDir(File freeDBSubDir, File targetSubDir, int targetFileSize, boolean quiet) throws IOException {
        int difference = 0;
        if (targetSubDir.mkdirs() || (targetSubDir.exists() && targetSubDir.isDirectory() && targetSubDir.canWrite())) {
            String line;
            int readFileIndex = 0;
            File[] freeDBSourceFiles = freeDBSubDir.listFiles();
            if (!quiet) Arrays.sort(freeDBSourceFiles);
            File targetFile = new File(targetSubDir.getAbsolutePath() + File.separatorChar + freeDBSourceFiles[readFileIndex].getName());
            RawTextLogger targetWriter = new RawTextLogger(targetFile);
            RawTextReader sourceReader = null;
            String enc;
            EncodingDeterminator det = new EncodingDeterminator(RawTextReader.UTF8, EncodingDeterminator.MODE_USE_ONLY_ICU);
            while (readFileIndex < freeDBSourceFiles.length) {
                try {
                    Long.parseLong(freeDBSourceFiles[readFileIndex].getName(), 16);
                    enc = det.getEncoding(freeDBSourceFiles[readFileIndex]);
                    sourceReader = new RawTextReader(freeDBSourceFiles[readFileIndex], enc);
                    mergeFilesRead++;
                    mergeFilesBytesRead = mergeFilesBytesRead + freeDBSourceFiles[readFileIndex].length();
                    while (sourceReader.hasMoreElements()) {
                        line = sourceReader.nextElement();
                        difference++;
                        targetWriter.println(line);
                        difference--;
                    }
                    if ((targetWriter.getNumberOfCharsWritten() > targetFileSize) || (readFileIndex == (freeDBSourceFiles.length - 1))) {
                        targetWriter.close();
                        mergeFilesWritten++;
                        mergeFilesBytesWritten = mergeFilesBytesWritten + targetFile.length();
                        if (!quiet) System.out.println(targetWriter.getLoggerName() + '\t' + targetWriter.getNumberOfCharsWritten() + " bytes");
                        if (readFileIndex != (freeDBSourceFiles.length - 1)) {
                            targetFile = new File(targetSubDir.getAbsolutePath() + File.separatorChar + freeDBSourceFiles[readFileIndex].getName());
                            targetWriter = new RawTextLogger(targetFile);
                        }
                    }
                    readFileIndex++;
                    sourceReader.close();
                } catch (NumberFormatException e) {
                    System.err.println(freeDBSourceFiles[readFileIndex] + " is an invalid name");
                }
            }
            sourceReader = null;
            targetWriter.close();
            targetWriter = null;
        } else System.err.println("Failed to create target " + targetSubDir.getName());
        return difference;
    }

    protected static Validator getValidatorFromArgs(String[] args) {
        if (containsSwitch(SKIP_DATA_VALIDATION_COMMAND_LINE_SWITCH, args)) return Validator.getEmptyValidator();
        Validator newValidator = new DefaultDiscValidator();
        CascadedValidator cNewValidator = new CascadedValidator();
        try {
            if (containsSwitch(GENRES_BLACKLIST_FILE_COMMAND_LINE_PREFIX, args)) cNewValidator.add(new BlackListingValidator(new File(getStringValueFor(GENRES_BLACKLIST_FILE_COMMAND_LINE_PREFIX, args))));
        } catch (IOException iE) {
            System.err.println("Failed to construct BlackListingnewValidator from file " + getStringValueFor(GENRES_BLACKLIST_FILE_COMMAND_LINE_PREFIX, args) + " - " + iE.getMessage());
        } catch (ValidationException vE) {
            System.err.println("Failed to construct BlackListingnewValidator - " + vE.getMessage());
        }
        try {
            if (containsSwitch(GENRES_WHITELIST_FILE_COMMAND_LINE_PREFIX, args)) cNewValidator.add(new WhiteListingValidator(new File(getStringValueFor(GENRES_WHITELIST_FILE_COMMAND_LINE_PREFIX, args)), false));
        } catch (IOException iE) {
            System.err.println("Failed to construct WhiteListingnewValidator from file " + getStringValueFor(GENRES_WHITELIST_FILE_COMMAND_LINE_PREFIX, args) + " - " + iE.getMessage());
        } catch (ValidationException vE) {
            System.err.println("Failed to construct WhiteListingnewValidator - " + vE.getMessage());
        }
        try {
            if (containsSwitch(GENRES_EXCLUSIVE_WHITELIST_FILE_COMMAND_LINE_PREFIX, args)) cNewValidator.add(new WhiteListingValidator(new File(getStringValueFor(GENRES_EXCLUSIVE_WHITELIST_FILE_COMMAND_LINE_PREFIX, args)), true));
        } catch (IOException iE) {
            System.err.println("Failed to construct WhiteListingnewValidator from file " + getStringValueFor(GENRES_EXCLUSIVE_WHITELIST_FILE_COMMAND_LINE_PREFIX, args) + " - " + iE.getMessage());
        } catch (ValidationException vE) {
            System.err.println("Failed to construct WhiteListingnewValidator - " + vE.getMessage());
        }
        if (cNewValidator.isEmpty()) cNewValidator.add(new DefaultDiscValidator());
        if (cNewValidator.validatorsCount() == 1) newValidator = cNewValidator.getValidatorAt(0); else newValidator = (Validator) cNewValidator;
        return newValidator;
    }

    public static boolean containsSwitch(String key, String[] args) {
        for (int argsTest = 0; argsTest < args.length; argsTest++) if (args[argsTest].equalsIgnoreCase(key)) return true;
        return false;
    }

    public static String getStringValueFor(String key, String[] args) {
        for (int argsTest = 0; argsTest < (args.length - 1); argsTest++) if (args[argsTest].equalsIgnoreCase(key)) return args[argsTest + 1].trim();
        return null;
    }

    public static int getIntValueFor(String key, String[] args) {
        Integer value = new Integer(-1);
        for (int argsTest = 0; argsTest < (args.length - 1); argsTest++) if (args[argsTest].equalsIgnoreCase(key)) try {
            value = new Integer(args[argsTest + 1]);
        } catch (NumberFormatException numberFormatException) {
        }
        return value.intValue();
    }

    public static double getDoubleValueFor(String key, String[] args) {
        double value = 100.0;
        for (int argsTest = 0; argsTest < (args.length - 1); argsTest++) if (args[argsTest].equalsIgnoreCase(key)) try {
            value = new Double(args[argsTest + 1]).doubleValue();
        } catch (NumberFormatException numberFormatException) {
        }
        return value;
    }

    public static void dumpArgs(String args[]) {
        System.out.print(args);
        System.out.print('\t');
        System.out.println(args.length);
        for (int argsTest = 0; argsTest < args.length; argsTest++) {
            System.out.print(argsTest);
            System.out.print('\t');
            System.out.println(args[argsTest]);
        }
    }

    public static String getDBNameFromCommandLine(String[] args) {
        return StringProfile.returnNewStringIfNotEmpty(MySQLStatementGenerator.DEFAULT_DATABASE, getStringValueFor(SPOOLER_DB_NAME_PREFIX, args));
    }

    public static MySQLStatementGenerator getMySQLStatementGeneratorConfiguredFromCommandLine(String[] args) {
        return new MySQLStatementGenerator(getDBNameFromCommandLine(args));
    }

    public static MySQLConnector getMySQLConnectorConfiguredFromCommandLine(String[] args) {
        return getMySQLConnectorConfiguredFromCommandLine(args, MySQLConnector.TABLES_COUNT_UNSET, new CDAdapter(), new ExceptionReporter());
    }

    public static MySQLConnector getMySQLConnectorConfiguredFromCommandLine(String[] args, int numberOfTables, CDAdapter adapter, ExceptionReporter spoolingReporter) {
        MySQLConnector connector = null;
        String dbHost = StringProfile.returnNewStringIfNotEmpty(MySQLConnector.DEFAULT_HOST, getStringValueFor(SPOOLER_DB_HOST_PREFIX, args));
        String dbUser = StringProfile.returnNewStringIfNotEmpty(MySQLConnector.DEFAULT_USER, getStringValueFor(SPOOLER_DB_USER_PREFIX, args));
        String pass = StringProfile.returnNewStringIfNotEmpty(MySQLConnector.DEFAULT_PASSWORD, getStringValueFor(SPOOLER_DB_PASSWORD_PREFIX, args));
        if (dbHost != null) try {
            if (containsSwitch(JDBC_NAME_PREFIX, args)) MySQLConnector.DEFAULT_DATABASE_DRIVER = getStringValueFor(JDBC_NAME_PREFIX, args);
            int port = MySQLConnector.DEFAULT_PORT;
            if ((containsSwitch(SPOOLER_DB_PORT, args)) && (getIntValueFor(SPOOLER_DB_PORT, args) > 0)) port = getIntValueFor(SPOOLER_DB_PORT, args);
            connector = new MySQLConnector(dbHost, port, dbUser, pass, numberOfTables, getMySQLStatementGeneratorConfiguredFromCommandLine(args), adapter);
        } catch (Exception e) {
            spoolingReporter.println("Failed to create MySQLConnector - " + e.getMessage());
        }
        return connector;
    }

    public static final String QUIET_COMMAND_LINE_SWITCH = "--quiet";

    public static final String NO_GUI_COMMAND_LINE_SWITCH = "--noGUI";

    public static final String DUMP_COMMAND_LINE_SWITCH = "--dump";

    public static final String SKIP_LANG_ANNO_COMMAND_LINE_SWITCH = "--skipLangAnno";

    public static final String SKIP_COLUMNS_WIDTHS_CALC_COMMAND_LINE_SWITCH = "--skipColumnsWidthsCalc";

    public static final String SKIP_INDEXING_DB_COMMAND_LINE_SWITCH = "--skipIndexing";

    public static final String SKIP_DATA_VALIDATION_COMMAND_LINE_SWITCH = "--noDataStringValidation";

    public static final String GENRES_BLACKLIST_FILE_COMMAND_LINE_PREFIX = "--genresBlackListFile";

    public static final String GENRES_WHITELIST_FILE_COMMAND_LINE_PREFIX = "--genresWhiteListFile";

    public static final String GENRES_EXCLUSIVE_WHITELIST_FILE_COMMAND_LINE_PREFIX = "--genresExclusiveWhiteListFile";

    public static final String ANNOTATE_WITHOUT_VALIDATION = "--annotateWithoutValidation";

    public static final String MERGE_COMMAND_LINE_SWITCH = "--merge";

    public static final String MERGE_SOURCE_DIR_PREFIX = "--freeDBSourceDir";

    public static final String MERGE_TARGET_FILE_SIZE_PREFIX = "--targetFileSize";

    public static final String MERGE_TARGET_DIR_PREFIX = "--freeDBTargetDir";

    public static final String MERGE_TARGET_FILE_EXTENSION = ".merged";

    public static final String MERGE_VERIFY_COMAND_LINE_SWITCH = "--verifyMerge";

    public static final String CREATE_TWO_TABLES_SWITCH_COMMAND_LINE_SWITCH = "--createLargeTables";

    public static final String DUMP_DB_SCHEMA_COMMAND_LINE_SWITCH = "--dumpDBschema";

    public static final String EVALUATE_FILES_COMMAND_LINE_SWITCH = "--eval";

    public static final String SPOOL_COMMAND_LINE_SWITCH = "--spool";

    public static final String JDBC_NAME_PREFIX = "--jdbcDriver";

    public static final String EXTRACTION_SOURCE_FILE_PREFIX = "--extractFrom";

    public static final String EXTRACTION_REPORT_COMMAND_LINE_SWITCH = "--reportExtracting";

    public static final String EXTRACTION_REPORT_FILE_PREFIX = "--reportExtractionToFile";

    public static final String EXTRACT_EASY_COMMAND_LINE_SWITCH = "--easyExtraction";

    public static final String EXTRACT_NO_DISC_WITHOUT_RELEASE_YEAR_SET_COMMAND_LINE_SWITCH = "--rejectDiscsWithoutReleaseYear";

    public static final String EXTRACT_DUPLICATE_DISC_DEFINITIONS = "--acceptDuplicateDiscs";

    public static final String SPOOLER_REPORT_COMMAND_LINE_SWITCH = "--reportSpooling";

    public static final String SPOOLER_REPORT_FILE_PREFIX = "--reportSpoolingToFile";

    public static final String SPOOLER_CONTINUE_TABLES_COMMAND_LINE_SWITCH = "--continueSpoolingToPreExistingTables";

    public static final String SPOOLER_LIMIT_ARTISTS_CACHE_PREFIX = "--limitArtistCacheTo";

    public static final String SPOOLER_LIMIT_GENRES_CACHE_PREFIX = "--limitGenreCacheTo";

    public static final String SPOOLER_DB_HOST_PREFIX = "--dbHost";

    public static final String SPOOLER_DB_USER_PREFIX = "--dbUser";

    public static final String SPOOLER_DB_PASSWORD_PREFIX = "--dbPassword";

    public static final String SPOOLER_DB_PORT = "--dbPort";

    public static final String SPOOLER_DB_NAME_PREFIX = "--dbName";

    public static final String DB_QUERY_SHOW_TRACKS_COMMAND_LINE_SWITCH = "--showTracks";

    public static final String DB_QUERY_DUMP_SERVER_COMPLETELY_COMMAND_LINE_SWITCH = "--serverDump";

    public static final String DB_QUERY_ARTIST_NAME_PREFIX = "--queryServerForArtist";

    public static final String DB_QUERY_DISC_TITLE_PREFIX = "--queryServerForDisc";

    public static final String DB_QUERY_FREEDB_DISCID_PREFIX = "--queryServerForFreeDBDiscId";

    private Starter() {
    }
}
