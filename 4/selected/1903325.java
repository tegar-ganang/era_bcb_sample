package org.wakhok.space;

import javax.swing.JOptionPane;
import net.jini.core.lease.Lease;
import net.jini.space.JavaSpace;

/**
 *
 * @author bishal acharya : bishalacharya@gmail.com
 * while refactoring make this class static so that we only 
 * make one instance of it
 */
public class SpaceSearch {

    private String channelName, email, password;

    static JavaSpace space;

    Integer id;

    public SpaceSearch() {
        try {
            space = SpaceAccessor.getSpace();
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public Request Search(String channelName) {
        this.channelName = channelName;
        Request template1 = new Request(channelName);
        Request result2 = null;
        try {
            result2 = (Request) space.read(template1, null, Long.MAX_VALUE);
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return result2;
    }

    public Request Search(String channelName, String email) {
        this.channelName = channelName;
        this.email = email;
        Request template1 = new Request(channelName, email);
        Request result2 = null;
        try {
            result2 = (Request) space.read(template1, null, Long.MAX_VALUE);
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return result2;
    }

    public PrivateRequest privateSearch(String channelName, String password) {
        this.channelName = channelName;
        this.password = password;
        PrivateRequest template1 = new PrivateRequest(channelName, email);
        PrivateRequest result2 = null;
        try {
            result2 = (PrivateRequest) space.read(template1, null, Long.MAX_VALUE);
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return result2;
    }

    public PrivateRequest privateSearch(String channelName, Integer id) {
        this.channelName = channelName;
        this.id = id;
        PrivateRequest template1 = new PrivateRequest(channelName, id);
        PrivateRequest result2 = null;
        try {
            result2 = (PrivateRequest) space.read(template1, null, Long.MAX_VALUE);
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return result2;
    }

    public Request Search(String channelName, Integer id) {
        this.channelName = channelName;
        this.id = id;
        Request template1 = new Request(channelName, id);
        Request result2 = null;
        try {
            result2 = (Request) space.read(template1, null, Long.MAX_VALUE);
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return result2;
    }

    public Request Search(String channelName, Integer id, String email) {
        this.channelName = channelName;
        Request template = new Request(channelName, id, email);
        Request result = null;
        try {
            result = (Request) space.read(template, null, Long.MAX_VALUE);
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return result;
    }

    public void takeItem(String channelName, Integer id) {
        this.channelName = channelName;
        this.id = id;
        Request template1 = new Request(channelName, id);
        Request result2 = null;
        try {
            result2 = (Request) space.take(template1, null, Long.MAX_VALUE);
            result2 = null;
            JOptionPane.showMessageDialog(null, "Successfully Deleted item with id =" + id);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "cannot remove and item from the space" + ex);
        }
    }

    public void takePrivateItem(String channelName, Integer id) {
        this.channelName = channelName;
        this.id = id;
        PrivateRequest template1 = new PrivateRequest(channelName, id);
        PrivateRequest result2 = null;
        try {
            result2 = (PrivateRequest) space.take(template1, null, Long.MAX_VALUE);
            result2 = null;
            JOptionPane.showMessageDialog(null, "Successfully Deleted item with id =" + id);
        } catch (Exception ex) {
            System.out.println(ex);
            JOptionPane.showMessageDialog(null, "Could not delete from the space item with id=" + id);
        }
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    private Integer getRequestNumber(String channel) {
        try {
            Index template = new Index("tail", channel);
            Index head = (Index) space.take(template, null, Long.MAX_VALUE);
            head.increment();
            space.write(head, null, Lease.FOREVER);
            return head.getId();
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }
}
