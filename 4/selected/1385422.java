package org.creativor.rayson.transport.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The class <code>PacketCounterTest</code> contains tests for the class
 * <code>{@link PacketCounter}</code>.
 * <p>
 * Copyright Creativor Studio (c) 2011
 * 
 * @generatedBy CodePro at 11-5-7 上午3:08
 * @author Nick Zhang
 * @version $Revision: 1.0 $
 */
public class PacketCounterTest {

    /**
	 * Launch the test.
	 * 
	 * @param args
	 *            the command line arguments
	 * 
	 * @generatedBy CodePro at 11-5-7 上午3:08
	 */
    public static void main(String[] args) {
        new org.junit.runner.JUnitCore().run(PacketCounterTest.class);
    }

    /**
	 * Perform pre-test initialization.
	 * 
	 * @throws Exception
	 *             if the initialization fails for some reason
	 * 
	 * @generatedBy CodePro at 11-5-7 上午3:08
	 */
    @Before
    public void setUp() throws Exception {
    }

    /**
	 * Perform post-test clean-up.
	 * 
	 * @throws Exception
	 *             if the clean-up fails for some reason
	 * 
	 * @generatedBy CodePro at 11-5-7 上午3:08
	 */
    @After
    public void tearDown() throws Exception {
    }

    /**
	 * Run the PacketCounter() constructor test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午3:08
	 */
    @Test
    public void testPacketCounter_1() throws Exception {
        PacketCounter result = new PacketCounter();
        assertNotNull(result);
        assertEquals("{read: 0, write: 0}", result.toString());
        assertEquals(0L, result.readCount());
        assertEquals(0L, result.writeCount());
    }

    /**
	 * Run the long readCount() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午3:08
	 */
    @Test
    public void testReadCount_1() throws Exception {
        PacketCounter fixture = new PacketCounter();
        fixture.readOne();
        fixture.writeOne();
        long result = fixture.readCount();
        assertEquals(1L, result);
    }

    /**
	 * Run the void readOne() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午3:08
	 */
    @Test
    public void testReadOne_1() throws Exception {
        PacketCounter fixture = new PacketCounter();
        fixture.readOne();
        fixture.writeOne();
        fixture.readOne();
    }

    /**
	 * Run the String toString() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午3:08
	 */
    @Test
    public void testToString_1() throws Exception {
        PacketCounter fixture = new PacketCounter();
        fixture.readOne();
        fixture.writeOne();
        String result = fixture.toString();
        assertEquals("{read: 1, write: 1}", result);
    }

    /**
	 * Run the long writeCount() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午3:08
	 */
    @Test
    public void testWriteCount_1() throws Exception {
        PacketCounter fixture = new PacketCounter();
        fixture.readOne();
        fixture.writeOne();
        long result = fixture.writeCount();
        assertEquals(1L, result);
    }

    /**
	 * Run the void writeOne() method test.
	 * 
	 * @throws Exception
	 * 
	 * @generatedBy CodePro at 11-5-7 上午3:08
	 */
    @Test
    public void testWriteOne_1() throws Exception {
        PacketCounter fixture = new PacketCounter();
        fixture.readOne();
        fixture.writeOne();
        fixture.writeOne();
    }
}
