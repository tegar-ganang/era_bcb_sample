package edu.kgi.biobridge.sbwdriver;

import java.util.Collection;
import edu.kgi.biobridge.gum.*;
import edu.kgi.biobridge.app.SBWTools;
import edu.caltech.sbw.*;

/**
 * The SBWCall class is one of the SBW driver's implementations of the Call 
 * interface. This class handles calls only to methods on public modules.
 * For calls to methods on private modules, see SBWPrivateCall.
 * 
 * @see SBWPrivateCall
 * 
 * @author Cameron Wellock
 */
class SBWCall implements Call {

    /**
 * Default array dimensions for a 1-dimensional array.
 */
    private static final int arrayDims1D[] = { 1 };

    /**
 * Default array dimensions for a 2-dimensional array.
 */
    private static final int arrayDims2D[] = { 1, 1 };

    /**
 * Module ID number.
 */
    int moduleId;

    /**
 * Service ID number.
 */
    int serviceId;

    /**
 * Method ID number.
 */
    int methodId;

    /**
 * Parameters set for the method.
 */
    private SBWParameters parameters;

    /**
 * Handle to SBW low-level interface.
 */
    protected SBWTools toSbw;

    /**
 * Create a new SBWCall object.
 * @param moduleId Module ID number.
 * @param serviceId Service ID number.
 * @param methodId Method ID number.
 * @param parameters Parameters list for the method.
 * @param toSbw SBW low-level API handle.
 */
    public SBWCall(int moduleId, int serviceId, int methodId, SBWParameters parameters, SBWTools toSbw) {
        this.moduleId = moduleId;
        this.serviceId = serviceId;
        this.methodId = methodId;
        this.parameters = parameters;
        this.toSbw = toSbw;
    }

    public DataList call(DataList parameters) throws ECallFailure {
        return call(parameters, moduleId);
    }

    /**
 * Call a method on a given module. This method is provided here in order
 * to allow both this and SBWPrivateCall to share the same calling code.
 * @param parameters Parameters set for call.
 * @param moduleId Module ID to call to.
 * @return Result of call.
 * @throws ECallFailure
 */
    public DataList call(DataList parameters, int moduleId) throws ECallFailure {
        DataBlockWriter writer = null;
        DataBlockReader reader = null;
        try {
            writer = writeData((SBWDataList) parameters);
            reader = toSbw.call(moduleId, serviceId, methodId, writer);
        } catch (SBWException e) {
            throw new ECallFailure(e.toString());
        } finally {
            writer.release();
        }
        return readData(reader);
    }

    public DataFactory dataFactory() {
        return SBWDataFactory.factory;
    }

    /**
 * Read data from a SBW DataBlockReader and pack into GUM Data objects.
 * @param reader Reader object.
 * @return Set of Data objects.
 * @throws ECallFailure
 */
    protected SBWDataList readData(DataBlockReader reader) throws ECallFailure {
        try {
            Collection c = reader.getIntoCollection();
            return new SBWDataList(c);
        } catch (SBWTypeMismatchException e) {
            throw new ECallFailure(e.toString());
        } catch (SBWUnsupportedObjectTypeException e) {
            throw new ECallFailure(e.toString());
        }
    }

    /**
 * Write data from GUM Data objects into a SBW DataBlockWriter.
 * @param data Set of Data objects.
 * @return DataBlockWriter object containing the Data objects' values.
 * @throws ECallFailure
 */
    protected DataBlockWriter writeData(SBWDataList data) throws ECallFailure {
        DataBlockWriter writer = new DataBlockWriter();
        data.writeTo(writer);
        return writer;
    }
}
