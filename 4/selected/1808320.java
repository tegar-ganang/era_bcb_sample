package i5d.gui;

import i5d.Image5D;
import i5d.cal.ChannelDisplayProperties;
import ij.*;
import ij.gui.*;
import ij.plugin.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

/**
 * ChannelControl contains one (sub-)Panel ("selectorPanel"), which contains a Button, 
 * which opens the color selection menu (grayed out in ONE_CHANNEL_GRAY mode) and a Choice 
 * "displayCoice", that controls the display mode (1 channel, overlay). The button and the
 * Coice are in a sub-Panel, which is in BorderLayout.NORTH. Additionally the selectorPanel
 * contains either a scrollbar (1 ch) or a checkbox group (overlay) in BorderLayout.CENTER. 
 * The selectorPanel is in BorderLayout.CENTER of the main panel.
 * The color selection menu is in BorderLayout.EAST of the main panel.
 */
public class ChannelControl extends Panel implements ItemListener, AdjustmentListener, ActionListener {

    /**
	 * 
	 */
    private static final long serialVersionUID = -3537465383241321652L;

    Image5DWindow win;

    Image5D i5d;

    ImageJ ij;

    Choice displayChoice;

    int displayMode;

    boolean displayGrayInTiles;

    Panel selectorPanel;

    ScrollbarWithLabel scrollbarWL;

    ChannelSelectorOverlay channelSelectorOverlay;

    Panel tiledSelectorPanel;

    Checkbox allGrayInTilesBox;

    Button colorButton;

    boolean colorChooserDisplayed;

    ChannelColorChooser cColorChooser;

    int nChannels;

    int currentChannel;

    public static final int ONE_CHANNEL_GRAY = 0;

    public static final int ONE_CHANNEL_COLOR = 1;

    public static final int OVERLAY = 2;

    public static final int TILED = 3;

    public static final String[] displayModes = { "gray", "color", "overlay", "tiled" };

    public static final String BUTTON_ACTIVATE_COLOR_CHOOSER = "Color";

    public static final String BUTTON_DEACTIVATE_COLOR_CHOOSER = "Color";

    public ChannelControl(Image5DWindow win) {
        super(new BorderLayout(5, 5));
        this.win = win;
        this.i5d = (Image5D) win.getImagePlus();
        this.ij = IJ.getInstance();
        currentChannel = i5d.getCurrentChannel();
        nChannels = i5d.getNChannels();
        addKeyListener(win);
        addKeyListener(ij);
        addKeyListener(win);
        selectorPanel = new Panel(new BorderLayout(0, 0));
        add(selectorPanel, BorderLayout.CENTER);
        Panel subPanel = new Panel(new GridLayout(2, 1, 1, 1));
        colorButton = new Button(BUTTON_ACTIVATE_COLOR_CHOOSER);
        colorButton.setEnabled(true);
        colorChooserDisplayed = false;
        subPanel.add(colorButton);
        colorButton.addActionListener(this);
        colorButton.addKeyListener(win);
        colorButton.addKeyListener(ij);
        colorButton.addKeyListener(win);
        displayChoice = new Choice();
        displayChoice.add(displayModes[ONE_CHANNEL_GRAY]);
        displayChoice.add(displayModes[ONE_CHANNEL_COLOR]);
        displayChoice.add(displayModes[OVERLAY]);
        displayChoice.add(displayModes[TILED]);
        displayChoice.select(ONE_CHANNEL_COLOR);
        displayMode = ONE_CHANNEL_COLOR;
        subPanel.add(displayChoice);
        displayChoice.addItemListener(this);
        displayChoice.addKeyListener(win);
        displayChoice.addKeyListener(ij);
        displayChoice.addKeyListener(win);
        selectorPanel.add(subPanel, BorderLayout.NORTH);
        addChannelSelectorOverlay();
        displayGrayInTiles = false;
        addTiledSelectorPanel();
        addScrollbar();
        if (nChannels <= 1) {
            if (scrollbarWL != null) selectorPanel.remove(scrollbarWL);
        }
        cColorChooser = new ChannelColorChooser(this);
    }

