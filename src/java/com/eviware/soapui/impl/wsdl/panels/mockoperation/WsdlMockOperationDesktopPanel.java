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

package com.eviware.soapui.impl.wsdl.panels.mockoperation;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;

import org.apache.xmlbeans.XmlObject;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.DispatchStyleConfig;
import com.eviware.soapui.config.DispatchStyleConfig.Enum;
import com.eviware.soapui.impl.wsdl.WsdlInterface;
import com.eviware.soapui.impl.wsdl.WsdlOperation;
import com.eviware.soapui.impl.wsdl.actions.mockoperation.NewMockResponseAction;
import com.eviware.soapui.impl.wsdl.actions.mockoperation.OpenRequestForMockOperationAction;
import com.eviware.soapui.impl.wsdl.actions.support.ShowOnlineHelpAction;
import com.eviware.soapui.impl.wsdl.mock.WsdlMockOperation;
import com.eviware.soapui.impl.wsdl.mock.WsdlMockRequest;
import com.eviware.soapui.impl.wsdl.mock.WsdlMockResponse;
import com.eviware.soapui.impl.wsdl.mock.WsdlMockResult;
import com.eviware.soapui.impl.wsdl.panels.teststeps.support.GroovyEditor;
import com.eviware.soapui.impl.wsdl.panels.teststeps.support.GroovyEditorModel;
import com.eviware.soapui.impl.wsdl.support.HelpUrls;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.iface.Interface;
import com.eviware.soapui.model.iface.Operation;
import com.eviware.soapui.model.mock.MockOperation;
import com.eviware.soapui.model.mock.MockResponse;
import com.eviware.soapui.model.mock.MockServiceListener;
import com.eviware.soapui.model.settings.Settings;
import com.eviware.soapui.model.support.InterfaceListenerAdapter;
import com.eviware.soapui.model.support.ProjectListenerAdapter;
import com.eviware.soapui.model.util.ModelItemNames;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.swing.ActionList;
import com.eviware.soapui.support.action.swing.ActionSupport;
import com.eviware.soapui.support.action.swing.DefaultActionList;
import com.eviware.soapui.support.action.swing.SwingActionDelegate;
import com.eviware.soapui.support.components.JComponentInspector;
import com.eviware.soapui.support.components.JInspectorPanel;
import com.eviware.soapui.support.components.JXToolBar;
import com.eviware.soapui.support.swing.ExtendedComboBoxModel;
import com.eviware.soapui.support.swing.ModelItemListKeyListener;
import com.eviware.soapui.support.swing.ModelItemListMouseListener;
import com.eviware.soapui.support.types.StringList;
import com.eviware.soapui.support.xml.XmlUtils;
import com.eviware.soapui.ui.support.ModelItemDesktopPanel;
import com.jgoodies.forms.builder.ButtonBarBuilder;

/**
 * DesktopPanel for WsdlGroovyTestSteps
 * 
 * @author Ole.Matzura
 */

public class WsdlMockOperationDesktopPanel extends ModelItemDesktopPanel<WsdlMockOperation>
{
	private JList responseList;
	private JComboBox interfaceCombo;
	private JComboBox operationCombo;
	private JComboBox dispatchCombo;
	private JPanel dispatchPanel;
	private InternalInterfaceListener interfaceListener = new InternalInterfaceListener();
	private InternalProjectListener projectListener = new InternalProjectListener();
	private WsdlInterface currentInterface;
	private JPanel groovyEditorPanel;
	private JPanel xpathEditorPanel;
	private JComboBox defaultResponseCombo;
	private ResponseListModel responseListModel;
	private GroovyEditor xpathEditor;
	private GroovyEditor groovyEditor;
	private JComponentInspector<JComponent> dispatchInspector;

	public WsdlMockOperationDesktopPanel(WsdlMockOperation mockOperation)
	{
		super( mockOperation );
		
		buildUI();
		setPreferredSize( new Dimension( 600, 440 ));
		
		mockOperation.getMockService().getProject().addProjectListener( projectListener );
		
		WsdlOperation operation = getModelItem().getOperation();
		if( operation != null )
		{
			currentInterface = operation.getInterface();
			currentInterface.addInterfaceListener( interfaceListener );
		}
	}

