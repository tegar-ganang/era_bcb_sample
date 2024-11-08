package net.kortsoft.gameportlet.rooms.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import javax.portlet.ActionResponse;
import net.kortsoft.gameportlet.model.GameType;
import net.kortsoft.gameportlet.model.impl.GameTypeImpl;
import net.kortsoft.gameportlet.model.impl.PlayerRangeImpl;
import net.kortsoft.gameportlet.model.impl.PortletGameInvoker;
import net.kortsoft.gameportlet.model.impl.PortletGameInvokerFactory;
import net.kortsoft.gameportlet.service.GameTypeService;
import net.kortsoft.gameportlet.service.RoomsService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.portlet.bind.annotation.ActionMapping;

@Controller
@RequestMapping("VIEW")
public class InitTestController extends BaseModelController {

    private static final Log log = LogFactory.getLog(InitTestController.class);

    private RoomsService roomsService;

    private GameTypeService gameTypeService;

    private PortletGameInvokerFactory portletGameInvokerFactory;

    @ActionMapping(params = "action=testInit")
    public void testSave(ActionResponse response) {
        try {
            prepareGameType();
            response.setRenderParameter("action", "");
        } catch (RuntimeException e) {
            response.setRenderParameter("action", "error");
            response.setRenderParameter("error", e.toString());
        }
    }

    private GameType prepareGameType() {
        Iterator<GameType> gameTypes = getGameTypeService().listAll().iterator();
        GameType gameType;
        if (gameTypes.hasNext()) gameType = gameTypes.next(); else {
            gameType = createTestGameType();
        }
        return gameType;
    }

    private GameType createTestGameType() {
        GameType gameType;
        GameTypeImpl gameTypeImpl = new GameTypeImpl();
        gameTypeImpl.setName("Agricola");
        gameTypeImpl.setPlayerRange(new PlayerRangeImpl(1, 5));
        try {
            gameTypeImpl.setThumbnail(readFile("/thumbnail.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        gameTypeImpl.setInvokerClass(PortletGameInvoker.class);
        gameTypeImpl.setSettings(PortletGameInvoker.createSettings("testgame_WAR_rooms", "showGame"));
        gameTypeImpl.getSettings().put("availableTeamColours", "red,blue,purple,green,natural");
        log.info("Creating test gameType");
        gameType = getGameTypeService().storeGameType(gameTypeImpl);
        return gameType;
    }

    private byte[] readFile(String resource) throws IOException {
        byte[] buf = new byte[4096];
        InputStream is = getClass().getResourceAsStream(resource);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int read = 0;
        while ((read = is.read(buf)) > 0) {
            os.write(buf, 0, read);
        }
        is.close();
        return os.toByteArray();
    }

    public RoomsService getRoomsService() {
        return roomsService;
    }

    public void setRoomsService(RoomsService roomsService) {
        this.roomsService = roomsService;
    }

    public GameTypeService getGameTypeService() {
        return gameTypeService;
    }

    public void setGameTypeService(GameTypeService gameTypeService) {
        this.gameTypeService = gameTypeService;
    }

    public PortletGameInvokerFactory getPortletGameInvokerFactory() {
        return portletGameInvokerFactory;
    }

    public void setPortletGameInvokerFactory(PortletGameInvokerFactory portletGameInvokerFactory) {
        this.portletGameInvokerFactory = portletGameInvokerFactory;
    }
}
