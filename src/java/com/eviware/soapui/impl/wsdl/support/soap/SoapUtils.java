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

package com.eviware.soapui.impl.wsdl.support.soap;

import java.util.List;

import javax.wsdl.BindingOperation;
import javax.wsdl.Message;
import javax.wsdl.Part;
import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.wsdl.WsdlOperation;
import com.eviware.soapui.impl.wsdl.mock.DispatchException;
import com.eviware.soapui.impl.wsdl.support.wsdl.WsdlUtils;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.types.StringToStringMap;
import com.eviware.soapui.support.xml.XmlUtils;

/**
 * SOAP-related utility-methods..
 * 
 * @author ole.matzura
 */

public class SoapUtils
{
	public static boolean isSoapFault( String responseContent, SoapVersion soapVersion ) throws XmlException
	{
		if( StringUtils.isNullOrEmpty( responseContent ))
			return false;
		
		// check manually before resource intensive xpath
		if( responseContent.indexOf( ":Fault" ) > 0 || responseContent.indexOf( "<Fault" ) > 0 )
		{
		   XmlObject xml = XmlObject.Factory.parse( responseContent );
		   XmlObject[] paths = xml.selectPath( "declare namespace env='" + soapVersion.getEnvelopeNamespace() + "';" + 
		         "//env:Fault");
		   if( paths.length > 0 )
		      return true;
		}
		
		return false;
	}

	/**
	 * Init soapversion from content-type header.. should envelope be checked and/or override? 
	 */
	
	public static SoapVersion initSoapVersion( String contentType )
	{
		if( StringUtils.isNullOrEmpty( contentType ) )
			return null;
		
		SoapVersion soapVersion = null;
		
		soapVersion = contentType.startsWith( SoapVersion.Soap11.getContentType() ) ? SoapVersion.Soap11 : null;
		soapVersion = soapVersion == null && contentType.startsWith( SoapVersion.Soap12.getContentType() ) ? SoapVersion.Soap12 : soapVersion;
		if( soapVersion == null && contentType.startsWith( "application/xop+xml" ))
		{
			if( contentType.indexOf(  "type=\"" + SoapVersion.Soap11.getContentType() + "\"" ) > 0 )
				soapVersion = SoapVersion.Soap11;
			else if( contentType.indexOf(  "type=\"" + SoapVersion.Soap12.getContentType() + "\"" ) > 0 )
				soapVersion = SoapVersion.Soap12;
		}
	
		return soapVersion;
	}

	public static String getSoapAction( SoapVersion soapVersion, StringToStringMap headers )
	{
		String soapAction = null;
		String contentType = headers.get( "Content-Type" );
		
		if( soapVersion == SoapVersion.Soap11 )
		{
			soapAction = headers.get( "SOAPAction" );
		}
		else if( soapVersion == SoapVersion.Soap12 )
		{
			int ix = contentType.indexOf( "action=" );
			if( ix > 0 )
			{
				int endIx = contentType.indexOf( ';', ix );
				soapAction = endIx == -1 ? contentType.substring( ix + 7 ) : contentType.substring( ix + 7, endIx );
			}
		}
		
		soapAction = StringUtils.unquote( soapAction );
		
		return soapAction;
	}

	public static XmlObject getBodyElement(XmlObject messageObject, SoapVersion soapVersion ) throws XmlException
	{
		XmlObject[] envelope = messageObject.selectChildren( soapVersion.getEnvelopeQName() );
		if( envelope.length != 1 )
		    throw new XmlException( "Missing/Invalid SOAP Envelope, expecting [" + soapVersion.getEnvelopeQName() + "]" );
		
		XmlObject[] body = envelope[0].selectChildren( soapVersion.getBodyQName() );
		if( body.length != 1 )
		    throw new XmlException( "Missing/Invalid SOAP Body, expecting [" + soapVersion.getBodyQName() + "]" );
		
		return body[0];
	}
	
