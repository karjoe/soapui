/*
 *  soapUI, copyright (C) 2004-2008 eviware.com 
 *
 *  soapUI is free software; you can redistribute it and/or modify it under the 
 *  terms of version 2.1 of the GNU Lesser General Public License as published by 
 *  the Free Software Foundation.
 *
 *  soapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU Lesser General Public License for more details at gnu.org.
 */

package com.eviware.soapui.report;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.eviware.soapui.model.testsuite.TestCase;
import com.eviware.soapui.model.testsuite.TestRunContext;
import com.eviware.soapui.model.testsuite.TestRunListener;
import com.eviware.soapui.model.testsuite.TestRunner;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.model.testsuite.TestSuite;
import com.eviware.soapui.model.testsuite.TestRunner.Status;
import com.eviware.soapui.model.testsuite.TestStepResult.TestStepStatus;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.xml.XmlUtils;

/**
 * Collects TestRun results and creates JUnitReports
 * 
 * @author ole.matzura
 */

public class JUnitReportCollector implements TestRunListener {

	HashMap<String, JUnitReport> reports;
	HashMap<TestCase, StringBuffer> failures;
	
	public JUnitReportCollector() {
		reports = new HashMap<String, JUnitReport>();
		failures = new HashMap<TestCase, StringBuffer>();
	}
	
	public List<String> saveReports(String path) throws Exception {
		
		File file = new File( path );
		if( !file.exists() || !file.isDirectory() )
			file.mkdirs();
		
		List<String> result = new ArrayList<String>();
		
		Iterator<String> keyset = reports.keySet().iterator();
		while (keyset.hasNext()) {
			String name = keyset.next();
			JUnitReport report = reports.get(name);
			String fileName = path + File.separatorChar + "TEST-" + StringUtils.createFileName( name, '_' ) + ".xml";
			saveReport(report, fileName);
			result.add( fileName );
		}
		
		return result;
	}
	
	public HashMap<String, JUnitReport> getReports()
	{
		return reports;
	}

	private void saveReport(JUnitReport report, String filename) throws Exception {
		report.save(new File( filename ));
	}
	
	public String getReport() {
		Set<String> keys = reports.keySet();
		if (keys.size() > 0) {
			String key = (String)keys.toArray()[0];
			return reports.get(key).toString();
		}
		return "No reports..:";
	}
	
	public void afterRun(TestRunner testRunner, TestRunContext runContext) {
		TestCase testCase = testRunner.getTestCase();
		JUnitReport report = reports.get(testCase.getTestSuite().getName());
		
		if (Status.INITIALIZED != testRunner.getStatus()
				&& Status.RUNNING != testRunner.getStatus()) {
			if (Status.CANCELED == testRunner.getStatus()) {
				report.addTestCaseWithFailure(testCase.getName(), testRunner.getTimeTaken(), testRunner.getReason(), "");
			}
			if ( Status.FAILED == testRunner.getStatus()) {
				String msg = "";
				if (failures.containsKey(testCase)) {
					msg = failures.get(testCase).toString();
				}
				report.addTestCaseWithFailure(testCase.getName(), testRunner.getTimeTaken(), testRunner.getReason(), msg);
			}
			if (Status.FINISHED == testRunner.getStatus()) {
				report.addTestCase(testCase.getName(), testRunner.getTimeTaken());
			}
			
		}
	}

	public void afterStep(TestRunner testRunner, TestRunContext runContext, TestStepResult result) {
		TestStep currentStep = runContext.getCurrentStep();
		TestCase testCase = currentStep.getTestCase();
		
		if( result.getStatus() == TestStepStatus.FAILED )
		{
			StringBuffer buf = new StringBuffer();
			if (failures.containsKey(testCase)) {
				buf = failures.get(testCase);
			} else
				failures.put(testCase, buf);
			
			buf.append( "<h3><b>" + result.getTestStep().getName() + " Failed</b></h3>" );
			buf.append( "<pre>" + XmlUtils.entitize( Arrays.toString( result.getMessages() )) + "\n" );
			
			StringWriter stringWriter = new StringWriter();
			PrintWriter writer = new PrintWriter( stringWriter );
			result.writeTo( writer );
			
			buf.append( XmlUtils.entitize( stringWriter.toString()) );
			buf.append( "</pre><hr/>" );
		}
	}

	public void beforeRun(TestRunner testRunner, TestRunContext runContext) {
		TestCase testCase = testRunner.getTestCase();
		TestSuite testSuite = testCase.getTestSuite();
		if (!reports.containsKey(testSuite.getName())) {
			JUnitReport report = new JUnitReport();
			report.setTestSuiteName( testSuite.getName());
			reports.put(testSuite.getName(), report);
		}
	}

	public void beforeStep(TestRunner testRunner, TestRunContext runContext) 
	{
	}

	public void reset()
	{
		reports.clear();
		failures.clear();
	}
}
