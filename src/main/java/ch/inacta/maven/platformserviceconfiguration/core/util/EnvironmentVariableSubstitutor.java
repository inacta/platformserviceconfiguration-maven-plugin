package ch.inacta.maven.platformserviceconfiguration.core.util;

import org.apache.commons.text.StringSubstitutor;

/**
 * Substitutes text placeholder with correpsonding env variables.
 *
 * @author Inacta AG
 * @since 1.0.0
 */
public class EnvironmentVariableSubstitutor extends StringSubstitutor {

    public EnvironmentVariableSubstitutor() {

        super(System::getenv);
        
        this.setEnableUndefinedVariableException(false);
        this.setEnableSubstitutionInVariables(false);
    }
}