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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimeZone;

import com.ibm.json.java.JSON;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

/**
 * Use this simple class to track how many times a Java based VCAP_APPLICATION was started.
 * Only one attempt to track is made. The class logs output to the STDOUT stream and STDERR stream 
 * (if a problem occurred). 
 *
 */
public class CFJavaTrackerClient {

	// client tracker source code download location (informational only)
	private static final String TRACKER_SRC_URL = "https://github.com/IBM-Bluemix/cf-deployment-tracker-client-java";

	// client tracker version
	private static final String CLIENT_VERSION = "0.0.4";
	
	// default URL to which tracking requests are sent
	private static final String DEFAULT_TRACKER_URL = "http://deployment-tracker.mybluemix.net/api/v1/track";
	
	// tracking request property names
	public static final String KEYWORD_APPLICATION_NAME = "application_name";		// defined in VCAP_APPLICATION env property
	public static final String KEYWORD_APPLICATION_VERSION = "application_version"; // defined in VCAP_APPLICATION env property 
	public static final String KEYWORD_APPLICATION_URIS = "application_uris";       // defined in VCAP_APPLICATION env property
	public static final String KEYWORD_SPACE_ID = "space_id";                       // defined in VCAP_APPLICATION env property
	public static final String KEYWORD_REPOSITORY_URL = "repository_url";           // defined by deployment tracker service
	public static final String KEYWORD_CODE_VERSION = "code_version";               // defined by deployment tracker service
	public static final String KEYWORD_REQUEST_DATE = "date_sent";					// defined by deployment tracker service
	public static final String KEYWORD_DEPLOYMENT_TRACKER = "DEPLOYMENT_TRACKER";   // defined by deployment tracker client
	public static final String KEYWORD_CUSTOM_TRACKER_URL = "custom_tracker_url";	// defined by deployment tracker client
		
	// key is generated by TrackingRequest.getTrackingKey()
	private static HashMap<String,TrackingRequest> trackingHistory = new HashMap<String,TrackingRequest>();
	
	public CFJavaTrackerClient() {
		
	} // constructor

	/**
	 * Submits a tracking request.
	 */
	public void track() {
		track(null);
	} // method track
		
	/**
	 * Submits a tracking request to the service that's defined by customurl.
	 * @param customurl the URL of a tracking service to which the request is to be sent. customurl is ignored if the value is null or an empty string
	 */
	public void track(String customurl) {
		TrackingRequest tr =  createTrackingRequest();
		// override tracker URL
		tr.setTrackingURL(customurl);		
		processTrackingRequest(tr);
	} // method track(String)
		
	/**
	 * Process the specified tracking request. If trackingrequest is null, this method does nothing.
	 * @param trackingrequest The request to be processed.
	 */
	private void processTrackingRequest(TrackingRequest trackingrequest) {
		
		if(trackingrequest == null)
		 return; // nothing to do
		
			HttpURLConnection con = null;
			URL url = null;
			DataOutputStream dos = null;
			
			try {
				
				if(! trackingHistory.containsKey(trackingrequest.getTrackingKey())) {
					
					// mark application as tracked (default response code 0 = not sent)
					trackingHistory.put(trackingrequest.getTrackingKey(),trackingrequest);
					
					// register the calling application with deployment tracker 			  
					url = new URL(trackingrequest.getTrackingURL());
										
					con = (HttpURLConnection) url.openConnection();
					// prevent infinite wait (where supported) if the tracker service isn't running or taking too much time to respond
					con.setConnectTimeout(2000);
					con.setReadTimeout(2000);
					// set request properties
					con.setRequestMethod("POST");
					con.setRequestProperty("Content-Type", "application/json");
					con.setDoOutput(true);
					// send tracking information
					dos = new DataOutputStream(con.getOutputStream());
					dos.writeBytes(trackingrequest.getRequestData().serialize());	
					dos.flush();
					dos.close();
					dos = null;
	
					// document request status
					trackingrequest.setRequestStatus(con.getResponseCode());
					trackingHistory.put(trackingrequest.getTrackingKey(),trackingrequest);
					
					// debug only
					System.out.println("[JavaAppTracker] Tracking request for application " + trackingrequest.getRequestingAppName() + " returned: " + con.getResponseCode() + " " + con.getResponseMessage());
								
					// display request and response details if return code does not indicate success 
					//   1xx: Informational
				    //   2xx: Success
				    //   3xx: Redirection
				    //   4xx: Client Error
				    //   5xx: Server Error 
					if(con.getResponseCode() >= 300) {
						
						System.err.println("[JavaAppTracker] Tracking request for application " + trackingrequest.getRequestingAppName() + ": " + trackingrequest.getRequestData().serialize());
						
						BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
	
						if (in != null) {
							String responseLine = null;
							while ((responseLine = in.readLine()) != null) {
								System.err.println(responseLine);	
							}
							in.close();
							in = null;
						}
					}
					
					
			  } // if (! trackingHistory ...
			}
			// catch all
			catch(Exception ex) {
				System.err.println("[JavaAppTracker] An error occurred while trying to track application " + trackingrequest.getRequestingAppName() + ": "+ ex.getClass().getName() + ":" + ex.getMessage());
				ex.printStackTrace(System.err);
			}
			finally {
				
				// cleanup
				if(dos != null) {
					try {
					dos.close();
					}
					catch(Exception ex){
						// ignore
					}
					dos = null;
				}
				
				if(con != null) {
					con.disconnect();
					con = null;
				}
				url = null;
			} // finally			
		
	} // end method
						
