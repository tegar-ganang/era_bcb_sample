package se.marianna.simpleDB.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import se.marianna.simpleDB.DBInitException;
import se.marianna.simpleDB.DuplicateColumnName;
import se.marianna.simpleDB.UnknownValueType;
import se.marianna.simpleDB.persisted.RMSDumpTable;

public class DumpTest extends RMSDumpTable {

    public DumpTest(String tableName, Object[] columnNamesAndTypes) throws DBInitException, DuplicateColumnName {
        super(tableName, columnNamesAndTypes);
    }

    public DumpTest(String tableName, String[] primaryKey, Object[] columnNamesAndTypes) throws DBInitException, DuplicateColumnName {
        super(tableName, primaryKey, columnNamesAndTypes);
    }

    public DumpTest(String tableName, String[] columns) throws DuplicateColumnName, UnknownValueType, DBInitException {
        super(tableName, columns);
    }

    private String pathToDBFile() {
        return System.getProperty("java.io.tmpdir") + "/" + this.tableName + ".simpleDB";
    }

    protected byte[] read() throws Exception {
        if (new File(pathToDBFile()).exists()) {
            FileInputStream fileIn = new FileInputStream(pathToDBFile());
            ByteArrayOutputStream toReturn = new ByteArrayOutputStream();
            byte[] buffer = new byte[32 * 1024];
            int read = 0;
            while ((read = fileIn.read(buffer)) != -1) {
                toReturn.write(buffer, 0, read);
            }
            fileIn.close();
            return toReturn.toByteArray();
        } else {
            return new byte[0];
        }
    }

    protected void write(byte[] data) throws Exception {
        FileOutputStream fileOut = new FileOutputStream(pathToDBFile());
        fileOut.write(data);
        fileOut.close();
    }
}
