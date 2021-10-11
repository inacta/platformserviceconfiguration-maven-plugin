package ch.inacta.maven.platformserviceconfiguration.core.strategy;

import static ch.inacta.maven.platformserviceconfiguration.core.strategy.ResourceMode.CREATE;
import static ch.inacta.maven.platformserviceconfiguration.core.strategy.ResourceMode.DELETE;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.sql.DriverManager.getConnection;
import static java.util.Arrays.stream;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.lang3.StringUtils.join;
import static org.keycloak.OAuth2Constants.PASSWORD;
import static org.keycloak.OAuth2Constants.USERNAME;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import org.apache.maven.plugin.MojoExecutionException;

import ch.inacta.maven.platformserviceconfiguration.core.Plugin;

/**
 * Strategy to handle Postgres specific configuration tasks.
 *
 * @author Inacta AG
 * @since 2.2.0
 */
public class PostgresStrategy {

    private final DatabaseResource databaseResource;
    private final Plugin plugin;

    /**
     * Default constructor.
     *
     * @param plugin
     *            this plugin with all the called parameters
     */
    PostgresStrategy(final Plugin plugin) throws MojoExecutionException {

        this.plugin = plugin;
        this.databaseResource = DatabaseResource.fromString(this.plugin.getResource()).orElseThrow(
                () -> new MojoExecutionException(format("Tag 'resource' must be one of the values: [%s]", join(DatabaseResource.values(), ", "))));
    }

    /**
     * Call to execute the specified database statements.
     */
    void executeDatabaseStatements() throws MojoExecutionException {

        final String url = String.valueOf(this.plugin.getEndpoint());
        final String username = this.plugin.getAuthorization().get(USERNAME);
        final String password = this.plugin.getAuthorization().get(PASSWORD);

        this.plugin.getLog()
                .info(format("- %s [%s] with name: [%s]", this.plugin.getMode(), this.databaseResource.toString(), this.plugin.getResourceName()));

        try (final Connection connection = getConnection(url, username, password); final Statement statement = connection.createStatement()) {

            if (this.databaseResource == DatabaseResource.SCRIPTS) {
                applyScripts(statement);
            } else {
                createOrDeleteDatabaseResource(statement);
            }

        } catch (final SQLException e) {

            throw new MojoExecutionException(format("Failed to connect to database with url: [%s]", url), e);
        }
    }

    private void createOrDeleteDatabaseResource(final Statement statement) throws MojoExecutionException {

        try {

            if (this.plugin.getMode() == CREATE && !statement.executeQuery(this.databaseResource.exists(this.plugin.getResourceName())).next()) {
                statement.execute(this.databaseResource.create(this.plugin.getResourceName(), this.plugin.getResourcePassword()));
            } else if (this.plugin.getMode() == DELETE) {
                statement.execute(this.databaseResource.delete(this.plugin.getResourceName()));
            }

        } catch (final SQLException e) {

            throw new MojoExecutionException("Failed to execute statement", e);
        }
    }

    private void applyScripts(final Statement statement) throws MojoExecutionException {

        for (final File file : this.plugin.getFilesToProcess().keySet()) {
            try {

                statement.execute(readFileToString(file, UTF_8));

            } catch (final SQLException | IOException e) {

                throw new MojoExecutionException(format("Failed to apply SQL script: [%s]", file.getName()), e);
            }
        }
    }

    private enum DatabaseResource {

        DATABASE {

            @Override
            String exists(final String name) {

                return format("SELECT datname FROM pg_database WHERE datname = '%s'", name);
            }

            @Override
            String create(final String name, final String password) {

                return format("CREATE DATABASE \"%s\"", name); // double quotes are necessary to handle names with hyphen '-'
            }

            @Override
            String delete(final String name) {

                return format("DROP DATABASE IF EXISTS \"%s\"", name); // double quotes are necessary to handle names with hyphen '-'
            }
        },
        USER {

            @Override
            String exists(final String name) {

                return format("SELECT rolname FROM pg_roles WHERE rolname = '%s'", name);
            }

            @Override
            String create(final String name, final String password) {

                return format("CREATE USER \"%s\" WITH PASSWORD '%s'", name, password); // double quotes are necessary to handle names with hyphen '-'
            }

            @Override
            String delete(final String name) {

                return format("DROP USER IF EXISTS \"%s\"", name); // double quotes are necessary to handle names with hyphen '-'
            }
        },
        SCRIPTS {

            @Override
            String exists(final String name) {

                return "NULL";
            }

            @Override
            String create(final String name, final String password) {

                return "NULL";
            }

            @Override
            String delete(final String name) {

                return "NULL";
            }
        };

        /**
         * Checks if the specified database resource already exists.
         *
         * @param name
         *            the name of the database resource
         * @return exists statement
         */
        abstract String exists(final String name);

        /**
         * Creates the specified database resource.
         *
         * @param name
         *            the name of the database resource
         * @param password
         *            the password for the created database resource
         * @return create statement
         */
        abstract String create(final String name, final String password);

        /**
         * Deletes the specified database resource.
         *
         * @param name
         *            the name of the database resource
         * @return delete statement
         */
        abstract String delete(final String name);

        private static Optional<DatabaseResource> fromString(final String resource) {

            return stream(DatabaseResource.values()).filter(databaseResource -> databaseResource.toString().equalsIgnoreCase(resource)).findAny();
        }
    }
}
