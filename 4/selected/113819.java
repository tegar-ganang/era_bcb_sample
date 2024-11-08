package org.melati.poem.prepro;

import java.util.Vector;
import java.io.Writer;
import java.io.IOException;

/**
 * A definition of a <tt>ColumnTypePoemType</tt>.
 * 
 * A <tt>ColumnType</tt> is a metadata field, is used in <tt>Poem.dsd</tt> 
 * and is not usually used in normal DSD files.
 */
public class ColumnTypeFieldDef extends FieldDef {

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
    public ColumnTypeFieldDef(TableDef table, String name, int displayOrder, Vector qualifiers) throws IllegalityException {
        super(table, name, "PoemTypeFactory", "Integer", displayOrder, qualifiers);
        table.addImport("org.melati.poem.PoemTypeFactory", "persistent");
        table.addImport("org.melati.poem.PoemTypeFactory", "table");
        table.addImport("org.melati.poem.ColumnTypePoemType", "table");
    }

    /**
  * @param w The base table java file.
  * @throws IOException 
  *           if something goes wrong with the file system
  */
    protected void generateColRawAccessors(Writer w) throws IOException {
        super.generateColRawAccessors(w);
        w.write("\n" + "          public Object getRaw(Persistent g)\n" + "              throws AccessPoemException {\n" + "            return ((" + mainClass + ")g).get" + suffix + "Code();\n" + "          }\n" + "\n");
        w.write("          public void setRaw(Persistent g, Object raw)\n" + "              throws AccessPoemException {\n" + "            ((" + mainClass + ")g).set" + suffix + "Code((" + rawType + ")raw);\n" + "          }\n");
    }

    /**
  * @param w The base persistent java file.
  * @throws IOException 
  *           if something goes wrong with the file system
  */
    public void generateBaseMethods(Writer w) throws IOException {
        super.generateBaseMethods(w);
        w.write("\n /**\n" + "  * Retrieves the <code>" + suffix + "</code> value as an <code>Integer</code> for this " + "<code>Column</code> of the <code>" + table.suffix + "</code> <code>Table</code>.\n" + ((description != null) ? "  * Field description: \n" + DSD.javadocFormat(2, 3, description) + "  * \n" : "") + "  * \n" + "  * @generator " + "org.melati.poem.prepro.ColumnTypeFieldDef" + "#generateBaseMethods \n" + "  * @throws AccessPoemException \n" + "  *         if the current <code>AccessToken</code> \n" + "  *         does not confer read access rights\n" + "  * @return the <code>" + suffix + "</code> value of this <code>Column</code>\n" + "  */\n");
        w.write("\n" + "  public Integer get" + suffix + "Code()\n" + "      throws AccessPoemException {\n" + "    readLock();\n" + "    return get" + suffix + "_unsafe();\n" + "  }\n");
        w.write("\n /**\n" + "  * Sets the <code>Integer</code> <code>" + suffix + "</code> value  for this <code>" + table.suffix + "</code> <code>Column</code> of the <code>" + table.suffix + "</code> <code>Table</code>.\n" + ((description != null) ? "  * Field description: \n" + DSD.javadocFormat(2, 3, description) + "  * \n" : "") + "  * \n" + "  * @generator " + "org.melati.poem.prepro.ColumnTypeFieldDef" + "#generateBaseMethods \n" + "  * @param raw the value to set \n" + "  * @throws AccessPoemException \n" + "  *         if the current <code>AccessToken</code> \n" + "  *         does not confer write access rights\n" + "  */\n");
        w.write("\n" + "  public void set" + suffix + "Code(Integer raw)\n" + "      throws AccessPoemException {\n" + "    " + tableAccessorMethod + "().get" + suffix + "Column()." + "getType().assertValidRaw(raw);\n" + "    writeLock();\n" + "    set" + suffix + "_unsafe(raw);\n" + "  }\n" + "\n");
        w.write("\n /**\n" + "  * Retrieves the <code>" + suffix + "</code> value as an <code>" + type + "</code> for this " + "<code>Column</code> of the <code>" + table.suffix + "</code> <code>Table</code>.\n" + ((description != null) ? "  * Field description: \n" + DSD.javadocFormat(2, 3, description) + "  * \n" : "") + "  * \n" + "  * @generator " + "org.melati.poem.prepro.ColumnTypeFieldDef" + "#generateBaseMethods \n" + "  * @throws AccessPoemException \n" + "  *         if the current <code>AccessToken</code> \n" + "  *         does not confer read access rights\n" + "  * @return the <code>" + suffix + "</code> value of this <code>Column</code>\n" + "  */\n");
        w.write("  public " + type + " get" + suffix + "()\n" + "      throws AccessPoemException {\n" + "    Integer code = get" + suffix + "Code();\n" + "    return code == null ? null :\n" + "        PoemTypeFactory.forCode(getDatabase(), " + "code.intValue());\n" + "  }\n" + "\n");
        w.write("\n /**\n" + "  * Sets the <code>" + type + "</code> <code>" + suffix + "</code> value  for this <code>" + table.suffix + "</code> <code>Column</code> of the <code>" + table.suffix + "</code> <code>Table</code>.\n" + ((description != null) ? "  * Field description: \n" + DSD.javadocFormat(2, 3, description) + "  * \n" : "") + "  * \n" + "  * @generator " + "org.melati.poem.prepro.ColumnTypeFieldDef" + "#generateBaseMethods \n" + "  * @param cooked the value to set \n" + "  * @throws AccessPoemException \n" + "  *         if the current <code>AccessToken</code> \n" + "  *         does not confer write access rights\n" + "  */\n");
        w.write("  public void set" + suffix + "(" + type + " cooked)\n" + "      throws AccessPoemException {\n" + "    set" + suffix + "Code(cooked == null ? null : " + "cooked.getCode());\n" + "  }\n");
    }

    /**
  * @param w whatever is being written to
  * @throws IOException 
  *           if something goes wrong with the file system
  */
    public void generateJavaDeclaration(Writer w) throws IOException {
        w.write("Integer " + name);
    }

    /** @return the Java string for this <code>PoemType</code>. */
    public String poemTypeJava() {
        return "new ColumnTypePoemType(getDatabase())";
    }
}
