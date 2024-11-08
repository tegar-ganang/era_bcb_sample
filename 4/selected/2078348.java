package de.jochenbrissier.backyard;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * 
 * The main / entry point of Backyard.
 * 
 *<br>
 * 
 *This class declares the main part of the API.
 * 
 *<br>
 * <br>
 * 
 *How to ues:
 * 
 *<br>
 * 
 *Backyard backyard = new Backyard(requeset,response); <br>
 * <br>
 * 
 * ba.startAsync();
 * 
 * <br>
 * <br>
 * 
 * To resume the async event. call <br>
 * <br>
 * 
 * ba.stopAsync();
 * 
 * <br>
 * <br>
 * 
 * 
 * Thats it.!! easy :)
 * 
 * 
 * 
 * @author jochenbrissier
 * 
 */
public class Backyard {

    Log log = LogFactory.getLog(Backyard.class);

    private static Injector in = Guice.createInjector(new BackyardModule());

    private static ChannelHandler channelhandler = in.getInstance(ChannelHandler.class);

    private static MemberHandler memberhandler = in.getInstance(MemberHandler.class);

    private Member member;

    private HttpServlet servlet;

    private HttpServletRequest req;

    private HttpServletResponse resp;

    /**
	 *Constructor of Backyard.
	 * 
	 * @param req
	 *            a HttpServletRequest object
	 * @param resp
	 *            a HttpServeltRespons object
	 */
    public Backyard(HttpServletRequest req, HttpServletResponse resp) {
        this.resp = resp;
        this.req = req;
        this.member = memberhandler.getMember(req.getSession().getId());
        listenToChannel(0);
    }

    /**
	 * Backyard also provides a alternative implementations for a specific
	 * servlet container such as Tomcat, Jetty or Glassfish You can also write
	 * your own implementation for a other servlet container. <br>
	 * <br>
	 * 
	 * First you have to extend the class BackyardEvent and implement the
	 * methods. <br>
	 * 
	 * Second you have to write a second class and extend it with AbstractModule
	 * from google guice project. <br>
	 * In the module class you have to call
	 * bind(Event.class).to(Yourclass.class);
	 * 
	 * <br>
	 * 
	 * Third you have to invoke Backyard.setAlternativeImpl(new YourModule());
	 * 
	 * @param ba
	 */
    public static void setAlternativeImpl(AbstractModule ba) {
        in = Guice.createInjector(ba);
    }

    /**
	 * Backyard also provides a alternative implementations for a specific
	 * servlet container such as Tomcat, Jetty or Glassfish You can also write
	 * your own implementation for a other servlet container. <br>
	 * <br>
	 * 
	 * First you have to extend the class BackyardEvent and implement the
	 * methods. <br>
	 * 
	 * Second you have to write a second class and extend it with AbstractModule
	 * from google guice project. <br>
	 * In the module class you have to call
	 * bind(Event.class).to(Yourclass.class);
	 * 
	 * <br>
	 * 
	 * Third you have to invoke Backyard.setAlternativeImpl(new YourModule());
	 * 
	 * @param ba
	 */
    public static void setAlternativeImpl(AbstractModule ba, AbstractModule ab) {
        in = Guice.createInjector(ba, ab);
    }

    /**
	 * Backyard also provides a alternative implementations for a specific
	 * servlet container such as Tomcat, Jetty or Glassfish You can also write
	 * your own implementation for a other servlet container. <br>
	 * <br>
	 * 
	 * First you have to extend the class BackyardEvent and implement the
	 * methods. <br>
	 * 
	 * Second you have to write a second class and extend it with AbstractModule
	 * from google guice project. <br>
	 * In the module class you have to call
	 * bind(Event.class).to(Yourclass.class);
	 * 
	 * <br>
	 * 
	 * Third you have to invoke Backyard.setAlternativeImpl(new YourModule());
	 * 
	 * @param ba
	 */
    public static void setAlternativeImpl(Iterable<? extends Module> ba) {
        in = Guice.createInjector(ba);
    }

    /**
	 * set a other member to backyard obj. standart member is the member from
	 * which the request comes
	 * 
	 * @param member
	 */
    public void setMember(Member member) {
        this.member = member;
    }

    /**
	 * get servelt has in this version no effect
	 * 
	 * @return
	 */
    public HttpServlet getServlet() {
        return servlet;
    }

    /**
	 * set the servelt has in this version no effect
	 * 
	 * @return
	 */
    public void setServlet(HttpServlet servlet) {
        this.servlet = servlet;
    }

    /**
	 * Returns the meta channel The meta channel contains all members.
	 * 
	 * 
	 * 
	 * @return
	 */
    public static Channel getMetaChannel() {
        return channelhandler.getChannel(0);
    }

