package com.ibm.sigtest;

import java.lang.reflect.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.io.*;
import java.util.*;

/**
 * This class implements static utility methods for use by the signature
 * test tool.
 *
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 */
public class SigTestUtils {

    /**
   * This method builds a new project description by using an existing one
   * as a reference. The new project description is built by resolving each
   * class specified in the reference description, and adding their signatures
   * to the newly built project.
   *
   * @param referencePD the reference project description
   *
   * @return the new project description
   */
    public static ProjectDesc getProjectDesc(ProjectDesc referencePD) {
        ProjectDesc candidatePD = new ProjectDesc("CandidateImpl");
        Iterator iterator = referencePD.getClassDescs().iterator();
        while (iterator.hasNext()) {
            ClassDesc referenceCD = (ClassDesc) iterator.next();
            String className = referenceCD.getName();
            ClassDesc candidateCD = null;
            try {
                Class candidateClass = Class.forName(className);
                candidateCD = getClassDesc(candidateClass);
            } catch (ClassNotFoundException e) {
            }
            if (candidateCD != null) {
                candidatePD.addClassDesc(candidateCD);
            }
        }
        return candidatePD;
    }

    public static ClassDesc getClassDesc(Class theClass) {
        String className = getClassName(theClass);
        ClassDesc classDesc = new ClassDesc(className, theClass.getModifiers());
        Class superClass = theClass.getSuperclass();
        if (superClass != null) {
            classDesc.setSuperClassName(getClassName(superClass));
        }
        List interfaceNames = getTypeNames(theClass.getInterfaces());
        classDesc.setInterfaceNames(interfaceNames);
        List constructorDescs = getConstructorDescs(className, theClass.getDeclaredConstructors());
        classDesc.setConstructorDescs(constructorDescs);
        List methodDescs = getMethodDescs(theClass.getDeclaredMethods());
        classDesc.setMethodDescs(methodDescs);
        return classDesc;
    }

    public static List getConstructorDescs(String className, Constructor[] constructors) {
        List constructorDescs = new Vector();
        for (int i = 0; i < constructors.length; i++) {
            MethodDesc constructorDesc = new MethodDesc(className, constructors[i].getModifiers(), null, getTypeNames(constructors[i].getParameterTypes()), getTypeNames(constructors[i].getExceptionTypes()));
            constructorDescs.add(constructorDesc);
        }
        return constructorDescs;
    }

    public static List getMethodDescs(Method[] methods) {
        List methodDescs = new Vector();
        for (int i = 0; i < methods.length; i++) {
            MethodDesc methodDesc = new MethodDesc(methods[i].getName(), methods[i].getModifiers(), getClassName(methods[i].getReturnType()), getTypeNames(methods[i].getParameterTypes()), getTypeNames(methods[i].getExceptionTypes()));
            methodDescs.add(methodDesc);
        }
        return methodDescs;
    }

    public static List getTypeNames(Class[] types) {
        List typeNames = new Vector();
        for (int i = 0; i < types.length; i++) {
            typeNames.add(getClassName(types[i]));
        }
        return typeNames;
    }

    public static String listToString(List list) {
        StringBuffer strBuf = new StringBuffer();
        int size = list.size();
        for (int i = 0; i < size; i++) {
            strBuf.append((i > 0 ? "," : "") + list.get(i));
        }
        return strBuf.toString();
    }

    public static List stringToList(String str, String delim) {
        return Arrays.asList(tokenize(str, delim));
    }

