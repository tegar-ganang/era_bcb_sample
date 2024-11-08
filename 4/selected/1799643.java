package com.ojt.process;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import com.ojt.Competitor;
import com.ojt.CompetitorGroup;
import com.ojt.OjtConstants;
import com.ojt.dao.CompetitorsDao;
import com.ojt.dao.CompetitorsDaoFactory;
import com.ojt.tools.FileNameComposer;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

public class DiscardCompetitorsGroupStep extends AbstractStep {

    private final Logger logger = Logger.getLogger(getClass());

    @Override
    public boolean finalizeStep() {
        return true;
    }

    @Override
    public JComponent getStepComponent() {
        return null;
    }

    @Override
    public String getTitle() {
        return "Gestion des groupes de comp�titeurs �cart�s du tri";
    }

    @Override
    public void process(final CompetitionDatas competitionDatas) {
        try {
            if (isAtLeastOneSavedGroup(competitionDatas.getCompetitorsGroups())) {
                final File savedFile = copyCompetitionFileToSavedFile(competitionDatas);
                final CompetitorsDao competitorsDao = CompetitorsDaoFactory.createCompetitorsDao(savedFile, true);
                final Iterator<CompetitorGroup> competitorsGroupIterator = competitionDatas.getCompetitorsGroups().iterator();
                final List<Competitor> competitorsToDelete = new LinkedList<Competitor>();
                for (; competitorsGroupIterator.hasNext(); ) {
                    final CompetitorGroup competitorGroup = competitorsGroupIterator.next();
                    logger.info("Groupe '" + competitorGroup.getName() + "' r�serv� : " + competitorGroup.isSavedForAnotherWeighing());
                    if (!competitorGroup.isSavedForAnotherWeighing()) {
                        competitorsToDelete.addAll(competitorGroup.getCompetitors());
                    }
                }
                competitorsDao.deleteCompetitors(competitorsToDelete);
            }
        } catch (final Exception ex) {
            JOptionPane.showMessageDialog(null, "Le fichier de r�serve n'a pas pu �tre cr��.", "OJT", JOptionPane.ERROR_MESSAGE);
            logger.error("Erreur lors de la cr�ation du fichier de r�serve.", ex);
        }
        stepFinish();
    }

    private boolean isAtLeastOneSavedGroup(final List<CompetitorGroup> competitorsGroups) {
        for (final CompetitorGroup group : competitorsGroups) {
            if (group.isSavedForAnotherWeighing()) {
                return true;
            }
        }
        return false;
    }

    private File copyCompetitionFileToSavedFile(final CompetitionDatas competitionDatas) throws IOException {
        final File savedFile = new File(OjtConstants.WEIGHING_DIRECTORY, FileNameComposer.composeSavedFileForAnotherSortName(competitionDatas.getCompetitionDescriptor(), competitionDatas.getCompetitionFile().getName().substring(competitionDatas.getCompetitionFile().getName().lastIndexOf('.'))));
        if (!savedFile.getParentFile().exists()) {
            savedFile.getParentFile().mkdirs();
        }
        FileUtils.copyFile(competitionDatas.getCompetitionFile(), savedFile);
        return savedFile;
    }
}
