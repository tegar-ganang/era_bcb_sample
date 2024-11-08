package itjava.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import itjava.data.BlankType;
import itjava.data.LocalMachine;
import itjava.model.BRDStore;
import itjava.model.BuildXMLCreate;
import itjava.model.Tutorial;
import itjava.model.WordInfo;
import itjava.presenter.TutorialPresenter;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.Before;
import org.junit.Test;

public class TutorialPresenterTest {

    private WordInfo wordInfo;

    private ArrayList<WordInfo> wordInfoList;

    private ArrayList<WordInfo> oriWordInfoList;

    private ArrayList<String> linesOfCode;

    private TutorialPresenter tutorialPresenter;

    private Tutorial tutorial;

    private String[] _bounds;

    @Test
    public final void MultipleWordsTutorialCodeHasBalancedParanthesis() throws Exception {
        GivenValidMultipleWordInfoList();
        GivenValidLinesOfCode();
        WhenGetTutorialIsCalled();
        ThenNumberofParanthesisIsBalanced();
    }

    @Before
    public final void SetUp() {
        wordInfo = new WordInfo();
        wordInfoList = new ArrayList<WordInfo>();
        oriWordInfoList = new ArrayList<WordInfo>();
        linesOfCode = new ArrayList<String>();
        tutorialPresenter = new TutorialPresenter();
    }

    @Test
    public final void SplitLabelsAreDeclaredInitiatedAndAdded() throws Exception {
        GivenValidLinesOfCode();
        GivenValidWordInfoList();
        WhenGetTutorialIsCalled();
        ThenLabelSyntaxHasPositiveIndices();
    }

    @Test
    public final void TextFieldHasPositiveWidthAndHigherXCoord() throws Exception {
        GivenValidLinesOfCode();
        GivenValidWordInfoList();
        WhenGetTutorialIsCalled();
        ThenWidthIsPositive();
        ThenXCoordIsHigherThanPriorComponent();
    }

    @Test
    public final void AllDataListArePopulated() throws Exception {
        GivenValidMultipleWordInfoList();
        GivenValidLinesOfCode();
        WhenGetTutorialIsCalled();
        ThenEdgeDataListIsPopulated();
        ThenLabelDataListIsPopulated();
    }

    @Test
    public final void BRDIsGenerated() throws Exception {
        GivenValidMultipleWordInfoList();
        GivenValidLinesOfCode();
        WhenGetTutorialIsCalled();
        WhenBRDStoreIsCalled();
        ThenBrdFilesAreStored();
    }

    @Test
    public final void TutorialCodeHasBalancedParanthesis() throws Exception {
        GivenValidWordInfoList();
        GivenValidMultipleOriWordInfoList();
        GivenValidLinesOfCode();
        WhenGetTutorialIsCalled();
        ThenNumberofParanthesisIsBalanced();
    }

    @Test
    public void ButtonFieldIsAdded() throws Exception {
        GivenValidLinesOfCode();
        GivenValidWordInfoList();
        WhenGetTutorialIsCalled();
        ThenDoneButtonSyntaxHasPositiveIndices();
        ThenButtonFieldHasProperBounds();
    }

    @Test
    public final void DeliverableIsGenerated() throws Exception {
        GivenValidMultipleWordInfoList();
        GivenValidLinesOfCode();
        WhenGetTutorialIsCalled();
        WhenCompiled();
        WhenBRDStoreIsCalled();
        ResetBuildXMLFile();
        WhenBuildPropertiesIsCreated();
        WhenBuildXMLCreateIsCalled();
        WhenDeployBatchScriptIsCalled();
        ThenAllDeliverablesArePresentInDeliveryFolder();
    }

