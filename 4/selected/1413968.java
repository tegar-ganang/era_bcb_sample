package edu.nps.moves.xmlpg;

import java.util.*;
import java.io.*;

/**
 * Given the input object, something of an abstract syntax tree, this generates
 * a source code file in the java language. It has ivars, getters,  setters,
 * and serialization/deserialization methods.
 *
 * @author DMcG
 * @author Andrew Sampson
 */
public class JavaGenerator extends Generator {

    protected class PrimitiveTypeInfo {

        /** The API type.  Java does not have unsigned types, so the next-larger 
		integer type must be used when storing unsigned integer data. */
        public String apiType = null;

        /** The wrapper type corresponding to the given API type.  For example: 
		int -> Integer.  Used for list attributes; java containers can't store 
		primitive types, only references. */
        public String wrapperType = null;

        /** The type used for serializing/deserializing.  Will match the length of 
		the original xmlpg type. */
        public String marshalType = null;

        /** The length of the serialized primitive, in bytes */
        public int marshaledLength = 0;

        /** The method in DataOutputStream to call when serializing. */
        public String marshalMethod = null;

        /** The method in DataInputStream to call when deserializing. */
        public String unmarshalMethod = null;

        public PrimitiveTypeInfo() {
        }

        public PrimitiveTypeInfo(String _apiType, String _wrapperType, String _marshalType, int _marshaledLength, String _marshalMethod, String _unmarshalMethod) {
            apiType = _apiType;
            wrapperType = _wrapperType;
            marshalType = _marshalType;
            marshaledLength = _marshaledLength;
            marshalMethod = _marshalMethod;
            unmarshalMethod = _unmarshalMethod;
        }
    }

    HashMap<String, PrimitiveTypeInfo> types = new HashMap<String, PrimitiveTypeInfo>();

    /** A property list that contains java-specific code generation information, such
     * as package names, imports, etc.
     */
    Properties javaProperties;

    HashMap<ClassAttribute.ClassAttributeType, JavaAttributeGenerator> writers = new HashMap<ClassAttribute.ClassAttributeType, JavaAttributeGenerator>();

    public JavaGenerator(List<GeneratedClass> pClassDescriptions, HashMap<String, GeneratedEnumeration> pEnumerations, String pDirectory, Properties pJavaProperties, String pLicenseStatement) {
        super(pClassDescriptions, pEnumerations, pDirectory, pJavaProperties, pLicenseStatement);
        types.put("unsigned byte", new PrimitiveTypeInfo("short", "Short", "byte", 1, "writeByte", "readUnsignedByte"));
        types.put("unsigned short", new PrimitiveTypeInfo("int", "Integer", "short", 2, "writeShort", "readUnsignedShort"));
        types.put("unsigned int", new PrimitiveTypeInfo("long", "Long", "int", 4, "writeInt", "readInt"));
        types.put("byte", new PrimitiveTypeInfo("byte", "Byte", "byte", 1, "writeByte", "readByte"));
        types.put("short", new PrimitiveTypeInfo("short", "Short", "short", 2, "writeShort", "readShort"));
        types.put("int", new PrimitiveTypeInfo("int", "Integer", "int", 4, "writeInt", "readInt"));
        types.put("long", new PrimitiveTypeInfo("long", "Long", "long", 8, "writeLong", "readLong"));
        types.put("double", new PrimitiveTypeInfo("double", "Double", "double", 8, "writeDouble", "readDouble"));
        types.put("float", new PrimitiveTypeInfo("float", "Float", "float", 4, "writeFloat", "readFloat"));
        writers.put(ClassAttribute.ClassAttributeType.PADDING, new JavaPaddingGenerator());
        writers.put(ClassAttribute.ClassAttributeType.PRIMITIVE, new JavaPrimitiveGenerator());
        writers.put(ClassAttribute.ClassAttributeType.CLASSREF, new JavaClassRefGenerator());
        writers.put(ClassAttribute.ClassAttributeType.ENUMREF, new JavaEnumRefGenerator());
        writers.put(ClassAttribute.ClassAttributeType.FIXED_LIST, new JavaFixedLengthListGenerator());
        writers.put(ClassAttribute.ClassAttributeType.VARIABLE_LIST, new JavaVariableLengthListGenerator());
        writers.put(ClassAttribute.ClassAttributeType.BITFIELD, new JavaBitfieldGenerator());
        writers.put(ClassAttribute.ClassAttributeType.BOOLEAN, new JavaBooleanGenerator());
        writers.put(ClassAttribute.ClassAttributeType.UNSET, null);
    }

    protected String getOutputSubdirectoryName() {
        return "java";
    }

    /**
     * Generate the classes and write them to a directory
     */
    public void writeClasses() {
        this.createDirectory();
        Iterator it = classDescriptions.iterator();
        while (it.hasNext()) {
            try {
                GeneratedClass aClass = (GeneratedClass) it.next();
                String name = aClass.getName();
                String pack = languageProperties.getProperty("package");
                String fullPath;
                if (pack != null) {
                    pack = pack.replace(".", "/");
                    fullPath = directory + "/" + pack + "/" + name + ".java";
                } else {
                    fullPath = directory + "/" + name + ".java";
                }
                System.out.println("Creating Java source code file for " + fullPath);
                File outputFile = new File(fullPath);
                outputFile.createNewFile();
                PrintWriter pw = new PrintWriter(outputFile);
                this.writeClass(pw, aClass);
            } catch (Exception e) {
                System.out.println("error creating source code: " + e);
                e.printStackTrace();
            }
        }
    }

