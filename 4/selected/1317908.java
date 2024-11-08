package org.javadelic.burrow.query;

import java.util.Vector;
import org.xml.sax.Attributes;
import org.javadelic.burrow.JabFavorite;

/**
 * The class Private is used to store and retreive private data on the Jabber
 * server.  Any valid XML string may be stored which leaves the implementation
 * of this class wide open.  This presents extreme problems for our refective
 * parser.
 * <P>
 * Rather than allow for completely arbitrary data, we will be using the 
 * <code>jabber:iq:private</code> namespace to store client preferences.  
 * <P>
 * Currently supported formats are:
 * <pre> 
 * &lt;!ELEMENT preferences (favorite)+&gt;
 * 
 *   &lt;!ELEMENT favorite (
 *         channel |
 *         server |
 *         nickname )?&gt;
 * 
 *   &lt;!ATTLIST favorite 
 *     name CDATA #IMPLIED&gt;
 *
 *   &lt;!ELEMENT channel (#PCDATA)&gt;
 *   &lt;!ELEMENT server (#PCDATA)&gt;
 *   &lt;!ELEMENT nickname (#PCDATA)&gt;
 *
 *</pre>
 */
public class Private extends Object implements JabQuery {

    public static boolean bDebug = false;

    private Vector favorites;

    private JabFavorite favorite;

    public Private() {
        debug("\n----------\nCreating new Private object");
        favorites = new Vector();
    }

    public void setAttributes(Attributes atts) {
        debug("creating new JabFavorite, name = " + atts.getValue("name"));
        favorite = new JabFavorite(atts.getValue("name"));
    }

    public void setChannel(String channel) {
        debug("setChannel = " + channel);
        favorite.setChannel(channel);
    }

    public void setServer(String server) {
        debug("setServer = " + server);
        favorite.setServer(server);
    }

    public void setNickname(String nickname) {
        debug("setNickname = " + nickname);
        favorite.setNickname(nickname);
    }

    /** 
	  * This method will be called at the completion of each &lt;favorite&gt;
	  * tag.  It will add the current JabFavorite to a Vector.  
	  */
    public void setFavorite() {
        debug("setFavorite");
        favorites.add(favorite);
    }

    public void setFavorites(Vector favorites) {
        debug("setFavorites");
        this.favorites = favorites;
    }

    public void setPreferences() {
    }

    public Vector getFavorites() {
        return favorites;
    }

    public String print() {
        StringBuffer sb = new StringBuffer();
        sb.append("<bajjer xmlns='bajjer:prefs'>");
        for (int i = 0; i < favorites.size(); i++) {
            favorite = (JabFavorite) favorites.get(i);
            sb.append("<favorite name='" + favorite.getName() + "'>");
            if (favorite.getChannel() != null) {
                sb.append("<channel>" + favorite.getChannel() + "</channel>");
            }
            if (favorite.getServer() != null) {
                sb.append("<server>" + favorite.getServer() + "</server>");
            }
            if (favorite.getNickname() != null) {
                sb.append("<nickname>" + favorite.getNickname() + "</nickname>");
            }
            sb.append("</favorite>");
        }
        sb.append("</bajjer>");
        return sb.toString();
    }

    private void debug(String msg) {
        if (bDebug) {
            System.out.println("Private: " + msg);
        }
    }
}
