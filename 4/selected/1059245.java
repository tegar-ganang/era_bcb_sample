package org.mcisb.massspectrometry.pride.converter;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import javax.xml.bind.*;
import javax.xml.namespace.*;
import javax.xml.stream.*;
import javax.xml.transform.stream.*;
import org.mcisb.massspectrometry.pride.model.*;
import org.mcisb.massspectrometry.pride.model.Peptide;
import org.mcisb.massspectrometry.pride.model.ExperimentType.*;
import org.mcisb.massspectrometry.pride.model.ExperimentType.MzData.SpectrumList.*;
import org.mcisb.massspectrometry.pride.model.SpectrumSettingsType.*;
import org.mcisb.tracking.proteomics.*;
import org.mcisb.util.*;
import org.mcisb.util.math.*;
import org.mcisb.util.xml.*;

/**
 * 
 * @author Neil Swainston
 */
public abstract class PrideUtils {

    /**
	 * 
	 */
    public static final String PRIDE_LABEL = "PRIDE";

    /**
	 * 
	 */
    public static final String DEFAULT_VERSION = "2.1";

    /**
	 * 
	 */
    public static final String PSI_LABEL = "PSI";

    /**
	 * 
	 */
    public static final String MASCOT_SCORE_ACCESSION = "PRIDE:0000069";

    /**
	 * 
	 */
    public static final String MASCOT_EXPECT_ACCESSION = "PRIDE:0000212";

    /**
	 * 
	 */
    public static final String PARENT_ION_CHARGE_STATE_ACCESSION = "PSI:1000041";

    /**
	 * 
	 */
    public static final String PARENT_ION_RETENTION_TIME_ACCESSION = "PRIDE:0000203";

    /**
	 * 
	 */
    public static final String PEPTIDE_PAIR_ID_ACCESSION = "PRIDE:0000209";

    /**
	 * 
	 */
    public static final String STABLE_ISOTOPE_RATIO_ACCESSION = "PRIDE:0000210";

    /**
	 * 
	 */
    public static final String CHARGE_ACCESSION = "PSI:1000041";

    /**
	 * 
	 */
    public static final String CALCULATED_MASS_TO_CHARGE_RATIO_ACCESSION = "PRIDE:0000220";

    /**
	 * 
	 */
    public static final String MASS_TO_CHARGE_RATIO_ACCESSION = "PSI:1000040";

    /**
	 * 
	 */
    public static final String RANK_ACCESSION = "PRIDE:0000091";

    /**
	 * 
	 */
    public static final String MISSED_CLEAVAGES = "MISSED_CLEAVAGES";

    /**
	 * 
	 */
    private static volatile Marshaller marshaller = null;

    /**
	 * 
	 * @param mzDataFile
	 * @return ExperimentCollection
	 * @throws JAXBException
	 */
    public static ExperimentCollection getExperimentCollection(final File mzDataFile) throws JAXBException {
        final String mzDataFilename = mzDataFile.getName();
        final MzData mzData = JAXBContext.newInstance("org.mcisb.massspectrometry.pride.model").createUnmarshaller().unmarshal(new StreamSource(mzDataFile), MzData.class).getValue();
        final Protocol protocol = new Protocol();
        protocol.setProtocolName("undefined");
        final ExperimentType experimentType = new ExperimentType();
        experimentType.setTitle(mzDataFilename);
        experimentType.setShortLabel(mzDataFilename);
        experimentType.setProtocol(protocol);
        experimentType.setMzData(mzData);
        final ExperimentCollection experimentCollection = new ExperimentCollection();
        experimentCollection.setVersion(PrideUtils.DEFAULT_VERSION);
        experimentCollection.getExperiment().add(experimentType);
        return experimentCollection;
    }

    /**
	 * 
	 * @param peptide
	 * @param modAccessions
	 * @return int
	 */
    public static int containsModifier(final Peptide peptide, final int[] modAccessions) {
        for (final Modification modification : peptide.getModificationItem()) {
            if (CollectionUtils.contains(modAccessions, Integer.parseInt(modification.getModAccession()))) {
                return Integer.parseInt(modification.getModAccession());
            }
        }
        return NumberUtils.UNDEFINED;
    }

    /**
	 * 
	 * @param paramType
	 * @param cvParamAccession
	 * @return String
	 */
    public static String getCvParamValue(final ParamType paramType, final String cvParamAccession) {
        for (final Object cvParamOrUserParam : paramType.getCvParamOrUserParam()) {
            if (cvParamOrUserParam instanceof CvParamType) {
                final CvParamType cvParamType = (CvParamType) cvParamOrUserParam;
                if (cvParamType.getAccession().equals(cvParamAccession)) {
                    return cvParamType.getValue();
                }
            }
        }
        return null;
    }

    /**
	 * 
	 * @param paramType
	 * @param name
	 * @return String
	 */
    public static String getUserParamValue(final ParamType paramType, final String name) {
        for (final Object cvParamOrUserParam : paramType.getCvParamOrUserParam()) {
            if (cvParamOrUserParam instanceof UserParamType) {
                final UserParamType userParamType = (UserParamType) cvParamOrUserParam;
                if (userParamType.getName().equals(name)) {
                    return userParamType.getValue();
                }
            }
        }
        return null;
    }

