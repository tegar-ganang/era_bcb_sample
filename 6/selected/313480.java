package client;

import com.sun.imageio.spi.RAFImageInputStreamSpi;
import tearsol.*;
import com.ssc.tnet.*;
import com.ssc.tnetmsg.*;
import java.util.LinkedList;
import client.users.*;
import org.lwjgl.input.*;
import org.lwjgl.opengl.GL11;

public class Game implements TNetTCPWrapperCallback, TNetUDPWrapperCallback {

    public TNetTCPWrapper m_MyTNetWrapper = null;

    public TNetUDPWrapper m_MyUDPWrapper = null;

    LinkedList fov_p;

    public local me;

    public Game() {
        fov_p = new LinkedList();
        me = new local();
        m_MyTNetWrapper = new TNetTCPWrapper(600, this);
        m_MyUDPWrapper = new TNetUDPWrapper(600, this);
    }

    public void drawGrid() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glPushMatrix();
        GL11.glBegin(GL11.GL_LINES);
        for (int z = -15; z <= 15; z++) {
            GL11.glColor3f(1, 1, 1);
            if (z == 0) GL11.glColor3f(0, 0, 1);
            GL11.glVertex3f(-15, -2, z);
            GL11.glVertex3f(15, -2, z);
            for (int x = -15; x <= 15; x++) {
                GL11.glColor3f(1, 1, 1);
                if (x == 0) GL11.glColor3f(1, 0, 0);
                GL11.glVertex3f(x, -2, -15);
                GL11.glVertex3f(x, -2, 15);
            }
        }
        GL11.glEnd();
        GL11.glPopMatrix();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public void connect() {
        me.init(0, -2, -10.0f, 90.0f, 0.0f);
        if (m_MyTNetWrapper == null) {
            System.out.println("m_MyTNetWrapper == null");
            return;
        }
        if (!m_MyTNetWrapper.connect("65.23.155.44", 5050)) {
            System.out.println("TNetTCPWrapperTester - connect failed.");
            return;
        }
        m_MyTNetWrapper.login("Robert", "robertpwd");
    }

    public void login_response() {
        if (m_MyTNetWrapper.m_AccountID == 0) {
            System.out.println("Wrong password");
            return;
        }
        if (m_MyTNetWrapper.m_AccountID == 1) {
            System.out.println("Username doesnt exist");
            return;
        }
        if (m_MyTNetWrapper.m_AccountID == 2) {
            System.out.println("User is already logged in");
            return;
        }
        me.setUID(m_MyTNetWrapper.m_AccountID);
        System.out.println("connected!");
        m_MyUDPWrapper.startUDPPump("65.23.155.44", 5051);
    }

    public void account_list_response() {
    }

    public void buddy_list_response() {
    }

    public void character_list_response() {
    }

    public void private_chat_response(int SenderID, String text) {
    }

    public void send_entity_location() {
        m_MyUDPWrapper.m_CharacterID = me.getUID();
        m_MyUDPWrapper.m_dPosX = me.getX();
        m_MyUDPWrapper.m_dPosY = me.getY();
        m_MyUDPWrapper.m_dPosZ = me.getZ();
    }

    public void entity_list_response() {
        if (m_MyUDPWrapper.m_CharacterID != 666) {
            if (m_MyUDPWrapper.m_CharacterID != me.getUID()) {
                if (!updateChar(m_MyUDPWrapper.m_CharacterID, (float) m_MyUDPWrapper.m_dPosX, (float) m_MyUDPWrapper.m_dPosY, (float) m_MyUDPWrapper.m_dPosZ)) {
                    System.out.println("adding character");
                    addChar(m_MyUDPWrapper.m_CharacterID, (float) m_MyUDPWrapper.m_dPosX, (float) m_MyUDPWrapper.m_dPosY, (float) m_MyUDPWrapper.m_dPosZ);
                } else {
                }
            } else {
            }
        }
    }

    public boolean addChar(int ID, float x, float y, float z) {
        remote r = new remote();
        r.init(x, 0, z, 90.0f, 0.0f);
        r.add_waypoint(x, 0, z, 0, 0, 0);
        r.setUID(ID);
        fov_p.add(r);
        return true;
    }

    public boolean updateChar(int ID, float x, float y, float z) {
        for (int i = 0; i < fov_p.size(); i++) {
            remote r = (remote) fov_p.get(i);
            if (r.getUID() == ID) {
                float[] get = r.getWP(r.getWPSize() - 1);
                if (get[0] != x || get[2] != z) {
                    r.add_waypoint(x, 0, z, 0.0f, 0.0f, 0.0f);
                    return true;
                }
                return true;
            }
        }
        return false;
    }

    public void onInput() {
        if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
            me.incAng(.5f, 0.0f);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
            me.incAng(-.5f, 0.0f);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
            float dx, dz;
            dx = (float) Math.cos(Math.toRadians(me.getHAng())) * me.getSpeed();
            dz = (float) Math.sin(Math.toRadians(me.getHAng())) * me.getSpeed();
            me.incPos(dx, 0, -dz);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
            float dx, dz;
            dx = (float) Math.cos(Math.toRadians(me.getHAng())) * me.getSpeed();
            dz = (float) Math.sin(Math.toRadians(me.getHAng())) * me.getSpeed();
            me.incPos(-dx, 0, dz);
        }
    }

    public void render() {
        me.render();
        for (int i = 0; i < fov_p.size(); i++) {
            remote r = (remote) fov_p.get(i);
            r.update();
            r.render();
        }
    }
}
