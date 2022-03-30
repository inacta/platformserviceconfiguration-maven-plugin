package ch.inacta.maven.platformserviceconfiguration.core.strategy;

import static ch.inacta.maven.platformserviceconfiguration.core.strategy.ResourceMode.CREATE;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static org.apache.commons.lang3.StringUtils.join;
import static org.keycloak.OAuth2Constants.PASSWORD;
import static org.keycloak.OAuth2Constants.USERNAME;
import static org.keycloak.util.JsonSerialization.readValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import ch.inacta.maven.platformserviceconfiguration.core.Plugin;
import ch.inacta.maven.platformserviceconfiguration.core.util.MavenPropertiesSubstitutor;

/**
 * Strategy to handle Keycloak specific configuration tasks.
 *
 * @author Inacta AG
 * @since 1.0.0
 */
class KeycloakStrategy {

    private static final String ADMIN_CLI = "admin-cli";

    private static final String JSON_PARSE_ERROR_MESSAGE = "Failed to parse JSON file!";

    private final Plugin plugin;
    private final Log logger;
    private final Keycloak keycloak;
    private final KeycloakResource keycloakResource;

    private MavenPropertiesSubstitutor envSubstitutor;

    /**
     * Default constructor.
     *
     * @param plugin
     *            this plugin with all the called parameters
     */
    KeycloakStrategy(final Plugin plugin) throws MojoExecutionException {

        this.plugin = plugin;
        this.logger = plugin.getLog();
        this.keycloakResource = KeycloakResource.fromString(this.plugin.getResource()).orElseThrow(
                () -> new MojoExecutionException(format("Tag 'resource' must be one of the values: [%s]", join(KeycloakResource.values(), ", "))));
        this.keycloak = initializeKeycloakClient();
    }

