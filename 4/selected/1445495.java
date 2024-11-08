package org.melati.poem.prepro;

import java.util.Vector;
import java.io.Writer;
import java.io.IOException;

/**
 * A definition of a <tt>DisplayLevelPoemType</tt> from the DSD.
 * A <tt>DisplayLevel</tt> is a metadata field used in the 
 * <tt>ColumnInfo</tt> table.
 * 
 * Its member variables are populated from the DSD or defaults.
 * Its methods are used to generate the java code.
 */
public class DisplayLevelFieldDef extends FieldDef {

    /**
  * Constructor.
  *
  * @param table        the {@link TableDef} that this <code>Field</code> is 
  *                     part of 
  * @param name         the name of this field
  * @param displayOrder where to place this field in a list
  * @param qualifiers   all the qualifiers of this field
  * 
  * @throws IllegalityException if a semantic inconsistency is detected
  */
    public DisplayLevelFieldDef(TableDef table, String name, int displayOrder, Vector qualifiers) throws IllegalityException {
        super(table, name, "DisplayLevel", "Integer", displayOrder, qualifiers);
        table.addImport("org.melati.poem.DisplayLevelPoemType", "table");
        table.addImport("org.melati.poem.DisplayLevel", "table");
        table.addImport("org.melati.poem.DisplayLevel", "persistent");
    }

    /**
  * @param w The base table java file.
  *
  * @throws IOException 
  *           if something goes wrong with the file system
  */
    protected void generateColRawAccessors(Writer w) throws IOException {
        super.generateColRawAccessors(w);
        w.write("\n" + "          public Object getRaw(Persistent g)\n" + "              throws AccessPoemException {\n" + "            return ((" + mainClass + ")g).get" + suffix + "Index();\n" + "          }\n" + "\n");
        w.write("          public void setRaw(Persistent g, Object raw)\n" + "              throws AccessPoemException {\n" + "            ((" + mainClass + ")g).set" + suffix + "Index((" + rawType + ")raw);\n" + "          }\n");
    }

    /**
  * @param w The base persistent java file.
  *
  * @throws IOException 
  *           if something goes wrong with the file system
  */
    public void generateBaseMethods(Writer w) throws IOException {
        super.generateBaseMethods(w);
        w.write("\n /**\n" + "  * Retrieves the " + suffix + " index value \n" + "  * of this <code>Persistent</code>.\n" + "  * \n" + ((description != null) ? "  * Field description: \n" + DSD.javadocFormat(2, 3, description) + "  * \n" : "") + "  * @generator " + "org.melati.poem.prepro.DisplayLevelFieldDef" + "#generateBaseMethods \n" + "  * @throws AccessPoemException \n" + "  *         if the current <code>AccessToken</code> \n" + "  *         does not confer read access rights\n" + "  * @return the " + rawType + " " + name + "\n" + "  */\n");
        w.write("\n" + "  public Integer get" + suffix + "Index()\n" + "      throws AccessPoemException {\n" + "    readLock();\n" + "    return get" + suffix + "_unsafe();\n" + "  }\n" + "\n");
        w.write("\n /**\n" + "  * Sets the <code>" + suffix + "</code> index value, with checking, \n" + "  * for this <code>Persistent</code>.\n" + ((description != null) ? "  * Field description: \n" + DSD.javadocFormat(2, 3, description) + "  * \n" : "") + "  * \n" + "  * @generator " + "org.melati.poem.prepro.DisplayLevelFieldDef" + "#generateBaseMethods \n" + "  * @param raw  the value to set \n" + "  * @throws AccessPoemException \n" + "  *         if the current <code>AccessToken</code> \n" + "  *         does not confer write access rights\n" + "  */\n");
        w.write("  public void set" + suffix + "Index(Integer raw)\n" + "      throws AccessPoemException {\n" + "    " + tableAccessorMethod + "().get" + suffix + "Column()." + "getType().assertValidRaw(raw);\n" + "    writeLock();\n" + "    set" + suffix + "_unsafe(raw);\n" + "  }\n" + "\n");
        w.write("\n /**\n" + "  * Retrieves the " + suffix + " value \n" + "  * of this <code>Persistent</code>.\n" + ((description != null) ? "  * Field description: \n" + DSD.javadocFormat(2, 3, description) + "  * \n" : "") + "  * \n" + "  * @generator " + "org.melati.poem.prepro.DisplayLevelFieldDef" + "#generateBaseMethods \n" + "  * @throws AccessPoemException \n" + "  *         if the current <code>AccessToken</code> \n" + "  *         does not confer read access rights\n" + "  * @return the " + type + "\n" + "  */\n");
        w.write("  public " + type + " get" + suffix + "()\n" + "      throws AccessPoemException {\n" + "    Integer index = get" + suffix + "Index();\n" + "    return index == null ? null :\n" + "        DisplayLevel.forIndex(index.intValue());\n" + "  }\n" + "\n");
        w.write("\n /**\n" + "  * Sets the <code>" + suffix + "</code> value, with checking, for the " + "<code>Persistent</code> argument.\n" + ((description != null) ? "  * Field description: \n" + DSD.javadocFormat(2, 3, description) + "  * \n" : "") + "  * \n" + "  * @generator " + "org.melati.poem.prepro.DisplayLevelFieldDef" + "#generateBaseMethods \n" + "  * @param cooked  the value to set \n" + "  * @throws AccessPoemException \n" + "  *         if the current <code>AccessToken</code> \n" + "  *         does not confer write access rights\n" + "  */\n");
        w.write("  public void set" + suffix + "(" + type + " cooked)\n" + "      throws AccessPoemException {\n" + "    set" + suffix + "Index(cooked == null ? null : cooked.index);\n" + "  }\n");
    }

    /**
  * Write out this <code>Field</code>'s java declaration string.
  *
  * @param w The base persistent java file.
  * @throws IOException 
  *           if something goes wrong with the file system
  */
    public void generateJavaDeclaration(Writer w) throws IOException {
        w.write("Integer " + name);
    }

    /** @return the Java string for this <code>PoemType</code>. */
    public String poemTypeJava() {
        return "new DisplayLevelPoemType()";
    }
}
