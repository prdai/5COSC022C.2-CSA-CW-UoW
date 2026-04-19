package com.w2120198.csa.cw.resource;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

@Path("/")
public class DiscoveryResource {

    private static final String API_NAME = "Smart Campus Sensor & Room Management API";
    private static final String API_VERSION = "1.0.0";
    private static final String ADMIN_NAME = "Ranuga Disansa Gamage";
    private static final String ADMIN_EMAIL = "ranuga.20231264@iit.ac.lk";
    private static final String ROOMS_PATH = "rooms";
    private static final String SENSORS_PATH = "sensors";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> discover(@Context UriInfo uriInfo) {
        URI roomsUri = uriInfo.getBaseUriBuilder().path(ROOMS_PATH).build();
        URI sensorsUri = uriInfo.getBaseUriBuilder().path(SENSORS_PATH).build();

        Map<String, String> administrator = new LinkedHashMap<>();
        administrator.put("name", ADMIN_NAME);
        administrator.put("email", ADMIN_EMAIL);

        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms", roomsUri.toString());
        resources.put("sensors", sensorsUri.toString());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("apiName", API_NAME);
        body.put("version", API_VERSION);
        body.put("administrator", administrator);
        body.put("resources", resources);
        return body;
    }
}
