package xml;

import gui.AntiHarmonicProperties;
import gui.BaseProperties;
import gui.BoxFactory;
import gui.ChordGroupProperties;
import gui.EditHandlerFactory;
import gui.EditViewFocusManager;
import gui.HarmonicProperties;
import gui.IntervalProperties;
import gui.MainProg;
import gui.RecordedSoundProperties;
import gui.RepetitionGroupProperties;
import gui.SequenceBox;
import gui.SequenceProperties;
import gui.SoundProperties;
import gui.IntervalProperties.Operator;
import gui.IntervalProperties.RelativeMode;
import gui.SoundProperties.DurationMode;
import java.awt.Component;
import javax.swing.JPanel;
import javax.swing.ProgressMonitor;
import xml.ImportBaseWorker.ActionDetailSeqHelper;
import xml.ImportBaseWorker.IActionDetailHelper;
import xml.ImportDialog.ImportParams;
import xml.castor.Children;
import xml.castor.ChordGroup;
import xml.castor.EntityType;
import xml.castor.Instrument;
import xml.castor.Interval;
import xml.castor.IntervalEntity;
import xml.castor.IntervalType;
import xml.castor.KangasSoundEditor;
import xml.castor.RecordedSound;
import xml.castor.RepGroupEntList;
import xml.castor.RepetitionGroup;
import xml.castor.SeqEntList;
import xml.castor.SequenceType;
import xml.castor.Sound;
import xml.castor.types.ChannelSelectorEnumType;
import xml.castor.types.DurationEnumType;
import xml.castor.types.InstrumentEnumType;
import xml.castor.types.IntervalRelativeToEnumType;
import xml.castor.types.RelativeToEnumType;
import xml.castor.types.VolumeOperatorEnumType;
import composition.CompositionListener;
import database.BaseParams;
import database.DbSequence;

public class SequenceDBDispatcher extends DBDispatcher {

    static DBDispatcher s_sequenceDbDispatcher = null;

    @Override
    boolean exists(String name, IActionDetailHelper helper) throws Exception {
        return false;
    }

    @Override
    void insert(String name, Object newData, Component parent, ProgressMonitor pm, String xmlImportPrefix, String xmlImportSuffix, ImportParams importParams, IActionDetailHelper helper) throws Exception {
        ActionDetailSeqHelper seqHelper = (ActionDetailSeqHelper) helper;
        if (!(newData instanceof xml.castor.SequenceType)) {
            throw new IllegalArgumentException("newData not of type SequenceType");
        }
        xml.castor.SequenceType xmlSequence = (xml.castor.SequenceType) newData;
        SequenceXMLConverter converter = new SequenceXMLConverter(xmlSequence, importParams, seqHelper.getEarliestSeqStartTime());
        BaseParams seqParams = converter.navigate();
        int destTrackNo = seqHelper.getDestTrack();
        int trackDbId = seqHelper.getTrackDbId();
        if (trackDbId == -1) {
            CompositionListener.addSeqBoxToUI(destTrackNo, seqParams, true);
            EditHandlerFactory.checkAndSetDefTrackProperties(destTrackNo);
        } else {
            SequenceBox seqBox = getSeqBox(seqParams);
            DbSequence.saveSynchronously(parent, trackDbId, seqBox);
        }
    }

    private SequenceBox getSeqBox(BaseParams params) {
        SequenceProperties seqProps = (SequenceProperties) params.getBaseProperties();
        SequenceBox seqBox = (SequenceBox) BoxFactory.createBox(MainProg.SEQUENCE, true);
        seqBox.putSequenceProperty(EditViewFocusManager.USER_PROPERTIES, seqProps);
        BaseParams[] baseParams = params.getChildren();
        for (int i = 0; i < baseParams.length; i++) {
            BaseParams p = baseParams[i];
            JPanel box = SequenceDBDispatcher.getBox(p);
            seqBox.add(box, false);
        }
        return seqBox;
    }

