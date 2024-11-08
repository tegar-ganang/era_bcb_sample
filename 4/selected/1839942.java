package loci.formats;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * A utility class for working with metadata objects,
 * including {@link MetadataStore}, {@link MetadataRetrieve},
 * and OME-XML strings.
 * Most of the methods require the optional {@link loci.formats.ome}
 * package, and optional ome-java.jar library, to be present at runtime.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/loci/formats/MetadataTools.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/loci/formats/MetadataTools.java">SVN</a></dd></dl>
 */
public final class MetadataTools {

    private MetadataTools() {
    }

    /**
   * Creates an OME-XML metadata object using reflection, to avoid
   * direct dependencies on the optional {@link loci.formats.ome} package.
   * @return A new instance of {@link loci.formats.ome.OMEXMLMetadata},
   *   or null if the class is not available.
   */
    public static MetadataStore createOMEXMLMetadata() {
        return createOMEXMLMetadata(null);
    }

    /**
   * Creates an OME-XML metadata object using reflection, to avoid
   * direct dependencies on the optional {@link loci.formats.ome} package,
   * wrapping a DOM representation of the given OME-XML string.
   * @return A new instance of {@link loci.formats.ome.OMEXMLMetadata},
   *   or null if the class is not available.
   */
    public static MetadataStore createOMEXMLMetadata(String xml) {
        MetadataStore store = null;
        ReflectedUniverse r = new ReflectedUniverse();
        try {
            r.exec("import loci.formats.ome.OMEXMLMetadata");
            r.setVar("xml", xml);
            store = (MetadataStore) r.exec("new OMEXMLMetadata(xml)");
        } catch (ReflectException exc) {
        }
        return store;
    }

    /**
   * Checks whether the given object is an OME-XML metadata object.
   * @return True iff the object is an instance of
   *   {@link loci.formats.ome.OMEXMLMetadata}.
   */
    public static boolean isOMEXMLMetadata(Object o) {
        ReflectedUniverse r = new ReflectedUniverse();
        try {
            r.exec("import loci.formats.ome.OMEXMLMetadata");
            Class c = (Class) r.getVar("OMEXMLMetadata");
            return c.isInstance(o);
        } catch (ReflectException exc) {
        }
        return false;
    }

    /**
   * Adds the specified key/value pair as a new OriginalMetadata node
   * to the given OME-XML metadata object.
   * Does nothing unless the given object is an OME-XML metadata object.
   * @param o An object of type {@link loci.formats.ome.OMEXMLMetadata}.
   * @param key Metadata key to populate.
   * @param value Metadata value corresponding to the specified key.
   */
    public static void populateOriginalMetadata(Object o, String key, String value) {
        ReflectedUniverse r = new ReflectedUniverse();
        r.setVar("omexmlMeta", o);
        r.setVar("key", key);
        r.setVar("value", value);
        try {
            r.exec("omexmlMeta.populateOriginalMetadata(key, value)");
        } catch (ReflectException exc) {
        }
    }

    /**
   * Extracts an OME-XML metadata string from the given metadata object,
   * by converting to an OME-XML metadata object if necessary.
   */
    public static String getOMEXML(MetadataRetrieve src) {
        MetadataStore omexmlMeta;
        if (isOMEXMLMetadata(src)) {
            omexmlMeta = (MetadataStore) src;
        } else {
            omexmlMeta = createOMEXMLMetadata();
            MetadataTools.convertMetadata(src, omexmlMeta);
        }
        ReflectedUniverse r = new ReflectedUniverse();
        r.setVar("omexmlMeta", omexmlMeta);
        try {
            return (String) r.exec("omexmlMeta.dumpXML()");
        } catch (ReflectException exc) {
        }
        return null;
    }

    /**
   * Attempts to validate the given OME-XML string using
   * Java's XML validation facility. Requires Java 1.5+.
   */
    public static void validateOMEXML(String xml) {
        XMLTools.validateXML(xml, "OME-XML");
    }

    /**
   * Converts information from an OME-XML string (source)
   * into a metadata store (destination).
   */
    public static void convertMetadata(String xml, MetadataStore dest) {
        if (isOMEXMLMetadata(dest)) {
            ReflectedUniverse r = new ReflectedUniverse();
            try {
                r.setVar("xml", xml);
                r.setVar("omexmlMeta", dest);
                r.exec("omexmlMeta.createRoot(xml)");
            } catch (ReflectException exc) {
            }
        } else {
            MetadataRetrieve src = (MetadataRetrieve) createOMEXMLMetadata(xml);
            convertMetadata(src, dest);
        }
    }

