package br.ufal.tci.nexos.arcolive.service.fileshare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import br.ufal.tci.nexos.arcolive.exception.ArCoLIVECannotStartServerSocketException;
import br.ufal.tci.nexos.arcolive.logger.ArCoLIVELogRepository;
import br.ufal.tci.nexos.arcolive.participant.ArCoLIVEParticipant;
import br.ufal.tci.nexos.arcolive.participant.ArCoLIVEParticipantGroup;
import br.ufal.tci.nexos.arcolive.service.chat.SendDataImpl;

/**
 * FileShareImpl.java
 * 
 * CLASS DESCRIPTION
 * 
 * @see 25/09/2007
 * 
 * @author <a href="mailto:txithihausen@gmail.com">Ivo Augusto Andrade R Calado</a>.
 * @author <a href="mailto:thiagobrunoms@gmail.com">Thiago Bruno Melo de Sales</a>.
 * @since 0.1
 * @version 0.1
 * 
 * <p>
 * <b>Revisions:</b>
 * 
 * <p>
 * <b>yyyymmdd USERNAME:</b>
 * <ul>
 * <li> VERSION
 * </ul>
 */
public class FileShareImpl {

    private long timeOutDown, timeOutUp;

    private int initialPort, finalPort;

    private String announceBase;

    private ArCoLIVEParticipantGroup target;

    private long maxFileSize;

    List<ServerSocket> activeServers;

    /**
	 * 
	 * @param timeOutUp
	 * @param timeOutDown
	 * @param initialPort
	 * @param finalPort
	 * @param maxFileSize
	 * @param target
	 * @param announce
	 * @throws IllegalArgumentException
	 */
    public FileShareImpl(long timeOutUp, long timeOutDown, int initialPort, int finalPort, long maxFileSize, ArCoLIVEParticipantGroup target, String announce) throws IllegalArgumentException {
        if (timeOutUp < 0 || timeOutDown < 0 || initialPort < 0 || target == null || announceBase == null || maxFileSize <= 0) {
            throw new IllegalArgumentException("Invalid arguments");
        }
        this.timeOutUp = timeOutUp;
        this.timeOutDown = timeOutDown;
        this.announceBase = announce;
        this.target = target;
        this.initialPort = initialPort;
        this.finalPort = finalPort;
        this.maxFileSize = maxFileSize;
        activeServers = new LinkedList<ServerSocket>();
    }

    /**
	 * Start the temporary server and return its listening port
	 * 
	 * 
	 * Inicia tranferencia, recebe como paremetro o participante que ira mandar
	 * o arquivo, além disso recebe como paramentro o restante da mensagem que
	 * fara parte do anuncio de um novo arquivo, como nome do arquivo e
	 * descricao e recebe tambem o tamanho da arquivo esperado.
	 * 
	 * Retorna a porta que sera aberta pelo servidor para conexao, ou -1 caso
	 * não seja possível iniciar o servidor
	 * 
	 * @return port
	 * @throws ArCoLIVECannotStartServerSocketException
	 */
    public int startTransfer(ArCoLIVEParticipant from, String complementAnnounce, String notifyErrorMessage, long fileSize) {
        if (fileSize > this.maxFileSize) {
            ArCoLIVELogRepository.getInstance().log("Max file size exceeded");
            return -1;
        }
        ServerSocket server = getConfiguratedServer(initialPort, finalPort, timeOutUp);
        if (server == null) {
            ArCoLIVELogRepository.getInstance().log("A FileShare server cannot be started");
            return -1;
        }
        new Thread(new TransferUp(this, server, from, fileSize, this.announceBase + complementAnnounce, notifyErrorMessage)).start();
        return server.getLocalPort();
    }

