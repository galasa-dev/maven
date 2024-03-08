/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.maven.plugin.file;

import java.io.OutputStream;

public interface Artifact {
    OutputStream getOutputStream();
}