    static JPanel getBox(BaseParams params) {
        BaseProperties baseProperties = params.getBaseProperties();
        String boxType = baseProperties.getBoxType();
        JPanel box = BoxFactory.createBox(boxType, false);
        box.putClientProperty(EditViewFocusManager.USER_PROPERTIES, baseProperties);
        BaseParams[] children = params.getChildren();
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                JPanel child = getBox(children[i]);
                box.add(child);
            }
        }
        return box;
    }

    @Override
    void overWrite(String name, int id, Object newData, Component parent, ProgressMonitor pm, String xmlImportPrefix, String xmlImportSuffix, ImportParams importParams, IActionDetailHelper helper) throws Exception {
    }

    @Override
    int getId(String name) throws Exception {
        return 0;
    }

    static int getId(String xmlGraphName, String graphType, boolean validateMode, String xmlImportPrefix, String xmlImportSuffix) throws Exception {
        return 0;
    }

    @Override
    String load(int id, KangasSoundEditor kSE, boolean loadDependentEntities, String exportPrefix, String exportSuffix) throws Exception {
        return null;
    }

    abstract static class SequenceNavigator {

        private final xml.castor.SequenceType m_xmlSequence;

        SequenceNavigator(xml.castor.SequenceType xmlSequence) {
            m_xmlSequence = xmlSequence;
        }

        abstract BaseParams visit(xml.castor.SequenceType seq) throws Exception;

        abstract BaseParams visit(RepetitionGroup repetitionGroup, BaseParams parentParams) throws Exception;

        abstract BaseParams visit(ChordGroup chordGroup, BaseParams parentParams) throws Exception;

        abstract BaseParams visit(Sound sound, BaseParams parentParams) throws Exception;

        abstract void visit(RecordedSound recordedSound, BaseParams parentParams) throws Exception;

        abstract void visit(Interval interval, BaseParams parentParams) throws Exception;

        abstract void visitHarmonic(Instrument instrument, BaseParams parentParams) throws Exception;

        abstract void visitAntiHarmonic(Instrument instrument, BaseParams parentParams) throws Exception;

        void navigateEntity(EntityType ent, BaseParams baseParams) throws Exception {
            Object choice = ent.getChoiceValue();
            if (choice instanceof RepetitionGroup) {
                RepetitionGroup repetitionGroup = ent.getRepetitionGroup();
                BaseParams repParams = visit(repetitionGroup, baseParams);
                RepGroupEntList repGroupEntList = repetitionGroup.getRepGroupEntList();
                int entityCount = repGroupEntList.getEntityCount();
                for (int i = 0; i < entityCount; i++) {
                    navigateEntity(repGroupEntList.getEntity(i), repParams);
                }
            } else if (choice instanceof ChordGroup) {
                ChordGroup chordGroup = ent.getChordGroup();
                BaseParams chordGrpParams = visit(chordGroup, baseParams);
                Children children = chordGroup.getChildren();
                if (children != null) {
                    navigateEntity(children.getChordGroupChildrenEnt(), chordGrpParams);
                    int intervalEntityCount = children.getIntervalEntityCount();
                    for (int i = 0; i < intervalEntityCount; i++) {
                        IntervalEntity intervalEntity = children.getIntervalEntity(i);
                        visit(intervalEntity.getInterval(), chordGrpParams);
                        navigateEntity(intervalEntity.getIntervalEnt(), chordGrpParams);
                    }
                }
            } else if (choice instanceof Sound) {
                Sound sound = ent.getSound();
                BaseParams soundParams = visit(sound, baseParams);
                int instrumentCount = sound.getInstrumentCount();
                for (int i = 0; i < instrumentCount; i++) {
                    Instrument instrument = sound.getInstrument(i);
                    InstrumentEnumType instrumentEnum = instrument.getInstrumentEnum();
                    switch(instrumentEnum) {
                        case HARMONIC:
                            {
                                visitHarmonic(instrument, soundParams);
                                break;
                            }
                        case ANTIHARMONIC:
                            {
                                visitAntiHarmonic(instrument, soundParams);
                                break;
                            }
                        default:
                            {
                                throw new IllegalStateException("Unexpected instrument type " + instrumentEnum + " in navigateEntity");
                            }
                    }
                }
            } else if (choice instanceof RecordedSound) {
                visit(ent.getRecordedSound(), baseParams);
            } else {
                throw new IllegalStateException("Unexpected choice type " + choice + " in navigateEntity");
            }
        }

        BaseParams navigate() throws Exception {
            BaseParams seqParams = visit(m_xmlSequence);
            SeqEntList seqEntList = m_xmlSequence.getSeqEntList();
            int entityCount = seqEntList.getEntityCount();
            for (int i = 0; i < entityCount; i++) {
                navigateEntity(seqEntList.getEntity(i), seqParams);
            }
            return seqParams;
        }
    }

    static class SequenceXMLConverter extends SequenceNavigator {

        private final ImportParams m_importParams;

        private String m_importPrefix;

        private String m_importSuffix;

        private final double m_earliestSeqStartTime;

        SequenceXMLConverter(SequenceType xmlSequence, ImportParams importParams, double earliestSeqStartTime) {
            super(xmlSequence);
            m_importParams = importParams;
            m_earliestSeqStartTime = earliestSeqStartTime;
            m_importPrefix = importParams.getXmlImportPrefix();
            m_importSuffix = importParams.getXmlImportSuffix();
        }

        @Override
        BaseParams visit(SequenceType xmlSeq) throws Exception {
            SequenceProperties seqProp = new SequenceProperties();
            seqProp.setDescription(xmlSeq.getDescription());
            double destTime = ImportBaseWorker.getSeqDestTime(xmlSeq, m_importParams, m_earliestSeqStartTime);
            seqProp.setStartTime(destTime);
            seqProp.setDuration(xmlSeq.getDurationSecs());
            seqProp.setVolume(xmlSeq.getStartVolume());
            seqProp.setVolumeMult(xmlSeq.getVolumeMult());
            seqProp.setPitch(xmlSeq.getStartPitch());
            seqProp.setIsKeepIntermediateFile(xmlSeq.isKeepIntermediateFile());
            BaseParams params = new BaseParams(seqProp);
            return params;
        }

        private IntervalProperties getIntervalFromXML(IntervalType xmlInterval) {
            IntervalProperties intervalProp = new IntervalProperties();
            intervalProp.setRelVolume(xmlInterval.getRelativeVolume());
            intervalProp.setVolumeRelative(getVolPitchRelative(xmlInterval.getVolumeRelativeTo()));
            intervalProp.setVolumeOperator(getVolOperator(xmlInterval.getVolumeOperator()));
            intervalProp.setPitchNumerator((int) xmlInterval.getRelPitchNumer());
            intervalProp.setPitchDenominator((int) xmlInterval.getRelPitchDenom());
            intervalProp.setPitchRelative(getVolPitchRelative(xmlInterval.getPitchRelativeTo()));
            intervalProp.setDuration(xmlInterval.getIntervalDurationSecs());
            intervalProp.setIntervalRelative(getIntervalRelative(xmlInterval.getIntervalRelativeTo()));
            return intervalProp;
        }

        private RelativeMode getIntervalRelative(IntervalRelativeToEnumType intervalRelativeTo) {
            switch(intervalRelativeTo) {
                case PREVIOUS:
                    {
                        return RelativeMode.PREVIOUS;
                    }
                case PREVIOUSSOUND:
                    {
                        return RelativeMode.PREVIOUS_SOUND;
                    }
                default:
                    {
                        throw new IllegalArgumentException("Unrecognized (interval) intervalRelativeTo:" + intervalRelativeTo);
                    }
            }
        }

        private Operator getVolOperator(VolumeOperatorEnumType volumeOperator) {
            switch(volumeOperator) {
                case ADD:
                    {
                        return Operator.ADD;
                    }
                case MULTIPLY:
                    {
                        return Operator.MULTIPLY;
                    }
                default:
                    {
                        throw new IllegalArgumentException("Unrecognized volumeOperator:" + volumeOperator);
                    }
            }
        }

        private RelativeMode getVolPitchRelative(RelativeToEnumType volumeRelativeTo) {
            switch(volumeRelativeTo) {
                case PREVIOUS:
                    {
                        return RelativeMode.PREVIOUS;
                    }
                case PREVIOUSSOUND:
                    {
                        return RelativeMode.PREVIOUS_SOUND;
                    }
                case STARTOFGROUP:
                    {
                        return RelativeMode.GROUP_START;
                    }
                case STARTOFSEQUENCE:
                    {
                        return RelativeMode.SEQUENCE_START;
                    }
                default:
                    {
                        throw new IllegalArgumentException("Unrecognized RelativeMode:" + volumeRelativeTo);
                    }
            }
        }

        @Override
        BaseParams visit(RepetitionGroup xmlRepGroup, BaseParams parentParams) throws Exception {
            RepetitionGroupProperties repProp = new RepetitionGroupProperties();
            repProp.setNoOfReps((int) xmlRepGroup.getNoOfReps());
            repProp.setSimpleReps(xmlRepGroup.getSimpleReps());
            repProp.setIntervalProperties(getIntervalFromXML(xmlRepGroup));
            repProp.setPitchGraphExtent(xmlRepGroup.getPitchExtent());
            IdAndName pitchGraphNameAndId = GraphDBDispatcher.getId(xmlRepGroup.getPitchGraph(), "Pitch", false, m_importPrefix, m_importSuffix);
            repProp.setPitchGraphId(pitchGraphNameAndId.getId());
            repProp.setPitchGraph(pitchGraphNameAndId.getDbName());
            repProp.setVolumeGraphExtent(xmlRepGroup.getVolumeExtent());
            IdAndName volGraphNameAndId = GraphDBDispatcher.getId(xmlRepGroup.getVolumeGraph(), "Volume", false, m_importPrefix, m_importSuffix);
            repProp.setVolumeGraphId(volGraphNameAndId.getId());
            repProp.setVolumeGraph(volGraphNameAndId.getDbName());
            repProp.setCumulativeVolume(xmlRepGroup.getCumulativeVolume());
            repProp.setDurationGraphExtent(xmlRepGroup.getDurationIntervalExtent());
            IdAndName durationGraphNameAndId = GraphDBDispatcher.getId(xmlRepGroup.getDurationIntervalGraph(), "Duration/Interval", false, m_importPrefix, m_importSuffix);
            repProp.setDurationGraphId(durationGraphNameAndId.getId());
            repProp.setDurationGraph(durationGraphNameAndId.getDbName());
            repProp.setIntervalGraphEnable(xmlRepGroup.isIntervalGraphEnable());
            repProp.setChildIntervalGraphEnable(xmlRepGroup.isChildIntervalGraphEnable());
            repProp.setChildDurationGraphEnable(xmlRepGroup.isChildDurationGraphEnable());
            repProp.setChildIntervalGraphExtent(xmlRepGroup.getChildIntervalExtent());
            IdAndName childIntervalGraphNameAndId = GraphDBDispatcher.getId(xmlRepGroup.getChildIntervalGraph(), "Child Interval", false, m_importPrefix, m_importSuffix);
            repProp.setChildIntervalGraphId(childIntervalGraphNameAndId.getId());
            repProp.setChildIntervalGraph(childIntervalGraphNameAndId.getDbName());
            repProp.setChildDurationGraphExtent(xmlRepGroup.getChildDurationExtent());
            IdAndName childDurationGraphNameAndId = GraphDBDispatcher.getId(xmlRepGroup.getChildDurationGraph(), "Child Duration", false, m_importPrefix, m_importSuffix);
            repProp.setChildDurationGraphId(childDurationGraphNameAndId.getId());
            repProp.setChildDurationGraph(childDurationGraphNameAndId.getDbName());
            repProp.setIsKeepIntermediateFile(xmlRepGroup.isKeepIntermediateFile());
            BaseParams params = new BaseParams(repProp);
            parentParams.add(params);
            return params;
        }

        @Override
        BaseParams visit(ChordGroup xmlChordGroup, BaseParams parentParams) throws Exception {
            ChordGroupProperties chordGrpProp = new ChordGroupProperties();
            chordGrpProp.setIntervalNumerator((int) xmlChordGroup.getIntervalNumer());
            chordGrpProp.setIntervalDenominator((int) xmlChordGroup.getIntervalDenom());
            chordGrpProp.setVolumeMult(xmlChordGroup.getRelativeVolume());
            chordGrpProp.setVolumeOperator(getVolOperator(xmlChordGroup.getVolumeOperator()));
            chordGrpProp.setPitchNumerMult((int) xmlChordGroup.getPitchNumerMult());
            chordGrpProp.setPitchDenomMult((int) xmlChordGroup.getPitchDenomMult());
            chordGrpProp.setChildDurationNumerator((int) xmlChordGroup.getChildDurationNumer());
            chordGrpProp.setChildDurationDenominator((int) xmlChordGroup.getChildDurationDenom());
            chordGrpProp.setChildIntervalNumerator((int) xmlChordGroup.getChildIntervalNumer());
            chordGrpProp.setChildIntervalDenominator((int) xmlChordGroup.getChildIntervalDenom());
            chordGrpProp.setIsKeepIntermediateFile(xmlChordGroup.isKeepIntermediateFile());
            BaseParams params = new BaseParams(chordGrpProp);
            parentParams.add(params);
            return params;
        }

        @Override
        BaseParams visit(Sound xmlSound, BaseParams parentParams) throws Exception {
            SoundProperties soundProp = new SoundProperties();
            soundProp.setPitchGraphExtent(xmlSound.getPitchExtent());
            IdAndName pitchGraphNameAndId = GraphDBDispatcher.getId(xmlSound.getPitchGraph(), "Pitch", false, m_importPrefix, m_importSuffix);
            soundProp.setPitchGraphId(pitchGraphNameAndId.getId());
            soundProp.setPitchGraph(pitchGraphNameAndId.getDbName());
            soundProp.setPitchCalcFreq((int) xmlSound.getPitchCalcFreq());
            soundProp.setScalePitch(xmlSound.isScalePitchGraph());
            soundProp.setInvertPitch(xmlSound.isInvertPitchGraph());
            soundProp.setVolumeGraphExtent(xmlSound.getVolExtent());
            IdAndName volGraphNameAndId = GraphDBDispatcher.getId(xmlSound.getVolumeGraph(), "Volume", false, m_importPrefix, m_importSuffix);
            soundProp.setVolumeGraphId(volGraphNameAndId.getId());
            soundProp.setVolumeGraph(volGraphNameAndId.getDbName());
            soundProp.setVolCalcFreq((int) xmlSound.getVolCalcFreq());
            soundProp.setScaleVolume(xmlSound.isScaleVolGraph());
            soundProp.setInvertVolume(xmlSound.isInvertVolGraph());
            soundProp.setVolumeGraphFromZero(xmlSound.isVolGraphFromZero());
            soundProp.setDuration(xmlSound.getDurationSecs());
            soundProp.setDurationMode(getDurationType(xmlSound.getDurationType()));
            soundProp.setIsKeepIntermediateFile(xmlSound.isKeepIntermediateFile());
            BaseParams params = new BaseParams(soundProp);
            parentParams.add(params);
            return params;
        }

        private DurationMode getDurationType(DurationEnumType durationType) {
            switch(durationType) {
                case FIXED:
                    {
                        return DurationMode.FIXED;
                    }
                case PROPTOVOL:
                    {
                        return DurationMode.VOLUME_RELATIVE;
                    }
                case PROPTOVOLLESSEXTENT:
                    {
                        return DurationMode.VOLUME_MINUS_EXTENT;
                    }
                default:
                    {
                        throw new IllegalArgumentException("Unexpected durationMode:" + durationType);
                    }
            }
        }

        @Override
        void visit(RecordedSound xmlRecSound, BaseParams parentParams) throws Exception {
            RecordedSoundProperties recSndProp = new RecordedSoundProperties();
            recSndProp.setRawSoundFile(xmlRecSound.getRawSoundFile());
            recSndProp.setStartOfs(xmlRecSound.getRawSoundFileStartOfsSecs());
            recSndProp.setDuration(xmlRecSound.getDurationSecs());
            recSndProp.setLeftChanSelected(getChanSelectorLeft(xmlRecSound.getChannelSelector()));
            recSndProp.setAmplitudeGraphExtentMult(xmlRecSound.getAmplitudeExtMult());
            IdAndName amplitudeGraphNameAndId = GraphDBDispatcher.getId(xmlRecSound.getAmplitudeGraph(), "Amplitude", false, m_importPrefix, m_importSuffix);
            recSndProp.setAmplitudeGraphId(amplitudeGraphNameAndId.getId());
            recSndProp.setAmplitudeGraph(amplitudeGraphNameAndId.getDbName());
            recSndProp.setIsKeepIntermediateFile(xmlRecSound.isKeepIntermediateFile());
            BaseParams params = new BaseParams(recSndProp);
            parentParams.add(params);
        }

        private boolean getChanSelectorLeft(ChannelSelectorEnumType channelSelector) {
            return channelSelector == ChannelSelectorEnumType.LEFT;
        }

        @Override
        void visit(Interval interval, BaseParams parentParams) throws Exception {
            IntervalProperties intervalProp = getIntervalFromXML(interval);
            BaseParams params = new BaseParams(intervalProp);
            parentParams.add(params);
        }

        @Override
        void visitAntiHarmonic(Instrument xmlInst, BaseParams parentParams) throws Exception {
            AntiHarmonicProperties ahProp = new AntiHarmonicProperties();
            ahProp.setAmplitude(xmlInst.getAmplitude());
            ahProp.setIsKeepIntermediateFile(xmlInst.isKeepIntermediateFile());
            IdAndName ahGraphNameAndId = AntiHarmonicDBDispatcher.getId(xmlInst.getCode(), null, false, m_importPrefix, m_importSuffix);
            ahProp.setId(ahGraphNameAndId.getId());
            ahProp.setCode(ahGraphNameAndId.getDbName());
            BaseParams params = new BaseParams(ahProp);
            parentParams.add(params);
        }

        @Override
        void visitHarmonic(Instrument xmlInst, BaseParams parentParams) throws Exception {
            HarmonicProperties harmonicProp = new HarmonicProperties();
            harmonicProp.setAmplitude(xmlInst.getAmplitude());
            harmonicProp.setIsKeepIntermediateFile(xmlInst.isKeepIntermediateFile());
            IdAndName harmonicGraphNameAndId = HarmonicDBDispatcher.getId(xmlInst.getCode(), null, false, m_importPrefix, m_importSuffix);
            harmonicProp.setId(harmonicGraphNameAndId.getId());
            harmonicProp.setCode(harmonicGraphNameAndId.getDbName());
            BaseParams params = new BaseParams(harmonicProp);
            parentParams.add(params);
        }
    }

    static class SequenceValidator extends SequenceNavigator {

        private final String m_xmlImportPrefix;

        private final String m_xmlImportSuffix;

        SequenceValidator(SequenceType xmlSequence, String xmlImportPrefix, String xmlImportSuffix) {
            super(xmlSequence);
            m_xmlImportPrefix = xmlImportPrefix;
            m_xmlImportSuffix = xmlImportSuffix;
        }

        @Override
        BaseParams visit(SequenceType seq) {
            return null;
        }

        @Override
        BaseParams visit(RepetitionGroup repetitionGroup, BaseParams parentParams) throws Exception {
            GraphDBDispatcher.getId(repetitionGroup.getPitchGraph(), "Pitch", true, m_xmlImportPrefix, m_xmlImportSuffix);
            GraphDBDispatcher.getId(repetitionGroup.getVolumeGraph(), "Volume", true, m_xmlImportPrefix, m_xmlImportSuffix);
            GraphDBDispatcher.getId(repetitionGroup.getDurationIntervalGraph(), "Duration/Interval", true, m_xmlImportPrefix, m_xmlImportSuffix);
            GraphDBDispatcher.getId(repetitionGroup.getChildIntervalGraph(), "Child interval", true, m_xmlImportPrefix, m_xmlImportSuffix);
            GraphDBDispatcher.getId(repetitionGroup.getChildDurationGraph(), "Child duration", true, m_xmlImportPrefix, m_xmlImportSuffix);
            return null;
        }

        @Override
        BaseParams visit(ChordGroup chordGroup, BaseParams parentParams) {
            return null;
        }

        @Override
        BaseParams visit(Sound sound, BaseParams parentParams) throws Exception {
            GraphDBDispatcher.getId(sound.getPitchGraph(), "Pitch", true, m_xmlImportPrefix, m_xmlImportSuffix);
            GraphDBDispatcher.getId(sound.getVolumeGraph(), "Volume", true, m_xmlImportPrefix, m_xmlImportSuffix);
            return null;
        }

        @Override
        void visit(RecordedSound recordedSound, BaseParams parentParams) throws Exception {
            GraphDBDispatcher.getId(recordedSound.getAmplitudeGraph(), "Amplitude", true, m_xmlImportPrefix, m_xmlImportSuffix);
        }

        @Override
        void visit(Interval interval, BaseParams parentParams) {
        }

        @Override
        void visitAntiHarmonic(Instrument instrument, BaseParams parentParams) throws Exception {
            AntiHarmonicDBDispatcher.getId(instrument.getCode(), null, true, m_xmlImportPrefix, m_xmlImportSuffix);
        }

        @Override
        void visitHarmonic(Instrument instrument, BaseParams parentParams) throws Exception {
            HarmonicDBDispatcher.getId(instrument.getCode(), null, true, m_xmlImportPrefix, m_xmlImportSuffix);
        }
    }

    @Override
    void validateRefs(Object xmlData, String xmlImportPrefix, String xmlImportSuffix) throws Exception {
        if (!(xmlData instanceof xml.castor.SequenceType)) {
            throw new IllegalArgumentException("xmlData not of type SequenceType");
        }
        xml.castor.SequenceType xmlSequence = (xml.castor.SequenceType) xmlData;
        SequenceValidator validator = new SequenceValidator(xmlSequence, xmlImportPrefix, xmlImportSuffix);
        validator.navigate();
    }

    @Override
    void addToImportMap(String xmlName, String dbName) {
    }
}