	private void buildUI()
	{
		add( buildToolbar(), BorderLayout.NORTH );
		
      JInspectorPanel inspectorPanel = new JInspectorPanel( buildResponseList() );
      inspectorPanel.setDefaultDividerLocation( 0.5F );
      dispatchInspector = new JComponentInspector<JComponent>( 
				      			buildDispatchEditor(), "Dispatch (" + getModelItem().getDispatchStyle().toString() + ")", 
				      			"Configures current dispatch style", true );
		inspectorPanel.addInspector( dispatchInspector);
      inspectorPanel.activate( dispatchInspector );
		
      add( inspectorPanel, BorderLayout.CENTER );
	}

	private JComponent buildResponseList()
	{
		responseListModel = new ResponseListModel();
		responseList = new JList( responseListModel );
		responseList.addKeyListener( new ModelItemListKeyListener() {

			@Override
			public ModelItem getModelItemAt( int ix )
			{
				return getModelItem().getMockResponseAt( ix );
			}});
		
		responseList.addMouseListener( new ModelItemListMouseListener() {

			private DefaultActionList defaultActions;

			@Override
			protected ActionList getActionsForRow( JList list, int row )
			{
				ActionList actions = super.getActionsForRow( list, row );
				
				actions.insertAction( SwingActionDelegate.createDelegate( NewMockResponseAction.SOAPUI_ACTION_ID, getModelItem(), null, 
							"/addToMockService.gif"	), 0 );
				
				actions.insertAction( SwingActionDelegate.createDelegate( OpenRequestForMockOperationAction.SOAPUI_ACTION_ID, getModelItem(), null, 
							"/open_request.gif"), 1 );
				
				if( actions.getActionCount() > 2 )
					actions.insertAction( ActionSupport.SEPARATOR_ACTION, 2 );
				
				return actions;
			}

			@Override
			protected ActionList getDefaultActions()
			{
				if( defaultActions == null )
				{
					defaultActions = new DefaultActionList();
					defaultActions.addAction( SwingActionDelegate.createDelegate( NewMockResponseAction.SOAPUI_ACTION_ID, getModelItem(), null, 
								"/addToMockService.gif"	) );
				}
				
				return defaultActions;
			}
			
			
		} );
		responseList.setCellRenderer( new ResponseListCellRenderer() );
		
		JScrollPane scrollPane = new JScrollPane( responseList );
		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab( "MockResponses", UISupport.buildPanelWithToolbar( buildMockResponseListToolbar(), scrollPane ));
		
		return UISupport.createTabPanel( tabs, true );
	}
	
	private JComponent buildMockResponseListToolbar()
	{
		JXToolBar toolbar = UISupport.createToolbar();
		toolbar.add( UISupport.createToolbarButton( 
					SwingActionDelegate.createDelegate( NewMockResponseAction.SOAPUI_ACTION_ID, getModelItem(), 
								null, "/mockResponse.gif" )));

		return toolbar;
	}

