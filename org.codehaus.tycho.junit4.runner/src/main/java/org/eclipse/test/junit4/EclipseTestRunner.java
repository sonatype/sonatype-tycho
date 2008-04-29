/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.test.junit4;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import junit.framework.AssertionFailedError;
import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.junit.FormatterElement;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;

/**
 * A TestRunner for JUnit that supports Ant JUnitResultFormatters and running
 * tests inside Eclipse. Example call: EclipseTestRunner -classname
 * junit.samples.SimpleTest
 * formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter
 */
public class EclipseTestRunner implements TestListener {
	class TestFailedException extends Exception {

		private static final long serialVersionUID = 6009335074727417445L;

		TestFailedException(String message) {
			super(message);
		}

		TestFailedException(Throwable e) {
			super(e);
		}
	}

	/**
	 * No problems with this test.
	 */
	public static final int SUCCESS = 0;

	/**
	 * Some tests failed.
	 */
	public static final int FAILURES = 1;

	/**
	 * An error occured.
	 */
	public static final int ERRORS = 2;

	private static final String SUITE_METHODNAME = "suite";

	/**
	 * debug mode
	 */
	private boolean debug = false;

	/**
	 * The current test result
	 */
	private TestResult fTestResult;

	/**
	 * The name of the plugin containing the test
	 */
	private String fTestPluginName;

	/**
	 * The corresponding testsuite.
	 */
	private Test fSuite;

	/**
	 * Formatters from the command line.
	 */
	private static Vector fgFromCmdLine = new Vector();

	/**
	 * Holds the registered formatters.
	 */
	private Vector formatters = new Vector();

	/**
	 * Do we stop on errors.
	 */
	private boolean fHaltOnError = false;

	/**
	 * Do we stop on test failures.
	 */
	private boolean fHaltOnFailure = false;

	/**
	 * The TestSuite we are currently running.
	 */
	private JUnitTest fJunitTest;

	/**
	 * output written during the test
	 */
	private PrintStream fSystemError;

	/**
	 * Error output during the test
	 */
	private PrintStream fSystemOut;

	/**
	 * Exception caught in constructor.
	 */
	private Exception fException;

	/**
	 * Returncode
	 */
	private int fRetCode = SUCCESS;

	/**
	 * The main entry point (the parameters are not yet consistent with the Ant
	 * JUnitTestRunner, but eventually they should be). Parameters
	 * 
	 * <pre>
	 *  -className: the name of the testSuite
	 *  -testPluginName: the name of the containing plugin
	 *  haltOnError: halt test on errors?
	 *  haltOnFailure: halt test on failures?
	 *  -testlistener listenerClass: deprecated
	 *  		print a warning that this option is deprecated
	 *  formatter: a JUnitResultFormatter given as classname,filename. 
	 *   	If filename is ommitted, System.out is assumed.
	 * </pre>
	 */
	public static void main(String[] args) throws IOException {
		System.exit(run(args));
	}

	public static int run(String[] args) throws IOException {
		String className = null;
		String testPluginName = null;

		boolean haltError = false;
		boolean haltFail = false;

		Properties props = new Properties();

		int startArgs = 0;
		if (args.length > 0) {
			// support the JUnit task commandline syntax where
			// the first argument is the name of the test class
			if (!args[0].startsWith("-")) {
				className = args[0];
				startArgs++;
			}
		}
		for (int i = startArgs; i < args.length; i++) {
			if (args[i].toLowerCase().equals("-classname")) {
				if (i < args.length - 1)
					className = args[i + 1];
				i++;
			} else if (args[i].toLowerCase().equals("-testpluginname")) {
				if (i < args.length - 1)
					testPluginName = args[i + 1];
				i++;
			} else if (args[i].startsWith("haltOnError=")) {
				haltError = Project.toBoolean(args[i].substring(12));
			} else if (args[i].startsWith("haltOnFailure=")) {
				haltFail = Project.toBoolean(args[i].substring(14));
			} else if (args[i].startsWith("formatter=")) {
				try {
					createAndStoreFormatter(args[i].substring(10));
				} catch (BuildException be) {
					System.err.println(be.getMessage());
					return ERRORS;
				}
			} else if (args[i].startsWith("propsfile=")) {
				FileInputStream in = new FileInputStream(args[i].substring(10));
				props.load(in);
				in.close();
			} else if (args[i].equals("-testlistener")) {
				System.err
						.println("The -testlistener option is no longer supported\nuse the formatter= option instead");
				return ERRORS;
			}
		}

		if (className == null)
			throw new IllegalArgumentException("Test class name not specified");

		JUnitTest t = new JUnitTest(className);

		// Add/overlay system properties on the properties from the Ant project
		Hashtable p = System.getProperties();
		for (Enumeration _enum = p.keys(); _enum.hasMoreElements();) {
			Object key = _enum.nextElement();
			props.put(key, p.get(key));
		}
		t.setProperties(props);

		EclipseTestRunner runner = new EclipseTestRunner(t, testPluginName,
				haltError, haltFail);
		transferFormatters(runner);
		runner.run();
		return runner.getRetCode();
	}

