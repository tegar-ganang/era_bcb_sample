package org.jcvi.common.core.assembly.ace.consed;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.jcvi.common.core.Range;
import org.jcvi.common.core.Range.CoordinateSystem;
import org.jcvi.common.core.assembly.ace.consed.ConsedNavigationWriter;
import org.jcvi.common.core.assembly.ace.consed.ConsensusNavigationElement;
import org.jcvi.common.core.assembly.ace.consed.ReadNavigationElement;
import org.jcvi.common.core.assembly.ace.consed.NavigationElement.Type;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestConsedNavigationWriter {

    private final String title = "title";

    private ByteArrayOutputStream out;

    private ConsedNavigationWriter sut;

    @Before
    public void setup() throws IOException {
        out = new ByteArrayOutputStream();
        sut = ConsedNavigationWriter.create(title, out);
    }

    @Test(expected = NullPointerException.class)
    public void nullOutputStreamShouldThrowNPE() throws IOException {
        ConsedNavigationWriter.create(title, null);
    }

    @Test
    public void noElementsToWriteShouldOnlyWriteTitle() throws IOException {
        sut.close();
        String expectedOutput = "TITLE: " + title + "\n";
        assertEquals(expectedOutput, new String(out.toByteArray()));
    }

    @Test
    public void partialWriterShouldNotWriteTitle() throws IOException {
        ByteArrayOutputStream partialOut = new ByteArrayOutputStream();
        ConsedNavigationWriter partialWriter = ConsedNavigationWriter.createPartial(partialOut);
        partialWriter.close();
        String expectedOutput = "";
        assertEquals(expectedOutput, new String(partialOut.toByteArray()));
    }

    @Test
    public void oneReadElement() throws IOException {
        String readId = "readId";
        Range range = Range.create(CoordinateSystem.RESIDUE_BASED, 10, 24);
        String comment = "read Comment";
        ReadNavigationElement element = new ReadNavigationElement(readId, range, comment);
        sut.writeNavigationElement(element);
        sut.close();
        StringBuilder expectedOutput = new StringBuilder("TITLE: " + title + "\n");
        expectedOutput.append("BEGIN_REGION\n").append(String.format("TYPE: %s\n", Type.READ)).append(String.format("READ: %s\n", element.getTargetId())).append(String.format("UNPADDED_READ_POS: %d %d\n", range.getBegin(CoordinateSystem.RESIDUE_BASED), range.getEnd(CoordinateSystem.RESIDUE_BASED))).append(String.format("COMMENT: %s\n", element.getComment())).append("END_REGION\n");
        assertEquals(expectedOutput.toString(), new String(out.toByteArray()));
    }

    @Test
    public void oneReadElementWithNoComment() throws IOException {
        String readId = "readId";
        Range range = Range.create(CoordinateSystem.RESIDUE_BASED, 10, 24);
        ReadNavigationElement element = new ReadNavigationElement(readId, range);
        sut.writeNavigationElement(element);
        sut.close();
        StringBuilder expectedOutput = new StringBuilder("TITLE: " + title + "\n");
        expectedOutput.append("BEGIN_REGION\n").append(String.format("TYPE: %s\n", Type.READ)).append(String.format("READ: %s\n", element.getTargetId())).append(String.format("UNPADDED_READ_POS: %d %d\n", range.getBegin(CoordinateSystem.RESIDUE_BASED), range.getEnd(CoordinateSystem.RESIDUE_BASED))).append("COMMENT: \n").append("END_REGION\n");
        assertEquals(expectedOutput.toString(), new String(out.toByteArray()));
    }

    @Test
    public void oneConsensusElement() throws IOException {
        String readId = "readId";
        Range range = Range.create(CoordinateSystem.RESIDUE_BASED, 10, 24);
        String comment = "read Comment";
        ConsensusNavigationElement element = new ConsensusNavigationElement(readId, range, comment);
        sut.writeNavigationElement(element);
        sut.close();
        StringBuilder expectedOutput = new StringBuilder("TITLE: " + title + "\n");
        expectedOutput.append("BEGIN_REGION\n").append(String.format("TYPE: %s\n", Type.CONSENSUS)).append(String.format("CONTIG: %s\n", element.getTargetId())).append(String.format("UNPADDED_CONS_POS: %d %d\n", range.getBegin(CoordinateSystem.RESIDUE_BASED), range.getEnd(CoordinateSystem.RESIDUE_BASED))).append(String.format("COMMENT: %s\n", element.getComment())).append("END_REGION\n");
        assertEquals(expectedOutput.toString(), new String(out.toByteArray()));
    }

    @Test
    public void oneConsensusElementWithoutComment() throws IOException {
        String readId = "readId";
        Range range = Range.create(CoordinateSystem.RESIDUE_BASED, 10, 24);
        ConsensusNavigationElement element = new ConsensusNavigationElement(readId, range);
        sut.writeNavigationElement(element);
        sut.close();
        StringBuilder expectedOutput = new StringBuilder("TITLE: " + title + "\n");
        expectedOutput.append("BEGIN_REGION\n").append(String.format("TYPE: %s\n", Type.CONSENSUS)).append(String.format("CONTIG: %s\n", element.getTargetId())).append(String.format("UNPADDED_CONS_POS: %d %d\n", range.getBegin(CoordinateSystem.RESIDUE_BASED), range.getEnd(CoordinateSystem.RESIDUE_BASED))).append("COMMENT: \n").append("END_REGION\n");
        assertEquals(expectedOutput.toString(), new String(out.toByteArray()));
    }

    @Test
    public void multipleElements() throws IOException {
        String readId = "readId";
        Range range = Range.create(CoordinateSystem.RESIDUE_BASED, 10, 24);
        ConsensusNavigationElement element = new ConsensusNavigationElement(readId, range);
        sut.writeNavigationElement(element);
        sut.writeNavigationElement(new ReadNavigationElement("another" + readId, range.shiftLeft(3)));
        sut.close();
        StringBuilder expectedOutput = new StringBuilder("TITLE: " + title + "\n");
        expectedOutput.append("BEGIN_REGION\n").append(String.format("TYPE: %s\n", Type.CONSENSUS)).append(String.format("CONTIG: %s\n", element.getTargetId())).append(String.format("UNPADDED_CONS_POS: %d %d\n", range.getBegin(CoordinateSystem.RESIDUE_BASED), range.getEnd(CoordinateSystem.RESIDUE_BASED))).append("COMMENT: \n").append("END_REGION\n").append("BEGIN_REGION\n").append(String.format("TYPE: %s\n", Type.READ)).append(String.format("READ: another%s\n", element.getTargetId())).append(String.format("UNPADDED_READ_POS: %d %d\n", range.getBegin(CoordinateSystem.RESIDUE_BASED) - 3, range.getEnd(CoordinateSystem.RESIDUE_BASED) - 3)).append("COMMENT: \n").append("END_REGION\n");
        assertEquals(expectedOutput.toString(), new String(out.toByteArray()));
    }
}
