package ch.inacta.maven.platformserviceconfiguration.core.strategy;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.join;
import static org.keycloak.OAuth2Constants.PASSWORD;
import static org.keycloak.OAuth2Constants.USERNAME;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;

import com.google.common.collect.ImmutableList;

import ch.inacta.maven.platformserviceconfiguration.core.Plugin;

/**
 * Strategy to handle Keycloak specific configuration tasks.
 *
 * @author Inacta AG
 * @since 1.0.0
 */
class KeycloakStrategy {

    private static final String ADMIN_CLI = "admin-cli";
    private final Plugin plugin;
    private final Log logger;

    /**
     * Default constructor.
     *
     * @param plugin
     *            this plugin with all the called parameters
     */
    KeycloakStrategy(final Plugin plugin) {

        this.plugin = plugin;
        this.logger = plugin.getLog();
    }

    /**
     * Creates the configured Keycloak resource by processing all configured JSON files.
     */
    void importFiles() throws MojoExecutionException {

        final KeycloakResource keycloakResource = KeycloakResource.fromString(this.plugin.getResource()).orElseThrow(
                () -> new MojoExecutionException(format("Tag 'resource' must be one of the values: [%s]", join(KeycloakResource.values(), ", "))));

        final Keycloak keycloak = initializeKeycloakClient();

        for (final File jsonFile : this.plugin.getFilesToProcess().keySet()) {

            for (final String realm : keycloakResource.getRealms(this.plugin.getRealms())) {

                this.logger.info(format("Create %s %s with JSON [%s]", keycloakResource.toString(), realm.isBlank() ? "\b" : "for " + realm,
                        jsonFile.getName()));
                try (final InputStream inputStream = new FileInputStream(jsonFile)) {
                    keycloakResource.create(keycloak, realm, inputStream);
                } catch (final IOException e) {
                    throw new MojoExecutionException(format("Failed to open: [%s]", jsonFile.getAbsolutePath()));
                }

            }

        }
    }

    private Keycloak initializeKeycloakClient() {

        return KeycloakBuilder.builder().serverUrl(this.plugin.getEndpoint() + "/auth").realm("master")
                .username(this.plugin.getAuthorization().get(USERNAME)).password(this.plugin.getAuthorization().get(PASSWORD)).clientId(ADMIN_CLI)
                .build();
    }

    private enum KeycloakResource {

        REALMS {

            @Override
            void create(final Keycloak keycloak, final String realm, final InputStream inputStream) throws MojoExecutionException {

                final RealmRepresentation representation = loadJSON(inputStream, RealmRepresentation.class);
                keycloak.realms().create(representation);
            }

            @Override
            List<String> getRealms(final String realms) {

                // we just need one element, the realm name is irrelevant
                return ImmutableList.of("");
            }
        },
        CLIENTS {

            @Override
            void create(final Keycloak keycloak, final String realm, final InputStream inputStream) throws MojoExecutionException {

                final ClientRepresentation representation = loadJSON(inputStream, ClientRepresentation.class);
                keycloak.realm(realm).clients().create(representation);
            }
        },
        USERS {

            @Override
            void create(final Keycloak keycloak, final String realm, final InputStream inputStream) throws MojoExecutionException {

                final UserRepresentation representation = loadJSON(inputStream, UserRepresentation.class);
                keycloak.realm(realm).users().create(representation);
            }
        };

        /**
         * Creates the Keycloak resource with the Keycloak client.
         * 
         * @param keycloak
         *            the Keycloak client
         * @param realm
         *            the name of the realm for which the Keycloak resource has to be created
         * @param inputStream
         *            the JSON definition file of the Keycloak resource
         */
        abstract void create(final Keycloak keycloak, final String realm, final InputStream inputStream) throws MojoExecutionException;

        /**
         * Gets all realms from a comma separated {@code String} for which the Keycloak resource has to be created.
         *
         * @param realms
         *            the comma separated {@code String} with realm names
         * @return possible object is {@code List<String>}
         */
        List<String> getRealms(final String realms) {

            return asList(realms.replace(" ", "").split(","));
        }

        private static Optional<KeycloakResource> fromString(final String resource) {

            return Arrays.stream(KeycloakResource.values()).filter(keycloakResource -> keycloakResource.toString().equalsIgnoreCase(resource))
                    .findAny();
        }

        private static <T> T loadJSON(final InputStream is, final Class<T> type) throws MojoExecutionException {

            try {
                return JsonSerialization.readValue(is, type);
            } catch (final IOException e) {
                throw new MojoExecutionException("Failed to parse JSON file!");
            }
        }
    }
}