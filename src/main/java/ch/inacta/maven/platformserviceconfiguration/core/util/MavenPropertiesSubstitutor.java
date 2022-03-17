package ch.inacta.maven.platformserviceconfiguration.core.util;

import java.util.Properties;

import org.apache.commons.text.StringSubstitutor;

/**
 * Substitutes text placeholder with correpsonding maven build properties.
 *
 * @author Inacta AG
 * @since 1.0.0
 */
public class MavenPropertiesSubstitutor extends StringSubstitutor {

    public MavenPropertiesSubstitutor(final Properties properties, String realm) {

        super(key -> "tenant".equals(key) ? realm : properties.getProperty(key));
        
        this.setEnableUndefinedVariableException(false);
        this.setEnableSubstitutionInVariables(false);
    }
}