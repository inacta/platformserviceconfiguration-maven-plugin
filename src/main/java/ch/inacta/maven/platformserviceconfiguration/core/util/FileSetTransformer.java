package ch.inacta.maven.platformserviceconfiguration.core.util;

import static java.lang.String.format;
import static org.codehaus.plexus.util.FileUtils.getFiles;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * Transformer to transform filesets.
 *
 * @author Inacta AG
 * @since 1.0.0
 */
public class FileSetTransformer {

    private final Log logger;
    private final FileSet fileSet;

    /**
     * Default constructor
     * 
     * @param logger
     *            to write logs
     * @param fileSet
     *            to transform
     */
    public FileSetTransformer(final Log logger, final FileSet fileSet) {

        this.logger = logger;
        this.fileSet = fileSet;
    }

    /**
     * Gets the searched files in the given directory.
     * 
     * @return a list of files
     * @throws MojoExecutionException
     *             if unable to get paths to filesets
     */
    public List<File> toFileList() throws MojoExecutionException {

        try {
            if (this.fileSet.getDirectory() != null) {
                final File directory = new File(this.fileSet.getDirectory());
                final String includes = toString(this.fileSet.getIncludes());
                final String excludes = toString(this.fileSet.getExcludes());
                return getFiles(directory, includes, excludes);
            } else {
                this.logger.warn(format("Fileset [%s] directory empty", this.fileSet.toString()));
                return new ArrayList<>();
            }
        } catch (final IOException e) {
            throw new MojoExecutionException(format("Unable to get paths to fileset [%s]", this.fileSet.toString()), e);
        }
    }

    private String toString(final List<String> strings) {

        final StringBuilder stringBuilder = new StringBuilder();
        for (final String string : strings) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append(", ");
            }
            stringBuilder.append(string);
        }
        return stringBuilder.toString();
    }
}
