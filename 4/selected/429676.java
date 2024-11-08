package be.lassi.ui.control;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import be.lassi.context.ShowContext;
import be.lassi.domain.Channel;
import be.lassi.domain.Channels;
import be.lassi.domain.Control;
import be.lassi.domain.Memory;
import be.lassi.ui.base.BasicFrame;
import be.lassi.ui.widgets.TimedSlider;
import be.lassi.util.Dmx;
import be.lassi.util.NLS;

/**
 *
 *
 */
public abstract class ControlFrame extends BasicFrame {

    /**
     * @see be.lassi.ui.basic.BasicFrame#BasicFrame(ShowContext, String)
     */
    public ControlFrame(final ShowContext context, final String title) {
        super(context, title);
        init();
        setResizable(false);
    }

    /**
     * {@inheritDoc}
     */
    protected JComponent createPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        initPresets(panel, c);
        initButtons(panel, c);
        initSliders(panel, c);
        return panel;
    }

    /**
     * Method createPresetPanel.
     * @param preset
     * @return JComponent
     */
    private JComponent createPresetPanel(final Memory preset) {
        return new PanelPreset(preset);
    }

    /**
     * Method createSliderMaster.
     * @return JComponent
     */
    private JComponent createSliderMaster() {
        TimedSlider slider = new TimedSlider(NLS.get("control.master"), getControl().getMaster());
        getContext().getKernel().addClockListener(getControl().getMaster());
        return slider;
    }

    /**
     * Method createSliderXFade.
     * @return JComponent
     */
    private JComponent createSliderXFade() {
        TimedSlider slider = new TimedSlider(NLS.get("control.crossfade"), getControl().getXFade());
        getContext().getKernel().addClockListener(getControl().getXFade());
        return slider;
    }

    /**
     * Method getControl.
     * @return Control
     */
    protected abstract Control getControl();

    /**
     * Return true if the output panel (with level indicators) needs
     * to be displayed.  The default is to not display the output panel,
     * subclasses can override this behavior.
     * 
     * @return boolean
     */
    protected boolean includeOutputPanel() {
        return false;
    }

    /**
     * Method initButtons.
     * @param panel
     * @param c
     */
    private void initButtons(final JPanel panel, final GridBagConstraints c) {
        c.gridx = 1;
        c.weightx = 0;
        c.gridy = includeOutputPanel() ? 2 : 1;
        panel.add(new ButtonPanelPresetA(getControl()), c);
        c.gridy++;
        panel.add(new ButtonPanelPresetB(getControl()), c);
    }

    /**
     * Add the preset-panels to given panel.
     * 
     * @param parent panel
     * @param c
     */
    private void initPresets(final JPanel panel, final GridBagConstraints c) {
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        if (includeOutputPanel()) {
            panel.add(new ControlOutputPanel(getContext().getLanbox().getMixer().getLevels()), c);
            c.gridy++;
        }
        if (includeOutputPanel()) {
            List<Channel> subset = new ArrayList<Channel>();
            Channels channels = getShow().getChannels();
            for (int i = 0; i < Dmx.CONTROL_CHANNELS; i++) {
                Channel channel = channels.get(i);
                subset.add(channel);
            }
            Channels controlChannels = new Channels(subset);
            panel.add(new ControlLabelPanel(controlChannels), c);
        } else {
            panel.add(new ControlLabelPanel(getShow().getSubmasters()), c);
        }
        c.gridy++;
        panel.add(createPresetPanel(getControl().getPresetA()), c);
        c.gridy++;
        panel.add(createPresetPanel(getControl().getPresetB()), c);
    }

    /**
     * Add crossfade and master sliders to given panel.
     * 
     * @param parent panel
     * @param c
     */
    private void initSliders(final JPanel panel, final GridBagConstraints c) {
        c.gridy = 0;
        c.weighty = 1;
        c.fill = GridBagConstraints.VERTICAL;
        c.gridheight = includeOutputPanel() ? 4 : 3;
        c.gridx = 2;
        panel.add(createSliderXFade(), c);
        c.gridx = 3;
        panel.add(createSliderMaster(), c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isObsolete() {
        return true;
    }
}
