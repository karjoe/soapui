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

package com.eviware.soapui.impl.wsdl.mock;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpFields;

import com.eviware.soapui.impl.wsdl.panels.mockoperation.WsdlMockResultMessageExchange;
import com.eviware.soapui.impl.wsdl.teststeps.actions.ShowMessageExchangeAction;
import com.eviware.soapui.model.mock.MockResult;
import com.eviware.soapui.support.action.swing.ActionList;
import com.eviware.soapui.support.action.swing.DefaultActionList;
import com.eviware.soapui.support.types.StringToStringMap;

/**
 * The result of a handled WsdlMockRequest
 * 
 * @author ole.matzura
 */

public class WsdlMockResult implements MockResult
{
	private WsdlMockResponse mockResponse;
	private String responseContent;
	private long timeTaken;
	private long timestamp;
	private DefaultActionList actions;
	private StringToStringMap responseHeaders = new StringToStringMap();
	private WsdlMockRequest mockRequest;
	private HttpServletResponse response;
	private byte[] rawResponseData;
	private WsdlMockOperation mockOperation;

	public WsdlMockResult( WsdlMockRequest request, HttpServletResponse response ) throws Exception
	{
		this.response = response;
		timestamp = System.currentTimeMillis();
		mockRequest = request;
	}

	public WsdlMockRequest getMockRequest()
	{
		return mockRequest;
	}

	public ActionList getActions()
	{
		if( actions == null )
		{
			actions = new DefaultActionList( "MockResult" );
			actions.setDefaultAction( new ShowMessageExchangeAction( new WsdlMockResultMessageExchange( this, mockResponse ), "MockResult") );
		}
		
		return actions;
	}

	public WsdlMockResponse getMockResponse()
	{
		return mockResponse;
	}
	
	public String getResponseContent()
	{
		return responseContent;
	}
	
	public long getTimeTaken()
	{
		return timeTaken;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	public void setTimestamp( long timestamp )
	{
		this.timestamp = timestamp;
	}

	public void setTimeTaken( long timeTaken )
	{
		this.timeTaken = timeTaken;
	}
	
	public StringToStringMap getResponseHeaders()
	{
		return responseHeaders;
	}

	public void setMockResponse( WsdlMockResponse mockResponse )
	{
		this.mockResponse = mockResponse;
		mockRequest.getRequestContext().setMockResponse( mockResponse );
	}

	/**
	 * @deprecated
	 */
	
	public void setReponseContent( String responseContent )
	{
		this.responseContent = responseContent;
	}
	
	public void setResponseContent( String responseContent )
	{
		this.responseContent = responseContent;
	}
	
	@SuppressWarnings("unchecked")
	public void finish()
	{
		HttpFields httpFields = ((org.mortbay.jetty.Response)response).getHttpFields();
		
		Enumeration<String> e = httpFields.getFieldNames();
		while( e.hasMoreElements() )
		{
			String nextElement = e.nextElement();
			responseHeaders.put( nextElement, httpFields.getStringField( nextElement ) );
		}
		
		response = null;
	}

	public void addHeader( String name, String value )
	{
		if( response != null )
			response.addHeader( name, value );
		else
			responseHeaders.put( name, value );
	}

	public boolean isCommitted()
	{
		return response.isCommitted();
	}

	public void setContentType( String string )
	{
		response.setContentType( string );
	}

	public OutputStream getOutputStream() throws IOException
	{
		return response.getOutputStream();
	}

	public void initResponse()
	{
		response.setStatus( HttpServletResponse.SC_OK );
	}

	public boolean isDiscarded()
	{
		return false;
	}

	public Vector<?> getRequestWssResult()
	{
		return mockRequest.getWssResult();
	}

	public byte [] getRawResponseData()
	{
		return rawResponseData;
	}

	public void setRawResponseData( byte[] rawResponseData )
	{
		this.rawResponseData = rawResponseData;
	}

	public void writeRawResponseData( byte[] bs ) throws IOException
	{
		getOutputStream().write( bs );
		setRawResponseData( bs );
	}

	public void setMockOperation( WsdlMockOperation mockOperation )
	{
		this.mockOperation = mockOperation;
	}
	
	public WsdlMockOperation getMockOperation()
	{
		if( mockOperation != null )
			return mockOperation;
		
		return mockResponse == null ? null : mockResponse.getMockOperation();
	}
}
