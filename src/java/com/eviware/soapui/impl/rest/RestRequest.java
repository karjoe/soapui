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

package com.eviware.soapui.impl.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.SchemaGlobalElement;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlString;

import com.eviware.soapui.config.RestRequestConfig;
import com.eviware.soapui.impl.rest.support.XmlBeansRestParamsTestPropertyHolder;
import com.eviware.soapui.impl.rest.support.XmlBeansRestParamsTestPropertyHolder.RestParamProperty;
import com.eviware.soapui.impl.support.AbstractHttpRequest;
import com.eviware.soapui.impl.wsdl.MutableTestPropertyHolder;
import com.eviware.soapui.impl.wsdl.HttpAttachmentPart;
import com.eviware.soapui.impl.wsdl.WsdlSubmit;
import com.eviware.soapui.impl.wsdl.submit.RequestTransportRegistry;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.iface.MessagePart;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.iface.MessagePart.ContentPart;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansion;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionUtils;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionsResult;
import com.eviware.soapui.model.testsuite.TestProperty;
import com.eviware.soapui.model.testsuite.TestPropertyListener;
import com.eviware.soapui.support.UISupport;

/**
 * Request implementation holding a SOAP request
 * 
 * @author Ole.Matzura
 */

public class RestRequest extends AbstractHttpRequest<RestRequestConfig> implements MutableTestPropertyHolder
{
	public final static Logger log = Logger.getLogger( RestRequest.class );
	public static final String DEFAULT_MEDIATYPE = "application/xml";
	public enum RequestMethod { GET, POST, PUT, DELETE, HEAD }
	
	private XmlBeansRestParamsTestPropertyHolder params;
	
   public RestRequest( RestResource resource, RestRequestConfig requestConfig )
   {
   	super( requestConfig, resource, null, false );
   	
   	if( requestConfig.getParameters() == null )
   		requestConfig.addNewParameters();
   	
   	if( !requestConfig.isSetMethod())
   		setMethod( RequestMethod.GET );

   	if( !requestConfig.isSetMediaType())
   		setMediaType( DEFAULT_MEDIATYPE );

   	
   	if( requestConfig.getParameters() == null )
   		requestConfig.addNewParameters();
   	
   	params = new XmlBeansRestParamsTestPropertyHolder( this, requestConfig.getParameters());
   }

	public MessagePart[] getRequestParts()
	{
		List<MessagePart> result = new ArrayList<MessagePart>();
		
		for( int c = 0; c < getPropertyCount(); c++ )
		{
			result.add( new ParameterMessagePart( getPropertyAt( c )));
		}
		
		if( getMethod() == RequestMethod.POST || getMethod() == RequestMethod.PUT )
		{
			result.add( new RestContentPart() );
		}
		
		return result.toArray( new MessagePart[result.size()] );
	}

	public MessagePart[] getResponseParts()
	{
		return new MessagePart[0];
	}

	public void setMethod( RequestMethod method )
	{
		RequestMethod old = getMethod();
		getConfig().setMethod(method.toString());
		notifyPropertyChanged("method", old, method);
	}
	
	public RequestMethod getMethod()
	{
		String method = getConfig().getMethod();
		return method == null ? null : RequestMethod.valueOf( method );
	}
	
	public void setMediaType( String mediaType )
	{
		String old = getMediaType();
		getConfig().setMediaType( mediaType );
		notifyPropertyChanged( "mediaType", old, mediaType );
	}
	
	public String getMediaType()
	{
		return getConfig().getMediaType();
	}

