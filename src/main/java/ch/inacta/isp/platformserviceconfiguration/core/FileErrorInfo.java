package ch.inacta.isp.platformserviceconfiguration.core;

/**
 * Representation of a file error info
 *
 * @author INACTA AG
 * @since 1.0.0
 */
public class FileErrorInfo extends ErrorInfo {

    private final String filename;

    /**
     * Default constructor
     * 
     * @param filename
     *            of the file unable to process
     * @param errorCode
     *            of the error
     * @param message
     *            describing the error
     */
    public FileErrorInfo(final String filename, final int errorCode, final String message) {

        super(errorCode, message);
        this.filename = filename;
    }

    @Override
    public String toString() {

        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.filename).append(": ").append(super.toString());
        return stringBuilder.toString();
    }
}
