package com.sg.aurora.apigateway.rest.resources;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.ParseException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;

import org.glassfish.jersey.client.ClientConfig;
import org.json.JSONObject;

import com.sg.aurora.apigateway.rest.service.RequestService;
import com.sg.aurora.apigateway.rest.service.UserService;
import com.sg.aurora.apigateway.rest.model.URLFormData;

@Path("/api")
public class APIController {
  
    @POST
	@Produces(MediaType.APPLICATION_JSON)
  	@Path("/urldata")
	public void getURLInfo(@FormParam("datepicker") String date, @FormParam("timepicker") String time,  @FormParam("nexrad_station") String station, @Context HttpServletRequest request) throws ParseException, SQLException {
  		HttpSession session = request.getSession();
  		int userId = (Integer)session.getAttribute("USERID");
  		RequestService requestService = new RequestService();
  		int requestId = requestService.generateUserRequest(userId);
		if(requestId != -1){
			URLFormData urlFormData = new URLFormData(station, date, time, userId, requestId);
			String dataIngestorServiceURL = "http://localhost:8080/dataingestor/urlcreation/generate";
			String responseFromDataIngestor = executeAndGetDataIngestorResponse(dataIngestorServiceURL, urlFormData);
			
			JSONObject responseFromDataIngestorJSON = new JSONObject(responseFromDataIngestor);
			responseFromDataIngestorJSON.put("userId", userId);
			responseFromDataIngestorJSON.put("requestId", requestId);
			String stormDetectorServiceURL = "http://127.0.0.1:5000/StormDetector";
			String responseFromStormDetector = executeAndGetStormDetectorResponse(stormDetectorServiceURL, responseFromDataIngestorJSON);
			System.out.println("responseFromStormDetector :: " + responseFromStormDetector);
			
			JSONObject responseFromStormDetectorJSON = new JSONObject(responseFromStormDetector);
			responseFromStormDetectorJSON.put("userId", userId);
			responseFromStormDetectorJSON.put("requestId", requestId);
			String stormClusterringServiceURL = "http://127.0.0.1:5050/StormClustering";
			executeAndGetStormClusterringResponse(stormClusterringServiceURL, responseFromStormDetectorJSON);
		}
    }
  
  	public String executeAndGetDataIngestorResponse( String incomingURL, URLFormData urlFormData) {
	  	Client client = ClientBuilder.newClient();
		Response response = client.target(incomingURL).request().post(Entity.json(urlFormData));
		return response.readEntity(String.class);		
  	}
  	
  	public String executeAndGetStormDetectorResponse(String incomingURL, JSONObject responseFromDataIngestorJSON) {
  	  Client client = ClientBuilder.newClient();
	  Response response = client.target(incomingURL).request().post(Entity.json(responseFromDataIngestorJSON.toString()));
	  System.out.println("Status of call to Storm Detector :: " + response.getStatus());
	  return response.readEntity(String.class);
    }	
	
  	public String executeAndGetStormClusterringResponse( String incomingURL, JSONObject responseFromStormDetectorJSON) {
	  Client client = ClientBuilder.newClient();
  	  Response response = client.target(incomingURL).request().post(Entity.json(responseFromStormDetectorJSON.toString()));
  	  System.out.println("Status of call to Storm Detector :: " + response.getStatus());
  	  return response.readEntity(String.class);
    }	
  	
  @POST
  @Path("/login")
  public Response validateUser(@FormParam("username") String userName, @FormParam("password") String password, @Context HttpServletRequest request) throws URISyntaxException {
	UserService userService = new UserService();
	URI targetURIForRedirection = null;
	int userId = userService.validateUser(userName, password);
	if(userId != -1){
		HttpSession session= request.getSession(true);
		session.setAttribute("USERID", userId);
		session.setAttribute("USERNAME", userName);
		targetURIForRedirection = new URI(request.getContextPath()+"/jsp/client.jsp");
	}
	else{
		targetURIForRedirection = new URI(request.getContextPath()+"/jsp/login.jsp");
	}
	return Response.seeOther(targetURIForRedirection).build();
  }
  
}