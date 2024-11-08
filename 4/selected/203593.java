package org.gnomekr.potron.service;

import static org.gnomekr.potron.util.PotronConstants.BEAN_ID_PROJECT_MANAGER;
import static org.gnomekr.potron.util.PotronConstants.BEAN_ID_TRANSLATION_MANAGER;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.gnomekr.potron.data.LanguageTeam;
import org.gnomekr.potron.data.Project;
import org.gnomekr.potron.data.TranslatedEntry;
import org.gnomekr.potron.data.Translation;

/**
 * TranslationManagerTest.java
 * @author iolo
 * @version $Revision 1.1 $ $Date: 2005/08/28 11:47:15 $
 */
public class TranslationManagerTest extends AbstractHibernateSpringContextTests {

    public void testTranslationPersistence() throws Exception {
        IProjectManager projectManager = (IProjectManager) applicationContext.getBean(BEAN_ID_PROJECT_MANAGER);
        assertNotNull(projectManager);
        ITranslationManager manager = (ITranslationManager) applicationContext.getBean(BEAN_ID_TRANSLATION_MANAGER);
        assertNotNull(manager);
        StringWriter writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        out.println("# SOME DESCRIPTIVE TITLE.");
        out.println("# Copyright (C) YEAR Free Software Foundation, Inc.");
        out.println("# FIRST AUTHOR <EMAIL@ADDRESS>, YEAR.");
        out.println("#");
        out.println("#, fuzzy");
        out.println("msgid \"\"");
        out.println("msgstr \"\"");
        out.println("\"POT-Creation-Date: 2001-02-09 01:25+0100\\n\"");
        out.println();
        out.println("#: gpl.xml:15");
        out.println("#, no-c-format");
        out.println("#, fuzzy");
        out.println("msgid \"Free Software Foundation, Inc.\"");
        out.println("msgstr \"test2\"");
        out.println();
        out.println("#: gpl.xml:16");
        out.println("#, no-c-format");
        out.println("#, fuzzy");
        out.println("msgid \"test3\"");
        out.println("msgstr \"test3\"");
        Project project = new Project("__test_project__");
        project.setRegisteredDate(new Date(System.currentTimeMillis()));
        project.setName("Test Project");
        project.setVersion("1.0");
        project.setHomepage("homepage");
        project.setDescription("description");
        projectManager.createProject(project);
        Reader reader = new StringReader(writer.toString());
        long templateId = projectManager.addTemplate(project.getId(), "test template", "description", reader);
        LanguageTeam team = new LanguageTeam();
        team.setRegisteredDate(new Date(System.currentTimeMillis()));
        team.setName("Test Team");
        team.setContact("contact");
        team.setPrefix("prefix");
        team.setEncoding("encoding");
        StringWriter writer2 = new StringWriter();
        PrintWriter out2 = new PrintWriter(writer2);
        out2.println("# SOME DESCRIPTIVE TITLE.");
        out2.println("# Copyright (C) YEAR Free Software Foundation, Inc.");
        out2.println("# FIRST AUTHOR <EMAIL@ADDRESS>, YEAR.");
        out2.println("#");
        out2.println("#, fuzzy");
        out2.println("msgid \"\"");
        out2.println("msgstr \"\"");
        out2.println("\"POT-Creation-Date: 2001-02-09 01:25+0100\\n\"");
        out2.println();
        out2.println("#: gpl.xml:15");
        out2.println("#, no-c-format");
        out2.println("#, fuzzy");
        out2.println("msgid \"Free Software Foundation, Inc.\"");
        out2.println("msgstr \"not yet translated\"");
        out2.println();
        out2.println("#: gpl.xml:16");
        out2.println("#, no-c-format");
        out2.println("msgid \"test3\"");
        out2.println("msgstr \"translated!\"");
        Reader reader2 = new StringReader(writer2.toString());
        Translation translation = new Translation();
        translation.setName("test");
        translation.setLanguageTeam(team);
        translation.setTemplate(projectManager.getTemplate(templateId));
        long id = manager.addTranslation(translation, reader2);
        flushCurrentSession();
        int count = jdbcTemplate.queryForInt("select count(*) from POTRON_TRANS where trans_id=?", new Long[] { id });
        assertEquals(1, count);
        String comment = (String) jdbcTemplate.queryForObject("select note from POTRON_TRANS where trans_id=?", new Long[] { id }, String.class);
        assertNotNull(comment);
        assertTrue(comment.startsWith("# SOME DESCRIPTIVE TITLE."));
        List headers = jdbcTemplate.queryForList("select * from POTRON_TRANS_HEADER where template_id=?", new Long[] { id });
        assertNotNull(headers);
        assertEquals(1, headers.size());
        Map values = (Map) headers.get(0);
        assertEquals("POT-Creation-Date", values.get("name"));
        assertEquals("2001-02-09 01:25+0100", values.get("value"));
        List entries = jdbcTemplate.queryForList("select * from POTRON_TRANS_ENTRY where trans_id=?", new Long[] { id });
        assertNotNull(entries);
        assertEquals(2, entries.size());
        values = (Map) entries.get(0);
        assertNull(values.get("checked_date"));
        assertNull(values.get("modified_date"));
        List<TranslatedEntry> list = manager.getFuzzyEntries(id, false);
        TranslatedEntry tran0 = list.get(0);
        assertEquals(tran0.getTranslatedString(), "not yet translated");
        assertTrue(tran0.isFuzzy());
        manager.exportTranslation(id, new PrintWriter(System.out));
        long entry_id = tran0.getId();
        TranslatedEntry tran0_co = manager.checkOutEntry(entry_id);
        assertNotNull(tran0_co);
        assertNotNull(tran0_co.getEntry());
        assertEquals(tran0_co.getEntry().getKey(), "Free Software Foundation, Inc.");
        TranslatedEntry tran0_co_fail = null;
        try {
            tran0_co_fail = manager.checkOutEntry(entry_id);
        } catch (Exception e) {
            assertTrue((e instanceof AlreadyCheckedOutException));
        }
        assertNull(tran0_co_fail);
        manager.checkInEntry(tran0_co_fail.getId(), "test", false);
    }
}