    void addScrollbar() {
        nChannels = i5d.getNChannels();
        if (scrollbarWL == null) {
            scrollbarWL = new ScrollbarWithLabel(Scrollbar.VERTICAL, 1, 1, 1, nChannels + 1, i5d.getDimensionLabel(2));
            scrollbarWL.addAdjustmentListener(this);
            scrollbarWL.setFocusable(false);
        } else {
            scrollbarWL.setMaximum(nChannels + 1);
        }
        int blockIncrement = nChannels / 10;
        if (blockIncrement < 1) blockIncrement = 1;
        scrollbarWL.setUnitIncrement(1);
        scrollbarWL.setBlockIncrement(blockIncrement);
        if (channelSelectorOverlay != null) selectorPanel.remove(channelSelectorOverlay);
        if (tiledSelectorPanel != null) selectorPanel.remove(tiledSelectorPanel);
        selectorPanel.add(scrollbarWL, BorderLayout.CENTER);
        win.pack();
        selectorPanel.repaint();
    }

    void addChannelSelectorOverlay() {
        if (channelSelectorOverlay == null) {
            channelSelectorOverlay = new ChannelSelectorOverlay(this);
        }
        if (scrollbarWL != null) selectorPanel.remove(tiledSelectorPanel);
        if (tiledSelectorPanel != null) selectorPanel.remove(scrollbarWL);
        selectorPanel.add(channelSelectorOverlay, BorderLayout.CENTER);
        win.pack();
        selectorPanel.repaint();
    }

    void addTiledSelectorPanel() {
        if (channelSelectorOverlay == null) {
            channelSelectorOverlay = new ChannelSelectorOverlay(this);
        }
        if (tiledSelectorPanel == null) {
            tiledSelectorPanel = new Panel(new BorderLayout());
        }
        if (allGrayInTilesBox == null) {
            allGrayInTilesBox = new Checkbox("all gray", displayGrayInTiles);
            allGrayInTilesBox.addItemListener(this);
            allGrayInTilesBox.addKeyListener(win);
            allGrayInTilesBox.addKeyListener(ij);
            allGrayInTilesBox.addKeyListener(win);
        }
        if (channelSelectorOverlay != null) selectorPanel.remove(channelSelectorOverlay);
        if (scrollbarWL != null) selectorPanel.remove(scrollbarWL);
        tiledSelectorPanel.add(allGrayInTilesBox, BorderLayout.NORTH);
        tiledSelectorPanel.add(channelSelectorOverlay, BorderLayout.CENTER);
        selectorPanel.add(tiledSelectorPanel, BorderLayout.CENTER);
        win.pack();
        selectorPanel.repaint();
    }

    public int getNChannels() {
        return nChannels;
    }

    public void setNChannels(int nChannels) {
        if (nChannels < 1) return;
        if (scrollbarWL != null) {
            int max = scrollbarWL.getMaximum();
            if (max != (nChannels + 1)) scrollbarWL.setMaximum(nChannels + 1);
        }
    }

    public int getCurrentChannel() {
        return currentChannel;
    }

    public Dimension getMinimumSize() {
        int width = selectorPanel.getMinimumSize().width;
        if (colorChooserDisplayed) {
            width += cColorChooser.getMinimumSize().width;
        }
        int height = selectorPanel.getMinimumSize().height;
        if (colorChooserDisplayed) {
            height = Math.max(height, cColorChooser.getMinimumSize().height);
        }
        if (height > 500) height = 350;
        return new Dimension(width, height);
    }

