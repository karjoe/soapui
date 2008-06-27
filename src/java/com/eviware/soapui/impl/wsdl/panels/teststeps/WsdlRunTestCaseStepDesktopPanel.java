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

package com.eviware.soapui.impl.wsdl.panels.teststeps;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.RunTestCaseRunModeTypeConfig;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.WsdlTestSuite;
import com.eviware.soapui.impl.wsdl.actions.support.ShowOnlineHelpAction;
import com.eviware.soapui.impl.wsdl.panels.support.MockTestRunContext;
import com.eviware.soapui.impl.wsdl.panels.support.MockTestRunner;
import com.eviware.soapui.impl.wsdl.panels.testcase.TestRunLog;
import com.eviware.soapui.impl.wsdl.panels.testcase.TestRunLog.TestRunLogTestRunListener;
import com.eviware.soapui.impl.wsdl.panels.teststeps.support.PropertyHolderTable;
import com.eviware.soapui.impl.wsdl.support.HelpUrls;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCaseRunner;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlRunTestCaseTestStep;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.model.testsuite.TestCase;
import com.eviware.soapui.model.testsuite.TestRunContext;
import com.eviware.soapui.model.testsuite.TestRunner;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JComponentInspector;
import com.eviware.soapui.support.components.JInspectorPanel;
import com.eviware.soapui.support.components.JXToolBar;
import com.eviware.soapui.support.types.StringList;
import com.eviware.soapui.ui.support.ModelItemDesktopPanel;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormField;
import com.eviware.x.form.XFormFieldListener;
import com.eviware.x.form.support.ADialogBuilder;
import com.eviware.x.form.support.AField;
import com.eviware.x.form.support.AForm;
import com.eviware.x.form.support.XFormMultiSelectList;
import com.eviware.x.form.support.AField.AFieldType;

public class WsdlRunTestCaseStepDesktopPanel extends ModelItemDesktopPanel<WsdlRunTestCaseTestStep> implements PropertyChangeListener
{
	private WsdlProject project;
	private TitledBorder titledBorder;
	private OptionsAction optionsAction;
	private RunAction runAction;
	private OpenTestCaseAction openTestCaseAction;
	private TestRunLog testRunLog;
	private CancelRunTestCaseAction cancelAction;
	private XFormDialog optionsDialog;

	public WsdlRunTestCaseStepDesktopPanel( WsdlRunTestCaseTestStep modelItem )
	{
		super( modelItem );
		
		project = getModelItem().getTestCase().getTestSuite().getProject();
		
		getModelItem().addPropertyChangeListener( WsdlRunTestCaseTestStep.TARGET_TESTCASE, this );
		
		buildUI();
		setEnabledState();
		
		if( modelItem.getTargetTestCase() == null )
		{
			SwingUtilities.invokeLater( new Runnable() {
	
				public void run()
				{
					optionsAction.actionPerformed( null );
				}} );
		}
		
		setPreferredSize( new Dimension( 400, 600 ) );
	}

	private void setEnabledState()
	{
		runAction.setEnabled( getModelItem().getTargetTestCase() != null );
		openTestCaseAction.setEnabled( getModelItem().getTargetTestCase() != null );
	}

	private void buildUI()
	{
		add( buildToolbar(), BorderLayout.NORTH );
		add( buildContent(), BorderLayout.CENTER );
	}

	private Component buildContent()
	{
		JInspectorPanel inspectorPanel = new JInspectorPanel( createPropertiesTable() );
		
		inspectorPanel.addInspector( new JComponentInspector<JComponent>( buildLog(), "TestCase Log", "log output from testcase run", true ) );
		
		return inspectorPanel;
	}

	private JComponent buildLog()
	{
		testRunLog = new TestRunLog( getModelItem().getSettings() );
		return testRunLog;
	}

