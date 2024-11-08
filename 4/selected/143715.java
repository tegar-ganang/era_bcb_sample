package com.saic.ship.fsml;

import org.apache.log4j.*;
import java.util.Vector;
import org.w3c.dom.*;
import com.saic.ship.util.GenericErrorHandler;

/**
 * This class serves the purpose of validating that a given FSML document
 * will work with the template file given. This class performs the validation
 * of the 2nd level FSML processing. This facilitates trapping reference
 * errors and other mistakes. DTD validation is performed by 1st level error
 * handling. 
 *
 * <p> Currently most of the methods on this class are not implemented. Only
 * attribute error checking works. 
 *
 * @author   Created By: <a href=mailto:j@ship.saic.com>J Bergquist</a>
 * @author   Last Revised By: $Author: bergquistj $
 * @see      FsmlValidation 
 * @see      FsmlDocument 
 * @see      FsmlProcessor
 * @since    Created On: 2005/03/01
 * @since    Last Revised on: $Date: 2005/03/03 20:12:09 $
 * @version  $Revision: 1.8 $
 *
 */
public class Fsml2ndLevelValidation implements ValidationListener {

    /**
   * Logger for this class
   */
    private static Logger logger = Logger.getLogger(Fsml2ndLevelValidation.class);

    /**
   * dom implementation for document
   *
   */
    private Document dom;

    private Vector checkedAtts;

    private GenericErrorHandler eh;

    /**
   * Constructor initializes all instance members, sets up dom and error 
   * handler. 
   *
   * Constructor detailled description
   *
   */
    public Fsml2ndLevelValidation(Document dom, GenericErrorHandler eh) {
        this.dom = dom;
        checkedAtts = new Vector();
        checkedAtts.add("size");
        checkedAtts.add("ndims");
        checkedAtts.add("dims");
        checkedAtts.add("dimIndex");
        checkedAtts.add("start");
        checkedAtts.add("end");
        checkedAtts.add("incr");
        checkedAtts.add("value");
        checkedAtts.add("readFormat");
        checkedAtts.add("writeFormat");
        this.eh = eh;
    }

    /**
   * No implementation currently. This method does nothing. 
   *
   */
    public void beginDocument() {
    }

    /**
   * No implementation currently. This method does nothing. 
   *
   * @param e an <code>Element</code> value
   */
    public void elementDeclared(Element e) {
    }

    /**
   * Basically, this method checks attributes for the declared and makes sure
   * it will work for this element. 
   *
   * @param e an <code>Element</code> value
   * @param a an <code>Attr</code> value
   */
    public void attributeDeclared(Element e, Attr a) {
        String name = a.getNodeName();
        if (checkedAtts.contains(name)) {
            if (!name.equals("readFormat") && !name.equals("writeFormat")) {
                if (!name.equals("dims")) {
                    if (!ReferenceAttribute.isValidIdRef(a, dom)) {
                        logger.warn("Error in Fsml2ndLevelValidation.");
                        eh.recordError(new InvalidIdRefException(e, a));
                    }
                } else {
                    if (!ReferenceAttributeList.isValidIdRef(a, dom)) {
                        logger.warn("Error in Fsml2ndLevelValidation.");
                        eh.recordError(new InvalidIdRefException(e, a));
                    }
                }
            } else {
                try {
                    FormatCache.getInstance().getFormat(a.getValue());
                } catch (Exception err) {
                    logger.warn("Error in Fsml2ndLevelValidation.");
                    eh.recordError(new InvalidFormatException(e, a));
                }
            }
        }
    }

    /**
   * No implementation
   *
   */
    public void endDocument() {
    }
}