	/**
	 * 
	 */
	public EclipseTestRunner(JUnitTest test, String testPluginName,
			boolean haltOnError, boolean haltOnFailure) {
		fJunitTest = test;
		fTestPluginName = testPluginName;
		fHaltOnError = haltOnError;
		fHaltOnFailure = haltOnFailure;

		try {
			fSuite = getTest(test.getName());
		} catch (Exception e) {
			fRetCode = ERRORS;
			fException = e;
		}
	}

	/**
	 * Returns the Test corresponding to the given suite.
	 */
	protected Test getTest(String suiteClassName) throws TestFailedException {
		if (suiteClassName.length() <= 0) {
			return null;
		}
		Class testClass = null;
		try {
			testClass = Activator.getInstance().loadClass(fTestPluginName, suiteClassName);
		} catch (ClassNotFoundException e) {
			if (e.getCause() != null) {
				runFailed(e.getCause());
			}
			String clazz = e.getMessage();
			if (clazz == null)
				clazz = suiteClassName;
			runFailed("Class not found \"" + clazz + "\"");
			return null;
		} catch (Exception e) {
			runFailed(e);
			return null;
		}
		Method suiteMethod = null;
		try {
			suiteMethod = testClass.getMethod(SUITE_METHODNAME, new Class[0]);
		} catch (Exception e) {
		}
		Test suite = null;
		try {
			if (suiteMethod != null) {
				// if there is a suite method available, then try
				// to extract the suite from it. If there is an error
				// here it will be caught below and reported.
				suite = (Test) suiteMethod.invoke(null, new Class[0]);

			} else {
                Class junit4TestAdapterClass = null;

                // Check for JDK 5 first. Will *not* help on JDK 1.4
                // if only junit-4.0.jar in CP because in that case
                // linkage of whole task will already have failed! But
                // will help if CP has junit-3.8.2.jar:junit-4.0.jar.

                // In that case first C.fN will fail with CNFE and we
                // will avoid UnsupportedClassVersionError.

                boolean junit4;
                try {
                    Class.forName("java.lang.annotation.Annotation");
                        junit4TestAdapterClass =
                            Class.forName("junit.framework.JUnit4TestAdapter");
                } catch (ClassNotFoundException e) {
                    // OK, fall back to JUnit 3.
                }
                junit4 = junit4TestAdapterClass != null;

                if (junit4) {
                    // Let's use it!
                    suite =
                        (Test) junit4TestAdapterClass
                        .getConstructor(new Class[] {Class.class}).
                        newInstance(new Object[] {testClass});
                } else {
                    // Use JUnit 3.

                    // try to extract a test suite automatically this
                    // will generate warnings if the class is no
                    // suitable Test
                    suite = new TestSuite(testClass);
                }
			}
		} catch (InvocationTargetException e) {
			runFailed("Failed to invoke suite():"
					+ e.getTargetException().toString());
			return null;
		} catch (Throwable e) {
			runFailed("Failed to invoke suite():" + e.toString());
			return null;
		}
		return suite;
	}

	protected void runFailed(String message) throws TestFailedException {
		System.err.println(message);
		throw new TestFailedException(message);
	}

	protected void runFailed(Throwable e) throws TestFailedException {
		e.printStackTrace();
		throw new TestFailedException(e);
	}

	public void run() {
		// IPerformanceMonitor pm =
		// PerfMsrCorePlugin.getPerformanceMonitor(true);

		fTestResult = new TestResult();
		fTestResult.addListener(this);
		for (int i = 0; i < formatters.size(); i++) {
			fTestResult.addListener((TestListener) formatters.elementAt(i));
		}

		long start = System.currentTimeMillis();
		fireStartTestSuite();

		if (fException != null) { // had an exception in the constructor
			for (int i = 0; i < formatters.size(); i++) {
				((TestListener) formatters.elementAt(i)).addError(null,
						fException);
			}
			fJunitTest.setCounts(1, 0, 1);
			fJunitTest.setRunTime(0);
		} else {
			ByteArrayOutputStream errStrm = new ByteArrayOutputStream();
			fSystemError = new PrintStream(errStrm);
			System.setErr(fSystemError);

			ByteArrayOutputStream outStrm = new ByteArrayOutputStream();
			fSystemOut = new PrintStream(outStrm);
			System.setOut(fSystemOut);
			
			try {
				// pm.snapshot(1); // before
				fSuite.run(fTestResult);
			} finally {
				// pm.snapshot(2); // after
				fSystemError.close();
				fSystemError = null;
				fSystemOut.close();
				fSystemOut = null;
				sendOutAndErr(new String(outStrm.toByteArray()), new String(
						errStrm.toByteArray()));
				fJunitTest.setCounts(fTestResult.runCount(), fTestResult
						.failureCount(), fTestResult.errorCount());
				fJunitTest.setRunTime(System.currentTimeMillis() - start);
			}
		}
		fireEndTestSuite();

		if (fRetCode != SUCCESS || fTestResult.errorCount() != 0) {
			fRetCode = ERRORS;
		} else if (fTestResult.failureCount() != 0) {
			fRetCode = FAILURES;
		}

		// pm.upload(getClass().getName());
	}