	public static XmlObject getContentElement( XmlObject messageObject, SoapVersion soapVersion ) throws XmlException
	{
		XmlObject bodyElement = SoapUtils.getBodyElement(messageObject, soapVersion);
		if( bodyElement != null )
		{
			XmlCursor cursor = bodyElement.newCursor();
			
			try
			{
				if( cursor.toFirstChild() )
				{
					while( !cursor.isContainer() )
						cursor.toNextSibling();

					if( cursor.isContainer() )
					{
						return cursor.getObject();
					}
				}
			}
			catch( Exception e )
			{
				SoapUI.logError( e );
			}
			finally
			{
				cursor.dispose();
			}
		}
		
		return null;
	}

	@SuppressWarnings("unchecked")
	public static WsdlOperation findOperationForRequest( SoapVersion soapVersion, String soapAction, XmlObject requestContent, 
				List<WsdlOperation> operations, boolean requireSoapVersionMatch, boolean requireSoapActionMatch ) throws XmlException, DispatchException, Exception
	{
		XmlObject contentElm = getContentElement( requestContent, soapVersion );
		if( contentElm == null )
			throw new DispatchException( "Missing content element in body" );
	
		QName contentQName = XmlUtils.getQName( contentElm.getDomNode() );
		NodeList contentChildNodes = null;
	
		for( int c = 0; c < operations.size(); c++ )
		{
			WsdlOperation wsdlOperation = operations.get( c );
			String action = wsdlOperation.getAction();

			// matches soapAction?
			if( !requireSoapActionMatch || (
						( soapAction == null && wsdlOperation.getAction() == null )	|| 
						( action != null && action.equals( soapAction )) 
						))
			{
				QName qname = wsdlOperation.getRequestBodyElementQName();

				if( !contentQName.equals( qname ) )
					continue;
				
				SoapVersion ifaceSoapVersion = wsdlOperation.getInterface().getSoapVersion();
				
				if( requireSoapVersionMatch && ifaceSoapVersion != soapVersion ) 
				{
					continue;
				}

				// check content
				if( wsdlOperation.getStyle().equals( WsdlOperation.STYLE_DOCUMENT ) )
				{
					// matches!
					return wsdlOperation;
				}
				else if( wsdlOperation.getStyle().equals( WsdlOperation.STYLE_RPC ) )
				{
					BindingOperation bindingOperation = wsdlOperation.getBindingOperation();
					Message message = bindingOperation.getOperation().getInput().getMessage();
					List<Part> parts = message.getOrderedParts( null );

					if( contentChildNodes == null )
						contentChildNodes = XmlUtils.getChildElements( ( Element ) contentElm.getDomNode() );

					int i = 0;

					if( parts.size() > 0 )
					{
						for( int x = 0; x < parts.size(); x++ )
						{
							if( WsdlUtils.isAttachmentInputPart( parts.get( x ), bindingOperation ) ||
								 WsdlUtils.isHeaderInputPart( parts.get( x ), message, bindingOperation ))
							{
								parts.remove( x );
								x--;
							}
						}

						for( ; i < contentChildNodes.getLength() && !parts.isEmpty(); i++ )
						{
							Node item = contentChildNodes.item( i );
							if( item.getNodeType() != Node.ELEMENT_NODE )
								continue;

							int j = 0;
							while( ( j < parts.size() ) && ( !item.getNodeName().equals( parts.get( j ).getName() ) ) )
							{
								j++;
							}

							if( j == parts.size() )
								break;

							parts.remove( j );
						}
					}

					// match?
					if( i == contentChildNodes.getLength() && parts.isEmpty() )
					{
						return wsdlOperation;
					}
				}
			}
		}
	
		throw new DispatchException( "Missing operation for soapAction [" + soapAction + "] and body element ["
					+ contentQName + "] with SOAP Version [" + soapVersion + "]" );
	}
	
	public static String removeEmptySoapHeaders( String content, SoapVersion soapVersion ) throws XmlException
	{
		XmlObject xmlObject = XmlObject.Factory.parse( content );
		XmlObject[] selectPath = xmlObject.selectPath( "declare namespace soap='" + soapVersion.getEnvelopeNamespace() + "';/soap:Envelope/soap:Header" );
		if( selectPath.length > 0 )
		{
			Node domNode = selectPath[0].getDomNode();
			if( !domNode.hasChildNodes() && !domNode.hasAttributes())
			{
				domNode.getParentNode().removeChild( domNode );
				return xmlObject.xmlText();
			}
		}
		
		return content;
	}
}
