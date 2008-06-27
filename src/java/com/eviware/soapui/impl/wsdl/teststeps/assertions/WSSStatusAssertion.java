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

package com.eviware.soapui.impl.wsdl.teststeps.assertions;

import java.util.Vector;

import com.eviware.soapui.config.RequestAssertionConfig;
import com.eviware.soapui.impl.wsdl.submit.WsdlMessageExchange;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.model.testsuite.AssertionException;
import com.eviware.soapui.model.testsuite.RequestAssertion;
import com.eviware.soapui.model.testsuite.ResponseAssertion;

/**
 * Assertion for verifiying that WS-Security processing was ok
 * 
 * @author Ole.Matzura
 */

public class WSSStatusAssertion extends WsdlMessageAssertion implements ResponseAssertion, RequestAssertion
{
	public static final String ID = "WSS Status Assertion";

	/**
	 * Constructor for our assertion.
	 * 
	 * @param assertionConfig
	 * @param modelItem
	 */
	public WSSStatusAssertion( RequestAssertionConfig assertionConfig, Assertable modelItem )
	{
		super( assertionConfig, modelItem, false, false, false, true );
	}

	/**
	 * @see com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion#internalAssertRequest(com.eviware.soapui.impl.wsdl.submit.AbstractWsdlMessageExchange,
	 *      com.eviware.soapui.model.iface.SubmitContext)
	 */
	protected String internalAssertRequest( WsdlMessageExchange messageExchange, SubmitContext context )
				throws AssertionException
	{
		Vector<?> result = messageExchange.getRequestWssResult();
		
		if( result == null || result.isEmpty() )
			throw new AssertionException( new AssertionError( "Missing WS-Security results" ));
		
		for( int c = 0; c < result.size(); c++ )
		{
			if( result.get( c ) instanceof Exception )
			{
				throw new AssertionException( new AssertionError( "WS-Security validation failed: " + result.get( c )));
			}
		}
		
		return "WS-Security status OK";
	}

	/**
	 * @see com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion#internalAssertResponse(com.eviware.soapui.impl.wsdl.submit.AbstractWsdlMessageExchange,
	 *      com.eviware.soapui.model.iface.SubmitContext)
	 */
	protected String internalAssertResponse( WsdlMessageExchange messageExchange, SubmitContext context )
				throws AssertionException
	{
		Vector<?> result = messageExchange.getResponseWssResult();
		
		if( result == null || result.isEmpty() )
			throw new AssertionException( new AssertionError( "Missing WS-Security results" ));
		
		for( int c = 0; c < result.size(); c++ )
		{
			if( result.get( c ) instanceof Exception )
			{
				throw new AssertionException( new AssertionError( "WS-Security validation failed: " + result.get( c )));
			}
		}
		
		return "WS-Security status OK";
	}
}
