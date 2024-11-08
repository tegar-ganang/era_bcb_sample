package org.tinymarbles.exception;

/**
 * This exception is thrown when an error accessing the storage occurs. It usually wraps another exception, describing
 * the transaction where the cause was throw.
 * 
 * @author Tiago Silveira
 */
public class GenericStorageException extends StorageException {

    private static final long serialVersionUID = 6807267395092321054L;

    private final boolean readOnly;

    private final String transactionName;

    /**
	 * Full constructor.
	 * 
	 * @param readOnly if the transaction where the exception occurred was read-only or read-write
	 * @param name the name of the transaction
	 * @param cause the cause of the exception
	 */
    public GenericStorageException(boolean readOnly, String name, Throwable cause) {
        super("error executing " + (readOnly ? "read" : "write") + " operation \"" + name + "\"", cause);
        this.readOnly = readOnly;
        this.transactionName = name;
    }

    /**
	 * Asks if the transaction being executed when this exception's cause was generated was read-only
	 * 
	 * @return <tt>true</tt> if the transaction was read-only, false otherwise
	 */
    public boolean getTransactionReadOnly() {
        return readOnly;
    }

    /**
	 * Gets the name of the transaction being executeed when this exception's cause was generated
	 * 
	 * @return the name of the transaction, or null if the transaction didnt't have a name
	 */
    public String getTransactionName() {
        return transactionName;
    }
}
