package rafa.midi.saiph.gen.gui;

import rafa.midi.saiph.gen.CMSegmentGenerator;
import rafa.midi.saiph.values.gen.gui.ValuesGeneratorChooser;
import rafa.midi.saiph.values.gen.gui.ValuesGeneratorEditor;

/**
 *
 * @author  rafa
 */
public class CMSegmentGeneratorEditor extends SegmentGeneratorEditor {

    private ValuesGeneratorChooser channelGeneratorChooser;

    /** Creates a new instance of CMSegmentGeneratorEditor */
    public CMSegmentGeneratorEditor(CMSegmentGenerator cmsg) {
        super(cmsg);
        initComponents(cmsg);
    }

    private void initComponents(CMSegmentGenerator cmsg) {
        channelGeneratorChooser = new ValuesGeneratorChooser(cmsg.getChannelGenerator());
        tabs.addTab(rb.getString("CMSegmentGenerator.channelGenerator"), generatorIcon, channelGeneratorChooser);
    }

    public boolean updateGenerator() {
        boolean result = true;
        result = super.updateGenerator();
        if (!result) return result;
        ValuesGeneratorEditor channelEditor = channelGeneratorChooser.getCurrentEditor();
        result = channelEditor.updateGenerator();
        if (!result) {
            tabs.setSelectedComponent(channelGeneratorChooser);
            getToolkit().beep();
            return result;
        }
        CMSegmentGenerator cmsg = (CMSegmentGenerator) segmentGenerator;
        cmsg.setChannelGenerator(channelEditor.getValuesGenerator());
        return result;
    }

    public void rollback() {
        super.rollback();
        channelGeneratorChooser.rollback();
    }

    public void takeSnapshot() {
        super.takeSnapshot();
        channelGeneratorChooser.takeSnapshot();
    }
}
