package ch.inacta.maven.platformserviceconfiguration.core;

/**
 * Representation of a file error info
 *
 * @author Inacta AG
 * @since 1.0.0
 */
public class FileErrorInfo extends ErrorInfo {

    private final String filename;

    /**
     * Default constructor
     * 
     * @param filename
     *            of the file unable to process
     * @param errorInfo
     *            containing the error infos
     */
    public FileErrorInfo(final String filename, final ErrorInfo errorInfo) {

        super(errorInfo.errorCode, errorInfo.message);
        this.filename = filename;
    }

    @Override
    public String toString() {

        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.filename).append(": ").append(super.toString());
        return stringBuilder.toString();
    }
}
