package com.googlecode.progobots.ui.text;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import jline.ConsoleReader;
import org.junit.Before;
import org.junit.Test;

public class JLineConsoleTest {

    private ConsoleReader reader;

    private ByteArrayInputStream in;

    private StringWriter writer;

    private JLineConsole console;

    @Before
    public void setUp() throws Exception {
        writer = new StringWriter();
        reader = mock(ConsoleReader.class);
        console = new JLineConsole(reader);
    }

    private void setUpRealConsoleReader() throws IOException {
        writer = new StringWriter();
        in = new ByteArrayInputStream(new byte[] {});
        reader = new ConsoleReader(in, writer);
        console = new JLineConsole(reader);
    }

    @Test
    public void testRead() throws IOException {
        console.read(">");
        verify(reader).readLine(">");
    }

    @Test(expected = ConsoleException.class)
    public void testReadException() throws IOException {
        doThrow(new IOException()).when(reader).readLine(">");
        console.read(">");
    }

    @Test
    public void testWrite() throws ConsoleException, IOException {
        setUpRealConsoleReader();
        console.printf("something");
        writer.flush();
        assertEquals("something", writer.toString());
    }

    @Test(expected = ConsoleException.class)
    public void testWriteException() throws IOException {
        setUpRealConsoleReader();
        console.printf(null);
    }
}
