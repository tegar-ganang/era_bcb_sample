package org.in4ama.translator;

public class TranslatorTest {

    private String propertiesFile = "C:\\openedgesvn\\in4ama-sf\\trunk\\DocumentEditor\\resources\\en.properties";

    private String spreadSheetFile = "C:\\openedgesvn\\in4ama-sf\\trunk\\DocumentEditor\\lang\\studio.xls";

    private String projectPath = "C:\\openedgesvn\\in4ama-sf\\trunk\\DocumentEditor\\resources";

    private String propertiesFileE = "C:\\openedgesvn\\in4ama\\trunk\\DocumentService\\WebContent\\WEB-INF\\lib\\en.properties";

    private String spreadSheetFileE = "C:\\openedgesvn\\in4ama\\trunk\\DocumentService\\lang\\enterprise.xls";

    private String projectPathE = "C:\\openedgesvn\\in4ama\\trunk\\DocumentService\\WebContent\\WEB-INF\\lib";

    private String language = "en";

    private boolean studio = false;

    public static void main(String[] args) {
        new TranslatorTest().createProperties();
        System.out.println("done...");
    }

    private void createProperties() {
        TranslatorReader reader = new TranslatorReader();
        try {
            if (studio) {
                reader.read(spreadSheetFile, projectPath, language, true);
            } else {
                reader.read(spreadSheetFileE, projectPathE, language, true);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void createSpreadSheet() {
        TranslatorWriter writer = new TranslatorWriter();
        try {
            if (studio) {
                writer.write(propertiesFile, spreadSheetFile);
            } else {
                writer.write(propertiesFileE, spreadSheetFileE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
