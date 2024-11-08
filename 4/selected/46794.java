package org.traccar;

import org.jboss.netty.channel.*;
import org.traccar.model.DataManager;
import org.traccar.model.Position;

/**
 * Tracker message handler
 */
@ChannelHandler.Sharable
public class TrackerEventHandler extends SimpleChannelHandler {

    /**
     * Data manager
     */
    private DataManager dataManager;

    TrackerEventHandler(DataManager newDataManager) {
        super();
        dataManager = newDataManager;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        if (e.getMessage() instanceof Position) {
            Position position = (Position) e.getMessage();
            if (position == null) {
                System.out.println("null message");
            } else {
                System.out.println("id: " + position.getId() + ", deviceId: " + position.getDeviceId() + ", valid: " + position.getValid() + ", time: " + position.getTime() + ", latitude: " + position.getLatitude() + ", longitude: " + position.getLongitude() + ", altitude: " + position.getAltitude() + ", speed: " + position.getSpeed() + ", course: " + position.getCourse());
            }
            try {
                dataManager.addPosition(position);
            } catch (Exception error) {
                System.out.println(error.getMessage());
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        e.getChannel().close();
    }
}
