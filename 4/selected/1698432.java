package org.openlms.datamodel.cmi;

import org.openlms.DebugIndicator;
import java.io.Serializable;

public class Element implements Serializable {

    private String value;

    private String type;

    private boolean writeable;

    private boolean implemented;

    private boolean initialized;

    private boolean readable;

    private boolean mandatory;

    private String vocabularyType;

    /****************************************************************************
    **
    ** Method:  Constructor
    ** Input:   String inValue - string to set the value attribute
    **          String inType - string to set the type attribute
    **          boolean writeableFlag - flag indicating if the element is
    **                                  writeable
    **          boolean readableFlag - flag indicating if the element is
    **                                 readable
    **          boolean mandatoryFlag - flag indicating if the element is
    **                                    mandatory
    ** Output:  none
    **
    ** Description:  The Constructor sets up the class and initalizes
    ** 	             all of its attributes
    **
    ***************************************************************************/
    public Element(String inValue, String inType, String inVocab, boolean writeableFlag, boolean readableFlag, boolean mandatoryFlag) {
        value = inValue;
        type = inType;
        vocabularyType = inVocab;
        writeable = writeableFlag;
        readable = readableFlag;
        if (inValue.equalsIgnoreCase("")) {
            initialized = false;
        } else {
            initialized = true;
        }
        mandatory = mandatoryFlag;
        implemented = true;
    }

    /****************************************************************************
    **
    ** Method:  Default Constructor
    ** Input:   none
    ** Output:  none
    **
    ** Description:  The Default Constructor sets up the class and initalizes
    ** 	             all of its attributes to default values
    **
    ***************************************************************************/
    public Element() {
        value = new String("");
        type = new String("");
        vocabularyType = new String("");
        writeable = false;
        readable = false;
        initialized = false;
        implemented = false;
        mandatory = false;
    }

    /****************************************************************************
   **
   ** Method:  isWriteable()
   ** Input:   none
   ** Output:  boolean result - indicates whether or not the element is
   **                           writeable
   **
   ** Description: Indicates whether or not the element is writeable (can be
   **              set with an LMSSetValue() request.
   **
   ***************************************************************************/
    public boolean isWriteable() {
        return writeable;
    }

    /****************************************************************************
   **
   ** Method:  isImplemented
   ** Input:   none
   ** Output:  boolean result - indicates whether or not the element is
   **                           implemented
   **
   ** Description: Indicates whether or not the element is implemented.
   **
   ***************************************************************************/
    public boolean isImplemented() {
        return implemented;
    }

    /****************************************************************************
   **
   ** Method:  isInitialized
   ** Input:   none
   ** Output:  boolean result - indicates whether or not the element is
   **                           initialized.
   **
   ** Description: Indicates whether or not the element is initialized.
   **
   ***************************************************************************/
    public boolean isInitialized() {
        return initialized;
    }

    /****************************************************************************
   **
   ** Method: isReadable
   ** Input:  none
   ** Output: boolean result - indicates whether or not the element is
   **                          readable
   **
   ** Description:  Indicates whether or not the element is able to be accessed.
   **               (accessed via an LMSGetValue();
   **
   ***************************************************************************/
    public boolean isReadable() {
        return readable;
    }

    /****************************************************************************
    **
    ** Method: isMandatory
    ** Input:  none
    ** Output: boolean result - indicates whether or not the element is
    **                          mandatory
    **
    ** Description:  Indicates whether or not the element was mandatory
    **
    ***************************************************************************/
    public boolean isMandatory() {
        return mandatory;
    }

    /****************************************************************************
    **
    ** Method: getVocabularyType()
    ** Input:  none
    ** Output: String - the specific vocabulary type
    **
    ** Description:  Returns the vocabulary type of Element.  Only those
    **               Elements that have a type of CMIVocabulary will have
    **               a valid Vocabulary Type.  All others will be set to
    **               "NULL"
    **
    ***************************************************************************/
    public String getVocabularyType() {
        return vocabularyType;
    }

    /****************************************************************************
   **
   ** Method: getValue
   ** Input:  none
   ** Output: String value - the value of the element
   **
   ** Description:  Returns the value held by the element. 
   **
   ***************************************************************************/
    public String getValue() {
        return value;
    }

    /****************************************************************************
    **
    ** Method: setValue
    ** Input:  String inValue - value to use in setting the element
    ** Output: none
    **
    ** Description:  Sets the elements value determined by the input argument.
    **
    ***************************************************************************/
    public void setValue(String inValue) {
        value = inValue;
        initialized = true;
    }

    /****************************************************************************
    **
    ** Method: getType
    ** Input:  none
    ** Output: String - the type of the element
    **
    ** Description:  Returns the type of the element
    **
    ***************************************************************************/
    public String getType() {
        return type;
    }

    /****************************************************************************
    **
    ** Method: setElement
    ** Input:  Element - the element to use to set up this element
    ** Output: none
    **
    ** Description:  Sets this object's attributes in accordance with the
    **               input element. (Acts like a copy constructor)
    **
    ***************************************************************************/
    protected void setElement(Element e) {
        this.type = e.getType();
        this.value = e.getValue();
        this.vocabularyType = e.getVocabularyType();
        this.implemented = e.isImplemented();
        this.initialized = e.isInitialized();
        this.mandatory = e.isMandatory();
        this.readable = e.isReadable();
        this.writeable = e.isWriteable();
    }

    /****************************************************************************
    **
    ** Method: showElement
    ** Input: none
    ** Output: none
    **
    ** Description:  Sends all of the attributes to System.out in a readable
    **               manner.
    **
    ***************************************************************************/
    public void showElement() {
        if (DebugIndicator.ON) {
            System.out.println("     Value         " + getValue());
            System.out.println("     Type          " + getType());
            System.out.println("     Vocab Type    " + getVocabularyType());
            System.out.println("     Writeable     " + isWriteable());
            System.out.println("     Readable      " + isReadable());
            System.out.println("     Mandatory     " + isMandatory());
            System.out.println("     Implemented   " + isImplemented());
            System.out.println("     Intialized    " + isInitialized());
        }
    }
}
