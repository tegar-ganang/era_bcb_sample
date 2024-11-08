package proguard;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.editor.*;
import proguard.classfile.instruction.*;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;
import proguard.io.*;
import proguard.obfuscate.*;
import proguard.optimize.*;
import proguard.optimize.evaluation.*;
import proguard.optimize.peephole.*;
import proguard.shrink.*;
import java.io.*;

/**
 * Tool for shrinking, optimizing, and obfuscating Java class files.
 *
 * @author Eric Lafortune
 */
public class ProGuard {

    public static final String VERSION = "ProGuard, version 3.2";

    private Configuration configuration;

    private ClassPool programClassPool = new ClassPool();

    private ClassPool libraryClassPool = new ClassPool();

    /**
     * Creates a new ProGuard object to process jars as specified by the given
     * configuration.
     */
    public ProGuard(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Performs all subsequent ProGuard operations.
     */
    public void execute() throws IOException {
        System.out.println(VERSION);
        readInput();
        if (configuration.shrink || configuration.optimize || configuration.obfuscate) {
            initialize();
        }
        if (configuration.printSeeds != null) {
            printSeeds();
        }
        if (configuration.shrink) {
            shrink();
        }
        if (configuration.optimize) {
            optimize();
            if (configuration.shrink) {
                configuration.printUsage = null;
                shrink();
            }
        }
        if (configuration.obfuscate) {
            obfuscate();
        }
        if (configuration.shrink || configuration.optimize || configuration.obfuscate) {
            sortConstantPools();
        }
        if (configuration.programJars.hasOutput()) {
            writeOutput();
        }
        if (configuration.dump != null) {
            dump();
        }
    }

    /**
     * Reads the input jars (or directories).
     */
    private void readInput() throws IOException {
        if (configuration.verbose) {
            System.out.println("Reading jars...");
        }
        if (configuration.programJars == null) {
            throw new IOException("The input is empty. You have to specify one or more '-injars' options.");
        }
        readInput("Reading program ", configuration.programJars, createDataEntryClassPoolFiller(false));
        if (programClassPool.size() == 0) {
            throw new IOException("The input doesn't contain any class files. Did you specify the proper '-injars' options?");
        }
        if (configuration.libraryJars != null) {
            readInput("Reading library ", configuration.libraryJars, createDataEntryClassPoolFiller(true));
        }
        if (configuration.defaultPackage != null) {
            configuration.allowAccessModification = true;
        }
    }

    /**
     * Creates a DataEntryReader that will decode class files and put them in
     * the proper class pool.
     */
    private DataEntryReader createDataEntryClassPoolFiller(boolean isLibrary) {
        ClassPool classPool = isLibrary ? libraryClassPool : programClassPool;
        return new ClassFileFilter(new ClassFileReader(isLibrary, configuration.skipNonPublicLibraryClasses, configuration.skipNonPublicLibraryClassMembers, new ClassPoolFiller(classPool, configuration.note)));
    }

    /**
     * Reads all input entries from the given class path.
     */
    private void readInput(String messagePrefix, ClassPath classPath, DataEntryReader reader) throws IOException {
        readInput(messagePrefix, classPath, 0, classPath.size(), reader);
    }

    /**
     * Reads all input entries from the given section of the given class path.
     */
    private void readInput(String messagePrefix, ClassPath classPath, int fromIndex, int toIndex, DataEntryReader reader) throws IOException {
        for (int index = fromIndex; index < toIndex; index++) {
            ClassPathEntry entry = classPath.get(index);
            if (!entry.isOutput()) {
                readInput(messagePrefix, entry, reader);
            }
        }
    }

    /**
     * Reads the given input class path entry.
     */
    private void readInput(String messagePrefix, ClassPathEntry classPathEntry, DataEntryReader dataEntryReader) throws IOException {
        try {
            DataEntryReader reader = DataEntryReaderFactory.createDataEntryReader(messagePrefix, classPathEntry, dataEntryReader);
            DirectoryPump directoryPump = new DirectoryPump(new File(classPathEntry.getName()));
            directoryPump.pumpDataEntries(reader);
        } catch (IOException ex) {
            throw new IOException("Can't read [" + classPathEntry + "] (" + ex.getMessage() + ")");
        }
    }

    /**
     * Initializes the cross-references between all class files.
     */
    private void initialize() throws IOException {
        ClassFileHierarchyInitializer classFileHierarchyInitializer = new ClassFileHierarchyInitializer(programClassPool, libraryClassPool, configuration.warn);
        programClassPool.classFilesAccept(classFileHierarchyInitializer);
        ClassFileHierarchyInitializer classFileHierarchyInitializer2 = new ClassFileHierarchyInitializer(programClassPool, libraryClassPool, false);
        libraryClassPool.classFilesAccept(classFileHierarchyInitializer2);
        ClassFileReferenceInitializer classFileReferenceInitializer = new ClassFileReferenceInitializer(programClassPool, libraryClassPool, configuration.warn, configuration.note);
        programClassPool.classFilesAccept(classFileReferenceInitializer);
        int noteCount = classFileReferenceInitializer.getNoteCount();
        if (noteCount > 0) {
            System.err.println("Note: there were " + noteCount + " class casts of dynamically created class instances.");
            System.err.println("      You might consider explicitly keeping the mentioned classes and/or");
            System.err.println("      their implementations (using '-keep').");
        }
        int hierarchyWarningCount = classFileHierarchyInitializer.getWarningCount();
        if (hierarchyWarningCount > 0) {
            System.err.println("Warning: there were " + hierarchyWarningCount + " unresolved references to superclasses or interfaces.");
            System.err.println("         You may need to specify additional library jars (using '-libraryjars'),");
            System.err.println("         or perhaps the '-dontskipnonpubliclibraryclasses' option.");
        }
        int referenceWarningCount = classFileReferenceInitializer.getWarningCount();
        if (referenceWarningCount > 0) {
            System.err.println("Warning: there were " + referenceWarningCount + " unresolved references to program class members.");
            System.err.println("         Your input class files appear to be inconsistent.");
            System.err.println("         You may need to recompile them and try again.");
        }
        if ((hierarchyWarningCount > 0 || referenceWarningCount > 0) && !configuration.ignoreWarnings) {
            System.err.println("         If you are sure the mentioned classes are not used anyway,");
            System.err.println("         you could try your luck using the '-ignorewarnings' option.");
            throw new IOException("Please correct the above warnings first.");
        }
        if (configuration.verbose) {
            System.out.println("Removing unused library classes...");
            System.out.println("    Original number of library classes: " + libraryClassPool.size());
        }
        ClassPool newLibraryClassPool = new ClassPool();
        programClassPool.classFilesAccept(new AllCpInfoVisitor(new ReferencedClassFileVisitor(new LibraryClassFileFilter(new ClassFileHierarchyTraveler(true, true, true, false, new LibraryClassFileFilter(new ClassPoolFiller(newLibraryClassPool, false)))))));
        libraryClassPool = newLibraryClassPool;
        if (configuration.verbose) {
            System.out.println("    Final number of library classes:    " + libraryClassPool.size());
        }
    }

    /**
     * Prints out classes and class members that are used as seeds in the
     * shrinking and obfuscation steps.
     */
    private void printSeeds() throws IOException {
        if (configuration.verbose) {
            System.out.println("Printing kept classes, fields, and methods...");
        }
        if (configuration.keep == null) {
            throw new IOException("You have to specify '-keep' options for the shrinking step.");
        }
        PrintStream ps = configuration.printSeeds.length() > 0 ? new PrintStream(new BufferedOutputStream(new FileOutputStream(configuration.printSeeds))) : System.out;
        SimpleClassFilePrinter printer = new SimpleClassFilePrinter(false, ps);
        ClassPoolVisitor classPoolvisitor = ClassSpecificationVisitorFactory.createClassPoolVisitor(configuration.keep, new ProgramClassFileFilter(printer), new ProgramMemberInfoFilter(printer));
        programClassPool.accept(classPoolvisitor);
        libraryClassPool.accept(classPoolvisitor);
        if (ps != System.out) {
            ps.close();
        }
    }

    /**
     * Performs the shrinking step.
     */
    private void shrink() throws IOException {
        if (configuration.verbose) {
            System.out.println("Shrinking...");
        }
        if (configuration.keep == null) {
            throw new IOException("You have to specify '-keep' options for the shrinking step.");
        }
        ClassFileCleaner classFileCleaner = new ClassFileCleaner();
        programClassPool.classFilesAccept(classFileCleaner);
        libraryClassPool.classFilesAccept(classFileCleaner);
        UsageMarker usageMarker = new UsageMarker();
        ClassPoolVisitor classPoolvisitor = ClassSpecificationVisitorFactory.createClassPoolVisitor(configuration.keep, usageMarker, usageMarker);
        programClassPool.accept(classPoolvisitor);
        libraryClassPool.accept(classPoolvisitor);
        programClassPool.classFilesAccept(new InterfaceUsageMarker());
        programClassPool.classFilesAccept(new InnerUsageMarker());
        if (configuration.printUsage != null) {
            if (configuration.verbose) {
                System.out.println("Printing usage" + (configuration.printUsage.length() > 0 ? " to [" + configuration.printUsage + "]" : "..."));
            }
            PrintStream ps = configuration.printUsage.length() > 0 ? new PrintStream(new BufferedOutputStream(new FileOutputStream(configuration.printUsage))) : System.out;
            programClassPool.classFilesAcceptAlphabetically(new UsagePrinter(true, ps));
            if (ps != System.out) {
                ps.close();
            }
        }
        if (configuration.verbose) {
            System.out.println("Removing unused program classes and class elements...");
            System.out.println("    Original number of program classes: " + programClassPool.size());
        }
        ClassPool newProgramClassPool = new ClassPool();
        programClassPool.classFilesAccept(new UsedClassFileFilter(new MultiClassFileVisitor(new ClassFileVisitor[] { new ClassFileShrinker(1024), new ClassPoolFiller(newProgramClassPool, false) })));
        programClassPool = newProgramClassPool;
        if (configuration.verbose) {
            System.out.println("    Final number of program classes:    " + programClassPool.size());
        }
        if (programClassPool.size() == 0) {
            throw new IOException("The output jar is empty. Did you specify the proper '-keep' options?");
        }
    }

    /**
     * Performs the optimization step.
     */
    private void optimize() throws IOException {
        if (configuration.verbose) {
            System.out.println("Optimizing...");
        }
        ClassFileCleaner classFileCleaner = new ClassFileCleaner();
        programClassPool.classFilesAccept(classFileCleaner);
        libraryClassPool.classFilesAccept(classFileCleaner);
        if (configuration.keep == null) {
            throw new IOException("You have to specify '-keep' options for the optimization step.");
        }
        KeepMarker keepMarker = new KeepMarker();
        ClassPoolVisitor classPoolvisitor = ClassSpecificationVisitorFactory.createClassPoolVisitor(configuration.keep, keepMarker, keepMarker);
        programClassPool.accept(classPoolvisitor);
        libraryClassPool.accept(classPoolvisitor);
        programClassPool.classFilesAccept(new ClassFileFinalizer());
        programClassPool.classFilesAccept(new AllMethodVisitor(new AllAttrInfoVisitor(new AllInstructionVisitor(new WriteOnlyFieldMarker()))));
        if (configuration.assumeNoSideEffects != null) {
            NoSideEffectMethodMarker noSideEffectMethodMarker = new NoSideEffectMethodMarker();
            ClassPoolVisitor noClassPoolvisitor = ClassSpecificationVisitorFactory.createClassPoolVisitor(configuration.assumeNoSideEffects, null, noSideEffectMethodMarker);
            programClassPool.accept(noClassPoolvisitor);
            libraryClassPool.accept(noClassPoolvisitor);
        }
        programClassPool.accept(new SideEffectMethodMarker());
        programClassPool.classFilesAccept(new SingleImplementationMarker(configuration.allowAccessModification));
        programClassPool.classFilesAccept(new AllMethodVisitor(new AllAttrInfoVisitor(new SingleImplementationInliner())));
        programClassPool.classFilesAccept(new AllMemberInfoVisitor(new SingleImplementationInliner()));
        programClassPool.classFilesAccept(new AllMethodVisitor(new PartialEvaluator()));
        BranchTargetFinder branchTargetFinder = new BranchTargetFinder(1024);
        CodeAttrInfoEditor codeAttrInfoEditor = new CodeAttrInfoEditor(1024);
        programClassPool.classFilesAccept(new AllMethodVisitor(new AllAttrInfoVisitor(new MultiAttrInfoVisitor(new AttrInfoVisitor[] { branchTargetFinder, new CodeAttrInfoEditorResetter(codeAttrInfoEditor), new AllInstructionVisitor(new MultiInstructionVisitor(new InstructionVisitor[] { new PushPopRemover(branchTargetFinder, codeAttrInfoEditor), new LoadStoreRemover(branchTargetFinder, codeAttrInfoEditor), new StoreLoadReplacer(branchTargetFinder, codeAttrInfoEditor), new GotoReturnReplacer(codeAttrInfoEditor), new NopRemover(codeAttrInfoEditor), new GetterSetterInliner(codeAttrInfoEditor, configuration.allowAccessModification) })), codeAttrInfoEditor }))));
    }

    /**
     * Performs the obfuscation step.
     */
    private void obfuscate() throws IOException {
        if (configuration.verbose) {
            System.out.println("Obfuscating...");
        }
        if (configuration.keep == null && configuration.keepNames == null) {
            throw new IOException("You have to specify '-keep' options for the obfuscation step.");
        }
        ClassFileCleaner classFileCleaner = new ClassFileCleaner();
        programClassPool.classFilesAccept(classFileCleaner);
        libraryClassPool.classFilesAccept(classFileCleaner);
        programClassPool.classFilesAccept(new BottomClassFileFilter(new MemberInfoLinker()));
        NameMarker nameMarker = new NameMarker();
        ClassPoolVisitor classPoolvisitor = new MultiClassPoolVisitor(new ClassPoolVisitor[] { ClassSpecificationVisitorFactory.createClassPoolVisitor(configuration.keep, nameMarker, nameMarker), ClassSpecificationVisitorFactory.createClassPoolVisitor(configuration.keepNames, nameMarker, nameMarker) });
        programClassPool.accept(classPoolvisitor);
        libraryClassPool.accept(classPoolvisitor);
        if (configuration.applyMapping != null) {
            if (configuration.verbose) {
                System.out.println("Applying mapping [" + configuration.applyMapping + "]");
            }
            MappingReader reader = new MappingReader(configuration.applyMapping);
            MappingProcessor keeper = new MultiMappingProcessor(new MappingProcessor[] { new MappingKeeper(programClassPool), new MappingKeeper(libraryClassPool) });
            reader.pump(keeper);
        }
        AttributeUsageMarker attributeUsageMarker = new AttributeUsageMarker();
        if (configuration.keepAttributes != null) {
            if (configuration.keepAttributes.size() != 0) {
                attributeUsageMarker.setKeepAttributes(configuration.keepAttributes);
            } else {
                attributeUsageMarker.setKeepAllAttributes();
            }
        }
        programClassPool.classFilesAccept(attributeUsageMarker);
        programClassPool.classFilesAccept(new AttributeShrinker());
        if (configuration.verbose) {
            System.out.println("Renaming program classes and class elements...");
        }
        programClassPool.classFilesAccept(new ClassFileObfuscator(programClassPool, configuration.defaultPackage, configuration.useMixedCaseClassNames));
        programClassPool.classFilesAccept(new BottomClassFileFilter(new MemberInfoObfuscator(configuration.overloadAggressively, configuration.obfuscationDictionary)));
        if (configuration.printMapping != null) {
            if (configuration.verbose) {
                System.out.println("Printing mapping" + (configuration.printMapping.length() > 0 ? " to [" + configuration.printMapping + "]" : "..."));
            }
            PrintStream ps = configuration.printMapping.length() > 0 ? new PrintStream(new BufferedOutputStream(new FileOutputStream(configuration.printMapping))) : System.out;
            programClassPool.classFilesAcceptAlphabetically(new MappingPrinter(ps));
            if (ps != System.out) {
                ps.close();
            }
        }
        programClassPool.classFilesAccept(new ClassFileRenamer(configuration.defaultPackage != null, configuration.newSourceFileAttribute));
        programClassPool.classFilesAccept(new NameAndTypeUsageMarker());
        programClassPool.classFilesAccept(new NameAndTypeShrinker(1024));
        programClassPool.classFilesAccept(new Utf8UsageMarker());
        programClassPool.classFilesAccept(new Utf8Shrinker(1024));
    }

    /**
     * Sorts the constant pools of all program class files.
     */
    private void sortConstantPools() {
        programClassPool.classFilesAccept(new ConstantPoolSorter(1024));
    }

    /**
     * Writes the output jars.
     */
    private void writeOutput() throws IOException {
        if (configuration.verbose) {
            System.out.println("Writing jars...");
        }
        ClassPath programJars = configuration.programJars;
        ClassPathEntry firstEntry = programJars.get(0);
        if (firstEntry.isOutput()) {
            throw new IOException("The output jar [" + firstEntry.getName() + "] must be specified after an input jar, or it will be empty.");
        }
        for (int index = 0; index < programJars.size() - 1; index++) {
            ClassPathEntry entry = programJars.get(index);
            if (entry.isOutput()) {
                if (entry.getFilter() == null && entry.getJarFilter() == null && entry.getWarFilter() == null && entry.getEarFilter() == null && entry.getZipFilter() == null && programJars.get(index + 1).isOutput()) {
                    throw new IOException("The output jar [" + entry.getName() + "] must have a filter, or all subsequent jars will be empty.");
                }
                for (int inIndex = 0; inIndex < programJars.size(); inIndex++) {
                    ClassPathEntry otherEntry = programJars.get(inIndex);
                    if (!otherEntry.isOutput() && entry.getName().equals(otherEntry.getName())) {
                        throw new IOException("The output jar [" + entry.getName() + "] must be different from all input jars.");
                    }
                }
            }
        }
        int firstInputIndex = 0;
        int lastInputIndex = 0;
        for (int index = 0; index < programJars.size(); index++) {
            ClassPathEntry entry = programJars.get(index);
            if (!entry.isOutput()) {
                lastInputIndex = index;
            } else {
                int nextIndex = index + 1;
                if (nextIndex == programJars.size() || !programJars.get(nextIndex).isOutput()) {
                    writeOutput(programJars, firstInputIndex, lastInputIndex + 1, nextIndex);
                    firstInputIndex = nextIndex;
                }
            }
        }
    }

    /**
     * Transfers the specified input jars to the specified output jars.
     */
    private void writeOutput(ClassPath classPath, int fromInputIndex, int fromOutputIndex, int toOutputIndex) throws IOException {
        try {
            DataEntryWriter writer = DataEntryWriterFactory.createDataEntryWriter(classPath, fromOutputIndex, toOutputIndex);
            DataEntryReader reader = new ClassFileFilter(new ClassFileRewriter(programClassPool, writer), new DataEntryCopier(writer));
            readInput("Copying resources from program ", classPath, fromInputIndex, fromOutputIndex, reader);
            writer.close();
        } catch (IOException ex) {
            throw new IOException("Can't write [" + classPath.get(fromOutputIndex).getName() + "] (" + ex.getMessage() + ")");
        }
    }

    /**
     * Prints out the contents of the program class files.
     */
    private void dump() throws IOException {
        if (configuration.verbose) {
            System.out.println("Printing classes" + (configuration.dump.length() > 0 ? " to [" + configuration.dump + "]" : "..."));
        }
        PrintStream ps = configuration.dump.length() > 0 ? new PrintStream(new BufferedOutputStream(new FileOutputStream(configuration.dump))) : System.out;
        programClassPool.classFilesAccept(new ClassFilePrinter(ps));
        if (configuration.dump.length() > 0) {
            ps.close();
        }
    }

    /**
     * The main method for ProGuard.
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java proguard.ProGuard [options ...]");
            System.exit(1);
        }
        Configuration configuration = new Configuration();
        try {
            ConfigurationParser parser = new ConfigurationParser(args);
            parser.parse(configuration);
            ProGuard proGuard = new ProGuard(configuration);
            proGuard.execute();
        } catch (Exception ex) {
            if (configuration.verbose) {
                ex.printStackTrace();
            } else {
                System.err.println("Error: " + ex.getMessage());
            }
            System.exit(1);
        }
        System.exit(0);
    }
}