	private JComponent buildDispatchEditor()
	{
		buildGroovyEditor();
		buildXPathEditor();
		
		dispatchPanel = new JPanel( new BorderLayout() );
		dispatchPanel.setOpaque( true );
		ButtonBarBuilder builder = new ButtonBarBuilder();
		builder.addFixed( new JLabel( "Dispatch: ") );
		builder.addRelatedGap();
		dispatchCombo = new JComboBox( new Object[] { "Sequence", "Random", "XPath", "Script"} );
		
		dispatchCombo.addItemListener( new ItemListener() {

			public void itemStateChanged( ItemEvent e )
			{
				if( dispatchPanel.getComponentCount() > 1 )
					dispatchPanel.remove( 1 );
				
				String item = ( String ) dispatchCombo.getSelectedItem();
				if( item.equalsIgnoreCase( "Script" ))
				{
					getModelItem().setDispatchStyle( DispatchStyleConfig.SCRIPT );
					dispatchPanel.add( groovyEditorPanel );
					groovyEditor.getEditArea().setText( getModelItem().getDispatchPath() );
					defaultResponseCombo.setEnabled( true );
				}
				else if( item.equalsIgnoreCase( "XPath" ))
				{
					getModelItem().setDispatchStyle( DispatchStyleConfig.XPATH );
					dispatchPanel.add( xpathEditorPanel );
					xpathEditor.getEditArea().setText( getModelItem().getDispatchPath() );
					defaultResponseCombo.setEnabled( true );
				}
				else if( item.equalsIgnoreCase( "Sequence" ))
				{
					getModelItem().setDispatchStyle( DispatchStyleConfig.SEQUENCE );
					defaultResponseCombo.setEnabled( false );
				}
				else if( item.equalsIgnoreCase( "Random" ))
				{
					getModelItem().setDispatchStyle( DispatchStyleConfig.RANDOM );
					defaultResponseCombo.setEnabled( false );
				}
				
				dispatchPanel.revalidate();
				dispatchPanel.repaint();
				
				Enum dispatchStyle = getModelItem().getDispatchStyle();
				if( dispatchInspector != null && dispatchStyle != null )
					dispatchInspector.setTitle( "Dispatch (" + dispatchStyle + ")" );
			}} );
		
		builder.addFixed( dispatchCombo );
		
		builder.addUnrelatedGap();
		builder.addFixed( new JLabel( "Default Response: ") );
		builder.addRelatedGap();
		
		ModelItemNames<MockResponse> names = new ModelItemNames<MockResponse>(getModelItem().getMockResponses());
		defaultResponseCombo = new JComboBox( new ExtendedComboBoxModel( names.getNames() ) );
		defaultResponseCombo.setPreferredSize( new Dimension( 150, 20 ) );
		defaultResponseCombo.addItemListener( new ItemListener() {

			public void itemStateChanged( ItemEvent e )
			{
				Object selectedItem = defaultResponseCombo.getSelectedItem();
				getModelItem().setDefaultResponse( ( String ) selectedItem );
			}} );
		
		builder.addFixed( defaultResponseCombo );
		builder.setBorder( BorderFactory.createEmptyBorder( 2, 3, 3, 3 ) );
		
		dispatchPanel.add( builder.getPanel(), BorderLayout.NORTH );
		
		// init data
		defaultResponseCombo.setSelectedItem( getModelItem().getDefaultResponse() );
		DispatchStyleConfig.Enum dispatchStyle = getModelItem().getDispatchStyle();
		if( dispatchStyle.equals( DispatchStyleConfig.SEQUENCE ))
		{
			dispatchCombo.setSelectedItem( "Sequence" );
			defaultResponseCombo.setEnabled( false );
		}
		else if( dispatchStyle.equals( DispatchStyleConfig.RANDOM ))
		{
			dispatchCombo.setSelectedItem( "Random" );
			defaultResponseCombo.setEnabled( false );
		}
		else if( dispatchStyle.equals( DispatchStyleConfig.SCRIPT ))
		{
			dispatchCombo.setSelectedItem( "Script" );
		}
		else if( dispatchStyle.equals( DispatchStyleConfig.XPATH ))
		{
			dispatchCombo.setSelectedItem( "XPath" );
		}
		
		return dispatchPanel;
	}

	private void buildXPathEditor()
	{
		xpathEditorPanel = new JPanel( new BorderLayout() );
		DispatchXPathGroovyEditorModel editorModel = new DispatchXPathGroovyEditorModel();
		xpathEditor = new GroovyEditor( editorModel );
		xpathEditorPanel.add( new JScrollPane( xpathEditor), BorderLayout.CENTER );
		xpathEditorPanel.add( buildXPathEditorToolbar( editorModel ), BorderLayout.PAGE_START );
	}
	
	public GroovyEditor getXPathEditor()
	{
		return xpathEditor;
	}

	protected JXToolBar buildXPathEditorToolbar( DispatchXPathGroovyEditorModel editorModel )
	{
		JXToolBar toolbar = UISupport.createToolbar();
		toolbar.addSpace( 3 );
		toolbar.addFixed( UISupport.createToolbarButton( editorModel.getRunAction() ));
		toolbar.addGlue();
		toolbar.addFixed( createActionButton( new ShowOnlineHelpAction(HelpUrls.MOCKOPERATION_XPATHDISPATCH_HELP_URL), true ) );
		return toolbar;
	}

