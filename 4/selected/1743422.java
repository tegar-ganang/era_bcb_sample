package wand.graphicsChooser;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import wand.ChannelFrame;
import wand.graphicsChooser.themeSets.FolderThemeSet;
import wand.graphicsChooser.themeSets.NumericThemeSet;
import wand.graphicsChooser.themeSets.Theme;
import wand.graphicsChooser.themeSets.ThemeSet;
import wand.patterns.vector.*;
import wand.patterns.PatternInterface;
import wand.patterns.clip.*;

public final class ThemeSelectorPanel extends JPanel {

    private static final int NO_MODIFIER = 16;

    private static final int SHIFT_MODIFIER = 17;

    private static final int CTRL_MODIFIER = 18;

    private static final int CHANNEL_CAPACITY_PER_THEME = 4;

    private static int buttonWidth = 90, buttonHeight = 70;

    private static int gridWidth = 8, gridHeight = 5;

    public ArrayList<String> unnumberedClipNames = new ArrayList<String>();

    private ThemeSet themeSet = new FolderThemeSet("clips/folders");

    public ThemeSelectorPanel() {
        setLayout(new GridLayout(gridHeight, gridWidth));
        setPreferredSize(new Dimension(buttonWidth * gridWidth, buttonHeight * gridHeight));
        this.setBorder(wand.ChannelFrame.chooserBorder);
        makeButtons();
    }

    private void makeButtons() {
        for (String themeName : this.themeSet.getThemeNames()) {
            this.addThemeButton(this.themeSet.getTheme(themeName));
        }
    }

    private void addThemeButton(Theme theme) {
        JButton button = new JButton();
        button.setMargin(new java.awt.Insets(0, 0, 0, 0));
        File buttonImageFile = theme.getButtonImageFile();
        Image buttonImage = Toolkit.getDefaultToolkit().getImage(buttonImageFile.getAbsolutePath());
        int imageInset = 10;
        ImageIcon imageIcon = new ImageIcon(buttonImage.getScaledInstance(buttonWidth - imageInset, buttonHeight - imageInset, 0));
        button.setIcon(imageIcon);
        int themeNumber = theme.getThemeNumber();
        System.out.println(themeNumber + " " + theme.getName());
        if (themeNumber % 8 == 0) button.setBackground(Color.black);
        if (themeNumber % 8 == 1) button.setBackground(Color.green);
        if (themeNumber % 8 == 2) button.setBackground(Color.magenta);
        if (themeNumber % 8 == 3) button.setBackground(Color.white);
        if (themeNumber % 8 == 4) button.setBackground(Color.orange);
        if (themeNumber % 8 == 5) button.setBackground(Color.blue);
        if (themeNumber % 8 == 6) button.setBackground(Color.yellow);
        if (themeNumber % 8 == 7) button.setBackground(Color.red);
        button.setBorder(null);
        button.addActionListener(new ButtonListener(theme));
        add(button);
    }

    private class ButtonListener implements java.awt.event.ActionListener {

        private Theme theme;

        public ButtonListener(Theme theme) {
            this.theme = theme;
        }

        public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
            int firstRowOffset = 0;
            int secondRowOffset = 4;
            int thirdRowOffset = 8;
            int firstChannel = firstRowOffset;
            int modifier = actionEvent.getModifiers();
            if (modifier == NO_MODIFIER) firstChannel = firstRowOffset; else if (modifier == SHIFT_MODIFIER) firstChannel = secondRowOffset; else if (modifier == CTRL_MODIFIER) firstChannel = thirdRowOffset; else System.out.println("Modifier error on themeSelector panel");
            addThemeToChannels(firstChannel, theme);
        }
    }

    private void addThemeToChannels(int offset, Theme theme) {
        for (int i = 0; i < Math.min(CHANNEL_CAPACITY_PER_THEME, theme.size()); i++) {
            File folder = theme.getFolder(i);
            String folderName = theme.getFolder(i).getName();
            PatternInterface loadPattern = new Nightrider();
            int channelIndex = i + offset;
            patternToChannel(loadPattern, channelIndex);
            new ClipLoaderWorker(theme, channelIndex, i).execute();
        }
    }

    private class ClipLoaderWorker extends SwingWorker<GenericClipPattern, Void> {

        private int channelIndex;

        private int clipIndex;

        private Theme theme;

        public ClipLoaderWorker(Theme theme, int channelIndex, int clipIndex) {
            super();
            this.channelIndex = channelIndex;
            this.clipIndex = clipIndex;
            this.theme = theme;
        }

        public GenericClipPattern doInBackground() {
            return theme.makeGenericClipPattern(this.clipIndex);
        }

        public void done() {
            try {
                GenericClipPattern clip = get();
                patternToChannel(clip, channelIndex);
            } catch (ExecutionException e) {
            } catch (InterruptedException i) {
            }
        }
    }

    private void patternToChannel(PatternInterface pattern, int channelIndex) {
        ChannelFrame.channelGridPanel.channels[channelIndex].setPatternType(pattern);
        ChannelFrame.controlPanel.clipParametersPanel.loadParameters();
        ChannelFrame.channelGridPanel.channels[channelIndex].getChannelBeat().setGearIndex(pattern.getInitDelayIndex(), false);
        ChannelFrame.controlPanel.delayPanel.loadValue();
    }
}
