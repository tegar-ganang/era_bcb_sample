package com.ojt.process;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import com.ojt.CompetitionDescriptor;
import com.ojt.OjtConstants;
import com.ojt.dao.CompetitorsDaoFactory;
import com.ojt.tools.FileNameComposer;
import java.io.File;
import java.io.IOException;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

/**
 * Etape de saisie des informations de la comp�tition
 * @author R�mi "DwarfConan" Guitreau
 * @since 17 oct. 2009 : Cr�ation
 */
public class PersistancyCreationStep extends AbstractStep {

    private final Logger logger = Logger.getLogger(getClass());

    public PersistancyCreationStep() {
        super();
    }

    @Override
    public JComponent getStepComponent() {
        return null;
    }

    @Override
    public void process(final CompetitionDatas competitionDatas) {
        try {
            competitionDatas.setCompetitionFile(createManifestationFile(competitionDatas));
            competitionDatas.setCompetitorsDao(CompetitorsDaoFactory.createCompetitorsDao(competitionDatas.getCompetitionFile(), competitionDatas.getOnlyWithWeight()));
        } catch (final IOException ex) {
            logger.error("Erreur lors de la cr�ation de la persistance.", ex);
            JOptionPane.showMessageDialog(null, "Impossible de cr�er la persistance pour cette manifestation. Les donn�es de poids ne seront pas conserv�es.", "OJT", JOptionPane.WARNING_MESSAGE);
        }
        stepFinish();
    }

    @Override
    public boolean finalizeStep() {
        return true;
    }

    @Override
    public String getTitle() {
        return "Mise en place de la persistance";
    }

    private File createManifestationFile(final CompetitionDatas competitionDatas) throws IOException {
        final File dir = new File(OjtConstants.PERSISTANCY_DIRECTORY, FileNameComposer.composeDirectoryName(competitionDatas.getCompetitionDescriptor()));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        final CompetitionDescriptor compDescr = competitionDatas.getCompetitionDescriptor();
        File manifFile = new File(dir, FileNameComposer.composeFileName(compDescr, extractExtensionFromFileName(competitionDatas.getCompetitionFile().getName())));
        if (manifFile.getName().equals(competitionDatas.getCompetitionFile().getName())) {
            manifFile = new File(dir, FileNameComposer.composeFileName(compDescr, "_1" + extractExtensionFromFileName(competitionDatas.getCompetitionFile().getName())));
        }
        FileUtils.copyFile(competitionDatas.getCompetitionFile(), manifFile);
        return manifFile;
    }

    private String extractExtensionFromFileName(final String fileName) {
        final int pos = fileName.lastIndexOf('.');
        if (pos != -1) {
            return fileName.substring(pos);
        }
        return "";
    }
}
