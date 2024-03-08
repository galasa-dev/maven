/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.maven.plugin.error;

// Logs errors, then raises an exception.
public interface ErrorRaiser <T extends Exception> {
    void raiseError(String template, Object...  parameters) throws T ;
    void raiseError(Throwable cause, String template, Object...  parameters) throws T ;
}
