package ch.inacta.maven.platformserviceconfiguration.core.model;

/**
 * Representation of an error info
 *
 * @author Inacta AG
 * @since 1.0.0
 */
public class ErrorInfo {

    protected final int errorCode;
    protected final String message;

    /**
     * Default constructor
     * 
     * @param errorCode
     *            of the error
     * @param message
     *            describing the error
     */
    public ErrorInfo(final int errorCode, final String message) {

        this.errorCode = errorCode;
        this.message = message;
    }

    @Override
    public String toString() {

        return " [" + this.errorCode + ":" + this.message + "]";
    }
}
