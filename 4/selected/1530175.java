package org.rubypeople.rdt.refactoring.tests.core.generateaccessors;

public class AccessorSelection {

    private String className;

    private String attributeName;

    private boolean readerSelected;

    private boolean writerSelected;

    public AccessorSelection(String className, String attributeName, boolean readerSelected, boolean writerSelected) {
        this.className = className;
        this.attributeName = attributeName;
        this.readerSelected = readerSelected;
        this.writerSelected = writerSelected;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getClassName() {
        return className;
    }

    public boolean isReaderSelected() {
        return readerSelected;
    }

    public boolean isWriterSelected() {
        return writerSelected;
    }
}
