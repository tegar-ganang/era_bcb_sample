package com.sin.server;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.cloud.sql.jdbc.Connection;
import com.google.cloud.sql.jdbc.PreparedStatement;
import com.google.cloud.sql.jdbc.ResultSet;
import com.google.cloud.sql.jdbc.Statement;
import com.google.gson.Gson;
import com.google.web.bindery.autobean.vm.AutoBeanFactorySource;
import com.sin.shared.autobean.ABeanFactory;
import com.sin.shared.autobean.AutoBeanHandlerImpl;
import com.sin.shared.domains.AllOnLineAutoBean;
import com.sin.shared.domains.CliCookie;
import com.sin.shared.domains.CliCookieImpl;
import com.sin.shared.domains.MessageAutoBean;
import com.sin.shared.domains.OnLineAutoBean;
import com.sin.shared.domains.SmsImages;

public class CliCookieDaoImpl implements CliCookieDao {

    private static final Logger log = Logger.getLogger(CliCookieDaoImpl.class.getName());

    Connection connection = null;

    PreparedStatement ptmt = null;

    ResultSet resultSet = null;

    private Date now;

    private CliCookieImpl cliCookieImpl;

    private static MemcacheService ms;

    private Gson gson;

    private String statement;

    private AutoBeanHandlerImpl autoBeanHandlerImpl;

    private ABeanFactory factory;

    private String json;

    private String iam;

    private List<CliCookieImpl> listCliCookieImpl;

    private int cronservletcount = 0;

    private static String token;

    private static List<SmsImages> smsimagesArrList;

    private static URL url;

    public CliCookieDaoImpl() {
    }

    private Connection getConnection() throws SQLException {
        Connection conn;
        conn = ConnectionFactory.getInstance().getConnection();
        return conn;
    }

