package com.unboundid.ldap.sdk.persist;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.TreeSet;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.Version;
import com.unboundid.ldap.sdk.schema.AttributeTypeDefinition;
import com.unboundid.ldap.sdk.schema.ObjectClassDefinition;
import com.unboundid.ldap.sdk.schema.ObjectClassType;
import com.unboundid.ldap.sdk.schema.Schema;
import com.unboundid.util.LDAPCommandLineTool;
import com.unboundid.util.Mutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.BooleanArgument;
import com.unboundid.util.args.DNArgument;
import com.unboundid.util.args.FileArgument;
import com.unboundid.util.args.StringArgument;
import static com.unboundid.ldap.sdk.persist.PersistMessages.*;
import static com.unboundid.util.Debug.*;
import static com.unboundid.util.StaticUtils.*;

/**
 * This class provides a tool which can be used to generate source code for a
 * Java class file based on information read from the schema of an LDAP
 * directory server.
 */
@Mutable()
@ThreadSafety(level = ThreadSafetyLevel.NOT_THREADSAFE)
public final class GenerateSourceFromSchema extends LDAPCommandLineTool implements Serializable {

    /**
   * The serial version UID for this serializable class.
   */
    private static final long serialVersionUID = 3488976364950590266L;

    /**
   * A pre-allocated empty tree set.
   */
    private static final TreeSet<String> EMPTY_TREE_SET = new TreeSet<String>();

    private BooleanArgument terseArg;

    private DNArgument defaultParentDNArg;

    private FileArgument outputDirectoryArg;

    private StringArgument auxiliaryClassArg;

    private StringArgument classNameArg;

    private StringArgument lazyAttributeArg;

    private StringArgument operationalAttributeArg;

    private StringArgument packageNameArg;

    private StringArgument rdnAttributeArg;

    private StringArgument structuralClassArg;

    private boolean needArrays;

    private boolean needDate;

    private boolean needDN;

    private boolean needPersistedObjects;

    /**
   * Parse the provided command line arguments and perform the appropriate
   * processing.
   *
   * @param  args  The command line arguments provided to this program.
   */
    public static void main(final String[] args) {
        final ResultCode resultCode = main(args, System.out, System.err);
        if (resultCode != ResultCode.SUCCESS) {
            System.exit(resultCode.intValue());
        }
    }

    /**
   * Parse the provided command line arguments and perform the appropriate
   * processing.
   *
   * @param  args       The command line arguments provided to this program.
   * @param  outStream  The output stream to which standard out should be
   *                    written.  It may be {@code null} if output should be
   *                    suppressed.
   * @param  errStream  The output stream to which standard error should be
   *                    written.  It may be {@code null} if error messages
   *                    should be suppressed.
   *
   * @return  A result code indicating whether the processing was successful.
   */
    public static ResultCode main(final String[] args, final OutputStream outStream, final OutputStream errStream) {
        final GenerateSourceFromSchema tool = new GenerateSourceFromSchema(outStream, errStream);
        return tool.runTool(args);
    }

    /**
   * Creates a new instance of this tool.
   *
   * @param  outStream  The output stream to which standard out should be
   *                    written.  It may be {@code null} if output should be
   *                    suppressed.
   * @param  errStream  The output stream to which standard error should be
   *                    written.  It may be {@code null} if error messages
   *                    should be suppressed.
   */
    public GenerateSourceFromSchema(final OutputStream outStream, final OutputStream errStream) {
        super(outStream, errStream);
        needArrays = false;
        needDate = false;
        needDN = false;
        needPersistedObjects = false;
    }

    /**
   * {@inheritDoc}
   */
    @Override()
    public String getToolName() {
        return "generate-source-from-schema";
    }

    /**
   * {@inheritDoc}
   */
    @Override()
    public String getToolDescription() {
        return INFO_GEN_SOURCE_TOOL_DESCRIPTION.get();
    }

    /**
   * Retrieves the version string for this tool.
   *
   * @return  The version string for this tool.
   */
    @Override()
    public String getToolVersion() {
        return Version.NUMERIC_VERSION_STRING;
    }

    /**
   * {@inheritDoc}
   */
    @Override()
    public void addNonLDAPArguments(final ArgumentParser parser) throws ArgumentException {
        outputDirectoryArg = new FileArgument('d', "outputDirectory", false, 1, INFO_GEN_SOURCE_VALUE_PLACEHOLDER_PATH.get(), INFO_GEN_SOURCE_ARG_DESCRIPTION_OUTPUT_DIRECTORY.get(), true, true, false, true);
        parser.addArgument(outputDirectoryArg);
        structuralClassArg = new StringArgument('s', "structuralClass", true, 1, INFO_GEN_SOURCE_VALUE_PLACEHOLDER_NAME.get(), INFO_GEN_SOURCE_ARG_DESCRIPTION_STRUCTURAL_CLASS.get());
        parser.addArgument(structuralClassArg);
        auxiliaryClassArg = new StringArgument('a', "auxiliaryClass", false, 0, INFO_GEN_SOURCE_VALUE_PLACEHOLDER_NAME.get(), INFO_GEN_SOURCE_ARG_DESCRIPTION_AUXILIARY_CLASS.get());
        parser.addArgument(auxiliaryClassArg);
        rdnAttributeArg = new StringArgument('r', "rdnAttribute", true, 0, INFO_GEN_SOURCE_VALUE_PLACEHOLDER_NAME.get(), INFO_GEN_SOURCE_ARG_DESCRIPTION_RDN_ATTRIBUTE.get());
        parser.addArgument(rdnAttributeArg);
        lazyAttributeArg = new StringArgument('l', "lazyAttribute", false, 0, INFO_GEN_SOURCE_VALUE_PLACEHOLDER_NAME.get(), INFO_GEN_SOURCE_ARG_DESCRIPTION_LAZY_ATTRIBUTE.get());
        parser.addArgument(lazyAttributeArg);
        operationalAttributeArg = new StringArgument('O', "operationalAttribute", false, 0, INFO_GEN_SOURCE_VALUE_PLACEHOLDER_NAME.get(), INFO_GEN_SOURCE_ARG_DESCRIPTION_OPERATIONAL_ATTRIBUTE.get());
        parser.addArgument(operationalAttributeArg);
        defaultParentDNArg = new DNArgument('b', "defaultParentDN", false, 1, INFO_GEN_SOURCE_VALUE_PLACEHOLDER_DN.get(), INFO_GEN_SOURCE_ARG_DESCRIPTION_DEFAULT_PARENT_DN.get());
        parser.addArgument(defaultParentDNArg);
        packageNameArg = new StringArgument('n', "packageName", false, 1, INFO_GEN_SOURCE_VALUE_PLACEHOLDER_NAME.get(), INFO_GEN_SOURCE_ARG_DESCRIPTION_PACKAGE_NAME.get());
        parser.addArgument(packageNameArg);
        classNameArg = new StringArgument('c', "className", false, 1, INFO_GEN_SOURCE_VALUE_PLACEHOLDER_NAME.get(), INFO_GEN_SOURCE_ARG_DESCRIPTION_CLASS_NAME.get());
        parser.addArgument(classNameArg);
        terseArg = new BooleanArgument('t', "terse", 1, INFO_GEN_SOURCE_ARG_DESCRIPTION_TERSE.get());
        parser.addArgument(terseArg);
    }