    /**
     * Generate a source code file with getters, setters, ivars, and marshal/unmarshal
     * methods for one class. This is way too long and should be broken up into several
     * methods.
     */
    public void writeClass(PrintWriter pw, GeneratedClass aClass) {
        pw.println("/*");
        pw.println(licenseStatement);
        pw.println("*/");
        pw.println();
        String packageName = languageProperties.getProperty("package");
        if (packageName != null) {
            pw.println("package " + packageName + ";");
        }
        pw.println();
        String imports = languageProperties.getProperty("imports");
        StringTokenizer tokenizer = new StringTokenizer(imports, ",");
        while (tokenizer.hasMoreTokens()) {
            String aPackage = (String) tokenizer.nextToken();
            pw.println("import " + aPackage + ";");
        }
        pw.println();
        pw.println("/**");
        if (aClass.getComment() != null && !aClass.getComment().equals("")) {
            pw.println(" * " + aClass.getComment());
        }
        pw.println(" */");
        pw.print("public class " + aClass.getName());
        GeneratedClass parentClass = aClass.getParentClass();
        if (parentClass == null) pw.println(); else pw.println(" extends " + parentClass.getName());
        pw.println("{");
        writeEnumerations(pw, aClass);
        List ivars = aClass.getClassAttributes();
        for (int idx = 0; idx < ivars.size(); idx++) {
            ClassAttribute anAttribute = (ClassAttribute) ivars.get(idx);
            JavaAttributeGenerator writer = writers.get(anAttribute.getAttributeKind());
            writer.writeDeclaration(pw, aClass, anAttribute);
        }
        pw.println();
        for (int idx = 0; idx < ivars.size(); idx++) {
            ClassAttribute anAttribute = (ClassAttribute) ivars.get(idx);
            JavaAttributeGenerator writer = writers.get(anAttribute.getAttributeKind());
            writer.writeGetterMethod(pw, aClass, anAttribute);
        }
        pw.println();
        for (int idx = 0; idx < ivars.size(); idx++) {
            ClassAttribute anAttribute = (ClassAttribute) ivars.get(idx);
            JavaAttributeGenerator writer = writers.get(anAttribute.getAttributeKind());
            writer.writeSetterMethod(pw, aClass, anAttribute);
        }
        this.writeMarshalMethod(pw, aClass);
        this.writeUnmarshalMethod(pw, aClass);
        this.writeEqualityMethod(pw, aClass);
        writeLengthMethod(pw, aClass);
        pw.println("} // end of class");
        pw.flush();
        pw.close();
    }

    protected void writeMarshalMethod(PrintWriter pw, GeneratedClass aClass) {
        pw.println();
        pw.println("public void marshal(DataOutputStream dos)");
        pw.println("{");
        if (aClass.getParentClass() != null) {
            pw.println("\tsuper.marshal(dos);");
        }
        pw.println("\ttry\n\t{");
        List ivars = aClass.getClassAttributes();
        for (int idx = 0; idx < ivars.size(); idx++) {
            ClassAttribute anAttribute = (ClassAttribute) ivars.get(idx);
            JavaAttributeGenerator writer = writers.get(anAttribute.getAttributeKind());
            writer.writeMarshallingStatement(pw, aClass, anAttribute);
        }
        pw.println("\t}\n\tcatch(Exception e)");
        pw.println("\t{\n\t\tSystem.out.println(e);\n\t}");
        pw.println("}\n");
    }

    protected void writeUnmarshalMethod(PrintWriter pw, GeneratedClass aClass) {
        pw.println();
        pw.println("public void unmarshal(DataInputStream dis)");
        pw.println("{");
        if (aClass.getParentClass() != null) {
            pw.println("\tsuper.unmarshal(dis);");
        }
        pw.println("\ttry\n\t{");
        List ivars = aClass.getClassAttributes();
        for (int idx = 0; idx < ivars.size(); idx++) {
            ClassAttribute anAttribute = (ClassAttribute) ivars.get(idx);
            JavaAttributeGenerator writer = writers.get(anAttribute.getAttributeKind());
            writer.writeUnmarshallingStatement(pw, aClass, anAttribute);
        }
        pw.println("\t}\n\tcatch(Exception e)");
        pw.println("\t{\n\t\tSystem.out.println(e);\n\t}");
        pw.println("}\n");
    }

    /**
 * Write the code for equality and hashcode methods.  The resulting methods 
 * override Object.equals() and Object.hashCode().
 */
    protected void writeEqualityMethod(PrintWriter pw, GeneratedClass aClass) {
        try {
            pw.println();
            pw.println("public boolean equals( Object rhsObj )");
            pw.println("{");
            pw.println("\tif( this == rhsObj )");
            pw.println("\t\treturn true;");
            pw.println();
            pw.println("\tif( !( rhsObj instanceof " + aClass.getName() + " ) )");
            pw.println("\t\treturn false;");
            pw.println();
            pw.println("\t" + aClass.getName() + " rhs = (" + aClass.getName() + ")rhsObj;");
            pw.println("\tif( ! super.equals( rhs ) )");
            pw.println("\t\treturn false;");
            pw.println();
            pw.println("\tboolean ivarsEqual = true;");
            pw.println();
            for (int idx = 0; idx < aClass.getClassAttributes().size(); idx++) {
                ClassAttribute anAttribute = (ClassAttribute) aClass.getClassAttributes().get(idx);
                JavaAttributeGenerator writer = writers.get(anAttribute.getAttributeKind());
                writer.writeEqualityTestStatement(pw, aClass, anAttribute);
            }
            pw.println();
            pw.println("\treturn ivarsEqual;");
            pw.println("}\n");
            pw.println();
            pw.println("public int hashCode()");
            pw.println("{");
            pw.println("\tfinal int magicNumber = 31;");
            pw.println("\tint hash = 1;");
            pw.println("\thash = hash * magicNumber + super.hashCode();");
            pw.println();
            for (int idx = 0; idx < aClass.getClassAttributes().size(); idx++) {
                ClassAttribute anAttribute = (ClassAttribute) aClass.getClassAttributes().get(idx);
                JavaAttributeGenerator writer = writers.get(anAttribute.getAttributeKind());
                writer.writeHashCodeStatement(pw, aClass, anAttribute);
            }
            pw.println();
            pw.println("\treturn hash;");
            pw.println("}\n");
        } catch (Exception e) {
            System.out.println("While writing equality method for class \"" + aClass.getName() + "\", encountered exception:");
            System.out.println(e);
        }
    }

