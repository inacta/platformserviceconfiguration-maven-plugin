package ch.inacta.maven.platformserviceconfiguration.core.strategy;

import org.apache.maven.plugin.MojoExecutionException;

import ch.inacta.maven.platformserviceconfiguration.core.Plugin;

/**
 * Enum for different application configuration strategies.
 *
 * @author Inacta AG
 * @since 1.0.0
 */
public enum ApplicationStrategy {

    KEYCLOAK {

        @Override
        public void execute(final Plugin plugin) throws MojoExecutionException {

            new KeycloakStrategy(plugin).importFiles();
        }
    },
    RABBITMQ {

        @Override
        public void execute(final Plugin plugin) throws MojoExecutionException {

            new RabbitMQStrategy(plugin).createQueue();
        }
    },
    MINIO {

        @Override
        public void execute(final Plugin plugin) throws MojoExecutionException {

            new MinioStrategy(plugin).createBucketAndUploadFiles();
        }
    };

    /**
     * Executes the request with the corresponding application strategy.
     *
     * @param plugin
     *            this plugin with all the called parameters
     * @throws MojoExecutionException
     *             if execution fails
     */
    public abstract void execute(final Plugin plugin) throws MojoExecutionException;
}