    /**
   * {@inheritDoc}
   */
    @Override()
    public ResultCode doToolProcessing() {
        final LDAPConnection conn;
        try {
            conn = getConnection();
        } catch (LDAPException le) {
            debugException(le);
            err(ERR_GEN_SOURCE_CANNOT_CONNECT.get(getExceptionMessage(le)));
            return le.getResultCode();
        }
        final Schema schema;
        try {
            schema = conn.getSchema();
            if (schema == null) {
                err(ERR_GEN_SOURCE_CANNOT_READ_SCHEMA.get(ERR_GEN_SOURCE_SCHEMA_NOT_RETURNED.get()));
                return ResultCode.NO_RESULTS_RETURNED;
            }
        } catch (LDAPException le) {
            debugException(le);
            err(ERR_GEN_SOURCE_CANNOT_READ_SCHEMA.get(getExceptionMessage(le)));
            return le.getResultCode();
        } finally {
            conn.close();
        }
        return generateSourceFile(schema, terseArg.isPresent());
    }

    /**
   * Generates the source file using the information in the provided schema.
   *
   * @param  schema  The schema to use to generate the source file.
   * @param  terse   Indicates whether to use terse mode when generating the
   *                 source file.  If this is {@code true}, then all optional
   *                 elements will be omitted from annotations.
   *
   * @return  A result code obtained for the processing.
   */
    private ResultCode generateSourceFile(final Schema schema, final boolean terse) {
        final TreeMap<String, AttributeTypeDefinition> requiredAttrs = new TreeMap<String, AttributeTypeDefinition>();
        final TreeMap<String, AttributeTypeDefinition> optionalAttrs = new TreeMap<String, AttributeTypeDefinition>();
        final TreeMap<String, TreeSet<String>> requiredAttrOCs = new TreeMap<String, TreeSet<String>>();
        final TreeMap<String, TreeSet<String>> optionalAttrOCs = new TreeMap<String, TreeSet<String>>();
        final TreeMap<String, String> types = new TreeMap<String, String>();
        final String structuralClassName = structuralClassArg.getValue();
        final ObjectClassDefinition structuralOC = schema.getObjectClass(structuralClassName);
        if (structuralOC == null) {
            err(ERR_GEN_SOURCE_STRUCTURAL_CLASS_NOT_FOUND.get(structuralClassName));
            return ResultCode.PARAM_ERROR;
        }
        if (structuralOC.getObjectClassType(schema) != ObjectClassType.STRUCTURAL) {
            err(ERR_GEN_SOURCE_STRUCTURAL_CLASS_NOT_STRUCTURAL.get(structuralClassName));
            return ResultCode.PARAM_ERROR;
        }
        processObjectClass(structuralOC, schema, requiredAttrs, requiredAttrOCs, optionalAttrs, optionalAttrOCs, types);
        final TreeMap<String, ObjectClassDefinition> auxiliaryOCs = new TreeMap<String, ObjectClassDefinition>();
        if (auxiliaryClassArg.isPresent()) {
            for (final String s : auxiliaryClassArg.getValues()) {
                final ObjectClassDefinition oc = schema.getObjectClass(s);
                if (oc == null) {
                    err(ERR_GEN_SOURCE_AUXILIARY_CLASS_NOT_FOUND.get(s));
                    return ResultCode.PARAM_ERROR;
                }
                if (oc.getObjectClassType(schema) != ObjectClassType.AUXILIARY) {
                    err(ERR_GEN_SOURCE_AUXILIARY_CLASS_NOT_AUXILIARY.get(s));
                    return ResultCode.PARAM_ERROR;
                }
                auxiliaryOCs.put(toLowerCase(s), oc);
                processObjectClass(oc, schema, requiredAttrs, requiredAttrOCs, optionalAttrs, optionalAttrOCs, types);
            }
        }
        final TreeMap<String, ObjectClassDefinition> superiorOCs = new TreeMap<String, ObjectClassDefinition>();
        for (final ObjectClassDefinition s : structuralOC.getSuperiorClasses(schema, true)) {
            superiorOCs.put(toLowerCase(s.getNameOrOID()), s);
        }
        for (final ObjectClassDefinition d : auxiliaryOCs.values()) {
            for (final ObjectClassDefinition s : d.getSuperiorClasses(schema, true)) {
                superiorOCs.put(toLowerCase(s.getNameOrOID()), s);
            }
        }
        superiorOCs.remove(toLowerCase(structuralClassName));
        for (final String s : auxiliaryOCs.keySet()) {
            superiorOCs.remove(s);
        }
        final TreeMap<String, AttributeTypeDefinition> operationalAttrs = new TreeMap<String, AttributeTypeDefinition>();
        if (operationalAttributeArg.isPresent()) {
            for (final String s : operationalAttributeArg.getValues()) {
                final AttributeTypeDefinition d = schema.getAttributeType(s);
                if (d == null) {
                    err(ERR_GEN_SOURCE_OPERATIONAL_ATTRIBUTE_NOT_DEFINED.get(s));
                    return ResultCode.PARAM_ERROR;
                } else if (!d.isOperational()) {
                    err(ERR_GEN_SOURCE_OPERATIONAL_ATTRIBUTE_NOT_OPERATIONAL.get(s));
                    return ResultCode.PARAM_ERROR;
                } else {
                    final String lowerName = toLowerCase(s);
                    operationalAttrs.put(lowerName, d);
                    types.put(lowerName, getJavaType(schema, d));
                }
            }
        }
        final TreeSet<String> rdnAttrs = new TreeSet<String>();
        for (final String s : rdnAttributeArg.getValues()) {
            final AttributeTypeDefinition d = schema.getAttributeType(s);
            if (d == null) {
                err(ERR_GEN_SOURCE_RDN_ATTRIBUTE_NOT_DEFINED.get(s));
                return ResultCode.PARAM_ERROR;
            }
            final String lowerName = toLowerCase(d.getNameOrOID());
            rdnAttrs.add(lowerName);
            if (requiredAttrs.containsKey(lowerName)) {
            } else if (optionalAttrs.containsKey(lowerName)) {
                requiredAttrs.put(lowerName, optionalAttrs.remove(lowerName));
                requiredAttrOCs.put(lowerName, optionalAttrOCs.remove(lowerName));
            } else {
                err(ERR_GEN_SOURCE_RDN_ATTRIBUTE_NOT_DEFINED.get(s));
                return ResultCode.PARAM_ERROR;
            }
        }
        final TreeSet<String> lazyAttrs = new TreeSet<String>();
        for (final String s : lazyAttributeArg.getValues()) {
            final AttributeTypeDefinition d = schema.getAttributeType(s);
            if (d == null) {
                err(ERR_GEN_SOURCE_LAZY_ATTRIBUTE_NOT_DEFINED.get(s));
                return ResultCode.PARAM_ERROR;
            }
            final String lowerName = toLowerCase(d.getNameOrOID());
            lazyAttrs.add(lowerName);
            if (requiredAttrs.containsKey(lowerName) || optionalAttrs.containsKey(lowerName) || operationalAttrs.containsKey(lowerName)) {
            } else {
                err(ERR_GEN_SOURCE_LAZY_ATTRIBUTE_NOT_ALLOWED.get(s));
                return ResultCode.PARAM_ERROR;
            }
        }
        final String className;
        if (classNameArg.isPresent()) {
            className = classNameArg.getValue();
            final StringBuilder invalidReason = new StringBuilder();
            if (!PersistUtils.isValidJavaIdentifier(className, invalidReason)) {
                err(ERR_GEN_SOURCE_INVALID_CLASS_NAME.get(className, invalidReason.toString()));
                return ResultCode.PARAM_ERROR;
            }
        } else {
            className = capitalize(PersistUtils.toJavaIdentifier(structuralClassName));
        }
        final File sourceFile = new File(outputDirectoryArg.getValue(), className + ".java");
        final PrintWriter writer;
        try {
            writer = new PrintWriter(new FileWriter(sourceFile));
        } catch (Exception e) {
            debugException(e);
            err(ERR_GEN_SOURCE_CANNOT_CREATE_WRITER.get(sourceFile.getAbsolutePath(), getExceptionMessage(e)));
            return ResultCode.LOCAL_ERROR;
        }
        if (packageNameArg.isPresent()) {
            final String packageName = packageNameArg.getValue();
            if (packageName.length() > 0) {
                writer.println("package " + packageName + ';');
                writer.println();
                writer.println();
                writer.println();
            }
        }
        boolean javaImports = false;
        if (needArrays) {
            writer.println("import " + Arrays.class.getName() + ';');
            javaImports = true;
        }
        if (needDate) {
            writer.println("import " + Date.class.getName() + ';');
            javaImports = true;
        }
        if (javaImports) {
            writer.println();
        }
        if (needDN) {
            writer.println("import " + DN.class.getName() + ';');
        }
        writer.println("import " + Entry.class.getName() + ';');
        writer.println("import " + Filter.class.getName() + ';');
        if (needDN) {
            writer.println("import " + LDAPException.class.getName() + ';');
            writer.println("import " + LDAPInterface.class.getName() + ';');
        }
        writer.println("import " + ReadOnlyEntry.class.getName() + ';');
        writer.println("import " + DefaultObjectEncoder.class.getName() + ';');
        writer.println("import " + FieldInfo.class.getName() + ';');
        writer.println("import " + FilterUsage.class.getName() + ';');
        writer.println("import " + LDAPEntryField.class.getName() + ';');
        writer.println("import " + LDAPField.class.getName() + ';');
        writer.println("import " + LDAPObject.class.getName() + ';');
        writer.println("import " + LDAPObjectHandler.class.getName() + ';');
        writer.println("import " + LDAPPersister.class.getName() + ';');
        writer.println("import " + LDAPPersistException.class.getName() + ';');
        if (needPersistedObjects) {
            writer.println("import " + PersistedObjects.class.getName() + ';');
        }
        writer.println("import " + PersistFilterType.class.getName() + ';');
        if (needDN) {
            writer.println("import " + PersistUtils.class.getName() + ';');
        }
        writer.println();
        writer.println();
        writer.println();
        writer.println("/**");
        writer.println(" * This class provides an implementation of an object " + "that can be used to");
        writer.println(" * represent " + structuralClassName + " objects in the directory.");
        writer.println(" * It was generated by the " + getToolName() + " tool provided with the");
        writer.println(" * UnboundID LDAP SDK for Java.  It " + "may be customized as desired to better suit");
        writer.println(" * your needs.");
        writer.println(" */");
        writer.println("@LDAPObject(structuralClass=\"" + structuralClassName + "\",");
        switch(auxiliaryOCs.size()) {
            case 0:
                break;
            case 1:
                writer.println("            auxiliaryClass=\"" + auxiliaryOCs.values().iterator().next().getNameOrOID() + "\",");
                break;
            default:
                final Iterator<ObjectClassDefinition> iterator = auxiliaryOCs.values().iterator();
                writer.println("            auxiliaryClass={ \"" + iterator.next().getNameOrOID() + "\",");
                while (iterator.hasNext()) {
                    final String ocName = iterator.next().getNameOrOID();
                    if (iterator.hasNext()) {
                        writer.println("                             \"" + ocName + "\",");
                    } else {
                        writer.println("                             \"" + ocName + "\" },");
                    }
                }
                break;
        }
        switch(superiorOCs.size()) {
            case 0:
                break;
            case 1:
                writer.println("            superiorClass=\"" + superiorOCs.values().iterator().next().getNameOrOID() + "\",");
                break;
            default:
                final Iterator<ObjectClassDefinition> iterator = superiorOCs.values().iterator();
                writer.println("            superiorClass={ \"" + iterator.next().getNameOrOID() + "\",");
                while (iterator.hasNext()) {
                    final String ocName = iterator.next().getNameOrOID();
                    if (iterator.hasNext()) {
                        writer.println("                             \"" + ocName + "\",");
                    } else {
                        writer.println("                             \"" + ocName + "\" },");
                    }
                }
                break;
        }
        if (defaultParentDNArg.isPresent()) {
            writer.println("            defaultParentDN=\"" + defaultParentDNArg.getValue() + "\",");
        }
        writer.println("            postDecodeMethod=\"doPostDecode\",");
        writer.println("            postEncodeMethod=\"doPostEncode\")");
        writer.println("public class " + className);
        writer.println("{");
        if (!terse) {
            writer.println("  /*");
            writer.println("   * NOTE:  This class includes a number of annotation " + "elements which are not");
            writer.println("   * required but have been provided to make it easier " + "to edit the resulting");
            writer.println("   * source code.  If you want to exclude these " + "unnecessary annotation");
            writer.println("   * elements, use the '--terse' command-line argument.");
            writer.println("   */");
            writer.println();
            writer.println();
            writer.println();
        }
        writer.println("  // The field to use to hold a read-only copy of the " + "associated entry.");
        writer.println("  @LDAPEntryField()");
        writer.println("  private ReadOnlyEntry ldapEntry;");
        for (final String lowerName : rdnAttrs) {
            final AttributeTypeDefinition d = requiredAttrs.get(lowerName);
            final TreeSet<String> ocNames = requiredAttrOCs.get(lowerName);
            writeField(writer, d, types.get(lowerName), ocNames, true, true, structuralClassName, false, terse);
        }
        for (final String lowerName : requiredAttrs.keySet()) {
            if (rdnAttrs.contains(lowerName)) {
                continue;
            }
            final AttributeTypeDefinition d = requiredAttrs.get(lowerName);
            final TreeSet<String> ocNames = requiredAttrOCs.get(lowerName);
            writeField(writer, d, types.get(lowerName), ocNames, false, true, structuralClassName, lazyAttrs.contains(lowerName), terse);
        }
        for (final String lowerName : optionalAttrs.keySet()) {
            final AttributeTypeDefinition d = optionalAttrs.get(lowerName);
            final TreeSet<String> ocNames = optionalAttrOCs.get(lowerName);
            writeField(writer, d, types.get(lowerName), ocNames, false, false, structuralClassName, lazyAttrs.contains(lowerName), terse);
        }
        for (final String lowerName : operationalAttrs.keySet()) {
            final AttributeTypeDefinition d = operationalAttrs.get(lowerName);
            final TreeSet<String> ocNames = EMPTY_TREE_SET;
            writeField(writer, d, types.get(lowerName), ocNames, false, false, structuralClassName, lazyAttrs.contains(lowerName), terse);
        }
        writer.println();
        writer.println();
        writer.println();
        writer.println("  /**");
        writer.println("   * Creates a new instance of this object.  All fields " + "will be uninitialized,");
        writer.println("   * so the setter methods should be used to assign " + "values to them.");
        writer.println("   */");
        writer.println("  public " + className + "()");
        writer.println("  {");
        writer.println("    // No initialization will be performed by default.  " + "Note that if you set");
        writer.println("    // values for any fields marked with an @LDAPField, " + "@LDAPDNField, or");
        writer.println("    // @LDAPEntryField annotation, they will be " + "overwritten in the course of");
        writer.println("    // decoding initializing this object from an LDAP " + "entry.");
        writer.println("  }");
        writer.println();
        writer.println();
        writer.println();
        writer.println("  /**");
        writer.println("   * Creates a new " + className + " object decoded");
        writer.println("   * from the provided entry.");
        writer.println("   *");
        writer.println("   * @param  entry  The entry to be decoded.");
        writer.println("   *");
        writer.println("   * @return  The decoded " + className + " object.");
        writer.println("   *");
        writer.println("   * @throws  LDAPPersistException  If a problem occurs " + "while attempting to");
        writer.println("   *                                decode the provided " + "entry.");
        writer.println("   */");
        writer.println("  public static " + className + " decode(final Entry entry)");
        writer.println("         throws LDAPPersistException");
        writer.println("  {");
        writer.println("    return getPersister().decode(entry);");
        writer.println("  }");
        writer.println("");
        writer.println("");
        writer.println("");
        writer.println("  /**");
        writer.println("   * Retrieves an {@code LDAPPersister} instance that " + "may be used to interact");
        writer.println("   * with objects of this type.");
        writer.println("   *");
        writer.println("   * @return  An {@code LDAPPersister} instance that may " + "be used to interact");
        writer.println("   *          with objects of this type.");
        writer.println("   *");
        writer.println("   * @throws  LDAPPersistException  If a problem occurs " + "while creating the");
        writer.println("   *                                " + "{@code LDAPPersister} instance.");
        writer.println("   */");
        writer.println("  public static LDAPPersister<" + className + "> getPersister()");
        writer.println("         throws LDAPPersistException");
        writer.println("  {");
        writer.println("    return LDAPPersister.getInstance(" + className + ".class);");
        writer.println("  }");
        writer.println();
        writer.println();
        writer.println();
        writer.println("  /**");
        writer.println("   * Performs any processing that may be necessary after " + "initializing this");
        writer.println("   * object from an LDAP entry.");
        writer.println("   *");
        writer.println("   * @throws  LDAPPersistException  If the generated " + "entry should not be used.");
        writer.println("   */");
        writer.println("  private void doPostDecode()");
        writer.println("          throws LDAPPersistException");
        writer.println("  {");
        writer.println("    // No processing is needed by default.  You may " + "provide an implementation");
        writer.println("    // for this method if custom post-decode processing " + "is needed.");
        writer.println("  }");
        writer.println();
        writer.println();
        writer.println();
        writer.println("  /**");
        writer.println("   * Performs any processing that may be necessary after " + "encoding this object");
        writer.println("   * to an LDAP entry.");
        writer.println("   *");
        writer.println("   * @param  entry  The entry that has been generated.  " + "It may be altered if");
        writer.println("   *                desired.");
        writer.println("   *");
        writer.println("   * @throws  LDAPPersistException  If there is a " + "problem with the object after");
        writer.println("   *                                it has been decoded " + "from an LDAP entry.");
        writer.println("   */");
        writer.println("  private void doPostEncode(final Entry entry)");
        writer.println("          throws LDAPPersistException");
        writer.println("  {");
        writer.println("    // No processing is needed by default.  You may " + "provide an implementation");
        writer.println("    // for this method if custom post-encode processing " + "is needed.");
        writer.println("  }");
        writer.println();
        writer.println();
        writer.println();
        writer.println("  /**");
        writer.println("   * Retrieves a read-only copy of the entry with which " + "this object is");
        writer.println("   * associated, if it is available.  It will only be " + "available if this object");
        writer.println("   * was decoded from or encoded to an LDAP entry.");
        writer.println("   *");
        writer.println("   * @return  A read-only copy of the entry with which " + "this object is");
        writer.println("   *          associated, or {@code null} if it is not " + "available.");
        writer.println("   */");
        writer.println("  public ReadOnlyEntry getLDAPEntry()");
        writer.println("  {");
        writer.println("    return ldapEntry;");
        writer.println("  }");
        writer.println();
        writer.println();
        writer.println();
        writer.println("  /**");
        writer.println("   * Retrieves the DN of the entry with which this " + "object is associated, if it");
        writer.println("   * is available.  It will only be available if this " + "object was decoded from or");
        writer.println("   * encoded to an LDAP entry.");
        writer.println("   *");
        writer.println("   * @return  The DN of the entry with which this object " + "is associated, or");
        writer.println("   *          {@code null} if it is not available.");
        writer.println("   */");
        writer.println("  public String getLDAPEntryDN()");
        writer.println("  {");
        writer.println("    if (ldapEntry == null)");
        writer.println("    {");
        writer.println("      return null;");
        writer.println("    }");
        writer.println("    else");
        writer.println("    {");
        writer.println("      return ldapEntry.getDN();");
        writer.println("    }");
        writer.println("  }");
        for (final String lowerName : rdnAttrs) {
            final AttributeTypeDefinition d = requiredAttrs.get(lowerName);
            writeFieldMethods(writer, d, types.get(lowerName), true);
        }
        for (final String lowerName : requiredAttrs.keySet()) {
            if (rdnAttrs.contains(lowerName)) {
                continue;
            }
            final AttributeTypeDefinition d = requiredAttrs.get(lowerName);
            writeFieldMethods(writer, d, types.get(lowerName), true);
        }
        for (final String lowerName : optionalAttrs.keySet()) {
            final AttributeTypeDefinition d = optionalAttrs.get(lowerName);
            writeFieldMethods(writer, d, types.get(lowerName), true);
        }
        for (final String lowerName : operationalAttrs.keySet()) {
            final AttributeTypeDefinition d = operationalAttrs.get(lowerName);
            writeFieldMethods(writer, d, types.get(lowerName), false);
        }
        writeToString(writer, className, requiredAttrs.values(), optionalAttrs.values(), operationalAttrs.values());
        writer.println("}");
        writer.println();
        writer.close();
        return ResultCode.SUCCESS;
    }

