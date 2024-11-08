package org.fetlar.spectatus.controller;

import java.awt.Desktop;
import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.fetlar.spectatus.model.FileOps;
import org.fetlar.spectatus.model.Section;
import org.fetlar.spectatus.model.AssessmentContentPackage;
import org.fetlar.spectatus.model.Question;
import org.fetlar.spectatus.utilities.MiscUtils;
import org.fetlar.spectatus.view.MQWrapper;
import org.fetlar.spectatus.view.MetadataWindow;
import org.fetlar.spectatus.view.TaxonSettingsWindow;
import org.fetlar.spectatus.view.ViewUtils;
import org.fetlar.spectatus.view.Icon;
import org.fetlar.spectatus.view.IconPane;
import org.fetlar.spectatus.view.LeftPane;
import org.fetlar.spectatus.view.MainWindow;
import org.fetlar.spectatus.view.MinibixBrowser;
import org.fetlar.spectatus.view.SectionPane;
import org.fetlar.spectatus.view.Splash;
import org.fetlar.spectatus.utilities.TmpData;
import org.imsglobal.xsd.imscp_v1p1.ManifestType;
import org.imsglobal.xsd.imsqti_v2p1.AssessmentItemRefType;
import org.imsglobal.xsd.imsqti_v2p1.AssessmentSectionType;
import org.imsglobal.xsd.imsqti_v2p1.AssessmentTestType;
import org.imsglobal.xsd.imsqti_v2p1.DivType;
import org.imsglobal.xsd.imsqti_v2p1.PType;
import org.imsglobal.xsd.imsqti_v2p1.RubricBlockType;
import org.imsglobal.xsd.imsqti_v2p1.SubmissionModeType;
import org.imsglobal.xsd.imsqti_v2p1.TimeLimitsType;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The base controlling class
 * @author paul
 *
 */
public class GroundControl {

    public static AssessmentContentPackage cp;

    public static MainWindow mainWindow;

    public static FileOps fileHelper;

    public static final int ZERO = 0;

    public static final int ADD_QUESTION = 1;

    public static final int SECTION_SELECTED = 2;

    public static final int UPDATE_QLIST = 3;

    public static final int ADD_NEW_SECTION = 4;

    public static final int DELETEQ = 5;

    public static final int SAVE_TEST = 6;

    public static final int IMPORT_QUESTION = 7;

    public static final int MINIBIX_TICKET_SELECTED = 8;

    public static final int LOAD_TEST = 9;

    public static final int MOVE_Q_LEFT = 10;

    public static final int MOVE_Q_RIGHT = 11;

    public static final int IMPORT_Q_FROM_DISK = 12;

    public static final int DELETE_SECTION = 14;

    public static final int EDIT_QUESTION = 15;

    public static final int FILE_CHANGED = 16;

    public static final int SECTION_UP = 17;

    public static final int SECTION_DOWN = 18;

    public static final int QUIT_APP = 19;

    public static final int IMPORT_XMLQ = 20;

    public static final int ADD_XML_QUESTION = 21;

    public static final int EXPORT_Q_AS_CP = 22;

    public static final int PREVIEW_TEST = 23;

    public static final int SET_METADATA = 24;

    public static final int DISPLAY_MD_WINDOW = 25;

    public static final int NEW_TEST = 26;

    public static final int EXPORT_TEST_TO_MB = 27;

    public static final int CHANGE_TAXON = 28;

    public static final int RELOAD_TAXON = 29;

    public static final int IMPORT_MOODLE = 30;