	/**
	 * Returns a list of recent tracking requests
	 * @return list of tracking requests, never null
	 */
	public Collection<TrackingRequest> getTrackingRequests() {
		return trackingHistory.values();
	} // end method
	
	/**
	 * Returns the CF client tracker version
	 * @return version string
	 */
	public String getVersion() {
		return CLIENT_VERSION;
	} // end method getVersion

	/**
	 * Returns the URL where the client tracker source code can be downloaded or null if not published
	 * @return
	 */
	public String getClientSourceURL() {
		return TRACKER_SRC_URL;
	} // end method getClientSourceURL
	
	/**
	 * Creates a tracking request.
	 * @return a complete tracking request or null in case of a problem
	 */
	private TrackingRequest createTrackingRequest() {
		
		TrackingRequest trackingrequest = null;
		
		// 'VCAP_APPLICATION' is in JSON format, it contains useful information about a deployed application
		String envApp = System.getenv("VCAP_APPLICATION");
			
		// Only proceed if we have access to the metadata we want to track
		if(envApp != null) {
			
			JSONObject payload = null;
			String customtrackingurl = null; 
			
			try {				
				payload = new JSONObject();
				JSONObject obj	= (JSONObject)JSON.parse(envApp);			
				SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'"); // ISO 8601 (extended format)
				dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT")); // not strictly needed but added for clarity
				payload.put(KEYWORD_REQUEST_DATE,dateFormatGmt.format(new Date()));					
				// VCAP_APPLICATION properties
				payload.put(KEYWORD_APPLICATION_NAME, (String) obj.get(KEYWORD_APPLICATION_NAME));			// singleton per VCAP_APPLICATION definition
				payload.put(KEYWORD_APPLICATION_VERSION, (String) obj.get(KEYWORD_APPLICATION_VERSION));    // singleton per VCAP_APPLICATION definition
				payload.put(KEYWORD_APPLICATION_URIS, (JSONArray) obj.get(KEYWORD_APPLICATION_URIS));       // array per VCAP_APPLICATION definition
				payload.put(KEYWORD_SPACE_ID, (String) obj.get(KEYWORD_SPACE_ID));                          // singleton per VCAP_APPLICATION definition
								
				// process optional payload that might have been provided by the DEPLOYMENT_TRACKER environment variable 
				envApp = System.getenv(KEYWORD_DEPLOYMENT_TRACKER);
				if(envApp != null) {
					obj	= (JSONObject)JSON.parse(envApp);
					// special processing needed because input JSON is not normalized
					Iterator<?> it = obj.keySet().iterator();
					String key = null;
					while(it.hasNext()) {
						key = (String) it.next();
						if(KEYWORD_REPOSITORY_URL.equalsIgnoreCase(key))							// (Optional) URL to a repository where the application's source code can be found
							payload.put(KEYWORD_REPOSITORY_URL, (String) obj.get(key));
						else
							if(KEYWORD_CODE_VERSION.equalsIgnoreCase(key))						    // (Optional) application's source code version
								payload.put(KEYWORD_CODE_VERSION, (String) obj.get(key));
							else 
							if(KEYWORD_CUSTOM_TRACKER_URL.equalsIgnoreCase(key))					// (Optional) Custom tracker URL (specified typically only for testing)
								customtrackingurl = (String) obj.get(key);							
						// ignore unrecognized keywords to prevent potential exploits
					}
				}
				
				// verify that the tracking information can be converted to JSON
				// serialize() throws an exception in case of a problem
				payload.serialize(); 
				
				trackingrequest = new TrackingRequest(payload);
				
				// assign this request to a tracker service
				if(customtrackingurl != null)
					trackingrequest.setTrackingURL(customtrackingurl);		// override default by using the environment variable value
				else
					trackingrequest.setTrackingURL(DEFAULT_TRACKER_URL);	// hard-wired default			
				
			} // try
			catch(Exception ex) {
				System.err.println("[JavaAppTracker] An error occurred while collecting tracking information: "+ ex.getClass().getName() + ":" + ex.getMessage());
				ex.printStackTrace(System.err);
				// invalidate the created objects
				payload = null;
				trackingrequest = null;
			}
			
		} // if(envApp != null)
		
		return trackingrequest;
	} // end method createTrackingRequest
	
} // end class