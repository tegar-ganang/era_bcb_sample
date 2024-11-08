package com.iver.cit.gvsig.fmap.drivers.shp;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.BitSet;
import com.hardcode.gdbms.driver.exceptions.ReadDriverException;
import com.hardcode.gdbms.engine.values.Value;
import com.hardcode.gdbms.engine.values.ValueFactory;
import com.iver.cit.gvsig.fmap.core.IGeometry;
import com.iver.cit.gvsig.fmap.layers.SelectableDataSource;

/**
 * DOCUMENT ME!
 *
 * @author Vicente Caballero Navarro
 */
public class DBFFromGeometries {

    private IGeometry[] geometries = null;

    private String dbfPath;

    private DbaseFileWriterNIO dbfWrite;

    private Value[] enteros;

    private Object[] record;

    public DBFFromGeometries(IGeometry[] geometries, File f) {
        this.geometries = geometries;
        setFile(f);
    }

    /**
	 * Inserta el fichero.
	 *
	 * @param f Fichero.
	 */
    private void setFile(File f) {
        String strFichDbf = f.getAbsolutePath().replaceAll("\\.shp", ".dbf");
        dbfPath = strFichDbf.replaceAll("\\.SHP", ".DBF");
    }

    /**
	 * Inicializa.
	 *
	 * @param sds Capa.
	 */
    public void create(SelectableDataSource sds, BitSet bitset) {
        try {
            if (sds == null) {
                DbaseFileHeaderNIO myHeader = DbaseFileHeaderNIO.createNewDbaseHeader();
                myHeader.setNumRecords(geometries.length);
                dbfWrite = new DbaseFileWriterNIO(myHeader, (FileChannel) getWriteChannel(dbfPath));
                enteros = new Value[1];
            } else {
                DbaseFileHeaderNIO myHeader;
                myHeader = DbaseFileHeaderNIO.createDbaseHeader(sds);
                myHeader.setNumRecords(geometries.length);
                dbfWrite = new DbaseFileWriterNIO(myHeader, (FileChannel) getWriteChannel(dbfPath));
                record = new Object[sds.getFieldCount()];
            }
            createdbf(sds, bitset);
            System.out.println("Acabado DBF");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ReadDriverException e2) {
            e2.printStackTrace();
        }
    }

    /**
	 * Rellena los registros del dbf.
	 *
	 * @param sds DOCUMENT ME!
	 */
    private void createdbf(SelectableDataSource sds, BitSet bitset) {
        int i = 0;
        try {
            if (sds == null) {
                for (int j = 0; j < geometries.length; j++) {
                    enteros[0] = ValueFactory.createValue((double) i);
                    dbfWrite.write(enteros);
                    i++;
                }
            } else {
                for (int j = bitset.nextSetBit(0); j >= 0; j = bitset.nextSetBit(j + 1)) {
                    for (int r = 0; r < sds.getFieldCount(); r++) {
                        record[r] = sds.getFieldValue(j, r);
                    }
                    dbfWrite.write(record);
                }
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (ReadDriverException e) {
            e.printStackTrace();
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param path DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    private WritableByteChannel getWriteChannel(String path) throws IOException {
        WritableByteChannel channel;
        File f = new File(path);
        if (!f.exists()) {
            System.out.println("Creando fichero " + f.getAbsolutePath());
            if (!f.createNewFile()) {
                throw new IOException("Cannot create file " + f);
            }
        }
        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        channel = raf.getChannel();
        return channel;
    }
}