    /**
	 * 
	 * @param paramType
	 * @param cvParamAccession
	 */
    public static void removeCvParamValue(final ParamType paramType, final String cvParamAccession) {
        for (Iterator<Object> iterator = paramType.getCvParamOrUserParam().iterator(); iterator.hasNext(); ) {
            final Object cvParamOrUserParam = iterator.next();
            if (cvParamOrUserParam instanceof CvParamType && ((CvParamType) cvParamOrUserParam).getAccession().equals(cvParamAccession)) {
                iterator.remove();
            }
        }
    }

    /**
	 * 
	 * @param spectrum
	 * @return float
	 */
    public static float getPrecursorMz(final org.mcisb.massspectrometry.pride.model.ExperimentType.MzData.SpectrumList.Spectrum spectrum) {
        final int FIRST = 0;
        final String mzValue = getCvParamValue(spectrum.getSpectrumDesc().getPrecursorList().getPrecursor().get(FIRST).getIonSelection(), MASS_TO_CHARGE_RATIO_ACCESSION);
        if (mzValue != null) {
            return Float.parseFloat(mzValue);
        }
        return NumberUtils.UNDEFINED;
    }

    /**
	 * 
	 * @param peptide
	 * @return double
	 * @throws Exception
	 */
    public static double getMass(final Peptide peptide) throws Exception {
        final org.mcisb.tracking.proteomics.Peptide trackingPeptide = new org.mcisb.tracking.proteomics.Peptide(peptide.getSequence());
        for (final Modification modification : peptide.getModificationItem()) {
            if (modification.getModDatabase().equals(Modifier.UNIMOD)) {
                final Modifier modifier = ModifierFactory.getInstance(false).getModifierFromId(modification.getModAccession());
                trackingPeptide.setModifier(modifier, Integer.parseInt(modification.getModAccession()));
            } else {
                throw new UnsupportedOperationException();
            }
        }
        return trackingPeptide.getMass();
    }

    /**
	 * 
	 * @param peptide
	 * @param mzData
	 * @return float
	 */
    public static float getPrecursorMz(final Peptide peptide, final MzData mzData) {
        if (peptide.getSpectrumReference() != null) {
            for (final Spectrum spectrum : mzData.getSpectrumList().getSpectrum()) {
                if (spectrum.getId() == peptide.getSpectrumReference().intValue()) {
                    return getPrecursorMz(spectrum);
                }
            }
        }
        return NumberUtils.UNDEFINED;
    }

    /**
	 * 
	 * @param peptide
	 * @param mzData
	 * @return double
	 * @throws Exception 
	 */
    public static long getZ(final Peptide peptide, final MzData mzData) throws Exception {
        final float precursorMz = getPrecursorMz(peptide, mzData);
        if (precursorMz == NumberUtils.UNDEFINED) {
            return NumberUtils.UNDEFINED;
        }
        return Math.round(getMass(peptide) / precursorMz);
    }

