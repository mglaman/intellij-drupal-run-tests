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
import com.jetbrains.php.drupal.DrupalVersion;
import com.jetbrains.php.drupal.settings.DrupalDataService;
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
    private final static String D8_TESTS_PATH = "/core/scripts/run-tests.sh";
    private final static String D7_TESTS_PATH = "/scripts/run-tests.sh";

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
        settings.setTestGroupExtra(PhpConfigurationUtil.deserializePath(settings.getTestGroupExtra()));
        settings.setSqliteDb(PhpConfigurationUtil.deserializePath(settings.getSqliteDb()));

        if (settings.getTestGroup() == TEST_CLASS) {
            settings.setTestGroupExtra(settings.getTestGroupExtra().replace("/", "\\"));
        }
    }

    @Override
    protected void fixSettingsBeforeSerialization(@NotNull DrupalRunConfiguration.Settings settings) {
        settings.setTestGroupExtra(PhpConfigurationUtil.serializePath(settings.getTestGroupExtra()));
        settings.setSqliteDb(PhpConfigurationUtil.serializePath(settings.getSqliteDb()));
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new DrupalRunTestsSettingsEditor(this.getProject());
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        DrupalDataService drupalDataService = DrupalDataService.getInstance(getProject());
        String drupalRoot = drupalDataService.getDrupalPath();

        if (drupalDataService.getVersion() == DrupalVersion.SIX) {
            throw new RuntimeConfigurationException("Drupal 6 is not supported, sorry.");
        }

        if (PhpRunUtil.findDirectory(drupalRoot) == null) {
            // @todo Can we link to a shortcut to configure Drupal?s
            throw new RuntimeConfigurationException("Invalid Drupal directory configured for this project.");
        }

        PhpRunUtil.checkPhpInterpreter(getProject());

        DrupalRunConfiguration.Settings settings = getSettings();
        PhpRunUtil.checkCommandLineSettings(settings.getCommandLineSettings());
    }

    public void fillCommandSettings(@NotNull Map<String, String> env, @NotNull PhpCommandSettings command) throws ExecutionException {
        DrupalRunConfiguration.Settings settings = this.getSettings();

        DrupalDataService drupalDataService = DrupalDataService.getInstance(getProject());
        String drupalRoot = drupalDataService.getDrupalPath();

        // @todo this is messy, but works for now ;)
        if (drupalDataService.getVersion() == DrupalVersion.EIGHT) {
            command.setScript(drupalRoot + D8_TESTS_PATH, true);
        } else if (drupalDataService.getVersion() == DrupalVersion.SEVEN) {
            command.setScript(drupalRoot + D7_TESTS_PATH, true);
        }

        command.addArgument("--php ");
        PhpInterpreter e = PhpProjectConfigurationFacade.getInstance(getProject()).getInterpreter();
        command.addArgument(e.getPathToPhpExecutable());

        command.addArgument("--url ");
        command.addArgument(settings.getSimpletestUrl());

        if (settings.getSimpletestDb() != null) {
            command.addArgument("--dburl ");
            command.addArgument(settings.getSimpletestDb());
        }

        if (settings.isUsingSqlite()) {
            command.addArgument("--sqlite ");
            command.addArgument(settings.getSqliteDb());
        }


        command.addArgument("--concurrency");
        command.addArgument(Integer.toString(settings.getTestConcurrency()));

        if (settings.hasColorOutput()) {
            command.addArgument("--color");
        }
        if (settings.hasVerboseOutput()) {
            command.addArgument("--verbose");
        }

        String testGroup, testGroupExtra = null;

        switch (settings.getTestGroup()) {
            case TEST_GROUP:
                testGroup = settings.getTestGroupExtra();
                break;
            case TEST_MODULE:
                testGroup = "--module ";
                testGroupExtra = settings.getTestGroupExtra();
                break;
            case TEST_DIRECTORY:
                testGroup = "--directory ";
                testGroupExtra = settings.getTestGroupExtra();
                break;
            case TEST_CLASS:
                testGroup = "--class ";
                testGroupExtra = settings.getTestGroupExtra();
                break;
            case TEST_ALL:
            default:
                testGroup = "--all";
                break;
        }

        command.addArgument(testGroup);
        if (testGroupExtra != null) {
            command.addArgument(testGroupExtra);
        }

        command.importCommandLineSettings(settings.getCommandLineSettings(), drupalRoot);
        command.addEnvs(env);

    }

    @Override
    protected Filter[] getConsoleMessageFilters(@NotNull Project project, @NotNull PhpPathMapper pathMapper) {
        return PhpExecutionUtil.getConsoleMessageFilters(project, pathMapper);
    }

    public static class Settings implements PhpRunConfigurationSettings {
        private String mySimpletestUrl = "http://localhost:8080";
        private String mySimpletestDb = null;
        private boolean myUseSqlite = false;
        private String mySqliteDb = "/tmp/tmp.sqlite";
        private boolean myVerboseOutput = false;
        private boolean myColorOutput = false;
        private int myTestGroup = TEST_ALL;
        private String myTestGroupExtra = null;
        private int myTestConcurrency = 1;
        private PhpCommandLineSettings myCommandLineSettings = new PhpCommandLineSettings();

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

        @Attribute("use_sqlite")
        public boolean isUsingSqlite() { return this.myUseSqlite; }

        public void setUseSqlite(boolean use) { this.myUseSqlite = use; }

        @Attribute("sqlite_db")
        public String getSqliteDb() { return this.mySqliteDb; }

        public void setSqliteDb(String db) { this.mySqliteDb = db; }

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

        @Attribute("concurrency")
        public int getTestConcurrency() { return this.myTestConcurrency; }

        public void setTestConcurrency(int concurrency) { this.myTestConcurrency = concurrency; }

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