    /**
   * Performs an appropriate set of processing for the provided object class to
   * ensure that all of the required and optional attributes are classified
   * properly.
   *
   * @param  oc   The object class to process.
   * @param  s    The server schema.
   * @param  ra   The set of required attributes identified so far.
   * @param  rac  The object classes referenced by the required attributes.
   * @param  oa   The set of optional attributes identified so far.
   * @param  oac  The object classes referenced by the optional attributes.
   * @param  t    A map of attribute type names to Java types.
   */
    void processObjectClass(final ObjectClassDefinition oc, final Schema s, final TreeMap<String, AttributeTypeDefinition> ra, final TreeMap<String, TreeSet<String>> rac, final TreeMap<String, AttributeTypeDefinition> oa, final TreeMap<String, TreeSet<String>> oac, final TreeMap<String, String> t) {
        for (final AttributeTypeDefinition d : oc.getRequiredAttributes(s, true)) {
            if (d.hasNameOrOID("objectClass")) {
                continue;
            }
            final String lowerName = toLowerCase(d.getNameOrOID());
            if (ra.containsKey(lowerName)) {
                rac.get(lowerName).add(oc.getNameOrOID());
            } else if (oa.containsKey(lowerName)) {
                oa.remove(lowerName);
                ra.put(lowerName, d);
                final TreeSet<String> ocSet = oac.remove(lowerName);
                ocSet.add(oc.getNameOrOID());
                rac.put(lowerName, ocSet);
            } else {
                final TreeSet<String> ocSet = new TreeSet<String>();
                ocSet.add(oc.getNameOrOID());
                ra.put(lowerName, d);
                rac.put(lowerName, ocSet);
                t.put(lowerName, getJavaType(s, d));
            }
        }
        for (final AttributeTypeDefinition d : oc.getOptionalAttributes(s, true)) {
            if (d.hasNameOrOID("objectClass")) {
                continue;
            }
            final String lowerName = toLowerCase(d.getNameOrOID());
            if (ra.containsKey(lowerName)) {
                rac.get(lowerName).add(oc.getNameOrOID());
            } else if (oa.containsKey(lowerName)) {
                oac.get(lowerName).add(oc.getNameOrOID());
            } else {
                final TreeSet<String> ocSet = new TreeSet<String>();
                ocSet.add(oc.getNameOrOID());
                oa.put(lowerName, d);
                oac.put(lowerName, ocSet);
                t.put(lowerName, getJavaType(s, d));
            }
        }
    }

