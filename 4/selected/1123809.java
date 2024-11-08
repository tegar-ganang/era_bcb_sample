package photospace.web.spring;

import java.io.*;
import java.util.*;
import org.apache.commons.io.*;
import org.springframework.context.support.*;
import org.springframework.validation.*;
import org.springframework.web.bind.*;
import com.mockobjects.servlet.*;
import junit.framework.*;
import photospace.meta.*;
import photospace.search.*;
import photospace.vfs.FileSystem;
import photospace.vfs.*;

public class EditControllerTest extends TestCase {

    EditController controller;

    PersisterImpl persister;

    String path1;

    String path2;

    public void setUp() throws Exception {
        File root = new File(System.getProperty("java.io.tmpdir"), "EditControllerTest");
        File subdir = new File(root, "subdir");
        subdir.mkdirs();
        File photo = new File(System.getProperty("project.root"), "build/test/exif-nordf.jpg");
        FileUtils.copyFileToDirectory(photo, root);
        FileUtils.copyFileToDirectory(photo, subdir);
        path1 = "/exif-nordf.jpg";
        path2 = "/subdir/exif-nordf.jpg";
        FileSystem filesystem = new FileSystemImpl();
        filesystem.setRoot(root);
        persister = new PersisterImpl();
        persister.setFilesystem(filesystem);
        persister.setTranslator(new Translator());
        FileSystemBrowser browser = new FileSystemBrowser();
        browser.setFilesystem(filesystem);
        browser.setPersister(persister);
        File index = new File(System.getProperty("java.io.tmpdir"), "test-index");
        SearchIndex searchIndex = new SearchIndex(index);
        Searcher searcher = new Searcher();
        searcher.setIndex(searchIndex);
        controller = new EditController();
        controller.setFilesystem(filesystem);
        controller.setPersister(persister);
        controller.setSearcher(searcher);
        StaticApplicationContext context = new StaticApplicationContext();
        context.registerSingleton(StaticApplicationContext.MESSAGE_SOURCE_BEAN_NAME, StaticMessageSource.class, null);
        context.refresh();
        context.addMessage("format.dateTime", Locale.US, "MM/dd/yyyy hh:mm aaa");
        context.addMessage("message.edit.success", Locale.US, "success");
        controller.setApplicationContext(context);
    }

    public void testEdit() throws Exception {
        MockHttpServletRequest viewRequest = new MockHttpServletRequest();
        viewRequest.setSession(new MockHttpSession());
        viewRequest.setupAddParameter("paths", new String[] { path1, path2 });
        EditCommand command = (EditCommand) controller.formBackingObject(viewRequest);
        assertEquals(2, command.getMetas().size());
        ServletRequestDataBinder binder = new ServletRequestDataBinder(command, "");
        controller.initBinder(null, binder);
        MockHttpServletRequest editRequest = new MockHttpServletRequest();
        Vector names = new Vector(Arrays.asList(new String[] { "meta.tags" }));
        editRequest.setupGetParameterNames(names.elements());
        editRequest.setupAddParameter("meta.tags", "foo bar");
        binder.bind(editRequest);
        assertTrue(Arrays.asList(command.getMeta().getTags()).contains("foo"));
        assertTrue(Arrays.asList(command.getMeta().getTags()).contains("bar"));
        assertFalse(binder.getErrors().hasErrors());
        controller.onSubmit(editRequest, null, command, binder.getErrors());
        Meta meta1 = persister.getMeta(path1);
        Meta meta2 = persister.getMeta(path2);
        assertTrue(Arrays.asList(meta1.getTags()).contains("foo"));
        assertTrue(Arrays.asList(meta1.getTags()).contains("bar"));
        assertTrue(Arrays.asList(meta2.getTags()).contains("foo"));
        assertTrue(Arrays.asList(meta2.getTags()).contains("bar"));
    }

    public void testValidation() {
        Validator validator = new MetaValidator();
        EditCommand command = new EditCommand(new PhotoMeta());
        BindException errors = new BindException(command, "");
        assertTrue(validator.supports(command.getClass()));
        validator.validate(command, errors);
        assertFalse(errors.hasErrors());
        command.getMeta().getPosition().setLatitude(new Double("45"));
        command.getMeta().getPosition().setLongitude(new Double("90"));
        validator.validate(command, errors);
        assertFalse(errors.hasErrors());
        command.getMeta().getPosition().setLatitude(new Double("91"));
        command.getMeta().getPosition().setLongitude(new Double("181"));
        validator.validate(command, errors);
        assertTrue(errors.hasErrors());
    }

    public void testRotatePreservesMetadata() throws Exception {
        PhotoMeta before = (PhotoMeta) persister.getMeta(path1);
        assertNotNull(before.getDevice());
        EditCommand command = new EditCommand(new Meta());
        command.setPaths(new String[] { path1 });
        command.getMetas().add(persister.getMeta(path1));
        command.setRotate(EditController.ROTATE_RIGHT);
        controller.onSubmit(new MockHttpServletRequest(), null, command, new BindException(command, ""));
        PhotoMeta after = (PhotoMeta) persister.getMeta(path1);
        assertNotNull(after.getDevice());
    }
}