    /**
     * Creates or deletes the configured Keycloak resource by processing all configured JSON files.
     */
    void processJSONFiles() throws MojoExecutionException {

        for (final File jsonFile : this.plugin.getFilesToProcess().keySet()) {

            for (final String realm : this.keycloakResource.getRealms(this.plugin.getRealms())) {

                this.logger.info(format("- %s [%s] %s with JSON: [%s]", this.plugin.getMode(), this.keycloakResource.toString(),
                        realm.isBlank() ? "\b" : "for realm [" + realm + "]", jsonFile.getName()));

                this.envSubstitutor = new MavenPropertiesSubstitutor(this.plugin.getProperties(), realm);

                try (final InputStream inputStream = new FileInputStream(jsonFile)) {

                    if (this.plugin.getMode() == CREATE) {
                        this.keycloakResource.create(this.plugin, this.keycloak, realm, inputStream, this.envSubstitutor);
                    } else {
                        this.keycloakResource.delete(this.keycloak, realm, inputStream);
                    }

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
            void create(final Plugin plugin, final Keycloak keycloak, final String realm, final InputStream inputStream,
                    final MavenPropertiesSubstitutor envSubstitutor) throws MojoExecutionException {

                String fileContent = loadJSON(inputStream);

                final RealmRepresentation representation = loadJSON(envSubstitutor.replace(fileContent), RealmRepresentation.class);

                final boolean isPresent = keycloak.realms().findAll().stream()
                        .anyMatch(realmRepresentation -> realmRepresentation.getId().equals(representation.getId()));
                if (!isPresent) {
                    keycloak.realms().create(representation);
                }
            }

            @Override
            List<String> getRealms(final String realms) {

                // we just need one element, the realm name is irrelevant
                return List.of("");
            }

            @Override
            void delete(final Keycloak keycloak, final String realm, final InputStream inputStream) throws MojoExecutionException {

                final RealmRepresentation representation = loadJSON(inputStream, RealmRepresentation.class);

                final boolean isPresent = keycloak.realms().findAll().stream()
                        .anyMatch(realmRepresentation -> realmRepresentation.getId().equals(representation.getId()));
                if (isPresent) {
                    keycloak.realm(representation.getRealm()).remove();
                }
            }
        },
        CLIENTS {

            @Override
            void create(final Plugin plugin, final Keycloak keycloak, final String realm, final InputStream inputStream,
                    final MavenPropertiesSubstitutor envSubstitutor) throws MojoExecutionException {

                String fileContent = loadJSON(inputStream);

                final ClientRepresentation representation = loadJSON(envSubstitutor.replace(fileContent), ClientRepresentation.class);

                final boolean isNotPresent = keycloak.realm(realm).clients().findByClientId(representation.getClientId()).isEmpty();
                if (isNotPresent) {
                    keycloak.realm(realm).clients().create(representation);
                }
            }

            @Override
            void delete(final Keycloak keycloak, final String realm, final InputStream inputStream) throws MojoExecutionException {

                final ClientRepresentation representation = loadJSON(inputStream, ClientRepresentation.class);

                final boolean isRealmPresent = keycloak.realms().findAll().stream()
                        .anyMatch(realmRepresentation -> realmRepresentation.getId().equals(realm));
                final boolean isPresent = isRealmPresent && !keycloak.realm(realm).clients().findByClientId(representation.getClientId()).isEmpty();
                if (isPresent) {
                    keycloak.realm(realm).clients().findByClientId(representation.getClientId())
                            .forEach(client -> keycloak.realm(realm).clients().get(client.getId()).remove());
                }
            }
        },
        USERS {

            @Override
            void create(final Plugin plugin, final Keycloak keycloak, final String realm, final InputStream inputStream,
                    final MavenPropertiesSubstitutor envSubstitutor) throws MojoExecutionException {

                String fileContent = loadJSON(inputStream);

                final UserRepresentation representation = loadJSON(envSubstitutor.replace(fileContent), UserRepresentation.class);
                keycloak.realm(realm).users().create(representation);

                final List<RoleRepresentation> rolesToAdd = new ArrayList<>();
                representation.getRealmRoles().forEach(role -> rolesToAdd.add(keycloak.realm(realm).roles().get(role).toRepresentation()));

                keycloak.realm(realm).users().search(representation.getUsername()).forEach(userRepresentation -> {

                    final UserResource userResource = keycloak.realm(realm).users().get(userRepresentation.getId());
                    userResource.roles().realmLevel().add(rolesToAdd);

                    representation.getClientRoles().entrySet().forEach(clientRoleEntry -> {

                        ClientRepresentation clientRepresentation = keycloak.realm(realm).clients().findByClientId(clientRoleEntry.getKey()).get(0);

                        final List<RoleRepresentation> clientRolesToAdd = new ArrayList<>();
                        clientRoleEntry.getValue().forEach(clientRole -> clientRolesToAdd
                                .add(keycloak.realm(realm).clients().get(clientRepresentation.getId()).roles().get(clientRole).toRepresentation()));

                        userResource.roles().clientLevel(clientRepresentation.getId()).add(clientRolesToAdd);
                    });
                });
            }

            @Override
            void delete(final Keycloak keycloak, final String realm, final InputStream inputStream) throws MojoExecutionException {

                final UserRepresentation representation = loadJSON(inputStream, UserRepresentation.class);

                final boolean isRealmPresent = keycloak.realms().findAll().stream()
                        .anyMatch(realmRepresentation -> realmRepresentation.getId().equals(realm));
                if (isRealmPresent) {
                    keycloak.realm(realm).users().search(representation.getUsername())
                            .forEach(user -> keycloak.realm(realm).users().delete(user.getId()));
                }
            }
        },
        ROLES {

            @Override
            void create(final Plugin plugin, final Keycloak keycloak, final String realm, final InputStream inputStream,
                    final MavenPropertiesSubstitutor envSubstitutor) throws MojoExecutionException {

                String fileContent = loadJSON(inputStream);

                final RoleRepresentation representation = loadJSON(envSubstitutor.replace(fileContent), RoleRepresentation.class);

                keycloak.realm(realm).roles().list().stream().filter(role -> role.getName().equals(representation.getName())).findAny()
                        .ifPresentOrElse((foundRole) -> {

                            plugin.getLog().info("CREATE PRESENT ROLE: " + foundRole.getName());

                            if (representation.isComposite()) {

                                plugin.getLog().info("ENHANCE COMPOSITE ROLE: " + foundRole.getName());

                                keycloak.realm(realm).roles().list().stream().filter(role -> role.getName().equals(representation.getName()))
                                        .findFirst().ifPresent(compositeRole -> {

                                            plugin.getLog().info(
                                                    "ENHANCE COMPOSITE ROLE WITH: " + String.join(",", representation.getComposites().getRealm()));

                                            if (compositeRole.getComposites() == null) {
                                                compositeRole.getComposites().setRealm(new HashSet<>());
                                            }
                                            compositeRole.getComposites().getRealm().addAll(representation.getComposites().getRealm());
                                        });
                            }

                        }, () -> keycloak.realm(realm).roles().create(representation));
            }

            @Override
            void delete(final Keycloak keycloak, final String realm, final InputStream inputStream) throws MojoExecutionException {

                final RoleRepresentation representation = loadJSON(inputStream, RoleRepresentation.class);

                final boolean isRealmPresent = keycloak.realms().findAll().stream()
                        .anyMatch(realmRepresentation -> realmRepresentation.getId().equals(realm));
                final boolean isPresent = isRealmPresent
                        && keycloak.realm(realm).roles().list().stream().anyMatch(role -> role.getName().equals(representation.getName()));
                if (isPresent) {
                    keycloak.realm(realm).roles().deleteRole(representation.getName());
                }
            }
        };

        /**
         * Creates the Keycloak resource with the Keycloak client.
         *
         * @param plugin
         *            the Maven plugin
         * @param keycloak
         *            the Keycloak client
         * @param realm
         *            the name of the realm for which the Keycloak resource has to be created
         * @param inputStream
         *            the JSON definition file of the Keycloak resource
         * @param envSubstitutor
         *            the MavenPropertiesSubstitutor to substitute values in files
         */
        abstract void create(final Plugin plugin, final Keycloak keycloak, final String realm, final InputStream inputStream,
                final MavenPropertiesSubstitutor envSubstitutor) throws MojoExecutionException;

        /**
         * Deletes the Keycloak resource with the Keycloak client.
         *
         * @param keycloak
         *            the Keycloak client
         * @param realm
         *            the name of the realm for which the Keycloak resource has to be deleted
         * @param inputStream
         *            the JSON definition file of the Keycloak resource
         */
        abstract void delete(final Keycloak keycloak, final String realm, final InputStream inputStream) throws MojoExecutionException;

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

            return stream(KeycloakResource.values()).filter(keycloakResource -> keycloakResource.toString().equalsIgnoreCase(resource)).findAny();
        }

        private static String loadJSON(final InputStream is) throws MojoExecutionException {

            try {
                return new String(is.readAllBytes(), UTF_8);
            } catch (final IOException e) {
                throw new MojoExecutionException(JSON_PARSE_ERROR_MESSAGE);
            }
        }

        private static <T> T loadJSON(final InputStream is, final Class<T> type) throws MojoExecutionException {

            try {
                return readValue(is, type);
            } catch (final IOException e) {
                throw new MojoExecutionException(JSON_PARSE_ERROR_MESSAGE);
            }
        }

        private static <T> T loadJSON(final String is, final Class<T> type) throws MojoExecutionException {

            try {
                return readValue(is, type);
            } catch (final IOException e) {
                throw new MojoExecutionException(JSON_PARSE_ERROR_MESSAGE);
            }
        }
    }
}
