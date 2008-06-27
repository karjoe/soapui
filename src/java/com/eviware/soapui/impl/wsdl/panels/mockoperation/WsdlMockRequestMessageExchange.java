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

import java.util.Vector;

import com.eviware.soapui.impl.wsdl.WsdlOperation;
import com.eviware.soapui.impl.wsdl.mock.WsdlMockOperation;
import com.eviware.soapui.impl.wsdl.mock.WsdlMockRequest;
import com.eviware.soapui.impl.wsdl.submit.AbstractWsdlMessageExchange;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.iface.Attachment;
import com.eviware.soapui.support.types.StringToStringMap;

public class WsdlMockRequestMessageExchange extends AbstractWsdlMessageExchange
{
	private final WsdlMockRequest request;
	private final WsdlMockOperation mockOperation;

	public WsdlMockRequestMessageExchange( WsdlMockRequest request, WsdlMockOperation mockOperation )
	{
		this.request = request;
		this.mockOperation = mockOperation;
	}
	
	@Override
	public WsdlOperation getOperation()
	{
		return mockOperation.getOperation();
	}

	public Vector<?> getRequestWssResult()
	{
		return null;
	}

	public Vector<?> getResponseWssResult()
	{
		return null;
	}

	public ModelItem getModelItem()
	{
		return mockOperation;
	}

	public Attachment[] getRequestAttachments()
	{
		return request.getRequestAttachments();
	}

	public String getRequestContent()
	{
		return request.getRequestContent();
	}

	public StringToStringMap getRequestHeaders()
	{
		return request.getRequestHeaders();
	}

	public Attachment[] getResponseAttachments()
	{
		return null;
	}

	public String getResponseContent()
	{
		return null;
	}

	public StringToStringMap getResponseHeaders()
	{
		return null;
	}

	public long getTimeTaken()
	{
		return 0;
	}

	public long getTimestamp()
	{
		return 0;
	}

	public boolean isDiscarded()
	{
		return false;
	}
}
