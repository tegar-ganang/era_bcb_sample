package com.iver.cit.gvsig.fmap.drivers.shp.write;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import com.hardcode.gdbms.driver.exceptions.ReadDriverException;
import com.iver.cit.gvsig.fmap.core.IGeometry;
import com.iver.cit.gvsig.fmap.drivers.shp.DbaseFileHeaderNIO;
import com.iver.cit.gvsig.fmap.drivers.shp.DbaseFileWriterNIO;
import com.iver.cit.gvsig.fmap.layers.FBitSet;
import com.iver.cit.gvsig.fmap.layers.SelectableDataSource;

/**
 * Visitor de zoom a lo seleccionado.
 *
 * @author Vicente Caballero Navarro
 */
public class DBFFromSelected {

    private IGeometry[] fgs = null;

    private boolean hasdbf = false;

    private String dbfPath;

    private int temp = 0;

    private DbaseFileWriterNIO dbfWrite;

    private Integer[] enteros;

    private SelectableDataSource sds;

    private Object[] record;

    /**
	 * Inserta el fichero.
	 *
	 * @param f Fichero.
	 */
    public void setFile(File f) {
        String strFichDbf = f.getAbsolutePath().replaceAll("\\.shp", ".dbf");
        dbfPath = strFichDbf.replaceAll("\\.SHP", ".DBF");
    }

    /**
	 * Inserta true si se quiere crear el dbf vacio y no copiar el contenido de otro dbf.
	 *
	 * @param b True si se quiere crear vacio el dbf.
	 */
    public void setIsNewDBF(boolean b) {
        hasdbf = b;
    }

    /**
	 * Inserta las geometr�as.
	 *
	 * @param g Geometr�as.
	 */
    public void setGeometries(IGeometry[] g) {
        fgs = g;
    }

    /**
	 * Inicializa.
	 *
	 * @param sds Capa.
	 */
    public void start(SelectableDataSource sds) {
        try {
            this.sds = sds;
            if (!hasdbf) {
                DbaseFileHeaderNIO myHeader = DbaseFileHeaderNIO.createNewDbaseHeader();
                myHeader.setNumRecords(fgs.length);
                dbfWrite = new DbaseFileWriterNIO(myHeader, (FileChannel) getWriteChannel(dbfPath));
                enteros = new Integer[1];
            } else {
                DbaseFileHeaderNIO myHeader;
                myHeader = DbaseFileHeaderNIO.createDbaseHeader(sds);
                myHeader.setNumRecords(fgs.length);
                dbfWrite = new DbaseFileWriterNIO(myHeader, (FileChannel) getWriteChannel(dbfPath));
                record = new Object[sds.getFieldCount()];
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ReadDriverException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Finaliza.
	 */
    public void stop() {
        System.out.println("Acabado el DBF con NIO escritura");
    }

    /**
	 * Rellena los registros del dbf.
	 */
    public void createdbf(FBitSet bitset) {
        int i = 0;
        for (int j = bitset.nextSetBit(0); j >= 0; j = bitset.nextSetBit(j + 1)) {
            try {
                if (!hasdbf) {
                    enteros[0] = Integer.valueOf(String.valueOf(i));
                    dbfWrite.write(enteros);
                    i++;
                } else {
                    for (int r = 0; r < sds.getFieldCount(); r++) {
                        record[r] = sds.getFieldValue(j, r);
                    }
                    dbfWrite.write(record);
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (ReadDriverException e) {
                e.printStackTrace();
            }
        }
    }

    private WritableByteChannel getWriteChannel(String path) throws IOException {
        WritableByteChannel channel;
        File f = new File(path);
        if (!f.exists() && !f.createNewFile()) {
            throw new IOException("Cannot create file " + f);
        }
        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        channel = raf.getChannel();
        return channel;
    }
}
