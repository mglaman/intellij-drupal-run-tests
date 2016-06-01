package com.mglaman.drupal_run_tests.run.configuration;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.Filter;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Property;
import com.jetbrains.php.config.PhpProjectConfigurationFacade;
import com.jetbrains.php.config.commandLine.PhpCommandSettings;
import com.jetbrains.php.config.interpreters.PhpInterpreter;
import com.jetbrains.php.run.*;
import com.jetbrains.php.util.PhpConfigurationUtil;
import com.jetbrains.php.util.pathmapper.PhpPathMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author mglaman
 */
public class DrupalRunConfiguration extends PhpCommandLineRunConfiguration<DrupalRunConfiguration.Settings> {
    private final static String RUN_TESTS_PATH = "/core/scripts/run-tests.sh";

    public final static int TEST_ALL = 0;
    public final static int TEST_GROUP = 1;
    public final static int TEST_MODULE = 2;
    public final static int TEST_DIRECTORY = 3;
    public final static int TEST_CLASS = 4;

    protected DrupalRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    @NotNull
    protected DrupalRunConfiguration.Settings createSettings() {
        return new DrupalRunConfiguration.Settings();
    }

    @Override
    protected void fixSettingsAfterDeserialization(@NotNull DrupalRunConfiguration.Settings settings) {
        settings.setDrupalRoot(PhpConfigurationUtil.deserializePath(settings.getDrupalRoot()));
        settings.setTestGroupExtra(PhpConfigurationUtil.deserializePath(settings.getTestGroupExtra()));
    }

    @Override
    protected void fixSettingsBeforeSerialization(@NotNull DrupalRunConfiguration.Settings settings) {
        settings.setDrupalRoot(PhpConfigurationUtil.serializePath(settings.getDrupalRoot()));
        settings.setTestGroupExtra(PhpConfigurationUtil.serializePath(settings.getTestGroupExtra()));
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new DrupalRunTestsSettingsEditor(this.getProject());
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        DrupalRunConfiguration.Settings settings = getSettings();
        String drupalRoot = settings.getDrupalRoot();

        if (PhpRunUtil.findDirectory(drupalRoot) == null) {
            throw new RuntimeConfigurationException("Invalid directory");
        }
        if (PhpRunUtil.findFile(drupalRoot + RUN_TESTS_PATH) == null) {
            throw new RuntimeConfigurationException("Unable to locate Drupal test runner script");
        }
        PhpRunUtil.checkPhpInterpreter(getProject());
        PhpRunUtil.checkCommandLineSettings(settings.getCommandLineSettings());
    }

    public void fillCommandSettings(@NotNull Map<String, String> env, @NotNull PhpCommandSettings command) throws ExecutionException {
        DrupalRunConfiguration.Settings settings = this.getSettings();
        String drupalRoot = settings.getDrupalRoot();
        PhpInterpreter e = PhpProjectConfigurationFacade.getInstance(getProject()).getInterpreter();

        // @todo this is messy, but works for now ;)
        command.setScript(drupalRoot + RUN_TESTS_PATH, true);
        command.addArgument("--php ");
        command.addArgument(e.getPathToPhpExecutable());
        command.addArgument("--url ");
        command.addArgument(settings.getSimpletestUrl());
        command.addArgument("--dburl ");
        command.addArgument(settings.getSimpletestDb());
        // @todo there should be just a "Use SQLite" option
        command.addArgument("--sqlite ");
        command.addArgument("/tmp/tmp.sqlite");
        command.addArgument("--concurrency");
        command.addArgument("4");

        if (settings.hasColorOutput()) {
            command.addArgument("--color");
        }
        if (settings.hasVerboseOutput()) {
            command.addArgument("--verbose");
        }

        switch (settings.getTestGroup()) {
            case TEST_GROUP:
                command.addArgument(settings.getTestGroupExtra());
                break;
            case TEST_MODULE:
                command.addArgument("--module ");
                command.addArgument(settings.getTestGroupExtra());
                break;
            case TEST_DIRECTORY:
                command.addArgument("--directory ");
                command.addArgument(settings.getTestGroupExtra());
                break;
            case TEST_ALL:
            default:
                command.addArgument("--all");
                break;
        }

        command.importCommandLineSettings(settings.getCommandLineSettings(), drupalRoot);
        command.addEnvs(env);

    }

    @Override
    protected Filter[] getConsoleMessageFilters(@NotNull Project project, @NotNull PhpPathMapper pathMapper) {
        return PhpExecutionUtil.getConsoleMessageFilters(project, pathMapper);
    }

    public static class Settings implements PhpRunConfigurationSettings {
        private String myDrupalRoot = null;
        private String mySimpletestUrl = "http://localhost:8080";
        private String mySimpletestDb = "sqlite://localhost/sites/default/files/.ht.sqlite";
        private boolean myVerboseOutput = false;
        private boolean myColorOutput = false;
        private int myTestGroup = TEST_ALL;
        private String myTestGroupExtra = null;
        private PhpCommandLineSettings myCommandLineSettings = new PhpCommandLineSettings();

        @Attribute("drupal_root")
        @Nullable
        public String getDrupalRoot() {
            return this.myDrupalRoot;
        }

        public void setDrupalRoot(@Nullable String path) {
            this.myDrupalRoot = StringUtil.nullize(path);
        }

        @Attribute("simpletest_url")
        @NotNull
        public String getSimpletestUrl() {
            return this.mySimpletestUrl;
        }

        public void setSimpletestUrl(@Nullable String url) {
            this.mySimpletestUrl = StringUtil.nullize(url);
        }

        @Attribute("simpletest_db")
        @Nullable
        public String getSimpletestDb() {
            return this.mySimpletestDb;
        }

        public void setSimpletestDb(@Nullable String db) {
            this.mySimpletestDb = StringUtil.nullize(db);
        }

        @Attribute("verbose")
        public boolean hasVerboseOutput() { return this.myVerboseOutput; }

        public void setVerboseOutput(boolean verbose) {
            this.myVerboseOutput = verbose;
        }

        @Attribute("color")
        public boolean hasColorOutput() { return this.myColorOutput; }

        public void setColorOutput(boolean color) {
            this.myColorOutput = color;
        }

        @Attribute("test_group")
        public int getTestGroup() { return this.myTestGroup; }

        public void setTestGroup(int group) { this.myTestGroup = group; }

        @Attribute("test_group_extra")
        public String getTestGroupExtra() { return this.myTestGroupExtra; }

        public void setTestGroupExtra(String groupExtra) { this.myTestGroupExtra = groupExtra; }

        @Property(
                surroundWithTag = false
        )
        @NotNull
        public PhpCommandLineSettings getCommandLineSettings() {
            PhpCommandLineSettings var10000 = this.myCommandLineSettings;
            if (this.myCommandLineSettings == null) {
                throw new IllegalStateException(String.format("@NotNull method %s.%s must not return null", new Object[]{"com/jetbrains/php/run/script/PhpScriptRunConfiguration$Settings", "getCommandLineSettings"}));
            } else {
                return var10000;
            }
        }

        public void setCommandLineSettings(@NotNull PhpCommandLineSettings commandLineSettings) {
            this.myCommandLineSettings = commandLineSettings;
        }

        @Nullable
        public String getWorkingDirectory() {
            return this.myCommandLineSettings.getWorkingDirectory();
        }

        public void setWorkingDirectory(@NotNull String workingDirectory) {
            this.myCommandLineSettings.setWorkingDirectory(workingDirectory);
        }
    }
}
