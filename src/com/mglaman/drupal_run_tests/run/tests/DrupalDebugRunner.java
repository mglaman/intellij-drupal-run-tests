/*
 * Copyright (c) 2016.
 */

package com.mglaman.drupal_run_tests.run.tests;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.jetbrains.php.config.PhpProjectConfigurationFacade;
import com.jetbrains.php.config.commandLine.PhpCommandSettings;
import com.jetbrains.php.config.commandLine.PhpCommandSettingsBuilder;
import com.jetbrains.php.config.interpreters.PhpInterpreter;
import com.jetbrains.php.debug.PhpDebugExtension;
import com.jetbrains.php.debug.PhpProjectDebugConfiguration;
import com.jetbrains.php.debug.common.PhpDebugDriver;
import com.jetbrains.php.debug.common.PhpDebugProcessFactory;
import com.jetbrains.php.debug.connection.PhpDebugConnectionManager;
import com.jetbrains.php.debug.connection.PhpDebugServer;
import com.jetbrains.php.run.PhpDebugRunner;
import com.jetbrains.php.run.script.PhpScriptDebugRunner;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author mglaman
 */
public class DrupalDebugRunner extends PhpDebugRunner<DrupalRunConfiguration> {
    public DrupalDebugRunner() { super(DrupalRunConfiguration.class); }

    @NotNull
    public String getRunnerId() {
        return "DrupalDebugRunner";
    }

    @Override
    protected RunContentDescriptor doExecute(@NotNull DrupalRunConfiguration runConfiguration, @NotNull RunProfileState runProfileState, @NotNull ExecutionEnvironment env) throws ExecutionException {
        final Project project = runConfiguration.getProject();
        PhpProjectDebugConfiguration.State debugConfiguration = PhpProjectDebugConfiguration.getInstance(project).getState();
        boolean breakAtFirstLine = (debugConfiguration != null) && debugConfiguration.isBreakAtFirstLine();

        final PhpInterpreter interpreter = PhpProjectConfigurationFacade.getInstance(project).getInterpreter();
        if(interpreter == null) {
            throw new ExecutionException(PhpCommandSettingsBuilder.INTERPRETER_NOT_FOUND_ERROR);
        }

        final PhpDebugExtension debugExtension = PhpProjectConfigurationFacade.getInstance(runConfiguration.getProject()).getInterpreterDebugExtension();
        if(debugExtension == null) {
            throw new ExecutionException("Unknown debugger.");
        } else {
            final PhpDebugServer debugServer = debugExtension.startDebugServer(project);
            final PhpDebugConnectionManager connectionsManager = debugExtension.createDebugConnectionManager();
            final String sessionId = debugServer.registerSessionHandler(false, connectionsManager);

            try {
                final PhpCommandSettings commandSettings = PhpCommandSettingsBuilder.create(project, interpreter, true);
                Map<String, String> commandLineEnv = debugExtension.getDebugEnv(project, breakAtFirstLine, sessionId);
                runConfiguration.buildCommand(commandLineEnv, commandSettings);
                final ProcessHandler processHandler = runConfiguration.createProcessHandler(project, commandSettings);
                ProcessTerminatedListener.attach(processHandler, project);
                XDebugSession debugSession = XDebuggerManager.getInstance(project).startSession(env, new XDebugProcessStarter() {
                    @NotNull
                    public XDebugProcess start(@NotNull XDebugSession session) {
                        PhpScriptDebugRunner.onSessionStart(session, debugServer, sessionId, connectionsManager, project, commandSettings, interpreter, processHandler);
                        PhpDebugDriver driver = debugExtension.getDebugDriver();
                        return PhpDebugProcessFactory.forPhpScript(project, session, sessionId, connectionsManager, driver, commandSettings.getPathProcessor());
                    }
                });
                debugSession.getConsoleView().attachToProcess(processHandler);
                processHandler.startNotify();
                return debugSession.getRunContentDescriptor();
            } catch (ExecutionException var16) {
                debugServer.unregisterSessionHandler(sessionId);
                throw var16;
            }
        }

    }

}
