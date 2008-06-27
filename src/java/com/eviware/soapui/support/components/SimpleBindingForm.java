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

package com.eviware.soapui.support.components;

import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.list.SelectionInList;

public class SimpleBindingForm extends SimpleForm
{
	private final PresentationModel<?> pm;

	public SimpleBindingForm( PresentationModel<?> pm )
	{
		this.pm = pm;
	}

	public JTextField appendTextField( String propertyName, String label, String tooltip )
	{
		JTextField textField = super.appendTextField( label, tooltip );
		Bindings.bind( textField, pm.getModel( propertyName ));
		return textField;
	}
	
	public JPasswordField appendPasswordField( String propertyName, String label, String tooltip )
	{
		JPasswordField textField = super.appendPasswordField( label, tooltip );
		Bindings.bind( textField, pm.getModel( propertyName ));
		return textField;
	}

	public JCheckBox appendCheckBox( String propertyName, String label, String tooltip )
	{
		JCheckBox checkBox = super.appendCheckBox( label, tooltip, false );
		Bindings.bind( checkBox, pm.getModel( propertyName ) );
		return checkBox;
	}

	public JComboBox appendComboBox( String propertyName, String label, Object[] values, String tooltip )
	{
		JComboBox comboBox = super.appendComboBox( label, values, tooltip );
		Bindings.bind( comboBox, new SelectionInList<Object>( values, pm.getModel( propertyName )) );
		return comboBox;
	}
	
	public JComboBox appendComboBox( String propertyName, String label, ComboBoxModel model, String tooltip )
	{
		JComboBox comboBox = super.appendComboBox( label, model, tooltip );
		Bindings.bind( comboBox, new SelectionInList<Object>( model, pm.getModel( propertyName )) );
		return comboBox;
	}
}