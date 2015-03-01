package com.prodigi.exception;

/**
 * Exception related to Trie and Node
 *
 * @author Wilkin Cheung
 */
public class TrieException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     */
    public TrieException() {
        super();
    }

    /**
     * Constructor
     *
     * @param message            error message
     * @param cause              root cause
     * @param enableSuppression  suppress?
     * @param writableStackTrace stack trace?
     */
    public TrieException(String message, Throwable cause,
                         boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * Constructor
     *
     * @param message error message
     * @param cause   root cause
     */
    public TrieException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor
     *
     * @param message error message
     */
    public TrieException(String message) {
        super(message);
    }

    /**
     * Constructor
     *
     * @param cause root cause
     */
    public TrieException(Throwable cause) {
        super(cause);
    }

}
