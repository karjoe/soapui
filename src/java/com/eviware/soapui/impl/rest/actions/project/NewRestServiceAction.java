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

package com.eviware.soapui.impl.rest.actions.project;

import java.io.File;

import org.apache.log4j.Logger;

import com.eviware.soapui.impl.rest.RestService;
import com.eviware.soapui.impl.rest.RestServiceFactory;
import com.eviware.soapui.impl.rest.support.RestUtils;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.support.HelpUrls;
import com.eviware.soapui.support.MessageSupport;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.support.AbstractSoapUIAction;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.support.ADialogBuilder;
import com.eviware.x.form.support.AField;
import com.eviware.x.form.support.AForm;
import com.eviware.x.form.support.AField.AFieldType;
import com.eviware.x.form.validators.RequiredValidator;

/**
 * Actions for importing an existing soapUI project file into the current workspace
 * 
 * @author Ole.Matzura
 */

public class NewRestServiceAction extends AbstractSoapUIAction<WsdlProject>
{
	public static final String SOAPUI_ACTION_ID = "NewRestServiceAction"; 
	public static final MessageSupport messages = MessageSupport.getMessages( NewRestServiceAction.class );
	private XFormDialog dialog;
	private final static Logger log = Logger.getLogger( NewRestServiceAction.class );

	public NewRestServiceAction()
   {
      super( messages.get( "title"), messages.get( "description") ); 
   }

	public void perform( WsdlProject project, Object param )
	{
		if( dialog == null )
   	{
			dialog = ADialogBuilder.buildDialog( Form.class );
			dialog.getFormField( Form.SERVICENAME ).addFormFieldValidator( new RequiredValidator( "Service Name is required") );
   	}
   	else 
   	{
   		dialog.setValue( Form.SERVICENAME, "" ); 
   		dialog.setValue( Form.SERVICEENDPOINT, "" ); 
   		dialog.setValue( Form.WADLURL, "" );
   	}
   	
   	if( dialog.show() )
   	{
   		RestService restService = (RestService) project.addNewInterface( dialog.getValue(Form.SERVICENAME), RestServiceFactory.REST_TYPE );
   		restService.setBasePath( dialog.getValue(Form.SERVICEENDPOINT));
   		
   		String wadl = dialog.getValue(Form.WADLURL);
   		if( StringUtils.hasContent(wadl))
   		{
   			try
				{
					File f = new File( wadl );
					if( f.exists())
						RestUtils.initFromWadl( restService, f.toURI().toURL().toString());
					else
						RestUtils.initFromWadl( restService, wadl );
				}
				catch (Exception e)
				{
					log.error(e.toString());
				}
   		}
   	}
   }
	
   @AForm( name="Form.Title", description = "Form.Description", helpUrl=HelpUrls.NEWRESTSERVICE_HELP_URL, icon=UISupport.TOOL_ICON_PATH)
	public interface Form 
	{
		@AField( description = "Form.ServiceName.Description", type = AFieldType.STRING ) 
		public final static String SERVICENAME = messages.get("Form.ServiceName.Label"); 
		
		@AField(description = "Form.ServiceUrl.Description", type = AFieldType.STRING ) 
		public final static String SERVICEENDPOINT = messages.get("Form.ServiceUrl.Label"); 

		@AField(description = "Form.WadlUrl.Description", type = AFieldType.FILE ) 
		public final static String WADLURL = messages.get("Form.WadlUrl.Label"); 

	}
}