    /**
	 * Configura um servidor em um porta presente no intervalo especificado
	 * pelos parametros minPort e maxPort e ajusta o timeToLive para tempo de
	 * espera por conexões ou retorna null caso não seja possível iniciar um
	 * servidor. Além disso adiciona o servidor criado em uma lista de forma
	 * que possa ser encerrado
	 * 
	 * @param minPort
	 * @param maxPort
	 * @param timeToLive
	 * @return
	 */
    private ServerSocket getConfiguratedServer(int minPort, int maxPort, long timeToLive) {
        for (int i = minPort; i <= maxPort; i++) {
            try {
                ServerSocket server = new ServerSocket(i);
                server.setSoTimeout((int) timeToLive);
                registryServer(server);
                return server;
            } catch (IOException e) {
                ArCoLIVELogRepository.getInstance().log("Cannot start server on port " + i);
            }
        }
        return null;
    }

    public void registryServer(ServerSocket s) {
        activeServers.add(s);
    }

    public void unregistryServer(ServerSocket s) {
        activeServers.remove(s);
    }

    public synchronized void unregistryAndCloseAllServers() throws IOException {
        for (ServerSocket s : activeServers) {
            s.close();
        }
        activeServers.clear();
    }

    /**
	 * Inicia o processo de abertura de um servidor para download de um arquivo
	 * 
	 * @param from
	 *            file sender
	 * @param notifyMessage
	 *            mensagem notificadora
	 * @param notifySenderErrorMessage
	 * @param file
	 */
    private void startDownloadTransfer(ArCoLIVEParticipant from, final String notifyMessage, final String notifySenderErrorMessage, File file) {
        ServerSocket server = this.getConfiguratedServer(initialPort, finalPort, timeOutDown);
        SendDataImpl sdi = new SendDataImpl();
        if (server == null) {
            sdi.sendUnicastData(from, target, notifySenderErrorMessage);
            file.delete();
            return;
        }
        sdi.sendMulticastData(from, target, notifyMessage);
        List<Thread> activeDownloadThreads = new LinkedList<Thread>();
        new ServerSocketControllerUtil(server, timeOutDown).start();
        try {
            while (true) {
                Socket client = server.accept();
                for (ArCoLIVEParticipant participant : target.getParticipants()) {
                    if (!participant.equals(from) && participant.getConnectionService().getConnection().getSocket().getRemoteSocketAddress().equals(client.getRemoteSocketAddress())) {
                        Thread thread = new Thread(new TransferDown(file, client));
                        activeDownloadThreads.add(thread);
                        thread.start();
                        break;
                    }
                }
            }
        } catch (IOException e) {
            unregistryServer(server);
            try {
                server.close();
            } catch (IOException e1) {
                ArCoLIVELogRepository.getInstance().log("The upload server couldn't be stopped");
                e.printStackTrace();
            } finally {
                new FileShareUtil(activeDownloadThreads, file).start();
            }
        }
    }

    private class TransferUp implements Runnable {

        private FileShareImpl listener;

        private ServerSocket server;

        private long fileSize;

        private ArCoLIVEParticipant from;

        private String notifyMessage;

        private String notifyErrorMessage;

        public TransferUp(FileShareImpl l, ServerSocket server, ArCoLIVEParticipant client, long fileSize, String notifyMessage, String notifyErrorMessage) {
            listener = l;
            this.server = server;
            this.fileSize = fileSize;
            this.from = client;
            this.notifyMessage = notifyMessage;
            this.notifyErrorMessage = notifyErrorMessage;
        }

        public void run() {
            Socket socket = null;
            new ServerSocketControllerUtil(server, timeOutUp).start();
            try {
                while (true) {
                    socket = server.accept();
                    if (from.getConnectionService().getConnection().getSocket().getRemoteSocketAddress().equals(socket.getRemoteSocketAddress())) break;
                }
            } catch (IOException ex) {
                ArCoLIVELogRepository.getInstance().log("Nobody client had started session");
                return;
            } finally {
                listener.unregistryServer(server);
                try {
                    server.close();
                } catch (IOException e) {
                    ArCoLIVELogRepository.getInstance().log("The upload server couldn't be stopped");
                    e.printStackTrace();
                }
            }
            File file = null;
            try {
                file = transfer(socket, fileSize);
            } catch (IOException e1) {
                from.getConnectionService().getConnection().sendMessage(this.notifyErrorMessage);
                if (file != null) file.delete();
                return;
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    ArCoLIVELogRepository.getInstance().log("The upload socket couldn't be stopped");
                }
            }
            startDownloadTransfer(from, notifyMessage, notifyErrorMessage, file);
        }