    /**
   * Writes information about a field to the Java class file.
   *
   * @param  writer    The writer to which the field information should be
   *                   written.
   * @param  d         The attribute type definition.
   * @param  type      The name of the Java type to use for the field.
   * @param  ocNames   The names of the object classes for the attribute type.
   * @param  inRDN     Indicates whether the attribute should be included in
   *                   generated entry RDNs.
   * @param  required  Indicates whether the attribute should be considered
   *                   required.
   * @param  sc        The name of the structural object class for the object.
   * @param  lazy      Indicates whether the field should be marked for lazy
   *                   loading.
   * @param  terse     Indicates whether to use terse mode.
   */
    static void writeField(final PrintWriter writer, final AttributeTypeDefinition d, final String type, final TreeSet<String> ocNames, final boolean inRDN, final boolean required, final String sc, final boolean lazy, final boolean terse) {
        final String attrName = d.getNameOrOID();
        final String fieldName = PersistUtils.toJavaIdentifier(attrName);
        writer.println();
        if (inRDN) {
            writer.println("  // The field used for RDN attribute " + attrName + '.');
        } else if (required) {
            writer.println("  // The field used for required attribute " + attrName + '.');
        } else if (d.isOperational()) {
            writer.println("  // The field used for operational attribute " + attrName + '.');
        } else {
            writer.println("  // The field used for optional attribute " + attrName + '.');
        }
        boolean added = false;
        if (terse && attrName.equalsIgnoreCase(fieldName)) {
            writer.print("  @LDAPField(");
        } else {
            writer.print("  @LDAPField(attribute=\"" + attrName + '"');
            added = true;
        }
        if (ocNames.isEmpty()) {
        } else if (ocNames.size() == 1) {
            if ((!terse) || (!ocNames.iterator().next().equalsIgnoreCase(sc))) {
                if (added) {
                    writer.println(",");
                    writer.print("             objectClass=\"" + ocNames.iterator().next() + '"');
                } else {
                    writer.println("objectClass=\"" + ocNames.iterator().next() + '"');
                    added = true;
                }
            }
        } else {
            final Iterator<String> iterator = ocNames.iterator();
            if (added) {
                writer.println(",");
                writer.println("             objectClass={ \"" + iterator.next() + "\",");
            } else {
                writer.println("objectClass={ \"" + iterator.next() + "\",");
                added = true;
            }
            while (iterator.hasNext()) {
                final String name = iterator.next();
                if (iterator.hasNext()) {
                    writer.println("                           \"" + name + "\",");
                } else {
                    writer.print("                           \"" + name + "\" }");
                }
            }
        }
        if (inRDN) {
            if (added) {
                writer.println(",");
                writer.println("             inRDN=true,");
            } else {
                writer.println("inRDN=true,");
                added = true;
            }
            writer.print("             filterUsage=FilterUsage.ALWAYS_ALLOWED");
        } else {
            if (!terse) {
                if (added) {
                    writer.println(",");
                    writer.print("             " + "filterUsage=FilterUsage.CONDITIONALLY_ALLOWED");
                } else {
                    writer.print("filterUsage=FilterUsage.CONDITIONALLY_ALLOWED");
                    added = true;
                }
            }
        }
        if (required) {
            if (added) {
                writer.println(",");
                writer.print("             requiredForEncode=true");
            } else {
                writer.print("requiredForEncode=true");
                added = true;
            }
        }
        if (d.isOperational()) {
            if (added) {
                writer.println(",");
                writer.println("             inAdd=false,");
            } else {
                writer.println("inAdd=false,");
                added = true;
            }
            writer.print("             inModify=false");
        }
        if (lazy) {
            if (added) {
                writer.println(",");
                writer.print("             lazilyLoad=true");
            } else {
                writer.print("lazilyLoad=true");
                added = true;
            }
        }
        writer.println(")");
        if (d.isSingleValued()) {
            writer.println("  private " + type + ' ' + fieldName + ';');
        } else {
            writer.println("  private " + type + "[] " + fieldName + ';');
        }
    }

