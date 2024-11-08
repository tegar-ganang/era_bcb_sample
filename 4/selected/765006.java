package fi.tuska.jalkametri.data.io;

import junit.framework.TestCase;
import fi.tuska.jalkametri.data.io.DataReader;
import fi.tuska.jalkametri.data.io.DataWriter;
import fi.tuska.jalkametri.data.io.ParseException;

public class DataReaderTest extends TestCase {

    public void testReadString() throws ParseException {
        DataWriter writer = new DataWriter();
        writer.writeString("Hassu");
        writer.writeString("Kala");
        DataReader reader = new DataReader(writer.getData());
        assertEquals("Hassu", reader.readString());
        assertEquals("Kala", reader.readString());
    }

    public void testCheckString() throws ParseException {
        DataWriter writer = new DataWriter();
        writer.writeString("Hassu");
        writer.writeString("Kala");
        DataReader reader = new DataReader(writer.getData());
        reader.checkString("Hassu");
        reader.checkString("Kala");
    }
}
