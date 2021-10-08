package ch.inacta.maven.platformserviceconfiguration.core;

import static ch.inacta.maven.platformserviceconfiguration.core.strategy.ResourceMode.CREATE;
import static java.lang.String.format;
import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;
import static org.apache.commons.lang3.StringUtils.stripStart;
import static org.keycloak.OAuth2Constants.PASSWORD;
import static org.keycloak.OAuth2Constants.USERNAME;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import ch.inacta.maven.platformserviceconfiguration.core.strategy.ApplicationStrategy;
import ch.inacta.maven.platformserviceconfiguration.core.strategy.ResourceMode;
import ch.inacta.maven.platformserviceconfiguration.core.util.FileSetTransformer;

/**
 * Maven plugin for configuring platform services.
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

    @Parameter(property = "bucket")
    private String bucket;

    @Parameter(property = "relative")
    private boolean relative;

    @Parameter(property = "mode")
    private ResourceMode mode;

    @Parameter(property = "resourceName")
    private String resourceName;

    @Parameter(property = "resourcePassword")
    private String resourcePassword;

    @Override
    public void execute() throws MojoExecutionException {

        validateAuthorization();

        getLog().info(format("[%s] resources for application [%s] on endpoint [%s]", getMode(), this.application, getEndpoint()));

        this.application.execute(this);
    }

    /**
     * Gets all files which have to be processed according to the configuration, including their relative path.
     *
     * @throws MojoExecutionException
     *             if execution fails
     * @return possible object is {@code Map<File, String>}
     */
    public Map<File, String> getFilesToProcess() throws MojoExecutionException {

        if (getFileSet() != null && getFileSet().getDirectory() != null) {
            getFileSets().add(getFileSet());
        }

        final Map<File, String> filesToProcess = new HashMap<>();
        for (final FileSet set : getFileSets()) {

            final FileSetTransformer fileSetTransformer = new FileSetTransformer(getLog(), set);
            fileSetTransformer.toFileList().forEach(file -> {
                final String relativePath = stripStart(file.getAbsolutePath().replace("\\", "/").replace(set.getDirectory().replace("\\", "/"), ""),
                        "/");
                filesToProcess.put(file, relativePath);
            });
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

    /**
     * Gets the value of the bucket property.
     *
     * @return possible object is {@link String}
     */
    public String getBucket() {

        return requireNonNullElseGet(this.bucket, String::new);
    }

    /**
     * Gets the value of the relative property.
     *
     * @return possible object is boolean
     */
    public boolean isRelative() {

        return requireNonNullElse(this.relative, false);
    }

    /**
     * Gets the value of the mode property.
     *
     * @return possible object is {@link ResourceMode}
     */
    public ResourceMode getMode() {

        return requireNonNullElse(this.mode, CREATE);
    }

    /**
     * Gets the value of the resource name property.
     *
     * @return possible object is {@link String}
     */
    public String getResourceName() {

        return requireNonNullElseGet(this.resourceName, String::new);
    }

    /**
     * Gets the value of the resource password property.
     *
     * @return possible object is {@link String}
     */
    public String getResourcePassword() {

        return requireNonNullElseGet(this.resourcePassword, String::new);
    }

    private void validateAuthorization() throws MojoExecutionException {

        if (!this.authorization.containsKey(USERNAME)) {
            throw new MojoExecutionException("Tag 'username' has to be defined in authorization!");
        }
        if (!this.authorization.containsKey(PASSWORD)) {
            throw new MojoExecutionException("Tag 'password' has to be defined in authorization!");
        }
    }

    private FileSet getFileSet() {

        return this.fileSet;
    }

    private List<FileSet> getFileSets() {

        return requireNonNullElseGet(this.fileSets, ArrayList::new);
    }
}