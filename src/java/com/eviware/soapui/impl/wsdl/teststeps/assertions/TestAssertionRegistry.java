/*
 *  soapUI, copyright (C) 2004-2011 eviware.com 
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.TestAssertionConfig;
import com.eviware.soapui.impl.wsdl.panels.assertions.AssertionCategoryMapping;
import com.eviware.soapui.impl.wsdl.panels.assertions.AssertionListEntry;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.basic.GroovyScriptAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.basic.ResponseSLAAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.basic.SchemaComplianceAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.basic.SimpleContainsAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.basic.SimpleNotContainsAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.basic.XPathContainsAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.basic.XQueryContainsAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.http.HttpDownloadAllResourcesAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.jdbc.JdbcStatusAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.jdbc.JdbcTimeoutAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.jms.JMSStatusAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.jms.JMSTimeoutAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.soap.NotSoapFaultAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.soap.SoapFaultAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.soap.SoapRequestAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.soap.SoapResponseAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.soap.WSARequestAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.soap.WSAResponseAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.soap.WSSStatusAssertion;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.TestAssertion;
import com.eviware.soapui.security.assertion.CrossSiteScriptAssertion;
import com.eviware.soapui.security.assertion.InvalidHttpStatusCodesAssertion;
import com.eviware.soapui.security.assertion.SensitiveInfoExposureAssertion;
import com.eviware.soapui.security.assertion.ValidHttpStatusCodesAssertion;
import com.eviware.soapui.support.types.StringToStringMap;

/**
 * Registry for WsdlAssertions
 * 
 * @author Ole.Matzura
 */

public class TestAssertionRegistry
{
	private static TestAssertionRegistry instance;
	private Map<String, TestAssertionFactory> availableAssertions = new HashMap<String, TestAssertionFactory>();
	private StringToStringMap assertionLabels = new StringToStringMap();
	private final static Logger log = Logger.getLogger( TestAssertionRegistry.class );

	private TestAssertionRegistry()
	{
		addAssertion( new SoapResponseAssertion.Factory() );
		addAssertion( new SoapRequestAssertion.Factory() );
		addAssertion( new SchemaComplianceAssertion.Factory() );
		addAssertion( new SimpleContainsAssertion.Factory() );
		addAssertion( new SimpleNotContainsAssertion.Factory() );
		addAssertion( new XPathContainsAssertion.Factory() );
		addAssertion( new NotSoapFaultAssertion.Factory() );
		addAssertion( new SoapFaultAssertion.Factory() );
		addAssertion( new ResponseSLAAssertion.Factory() );
		addAssertion( new GroovyScriptAssertion.Factory() );
		addAssertion( new XQueryContainsAssertion.Factory() );
		addAssertion( new WSSStatusAssertion.Factory() );
		addAssertion( new WSAResponseAssertion.Factory() );
		addAssertion( new WSARequestAssertion.Factory() );
		addAssertion( new JMSStatusAssertion.Factory() );
		addAssertion( new JMSTimeoutAssertion.Factory() );
		addAssertion( new JdbcStatusAssertion.Factory() );
		addAssertion( new JdbcTimeoutAssertion.Factory() );
		addAssertion( new HttpDownloadAllResourcesAssertion.Factory() );

		// security
		addAssertion( new ValidHttpStatusCodesAssertion.Factory() );
		addAssertion( new InvalidHttpStatusCodesAssertion.Factory() );
		addAssertion( new SensitiveInfoExposureAssertion.Factory() );
		addAssertion( new CrossSiteScriptAssertion.Factory() );

		for( TestAssertionFactory factory : SoapUI.getFactoryRegistry().getFactories( TestAssertionFactory.class ) )
		{
			addAssertion( factory );
		}
	}

	public void addAssertion( TestAssertionFactory factory )
	{
		availableAssertions.put( factory.getAssertionId(), factory );
		assertionLabels.put( factory.getAssertionLabel(), factory.getAssertionId() );
	}

	public static synchronized TestAssertionRegistry getInstance()
	{
		if( instance == null )
			instance = new TestAssertionRegistry();

		return instance;
	}

