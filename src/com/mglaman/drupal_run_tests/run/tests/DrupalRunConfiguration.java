package com.mglaman.drupal_run_tests.run.tests;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Property;
import com.jetbrains.php.config.PhpProjectConfigurationFacade;
import com.jetbrains.php.config.commandLine.PhpCommandSettings;
import com.jetbrains.php.config.commandLine.PhpCommandSettingsBuilder;
import com.jetbrains.php.config.interpreters.PhpInterpreter;
import com.jetbrains.php.drupal.DrupalVersion;
import com.jetbrains.php.drupal.settings.DrupalConfigurable;
import com.jetbrains.php.drupal.settings.DrupalDataService;
import com.jetbrains.php.run.PhpCommandLineSettings;
import com.jetbrains.php.run.PhpRefactoringListenerRunConfiguration;
import com.jetbrains.php.run.PhpRunConfigurationSettings;
import com.jetbrains.php.run.PhpRunUtil;
import com.jetbrains.php.run.filters.PhpErrorMessageFilter;
import com.jetbrains.php.run.filters.PhpUnitFilter;
import com.jetbrains.php.run.filters.XdebugCallStackFilter;
import com.jetbrains.php.ui.PhpUiUtil;
import com.jetbrains.php.util.PhpConfigurationUtil;
import com.jetbrains.php.util.pathmapper.PhpPathMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

/**
 * @author mglaman
 */
class DrupalRunConfiguration extends PhpRefactoringListenerRunConfiguration<DrupalRunConfiguration.Settings> implements PhpRunConfigurationSettings {
    private String workingDir = null;

    DrupalRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory) {
        super(project, factory, "");

        DrupalDataService drupalDataService = DrupalDataService.getInstance(getProject());
        if (drupalDataService.getDrupalPath() != null) {
            setWorkingDirectory(drupalDataService.getDrupalPath());
        }
    }

    @NotNull
    protected DrupalRunConfiguration.Settings createSettings() {
        return new DrupalRunConfiguration.Settings();
    }

    @Override
    protected void fixSettingsAfterDeserialization(@NotNull DrupalRunConfiguration.Settings settings) {
        settings.setTestGroupExtra(PhpConfigurationUtil.deserializePath(settings.getTestGroupExtra()));
        settings.setSqliteDb(PhpConfigurationUtil.deserializePath(settings.getSqliteDb()));

        if (settings.getTestGroup() == DrupalRunTestsExecutionUtil.TEST_CLASS) {
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

    @Nullable
    @Override
    public String getWorkingDirectory() {
        return this.workingDir;
    }

    @Override
    public void setWorkingDirectory(@NotNull String s) {
        this.workingDir = s;
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        DrupalDataService drupalDataService = DrupalDataService.getInstance(getProject());

        if (drupalDataService.getVersion() == DrupalVersion.SIX) {
            throw new RuntimeConfigurationException("Drupal 6 is not supported, sorry.");
        }

        try {
            String scriptPath = DrupalRunTestsExecutionUtil.getRunTestsPath(getProject());
            if (!PhpRunUtil.isValidFilePath(scriptPath)) {
                throw new RuntimeConfigurationError("Unable to locate test script. Is Drupal configured properly?", createDrupalFix(getProject()));
            }
            if (!DrupalRunTestsExecutionUtil.isVendorInstalled(getProject())) {
                throw new RuntimeConfigurationWarning("Cannot detect `vendor` directory inside of Drupal root.");
            }
        } catch (RuntimeConfigurationError dve) {
            throw new RuntimeConfigurationError("Unable to locate test script. Is Drupal configured properly?", createDrupalFix(getProject()));
        } catch (DrupalVersionException e) {
            throw new RuntimeConfigurationError("Unsupported Drupal version found.");
        }

        DrupalRunConfiguration.Settings settings = getSettings();
        PhpRunUtil.checkCommandLineSettings(getProject(), settings.getCommandLineSettings());
    }

    @NotNull
    private static Runnable createDrupalFix(@NotNull final Project project) {
        return new Runnable() {
            public void run() {
                PhpUiUtil.editConfigurable(project, new DrupalConfigurable(project));
            }
        };
    }

    public RunProfileState getState(@NotNull Executor executor, @NotNull final ExecutionEnvironment env) throws ExecutionException {
        try {
            this.checkConfiguration();
        } catch (RuntimeConfigurationWarning var7) {
        } catch (RuntimeConfigurationException var8) {
            throw new ExecutionException(var8.getMessage());
        }

        final Project project = this.getProject();
        PhpInterpreter interpreter = PhpProjectConfigurationFacade.getInstance(project).getInterpreter();
        if (interpreter == null) {
            throw new ExecutionException(PhpCommandSettingsBuilder.INTERPRETER_NOT_FOUND_ERROR);
        } else {
            final PhpCommandSettings command = PhpCommandSettingsBuilder.create(project, interpreter, false);
            return new CommandLineState(env) {
                @NotNull
                protected ProcessHandler startProcess() throws ExecutionException {
                    // Build the command.
                    DrupalRunConfiguration.this.buildCommand(Collections.<String, String>emptyMap(), command);
                    ProcessHandler processHandler = DrupalRunConfiguration.this.createProcessHandler(project, null, command);
                    PhpRunUtil.attachProcessOutputDebugDumper(processHandler);
                    ProcessTerminatedListener.attach(processHandler, project);
                    return processHandler;
                }

                @NotNull
                @Override
                public ExecutionResult execute(@NotNull Executor executor, @NotNull ProgramRunner runner) throws ExecutionException {
                    final ProcessHandler processHandler = startProcess();
                    final ConsoleView console = createConsole(executor);
                    if (console != null) {
                        console.attachToProcess(processHandler);
                        Filter[] filters = DrupalRunConfiguration.this.getConsoleMessageFilters(project, command.getPathProcessor().createPathMapper(project));
                        for (Filter filter : filters) {
                            console.addMessageFilter(filter);
                        }
                    }

                    return new DefaultExecutionResult(console, processHandler, createActions(console, processHandler, executor));
                }
            };
        }
    }


    public void buildCommand(@NotNull Map<String, String> env, @NotNull PhpCommandSettings command) throws ExecutionException {
        Project project = getProject();
        DrupalRunConfiguration.Settings settings = this.getSettings();
        DrupalDataService drupalDataService = DrupalDataService.getInstance(project);
        String drupalRoot = drupalDataService.getDrupalPath();

        // Discover the proper path to the run-tests.sh script.
        try {
            command.setScript(DrupalRunTestsExecutionUtil.getRunTestsPath(project), true);
        } catch (DrupalVersionException dve) {
            throw new ExecutionException(dve);
        }

        PhpInterpreter e = PhpProjectConfigurationFacade.getInstance(project).getInterpreter();
        if (e == null || e.getPathToPhpExecutable() == null) {
            throw new ExecutionException("Unable to find PHP executable");
        }
        command.addArgument("--php");
        command.addArgument(e.getPathToPhpExecutable());

        command.addArgument("--url");
        command.addArgument(settings.getSimpletestUrl());

        if (settings.getSimpletestDb() != null) {
            command.addArgument("--dburl");
            command.addArgument(settings.getSimpletestDb());
        }

        if (settings.isUsingSqlite()) {
            command.addArgument("--sqlite");
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
        if (settings.hasDieOnFail()) {
            command.addArgument("--die-on-fail");
        }
        if (settings.hasRepeat()) {
            command.addArgument("--repeat");
            command.addArgument(Integer.toString(settings.getRepeatCount()));
        }

        String testTypes = settings.getTestTypes();
        if (null != testTypes) {
            command.addArgument("--types");
            command.addArgument(testTypes);
        }

        // @todo This saves each test result individually. Can we parse this.
//        command.addArgument("--xml ");
//        command.addArgument("/tmp/drupal-tests");

        DrupalRunTestsExecutionUtil.setTestGroup(command, settings.getTestGroup(), settings.getTestGroupExtra());

        command.importCommandLineSettings(settings.getCommandLineSettings(), drupalRoot);
        command.addEnvs(env);

    }

    private Filter[] getConsoleMessageFilters(@NotNull Project project, @NotNull PhpPathMapper pathMapper) {
        return new Filter[]{
                new PhpErrorMessageFilter(project, pathMapper),
                new PhpUnitFilter(project, pathMapper),
                new XdebugCallStackFilter(project, pathMapper),
                new DrupalRunTestMessageFilter(project, pathMapper)
        };
    }

    public static class Settings implements PhpRunConfigurationSettings {
        private String mySimpletestUrl = "http://localhost:8080";
        private String mySimpletestDb = null;
        private boolean myUseSqlite = false;
        private String mySqliteDb = "/tmp/tmp.sqlite";
        private boolean myVerboseOutput = false;
        private boolean myColorOutput = true;
        private int myTestGroup = DrupalRunTestsExecutionUtil.TEST_ALL;
        private String myTestGroupExtra = null;
        private int myTestConcurrency = 1;
        private boolean myDieOnFail = false;
        private boolean myUseRepeat = false;
        private int myRepeatCount = 1;
        private String myTestTypes = null;
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
        public boolean isUsingSqlite() {
            return this.myUseSqlite;
        }

        public void setUseSqlite(boolean use) {
            this.myUseSqlite = use;
        }

        @Attribute("sqlite_db")
        public String getSqliteDb() {
            return this.mySqliteDb;
        }

        public void setSqliteDb(String db) {
            this.mySqliteDb = db;
        }

        @Attribute("verbose")
        public boolean hasVerboseOutput() {
            return this.myVerboseOutput;
        }

        public void setVerboseOutput(boolean verbose) {
            this.myVerboseOutput = verbose;
        }

        @Attribute("color")
        public boolean hasColorOutput() {
            return this.myColorOutput;
        }

        public void setColorOutput(boolean color) {
            this.myColorOutput = color;
        }

        @Attribute("test_group")
        public int getTestGroup() {
            return this.myTestGroup;
        }

        public void setTestGroup(int group) {
            this.myTestGroup = group;
        }

        @Attribute("test_group_extra")
        public String getTestGroupExtra() {
            return this.myTestGroupExtra;
        }

        public void setTestGroupExtra(String groupExtra) {
            this.myTestGroupExtra = groupExtra;
        }

        @Attribute("concurrency")
        public int getTestConcurrency() {
            return this.myTestConcurrency;
        }

        public void setTestConcurrency(int concurrency) {
            this.myTestConcurrency = concurrency;
        }

        @Attribute("die_on_fail")
        public boolean hasDieOnFail() {
            return this.myDieOnFail;
        }

        public void setDieOnFail(boolean dieOnFail) {
            this.myDieOnFail = dieOnFail;
        }

        @Attribute("repeat")
        public boolean hasRepeat() {
            return this.myUseRepeat;
        }

        public void setHasRepeat(boolean repeat) {
            this.myUseRepeat = repeat;
        }

        @Attribute("repeat_count")
        public int getRepeatCount() {
            return this.myRepeatCount;
        }

        public void setRepeatCount(int count) {
            this.myRepeatCount = count;
        }

        @Attribute("test_types")
        public String getTestTypes() {
            return this.myTestTypes;
        }

        public String[] getTestTypesAsArray() {
            return (null != this.myTestTypes) ? this.myTestTypes.split(",") : null;
        }

        public void setTestTypes(@Nullable String types) {
            this.myTestTypes = types;
        }

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
