package com.iver.cit.gvsig.fmap.layers;

import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import com.hardcode.driverManager.DriverLoadException;
import com.hardcode.gdbms.driver.exceptions.ReadDriverException;
import com.hardcode.gdbms.engine.data.DataSourceFactory;
import com.hardcode.gdbms.engine.data.NoSuchTableException;
import com.hardcode.gdbms.engine.data.driver.ObjectDriver;
import com.hardcode.gdbms.engine.values.Value;
import com.iver.cit.gvsig.fmap.core.FShape;
import com.iver.cit.gvsig.fmap.core.IGeometry;
import com.iver.cit.gvsig.fmap.drivers.IVectorialDatabaseDriver;

/**
 * Adapta un driver de base de datos vectorial a la interfaz vectorial,
 * manteniendo adem�s el estado necesario por una capa vectorial de base de
 * datos (par�metros de la conexi�n)
 */
public class VectorialDisconnectedDBAdapter extends VectorialAdapter {

    private static final int REFRESH = 0;

    private static final int RECEIVING = 1;

    private static final int LOCAL = 2;

    private int numReg = -1;

    private SelectableDataSource ds = null;

    private int status = REFRESH;

    private VectorialDBAdapter connectedAdapter;

    private File dataFile;

    private File indexFile;

    /**
	 * incrementa el contador de las veces que se ha abierto el fichero.
	 * Solamente cuando el contador est� a cero pide al driver que conecte con
	 * la base de datos
	 */
    public void start() throws ReadDriverException {
        switch(status) {
            case REFRESH:
                Thread t = new Thread(new Receiver());
                t.run();
                break;
            case RECEIVING:
            case LOCAL:
                break;
        }
    }

    /**
	 * decrementa el contador de n�mero de aperturas y cuando llega a cero pide
	 * al driver que cierre la conexion con el servidor de base de datos
	 */
    public void stop() throws ReadDriverException {
        ((IVectorialDatabaseDriver) driver).close();
    }

    /**
	 * @see com.iver.cit.gvsig.fmap.layers.ReadableVectorial#getShape(int)
	 */
    public IGeometry getShape(int index) throws ReadDriverException {
        return ((IVectorialDatabaseDriver) driver).getShape(index);
    }

    private String getTableName() {
        return ((IVectorialDatabaseDriver) driver).getTableName();
    }

    /**
	 * @see com.iver.cit.gvsig.fmap.layers.ReadableVectorial#getShapeType()
	 */
    public int getShapeType() throws ReadDriverException {
        return FShape.MULTI;
    }

    /**
	 * @throws ReadDriverException
	 * @see com.iver.cit.gvsig.fmap.layers.VectorialAdapter#getRecordset()
	 */
    public SelectableDataSource getRecordset() throws ReadDriverException {
        if (driver instanceof ObjectDriver) {
            String name = LayerFactory.getDataSourceFactory().addDataSource((ObjectDriver) driver);
            try {
                ds = new SelectableDataSource(LayerFactory.getDataSourceFactory().createRandomDataSource(name, DataSourceFactory.AUTOMATIC_OPENING));
            } catch (NoSuchTableException e) {
                throw new RuntimeException(e);
            } catch (DriverLoadException e) {
                throw new ReadDriverException(name, e);
            }
        }
        return ds;
    }

    public class Receiver implements Runnable {

        private ByteBuffer bb;

        private int currentCapacity = 0;

        private FileChannel channel;

        private ByteBuffer indexBuffer = ByteBuffer.allocate(4);

        private FileChannel indexChannel;

        private Rectangle2D extent = new Rectangle2D.Double();

        public ByteBuffer getBuffer(int capacity) {
            if (capacity > currentCapacity) {
                bb = ByteBuffer.allocate(capacity);
                currentCapacity = capacity;
            }
            return bb;
        }

        private void writeObject(byte[] bytes) throws IOException {
            bb = getBuffer(bytes.length + 4);
            bb.clear();
            bb.putInt(bytes.length);
            bb.put(bytes);
            bb.flip();
            channel.write(bb);
            indexBuffer.clear();
            indexBuffer.putInt((int) channel.position());
            indexBuffer.flip();
            indexChannel.write(indexBuffer);
        }

        /**
		 * @see java.lang.Runnable#run()
		 */
        public void run() {
            try {
                dataFile = new File("/root/tirar/gvSIGtmp.gvs");
                FileOutputStream fs = new FileOutputStream(dataFile);
                channel = fs.getChannel();
                indexFile = new File("/root/tirar/gvSIGindex.gvi");
                FileOutputStream indexfs = new FileOutputStream(indexFile);
                indexChannel = indexfs.getChannel();
                IVectorialDatabaseDriver d = (IVectorialDatabaseDriver) VectorialDisconnectedDBAdapter.this.driver;
                connectedAdapter = new VectorialDBAdapter();
                connectedAdapter.setDriver(d);
                indexBuffer.clear();
                indexBuffer.putInt(connectedAdapter.getRecordset().getFieldCount());
                indexBuffer.flip();
                indexChannel.write(indexBuffer);
                indexChannel.position(indexChannel.position() + 4);
                indexChannel.position(indexChannel.position() + 4 * 8);
                connectedAdapter.start();
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bytes);
                int geom = 0;
                for (int i = 0; i < connectedAdapter.getShapeCount(); i++) {
                    geom++;
                    IGeometry g = connectedAdapter.getShape(i);
                    extent.add(g.getBounds2D());
                    bytes = new ByteArrayOutputStream();
                    oos = new ObjectOutputStream(bytes);
                    oos.writeObject(g);
                    oos.close();
                    writeObject(bytes.toByteArray());
                    for (int j = 0; j < connectedAdapter.getRecordset().getFieldCount(); j++) {
                        Value v = connectedAdapter.getRecordset().getFieldValue(i, j);
                        bytes = new ByteArrayOutputStream();
                        oos = new ObjectOutputStream(bytes);
                        oos.writeObject(v);
                        oos.close();
                        writeObject(bytes.toByteArray());
                    }
                }
                indexBuffer.clear();
                indexBuffer.putInt(geom);
                indexBuffer.flip();
                indexChannel.position(4);
                indexChannel.write(indexBuffer);
                indexChannel.position(indexChannel.position() + 4);
                ByteBuffer extentBuffer = ByteBuffer.allocate(4 * 8);
                extentBuffer.putDouble(extent.getMinX());
                extentBuffer.putDouble(extent.getMinY());
                extentBuffer.putDouble(extent.getMaxX());
                extentBuffer.putDouble(extent.getMaxY());
                extentBuffer.flip();
                indexChannel.position(8);
                indexChannel.write(extentBuffer);
                channel.close();
                indexChannel.close();
                connectedAdapter.stop();
                status = LOCAL;
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ReadDriverException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
