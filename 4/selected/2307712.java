package com.tegsoft.tobe.crm;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import com.google.gdata.client.Query;
import com.google.gdata.client.Service.GDataRequest;
import com.google.gdata.client.contacts.ContactsService;
import com.google.gdata.data.Link;
import com.google.gdata.data.TextConstruct;
import com.google.gdata.data.contacts.Birthday;
import com.google.gdata.data.contacts.ContactEntry;
import com.google.gdata.data.contacts.ContactFeed;
import com.google.gdata.data.contacts.ContactGroupEntry;
import com.google.gdata.data.contacts.ContactGroupFeed;
import com.google.gdata.data.contacts.GroupMembershipInfo;
import com.google.gdata.data.contacts.Nickname;
import com.google.gdata.data.contacts.Website;
import com.google.gdata.data.contacts.Website.Rel;
import com.google.gdata.data.extensions.AdditionalName;
import com.google.gdata.data.extensions.Email;
import com.google.gdata.data.extensions.FamilyName;
import com.google.gdata.data.extensions.FullName;
import com.google.gdata.data.extensions.GivenName;
import com.google.gdata.data.extensions.Im;
import com.google.gdata.data.extensions.Name;
import com.google.gdata.data.extensions.NameSuffix;
import com.google.gdata.data.extensions.Organization;
import com.google.gdata.data.extensions.PhoneNumber;
import com.google.gdata.data.extensions.PostalAddress;
import com.google.gdata.util.ContentType;
import com.tegsoft.tobe.db.dataset.DataRow;
import com.tegsoft.tobe.db.dataset.Dataset;
import com.tegsoft.tobe.util.Compare;
import com.tegsoft.tobe.util.Converter;
import com.tegsoft.tobe.util.DateUtil;
import com.tegsoft.tobe.util.NullStatus;
import com.tegsoft.tobe.util.UiUtil;

public class ContactSync {

    private static final String entry_id_column = "CONTID";

    private Dataset TBLENTRIES;

    private Dataset TBLSYNC;

    private Dataset TBLSYNC_ALL;

    private Dataset TBLCRMCONTACC;

    private final ContactsService service = new ContactsService("TobeCRM");

    private final String LOGTYPE = "CONTACT";

    private ArrayList<String> photoSyncList = new ArrayList<String>();

    private ArrayList<byte[]> photos = new ArrayList<byte[]>();

    private String myContactsId;

    public void sync() throws Exception {
        TBLCRMCONTACC = UiUtil.getDataset("TBLCRMCONTACC");
        TBLCRMCONTACC.reFill();
        TBLENTRIES = UiUtil.getDataset("TBLCRMCONTACTS");
        TBLENTRIES.reFill();
        for (int i = 0; i < TBLCRMCONTACC.getRowCount(); i++) {
            UiUtil.getDataSource("TBLCRMCONTACC").seekTo(i);
            photoSyncList.clear();
            photos.clear();
            TBLSYNC = UiUtil.getDataset("TBLCRMCONTSYNC");
            TBLSYNC.reFill();
            final String ACCNAME = TBLCRMCONTACC.getRow(i).getString("ACCNAME");
            final String PASSWORD = TBLCRMCONTACC.getRow(i).getString("PASSWORD");
            TBLSYNC_ALL = UiUtil.getDataset("TBLCRMGRPSYNC_ALL");
            TBLSYNC_ALL.reFill();
            try {
                service.setUserCredentials(ACCNAME, PASSWORD);
            } catch (Exception ex) {
                GoogleSync.log(ACCNAME, LOGTYPE, ex.getMessage());
                throw ex;
            }
            Query query = new Query(new URL("http://www.google.com/m8/feeds/groups/" + ACCNAME + "/full"));
            query.setMaxResults(10000);
            ContactGroupFeed groupFeed = service.getFeed(query, ContactGroupFeed.class);
            for (ContactGroupEntry entry : groupFeed.getEntries()) {
                if (!entry.hasSystemGroup()) {
                    continue;
                }
                if (entry.getPlainTextContent().contains("My Contacts")) {
                    myContactsId = entry.getId();
                }
            }
            query = new Query(new URL("http://www.google.com/m8/feeds/contacts/" + ACCNAME + "/full"));
            query.setMaxResults(10000);
            ContactFeed feed = service.getFeed(query, ContactFeed.class);
            sync(feed, ACCNAME);
            if (photoSyncList.size() > 0) {
                query = new Query(new URL("http://www.google.com/m8/feeds/contacts/" + ACCNAME + "/full"));
                query.setMaxResults(10000);
                feed = service.getFeed(query, ContactFeed.class);
                for (ContactEntry entry : feed.getEntries()) {
                    int index = photoSyncList.indexOf(entry.getId());
                    if (index >= 0) {
                        writePhoto(photos.get(index), entry);
                    }
                }
            }
        }
    }