    public static String[] tokenize(String tokenStr, String delim) {
        StringTokenizer strTok = new StringTokenizer(tokenStr, delim);
        String[] tokens = new String[strTok.countTokens()];
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = strTok.nextToken();
        }
        return tokens;
    }

    public static boolean objectsEqual(Object obj1, Object obj2) {
        if (obj1 == null) {
            return (obj2 == null);
        } else {
            return obj1.equals(obj2);
        }
    }

    public static boolean collectionsMatch(Collection c1, Collection c2) {
        if (c1 == null) {
            return (c2 == null);
        } else {
            return c1.containsAll(c2) && c2.containsAll(c1);
        }
    }

    public static String getExpandedMethodList(List list) {
        StringBuffer strBuf = new StringBuffer("[");
        int size = list.size();
        for (int i = 0; i < size; i++) {
            strBuf.append((i > 0 ? ", " : "") + ((MethodDesc) list.get(i)).toString(true));
        }
        strBuf.append("]");
        return strBuf.toString();
    }

    public static String getCondensedClassList(List list) {
        StringBuffer strBuf = new StringBuffer("[");
        int size = list.size();
        for (int i = 0; i < size; i++) {
            strBuf.append((i > 0 ? ", " : "") + ((ClassDesc) list.get(i)).getName());
        }
        strBuf.append("]");
        return strBuf.toString();
    }

    public static void findExtras(Collection reference, Collection candidate, List referenceExtras, List candidateExtras) {
        referenceExtras.addAll(reference);
        referenceExtras.removeAll(candidate);
        candidateExtras.addAll(candidate);
        candidateExtras.removeAll(reference);
        if (candidateExtras.size() > 0) {
            Iterator memberDescs = candidateExtras.iterator();
            while (memberDescs.hasNext()) {
                MemberDesc memberDesc = (MemberDesc) memberDescs.next();
                int modifiers = memberDesc.getModifiers();
                if (Modifier.isPrivate(modifiers) || !(Modifier.isProtected(modifiers) || Modifier.isPublic(modifiers))) {
                    memberDescs.remove();
                }
            }
        }
    }

    public static void findExtraClasses(ProjectDesc referencePD, ProjectDesc candidatePD, List referenceExtras, List candidateExtras) {
        referenceExtras.addAll(referencePD.getClassDescs());
        candidateExtras.addAll(candidatePD.getClassDescs());
        Iterator iterator = referenceExtras.iterator();
        while (iterator.hasNext()) {
            ClassDesc referenceCD = (ClassDesc) iterator.next();
            ClassDesc candidateCD = candidatePD.getClassDesc(referenceCD.getName());
            if (candidateCD != null) {
                iterator.remove();
            }
        }
        iterator = candidateExtras.iterator();
        while (iterator.hasNext()) {
            ClassDesc candidateCD = (ClassDesc) iterator.next();
            ClassDesc referenceCD = referencePD.getClassDesc(candidateCD.getName());
            if (referenceCD != null) {
                iterator.remove();
            }
        }
    }

    public static String getClassName(Class targetClass) {
        String className = targetClass.getName();
        return targetClass.isArray() ? parseDescriptor(className) : className;
    }

    private static String parseDescriptor(String className) {
        char[] classNameChars = className.toCharArray();
        int arrayDim = 0;
        int i = 0;
        while (classNameChars[i] == '[') {
            arrayDim++;
            i++;
        }
        StringBuffer classNameBuf = new StringBuffer();
        switch(classNameChars[i++]) {
            case 'B':
                classNameBuf.append("byte");
                break;
            case 'C':
                classNameBuf.append("char");
                break;
            case 'D':
                classNameBuf.append("double");
                break;
            case 'F':
                classNameBuf.append("float");
                break;
            case 'I':
                classNameBuf.append("int");
                break;
            case 'J':
                classNameBuf.append("long");
                break;
            case 'S':
                classNameBuf.append("short");
                break;
            case 'Z':
                classNameBuf.append("boolean");
                break;
            case 'L':
                classNameBuf.append(classNameChars, i, classNameChars.length - i - 1);
                break;
        }
        for (i = 0; i < arrayDim; i++) classNameBuf.append("[]");
        return classNameBuf.toString();
    }

    private static OutputStream getOutputStream(String root, String name, boolean overwrite, boolean verbose) throws IOException {
        if (root != null) {
            File directory = new File(root);
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    throw new IOException("Failed to create directory '" + root + "'.");
                } else if (verbose) {
                    System.out.println("Created directory '" + directory.getAbsolutePath() + "'.");
                }
            }
        }
        File file = new File(root, name);
        String absolutePath = file.getAbsolutePath();
        if (file.exists()) {
            if (!overwrite) {
                throw new IOException("File '" + absolutePath + "' already exists. " + "Please remove it or enable the " + "overwrite option.");
            } else {
                file.delete();
                if (verbose) {
                    System.out.println("Deleted file '" + absolutePath + "'.");
                }
            }
        }
        FileOutputStream fos = new FileOutputStream(absolutePath);
        if (verbose) {
            System.out.println("Created file '" + absolutePath + "'.");
        }
        return fos;
    }

    public static void generateProjectFile(String classListFile, String projectFile, boolean overwrite) throws IOException, ClassNotFoundException {
        FileReader in = new FileReader(classListFile);
        BufferedReader buf = new BufferedReader(in);
        OutputStream out = getOutputStream(null, projectFile, overwrite, true);
        PrintWriter pw = new PrintWriter(out);
        String tempLine;
        while ((tempLine = buf.readLine()) != null) {
            pw.println(getClassDesc(Class.forName(tempLine)));
        }
        pw.flush();
        pw.close();
        buf.close();
        in.close();
    }

    public static void generateProjectFileFromPackage(String rootPackage, String projectFile, boolean overwrite) throws IOException, ClassNotFoundException, URISyntaxException {
        OutputStream out = getOutputStream(null, projectFile, overwrite, true);
        PrintWriter pw = new PrintWriter(out);
        String rootPackageDir = rootPackage.replace('.', '\\');
        URL url = ClassLoader.getSystemResource(rootPackageDir);
        StringTokenizer tok = new StringTokenizer(rootPackageDir, "\\");
        String rootPackagePath = "";
        int tokens = tok.countTokens();
        for (int i = 0; i < tokens; i++) {
            rootPackagePath = rootPackagePath + tok.nextToken() + "\\";
        }
        File wsdlRoot = new File(new URI(url.toString()));
        if (wsdlRoot.exists()) {
            List list = getAllClasses(wsdlRoot);
            List allClasses = new ArrayList();
            Iterator itr = list.iterator();
            while (itr.hasNext()) {
                File file = (File) itr.next();
                String fullName = file.getAbsolutePath();
                int packageStart = fullName.indexOf(rootPackagePath);
                String relativeName = fullName.substring(packageStart);
                int dot = relativeName.indexOf(".");
                String className = relativeName.substring(0, dot);
                className = className.replace('\\', '.');
                allClasses.add(className);
            }
            Collections.sort(allClasses, new ClassNameSorter());
            itr = allClasses.iterator();
            while (itr.hasNext()) {
                String className = (String) itr.next();
                if (className.indexOf("$") < 0) {
                    System.out.println(className);
                    pw.println(getClassDesc(Class.forName(className)));
                }
            }
        } else {
            throw new IOException(url + " is not a directory");
        }
        pw.flush();
        pw.close();
    }

    private static class ClassNameSorter implements Comparator {

        public int compare(Object arg0, Object arg1) {
            String name1 = (String) arg0;
            String name2 = (String) arg1;
            StringTokenizer tok1 = new StringTokenizer(name1, ".");
            StringTokenizer tok2 = new StringTokenizer(name2, ".");
            String namePart1 = "";
            String namePart2 = "";
            while (tok1.hasMoreTokens() && tok2.hasMoreTokens()) {
                namePart1 = tok1.nextToken();
                namePart2 = tok2.nextToken();
                if (!namePart1.equals(namePart2)) {
                    return namePart1.compareTo(namePart2);
                }
            }
            return 0;
        }
    }

    private static List getAllClasses(File dir) {
        ArrayList list = new ArrayList();
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    list.addAll(getAllClasses(files[i]));
                } else {
                    list.add(files[i]);
                }
            }
        }
        return list;
    }
}
