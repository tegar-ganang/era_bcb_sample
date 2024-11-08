package td2jira.sync;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;
import org.apache.log4j.Logger;
import td2jira.Config;
import td2jira.jira.IJIRAConnector;
import td2jira.jira.JIRAComment;
import td2jira.jira.JIRAIssue;
import td2jira.jira.http.AddAttachmentToIssue;
import td2jira.td.ITDConnector;
import td2jira.td.api.Comment;
import td2jira.td.api.IBug;
import td2jira.td.api.Utils;

public class SyncUtils {

    private static Logger logger = Logger.getLogger(SyncUtils.class);

    /**
   * only update TD is this is false
   */
    private static Boolean readOnlyTD = null;

    public static void sync(ITDConnector tc, IJIRAConnector jc, IssuePair pair) throws Exception {
        initTDReadOnly();
        List<Comment> tdComments = tc.getComments(pair.getTdIssue());
        List<JIRAComment> jiraComments = jc.getComments(pair.getJiraIssue());
        syncCommentsToJIRA(jc, pair, tdComments, jiraComments);
        syncCommentsToTD(tc, pair, jiraComments, tdComments);
        syncPriorityToJIRA(jc, pair);
        syncAssigneeToTD(tc, pair);
    }

    /**
   * Only enable writing to TD if user really marked Config.TD_READ_ONLY as 'false'
   */
    private static void initTDReadOnly() {
        Boolean oldReadOnlyTD = readOnlyTD;
        readOnlyTD = !Config.TD_READ_ONLY.equals("false");
        if (oldReadOnlyTD != readOnlyTD) {
            logger.info("TD is now " + (readOnlyTD ? "read-only" : "read-write"));
        }
    }

    private static void syncPriorityToJIRA(IJIRAConnector jc, IssuePair pair) throws Exception {
        JIRAIssue jiraIssue = pair.getJiraIssue();
        String tdPriority = pair.getTdIssue().getPriority();
        String jiraPriority = Config.getJiraPrioriryForTDPriority(tdPriority);
        if (jiraPriority == null) {
            logger.warn("Coudn't map TD priority " + tdPriority + " for issues " + pair);
            return;
        }
        String oldJiraPriority = pair.getJiraIssue().getPriority();
        if (jiraPriority.equals(oldJiraPriority)) {
            return;
        }
        logger.info("Changing JIRA priority for issue " + jiraIssue.getKey() + " from " + oldJiraPriority + " to " + jiraPriority);
        jiraIssue.setPriority(jiraPriority);
        jc.updatePriority(jiraIssue);
    }

    public static void syncAttachmentsToJIRA(ITDConnector tc, IJIRAConnector jc, IssuePair pair) {
        if (!tc.hasAttachments(pair.getTdIssue())) return;
        List<JIRAComment> comments = jc.getComments(pair.getJiraIssue());
        List<String> tdAttachments = tc.getAttachmentsNames(pair.getTdIssue());
        for (String attachmentName : tdAttachments) {
            if (hasAttachmentInJira(comments, attachmentName)) continue;
            try {
                addAttachment(tc, jc, pair, pair.getJiraIssue(), attachmentName);
            } catch (com.jacob.com.ComFailException e) {
                if (e.getMessage().contains("The Factory failed to create attachment storage object.")) {
                    logger.warn("Ignoring JACOB issue for attachment " + pair + " . To investigate...", e);
                } else {
                    throw e;
                }
            }
        }
    }

    private static boolean hasAttachmentInJira(List<JIRAComment> comments, String attachmentName) {
        for (JIRAComment comment : comments) {
            String body = comment.getBody();
            if (body.indexOf(Config.SYNC_ATTACH_COMMENT_PREFIX) < 0) continue;
            if (body.indexOf(attachmentName) >= 0) return true;
        }
        return false;
    }

    private static void addAttachment(ITDConnector tc, IJIRAConnector jc, IssuePair pair, JIRAIssue jiraIssue, String attachmentName) {
        byte[] data = tc.getAttachmentData(pair.getTdIssue(), attachmentName);
        AddAttachmentToIssue.addAttachmentToIssue(pair.getJiraIssue(), attachmentName, data);
    }

    private static void syncAssigneeToTD(ITDConnector tc, IssuePair pair) {
        if (readOnlyTD) return;
        IBug tdIssue = pair.getTdIssue();
        if (!tdIssue.getStatus().equals("Assigned")) return;
        JIRAIssue jiraIssue = pair.getJiraIssue();
        if (jiraIssue.getStatus() == null) return;
        if (jiraIssue.getStatus().startsWith("Assigned") || jiraIssue.getStatus().equals("Open")) {
            Set<String> tdDevelopers = Config.getDevelopersNamesInTD();
            if (tdDevelopers.contains(tdIssue.getAssignedTo())) {
                String assignedInJIRA = jiraIssue.getAssignee();
                String matchInTd = Config.getTDDeveloperForJIRADeveloper(assignedInJIRA);
                if (matchInTd == null) {
                    logger.warn("no match for JIRA user " + assignedInJIRA + " found in TD");
                    return;
                }
                tc.assignTo(tdIssue, matchInTd);
            }
        }
    }

