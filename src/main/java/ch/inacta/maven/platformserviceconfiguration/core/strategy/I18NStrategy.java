package ch.inacta.maven.platformserviceconfiguration.core.strategy;

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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import org.apache.maven.plugin.MojoExecutionException;

import ch.inacta.maven.platformserviceconfiguration.core.Plugin;

/**
 * Strategy to handle I18N specific configuration tasks.
 *
 * @author Inacta AG
 * @since 2.3.1
 */
class I18NStrategy {

    private final Plugin plugin;
    private final I18NResource i18NResource;

    /**
     * Default constructor.
     *
     * @param plugin
     *            this plugin with all the called parameters
     */
    I18NStrategy(final Plugin plugin) throws MojoExecutionException {

        this.plugin = plugin;
        this.i18NResource = I18NResource.fromString(this.plugin.getResource()).orElseThrow(
                () -> new MojoExecutionException(format("Tag 'resource' must be one of the values: [%s]", join(I18NResource.values(), ", "))));
    }

    /**
     * Creates the SQL statements for the specified resources.
     */
    void createAndExecuteSQLStatement() throws MojoExecutionException {

        final String url = String.valueOf(this.plugin.getEndpoint());
        final String username = this.plugin.getAuthorization().get(USERNAME);
        final String password = this.plugin.getAuthorization().get(PASSWORD);

        try (final Connection connection = getConnection(url, username, password); final Statement statement = connection.createStatement()) {

            for (final File file : this.plugin.getFilesToProcess().keySet()) {
                processFile(statement, file);
            }

        } catch (final SQLException e) {

            throw new MojoExecutionException(format("Failed to connect to database with url: [%s]", url), e);
        }
    }

    private void processFile(final Statement statement, final File file) throws MojoExecutionException {

        this.plugin.getLog()
                .info(format("- Generate [%s] SQL for table [%s] with file: [%s]", this.plugin.getMode(), this.i18NResource, file.getName()));

        try {

            final ResultSet resultSet = statement.executeQuery(this.i18NResource.exists(file));
            if (resultSet.next()) {

                final String id = resultSet.getString("id");
                this.plugin.getLog().info(format("- Entity already exists, going to delete it first with id: [%s]", id));
                statement.execute(this.i18NResource.delete(id));
            }
            statement.execute(this.i18NResource.insert(file));

        } catch (final SQLException e) {

            throw new MojoExecutionException(format("Failed to apply SQL for file: [%s]", file.getName()), e);
        }
    }

    private enum I18NResource {

        SELECTION_LIST {

            @Override
            String insert(final File file) throws MojoExecutionException {

                try {

                    final String language = getLanguage(file);
                    return "INSERT INTO selection_list(id, created, created_by, discriminator, last_modified, modified_by, config, language, type) VALUES("
                            + "md5(random()::text || clock_timestamp()::text)::uuid,"
                            + "current_timestamp,"
                            + "'platformserviceconfiguration-maven-plugin',"
                            + "'"
                            + getDiscriminator(file)
                            + "',"
                            + "current_timestamp,"
                            + "'platformserviceconfiguration-maven-plugin',"
                            + "'"
                            + readFileToString(file, UTF_8).replace("'", "''")
                            + "',"
                            + (language == null ? "null" : "'" + language + "'")
                            + ",'"
                            + getType(file)
                            + "'"
                            + ");";

                } catch (final IOException e) {

                    throw new MojoExecutionException(format("Failed to open file: [%s]", file.getName()), e);
                }
            }

            @Override
            String exists(final File file) {

                final String language = getLanguage(file);
                return "SELECT id FROM selection_list WHERE discriminator = '"
                        + getDiscriminator(file)
                        + "' and language "
                        + (language == null ? "is null" : "= '" + language + "'")
                        + " and type = '"
                        + getType(file)
                        + "';";
            }

            @Override
            String delete(final String id) {

                return "DELETE FROM selection_list WHERE id = '" + id + "'";
            }
        };

        /**
         * Checks if the specified resource already exists.
         *
         * @param file
         *            the file with the content of the resource
         * @return exists statement
         */
        abstract String exists(final File file);

        /**
         * Inserts the specified resource.
         *
         * @param file
         *            the file with the content of the resource
         * @return insert statement
         */
        abstract String insert(final File file) throws MojoExecutionException;

        /**
         * Deletes the specified resource.
         *
         * @param id
         *            the id of the resource
         * @return delete statement
         */
        abstract String delete(final String id);

        private static Optional<I18NResource> fromString(final String resource) {

            return stream(I18NResource.values()).filter(i18NResource -> i18NResource.toString().equalsIgnoreCase(resource)).findAny();
        }

        private static String getDiscriminator(final File file) {

            final int indexOfFileName = file.getPath().indexOf(file.getName());
            final String separator = file.getPath().substring(indexOfFileName - 1, indexOfFileName);
            final String pathWithoutFileName = file.getPath().substring(0, indexOfFileName - 1);
            return pathWithoutFileName.substring(pathWithoutFileName.lastIndexOf(separator) + 1);
        }

        private static String getLanguage(final File file) {

            final int underscore = file.getName().indexOf("_");
            if (underscore == -1) {
                return null;
            }
            return file.getName().substring(underscore + 1, file.getName().indexOf("."));
        }

        private static String getType(final File file) {

            final int underscore = file.getName().indexOf("_");
            return file.getName().substring(0, underscore == -1 ? file.getName().indexOf(".") : underscore);
        }
    }
}