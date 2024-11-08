package uk.co.weft.dealersys2;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.ServletException;
import uk.co.weft.dbutil.ConnectionPool;
import uk.co.weft.dbutil.Context;
import uk.co.weft.dbutil.Contexts;
import uk.co.weft.dbutil.DataFormatException;
import uk.co.weft.dbutil.DataStoreException;
import uk.co.weft.dbutil.RSContexts;
import uk.co.weft.htform.ActionWidget;
import uk.co.weft.htform.AuthenticatedForm;
import uk.co.weft.htform.CurrencyWidget;
import uk.co.weft.htform.DataMenuWidget;
import uk.co.weft.htform.DateWidget;
import uk.co.weft.htform.InitialisationException;
import uk.co.weft.htform.MenuOption;
import uk.co.weft.htform.MenuWidget;
import uk.co.weft.htform.SimpleDataMenuWidget;
import uk.co.weft.htform.Widget;

public class Checkout extends AuthenticatedForm {

    /**
	 * whether prices on this system are VAT exclusive (and we show VAT) or
	 * VAT inclusive (and we don't)
	 */
    boolean showVAT = true;

    /** rate of VAT to be computed and added on to the total */
    float vat = (float) 0.175;

    /**
	 * set up widgets to edit each of my fields
	 */
    public void init(Context config) throws InitialisationException {
        Widget w = new DataMenuWidget("Customer", "Customer", "You. If this is wrong, please log out", "select Customer, Salutation, forenames, surname " + "from customer order by surname");
        w.setImmutable(true);
        w.setMandatory(true);
        addWidget(w);
        w = new CurrencyWidget("Total", "Total", "The total amount we will charge to your card");
        w.setImmutable(true);
        addWidget(w);
        w = new SimpleDataMenuWidget("CreditType", "Card Type", "The type of credit card you are using", "CREDITTYPE", "CreditType", "CreditType");
        w.setMandatory(true);
        addWidget(w);
        w = new Widget("CreditCN", "Card Number", "The number on your card");
        w.setMandatory(true);
        w.setSize(20);
        addWidget(w);
        w = new Widget("NameOnCard", "Name on Card", "Your name, exactly as it appears on the credit card");
        w.setSize(40);
        w.setMandatory(true);
        addWidget(w);
        DateWidget dw = new DateWidget("Expires", "Expires end", "The date when your credit card expires");
        dw.showDayWidget = false;
        dw.setMandatory(true);
        addWidget(dw);
        MenuWidget mw = new DataMenuWidget("Address", "Card address", "The address to which your credit card is " + "registered", "select address, line1 from address", false, false);
        mw.addOption(new MenuOption("Use the address below", mw));
        mw.setMandatory(true);
        addWidget(mw);
        w = new Widget("Line1", "Address", "The address. Complete this only is you weren't able " + "to find the address you want in the menus above");
        w.setSize(64);
        addWidget(w);
        w = new Widget("Line2", "(continued)", "");
        w.setSize(64);
        addWidget(w);
        w = new Widget("Line3", "(continued)", "");
        w.setSize(64);
        addWidget(w);
        w = new Widget("PostCode", "Post Code", "The postal code of this address");
        w.setSize(12);
        addWidget(w);
        mw = new SimpleDataMenuWidget("Country", "Country", "The country where this address is", "COUNTRY", "Country", "Name");
        mw.setDefault("UK");
        mw.setSize(5);
        addWidget(mw);
        addWidget(new BuyWidget());
        super.init(config);
    }