	protected JComponent createPropertiesTable()
	{
		PropertyHolderTable propertyHolderTable = new PropertyHolderTable( getModelItem() );
		
		titledBorder = BorderFactory.createTitledBorder( createTitleForBorder() );
		propertyHolderTable.setBorder( titledBorder);
		
		return propertyHolderTable;
	}

	private String createTitleForBorder()
	{
		WsdlTestCase targetTestCase = getModelItem().getTargetTestCase();
		return "TestCase [" + (targetTestCase == null ? "-none selected-" : targetTestCase.getName() ) + "] Run Properties";
	}

	private Component buildToolbar()
	{
		JXToolBar toolbar = UISupport.createToolbar();
		
		toolbar.add( UISupport.createToolbarButton( runAction = new RunAction() ));
		toolbar.add( UISupport.createToolbarButton( cancelAction = new CancelRunTestCaseAction(), false ));
		toolbar.add( UISupport.createToolbarButton( optionsAction = new OptionsAction() ));
		toolbar.add( UISupport.createToolbarButton( openTestCaseAction = new OpenTestCaseAction() ));
		
		toolbar.addGlue();
		toolbar.add( UISupport.createToolbarButton( new ShowOnlineHelpAction( HelpUrls.RUNTESTCASESTEP_HELP_URL )));
		
		return toolbar;
	}

	@Override
	public boolean dependsOn( ModelItem modelItem )
	{
		WsdlRunTestCaseTestStep callStep = getModelItem();
		
		return modelItem == callStep || modelItem == callStep.getTestCase() ||
			modelItem == callStep.getTestCase().getTestSuite() ||
			modelItem == callStep.getTestCase().getTestSuite().getProject();
	}

	public boolean onClose( boolean canCancel )
	{
		getModelItem().removePropertyChangeListener( WsdlRunTestCaseTestStep.TARGET_TESTCASE, this );
		testRunLog.release();
		if( optionsDialog != null )
		{
			optionsDialog.release();
			optionsDialog = null;
		}
		
		return release();
	}
	
	private class RunAction extends AbstractAction
	{
		public RunAction()
		{
			putValue( Action.SMALL_ICON, UISupport.createImageIcon( "/run_testcase.gif" ));
			putValue( Action.SHORT_DESCRIPTION, "Runs the selected TestCases" );
		}
		
		public void actionPerformed(ActionEvent e)
		{
			runAction.setEnabled( false );
			cancelAction.setEnabled( true );

			new Thread( new Runnable() {

				public void run()
				{
					WsdlRunTestCaseTestStep testStep = getModelItem();
					InternalTestRunListener testRunListener = new InternalTestRunListener();
					testStep.addTestRunListener( testRunListener );
					
					try
					{
						testRunLog.clear();
						MockTestRunner mockTestRunner = new MockTestRunner( testStep.getTestCase(), SoapUI.ensureGroovyLog() );
						WsdlTestStepResult result = (WsdlTestStepResult) testStep.run( mockTestRunner, 
								new MockTestRunContext( mockTestRunner, testStep ) );
						
						Throwable er = result.getError();
						if( er != null )
						{
							UISupport.showErrorMessage( er.toString() );
						}
					}
					catch( Throwable t )
					{
						UISupport.showErrorMessage( t );
					}
					finally 
					{
						testStep.removeTestRunListener( testRunListener );
						runAction.setEnabled( true );
						cancelAction.setEnabled( false );
					}
				}} ).start();
		}
	}
	
	private class OpenTestCaseAction extends AbstractAction
	{
		public OpenTestCaseAction()
		{
			putValue( Action.SMALL_ICON, UISupport.createImageIcon( "/testCase.gif" ));
			putValue( Action.SHORT_DESCRIPTION, "Opens the target TestCases editor" );
		}
		
		public void actionPerformed(ActionEvent e)
		{
			WsdlTestCase targetTestCase = getModelItem().getTargetTestCase();
			if( targetTestCase == null )
				UISupport.showErrorMessage( "No target TestCase selected" );
			else
				UISupport.showDesktopPanel( targetTestCase );
		}
	}
	
