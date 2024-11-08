package org.openscience.cdk.io;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import org.junit.Assert;
import org.junit.Test;
import org.openscience.cdk.Molecule;
import org.openscience.cdk.CDKTestCase;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IChemFile;

/**
 * Combined TestCase for the reading/writing of mdl and cml files.
 *
 * @cdk.module test-io
 */
public class MDLCMLRoundtripTest extends CDKTestCase {

    public MDLCMLRoundtripTest() {
        super();
    }

    /**
     * @cdk.bug 1649526
     */
    @Test
    public void testBug1649526() throws CDKException {
        String filename = "data/mdl/bug-1649526.mol";
        InputStream ins = this.getClass().getClassLoader().getResourceAsStream(filename);
        MDLReader reader = new MDLReader(ins);
        Molecule mol = (Molecule) reader.read(new Molecule());
        StringWriter writer = new StringWriter();
        CMLWriter cmlWriter = new CMLWriter(writer);
        cmlWriter.write(mol);
        CMLReader cmlreader = new CMLReader(new ByteArrayInputStream(writer.toString().getBytes()));
        IChemFile file = (IChemFile) cmlreader.read(new org.openscience.cdk.ChemFile());
        StringWriter writermdl = new StringWriter();
        MDLWriter mdlWriter = new MDLWriter(writermdl);
        mdlWriter.write(file);
        String output = writermdl.toString();
        Assert.assertEquals(2994, output.indexOf("M  END"));
        Assert.assertEquals(-1, output.indexOf("$$$$"));
        Assert.assertEquals(25, output.indexOf(" 31 33  0  0  0  0"));
    }
}
