package net.sf.jerkbot.util;

import jerklib.Session;
import net.sf.jerkbot.commands.MessageContext;

/**
 * @author Yves Zoundi <yveszoundi at users dot sf dot net>
 *         IRC message sending utility class
 * @version 0.0.1
 */
public class MessageUtil {

    public static void say(MessageContext context, String message) {
        if (context.isPrivateMessage()) {
            context.getSession().sayPrivate(context.getSender(), message);
        } else {
            context.getSession().getChannel(context.getChannel()).say(context.getUserTarget() + ": " + message);
        }
    }

    public static void sayPrivate(MessageContext context, String message) {
        Session session = context.getSession();
        session.sayPrivate(context.getSender(), message);
    }

    public static void sayPrivateFormatted(MessageContext context, String message, Object... args) {
        sayPrivate(context, String.format(message, args));
    }

    public static void sayFormatted(MessageContext context, String message, Object... args) {
        say(context, String.format(message, args));
    }

    public static void action(MessageContext context, String message) {
        Session session = context.getSession();
        if (context.isPrivateMessage()) {
            session.sayPrivate(context.getSender(), " " + message);
        } else {
            session.getChannel(context.getChannel()).action(message);
        }
    }

    public static void actionFormatted(MessageContext context, String message, Object... args) {
        action(context, String.format(message, args));
    }

    public static void talk(MessageContext context, String message) {
        Session session = context.getSession();
        if (context.isPrivateMessage()) {
            session.sayPrivate(context.getSender(), message);
        } else {
            session.getChannel(context.getChannel()).say(message);
        }
    }

    public static void talkFormatted(MessageContext context, String message, Object... args) {
        talk(context, String.format(message, args));
    }
}