    protected void writeLengthMethod(PrintWriter pw, GeneratedClass aClass) {
        pw.println();
        pw.println("public int length()");
        pw.println("{");
        pw.println("\tint result = 0;");
        if (aClass.getParentClass() != null) pw.println("\tresult += super.length();");
        for (ClassAttribute attr : aClass.getClassAttributes()) {
            JavaAttributeGenerator writer = writers.get(attr.getAttributeKind());
            if (writer != null) {
                writer.writeLengthRetrievalStatement(pw, aClass, attr);
            } else {
                System.out.println("While writing length() method in Java code for class \"" + aClass.getName() + "\", could not find a writer for attribute \"" + attr.getName() + "\" (which is of attribute type \"" + attr.getAttributeKind() + "\")");
            }
        }
        pw.println("\treturn result;");
        pw.println("}\n");
    }

    /**  */
    protected void writeEnumerations(PrintWriter pw, GeneratedClass aClass) {
        List enumList = aClass.getClassEnumerations();
        Iterator iterator = enumList.iterator();
        while (iterator.hasNext()) {
            GeneratedEnumeration e = (GeneratedEnumeration) iterator.next();
            if (e.getComment() != null && !e.getComment().equals("")) {
                pw.println("/**");
                pw.println(" * " + e.getComment());
                pw.println(" */");
            }
            pw.println("public enum " + e.getName());
            pw.println("{");
            List namesValues = e.getNameValuePairs();
            Iterator pairIter = namesValues.iterator();
            while (pairIter.hasNext()) {
                GeneratedEnumeration.EnumPair pair = (GeneratedEnumeration.EnumPair) pairIter.next();
                pw.print("\t" + pair.name + "( " + Integer.toString(pair.value) + " )");
                if (pairIter.hasNext()) pw.println(","); else pw.println(";");
            }
            String firstCharLowerName = initialLower(e.getName());
            pw.println();
            pw.println("\tprivate static final Map<Integer, " + e.getName() + "> numberTo" + e.getName() + "Map;");
            pw.println("\tprivate final int " + firstCharLowerName + "Num;");
            pw.println();
            pw.println("\tstatic { ");
            pw.println("\t\tnumberTo" + e.getName() + "Map = new HashMap<Integer, " + e.getName() + ">();");
            pw.println("\t\tfor (" + e.getName() + " " + firstCharLowerName + " : EnumSet.allOf(" + e.getName() + ".class)) {");
            pw.println("\t\t\tnumberTo" + e.getName() + "Map.put(" + firstCharLowerName + ".toInt(), " + firstCharLowerName + "); ");
            pw.println("\t\t}");
            pw.println("\t}");
            pw.println();
            pw.println("\tprivate " + e.getName() + "(int p" + e.getName() + "Num) {");
            pw.println("\t\t" + firstCharLowerName + "Num = p" + e.getName() + "Num;");
            pw.println("\t}");
            pw.println();
            pw.println("\tpublic int toInt() {");
            pw.println("\t\treturn " + firstCharLowerName + "Num;");
            pw.println("\t}");
            pw.println();
            pw.println("\tpublic static " + e.getName() + " from" + e.getName() + "Num(int value) {");
            pw.println("\t\treturn numberTo" + e.getName() + "Map.get(value);");
            pw.println("\t}");
            pw.println("}");
            pw.println();
        }
    }

    /** 
     * returns a string with the first letter capitalized. 
     */
    public String initialCap(String aString) {
        StringBuffer stb = new StringBuffer(aString);
        stb.setCharAt(0, Character.toUpperCase(aString.charAt(0)));
        return stb.toString();
    }

    /** 
     * returns a string with the first letter converted to lower case. 
     */
    public String initialLower(String aString) {
        StringBuffer stb = new StringBuffer(aString);
        stb.setCharAt(0, Character.toLowerCase(aString.charAt(0)));
        return stb.toString();
    }

    protected String formatVariableName(String unadornedName) {
        return unadornedName;
    }

    protected int sizeof(String type) {
        int result = 0;
        PrimitiveTypeInfo typeInfo = types.get(type);
        if (typeInfo == null) {
            System.out.println("Could not find sizeof entry for the following type: " + type);
        } else {
            result = typeInfo.marshaledLength;
        }
        return result;
    }

    protected abstract class JavaAttributeGenerator {

        public abstract void writeDeclaration(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute);

        public abstract void writeGetterMethod(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute);

        public abstract void writeSetterMethod(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute);

        public abstract void writeMarshallingStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute);