    public static void main(String[] args) {
        if ((System.getProperty("os.name").contains("Mac"))) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Spectatus");
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Error setting native LAF: " + e);
        }
        MainWindow.loadIcons();
        Splash splash = new Splash();
        fileHelper = new FileOps();
        setup();
        splash.dispose();
        mainWindow = new MainWindow();
        loadTestIntoView();
    }

    /**
	 * The main controlling method
	 */
    public static void perform(int instructions, Object what) {
        switch(instructions) {
            case ADD_QUESTION:
                reallyMessyTemporaryAddNewQuestion(what.toString());
                updateQs();
                break;
            case ADD_XML_QUESTION:
                reallyMessyTemporaryAddNewXMLQuestion(what.toString());
                updateQs();
                break;
            case UPDATE_QLIST:
                updateQs();
                break;
            case ADD_NEW_SECTION:
                addNewSection();
                updateQs();
                break;
            case DELETE_SECTION:
                {
                    int secno = mainWindow.getSelectedSection();
                    cp.removeSection(secno);
                    SectionPane s = (SectionPane) what;
                    s.kamikazi();
                    updateQs();
                    break;
                }
            case DELETEQ:
                int value = (Integer) what;
                if (value != -1) deleteQuestion(value);
                updateQs();
                break;
            case SAVE_TEST:
                saveTest();
                break;
            case IMPORT_QUESTION:
                displayMinibix();
                break;
            case MINIBIX_TICKET_SELECTED:
                downloadTicketFromMb((Integer) what);
                try {
                    mainWindow.getMinibix().dispose();
                } catch (Exception e) {
                }
                ;
                break;
            case EXPORT_Q_AS_CP:
                {
                    int qNo = (Integer) what;
                    int secno = mainWindow.getSelectedSection();
                    if (qNo != -1) {
                        Question q = cp.getSections().get(secno).getQuestions().get(qNo);
                        String saveFilename = ViewUtils.chooseFile(true, "zip");
                        fileHelper.saveQuestionToZip(q, saveFilename, cp);
                    }
                    break;
                }
            case LOAD_TEST:
                String loadFilename = ViewUtils.chooseFile(false, "zip");
                if (loadFilename != null) {
                    cp = new AssessmentContentPackage(new File(loadFilename));
                    mainWindow.dispose();
                    mainWindow = new MainWindow();
                    loadTestIntoView();
                }
                break;
            case MOVE_Q_LEFT:
                {
                    int qToMove = (Integer) what;
                    if (qToMove != 0) {
                        int secno = mainWindow.getSelectedSection();
                        Section currentSection = cp.getSections().get(secno);
                        currentSection.moveQleft(qToMove);
                        mainWindow.getIconPane().moveIconleft(qToMove);
                        updateQs();
                        mainWindow.getIconPane().getIcons().get(qToMove - 1).setSelected(true);
                    }
                    break;
                }
            case MOVE_Q_RIGHT:
                int qToMove = (Integer) what;
                if (qToMove != mainWindow.getDisplayedItemsSize() - 1) {
                    int secno = mainWindow.getSelectedSection();
                    Section currentSection = cp.getSections().get(secno);
                    currentSection.moveQright(qToMove);
                    mainWindow.getIconPane().moveIconright(qToMove);
                    updateQs();
                    mainWindow.getIconPane().getIcons().get(qToMove + 1).setSelected(true);
                }
                break;
            case IMPORT_XMLQ:
                {
                    String filename = ViewUtils.chooseFile(false, "xml");
                    if (filename != null) {
                        perform(GroundControl.ADD_XML_QUESTION, filename);
                    }
                    break;
                }
            case IMPORT_Q_FROM_DISK:
                {
                    String filename = ViewUtils.chooseFile(false, "zip");
                    if (filename != null) {
                        perform(GroundControl.ADD_QUESTION, filename);
                    }
                    break;
                }
            case EDIT_QUESTION:
                {
                    int secno = mainWindow.getSelectedSection();
                    Section currentSection = cp.getSections().get(secno);
                    int qNo = mainWindow.getSelectedQuestion();
                    Question currentQuestion = currentSection.getQuestions().get(qNo);
                    String zipFilename = FileOps.tempdir + FileOps.sep + "temporaryFromSpectatus.qtiquestionzip";
                    fileHelper.saveQuestionToZip(currentQuestion, zipFilename, cp);
                    new MQWrapper(zipFilename, secno, qNo);
                    break;
                }
            case FILE_CHANGED:
                {
                    Object[] paramArray = (Object[]) what;
                    String fileChanged = (String) paramArray[0];
                    int secNo = (Integer) paramArray[1];
                    int qNo = (Integer) paramArray[2];
                    int origSelected = mainWindow.getSelectedQuestion();
                    perform(ADD_QUESTION, FileOps.tempdir + FileOps.sep + "temporaryFromSpectatus.qtiquestionzip");
                    new File(FileOps.tempdir + FileOps.sep + "temporaryFromSpectatus.qtiquestionzip").delete();
                    Section currentSection = cp.getSections().get(secNo);
                    int lastQloc = currentSection.getQuestions().size() - 1;
                    currentSection.swapQs(qNo, lastQloc);
                    mainWindow.getIconPane().swapIcons(qNo, lastQloc);
                    perform(DELETEQ, lastQloc);
                    mainWindow.getIconPane().getIcons().get(origSelected).setSelected(true);
                    break;
                }
            case SECTION_DOWN:
                {
                    int secToMove = mainWindow.getSelectedSection();
                    if (secToMove != mainWindow.getLeftPane().getSectionPanes().size() - 1) {
                        Section msp1 = cp.getSections().get(secToMove);
                        Section msp2 = cp.getSections().get(secToMove + 1);
                        cp.swapSections(msp1, msp2);
                        SectionPane sp1 = mainWindow.getLeftPane().getSectionPanes().get(secToMove);
                        SectionPane sp2 = mainWindow.getLeftPane().getSectionPanes().get(secToMove + 1);
                        sp1.swapMeWith(sp2);
                        sp1.setSelected(false);
                        sp1.setVisible(false);
                        sp2.setSelected(true);
                        sp2.setVisible(true);
                        updateQs();
                    }
                    break;
                }
            case SECTION_UP:
                {
                    int secToMove = mainWindow.getSelectedSection();
                    if (secToMove != 0) {
                        Section msp1 = cp.getSections().get(secToMove);
                        Section msp2 = cp.getSections().get(secToMove - 1);
                        cp.swapSections(msp1, msp2);
                        SectionPane sp1 = mainWindow.getLeftPane().getSectionPanes().get(secToMove);
                        SectionPane sp2 = mainWindow.getLeftPane().getSectionPanes().get(secToMove - 1);
                        sp1.swapMeWith(sp2);
                        sp1.setSelected(false);
                        sp1.setVisible(false);
                        sp2.setSelected(true);
                        sp2.setVisible(true);
                        updateQs();
                    }
                    break;
                }
            case PREVIEW_TEST:
                {
                    String tempZip = FileOps.tempdir + FileOps.sep + "temporaryFromSpectatus.zip";
                    saveTest(tempZip, false);
                    previewTest(tempZip);
                    break;
                }
            case QUIT_APP:
                System.exit(0);
                break;
            case DISPLAY_MD_WINDOW:
                MetadataWindow md = new MetadataWindow(cp.getMetadata());
                md.setVisible(true);
                break;
            case SET_METADATA:
                cp.setMetadata((String[][]) what);
                break;
            case NEW_TEST:
                setup();
                mainWindow.dispose();
                mainWindow = new MainWindow();
                loadTestIntoView();
                break;
            case EXPORT_TEST_TO_MB:
                {
                    if (cp.getMetadata() == null) {
                        JOptionPane.showMessageDialog(null, "<html>You cannot upload to Minibix without setting the question<br>metadata.</html>", "No Metadata!", JOptionPane.ERROR_MESSAGE);
                        break;
                    }
                    int confirm = JOptionPane.showConfirmDialog(null, "<html>You are about to upload the current test to<br>" + Preferences.userRoot().node("Spectatus").get("MBAddr", "http://mathassess.caret.cam.ac.uk") + ":" + Preferences.userRoot().node("Spectatus").get("MBPort", "80"), "Confirmation", JOptionPane.OK_CANCEL_OPTION);
                    if (confirm == JOptionPane.OK_OPTION) {
                        String tempZip = FileOps.tempdir + FileOps.sep + "uploadFromSpectatus.zip";
                        saveTest(tempZip, false);
                        if (MiscUtils.pushZipToMinibix(cp.getMinibixTicketNumber(), tempZip) == -1) {
                        } else {
                        }
                    }
                    break;
                }
            case CHANGE_TAXON:
                new TaxonSettingsWindow();
                break;
            case RELOAD_TAXON:
                fileHelper.setTaxon(fileHelper.getTaxon());
                break;
            case IMPORT_MOODLE:
                {
                    String filename = ViewUtils.chooseFile(false, "txt");
                    int confirm = JOptionPane.showConfirmDialog(null, "<html>This will attempt to import the file selected, treating<br>" + "it as a GIFT format file exported from Moodle.<br><br>" + "Any multiple choice questions or true or false questions<br>" + "will be imported - anything else will be ignored. If this<br>" + "is not a GIFT format file from Moodle Spectatus will almost<br>" + "certainly crash. In fact this is a highly experimental<br>" + "feature, so Spectatus may well crash even IF it's a GIFT<br>" + "format file! :-)<br><br>" + "Please confirm you want to proceed. Proceeding will lose any<br>" + "unsaved data!", "Confirmation", JOptionPane.OK_CANCEL_OPTION);
                    if (confirm == JOptionPane.OK_OPTION) {
                        importGift(filename);
                    }
                    break;
                }
            default:
                System.out.println("No handler in GroundControl for #" + instructions + " yet.");
                break;
        }
    }

    public static void importGift(String giftFilename) {
        String inGift;
        try {
            inGift = FileUtils.readFileToString(new File(giftFilename));
        } catch (Exception e) {
            return;
        }
        perform(GroundControl.NEW_TEST, null);
        String giftTempDir = FileOps.tempdir + FileOps.sep + "giftimport";
        File giftTempDirF = new File(giftTempDir);
        giftTempDirF.mkdir();
        String[] questions = inGift.split("\\n\\s*\\n|\\r\\n\\s*\\r\\n");
        int questionNo = 1;
        for (String q : questions) {
            String[] x = q.split("::");
            String title = "";
            String questionText = "";
            if (x.length == 1) {
                questionText = x[0];
                title = StringUtils.split(questionText, "{")[0];
            } else {
                title = x[1];
                questionText = x[2];
            }
            if (questionText.contains("<meta content\\=\"Word.Document\"")) {
                questionText = questionText.replaceAll("\\[html\\](.*)<!--StartFragment-->", "");
                questionText = questionText.replace("<!--EndFragment-->", "");
                System.out.println("MS cruft: qt now " + questionText);
            }
            if (StringUtils.containsIgnoreCase(questionText, "{T}") || StringUtils.containsIgnoreCase(questionText, "{TRUE}")) {
                questionText = StringUtils.replace(questionText, "{T}", "{\n=True\n~False\n}");
                questionText = StringUtils.replace(questionText, "{TRUE}", "{\n=True\n~False\n}");
            } else if (StringUtils.containsIgnoreCase(questionText, "{F}") || StringUtils.containsIgnoreCase(questionText, "{FALSE}")) {
                questionText = StringUtils.replace(questionText, "{F}", "{\n=True\n~False\n}");
                questionText = StringUtils.replace(questionText, "{FALSE}", "{\n=True\n~False\n}");
            }
            if (!questionText.contains("{")) {
                System.out.println("No answers - dropping!");
                continue;
            }
            x = StringUtils.split(questionText, "{");
            if (x.length == 1) {
                System.out.println("Split failed " + x[0] + " : " + questionText);
                continue;
            }
            questionText = x[0];
            questionText = StringUtils.remove(questionText, "\\n");
            questionText = StringUtils.remove(questionText, "\\");
            questionText = StringUtils.remove(questionText, "[html]");
            String answersBlob = x[1];
            answersBlob = answersBlob.trim();
            answersBlob = StringUtils.remove(answersBlob, "}");
            String[] answers = answersBlob.split("[\n]|[\r\n]");
            ArrayList<Object[]> answersList = new ArrayList<Object[]>();
            for (int i = 0; i < answers.length; i++) {
                answers[i] = answers[i].trim();
                if (answers[i].startsWith("=")) {
                    Object[] answerBlob = new Object[2];
                    answerBlob[0] = answers[i].substring(1);
                    answerBlob[1] = true;
                    answersList.add(answerBlob);
                } else {
                    Object[] answerBlob = new Object[2];
                    answerBlob[0] = answers[i].substring(1);
                    answerBlob[1] = false;
                    answersList.add(answerBlob);
                }
            }
            String result = MiscUtils.generateSimpleChoiceXML(answersList, title, 1.0, questionText, "Well done, that's correct!", "Sorry, that's incorrect.");
            try {
                FileUtils.writeStringToFile(new File(giftTempDir + "/" + questionNo + ".xml"), result, "UTF-8");
            } catch (Exception e) {
            }
            questionNo++;
        }
        for (int i = 1; i < questionNo; i++) {
            perform(GroundControl.ADD_XML_QUESTION, giftTempDir + "/" + i + ".xml");
        }
        FileOps.deleteDirectory(giftTempDirF);
    }

    public static void previewTest(String testFilename) {
        String baseURL = "http://www2.ph.ed.ac.uk/MathAssessEngine";
        HttpClient httpclient = new DefaultHttpClient();
        String retStr = null;
        try {
            String baseWebAddress = Preferences.userRoot().node("Spectatus").get("MAEngineURL", "http://www2.ph.ed.ac.uk");
            String baseWebDir = Preferences.userRoot().node("Spectatus").get("MAEngineDir", "MathAssessEngine");
            String basePort = Preferences.userRoot().node("Spectatus").get("MAEnginePort", "80");
            if (baseWebDir.trim().length() != 0) {
                baseWebDir = "/" + baseWebDir;
            }
            baseURL = baseWebAddress + ":" + basePort + baseWebDir;
            String URL = baseURL + "/article/save";
            System.out.println(URL);
            httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(URL);
            File file = new File(testFilename);
            InputStreamEntity reqEntity = new InputStreamEntity(new FileInputStream(file), -1);
            reqEntity.setContentType("application/zip");
            httppost.setEntity(reqEntity);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            retStr = httpclient.execute(httppost, responseHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
        httpclient.getConnectionManager().shutdown();
        if (retStr != null) {
            if (retStr.startsWith("id:")) {
                String[] x = retStr.split(":");
                String id = x[1];
                try {
                    Desktop.getDesktop().browse(new URI(baseURL + "/article/reset/" + id));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void loadTestIntoView() {
        loadBasicTestDetails();
        loadSections();
        updateQs();
    }

    public static void displayMinibix() {
        mainWindow.setMinibix(new MinibixBrowser());
        mainWindow.getMinibix().setSize(new Dimension(740, 600));
    }

    public static void downloadTicketFromMb(int ticketNo) {
        fileHelper.downloadQFromMinibix(ticketNo);
        try {
            reallyMessyTemporaryAddNewQuestion(fileHelper.tempdir + fileHelper.sep + "minibix.zip");
        } catch (Exception e) {
        }
        new File(fileHelper.tempdir + fileHelper.sep + "minibix.zip").delete();
        updateQs();
    }

    public static void deleteQuestion(int qno) {
        int secno = mainWindow.getSelectedSection();
        Section currentSection = cp.getSections().get(secno);
        Question qToKill = currentSection.getQuestions().get(qno);
        currentSection.removeQno(qno);
        fileHelper.deleteQuestionFiles(qToKill, cp.getManifestPath());
    }

    public static void saveTest(String destination, boolean confirmMd) {
        if (cp.getMetadata() != null) cp.setMetadata(cp.getMetadata()); else if (confirmMd) {
            int response = JOptionPane.showConfirmDialog(null, "<html>You have not entered any metadata for this test.<br>You really should do so before saving it.<br>Do you want to continue to save regardless?</html>", "Missing Metadata", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (response == JOptionPane.NO_OPTION) {
                return;
            }
        }
        if (mainWindow.getLeftPane().getPostProcessing()) cp.addPostProcessing();
        if (mainWindow.getLeftPane().getIndividual()) {
            cp.getTest().getTestPart().get(0).setSubmissionMode(SubmissionModeType.INDIVIDUAL);
        } else {
            cp.getTest().getTestPart().get(0).setSubmissionMode(SubmissionModeType.SIMULTANEOUS);
        }
        try {
            fileHelper.saveTestToDisk(cp.getTest(), FileOps.tempdir + FileOps.sep + "spectemp" + FileOps.sep + cp.getTestXMLlocationHref());
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        ManifestType manifest = fileHelper.createRootManifest(cp, mainWindow.getSaveFormat());
        try {
            fileHelper.saveManifestToDisk(manifest, FileOps.tempdir + FileOps.sep + "spectemp" + FileOps.sep + "imsmanifest.xml");
        } catch (JAXBException e1) {
            e1.printStackTrace();
        }
        if (destination == null) destination = ViewUtils.chooseFile(true, "zip");
        if (destination != null) {
            File tempDirFile = new File(FileOps.tempdir + FileOps.sep + "spectemp");
            if (!destination.contains(".zip")) {
                destination = destination + ".zip";
            }
            File outFile = new File(destination);
            try {
                fileHelper.zipDirectory(tempDirFile, outFile);
            } catch (IOException e) {
            }
        }
    }

    public static void saveTest(boolean confirmMd) {
        saveTest(null, confirmMd);
    }

    public static void saveTest() {
        saveTest(true);
    }

    public static void reallyMessyTemporaryAddNewXMLQuestion(String qFilename) {
        Question newQuestion = fileHelper.getQuestionFromXML(qFilename, cp.getPath());
        cp.incSize(newQuestion.getSize());
        int currentSectionNo = mainWindow.getSelectedSection();
        Section currentSection = cp.getSections().get(currentSectionNo);
        currentSection.getQuestions().add(newQuestion);
        AssessmentItemRefType assIRF = new AssessmentItemRefType();
        assIRF.setHref(newQuestion.getHref());
        assIRF.setIdentifier(newQuestion.getRefId());
        currentSection.getSection().getSectionPartElementGroup().add(assIRF);
        try {
            fileHelper.saveTestToDisk(cp.getTest(), FileOps.tempdir + FileOps.sep + "spectemp" + FileOps.sep + cp.getTestXMLlocationHref());
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public static void reallyMessyTemporaryAddNewQuestion(String qFilename) {
        Question newQuestion = fileHelper.getQuestionFromCP(qFilename, cp.getPath());
        if (newQuestion == null) {
            AssessmentContentPackage newCp = new AssessmentContentPackage(new File(qFilename));
            if (newCp.isValid()) {
                cp = newCp;
                mainWindow.dispose();
                mainWindow = new MainWindow();
                loadTestIntoView();
            }
            return;
        }
        cp.incSize(newQuestion.getSize());
        int currentSectionNo = mainWindow.getSelectedSection();
        Section currentSection = cp.getSections().get(currentSectionNo);
        currentSection.getQuestions().add(newQuestion);
        AssessmentItemRefType assIRF = new AssessmentItemRefType();
        assIRF.setHref(newQuestion.getHref());
        assIRF.setIdentifier(newQuestion.getRefId());
        currentSection.getSection().getSectionPartElementGroup().add(assIRF);
        try {
            fileHelper.saveTestToDisk(cp.getTest(), FileOps.tempdir + FileOps.sep + "spectemp" + FileOps.sep + cp.getTestXMLlocationHref());
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public static void addNewSection() {
        Section section = new Section();
        section.setSection(new AssessmentSectionType());
        section.getSection().setIdentifier("id-" + MiscUtils.miniUUID());
        section.getSection().setVisible(true);
        section.setQuestions(new ArrayList<Question>());
        section.getSection().setTitle(TmpData.getNewSectionName());
        cp.getSections().add(section);
        cp.getTest().getTestPart().get(0).getAssessmentSection().add(section.getSection());
        try {
            fileHelper.saveTestToDisk(cp.getTest(), FileOps.tempdir + FileOps.sep + "spectemp" + FileOps.sep + cp.getTestXMLlocationHref());
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public static void loadBasicTestDetails() {
        AssessmentTestType test = cp.getTest();
        LeftPane lp = mainWindow.getLeftPane();
        lp.setTestTitle(test.getTitle());
        TimeLimitsType timeLimit = test.getTimeLimits();
        if (timeLimit != null) {
            if (timeLimit.getMaxTime() != null) lp.setTimeLimitMax(Double.toString(timeLimit.getMaxTime()));
            if (timeLimit.getMinTime() != null) lp.setTimeLimitMin(Double.toString(timeLimit.getMinTime()));
        }
        if (test.getTestPart().size() > 1) {
            ViewUtils.testPartError();
        }
    }

    /**
	 * Updates the question list on a section
	 * TODO: Another fairly messy chunk of code. Some of it should be in the view.
	 */
    public static void updateQs() {
        LeftPane lp = mainWindow.getLeftPane();
        IconPane iconPane = mainWindow.getIconPane();
        Section selSec = cp.getSections().get(lp.getSelectedSection());
        ArrayList<Question> qs = selSec.getQuestions();
        iconPane.removeAll();
        iconPane.repaint();
        for (Question q : qs) {
            Icon icon = new Icon(q.getQtiAssItem().getTitle());
            String toolTip = "";
            try {
                List<Object> elements = q.getMetadata().getAny();
                for (Object object : elements) {
                    Element element = (Element) object;
                    XPathFactory xpfactory = XPathFactory.newInstance();
                    XPath xpath = xpfactory.newXPath();
                    NamespaceContext nc = new NamespaceContext() {

                        @Override
                        public String getNamespaceURI(String prefix) {
                            return ("http://www.imsglobal.org/xsd/imsmd_v1p2");
                        }

                        @Override
                        public String getPrefix(String namespaceURI) {
                            return "lom";
                        }

                        @Override
                        public Iterator getPrefixes(String namespaceURI) {
                            return null;
                        }
                    };
                    xpath.setNamespaceContext(nc);
                    XPathExpression expr = xpath.compile("//lom:description");
                    Object result = expr.evaluate(element, XPathConstants.NODESET);
                    NodeList nodes = (NodeList) result;
                    String desc = "";
                    for (int x = 0; x < nodes.getLength(); x++) {
                        String s = nodes.item(x).getTextContent();
                        if (!s.contains("a Content Package generated by the Mathqurate")) {
                            s = s.trim();
                            desc = desc + s + " ";
                        }
                    }
                    String[] words = desc.split("\\s");
                    desc = "";
                    String lastline = "";
                    for (String word : words) {
                        desc = desc + word + " ";
                        lastline = lastline + word + " ";
                        if (lastline.length() > 30) {
                            desc = desc + "<br>";
                            lastline = "";
                        }
                    }
                    toolTip = toolTip + desc;
                    expr = xpath.compile("//lom:vcard");
                    result = expr.evaluate(element, XPathConstants.NODESET);
                    nodes = (NodeList) result;
                    String vcard = nodes.item(0).getTextContent();
                    vcard = vcard.replace("BEGIN:vcard FN:", "");
                    vcard = vcard.replace(" END:vcard", "");
                    toolTip = toolTip + "<hr>Author: " + vcard;
                    expr = xpath.compile("//lom:taxonpath/lom:taxon/lom:entry/lom:langstring");
                    result = expr.evaluate(element, XPathConstants.NODESET);
                    nodes = (NodeList) result;
                    String taxon = nodes.item(0).getTextContent();
                    toolTip = toolTip + "<hr>" + taxon;
                }
            } catch (Exception e) {
            }
            toolTip = toolTip.trim();
            if (toolTip.length() != 0) {
                toolTip = "<html>" + toolTip + "</html>";
                icon.setToolTipText(toolTip);
            }
            if (q.getNormalMaximum() != -2000) {
                toolTip = icon.getToolTipText();
                toolTip = toolTip.replace("<html>", "");
                toolTip = toolTip.replace("</html>", "");
                toolTip += "<hr/>Max score: " + q.getNormalMaximum();
                toolTip = "<html>" + toolTip + "</html>";
                icon.setToolTipText(toolTip);
            }
            iconPane.addIcon(icon);
        }
    }

    public static void loadSections() {
        LeftPane lp = mainWindow.getLeftPane();
        List<Section> ourSections = cp.getSections();
        for (Section ourSection : ourSections) {
            AssessmentSectionType section = ourSection.getSection();
            SectionPane sectionPane = lp.addNewSectionPane();
            sectionPane.setSectionName(section.getTitle());
            List<RubricBlockType> rbl = section.getRubricBlock();
            if (rbl.size() != 0) {
                Object ourRub = rbl.get(0).getBlockElementGroup().get(0);
                if (ourRub.getClass() == DivType.class) {
                    DivType div = (DivType) ourRub;
                    try {
                        sectionPane.setRubric(div.getContent().get(0).toString());
                    } catch (Exception e) {
                        sectionPane.setRubric("");
                    }
                }
                if (ourRub.getClass() == PType.class) {
                    PType p = (PType) ourRub;
                    sectionPane.setRubric(p.getContent().get(0).toString());
                }
            }
            if (section.getSelection() != null) {
                sectionPane.setNoQs(Integer.toString(section.getSelection().getSelect()));
                sectionPane.setShuffle(section.getSelection().isWithReplacement());
            }
            if (section.getOrdering() != null) sectionPane.setShuffle(section.getOrdering().isShuffle());
        }
    }

    public static void setup() {
        InputStream inputStream = GroundControl.class.getResourceAsStream("/res/test-ass.zip");
        File f = new File(FileOps.tempdir + FileOps.sep + "test-ass.zip");
        f.deleteOnExit();
        try {
            OutputStream out = new FileOutputStream(f);
            byte buf[] = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) out.write(buf, 0, len);
            out.close();
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        cp = new AssessmentContentPackage(f);
    }
}