    /**
   * Writes getter, setter, and filter creation methods for the specified
   * attribute.
   *
   * @param  writer     The writer to use to write the methods.
   * @param  d          The attribute type definition to be written.
   * @param  type       The name of the Java type to use for the attribute.
   * @param  addSetter  Indicates whether to write a setter method.
   */
    static void writeFieldMethods(final PrintWriter writer, final AttributeTypeDefinition d, final String type, final boolean addSetter) {
        writer.println();
        writer.println();
        writer.println();
        final String attrName = d.getNameOrOID();
        final String fieldName = PersistUtils.toJavaIdentifier(attrName);
        final String capFieldName = capitalize(fieldName);
        if (d.isSingleValued()) {
            if (type.equals("DN")) {
                writer.println("  /**");
                writer.println("   * Retrieves the first value for the field " + "associated with the");
                writer.println("   * " + attrName + " attribute as a DN, if present.");
                writer.println("   *");
                writer.println("   * @return  The first value for the field " + "associated with the");
                writer.println("   *          " + attrName + " attribute, or");
                writer.println("   *          {@code null} if the field does not " + "have a value.");
                writer.println("   */");
                writer.println("  public DN get" + capFieldName + "DN()");
                writer.println("  {");
                writer.println("    return " + fieldName + ';');
                writer.println("  }");
                writer.println();
                writer.println();
                writer.println();
                writer.println("  /**");
                writer.println("   * Retrieves the object referenced by the DN held " + "in the");
                writer.println("   * " + attrName + " attribute, if present.");
                writer.println("   *");
                writer.println("   * @param  <T>  The type of object to return.");
                writer.println("   *");
                writer.println("   * @param  connection  The connection to use to " + "retrieve the entry.  It must");
                writer.println("   *                     not be {@code null}.");
                writer.println("   * @param  type        The type of object as which " + "to decode the entry.  It");
                writer.println("   *                     must not be {@code null}, " + "and the class must be marked");
                writer.println("   *                     with the {@code LDAPObject} " + "annotation type.");
                writer.println("   *");
                writer.println("   * @return  The object decoded from the entry with " + "the associated DN, or");
                writer.println("   *          {@code null} if the field does not " + "have a value or the referenced");
                writer.println("   *          entry does not exist.");
                writer.println("   *");
                writer.println("   * @throws  LDAPException  If a problem occurs " + "while attempting to retrieve");
                writer.println("   *                         the entry or decode it " + "as an object of the");
                writer.println("   *                         specified type.");
                writer.println("   */");
                writer.println("  public <T> T get" + capFieldName + "Object(");
                writer.println("                    final LDAPInterface connection,");
                writer.println("                    final Class<T> type)");
                writer.println("         throws LDAPException");
                writer.println("  {");
                writer.println("    return PersistUtils.getEntryAsObject(" + fieldName + ',');
                writer.println("         type, connection);");
                writer.println("  }");
                if (addSetter) {
                    writer.println();
                    writer.println();
                    writer.println();
                    writer.println("  /**");
                    writer.println("   * Sets the value for the field associated with " + "the");
                    writer.println("   * " + attrName + " attribute.");
                    writer.println("   *");
                    writer.println("   * @param  v  The value for the field associated " + "with the");
                    writer.println("   *            " + attrName + " attribute.");
                    writer.println("   */");
                    writer.println("  public void set" + capFieldName + "(final DN v)");
                    writer.println("  {");
                    writer.println("    this." + fieldName + " = v;");
                    writer.println("  }");
                    writer.println();
                    writer.println();
                    writer.println();
                    writer.println("  /**");
                    writer.println("   * Sets the value for the field associated with " + "the");
                    writer.println("   * " + attrName + " attribute.");
                    writer.println("   *");
                    writer.println("   * @param  v  The string representation of the " + "value for the field associated");
                    writer.println("   *            with the " + attrName + " attribute.");
                    writer.println("   *");
                    writer.println("   * @throws  LDAPException  If the provided " + "string cannot be parsed as a DN.");
                    writer.println("   */");
                    writer.println("  public void set" + capFieldName + "(final String v)");
                    writer.println("         throws LDAPException");
                    writer.println("  {");
                    writer.println("    if (v == null)");
                    writer.println("    {");
                    writer.println("      this." + fieldName + " = null;");
                    writer.println("    }");
                    writer.println("    else");
                    writer.println("    {");
                    writer.println("      this." + fieldName + " = new DN(v);");
                    writer.println("    }");
                    writer.println("  }");
                }
            } else {
                writer.println("  /**");
                writer.println("   * Retrieves the value for the field associated " + "with the");
                writer.println("   * " + attrName + " attribute, if present.");
                writer.println("   *");
                writer.println("   * @return  The value for the field associated " + "with the");
                writer.println("   *          " + attrName + " attribute, or");
                writer.println("   *          {@code null} if the field does not " + "have a value.");
                writer.println("   */");
                writer.println("  public " + type + " get" + capFieldName + "()");
                writer.println("  {");
                writer.println("    return " + fieldName + ';');
                writer.println("  }");
                if (addSetter) {
                    writer.println();
                    writer.println();
                    writer.println();
                    writer.println("  /**");
                    writer.println("   * Sets the value for the field associated with " + "the");
                    writer.println("   * " + attrName + " attribute.");
                    writer.println("   *");
                    writer.println("   * @param  v  The value for the field associated " + "with the");
                    writer.println("   *            " + attrName + " attribute.");
                    writer.println("   */");
                    writer.println("  public void set" + capFieldName + "(final " + type + " v)");
                    writer.println("  {");
                    writer.println("    this." + fieldName + " = v;");
                    writer.println("  }");
                }
            }
        } else {
            if (type.equals("DN")) {
                writer.println("  /**");
                writer.println("   * Retrieves the first value for the field " + "associated with the");
                writer.println("   * " + attrName + " attribute as a DN, if present.");
                writer.println("   *");
                writer.println("   * @return  The first value for the field " + "associated with the");
                writer.println("   *          " + attrName + " attribute, or");
                writer.println("   *          {@code null} if that attribute was not " + "present in the entry or");
                writer.println("   *          does not have any values.");
                writer.println("   */");
                writer.println("  public DN getFirst" + capFieldName + "DN()");
                writer.println("  {");
                writer.println("    if ((" + fieldName + " == null) ||");
                writer.println("        (" + fieldName + ".length == 0))");
                writer.println("    {");
                writer.println("      return null;");
                writer.println("    }");
                writer.println("    else");
                writer.println("    {");
                writer.println("      return " + fieldName + "[0];");
                writer.println("    }");
                writer.println("  }");
                writer.println();
                writer.println();
                writer.println();
                writer.println("  /**");
                writer.println("   * Retrieves the values for the field associated " + "with the");
                writer.println("   * " + attrName + " attribute as DNs, if present.");
                writer.println("   *");
                writer.println("   * @return  The values for the field associated " + "with the");
                writer.println("   *          " + attrName + " attribute, or");
                writer.println("   *          {@code null} if that attribute was not " + "present in the entry.");
                writer.println("   */");
                writer.println("  public DN[] get" + capFieldName + "DNs()");
                writer.println("  {");
                writer.println("    return " + fieldName + ';');
                writer.println("  }");
                writer.println();
                writer.println();
                writer.println();
                writer.println("  /**");
                writer.println("   * Retrieves the values for the field associated " + "with the");
                writer.println("   * " + attrName + " attribute as objects of the " + "specified type,");
                writer.println("   * if present.");
                writer.println("   *");
                writer.println("   * @param  <T>  The type of object to return.");
                writer.println("   *");
                writer.println("   * @param  connection  The connection to use to " + "retrieve the entries.  It");
                writer.println("   *                     must not be {@code null}.");
                writer.println("   * @param  type        The type of object as which " + "the entries should be");
                writer.println("   *                     decoded.  It must not be " + "{@code null}, and the class");
                writer.println("   *                     must be marked with the " + "{@code LDAPObject} annotation");
                writer.println("   *                     type.");
                writer.println("   *");
                writer.println("   * @return  A {@code PersistedObjects} object that " + "may be used to iterate");
                writer.println("   *          across the resulting objects.");
                writer.println("   *");
                writer.println("   * @throws  LDAPException  If the requested type " + "cannot be used with the LDAP");
                writer.println("   *                         SDK persistence " + "framework.");
                writer.println("   */");
                writer.println("  public <T> PersistedObjects<T> get" + capFieldName + "Objects(");
                writer.println("                                      final " + "LDAPInterface connection,");
                writer.println("                                      final Class<T> " + "type)");
                writer.println("         throws LDAPException");
                writer.println("  {");
                writer.println("    return PersistUtils.getEntriesAsObjects(" + fieldName + ',');
                writer.println("         type, connection);");
                writer.println("  }");
                if (addSetter) {
                    writer.println();
                    writer.println();
                    writer.println();
                    writer.println("  /**");
                    writer.println("   * Sets the values for the field associated with " + "the");
                    writer.println("   * " + attrName + " attribute.");
                    writer.println("   *");
                    writer.println("   * @param  v  The values for the field " + "associated with the");
                    writer.println("   *            " + attrName + " attribute.");
                    writer.println("   */");
                    writer.println("  public void set" + capFieldName + "(final DN... v)");
                    writer.println("  {");
                    writer.println("    this." + fieldName + " = v;");
                    writer.println("  }");
                    writer.println();
                    writer.println();
                    writer.println();
                    writer.println("  /**");
                    writer.println("   * Sets the values for the field associated with " + "the");
                    writer.println("   * " + attrName + " attribute.");
                    writer.println("   *");
                    writer.println("   * @param  v  The string representations of the " + "values for the field");
                    writer.println("   *            associated with the " + attrName + " attribute.");
                    writer.println("   *");
                    writer.println("   * @throws  LDAPException  If any of the " + "provided strings cannot be parsed as");
                    writer.println("   *                         a DN.");
                    writer.println("   */");
                    writer.println("  public void set" + capFieldName + "(final String... v)");
                    writer.println("         throws LDAPException");
                    writer.println("  {");
                    writer.println("    if (v == null)");
                    writer.println("    {");
                    writer.println("      this." + fieldName + " = null;");
                    writer.println("    }");
                    writer.println("    else");
                    writer.println("    {");
                    writer.println("      this." + fieldName + " = new DN[v.length];");
                    writer.println("      for (int i=0; i < v.length; i++)");
                    writer.println("      {");
                    writer.println("        this." + fieldName + "[i] = new DN(v[i]);");
                    writer.println("      }");
                    writer.println("    }");
                    writer.println("  }");
                }
            } else {
                writer.println("  /**");
                writer.println("   * Retrieves the first value for the field " + "associated with the");
                writer.println("   * " + attrName + " attribute, if present.");
                writer.println("   *");
                writer.println("   * @return  The first value for the field " + "associated with the");
                writer.println("   *          " + attrName + " attribute, or");
                writer.println("   *          {@code null} if that attribute was not " + "present in the entry or");
                writer.println("   *          does not have any values.");
                writer.println("   */");
                writer.println("  public " + type + " getFirst" + capFieldName + "()");
                writer.println("  {");
                writer.println("    if ((" + fieldName + " == null) ||");
                writer.println("        (" + fieldName + ".length == 0))");
                writer.println("    {");
                writer.println("      return null;");
                writer.println("    }");
                writer.println("    else");
                writer.println("    {");
                writer.println("      return " + fieldName + "[0];");
                writer.println("    }");
                writer.println("  }");
                writer.println();
                writer.println();
                writer.println();
                writer.println("  /**");
                writer.println("   * Retrieves the values for the field associated " + "with the");
                writer.println("   * " + attrName + " attribute, if present.");
                writer.println("   *");
                writer.println("   * @return  The values for the field associated " + "with the");
                writer.println("   *          " + attrName + " attribute, or");
                writer.println("   *          {@code null} if that attribute was not " + "present in the entry.");
                writer.println("   */");
                writer.println("  public " + type + "[] get" + capFieldName + "()");
                writer.println("  {");
                writer.println("    return " + fieldName + ';');
                writer.println("  }");
                if (addSetter) {
                    writer.println();
                    writer.println();
                    writer.println();
                    writer.println("  /**");
                    writer.println("   * Sets the values for the field associated with " + "the");
                    writer.println("   * " + attrName + " attribute.");
                    writer.println("   *");
                    writer.println("   * @param  v  The values for the field " + "associated with the");
                    writer.println("   *            " + attrName + " attribute.");
                    writer.println("   */");
                    writer.println("  public void set" + capFieldName + "(final " + type + "... v)");
                    writer.println("  {");
                    writer.println("    this." + fieldName + " = v;");
                    writer.println("  }");
                }
            }
        }
        writer.println();
        writer.println();
        writer.println();
        writer.println("  /**");
        writer.println("   * Generates a filter that may be used to search for " + "objects of this type");
        writer.println("   * using the " + attrName + " attribute.");
        writer.println("   * The resulting filter may be combined with other " + "filter elements to create a");
        writer.println("   * more complex filter.");
        writer.println("   *");
        writer.println("   * @param  filterType  The type of filter to generate.");
        writer.println("   * @param  value       The value to use to use for the " + "filter.  It may be");
        writer.println("   *                     {@code null} only for a filter " + "type of");
        writer.println("   *                     {@code PRESENCE}.");
        writer.println("   *");
        writer.println("   * @return  The generated search filter.");
        writer.println("   *");
        writer.println("   * @throws  LDAPPersistException  If a problem is " + "encountered while attempting");
        writer.println("   *                                to generate the " + "filter.");
        writer.println("   */");
        writer.println("  public static Filter generate" + capFieldName + "Filter(");
        writer.println("                            final PersistFilterType " + "filterType,");
        writer.println("                            final " + type + " value)");
        writer.println("         throws LDAPPersistException");
        writer.println("  {");
        writer.println("    final byte[] valueBytes;");
        writer.println("    if (filterType == PersistFilterType.PRESENCE)");
        writer.println("    {");
        writer.println("      valueBytes = null;");
        writer.println("    }");
        writer.println("    else");
        writer.println("    {");
        writer.println("      if (value == null)");
        writer.println("      {");
        writer.println("        throw new LDAPPersistException(\"Unable to " + "generate a filter of type \" +");
        writer.println("             filterType.name() + \" with a null value " + "for attribute \" +");
        writer.println("             \"" + attrName + "\");");
        writer.println("      }");
        writer.println();
        writer.println("      final LDAPObjectHandler<?> objectHandler =");
        writer.println("           getPersister().getObjectHandler();");
        writer.println("      final FieldInfo fieldInfo = " + "objectHandler.getFields().get(");
        writer.println("           \"" + toLowerCase(attrName) + "\");");
        writer.println();
        writer.println("      final DefaultObjectEncoder objectEncoder = new " + "DefaultObjectEncoder();");
        writer.println("      valueBytes = " + "objectEncoder.encodeFieldValue(fieldInfo.getField(),");
        if (d.isSingleValued()) {
            writer.println("           value,");
        } else {
            writer.println("           new " + type + "[] { value },");
        }
        writer.println("           \"" + attrName + "\").getValueByteArray();");
        writer.println("    }");
        writer.println();
        writer.println("    switch (filterType)");
        writer.println("    {");
        writer.println("      case PRESENCE:");
        writer.println("        return Filter.createPresenceFilter(");
        writer.println("             \"" + attrName + "\");");
        writer.println("      case EQUALITY:");
        writer.println("        return Filter.createEqualityFilter(");
        writer.println("             \"" + attrName + "\",");
        writer.println("             valueBytes);");
        writer.println("      case STARTS_WITH:");
        writer.println("        return Filter.createSubstringFilter(");
        writer.println("             \"" + attrName + "\",");
        writer.println("             valueBytes, null, null);");
        writer.println("      case ENDS_WITH:");
        writer.println("        return Filter.createSubstringFilter(");
        writer.println("             \"" + attrName + "\",");
        writer.println("             null, null, valueBytes);");
        writer.println("      case CONTAINS:");
        writer.println("        return Filter.createSubstringFilter(");
        writer.println("             \"" + attrName + "\",");
        writer.println("             null, new byte[][] { valueBytes }, null);");
        writer.println("      case GREATER_OR_EQUAL:");
        writer.println("        return Filter.createGreaterOrEqualFilter(");
        writer.println("             \"" + attrName + "\",");
        writer.println("             valueBytes);");
        writer.println("      case LESS_OR_EQUAL:");
        writer.println("        return Filter.createLessOrEqualFilter(");
        writer.println("             \"" + attrName + "\",");
        writer.println("             valueBytes);");
        writer.println("      case APPROXIMATELY_EQUAL_TO:");
        writer.println("        return Filter.createApproximateMatchFilter(");
        writer.println("             \"" + attrName + "\",");
        writer.println("             valueBytes);");
        writer.println("      default:");
        writer.println("        // This should never happen.");
        writer.println("        throw new LDAPPersistException(\"Unrecognized " + "filter type \" +");
        writer.println("             filterType.name());");
        writer.println("    }");
        writer.println("  }");
    }

