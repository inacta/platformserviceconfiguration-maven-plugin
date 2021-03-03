package ch.inacta.maven.platformserviceconfiguration.core;

import static java.lang.String.format;
import static java.util.Objects.requireNonNullElseGet;
import static org.apache.commons.lang3.StringUtils.join;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import ch.inacta.maven.platformserviceconfiguration.core.strategy.ApplicationStrategy;
import ch.inacta.maven.platformserviceconfiguration.core.util.FileSetTransformer;

/**
 * Platformservice configuration plugin implementation.
 *
 * @author Inacta AG
 * @since 1.0.0
 */
@Mojo(name = "configure")
public class Plugin extends AbstractMojo {

    @Parameter(property = "application",
            required = true)
    private ApplicationStrategy application;

    @Parameter(property = "authorization",
            required = true)
    private Map<String, String> authorization;

    @Parameter(property = "endpoint",
            required = true)
    private URI endpoint;

    @Parameter(property = "resource")
    private String resource;

    @Parameter(property = "fileSet")
    private FileSet fileSet;

    @Parameter(property = "fileSets")
    private final List<FileSet> fileSets = new ArrayList<>();

    @Parameter(property = "realms")
    private String realms;

    @Override
    public void execute() throws MojoExecutionException {

        if (!this.authorization.containsKey("username")) {
            throw new MojoExecutionException("Tag 'username' has to be defined in authorization!");
        }
        if (!this.authorization.containsKey("password")) {
            throw new MojoExecutionException("Tag 'password' has to be defined in authorization!");
        }

        getLog().info(format("Selected application strategy: [%s]", this.application));
        getLog().info(format("Endpoint: [%s]", this.endpoint));

        this.application.execute(this);
    }

    /**
     * Gets all files which have to be processed according to the configuration.
     *
     * @return possible object is {@code List<File>}
     */
    public List<File> getFilesToProcess() throws MojoExecutionException {

        if (getFileSet() != null && getFileSet().getDirectory() != null) {
            getFileSets().add(getFileSet());
        }

        final List<File> filesToProcess = new ArrayList<>();
        for (final FileSet set : getFileSets()) {
            final FileSetTransformer fileSetTransformer = new FileSetTransformer(getLog(), set);
            filesToProcess.addAll(fileSetTransformer.toFileList());
            getLog().info(format("Files found: %s", join(fileSetTransformer.toFileList(), "\n")));
        }

        return filesToProcess;
    }

    /**
     * Gets the value of the authorization property.
     *
     * @return possible object is {@code Map<String, String>}
     */
    public Map<String, String> getAuthorization() {

        return this.authorization;
    }

    /**
     * Gets the value of the endpoint property.
     *
     * @return possible object is {@link URI}
     */
    public URI getEndpoint() {

        return this.endpoint;
    }

    /**
     * Gets the value of the resource property.
     *
     * @return possible object is {@link String}
     */
    public String getResource() {

        return requireNonNullElseGet(this.resource, String::new);
    }

    /**
     * Gets the value of the realms property.
     *
     * @return possible object is {@link String}
     */
    public String getRealms() {

        return requireNonNullElseGet(this.realms, String::new);
    }

    private FileSet getFileSet() {

        return this.fileSet;
    }

    private List<FileSet> getFileSets() {

        return requireNonNullElseGet(this.fileSets, ArrayList::new);
    }
}