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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import net.sf.taverna.t2.activities.dependencyactivity.AbstractAsynchronousDependencyActivity.FileExtFilter;
import net.sf.taverna.t2.visit.VisitReport;
import net.sf.taverna.t2.visit.VisitReport.Status;
import net.sf.taverna.t2.workflowmodel.Processor;
import net.sf.taverna.t2.workflowmodel.health.HealthCheck;
import net.sf.taverna.t2.workflowmodel.health.HealthChecker;
import bsh.ParseException;
import bsh.Parser;

public class ScriptActivityHealthChecker implements HealthChecker<ScriptActivity> {

	public boolean canVisit(Object subject) {
		return (subject!=null && subject instanceof ScriptActivity);
	}

	public VisitReport visit(ScriptActivity activity, List<Object> ancestors) {
		Object subject = (Processor) VisitReport.findAncestor(ancestors, Processor.class);
		if (subject == null) {
			// Fall back to using the activity itself as the subject of the reports
			subject = activity;
		}
		List<VisitReport> reports = new ArrayList<VisitReport>();
		
		String engineName = activity.getConfiguration().getEngineName();
		if (ScriptEngineUtils.getScriptEngine(engineName) == null) {
			VisitReport report = new VisitReport(HealthCheck.getInstance(), subject , "Unknown script language", HealthCheck.INVALID_SCRIPT, Status.SEVERE);
		    reports.add(report);
		}
		
		// Check if we can find all the Beanshell's dependencies
		LinkedHashSet<String> localDependencies = new LinkedHashSet<String>();
		localDependencies.addAll(activity.getConfiguration().getLocalDependencies());

		if (!localDependencies.isEmpty()) {
		String[] jarArray = ScriptActivity.libDir.list(new FileExtFilter(".jar"));
		if (jarArray != null) {
		    List<String> jarFiles = Arrays.asList(jarArray); // URLs of all jars found in the lib directory 
		    for (String jar : localDependencies) {
			if (jarFiles.contains(jar)){
			    localDependencies.remove(jar);
			}
		    }
		}
		if (localDependencies.isEmpty()){ // all dependencies found
			reports.add(new VisitReport(HealthCheck.getInstance(), subject, "Beanshell dependencies found", HealthCheck.NO_PROBLEM, Status.OK));
		}
		else{
			VisitReport vr = new VisitReport(HealthCheck.getInstance(), subject, "Beanshell dependencies missing", HealthCheck.MISSING_DEPENDENCY, Status.SEVERE);
			vr.setProperty("dependencies", localDependencies);
			vr.setProperty("directory", ScriptActivity.libDir);
			reports.add(vr);
		}
		
		}
		Status status = VisitReport.getWorstStatus(reports);
		VisitReport report = new VisitReport(HealthCheck.getInstance(), subject, "Beanshell report", HealthCheck.NO_PROBLEM,
				status, reports);

		return report;

	}

	public boolean isTimeConsuming() {
		return false;
	}


}
