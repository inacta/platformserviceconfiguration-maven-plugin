package ch.inacta.maven.platformserviceconfiguration.core.strategy;

import static ch.inacta.maven.platformserviceconfiguration.core.strategy.ResourceMode.CREATE;
import static ch.inacta.maven.platformserviceconfiguration.core.strategy.ResourceMode.DELETE;
import static java.lang.String.format;
import static java.sql.DriverManager.getConnection;
import static org.keycloak.OAuth2Constants.PASSWORD;
import static org.keycloak.OAuth2Constants.USERNAME;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.maven.plugin.MojoExecutionException;

import ch.inacta.maven.platformserviceconfiguration.core.Plugin;

/**
 * Strategy to handle Postgres specific configuration tasks.
 *
 * @author Inacta AG
 * @since 1.0.0
 */
public class PostgresStrategy {

    private final Plugin plugin;

    /**
     * Default constructor.
     *
     * @param plugin
     *            this plugin with all the called parameters
     */
    PostgresStrategy(final Plugin plugin) {

        this.plugin = plugin;
    }

    /**
     * Simple call to create or delete a database.
     */
    void handleDatabase() throws MojoExecutionException {

        if (this.plugin.getResource().isEmpty()) {
            throw new MojoExecutionException("Tag 'resource' has to be defined with the database name!");
        }

        executeDatabaseScripts();
    }

    private void executeDatabaseScripts() throws MojoExecutionException {

        final String url = String.valueOf(this.plugin.getEndpoint());
        final String username = this.plugin.getAuthorization().get(USERNAME);
        final String password = this.plugin.getAuthorization().get(PASSWORD);

        final String existsStatement = format("SELECT datname FROM pg_database WHERE datname = '%s'", this.plugin.getResource());
        final String createStatement = format("CREATE DATABASE \"%s\"", this.plugin.getResource()); // double quotes are necessary to handle names
                                                                                                    // with hyphen '-'
        final String dropStatement = format("DROP DATABASE IF EXISTS \"%s\"", this.plugin.getResource());

        try (final Connection connection = getConnection(url, username, password); final Statement statement = connection.createStatement()) {

            final ResultSet resultSet = statement.executeQuery(existsStatement);
            if (this.plugin.getMode() == CREATE && !resultSet.next()) {
                statement.execute(createStatement);
            } else if (this.plugin.getMode() == DELETE) {
                statement.execute(dropStatement);
            }

        } catch (final SQLException e) {
            throw new MojoExecutionException(format("Failed to connect to database with url: %s", url), e);
        }
    }
}