	private void buildGroovyEditor()
	{
		groovyEditorPanel = new JPanel( new BorderLayout() );
		DispatchScriptGroovyEditorModel editorModel = new DispatchScriptGroovyEditorModel();
		groovyEditor = new GroovyEditor( editorModel );
		JScrollPane scrollPane = new JScrollPane( groovyEditor );
		groovyEditorPanel.add( scrollPane, BorderLayout.CENTER );
		UISupport.addPreviewCorner( scrollPane, true );
		groovyEditorPanel.add( buildGroovyEditorToolbar( editorModel ), BorderLayout.PAGE_START );
	}

	protected JXToolBar buildGroovyEditorToolbar( DispatchScriptGroovyEditorModel editorModel )
	{
		JXToolBar toolbar = UISupport.createToolbar();
		toolbar.addSpace( 3 );
		toolbar.addFixed( UISupport.createToolbarButton( editorModel.getRunAction() ));
		toolbar.addGlue();
		
		JLabel label = new JLabel("<html>Script is invoked with <code>log</code>, <code>context</code>, " +
				"<code>requestContext</code>, <code>mockRequest</code> and <code>mockOperation</code> variables</html>");
		label.setToolTipText( label.getText() );
		label.setMaximumSize( label.getPreferredSize() );
		
		toolbar.add( label);
		toolbar.addFixed( createActionButton( new ShowOnlineHelpAction(HelpUrls.MOCKOPERATION_SCRIPTDISPATCH_HELP_URL), true ) );
		return toolbar;
	}

	private Component buildToolbar()
	{
		JXToolBar toolbar = UISupport.createToolbar();
		toolbar.addSpace( 3 );
		
		toolbar.addFixed( UISupport.createToolbarButton( 
					SwingActionDelegate.createDelegate( NewMockResponseAction.SOAPUI_ACTION_ID, getModelItem(), null, 
							"/addToMockService.gif"	)));
		toolbar.addFixed( UISupport.createToolbarButton(
					SwingActionDelegate.createDelegate( OpenRequestForMockOperationAction.SOAPUI_ACTION_ID, getModelItem(), null, 
								"/open_request.gif") ));
		toolbar.addUnrelatedGap();
		
		ModelItemNames<Interface> names = new ModelItemNames<Interface>(getModelItem().getMockService().getProject().getInterfaceList());
		interfaceCombo = new JComboBox( names.getNames() );
		interfaceCombo.setSelectedIndex( -1 );
		interfaceCombo.addItemListener( new InterfaceComboListener() );
		
		toolbar.addLabeledFixed( "Interface", interfaceCombo );
		toolbar.addUnrelatedGap();
		operationCombo = new JComboBox( new ExtendedComboBoxModel() );
		operationCombo.setPreferredSize( new Dimension( 150, 20 ) );
		operationCombo.addItemListener( new OperationComboListener() );
		
		toolbar.addLabeledFixed( "Operation", operationCombo );
		
		WsdlOperation operation = getModelItem().getOperation();
		interfaceCombo.setSelectedItem( operation == null ? null : operation.getInterface().getName() );
		operationCombo.setSelectedItem( operation == null ? null : operation.getName() );
		
		toolbar.addGlue();
		toolbar.addFixed( createActionButton( new ShowOnlineHelpAction(HelpUrls.MOCKOPERATION_HELP_URL), true ) );
			
		return toolbar;
	}
	
	public boolean onClose( boolean canCancel )
	{
		if( currentInterface != null )
			currentInterface.removeInterfaceListener( interfaceListener );
		
		getModelItem().getMockService().getProject().removeProjectListener( projectListener );
		responseListModel.release();
		
		groovyEditor.release();
		xpathEditor.release();
		
		return release();
	}
	
	public boolean dependsOn(ModelItem modelItem)
	{
		return modelItem == getModelItem() || modelItem == getModelItem().getMockService()
				|| modelItem == getModelItem().getMockService().getProject();
	}
	
	private final class OperationComboListener implements ItemListener
	{
		public void itemStateChanged( ItemEvent e )
		{
			WsdlInterface iface = (WsdlInterface) getModelItem().getMockService().getProject().getInterfaceByName( interfaceCombo.getSelectedItem().toString() );
			WsdlOperation operation = iface.getOperationByName( operationCombo.getSelectedItem().toString() );
			getModelItem().setOperation( operation );
		}
	}

