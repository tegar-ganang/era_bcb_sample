package org.opencms.file;

import org.opencms.file.types.CmsResourceTypeFolder;
import org.opencms.file.types.CmsResourceTypeJsp;
import org.opencms.file.types.CmsResourceTypePlain;
import org.opencms.file.types.CmsResourceTypeXmlPage;
import org.opencms.lock.CmsLockException;
import org.opencms.main.CmsIllegalArgumentException;
import org.opencms.main.OpenCms;
import org.opencms.security.CmsPermissionViolationException;
import org.opencms.security.CmsSecurityException;
import org.opencms.security.I_CmsPrincipal;
import org.opencms.test.OpenCmsTestCase;
import org.opencms.test.OpenCmsTestProperties;
import org.opencms.test.OpenCmsTestResourceConfigurableFilter;
import org.opencms.test.OpenCmsTestResourceFilter;
import org.opencms.util.CmsUUID;
import java.util.ArrayList;
import java.util.List;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Unit tests for the create and import methods.<p>
 * 
 * @author Alexander Kandzior 
 * 
 * @version $Revision: 1.25 $
 */
public class TestCreateWriteResource extends OpenCmsTestCase {

    /**
     * Default JUnit constructor.<p>
     * 
     * @param arg0 JUnit parameters
     */
    public TestCreateWriteResource(String arg0) {
        super(arg0);
    }

    /**
     * Test suite for this test class.<p>
     * 
     * @return the test suite
     */
    public static Test suite() {
        OpenCmsTestProperties.initialize(org.opencms.test.AllTests.TEST_PROPERTIES_PATH);
        TestSuite suite = new TestSuite();
        suite.setName(TestCreateWriteResource.class.getName());
        suite.addTest(new TestCreateWriteResource("testCreateResourceLockedFolder"));
        suite.addTest(new TestCreateWriteResource("testImportResource"));
        suite.addTest(new TestCreateWriteResource("testImportResourceAgain"));
        suite.addTest(new TestCreateWriteResource("testImportSibling"));
        suite.addTest(new TestCreateWriteResource("testImportFolder"));
        suite.addTest(new TestCreateWriteResource("testImportFolderAgain"));
        suite.addTest(new TestCreateWriteResource("testCreateResource"));
        suite.addTest(new TestCreateWriteResource("testCreateResourceJsp"));
        suite.addTest(new TestCreateWriteResource("testCreateResourceAgain"));
        suite.addTest(new TestCreateWriteResource("testCreateFolder"));
        suite.addTest(new TestCreateWriteResource("testCreateFolderAgain"));
        suite.addTest(new TestCreateWriteResource("testCreateDotnameResources"));
        suite.addTest(new TestCreateWriteResource("testOverwriteInvisibleResource"));
        suite.addTest(new TestCreateWriteResource("testCreateResourceWithSpecialChars"));
        TestSetup wrapper = new TestSetup(suite) {

            protected void setUp() {
                setupOpenCms("simpletest", "/sites/default/");
            }

            protected void tearDown() {
                removeOpenCms();
            }
        };
        return wrapper;
    }

