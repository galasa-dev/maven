/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.maven.plugin.error;

import static org.assertj.core.api.Assertions.*;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import dev.galasa.maven.plugin.ErrorRaiserMavenImpl;
import dev.galasa.maven.plugin.MockLog;

public class ErrorRaiserTest {
    @Test
    public void TestCanLogErrorWithOneParameterOk() throws Exception {

        MockLog mockLog = new MockLog();
        ErrorRaiser<MojoExecutionException> raiser = new ErrorRaiserMavenImpl(mockLog);
        Exception ex = catchException(()-> raiser.raiseError("simple template {0}","param1"));

        assertThat(ex).isInstanceOf(MojoExecutionException.class).hasMessageContaining("simple template param1");
        mockLog.assertContainsRecord("ERROR:simple template param1");
    }

    @Test
    public void TestCanLogErrorWithTwoParametersOk() throws Exception {
        MockLog mockLog = new MockLog();
        ErrorRaiser<MojoExecutionException> raiser = new ErrorRaiserMavenImpl(mockLog);
        Exception ex = catchException(()-> raiser.raiseError("simple template {0} {1}","param1","param2"));

        assertThat(ex).isInstanceOf(MojoExecutionException.class).hasMessageContaining("simple template param1 param2");
        mockLog.assertContainsRecord("ERROR:simple template param1 param2");
    }

    @Test
    public void TestCanLogErrorWithZeroParametersOk() throws Exception {
        MockLog mockLog = new MockLog();
        ErrorRaiser<MojoExecutionException> raiser = new ErrorRaiserMavenImpl(mockLog);
        Exception ex = catchException(()-> raiser.raiseError("simple template"));

        assertThat(ex).isInstanceOf(MojoExecutionException.class).hasMessageContaining("simple template");
        mockLog.assertContainsRecord("ERROR:simple template");
    }

}