    private void WhenBuildPropertiesIsCreated() {
        try {
            File buildPropertiesFile = new File(LocalMachine.home + "automate/deploy-tutor/build.properties");
            if (buildPropertiesFile.exists()) {
                buildPropertiesFile.delete();
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(buildPropertiesFile));
            String buildProperties = "server=http://localhost:8080";
            buildProperties += ("\nserver-dir=/itjava/delivery/" + tutorial.getReadableName());
            buildProperties += ("\nserver-login=");
            buildProperties += ("\ndefault-codebase=http://localhost:8080/itjava/delivery/" + tutorial.getReadableName());
            writer.write(buildProperties);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public final void GetTutorialReturnsNotNullTutorial() throws Exception {
        GivenValidWordInfoList();
        GivenValidLinesOfCode();
        WhenGetTutorialIsCalled();
        ThenReturnedTutorialIsNotNull();
    }

    @Test
    public final void GetTutorialReturnsNullOnPassingInvalidParams() throws Exception {
        wordInfo.wordToBeBlanked = "new";
        wordInfo.lineNumber = 2;
        wordInfo.blankType = BlankType.Text;
        wordInfo.columnNumber = 10;
        wordInfoList.add(wordInfo);
        tutorial = tutorialPresenter.GetTutorial("sampleName", "query", linesOfCode, wordInfoList, "blank source", oriWordInfoList);
        assertEquals(null, tutorial);
    }

    @Test
    public final void GetTutorialWithMultipleWordsReturnsNotNullTutorial() throws Exception {
        GivenValidMultipleWordInfoList();
        GivenValidLinesOfCode();
        WhenGetTutorialIsCalled();
        ThenReturnedTutorialIsNotNull();
    }

    private void GivenValidLinesOfCode() {
        linesOfCode.add("class myfirstjavaprog {");
        linesOfCode.add("public static void main(String args[]) {");
        linesOfCode.add("System.out.println(\"Hello World!\");");
        linesOfCode.add("}");
        linesOfCode.add("}");
    }

    private void GivenValidMultipleOriWordInfoList() {
        wordInfo = new WordInfo();
        wordInfo.wordToBeBlanked = "out";
        wordInfo.lineNumber = 3;
        wordInfo.columnNumber = 7;
        wordInfo.blankType = BlankType.Text;
        oriWordInfoList.add(wordInfo);
        wordInfo = new WordInfo();
        wordInfo.wordToBeBlanked = "static";
        wordInfo.blankType = BlankType.Text;
        wordInfo.lineNumber = 2;
        wordInfo.columnNumber = 7;
        oriWordInfoList.add(wordInfo);
        wordInfo = new WordInfo();
        wordInfo.wordToBeBlanked = "myfirst";
        wordInfo.blankType = BlankType.Text;
        wordInfo.lineNumber = 1;
        wordInfo.columnNumber = 6;
        oriWordInfoList.add(wordInfo);
    }

    private void GivenValidMultipleWordInfoList() {
        wordInfo = new WordInfo();
        wordInfo.wordToBeBlanked = "out";
        wordInfo.lineNumber = 3;
        wordInfo.columnNumber = 7;
        wordInfo.blankType = BlankType.Text;
        wordInfoList.add(wordInfo);
        wordInfo = new WordInfo();
        wordInfo.wordToBeBlanked = "static";
        wordInfo.blankType = BlankType.Text;
        wordInfo.lineNumber = 2;
        wordInfo.columnNumber = 7;
        wordInfoList.add(wordInfo);
    }

    private void GivenValidWordInfoList() {
        wordInfo.wordToBeBlanked = "out";
        wordInfo.lineNumber = 3;
        wordInfo.columnNumber = 7;
        wordInfo.blankType = BlankType.Text;
        wordInfoList.add(wordInfo);
    }

    private void ThenAllDeliverablesArePresentInDeliveryFolder() {
    }

    private void ThenBrdFilesAreStored() {
        String fileToCheck = LocalMachine.home + "generated/" + tutorial.getTutorialName() + ".brd";
        File f = new File(fileToCheck);
        assertTrue(f.exists());
    }

    private void ThenButtonFieldHasProperBounds() {
        String subStringCode = tutorial.tutorialCode.substring(tutorial.tutorialCode.indexOf("doneButton.setBounds(") + ("doneButton.setBounds(").length());
        subStringCode = subStringCode.substring(0, subStringCode.indexOf(')') - 1);
        _bounds = subStringCode.split(",");
        assertTrue(Integer.parseInt(_bounds[1]) > 20);
    }

    private void ThenDoneButtonSyntaxHasPositiveIndices() {
        assertTrue(tutorial.tutorialCode.indexOf("private pact.DorminWidgets.DorminButton doneButton;") > 0);
        assertTrue(tutorial.tutorialCode.indexOf("doneButton = new pact.DorminWidgets.DorminButton();") > 0);
        assertTrue(tutorial.tutorialCode.indexOf("add(doneButton);") > 0);
    }

    private void ThenEdgeDataListIsPopulated() {
        assertTrue(this.tutorial.getEdgeDataList().size() > 0);
    }

    private void ThenLabelDataListIsPopulated() {
        System.out.println(this.tutorial.getLabelDataList());
        assertTrue(this.tutorial.getLabelDataList().size() > 0);
    }

    private void ThenLabelSyntaxHasPositiveIndices() {
        assertTrue(tutorial.tutorialCode.indexOf("lblLine3Col7 = new pact.DorminWidgets.DorminLabel();") > 0);
        assertTrue(tutorial.tutorialCode.indexOf("private pact.DorminWidgets.DorminLabel lblLine3Col7;") > 0);
        assertTrue(tutorial.tutorialCode.indexOf("add(lblLine3Col7);") > 0);
    }

    private void ThenNumberofParanthesisIsBalanced() {
        System.out.println(tutorial.tutorialCode);
        ArrayList<Character> paranthesisStack = new ArrayList<Character>();
        char[] tutorialCodeArr = tutorial.tutorialCode.toCharArray();
        for (char currCharacter : tutorialCodeArr) {
            if (currCharacter == '{') {
                paranthesisStack.add(currCharacter);
            } else if (currCharacter == '}') {
                paranthesisStack.remove(paranthesisStack.size() - 1);
            }
        }
        assertEquals(0, paranthesisStack.size());
    }

    private void ThenReturnedTutorialIsNotNull() {
        assertNotNull(tutorial);
    }

    private void ThenWidthIsPositive() {
        String subStringCode = tutorial.tutorialCode.substring(tutorial.tutorialCode.indexOf("txtLine3Col7.setBounds(") + ("txtLine3Col7.setBounds(").length());
        subStringCode = subStringCode.substring(0, subStringCode.indexOf(')') - 1);
        _bounds = subStringCode.split(",");
        assertTrue(Integer.parseInt(_bounds[2]) > 0);
    }

    private void ThenXCoordIsHigherThanPriorComponent() {
        assertTrue(Integer.parseInt(_bounds[1]) > 20);
    }

    private void WhenBRDStoreIsCalled() {
        BRDStore.GenerateBRD(tutorial);
    }

    private void WhenDeployBatchScriptIsCalled() {
        try {
            System.out.println("Running the batch script for Building Webserver Files");
            String command = "cmd /C start " + LocalMachine.home + "automate/autoBuild.bat";
            Runtime rt = Runtime.getRuntime();
            rt.exec(command);
            System.out.println("Finished running the autoBuild batch script");
        } catch (Exception e) {
            System.out.println("Error creating the FileInfo panel: " + e);
            e.printStackTrace();
        }
    }

    private void WhenGetTutorialIsCalled() throws Exception {
        tutorial = tutorialPresenter.GetTutorial("testClassName", "test query", linesOfCode, wordInfoList, "blank source", oriWordInfoList);
    }

    private void WhenCompiled() {
        String CompilePath = "generated/" + tutorial.getTutorialName() + ".java";
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int result = compiler.run(null, null, null, CompilePath);
        System.out.println("Compile result code = " + result);
    }

    private void ResetBuildXMLFile() {
        String XMLpath = LocalMachine.home + "automate/deploy-tutor/build.xml";
        String BasicXML = LocalMachine.home + "generated/build.xml";
        File toDel = new File(XMLpath);
        toDel.delete();
        File sourceFile = new File(BasicXML);
        File destFile = new File(XMLpath);
        try {
            copyFile(sourceFile, destFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyFile(File sourceFile, File destFile) throws IOException {
        if (!sourceFile.exists()) {
            return;
        }
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        FileChannel source = null;
        FileChannel destination = null;
        source = new FileInputStream(sourceFile).getChannel();
        destination = new FileOutputStream(destFile).getChannel();
        if (destination != null && source != null) {
            destination.transferFrom(source, 0, source.size());
        }
        if (source != null) {
            source.close();
        }
        if (destination != null) {
            destination.close();
        }
    }

    private void WhenBuildXMLCreateIsCalled() {
        BuildXMLCreate.GenerateBuildXML(tutorial);
    }
}