    /**
   * Writes a {@code toString} method for the generated class.
   *
   * @param  writer            The writer to use to write the methods.
   * @param  className         The base name (without package information) for
   *                           the generated class.
   * @param  requiredAttrs     The set of required attributes for the generated
   *                           class.
   * @param  optionalAttrs     The set of optional attributes for the generated
   *                           class.
   * @param  operationalAttrs  The set of operational attributes for the
   *                           generated class.
   */
    static void writeToString(final PrintWriter writer, final String className, final Collection<AttributeTypeDefinition> requiredAttrs, final Collection<AttributeTypeDefinition> optionalAttrs, final Collection<AttributeTypeDefinition> operationalAttrs) {
        writer.println();
        writer.println();
        writer.println();
        writer.println("  /**");
        writer.println("   * Retrieves a string representation of this");
        writer.println("   * {@code " + className + "} object.");
        writer.println("   *");
        writer.println("   * @return  A string representation of this");
        writer.println("   *          {@code " + className + "} object.");
        writer.println("   */");
        writer.println("  @Override()");
        writer.println("  public String toString()");
        writer.println("  {");
        writer.println("    final StringBuilder buffer = new StringBuilder();");
        writer.println("    toString(buffer);");
        writer.println("    return buffer.toString();");
        writer.println("  }");
        writer.println();
        writer.println();
        writer.println();
        writer.println("  /**");
        writer.println("   * Appends a string representation of this");
        writer.println("   * {@code " + className + "} object");
        writer.println("   * to the provided buffer.");
        writer.println("   *");
        writer.println("   * @param  buffer  The buffer to which the string " + "representation should be");
        writer.println("   *                 appended.");
        writer.println("   */");
        writer.println("  public void toString(final StringBuilder buffer)");
        writer.println("  {");
        writer.println("    buffer.append(\"" + className + "(\");");
        writer.println();
        writer.println("    boolean appended = false;");
        writer.println("    if (ldapEntry != null)");
        writer.println("    {");
        writer.println("      appended = true;");
        writer.println("      buffer.append(\"entryDN='\");");
        writer.println("      buffer.append(ldapEntry.getDN());");
        writer.println("      buffer.append('\\'');");
        writer.println("    }");
        for (final AttributeTypeDefinition d : requiredAttrs) {
            writeToStringField(writer, d);
        }
        for (final AttributeTypeDefinition d : optionalAttrs) {
            writeToStringField(writer, d);
        }
        for (final AttributeTypeDefinition d : operationalAttrs) {
            writeToStringField(writer, d);
        }
        writer.println();
        writer.println("    buffer.append(')');");
        writer.println("  }");
    }

