package com.kyte.api.example;

import com.kyte.api.rest.KyteInvoker;
import com.kyte.api.rest.KyteSession;
import com.kyte.api.service.ServiceFactory;
import com.kyte.api.service.ChannelService;
import com.kyte.api.service.UserService;
import com.kyte.api.util.ApiUtil;
import com.kyte.api.model.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.io.*;
import org.stringtree.json.JSONWriter;

/**
 *
 * Created: elliott, Mar 12, 2008
 */
public class HelloKyte {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(HelloKyte.class);

    public static final String API_HOST = "www.dtvsand.net";

    public static final int API_PORT = 80;

    public static final String API_KEY = "_test_partner";

    public static final String API_SECRET = "secret";

    public static final String TEST_LOGIN = "test";

    public static final String TEST_PASSWORD = "test";

    public static void main(String[] args) throws Exception {
        KyteSession session = getSession();
        testBrowseChannels(session);
        testCreateUser(session);
        String channelUri = testCreateChannel(session);
        Media media = testCreateMedia(session);
        testPostShow(session, channelUri, media.getUri());
        testPostShow2(session, channelUri);
    }

    public static KyteSession getSession() {
        KyteInvoker invoker = new KyteInvoker(API_HOST, API_PORT, API_KEY, API_SECRET);
        KyteSession session = new KyteSession(invoker);
        String serverTime = ServiceFactory.getUtilService().getServerTime(session);
        LOGGER.info("serverTime: " + serverTime);
        String passwordHash = ApiUtil.generatePasswordHash(TEST_PASSWORD, API_SECRET);
        String userTicket = ServiceFactory.getUserService().getTicket(session, TEST_LOGIN, passwordHash);
        LOGGER.info("userTicket: " + userTicket);
        session.setUserTicket(userTicket);
        KyteUser user = ServiceFactory.getUserService().fetchUserData(session);
        LOGGER.info("user: " + user);
        return session;
    }

    public static void testBrowseChannels(KyteSession session) {
        ChannelService channelService = ServiceFactory.getChannelService();
        Page<Channel> channels = channelService.browseChannels(session, "LATEST", 0, 5, null);
        LOGGER.info("channels: " + channels);
        Channel channel = channels.getItems().get(0);
        Page<Show> shows = channel.fetchShows(session, 0, 10);
        LOGGER.info("channel: " + channel.getUri() + " has " + shows.getTotalSize() + " shows.");
    }

    public static String testCreateChannel(KyteSession session) {
        Channel channel = ModelFactory.newChannel();
        String title = "japanese test 5 グ ヰ ぉ ま";
        channel.setTitle(title);
        Map<String, Object> channelData = new HashMap<String, Object>();
        channelData.put("title", title);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("channelData", channelData);
        Object obj = session.invoke("channelService", "createChannel", params);
        LOGGER.info("createChannel result: " + obj);
        return (String) obj;
    }

    public static Media testCreateMedia(KyteSession session) throws Exception {
        String fileName = "c:\\Sunset.jpg";
        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);
        Media media = ServiceFactory.getMediaService().createMedia(session, fis, file.length(), "image/jpg");
        LOGGER.info("createMediaresult: " + media);
        return media;
    }

    public static void testPostShow(KyteSession session, String channelUri, String mediaUri) {
        Channel channel = ServiceFactory.getChannelService().fetchChannel(session, channelUri);
        Map<String, Object> showData = new HashMap<String, Object>();
        showData.put("title", "a test show");
        String showUri = channel.postShow(session, showData, "", mediaUri);
        LOGGER.info("postShow result: " + showUri);
    }

    public static void testPostShow2(KyteSession session, String channelUri) throws Exception {
        Channel channel = ServiceFactory.getChannelService().fetchChannel(session, channelUri);
        String fileName = "c:\\Sunset.jpg";
        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);
        Map<String, Object> showData = new HashMap<String, Object>();
        showData.put("title", "a test show");
        String showUri = channel.postShow(session, showData, fis, file.length(), "image/jpg");
        LOGGER.info("postShow result: " + showUri);
    }

    public static void testCreateUser(KyteSession session) {
        KyteUser user = ModelFactory.newKyteUser();
        user.setUsername("johndoe");
        user.setEmail("somemail@someserver.net");
        user.setAgreedTosTime(new Date());
        Map auxData = new HashMap();
        auxData.put("password", "thepassword");
        UserService userService = ServiceFactory.getUserService();
        String ticket = userService.createUser(session, user, auxData);
        LOGGER.info("createUser result: " + ticket);
    }

    public static void testUnicode() {
        String title = "turkish ä Ş";
        LOGGER.info("title: " + title);
        JSONWriter jsonWriter = new JSONWriter();
        String json = jsonWriter.write(title);
        LOGGER.info("json: " + json);
    }
}
