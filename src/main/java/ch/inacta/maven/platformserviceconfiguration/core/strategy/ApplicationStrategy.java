package ch.inacta.maven.platformserviceconfiguration.core.strategy;

import static ch.inacta.maven.platformserviceconfiguration.core.strategy.ResourceMode.CREATE;
import static java.lang.String.format;

import org.apache.maven.plugin.MojoExecutionException;

import ch.inacta.maven.platformserviceconfiguration.core.Plugin;

/**
 * Enum for different application configuration strategies.
 *
 * @author Inacta AG
 * @since 2.0.0
 */
public enum ApplicationStrategy {

    KEYCLOAK {

        @Override
        public void execute(final Plugin plugin) throws MojoExecutionException {

            new KeycloakStrategy(plugin).processJSONFiles();
        }
    },
    RABBITMQ {

        @Override
        public void execute(final Plugin plugin) throws MojoExecutionException {

            new RabbitMQStrategy(plugin).invokeAPI();
        }
    },
    MINIO {

        @Override
        public void execute(final Plugin plugin) throws MojoExecutionException {

            if (plugin.getMode() == CREATE) {
                new MinioStrategy(plugin).createBucketAndUploadFiles();
            } else {
                throw new MojoExecutionException(format("Mode [%s] is not supported for MINIO application strategy", plugin.getMode()));
            }
        }
    },
    POSTGRES {

        @Override
        public void execute(final Plugin plugin) throws MojoExecutionException {

            new PostgresStrategy(plugin).executeDatabaseStatements();
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