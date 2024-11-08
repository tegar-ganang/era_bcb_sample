package client.entidades;

import client.Exception.ExceptionServidorNaoEncontrado;
import client.JanelaPrincipal;
import client.colecoes.ColecaoCanal;
import client.colecoes.ColecaoUsuario;
import comum.Message;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;
import proxy.EscritorSocket;
import proxy.ISocketPeer;

/**
 *
 * @author Anderson
 */
public class Client extends Observable implements Runnable, ISocketPeer {

    private Socket socket;

    private boolean run;

    private BufferedWriter out;

    private BufferedReader in;

    private EscritorSocket escritor;

    private ColecaoCanal channels = new ColecaoCanal();

    private ColecaoUsuario users = new ColecaoUsuario();

    private Client() {
        escritor = new EscritorSocket(this);
    }

    private static Client instance;

    public static Client getInstance() {
        if (instance == null) {
            instance = new Client();
        }
        return instance;
    }

    private Thread t;

    public void conectar() {
        if (!isConectado()) {
            t = new Thread(this);
            t.start();
        }
    }

    public void disconnect() {
        if (isConectado()) {
            escreverLinha("QUIT :saindo...");
            JanelaPrincipal.getInstancia().trocaStatus("Desconectado");
            stop();
        }
    }

    public ColecaoUsuario getUsers() {
        return users;
    }

    public void setUsers(ColecaoUsuario users) {
        this.users = users;
    }

    public ColecaoCanal getChannels() {
        return channels;
    }

    public void setChannels(ColecaoCanal channels) {
        this.channels = channels;
    }

    public void addChannel(Channel c) {
        this.channels.adicionarCanal(c);
    }

    public void rmChannel(Channel c) {
        this.channels.removerCanal(c);
    }

    public void addUser(User u) {
        this.users.adicionarUsuario(u);
    }

    public void rmUser(User u) {
        this.users.removerUsuario(u);
    }

    public void stop() {
        this.run = false;
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void escreverLinha(String linha) {
        if (isConectado()) {
            escritor.escreverLinha(linha);
        }
    }

    public void run() {
        try {
            this.socket = new Socket(ConfigConexao.getInstance().getServer(), ConfigConexao.getInstance().getIntegerPort());
            if (socket.isConnected()) {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                Thread t = new Thread(escritor);
                t.start();
                setChanged();
                notifyObservers(Boolean.TRUE);
                this.run = true;
                enviaNickeUser();
                while (run) {
                    String leitura = in.readLine();
                    setChanged();
                    notifyObservers(leitura);
                }
            }
            setChanged();
            notifyObservers(Boolean.FALSE);
        } catch (Exception e) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, e);
            new ExceptionServidorNaoEncontrado(e);
            this.stop();
        }
    }

    public BufferedWriter getBuffWriter() {
        return this.out;
    }

    public BufferedReader getBuffReader() {
        return this.in;
    }

    public Socket getSocket() {
        return this.socket;
    }

    public boolean isConectado() {
        return socket != null && socket.isConnected() && run;
    }

    public void update(Observable o, Object arg) {
        if (arg instanceof String) {
            String leitura = (String) arg;
            Message m = new Message(leitura);
            setChanged();
            notifyObservers(m.toString());
        }
    }

    private void enviaNickeUser() {
        escreverLinha("NICK " + ConfigUsuario.getInstance().getNickName());
        escreverLinha("USER " + ConfigUsuario.getInstance().getUserId() + " 0 * :" + ConfigUsuario.getInstance().getNome());
    }

    public boolean existeCanal(String nome) {
        for (Channel c : Client.getInstance().channels.getCanais()) {
            if (c.getNome().equals(nome)) {
                return true;
            }
        }
        return false;
    }

    public boolean existeUsuario(String nome) {
        for (User u : Client.getInstance().users.getUsuarios()) {
            if (u.getNick().equals(nome)) {
                return true;
            }
        }
        return false;
    }
}
