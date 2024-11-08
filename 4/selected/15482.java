package ch.oscg.jreleaseinfo;

import junit.framework.TestCase;
import java.util.Date;

/**
 * Some JUnit testcases for the SourceGenerator.
 *
 * @author Thomas Cotting, Tangarena Engineering AG, Luzern
 * @version $Revision: 1.2 $ ($Date: 2005/08/06 14:13:17 $ / $Author: tcotting $)
 */
public class SourceGeneratorTest extends TestCase {

    /**
    * Constructor for SourceGeneratorTest.
    * @param arg0
    */
    public SourceGeneratorTest(String arg0) {
        super(arg0);
    }

    public void testWriteObjectMethod() {
        SourceGenerator gen = new SourceGenerator();
        StringBuffer buf = new StringBuffer();
        gen.writeObjectMethod(buf, JReleaseInfoProperty.TYPE_PRI_INT, "number", "33");
        gen.writeObjectMethod(buf, JReleaseInfoProperty.TYPE_PRI_BOOLEAN, "ready", "true");
        gen.writeObjectMethod(buf, JReleaseInfoProperty.TYPE_OBJ_STRING, "name", "Name");
        gen.writeObjectMethod(buf, JReleaseInfoProperty.TYPE_OBJ_BOOLEAN, "done", "false");
        gen.writeObjectMethod(buf, JReleaseInfoProperty.TYPE_OBJ_INTEGER, "count", "33");
        gen.writeDateMethod(buf, JReleaseInfoProperty.TYPE_OBJ_DATE, "build", new Date());
        System.out.println(buf.toString());
    }

    public void testWriteObjectDeclaration() {
    }

    public void testWriteMethodDeclaration() {
    }
}