	private class OptionsAction extends AbstractAction
	{
		public OptionsAction()
		{
			putValue( Action.SMALL_ICON, UISupport.createImageIcon( "/options.gif" ));
			putValue( Action.SHORT_DESCRIPTION, "Sets Options");
		}
		
		public void actionPerformed( ActionEvent e )
		{
			if( optionsDialog == null )
			{
				optionsDialog = ADialogBuilder.buildDialog( OptionsForm.class );
				optionsDialog.getFormField( OptionsForm.TESTSUITE ).addFormFieldListener( new XFormFieldListener() {

					public void valueChanged( XFormField sourceField, String newValue, String oldValue )
					{
						List<TestCase> testCaseList = project.getTestSuiteByName( newValue ).getTestCaseList();
						testCaseList.remove( getModelItem().getTestCase() );
						optionsDialog.setOptions( OptionsForm.TESTCASE, ModelSupport.getNames( testCaseList ));
						
						if( testCaseList.size() > 0 )
						{
							WsdlTestCase testCase = project.getTestSuiteByName( newValue ).getTestCaseAt( 0 );
							optionsDialog.setOptions( OptionsForm.RETURN_PROPERTIES, testCase.getPropertyNames() );
							((XFormMultiSelectList)optionsDialog.getFormField( OptionsForm.RETURN_PROPERTIES )).setSelectedOptions( 
										getModelItem().getReturnProperties().toStringArray() );
						}
					}
				} );
				optionsDialog.getFormField( OptionsForm.TESTCASE ).addFormFieldListener( new XFormFieldListener() {

					public void valueChanged( XFormField sourceField, String newValue, String oldValue )
					{
						WsdlTestSuite testSuite = project.getTestSuiteByName( optionsDialog.getValue( OptionsForm.TESTSUITE  ) );
						WsdlTestCase testCase = testSuite.getTestCaseByName( newValue );
						optionsDialog.setOptions( OptionsForm.RETURN_PROPERTIES, testCase.getPropertyNames() );
						((XFormMultiSelectList)optionsDialog.getFormField( OptionsForm.RETURN_PROPERTIES )).setSelectedOptions( 
									getModelItem().getReturnProperties().toStringArray() );
					}
				} );
			}
			
			WsdlTestCase targetTestCase = getModelItem().getTargetTestCase();
			
			optionsDialog.setOptions( OptionsForm.TESTSUITE, ModelSupport.getNames( project.getTestSuiteList() ) );
			if( targetTestCase != null )
			{
				optionsDialog.setValue( OptionsForm.TESTSUITE, targetTestCase.getTestSuite().getName() );
				
				List<TestCase> testCaseList = targetTestCase.getTestSuite().getTestCaseList();
				testCaseList.remove( getModelItem().getTestCase() );
				
				optionsDialog.setOptions( OptionsForm.TESTCASE, ModelSupport.getNames( testCaseList ));
				optionsDialog.setValue( OptionsForm.TESTCASE, targetTestCase.getName() );
				
				optionsDialog.setOptions( OptionsForm.RETURN_PROPERTIES, targetTestCase.getPropertyNames() );
				((XFormMultiSelectList)optionsDialog.getFormField( OptionsForm.RETURN_PROPERTIES )).setSelectedOptions( 
							getModelItem().getReturnProperties().toStringArray() );
			}
			else
			{
				if( project.getTestSuiteCount() == 0 )
				{
					optionsDialog.setOptions( OptionsForm.TESTCASE, new String[0] );
					optionsDialog.setOptions( OptionsForm.RETURN_PROPERTIES, new String[0] );
				}
				else
				{
					List<TestCase> testCaseList = project.getTestSuiteAt(0).getTestCaseList();
					testCaseList.remove( getModelItem().getTestCase() );
					optionsDialog.setOptions( OptionsForm.TESTCASE, ModelSupport.getNames( testCaseList ) );
					
					if( testCaseList.isEmpty() )
						optionsDialog.setOptions( OptionsForm.RETURN_PROPERTIES, new String[0] );
					else
						optionsDialog.setOptions( OptionsForm.RETURN_PROPERTIES, testCaseList.get( 0 ).getPropertyNames() );
				}
			}
			
			optionsDialog.setValue( OptionsForm.RUN_MODE, getModelItem().getRunMode() == RunTestCaseRunModeTypeConfig.PARALLELL ?
						OptionsForm.CREATE_ISOLATED_COPY_FOR_EACH_RUN : OptionsForm.RUN_PRIMARY_TEST_CASE );
			
			if( optionsDialog.show() )
			{
				WsdlTestSuite testSuite = project.getTestSuiteByName( optionsDialog.getValue( OptionsForm.TESTSUITE ) );
				getModelItem().setTargetTestCase( testSuite == null ? null : 
					testSuite.getTestCaseByName( optionsDialog.getValue( OptionsForm.TESTCASE ) ));
				getModelItem().setReturnProperties( new StringList( 
					((XFormMultiSelectList)optionsDialog.getFormField( OptionsForm.RETURN_PROPERTIES )).getSelectedOptions()) );
				getModelItem().setRunMode( optionsDialog.getValueIndex( OptionsForm.RUN_MODE ) == 0 ? 
							RunTestCaseRunModeTypeConfig.PARALLELL : RunTestCaseRunModeTypeConfig.SINGLETON_AND_FAIL );
			}
		}
	}
	
