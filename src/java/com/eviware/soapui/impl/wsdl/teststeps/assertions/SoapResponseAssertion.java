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

import com.eviware.soapui.config.RequestAssertionConfig;
import com.eviware.soapui.impl.wsdl.submit.WsdlMessageExchange;
import com.eviware.soapui.impl.wsdl.support.wsdl.WsdlContext;
import com.eviware.soapui.impl.wsdl.support.wsdl.WsdlValidator;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.model.testsuite.AssertionException;
import com.eviware.soapui.model.testsuite.RequestAssertion;
import com.eviware.soapui.model.testsuite.ResponseAssertion;

/**
 * Asserts that the specified message is a valid SOAP Message 
 * 
 * @author ole.matzura
 */

public class SoapResponseAssertion extends WsdlMessageAssertion implements ResponseAssertion, RequestAssertion
{
	public static final String ID = "SOAP Response";

	public SoapResponseAssertion( RequestAssertionConfig assertionConfig, Assertable assertable )
	{
		super( assertionConfig, assertable, false, false, false, true );
	}
	
	@Override
	protected String internalAssertResponse( WsdlMessageExchange messageExchange, SubmitContext context )
				throws AssertionException
	{
		WsdlContext wsdlContext = messageExchange.getOperation().getInterface().getWsdlContext();
		WsdlValidator validator = new WsdlValidator( wsdlContext );
		
		try
		{
			AssertionError[] errors = validator.assertResponse( messageExchange, true );
			if (errors.length > 0)
				throw new AssertionException(errors);
		}
		catch( AssertionException e )
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new AssertionException( new AssertionError( e.getMessage() ));
		}
		
		return "Response Envelope OK";
	}

	@Override
	protected String internalAssertRequest( WsdlMessageExchange messageExchange, SubmitContext context ) throws AssertionException
	{
		WsdlContext wsdlContext = messageExchange.getOperation().getInterface().getWsdlContext();
		WsdlValidator validator = new WsdlValidator( wsdlContext );
		
		try
		{
			AssertionError[] errors = validator.assertRequest( messageExchange, true );
			if (errors.length > 0)
				throw new AssertionException(errors);
		}
		catch( AssertionException e )
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new AssertionException( new AssertionError( e.getMessage() ));
		}
		
		return "Request Envelope OK";
	}
}
