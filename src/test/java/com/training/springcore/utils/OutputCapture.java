package com.training.springcore.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static org.hamcrest.Matchers.allOf;

/**
 * JUnit {@code @Rule} to capture output from System.out and System.err.
 * This class is a Spring Boot class. We don't use this module i the first part of the training
 */
public class OutputCapture implements TestRule {

    private CaptureOutputStream captureOut;

    private CaptureOutputStream captureErr;

    private ByteArrayOutputStream copy;

    private List<Matcher<? super String>> matchers = new ArrayList<>();

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                captureOutput();
                try {
                    base.evaluate();
                }
                finally {
                    try {
                        if (!OutputCapture.this.matchers.isEmpty()) {
                            String output = OutputCapture.this.toString();
                            Assert.assertThat(output, allOf(OutputCapture.this.matchers));
                        }
                    }
                    finally {
                        releaseOutput();
                    }
                }
            }
        };
    }

    protected void captureOutput() {
        AnsiOutputControl.get().disableAnsiOutput();
        this.copy = new ByteArrayOutputStream();
        this.captureOut = new CaptureOutputStream(System.out, this.copy);
        this.captureErr = new CaptureOutputStream(System.err, this.copy);
        System.setOut(new PrintStream(this.captureOut));
        System.setErr(new PrintStream(this.captureErr));
    }

    protected void releaseOutput() {
        AnsiOutputControl.get().enabledAnsiOutput();
        System.setOut(this.captureOut.getOriginal());
        System.setErr(this.captureErr.getOriginal());
        this.copy = null;
    }

    /**
     * Discard all currently accumulated output.
     */
    public void reset() {
        this.copy.reset();
    }

    public void flush() {
        try {
            this.captureOut.flush();
            this.captureErr.flush();
        }
        catch (IOException ex) {
            // ignore
        }
    }

    @Override
    public String toString() {
        flush();
        return this.copy.toString();
    }

    /**
     * Verify that the output is matched by the supplied {@code matcher}. Verification is
     * performed after the test method has executed.
     * @param matcher the matcher
     */
    public void expect(Matcher<? super String> matcher) {
        this.matchers.add(matcher);
    }

    private static class CaptureOutputStream extends OutputStream {

        private final PrintStream original;

        private final OutputStream copy;

        CaptureOutputStream(PrintStream original, OutputStream copy) {
            this.original = original;
            this.copy = copy;
        }

        @Override
        public void write(int b) throws IOException {
            this.copy.write(b);
            this.original.write(b);
            this.original.flush();
        }

        @Override
        public void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            this.copy.write(b, off, len);
            this.original.write(b, off, len);
        }

        public PrintStream getOriginal() {
            return this.original;
        }

        @Override
        public void flush() throws IOException {
            this.copy.flush();
            this.original.flush();
        }

    }

    /**
     * Allow AnsiOutput to not be on the test classpath.
     */
    private static class AnsiOutputControl {

        public void disableAnsiOutput() {
        }

        public void enabledAnsiOutput() {
        }

        public static AnsiOutputControl get() {
            try {
                Class.forName("org.springframework.boot.ansi.AnsiOutput");
                return new AnsiPresentOutputControl();
            }
            catch (ClassNotFoundException ex) {
                return new AnsiOutputControl();
            }
        }

    }

    private static class AnsiPresentOutputControl extends AnsiOutputControl {

        @Override
        public void disableAnsiOutput() {
            //AnsiOutput.setEnabled(Enabled.NEVER);
        }

        @Override
        public void enabledAnsiOutput() {
            //AnsiOutput.setEnabled(Enabled.DETECT);
        }

    }

}
