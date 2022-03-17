package ch.inacta.maven.platformserviceconfiguration.core.util;

import org.apache.commons.text.StringSubstitutor;

/**
 * Substitutes text placeholder with correpsonding maven build properties.
 *
 * @author Inacta AG
 * @since 1.0.0
 */
public class MavenPropertiesSubstitutor extends StringSubstitutor {

    public MavenPropertiesSubstitutor() {

        super(System::getenv);
        
        this.setEnableUndefinedVariableException(false);
        this.setEnableSubstitutionInVariables(false);
    }
}