	private final class InterfaceComboListener implements ItemListener
	{
		public void itemStateChanged( ItemEvent e )
		{
			if( currentInterface != null )
			{
				currentInterface.removeInterfaceListener( interfaceListener );
			}
			
			Object selectedItem = interfaceCombo.getSelectedItem();
			if( selectedItem == null )
			{
				operationCombo.setModel( new ExtendedComboBoxModel() );
				currentInterface = null;
			}
			else
			{
				currentInterface = (WsdlInterface) getModelItem().getMockService().getProject().getInterfaceByName( selectedItem.toString() );
				ModelItemNames<Operation> names = new ModelItemNames<Operation>( currentInterface.getOperationList() );
				operationCombo.setModel( new ExtendedComboBoxModel( names.getNames()) );
				
				currentInterface.addInterfaceListener( interfaceListener );
			}
		}
	}

	private final class InternalProjectListener extends ProjectListenerAdapter
	{
		@Override
		public void interfaceAdded( Interface iface )
		{
			interfaceCombo.addItem( iface.getName() );
		}

		@Override
		public void interfaceRemoved( Interface iface )
		{
			if( interfaceCombo.getSelectedItem().equals( iface.getName() ))
			{
				getModelItem().setOperation( null );
			}
		}
	}

	private final class InternalInterfaceListener extends InterfaceListenerAdapter
	{
		@Override
		public void operationAdded( Operation operation )
		{
			operationCombo.addItem( operation.getName() );
		}

		@Override
		public void operationRemoved( Operation operation )
		{
			Object selectedItem = operationCombo.getSelectedItem();
			operationCombo.removeItem( operation.getName() );
			
			if( selectedItem.equals( operation.getName() ))
			{
				getModelItem().setOperation( null );
				interfaceCombo.setSelectedIndex( -1 );
			}
		}

		@Override
		public void operationUpdated( Operation operation )
		{
			ExtendedComboBoxModel model = ((ExtendedComboBoxModel)operationCombo.getModel());
			int ix = model.getIndexOf( operation.getName() );
			if( ix != -1 )
			{
				model.setElementAt( operation.getName(), ix );
			}
		}
	}

	public class DispatchScriptGroovyEditorModel implements GroovyEditorModel
	{
		private RunScriptAction runScriptAction = new RunScriptAction();
		
		public String[] getKeywords()
		{
			return new String[] { "mockRequest", "context", "requestContext", "log", "mockOperation" };
		}

		public Action getRunAction()
		{
			return runScriptAction;
		}

		public String getScript()
		{
			return getModelItem().getDispatchPath();
		}

		public Settings getSettings()
		{
			return getModelItem().getSettings();
		}

		public void setScript( String text )
		{
			getModelItem().setDispatchPath( text );
		}

		public String getScriptName()
		{
			return "Dispatch";
		}
	}
	
	public class DispatchXPathGroovyEditorModel implements GroovyEditorModel
	{
		private RunXPathAction runXPathAction = new RunXPathAction();

		public String[] getKeywords()
		{
			return new String[] { "define", "namespace"};
		}

		public Action getRunAction()
		{
			return runXPathAction;
		}

		public String getScript()
		{
			return getModelItem().getDispatchPath();
		}

		public Settings getSettings()
		{
			return getModelItem().getSettings();
		}

		public void setScript( String text )
		{
			getModelItem().setDispatchPath( text );
		}

		public String getScriptName()
		{
			return null;
		}
	}

	public class ResponseListModel extends AbstractListModel implements ListModel, MockServiceListener, PropertyChangeListener
	{
		private List<WsdlMockResponse> responses = new ArrayList<WsdlMockResponse>();
		
		public ResponseListModel()
		{
			for( int c = 0; c < getModelItem().getMockResponseCount(); c++ )
			{
				WsdlMockResponse mockResponse = ( WsdlMockResponse ) getModelItem().getMockResponseAt( c );
				mockResponse.addPropertyChangeListener( this );
				
				responses.add( mockResponse);
			}
			
			getModelItem().getMockService().addMockServiceListener( this );
		}
		