    private void sync(ContactFeed feed, String ACCNAME) throws Exception {
        for (ContactEntry entry : feed.getEntries()) {
            if (entry.getName() == null) {
                continue;
            }
            boolean done = false;
            for (int j = 0; j < TBLSYNC.getRowCount(); j++) {
                DataRow rowTBLSYNC = TBLSYNC.getRow(j);
                if (Compare.equal(entry.getId(), rowTBLSYNC.get("SYNCID"))) {
                    for (int i = 0; i < TBLENTRIES.getRowCount(); i++) {
                        DataRow rowTBLENTRIES = TBLENTRIES.getRow(i);
                        if (Compare.equal(rowTBLSYNC.getString(entry_id_column), rowTBLENTRIES.get(entry_id_column))) {
                            if ((rowTBLENTRIES.getDate("MODDATE").getTime() - rowTBLSYNC.getDate("SYNCDATE").getTime()) > 1 * 60 * 1000) {
                                GoogleSync.log(ACCNAME, LOGTYPE, Converter.asNotNullString(rowTBLENTRIES.get("FIRSTNAME")) + " " + Converter.asNotNullString(rowTBLENTRIES.get("MIDDLENAME")) + " " + Converter.asNotNullString(rowTBLENTRIES.get("LASTNAME")) + " (DB->Google)");
                                overrideGoogle(rowTBLENTRIES, entry);
                                entry.update();
                                rowTBLSYNC.set("UPDDATE", new Timestamp(entry.getUpdated().getValue()));
                                rowTBLSYNC.set("SYNCDATE", DateUtil.now());
                                rowTBLSYNC.set("SYNCID", entry.getId());
                                photoSyncList.add(entry.getId());
                                photos.add(rowTBLENTRIES.getBytes("PHOTO"));
                                break;
                            }
                            if ((entry.getUpdated().getValue() - rowTBLSYNC.getDate("UPDDATE").getTime()) > 1 * 60 * 1000) {
                                GoogleSync.log(ACCNAME, LOGTYPE, getDisplayName(entry) + " (Google->DB)");
                                overrideTBL(rowTBLENTRIES, entry);
                                rowTBLSYNC.set("UPDDATE", new Timestamp(entry.getUpdated().getValue()));
                                rowTBLSYNC.set("SYNCDATE", DateUtil.now());
                                rowTBLSYNC.set("SYNCID", entry.getId());
                                break;
                            }
                            break;
                        }
                    }
                    done = true;
                }
            }
            if (!done) {
                for (int j = 0; j < TBLSYNC_ALL.getRowCount(); j++) {
                    DataRow rowTBLSYNC = TBLSYNC_ALL.getRow(j);
                    if (Compare.equal(entry.getId(), rowTBLSYNC.get("SYNCID"))) {
                        done = true;
                    }
                }
                if (!done) {
                    GoogleSync.log(ACCNAME, LOGTYPE, getDisplayName(entry) + " (Google->DB*)");
                    DataRow rowTBLENTRIES = TBLENTRIES.addNewDataRow();
                    overrideTBL(rowTBLENTRIES, entry);
                    TBLENTRIES.save();
                    DataRow rowTBLSYNC = TBLSYNC.addNewDataRow();
                    rowTBLSYNC.set(entry_id_column, rowTBLENTRIES.get(entry_id_column));
                    rowTBLSYNC.set("ACCNAME", ACCNAME);
                    rowTBLSYNC.set("UPDDATE", new Timestamp(entry.getUpdated().getValue()));
                    rowTBLSYNC.set("SYNCDATE", DateUtil.now());
                    rowTBLSYNC.set("SYNCID", entry.getId());
                    TBLSYNC.save();
                }
            }
        }
        TBLENTRIES.save();
        TBLSYNC.save();
        for (int j = 0; j < TBLENTRIES.getRowCount(); j++) {
            DataRow rowTBLENTRIES = TBLENTRIES.getRow(j);
            DataRow rowTBLSYNC = null;
            for (int k = 0; k < TBLSYNC.getRowCount(); k++) {
                if (Compare.equal(TBLSYNC.getRow(k).get(entry_id_column), rowTBLENTRIES.get(entry_id_column))) {
                    rowTBLSYNC = TBLSYNC.getRow(k);
                }
            }
            if (rowTBLSYNC == null) {
                ContactEntry entry = new ContactEntry();
                GoogleSync.log(ACCNAME, LOGTYPE, Converter.asNotNullString(rowTBLENTRIES.get("FIRSTNAME")) + " " + Converter.asNotNullString(rowTBLENTRIES.get("MIDDLENAME")) + " " + Converter.asNotNullString(rowTBLENTRIES.get("LASTNAME")) + " (DB->Google*)");
                overrideGoogle(rowTBLENTRIES, entry);
                entry = service.insert(new URL("http://www.google.com/m8/feeds/contacts/" + ACCNAME + "/full"), entry);
                DataRow newRowTBLSYNC = TBLSYNC.addNewDataRow();
                newRowTBLSYNC.set(entry_id_column, rowTBLENTRIES.get(entry_id_column));
                newRowTBLSYNC.set("ACCNAME", ACCNAME);
                newRowTBLSYNC.set("UPDDATE", new Timestamp(entry.getUpdated().getValue()));
                newRowTBLSYNC.set("SYNCDATE", DateUtil.now());
                newRowTBLSYNC.set("SYNCID", entry.getId());
                TBLSYNC.save();
                photoSyncList.add(entry.getId());
                photos.add(rowTBLENTRIES.getBytes("PHOTO"));
                continue;
            } else {
                boolean exists = false;
                for (ContactEntry entry : feed.getEntries()) {
                    if (Compare.equal(rowTBLSYNC.getString("SYNCID"), entry.getId())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    ContactEntry entry = new ContactEntry();
                    GoogleSync.log(ACCNAME, LOGTYPE, Converter.asNotNullString(rowTBLENTRIES.get("FIRSTNAME")) + " " + Converter.asNotNullString(rowTBLENTRIES.get("MIDDLENAME")) + " " + Converter.asNotNullString(rowTBLENTRIES.get("LASTNAME")) + " (DB->Google*)");
                    overrideGoogle(rowTBLENTRIES, entry);
                    entry = service.insert(new URL("http://www.google.com/m8/feeds/contacts/" + ACCNAME + "/full"), entry);
                    rowTBLSYNC.set(entry_id_column, rowTBLENTRIES.get(entry_id_column));
                    rowTBLSYNC.set("ACCNAME", ACCNAME);
                    rowTBLSYNC.set("UPDDATE", new Timestamp(entry.getUpdated().getValue()));
                    rowTBLSYNC.set("SYNCDATE", DateUtil.now());
                    rowTBLSYNC.set("SYNCID", entry.getId());
                    TBLSYNC.save();
                    photoSyncList.add(entry.getId());
                    photos.add(rowTBLENTRIES.getBytes("PHOTO"));
                    continue;
                }
            }
        }
    }

    private void writePhoto(byte[] photo, ContactEntry entry) throws Exception {
        if (photo != null) {
            try {
                Link photoLink = entry.getContactPhotoLink();
                URL photoUrl = new URL(photoLink.getHref());
                GDataRequest request = service.createRequest(GDataRequest.RequestType.UPDATE, photoUrl, new ContentType("image/jpeg"));
                request.setEtag(photoLink.getEtag());
                OutputStream requestStream = request.getRequestStream();
                requestStream.write(photo);
                request.execute();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void overrideGoogle(DataRow rowTBLENTRIES, ContactEntry entry) throws Exception {
        Name name = new Name();
        name.setGivenName(getGivenName(rowTBLENTRIES.getString("FIRSTNAME")));
        name.setAdditionalName(getAdditionalName(rowTBLENTRIES.getString("MIDDLENAME")));
        name.setFamilyName(getFamilyName(rowTBLENTRIES.getString("LASTNAME")));
        name.setNameSuffix(getNameSuffix(rowTBLENTRIES.getString("NAMESUFFIX")));
        entry.setName(name);
        entry.setNickname(getNickname(rowTBLENTRIES.getString("NICKNAME")));
        entry.setTitle(getTextConstruct(rowTBLENTRIES.getString("PROFESSION")));
        entry.getEmailAddresses().clear();
        for (int i = 1; i <= 4; i++) {
            if (NullStatus.isNotNull(rowTBLENTRIES.getString("EMAIL" + i))) {
                entry.getEmailAddresses().add(getEmail(rowTBLENTRIES.getString("EMAILTYPE" + i), rowTBLENTRIES.getString("EMAIL" + i)));
            }
        }
        entry.getPhoneNumbers().clear();
        for (int i = 1; i <= 4; i++) {
            if (NullStatus.isNotNull(rowTBLENTRIES.getString("PHONE" + i))) {
                entry.getPhoneNumbers().add(getPhoneNumber(rowTBLENTRIES.getString("PHONETYPE" + i), rowTBLENTRIES.getString("PHONE" + i)));
            }
        }
        entry.getImAddresses().clear();
        for (int i = 1; i <= 4; i++) {
            if (NullStatus.isNotNull(rowTBLENTRIES.getString("INST" + i))) {
                entry.getImAddresses().add(getIm(rowTBLENTRIES.getString("INSTTYPE" + i), rowTBLENTRIES.getString("INST" + i)));
            }
        }
        entry.getWebsites().clear();
        if (NullStatus.isNotNull(rowTBLENTRIES.getString("PERSPAGE"))) {
            entry.getWebsites().add(getWebsite("HOME", rowTBLENTRIES.getString("PERSPAGE")));
        }
        if (NullStatus.isNotNull(rowTBLENTRIES.getString("BUSSPAGE"))) {
            entry.getWebsites().add(getWebsite("WORK", rowTBLENTRIES.getString("BUSSPAGE")));
        }
        if (NullStatus.isNotNull(rowTBLENTRIES.getString("WEBLOG"))) {
            entry.getWebsites().add(getWebsite("BLOG", rowTBLENTRIES.getString("WEBLOG")));
        }
        if (NullStatus.isNotNull(rowTBLENTRIES.getDate("BIRTHDAY"))) {
            entry.setBirthday(new Birthday(Converter.asNotNullString(rowTBLENTRIES.getDate("BIRTHDAY"))));
        }
        entry.getPostalAddresses().clear();
        if (NullStatus.isNotNull(rowTBLENTRIES.getString("HOMEADDR"))) {
            entry.getPostalAddresses().add(getPostalAddress("HOME", rowTBLENTRIES.getString("HOMEADDR")));
        }
        if (NullStatus.isNotNull(rowTBLENTRIES.getString("WORKADDR"))) {
            entry.getPostalAddresses().add(getPostalAddress("WORK", rowTBLENTRIES.getString("WORKADDR")));
        }
        if (NullStatus.isNotNull(rowTBLENTRIES.getString("OTHRADDR"))) {
            entry.getPostalAddresses().add(getPostalAddress("OTHER", rowTBLENTRIES.getString("OTHRADDR")));
        }
        boolean addToMyContacts = true;
        for (GroupMembershipInfo groupMembershipInfo : entry.getGroupMembershipInfos()) {
            if (myContactsId.equals(groupMembershipInfo.getHref())) {
                addToMyContacts = false;
            }
        }
        if (addToMyContacts) {
            if (NullStatus.isNotNull(myContactsId)) {
                GroupMembershipInfo groupMembershipInfo = new GroupMembershipInfo();
                groupMembershipInfo.setHref(myContactsId);
                entry.addGroupMembershipInfo(groupMembershipInfo);
            }
        }
    }

    private void overrideTBL(DataRow rowTBLENTRIES, ContactEntry entry) throws Exception {
        if (entry.getName() == null) {
            return;
        }
        String FULLNAME = getValue(entry.getName().getFullName());
        String FIRSTNAME = getValue(entry.getName().getGivenName());
        String MIDDLENAME = getValue(entry.getName().getAdditionalName());
        String LASTNAME = getValue(entry.getName().getFamilyName());
        String NICKNAME = getValue(entry.getNickname());
        String NAMESUFFIX = getValue(entry.getName().getNameSuffix());
        if ((NullStatus.isNull(FIRSTNAME)) && (NullStatus.isNotNull(FULLNAME))) {
            if (FULLNAME.indexOf(" ") > 0) {
                FIRSTNAME = FULLNAME.substring(0, FULLNAME.indexOf(" "));
            } else {
                FIRSTNAME = FULLNAME;
            }
        }
        if ((NullStatus.isNull(LASTNAME)) && (NullStatus.isNotNull(FULLNAME))) {
            if (FULLNAME.indexOf(" ") > 0) {
                LASTNAME = FULLNAME.substring(FULLNAME.indexOf(" ") + 1);
            }
        }
        rowTBLENTRIES.set("FIRSTNAME", FIRSTNAME);
        rowTBLENTRIES.set("MIDDLENAME", MIDDLENAME);
        rowTBLENTRIES.set("LASTNAME", LASTNAME);
        rowTBLENTRIES.set("NICKNAME", NICKNAME);
        rowTBLENTRIES.set("NAMESUFFIX", NAMESUFFIX);
        int counter = 0;
        for (Email email : entry.getEmailAddresses()) {
            counter++;
            rowTBLENTRIES.set("EMAIL" + counter, email.getAddress());
            rowTBLENTRIES.set("EMAILTYPE" + counter, getRel(email.getRel()));
            if (counter == 4) {
                break;
            }
        }
        counter = 0;
        for (PhoneNumber phoneNumber : entry.getPhoneNumbers()) {
            counter++;
            rowTBLENTRIES.set("PHONE" + counter, phoneNumber.getPhoneNumber());
            rowTBLENTRIES.set("PHONETYPE" + counter, getRel(phoneNumber.getRel()));
            if (counter == 8) {
                break;
            }
        }
        counter = 0;
        for (Im im : entry.getImAddresses()) {
            counter++;
            rowTBLENTRIES.set("INST" + counter, im.getAddress());
            rowTBLENTRIES.set("INSTTYPE" + counter, getUnDashedValue(im.getProtocol()));
            if (counter == 4) {
                break;
            }
        }
        for (Website website : entry.getWebsites()) {
            if (Rel.HOME.equals(website.getRel())) {
                rowTBLENTRIES.set("PERSPAGE", website.getHref());
            } else if (Rel.WORK.equals(website.getRel())) {
                rowTBLENTRIES.set("BUSSPAGE", website.getHref());
            } else if (Rel.BLOG.equals(website.getRel())) {
                rowTBLENTRIES.set("WEBLOG", website.getHref());
            }
        }
        for (Organization organization : entry.getOrganizations()) {
            if (organization.getPrimary()) {
                if (organization.getOrgTitle() != null) {
                    rowTBLENTRIES.set("JOBTITLE", organization.getOrgTitle().getValue());
                }
                if (organization.getOrgDepartment() != null) {
                    rowTBLENTRIES.set("DEPARTMENT", organization.getOrgDepartment().getValue());
                }
            }
        }
        try {
            Link photoLink = entry.getContactPhotoLink();
            if (photoLink != null) {
                if (photoLink.getEtag() != null) {
                    GDataRequest request = service.createLinkQueryRequest(photoLink);
                    request.execute();
                    InputStream in = request.getResponseStream();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    for (int read = 0; (read = in.read(buffer)) != -1; out.write(buffer, 0, read)) ;
                    rowTBLENTRIES.set("PHOTO", out.toByteArray());
                    in.close();
                    request.end();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        rowTBLENTRIES.set("BIRTHDAY", Converter.asDate(getValue(entry.getBirthday())));
        for (PostalAddress postalAddress : entry.getPostalAddresses()) {
            if ("HOME".equals(getRel(postalAddress.getRel()))) {
                rowTBLENTRIES.set("HOMEADDR", getValue(postalAddress));
            } else if ("WORK".equals(getRel(postalAddress.getRel()))) {
                rowTBLENTRIES.set("WORKADDR", getValue(postalAddress));
            } else if ("OTHER".equals(getRel(postalAddress.getRel()))) {
                rowTBLENTRIES.set("OTHRADDR", getValue(postalAddress));
            }
        }
    }

    private static String getDisplayName(ContactEntry entry) {
        if (entry.getName() == null) {
            return "NO NAME";
        }
        return getValue(entry.getName().getGivenName()) + getValue(entry.getName().getAdditionalName()) + getValue(entry.getName().getFamilyName());
    }

    private static String getValue(FullName value) {
        if (value != null) {
            return Converter.asNotNullString(value.getValue());
        }
        return "";
    }

    private static String getValue(GivenName value) {
        if (value != null) {
            return Converter.asNotNullString(value.getValue());
        }
        return "";
    }

    private static String getValue(AdditionalName value) {
        if (value != null) {
            return Converter.asNotNullString(value.getValue());
        }
        return "";
    }

    private static String getValue(FamilyName value) {
        if (value != null) {
            return Converter.asNotNullString(value.getValue());
        }
        return "";
    }

    private static String getValue(Nickname value) {
        if (value != null) {
            return Converter.asNotNullString(value.getValue());
        }
        return "";
    }

    private static String getValue(NameSuffix value) {
        if (value != null) {
            return Converter.asNotNullString(value.getValue());
        }
        return "";
    }

    private static String getValue(Birthday value) {
        if (value != null) {
            return Converter.asNotNullString(value.getValue());
        }
        return "";
    }

    private static String getValue(PostalAddress value) {
        if (value != null) {
            return Converter.asNotNullString(value.getValue());
        }
        return "";
    }

    private static String getRel(String rel) {
        return getUnDashedValue(rel);
    }

    private static String getUnDashedValue(String value) {
        if (NullStatus.isNull(value)) {
            return null;
        }
        if (value.indexOf("#") > 0) {
            return value.substring(value.indexOf("#") + 1);
        }
        return value;
    }

    private static GivenName getGivenName(String value) {
        if (NullStatus.isNull(value)) {
            return null;
        }
        GivenName typedValue = new GivenName();
        typedValue.setValue(value);
        return typedValue;
    }

    private static AdditionalName getAdditionalName(String value) {
        if (NullStatus.isNull(value)) {
            return null;
        }
        AdditionalName typedValue = new AdditionalName();
        typedValue.setValue(value);
        return typedValue;
    }

    private static FamilyName getFamilyName(String value) {
        if (NullStatus.isNull(value)) {
            return null;
        }
        FamilyName typedValue = new FamilyName();
        typedValue.setValue(value);
        return typedValue;
    }

    private static Nickname getNickname(String value) {
        if (NullStatus.isNull(value)) {
            return null;
        }
        Nickname typedValue = new Nickname();
        typedValue.setValue(value);
        return typedValue;
    }

    private static NameSuffix getNameSuffix(String value) {
        if (NullStatus.isNull(value)) {
            return null;
        }
        NameSuffix typedValue = new NameSuffix();
        typedValue.setValue(value);
        return typedValue;
    }

    private static TextConstruct getTextConstruct(String value) {
        if (NullStatus.isNull(value)) {
            return null;
        }
        return TextConstruct.plainText(value);
    }

    private static Email getEmail(String type, String value) {
        if (NullStatus.isNull(value)) {
            return null;
        }
        Email typedValue = new Email();
        if ("WORK".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.Email.Rel.WORK);
        } else if ("HOME".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.Email.Rel.HOME);
        } else if ("OTHER".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.Email.Rel.OTHER);
        } else if ("GENERAL".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.Email.Rel.GENERAL);
        }
        typedValue.setAddress(value);
        return typedValue;
    }

    private static PostalAddress getPostalAddress(String type, String value) {
        if (NullStatus.isNull(value)) {
            return null;
        }
        PostalAddress typedValue = new PostalAddress();
        if ("WORK".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PostalAddress.Rel.WORK);
        } else if ("HOME".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PostalAddress.Rel.HOME);
        } else if ("OTHER".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PostalAddress.Rel.OTHER);
        } else if ("GENERAL".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PostalAddress.Rel.GENERAL);
        }
        typedValue.setValue(value);
        return typedValue;
    }

    private static PhoneNumber getPhoneNumber(String type, String value) {
        if (NullStatus.isNull(value)) {
            return null;
        }
        PhoneNumber typedValue = new PhoneNumber();
        if ("GENERAL".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PhoneNumber.Rel.GENERAL);
        } else if ("MOBILE".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PhoneNumber.Rel.MOBILE);
        } else if ("HOME".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PhoneNumber.Rel.HOME);
        } else if ("WORK".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PhoneNumber.Rel.WORK);
        } else if ("WORK_MOBILE".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PhoneNumber.Rel.WORK_MOBILE);
        } else if ("CALLBACK".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PhoneNumber.Rel.CALLBACK);
        } else if ("ASSISTANT".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PhoneNumber.Rel.ASSISTANT);
        } else if ("COMPANY_MAIN".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PhoneNumber.Rel.COMPANY_MAIN);
        } else if ("INTERNAL_EXTENSION".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PhoneNumber.Rel.INTERNAL_EXTENSION);
        } else if ("FAX".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PhoneNumber.Rel.FAX);
        } else if ("HOME_FAX".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PhoneNumber.Rel.HOME_FAX);
        } else if ("WORK_FAX".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PhoneNumber.Rel.WORK_FAX);
        } else if ("OTHER_FAX".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PhoneNumber.Rel.OTHER_FAX);
        } else if ("PAGER".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PhoneNumber.Rel.PAGER);
        } else if ("WORK_PAGER".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PhoneNumber.Rel.WORK_PAGER);
        } else if ("CAR".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PhoneNumber.Rel.CAR);
        } else if ("SATELLITE".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PhoneNumber.Rel.SATELLITE);
        } else if ("RADIO".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PhoneNumber.Rel.RADIO);
        } else if ("TTY_TDD".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PhoneNumber.Rel.TTY_TDD);
        } else if ("ISDN".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PhoneNumber.Rel.ISDN);
        } else if ("TELEX".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PhoneNumber.Rel.TELEX);
        } else if ("OTHER".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PhoneNumber.Rel.OTHER);
        } else if ("MAIN".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.PhoneNumber.Rel.MAIN);
        }
        typedValue.setPhoneNumber(value);
        return typedValue;
    }

    private static Im getIm(String type, String value) {
        if (NullStatus.isNull(value)) {
            return null;
        }
        Im typedValue = new Im();
        if ("MSN".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.Im.Rel.OTHER);
            typedValue.setProtocol(com.google.gdata.data.extensions.Im.Protocol.MSN);
        } else if ("GOOGLE_TALK".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.Im.Rel.OTHER);
            typedValue.setProtocol(com.google.gdata.data.extensions.Im.Protocol.GOOGLE_TALK);
        } else if ("ICQ".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.Im.Rel.OTHER);
            typedValue.setProtocol(com.google.gdata.data.extensions.Im.Protocol.ICQ);
        } else if ("AIM".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.Im.Rel.OTHER);
            typedValue.setProtocol(com.google.gdata.data.extensions.Im.Protocol.AIM);
        } else if ("YAHOO".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.Im.Rel.OTHER);
            typedValue.setProtocol(com.google.gdata.data.extensions.Im.Protocol.YAHOO);
        } else if ("JABBER".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.Im.Rel.OTHER);
            typedValue.setProtocol(com.google.gdata.data.extensions.Im.Protocol.JABBER);
        } else if ("NETMEETING".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.Im.Rel.OTHER);
            typedValue.setProtocol(com.google.gdata.data.extensions.Im.Protocol.NETMEETING);
        } else if ("SKYPE".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.Im.Rel.OTHER);
            typedValue.setProtocol(com.google.gdata.data.extensions.Im.Protocol.SKYPE);
        } else if ("QQ".equalsIgnoreCase(type)) {
            typedValue.setRel(com.google.gdata.data.extensions.Im.Rel.OTHER);
            typedValue.setProtocol(com.google.gdata.data.extensions.Im.Protocol.QQ);
        } else {
            typedValue.setRel(com.google.gdata.data.extensions.Im.Rel.OTHER);
            typedValue.setProtocol(com.google.gdata.data.extensions.Im.Protocol.MSN);
        }
        typedValue.setAddress(value);
        return typedValue;
    }

    private static Website getWebsite(String type, String value) {
        if (NullStatus.isNull(value)) {
            return null;
        }
        Website typedValue = new Website();
        typedValue.setRel(Rel.valueOf(type));
        typedValue.setHref(value);
        return typedValue;
    }
}
