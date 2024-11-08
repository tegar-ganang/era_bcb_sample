package jimm.twice.subscriber;

import org.icestandard.ICE.V20.syndicator.full.*;
import org.icestandard.ICE.V20.delivery.*;
import org.icestandard.ICE.V20.subscribe.*;
import org.icestandard.ICE.V20.message.*;
import org.icestandard.ICE.V20.simpledatatypes.*;
import org.apache.axis.utils.*;
import org.apache.axis.*;
import org.apache.axis.types.*;
import org.apache.axis.client.*;
import org.apache.axis.message.*;
import jimm.twice.ice.*;
import jimm.twice.ice.xml.Namespaces;
import jimm.twice.xml.dom.*;
import jimm.twice.util.*;
import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;
import javax.xml.rpc.ServiceException;
import javax.xml.namespace.QName;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*;
import java.text.SimpleDateFormat;
import org.icestandard.ICE.V20.syndicator.full.*;

/**
 * A subscriber that can deal with subscriptions, persistence, and HTTP.
 *
 * @author Jim Menard, <a href="mailto:jimm@io.com">jimm@io.com</a>
 */
public class TwICESubscriber extends Subscriber {

    protected static final int BUFSIZ = 4096;

    protected static final String SAVED_FILE_DATE_FORMAT = "yyyyMMdd-HHmmss-";

    protected String pstoreDir;

    protected String cstoreDir;

    protected URL syndicatorURL;

    protected boolean dateTimeStamps;

    protected boolean uniqueFileNames;

    protected SimpleDateFormat dateFormatter;

    protected IceSyndicatorFullPortType syndicator;

    protected MessageContext msgContext;

    protected ArrayList confirmationsList;

    protected ArrayList remainingOffers;

    protected ArrayList itemContents;

    /**
 * Either finds the current subscriber or creates a new one.
 *
 * @param HttpSession the current HTTP session
 * @return a subscriber
 */
    public static TwICESubscriber findOrCreate(HttpSession session) {
        TwICESubscriber subscriber = (TwICESubscriber) session.getAttribute("subscriber");
        if (subscriber == null) {
            subscriber = new TwICESubscriber();
            session.setAttribute("subscriber", subscriber);
        }
        return subscriber;
    }

    /**
 * Constructor. Reads subscriptions from persistent store.
 */
    public TwICESubscriber() {
        super(Props.instance().getProperty("subscriber.domain"), new Party(Props.instance().getProperty("subscriber.uuid"), Props.instance().getProperty("subscriber.name"), Party.ROLE_SUBSCRIBER, Props.instance().getProperty("subscriber.listener_url")));
        confirmationsList = new ArrayList();
        Props p = Props.instance();
        pstoreDir = p.getProperty("subscriber.pstore_dir");
        cstoreDir = p.getProperty("subscriber.subscription_contents_dir");
        String url = null;
        try {
            url = p.getProperty("syndicator.listener_url");
            if (url != null) {
                syndicatorURL = new URL(url);
            }
        } catch (MalformedURLException mue) {
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber (ctor)", mue);
        }
        dateFormatter = new SimpleDateFormat(SAVED_FILE_DATE_FORMAT);
        dateTimeStamps = p.getBooleanProperty("subscriber.file_name_prepend_date_time_stamp");
        uniqueFileNames = p.getBooleanProperty("subscriber.unique_file_names");
        load();
        try {
            syndicator = getIceSyndicator(url);
        } catch (Exception e) {
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber (ctor)", e);
        }
    }

    public void setMessageContext(MessageContext msgContext) {
        this.msgContext = msgContext;
    }

    /**
 * Returns the Axis handle to the syndicator on which
 * operations can be invoked
 *
 * @param listenerURL the listener URL (optional)
 * @return the Axis handle
 *
 */
    private IceSyndicatorFullPortType getIceSyndicator(String listenerURL) throws MalformedURLException, ServiceException {
        IceSyndicatorFullPortType iceSyndicator = null;
        TwiceSyndicatorFull synLocator = new TwiceSyndicatorFullLocator();
        if (listenerURL == null) {
            iceSyndicator = synLocator.getIceSyndicatorFullPortType();
        } else {
            URL serviceURL = new URL(listenerURL);
            iceSyndicator = synLocator.getIceSyndicatorFullPortType(serviceURL);
        }
        return iceSyndicator;
    }

    private IceSyndicatorFullPortType getIceSyndicator() throws MalformedURLException, ServiceException {
        return getIceSyndicator(null);
    }

    /**
 * Returns the pull delivery endpoint for the subscription
 *
 * @param sub the subscription
 * @param protocol the transport protocol desired (usually "soap")
 * @return the delivery endpoint URL
 *
 */
    private String getSubscriptionURL(SubscriptionType sub, String protocol) {
        DeliveryPolicyType dp = sub.getOffer().getDeliveryPolicy();
        for (int i = 0; i < dp.getDeliveryRule().length; i++) {
            DeliveryRuleType rule = dp.getDeliveryRule(i);
            if (rule.getMode().equals(DeliveryRuleTypeMode.pull)) {
                for (int j = 0; j < rule.getTransport().length; j++) {
                    TransportType transport = rule.getTransport(j);
                    if (transport.getProtocol().toString().equals(protocol)) {
                        UrlAccessType ua = transport.getDeliveryEndpoint();
                        if (ua != null) {
                            return (ua.getUrl().toString());
                        }
                    }
                }
            }
        }
        return null;
    }

    public String getListenerURL() {
        return party.getLocationURL();
    }

    /**
 * Returns offers that have not already been subscribed to.
 *
 */
    private void getRemainingOffers() {
        if (offers == null) {
            requestAndParseCatalog();
        }
        if (!hasSubscriptions()) {
            remainingOffers = offers;
        } else {
            remainingOffers = new ArrayList();
            Iterator it = offers.iterator();
            while (it.hasNext()) {
                boolean isRemaining = true;
                OfferType offer = (OfferType) it.next();
                Iterator it2 = subscriptions();
                while (it2.hasNext()) {
                    SubscriptionType sub = (SubscriptionType) it2.next();
                    if (sub.getSubscriptionId().toString().indexOf(offer.getOfferId().toString()) > 0) {
                        isRemaining = false;
                    }
                }
                if (isRemaining) {
                    remainingOffers.add(offer);
                }
            }
        }
    }

