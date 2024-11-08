package free.jin.console.ics;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.SwingConstants;
import org.jdesktop.layout.GroupLayout;
import org.jdesktop.layout.LayoutStyle;
import free.jin.BadChangesException;
import free.jin.I18n;
import free.jin.Preferences;
import free.jin.console.Channel;
import free.jin.console.ConsoleManager;
import free.jin.console.prefs.CustomConsolesPrefsPanel;
import free.util.TextUtilities;

/**
 * A common base <code>CustomConsolesPrefsPanel</code> for ICS-based servers.
 * 
 * @author Maryanovsky Alexander
 */
public class IcsCustomConsolesPrefsPanel extends CustomConsolesPrefsPanel {

    /**
   * A checkbox allowing the user to include shouts in the console.
   */
    private final JCheckBox shoutsBox;

    /**
   * A checkbox allowing the user to include c-shouts in the console.
   * Note that on ICC, they are called "s-shouts". Ideally, we'd implement
   * c-shouts in a FICS-specific subclass and s-shouts in an ICC-specific
   * subclass, but this is really too much (unnecessary) work.
   */
    private final JCheckBox cshoutsBox;

    /**
   * Creates a new <code>IcsCustomConsolesPrefsPanel</code> for the
   * specified console manager. 
   */
    public IcsCustomConsolesPrefsPanel(ConsoleManager cm) {
        super(cm);
        I18n i18n = I18n.get(IcsCustomConsolesPrefsPanel.class);
        this.shoutsBox = i18n.createCheckBox("shoutsBox");
        this.cshoutsBox = I18n.get(getClass(), IcsCustomConsolesPrefsPanel.class).createCheckBox("cshoutsBox");
        ItemListener changeFiringItemListener = new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (!isIgnoreConsolePropertiesChange()) fireStateChanged();
            }
        };
        shoutsBox.addItemListener(changeFiringItemListener);
        cshoutsBox.addItemListener(changeFiringItemListener);
    }

    /**
   * {@inheritDoc}
   */
    protected void createLayout() {
        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setAutocreateContainerGaps(true);
        layout.setHonorsVisibility(true);
        layout.setHorizontalGroup(layout.createParallelGroup().add(consolesListLabel).add(layout.createSequentialGroup().add(consolesListScrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).addPreferredGap(LayoutStyle.RELATED).add(layout.createParallelGroup(GroupLayout.LEADING, false).add(addConsoleButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).add(removeConsoleButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).add(moveUpButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).add(moveDownButton, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))).add(layout.createSequentialGroup().add(LayoutStyle.getSharedInstance().getPreferredGap(consolesList, titleLabel, LayoutStyle.INDENT, SwingConstants.SOUTH, this)).add(layout.createParallelGroup(GroupLayout.TRAILING).add(titleLabel).add(windowLabel).add(encodingLabel).add(channelsLabel).add(messageRegexLabel)).addPreferredGap(LayoutStyle.RELATED).add(layout.createParallelGroup().add(titleField).add(windowBox).add(encodingBox).add(channelsField).add(messageRegexField).add(layout.createSequentialGroup().add(shoutsBox).addPreferredGap(LayoutStyle.UNRELATED).add(cshoutsBox))).addPreferredGap(LayoutStyle.RELATED).add(layout.createParallelGroup().add(addRemoveChannels))));
        layout.setVerticalGroup(layout.createSequentialGroup().add(consolesListLabel).addPreferredGap(LayoutStyle.RELATED).add(layout.createParallelGroup(GroupLayout.LEADING).add(consolesListScrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).add(layout.createSequentialGroup().add(addConsoleButton).add(removeConsoleButton).addPreferredGap(LayoutStyle.UNRELATED).add(moveUpButton).add(moveDownButton))).addPreferredGap(LayoutStyle.UNRELATED).add(layout.createParallelGroup(GroupLayout.BASELINE).add(titleLabel).add(titleField)).addPreferredGap(LayoutStyle.RELATED).add(layout.createParallelGroup(GroupLayout.BASELINE).add(windowLabel).add(windowBox)).addPreferredGap(LayoutStyle.RELATED).add(layout.createParallelGroup(GroupLayout.BASELINE).add(encodingLabel).add(encodingBox)).addPreferredGap(LayoutStyle.RELATED).add(layout.createParallelGroup(GroupLayout.BASELINE).add(channelsLabel).add(channelsField).add(addRemoveChannels)).addPreferredGap(LayoutStyle.RELATED).add(layout.createParallelGroup(GroupLayout.BASELINE).add(messageRegexLabel).add(messageRegexField)).addPreferredGap(LayoutStyle.RELATED).add(layout.createParallelGroup(GroupLayout.BASELINE).add(shoutsBox).add(cshoutsBox)));
    }

    /**
   * {@inheritDoc}
   */
    protected ConsoleSpec createNewConsoleSpec() {
        return new IcsConsoleSpec();
    }

    /**
   * {@inheritDoc}
   */
    protected void updateUiFromSelectedConsole() {
        super.updateUiFromSelectedConsole();
        try {
            setIgnoreConsolePropertiesChange(true);
            IcsConsoleSpec spec = (IcsConsoleSpec) getSelectedConsole();
            updateShoutsUi(spec);
            updateCShoutsUi(spec);
        } finally {
            setIgnoreConsolePropertiesChange(false);
        }
    }

    /**
   * Updates the shouts UI from the specified selected spec (which may be
   * <code>null</code>, if there is no spec selected).
   */
    private void updateShoutsUi(IcsConsoleSpec spec) {
        shoutsBox.setEnabled(spec != null);
        shoutsBox.setSelected((spec != null) && spec.isIncludeShouts());
    }

    /**
   * Updates the c-shouts UI from the specified selected spec (which may be
   * <code>null</code>, if there is no spec selected).
   */
    private void updateCShoutsUi(IcsConsoleSpec spec) {
        cshoutsBox.setEnabled(spec != null);
        cshoutsBox.setSelected((spec != null) && spec.isIncludeCShouts());
    }

    /**
   * {@inheritDoc}
   */
    protected void updateConsoleFromUi(ConsoleSpec cSpec) throws BadChangesException {
        super.updateConsoleFromUi(cSpec);
        IcsConsoleSpec spec = (IcsConsoleSpec) cSpec;
        spec.setIncludeShouts(shoutsBox.isSelected());
        spec.setIncludeCShouts(cshoutsBox.isSelected());
    }

    /**
   * {@inheritDoc}
   */
    protected ConsoleSpec loadConsoleSpec(Preferences prefs, String prefix) {
        IcsConsoleSpec spec = (IcsConsoleSpec) super.loadConsoleSpec(prefs, prefix);
        spec.setIncludeShouts(prefs.getBool(prefix + "includeShouts", false));
        spec.setIncludeCShouts(prefs.getBool(prefix + "includeCShouts", false));
        return spec;
    }

    /**
   * {@inheritDoc}
   */
    protected void storeConsoleSpec(Preferences prefs, ConsoleSpec cSpec, String prefix) {
        super.storeConsoleSpec(prefs, cSpec, prefix);
        IcsConsoleSpec spec = (IcsConsoleSpec) cSpec;
        prefs.setBool(prefix + "includeShouts", spec.isIncludeShouts());
        prefs.setBool(prefix + "includeCShouts", spec.isIncludeCShouts());
    }

    /**
   * {@inheritDoc}
   */
    protected String makeChannelListDisplayString(List channels) {
        StringBuffer buffer = new StringBuffer();
        String separator = ", ";
        for (Iterator i = channels.iterator(); i.hasNext(); ) {
            IcsChannel channel = (IcsChannel) i.next();
            buffer.append(channel.getNumber());
            if (i.hasNext()) buffer.append(separator);
        }
        return buffer.toString();
    }

    /**
   * {@inheritDoc}
   */
    protected List parseChannelsListDisplayString(String channelsString) throws BadChangesException {
        Map allChannels = consoleManager.getChannels();
        List channels = new LinkedList();
        String[] channelNumbers = TextUtilities.getTokens(channelsString, ", ");
        for (int i = 0; i < channelNumbers.length; i++) {
            try {
                channels.add(allChannels.get(new Integer(channelNumbers[i].trim())));
            } catch (NumberFormatException e) {
                I18n i18n = I18n.get(IcsCustomConsolesPrefsPanel.class);
                throw new BadChangesException(i18n.getFormattedString("badChannelNumber", new Object[] { channelNumbers[i] }), channelsField);
            }
        }
        return channels;
    }

    /**
   * {@inheritDoc}
   */
    protected String makeChannelPopupString(Channel channel) {
        String id = channel.getId().toString();
        String name = channel.getLongName();
        StringBuffer buf = new StringBuffer(name);
        if (!name.equals(id)) buf.append(" (").append(id).append(")");
        ;
        return buf.toString();
    }

    /**
   * An ICS-specific <code>ConsoleSpec</code>, which adds certain ICS-specific
   * properties.
   */
    protected static class IcsConsoleSpec extends ConsoleSpec {

        /**
     * Whether shouts are displayed in the console.
     */
        private boolean isIncludeShouts = false;

        /**
     * Whether c-shouts are included in the console.
     */
        private boolean isIncludeCShouts = false;

        /**
     * Returns whether shouts are included in the console.
     */
        public boolean isIncludeShouts() {
            return isIncludeShouts;
        }

        /**
     * Sets whether shouts are included in the console.
     */
        public void setIncludeShouts(boolean isIncludeShouts) {
            this.isIncludeShouts = isIncludeShouts;
        }

        /**
     * Returns whether c-shouts are included in the console.
     */
        public boolean isIncludeCShouts() {
            return isIncludeCShouts;
        }

        /**
     * Sets whether c-shouts are included in the console.
     */
        public void setIncludeCShouts(boolean isIncludeCShouts) {
            this.isIncludeCShouts = isIncludeCShouts;
        }
    }
}