	/**
	 * Returns what System.exit() would return in the standalone version.
	 * 
	 * @return 2 if errors occurred, 1 if tests failed else 0.
	 */
	public int getRetCode() {
		return fRetCode;
	}

	/*
	 * @see TestListener.addFailure
	 */
	public void startTest(Test t) {
	}

	/*
	 * @see TestListener.addFailure
	 */
	public void endTest(Test test) {
	}

	/*
	 * @see TestListener.addFailure
	 */
	public void addFailure(Test test, AssertionFailedError t) {
		if (fHaltOnFailure) {
			fTestResult.stop();
		}
	}

	/*
	 * @see TestListener.addError
	 */
	public void addError(Test test, Throwable t) {
		if (fHaltOnError) {
			fTestResult.stop();
		}
	}

	private void fireStartTestSuite() {
		for (int i = 0; i < formatters.size(); i++) {
			((JUnitResultFormatter) formatters.elementAt(i))
					.startTestSuite(fJunitTest);
		}
	}

	private void fireEndTestSuite() {
		for (int i = 0; i < formatters.size(); i++) {
			((JUnitResultFormatter) formatters.elementAt(i))
					.endTestSuite(fJunitTest);
		}
	}

	public void addFormatter(JUnitResultFormatter f) {
		formatters.addElement(f);
	}

	/**
	 * Line format is: formatter=<classname>(,<pathname>)?
	 */
	private static void createAndStoreFormatter(String line)
			throws BuildException {
		FormatterElement fe = new FormatterElement();
		String formatterClassName = null;
		File formatterFile = null;

		int pos = line.indexOf(',');
		if (pos == -1) {
			formatterClassName = line;
		} else {
			formatterClassName = line.substring(0, pos);
			formatterFile = new File(line.substring(pos + 1)); // the method is
																// package
																// visible
		}
		fgFromCmdLine.addElement(createFormatter(formatterClassName,
				formatterFile));
	}

	private static void transferFormatters(EclipseTestRunner runner) {
		for (int i = 0; i < fgFromCmdLine.size(); i++) {
			runner.addFormatter((JUnitResultFormatter) fgFromCmdLine
					.elementAt(i));
		}
	}

	/*
	 * DUPLICATED from FormatterElement, since it is package visible only
	 */
	private static JUnitResultFormatter createFormatter(String classname,
			File outfile) throws BuildException {
		OutputStream out = System.out;

		if (classname == null) {
			throw new BuildException("you must specify type or classname");
		}
		Class f = null;
		try {
			f = EclipseTestRunner.class.getClassLoader().loadClass(classname);
		} catch (ClassNotFoundException e) {
			throw new BuildException(e);
		}

		Object o = null;
		try {
			o = f.newInstance();
		} catch (InstantiationException e) {
			throw new BuildException(e);
		} catch (IllegalAccessException e) {
			throw new BuildException(e);
		}

		if (!(o instanceof JUnitResultFormatter)) {
			throw new BuildException(classname
					+ " is not a JUnitResultFormatter");
		}

		JUnitResultFormatter r = (JUnitResultFormatter) o;

		if (outfile != null) {
			try {
				out = new FileOutputStream(outfile);
			} catch (java.io.IOException e) {
				throw new BuildException(e);
			}
		}
		r.setOutput(out);
		return r;
	}

	private void sendOutAndErr(String out, String err) {
		for (int i = 0; i < formatters.size(); i++) {
			JUnitResultFormatter formatter = ((JUnitResultFormatter) formatters
					.elementAt(i));

			formatter.setSystemOutput(out);
			formatter.setSystemError(err);
		}
	}

	protected void handleOutput(String line) {
		if (fSystemOut != null) {
			fSystemOut.println(line);
		}
	}

	protected void handleErrorOutput(String line) {
		if (fSystemError != null) {
			fSystemError.println(line);
		}
	}
}
