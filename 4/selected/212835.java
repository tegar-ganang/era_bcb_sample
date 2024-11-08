package org.jcvi.assembly.ace.consed;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.jcvi.assembly.ace.consed.NavigationElement.Type;
import org.jcvi.common.util.Range;
import org.jcvi.common.util.Range.CoordinateSystem;
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
        sut = new ConsedNavigationWriter(title, out);
    }

    @Test(expected = NullPointerException.class)
    public void nullTitleShouldThrowNPE() throws IOException {
        new ConsedNavigationWriter(null, out);
    }

    @Test(expected = NullPointerException.class)
    public void nullOutputStreamShouldThrowNPE() throws IOException {
        new ConsedNavigationWriter(title, null);
    }

    @Test
    public void noElementsToWriteShouldOnlyWriteTitle() throws IOException {
        sut.close();
        String expectedOutput = "TITLE: " + title + "\n";
        assertEquals(expectedOutput, new String(out.toByteArray()));
    }

    @Test
    public void oneReadElement() throws IOException {
        String readId = "readId";
        Range range = Range.buildRange(CoordinateSystem.RESIDUE_BASED, 10, 24);
        String comment = "read Comment";
        ReadNavigationElement element = new ReadNavigationElement(readId, range, comment);
        sut.writeNavigationElement(element);
        sut.close();
        StringBuilder expectedOutput = new StringBuilder("TITLE: " + title + "\n");
        expectedOutput.append("BEGIN_REGION\n").append(String.format("TYPE: %s\n", Type.READ)).append(String.format("READ: %s\n", element.getTargetId())).append(String.format("UNPADDED_READ_POS: %d %d\n", range.getLocalStart(), range.getLocalEnd())).append(String.format("COMMENT: %s\n", element.getComment())).append("END_REGION\n");
        assertEquals(expectedOutput.toString(), new String(out.toByteArray()));
    }

    @Test
    public void oneReadElementWithNoComment() throws IOException {
        String readId = "readId";
        Range range = Range.buildRange(CoordinateSystem.RESIDUE_BASED, 10, 24);
        ReadNavigationElement element = new ReadNavigationElement(readId, range);
        sut.writeNavigationElement(element);
        sut.close();
        StringBuilder expectedOutput = new StringBuilder("TITLE: " + title + "\n");
        expectedOutput.append("BEGIN_REGION\n").append(String.format("TYPE: %s\n", Type.READ)).append(String.format("READ: %s\n", element.getTargetId())).append(String.format("UNPADDED_READ_POS: %d %d\n", range.getLocalStart(), range.getLocalEnd())).append("END_REGION\n");
        assertEquals(expectedOutput.toString(), new String(out.toByteArray()));
    }

    @Test
    public void oneConsensusElement() throws IOException {
        String readId = "readId";
        Range range = Range.buildRange(CoordinateSystem.RESIDUE_BASED, 10, 24);
        String comment = "read Comment";
        ConsensusNavigationElement element = new ConsensusNavigationElement(readId, range, comment);
        sut.writeNavigationElement(element);
        sut.close();
        StringBuilder expectedOutput = new StringBuilder("TITLE: " + title + "\n");
        expectedOutput.append("BEGIN_REGION\n").append(String.format("TYPE: %s\n", Type.CONSENSUS)).append(String.format("CONTIG: %s\n", element.getTargetId())).append(String.format("UNPADDED_CONS_POS: %d %d\n", range.getLocalStart(), range.getLocalEnd())).append(String.format("COMMENT: %s\n", element.getComment())).append("END_REGION\n");
        assertEquals(expectedOutput.toString(), new String(out.toByteArray()));
    }

    @Test
    public void oneConsensusElementWithoutComment() throws IOException {
        String readId = "readId";
        Range range = Range.buildRange(CoordinateSystem.RESIDUE_BASED, 10, 24);
        ConsensusNavigationElement element = new ConsensusNavigationElement(readId, range);
        sut.writeNavigationElement(element);
        sut.close();
        StringBuilder expectedOutput = new StringBuilder("TITLE: " + title + "\n");
        expectedOutput.append("BEGIN_REGION\n").append(String.format("TYPE: %s\n", Type.CONSENSUS)).append(String.format("CONTIG: %s\n", element.getTargetId())).append(String.format("UNPADDED_CONS_POS: %d %d\n", range.getLocalStart(), range.getLocalEnd())).append("END_REGION\n");
        assertEquals(expectedOutput.toString(), new String(out.toByteArray()));
    }

    @Test
    public void multipleElements() throws IOException {
        String readId = "readId";
        Range range = Range.buildRange(CoordinateSystem.RESIDUE_BASED, 10, 24);
        ConsensusNavigationElement element = new ConsensusNavigationElement(readId, range);
        sut.writeNavigationElement(element);
        sut.writeNavigationElement(new ReadNavigationElement("another" + readId, range.shiftLeft(3)));
        sut.close();
        StringBuilder expectedOutput = new StringBuilder("TITLE: " + title + "\n");
        expectedOutput.append("BEGIN_REGION\n").append(String.format("TYPE: %s\n", Type.CONSENSUS)).append(String.format("CONTIG: %s\n", element.getTargetId())).append(String.format("UNPADDED_CONS_POS: %d %d\n", range.getLocalStart(), range.getLocalEnd())).append("END_REGION\n").append("BEGIN_REGION\n").append(String.format("TYPE: %s\n", Type.READ)).append(String.format("READ: another%s\n", element.getTargetId())).append(String.format("UNPADDED_READ_POS: %d %d\n", range.getLocalStart() - 3, range.getLocalEnd() - 3)).append("END_REGION\n");
        assertEquals(expectedOutput.toString(), new String(out.toByteArray()));
    }
}