    /**
   * Writes information about the provided field for use in the {@code toString}
   * method.
   *
   * @param  w  The writer to use to write the {@code toString} content.
   * @param  d  The attribute type definition for the field to write.
   */
    private static void writeToStringField(final PrintWriter w, final AttributeTypeDefinition d) {
        final String fieldName = PersistUtils.toJavaIdentifier(d.getNameOrOID());
        w.println();
        w.println("    if (" + fieldName + " != null)");
        w.println("    {");
        w.println("      if (appended)");
        w.println("      {");
        w.println("        buffer.append(\", \");");
        w.println("      }");
        w.println("      appended = true;");
        w.println("      buffer.append(\"" + fieldName + "=\");");
        if (d.isSingleValued()) {
            w.println("      buffer.append(" + fieldName + ");");
        } else {
            w.println("      buffer.append(Arrays.toString(" + fieldName + "));");
        }
        w.println("    }");
    }

    /**
   * Retrieves the Java type to use for the provided attribute type definition.
   * For multi-valued attributes, the value returned will be the base type
   * without square brackets to indicate an array.
   *
   * @param  schema  The schema to use to determine the syntax for the
   *                 attribute.
   * @param  d       The attribute type definition for which to get the Java
   *                 type.
   *
   * @return  The Java type to use for the provided attribute type definition.
   */
    String getJavaType(final Schema schema, final AttributeTypeDefinition d) {
        if (!d.isSingleValued()) {
            needArrays = true;
        }
        final String syntaxOID = d.getSyntaxOID(schema);
        if (syntaxOID == null) {
            return "String";
        }
        final String oid;
        final int bracePos = syntaxOID.indexOf('{');
        if (bracePos > 0) {
            oid = syntaxOID.substring(0, bracePos);
        } else {
            oid = syntaxOID;
        }
        if (oid.equals("1.3.6.1.4.1.1466.115.121.1.7")) {
            return "Boolean";
        } else if (oid.equals("1.3.6.1.4.1.4203.1.1.2") || oid.equals("1.3.6.1.4.1.1466.115.121.1.5") || oid.equals("1.3.6.1.4.1.1466.115.121.1.8") || oid.equals("1.3.6.1.4.1.1466.115.121.1.9") || oid.equals("1.3.6.1.4.1.1466.115.121.1.10") || oid.equals("1.3.6.1.4.1.1466.115.121.1.28") || oid.equals("1.3.6.1.4.1.1466.115.121.1.40")) {
            return "byte[]";
        } else if (oid.equals("1.3.6.1.4.1.1466.115.121.1.24")) {
            needDate = true;
            return "Date";
        } else if (oid.equals("1.3.6.1.4.1.1466.115.121.1.27")) {
            return "Long";
        } else if (oid.equals("1.3.6.1.4.1.1466.115.121.1.12") || oid.equals("1.3.6.1.4.1.1466.115.121.1.34")) {
            needDN = true;
            if (!d.isSingleValued()) {
                needPersistedObjects = true;
            }
            return "DN";
        } else {
            return "String";
        }
    }

    /**
   * {@inheritDoc}
   */
    @Override()
    public LinkedHashMap<String[], String> getExampleUsages() {
        final LinkedHashMap<String[], String> examples = new LinkedHashMap<String[], String>(1);
        final String[] args = { "--hostname", "server.example.com", "--port", "389", "--bindDN", "uid=admin,dc=example,dc=com", "--bindPassword", "password", "--outputDirectory", "src/com/example", "--structuralClass", "myStructuralClass", "--auxiliaryClass", "auxClass1", "--auxiliaryClass", "auxClass2", "--rdnAttribute", "cn", "--defaultParentDN", "dc=example,dc=com", "--packageName", "com.example", "--className", "MyObject" };
        examples.put(args, INFO_GEN_SOURCE_EXAMPLE_1.get());
        return examples;
    }
}
