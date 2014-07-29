/**
 * 
 */
package net.sf.taverna.t2.activities.script;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import net.minidev.json.JSONValue;

/**
 * 
 * Adapted from JSONActivityPortType by Mark Borkum
 * 
 * @author alanrw
 *
 */
public enum ScriptEscapingType {

	
	JSON("JSON", Collections.unmodifiableList(Arrays.asList("application/json"))),
	TEXT("Text", Collections.unmodifiableList(Arrays.asList("text/plain"))),
	XML("XML", Collections.unmodifiableList(Arrays.asList("application/xml")))
	;
	
	private final List<String> mimeTypes;
	
	private final String name;

	/**
	 * Sole constructor.
	 * 
	 * @param name  The name of this port type.
	 */
	private ScriptEscapingType(final String name, final List<String> mimeTypes) {
		this.name = name;
		this.mimeTypes = mimeTypes;
	}
	
	/**
	 * The list of acceptable MIME types for this port type. 
	 * 
	 * @return  The list of acceptable MIME types for this port type. 
	 */
	public List<String> getMimeTypes() {
		return mimeTypes;
	}
	
	/**
	 * The name of this port type.
	 * 
	 * @return  The name of this port type.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Parse the specified input as plain text, JSON or XML. 
	 * 
	 * @param value  The input.
	 * @return  The input as plain text, JSON or XML.
	 */
	public Object parse(final Object value) {
		if (value == null) {
			return null;
		}
		if (this.equals(TEXT)) {
			return value;
		}
		if (value instanceof List) {
			List<Object> l = (List<Object>) value;
			List<Object> result = new ArrayList<Object>();
			for (Object o : l) {
				result.add(this.parse(o));
			}
			return result;
		}
		if (this.equals(JSON)) {
			String s = value.toString();
			return JSONValue.toJSONString(s);
		}
		if (this.equals(XML)) {
			String s = value.toString();
			return (StringEscapeUtils.escapeXml(s));
		}
		return value;
	}

	@Override
	public String toString() {
		return this.getName();
	}
	
}