    /**
	 * returns the member object
	 * 
	 * @return
	 */
    public Member getMember() {
        return memberhandler.getMember(this.req.getSession().getId());
    }

    /**
	 * let the current member, from which the request came listen to the a
	 * channel
	 * 
	 * @param name
	 * @return
	 */
    public Channel listenToChannel(String name) {
        Channel cn = channelhandler.getChannel(name);
        cn.addMember(this.member);
        return cn;
    }

    /**
	 * adds a channel to the queue
	 * 
	 * @param name
	 * @return
	 */
    public static Channel addChannel(String name) {
        return channelhandler.getChannel(name);
    }

    /**
	 * add a listener to a specific channel
	 * 
	 * @param channel
	 * @param cl
	 */
    public static void addChannelListener(String channel, ChannelListener cl) {
        channelhandler.getChannel(channel).addListener(cl);
    }

    /**
	 * add a listener to a specific channel
	 * 
	 * @param channel
	 * @param cl
	 */
    public static void addChannelListener(int id, ChannelListener cl) {
        channelhandler.getChannel(id).addListener(cl);
    }

    /**
	 * removes a channellistener from a channel
	 * 
	 * @param id
	 */
    public static void removeChannelListener(int id) {
        channelhandler.getChannel(id).addListener(null);
    }

    /**
	 * removes a channellistener from a channel
	 * 
	 * @param id
	 */
    public static void removeChannelListener(String name) {
        channelhandler.getChannel(name).addListener(null);
    }

    /**
	 * removes a channel from the queue
	 * 
	 * @param id
	 */
    public static void removeChannel(int id) {
        channelhandler.removeChannel(channelhandler.getChannel(id));
    }

    /**
	 * removes a channel from the queue
	 * 
	 * @param id
	 */
    public static void removeChannel(String name) {
        channelhandler.removeChannel(channelhandler.getChannel(name));
    }

    /**
	 * lets a specific member listen to a channel
	 * 
	 * @param channel
	 * @param member
	 * @return
	 */
    public static Channel listentoChannel(String channel, Member member) {
        Channel cn = channelhandler.getChannel(channel);
        cn.addMember(member);
        return cn;
    }

    /**
	 * lets a specific member listen to a channel
	 * 
	 * @param channel
	 * @param member
	 * @return
	 */
    public static Channel listentoChannel(int id, Member member) {
        Channel cn = channelhandler.getChannel(id);
        cn.addMember(member);
        return cn;
    }

    /**
	 * lets a the member, from which the request came listen to a channel
	 * 
	 * @param channel
	 * @param member
	 * @return
	 */
    public Channel listenToChannel(int id) {
        Channel cn = channelhandler.getChannel(id);
        cn.addMember(this.member);
        return cn;
    }

    /**
	 * stops the current req -> res processing until a message will send to the
	 * member or the function stopAsync will called
	 * 
	 * 
	 * 
	 * @return
	 */
    public Event startAsync() {
        Event event = in.getInstance(Event.class);
        event.init(this.req, this.resp);
        this.member.setEvent(event);
        Member anMember = memberhandler.getMember(this.req.getSession().getId());
        anMember.setEvent(event);
        return event;
    }

    /**
	 * stops the current req -> res processing until a message will send to the
	 * member or the function stopAsync will called
	 * 
	 * if this function will called it will give the BackyardEvent implmentation
	 * the given obj.
	 * 
	 * 
	 * 
	 * 
	 * @return
	 */
    public Event startAsync(Object obj) {
        Event event = in.getInstance(Event.class);
        event.setEvent(obj);
        event.init(this.req, this.resp);
        this.member.setEvent(event);
        Member anMember = memberhandler.getMember(this.req.getSession().getId());
        anMember.setEvent(event);
        return event;
    }

    /**
	 * stops the async prossesing
	 */
    public void stopAsync() {
        stopAsync("na");
    }

    /**
	 * sops the asyn prossesing and will send the message to the member
	 * 
	 * @param message
	 */
    public void stopAsync(String message) {
        this.member.sendMessage(new Message(message));
    }

    /***
	 * returns a channel. if the channel not exists it will creat it
	 * 
	 * @param name
	 * @return
	 */
    public Channel getChannel(String name) {
        return channelhandler.getChannel(name);
    }

    /**
	 * 
	 * @return true if the member has a comet event.
	 */
    public boolean hasEvent() {
        return getMetaChannel().hasEvent(this.member);
    }

    /**
	 * 
	 * @param member
	 * @return return true if the given member has an event
	 */
    public boolean hasEvent(Member member) {
        return getMetaChannel().isMember(member.getMemberlId());
    }
}
