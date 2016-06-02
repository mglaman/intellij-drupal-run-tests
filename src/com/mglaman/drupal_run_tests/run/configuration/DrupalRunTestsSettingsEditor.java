package com.mglaman.drupal_run_tests.run.configuration;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author mglaman
 */
public class DrupalRunTestsSettingsEditor extends SettingsEditor<DrupalRunConfiguration> implements ChangeListener {
    private JPanel myMainPanel;
    private JTextField mySimpletestUrl;
    private JTextField mySimpletestDb;
    private JCheckBox myVerboseOutput;
    private JCheckBox myColorOutput;
    private JPanel myEnvironmentPanel;
    private JPanel myOptionsPanel;
    private JPanel myTestTargetPanel;
    private JRadioButton allRadioButton;
    private JRadioButton groupRadioButton;
    private JRadioButton moduleRadioButton;
    private JRadioButton directoryRadioButton;
    private JTextField myTestGroup;
    private JTextField myTestModule;
    private TextFieldWithBrowseButton myTestDirectory;
    private JSpinner myTestConcurrency;
    private JRadioButton classRadioButton;
    private JTextField myTestClass;
    private JCheckBox myUseSqlite;
    private JTextField mySQLiteDb;

    private final Project myProject;

    public DrupalRunTestsSettingsEditor(@NotNull Project project) {
        myProject = project;

        ActionListener updateStateActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DrupalRunTestsSettingsEditor.this.updateState();
            }
        };
        allRadioButton.addActionListener(updateStateActionListener);
        groupRadioButton.addActionListener(updateStateActionListener);
        moduleRadioButton.addActionListener(updateStateActionListener);
        directoryRadioButton.addActionListener(updateStateActionListener);
        myUseSqlite.addActionListener(updateStateActionListener);
    }

    protected void resetEditorFrom(DrupalRunConfiguration configuration) {
        DrupalRunConfiguration.Settings params = configuration.getSettings();
        mySimpletestUrl.setText(params.getSimpletestUrl());
        mySimpletestDb.setText(params.getSimpletestDb());
        myVerboseOutput.setSelected(params.hasVerboseOutput());
        myColorOutput.setSelected(params.hasColorOutput());

        myUseSqlite.setSelected(params.isUsingSqlite());
        mySQLiteDb.setText(params.getSqliteDb());
        mySQLiteDb.setVisible(params.isUsingSqlite());

        switch (params.getTestGroup()) {
            case DrupalRunConfiguration.TEST_GROUP:
                groupRadioButton.setSelected(true);
                myTestGroup.setText(params.getTestGroupExtra());
                myTestGroup.setVisible(true);
                break;
            case DrupalRunConfiguration.TEST_MODULE:
                moduleRadioButton.setSelected(true);
                myTestModule.setText(params.getTestGroupExtra());
                myTestModule.setVisible(true);
                break;
            case DrupalRunConfiguration.TEST_DIRECTORY:
                directoryRadioButton.setSelected(true);
                myTestDirectory.setText(params.getTestGroupExtra());
                myTestDirectory.setVisible(true);
                break;
            case DrupalRunConfiguration.TEST_CLASS:
                classRadioButton.setSelected(true);
                myTestClass.setText(params.getTestGroupExtra());
                myTestClass.setVisible(true);
                break;
            case DrupalRunConfiguration.TEST_ALL:
            default:
                allRadioButton.setSelected(true);
                break;
        }
    }

    protected void applyEditorTo(DrupalRunConfiguration configuration) throws ConfigurationException {
        DrupalRunConfiguration.Settings params = configuration.getSettings();
        params.setSimpletestUrl(mySimpletestUrl.getText());
        params.setSimpletestDb(mySimpletestDb.getText());
        params.setVerboseOutput(myVerboseOutput.isSelected());
        params.setColorOutput(myColorOutput.isSelected());
        params.setUseSqlite(myUseSqlite.isSelected());
        params.setSqliteDb(mySQLiteDb.getText());

        if (allRadioButton.isSelected()) {
            params.setTestGroup(DrupalRunConfiguration.TEST_ALL);
        } else if(groupRadioButton.isSelected()) {
            params.setTestGroup(DrupalRunConfiguration.TEST_GROUP);
            params.setTestGroupExtra(myTestGroup.getText());
        } else if(moduleRadioButton.isSelected()) {
            params.setTestGroup(DrupalRunConfiguration.TEST_MODULE);
            params.setTestGroupExtra(myTestModule.getText());
        } else if(directoryRadioButton.isSelected()) {
            params.setTestGroup(DrupalRunConfiguration.TEST_DIRECTORY);
            params.setTestGroupExtra(myTestDirectory.getText());
        } else if(classRadioButton.isSelected()) {
            params.setTestGroup(DrupalRunConfiguration.TEST_CLASS);
            params.setTestGroupExtra(myTestClass.getText());
        }
    }

    public void stateChanged(ChangeEvent e) {
        this.fireEditorStateChanged();
    }

    @NotNull
    protected JComponent createEditor() {
        JPanel var10000 = this.myMainPanel;
        if(this.myMainPanel == null) {
            throw new IllegalStateException(String.format("@NotNull method %s.%s must not return null", new Object[]{"com/jetbrains/php/run/builtInWebServer/PhpBuiltInWebServerRunConfigurationEditor", "createEditor"}));
        } else {
            return var10000;
        }
    }

    private void updateState() {
        myTestGroup.setVisible(groupRadioButton.isSelected());
        myTestModule.setVisible(moduleRadioButton.isSelected());
        myTestDirectory.setVisible(directoryRadioButton.isSelected());
        myTestClass.setVisible(classRadioButton.isSelected());
        mySQLiteDb.setVisible(myUseSqlite.isSelected());
    }

    private void createUIComponents() {
        assert this.myProject != null;
        myTestDirectory = new TextFieldWithBrowseButton();
        myTestDirectory.addBrowseFolderListener(null, null, myProject, FileChooserDescriptorFactory.createSingleFolderDescriptor());
    }
}