    /**
   * Copies information from a metadata retrieval object
   * (source) into a metadata store (destination).
   */
    public static void convertMetadata(MetadataRetrieve src, MetadataStore dest) {
        Integer ii = null;
        int globalPixCount = 0;
        for (int i = 0; i < src.getImageCount(); i++) {
            ii = new Integer(i);
            dest.setImage(src.getImageName(ii), src.getCreationDate(ii), src.getDescription(ii), ii);
            dest.setDimensions(src.getPixelSizeX(ii), src.getPixelSizeY(ii), src.getPixelSizeZ(ii), src.getPixelSizeC(ii), src.getPixelSizeT(ii), ii);
            for (int j = 0; j < src.getPixelsCount(ii); j++) {
                Integer p = new Integer(j);
                dest.setPixels(src.getSizeX(ii), src.getSizeY(ii), src.getSizeZ(ii), src.getSizeC(ii), src.getSizeT(ii), new Integer(FormatTools.pixelTypeFromString(src.getPixelType(ii))), src.getBigEndian(ii), src.getDimensionOrder(ii), ii, p);
                dest.setDisplayOptions(src.getZoom(ii), src.isRedChannelOn(ii), src.isGreenChannelOn(ii), src.isBlueChannelOn(ii), src.isDisplayRGB(ii), src.getColorMap(ii), src.getZStart(ii), src.getZStop(ii), src.getTStart(ii), src.getTStop(ii), ii, p, new Integer(0), new Integer(1), new Integer(2), new Integer(0));
                Integer globalPix = new Integer(globalPixCount);
                for (int ch = 0; ch < src.getChannelCount(globalPix); ch++) {
                    Integer c = new Integer(ch);
                    dest.setLogicalChannel(ch, src.getChannelName(globalPix, c), null, null, null, null, null, null, null, null, null, null, null, src.getPhotometricInterpretation(globalPix, c), src.getMode(globalPix, c), null, null, null, null, null, src.getEmWave(globalPix, c), src.getExWave(globalPix, c), null, src.getChannelNDFilter(globalPix, c), globalPix);
                    dest.setChannelGlobalMinMax(ch, src.getGlobalMin(globalPix, c), src.getGlobalMax(globalPix, c), globalPix);
                    dest.setDisplayChannel(c, src.getBlackLevel(globalPix, c), src.getWhiteLevel(globalPix, c), src.getGamma(globalPix, c), globalPix);
                }
                globalPixCount++;
            }
            dest.setImagingEnvironment(src.getTemperature(ii), src.getAirPressure(ii), src.getHumidity(ii), src.getCO2Percent(ii), ii);
        }
        for (int i = 0; i < src.getExperimenterCount(); i++) {
            ii = new Integer(i);
            dest.setExperimenter(src.getFirstName(ii), src.getLastName(ii), src.getEmail(ii), src.getInstitution(ii), src.getDataDirectory(ii), src.getGroup(ii), ii);
        }
        for (int i = 0; i < src.getGroupCount(); i++) {
            ii = new Integer(i);
            dest.setGroup(src.getGroupName(ii), src.getLeader(ii), src.getContact(ii), ii);
        }
        for (int i = 0; i < src.getInstrumentCount(); i++) {
            ii = new Integer(i);
            dest.setInstrument(src.getManufacturer(ii), src.getModel(ii), src.getSerialNumber(ii), src.getType(ii), ii);
        }
        for (int i = 0; i < src.getDisplayROICount(); i++) {
            ii = new Integer(i);
            dest.setDisplayROI(src.getX0(ii), src.getY0(ii), src.getZ0(ii), src.getX1(ii), src.getY1(ii), src.getZ1(ii), src.getT0(ii), src.getT1(ii), src.getDisplayOptions(ii), ii);
        }
        for (int i = 0; i < src.getStageLabelCount(); i++) {
            ii = new Integer(i);
            dest.setStageLabel(src.getStageName(ii), src.getStageX(ii), src.getStageY(ii), src.getStageZ(ii), ii);
        }
        ii = null;
        dest.setPlaneInfo(0, 0, 0, src.getTimestamp(ii, ii, ii, ii), src.getExposureTime(ii, ii, ii, ii), ii);
        dest.setLightSource(src.getLightManufacturer(ii), src.getLightModel(ii), src.getLightSerial(ii), ii, ii);
        dest.setLaser(src.getLaserType(ii), src.getLaserMedium(ii), src.getLaserWavelength(ii), src.isFrequencyDoubled(ii), src.isTunable(ii), src.getPulse(ii), src.getPower(ii), ii, ii, ii, ii);
        dest.setFilament(src.getFilamentType(ii), src.getFilamentPower(ii), ii, ii);
        dest.setArc(src.getArcType(ii), src.getArcPower(ii), ii, ii);
        dest.setDetector(src.getDetectorManufacturer(ii), src.getDetectorModel(ii), src.getDetectorSerial(ii), src.getDetectorType(ii), src.getDetectorGain(ii), src.getDetectorVoltage(ii), src.getDetectorOffset(ii), ii, ii);
        dest.setObjective(src.getObjectiveManufacturer(ii), src.getObjectiveModel(ii), src.getObjectiveSerial(ii), src.getLensNA(ii), src.getObjectiveMagnification(ii), ii, ii);
        dest.setExcitationFilter(src.getExcitationManufacturer(ii), src.getExcitationModel(ii), src.getExcitationLotNumber(ii), src.getExcitationType(ii), ii);
        dest.setDichroic(src.getDichroicManufacturer(ii), src.getDichroicModel(ii), src.getDichroicLotNumber(ii), ii);
        dest.setEmissionFilter(src.getEmissionManufacturer(ii), src.getEmissionModel(ii), src.getEmissionLotNumber(ii), src.getEmissionType(ii), ii);
        dest.setFilterSet(src.getFilterSetManufacturer(ii), src.getFilterSetModel(ii), src.getFilterSetLotNumber(ii), ii, ii);
        dest.setOTF(src.getOTFSizeX(ii), src.getOTFSizeY(ii), src.getOTFPixelType(ii), src.getOTFPath(ii), src.getOTFOpticalAxisAverage(ii), ii, ii, ii, ii);
    }

    /** Used by testRead to handle XML validation errors. */
    private static class ValidationHandler implements ErrorHandler {

        private boolean ok = true;

        public boolean ok() {
            return ok;
        }

        public void error(SAXParseException e) {
            LogTools.println("error: " + e.getMessage());
            ok = false;
        }

        public void fatalError(SAXParseException e) {
            LogTools.println("fatal error: " + e.getMessage());
            ok = false;
        }

        public void warning(SAXParseException e) {
            LogTools.println("warning: " + e.getMessage());
            ok = false;
        }
    }
}
