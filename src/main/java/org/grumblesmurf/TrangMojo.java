/**
 * Copyright (c) 2007 Espen Wiborg <espenhw@grumblesmurf.org>
 * 
 * Permission to use, copy, modify, and distribute this software for any purpose
 * with or without fee is hereby granted, provided that the above copyright
 * notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
 * INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
 * LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
 * OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THIS SOFTWARE.
 */
package org.grumblesmurf;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Arrays;

import com.thaiopensource.relaxng.translate.Driver;

/**
 * Goal which executes Trang on a set of files.
 *
 * @goal trang
 */
public class TrangMojo
    extends AbstractMojo
{
    /**
     * The input files.
     * @parameter
     * @required
     */
    private File[] inputFiles;

    /**
     * The output directory.
     * @parameter default-value="${project.build.directory}/trang"
     * @required
     */
    private File outputDirectory;
    
    /**
     * The output file name.
     * @parameter
     * @required
     */
    private String outputFileName;

    public void execute()
        throws MojoExecutionException, MojoFailureException {
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        File outputFile = new File(outputDirectory, outputFileName);
        long outputModified = outputFile.lastModified();

        boolean stale = false;
        
        String[] args = new String[inputFiles.length + 1];
        int i;
        for (i = 0; i < inputFiles.length; i++) {
            File inputFile = inputFiles[i];
            if (!inputFile.isFile())
                throw new MojoExecutionException("Input file " + inputFile.getAbsolutePath() + " does not exist as a file");
            if (inputFile.lastModified() > outputModified)
                stale = true;
            args[i] = inputFiles[i].getAbsolutePath();
        }
        args[i] = outputFile.getAbsolutePath();

        if (!stale) {
            getLog().info("Output is current, skipping trang invocation");
            return;
        }
        
        getLog().debug("Executing trang with parameters " + Arrays.toString(args));
        
        String driverClassName = Driver.class.getName();
        try {
            Driver d = new Driver();

            Method doMain = Driver.class.getDeclaredMethod("doMain", String[].class);
            doMain.setAccessible(true);

            Integer returnValue = (Integer)doMain.invoke(d, (Object)args);
            if (returnValue.intValue() != 0)
                throw new MojoFailureException("Trang execution failed");
        } catch (SecurityException e) {
            throw new MojoExecutionException("Failed to set accessibility of " + driverClassName + ".doMain()", e);
        } catch (InvocationTargetException e) {
            throw new MojoExecutionException("Invocation of " + driverClassName + ".doMain() threw " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new MojoExecutionException("Could not access " + driverClassName + ".doMain()", e);
        } catch (NoSuchMethodException e) {
            throw new MojoExecutionException("Class " + driverClassName + " has no method doMain(String[])");
        }
    }
}
