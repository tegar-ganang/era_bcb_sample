package com.sin.server;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.web.bindery.autobean.vm.AutoBeanFactorySource;
import com.sin.shared.autobean.ABeanFactory;
import com.sin.shared.autobean.AutoBeanHandlerImpl;
import com.sin.shared.domains.AllOnLineAutoBean;
import com.sin.shared.domains.CliCookie;
import com.sin.shared.domains.OnLineAutoBean;
import com.google.appengine.api.rdbms.AppEngineDriver;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.Environment;
import com.google.cloud.sql.jdbc.Connection;
import com.google.cloud.sql.jdbc.PreparedStatement;
import com.google.cloud.sql.jdbc.ResultSet;
import com.google.cloud.sql.jdbc.Statement;
import de.beimax.janag.NameGenerator;

public class ClientDao {

    private static final Logger log = Logger.getLogger(ClientDao.class.getName());

    private static String channelKey;

    private NameGenerator ng;

    private UUID uuidcr;

    private String server;

    private String statement;

    private String json;

    private Connection c;

    private Date now;

    public String getClientJsonStrFromMemCache(String uuid) {
        ng = new NameGenerator("languages.txt", "semantics.txt");
        String[] namegn = ng.getRandomName("Finnish", "Male", 1);
        uuidcr = UUID.randomUUID();
        now = new Date();
        ABeanFactory factory = AutoBeanFactorySource.create(ABeanFactory.class);
        AutoBeanHandlerImpl autoBeanHandlerImpl = new AutoBeanHandlerImpl();
        CliCookie cliCookie = autoBeanHandlerImpl.makeCliCookie(factory);
        cliCookie.setId(uuidcr.toString());
        cliCookie.setCity("Helsinki");
        cliCookie.setCreated_at(now);
        cliCookie.setUpdated_at(now);
        cliCookie.setNickname(namegn[0]);
        channelKey = null;
        if (channelKey == null) {
            channelKey = ChannelServiceFactory.getChannelService().createChannel(uuidcr.toString());
            log.info("channelKey created " + channelKey);
        } else {
            log.info("channelKey NOT created " + channelKey);
        }
        cliCookie.setToken(channelKey);
        log.info(cliCookie.toString());
        json = autoBeanHandlerImpl.serializeToJsonCliCookie(cliCookie);
        Environment env = ApiProxy.getCurrentEnvironment();
        if (env.getAttributes().get("com.google.appengine.runtime.default_version_hostname") != null) {
            server = env.getAttributes().get("com.google.appengine.runtime.default_version_hostname").toString();
        } else {
            server = "server";
        }
        java.sql.Timestamp nowsql = new java.sql.Timestamp(now.getTime());
        c = null;
        try {
            DriverManager.registerDriver(new AppEngineDriver());
            c = (Connection) DriverManager.getConnection("jdbc:google:rdbms://sinelga.com:sinelgamysql:sinelga/keskustelu");
            statement = "INSERT INTO online (id,nickname,channel,server,created_at,updated_at,status) VALUES(?,?,?,?,?,?,?)";
            PreparedStatement stmt = c.prepareStatement(statement);
            stmt.setString(1, uuidcr.toString());
            stmt.setString(2, namegn[0]);
            stmt.setString(3, channelKey);
            stmt.setString(4, server);
            stmt.setTimestamp(5, nowsql);
            stmt.setTimestamp(6, nowsql);
            stmt.setString(7, "connected");
            int success = 2;
            success = stmt.executeUpdate();
            if (success == 1) {
                log.info("Update Ok");
            } else if (success == 0) {
                log.severe("Update NOT Ok");
            }
        } catch (SQLException e) {
            log.severe(e.getMessage());
        } finally {
            if (c != null) try {
                c.close();
            } catch (SQLException ignore) {
            }
        }
        return json;
    }

    public String iamOnLineToAll(String clijson) {
        log.info("Start iam " + clijson);
        ABeanFactory factory = AutoBeanFactorySource.create(ABeanFactory.class);
        AutoBeanHandlerImpl autoBeanHandlerImpl = new AutoBeanHandlerImpl();
        String iam = autoBeanHandlerImpl.deserializeCliCookieFromJson(factory, clijson).getId();
        ChannelService channelService = ChannelServiceFactory.getChannelService();
        statement = "SELECT id,nickname from online where status='connected'";
        c = null;
        try {
            DriverManager.registerDriver(new AppEngineDriver());
            c = (Connection) DriverManager.getConnection("jdbc:google:rdbms://sinelga.com:sinelgamysql:sinelga/keskustelu");
            Statement st = c.createStatement();
            ResultSet rs = st.executeQuery(statement);
            List<OnLineAutoBean> listOnLineAutoBean = new ArrayList<OnLineAutoBean>();
            while (rs.next()) {
                log.info("Send to " + rs.getString(1) + " name " + rs.getString(2));
                OnLineAutoBean onLineAutoBean = autoBeanHandlerImpl.makeOnLineAutoBean(factory);
                onLineAutoBean.setId(rs.getString(1));
                onLineAutoBean.setNickname(rs.getString(2));
                listOnLineAutoBean.add(onLineAutoBean);
                if (!rs.getString(1).equals(iam)) {
                    ChannelMessage channelMessage = new ChannelMessage(rs.getString(1), clijson);
                    channelService.sendMessage(channelMessage);
                }
            }
            AllOnLineAutoBean allOnLineAutoBean = autoBeanHandlerImpl.makeAllOnLineAutoBean(factory);
            allOnLineAutoBean.setAllOnLineAutoBean(listOnLineAutoBean);
            json = autoBeanHandlerImpl.serializeToJsonAllOnLineAutoBean(allOnLineAutoBean);
        } catch (SQLException e) {
            log.severe(e.getMessage());
        }
        return json;
    }
}
