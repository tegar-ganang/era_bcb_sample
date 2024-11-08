package net.sf.traser.client.minimalist;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import net.sf.traser.databinding.base.CreateEvent;
import net.sf.traser.databinding.base.GetPropertyValues;
import net.sf.traser.databinding.base.PropertyValuesReport;
import net.sf.traser.databinding.management.Authorize;
import net.sf.traser.databinding.management.Authorize.Property;
import net.sf.traser.databinding.management.Authorize.Property.Partner;
import net.sf.traser.databinding.management.ListPartners;
import net.sf.traser.databinding.management.PartnersList;
import net.sf.traser.databinding.management.SetEndpoint;
import net.sf.traser.databinding.management.SetEndpoint.Endpoint;
import net.sf.traser.databinding.management.SetPartner;
import net.sf.traser.service.AuthorizationFault;
import net.sf.traser.service.ExistenceFault;
import net.sf.traser.service.Management;
import net.sf.traser.service.ManagementStub;
import org.apache.axis2.databinding.types.URI;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Marcell Szathmári
 */
public class ManagementTest extends ServiceTester {

    static {
        URI a, b;
        try {
            a = new URI("http://localhost:8081/traser/services");
        } catch (Exception ex) {
            a = null;
        }
        try {
            b = new URI("http://localhost:8082/traser/services");
        } catch (Exception ex) {
            b = null;
        }
        URI_0 = a;
        URI_1 = b;
    }

    public ManagementTest() {
    }

    public static final URI URI_0;

    public static final URI URI_1;

    public static final String NEW_PARTNER = "CN=Marcell Szathmari, OU=EMI, O=MTA SZTAKI, L=Budapest, ST=Budapest, C=HU";

    @Test(expected = AuthorizationFault.class)
    public void clientListPartnersTestCallOnly() throws Exception {
        System.out.println("clientListPartners");
        ListPartners request = new ListPartners();
        mgmtClient.listPartners(request);
    }

    @Test
    public void adminListPartnersTestCallOnly() throws Exception {
        System.out.println("adminListPartners");
        ListPartners request = new ListPartners();
        PartnersList result = mgmtAdmin.listPartners(request);
    }

    private void setTestPartner(Management stub) throws Exception {
        SetPartner request = new SetPartner();
        try {
            BufferedReader certReader = new BufferedReader(new FileReader("test/certificate.cer"));
            String cert = "";
            for (String line = certReader.readLine(); line != null; line = certReader.readLine()) {
                cert += line + "\n";
            }
            request.setSetPartner(cert);
        } catch (Exception ex) {
            fail("could not load certificate file to add");
        }
        stub.setPartner(request);
    }

    @Test(expected = AuthorizationFault.class)
    public void clientSetPartner() throws Exception {
        System.out.println("clientSetPartner");
        setTestPartner(mgmtClient);
    }

    @Test
    public void adminSetPartner() throws Exception {
        System.out.println("adminSetPartner");
        ListPartners request = new ListPartners();
        PartnersList result = mgmtAdmin.listPartners(request);
        HashSet<String> partners = new HashSet<String>();
        HashSet<String> added = new HashSet<String>();
        for (String partner : result.getPartners()) {
            partners.add(partner);
        }
        assertFalse("Cannot test setPartner operation, because test certificate is already installed", partners.contains(NEW_PARTNER));
        setTestPartner(mgmtAdmin);
        result = mgmtAdmin.listPartners(request);
        for (String partner : result.getPartners()) {
            if (!partners.contains(partner)) {
                added.add(partner);
            } else {
                partners.remove(partner);
            }
        }
        for (String partner : added) {
            System.out.println("Added partner: " + partner);
        }
        for (String partner : partners) {
            System.out.println("Removed partner: " + partner);
        }
        assertFalse("There were no partners added ", added.size() < 1);
        assertFalse("There were at least two partners added ", added.size() > 1);
        assertEquals(NEW_PARTNER, added.toArray(new String[] {})[0]);
        assertEquals("Some partners were removed from the list", 0, partners.size());
    }

