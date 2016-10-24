/*
 * Copyright (c) 2016.
 */

package com.mglaman.drupal_run_tests.run.tests;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.run.filters.PhpFilter;
import com.jetbrains.php.util.pathmapper.PhpPathMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author mglaman
 */
class DrupalRunTestMessageFilter extends PhpFilter {
    private static final Logger LOG = Logger.getInstance("#com.mglaman.drupal_run_tests.run.tests.DrupalRunTestMessageFilter");
    private static final Pattern PASSES_MESSAGE_PATTERN;
    private final Project myProject;

    DrupalRunTestMessageFilter(@NotNull Project project, @NotNull PhpPathMapper pathMapper) {
        super(project, pathMapper);
        myProject = project;
    }

    private Project getProject() {
        return this.myProject;
    }

    @Nullable
    @Override
    public MyResult applyFilter(@NotNull String s) {
        try {
            Matcher e = PASSES_MESSAGE_PATTERN.matcher(StringUtil.newBombedCharSequence(s, 1000L));
            if (e.find()) {
                String className = e.group(1);
                if (className == null) {
                    return null;
                }
                className = className.trim();

                try {
                    Collection<PhpClass> classes = PhpIndex.getInstance(getProject()).getClassesByFQN(className);
                    if (classes.isEmpty()) {
                        return null;
                    }
                    String test = classes.iterator().next().getContainingFile().getVirtualFile().getPath();
                    return new MyResult(test, 1, e.start(1), e.end(1));
                } catch (NumberFormatException var6) {
                }
            }

            return null;
        } catch (ProcessCanceledException var7) {
            LOG.warn("Matching took too long for line: " + s);
            return null;
        }
    }

    static {
        // Test result with failures or exceptions.
        PASSES_MESSAGE_PATTERN = Pattern.compile("([\\w\\\\]*)\\s+(\\d+\\spasses)(.*)(\\d+\\s(fails?|exceptions?))(.*)");
    }
}
