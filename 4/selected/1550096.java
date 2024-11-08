package net.sf.chineseutils;

import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author <a href="mailto:luhuiguo@gmail.com">Lu,Huiguo</a>
 * @version $Id: ChineseUtilsTest.java 50 2006-08-31 15:02:13Z fantasy4u $
 */
public class ChineseUtilsTest {

    /**
	 * @throws java.lang.Exception
	 */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
	 * @throws java.lang.Exception
	 */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
	 * @throws java.lang.Exception
	 */
    @Before
    public void setUp() throws Exception {
    }

    /**
	 * @throws java.lang.Exception
	 */
    @After
    public void tearDown() throws Exception {
    }

    /**
	 * Test method for
	 * {@link net.sf.chineseutils.ChineseUtils#simpToTrad(java.lang.String)}.
	 */
    @Test
    public void testSimpToTradString() {
        String expected = "ܛ�w";
        String result = ChineseUtils.simpToTrad("���");
        assertTrue(expected.equals(result));
    }

    /**
	 * Test method for
	 * {@link net.sf.chineseutils.ChineseUtils#tradToSimp(java.lang.String)}.
	 */
    @Test
    public void testTradToSimpString() {
        String expected = "���";
        String result = ChineseUtils.tradToSimp("ܛ�w");
        assertTrue(expected.equals(result));
    }

    @Test
    public void testTradToSimpReader() throws IOException {
        String simpStr = "������ԡ��˹���������ͺ˷�Ӧ�ѣ�";
        String tradStr = "�����Z�ԡ��˹��ǻ�ܛ�w�ͺ˷����t��";
        Reader reader = new StringReader(tradStr);
        Writer writer = new StringWriter();
        ChineseUtils.tradToSimp(reader, writer, true);
        System.out.println(writer.toString());
        assertTrue(simpStr.equals(writer.toString()));
    }

    @Test
    public void testSimpToTradReader() throws IOException {
        String simpStr = "������ԡ��˹���������ͺ˷�Ӧ�ѣ�";
        String tradStr = "�����Z�ԡ��˹��ǻ�ܛ�w�ͺ˷����t��";
        Reader reader = new StringReader(simpStr);
        Writer writer = new StringWriter();
        ChineseUtils.simpToTrad(reader, writer, true);
        System.out.println(writer.toString());
        assertTrue(tradStr.equals(writer.toString()));
    }
}
