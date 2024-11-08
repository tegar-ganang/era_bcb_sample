package monitor.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Repeater implements Runnable {

    private int puerto;

    private IOManager receptor;

    private ServerSocket servidor;

    private Socket conexion;

    private DataInputStream dataIn;

    private DataOutputStream dataOut;

    private boolean seguirVivo;

    private int timeout;

    private boolean conexionActiva;

    private long frecuenciaChequeo;

    public Repeater(IOManager receptor, int puerto, int timeout, long frecuenciaChequeo) {
        this.puerto = puerto;
        this.receptor = receptor;
        this.timeout = timeout;
        seguirVivo = true;
        conexionActiva = false;
        this.frecuenciaChequeo = frecuenciaChequeo;
    }

    public void run() {
        receptor.log("Arrancando repetidor en puerto " + puerto);
        while (seguirVivo) {
            try {
                conexionActiva = false;
                servidor = new ServerSocket(puerto);
                servidor.setSoTimeout(timeout);
                conexion = servidor.accept();
                conexionActiva = true;
                log("Conexion activa en el repetidor del puerto " + puerto);
                dataIn = new DataInputStream(conexion.getInputStream());
                dataOut = new DataOutputStream(conexion.getOutputStream());
                while (true) {
                    while (dataIn.available() > 0) {
                        dataOut.writeByte(dataIn.readByte());
                        dataOut.flush();
                    }
                    Thread.sleep(frecuenciaChequeo);
                }
            } catch (Exception e) {
                e.printStackTrace(receptor.server.loggerError);
                conexionActiva = false;
            }
        }
    }

    private void log(String s) {
        receptor.log(s);
    }
}