		public Object getElementAt( int arg0 )
		{
			return responses.get( arg0 );
		}

		public int getSize()
		{
			return responses.size();
		}

		public void mockOperationAdded( MockOperation operation )
		{
			
		}

		public void mockOperationRemoved( MockOperation operation )
		{
			
		}

		public void mockResponseAdded( MockResponse response )
		{
			if( response.getMockOperation() != getModelItem() )
				return;
			
			responses.add( ( WsdlMockResponse ) response );
			response.addPropertyChangeListener( this );
			fireIntervalAdded( this, responses.size()-1, responses.size()-1 );
			
			defaultResponseCombo.addItem( response.getName() );
		}

		public void mockResponseRemoved( MockResponse response )
		{
			if( response.getMockOperation() != getModelItem() )
				return;
			
			int ix = responses.indexOf( response );
			responses.remove( ix );
			response.removePropertyChangeListener( this );
			fireIntervalRemoved( this, ix, ix );
			
			defaultResponseCombo.removeItem( response.getName() );
		}

		public void propertyChange( PropertyChangeEvent arg0 )
		{
			if( arg0.getPropertyName().equals( WsdlMockOperation.NAME_PROPERTY ))
			{
				int ix = responses.indexOf( arg0.getSource() );
				fireContentsChanged( this, ix, ix );
				
				ExtendedComboBoxModel model = ( ExtendedComboBoxModel ) defaultResponseCombo.getModel();
				model.setElementAt( arg0.getNewValue(), ix );
				
				if( model.getSelectedItem().equals( arg0.getOldValue()  ))
						model.setSelectedItem( arg0.getNewValue() );
			}
		}
		
		public void release()
		{
			for( WsdlMockResponse operation : responses )
			{
				operation.removePropertyChangeListener( this );
			}
			
			getModelItem().getMockService().removeMockServiceListener( this );
		}
	}
	
	private final static class ResponseListCellRenderer extends JLabel implements ListCellRenderer
	{
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus)
		{
			MockResponse testStep = (MockResponse) value;
			setText(testStep.getName());
			setIcon(testStep.getIcon());

			if (isSelected)
			{
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			}
			else
			{
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}

			setEnabled(list.isEnabled());
			setFont(list.getFont());
			setOpaque(true);
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

			return this;
		}
	}
	
	private class RunScriptAction extends AbstractAction
	{
		public RunScriptAction()
		{
			putValue( Action.SMALL_ICON, UISupport.createImageIcon( "/run_groovy_script.gif" ));
			putValue( Action.SHORT_DESCRIPTION, "Runs this script using a mockRequest and context" );
		}
		
		public void actionPerformed( ActionEvent e )
		{
			WsdlMockResult lastMockResult = getModelItem().getLastMockResult();
			WsdlMockRequest mockRequest = lastMockResult == null ? null : lastMockResult.getMockRequest();

			try
			{
				Object retVal = getModelItem().evaluateDispatchScript( mockRequest );
				UISupport.showInfoMessage( "Script returned [" + retVal + "]" );
			}
			catch( Exception e1 )
			{
				UISupport.showErrorMessage( e1 );
			}
		}}
	
	private class RunXPathAction extends AbstractAction
	{
		public RunXPathAction()
		{
			putValue( Action.SMALL_ICON, UISupport.createImageIcon( "/run_groovy_script.gif" ));
			putValue( Action.SHORT_DESCRIPTION, "Evaluates this xpath expression against the latest request" );
		}
		
		public void actionPerformed( ActionEvent e )
		{
			WsdlMockResult lastMockResult = getModelItem().getLastMockResult();
			if( lastMockResult == null )
			{
				UISupport.showErrorMessage( "Missing last request to select from" );
				return;
			}
			
			try
			{
				XmlObject[] retVal = getModelItem().evaluateDispatchXPath( lastMockResult.getMockRequest() );
				StringList list = new StringList();
				for( XmlObject xmlObject : retVal )
				{
					list.add( XmlUtils.getNodeValue( xmlObject.getDomNode() ) );
				}
				
				UISupport.showInfoMessage( "XPath returned " + list.toString() );
			}
			catch( Exception e1 )
			{
				SoapUI.logError( e1 );
			}
		}}
}