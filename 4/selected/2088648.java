package jlibtracker.main;

import jlibtracker.util.LibrarySet;
import jlibtracker.util.ClazzUtil;
import jlibtracker.io.ConstantPoolReader;
import jlibtracker.io.ReportGenerator;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.io.*;

public class Main {

    private static LibrarySet<String> totalEntries = new LibrarySet<String>();

    private static LibrarySet<String> libEntries = new LibrarySet<String>();

    private static LibrarySet<String> transitiveEntries = new LibrarySet<String>();

    private String[] sourceList = new String[0];

    private String[] includesList = new String[0];

    private String[] libraryList = new String[0];

    private ConstantPoolReader poolReader = new ConstantPoolReader();

    private ReportGenerator writer = null;

    public static void main(String arg[]) {
        Main main = new Main();
        main.validateArgs(arg);
        main.process();
    }

    private void process() {
        writer.println("Parsing source classes...");
        for (String sourceClazz : sourceList) {
            parseSource(sourceClazz);
        }
        for (String includeClazz : includesList) {
            if (new ClazzUtil.ClassFileFilter().accept(includeClazz)) {
                String clazzName = includeClazz.substring(0, includeClazz.lastIndexOf(".")).replaceAll("\\.", "/");
                totalEntries.add(clazzName);
            } else {
                writer.println("Warning: Ignoring invalid entry " + includeClazz);
            }
        }
        writer.println("\nFinding code dependencies...");
        for (String library : libraryList) {
            libEntries.addAll(findCodeDependencies(library, totalEntries));
        }
        writer.println("\nCode Dependencies:\n");
        for (LibrarySet<String>.Entry<String> name : libEntries.entrySet()) {
            writer.println(name.toString());
        }
        writer.println("\nFinding transitive dependencies...(this may take few minutes)");
        for (String library : libraryList) {
            findTransitiveDependencies(library, libEntries);
        }
        Set<LibrarySet<String>.Entry<String>> effectiveTransitiveEntries = new HashSet<LibrarySet<String>.Entry<String>>(transitiveEntries.entrySet());
        effectiveTransitiveEntries.removeAll(libEntries.entrySet());
        writer.println("\nTransitive Dependencies:\n");
        for (LibrarySet<String>.Entry<String> name : effectiveTransitiveEntries) {
            writer.println(name.toString());
        }
        writer.println("\nFinding duplicate libraries...");
        Set<LibrarySet<String>.Entry<String>> duplicateLibraries = new HashSet<LibrarySet<String>.Entry<String>>();
        for (LibrarySet<String>.Entry<String> entry : totalEntries.entrySet()) {
            Set<LibrarySet<String>.Entry<String>> containingLib = entry.getSelfReferences();
            if (containingLib.size() > 1) {
                duplicateLibraries.addAll(containingLib);
            }
        }
        Set<String> actualSet = new HashSet<String>();
        for (LibrarySet<String>.Entry<String> dupLibrary : duplicateLibraries) {
            Set<LibrarySet<String>.Entry<String>> usedLibClasses = dupLibrary.getClassReferences();
            for (LibrarySet<String>.Entry<String> dupLibrary1 : duplicateLibraries) {
                if (dupLibrary.equals(dupLibrary1)) {
                    continue;
                }
                if (dupLibrary1.getClassReferences().containsAll(usedLibClasses)) {
                    actualSet.add(dupLibrary.toString() + " is duplicate of " + dupLibrary1.toString());
                }
            }
        }
        writer.println("\nDuplicate Libraries:\n");
        for (String name : actualSet) {
            writer.println(name);
        }
        writer.println("\nCode dependency tracking completed.");
    }

    private void parseSource(String path) {
        if ((path != null) && (new File(path).exists())) {
            File[] classFiles = ClazzUtil.getClassFiles(path);
            for (File fileToParse : classFiles) {
                try {
                    if (new ClazzUtil.JarFileFilter().accept(fileToParse)) {
                        JarFile jarFile = new JarFile(fileToParse);
                        Enumeration<JarEntry> jarEntries = jarFile.entries();
                        while (jarEntries.hasMoreElements()) {
                            JarEntry jarEntry = jarEntries.nextElement();
                            if (new ClazzUtil.ClassFileFilter().accept(jarEntry.getName())) {
                                InputStream is = jarFile.getInputStream(jarEntry);
                                totalEntries.addAll(poolReader.getClassesInUse(is));
                            }
                        }
                    } else {
                        FileInputStream fis = new FileInputStream(fileToParse);
                        totalEntries.addAll(poolReader.getClassesInUse(fis));
                    }
                } catch (IOException e) {
                    writer.println("Error: Unable to read " + fileToParse + ", skipping the entry.");
                }
            }
        } else {
            writer.println("Skipping invalid entry: " + path);
        }
    }