    /**
 * Returns an iterator over the offer id strings. Requests a catalog from
 * the syndicator if necessary.
 *
 * @return an iterator over offer id strings
 */
    public Iterator offers() {
        if (offers == null) {
            requestAndParseCatalog();
        }
        getRemainingOffers();
        return remainingOffers.iterator();
    }

    /**
 * Returns <code>true</code> if there are any offers. Requests a catalog from
 * the syndicator if necessary.
 *
 * @return an iterator over offer id strings
 */
    public boolean hasOffers() {
        if (offers == null) {
            requestAndParseCatalog();
        }
        getRemainingOffers();
        return remainingOffers != null && remainingOffers.size() > 0;
    }

    /**
 * Returns the offer with the given id. Request a catalog from the syndicator
 * if necessary.
 *
 * @return an offer or <code>null</code> if not found
 */
    public OfferType findOffer(String id) {
        for (Iterator iter = offers(); iter.hasNext(); ) {
            OfferType o = (OfferType) iter.next();
            if (o.getOfferId().toString().equals(id)) return o;
        }
        return null;
    }

    /** For testing. Does <em>not</em> automatically retrieve offers. */
    public List offerList() {
        return offers;
    }

    /**
 * Requests a catalog and parses the results, saving a list of outstanding
 * offers. The list may be empty, but it will not be <code>null</code>.
 */
    public void requestAndParseCatalog() {
        GetPackageType getPackageReq = new GetPackageType(new Parameters(), new Token("ICE-INITIAL"), new Token("1"));
        try {
            TwICEUtil.setRequestHeader((Stub) syndicator, createIceHeader(null));
            PackageType pt = syndicator.getPackage(getPackageReq);
            OfferType[] o = pt.getCmPackage().getAdd(0).getItem().getOffer();
            offers = new ArrayList(Arrays.asList(o));
            Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber (ctor)", "getPackage success id=" + pt.getSubscriptionId() + " offers: " + o.length);
        } catch (StatusCode sc) {
            IceFault fault = new IceFault(sc.getCode().intValue(), null);
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber requestAndParseCatalog", fault.toString());
        } catch (Exception e) {
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber requestAndParseCatalog", e);
        }
    }

    /**
 * Subscribes to offers selected in HTTP form by sending one subscription
 * request to the syndicator for each offer. Looks for subscription ids in
 * HTTP request form fields that correspond to offer ids.
 *
 * @param request the http servlet request
 */
    public void subscribe(HttpServletRequest request) {
        Collection wantedAsIs = new ArrayList();
        Collection wantedModified = new ArrayList();
        String id;
        for (int i = 0; (id = request.getParameter("offer-" + i + "-id")) != null; ++i) {
            if (request.getParameter("offer-" + i + "-accept") == null) {
                continue;
            }
            OfferType offer = findOffer(id);
            if (offer == null) {
                Logger.instance().log(Logger.WARNING, loggerPrefix, "TwICESubscriber.subscribe(HttpServletRequest)", "No offer with id " + id + " exists.");
                continue;
            }
            Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.subscribe", "subscribing to offer: " + offer.getName());
            OfferType offerModified = null;
            try {
                offerModified = removeUnwantedTransports(offer, i, request);
            } catch (Exception e) {
                Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber subscribe", e);
                continue;
            }
            if (offerModified == null) {
                wantedAsIs.add(offer);
            } else {
                wantedModified.add(offerModified);
            }
        }
        subscribeDirect(wantedAsIs);
        subscribeFull(wantedModified);
    }

    /**
 * Given an offer, remove the transports that are <em>not</em> selected
 * by the user.  Also specifies push delivery endpoint.
 *
 * @param offer an offer
 * @param offerNum offer's number in list of HTTP fields; not same as id
 * @param request the http servlet request
 * @return modified offer if changed, otherwise null
 */
    protected OfferType removeUnwantedTransports(OfferType offer, int offerNum, HttpServletRequest request) throws URI.MalformedURIException {
        OfferType modifiedOffer = null;
        UrlAccessType ua = null;
        ArrayList rules = new ArrayList();
        DeliveryPolicyType dp = offer.getDeliveryPolicy();
        for (int i = 0; i < dp.getDeliveryRule().length; ++i) {
            DeliveryRuleType newRule = null;
            DeliveryRuleType rule = dp.getDeliveryRule(i);
            if (!rule.isRequired() && request.getParameter("offer-" + offerNum + "-" + i + "-rule") == null) {
                Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.removeUnwantedTransports", "offer " + offerNum + " removing rule " + i);
                if (modifiedOffer == null) {
                    modifiedOffer = cloneOffer(offer);
                }
                continue;
            }
            if (rule.getMode().equals(DeliveryRuleTypeMode.push)) {
                Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.removeUnwantedTransports", "offer " + offerNum + " has PUSH rule");
                if (modifiedOffer == null) {
                    modifiedOffer = cloneOffer(offer);
                }
                newRule = cloneDeliveryRule(rule);
                if (ua == null) {
                    String deliveryEndpoint = getListenerURL();
                    ua = new UrlAccessType();
                    ua.setUrl(new URI(deliveryEndpoint));
                }
            }
            ArrayList transports = new ArrayList();
            for (int j = 0; j < rule.getTransport().length; j++) {
                if (request.getParameter("offer-" + offerNum + "-" + i + "-" + j + "-transport") == null) {
                    Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.removeUnwantedTransports", "offer " + offerNum + " rule " + i + " removing transport " + j);
                    if (modifiedOffer == null) {
                        modifiedOffer = cloneOffer(offer);
                    }
                    if (newRule == null) {
                        newRule = cloneDeliveryRule(rule);
                    }
                } else {
                    Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.removeUnwantedTransports", "offer " + offerNum + " rule " + i + " transport " + j + " accepted");
                    TransportType transport = rule.getTransport(j);
                    if (rule.getMode().equals(DeliveryRuleTypeMode.push)) {
                        TransportType newTransport = cloneTransport(transport);
                        newTransport.setDeliveryEndpoint(ua);
                        transports.add(newTransport);
                    } else {
                        transports.add(transport);
                    }
                }
            }
            if (newRule != null) {
                TransportType[] tAll = (TransportType[]) transports.toArray(new TransportType[transports.size()]);
                newRule.setTransport(tAll);
                rules.add(newRule);
            } else {
                rules.add(rule);
            }
        }
        if (modifiedOffer != null) {
            DeliveryRuleType[] dAll = (DeliveryRuleType[]) rules.toArray(new DeliveryRuleType[rules.size()]);
            modifiedOffer.getDeliveryPolicy().setDeliveryRule(dAll);
        }
        return modifiedOffer;
    }

