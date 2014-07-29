/**
 * 
 */
package net.sf.taverna.t2.activities.script;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.apache.log4j.Logger;

import net.sf.taverna.raven.appconfig.ApplicationRuntime;

/**
 * @author alson
 *
 */
public class ScriptEngineUtils {
	
	private static Logger logger = Logger.getLogger(ScriptEngineUtils.class);
	
	private static ScriptEngineManager sem = new ScriptEngineManager(getClassLoader());
	
	/**
	 * Probably should filter
	 * 
	 * @return
	 */
	public static List<ScriptEngineFactory> getApplicableFactories() {
		return sem.getEngineFactories();
	}
	
	private static URLClassLoader getClassLoader() {
		ApplicationRuntime.getInstance().getApplicationHomeDir();
		
		List<URL> urls = new ArrayList<URL>();
		
		File libDir = new File(ApplicationRuntime.getInstance().getApplicationHomeDir(), "lib");
		for (File f : libDir.listFiles()) {
			try {
				urls.add(f.toURL());
			} catch (MalformedURLException e) {
				logger.error(e);
			}
		}
		return new URLClassLoader(urls.toArray(new URL[]{}), Thread.currentThread().getContextClassLoader());
	}
	
	public static ScriptEngineFactory getApplicableFactory(final String engineName) {
		for (ScriptEngineFactory sef : getApplicableFactories()) {
			if (sef.getEngineName().equals(engineName)) {
				return sef;
			}
		}
		return null;
	}

	public static ScriptEngine getScriptEngine(final String engineName) {
		ScriptEngineFactory sef = getApplicableFactory(engineName);
		if (sef == null) {
			return null;
		}
		List<String> shortNames = sef.getNames();
		if (shortNames.isEmpty()) {
			return null;
		}
		return sem.getEngineByName(shortNames.get(0));
	}
}