    private LibrarySet<String> findCodeDependencies(String libPath, LibrarySet<String> srcClazzes) {
        LibrarySet<String> libEntries = new LibrarySet<String>();
        if ((libPath != null) && (new File(libPath).exists())) {
            File[] classFiles = ClazzUtil.getClassFiles(libPath);
            for (Iterator<LibrarySet<String>.Entry<String>> srcIterator = srcClazzes.iterator(); srcIterator.hasNext(); ) {
                LibrarySet<String>.Entry<String> srcEntry = srcIterator.next();
                for (File fileToParse : classFiles) {
                    try {
                        LibrarySet<String>.Entry<String> libEntry = null;
                        if (new ClazzUtil.JarFileFilter().accept(fileToParse)) {
                            JarFile jarFile = new JarFile(fileToParse);
                            if (jarFile.getEntry(srcEntry.toString()) != null) {
                                libEntry = libEntries.add(fileToParse.getAbsolutePath());
                            }
                        } else if (fileToParse.equals(srcEntry.toString())) {
                            libEntry = libEntries.add(fileToParse.getAbsolutePath());
                        }
                        if (libEntry != null) {
                            libEntry.addClassReference(srcEntry);
                        }
                    } catch (IOException e) {
                        writer.println("Error: Unable to read " + fileToParse + ", skipping the entry.");
                    }
                }
            }
        }
        return libEntries;
    }

    private void findTransitiveDependencies(String libPath, LibrarySet<String> libraryEntries) {
        LibrarySet<String> clazzSet = new LibrarySet<String>();
        for (LibrarySet<String>.Entry<String> library : libraryEntries.entrySet()) {
            try {
                JarFile jarFile = new JarFile(library.toString());
                LibrarySet<String>.Entry<String>[] clazzes = library.getClassReferences().toArray(new LibrarySet.Entry[0]);
                for (int i = 0; i < clazzes.length; i++) {
                    LibrarySet.Entry clazzEntry = clazzes[i];
                    InputStream clazzStream = jarFile.getInputStream(jarFile.getEntry(clazzEntry.toString()));
                    clazzSet.addAll(poolReader.getClassesInUse(clazzStream));
                }
            } catch (IOException e) {
                writer.println("Error: Error while reading " + library + ", skipping the entry.");
            }
        }
        LibrarySet<String> transitiveCodeDeps = findCodeDependencies(libPath, clazzSet);
        transitiveCodeDeps.removeAll(libEntries);
        transitiveCodeDeps.removeAll(transitiveEntries);
        if (transitiveCodeDeps.entrySet().size() > 0) {
            transitiveEntries.addAll(transitiveCodeDeps);
            findTransitiveDependencies(libPath, transitiveCodeDeps);
        }
    }

    private void validateArgs(String[] args) {
        List argsList = Arrays.asList(args);
        int outputFileOption = argsList.indexOf("-o");
        if (outputFileOption > -1) {
            if (args.length > outputFileOption + 1) {
                String outputFilePath = args[outputFileOption + 1];
                writer = new ReportGenerator(outputFilePath);
            } else {
                printUsage();
                System.exit(0);
            }
        } else {
            writer = new ReportGenerator();
        }
        if (args.length < 4) {
            printUsage();
            System.exit(0);
        }
        if (argsList.indexOf("-?") > -1) {
            printUsage();
            System.exit(0);
        }
        if (args[0].equalsIgnoreCase("-s")) {
            sourceList = args[1].split(";");
        } else {
            printUsage();
            System.exit(0);
        }
        if (args[2].equalsIgnoreCase("-i")) {
            includesList = args[3].split(";");
            if ((args.length >= 6) && (args[4].equalsIgnoreCase("-l"))) {
                libraryList = args[5].split(";");
            } else {
                printUsage();
                System.exit(0);
            }
        } else if (args[2].equalsIgnoreCase("-l")) {
            libraryList = args[3].split(";");
        } else {
            printUsage();
            System.exit(0);
        }
    }

    private void printUsage() {
        System.out.println("Usage:java -jar jlibtracker.jar <options>\n");
        System.out.println("Options: -s <classpath> [-i <classpath>] -l <classpath> [-o <filepath>]\n");
        System.out.println("-s <classpath>                      Specifies the classfiles to be processed.");
        System.out.println("                                    \tValid entries are directories, .jar and .class files");
        System.out.println("-i <classpath>                      (Optional) Specifies the additional classfiles to be included for processing.");
        System.out.println("                                    \tValid entries are .class files");
        System.out.println("-l <classpath>                      Specifies the libraries to find dependencies.");
        System.out.println("                                    \tValid entries are directories, .jar and .class files");
        System.out.println("-o <filepath>                       Specifies the location of the output file to write.");
        System.out.println("\nNote: In all cases multiple classpaths are to be semi-colon(;) separated");
    }
}
