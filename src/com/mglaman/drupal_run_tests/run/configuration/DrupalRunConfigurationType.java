package com.mglaman.drupal_run_tests.run.configuration;

import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationFactory;
import javax.swing.Icon;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.jetbrains.php.run.PhpRunConfigurationFactoryBase;
import org.jetbrains.annotations.NotNull;

/**
 * @author mglaman
 */
public class DrupalRunConfigurationType implements ConfigurationType {
    private static final String DISPLAY_NAME = "Drupal Run Tests";
    private static final String DESCRIPTION = "Runs Drupal Simpletest and PHPUnit.";

    private final ConfigurationFactory myFactory = new PhpRunConfigurationFactoryBase(this) {
        @Override
        @NotNull
        public RunConfiguration createTemplateConfiguration(@NotNull  Project project) {
            return new DrupalRunConfiguration(project, this, "");
        }
    };

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getConfigurationTypeDescription() {
        return DESCRIPTION;
    }

    @Override
    public Icon getIcon() {
        return IconLoader.getIcon("/druplicon-small.png");
    }

    @NotNull
    @Override
    public String getId() {
        return "DrupalRunTestsConfigurationType";
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[]{this.myFactory};
    }
}
