package openminer.connect;

import java.io.*;

public abstract class Connection {

    public static Connection getConnection(String name) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        String packageName = Connection.class.getPackage().getName();
        return (Connection) Class.forName(packageName + "." + name).newInstance();
    }

    public static Connection getConnection() {
        return new OpenMinerConnection();
    }

    public abstract void connect(String server, int port, String username, String password) throws Exception;

    public abstract void train(String model, String dataType, int dataSize) throws Exception;

    public abstract void write(byte[] buf, int off, int len) throws Exception;

    public abstract void writeInt(int v) throws Exception;

    public abstract void writeDouble(double v) throws Exception;

    public abstract void writeUTF(String v) throws Exception;

    public abstract void read(byte[] buf, int off, int len) throws Exception;

    public abstract int readInt() throws Exception;

    public abstract double readDouble() throws Exception;

    public abstract String readUTF() throws Exception;

    public abstract void close() throws Exception;

    public void write(byte[] buf) throws Exception {
        write(buf, 0, buf.length);
    }

    public void read(byte[] buf) throws Exception {
        read(buf, 0, buf.length);
    }

    public String trainAsString(String model, String dataType, InputStream is) throws Exception {
        byte[] buffer = new byte[8 * 1024];
        int num;
        train(model, dataType, is.available());
        while ((num = is.read(buffer)) > 0) write(buffer, 0, num);
        int retSize = readInt();
        if (retSize > 0) {
            buffer = new byte[retSize];
            read(buffer);
            return new String(buffer);
        }
        return null;
    }

    public String trainAsString(String model, String dataType, String dataFile) throws Exception {
        FileInputStream fis = new FileInputStream(dataFile);
        String ret = trainAsString(model, dataType, fis);
        fis.close();
        return ret;
    }
}
