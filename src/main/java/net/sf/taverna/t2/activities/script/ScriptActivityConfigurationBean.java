/*******************************************************************************
 * Copyright (C) 2007 The University of Manchester   
 * 
 *  Modifications to the initial code base are copyright of their
 *  respective authors, or their employers as appropriate.
 * 
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *    
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *    
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 ******************************************************************************/
package net.sf.taverna.t2.activities.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sf.taverna.t2.activities.dependencyactivity.DependencyActivityConfigurationBean;

/**
 * A configuration bean specific to a Beanshell activity; provides details
 * about the Beanshell script and its local and artifact dependencies.
 * 
 * @author Stuart Owen
 * @author David Withers
 * @author Alex Nenadic
 */
public class ScriptActivityConfigurationBean extends DependencyActivityConfigurationBean {
	
	private static final String DEFAULT_ENGINE_NAME = "velocity";
	
	private String engineName;

	private String script;
	
	private Map<String, ScriptEscapingType> inputEscaping;
	
	public ScriptActivityConfigurationBean() {
		super();
		this.script = "";
		this.engineName = DEFAULT_ENGINE_NAME;
		this.inputEscaping = new TreeMap<String, ScriptEscapingType>();
	}

	/**
	 * @return the Beanshell script
	 */
	public String getScript() {
		return script;
	}

	/**
	 * @param script the Beanshell script
	 */
	public void setScript(String script) {
		this.script = script;
	}
	
	///////////// From old code //////////
	@Deprecated
	private List<String> dependencies = new ArrayList<String>();

	/**
	 * Returns the dependencies.
	 *
	 * @return the dependencies
	 */
	public List<String> getDependencies() {
		return dependencies;
	}

	/**
	 * Sets the dependencies.
	 *
	 * @param dependencies the new dependencies
	 */
	public void setDependencies(List<String> dependencies) {
		this.dependencies = dependencies;
	}
	///////////// From old code //////////

	/**
	 * @return the engineName
	 */
	public final String getEngineName() {
		return engineName;
	}

	/**
	 * @param engineName the engineName to set
	 */
	public final void setEngineName(String engineName) {
		this.engineName = engineName;
	}

	public Map<String, ScriptEscapingType> getInputEscaping() {
		return inputEscaping;
	}

	public ScriptEscapingType getInputEscaping(String portName) {
		ScriptEscapingType result = inputEscaping.get(portName);
		if (result == null) {
			return ScriptEscapingType.TEXT;
		} else {
			return result;
		}
	}

	public void setInputEscaping(Map<String, ScriptEscapingType> inputEscaping) {
		this.inputEscaping = inputEscaping;
	}
	
}
