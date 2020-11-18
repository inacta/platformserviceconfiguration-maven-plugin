package ch.inacta.isp.platformserviceconfiguration.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import ch.inacta.isp.platformserviceconfiguration.core.strategy.AuthorizationStrategy;
import ch.inacta.isp.platformserviceconfiguration.core.strategy.KeycloakStrategy;
import ch.inacta.isp.platformserviceconfiguration.core.strategy.RabbitMQStrategy;

@Mojo(name = "rest-request")
public class Plugin extends AbstractMojo {

    @Parameter(property = "app")
    private String app;

    @Parameter(property = "fileSet")
    private FileSet fileSet;

    @Parameter(property = "fileSets")
    private final List<FileSet> fileSets = new ArrayList<>();

    @Override
    public void execute() throws MojoExecutionException {

        final List<ErrorInfo> errorInfos = new ArrayList<>();

        final AuthorizationStrategy authorizationStrategy = getStrategy();
    }

    private AuthorizationStrategy getStrategy() throws MojoExecutionException {

        if (getApp().equalsIgnoreCase("keycloak")) {
            return new KeycloakStrategy(getLog());
        } else {
            return new RabbitMQStrategy();
        }
    }

    private List<File> getFilesToProcess() throws MojoExecutionException {

        final List<File> files = new ArrayList<>();

        if (getFileSet() != null) {
            getFileSets().add(getFileSet());
        }

        for (final FileSet fileSet : getFileSets()) {
            if (fileSet != null) {
                final FileSetTransformer fileSetTransformer = new FileSetTransformer(getLog(), fileSet);
                files.addAll(fileSetTransformer.toFileList());
            }
        }

        return files;
    }

    /**
     * Gets the in the plugin configuration defined app.
     * 
     * @return the app
     * @throws MojoExecutionException
     */
    public String getApp() throws MojoExecutionException {

        if (this.app == null) {
            throw new MojoExecutionException("Tag 'app' has to be defined in configuration!");
        }

        return this.app;
    }

    /**
     * Gets the defined file set.
     * 
     * @return a file set
     */
    public FileSet getFileSet() {

        return this.fileSet;
    }

    /**
     * Gets the defined file sets.
     * 
     * @return a list of file sets
     */
    public List<FileSet> getFileSets() {

        return this.fileSets;
    }
}