    private void setTestEndpoint(Management stub) throws Exception {
        SetEndpoint request = new SetEndpoint();
        Endpoint ep0 = new Endpoint();
        ep0.setString(URI_0.toString());
        ep0.setValue(true);
        request.addEndpoint(ep0);
        request.setPartner(NEW_PARTNER);
        stub.setEndpoint(request);
    }

    @Test(expected = AuthorizationFault.class)
    public void clientSetEndpoint() throws Exception {
        System.out.println("clientSetEndpoint");
        setTestEndpoint(mgmtClient);
    }

    @Test
    public void adminSetEndpoint() throws Exception {
        System.out.println("adminSetEndpoint");
        setTestEndpoint(mgmtAdmin);
    }

    @Test(expected = ExistenceFault.class)
    public void clientAuthorizeNonExistingItem() throws Exception {
        System.out.println("clientAuthorizeTestCallOnly");
        Authorize request = new Authorize();
        request.setItem(createItem(ITEM_1));
        Property prop = new Property();
        prop.setName(PROP_1);
        Partner partner = new Partner();
        partner.setString(NEW_PARTNER);
        partner.setAction(Partner.Action.GRANT);
        partner.setValue(Partner.Value.WRITE);
        prop.addPartner(partner);
        request.addProperty(prop);
        mgmtClient.authorize(request);
    }

    @Test(expected = AuthorizationFault.class)
    public void clientAuthorizeTestCallOnly() throws Exception {
        System.out.println("clientAuthorizeTestCallOnly");
        basicClient.requestID(createRequestID(ITEM_1));
        Authorize request = new Authorize();
        request.setItem(createItem(ITEM_1));
        Property prop = new Property();
        prop.setName(PROP_1);
        Partner partner = new Partner();
        partner.setString(NEW_PARTNER);
        partner.setAction(Partner.Action.GRANT);
        partner.setValue(Partner.Value.WRITE);
        prop.addPartner(partner);
        request.addProperty(prop);
        mgmtClient.authorize(request);
    }

    @Test
    public void adminAuthorizeTestCallOnly() throws Exception {
        System.out.println("adminAuthorizeTestCallOnly");
        basicAdmin.createProperty(createCreateProperty(ITEM_1, PROP_1));
        Authorize request = new Authorize();
        request.setItem(createItem(ITEM_1));
        Property prop = new Property();
        prop.setName(PROP_1);
        Partner partner = new Partner();
        partner.setString(NEW_PARTNER);
        partner.setAction(Partner.Action.GRANT);
        partner.setValue(Partner.Value.WRITE);
        prop.addPartner(partner);
        request.addProperty(prop);
        mgmtAdmin.authorize(request);
    }

    @Test
    public void adminAuthorizeFunctionalTest_unauthRead() throws Exception {
        System.out.println("adminAuthorizeFunctionalTest_unauthRead -- client is not authorized to read a property");
        CreateEvent evt = createCreateEvent(ITEM_1, getPastDate(2 * HOUR), createPropertyValueUpdate(PROP_1));
        basicAdmin.createEvent(evt);
        GetPropertyValues gpva = new GetPropertyValues();
        gpva.addProperty(PROP_1);
        gpva.setItem(createItem(ITEM_1));
        PropertyValuesReport report = basicClient.getPropertyValues(gpva);
        if (report.getProperty(0).sizeValue() > 0 || report.getProperty(0).getCanRead()) {
            fail("The service should have thrown an authorization error exception.");
        }
    }