	@AForm( name="Run TestCase Options", description="Set options for the Run TestCase Step below" )
	private static interface OptionsForm 
	{
		public static final String RUN_PRIMARY_TEST_CASE = "Run primary TestCase (fail if already running)";
		public static final String CREATE_ISOLATED_COPY_FOR_EACH_RUN = "Create isolated copy for each run (Thread-Safe)";

		@AField( name="Target TestCase", description="Selects the TestCase to run", type=AFieldType.ENUMERATION)
		public static final String TESTCASE = "Target TestCase";

		@AField( name="Target TestSuite", description="Selects the containing TestSuite to run", type=AFieldType.ENUMERATION)
		public static final String TESTSUITE = "Target TestSuite";

		@AField( name="Return Properties", description="Selects the properties that are return values", type=AFieldType.MULTILIST)
		public static final String RETURN_PROPERTIES = "Return Properties";
		
		@AField( name="Run Mode", description="Sets how to run the target TestCase", type=AFieldType.RADIOGROUP, 
					values= {CREATE_ISOLATED_COPY_FOR_EACH_RUN, RUN_PRIMARY_TEST_CASE})
		public static final String RUN_MODE = "Run Mode";
	}

	public void propertyChange( PropertyChangeEvent evt )
	{
		setEnabledState();
		titledBorder.setTitle( createTitleForBorder() );
		repaint();
	}
	
	public class InternalTestRunListener extends TestRunLogTestRunListener
	{
		public InternalTestRunListener()
		{
			super( testRunLog, true );
		}

		public void beforeRun( TestRunner testRunner, TestRunContext runContext )
		{
			runAction.setEnabled( false );
			cancelAction.setEnabled( true );
		}

		public void afterRun( TestRunner testRunner, TestRunContext runContext )
		{
			runAction.setEnabled( true );
			cancelAction.setEnabled( false );
		}
	}
	
	public class CancelRunTestCaseAction extends AbstractAction
	{
		public CancelRunTestCaseAction()
		{
			putValue( Action.SMALL_ICON, UISupport.createImageIcon( "/stop_testcase.gif" ) );
			putValue( Action.SHORT_DESCRIPTION, "Stops running this testcase" );
		}

		public void actionPerformed( ActionEvent e )
		{
			 WsdlTestCaseRunner testCaseRunner = getModelItem().getTestCaseRunner();
			 if( testCaseRunner != null )
				 testCaseRunner.cancel( "Canceled from RunTestCase UI" );
		}
	}
}