    /**
	 * 
	 * @param peakListBinaryType
	 * @return double
	 * @throws JAXBException 
	 * @throws XMLStreamException 
	 */
    public static double[] getValues(final PeakListBinaryType peakListBinaryType) throws JAXBException, XMLStreamException {
        final StringWriter writer = new StringWriter();
        getMarshaller().marshal(new JAXBElement<PeakListBinaryType.Data>(new QName("", "peakListBinaryType"), PeakListBinaryType.Data.class, peakListBinaryType.getData()), writer);
        final XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(writer.toString()));
        final StringBuffer characters = new StringBuffer();
        int event = XMLStreamConstants.START_DOCUMENT;
        while ((event = reader.next()) != XMLStreamConstants.END_DOCUMENT) {
            switch(event) {
                case XMLStreamConstants.CHARACTERS:
                    {
                        characters.append(reader.getText());
                        break;
                    }
                default:
                    {
                        continue;
                    }
            }
        }
        return MathUtils.decode(characters.toString().getBytes(Charset.defaultCharset()), !peakListBinaryType.getData().getEndian().equals(org.mcisb.massspectrometry.PropertyNames.LITTLE), !peakListBinaryType.getData().getPrecision().equals(org.mcisb.massspectrometry.PropertyNames.PRECISION_FLOAT));
    }

    /**
	 * 
	 * @param xValues 
	 * @param yValues 
	 * @param spectrumId
	 * @param msLevel
	 * @return Spectrum
	 */
    public static org.mcisb.massspectrometry.pride.model.ExperimentType.MzData.SpectrumList.Spectrum getSpectrum(final double[] xValues, final double[] yValues, final int spectrumId, final int msLevel) {
        final boolean IS_BIG_ENDIAN = false;
        final PeakListBinaryType rtArrayBinary = new PeakListBinaryType();
        rtArrayBinary.setData(getData(xValues, IS_BIG_ENDIAN));
        final PeakListBinaryType intenArrayBinary = new PeakListBinaryType();
        intenArrayBinary.setData(getData(yValues, IS_BIG_ENDIAN));
        final SpectrumInstrument spectrumInstrument = new SpectrumInstrument();
        spectrumInstrument.setMsLevel(msLevel);
        final SpectrumSettingsType spectrumSettings = new SpectrumSettingsType();
        spectrumSettings.setSpectrumInstrument(spectrumInstrument);
        final SpectrumDescType spectrumDesc = new SpectrumDescType();
        spectrumDesc.setSpectrumSettings(spectrumSettings);
        final org.mcisb.massspectrometry.pride.model.ExperimentType.MzData.SpectrumList.Spectrum newSpectrum = new org.mcisb.massspectrometry.pride.model.ExperimentType.MzData.SpectrumList.Spectrum();
        newSpectrum.setId(spectrumId);
        newSpectrum.setSpectrumDesc(spectrumDesc);
        newSpectrum.setMzArrayBinary(rtArrayBinary);
        newSpectrum.setIntenArrayBinary(intenArrayBinary);
        return newSpectrum;
    }

    /**
	 * 
	 * @param values
	 * @param isBigEndian
	 * @return PeakListBinaryType.Data
	 */
    public static PeakListBinaryType.Data getData(final double[] values, final boolean isBigEndian) {
        final String BIG = "big";
        final String LITTLE = "little";
        final byte[] byteArray = MathUtils.getBytes(values, isBigEndian);
        final PeakListBinaryType.Data data = new PeakListBinaryType.Data();
        data.setValue(byteArray);
        data.setLength(byteArray.length);
        data.setPrecision(org.mcisb.massspectrometry.PropertyNames.PRECISION_DOUBLE);
        data.setEndian(isBigEndian ? BIG : LITTLE);
        return data;
    }

    /**
	 * 
	 * @param peptide1
	 * @param peptide2
	 * @return boolean
	 */
    public static boolean equals(final Peptide peptide1, final Peptide peptide2) {
        if (!peptide1.getSequence().equals(peptide2.getSequence())) {
            return false;
        }
        if (peptide1.getModificationItem().size() != peptide2.getModificationItem().size()) {
            return false;
        }
        outer: for (final Modification modification1 : peptide1.getModificationItem()) {
            for (final Modification modification2 : peptide2.getModificationItem()) {
                if (equals(modification1, modification2)) {
                    break outer;
                }
            }
            return false;
        }
        outer: for (final Modification modification2 : peptide2.getModificationItem()) {
            for (final Modification modification1 : peptide1.getModificationItem()) {
                if (equals(modification1, modification2)) {
                    break outer;
                }
            }
            return false;
        }
        return true;
    }

    /**
	 * 
	 * @param modification1
	 * @param modification2
	 * @return boolean
	 */
    public static boolean equals(final Modification modification1, final Modification modification2) {
        return (modification1.getModAccession().equals(modification2.getModAccession()) && modification1.getModDatabase().equals(modification2.getModDatabase()) && modification1.getModLocation().equals(modification2.getModLocation()));
    }

    /**
	 * 
	 * @param peptide
	 * @param ignoredModAccessions
	 * @return Peptide
	 */
    public static Peptide clone(final Peptide peptide, final int[] ignoredModAccessions) {
        final Peptide clone = new Peptide();
        clone.setSequence(peptide.getSequence());
        clone.setStart(peptide.getStart());
        clone.setEnd(peptide.getEnd());
        for (final Modification modification : peptide.getModificationItem()) {
            if (!CollectionUtils.contains(ignoredModAccessions, Integer.parseInt(modification.getModAccession()))) {
                clone.getModificationItem().add(modification);
            }
        }
        return clone;
    }

    /**
	 * 
	 * @param spectra
	 * @param matchedSpectrumReferences
	 */
    public static void stripSpectra(final Collection<org.mcisb.massspectrometry.pride.model.ExperimentType.MzData.SpectrumList.Spectrum> spectra, final Collection<Integer> matchedSpectrumReferences) {
        for (final Iterator<org.mcisb.massspectrometry.pride.model.ExperimentType.MzData.SpectrumList.Spectrum> iterator = spectra.iterator(); iterator.hasNext(); ) {
            final org.mcisb.massspectrometry.pride.model.ExperimentType.MzData.SpectrumList.Spectrum spectrum = iterator.next();
            final int spectrumId = spectrum.getId();
            if (!matchedSpectrumReferences.contains(Integer.valueOf(spectrumId))) {
                iterator.remove();
            }
        }
    }

    /**
	 * 
	 * @param peptide
	 * @return int
	 */
    public static int getMissedCleavages(final Peptide peptide) {
        return Integer.parseInt(getUserParamValue(peptide.getAdditional(), MISSED_CLEAVAGES));
    }

    /**
	 * 
	 * @return Marshaller
	 * @throws JAXBException
	 */
    public static Marshaller getMarshaller() throws JAXBException {
        if (marshaller == null) {
            marshaller = XmlUtils.getMarshaller("org.mcisb.massspectrometry.pride.model");
        }
        return marshaller;
    }
}
