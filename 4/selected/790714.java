package test.net.hawk.digiextractor.digic;

import static org.junit.Assert.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;
import net.hawk.digiextractor.digic.PartitionTable;
import net.hawk.digiextractor.digic.PartitionTableEntry;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The Class PartitionTableTest.
 * Test parsing full partition tables.
 */
public class PartitionTableTest {

    /** The expected number of partition entries in the table. */
    private static final int EXPECTED_ENTRY_COUNT = 7;

    /** The expected size of the first partition. */
    private static final long FIRST_SIZE = 976752000L;

    /** The expected start sector of the first partition. */
    private static final int FIRST_START_SECTOR = 0x3EC1;

    /** The expected type of the first partiton. */
    private static final int FIRST_TYPE = 0x0E;

    /** A file containing a partition table. */
    private static FileChannel partTable;

    /** An image file without a partition table (S1/T1). */
    private static FileChannel nonPartTable;

    /** An image with a more complex partition structure. */
    private static FileChannel complexImage;

    /** A list containing the partiton table entries. */
    private static List<PartitionTableEntry> table;

    /** The expected preceding sectors values. */
    private static final int[] EXPECTED_PRECEDING_SECTORS = { 8, 200, 1032, 4104, 5128, 6400, 7680 };

    /** The expected partition sizes. */
    private static final int[] EXPECTED_PARTITION_SIZES = { 184, 824, 3064, 1016, 1272, 1280, 512 };

    /**
	 * Sets the up before class.
	 * open the image files and prepare filechannels for reading.
	 *
	 * @throws Exception the exception
	 */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        File f = new File("./testdata/bootrecord1.dat");
        FileInputStream fs = new FileInputStream(f);
        partTable = fs.getChannel();
        f = new File("./testdata/first_sect_t1.dat");
        fs = new FileInputStream(f);
        nonPartTable = fs.getChannel();
        f = new File("./testdata/image.img");
        fs = new FileInputStream(f);
        complexImage = fs.getChannel();
    }

    /**
	 * Tear down after class.
	 * Clos all opened FileChannels.
	 *
	 * @throws Exception the exception
	 */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        nonPartTable.close();
        partTable.close();
        complexImage.close();
    }

    /**
	 * Test parsing the partiton tables.
	 * First check if a image without a partition table is detected correctly.
	 * Then check if a partition table can be opened correctly.
	 *
	 * @throws Exception the exception
	 */
    @Test
    public final void testParseTable() throws Exception {
        table = PartitionTable.getPartitions(nonPartTable);
        assertTrue("list should be empty", table.isEmpty());
        table = PartitionTable.getPartitions(partTable);
        assertEquals("List should contain one entry", 1, table.size());
    }

    /**
	 * Test getting the partition type.
	 */
    @Test
    public final void testGetType() {
        assertEquals("check Type of partition 1", FIRST_TYPE, table.get(0).getType());
    }

    /**
	 * Test getting the partitions start sector.
	 */
    @Test
    public final void testGetStartSector() {
        assertEquals("test partition start sector", FIRST_START_SECTOR, table.get(0).getSectorsPrecedingPartition());
    }

    /**
	 * Test getting the partitions length.
	 */
    @Test
    public final void testPartitionLength() {
        assertEquals("test partition size", FIRST_SIZE, table.get(0).getSectorsInPartition());
    }

    /**
	 * Test if partition has "extended" flag set.
	 */
    @Test
    public final void testIsExtendedPartition() {
        assertFalse("check if partition is extended", table.get(0).isExtended());
    }

    /**
	 * Test parsing extended partition tables.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
    @Test
    public final void testParseExtendedPartitionTables() throws IOException {
        table = PartitionTable.getPartitions(complexImage);
        assertEquals("List should contain seven entries", EXPECTED_ENTRY_COUNT, table.size());
        for (PartitionTableEntry e : table) {
            System.out.println(e.toString());
        }
    }

    /**
	 * Test if partition start addresses are correctly parsed.
	 */
    @Test
    public final void testPartitionStartAdresses() {
        for (int i = 0; i < table.size(); ++i) {
            assertEquals("check part " + i + " start", EXPECTED_PRECEDING_SECTORS[i], table.get(i).getSectorsPrecedingPartition());
        }
    }

    /**
	 * Test partition sizes.
	 */
    @Test
    public final void testPartitionSizes() {
        for (int i = 0; i < table.size(); ++i) {
            assertEquals("check part " + i + " size", EXPECTED_PARTITION_SIZES[i], table.get(i).getSectorsInPartition());
        }
    }
}
