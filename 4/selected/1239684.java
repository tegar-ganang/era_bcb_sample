package org.dimi.consync;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.TimeZone;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import com.google.gdata.client.Query;
import com.google.gdata.client.Service;
import com.google.gdata.client.contacts.ContactsService;
import com.google.gdata.data.DateTime;
import com.google.gdata.data.Link;
import com.google.gdata.data.contacts.BillingInformation;
import com.google.gdata.data.contacts.CalendarLink;
import com.google.gdata.data.contacts.ContactEntry;
import com.google.gdata.data.contacts.ContactFeed;
import com.google.gdata.data.extensions.Email;
import com.google.gdata.data.extensions.ExtendedProperty;
import com.google.gdata.data.extensions.FamilyName;
import com.google.gdata.data.extensions.GivenName;
import com.google.gdata.data.extensions.Name;
import com.google.gdata.data.extensions.Organization;
import com.google.gdata.data.extensions.PhoneNumber;
import com.google.gdata.data.extensions.StructuredPostalAddress;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ResourceNotFoundException;
import com.google.gdata.util.ServiceException;
import com.megginson.sax.DataWriter;

class ExportingContactsService extends ContactsService {

    private URL feedURL;

    private Query myQuery;

    private static Logger logger = Logger.getLogger(ExportingContactsService.class);

    public ExportingContactsService() {
        super("exampleCo-exampleApp-1");
    }

    protected ExportingContactsService(final String appName, final ExtProperties configFile, final boolean fetchdeleted) throws AuthenticationException, IOException, MalformedURLException {
        super(appName);
        setUserCredentials(configFile.getProperty("googleLogin"), configFile.getProperty("googlePass"));
        feedURL = new URL("http://www.google.com/m8/feeds/contacts" + "/" + configFile.getProperty("googleLogin") + "/full");
        myQuery = new Query(feedURL);
        myQuery.setMaxResults(10000);
        if (fetchdeleted == true) {
            myQuery.setStringCustomParameter("showdeleted", "true");
        }
    }

    private String downloadPhoto(final ContactEntry entry, final ContactsService service) throws ServiceException, IOException {
        try {
            final Link photoLink = entry.getContactPhotoLink();
            if (photoLink.getEtag() != null) {
                final Service.GDataRequest request = service.createLinkQueryRequest(photoLink);
                request.execute();
                final InputStream in = request.getResponseStream();
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                final String fileName = entry.getSelfLink().getHref().substring(entry.getSelfLink().getHref().lastIndexOf('/') + 1) + ".jpg";
                final RandomAccessFile file = new RandomAccessFile(fileName, "rw");
                final byte[] buffer = new byte[4096];
                for (int read = 0; (read = in.read(buffer)) != -1; out.write(buffer, 0, read)) {
                    ;
                }
                file.write(out.toByteArray());
                file.close();
                in.close();
                request.end();
                return (fileName);
            } else return ("");
        } catch (final ResourceNotFoundException e) {
        }
        return ("");
    }

    private ContactEntry fetchContactById(final String googleId) throws IOException, ServiceException, InterruptedException {
        final Query fetchQuery = new Query(feedURL);
        fetchQuery.setStringCustomParameter("entryID", googleId);
        final ContactFeed cf = query(fetchQuery, ContactFeed.class);
        if (cf.getEntries().size() > 0) return (cf.getEntries().get(0)); else return (null);
    }

