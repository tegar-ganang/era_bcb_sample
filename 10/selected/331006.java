package uk.co.weft.pres.server;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import uk.co.weft.dbutil.Context;
import uk.co.weft.dbutil.DataAuthException;
import uk.co.weft.dbutil.DataFormatException;
import uk.co.weft.dbutil.DataStoreException;
import uk.co.weft.dbutil.TableDescriptor;
import uk.co.weft.htform.HiddenWidget;
import uk.co.weft.htform.InitialisationException;
import uk.co.weft.htform.SimpleDataMenuWidget;
import uk.co.weft.htform.Widget;

/**
 * Add or edit a moderation in the database; if approved, mail details to
 * parent of this moderation?.
 *
 * @author Simon Brooke
 * @version $Revision: 1.6 $ This revision: $Author: simon_brooke $
 */
public class Moderation extends WithSubscriberForm {

    /**
	 * set up widgets to edit each of my fields
	 */
    public void init(Context config) throws InitialisationException {
        table = "MODERATION";
        keyField = "Moderation";
        Widget w = addWidget(new HiddenWidget("Response"));
        authentificationWidgets.addWidget(w);
        w = new SimpleDataMenuWidget("Subscriber", "You are", "The person moderating this response: you " + "(if we don't know you, " + "you must subscribe)", "SUBSCRIBER", "Subscriber", "Name", false, 8);
        w.setImmutable(true);
        addWidget(w);
        w = addWidget(new SimpleDataMenuWidget("Reason", "Reason", "What you thought of this comment", "MODREASON", "ModReason", "Reason", true, 8));
        super.init(config);
    }

    /**
	 * Show the user how many moderation points are left
	 */
    protected void preForm(Context context) throws Exception {
        super.preForm(context);
        Object subscriber = SubscriberAuthenticator.getSubscriber(context);
        Object response = context.get("response");
        ServletOutputStream out = (ServletOutputStream) context.get(OUTPUTSTREAMMAGICTOKEN);
        if (response != null) {
            TableDescriptor.getDescriptor("response", "response", context).fetch(context);
            out.println("<div class=\"threaded-discussion\">");
            out.print("<h3>");
            out.print(context.getValueAsString("title"));
            out.println("</h3>");
            out.println(context.getValueAsString("comment"));
            out.println("</div>");
        }
        if (subscriber != null) {
            TableDescriptor.getDescriptor("subscriber", "subscriber", context).fetch(context);
            Integer mods = context.getValueAsInteger("mods");
            out.print("<p>");
            if (mods != null) {
                out.print("You have " + mods + " moderation points left");
            } else {
                out.print(grs("You have no moderation points left", context));
            }
            out.println("</p>");
        }
    }

    /**
	 * Specialisation: after storing cache sum of moderation for response on
	 * response; decrement subscribers' mod points.
	 */
    protected boolean store(Context context) throws DataStoreException, ServletException {
        Connection db = context.getConnection();
        Statement st = null;
        String q = null;
        Integer subscriber = context.getValueAsInteger("subscriber");
        int amount = 0;
        if (subscriber == null) {
            throw new DataAuthException("Don't know who moderator is");
        }
        Object response = context.get("Response");
        if (response == null) {
            throw new DataStoreException("Don't know what to moderate");
        } else {
            Context scratch = (Context) context.clone();
            TableDescriptor.getDescriptor("response", "response", scratch).fetch(scratch);
            Integer author = scratch.getValueAsInteger("author");
            if (subscriber.equals(author)) {
                throw new SelfModerationException("You may not moderate your own responses");
            }
        }
        context.put("moderator", subscriber);
        context.put("moderated", response);
        if (db != null) {
            try {
                st = db.createStatement();
                q = "select mods from subscriber where subscriber = " + subscriber.toString();
                ResultSet r = st.executeQuery(q);
                if (r.next()) {
                    if (r.getInt("mods") < 1) {
                        throw new DataAuthException("You have no moderation points left");
                    }
                } else {
                    throw new DataAuthException("Don't know who moderator is");
                }
                Object reason = context.get("reason");
                q = "select score from modreason where modreason = " + reason;
                r = st.executeQuery(q);
                if (r.next()) {
                    amount = r.getInt("score");
                    context.put("amount", new Integer(amount));
                } else {
                    throw new DataStoreException("Don't recognise reason (" + reason + ") to moderate");
                }
                context.put(keyField, null);
                if (super.store(context, db)) {
                    db.setAutoCommit(false);
                    q = "update RESPONSE set Moderation = " + "( select sum( Amount) from MODERATION " + "where Moderated = " + response + ") " + "where Response = " + response;
                    st.executeUpdate(q);
                    q = "update subscriber set mods = mods - 1 " + "where subscriber = " + subscriber;
                    st.executeUpdate(q);
                    q = "select author from response " + "where response = " + response;
                    r = st.executeQuery(q);
                    if (r.next()) {
                        int author = r.getInt("author");
                        if (author != 0) {
                            int points = -1;
                            if (amount > 0) {
                                points = 1;
                            }
                            StringBuffer qb = new StringBuffer("update subscriber ");
                            qb.append("set score = score + ").append(amount);
                            qb.append(", mods = mods + ").append(points);
                            qb.append(" where subscriber = ").append(author);
                            st.executeUpdate(qb.toString());
                        }
                    }
                    db.commit();
                }
            } catch (Exception e) {
                try {
                    db.rollback();
                } catch (Exception whoops) {
                    throw new DataStoreException("Shouldn't happen: " + "failed to back out " + "failed insert: " + whoops.getMessage());
                }
                throw new DataStoreException("Failed to store moderation: " + e.getMessage());
            } finally {
                if (st != null) {
                    try {
                        st.close();
                    } catch (Exception noclose) {
                    }
                    context.releaseConnection(db);
                }
            }
        }
        return true;
    }

    /**
	 * an exception to throw when people try to moderate their own responses.
	 * Extends DataFormatException, since this can be  (and is) trapped
	 * gracefully.
	 */
    class SelfModerationException extends DataFormatException {

        /**
		 * @param message
		 * @param cause
		 */
        public SelfModerationException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
		 * @param message
		 */
        public SelfModerationException(String message) {
            super(message);
        }
    }
}
