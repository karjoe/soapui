package com.eviware.soapui.impl.wsdl.teststeps;

import java.util.List;

import org.apache.log4j.Logger;

import com.eviware.soapui.impl.wsdl.mock.MockRunnerManager;
import com.eviware.soapui.impl.wsdl.mock.MockRunnerManagerException;
import com.eviware.soapui.impl.wsdl.mock.MockRunnerManagerImpl;
import com.eviware.soapui.model.support.TestRunListenerAdapter;
import com.eviware.soapui.model.testsuite.LoadTestRunner;
import com.eviware.soapui.model.testsuite.TestCase;
import com.eviware.soapui.model.testsuite.TestRunContext;
import com.eviware.soapui.model.testsuite.TestRunner;

public class TestRunListenerImpl extends TestRunListenerAdapter
{
	private final static Logger log = 
		Logger.getLogger(TestRunListenerImpl.class);
	
	public void beforeRun(TestRunner testRunner, TestRunContext runContext)
	{
		LoadTestRunner loadTestRunner = 
			(LoadTestRunner) runContext.getProperty("LoadTestRunner");

		if (loadTestRunner == null)
		{
			TestCase testCase = testRunner.getTestCase();

			if (needsMockRunnerManager(testCase))
			{
				createMockServices(testCase);
				
				MockRunnerManager manager = MockRunnerManagerImpl.getInstance(
						testCase);
				
				try
				{
					manager.start();
				}
				catch (MockRunnerManagerException e)
				{
					log.error("Unable to start MockRunnerManager", e);
				}
			}
		}
	}

	public void afterRun(TestRunner testRunner, TestRunContext runContext)
	{
		LoadTestRunner loadTestRunner = 
			(LoadTestRunner) runContext.getProperty("LoadTestRunner");

		if (loadTestRunner == null)
		{
			TestCase testCase = testRunner.getTestCase();

			MockRunnerManager manager = MockRunnerManagerImpl.getInstance(
					testCase);

			if (manager != null)
			{
				manager.stop();
			}
		}
	}
	
	private boolean needsMockRunnerManager(TestCase testCase)
	{
		return testCase.getTestStepsOfType(
				WsdlAsyncResponseTestStep.class).size() > 0;
	}

	private void createMockServices(TestCase testCase)
	{
		List<WsdlAsyncResponseTestStep> teststeps = 
			testCase.getTestStepsOfType(WsdlAsyncResponseTestStep.class);
		
		for (WsdlAsyncResponseTestStep teststep : teststeps)
		{
			teststep.createMockService();
		}
	}
}