    /**
 * Clones an existing offer without specifying delivery rules or transports
 *
 * @param offer an offer as presented by the syndicator
 * @return the new offer
 */
    private OfferType cloneOffer(OfferType offer) {
        DeliveryPolicyType newDp = new DeliveryPolicyType(null, null, offer.getDeliveryPolicy().getQuantity(), offer.getDeliveryPolicy().getStartdate(), offer.getDeliveryPolicy().getStopdate(), offer.getDeliveryPolicy().getExpirationPriority());
        OfferType newOffer = new OfferType(offer.getContentMetadata(), offer.getOfferMetadata(), offer.getDescription(), newDp, offer.getBusinessTerm(), offer.getRequiredExtension(), offer.getOfferId(), offer.getName(), offer.getValidAfter(), offer.getExpirationDate(), offer.isFullIce());
        return newOffer;
    }

    /**
 * Clones an existing transport without specifying delivery endpoint
 *
 * @param transport a transport as presented by the syndicator
 * @return the new transport
 */
    private TransportType cloneTransport(TransportType transport) {
        TransportType newTransport = new TransportType(null, null, transport.getProtocol(), transport.getPackagingStyle());
        return newTransport;
    }

    /**
 * Clones an existing delivery rule without specifying transports
 *
 * @param deliveryRule a delivery rule as presented by the syndicator
 * @return the new delivery rule
 */
    private DeliveryRuleType cloneDeliveryRule(DeliveryRuleType deliveryRule) {
        DeliveryRuleType newRule = new DeliveryRuleType(null, null, deliveryRule.getMode(), deliveryRule.getMonthday(), deliveryRule.getWeekday(), deliveryRule.getStarttime(), deliveryRule.getDuration(), deliveryRule.getMinNumUpdates(), deliveryRule.getMaxNumUpdates(), deliveryRule.isIncrementalUpdate(), deliveryRule.isConfirmation(), deliveryRule.isRequired());
        return newRule;
    }

    /**
 * Subscribes to offers contained in a list by building and sending a
 * subscription request to the syndicator.  The offer is being accepted
 * as presented, so we use the short cut of the offer-id attribute
 *
 * @param wanted list of subscription offers
 */
    public void subscribeDirect(Collection wanted) {
        if (subscriptions == null) {
            subscriptions = new ArrayList();
        }
        if (wanted.isEmpty()) {
            return;
        }
        Iterator it = wanted.iterator();
        while (it.hasNext()) {
            try {
                Subscribe subscribeReq = new Subscribe();
                OfferType offer = (OfferType) it.next();
                subscribeReq.setOfferId(offer.getOfferId());
                Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.subscribeDirect", "subscribing directly to offer id " + offer.getOfferId());
                TwICEUtil.setRequestHeader((Stub) syndicator, createIceHeader(null));
                SubscriptionType subscription = syndicator.subscribe(subscribeReq);
                Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.subscribeDirect", "subscribed successfully to offer id " + offer.getOfferId());
                subscription.setCurrentState(new Token("ICE-INITIAL"));
                subscriptions.add(subscription);
                store();
            } catch (StatusCode sc) {
                IceFault fault = new IceFault(sc.getCode().intValue(), null);
                Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.subscribeDirect", fault.toString());
            } catch (Exception e) {
                Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.subscribeDirect", e);
            }
        }
        return;
    }

    /**
 * Subscribes to offers contained in a list by building and sending a
 * subscription request to the syndicator.  In this case, the subscriber
 * is specifying delivery style selections or a push endpoint, so we
 * include the full offer within the subscribe message.
 *
 * @param wanted list of subscription offers
 */
    public void subscribeFull(Collection wanted) {
        if (subscriptions == null) {
            subscriptions = new ArrayList();
        }
        if (wanted.isEmpty()) {
            return;
        }
        Iterator it = wanted.iterator();
        while (it.hasNext()) {
            try {
                Subscribe subscribeReq = new Subscribe();
                OfferType offer = (OfferType) it.next();
                subscribeReq.setOffer(offer);
                Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.subscribeFull", "subscribing to offer id " + offer.getOfferId());
                TwICEUtil.setRequestHeader((Stub) syndicator, createIceHeader(null));
                SubscriptionType subscription = syndicator.subscribe(subscribeReq);
                Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.subscribeFull", "subscribed successfully to offer id " + offer.getOfferId());
                subscriptions.add(subscription);
                store();
            } catch (StatusCode sc) {
                IceFault fault = new IceFault(sc.getCode().intValue(), null);
                Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.subscribeFull", fault.toString());
            } catch (Exception e) {
                Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.subscribeFull", e);
            }
        }
        return;
    }

    /**
 * Add a subscription. Only used for unit testing.
 */
    public void addSubscription(Subscription sub) {
        subscriptions.add(sub);
        store();
    }

    /**
 * Removes all subscriptions. Only used for unit testing.
 */
    public void removeAllSubscriptions() {
        subscriptions.clear();
        store();
    }

