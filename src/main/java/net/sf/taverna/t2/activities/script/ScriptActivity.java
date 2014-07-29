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

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import net.sf.taverna.t2.activities.dependencyactivity.AbstractAsynchronousDependencyActivity;
import net.sf.taverna.t2.reference.ErrorDocumentService;
import net.sf.taverna.t2.reference.ExternalReferenceSPI;
import net.sf.taverna.t2.reference.ReferenceService;
import net.sf.taverna.t2.reference.ReferenceServiceException;
import net.sf.taverna.t2.reference.T2Reference;
import net.sf.taverna.t2.workflowmodel.OutputPort;
import net.sf.taverna.t2.workflowmodel.processor.activity.ActivityConfigurationException;
import net.sf.taverna.t2.workflowmodel.processor.activity.ActivityInputPort;
import net.sf.taverna.t2.workflowmodel.processor.activity.AsynchronousActivityCallback;
import net.sf.taverna.t2.workflowmodel.processor.activity.config.ActivityOutputPortDefinitionBean;

import org.apache.log4j.Logger;

/**
 * <p>
 * An Activity providing Beanshell functionality.
 * </p>
 * 
 * @author David Withers
 * @author Stuart Owen
 * @author Alex Nenadic
 */
public class ScriptActivity extends
	AbstractAsynchronousDependencyActivity<ScriptActivityConfigurationBean> {

	protected ScriptActivityConfigurationBean configurationBean;
	
    private static final String STDERR = "STDERR";

    private static final String STDOUT = "STDOUT";

    private static final String STDIN = "STDIN";

	private static Logger logger = Logger.getLogger(ScriptActivity.class);

	public ScriptActivity() {
	}

	@Override
	public void configure(ScriptActivityConfigurationBean configurationBean)
			throws ActivityConfigurationException {
		this.configurationBean = configurationBean;
		checkGranularDepths();
		configurePorts(configurationBean);
		if (configurationBean.isIncludedStdIn()) {
			List<Class<? extends ExternalReferenceSPI>> handledReferenceSchemes = new ArrayList<Class<? extends ExternalReferenceSPI>>();
			addInput(STDIN, 0, true, handledReferenceSchemes, String.class);
		}
		if (configurationBean.isIncludedStdOut()) {
			addOutput(STDOUT, 0);
		}
		if (configurationBean.isIncludedStdErr()) {
			addOutput(STDERR, 0);
		}
	}

	/**
	 * As the Beanshell activity currently only can output values at the
	 * specified depth, the granular depths should always be equal to the actual
	 * depth.
	 * <p>
	 * Workflow definitions created with Taverna 2.0b1 would not honour this and
	 * always set the granular depth to 0.
	 * <p>
	 * This method modifies the granular depths to be equal to the depths.
	 * 
	 */
	protected void checkGranularDepths() {
		for (ActivityOutputPortDefinitionBean outputPortDef : configurationBean
				.getOutputPortDefinitions()) {
			if (outputPortDef.getGranularDepth() != outputPortDef.getDepth()) {
				logger.warn("Replacing granular depth of port "
						+ outputPortDef.getName());
				outputPortDef.setGranularDepth(outputPortDef.getDepth());
			}
		}
	}

	@Override
	public ScriptActivityConfigurationBean getConfiguration() {
		return configurationBean;
	}
	
	public ActivityInputPort getInputPort(String name) {
		for (ActivityInputPort port : getInputPorts()) {
			if (port.getName().equals(name)) {
				return port;
			}
		}
		return null;
	}

	@Override
	public void executeAsynch(final Map<String, T2Reference> data,
			final AsynchronousActivityCallback callback) {
		callback.requestRun(new Runnable() {

			public void run() {
				
				ScriptEngine se = ScriptEngineUtils.getScriptEngine(configurationBean.getEngineName());
				if (se == null) {
					callback.fail("Unable to find script engine");
					return;
				}
					
					ReferenceService referenceService = callback.getContext().getReferenceService();
	
					Map<String, T2Reference> outputData = new HashMap<String, T2Reference>();
					
					ScriptContext context = se.getContext();
					Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
	
					Reader stdInReader = new StringReader("");
					StringWriter stdOutWriter = new StringWriter();
					StringWriter stdErrWriter = new StringWriter();
					try {
						// set inputs
						for (String inputName : data.keySet()) {

							ActivityInputPort inputPort = getInputPort(inputName);
							if (inputPort == null) {
								callback.fail("Unexpected data for port " + inputName);
								return;
							}
							Object input = referenceService.renderIdentifier(data
									.get(inputName), inputPort
									.getTranslatedElementClass(), callback
									.getContext());
							inputName = sanatisePortName(inputName);
							if (inputName.equals(STDIN)) {
								stdInReader = new StringReader((String) input);
							} else {
								ScriptEscapingType escaping = getConfiguration().getInputEscaping(inputName);
								bindings.put(inputName, escaping.parse(input));
							}
						}
						context.setReader(stdInReader);
						context.setErrorWriter(stdErrWriter);
						context.setWriter(stdOutWriter);
						// run
						se.eval(configurationBean.getScript(), context);
						// get outputs
						for (OutputPort outputPort : getOutputPorts()) {
							String name = outputPort.getName();
							Object value;
							if (name.equals(STDOUT)) {
								value = stdOutWriter.toString();
							} else if (name.equals(STDERR)) {
								value = stdErrWriter.toString();
							} else {
								value = bindings.get(name);
							}
							if (value == null) {
								ErrorDocumentService errorDocService = referenceService.getErrorDocumentService();
								value = errorDocService.registerError("No value produced for output variable " + name, 
										outputPort.getDepth(), callback.getContext());
							}
							outputData.put(name, referenceService.register(value,
									outputPort.getDepth(), true, callback
											.getContext()));
						}
						callback.receiveResult(outputData, new int[0]);
					} catch (ScriptException e) {
						logger.error(e);
						try {
							int lineNumber = e.getLineNumber();
						
							callback.fail("Line " + lineNumber+": "+ e.getMessage());
						}
						catch (NullPointerException e2) {
							callback.fail(e2.getMessage());
						}
					} catch (ReferenceServiceException e) {
						logger.error(e);
						callback.fail(
								"Error accessing input/output data for " + this);
					}
			}
			
			/**
			 * Removes any invalid characters from the port name.
			 * For example, xml-text would become xmltext.
			 * 
			 * 
			 * @param name
			 * @return
			 */
			private String sanatisePortName(String name) {
				String result=name;
				if (Pattern.matches("\\w++", name) == false) {
					result="";
					for (char c : name.toCharArray()) {
						if (Character.isLetterOrDigit(c) || c=='_') {
							result+=c;
						}
					}
				}
				return result;
			}
		});

	}

}