    protected void exportNewContacts(final DateTime minUpdate, final String fileName, final ExtProperties props) throws IOException, ServiceException, SAXException {
        final String lastExport = props.getProperty("lastExportedItemChange", "0");
        final ExtProperties idMap = new ExtProperties(props.getProperty("mapFile"));
        myQuery.setUpdatedMin(new DateTime(new Date(Long.parseLong(lastExport) + 1), TimeZone.getDefault()));
        myQuery.setStringCustomParameter("orderby", "lastmodified");
        myQuery.setStringCustomParameter("sortorder", "ascending");
        final GregorianCalendar since = new GregorianCalendar();
        since.setTime(new Date(myQuery.getUpdatedMin().getValue()));
        logger.info("Exporting since: " + myQuery.getUpdatedMin().getValue() + " / " + since.getTime().toString());
        final ContactFeed cf = query(myQuery, ContactFeed.class);
        final File file = new File(fileName);
        if (file.exists() == true) file.delete();
        file.createNewFile();
        final FileWriter txt = new FileWriter(file);
        final PrintWriter out = new PrintWriter(txt);
        final DataWriter w = new DataWriter();
        w.setOutput(out);
        w.setIndentStep(2);
        w.startDocument();
        w.startElement("Contacts");
        for (final ContactEntry ce : cf.getEntries()) {
            final GregorianCalendar foundCal = new GregorianCalendar();
            String UniversalID = "";
            String Updated = "";
            if (idMap.getKeyFromValue(ce.getId()) != null) {
                if (idMap.containsKey(idMap.getKeyFromValue(ce.getId()) + ".updated")) {
                    Updated = idMap.getProperty(idMap.getKeyFromValue(ce.getId()) + ".updated");
                    UniversalID = idMap.getKeyFromValue(ce.getId());
                    foundCal.setTime(new Date(Long.parseLong(Updated)));
                } else {
                    continue;
                }
                logger.info("Found entry: " + ((ce.hasName() == true) ? (ce.getName().hasFullName() == true ? (ce.getName().getFullName().getValue()) : (ce.getName().hasGivenName() == true ? ce.getName().getGivenName().getValue() : " ") + " " + (ce.getName().hasFamilyName() == true ? ce.getName().getFamilyName().getValue() : " ")) : " unnamed") + " / " + ce.getUpdated().getValue() + " / " + new Date(ce.getUpdated().getValue()));
                if (Math.abs(ce.getUpdated().getValue() - Long.parseLong(Updated)) < 3000) {
                    logger.debug("Too small difference (" + new Date(ce.getUpdated().getValue()) + " vs. " + new Date(Long.parseLong(Updated)) + "), probably same timestamp, not exporting.");
                    continue;
                }
                if (Math.abs(since.getTimeInMillis() - ce.getUpdated().getValue()) < 3000) {
                    logger.info("Too recent timestamp, not exporting: " + ce.getUpdated().getValue());
                    continue;
                }
                props.setProperty("lastExportedItemChange", String.valueOf(ce.getUpdated().getValue()));
                props.flush("Autogenerated config file, edit at your own risk.");
            } else {
                UniversalID = "___";
                Updated = String.valueOf(ce.getUpdated().getValue());
            }
            w.startElement("Contact");
            w.dataElement("UniversalID", UniversalID);
            w.dataElement("googleID", ce.getId());
            w.dataElement("Updated", Updated);
            if (ce.hasDeleted() == true) {
                w.dataElement("Deleted", "True");
            }
            if (ce.hasBillingInformation()) {
                final BillingInformation bi = ce.getBillingInformation();
                w.dataElement("BillingInformation", bi.toString());
            }
            if (ce.hasBirthday() == true) {
                w.dataElement("Birthday", ce.getBirthday().getWhen());
            }
            for (final CalendarLink cl : ce.getCalendarLinks()) {
                w.dataElement("CalendarLink", cl.toString());
            }
            for (final Email mail : ce.getEmailAddresses()) {
                w.startElement("Email");
                if (mail.getDisplayName() != null && mail.getDisplayName().length() > 0) {
                    w.dataElement("DisplayName", mail.getDisplayName());
                }
                String ma = mail.getAddress();
                ma = ma.replaceAll("\\A.*<", "");
                ma = ma.replaceAll(">*\\Z", "");
                w.dataElement("MailAddress", ma);
                w.dataElement("Role", mail.getRel());
                w.endElement("Email");
            }
            if (ce.hasName()) {
                final Name name = ce.getName();
                if (name.hasFullName()) {
                    w.dataElement("FullName", name.getFullName().getValue());
                }
                if (name.hasFamilyName()) {
                    w.dataElement("LastName", name.getFamilyName().getValue());
                }
                if (name.hasGivenName()) {
                    w.dataElement("FirstName", name.getGivenName().getValue());
                }
            }
            for (final StructuredPostalAddress addr : ce.getStructuredPostalAddresses()) {
                w.startElement("Address");
                if (addr.hasFormattedAddress()) {
                    w.dataElement("FormattedAddress", addr.getFormattedAddress().getValue().toString());
                }
                if (addr.hasStreet() == true) {
                    w.dataElement("Street", addr.getStreet().getValue().toString());
                }
                if (addr.hasCity() == true) {
                    w.dataElement("City", addr.getCity().getValue().toString());
                }
                if (addr.hasCountry() == true) {
                    w.dataElement("Country", addr.getCountry().getValue().toString());
                }
                if (addr.hasHousename() == true) {
                    w.dataElement("Housename", addr.getHousename().getValue().toString());
                }
                if (addr.hasPostcode() == true) {
                    w.dataElement("PostCode", addr.getPostcode().getValue().toString());
                }
                if (addr.hasRegion() == true) {
                    w.dataElement("Region", addr.getRegion().getValue().toString());
                }
                if (addr.hasSubregion() == true) {
                    w.dataElement("Subregion", addr.getSubregion().getValue().toString());
                }
                if (addr.getRel().matches(".*#home") == true) {
                    w.dataElement("Role", "Home");
                } else {
                    w.dataElement("Role", "Office");
                }
                w.endElement("Address");
            }
            for (final PhoneNumber phone : ce.getPhoneNumbers()) {
                w.startElement("PhoneNumber");
                w.dataElement("Number", phone.getPhoneNumber());
                w.dataElement("Role", phone.getRel());
                w.endElement("PhoneNumber");
            }
            if (ce.hasOccupation() == true) {
                w.dataElement("Occupation", ce.getOccupation().toString());
            }
            if (ce.hasSubject() == true) {
                w.dataElement("Subject", ce.getSubject().toString());
            }
            if (ce.hasBillingInformation()) {
                final BillingInformation bi = ce.getBillingInformation();
                w.dataElement("BillingInformation", bi.toString());
            }
            for (final ExtendedProperty ep : ce.getExtendedProperties()) {
                w.dataElement(ep.getName(), ep.getValue());
            }
            for (final Organization org : ce.getOrganizations()) {
                w.startElement("Organization");
                if (org.hasOrgName()) {
                    w.dataElement("OrgName", org.getOrgName().getValue().toString());
                }
                if (org.hasOrgTitle()) {
                    w.dataElement("JobTitle", org.getOrgTitle().getValue().toString());
                }
                w.endElement("Organization");
            }
            try {
                final String p = downloadPhoto(ce, this);
                if (p.compareTo("") != 0) {
                    w.dataElement("Photo", System.getProperty("user.dir") + File.separator + p);
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
            w.endElement("Contact");
        }
        w.endElement("Contacts");
        w.endDocument();
        w.flush();
    }

    protected ContactEntry fetchContact(final String UniversalID, final Properties idMap) throws IOException, ServiceException, InterruptedException {
        return (fetchContact(UniversalID, idMap, true));
    }

    protected ContactEntry fetchContact(final String UniversalID, final Properties idMap, final boolean createNotExisting) throws IOException, ServiceException, InterruptedException {
        final String gid = idMap.getProperty(UniversalID, "___");
        ContactEntry tContact = null;
        if (gid.compareTo("___") != 0) {
            tContact = fetchContactById(gid);
        }
        if (tContact == null && createNotExisting == true) {
            logger.info(UniversalID + " not found, creating new entry");
            tContact = new ContactEntry();
            final Name name = new Name();
            final String NO_YOMI = null;
            name.setGivenName(new GivenName("_", NO_YOMI));
            name.setFamilyName(new FamilyName("_", NO_YOMI));
            tContact.setName(name);
        }
        return (tContact);
    }

    protected ContactEntry insert(final ContactEntry ce) throws IOException, ServiceException {
        return (insert(feedURL, ce));
    }

    protected ContactFeed query() throws IOException, ServiceException {
        return (query(myQuery, ContactFeed.class));
    }
}
