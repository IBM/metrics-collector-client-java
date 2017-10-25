/*-------------------------------------------------------------------------------
 Copyright IBM Corp. 2015

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-------------------------------------------------------------------------------*/
package com.ibm.bluemix.deploymenttracker.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Map;
import org.yaml.snakeyaml.Yaml;

import com.ibm.json.java.JSON;
import com.ibm.json.java.JSONObject;
import com.ibm.json.java.JSONArray;

/**
 * Simple wrapper class for the Cloud Foundry Java application tracker client. 
 * This servlet is launched automatically by the container.
 * Basic information about the tracker and its tracking payload can be retrieved 
 * by sending a GET request to /comibmbluemix/AppTracker
 *
 */
@WebServlet(urlPatterns={"/comibmbluemix/AppTracker"},loadOnStartup=128)
public class CFJavaTrackerServlet extends HttpServlet {

	private static final long serialVersionUID = -5251459961638725024L;

	/**
	 * Called by the servlet container to indicate to a servlet that the servlet is being placed into service. During servlet initialization
	 * a CF tracking request is submitted.
	 */
	public void init() throws ServletException {
		 
		JSONObject configFile = null;
		InputStream configFileinwar = null;
		
		try {
			 // retrieve tracking data that might be optionally packaged with the sample application
			 // getResourceAsStream returns null if resource is not found 
			 configFileinwar = getServletContext().getResourceAsStream("/META-INF/repository.yaml");
			 if(configFileinwar != null){
		 	    Yaml yaml= new Yaml();
				Map<String,Object> map = (Map<String, Object>) yaml.load(configFileinwar);
			 	configFile = new JSONObject();
			 	Iterator it = map.entrySet().iterator();
				while (it.hasNext()) {
				    Map.Entry pairs = (Map.Entry)it.next();
				    if(pairs.getValue() instanceof String){
   					 configFile.put(pairs.getKey(), pairs.getValue());
					}else{
						JSONArray temp = new JSONArray();
						ArrayList<String> array = (ArrayList <String>) pairs.getValue();
						for(int i = 0; i < array.size(); i++){
							temp.add(array.get(i));
						}
				    	configFile.put(pairs.getKey(), temp);
					}
				}
			 }
		}
		catch(Exception ex) {
			// ignore errors 
			configFile = null;
		}
		finally {
			try {
			    // cleanup                                                                                                                                                                                 
				if(configFileinwar != null)
					configFileinwar.close();
				configFileinwar = null;
			}
			catch(Exception ex) {
				// ignore
			}
		}
		
		 try {
			new CFJavaTrackerClient().track(configFile);		        
		 }
		 catch(Exception ex) {
			System.err.println("[Metrics-Collector-Client-Java] An error occurred while trying to track application: " + ex.getClass().getName() + ":" + ex.getMessage());
			ex.printStackTrace(System.err);
		 }
		 
	 } // end method init
	
	
	/**
     * Displays the tracker client status page at /comibmbluemix/AppTracker 
     * @param req  the request to be processed 
     * @param resp the request response
     * @throws ServletException if the request for the GET could not be handled
     * @throws IOException if an input or output error is detected when the servlet handles the GET request 
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
			throws ServletException, IOException {
		
		CFJavaTrackerClient cfjtc = new CFJavaTrackerClient();
		
		try {
			resp.setContentType("text/html");
		
			PrintWriter out = resp.getWriter();
			out.println("<html><head><title> Cloud Foundry application tracker client </title>");
			out.println("<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.4/css/bootstrap.min.css\">");
			out.println("<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.4/css/bootstrap-theme.min.css\">");
			out.println("</head><body>");
			out.println("<div class=\"container\">");
			out.println("<h2>Tracker information</h2>");
			out.println("<table class=\"table-condensed\">");
			out.println("<tr><th>Tracker name</th><td>" + this.getClass().getCanonicalName() + "</td></tr>");
			out.println("<tr><th>Tracker version</th><td>" + cfjtc.getVersion() + "</td></tr>");
			out.println("<tr><th>Tracker source code URL</th><td><a href=\"" + cfjtc.getClientSourceURL() + "\" target=\"_blank\">" + cfjtc.getClientSourceURL() +"</a></td></tr>");
			out.println("</table>");
			out.println("</div>");

			out.println("<div class=\"container\">");
			out.println("<h2>Tracking information</h2>");
			out.println("<table class=\"table-condensed\">");
			
			URL url = null; 
			Iterator<TrackingRequest> it = cfjtc.getTrackingRequests().iterator();
			TrackingRequest tr = null;
			while(it.hasNext()) {
				tr = it.next();
				url = new URL(tr.getTrackingURL());
				if(url.getPort() == -1)
				    out.println("<tr><th>Tracker instance URL</th><td><a href=\"" + url.getProtocol() + "://" + url.getHost() + "\" target=\"_blank\">" + tr.getTrackingURL() +"</a></td></tr>");
				else
			        out.println("<tr><th>Tracker instrance URL</th><td><a href=\"" + url.getProtocol() + "://" + url.getHost() + ":" + url.getPort() + "\" target=\"_blank\">" + tr.getTrackingURL() +"</a></td></tr>");
				out.println("<tr>");
				out.println("<th>Application name</th><td>" + tr.getRequestingAppName() + "</td>");
				out.println("</tr>");
				out.println("<tr>");
				out.println("<th>Application id</th><td>" + tr.getRequestingAppGUID() + "</td>");
				out.println("</tr>");
				out.println("<tr>");
				out.println("<th>Application instance index</th><td>" + tr.getRequestingAppInstanceIndex() + "</td>");
				out.println("</tr>");				
				out.println("<th>Application version</th><td>" + tr.getRequestingAppVersion() + "</td>");
				out.println("</tr>");
				out.println("<th>Tracking date</th><td>" + tr.getRequestDate() + "</td>");
				out.println("</tr>");
				out.println("<th>Tracking status</th><td>" + tr.getRequestStatus() + "</td>");
				out.println("</tr>");
			}			
			
			out.println("</table>");
			out.println("</div>");
			out.println("</body></html>");
			out.close();
		}
		catch(Exception ex) {
			System.err.println("[Metrics-Collector-Client-Java] An error occurred in doGet: " + ex.getClass().getName() + ":" + ex.getMessage());
			ex.printStackTrace(System.err);
		}
				
	} // end method doGet
	 
} //end class CFJavaTrackerServlet
