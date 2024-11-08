package mipt.gui;

/**
 * Bundle for strings often required in standard dialogs and components
 * Accesses through GUIResourse interface
 * @author Evdokimov
 */
public class Bundle extends mipt.common.Bundle_en {

    /**
	 * 
	 */
    protected Object[][] initContents() {
        return new Object[][] { { "No", "No" }, { "Yes", "Yes" }, { "Cancel", "Cancel" }, { "OK", "OK" }, { "Apply", "Apply" }, { "NoToAll", "No to all" }, { "YesToAll", "Yes to all" }, { "Inform", "Information" }, { "Warning", "Warning" }, { "Error", "Error" }, { "Confirm", "Confirmation" }, { "Question", "Question" }, { "InputName", "Input name" }, { "isChanged.", "is changed." }, { "ObjectIsChanged.", "Editing object is changed." }, { "ObjectsAreChanged.", "Editing objects are changed." }, { "SaveChanges?", "Save changes?" }, { "ReallyDelete", "Are you sure to delete" }, { "OpenInNew?", "Open in a new window?" }, { "ReallyRewrite?", "Are you sure to rewrite?" }, { "FileReadOnly", "The file has read only access" }, { "CantSave", "Can't save data" }, { "Browse", "Browse..." }, { "Close", "Close" }, { "Edit", "Edit" }, { "Create", "Create" }, { "Delete", "Delete" }, { "Rename", "Rename" }, { "Add", "Add" }, { "Remove", "Remove" } };
    }
}
