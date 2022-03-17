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

        super(key -> {
            
            System.out.println(key);
            System.out.println(realm);
            System.out.println("tenant".equalsIgnoreCase(key.trim()));
            System.out.println("VALUE-EVALUATED: " + ("tenant".equalsIgnoreCase(key.trim()) ? realm : properties.getProperty(key)));

            return "tenant".equalsIgnoreCase(key.trim()) ? realm : properties.getProperty(key);
        });
        
        this.setEnableUndefinedVariableException(false);
        this.setEnableSubstitutionInVariables(true);
    }
}