    public Dimension getPreferredSize() {
        int width = selectorPanel.getPreferredSize().width;
        if (colorChooserDisplayed) {
            width += cColorChooser.getPreferredSize().width;
        }
        int height = selectorPanel.getPreferredSize().height;
        if (colorChooserDisplayed) {
            height = Math.max(height, cColorChooser.getPreferredSize().height);
        }
        return new Dimension(width, height);
    }

    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == displayChoice) {
            if (displayChoice.getSelectedIndex() == displayMode) {
                return;
            }
            i5d.setDisplayMode(displayChoice.getSelectedIndex());
            updateChannelSelector();
        } else if (e.getSource() == allGrayInTilesBox) {
            if (allGrayInTilesBox.getState() == displayGrayInTiles) {
                return;
            }
            displayGrayInTiles = allGrayInTilesBox.getState();
            i5d.setDisplayGrayInTiles(displayGrayInTiles);
        }
    }

    public synchronized void updateSelectorDisplay() {
        nChannels = i5d.getNChannels();
        if (nChannels <= 1) {
            selectorPanel.remove(scrollbarWL);
            selectorPanel.remove(channelSelectorOverlay);
            selectorPanel.remove(tiledSelectorPanel);
        } else if (displayMode == OVERLAY) {
            addChannelSelectorOverlay();
        } else if (displayMode == TILED) {
            addTiledSelectorPanel();
        } else {
            addScrollbar();
        }
    }

    /** Sets the displayMode as seen in the channelControl. Doesn't do anything to the Image5D.
	 */
    public synchronized void setDisplayMode(int mode) {
        if (mode == displayMode) {
            return;
        }
        if (mode == ONE_CHANNEL_GRAY) {
            if (colorChooserDisplayed) {
                remove(cColorChooser);
                colorButton.setLabel(BUTTON_ACTIVATE_COLOR_CHOOSER);
                colorChooserDisplayed = false;
            }
            colorButton.setEnabled(false);
            if (nChannels > 1) {
                addScrollbar();
            }
            displayMode = mode;
        } else if (mode == ONE_CHANNEL_COLOR) {
            colorButton.setEnabled(true);
            if (nChannels > 1) {
                addScrollbar();
            }
            displayMode = mode;
        } else if (mode == OVERLAY) {
            colorButton.setEnabled(true);
            if (nChannels > 1) {
                addChannelSelectorOverlay();
            }
            displayMode = mode;
        } else if (mode == TILED) {
            colorButton.setEnabled(true);
            if (nChannels > 1) {
                addTiledSelectorPanel();
            }
            displayMode = mode;
        } else {
            throw new IllegalArgumentException("Unknown display mode: " + mode);
        }
        displayChoice.select(mode);
    }

    public int getDisplayMode() {
        return displayMode;
    }

    public synchronized void setDisplayGrayInTiles(boolean displayGrayInTiles) {
        if (this.displayGrayInTiles == displayGrayInTiles && allGrayInTilesBox != null && allGrayInTilesBox.getState() == displayGrayInTiles) {
            return;
        }
        this.displayGrayInTiles = displayGrayInTiles;
        if (allGrayInTilesBox != null) {
            allGrayInTilesBox.setState(displayGrayInTiles);
        }
    }

    public boolean isDisplayGrayInTiles() {
        return displayGrayInTiles;
    }

    /** Executed if scrollbar has been moved.
	 * 
	 */
    public void adjustmentValueChanged(AdjustmentEvent e) {
        if (scrollbarWL != null && e.getSource() == scrollbarWL) {
            currentChannel = scrollbarWL.getValue();
            channelChanged(currentChannel);
        }
    }

    /** Called by ChannelSelectorOverlay if current channel has been changed.
	 * 
	 */
    public void channelChanged(int newChannel) {
        currentChannel = newChannel;
        win.channelChanged();
        cColorChooser.channelChanged();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == colorButton) {
            if (colorChooserDisplayed) {
                remove(cColorChooser);
                colorButton.setLabel(BUTTON_ACTIVATE_COLOR_CHOOSER);
                colorChooserDisplayed = false;
                win.pack();
            } else {
                add(cColorChooser, BorderLayout.EAST);
                colorButton.setLabel(BUTTON_DEACTIVATE_COLOR_CHOOSER);
                colorChooserDisplayed = true;
                win.pack();
            }
        }
    }

    /** Updates nChannels, currentChannel and colors of channels from values in Image5D. 
	 */
    public void updateChannelSelector() {
        currentChannel = i5d.getCurrentChannel();
        nChannels = i5d.getNChannels();
        int max = scrollbarWL.getMaximum();
        if (max != (nChannels + 1)) scrollbarWL.setMaximum(nChannels + 1);
        scrollbarWL.setValue(i5d.getCurrentChannel());
        channelSelectorOverlay.updateFromI5D();
        cColorChooser.channelChanged();
    }

    /** Contains a radio button and a check button for each channel to select the current channel
	 * and toggle display of this channel. The background color of the buttons corresponds to 
	 * the color of the channel (highest color in colormap).
	 * Also has a button to select all channels for display and deselect all.
	 * 
	 * @author Joachim Walter 
	 */
    protected class ChannelSelectorOverlay extends Panel implements ActionListener, ItemListener {

        /**
		 * 
		 */
        private static final long serialVersionUID = -8138803830180530129L;

        ChannelControl cControl;

        Panel allNonePanel;

        Button allButton;

        Button noneButton;

        ScrollPane channelPane;

        Panel channelPanel;

        Checkbox[] channelDisplayed;

        Checkbox[] channelActive;

        CheckboxGroup channelActiveGroup;

        int nChannels;

        public ChannelSelectorOverlay(ChannelControl cControl) {
            super(new BorderLayout());
            this.cControl = cControl;
            nChannels = cControl.nChannels;
            allNonePanel = new Panel(new GridLayout(1, 2));
            allButton = new Button("All");
            allButton.addActionListener(this);
            allNonePanel.add(allButton);
            noneButton = new Button("None");
            noneButton.addActionListener(this);
            allNonePanel.add(noneButton);
            add(allNonePanel, BorderLayout.NORTH);
            addKeyListener(win);
            allButton.addKeyListener(win);
            noneButton.addKeyListener(win);
            addKeyListener(ij);
            allButton.addKeyListener(ij);
            noneButton.addKeyListener(ij);
            addKeyListener(win);
            allButton.addKeyListener(win);
            noneButton.addKeyListener(win);
            buildChannelSelector();
        }

        protected void buildChannelSelector() {
            channelPane = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
            channelPanel = new Panel();
            channelPane.add(channelPanel);
            add(channelPane, BorderLayout.CENTER);
            channelPane.addMouseWheelListener(win);
            channelPane.addKeyListener(win);
            channelPanel.addKeyListener(win);
            channelPane.addKeyListener(ij);
            channelPanel.addKeyListener(ij);
            channelPane.addKeyListener(win);
            channelPanel.addKeyListener(win);
            nChannels = 0;
            updateFromI5D();
        }

        public void updateFromI5D() {
            if (nChannels != i5d.getNChannels()) {
                nChannels = i5d.getNChannels();
                channelPane.getVAdjustable().setUnitIncrement(20);
                channelPanel.removeAll();
                channelPanel.setLayout(new GridLayout(nChannels, 1));
                channelActiveGroup = new CheckboxGroup();
                channelDisplayed = new Checkbox[nChannels];
                channelActive = new Checkbox[nChannels];
                for (int i = 0; i < nChannels; ++i) {
                    Panel p = new Panel(new BorderLayout());
                    channelActive[i] = new Checkbox();
                    channelActive[i].setCheckboxGroup(channelActiveGroup);
                    channelActive[i].addItemListener(this);
                    p.add(channelActive[i], BorderLayout.WEST);
                    channelDisplayed[i] = new Checkbox();
                    channelDisplayed[i].addItemListener(this);
                    p.add(channelDisplayed[i], BorderLayout.CENTER);
                    channelPanel.add(p);
                    channelActive[i].addKeyListener(win);
                    channelDisplayed[i].addKeyListener(win);
                    channelActive[i].addKeyListener(ij);
                    channelDisplayed[i].addKeyListener(ij);
                    channelActive[i].addKeyListener(win);
                    channelDisplayed[i].addKeyListener(win);
                }
                win.pack();
            }
            for (int i = 0; i < nChannels; ++i) {
                channelActive[i].setBackground(new Color(i5d.getChannelDisplayProperties(i + 1).getColorModel().getRGB(255)));
                channelActive[i].setForeground(Color.black);
                channelActive[i].repaint();
                channelDisplayed[i].setLabel(i5d.getChannelCalibration(i + 1).getLabel());
                channelDisplayed[i].setState(i5d.getChannelDisplayProperties(i + 1).isDisplayedInOverlay());
                channelDisplayed[i].setBackground(new Color(i5d.getChannelDisplayProperties(i + 1).getColorModel().getRGB(255)));
                channelDisplayed[i].setForeground(Color.black);
                channelDisplayed[i].repaint();
            }
            channelActive[(i5d.getCurrentChannel() - 1)].setState(true);
        }

        public Dimension getMinimumSize() {
            int widthButtons = allNonePanel.getMinimumSize().width;
            int widthChecks = channelPane.getMinimumSize().width;
            int width = Math.max(widthButtons, widthChecks);
            int height = allNonePanel.getMinimumSize().height;
            height += nChannels * channelDisplayed[0].getMinimumSize().height;
            return new Dimension(width, height);
        }

        public Dimension getPreferredSize() {
            int widthButtons = allNonePanel.getPreferredSize().width;
            int widthChecks = channelPane.getPreferredSize().width;
            int width = Math.max(widthButtons, widthChecks);
            int height = allNonePanel.getPreferredSize().height;
            height += nChannels * channelDisplayed[0].getPreferredSize().height;
            return new Dimension(width, height);
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == allButton) {
                for (int i = 0; i < nChannels; ++i) {
                    channelDisplayed[i].setState(true);
                    i5d.setDisplayedInOverlay(i + 1, true);
                }
                updateFromI5D();
                i5d.updateImageAndDraw();
            } else if (e.getSource() == noneButton) {
                for (int i = 0; i < nChannels; ++i) {
                    channelDisplayed[i].setState(false);
                    i5d.setDisplayedInOverlay(i + 1, false);
                }
                updateFromI5D();
                i5d.updateImageAndDraw();
            }
        }

        public void itemStateChanged(ItemEvent e) {
            for (int i = 0; i < nChannels; ++i) {
                if (e.getItemSelectable() == channelDisplayed[i]) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        i5d.setDisplayedInOverlay(i + 1, true);
                    } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                        i5d.setDisplayedInOverlay(i + 1, false);
                    }
                    updateFromI5D();
                    i5d.updateImageAndDraw();
                    return;
                }
                if (e.getItemSelectable() == channelActive[i]) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        currentChannel = i + 1;
                        cControl.channelChanged(currentChannel);
                        return;
                    }
                }
            }
        }
    }

    /** Panel to select the display color or colormap of the current channel.
	 * 	Contains the ChannelColorCanvas, where the user can select a color. 
	 * 	There is a checkbox "displayGrayBox" to temporarily switch the channel to 
	 * 	grayscale display without losing the display color.
	 * 	Pressing the Button "editLUTButton" opens the builtin LUT_Editor plugin for 
	 * 	defining more complex LUTs.
	 * 
	 * @author Joachim Walter
	 */
    protected class ChannelColorChooser extends Panel implements ActionListener, ItemListener {

        /**
		 * 
		 */
        private static final long serialVersionUID = -2987819237979841674L;

        ChannelControl cControl;

        Checkbox displayGrayBox;

        Button editColorButton;

        Button editLUTButton;

        ChannelColorCanvas cColorCanvas;

        public ChannelColorChooser(ChannelControl cControl) {
            super(new BorderLayout(5, 5));
            this.cControl = cControl;
            displayGrayBox = new Checkbox("Grayscale");
            displayGrayBox.setState(i5d.getChannelDisplayProperties(cControl.currentChannel).isDisplayedGray());
            displayGrayBox.addItemListener(this);
            add(displayGrayBox, BorderLayout.NORTH);
            cColorCanvas = new ChannelColorCanvas(this);
            add(cColorCanvas, BorderLayout.CENTER);
            Panel tempPanel = new Panel(new GridLayout(2, 1, 5, 2));
            editColorButton = new Button("Edit Color");
            editColorButton.addActionListener(this);
            tempPanel.add(editColorButton);
            editLUTButton = new Button("Edit LUT");
            editLUTButton.addActionListener(this);
            tempPanel.add(editLUTButton);
            add(tempPanel, BorderLayout.SOUTH);
            addKeyListener(win);
            displayGrayBox.addKeyListener(win);
            cColorCanvas.addKeyListener(win);
            editColorButton.addKeyListener(win);
            editLUTButton.addKeyListener(win);
            addKeyListener(ij);
            displayGrayBox.addKeyListener(ij);
            cColorCanvas.addKeyListener(ij);
            editColorButton.addKeyListener(ij);
            editLUTButton.addKeyListener(ij);
            addKeyListener(win);
            displayGrayBox.addKeyListener(win);
            cColorCanvas.addKeyListener(win);
            editColorButton.addKeyListener(win);
            editLUTButton.addKeyListener(win);
        }

        /** Sets the Color of the current channel to c in the Image5D.
	     * 
	     * @param c: Color
	     */
        public void setColor(Color c) {
            i5d.storeChannelProperties(cControl.currentChannel);
            i5d.getChannelDisplayProperties(cControl.currentChannel).setColorModel(ChannelDisplayProperties.createModelFromColor(c));
            i5d.restoreChannelProperties(cControl.currentChannel);
            i5d.updateAndDraw();
            cControl.updateChannelSelector();
            cColorCanvas.repaint();
        }

        /** Called from ChannelControl when active channel changes.
         */
        public void channelChanged() {
            cColorCanvas.repaint();
            displayGrayBox.setState(i5d.getChannelDisplayProperties(cControl.currentChannel).isDisplayedGray());
            cColorCanvas.setEnabled(!displayGrayBox.getState());
            editColorButton.setEnabled(!displayGrayBox.getState());
            editLUTButton.setEnabled(!displayGrayBox.getState());
        }

        public Dimension getMinimumSize() {
            int width = cColorCanvas.getMinimumSize().width;
            int height = displayGrayBox.getMinimumSize().height + 5 + cColorCanvas.getMinimumSize().height + 5 + editColorButton.getMinimumSize().height + 5 + editLUTButton.getMinimumSize().height;
            return new Dimension(width, height);
        }

        public Dimension getPreferredSize() {
            int width = cColorCanvas.getPreferredSize().width;
            int height = displayGrayBox.getPreferredSize().height + 5 + cColorCanvas.getPreferredSize().height + 5 + editColorButton.getPreferredSize().height + 5 + editLUTButton.getPreferredSize().height;
            return new Dimension(width, height);
        }

        public void actionPerformed(ActionEvent e) {
            ColorModel cm = i5d.getChannelDisplayProperties(cControl.currentChannel).getColorModel();
            i5d.storeChannelProperties(cControl.currentChannel);
            if (e.getSource() == editLUTButton) {
                new LUT_Editor().run("");
                cm = i5d.getProcessor(cControl.currentChannel).getColorModel();
            } else if (e.getSource() == editColorButton) {
                Color c = new Color(i5d.getProcessor(cControl.currentChannel).getColorModel().getRGB(255));
                ColorChooser cc = new ColorChooser("Channel Color", c, false);
                c = cc.getColor();
                if (c != null) {
                    cm = ChannelDisplayProperties.createModelFromColor(c);
                }
            }
            i5d.getChannelDisplayProperties(cControl.currentChannel).setColorModel(cm);
            i5d.restoreChannelProperties(cControl.currentChannel);
            i5d.updateAndDraw();
            cControl.updateChannelSelector();
            cColorCanvas.repaint();
        }

        public void itemStateChanged(ItemEvent e) {
            if (e.getItemSelectable() == displayGrayBox) {
                i5d.storeChannelProperties(cControl.currentChannel);
                i5d.getChannelDisplayProperties(cControl.currentChannel).setDisplayedGray(displayGrayBox.getState());
                i5d.restoreChannelProperties(cControl.currentChannel);
                i5d.updateImageAndDraw();
                cColorCanvas.setEnabled(!displayGrayBox.getState());
                editColorButton.setEnabled(!displayGrayBox.getState());
                editLUTButton.setEnabled(!displayGrayBox.getState());
            }
        }
    }

    /**
 * Canvas to select the display color of a channel.
 * The Canvas contains a rectangle of colored squares. The hue of the squares changes
 * in vertical direction. The saturation in horizontal direction. "Value" can be changed
 * by changing contrast and brightness of the channel.
 * The Canvas also contains an image of the current LUT. 
 *  
 * @author Joachim Walter
 */
    protected class ChannelColorCanvas extends Canvas implements MouseListener {

        /**
	 * 
	 */
        private static final long serialVersionUID = 3918313955836224104L;

        ChannelColorChooser cColorChooser;

        Image lutImage;

        Image colorPickerImage;

        Image commonsImage;

        int lutWidth = 15;

        int lutHeight = 129;

        int cpNHues = 24;

        int cpNSats = 8;

        int cpRectWidth = 10;

        int cpRectHeight = 10;

        int cpWidth;

        int cpHeight;

        int commonsWidth = 0;

        int commonsHeight = 0;

        int hSpacer = 5;

        int vSpacer = 0;

        int canvasWidth;

        int canvasHeight;

        public ChannelColorCanvas(ChannelColorChooser cColorChooser) {
            super();
            this.cColorChooser = cColorChooser;
            setForeground(Color.black);
            setBackground(Color.white);
            cpWidth = cpRectWidth * cpNSats;
            cpHeight = cpRectHeight * cpNHues;
            canvasWidth = lutWidth + 2 + hSpacer + cpWidth + 2;
            canvasHeight = cpHeight + 2 + vSpacer + commonsHeight;
            setSize(canvasWidth, canvasHeight);
            lutImage = GUI.createBlankImage(lutWidth + 2, lutHeight + 2);
            colorPickerImage = GUI.createBlankImage(cpWidth + 2, cpHeight + 2);
            drawLUT();
            drawColorPicker();
            addMouseListener(this);
        }

        /** Draws the LUT of the currently displayed channel into a rectangle.
	     */
        void drawLUT() {
            Graphics g = lutImage.getGraphics();
            int[] rgb = new int[256];
            ColorModel currentLUT = i5d.getChannelDisplayProperties(cColorChooser.cControl.currentChannel).getColorModel();
            if (!(currentLUT instanceof IndexColorModel)) throw new IllegalArgumentException("Color Model has to be IndexColorModel.");
            for (int i = 0; i < 256; i++) {
                rgb[i] = ((IndexColorModel) currentLUT).getRGB(i);
            }
            g.setColor(Color.black);
            g.drawRect(0, 0, lutWidth + 2, lutHeight + 2);
            g.setColor(new Color(rgb[255]));
            g.drawLine(1, 1, lutWidth, 1);
            for (int i = 0; i < 128; i++) {
                g.setColor(new Color(rgb[255 - i * 2]));
                g.drawLine(1, i + 2, lutWidth, i + 2);
            }
        }

        /** Draws the color picker image: a rectangle of small squares all with different hue or saturation.
	     */
        void drawColorPicker() {
            Graphics g = colorPickerImage.getGraphics();
            float hue = 0;
            float sat = 0;
            g.setColor(Color.black);
            g.drawRect(0, 0, cpWidth + 2, cpHeight + 2);
            for (int h = 0; h < cpNHues; h++) {
                for (int s = 0; s < cpNSats; s++) {
                    hue = h / (float) cpNHues;
                    sat = s / (float) (cpNSats - 1);
                    g.setColor(Color.getHSBColor(hue, sat, 1f));
                    g.fillRect(cpWidth - (s + 1) * cpRectWidth + 1, h * cpRectHeight + 1, cpRectWidth, cpRectHeight);
                }
            }
        }

        public void paint(Graphics g) {
            g.drawImage(colorPickerImage, lutWidth + 2 + hSpacer, 0, cpWidth + 2, cpHeight + 2, null);
            drawLUT();
            g.drawImage(lutImage, 0, 0, lutWidth + 2, lutHeight + 2, null);
        }

        public Dimension getMinimumSize() {
            return new Dimension(canvasWidth, canvasHeight);
        }

        public Dimension getPreferredSize() {
            return new Dimension(canvasWidth, canvasHeight);
        }

        /** Change color, if user presses in one of the colored squares.
         */
        public void mousePressed(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            Rectangle colorPickerRect = new Rectangle(lutWidth + 2 + hSpacer + 1, 1, cpWidth, cpHeight);
            if (colorPickerRect.contains(x, y)) {
                int xCP = x - (lutWidth + 2 + hSpacer);
                int yCP = y;
                int[] pixels32 = new int[1];
                PixelGrabber pg = new PixelGrabber(colorPickerImage, xCP, yCP, 1, 1, pixels32, 0, cpWidth);
                pg.setColorModel(ColorModel.getRGBdefault());
                try {
                    pg.grabPixels();
                } catch (InterruptedException ie) {
                    return;
                }
                Color c = new Color(pixels32[0]);
                cColorChooser.setColor(c);
                if (e.getClickCount() == 2) {
                    ColorChooser cc = new ColorChooser("Channel Color", c, false);
                    c = cc.getColor();
                    if (c != null) {
                        cColorChooser.setColor(c);
                    }
                }
            }
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }
    }
}
