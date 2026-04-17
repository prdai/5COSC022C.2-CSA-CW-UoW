package com.w2120198.csa.cw.resource;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

/**
 * Root discovery endpoint. Returns metadata about the API and a map
 * of primary resource collection URIs so clients can traverse the API
 * from a single well-known entry point (HATEOAS-style).
 */
@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> discover(@Context UriInfo uriInfo) {
        String base = uriInfo.getBaseUri().toString();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        Map<String, String> administrator = new LinkedHashMap<>();
        administrator.put("name", "Smart Campus Operations");
        administrator.put("email", "smartcampus-ops@westminster.ac.uk");

        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms", base + "/rooms");
        resources.put("sensors", base + "/sensors");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("apiName", "Smart Campus Sensor & Room Management API");
        body.put("version", "1.0.0");
        body.put("module", "5COSC022W — Client-Server Architectures");
        body.put("administrator", administrator);
        body.put("resources", resources);
        return body;
    }
}