    @Test(expected = AuthorizationFault.class)
    public void adminAuthorizeFunctionalTest_unauthWrite() throws Exception {
        System.out.println("adminAuthorizeFunctionalTest_unauthWrite -- client is not authorized to write a property");
        CreateEvent evt = createCreateEvent(ITEM_1, getPastDate(3 * HOUR), createPropertyValueUpdate(PROP_1));
        basicClient.createEvent(evt);
        fail("The service should have thrown an authorization error exception.");
    }

    @Test(expected = AuthorizationFault.class)
    public void adminAuthorizeFunctionalTest_grantRead() throws Exception {
        System.out.println("adminAuthorizeFunctionalTest_grantRead -- client is given authorization to read a property, but not to write it");
        Authorize request = new Authorize();
        request.setItem(createItem(ITEM_1));
        Property prop = new Property();
        prop.setName(PROP_1);
        Partner partner = new Partner();
        partner.setString(CLIENT_NAME);
        partner.setAction(Partner.Action.GRANT);
        partner.setValue(Partner.Value.READ);
        prop.addPartner(partner);
        request.addProperty(prop);
        mgmtAdmin.authorize(request);
        GetPropertyValues gpva = new GetPropertyValues();
        gpva.addProperty(PROP_1);
        gpva.setItem(createItem(ITEM_1));
        PropertyValuesReport pvr = basicClient.getPropertyValues(gpva);
        assertTrue("The returned report does not contain a property", pvr.getProperties().size() > 0);
        assertTrue("The returned report contains too many properties", pvr.getProperties().size() < 2);
        assertEquals("The returned report does not contain the correct property", PROP_1, pvr.getProperties().get(0).getPropertyName());
        assertEquals("The returned report does not contain the correct number of property values", 1, pvr.getProperties().get(0).getValues().size());
        CreateEvent evt = createCreateEvent(ITEM_1, getPastDate(3 * HOUR), createPropertyValueUpdate(PROP_1));
        basicClient.createEvent(evt);
        fail("The service should have thrown an authorization error exception.");
    }

    @Test
    public void adminAuthorizeFunctionalTest_grantWrite() throws Exception {
        System.out.println("adminAuthorizeFunctionalTest_grantWrite -- client is given authorization to write a property");
        Authorize request = new Authorize();
        request.setItem(createItem(ITEM_1));
        Property prop = new Property();
        prop.setName(PROP_1);
        Partner partner = new Partner();
        partner.setString(CLIENT_NAME);
        partner.setAction(Partner.Action.GRANT);
        partner.setValue(Partner.Value.WRITE);
        prop.addPartner(partner);
        request.addProperty(prop);
        mgmtAdmin.authorize(request);
        CreateEvent evt = createCreateEvent(ITEM_1, getPastDate(HOUR), createPropertyValueUpdate(PROP_1));
        basicClient.createEvent(evt);
    }

    @Test
    public void adminAuthorizeFunctionalTest_revokeRead() throws Exception {
        System.out.println("adminAuthorizeFunctionalTest_revokeRead -- client is deprived of authorization to read a property, but it can still write it");
        Authorize request = new Authorize();
        request.setItem(createItem(ITEM_1));
        Property prop = new Property();
        prop.setName(PROP_1);
        Partner partner = new Partner();
        partner.setString(CLIENT_NAME);
        partner.setAction(Partner.Action.REVOKE);
        partner.setValue(Partner.Value.READ);
        prop.addPartner(partner);
        request.addProperty(prop);
        mgmtAdmin.authorize(request);
        try {
            CreateEvent evt = createCreateEvent(ITEM_1, getPastDate(HOUR), createPropertyValueUpdate(PROP_1));
            basicClient.createEvent(evt);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Error, could not write the property!");
        }
        GetPropertyValues gpva = new GetPropertyValues();
        gpva.addProperty(PROP_1);
        gpva.setItem(createItem(ITEM_1));
        PropertyValuesReport pvr = basicClient.getPropertyValues(gpva);
        if (pvr.getProperty(0).sizeValue() > 0 || pvr.getProperty(0).getCanRead()) {
            fail("The service should have thrown an authorization error exception.");
        }
    }