	public WsdlSubmit<RestRequest> submit(SubmitContext submitContext, boolean async) throws SubmitException
	{
      String endpoint = PropertyExpansionUtils.expandProperties( submitContext, getEndpoint());
		if( endpoint == null || endpoint.trim().length() == 0 )
      {
      	UISupport.showErrorMessage( "Missing endpoint for request [" + getName() + "]" );
      	return null;
      }
		
		try
		{
			WsdlSubmit<RestRequest> submitter = new WsdlSubmit<RestRequest>(this, getSubmitListeners(), 
					RequestTransportRegistry.getTransport(endpoint, submitContext));
			submitter.submitRequest(submitContext, async);
			return submitter;
		}
		catch( Exception e )
		{
			throw new SubmitException( e.toString() );
		}
	}

	public RestResponse getResponse()
	{
		return (RestResponse) super.getResponse();
	}
	
	public PropertyExpansion[] getPropertyExpansions()
	{
		PropertyExpansionsResult result = new PropertyExpansionsResult( this, this );
		result.addAll( super.getPropertyExpansions() );
		result.addAll( params.getPropertyExpansions());
		
		return result.toArray();
	}

	public RestParamProperty addProperty(String name)
	{
		return params.addProperty(name);
	}

	public void moveProperty(String propertyName, int targetIndex)
	{
		params.moveProperty(propertyName, targetIndex);
	}

	public RestParamProperty removeProperty(String propertyName)
	{
		return params.removeProperty(propertyName);
	}

	public boolean renameProperty(String name, String newName)
	{
		return params.renameProperty(name, newName);
	}

	public void addTestPropertyListener(TestPropertyListener listener)
	{
		params.addTestPropertyListener(listener);
	}

	public ModelItem getModelItem()
	{
		return this;
	}

	public Map<String, TestProperty> getProperties()
	{
		return params.getProperties();
	}

	public RestParamProperty getProperty(String name)
	{
		return params.getProperty(name);
	}

	public RestParamProperty getPropertyAt(int index)
	{
		return params.getPropertyAt(index);
	}

	public int getPropertyCount()
	{
		return params.getPropertyCount();
	}

	public String[] getPropertyNames()
	{
		return params.getPropertyNames();
	}

	public String getPropertyValue(String name)
	{
		return params.getPropertyValue(name);
	}

	public boolean hasProperty(String name)
	{
		return params.hasProperty(name);
	}

	public void removeTestPropertyListener(TestPropertyListener listener)
	{
		params.removeTestPropertyListener(listener);
	}

	public void setPropertyValue(String name, String value)
	{
		params.setPropertyValue(name, value);
	}
	
	public final static class ParameterMessagePart extends MessagePart.ParameterPart
	{
		private String name;

		public ParameterMessagePart(TestProperty propertyAt)
		{
			this.name = propertyAt.getName();
		}

		@Override
		public SchemaType getSchemaType()
		{
			return XmlString.type;
		}

		@Override
		public SchemaGlobalElement getPartElement()
		{
			return null;
		}

		@Override
		public QName getPartElementName()
		{
			return new QName( getName() );
		}

		public String getDescription()
		{
			return null;
		}

		public String getName()
		{
			return name;
		}
	}

	public String getPropertiesLabel()
	{
		return "Request Params";
	}

	public XmlBeansRestParamsTestPropertyHolder getParams()
	{
		return params;
	}

	public HttpAttachmentPart getAttachmentPart(String partName)
	{
		return null;
	}

	public HttpAttachmentPart[] getDefinedAttachmentParts()
	{
		return new HttpAttachmentPart[0];
	}
	
	public class RestContentPart extends ContentPart implements MessagePart
	{
		@Override
		public SchemaGlobalElement getPartElement()
		{
			return null;
		}

		@Override
		public QName getPartElementName()
		{
			return null;
		}

		@Override
		public SchemaType getSchemaType()
		{
			return null;
		}

		public String getDescription()
		{
			return null;
		}

		public String getName()
		{
			return null;
		}
		
		public String getMediaType()
		{
			return "application/xml";
		}
	}

	public boolean hasRequestBody()
	{
		RequestMethod method = getMethod();
		return method == RequestMethod.POST || method == RequestMethod.PUT;
	}
}
