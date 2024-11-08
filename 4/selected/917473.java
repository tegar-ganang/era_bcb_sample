package com.peterhi.player.handlers;

import static com.peterhi.player.ResourceLocator.*;
import java.net.URL;
import java.nio.channels.DatagramChannel;
import javax.swing.SwingUtilities;
import javax.swing.ImageIcon;
import com.peterhi.net.messages.*;
import com.peterhi.player.shapes.*;
import com.peterhi.client.SocketClient;
import com.peterhi.client.SocketHandler;
import com.peterhi.player.ChannelDialog;
import com.peterhi.player.Classroom;
import com.peterhi.player.Classmate;
import com.peterhi.player.Window;
import com.peterhi.player.Whiteboard;
import com.peterhi.player.Elements;
import com.peterhi.player.ElementsTreeModel;
import com.peterhi.Code;
import com.peterhi.State;
import com.peterhi.client.DatagramClient;

public class EnterChannelResponseHandler implements SocketHandler<EnterChannelResponse> {

    public void handle(final EnterChannelResponse response) {
        final ChannelDialog c = ChannelDialog.getChannelDialog();
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                switch(response.code) {
                    case Code.OK:
                        c.reset();
                        c.setFrozen(false);
                        c.setVisible(false);
                        int state = Window.getWindow().getState();
                        int role = State.getRole(response.state);
                        switch(role) {
                            case State.STUDENT:
                                state = State.setRole(state, State.STUDENT);
                                break;
                            case State.TEACHER:
                                state = State.setRole(state, State.TEACHER);
                                break;
                            case State.MODERATOR:
                                state = State.setRole(state, State.MODERATOR);
                                break;
                        }
                        state = State.set(state, State.IN_A_CHANNEL, true);
                        state = State.set(state, State.AVAILABLE_OR_AWAY, true);
                        Window.getWindow().displayName = response.displayName;
                        Window.getWindow().setState(state, State.IN_A_CHANNEL);
                        break;
                    case Code.NOT_FOUND:
                        c.setStatusText(getString(c, "NOT_FOUND"));
                        c.setFrozen(false);
                        Window.getWindow().channel = null;
                        break;
                    case Code.DENIED:
                        c.setStatusText(getString(c, "DENIED"));
                        c.setFrozen(false);
                        Window.getWindow().channel = null;
                        break;
                    case Code.UNKNOWN:
                        c.setStatusText(getString(c, "UNKNOWN"));
                        c.setFrozen(false);
                        Window.getWindow().channel = null;
                        break;
                }
                if (response.ids != null && response.ids.length > 0) {
                    Classroom cr = Classroom.getClassroom();
                    for (int i = 0; i < response.ids.length; i++) {
                        int id = response.ids[i];
                        String email = response.emails[i];
                        String displayName = response.displayNames[i];
                        int state = response.states[i];
                        Classmate cm = new Classmate();
                        cm.setId(id);
                        cm.setEmail(email);
                        cm.setDisplayName(displayName);
                        cm.setState(state);
                        cr.getClassmateModel().add(cm);
                        reconnect(cm);
                    }
                    cr.updateTableUI();
                }
                Elements elements = Elements.getElements();
                ElementsTreeModel model = elements.getModel();
                Whiteboard w = Whiteboard.getWhiteboard();
                if (response.shapes != null && response.shapes.length > 0) {
                    Shape[] array = convert(response.shapes);
                    for (Shape item : array) {
                        model.add(item);
                    }
                }
                elements.updateTreeUI();
                w.repaint();
            }
        });
    }

    private void reconnect(Classmate cm) {
        DatagramClient dc = DatagramClient.getInstance();
        InitiateSessionMessage message = new InitiateSessionMessage();
        message.sender = Window.getWindow().id;
        message.receiver = cm.getId();
        int state = cm.getState();
        state = State.set(state, State.CONNECTING, true);
        cm.setState(state);
        try {
            DatagramChannel ch = dc.add(cm.getId());
            ch.connect(SocketClient.getInstance().getRemoteSocketAddress());
            System.out.println("CONNECTED!!!");
            ch.disconnect();
            ch.connect(new java.net.InetSocketAddress("localhost", 9900));
            System.out.println("CONNECTED AGAIN!!!");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private Shape[] convert(ShapeMessage[] messages) {
        Shape[] ret = new Shape[messages.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = toShape(messages[i]);
        }
        return ret;
    }

    private Shape toShape(ShapeMessage message) {
        Shape ret = null;
        if (message instanceof RectangleMessage) {
            Rectangle shp = new Rectangle(false);
            RectangleMessage msg = (RectangleMessage) message;
            shp.width = msg.width;
            shp.height = msg.height;
            ret = shp;
        } else if (message instanceof EllipseMessage) {
            Ellipse shp = new Ellipse(false);
            EllipseMessage msg = (EllipseMessage) message;
            shp.width = msg.width;
            shp.height = msg.height;
            ret = shp;
        } else if (message instanceof LineMessage) {
            Line shp = new Line(false);
            LineMessage msg = (LineMessage) message;
            shp.x2 = msg.x2;
            shp.y2 = msg.y2;
            ret = shp;
        } else if (message instanceof PolygonMessage) {
            Polygon shp = new Polygon(false);
            PolygonMessage msg = (PolygonMessage) message;
            shp.xs = msg.xs;
            shp.ys = msg.ys;
            ret = shp;
        } else if (message instanceof PolylineMessage) {
            Polyline shp = new Polyline(false);
            PolylineMessage msg = (PolylineMessage) message;
            shp.xs = msg.xs;
            shp.ys = msg.ys;
            ret = shp;
        } else if (message instanceof TextMessage) {
            Text shp = new Text(false);
            TextMessage msg = (TextMessage) message;
            shp.width = msg.width;
            shp.height = msg.height;
            shp.text = msg.text;
            ret = shp;
        } else if (message instanceof URLImageMessage) {
            Image shp = new Image(false);
            URLImageMessage msg = (URLImageMessage) message;
            shp.width = msg.width;
            shp.height = msg.height;
            try {
                shp.image = new ImageIcon(new URL(msg.url)).getImage();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            ret = shp;
        }
        if (ret != null) {
            ret.name = message.name;
            ret.x = message.x;
            ret.y = message.y;
        }
        return ret;
    }
}
