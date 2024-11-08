package com.ojt.process;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import com.ojt.OjtConstants;
import com.ojt.tools.FileNameComposer;

/**
 * Etape de cr�ation du fichier source
 * @author R�mi "DwarfConan" Guitreau
 * @since 17 oct. 2009 : Cr�ation
 */
public class CreateSourceFileStep extends AbstractStep {

    private final Logger logger = Logger.getLogger(getClass());

    private JPanel stepPanel;

    private JLabel exportLabel;

    public CreateSourceFileStep() {
        super();
        initStepPanel();
    }

    @Override
    public JComponent getStepComponent() {
        return stepPanel;
    }

    @Override
    public void process(final CompetitionDatas competitionDatas) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                exportLabel.setForeground(Color.black);
                final File sourceFile = new File(OjtConstants.SOURCE_DIRECTORY, FileNameComposer.composeFileName(competitionDatas.getCompetitionDescriptor(), competitionDatas.getCompetitionFile().getName().substring(competitionDatas.getCompetitionFile().getName().lastIndexOf('.'))));
                try {
                    FileUtils.copyFile(competitionDatas.getCompetitionFile(), sourceFile);
                    exportLabel.setText("Fichier source enregistr� dans le fichier : " + sourceFile.getAbsolutePath());
                } catch (final Exception ex) {
                    logger.error("Erreur lors de la cr�ation du fichier source : " + sourceFile, ex);
                    exportLabel.setForeground(Color.red);
                    exportLabel.setText("Erreur lors du fichier source.");
                }
                stepFinish();
            }
        });
    }

    @Override
    public boolean finalizeStep() {
        return true;
    }

    @Override
    public String getTitle() {
        return "Cr�ation du fichier source";
    }

    private void initStepPanel() {
        stepPanel = new JPanel(new GridBagLayout());
        exportLabel = new JLabel();
        stepPanel.add(exportLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    }
}