        public abstract void writeUnmarshallingStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute);

        public abstract void writeEqualityTestStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute);

        public abstract void writeHashCodeStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute);

        public abstract void writeLengthRetrievalStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute);
    }

    protected class JavaPaddingGenerator extends JavaAttributeGenerator {

        public void writeDeclaration(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
        }

        public void writeGetterMethod(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
        }

        public void writeSetterMethod(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
        }

        public void writeMarshallingStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            PaddingAttribute padding = (PaddingAttribute) anAttribute;
            PrimitiveTypeInfo typeInfo = types.get(padding.getPaddingType());
            pw.println("\t\tdos." + typeInfo.marshalMethod + "( (" + typeInfo.marshalType + ")" + padding.getDefaultValue() + " ); // padding");
        }

        public void writeUnmarshallingStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            PaddingAttribute padding = (PaddingAttribute) anAttribute;
            PrimitiveTypeInfo typeInfo = types.get(padding.getPaddingType());
            pw.println("\t\t{\n\t\t\t// skip over some padding");
            pw.println("\t\t\t" + typeInfo.marshalType + " temp = (" + typeInfo.marshalType + ")dis." + typeInfo.unmarshalMethod + "();");
            pw.println("\t\t}");
        }

        public void writeEqualityTestStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
        }

        public void writeHashCodeStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
        }

        public void writeLengthRetrievalStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            PaddingAttribute padding = (PaddingAttribute) anAttribute;
            pw.println("\tresult += " + sizeof(padding.getPaddingType()) + "; // padding");
        }
    }

    protected class JavaPrimitiveGenerator extends JavaAttributeGenerator {

        public void writeDeclaration(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            PrimitiveAttribute primitive = (PrimitiveAttribute) anAttribute;
            PrimitiveTypeInfo typeInfo = types.get(primitive.getPrimitiveType());
            if (primitive.getComment() != null && !primitive.getComment().equals("")) {
                pw.println("/** " + primitive.getComment() + " */");
            }
            String defaultValue = primitive.getDefaultValue();
            pw.print("protected " + typeInfo.apiType + " " + primitive.getName());
            if (defaultValue != null) {
                pw.print(" = (" + typeInfo.apiType + ")" + defaultValue);
            }
            pw.println(";\n");
        }

        public void writeGetterMethod(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            PrimitiveAttribute primitive = (PrimitiveAttribute) anAttribute;
            String nameWithInitialCaps = initialCap(primitive.getName());
            PrimitiveTypeInfo typeInfo = types.get(primitive.getPrimitiveType());
            pw.println("public " + typeInfo.apiType + " get" + nameWithInitialCaps + "()");
            pw.println("{\n\treturn " + primitive.getName() + ";\n}");
            pw.println();
        }

        public void writeSetterMethod(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            PrimitiveAttribute primitive = (PrimitiveAttribute) anAttribute;
            String nameWithInitialCaps = initialCap(primitive.getName());
            PrimitiveTypeInfo typeInfo = types.get(primitive.getPrimitiveType());
            pw.println("public void set" + nameWithInitialCaps + "(" + typeInfo.apiType + " p" + nameWithInitialCaps + ")");
            pw.println("{\n\t" + primitive.getName() + " = p" + nameWithInitialCaps + ";");
            pw.println("}");
            pw.println();
        }

        public void writeMarshallingStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            PrimitiveAttribute primitive = (PrimitiveAttribute) anAttribute;
            PrimitiveTypeInfo typeInfo = types.get(primitive.getPrimitiveType());
            pw.println("\t\tdos." + typeInfo.marshalMethod + "( (" + typeInfo.marshalType + ")" + primitive.getName() + " );");
        }

        public void writeUnmarshallingStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            PrimitiveAttribute primitive = (PrimitiveAttribute) anAttribute;
            PrimitiveTypeInfo typeInfo = types.get(primitive.getPrimitiveType());
            pw.println("\t\t" + primitive.getName() + " = (" + typeInfo.apiType + ")dis." + typeInfo.unmarshalMethod + "();");
        }

        public void writeEqualityTestStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            pw.println("\tif( ! (" + anAttribute.getName() + " == rhs." + anAttribute.getName() + ") ) ivarsEqual = false;");
        }

        public void writeHashCodeStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            PrimitiveAttribute primitive = (PrimitiveAttribute) anAttribute;
            PrimitiveTypeInfo typeInfo = types.get(primitive.getPrimitiveType());
            pw.println("\thash = hash * magicNumber + " + typeInfo.wrapperType + ".valueOf( " + formatVariableName(anAttribute.getName()) + " ).hashCode();");
        }

        public void writeLengthRetrievalStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            PrimitiveAttribute primitive = (PrimitiveAttribute) anAttribute;
            pw.println("\tresult += " + sizeof(primitive.getPrimitiveType()) + "; // " + formatVariableName(primitive.getName()));
        }
    }

    protected class JavaClassRefGenerator extends JavaAttributeGenerator {

        public void writeDeclaration(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            ClassRefAttribute classRef = (ClassRefAttribute) anAttribute;
            String className = classRef.getClassType();
            if (classRef.getComment() != null && !classRef.getComment().equals("")) {
                pw.println("/** " + classRef.getComment() + " */");
            }
            pw.println("protected " + className + " " + classRef.getName() + " = new " + className + "();\n");
        }

        public void writeGetterMethod(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            ClassRefAttribute classRef = (ClassRefAttribute) anAttribute;
            String nameWithInitialCaps = initialCap(classRef.getName());
            pw.println("public " + classRef.getClassType() + " get" + nameWithInitialCaps + "()");
            pw.println("{\n\treturn " + classRef.getName() + ";\n}");
            pw.println();
        }

        public void writeSetterMethod(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            ClassRefAttribute classRef = (ClassRefAttribute) anAttribute;
            String nameWithInitialCaps = initialCap(classRef.getName());
            pw.println("public void set" + nameWithInitialCaps + "(" + classRef.getClassType() + " p" + nameWithInitialCaps + ")");
            pw.println("{\n\t" + classRef.getName() + " = p" + nameWithInitialCaps + ";");
            pw.println("}");
            pw.println();
        }

        public void writeMarshallingStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            pw.println("\t\t" + anAttribute.getName() + ".marshal( dos );");
        }

        public void writeUnmarshallingStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            pw.println("\t\t" + anAttribute.getName() + ".unmarshal( dis );");
        }

        public void writeEqualityTestStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            pw.println("\tif( ! (" + anAttribute.getName() + ".equals( rhs." + anAttribute.getName() + ") ) ) ivarsEqual = false;");
        }

        public void writeHashCodeStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            pw.println("\thash = hash * magicNumber + " + formatVariableName(anAttribute.getName()) + ".hashCode();");
        }

        public void writeLengthRetrievalStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            pw.println("\tresult += " + formatVariableName(anAttribute.getName()) + ".length(); // " + formatVariableName(anAttribute.getName()));
        }
    }

    protected class JavaEnumRefGenerator extends JavaAttributeGenerator {

        public void writeDeclaration(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            EnumRefAttribute enumRef = (EnumRefAttribute) anAttribute;
            if (enumRef.getComment() != null && !enumRef.getComment().equals("")) {
                pw.println("/** " + enumRef.getComment() + " */");
            }
            String attributeType = enumRef.getEnumName();
            String defaultValue = enumRef.getDefaultValue();
            pw.print("protected " + attributeType + " " + enumRef.getName());
            if (defaultValue != null) pw.print(" = " + attributeType + "." + defaultValue);
            pw.println(";\n");
        }

        public void writeGetterMethod(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            EnumRefAttribute enumRef = (EnumRefAttribute) anAttribute;
            pw.println("public " + enumRef.getEnumName() + " get" + initialCap(enumRef.getName()) + "()");
            pw.println("{\n\treturn " + enumRef.getName() + ";\n}");
            pw.println();
        }

        public void writeSetterMethod(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            EnumRefAttribute enumRef = (EnumRefAttribute) anAttribute;
            String nameWithInitialCaps = initialCap(enumRef.getName());
            pw.println("public void set" + nameWithInitialCaps + "(" + enumRef.getEnumName() + " p" + nameWithInitialCaps + ")");
            pw.println("{\n\t" + enumRef.getName() + " = p" + nameWithInitialCaps + ";");
            pw.println("}");
            pw.println();
            pw.println("public void set" + nameWithInitialCaps + "(int p" + nameWithInitialCaps + ")");
            pw.println("{\n\t" + enumRef.getName() + " = " + enumRef.getEnumName() + ".from" + enumRef.getEnumName() + "Num(p" + nameWithInitialCaps + ");");
            pw.println("}");
            pw.println();
        }

        public void writeMarshallingStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            EnumRefAttribute enumRef = (EnumRefAttribute) anAttribute;
            PrimitiveTypeInfo typeInfo = types.get(enumRef.getSerializedType());
            pw.println("\t\tdos." + typeInfo.marshalMethod + "( (" + typeInfo.marshalType + ")" + enumRef.getName() + ".toInt() );");
        }

        public void writeUnmarshallingStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            EnumRefAttribute enumRef = (EnumRefAttribute) anAttribute;
            PrimitiveTypeInfo typeInfo = types.get(enumRef.getSerializedType());
            pw.println("\t\tset" + initialCap(enumRef.getName()) + "( dis." + typeInfo.unmarshalMethod + "() );");
        }

        public void writeEqualityTestStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            pw.println("\tif( ! (" + anAttribute.getName() + " == rhs." + anAttribute.getName() + " ) ) ivarsEqual = false;");
        }

        public void writeHashCodeStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            pw.println("\thash = hash * magicNumber + " + "Integer.valueOf( " + formatVariableName(anAttribute.getName()) + ".toInt() ).hashCode();");
        }

        public void writeLengthRetrievalStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            EnumRefAttribute enumRef = (EnumRefAttribute) anAttribute;
            pw.println("\tresult += " + sizeof(enumRef.getSerializedType()) + "; // " + formatVariableName(anAttribute.getName()));
        }
    }

    protected class JavaFixedLengthListGenerator extends JavaAttributeGenerator {

        protected String getListAPIType(FixedListAttribute fixedList) {
            if (fixedList.getElementTypeIsPrimitive()) return types.get(fixedList.getListElementType()).apiType; else return fixedList.getListElementType();
        }

        public void writeDeclaration(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            FixedListAttribute fixedList = (FixedListAttribute) anAttribute;
            int listLength = fixedList.getListLength();
            String listLengthString = Integer.toString(listLength);
            if (fixedList.getComment() != null && !fixedList.getComment().equals("")) {
                pw.println("/** " + fixedList.getComment() + " */");
            }
            pw.println("protected " + getListAPIType(fixedList) + "[] " + fixedList.getName() + " = new " + getListAPIType(fixedList) + "[" + listLengthString + "]" + ";\n");
        }

        public void writeGetterMethod(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            FixedListAttribute fixedList = (FixedListAttribute) anAttribute;
            pw.println("public " + getListAPIType(fixedList) + "[] get" + initialCap(fixedList.getName()) + "()");
            pw.println("{\n\treturn " + fixedList.getName() + ";\n}");
            pw.println();
        }

        public void writeSetterMethod(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            FixedListAttribute fixedList = (FixedListAttribute) anAttribute;
            String nameWithInitialCaps = initialCap(fixedList.getName());
            pw.println("public void set" + nameWithInitialCaps + "(" + getListAPIType(fixedList) + "[] p" + nameWithInitialCaps + ")");
            pw.println("{");
            if (fixedList.getElementTypeIsPrimitive()) {
                pw.println("\tArrays.fill( " + fixedList.getName() + ", (" + getListAPIType(fixedList) + ")0 );");
            } else {
                pw.println("\tArrays.fill( " + fixedList.getName() + ", null );");
            }
            pw.println("\tSystem.arraycopy( p" + nameWithInitialCaps + ", 0, " + fixedList.getName() + ", 0, " + "Math.min( p" + nameWithInitialCaps + ".length, " + fixedList.getName() + ".length ) );");
            pw.println("}");
            pw.println();
        }

        public void writeMarshallingStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            FixedListAttribute fixedList = (FixedListAttribute) anAttribute;
            pw.println("\t\tfor(int idx = 0; idx < " + fixedList.getName() + ".length; idx++)");
            pw.println("\t\t{");
            if (fixedList.getElementTypeIsPrimitive()) {
                PrimitiveTypeInfo typeInfo = types.get(fixedList.getListElementType());
                pw.println("\t\t\tdos." + typeInfo.marshalMethod + "( " + fixedList.getName() + "[idx] );");
            } else {
                pw.println("\t\t\t" + fixedList.getName() + "[idx].marshal( dos );");
            }
            pw.println("\t\t}");
        }

        public void writeUnmarshallingStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            FixedListAttribute fixedList = (FixedListAttribute) anAttribute;
            pw.println("\t\tfor( int idx = 0; idx < " + fixedList.getName() + ".length; idx++ )");
            pw.println("\t\t{");
            if (fixedList.getElementTypeIsPrimitive()) {
                PrimitiveTypeInfo typeInfo = types.get(fixedList.getListElementType());
                pw.println("\t\t\t" + fixedList.getName() + "[idx] = (" + typeInfo.apiType + ")dis." + typeInfo.unmarshalMethod + "();");
            } else {
                pw.println("\t\t\t" + fixedList.getName() + "[idx].unmarshal( dis );");
            }
            pw.println("\t\t}");
        }

        public void writeEqualityTestStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            FixedListAttribute fixedList = (FixedListAttribute) anAttribute;
            pw.println();
            pw.println("\tfor( int idx = 0; idx < " + anAttribute.getName() + ".length; idx++ )");
            pw.println("\t{");
            if (fixedList.getElementTypeIsPrimitive()) {
                pw.println("\t\tif( ! ( " + anAttribute.getName() + "[idx] == rhs." + anAttribute.getName() + "[idx] ) )");
            } else {
                pw.println("\t\tif( ! ( " + anAttribute.getName() + "[idx].equals( rhs." + anAttribute.getName() + "[idx] ) ) )");
            }
            pw.println("\t\t\tivarsEqual = false;");
            pw.println("\t}");
            pw.println();
        }

        public void writeHashCodeStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            FixedListAttribute fixedList = (FixedListAttribute) anAttribute;
            pw.println("\tfor( int idx = 0; idx < " + formatVariableName(anAttribute.getName()) + ".length; idx++ )");
            pw.println("\t{");
            if (fixedList.getElementTypeIsPrimitive()) {
                PrimitiveTypeInfo typeInfo = types.get(fixedList.getListElementType());
                pw.println("\t\thash = hash * magicNumber + " + typeInfo.wrapperType + ".valueOf( " + formatVariableName(anAttribute.getName()) + "[idx] ).hashCode();");
            } else {
                pw.println("\t\thash = hash * magicNumber + " + formatVariableName(anAttribute.getName()) + "[idx].hashCode();");
            }
            pw.println("\t}");
        }

        public void writeLengthRetrievalStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            FixedListAttribute fixedList = (FixedListAttribute) anAttribute;
            String name = formatVariableName(anAttribute.getName());
            if (fixedList.getElementTypeIsPrimitive()) {
                pw.println("\tresult += " + sizeof(fixedList.getListElementType()) + " * " + name + ".length; // " + name);
            } else {
                pw.println("\t// " + name);
                pw.println("\tfor( int idx = 0; idx < " + name + ".length; idx++ )");
                pw.println("\t{");
                pw.println("\t\tresult += " + name + "[idx].length();");
                pw.println("\t}");
            }
        }
    }

    protected class JavaVariableLengthListGenerator extends JavaAttributeGenerator {

        protected String getListType(VariableListAttribute variableList) {
            if (variableList.getElementTypeIsPrimitive()) return "List<" + types.get(variableList.getListElementType()).wrapperType + ">"; else return "List<" + variableList.getListElementType() + ">";
        }

        protected String getArrayListType(VariableListAttribute variableList) {
            if (variableList.getElementTypeIsPrimitive()) return "ArrayList<" + types.get(variableList.getListElementType()).wrapperType + ">"; else return "ArrayList<" + variableList.getListElementType() + ">";
        }

        public void writeDeclaration(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            VariableListAttribute variableList = (VariableListAttribute) anAttribute;
            if (variableList.getComment() != null && !variableList.getComment().equals("")) {
                pw.println("/** " + variableList.getComment() + " */");
            }
            pw.println("protected " + getListType(variableList) + " " + variableList.getName() + " = new " + getArrayListType(variableList) + "();");
        }

        public void writeGetterMethod(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            VariableListAttribute variableList = (VariableListAttribute) anAttribute;
            pw.println("public " + getListType(variableList) + " get" + initialCap(variableList.getName()) + "()");
            pw.println("{\n\treturn " + variableList.getName() + ";\n}");
            pw.println();
        }

        public void writeSetterMethod(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            VariableListAttribute variableList = (VariableListAttribute) anAttribute;
            String nameWithInitialCaps = initialCap(variableList.getName());
            pw.println("public void set" + nameWithInitialCaps + "( " + getListType(variableList) + " p" + nameWithInitialCaps + " )");
            pw.println("{\n\t" + variableList.getName() + " = p" + nameWithInitialCaps + ";");
            pw.println("}");
            pw.println();
        }

        public void writeMarshallingStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            VariableListAttribute variableList = (VariableListAttribute) anAttribute;
            if (variableList.getElementTypeIsPrimitive()) {
                PrimitiveTypeInfo typeInfo = types.get(variableList.getListElementType());
                pw.print("\t\tfor( " + typeInfo.wrapperType);
                pw.println(" element : " + variableList.getName() + " )");
                pw.println("\t\t{");
                pw.println("\t\t\tdos." + typeInfo.marshalMethod + "( element." + typeInfo.apiType + "Value() );");
                pw.println("\t\t}");
            } else {
                pw.print("\t\tfor( " + variableList.getListElementType());
                pw.println(" element : " + variableList.getName() + " )");
                pw.println("\t\t{");
                pw.println("\t\t\telement.marshal( dos );");
                pw.println("\t\t}");
            }
        }

        public void writeUnmarshallingStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            VariableListAttribute variableList = (VariableListAttribute) anAttribute;
            pw.println("\t\tfor( int idx = 0; idx < " + variableList.getCountFieldName() + "; idx++ )");
            pw.println("\t\t{");
            if (variableList.getElementTypeIsPrimitive()) {
                PrimitiveTypeInfo typeInfo = types.get(variableList.getListElementType());
                pw.println("\t\t\t" + typeInfo.marshalType + " temp = (" + typeInfo.marshalType + ")dis." + typeInfo.unmarshalMethod + "();");
                pw.println("\t\t\t" + variableList.getName() + ".add( new " + typeInfo.wrapperType + "( temp ) );");
            } else {
                pw.println("\t\t\t" + variableList.getListElementType() + " temp = new " + variableList.getListElementType() + "();");
                pw.println("\t\t\ttemp.unmarshal( dis );");
                pw.println("\t\t\t" + variableList.getName() + ".add( temp );");
            }
            pw.println("\t\t}");
            pw.println();
        }

        public void writeEqualityTestStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            VariableListAttribute variableList = (VariableListAttribute) anAttribute;
            String name = formatVariableName(anAttribute.getName());
            pw.println();
            pw.println("\tif( " + name + ".size() != rhs." + name + ".size() ) ivarsEqual = false;");
            pw.println("\tfor( int idx = 0; idx < " + name + ".size(); idx++ )");
            pw.println("\t{");
            if (variableList.getElementTypeIsPrimitive()) {
                pw.println("\t\tif( ! ( " + name + ".get(idx) == rhs." + name + ".get(idx) ) )");
            } else {
                pw.println("\t\tif( ! ( " + name + ".get(idx).equals(rhs." + name + ".get(idx)) ) )");
            }
            pw.println("\t\t\tivarsEqual = false;");
            pw.println("\t}");
            pw.println();
        }

        public void writeHashCodeStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            VariableListAttribute variableList = (VariableListAttribute) anAttribute;
            if (variableList.getElementTypeIsPrimitive()) {
                PrimitiveTypeInfo typeInfo = types.get(variableList.getListElementType());
                pw.print("\tfor( " + typeInfo.wrapperType);
                pw.println(" element : " + formatVariableName(anAttribute.getName()) + " )");
                pw.println("\t{");
                pw.println("\t\thash = hash * magicNumber + element.hashCode();");
                pw.println("\t}");
            } else {
                pw.print("\tfor( " + variableList.getListElementType());
                pw.println(" element : " + formatVariableName(anAttribute.getName()) + " )");
                pw.println("\t{");
                pw.println("\t\thash = hash * magicNumber + element.hashCode();");
                pw.println("\t}");
            }
        }

        public void writeLengthRetrievalStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            VariableListAttribute variableList = (VariableListAttribute) anAttribute;
            String name = formatVariableName(anAttribute.getName());
            if (variableList.getElementTypeIsPrimitive()) {
                pw.println("\tresult += " + sizeof(variableList.getListElementType()) + " * " + name + ".size(); // " + name);
            } else {
                pw.println("\t// " + name);
                pw.println("\tfor( " + variableList.getListElementType() + " element : " + name + " )");
                pw.println("\t{");
                pw.println("\t\tresult += element.length();");
                pw.println("\t}");
            }
        }
    }

    protected class JavaBitfieldGenerator extends JavaAttributeGenerator {

        HashMap<ClassAttribute.ClassAttributeType, JavaAttributeGenerator> writers = new HashMap<ClassAttribute.ClassAttributeType, JavaAttributeGenerator>();

        public JavaBitfieldGenerator() {
            writers.put(ClassAttribute.ClassAttributeType.PADDING, new JavaPaddingGenerator());
            writers.put(ClassAttribute.ClassAttributeType.PRIMITIVE, new JavaPrimitiveGenerator());
            writers.put(ClassAttribute.ClassAttributeType.ENUMREF, new JavaEnumRefGenerator());
            writers.put(ClassAttribute.ClassAttributeType.BOOLEAN, new JavaBooleanGenerator());
        }

        protected int createUnshiftedMask(int numConsecutiveOnes) {
            return (0x1 << numConsecutiveOnes) - 1;
        }

        public void writeDeclaration(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            BitfieldAttribute bitfield = (BitfieldAttribute) anAttribute;
            List<ClassAttribute> entries = bitfield.getBitfieldEntries();
            for (ClassAttribute entry : entries) {
                JavaAttributeGenerator writer = writers.get(entry.getAttributeKind());
                writer.writeDeclaration(pw, aClass, entry);
            }
        }

        public void writeGetterMethod(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            BitfieldAttribute bitfield = (BitfieldAttribute) anAttribute;
            List<ClassAttribute> entries = bitfield.getBitfieldEntries();
            for (ClassAttribute entry : entries) {
                JavaAttributeGenerator writer = writers.get(entry.getAttributeKind());
                writer.writeGetterMethod(pw, aClass, entry);
            }
        }

        public void writeSetterMethod(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            BitfieldAttribute bitfield = (BitfieldAttribute) anAttribute;
            List<ClassAttribute> entries = bitfield.getBitfieldEntries();
            for (ClassAttribute entry : entries) {
                JavaAttributeGenerator writer = writers.get(entry.getAttributeKind());
                writer.writeSetterMethod(pw, aClass, entry);
            }
        }

        public void writeMarshallingStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            BitfieldAttribute bitfield = (BitfieldAttribute) anAttribute;
            PrimitiveTypeInfo typeInfo = types.get(bitfield.getSerializedType());
            pw.println("\t\t{");
            pw.println("\t\t\t" + typeInfo.apiType + " temp = 0;");
            int entryOffset = 8;
            List<ClassAttribute> entries = bitfield.getBitfieldEntries();
            for (ClassAttribute entry : entries) {
                entryOffset -= ((BitfieldAttribute.Entry) entry).getNumberOfBits();
                pw.print("\t\t\ttemp = (" + typeInfo.apiType + ")( temp | ( ( ");
                if (entry.getAttributeKind() == ClassAttribute.ClassAttributeType.ENUMREF) {
                    pw.print(entry.getName() + ".toInt()");
                } else if (entry.getAttributeKind() == ClassAttribute.ClassAttributeType.PRIMITIVE) {
                    pw.print(entry.getName());
                } else if (entry.getAttributeKind() == ClassAttribute.ClassAttributeType.BOOLEAN) {
                    pw.print("(" + entry.getName() + "?1:0)");
                } else if (entry.getAttributeKind() == ClassAttribute.ClassAttributeType.PADDING) {
                    pw.print("0");
                }
                pw.print(" & " + createUnshiftedMask(((BitfieldAttribute.Entry) entry).getNumberOfBits()) + " ) ");
                pw.println("<< " + entryOffset + " ) );");
            }
            pw.println("\t\t\tdos." + typeInfo.marshalMethod + "( (" + typeInfo.marshalType + ")temp );");
            pw.println("\t\t}");
        }

        public void writeUnmarshallingStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            BitfieldAttribute bitfield = (BitfieldAttribute) anAttribute;
            PrimitiveTypeInfo typeInfo = types.get(bitfield.getSerializedType());
            pw.println("\t\t{");
            pw.println("\t\t\t" + typeInfo.apiType + " temp = (" + typeInfo.apiType + ")dis." + typeInfo.unmarshalMethod + "();");
            int entryOffset = 8;
            List<ClassAttribute> entries = bitfield.getBitfieldEntries();
            for (ClassAttribute entry : entries) {
                entryOffset -= ((BitfieldAttribute.Entry) entry).getNumberOfBits();
                String decodingString = "( temp >>> " + entryOffset + " ) & " + createUnshiftedMask(((BitfieldAttribute.Entry) entry).getNumberOfBits());
                if (entry.getAttributeKind() == ClassAttribute.ClassAttributeType.ENUMREF) {
                    pw.println("\t\t\t" + "set" + initialCap(entry.getName()) + "( " + decodingString + " );");
                } else if (entry.getAttributeKind() == ClassAttribute.ClassAttributeType.PRIMITIVE) {
                    BitfieldAttribute.IntegerEntry intEntry = (BitfieldAttribute.IntegerEntry) entry;
                    PrimitiveTypeInfo intTypeInfo = types.get(intEntry.getPrimitiveType());
                    pw.println("\t\t\t" + entry.getName() + " = (" + intTypeInfo.apiType + ")( " + decodingString + " );");
                } else if (entry.getAttributeKind() == ClassAttribute.ClassAttributeType.BOOLEAN) {
                    pw.println("\t\t\t" + entry.getName() + " = ( " + decodingString + " ) != 0;");
                } else if (entry.getAttributeKind() == ClassAttribute.ClassAttributeType.PADDING) {
                }
            }
            pw.println("\t\t}");
        }

        public void writeEqualityTestStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            BitfieldAttribute bitfield = (BitfieldAttribute) anAttribute;
            List<ClassAttribute> entries = bitfield.getBitfieldEntries();
            for (ClassAttribute entry : entries) {
                JavaAttributeGenerator writer = writers.get(entry.getAttributeKind());
                writer.writeEqualityTestStatement(pw, aClass, entry);
            }
        }

        public void writeHashCodeStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            BitfieldAttribute bitfield = (BitfieldAttribute) anAttribute;
            List<ClassAttribute> entries = bitfield.getBitfieldEntries();
            for (ClassAttribute entry : entries) {
                JavaAttributeGenerator writer = writers.get(entry.getAttributeKind());
                writer.writeHashCodeStatement(pw, aClass, entry);
            }
        }

        public void writeLengthRetrievalStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            BitfieldAttribute bitfield = (BitfieldAttribute) anAttribute;
            pw.println("\tresult += " + sizeof(bitfield.getSerializedType()) + "; // " + formatVariableName(anAttribute.getName()));
        }
    }

    protected class JavaBooleanGenerator extends JavaAttributeGenerator {

        public void writeDeclaration(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            BooleanAttribute boolAttr = (BooleanAttribute) anAttribute;
            if (boolAttr.getComment() != null && !boolAttr.getComment().equals("")) {
                pw.println("/** " + boolAttr.getComment() + " */");
            }
            pw.print("protected boolean " + boolAttr.getName() + " = ");
            if (boolAttr.getDefaultValue()) pw.print("true"); else pw.print("false");
            pw.println(";\n");
        }

        public void writeGetterMethod(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            BooleanAttribute boolAttr = (BooleanAttribute) anAttribute;
            pw.println("public boolean get" + initialCap(boolAttr.getName()) + "()");
            pw.println("{\n\treturn " + boolAttr.getName() + ";\n}");
            pw.println();
        }

        public void writeSetterMethod(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            BooleanAttribute boolAttr = (BooleanAttribute) anAttribute;
            String nameWithInitialCaps = initialCap(boolAttr.getName());
            pw.println("public void set" + nameWithInitialCaps + "( boolean p" + nameWithInitialCaps + " )");
            pw.println("{\n\t" + boolAttr.getName() + " = p" + nameWithInitialCaps + ";");
            pw.println("}");
            pw.println();
        }

        public void writeMarshallingStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            BooleanAttribute boolAttr = (BooleanAttribute) anAttribute;
            PrimitiveTypeInfo typeInfo = types.get(boolAttr.getSerializedType());
            pw.println("\t\tdos." + typeInfo.marshalMethod + "( " + boolAttr.getName() + "?1:0 );");
        }

        public void writeUnmarshallingStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            BooleanAttribute boolAttr = (BooleanAttribute) anAttribute;
            PrimitiveTypeInfo typeInfo = types.get(boolAttr.getSerializedType());
            pw.println("\t\t" + boolAttr.getName() + " = dis." + typeInfo.unmarshalMethod + "() != 0;");
        }

        public void writeEqualityTestStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            pw.println("\tif( ! (" + anAttribute.getName() + " == rhs." + anAttribute.getName() + " ) ) ivarsEqual = false;");
        }

        public void writeHashCodeStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            pw.println("\thash = hash * magicNumber + " + "Boolean.valueOf( " + formatVariableName(anAttribute.getName()) + " ).hashCode();");
        }

        public void writeLengthRetrievalStatement(PrintWriter pw, GeneratedClass aClass, ClassAttribute anAttribute) {
            BooleanAttribute boolAttr = (BooleanAttribute) anAttribute;
            pw.println("\tresult += " + sizeof(boolAttr.getSerializedType()) + "; // " + formatVariableName(boolAttr.getName()));
        }
    }
}