	public WsdlMessageAssertion buildAssertion( TestAssertionConfig config, Assertable assertable )
	{
		try
		{
			String type = config.getType();
			TestAssertionFactory factory = availableAssertions.get( type );
			if( factory == null )
			{
				log.error( "Missing assertion for type [" + type + "]" );
			}
			else
			{
				return ( WsdlMessageAssertion )factory.buildAssertion( config, assertable );
			}
		}
		catch( Exception e )
		{
			SoapUI.logError( e );
		}

		return null;
	}

	public Class<? extends WsdlMessageAssertion> getAssertionClassType( TestAssertionConfig config )
	{
		try
		{
			String type = config.getType();
			TestAssertionFactory factory = availableAssertions.get( type );
			if( factory == null )
			{
				log.error( "Missing assertion for type [" + type + "]" );
			}
			else
			{
				return factory.getAssertionClassType();
			}
		}
		catch( Exception e )
		{
			SoapUI.logError( e );
		}

		return null;
	}

	public boolean canBuildAssertion( TestAssertionConfig config )
	{
		return availableAssertions.containsKey( config.getType() );
	}

	public String getAssertionTypeForName( String name )
	{
		return assertionLabels.get( name );
	}

	public enum AssertableType
	{
		REQUEST, RESPONSE, BOTH
	}

	public AssertionListEntry getAssertionListEntry( String type )
	{
		TestAssertionFactory factory = availableAssertions.get( type );
		if( factory != null )
		{
			return factory.getAssertionListEntry();
		}
		else
		{
			return null;
		}
	}

	public boolean canAssert( String type, Assertable assertable )
	{
		TestAssertionFactory factory = availableAssertions.get( type );
		if( factory != null )
		{
			return factory.canAssert( assertable );
		}
		else
		{
			return false;
		}
	}

	public LinkedHashMap<String, LinkedHashSet<AssertionListEntry>> getCategoriesAssertionsMap( Assertable assertable )
	{
		LinkedHashMap<String, LinkedHashSet<AssertionListEntry>> categoryAssertionsMap = new LinkedHashMap<String, LinkedHashSet<AssertionListEntry>>();
		for( String category : AssertionCategoryMapping.getAssertionCategories() )
		{
			LinkedHashSet<AssertionListEntry> assertionCategorySet = new LinkedHashSet<AssertionListEntry>();
			categoryAssertionsMap.put( category, assertionCategorySet );
		}

		for( TestAssertionFactory assertion : availableAssertions.values() )
		{
			LinkedHashSet<AssertionListEntry> set;
			if( assertion.canAssert( assertable ) )
			{
				set = categoryAssertionsMap.get( assertion.getCategory() );
				set.add( assertion.getAssertionListEntry() );
				categoryAssertionsMap.put( assertion.getCategory(), set );

			}
		}

		return categoryAssertionsMap;
	}

	public String[] getAvailableAssertionNames( Assertable assertable )
	{
		List<String> result = new ArrayList<String>();

		for( TestAssertionFactory assertion : availableAssertions.values() )
		{
			if( assertion.canAssert( assertable ) )
				result.add( assertion.getAssertionLabel() );
		}

		return result.toArray( new String[result.size()] );
	}

	public String getAssertionNameForType( String type )
	{
		for( String assertion : assertionLabels.keySet() )
		{
			if( assertionLabels.get( assertion ).equals( type ) )
				return assertion;
		}

		return null;
	}

	public boolean canAddMultipleAssertions( String name, Assertable assertable )
	{
		for( int c = 0; c < assertable.getAssertionCount(); c++ )
		{
			TestAssertion assertion = assertable.getAssertionAt( c );
			if( assertion.isAllowMultiple() )
				continue;

			if( assertion.getClass().equals(
					availableAssertions.get( getAssertionTypeForName( name ) ).getAssertionClassType() ) )
			{
				return false;
			}
		}

		return true;
	}

	public boolean canAddAssertion( WsdlMessageAssertion assertion, Assertable assertable )
	{
		if( assertion.isAllowMultiple() )
			return true;

		for( int c = 0; c < assertable.getAssertionCount(); c++ )
		{
			if( assertion.getClass().equals( assertable.getAssertionAt( c ).getClass() ) )
			{
				return false;
			}
		}

		return true;
	}
}
