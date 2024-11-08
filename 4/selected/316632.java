package de.schwarzrot.epgmgr.app.view;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.springframework.context.MessageSource;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import com.jgoodies.binding.PresentationModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.schwarzrot.app.support.ApplicationServiceProvider;
import de.schwarzrot.ui.service.FormComponentFactory;
import de.schwarzrot.vdr.data.domain.EpgEvent;

public class EpgDetails implements ListSelectionListener {

    private static FormComponentFactory formFactory;

    private static MessageSource msgSource;

    public EpgDetails(DefaultEventSelectionModel<EpgEvent> model) {
        this.model = model;
        instance = new EpgEvent();
        readOnly = true;
        this.model.addListSelectionListener(this);
        pm = new PresentationModel<EpgEvent>(instance);
    }

    public JComponent buildPanel() {
        if (formFactory == null) formFactory = ApplicationServiceProvider.getService(FormComponentFactory.class);
        if (msgSource == null) msgSource = ApplicationServiceProvider.getService(MessageSource.class);
        channel = new JTextField();
        JFormattedTextField day = formFactory.createDateField(pm.getModel("begin"));
        JFormattedTextField when = formFactory.createTimeField(pm.getModel("begin"));
        JFormattedTextField vps = formFactory.createDateTimeField(pm.getModel("vpsBegin"));
        JTextField duration = formFactory.createIntegerField(pm.getModel("duration"));
        JTextField title = formFactory.createTextField(pm.getModel("title"));
        JTextField subTitle = formFactory.createTextField(pm.getModel("subTitle"));
        JTextField videoMode = formFactory.createTextField(pm.getModel("videoMode"));
        JCheckBox withAC3 = formFactory.createCheckBox(pm.getModel("withAC3"), msgSource.getMessage("EpgEvent.withAC3", null, "withAC3", null));
        JTextArea description = formFactory.createTextArea(pm.getModel("description"));
        if (readOnly) {
            channel.setEditable(false);
            day.setEditable(false);
            when.setEditable(false);
            vps.setEditable(false);
            duration.setEditable(false);
            title.setEditable(false);
            subTitle.setEditable(false);
            videoMode.setEditable(false);
            withAC3.setEnabled(false);
            description.setEditable(false);
        }
        description.setRows(5);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        FormLayout layout = new FormLayout("max(40dlu;pref), 3dlu, max(60dlu;pref), 3dlu, max(20dlu;pref), 5dlu, max(20dlu;pref), " + "5dlu, max(20dlu;pref), 5dlu, fill:default:grow", "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, fill:max(50dlu;p)");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();
        CellConstraints cc = new CellConstraints();
        builder.addLabel(msgSource.getMessage("EpgEvent.event", null, "EpgEvent.event", null), cc.xy(1, 1));
        builder.add(day, cc.xy(3, 1));
        builder.add(when, cc.xy(5, 1));
        builder.add(duration, cc.xy(7, 1));
        builder.add(channel, cc.xyw(9, 1, 3));
        builder.addLabel(msgSource.getMessage("EpgEvent.title", null, "EpgEvent.title", null), cc.xy(1, 3));
        builder.add(title, cc.xyw(3, 3, 9));
        builder.addLabel(msgSource.getMessage("EpgEvent.subTitle", null, "EpgEvent.subTitle", null), cc.xy(1, 5));
        builder.add(subTitle, cc.xyw(3, 5, 9));
        builder.addLabel(msgSource.getMessage("EpgEvent.videoMode", null, "EpgEvent.videoMode", null), cc.xy(1, 7));
        builder.add(videoMode, cc.xy(3, 7));
        builder.addLabel(msgSource.getMessage("EpgEvent.audioMode", null, "EpgEvent.audioMode", null), cc.xy(5, 7, "right, center"));
        builder.add(withAC3, cc.xy(7, 7));
        builder.addLabel(msgSource.getMessage("EpgEvent.vps", null, "EpgEvent.vps", null), cc.xy(9, 7, "right, center"));
        builder.add(vps, cc.xy(11, 7));
        builder.addLabel(msgSource.getMessage("EpgEvent.description", null, "EpgEvent.description", null), cc.xy(1, 9, "left, top"));
        builder.add(new JScrollPane(description), cc.xyw(3, 9, 9));
        validate();
        return builder.getPanel();
    }

    public final EpgEvent getInstance() {
        return instance;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        EventList<EpgEvent> list = model.getSelected();
        if (list.size() > 0) {
            EpgEvent evt = list.get(0);
            channel.setText(evt.getChannel().getName());
            pm.setBean(evt);
            instance = evt;
        }
    }

    protected void validate() {
        if (model.getSelected() == null || model.getSelected().size() < 1) {
            try {
                model.addSelectionInterval(0, 0);
            } catch (Exception e) {
            }
        }
    }

    private DefaultEventSelectionModel<EpgEvent> model;

    private EpgEvent instance;

    private PresentationModel<EpgEvent> pm;

    private JTextField channel;

    private boolean readOnly;
}
