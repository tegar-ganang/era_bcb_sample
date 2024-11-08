package grobid.utilities;

import grobid.data.BiblioItem;
import grobid.exceptions.GROBIDServiceException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Some utilities methods that I don't know where to put.
 *
 *  @author Patrice Lopez
 */
public class Utilities {

    /**
     * Deletes all files and subdirectories under dir. Returns true if all deletions were successful.
     * If a deletion fails, the method stops attempting to delete and returns false.
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    /**
     * Cleaninig of the body of text prior to term extraction.
     * Try to remove the pdf extraction garbage and the citation marks. 
     */
    public static String cleanBody(String text) {
        if (text == null) return null;
        String res = "";
        Pattern cleaner = Pattern.compile("[-]?[a-z][\\d]+[ ]*");
        Matcher m = cleaner.matcher(text);
        res = m.replaceAll("");
        Pattern cleaner2 = Pattern.compile("[\\w]*[@|#|=]+[\\w]+");
        Matcher m2 = cleaner2.matcher(res);
        res = m2.replaceAll("");
        res = res.replace("Introduction", "");
        return res;
    }

    public static String uploadFile(String urlmsg, String path, String name) {
        try {
            System.out.println("Sending: " + urlmsg);
            URL url = new URL(urlmsg);
            if (url == null) {
                System.out.println("Resource " + urlmsg + " not found");
                return null;
            }
            File outFile = new File(path, name);
            FileOutputStream out = new FileOutputStream(outFile);
            InputStream in = url.openStream();
            byte[] buf = new byte[4 * 1024];
            int bytesRead;
            while ((bytesRead = in.read(buf)) != -1) {
                out.write(buf, 0, bytesRead);
            }
            out.close();
            in.close();
            return path + name;
        } catch (Exception e) {
            throw new GROBIDServiceException("An exception occured while running Grobid.", e);
        }
    }

    public static String punctuationsSub = "([,;])";

    /**
     *  Special cleaning for ZFN extracted data in a BiblioItem 
     */
    public static BiblioItem cleanZFNMetadata(BiblioItem item) {
        if (item.getPublicationDate() != null) {
            String new_date = "";
            for (int i = 0; i < item.getPublicationDate().length(); i++) {
                char c = item.getPublicationDate().charAt(i);
                if (TextUtilities.fullPunctuations.indexOf(c) == -1) new_date += c;
            }
            item.setPublicationDate(new_date.trim());
        }
        String affiliation = item.getAffiliation();
        if (affiliation != null) {
            if (affiliation.startsWith("Aus dem")) affiliation = affiliation.replace("Aus dem", "");
            if (affiliation.startsWith("Aus der")) affiliation = affiliation.replace("Aus der", "");
            affiliation = affiliation.trim();
            item.setAffiliation(affiliation);
        }
        String journal = item.getJournal();
        if (journal != null) {
            String new_journal = "";
            for (int i = 0; i < journal.length(); i++) {
                char c = journal.charAt(i);
                if (punctuationsSub.indexOf(c) == -1) new_journal += c;
            }
            journal = new_journal.trim();
            journal = journal.replace(" .", ".");
            if ((journal.startsWith(",")) | (journal.startsWith("."))) {
                journal = journal.substring(1, journal.length()).trim();
            }
            item.setJournal(journal);
        }
        String pageRange = item.getPageRange();
        if (pageRange != null) {
            String new_pageRange = "";
            for (int i = 0; i < pageRange.length(); i++) {
                char c = pageRange.charAt(i);
                if (punctuationsSub.indexOf(c) == -1) new_pageRange += c;
            }
            pageRange = new_pageRange.trim();
            item.setPageRange(pageRange);
        }
        String note = item.getNote();
        if (note != null) {
            String new_note = "";
            for (int i = 0; i < note.length(); i++) {
                char c = note.charAt(i);
                if (punctuationsSub.indexOf(c) == -1) new_note += c;
            }
            note = new_note.trim();
            note = note.replace(" .", ".");
            note = note.replace("...", ".");
            note = note.replace("..", ".");
            if ((note.startsWith(",")) | (note.startsWith("."))) {
                note = note.substring(1, note.length()).trim();
            }
            note = note.replace("@BULLET", " • ");
            item.setNote(note);
        }
        String submission = item.getSubmission();
        if (submission != null) {
            String new_submission = "";
            for (int i = 0; i < submission.length(); i++) {
                char c = submission.charAt(i);
                if (punctuationsSub.indexOf(c) == -1) new_submission += c;
            }
            submission = new_submission.trim();
            submission = submission.replace(" .", ".");
            submission = submission.replace("...", ".");
            submission = submission.replace("..", ".");
            if ((submission.startsWith(",")) | (submission.startsWith("."))) {
                submission = submission.substring(1, submission.length()).trim();
            }
            submission = submission.replace("@BULLET", " • ");
            item.setSubmission(submission);
        }
        String dedication = item.getDedication();
        if (dedication != null) {
            String new_dedication = "";
            for (int i = 0; i < dedication.length(); i++) {
                char c = dedication.charAt(i);
                if (punctuationsSub.indexOf(c) == -1) new_dedication += c;
            }
            dedication = new_dedication.trim();
            dedication = dedication.replace(" .", ".");
            dedication = dedication.replace("...", ".");
            dedication = dedication.replace("..", ".");
            if ((dedication.startsWith(",")) | (dedication.startsWith("."))) {
                dedication = dedication.substring(1, dedication.length()).trim();
            }
            dedication = dedication.replace("@BULLET", " • ");
            item.setDedication(dedication);
        }
        String title = item.getTitle();
        if (title != null) {
            if (title.endsWith("'")) {
                title = title.substring(0, title.length() - 1).trim();
            }
            title = title.replace("@BULLET", " • ");
            item.setTitle(title);
        }
        String english_title = item.getEnglishTitle();
        if (english_title != null) {
            if (english_title.endsWith("'")) {
                english_title = english_title.substring(0, english_title.length() - 1).trim();
            }
            english_title = english_title.replace("@BULLET", " • ");
            item.setEnglishTitle(english_title);
        }
        String abstract_ = item.getAbstract();
        if (abstract_ != null) {
            if (abstract_.startsWith(") ")) {
                abstract_ = abstract_.substring(1, abstract_.length()).trim();
            }
            abstract_ = abstract_.replace("@BULLET", " • ");
            item.setAbstract(abstract_);
        }
        String address = item.getAddress();
        if (address != null) {
            address.replace("\t", " ");
            address = address.trim();
            if ((address.startsWith(",")) | (address.startsWith("("))) {
                address = address.substring(1, address.length()).trim();
            }
            if (address.endsWith(")")) {
                address = address.substring(0, address.length() - 1).trim();
            }
            item.setAddress(address);
        }
        String email = item.getEmail();
        if (email != null) {
            if (email.startsWith("E-mail :")) {
                email = email.replace("E-mail :", "").trim();
                item.setEmail(email);
            }
        }
        String authors = item.getAuthors();
        if (authors != null) {
            authors = authors.replace("0. ", "O. ");
            item.setAuthors(authors);
        }
        String keyword = item.getKeyword();
        if (keyword != null) {
            if (keyword.startsWith(":")) {
                keyword = keyword.substring(1, keyword.length()).trim();
                item.setKeyword(keyword);
            }
        }
        return item;
    }
}
