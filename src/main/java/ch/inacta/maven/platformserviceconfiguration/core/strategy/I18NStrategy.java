package ch.inacta.maven.platformserviceconfiguration.core.strategy;

import static ch.inacta.maven.platformserviceconfiguration.core.strategy.ResourceMode.CREATE;
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

    private static final String CREATED_AND_MODIFIED_BY = "platformserviceconfiguration-maven-plugin";
    private static final String CURRENT_TIMESTAMP = "current_timestamp";
    private static final String GENERATE_UUID = "md5(random()::text || clock_timestamp()::text)::uuid";

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

        this.plugin.getLog().info(format("- Generate SQL for [%s] with [%s] (discriminator: [%s], language: [%s])", this.i18NResource, file.getName(),
                getDiscriminator(file), getLanguage(file)));

        try {

            final ResultSet resultSet = statement.executeQuery(this.i18NResource.exists(file));
            if (resultSet.next()) {
                final String id = resultSet.getString("id");
                this.plugin.getLog().info(format("-- %s with id [%s] found, will be %s", this.i18NResource, id,
                        this.plugin.getMode() == CREATE ? "replaced" : "deleted"));
                statement.execute(this.i18NResource.delete(id));
            }

            if (this.plugin.getMode() == CREATE) {
                
                final String insertStatement = this.i18NResource.insert(file);

                this.plugin.getLog().info(
                        format("-- Data record of type [%s] is inserted with the following statment: %s", this.i18NResource.name(), insertStatement));

                statement.execute(insertStatement);
            }

        } catch (final SQLException e) {

            throw new MojoExecutionException(format("Failed to apply SQL for file: [%s]", file.getName()), e);

        } catch (final IOException e) {

            throw new MojoExecutionException(format("Failed to open file: [%s]", file.getName()), e);
        }
    }

    private static String getDiscriminator(final File file) {

        final int fileNameIndex = file.getPath().indexOf(file.getName());
        final String separator = file.getPath().substring(fileNameIndex - 1, fileNameIndex);
        final String pathWithoutFileName = file.getPath().substring(0, fileNameIndex - 1);
        return pathWithoutFileName.substring(pathWithoutFileName.lastIndexOf(separator) + 1);
    }

    private static String getLanguage(final File file) {

        final int underscoreIndex = file.getName().lastIndexOf("_");
        if (underscoreIndex == -1 || underscoreIndex < file.getName().substring(0, file.getName().indexOf(".")).length() - 3) {
            return null;
        }
        return file.getName().substring(underscoreIndex + 1, file.getName().indexOf("."));
    }

    private static String getFileNameWithoutExtension(final File file) {

        if (getLanguage(file) == null) {
            return file.getName().substring(0, file.getName().indexOf("."));
        }
        return file.getName().substring(0, file.getName().indexOf(".")).substring(0, file.getName().lastIndexOf("_"));
    }

    private enum I18NResource {

        SELECTION_LIST {

            @Override
            String insert(final File file) throws IOException {

                final String language = getLanguage(file);
                return "INSERT INTO selection_list(id, created, created_by, discriminator, last_modified, modified_by, config, language, type) VALUES("
                        + GENERATE_UUID
                        + ","
                        + CURRENT_TIMESTAMP
                        + ",'"
                        + CREATED_AND_MODIFIED_BY
                        + "',"
                        + "'"
                        + getDiscriminator(file)
                        + "',"
                        + CURRENT_TIMESTAMP
                        + ",'"
                        + CREATED_AND_MODIFIED_BY
                        + "',"
                        + "'"
                        + readFileToString(file, UTF_8).replace("'", "''")
                        + "',"
                        + (language == null ? "null" : "'" + language + "'")
                        + ",'"
                        + getFileNameWithoutExtension(file)
                        + "'"
                        + ");";
            }

            @Override
            String exists(final File file) {

                return "SELECT id FROM selection_list WHERE discriminator = '"
                        + getDiscriminator(file)
                        + "'"
                        + addLanguageExpression(file)
                        + " and type = '"
                        + getFileNameWithoutExtension(file)
                        + "';";
            }

            @Override
            String delete(final String id) {

                return "DELETE FROM selection_list WHERE id = '" + id + "'";
            }
        },
        TEMPLATE {

            @Override
            String exists(final File file) {

                return "SELECT id FROM template WHERE discriminator = '"
                        + getDiscriminator(file)
                        + "'"
                        + addLanguageExpression(file)
                        + " and key = '"
                        + getFileNameWithoutExtension(file)
                        + "';";
            }

            @Override
            String insert(final File file) throws IOException {

                final String language = getLanguage(file);
                return "INSERT INTO template(id, created, created_by, discriminator, last_modified, modified_by, key, language, template) VALUES("
                        + GENERATE_UUID
                        + ","
                        + CURRENT_TIMESTAMP
                        + ",'"
                        + CREATED_AND_MODIFIED_BY
                        + "',"
                        + "'"
                        + getDiscriminator(file)
                        + "',"
                        + CURRENT_TIMESTAMP
                        + ",'"
                        + CREATED_AND_MODIFIED_BY
                        + "',"
                        + "'"
                        + getFileNameWithoutExtension(file)
                        + "',"
                        + (language == null ? "null" : "'" + language + "'")
                        + ","
                        + "decode('"
                        + readFileToString(file, UTF_8).replace("'", "''")
                        + "', 'escape')"
                        + ");";
            }

            @Override
            String delete(final String id) {

                return "DELETE FROM template WHERE id = '" + id + "'";
            }
        },
        LABEL {

            @Override
            String exists(final File file) {

                return "SELECT id FROM i18n WHERE discriminator = '" + getDiscriminator(file) + "'" + addLanguageExpression(file) + ";";
            }

            @Override
            String insert(final File file) throws IOException {

                final String language = getLanguage(file);
                return "INSERT INTO i18n(id, created, created_by, discriminator, last_modified, modified_by, language, translation) VALUES("
                        + GENERATE_UUID
                        + ","
                        + CURRENT_TIMESTAMP
                        + ",'"
                        + CREATED_AND_MODIFIED_BY
                        + "',"
                        + "'"
                        + getDiscriminator(file)
                        + "',"
                        + CURRENT_TIMESTAMP
                        + ",'"
                        + CREATED_AND_MODIFIED_BY
                        + "',"
                        + (language == null ? "null" : "'" + language + "'")
                        + ","
                        + "decode('"
                        + readFileToString(file, UTF_8).replace("'", "''")
                        + "', 'escape')"
                        + ");";
            }

            @Override
            String delete(final String id) {

                return "DELETE FROM i18n WHERE id = '" + id + "'";
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
        abstract String insert(final File file) throws IOException;

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

        private static String addLanguageExpression(final File file) {

            final String language = getLanguage(file);
            return " and language " + (language == null ? "is null" : "= '" + language + "'");
        }
    }
}