    private static void syncCommentsToTD(ITDConnector tc, IssuePair pair, List<JIRAComment> jiraComments, List<Comment> tdComments) throws Exception {
        if (readOnlyTD) return;
        IBug tdIssue = pair.getTdIssue();
        for (JIRAComment jiraComment : jiraComments) {
            if (jiraComment.getAuthor().indexOf("@TD") >= 0) continue;
            if (jiraComment.getBody().indexOf("@TD") >= 0) continue;
            String body = jiraComment.getBody().trim();
            String toQA = "^2QA\\:([a-z0-9]+){0,1}";
            Pattern pattern = Pattern.compile(toQA, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(body);
            if (matcher.find()) {
                int end = matcher.end();
                body = body.substring(end).trim();
                jiraComment.setBody(body);
                if (body.length() > 0) {
                    Comment tdComment = findCommentInTD(tc, tdIssue, jiraComment, tdComments);
                    if (tdComment == null) {
                        tdComment = Utils.fromJIRAComment(jiraComment);
                        tc.addComment(tdIssue, tdComment);
                        String qaUserName = matcher.group(1);
                        if (qaUserName != null) {
                            tc.assignTo(tdIssue, qaUserName.trim());
                        }
                    }
                }
            }
        }
    }

    private static void syncCommentsToJIRA(IJIRAConnector jc, IssuePair pair, List<Comment> tdComments, List<JIRAComment> jiraComments) throws Exception {
        JIRAIssue jiraIssue = pair.getJiraIssue();
        for (Comment tdComment : tdComments) {
            if (tdComment.getAuthor().indexOf("@JIRA") >= 0) continue;
            if (tdComment.getBody().indexOf("@JIRA") >= 0) continue;
            JIRAComment jiraComment = findCommentInJIRA(jc, jiraIssue, tdComment, jiraComments);
            if (jiraComment == null) {
                jiraComment = JIRAComment.fromTDComment(tdComment);
                jc.addComment(jiraIssue, JIRAComment.format(tdComment));
            }
        }
    }

    private static Comment findCommentInTD(ITDConnector tc, IBug tdIssue, JIRAComment jiraComment, List<Comment> tdComments) {
        for (Comment comment : tdComments) {
            if (comment.getAuthor().indexOf("@JIRA" + jiraComment.getId()) >= 0) return comment;
            if (comment.getBody().indexOf("@JIRA" + jiraComment.getId()) >= 0) return comment;
        }
        return null;
    }

    private static JIRAComment findCommentInJIRA(IJIRAConnector jc, JIRAIssue jiraIssue, Comment tdComment, List<JIRAComment> jiraComments) {
        String wouldBeJIRABody = JIRAComment.format(tdComment).trim();
        wouldBeJIRABody = streamLine(wouldBeJIRABody);
        for (JIRAComment comment : jiraComments) {
            String jiraBody = comment.getBody().trim();
            jiraBody = streamLine(jiraBody);
            if (wouldBeJIRABody.equals(jiraBody.trim())) {
                return comment;
            }
        }
        return null;
    }

    private static String streamLine(String s) {
        s = s.replaceAll("&amp;", "&");
        s = s.replaceAll("&lt;", "<");
        s = s.replaceAll("&gt;", ">");
        s = s.replaceAll("&quot;", "'");
        s = s.replaceAll("\\n\\r\\s", " ");
        s = s.replaceAll("&", "");
        return s.trim();
    }

    public static JIRAIssue createJiraIssue(IJIRAConnector jiraConnector, ITDConnector tdConnector, IBug tdIssue) throws Exception {
        String detectedBy = tdIssue.getDetectedBy();
        if (detectedBy == null || detectedBy.length() == 0) detectedBy = "";
        String prefix = Config.JIRA_SUMMARY_PREFIX;
        String jiraProject = Config.JIRA_PROJECT;
        JIRAIssue jt = new JIRAIssue();
        jt.setDescription(Utils.normalize(detectedBy + "\n" + tdIssue.getDescription()));
        jt.setSummary(prefix + " " + tdIssue.getId() + ": " + tdIssue.getSummary());
        jt.setProjectId(jiraProject);
        jt.setAssignee(Config.getJIRADeveloperForTDDeveloper(tdIssue.getAssignedTo()));
        String at = Config.JIRA_ASSIGN_NEW_ISSUE_TO;
        if (at != null && jt.getAssignee() == null) jt.setAssignee(at);
        jt.setPriority(Config.getJiraPrioriryForTDPriority(tdIssue.getPriority()));
        jiraConnector.createJIRATask(jt);
        IssuePair pair = new IssuePair();
        pair.setJiraIssue(jt);
        pair.setTdIssue(tdIssue);
        sync(tdConnector, jiraConnector, pair);
        if (readOnlyTD) return jt;
        Comment tdc = new Comment();
        tdc.setAuthor(Config.TD_ROBOT_NAME);
        tdc.setCreated(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
        tdc.setBody("JIRA task is created: " + Config.JIRA_URL + "/browse/" + jt.getKey());
        tdConnector.addComment(tdIssue, tdc);
        return jt;
    }
}
