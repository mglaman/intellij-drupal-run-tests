/*
 * Copyright (c) 2016.
 */

package com.mglaman.drupal_run_tests;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.php.config.commandLine.PhpCommandSettings;
import com.jetbrains.php.drupal.settings.DrupalDataService;
import com.jetbrains.php.run.PhpRunUtil;
import org.jetbrains.annotations.Nullable;

/**
 * Run tests execution utility class.
 */
final class DrupalRunTestsExecutionUtil {
    private final static String D8_TESTS_PATH = "/core/scripts/run-tests.sh";
    private final static String D7_TESTS_PATH = "/scripts/run-tests.sh";

    final static int TEST_ALL = 0;
    final static int TEST_GROUP = 1;
    final static int TEST_MODULE = 2;
    final static int TEST_DIRECTORY = 3;
    final static int TEST_CLASS = 4;

    static boolean isVendorInstalled(Project project) {
        DrupalDataService drupalDataService = DrupalDataService.getInstance(project);
        String drupalRoot = drupalDataService.getDrupalPath();

        return PhpRunUtil.isValidDirectoryPath(drupalRoot + "/vendor") ||
                PhpRunUtil.isValidFileOrDirectoryPath(project.getBasePath() + "/vendor");
    }

    static String getRunTestsPath(Project project) throws DrupalVersionException {
        DrupalDataService drupalDataService = DrupalDataService.getInstance(project);
        String drupalRoot = drupalDataService.getDrupalPath();

        switch (drupalDataService.getVersion()) {
            case EIGHT:
                return drupalRoot + DrupalRunTestsExecutionUtil.D8_TESTS_PATH;
            case SEVEN:
                return drupalRoot + DrupalRunTestsExecutionUtil.D7_TESTS_PATH;
            case SIX:
                throw new DrupalVersionException("Drupal 6 is not supported.");
            default:
                throw new DrupalVersionException("Invalid Drupal version.");
        }
    }

    static void setTestGroup(PhpCommandSettings command, int group, @Nullable String groupExra) {
        String testGroup;

        switch (group) {
            case TEST_GROUP:
                // Technically "group extra" has our group value. No need to pass an argument.
                testGroup = groupExra;
                groupExra = null;
                break;
            case TEST_MODULE:
                testGroup = "--module";
                break;
            case TEST_DIRECTORY:
                testGroup = "--directory";
                break;
            case TEST_CLASS:
                testGroup = "--class";
                break;
            case TEST_ALL:
            default:
                testGroup = "--all";
                break;
        }

        command.addArgument(StringUtil.notNullize(testGroup));
        if (groupExra != null) {
            command.addArgument(StringUtil.notNullize(groupExra));
        }
    }

}
