package jhomenet.server.dao.txt;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import jhomenet.commons.cfg.*;
import jhomenet.commons.hw.data.HardwareData;
import jhomenet.server.dao.HardwareDataDao;
import jhomenet.server.persistence.txt.FilePersistenceUtil;

/**
 * TODO: Class description.
 *
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public class HardwareDataDaoTxt implements HardwareDataDao {

    /**
	 * Text based repository filename.
	 */
    private final String filename;

    private static final String defaultFilename = "hardware-data.txt";

    /**
	 * 
	 * @param filename
	 */
    public HardwareDataDaoTxt(String filename) {
        super();
        if (filename == null) throw new IllegalArgumentException("Filename cannot be null!");
        this.filename = filename;
    }

    /**
	 * 
	 */
    public HardwareDataDaoTxt() {
        this(defaultFilename);
    }

    /**
	 * @see jhomenet.server.dao.HardwareDataDao#getAllData(java.lang.String)
	 */
    public List<HardwareData> getAllData(String hardwareAddr) {
        return null;
    }

    /**
	 * @see jhomenet.server.dao.HardwareDataDao#getDataAfter(java.lang.String, java.util.Date)
	 */
    public List<HardwareData> getDataAfter(String hardwareAddr, Date afterDate) {
        return null;
    }

    /**
	 * @see jhomenet.server.dao.HardwareDataDao#getDataBefore(java.lang.String, java.util.Date)
	 */
    public List<HardwareData> getDataBefore(String hardwareAddr, Date beforeDate) {
        return null;
    }

    /**
	 * @see jhomenet.server.dao.HardwareDataDao#getDataBetween(java.lang.String, java.util.Date, java.util.Date)
	 */
    public List<HardwareData> getDataBetween(String hardwareAddr, Date afterDate, Date beforeDate) {
        return null;
    }

    /**
	 * @see jhomenet.server.dao.GenericDao#clear()
	 */
    public void clear() {
    }

    /**
	 * @see jhomenet.server.dao.GenericDao#findAll()
	 */
    public List<HardwareData> findAll() {
        return null;
    }

    /**
	 * @see jhomenet.server.dao.GenericDao#findByExample(java.lang.Object, java.lang.String[])
	 */
    public List<HardwareData> findByExample(HardwareData exampleInstance, String... excludeProperty) {
        return null;
    }

    /**
	 * @see jhomenet.server.dao.GenericDao#findById(java.io.Serializable, boolean)
	 */
    public HardwareData findById(Long id, boolean lock) {
        return null;
    }

    /**
	 * @see jhomenet.server.dao.GenericDao#flush()
	 */
    public void flush() {
    }

    /**
	 * @see jhomenet.server.dao.GenericDao#makePersistent(java.lang.Object)
	 */
    public HardwareData makePersistent(HardwareData data) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(Environment.getWorkingFolderAbsolute() + Environment.SEPARATOR + FilePersistenceUtil.dataDirectoryRelative + Environment.SEPARATOR + filename, true));
            bw.write("[" + data.getTimestamp().toString() + "] " + data.getHardwareAddrRef() + ", CH-" + data.getChannel() + ", " + data.getDataString());
            bw.newLine();
            bw.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException ioe2) {
                }
            }
        }
        return data;
    }

    /**
	 * @see jhomenet.server.dao.GenericDao#makeTransient(java.lang.Object)
	 */
    public void makeTransient(HardwareData entity) {
    }
}
