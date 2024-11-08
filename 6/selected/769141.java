package powerPlantDB;

import gui.Gui;
import gui.state.EditorState;
import gui.state.PropertyNames;
import gui.utils.Dialogs;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Scanner;
import javax.media.opengl.GLAutoDrawable;
import opengl.perspective.FirstPerson;
import powerPlantDB.tcpTraffic.TCPController;
import powerPlantDB.udpTraffic.OpenGLChat;
import powerPlantDB.udpTraffic.UDPController;
import util.Vector3d;
import landscape.SingleEntity;
import main.EditorApplication;

public class PowerPlantDB {

    protected static boolean isActive;

    public static boolean isActive() {
        return isActive;
    }

    protected static PositionSynchronizer posSynchronizer;

    protected static SceneSynchronizer sceneSynchronizer;

    protected static OpenGLChat chat;

    public static void draw(GLAutoDrawable gld) {
        try {
            if (chat == null) chat = new OpenGLChat(gld, TCPController.getUsername());
        } catch (SocketException e) {
            e.printStackTrace();
        }
        if (!EditorApplication.perspective.toString().equals(PropertyNames.CAMERA_MODE_FIRST_PERSON)) EditorState.getAppProperties().setProperty(PropertyNames.CAMERA_MODE, PropertyNames.CAMERA_MODE_FIRST_PERSON); else ((FirstPerson) EditorApplication.perspective).setOffset(500);
        for (SingleEntity se : PositionSynchronizer.playersToDraw.values()) {
            se.draw(gld);
        }
        chat.draw(gld);
    }

    public static boolean login(SocketAddress tcp, SocketAddress udp, String mailAddress, String password) {
        if (!TCPController.connect(tcp)) return false;
        if (!UDPController.connect(udp)) return false;
        return TCPController.login(mailAddress, password);
    }

    public static void logoff() {
        TCPController.disConnect();
        UDPController.disConnect();
    }

    public static void start(Gui gui, int posRate, int sceneRate) {
        if (posSynchronizer == null) posSynchronizer = new PositionSynchronizer();
        if (sceneSynchronizer == null) sceneSynchronizer = new SceneSynchronizer(gui);
        posSynchronizer.start(posRate, TCPController.getId());
        sceneSynchronizer.start(sceneRate, TCPController.getId());
        isActive = true;
    }

    public static void stop() {
        posSynchronizer.stop();
        sceneSynchronizer.stop();
        isActive = false;
    }
}
