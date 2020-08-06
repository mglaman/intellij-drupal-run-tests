package com.mglaman.drupal_run_tests;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

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
    private JCheckBox dieOnFailureCheckBox;
    private JCheckBox repeatCheckBox;
    private JSpinner myRepeatCount;
    private JCheckBox simpletestCheckBox;
    private JCheckBox functionalBrowserTestsCheckBox;
    private JCheckBox javascriptBrowserTestsCheckBox;
    private JCheckBox kernelTestsCheckBox;
    private JPanel myTestTypePanel;
    private JCheckBox unitTestsCheckBox;

    private final Project myProject;

    DrupalRunTestsSettingsEditor(@NotNull Project project) {
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
        repeatCheckBox.addActionListener(updateStateActionListener);
    }

    protected void resetEditorFrom(@NotNull DrupalRunConfiguration configuration) {
        DrupalRunConfiguration.Settings params = configuration.getSettings();
        mySimpletestUrl.setText(params.getSimpletestUrl());
        mySimpletestDb.setText(params.getSimpletestDb());
        myVerboseOutput.setSelected(params.hasVerboseOutput());
        myColorOutput.setSelected(params.hasColorOutput());
        dieOnFailureCheckBox.setSelected(params.hasDieOnFail());

        myUseSqlite.setSelected(params.isUsingSqlite());
        mySQLiteDb.setText(params.getSqliteDb());
        mySQLiteDb.setVisible(params.isUsingSqlite());

        repeatCheckBox.setSelected(params.hasRepeat());
        myRepeatCount.setValue(params.getRepeatCount());
        myRepeatCount.setVisible(params.hasRepeat());

        myTestConcurrency.setValue(params.getTestConcurrency());

        switch (params.getTestGroup()) {
            case DrupalRunTestsExecutionUtil.TEST_GROUP:
                groupRadioButton.setSelected(true);
                myTestGroup.setText(params.getTestGroupExtra());
                myTestGroup.setVisible(true);
                break;
            case DrupalRunTestsExecutionUtil.TEST_MODULE:
                moduleRadioButton.setSelected(true);
                myTestModule.setText(params.getTestGroupExtra());
                myTestModule.setVisible(true);
                break;
            case DrupalRunTestsExecutionUtil.TEST_DIRECTORY:
                directoryRadioButton.setSelected(true);
                myTestDirectory.setText(params.getTestGroupExtra());
                myTestDirectory.setVisible(true);
                break;
            case DrupalRunTestsExecutionUtil.TEST_CLASS:
                classRadioButton.setSelected(true);
                myTestClass.setText(params.getTestGroupExtra());
                myTestClass.setVisible(true);
                break;
            case DrupalRunTestsExecutionUtil.TEST_ALL:
            default:
                allRadioButton.setSelected(true);
                break;
        }

        String[] testTypes = params.getTestTypesAsArray();
        if (testTypes == null) {
            simpletestCheckBox.setSelected(true);
            unitTestsCheckBox.setSelected(true);
            kernelTestsCheckBox.setSelected(true);
            functionalBrowserTestsCheckBox.setSelected(true);
            javascriptBrowserTestsCheckBox.setSelected(true);
        } else {
            for (String type : testTypes) {
                if (type.equals("Simpletest")) {
                    simpletestCheckBox.setSelected(true);
                } else if (type.equals("PHPUnit-Unit")) {
                    unitTestsCheckBox.setSelected(true);

                } else if (type.equals("PHPUnit-Kernel")) {
                    kernelTestsCheckBox.setSelected(true);
                } else if (type.equals("PHPUnit-Functional")) {
                    functionalBrowserTestsCheckBox.setSelected(true);
                } else if (type.equals("PHPUnit-FunctionalJavascript")) {
                    javascriptBrowserTestsCheckBox.setSelected(true);
                }
            }
        }

    }

    protected void applyEditorTo(@NotNull DrupalRunConfiguration configuration) throws ConfigurationException {
        DrupalRunConfiguration.Settings params = configuration.getSettings();
        params.setSimpletestUrl(mySimpletestUrl.getText());
        params.setSimpletestDb(mySimpletestDb.getText());
        params.setVerboseOutput(myVerboseOutput.isSelected());
        params.setColorOutput(myColorOutput.isSelected());
        params.setUseSqlite(myUseSqlite.isSelected());
        params.setSqliteDb(mySQLiteDb.getText());
        params.setTestConcurrency(((SpinnerNumberModel) myTestConcurrency.getModel()).getNumber().intValue());
        params.setDieOnFail(dieOnFailureCheckBox.isSelected());
        params.setHasRepeat(repeatCheckBox.isSelected());
        params.setRepeatCount(((SpinnerNumberModel) myRepeatCount.getModel()).getNumber().intValue());

        if (allRadioButton.isSelected()) {
            params.setTestGroup(DrupalRunTestsExecutionUtil.TEST_ALL);
        } else if(groupRadioButton.isSelected()) {
            params.setTestGroup(DrupalRunTestsExecutionUtil.TEST_GROUP);
            params.setTestGroupExtra(myTestGroup.getText());
        } else if(moduleRadioButton.isSelected()) {
            params.setTestGroup(DrupalRunTestsExecutionUtil.TEST_MODULE);
            params.setTestGroupExtra(myTestModule.getText());
        } else if(directoryRadioButton.isSelected()) {
            params.setTestGroup(DrupalRunTestsExecutionUtil.TEST_DIRECTORY);
            params.setTestGroupExtra(myTestDirectory.getText());
        } else if(classRadioButton.isSelected()) {
            params.setTestGroup(DrupalRunTestsExecutionUtil.TEST_CLASS);
            params.setTestGroupExtra(myTestClass.getText());
        }

        if (simpletestCheckBox.isSelected() && unitTestsCheckBox.isSelected() && kernelTestsCheckBox.isSelected() && functionalBrowserTestsCheckBox.isSelected() && javascriptBrowserTestsCheckBox.isSelected()) {
            params.setTestTypes(null);
        } else {
            ArrayList<String> enabledTypesList = new ArrayList<String>();
            if (simpletestCheckBox.isSelected()) {
                enabledTypesList.add("Simpletest");
            }
            if (unitTestsCheckBox.isSelected()) {
                enabledTypesList.add("PHPUnit-Unit");
            }
            if (kernelTestsCheckBox.isSelected()) {
                enabledTypesList.add("PHPUnit-Kernel");
            }
            if (functionalBrowserTestsCheckBox.isSelected()) {
                enabledTypesList.add("PHPUnit-Functional");
            }
            if (javascriptBrowserTestsCheckBox.isSelected()) {
                enabledTypesList.add("PHPUnit-FunctionalJavascript");
            }

            params.setTestTypes(StringUtil.join(enabledTypesList, ","));
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
        myRepeatCount.setVisible(repeatCheckBox.isSelected());
    }

    private void createUIComponents() {
        assert this.myProject != null;
        myTestDirectory = new TextFieldWithBrowseButton();
        myTestDirectory.addBrowseFolderListener(null, null, myProject, FileChooserDescriptorFactory.createSingleFolderDescriptor());

        // @todo essentially copy com.intellij.ui.PortField for a concurrency editor.
        myTestConcurrency = new JSpinner(new SpinnerNumberModel(1, 1, 30, 1));
        JSpinner.NumberEditor concurrencyEditor = new JSpinner.NumberEditor(myTestConcurrency, "#");
        concurrencyEditor.getTextField().setColumns(4);
        myTestConcurrency.setEditor(concurrencyEditor);

        myRepeatCount = new JSpinner(new SpinnerNumberModel(1, 1, 30, 1));
        JSpinner.NumberEditor repeatEditor = new JSpinner.NumberEditor(myRepeatCount, "#");
        repeatEditor.getTextField().setColumns(4);
        myRepeatCount.setEditor(repeatEditor);
    }
}