    /**
     * Test creation of invalid resources that have only dots in their name.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testCreateDotnameResources() throws Throwable {
        CmsObject cms = getCmsObject();
        echo("Testing creating a resource with only dots in the name");
        Exception error = null;
        try {
            cms.createResource("/folder1/.", CmsResourceTypeFolder.getStaticTypeId(), null, null);
        } catch (CmsIllegalArgumentException e) {
            assertEquals(org.opencms.db.Messages.ERR_CREATE_RESOURCE_1, e.getMessageContainer().getKey());
            error = e;
        }
        assertNotNull(error);
        error = null;
        try {
            cms.createResource("/folder1/..", CmsResourceTypePlain.getStaticTypeId(), null, null);
        } catch (CmsIllegalArgumentException e) {
            assertEquals(org.opencms.db.Messages.ERR_CREATE_RESOURCE_1, e.getMessageContainer().getKey());
            error = e;
        }
        assertNotNull(error);
        error = null;
        try {
            cms.createResource("/folder1/.../", CmsResourceTypeFolder.getStaticTypeId(), null, null);
        } catch (CmsIllegalArgumentException e) {
            assertEquals(org.opencms.db.Messages.ERR_CREATE_RESOURCE_1, e.getMessageContainer().getKey());
            error = e;
        }
        assertNotNull(error);
        error = null;
        try {
            cms.createResource("/folder1/....", CmsResourceTypePlain.getStaticTypeId(), null, null);
        } catch (CmsIllegalArgumentException e) {
            assertEquals(org.opencms.db.Messages.ERR_CREATE_RESOURCE_1, e.getMessageContainer().getKey());
            error = e;
        }
        assertNotNull(error);
        error = null;
        try {
            cms.createResource("/folder1/...../", CmsResourceTypeFolder.getStaticTypeId(), null, null);
        } catch (CmsIllegalArgumentException e) {
            assertEquals(org.opencms.db.Messages.ERR_CREATE_RESOURCE_1, e.getMessageContainer().getKey());
            error = e;
        }
        assertNotNull(error);
    }

    /**
     * Test the create resource method for a folder.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testCreateFolder() throws Throwable {
        CmsObject cms = getCmsObject();
        echo("Testing creating a folder");
        String resourcename = "/folder1/test2/";
        long timestamp = System.currentTimeMillis() - 1;
        cms.createResource(resourcename, CmsResourceTypeFolder.getStaticTypeId(), null, null);
        CmsFolder folder = cms.readFolder(resourcename);
        assertEquals(folder.getState(), CmsResource.STATE_NEW);
        assertTrue(folder.getDateLastModified() > timestamp);
        assertTrue(folder.getDateCreated() > timestamp);
        assertIsFolder(cms, resourcename);
        assertProject(cms, resourcename, cms.getRequestContext().currentProject());
        assertState(cms, resourcename, CmsResource.STATE_NEW);
        assertDateLastModifiedAfter(cms, resourcename, timestamp);
        assertDateCreatedAfter(cms, resourcename, timestamp);
        assertUserLastModified(cms, resourcename, cms.getRequestContext().currentUser());
        cms.unlockProject(cms.getRequestContext().currentProject().getUuid());
        OpenCms.getPublishManager().publishProject(cms);
        OpenCms.getPublishManager().waitWhileRunning();
        assertState(cms, resourcename, CmsResource.STATE_UNCHANGED);
    }

    /**
     * Test the create a folder again.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testCreateFolderAgain() throws Throwable {
        CmsObject cms = getCmsObject();
        echo("Testing to create an existing folder again");
        String resourcename = "/folder1/test2/";
        storeResources(cms, resourcename);
        long timestamp = System.currentTimeMillis();
        assertState(cms, resourcename, CmsResource.STATE_UNCHANGED);
        cms.lockResource(resourcename);
        try {
            cms.createResource(resourcename, CmsResourceTypeFolder.getStaticTypeId(), null, null);
            fail("Existing resource '" + resourcename + "' was not detected!");
        } catch (CmsVfsResourceAlreadyExistsException e) {
        }
        CmsResource original = cms.readResource(resourcename);
        cms.deleteResource(resourcename, CmsResource.DELETE_PRESERVE_SIBLINGS);
        cms.createResource(resourcename, CmsResourceTypeFolder.getStaticTypeId(), null, null);
        assertIsFolder(cms, resourcename);
        assertProject(cms, resourcename, cms.getRequestContext().currentProject());
        assertState(cms, resourcename, CmsResource.STATE_CHANGED);
        assertDateLastModifiedAfter(cms, resourcename, timestamp);
        assertUserLastModified(cms, resourcename, cms.getRequestContext().currentUser());
        assertDateCreated(cms, resourcename, original.getDateCreated());
        assertUserCreated(cms, resourcename, cms.readUser(original.getUserCreated()));
        CmsResource created = cms.readResource(resourcename);
        if (!created.getResourceId().equals(original.getResourceId())) {
            fail("A created folder that replaced a deleted folder must have the same resource id!");
        }
        cms.unlockProject(cms.getRequestContext().currentProject().getUuid());
        OpenCms.getPublishManager().publishProject(cms);
        OpenCms.getPublishManager().waitWhileRunning();
        assertState(cms, resourcename, CmsResource.STATE_UNCHANGED);
    }

    /**
     * Test the create resource method.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testCreateResource() throws Throwable {
        CmsObject cms = getCmsObject();
        echo("Testing create resource");
        String resourcename = "/folder1/test2.html";
        long timestamp = System.currentTimeMillis() - 1;
        String contentStr = "Hello this is my other content";
        byte[] content = contentStr.getBytes();
        cms.createResource(resourcename, CmsResourceTypePlain.getStaticTypeId(), content, null);
        assertResourceType(cms, resourcename, CmsResourceTypePlain.getStaticTypeId());
        assertProject(cms, resourcename, cms.getRequestContext().currentProject());
        assertState(cms, resourcename, CmsResource.STATE_NEW);
        assertDateLastModifiedAfter(cms, resourcename, timestamp);
        assertDateCreatedAfter(cms, resourcename, timestamp);
        assertUserLastModified(cms, resourcename, cms.getRequestContext().currentUser());
        assertContent(cms, resourcename, content);
        cms.unlockProject(cms.getRequestContext().currentProject().getUuid());
        OpenCms.getPublishManager().publishProject(cms);
        OpenCms.getPublishManager().waitWhileRunning();
        assertState(cms, resourcename, CmsResource.STATE_UNCHANGED);
    }

    /**
     * Test the create resource method for an already existing resource.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testCreateResourceAgain() throws Throwable {
        CmsObject cms = getCmsObject();
        echo("Testing to create an existing resource again");
        String resourcename = "/folder1/test2.html";
        storeResources(cms, resourcename);
        long timestamp = System.currentTimeMillis();
        String contentStr = "Hello this is my NEW AND ALSO CHANGED other content";
        byte[] content = contentStr.getBytes();
        assertState(cms, resourcename, CmsResource.STATE_UNCHANGED);
        cms.lockResource(resourcename);
        try {
            cms.createResource(resourcename, CmsResourceTypePlain.getStaticTypeId(), content, null);
        } catch (Throwable e) {
            if (!(e instanceof CmsVfsResourceAlreadyExistsException)) {
                fail("Existing resource '" + resourcename + "' was not detected!");
            }
        }
        CmsResource original = cms.readResource(resourcename);
        cms.deleteResource(resourcename, CmsResource.DELETE_PRESERVE_SIBLINGS);
        cms.createResource(resourcename, CmsResourceTypePlain.getStaticTypeId(), content, null);
        assertProject(cms, resourcename, cms.getRequestContext().currentProject());
        assertState(cms, resourcename, CmsResource.STATE_CHANGED);
        assertDateLastModifiedAfter(cms, resourcename, timestamp);
        assertUserLastModified(cms, resourcename, cms.getRequestContext().currentUser());
        assertDateCreatedAfter(cms, resourcename, timestamp);
        assertUserCreated(cms, resourcename, cms.getRequestContext().currentUser());
        assertContent(cms, resourcename, content);
        CmsResource created = cms.readResource(resourcename);
        if (created.getResourceId().equals(original.getResourceId())) {
            fail("A created resource that replaced a deleted resource must not have the same resource id!");
        }
        cms.unlockProject(cms.getRequestContext().currentProject().getUuid());
        OpenCms.getPublishManager().publishProject(cms);
        OpenCms.getPublishManager().waitWhileRunning();
        assertState(cms, resourcename, CmsResource.STATE_UNCHANGED);
    }

    /**
     * Test the create resource method for jsp files without permissions.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testCreateResourceJsp() throws Throwable {
        CmsObject cms = getCmsObject();
        echo("Testing create resource for jsp files without permissions");
        CmsProject offlineProject = cms.getRequestContext().currentProject();
        String path = "/testCreateResourceJsp.jsp";
        String contentStr = "this is a really bad jsp code";
        cms.createResource(path, CmsResourceTypeJsp.getStaticTypeId(), contentStr.getBytes(), null);
        cms.loginUser("test1", "test1");
        cms.getRequestContext().setCurrentProject(offlineProject);
        String path2 = "/testCreateResourceJsp2.jsp";
        try {
            cms.createResource(path2, CmsResourceTypeJsp.getStaticTypeId(), contentStr.getBytes(), null);
            fail("createResource for jsp without permissions should fail");
        } catch (CmsSecurityException e) {
        }
    }

    /**
     * Test resource creation in a locked folder.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testCreateResourceLockedFolder() throws Throwable {
        CmsObject cms = getCmsObject();
        echo("Testing resource creation in a locked folder");
        String foldername = "/folder1/";
        String resourcename = foldername + "newResLF.html";
        cms.lockResource(foldername);
        cms.loginUser("test2", "test2");
        cms.getRequestContext().setCurrentProject(cms.readProject("Offline"));
        try {
            cms.createResource(resourcename, CmsResourceTypePlain.getStaticTypeId());
            fail("should not be able to create a file in a locked folder.");
        } catch (CmsLockException e) {
        }
        cms = getCmsObject();
        cms.createResource(resourcename, CmsResourceTypePlain.getStaticTypeId());
    }

    /**
     * Test the create resource method.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testCreateResourceWithSpecialChars() throws Throwable {
        CmsObject cms = getCmsObject();
        echo("Testing create resource with special character \"$\"");
        String resourcename = "/folder1/test$2.html";
        long timestamp = System.currentTimeMillis() - 1;
        String contentStr = "Hello this is my content";
        byte[] content = contentStr.getBytes();
        cms.createResource(resourcename, CmsResourceTypePlain.getStaticTypeId(), content, null);
        assertResourceType(cms, resourcename, CmsResourceTypePlain.getStaticTypeId());
        assertProject(cms, resourcename, cms.getRequestContext().currentProject());
        assertState(cms, resourcename, CmsResource.STATE_NEW);
        assertDateLastModifiedAfter(cms, resourcename, timestamp);
        assertDateCreatedAfter(cms, resourcename, timestamp);
        assertUserLastModified(cms, resourcename, cms.getRequestContext().currentUser());
        assertContent(cms, resourcename, content);
        cms.unlockProject(cms.getRequestContext().currentProject().getUuid());
        OpenCms.getPublishManager().publishProject(cms);
        OpenCms.getPublishManager().waitWhileRunning();
        assertState(cms, resourcename, CmsResource.STATE_UNCHANGED);
    }

    /**
     * Test the import resource method with a folder.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testImportFolder() throws Throwable {
        CmsObject cms = getCmsObject();
        echo("Testing import resource for a folder");
        String resourcename = "/folder1/test1/";
        long timestamp = System.currentTimeMillis() - 87654321;
        CmsResource resource = new CmsResource(CmsUUID.getNullUUID(), CmsUUID.getNullUUID(), resourcename, CmsResourceTypeFolder.getStaticTypeId(), true, 0, cms.getRequestContext().currentProject().getUuid(), CmsResource.STATE_NEW, timestamp, cms.getRequestContext().currentUser().getId(), timestamp, cms.getRequestContext().currentUser().getId(), CmsResource.DATE_RELEASED_DEFAULT, CmsResource.DATE_EXPIRED_DEFAULT, 1, -1, 0, 0);
        cms.importResource(resourcename, resource, null, null);
        assertIsFolder(cms, resourcename);
        assertProject(cms, resourcename, cms.getRequestContext().currentProject());
        assertState(cms, resourcename, CmsResource.STATE_NEW);
        assertDateLastModified(cms, resourcename, timestamp);
        assertDateCreated(cms, resourcename, timestamp);
        assertUserLastModified(cms, resourcename, cms.getRequestContext().currentUser());
        cms.unlockProject(cms.getRequestContext().currentProject().getUuid());
        OpenCms.getPublishManager().publishProject(cms);
        OpenCms.getPublishManager().waitWhileRunning();
        assertState(cms, resourcename, CmsResource.STATE_UNCHANGED);
    }

    /**
     * Test the import resource method for an existing folder.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testImportFolderAgain() throws Throwable {
        CmsObject cms = getCmsObject();
        echo("Testing to import an existing folder again");
        String resourcename = "/folder1/test1/";
        storeResources(cms, resourcename);
        long timestamp = System.currentTimeMillis() - 12345678;
        CmsFolder folder = cms.readFolder(resourcename);
        CmsResource resource = new CmsResource(folder.getStructureId(), new CmsUUID(), resourcename, CmsResourceTypeFolder.getStaticTypeId(), true, 0, cms.getRequestContext().currentProject().getUuid(), CmsResource.STATE_NEW, timestamp, cms.getRequestContext().currentUser().getId(), timestamp, cms.getRequestContext().currentUser().getId(), CmsResource.DATE_RELEASED_DEFAULT, CmsResource.DATE_EXPIRED_DEFAULT, 1, -1, 0, 0);
        cms.importResource(resourcename, resource, null, null);
        assertIsFolder(cms, resourcename);
        assertProject(cms, resourcename, cms.getRequestContext().currentProject());
        assertState(cms, resourcename, CmsResource.STATE_CHANGED);
        assertDateLastModified(cms, resourcename, timestamp);
        assertUserLastModified(cms, resourcename, cms.getRequestContext().currentUser());
        assertFilter(cms, resourcename, OpenCmsTestResourceFilter.FILTER_CREATE_RESOURCE);
        cms.unlockProject(cms.getRequestContext().currentProject().getUuid());
        OpenCms.getPublishManager().publishProject(cms);
        OpenCms.getPublishManager().waitWhileRunning();
        assertState(cms, resourcename, CmsResource.STATE_UNCHANGED);
    }

    /**
     * Test the import resource method.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testImportResource() throws Throwable {
        CmsObject cms = getCmsObject();
        echo("Testing import resource");
        String resourcename = "/folder1/test1.html";
        String contentStr = "Hello this is my content";
        byte[] content = contentStr.getBytes();
        long timestamp = System.currentTimeMillis() - 87654321;
        CmsResource resource = new CmsResource(CmsUUID.getNullUUID(), CmsUUID.getNullUUID(), resourcename, CmsResourceTypePlain.getStaticTypeId(), false, 0, cms.getRequestContext().currentProject().getUuid(), CmsResource.STATE_NEW, timestamp, cms.getRequestContext().currentUser().getId(), timestamp, cms.getRequestContext().currentUser().getId(), CmsResource.DATE_RELEASED_DEFAULT, CmsResource.DATE_EXPIRED_DEFAULT, 1, content.length, 0, 0);
        cms.importResource(resourcename, resource, content, null);
        assertResourceType(cms, resourcename, CmsResourceTypePlain.getStaticTypeId());
        assertProject(cms, resourcename, cms.getRequestContext().currentProject());
        assertState(cms, resourcename, CmsResource.STATE_NEW);
        assertDateLastModified(cms, resourcename, timestamp);
        assertDateCreated(cms, resourcename, timestamp);
        assertUserLastModified(cms, resourcename, cms.getRequestContext().currentUser());
        assertContent(cms, resourcename, content);
        cms.unlockProject(cms.getRequestContext().currentProject().getUuid());
        OpenCms.getPublishManager().publishProject(cms);
        OpenCms.getPublishManager().waitWhileRunning();
        assertState(cms, resourcename, CmsResource.STATE_UNCHANGED);
    }

    /**
     * Test the import resource method.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testImportResourceAgain() throws Throwable {
        CmsObject cms = getCmsObject();
        echo("Testing to import an existing resource again");
        String resourcename = "/folder1/test1.html";
        storeResources(cms, resourcename);
        long timestamp = System.currentTimeMillis() - 12345678;
        CmsResource res = cms.readResource(resourcename);
        String contentStr = "Hello this is my NEW AND CHANGED content";
        byte[] content = contentStr.getBytes();
        CmsResource resource = new CmsResource(res.getStructureId(), res.getResourceId(), resourcename, CmsResourceTypePlain.getStaticTypeId(), false, 0, cms.getRequestContext().currentProject().getUuid(), CmsResource.STATE_NEW, timestamp, cms.getRequestContext().currentUser().getId(), timestamp, cms.getRequestContext().currentUser().getId(), CmsResource.DATE_RELEASED_DEFAULT, CmsResource.DATE_EXPIRED_DEFAULT, 1, content.length, 0, 0);
        cms.importResource(resourcename, resource, content, null);
        assertResourceType(cms, resourcename, CmsResourceTypePlain.getStaticTypeId());
        assertProject(cms, resourcename, cms.getRequestContext().currentProject());
        assertState(cms, resourcename, CmsResource.STATE_CHANGED);
        assertDateLastModified(cms, resourcename, timestamp);
        assertUserLastModified(cms, resourcename, cms.getRequestContext().currentUser());
        assertFilter(cms, resourcename, OpenCmsTestResourceFilter.FILTER_CREATE_RESOURCE);
        cms.unlockProject(cms.getRequestContext().currentProject().getUuid());
        OpenCms.getPublishManager().publishProject(cms);
        OpenCms.getPublishManager().waitWhileRunning();
        assertState(cms, resourcename, CmsResource.STATE_UNCHANGED);
    }

    /**
     * Test the import of a sibling.<p>
     * 
     * @throws Throwable if something goes wrong
     */
    public void testImportSibling() throws Throwable {
        CmsObject cms = getCmsObject();
        echo("Testing to import an existing resource as sibling");
        CmsProperty prop1 = new CmsProperty(CmsPropertyDefinition.PROPERTY_TITLE, "The title", null);
        CmsProperty prop2 = new CmsProperty(CmsPropertyDefinition.PROPERTY_DESCRIPTION, "The description", null);
        CmsProperty prop3 = new CmsProperty(CmsPropertyDefinition.PROPERTY_KEYWORDS, "The keywords", null);
        List properties = new ArrayList();
        properties.add(prop1);
        String siblingname = "/folder1/test1.html";
        cms.lockResource(siblingname);
        cms.writePropertyObjects(siblingname, properties);
        cms.unlockResource(siblingname);
        long timestamp = System.currentTimeMillis() - 12345678;
        String resourcename1 = "/folder2/test1_sib1.html";
        String resourcename2 = "/folder1/subfolder11/test1_sib2.html";
        CmsFile file = cms.readFile(siblingname);
        byte[] content = file.getContents();
        assertTrue(file.getLength() > 0);
        assertTrue(content.length > 0);
        System.err.println(OpenCms.getSiteManager().getCurrentSite(cms).getSiteRoot());
        storeResources(cms, siblingname);
        System.err.println(OpenCms.getSiteManager().getCurrentSite(cms).getSiteRoot());
        CmsResource resource;
        resource = new CmsResource(CmsUUID.getNullUUID(), file.getResourceId(), resourcename2, CmsResourceTypePlain.getStaticTypeId(), false, 0, cms.getRequestContext().currentProject().getUuid(), CmsResource.STATE_NEW, timestamp, cms.getRequestContext().currentUser().getId(), timestamp, cms.getRequestContext().currentUser().getId(), CmsResource.DATE_RELEASED_DEFAULT, CmsResource.DATE_EXPIRED_DEFAULT, 1, content.length, 0, 0);
        properties.add(prop2);
        cms.importResource(resourcename2, resource, null, properties);
        System.err.println(OpenCms.getSiteManager().getCurrentSite(cms).getSiteRoot());
        assertProject(cms, resourcename2, cms.getRequestContext().currentProject());
        assertResourceType(cms, resourcename2, CmsResourceTypePlain.getStaticTypeId());
        assertResourceType(cms, siblingname, CmsResourceTypePlain.getStaticTypeId());
        assertState(cms, resourcename2, CmsResource.STATE_NEW);
        assertState(cms, siblingname, CmsResource.STATE_CHANGED);
        assertDateLastModified(cms, resourcename2, file.getDateLastModified());
        assertDateLastModified(cms, siblingname, file.getDateLastModified());
        assertUserLastModified(cms, resourcename2, cms.getRequestContext().currentUser());
        assertUserLastModified(cms, siblingname, cms.getRequestContext().currentUser());
        assertContent(cms, resourcename2, content);
        assertContent(cms, siblingname, content);
        assertSiblingCountIncremented(cms, siblingname, 1);
        OpenCmsTestResourceConfigurableFilter filter = new OpenCmsTestResourceConfigurableFilter(OpenCmsTestResourceFilter.FILTER_CREATE_RESOURCE);
        filter.disableSiblingCountTest();
        assertFilter(cms, siblingname, filter);
        String contentStr = "Hello this is my NEW AND CHANGED sibling content";
        content = contentStr.getBytes();
        resource = new CmsResource(new CmsUUID(), file.getResourceId(), resourcename1, CmsResourceTypePlain.getStaticTypeId(), false, 0, cms.getRequestContext().currentProject().getUuid(), CmsResource.STATE_NEW, timestamp, cms.getRequestContext().currentUser().getId(), timestamp, cms.getRequestContext().currentUser().getId(), CmsResource.DATE_RELEASED_DEFAULT, CmsResource.DATE_EXPIRED_DEFAULT, 1, content.length, timestamp, 0);
        properties.add(prop3);
        cms.importResource(resourcename1, resource, content, properties);
        assertProject(cms, resourcename1, cms.getRequestContext().currentProject());
        assertProject(cms, resourcename2, cms.getRequestContext().currentProject());
        assertResourceType(cms, resourcename1, CmsResourceTypePlain.getStaticTypeId());
        assertResourceType(cms, resourcename2, CmsResourceTypePlain.getStaticTypeId());
        assertResourceType(cms, siblingname, CmsResourceTypePlain.getStaticTypeId());
        assertState(cms, resourcename1, CmsResource.STATE_NEW);
        assertState(cms, resourcename2, CmsResource.STATE_NEW);
        assertState(cms, siblingname, CmsResource.STATE_CHANGED);
        assertDateLastModified(cms, resourcename1, timestamp);
        assertDateLastModified(cms, resourcename2, timestamp);
        assertDateLastModified(cms, siblingname, timestamp);
        assertUserLastModified(cms, resourcename1, cms.getRequestContext().currentUser());
        assertUserLastModified(cms, resourcename2, cms.getRequestContext().currentUser());
        assertUserLastModified(cms, siblingname, cms.getRequestContext().currentUser());
        assertContent(cms, resourcename1, content);
        assertContent(cms, resourcename2, content);
        assertContent(cms, siblingname, content);
        assertSiblingCountIncremented(cms, siblingname, 2);
        assertFilter(cms, siblingname, filter);
        OpenCms.getPublishManager().publishProject(cms);
        OpenCms.getPublishManager().waitWhileRunning();
        assertState(cms, resourcename1, CmsResource.STATE_UNCHANGED);
        assertState(cms, resourcename2, CmsResource.STATE_UNCHANGED);
    }