    @Test(expected = AuthorizationFault.class)
    public void adminAuthorizeFunctionalTest_revokeWrite() throws Exception {
        System.out.println("adminAuthorizeFunctionalTest_revokeWrite -- client is deprived of authorization to write a property");
        Authorize request = new Authorize();
        request.setItem(createItem(ITEM_1));
        Property prop = new Property();
        prop.setName(PROP_1);
        Partner partner = new Partner();
        partner.setString(CLIENT_NAME);
        partner.setAction(Partner.Action.REVOKE);
        partner.setValue(Partner.Value.WRITE);
        prop.addPartner(partner);
        request.addProperty(prop);
        mgmtAdmin.authorize(request);
        CreateEvent evt = createCreateEvent(ITEM_1, getPastDate(HOUR), createPropertyValueUpdate(PROP_1));
        basicClient.createEvent(evt);
        fail("The service should have thrown an authorization error exception.");
    }

    @Test
    public void adminAuthorizeFunctionalTest_grantReadAndWrite() throws Exception {
        System.out.println("adminAuthorizeFunctionalTest_grantReadAndWrite -- client is granted authorization to read and write a property");
        Authorize request = new Authorize();
        request.setItem(createItem(ITEM_1));
        Property prop = new Property();
        prop.setName(PROP_1);
        Partner partner = new Partner();
        partner.setString(CLIENT_NAME);
        partner.setAction(Partner.Action.GRANT);
        partner.setValue(Partner.Value.WRITE);
        prop.addPartner(partner);
        partner = new Partner();
        partner.setString(CLIENT_NAME);
        partner.setAction(Partner.Action.GRANT);
        partner.setValue(Partner.Value.READ);
        prop.addPartner(partner);
        request.addProperty(prop);
        mgmtAdmin.authorize(request);
        CreateEvent evt = createCreateEvent(ITEM_1, getPastDate(HOUR), createPropertyValueUpdate(PROP_1));
        basicClient.createEvent(evt);
        GetPropertyValues gpva = new GetPropertyValues();
        gpva.addProperty(PROP_1);
        gpva.setItem(createItem(ITEM_1));
        PropertyValuesReport pvr = basicClient.getPropertyValues(gpva);
    }

    @Test
    public void adminAuthorizeFunctionalTest_revokeReadAndWrite() throws Exception {
        System.out.println("adminAuthorizeFunctionalTest_revokeReadAndWrite -- client is deprived of authorization to read and write a property");
        Authorize request = new Authorize();
        request.setItem(createItem(ITEM_1));
        Property prop = new Property();
        prop.setName(PROP_1);
        Partner partner = new Partner();
        partner.setString(CLIENT_NAME);
        partner.setAction(Partner.Action.REVOKE);
        partner.setValue(Partner.Value.WRITE);
        prop.addPartner(partner);
        partner = new Partner();
        partner.setString(CLIENT_NAME);
        partner.setAction(Partner.Action.REVOKE);
        partner.setValue(Partner.Value.READ);
        prop.addPartner(partner);
        request.addProperty(prop);
        mgmtAdmin.authorize(request);
        try {
            CreateEvent evt = createCreateEvent(ITEM_1, getPastDate(HOUR), createPropertyValueUpdate(PROP_1));
            basicClient.createEvent(evt);
            fail("The service should have thrown an authorization error exception.");
        } catch (AuthorizationFault ex) {
        }
        GetPropertyValues gpva = new GetPropertyValues();
        gpva.addProperty(PROP_1);
        gpva.setItem(createItem(ITEM_1));
        PropertyValuesReport pvr = basicClient.getPropertyValues(gpva);
        if (pvr.getProperty(0).sizeValue() > 0 || pvr.getProperty(0).getCanRead()) {
            fail("The service should have thrown an authorization error exception.");
        }
    }
}