    /**
 * Cancels selected subscriptions and returns HTML for display.
 *
 * @request the servlet request with parameters
 * @return HTML ready for display
 */
    public String cancelSelectedSubscriptions(HttpServletRequest request) {
        String subId;
        ArrayList toBeCancelled = new ArrayList();
        for (int i = 0; (subId = request.getParameter("sub-" + i + "-id")) != null; ++i) {
            if (request.getParameter("sub-" + i + "-select") != null) {
                toBeCancelled.add(subId);
            }
        }
        if (toBeCancelled.size() > 0) {
            StringBuffer buf = new StringBuffer("<ul>\n");
            for (Iterator iter = toBeCancelled.iterator(); iter.hasNext(); ) {
                subId = (String) iter.next();
                String retval = cancelSubscription(subId);
                buf.append("<li>Subscription ");
                buf.append(subId);
                if ("OK".equals(retval)) buf.append(" cancelled"); else {
                    buf.append(" not cancelled; ");
                    buf.append(retval);
                }
                buf.append("</li>");
            }
            buf.append("</ul>\n");
            return buf.toString();
        } else return "<p>No cancellations requested.</p>";
    }

    /**
 * Requests a subscription cancellation. If successful, removes the
 * subscription from our list. Returns either an error string
 * or the string "OK".
 *
 * @param subId a subscription id
 * @return an error string or the string "OK"
 */
    public String cancelSubscription(String subId) {
        Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.cancelSubscription", "cancelling subscription id " + subId);
        SubscriptionType subscription = findSubscription(subId);
        if (subscription == null) {
            String msg = "Error: unknown subscription " + subId;
            Logger.instance().log(Logger.ERROR, loggerPrefix, msg);
            return msg;
        }
        try {
            Cancel cancelReq = new Cancel();
            cancelReq.setSubscriptionId(new Token(subId));
            TwICEUtil.setRequestHeader((Stub) syndicator, createIceHeader(null));
            Cancellation cancellationResp = syndicator.cancelSubscription(cancelReq);
            clearContents(subId, true);
            subscriptions.remove(subscription);
            store();
            Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.cancelSubscription", "successfully cancelled subscription id " + subId);
            return "OK";
        } catch (StatusCode sc) {
            IceFault fault = new IceFault(sc.getCode().intValue(), null);
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.cancelSubscription", fault.toString());
            return fault.toString();
        } catch (Exception e) {
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.cancelSubscription", e);
            return e.toString();
        }
    }

    /**
 * Formats an item reference using HTML anchor tag
 *
 * @return HTML anchor tag
 */
    private String formatItemRef(AddTypeItemRef itemRef) {
        UrlAccessType ref = itemRef.getReference();
        String uri = ref.getUrl().toString();
        return "<a href='" + uri + "'>" + uri + "</a><br>";
    }

    /**
 * Retrieves and returns the contents of the subscription with
 * the specified subscription id.  This does not send a request
 * to the syndicator, it only retrieves stored contents that
 * have already been pulled or pushed.
 *
 * @param subId a subscription id
 */
    public String retrieveSubscriptionContents(String subId) {
        Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.retrieveSubscriptionContents", "subscriptionId=" + subId);
        StringBuffer sb = new StringBuffer();
        SubscriptionType sub = findSubscription(subId);
        if (sub == null) {
            String msg = "No such subscription " + subId;
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.retrieveSubscriptionContents", msg);
            return msg;
        }
        try {
            ArrayList items = loadSubscriptionContents(subId);
            if (items == null) {
                return "No subscription contents found.";
            }
            int itemCount = 0;
            Iterator it = items.iterator();
            while (it.hasNext()) {
                LocalItem item = (LocalItem) it.next();
                AddType add = item.getAdd();
                AddTypeItemRef itemRef = add.getItemRef();
                if (itemRef != null) {
                    sb.append(formatItemRef(itemRef));
                } else if (add.getItem() != null) {
                    if (add.getMetadata() != null && add.getMetadata().getContentFilename() != null) {
                        String fileContents = getSubscriptionFileContents(subId, add.getMetadata().getContentFilename().toString());
                        sb.append(fileContents + "<br>");
                    } else {
                        sb.append(item.getItemContent() + "<br>");
                    }
                }
            }
        } catch (Exception e) {
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.retrieveSubscriptionContents", e);
            return e.toString();
        }
        return sb.toString();
    }