    /**
     * Tests to overwrite invisible resource.<p>
     * 
     * @throws Exception if the test fails
     */
    public void testOverwriteInvisibleResource() throws Exception {
        CmsObject cms = getCmsObject();
        echo("Testing to overwrite invisible resource");
        String source = "index.html";
        String target = "/test_index.html";
        cms.createResource(target, CmsResourceTypePlain.getStaticTypeId());
        cms.chacc(target, I_CmsPrincipal.PRINCIPAL_USER, "test2", "-r+v+i");
        cms.unlockResource(target);
        storeResources(cms, source);
        storeResources(cms, target);
        cms.loginUser("test2", "test2");
        cms.getRequestContext().setCurrentProject(cms.readProject("Offline"));
        try {
            cms.readResource(target, CmsResourceFilter.ALL);
            fail("should fail to read the resource without permissions");
        } catch (CmsPermissionViolationException e) {
        }
        try {
            cms.copyResource(source, target);
            fail("should fail to overwrite a resource without a lock on the target");
        } catch (CmsLockException e) {
        }
        try {
            cms.lockResource(target);
            fail("should fail to overwrite the resource without read permissions");
        } catch (CmsPermissionViolationException e) {
        }
        try {
            cms.createResource(target, CmsResourceTypeXmlPage.getStaticTypeId());
            fail("should fail to create a resource that already exists");
        } catch (CmsLockException e) {
        }
        cms.loginUser("Admin", "admin");
        cms.getRequestContext().setCurrentProject(cms.readProject("Offline"));
        assertFilter(cms, source, OpenCmsTestResourceFilter.FILTER_EQUAL);
        assertFilter(cms, target, OpenCmsTestResourceFilter.FILTER_EQUAL);
    }
}