    @Override
    public void insert(CliCookieImpl cliCookie) {
        now = new Date();
        java.sql.Timestamp nowsql = new java.sql.Timestamp(now.getTime());
        String queryString = "INSERT INTO clicookie(id,created_at,updated_at,hits,country,city,facebookid,regionname,latitude,longitude,regioncode,ip,avatar,nickname,token,server) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            connection = getConnection();
            ptmt = connection.prepareStatement(queryString);
            ptmt.setString(1, cliCookie.getId());
            ptmt.setTimestamp(2, nowsql);
            ptmt.setTimestamp(3, nowsql);
            ptmt.setInt(4, cliCookie.getHits());
            ptmt.setString(5, cliCookie.getCountry());
            ptmt.setString(6, cliCookie.getCity());
            ptmt.setString(7, cliCookie.getFacebookid());
            ptmt.setString(8, cliCookie.getRegionname());
            ptmt.setString(9, cliCookie.getLatitude());
            ptmt.setString(10, cliCookie.getLongitude());
            ptmt.setString(11, cliCookie.getRegioncode());
            ptmt.setString(12, cliCookie.getIp());
            ptmt.setString(13, cliCookie.getAvatar());
            ptmt.setString(14, cliCookie.getNickname());
            ptmt.setString(15, cliCookie.getToken());
            ptmt.setString(16, cliCookie.getServer());
            ptmt.executeUpdate();
        } catch (SQLException e) {
            log.severe(e.getMessage());
        } finally {
            try {
                if (ptmt != null) ptmt.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void update(CliCookieImpl cliCookie) {
    }

    @Override
    public void delete(String cliCookieId) {
    }

    @Override
    public List<CliCookieImpl> findAll() {
        statement = "SELECT id,created_at,updated_at,hits,country,city,facebookid,regionname,latitude,longitude,regioncode,ip,avatar,nickname,token,server,status from clicookie where status='connected' order by updated_at desc";
        List<CliCookieImpl> listCliCookieImpl = new ArrayList<CliCookieImpl>();
        try {
            connection = getConnection();
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(statement);
            while (rs.next()) {
                CliCookieImpl cliCookieImpl = new CliCookieImpl();
                cliCookieImpl.setId(rs.getString(1));
                cliCookieImpl.setCreated_at(rs.getTimestamp(2));
                cliCookieImpl.setUpdated_at(rs.getTimestamp(3));
                cliCookieImpl.setHits(rs.getInt(4));
                cliCookieImpl.setCountry(rs.getString(5));
                cliCookieImpl.setCity(rs.getString(6));
                cliCookieImpl.setFacebookid(rs.getString(7));
                cliCookieImpl.setRegionname(rs.getString(8));
                cliCookieImpl.setLatitude(rs.getString(9));
                cliCookieImpl.setLongitude(rs.getString(10));
                cliCookieImpl.setRegioncode(rs.getString(11));
                cliCookieImpl.setIp(rs.getString(12));
                cliCookieImpl.setAvatar(rs.getString(13));
                cliCookieImpl.setNickname(rs.getString(14));
                cliCookieImpl.setToken(rs.getString(15));
                cliCookieImpl.setServer(rs.getString(16));
                cliCookieImpl.setStatus(rs.getString(17));
                listCliCookieImpl.add(cliCookieImpl);
            }
            return listCliCookieImpl;
        } catch (SQLException e) {
            log.severe(e.getMessage());
        } finally {
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public CliCookieImpl findByKey(String cliCookieId) {
        return null;
    }

    @Override
    public String getClientJsonStrFromMemCache(String uuid) {
        ms = MemcacheServiceFactory.getMemcacheService();
        if (ms.contains(uuid)) {
            cliCookieImpl = (CliCookieImpl) ms.get(uuid);
            gson = new Gson();
            String json = gson.toJson(cliCookieImpl);
            ABeanFactory factory = AutoBeanFactorySource.create(ABeanFactory.class);
            AutoBeanHandlerImpl autoBeanHandlerImpl = new AutoBeanHandlerImpl();
            CliCookie cliCookie = autoBeanHandlerImpl.deserializeCliCookieFromJson(factory, json);
            now = new Date();
            cliCookie.setCreated_at(now);
            cliCookie.setUpdated_at(now);
            json = autoBeanHandlerImpl.serializeToJsonCliCookie(cliCookie);
            return json;
        } else {
            log.severe("NO ciCookieImpl in ms!!!!");
        }
        return null;
    }

    @Override
    public String iamOnLineToAll(String clijson) {
        ABeanFactory factory = AutoBeanFactorySource.create(ABeanFactory.class);
        AutoBeanHandlerImpl autoBeanHandlerImpl = new AutoBeanHandlerImpl();
        List<OnLineAutoBean> listOnLineAutoBean = new ArrayList<OnLineAutoBean>();
        if (!clijson.equals("refresh")) {
            listCliCookieImpl = findAll();
            CliCookie cliCookie = autoBeanHandlerImpl.deserializeCliCookieFromJson(factory, clijson);
            iam = cliCookie.getId();
            for (CliCookieImpl cliCookieImpl : listCliCookieImpl) {
                ChannelService channelService = ChannelServiceFactory.getChannelService();
                if (!cliCookieImpl.getId().equals(iam)) {
                    OnLineAutoBean onLineAutoBean = autoBeanHandlerImpl.makeOnLineAutoBean(factory);
                    onLineAutoBean.setId(cliCookieImpl.getId());
                    onLineAutoBean.setNickname(cliCookieImpl.getNickname());
                    onLineAutoBean.setAvatar(cliCookieImpl.getAvatar());
                    listOnLineAutoBean.add(onLineAutoBean);
                    MessageAutoBean messageAutoBean = autoBeanHandlerImpl.makeMessageAutoBean(factory);
                    messageAutoBean.setAbout("manconnected");
                    messageAutoBean.setContext(clijson);
                    String jsonstr = autoBeanHandlerImpl.serializeToJsonMessageAutoBean(messageAutoBean);
                    ChannelMessage newCliConnectedMsg = new ChannelMessage(cliCookieImpl.getId(), jsonstr);
                    channelService.sendMessage(newCliConnectedMsg);
                }
            }
        } else {
            listCliCookieImpl = findAll();
            for (CliCookieImpl cliCookieImpl : listCliCookieImpl) {
                OnLineAutoBean onLineAutoBean = autoBeanHandlerImpl.makeOnLineAutoBean(factory);
                onLineAutoBean.setId(cliCookieImpl.getId());
                onLineAutoBean.setNickname(cliCookieImpl.getNickname());
                onLineAutoBean.setAvatar(cliCookieImpl.getAvatar());
                listOnLineAutoBean.add(onLineAutoBean);
            }
            cronservletcount = cronservletcount + 1;
            if (cronservletcount > 4) {
                cronservletcount = 0;
                Queue queue = QueueFactory.getDefaultQueue();
                queue.add(TaskOptions.Builder.withUrl("/cronservlet"));
            }
        }
        AllOnLineAutoBean allOnLineAutoBean = autoBeanHandlerImpl.makeAllOnLineAutoBean(factory);
        allOnLineAutoBean.setAllOnLineAutoBean(listOnLineAutoBean);
        json = autoBeanHandlerImpl.serializeToJsonAllOnLineAutoBean(allOnLineAutoBean);
        return json;
    }

    @Override
    public String getAllSmsImages(String ragionname) {
        return "smsimagesArrList";
    }

    @Override
    public int UpdateConnect(String id, String status) {
        statement = "update clicookie set status=?,updated_at=now() where id=?";
        try {
            connection = getConnection();
            ptmt = connection.prepareStatement(statement);
            ptmt.setString(1, status);
            ptmt.setString(2, id);
            int count = ptmt.executeUpdate();
            log.warning("Was updated " + count);
            return count;
        } catch (SQLException e) {
            log.severe(e.getMessage());
        } finally {
            try {
                if (ptmt != null) ptmt.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    @Override
    public String reconnect_channel(String id) {
        log.severe("Ok lets reconnect  " + id);
        token = ChannelServiceFactory.getChannelService().createChannel(id);
        if (token != null) {
            return token;
        } else {
            log.severe("token null can't create");
            return null;
        }
    }
}
