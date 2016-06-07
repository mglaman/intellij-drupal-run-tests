/*
 * Copyright (c) 2016.
 */

package com.mglaman.drupal_run_tests.run.configuration;

/**
 * Exception to be thrown when handling Drupal versions.
 */
class DrupalVersionException extends Exception {
    DrupalVersionException(String message) {
        super(message);
    }
}
