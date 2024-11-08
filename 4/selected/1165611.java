package org.timothyb89.jtelirc.plugins.bag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.timothyb89.jtelirc.filter.FilterSet;
import org.timothyb89.jtelirc.filter.base.DefaultFilter;
import org.timothyb89.jtelirc.listener.MessageListener;
import org.timothyb89.jtelirc.message.Message;
import org.timothyb89.jtelirc.message.UserMessage;
import org.timothyb89.jtelirc.server.Server;
import org.timothyb89.jtelirc.user.User;
import org.timothyb89.jtelirc.util.ListUtil;

/**
 *
 * @author tim
 */
public class BagListener implements MessageListener {

    private Server server;

    private BagManager manager;

    private Bag bag;

    private Templates templates;

    public BagListener(Server server, BagManager manager) {
        this.server = server;
        this.manager = manager;
        bag = manager.getBag();
        templates = new Templates(bag);
    }

    public void onMessageReceived(Message message) {
        if (!(message instanceof UserMessage)) {
            return;
        }
        UserMessage m = (UserMessage) message;
        if (!m.getContext().isPublic()) {
            return;
        }
        String text = m.getText();
        String nick = server.getNick();
        String addr = server.getNick() + "[,\\:\\- ] ";
        List<String> l = regex(text, action("gives " + nick + " (\\S+ )?(.+)"));
        if (l != null) {
            String spec = l.get(1);
            if (spec == null) {
                spec = "";
            } else {
                spec = spec.trim();
            }
            String name = l.get(2).trim();
            BagItem bitem = new BagItem(name, spec);
            take(bitem, m);
            return;
        }
        l = regex(text, action("throws (\\S+ )?(.+) at " + nick));
        if (l != null) {
            String spec = l.get(1);
            if (spec == null) {
                spec = "";
            } else {
                spec = spec.trim();
            }
            String name = l.get(2).trim();
            BagItem bitem = new BagItem(name, spec);
            take(bitem, m);
            return;
        }
        l = regex(text, addr + "have (\\S+ )?(.+)");
        if (l != null) {
            String spec = l.get(1);
            if (spec == null) {
                spec = "";
            } else {
                spec = spec.trim();
            }
            String name = l.get(2).trim();
            BagItem bitem = new BagItem(name, spec);
            take(bitem, m);
            return;
        }
        l = regex(text, addr + "throw something");
        if (l != null) {
            if (bag.isEmpty()) {
                m.reply("Sorry " + m.getUser().getNick() + ", my bag is empty", false);
                return;
            }
            BagItem bitem = bag.getRandomItem();
            User target = m.getChannel().getRandomUser();
            m.reply(templates.applyThrow(bitem, target.getNick()), false);
            bag.removeItem(bitem);
            manager.saveBag();
            return;
        }
        l = regex(text, addr + "regift something");
        if (l != null) {
            if (bag.isEmpty()) {
                m.reply("Sorry " + m.getUser().getNick() + ", my bag is empty", false);
                return;
            }
            BagItem bitem = bag.getRandomItem();
            User target = m.getChannel().getRandomUser();
            m.reply(action("gives " + target.getNick() + " " + bitem.getStatement()), false);
            bag.removeItem(bitem);
            manager.saveBag();
            return;
        }
    }

    public void take(BagItem bitem, UserMessage m) {
        if (bag.isFull()) {
            BagItem drop = bag.getRandomItem();
            bag.removeItem(drop);
            String person = m.getChannel().getRandomUser().getNick();
            m.reply(templates.applyExchange(drop, bitem, person), false);
        } else {
            m.reply(templates.applyPut(bitem), false);
        }
        bag.addItem(bitem);
        manager.saveBag();
    }

    private List<String> regex(String input, String regex) {
        Pattern pat = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher mat = pat.matcher(input);
        if (mat.matches()) {
            List<String> ret = new ArrayList<String>();
            for (int i = 0; i <= mat.groupCount(); i++) {
                ret.add(mat.group(i));
            }
            return ret;
        } else {
            return null;
        }
    }

    /**
	 * Returns the appropriate 'a' or 'an' for the given word.
	 * Doesn't work so well with the acronym conventions, but hey.
	 * @param text The word (or phrase) to use
	 * @return an appropriate a or an prepended to the text
	 */
    private String a(String text) {
        if (text.length() == 0) {
            return "a";
        }
        char c = text.toLowerCase().charAt(0);
        switch(c) {
            case 'a':
            case 'e':
            case 'i':
            case 'o':
            case 'u':
                return "an " + text;
            default:
                return "a " + text;
        }
    }

    private String a(BagItem item) {
        return a(item.getName());
    }

    /**
	 * Makes the given text a CTCP action
	 * @param text The text to use as the action
	 * @return The text as a CTCP ACTION.
	 */
    private String action(String text) {
        return "\001ACTION " + text + "\001";
    }

    public List<FilterSet> getFilters() {
        return ListUtil.singleFilterSet(new DefaultFilter());
    }
}