        /**
		 * Efetua uma transmissao
		 * 
		 * @param origem
		 * @param b
		 * @throws IOException
		 */
        private File transfer(Socket origem, long maxSize) throws IOException {
            File tmp = File.createTempFile("arclv", "ftrans");
            DataOutputStream output = new DataOutputStream(new FileOutputStream(tmp));
            DataInputStream input = new DataInputStream(origem.getInputStream());
            byte[] buffer = new byte[1024];
            long length = 0;
            long tam;
            while ((tam = input.read(buffer, 0, buffer.length)) != -1) {
                if (length + tam <= maxSize) {
                    length += tam;
                    output.write(buffer, 0, (int) tam);
                } else break;
            }
            if (length != maxSize) {
                input.close();
                output.close();
                throw new IOException("Invalid file or corrupted");
            }
            return tmp;
        }
    }

    /**
	 * 
	 * FileShareImpl.java
	 * 
	 * 
	 * Classe interna que efetua o envio de um arquivo via rede. Para que o
	 * envio n�o bloqueie a possibilidade de uma nova conex�o para download do
	 * arquivo, e executada em uma thread separada da thread original
	 * 
	 * 
	 * @see Jan 3, 2008
	 * 
	 * @author <a href="mailto:txithihausen@gmail.com">Ivo Augusto Andrade R
	 *         Calado</a>.
	 * @author <a href="mailto:thiagobrunoms@gmail.com">Thiago Bruno Melo de
	 *         Sales</a>.
	 * @since 0.1
	 * @version 0.1
	 * 
	 * <p>
	 * <b>Revisions:</b>
	 * 
	 * <p>
	 * <b>yyyymmdd USERNAME:</b>
	 * <ul>
	 * <li> VERSION
	 * </ul>
	 */
    private class TransferDown implements Runnable {

        private File file;

        private Socket client;

        public TransferDown(File file, Socket client) {
            this.file = file;
            this.client = client;
        }

        public void run() {
            try {
                byte[] cache = new byte[1024];
                DataInputStream reader = new DataInputStream(new FileInputStream(this.file));
                DataOutputStream writer = new DataOutputStream(this.client.getOutputStream());
                int tam;
                while ((tam = reader.read(cache)) != -1) writer.write(cache, 0, tam);
                writer.flush();
                writer.close();
                reader.close();
                this.client.close();
            } catch (IOException e) {
                ArCoLIVELogRepository.getInstance().log("The file tranfer to host " + client.getRemoteSocketAddress() + " has failed");
                return;
            }
            ArCoLIVELogRepository.getInstance().log("The file transfer to host " + client.getRemoteSocketAddress() + " has finished sucessfully");
        }
    }

    /**
	 * 
	 * FileShareImpl.java
	 * 
	 * Classe que efetua o controle do tempo em que um server socket podera
	 * ficar ativo,
	 * 
	 * @see Jan 3, 2008
	 * 
	 * @author <a href="mailto:txithihausen@gmail.com">Ivo Augusto Andrade R
	 *         Calado</a>.
	 * @author <a href="mailto:thiagobrunoms@gmail.com">Thiago Bruno Melo de
	 *         Sales</a>.
	 * @since 0.1
	 * @version 0.1
	 * 
	 * <p>
	 * <b>Revisions:</b>
	 * 
	 * <p>
	 * <b>yyyymmdd USERNAME:</b>
	 * <ul>
	 * <li> VERSION
	 * </ul>
	 */
    private class ServerSocketControllerUtil extends Thread {

        private long timeOut;

        private ServerSocket server;

        public ServerSocketControllerUtil(ServerSocket server, long timeOut) {
            this.timeOut = timeOut;
            this.server = server;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(this.timeOut);
                this.server.close();
            } catch (Exception e) {
                ArCoLIVELogRepository.getInstance().log("An unknown error occurred in fileShare server");
            }
        }
    }
}