    /**
	 * compute the total for open bids for fixed price items and accepted
	 * quotations on this account on this account
	 */
    protected int computeCheckoutTotal(Context context) throws DataStoreException {
        Connection c = null;
        Statement s = null;
        int result = -1;
        try {
            c = context.getConnection();
            s = c.createStatement();
            result = computeCheckoutTotal(context, s);
        } catch (DataStoreException dse) {
            throw dse;
        } catch (Exception any) {
            throw new DataStoreException(any.getMessage());
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
                if (c != null) {
                    context.releaseConnection(c);
                }
            } catch (SQLException sex) {
            } catch (DataStoreException dse) {
            }
        }
        return result;
    }

    /**
	 * compute the total for open bids for fixed price items and accepted
	 * quotations on this account. Use this JDBC statement, but not
	 * responsible for the statement or its connection.
	 */
    protected int computeCheckoutTotal(Context context, Statement s) throws DataStoreException, SQLException {
        Object customer = CustomerAuthenticator.getCustomer(context);
        int result = -1;
        StringBuffer q = new StringBuffer("select ( sum( BIDSTATE.Amount) + ");
        q.append(" sum ( BIDSTATE.QShipping) + sum( BIDSTATE.QInsure) + ");
        q.append(" sum( BIDSTATE.VAT)) as Total from BID, BIDSTATE ");
        q.append(" where  BIDSTATE.Bid = BID.Bid ");
        q.append(" and ( BIDSTATE.BidStatus = ").append(BidStatus.ENQUIRY);
        q.append(" or BIDSTATE.BidStatus = ").append(BidStatus.QUOTATION).append(") ");
        q.append(" and BID.Customer = ").append(customer);
        q.append(" and BIDSTATE.BidState =  ");
        q.append(" ( select max( BIDSTATE.BidState) ").append("from BIDSTATE ");
        q.append(" where Bid = BID.Bid) ");
        System.err.println("==> Computing checksum: " + q.toString());
        ResultSet r = s.executeQuery(q.toString());
        if (r.next()) {
            result = r.getInt("Total");
        } else {
            throw new DataFormatException("Found nothing in your basket");
        }
        return result;
    }

    /**
	 * Specialisation: make the customer the authenticated customer, not some
	 * interloper.
	 */
    protected void handleAction(Context context) throws Exception {
        context.put(Customer.KEYFN, CustomerAuthenticator.getCustomer(context));
        super.handleAction(context);
    }

    /**
	 * calculate the total; set up the address menu
	 */
    protected void postProcess(Context context) throws DataStoreException, ServletException {
        Context menus = (Context) context.get(CONTEXTMENUMAGICTOKEN);
        Object customer = context.get("Customer");
        if (customer == null) {
            throw new DataStoreException("You have not identified yourself");
        }
        StringBuffer q = new StringBuffer("select address, line1, postcode, country " + "from address ");
        q.append("where customer = ").append(customer);
        q.append(" order by line1");
        menus.put("address", q.toString());
        context.put("Total", computeCheckoutTotal(context));
    }

    /**
	 * there's a lot of 'magic' values in here - change only with extreme
	 * care. This widget handles the acutal process of committing to buy the
	 * contents of the basket and consequently it's action is critically
	 * important.
	 */
    class BuyWidget extends ActionWidget {

        /**
		 * the fields in bidState into which I want to insert. Carefully
		 * coordinate any changes to the BIDSTATE table (which is our core
		 * audit trail) with this. Note that we don't try to insert the
		 * changed field because (a) that will get defaulted by the database
		 * and (b) the querywriter I'm using here (which doesn't have access
		 * to metadata) can't tell what types the fields it's writing are and
		 * consequently is only safe for integer fields
		 */
        protected String[] bidStateFields = { "Bid", "Bidstatus", "Amount", "QShipping", "QInsure", "VAT", "Username", "Invoice" };

        BuyWidget() {
            super("Buy!", "To buy the items in your basket");
        }

        /**
		 * buy the current basket contents. This is done using a lot of JDBC
		 * directly rather than using dbutil, because we can't reliably
		 * extract metadata for the bidprivate table
		 */
        protected void execute(Context context) throws java.lang.Exception {
            Connection c = null;
            Statement s = null;
            Integer check = context.getValueAsInteger("Total");
            System.err.println("In BuyWidget.execute()");
            try {
                c = context.getConnection();
                c.setAutoCommit(false);
                s = c.createStatement();
                int total = computeCheckoutTotal(context, s);
                if (check == null) {
                    throw new Exception("Shouldn't: No total?");
                }
                if (check.intValue() != total) {
                    throw new Exception("Shouldn't: Basket changed? " + "total was " + total + "; checksum was " + check);
                }
                StringBuffer q = new StringBuffer("select BIDSTATE.Bid, BIDSTATE.Amount, " + "BIDSTATE.QShipping, BIDSTATE.QInsure " + "from BID, BIDSTATE " + "where  BIDSTATE.Bid = BID.Bid " + "and ( BIDSTATE.BidStatus = 0 " + "or BIDSTATE.BidStatus = 15) " + "and BID.Customer = ");
                q.append(context.get("customer"));
                q.append(" and bidstate.bidstate =  " + "( select max( bidstate.bidstate) " + "from bidstate " + "where bid = bid.bid) ");
                System.err.println(q.toString());
                Contexts rows = new RSContexts(s.executeQuery(q.toString()));
                Enumeration e = rows.elements();
                while (e.hasMoreElements()) {
                    Context row = (Context) e.nextElement();
                    row.merge((Map) context);
                    row.put("Username", context.get(ConnectionPool.DBUSERMAGICTOKEN));
                    row.put("BidStatus", BidStatus.OFFER);
                    s.executeUpdate(bidStateInsert(row));
                    s.execute(bidPrivateInsert(context, row));
                }
                c.commit();
            } catch (Exception any) {
                c.rollback();
                throw new DataStoreException("Your card will not be debited: " + any.getMessage());
            } finally {
                try {
                    if (s != null) {
                        s.close();
                    }
                    if (c != null) {
                        context.releaseConnection(c);
                    }
                } catch (SQLException sex) {
                } catch (DataStoreException dse) {
                }
            }
            context.put(REDIRECTMAGICTOKEN, "account");
        }

        /**
		 * construct the necessary SQL string to insert this row into the
		 * BIDPRIVATE table, which holds the credit card information
		 */
        private String bidPrivateInsert(Context context, Context row) {
            StringBuffer q = new StringBuffer("insert into BIDPRIVATE ");
            q.append("(Bid, CreditCN, CreditType, NameOnCard, ");
            q.append("Expires, Address) values (");
            q.append(row.get("Bid")).append(", '");
            q.append(context.get("CreditCN")).append("', '");
            q.append(context.get("CreditType")).append("', '");
            q.append(context.get("NameOnCard")).append("', '");
            q.append(context.get("Expires")).append("', ");
            q.append(context.get("Address")).append(")");
            System.err.println(q.toString());
            return q.toString();
        }

        /**
		 * construct the necessary SQL string to insert this row into the
		 * BIDSTATE table, the main audit trail
		 */
        private String bidStateInsert(Context row) {
            StringBuffer q = new StringBuffer("insert into BIDSTATE (");
            boolean coma = false;
            for (int field = 0; field < bidStateFields.length; field++) {
                String n = bidStateFields[field];
                if (row.get(n) != null) {
                    if (coma) {
                        q.append(", ");
                    }
                    q.append(n);
                    coma = true;
                }
            }
            q.append(") values (");
            coma = false;
            for (int field = 0; field < bidStateFields.length; field++) {
                String n = bidStateFields[field];
                Integer i = row.getValueAsInteger(n);
                String s = row.getValueAsString(n);
                if (s != null) {
                    if (coma) {
                        q.append(", ");
                    }
                    if (i != null) {
                        q.append(i);
                    } else {
                        q.append("'").append(s).append("'");
                    }
                    coma = true;
                }
            }
            q.append(")");
            System.err.println(q.toString());
            return q.toString();
        }
    }
}