    protected String getSubscriptionFileContents(String subId, String contentFilename) throws IOException {
        String contentsDir = Props.instance().getProperty("subscriber.subscription_contents_dir");
        String filesDir = contentsDir + File.separator + subId + "-files" + File.separator;
        File contentsFile = new File(filesDir, contentFilename);
        StringWriter out = new StringWriter();
        FileReader in = null;
        try {
            in = new FileReader(contentsFile);
            char[] buf = new char[BUFSIZ];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.getSubscriptionFileContents", e);
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException ioe2) {
            }
        }
        return out.toString();
    }

    /**
 * Loads the subscription contents into memory
 * represented as an ArrayList of LocalItems.
 *
 * @param subId the subscriptionId
 *
 * @return list of LocalItems representing subscription contents
 */
    private ArrayList loadSubscriptionContents(String subId) throws Exception {
        String contentsDir = Props.instance().getProperty("subscriber.subscription_contents_dir");
        String path = contentsDir + File.separator + subId + File.separator;
        File dir = new File(path);
        if (!dir.isDirectory() || dir.listFiles().length == 0) {
            return null;
        }
        ArrayList items = new ArrayList();
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            FileInputStream in = new FileInputStream(files[i]);
            AddParser aparser = new AddParser();
            InputSource src = new InputSource(in);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser parser = factory.newSAXParser();
            XMLReader reader = parser.getXMLReader();
            reader.setContentHandler(aparser);
            reader.parse(src);
            LocalItem item = new LocalItem();
            AddType add = aparser.getAdd();
            item.setAdd(add);
            item.setAddFile(files[i]);
            if (add.getItem() != null && (add.getMetadata() == null || add.getMetadata().getContentFilename() == null)) {
                String iContent = aparser.getItemContentAsString();
                item.setItemContent(iContent);
            }
            items.add(item);
        }
        return items;
    }

    /**
 * Determine whether a subscription has any push delivery rules
 *
 * @return true if this is a push subscription
 */
    private boolean isSubscriptionPush(SubscriptionType sub) {
        OfferType offer = sub.getOffer();
        DeliveryPolicyType dp = offer.getDeliveryPolicy();
        for (int i = 0; i < dp.getDeliveryRule().length; i++) {
            DeliveryRuleType rule = dp.getDeliveryRule(i);
            if (rule.getMode().equals(DeliveryRuleTypeMode.push)) {
                return true;
            }
        }
        return false;
    }

    /**
 * Determine whether a subscription has any pull delivery rules
 *
 * @return true if this is a pull subscription
 */
    private boolean isSubscriptionPull(SubscriptionType sub) {
        OfferType offer = sub.getOffer();
        DeliveryPolicyType dp = offer.getDeliveryPolicy();
        for (int i = 0; i < dp.getDeliveryRule().length; i++) {
            DeliveryRuleType rule = dp.getDeliveryRule(i);
            if (rule.getMode().equals(DeliveryRuleTypeMode.pull)) {
                return true;
            }
        }
        return false;
    }

    /**
 * Sends a delivery confirmations message to the syndicator
 */
    protected void sendDeliveryConfirmations() {
        if (confirmationsList.size() > 0) {
            ConfirmationType[] confirmations = (ConfirmationType[]) confirmationsList.toArray(new ConfirmationType[confirmationsList.size()]);
            confirmationsList.clear();
            PackageConfirmationsType pConf = new PackageConfirmationsType(confirmations);
            try {
                TwICEUtil.setRequestHeader((Stub) syndicator, createIceHeader(null));
                syndicator.packageConfirmations(pConf);
            } catch (StatusCode sc) {
                IceFault fault = new IceFault(sc.getCode().intValue(), null);
                Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.sendDeliveryConfirmations", fault.toString());
            } catch (Exception e) {
                Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber sendDeliveryConfirmations", e);
            }
        }
    }

    /**
 * Requests the contents of all specified "pull" subscriptions from
 * the syndicator.
 */
    public String[] requestSubscriptionContents(HttpServletRequest request) {
        String subId;
        ArrayList requests = new ArrayList();
        for (int i = 0; (subId = request.getParameter("sub-" + i + "-id")) != null; ++i) {
            if (request.getParameter("sub-" + i + "-select") != null) {
                SubscriptionType sub = findSubscription(subId);
                if (sub == null) {
                    String msg = "Error: unknown subscription " + subId;
                    Logger.instance().log(Logger.ERROR, loggerPrefix, msg);
                    String[] status = { msg };
                    return status;
                }
                if (!isSubscriptionPull(sub)) {
                    continue;
                }
                Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.requestSubscriptionContents", "subscriptionId=" + sub.getSubscriptionId() + " current-state=" + sub.getCurrentState());
                GetPackageType getPackageReq = new GetPackageType(new Parameters(), sub.getCurrentState(), sub.getSubscriptionId());
                requests.add(getPackageReq);
            }
        }
        if (requests.size() == 0) {
            String[] status = { "No pull subscriptions specified" };
            return status;
        }
        GetPackageType[] gAll = (GetPackageType[]) requests.toArray(new GetPackageType[requests.size()]);
        GetPackages getPackagesReq = new GetPackages(gAll, null);
        try {
            TwICEUtil.setRequestHeader((Stub) syndicator, createIceHeader(null));
            Packages p = syndicator.getPackages(getPackagesReq);
            Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.requestSubscriptionContents", "getPackages returned successfully");
            String[] status = processContentPackages(p);
            sendDeliveryConfirmations();
            return status;
        } catch (StatusCode sc) {
            IceFault fault = new IceFault(sc.getCode().intValue(), null);
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber requestSubscriptionContents", fault.toString());
            String[] status = { fault.toString() };
            return status;
        } catch (Exception e) {
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber requestSubscriptionContents", e);
            String[] status = { e.toString() };
            return status;
        }
    }

    /**
 * Requests the status of the specified subscription from
 * the syndicator.  If no subscription is specified, requests
 * status from all subscriptions.
 *
 * @param subId subscription ID
 */
    public String[] requestSubscriptionStatus(String subId) {
        Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.requestSubscriptionStatus", "subscriptionId=" + subId);
        GetStatus getStatusReq = new GetStatus();
        if (subId != null) {
            getStatusReq.setSubscriptionId(new Token(subId));
        }
        try {
            TwICEUtil.setRequestHeader((Stub) syndicator, createIceHeader(null));
            StatusType status = syndicator.getStatus(getStatusReq);
            SubscriptionType[] subscriptions = status.getSubscription();
            String[] result = new String[subscriptions.length];
            for (int i = 0; i < subscriptions.length; i++) {
                result[i] = "current-state=" + subscriptions[i].getCurrentState();
            }
            return result;
        } catch (StatusCode sc) {
            IceFault fault = new IceFault(sc.getCode().intValue(), null);
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber requestSubscriptionStatus", fault.toString());
            String[] status = { fault.toString() };
            return status;
        } catch (Exception e) {
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber requestSubscriptionStatus", e);
            String[] status = { e.toString() };
            return status;
        }
    }

    private String processContentPackage(PackageType pkg) throws StatusCode {
        if (pkg.isConfirmation()) {
            ConfirmationType confirmation = new ConfirmationType();
            confirmation.setPackageId(pkg.getPackageId());
            confirmation.setConfirmed(true);
            confirmation.setProcessingCompleted(ConfirmationTypeProcessingCompleted.received);
            confirmationsList.add(confirmation);
        }
        int numAdds = 0;
        int numRemoves = 0;
        String subId = pkg.getSubscriptionId().toString();
        SubscriptionType subscription = findSubscription(subId);
        if (subscription == null) {
            throw new StatusCode(new PositiveInteger("406"), null, pkg.getSubscriptionId(), null, null, null);
        }
        Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.processContentPackage", "subscriptionId=" + subId + " new-state=" + pkg.getNewState());
        ArrayList existingItems = null;
        if (pkg.isFullupdate()) {
            clearContents(subId, false);
        }
        CmPackage cm = pkg.getCmPackage();
        RemoveType[] removes = cm.getRemoveItem();
        if (removes != null) {
            numRemoves = removes.length;
            try {
                existingItems = loadSubscriptionContents(subId);
            } catch (Exception e) {
                Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.processContentPackage", e);
                throw new StatusCode(new PositiveInteger("500"), null, new Token(subId), null, null, null);
            }
            for (int j = 0; j < numRemoves; j++) {
                RemoveType remove = cm.getRemoveItem(j);
                if (remove != null) {
                    removeContent(remove, existingItems);
                }
            }
        }
        AddType[] adds = cm.getAdd();
        if (adds != null) {
            numAdds = adds.length;
        }
        for (int j = 0; j < numAdds; j++) {
            AddType add = cm.getAdd(j);
            if (add.isIsNew()) {
                addContent(subId, add);
            } else {
                if (existingItems == null) {
                    try {
                        existingItems = loadSubscriptionContents(subId);
                    } catch (Exception e) {
                        Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.processContentPackage", e);
                        throw new StatusCode(new PositiveInteger("500"), null, new Token(subId), null, null, null);
                    }
                }
                updateContent(subId, add, existingItems);
            }
        }
        if (pkg.getNewState() != null) {
            String newState = pkg.getNewState().toString();
            subscription.setCurrentState(pkg.getNewState());
            store();
        }
        String pstatus = subId + ": processed " + numAdds + " adds and " + numRemoves + " removes";
        return pstatus;
    }

    private String[] processContentPackages(Packages p) throws StatusCode {
        int numPackages = p.get_package().length;
        Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.processContentPackages", numPackages + " packages");
        String[] status = new String[numPackages];
        for (int i = 0; i < numPackages; i++) {
            PackageType pkg = p.get_package(i);
            status[i] = processContentPackage(pkg);
        }
        return status;
    }

    private void clearContents(String subId, boolean removeDir) {
        String contentsDir = Props.instance().getProperty("subscriber.subscription_contents_dir");
        String path = contentsDir + File.separator + subId + File.separator;
        File dir = new File(path);
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                files[i].delete();
            }
            if (removeDir) {
                dir.delete();
            }
        }
    }

    private void addContent(String subId, AddType add) {
        String contentsDir = Props.instance().getProperty("subscriber.subscription_contents_dir");
        String path = contentsDir + File.separator + subId + File.separator;
        String filesPath = null;
        String contentFilename = null;
        if (add.getMetadata() != null && add.getMetadata().getContentFilename() != null) {
            contentFilename = add.getMetadata().getContentFilename().toString();
            filesPath = contentsDir + File.separator + subId + "-files" + File.separator;
        }
        String fileName = subId;
        if (dateTimeStamps) {
            fileName = dateFormatter.format(new Date()) + fileName;
        }
        fileName = FileUtils.uniqueFileName(path, fileName);
        File addFile = new File(path, fileName);
        File parentDir = addFile.getParentFile();
        if (!parentDir.isDirectory() && !parentDir.mkdir()) {
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.addContent", "Error creating contents' parent directory " + parentDir.getPath());
            return;
        }
        if (contentFilename != null && add.getItem() != null) {
            File filesDir = new File(filesPath);
            if (!filesDir.isDirectory() && !filesDir.mkdir()) {
                Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.addContent", "Error creating contents files directory " + filesDir.getPath());
                return;
            }
            if (contentFilename.indexOf('/') > 0) {
                int index = contentFilename.lastIndexOf('/');
                String contentDirname = contentFilename.substring(0, index);
                File contentDir = new File(filesDir, contentDirname);
                if (!contentDir.isDirectory() && !contentDir.mkdirs()) {
                    Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.addContent", "Error creating content files subdirectory " + contentDir.getPath());
                    return;
                }
            }
            File contentsFile = new File(filesDir, contentFilename);
            MessageElement[] elements = add.getItem().get_any();
            MessageElement element = elements[0];
            FileOutputStream fOut = null;
            try {
                fOut = new FileOutputStream(contentsFile);
                if (add.getItem().getContentTransferEncoding().toString().equals("base64")) {
                    byte[] data = (byte[]) element.getValueAsType(new QName("http://www.w3.org/1999/XMLSchema", "base64Binary"));
                    fOut.write(data);
                } else {
                    org.w3c.dom.Document doc = XMLUtils.newDocument();
                    org.w3c.dom.Element domElement = element.getAsDOM();
                    doc.appendChild(doc.importNode(domElement, true));
                    XMLUtils.DocumentToStream(doc, fOut);
                }
            } catch (Exception e) {
                Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.addContent", e);
            } finally {
                try {
                    if (fOut != null) fOut.close();
                } catch (IOException e) {
                }
            }
            add.getItem().set_any(null);
        }
        FileWriter out = null;
        try {
            out = new FileWriter(addFile);
            storeAdd(add, out);
        } catch (Exception e) {
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.addContent", e);
        } finally {
            try {
                if (out != null) out.close();
            } catch (IOException e) {
            }
        }
    }

    private void updateContent(String subId, AddType add, ArrayList existingItems) {
        String elementId = add.getSubscriptionElementId().toString();
        Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.updateContent", "subscription-element-id=" + elementId);
        LocalItem localItem = null;
        Iterator it = existingItems.iterator();
        while (it.hasNext()) {
            LocalItem cur = (LocalItem) it.next();
            if (cur.getAdd().getSubscriptionElementId() != null) {
                if (elementId.equals(cur.getAdd().getSubscriptionElementId().toString())) {
                    localItem = cur;
                    break;
                }
            }
        }
        if (localItem == null) {
            throw new RuntimeException("updateContent item not found");
        }
        String contentsDir = Props.instance().getProperty("subscriber.subscription_contents_dir");
        String path = contentsDir + File.separator + subId + File.separator;
        String filesPath = null;
        String contentFilename = null;
        if (add.getMetadata() != null && add.getMetadata().getContentFilename() != null) {
            contentFilename = add.getMetadata().getContentFilename().toString();
            filesPath = contentsDir + File.separator + subId + "-files" + File.separator;
        }
        if (contentFilename != null && add.getItem() != null) {
            File filesDir = new File(filesPath);
            File contentsFile = new File(filesDir, contentFilename);
            MessageElement[] elements = add.getItem().get_any();
            MessageElement element = elements[0];
            FileOutputStream fOut = null;
            try {
                fOut = new FileOutputStream(contentsFile);
                if (add.getItem().getContentTransferEncoding().toString().equals("base64")) {
                    byte[] data = (byte[]) element.getValueAsType(new QName("http://www.w3.org/1999/XMLSchema", "base64Binary"));
                    fOut.write(data);
                } else {
                    org.w3c.dom.Document doc = XMLUtils.newDocument();
                    org.w3c.dom.Element domElement = element.getAsDOM();
                    doc.appendChild(doc.importNode(domElement, true));
                    XMLUtils.DocumentToStream(doc, fOut);
                }
            } catch (Exception e) {
                Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.updateContent", e);
            } finally {
                try {
                    if (fOut != null) fOut.close();
                } catch (IOException e) {
                }
            }
            add.getItem().set_any(null);
        }
        FileWriter out = null;
        File addFile = localItem.getAddFile();
        try {
            out = new FileWriter(addFile);
            storeAdd(add, out);
        } catch (Exception e) {
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.updateContent", e);
        } finally {
            try {
                if (out != null) out.close();
            } catch (IOException e) {
            }
        }
    }

    private void storeAdd(AddType add, FileWriter out) throws Exception {
        if (add.getItemRef() != null) {
            Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.storeAdd", "storing ItemRef");
        } else if (add.getItem() != null) {
            ItemType item = add.getItem();
            Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.storeAdd", "storing Item with encoding " + item.getContentTransferEncoding());
        } else {
            Logger.instance().log(Logger.WARNING, loggerPrefix, "TwICESubscriber.storeAdd", "no content found");
        }
        org.w3c.dom.Document doc = XMLUtils.newDocument();
        MessageElement element = new MessageElement(AddType.getTypeDesc().getXmlType().getNamespaceURI(), AddType.getTypeDesc().getXmlType().getLocalPart(), add);
        org.w3c.dom.Element domElement = element.getAsDOM();
        doc.appendChild(doc.importNode(domElement, true));
        XMLUtils.DocumentToWriter(doc, out);
    }

    private void removeContent(RemoveType remove, ArrayList existingItems) {
        String removeElementId = remove.getSubscriptionElementId().toString();
        Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.removeContent", "subscription-element-id=" + removeElementId);
        Iterator it = existingItems.iterator();
        while (it.hasNext()) {
            LocalItem localItem = (LocalItem) it.next();
            String elementId = localItem.getAdd().getSubscriptionElementId().toString();
            if (elementId != null && elementId.equals(removeElementId)) {
                localItem.getAddFile().delete();
                existingItems.remove(localItem);
                return;
            }
        }
        return;
    }

    public void handlePing() throws StatusCode {
        Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.handlePing", "received ping request");
        Header hdr = null;
        try {
            hdr = TwICEUtil.getIceHeader(msgContext);
            TwICEUtil.setResponseHeader(msgContext, createIceHeader(hdr));
            return;
        } catch (StatusCode sc) {
            if (sc.getMessageId() == null) {
                sc.setMessageId(hdr.getMessageId());
            }
            throw sc;
        } catch (Exception e) {
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.handlePing", e);
            throw new StatusCode(new PositiveInteger("500"), hdr.getMessageId(), new Token("0"), null, null, null);
        }
    }

    public PackageConfirmationsType receivePackage(PackageType p) throws StatusCode {
        Header hdr = null;
        try {
            hdr = TwICEUtil.getIceHeader(msgContext);
            String status = processContentPackage(p);
            Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.receiveContentPackage", status);
            ConfirmationType[] confirmations = (ConfirmationType[]) confirmationsList.toArray(new ConfirmationType[confirmationsList.size()]);
            confirmationsList.clear();
            PackageConfirmationsType pConf = new PackageConfirmationsType(confirmations);
            TwICEUtil.setResponseHeader(msgContext, createIceHeader(hdr));
            return pConf;
        } catch (StatusCode sc) {
            if (sc.getMessageId() == null) {
                sc.setMessageId(hdr.getMessageId());
            }
            throw sc;
        } catch (Exception e) {
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.receivePackage", e);
            throw new StatusCode(new PositiveInteger("500"), hdr.getMessageId(), p.getSubscriptionId(), null, null, null);
        }
    }

    public PackageConfirmationsType receivePackages(Packages p) throws StatusCode {
        Header hdr = null;
        try {
            hdr = TwICEUtil.getIceHeader(msgContext);
            String[] status = processContentPackages(p);
            ConfirmationType[] confirmations = (ConfirmationType[]) confirmationsList.toArray(new ConfirmationType[confirmationsList.size()]);
            confirmationsList.clear();
            PackageConfirmationsType pConf = new PackageConfirmationsType(confirmations);
            TwICEUtil.setResponseHeader(msgContext, createIceHeader(hdr));
            return pConf;
        } catch (StatusCode sc) {
            if (sc.getMessageId() == null) {
                sc.setMessageId(hdr.getMessageId());
            }
            throw sc;
        } catch (Exception e) {
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.receivePackages", e);
            throw new StatusCode(new PositiveInteger("500"), hdr.getMessageId(), p.get_package(0).getSubscriptionId(), null, null, null);
        }
    }

    public OK receiveCancellation(Cancellation cancellationResp) throws StatusCode {
        Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.receiveCancellation", "cancellation");
        Header hdr = null;
        try {
            hdr = TwICEUtil.getIceHeader(msgContext);
            String subId = cancellationResp.getSubscriptionId().toString();
            SubscriptionType subscription = findSubscription(subId);
            if (subscription == null) {
                throw new StatusCode(new PositiveInteger("406"), hdr.getMessageId(), cancellationResp.getSubscriptionId(), null, null, null);
            }
            clearContents(subId, true);
            subscriptions.remove(subscription);
            store();
            TwICEUtil.setResponseHeader(msgContext, createIceHeader(hdr));
            return new OK();
        } catch (StatusCode sc) {
            if (sc.getMessageId() == null) {
                sc.setMessageId(hdr.getMessageId());
            }
            throw sc;
        } catch (Exception e) {
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.receiveCancellation", e);
            throw new StatusCode(new PositiveInteger("500"), hdr.getMessageId(), cancellationResp.getSubscriptionId(), null, null, null);
        }
    }

    public OK receiveSubscription(SubscriptionType subscriptionResp) throws StatusCode {
        Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.receiveSubscription", "subscription");
        Header hdr = null;
        try {
            hdr = TwICEUtil.getIceHeader(msgContext);
            subscriptionResp.setCurrentState(new Token("ICE-INITIAL"));
            subscriptions.add(subscriptionResp);
            store();
            TwICEUtil.setResponseHeader(msgContext, createIceHeader(hdr));
            return new OK();
        } catch (StatusCode sc) {
            throw sc;
        } catch (Exception e) {
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.receiveSubscription", e);
            throw new StatusCode(new PositiveInteger("500"), hdr.getMessageId(), subscriptionResp.getSubscriptionId(), null, null, null);
        }
    }

    public OK receiveStatus(StatusType statusResp) throws StatusCode {
        Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.receiveStatus", "status");
        Header hdr = null;
        try {
            hdr = TwICEUtil.getIceHeader(msgContext);
            SubscriptionType[] subscriptions = statusResp.getSubscription();
            for (int i = 0; i < subscriptions.length; i++) {
                SubscriptionType subscription = subscriptions[i];
                String subId = subscription.getSubscriptionId().toString();
                if (findSubscription(subId) == null) {
                    Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.receiveStatus", "unknown subscriptionId=" + subId);
                    throw new StatusCode(new PositiveInteger("406"), hdr.getMessageId(), subscription.getSubscriptionId(), null, null, null);
                }
            }
            TwICEUtil.setResponseHeader(msgContext, createIceHeader(hdr));
            return new OK();
        } catch (StatusCode sc) {
            throw sc;
        } catch (Exception e) {
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.receiveStatus", e);
            throw new StatusCode(new PositiveInteger("500"), hdr.getMessageId(), statusResp.getSubscription(0).getSubscriptionId(), null, null, null);
        }
    }

    /**
 * Returns the contents of a subscription as a string. If the subscription
 * contents directory contains a single file whose name is the subscription
 * id, return the contents of that file. Else the subscription must contain
 * directory structure; return a string representation of the file hierarchy.
 *
 * @param sub a subscription
 * @return the contents of the subscription
 */
    protected String subscriptionContentsAsString(SubscriptionType sub) {
        String contentsDir = Props.instance().getProperty("subscriber.subscription_contents_dir");
        String subId = sub.getSubscriptionId().toString();
        String contentsDirPath = contentsDir + File.separator + subId;
        File f = new File(contentsDirPath + File.separator + subId);
        if (f.exists() && !f.isDirectory()) {
            StringWriter out = new StringWriter();
            FileReader in = null;
            try {
                in = new FileReader(f);
                char[] buf = new char[BUFSIZ];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            } catch (IOException ioe) {
                Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.subscriptionContentsAsString", ioe);
            } finally {
                try {
                    if (in != null) in.close();
                } catch (IOException ioe2) {
                    Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.subscriptionContentsAsString", ioe2);
                }
            }
            return out.toString();
        } else {
            f = new File(contentsDirPath);
            if (f.exists() && f.isDirectory()) {
                StringBuffer buf = new StringBuffer();
                buildDirectoryHierarchy(buf, "", f);
                return buf.toString();
            } else {
                return "No subscription contents found.";
            }
        }
    }

    /**
 * Recursively builds a string representation of a directory structure.
 *
 * @param buf output goes into this string buffer
 * @param dirPrefix a string representing the directories above this one
 * @param dir a directory
 */
    protected void buildDirectoryHierarchy(StringBuffer buf, String dirPrefix, File dir) {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; ++i) {
            String name = files[i].getName();
            if (files[i].isDirectory()) buildDirectoryHierarchy(buf, dirPrefix + File.separator + name, files[i]); else {
                buf.append(dirPrefix);
                buf.append(File.separator);
                buf.append(name);
                buf.append("\n");
            }
        }
    }

    /**
 * Pings the syndicator and returns the response.
 */
    public String pingSyndicator() {
        try {
            TwICEUtil.setRequestHeader((Stub) syndicator, createIceHeader(null));
            syndicator.ping();
            return "Ping Success!";
        } catch (Exception e) {
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber pingSyndicator", e);
            return "Ping Failure: " + e.toString();
        }
    }

    /**
 * Loads subscriber and subscription information from permanent storage.
 */
    protected void load() {
        FileInputStream in = null;
        try {
            Persistence store = new Persistence(pstoreDir);
            in = store.getFileInputStream(getUuid());
            if (in == null) {
                return;
            }
            SubscriberParser sparser = new SubscriberParser();
            InputSource src = new InputSource(in);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser parser = factory.newSAXParser();
            XMLReader reader = parser.getXMLReader();
            reader.setContentHandler(sparser);
            reader.parse(src);
            subscriptions = sparser.getSubscriptions();
            Logger.instance().log(Logger.DEBUG, loggerPrefix, "TwICESubscriber.load", subscriptions.size() + " subscriptions found");
        } catch (Exception e) {
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.load", e);
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException ioe2) {
            }
        }
    }

    /**
 * Stores subscriber and subscription information to permanent storage.
 */
    protected void store() {
        Persistence store = new Persistence(pstoreDir);
        PrintWriter out = store.getPrintWriter(getUuid());
        try {
            org.w3c.dom.Document doc = XMLUtils.newDocument();
            org.w3c.dom.Element root = doc.createElement("subscriber");
            doc.appendChild(root);
            for (Iterator iter = subscriptions(); iter.hasNext(); ) {
                SubscriptionType sub = (SubscriptionType) iter.next();
                MessageElement element = new MessageElement(SubscriptionType.getTypeDesc().getXmlType().getNamespaceURI(), SubscriptionType.getTypeDesc().getXmlType().getLocalPart(), sub);
                org.w3c.dom.Element domElement = element.getAsDOM();
                root.appendChild(doc.importNode(domElement, true));
            }
            XMLUtils.PrettyDocumentToWriter(doc, out);
        } catch (Exception e) {
            Logger.instance().log(Logger.ERROR, loggerPrefix, "TwICESubscriber.store", e);
        } finally {
            if (out != null) out.close();
        }
    }
}
