package vqwiki.actions;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import vqwiki.Environment;
import vqwiki.TopicVersion;
import vqwiki.WikiBase;
import vqwiki.WikiMember;
import vqwiki.persistence.AbstractPersistenceFactory;
import vqwiki.persistence.PersistenceHandler;
import vqwiki.persistence.db.DatabaseNotify;
import vqwiki.persistence.db.DatabasePersistenceFactory;
import vqwiki.persistence.db.DatabaseWikiMembers;
import vqwiki.persistence.file.FileNotify;
import vqwiki.persistence.file.FileWikiMembers;
import vqwiki.search.SearchEngine;
import vqwiki.svc.VersionManager;
import vqwiki.svc.WikiMembers;

/**
 * Servlet for migrating a file-based wiki into a database-based one
 *
 * @author garethc
 * @since Apr 28, 2003
 */
public class ImportAction extends AbstractAdminWikiAction {

    private static final long serialVersionUID = 2523802067144622481L;

    /**
     * {@inheritDoc}
     */
    public void doAsAdmin(HttpServletRequest request, HttpServletResponse response) throws Exception {
        logger.fine("Importing...");
        request.setAttribute("results", importAll());
        dispatch("/jsp/after-import.jsp", request, response);
    }

    /**
     *
     */
    private String importAll() throws Exception {
        final AbstractPersistenceFactory targetPersistenceFactory = new DatabasePersistenceFactory(Environment.getInstance());
        final PersistenceHandler targetPersistenceHandler = targetPersistenceFactory.createPersistenceHandler();
        final VersionManager targetVersionManager = targetPersistenceFactory.createVersionManager();
        final PersistenceHandler sourcePersistenceHandler = WikiBase.getInstance().getPersistenceHandler();
        final VersionManager sourceVersionManager = WikiBase.getInstance().getVersionManager();
        final SearchEngine sourceSearchEngine = WikiBase.getInstance().getSearchEngine();
        StringBuffer buffer = new StringBuffer();
        Collection virtualWikis = sourcePersistenceHandler.getVirtualWikis();
        for (Iterator virtualWikiIterator = virtualWikis.iterator(); virtualWikiIterator.hasNext(); ) {
            String virtualWiki = (String) virtualWikiIterator.next();
            logger.info("importing for virtual wiki " + virtualWiki);
            buffer.append("imported for virtual wiki " + virtualWiki);
            buffer.append("<br/>");
            targetPersistenceHandler.addVirtualWiki(virtualWiki);
            Collection topics = sourceSearchEngine.getAllTopicNames(virtualWiki);
            for (Iterator topicIterator = topics.iterator(); topicIterator.hasNext(); ) {
                String topicName = (String) topicIterator.next();
                List versions = sourceVersionManager.getAllVersions(virtualWiki, topicName);
                logger.info("importing " + versions.size() + " versions of topic " + topicName);
                buffer.append("imported " + versions.size() + " versions of topic " + topicName);
                buffer.append("<br/>");
                for (Iterator topicVersionIterator = versions.iterator(); topicVersionIterator.hasNext(); ) {
                    TopicVersion topicVersion = (TopicVersion) topicVersionIterator.next();
                    targetVersionManager.addVersion(virtualWiki, topicVersion.getTopicName(), topicVersion.getRawContents(), topicVersion.getRevisionDate());
                }
            }
            for (Iterator topicIterator = topics.iterator(); topicIterator.hasNext(); ) {
                String topicName = (String) topicIterator.next();
                logger.info("importing topic " + topicName);
                buffer.append("imported topic " + topicName);
                buffer.append("<br/>");
                targetPersistenceHandler.write(virtualWiki, sourcePersistenceHandler.read(virtualWiki, topicName), false, topicName);
            }
            Collection readOnlys = sourcePersistenceHandler.getReadOnlyTopics(virtualWiki);
            for (Iterator readOnlyIterator = readOnlys.iterator(); readOnlyIterator.hasNext(); ) {
                String topicName = (String) readOnlyIterator.next();
                logger.info("import read-only topicname " + topicName);
                buffer.append("imported read-only topicname " + topicName);
                buffer.append("<br/>");
                targetPersistenceHandler.addReadOnlyTopic(virtualWiki, topicName);
            }
            WikiMembers fileMembers = new FileWikiMembers(getEnvironment(), virtualWiki);
            WikiMembers databaseMembers = new DatabaseWikiMembers(getEnvironment(), virtualWiki);
            Collection members = fileMembers.getAllMembers();
            for (Iterator memberIterator = members.iterator(); memberIterator.hasNext(); ) {
                WikiMember wikiMember = (WikiMember) memberIterator.next();
                logger.info("importing member " + wikiMember);
                buffer.append("imported member " + wikiMember);
                buffer.append("<br/>");
                databaseMembers.addMember(wikiMember.getUserName(), wikiMember.getEmail(), wikiMember.getKey());
            }
            Collection fileNotifications = FileNotify.getAll(virtualWiki);
            for (Iterator iterator = fileNotifications.iterator(); iterator.hasNext(); ) {
                FileNotify fileNotify = (FileNotify) iterator.next();
                logger.info("importing notification " + fileNotify);
                buffer.append("imported notification " + fileNotify);
                buffer.append("<br/>");
                DatabaseNotify databaseNotify = new DatabaseNotify(virtualWiki, fileNotify.getTopicName());
                Collection notifyMembers = fileNotify.getMembers();
                for (Iterator notifyMemberIterator = notifyMembers.iterator(); notifyMemberIterator.hasNext(); ) {
                    String memberName = (String) notifyMemberIterator.next();
                    databaseNotify.addMember(memberName);
                }
            }
            Collection templates = sourcePersistenceHandler.getTemplateNames(virtualWiki);
            for (Iterator templateIterator = templates.iterator(); templateIterator.hasNext(); ) {
                String templateName = (String) templateIterator.next();
                logger.info("importing template " + templateName);
                buffer.append("imported template " + templateName);
                buffer.append("<br/>");
                targetPersistenceHandler.saveAsTemplate(virtualWiki, templateName, sourcePersistenceHandler.getTemplate(virtualWiki, templateName));
            }
        }
        return buffer.toString();
    }
}
