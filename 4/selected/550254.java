package logic;

import enums.ServiceTags;
import framework.IATMSModel;
import framework.IClientThread;
import framework.IPlaneUpdateObject;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.UserInfo;
import framework.IUserInfo;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import messages.PlaneDataObject;
import messages.PlaneUpdateObject;
import messages.ServiceObject;

/**
 *
 * @author swift
 */
public class ClientThread implements IClientThread, Runnable {

    private IATMSModel model;

    private ObjectInputStream in;

    private Socket clientSocket;

    private Object input;

    private ServiceObject so;

    private IUserInfo ui;

    private static final int SENDKEEPALIVEMINUTES = 5;

    private static final int DELAY = 60000;

    private MyTimerTask timerTask;

    private Timer timer;

    public ClientThread(Socket clientSocket, IATMSModel model) {
        this.model = model;
        this.clientSocket = clientSocket;
        try {
            in = new ObjectInputStream(clientSocket.getInputStream());
            ui = new UserInfo(null, null, new ObjectOutputStream(clientSocket.getOutputStream()), this);
        } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        timer = new Timer();
        timerTask = new MyTimerTask();
        timer.schedule(timerTask, SENDKEEPALIVEMINUTES * 60000L, SENDKEEPALIVEMINUTES * 60000L);
    }

    public void run() {
        try {
            while ((input = in.readObject()) != null) {
                timerTask.setActive();
                if (input instanceof ServiceObject) {
                    so = (ServiceObject) input;
                    if (so.isTag(ServiceTags.KeepAlive)) {
                        System.out.println("sending keepalive");
                        ui.getOOS().writeObject(new ServiceObject("Server", ServiceTags.KeepAlive));
                    } else if (so.isTag(ServiceTags.ClientLoggedIn)) {
                        if (model.isNameUnique(so.getSenderName())) {
                            ui.setNickName(so.getSenderName());
                            model.addUserInfo(ui);
                            System.out.println(so.getSenderName() + " logged in.");
                            ServiceObject sout = new ServiceObject("Server", ServiceTags.FunctionList);
                            sout.setFunctions(model.getFuncs());
                            System.out.println("Sending functionlist to: " + ui.getNickName());
                            ui.getOOS().writeObject(sout);
                            model.getServer().sendUserList(ui);
                            model.sendUpdateList(ui);
                        } else {
                            System.out.println(so.getSenderName() + " already exists, closing connection.");
                            ui.getOOS().writeObject(new ServiceObject("Server", ServiceTags.NameAlreadyExists));
                            throw new IOException();
                        }
                    } else if (so.isTag(ServiceTags.chatMessage)) {
                        ServiceObject sout = new ServiceObject(ui.getNickName(), ServiceTags.chatMessage);
                        sout.setValue(so.getValue());
                        model.getServer().broadcast(sout, ui);
                    } else if (so.isTag(ServiceTags.ClientLoggedOut)) {
                        throw new IOException();
                    } else if (so.isTag(ServiceTags.FunctionChoosen)) {
                        System.out.println("User " + ui.getNickName() + " choose function " + so.getValue());
                        if (ui.getFunction() != null) {
                            System.out.println("old function was: " + ui.getFunction());
                            model.addFunction(ui.getFunction());
                        }
                        ui.setFunction(so.getValue());
                        model.removeFunction(so.getValue());
                        model.updateUserInfo(ui);
                    }
                } else if (input instanceof PlaneUpdateObject) {
                    model.addPlaneUpdate((IPlaneUpdateObject) input, ui);
                } else if (input instanceof PlaneDataObject) {
                    PlaneDataObject pdo = (PlaneDataObject) input;
                    model.getServer().broadcast(input);
                }
            }
        } catch (IOException ex) {
            System.err.println(ui.getNickName() + ": logged out");
            model.addFunction(ui.getFunction());
            model.removeUserInfo(ui);
            if (ui.getFunction() != null) model.resetPlanes(ui.getFunction());
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private class MyTimerTask extends TimerTask {

        private long start;

        public MyTimerTask() {
            start = System.currentTimeMillis();
        }

        public void run() {
            if (System.currentTimeMillis() - start > (SENDKEEPALIVEMINUTES * 60000L) + DELAY) {
            }
        }

        public void setActive() {
            start = System.currentTimeMillis();
        }
    }
}
