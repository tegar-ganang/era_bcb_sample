package org.hsqldb;

import org.hsqldb.lib.UnifiedTable;
import java.sql.SQLException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

/**
 *  Experimental routine to defrag the *.data file.
 *  This method iterates through the primary index of a table to find the
 *  disk position for each row and stores it, together with the new position
 *  in an array. Simulatneously, the disk record is copied from the old file
 *  into the new.
 *  A second pass over the new disk image translates the old pointers to the
 *  new.
 *
 * @version    1.7.2
 * @author     frest@users
 */
class DataFileDefrag1 {

    ArrayList defrag(Database db, DatabaseFile source, String filename) throws IOException, SQLException {
        ArrayList rootsList = new ArrayList();
        org.hsqldb.lib.HsqlArrayList tTable = db.getTables();
        RandomAccessFile dest = new RandomAccessFile(filename + ".new", "rw");
        dest.seek(Cache.INITIAL_FREE_POS);
        for (int i = 0, tSize = tTable.size(); i < tSize; i++) {
            Table t = (Table) tTable.get(i);
            if (t.tableType == Table.CACHED_TABLE) {
                int[] rootsArray = writeTableToDataFile(t, source, dest);
                rootsList.add(rootsArray);
            } else {
                rootsList.add(null);
            }
            Trace.printSystemOut(t.getName().name + " complete");
        }
        int pos = (int) dest.getFilePointer();
        dest.seek(Cache.FREE_POS_POS);
        dest.writeInt(pos);
        dest.close();
        for (int i = 0; i < rootsList.size(); i++) {
            int[] roots = (int[]) rootsList.get(i);
            if (roots != null) {
                Trace.printSystemOut(org.hsqldb.lib.StringUtil.getList(roots, ",", ""));
            }
        }
        return rootsList;
    }

    static void updateTableIndexRoots(org.hsqldb.lib.HsqlArrayList tTable, ArrayList rootsList) throws SQLException {
        for (int i = 0, tSize = tTable.size(); i < tSize; i++) {
            Table t = (Table) tTable.get(i);
            if (t.tableType == Table.CACHED_TABLE) {
                int[] rootsArray = (int[]) rootsList.get(i);
                t.setIndexRoots(rootsArray);
            }
        }
    }

    int[] writeTableToDataFile(Table table, DatabaseFile source, RandomAccessFile dest) throws IOException, SQLException {
        UnifiedTable pointerLookup = new UnifiedTable(int.class, 2, 1000000, 100000);
        int[] rootsArray = table.getIndexRootsArray();
        Index index = table.getPrimaryIndex();
        Node n = index.first();
        RawDiskRow readRow = new RawDiskRow();
        long pos = dest.getFilePointer();
        int count = 0;
        int[] pointerPair = new int[2];
        System.out.println("lookup begins: " + new java.util.Date(System.currentTimeMillis()));
        for (; n != null; count++) {
            CachedRow row = (CachedRow) n.getRow();
            int oldPointer = row.iPos;
            source.readSeek(oldPointer);
            readRow.read(source, rootsArray.length);
            int newPointer = (int) dest.getFilePointer();
            readRow.write(dest);
            pointerPair[0] = oldPointer;
            pointerPair[1] = newPointer;
            pointerLookup.addRow(pointerPair);
            if (count % 50000 == 0) {
                Trace.printSystemOut("pointer pair for row " + oldPointer + " " + newPointer);
            }
            n = index.next(n);
        }
        Trace.printSystemOut(table.getName().name + " transfered");
        dest.seek(pos);
        System.out.println("sort begins: " + new java.util.Date(System.currentTimeMillis()));
        pointerLookup.sort(0, true);
        System.out.println("sort ends: " + new java.util.Date(System.currentTimeMillis()));
        for (int i = 0; i < count; i++) {
            readRow.readNodes(dest, rootsArray.length);
            readRow.replacePointers(pointerLookup);
            dest.seek(readRow.filePosition);
            readRow.writeNodes(dest);
            dest.seek(readRow.filePosition + readRow.storageSize);
            if (i != 0 && i % 50000 == 0) {
                System.out.println(i + " rows " + new java.util.Date(System.currentTimeMillis()));
            }
        }
        for (int i = 0; i < rootsArray.length; i++) {
            int lookupIndex = pointerLookup.search(rootsArray[i]);
            if (lookupIndex == -1) {
                throw new SQLException();
            }
            rootsArray[i] = pointerLookup.getIntCell(lookupIndex, 1);
        }
        Trace.printSystemOut(table.getName().name + " : table converted");
        return rootsArray;
    }
}
