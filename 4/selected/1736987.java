package com.kni.etl.ketl;

import org.w3c.dom.DOMException;
import com.kni.etl.EngineConstants;
import com.kni.etl.ketl.exceptions.KETLThreadException;
import com.kni.etl.util.XMLHelper;

/**
 * The Class ETLOutPort.
 */
public class ETLOutPort extends ETLPort {

    /**
     * Instantiates a new ETL out port.
     * 
     * @param esOwningStep the es owning step
     * @param esSrcStep the es src step
     */
    public ETLOutPort(ETLStep esOwningStep, ETLStep esSrcStep) {
        super(esOwningStep, esSrcStep);
    }

    /**
     * Gets the channel.
     * 
     * @return the channel
     */
    public String getChannel() {
        return (this.getXMLConfig()).getAttribute("CHANNEL");
    }

    @Override
    public String getPortName() throws DOMException, KETLThreadException {
        if (this.mstrName != null) return this.mstrName;
        if (XMLHelper.getElementsByName(this.getXMLConfig(), "OUT", "CHANNEL", null) == null) (this.getXMLConfig()).setAttribute("CHANNEL", "DEFAULT");
        this.mstrName = XMLHelper.getAttributeAsString(this.getXMLConfig().getAttributes(), ETLPort.NAME_ATTRIB, null);
        if (this.isConstant() == false && this.containsCode() == false) {
            ETLPort port = this.getAssociatedInPort();
            if (this.mstrName == null && port != null) {
                (this.getXMLConfig()).setAttribute("NAME", port.mstrName);
                this.mstrName = port.mstrName;
            }
        }
        return this.mstrName;
    }

    @Override
    public String generateCode(int portReferenceIndex) throws KETLThreadException {
        if (this.isUsed() == false) return "";
        String baseCode = XMLHelper.getTextContent(this.getXMLConfig());
        if (baseCode == null || baseCode.length() == 0) baseCode = "null"; else {
            String[] params = EngineConstants.getParametersFromText(baseCode);
            for (String element : params) {
                ETLInPort inport = this.mesStep.getInPort(element);
                if (inport == null) {
                    throw new KETLThreadException("Source port " + element + " for step " + this.mesStep.getName() + " could not be found, has it been declared as an IN port", this);
                } else {
                    baseCode = EngineConstants.replaceParameter(baseCode, element, inport.generateReference());
                }
            }
        }
        return this.getCodeGenerationReferenceObject() + "[" + this.mesStep.getUsedPortIndex(this) + "] = " + baseCode;
    }

    @Override
    public String getCodeGenerationReferenceObject() {
        return this.mesStep.getCodeGenerationOutputObject(this.getXMLConfig().getAttribute("CHANNEL"));
    }
}
