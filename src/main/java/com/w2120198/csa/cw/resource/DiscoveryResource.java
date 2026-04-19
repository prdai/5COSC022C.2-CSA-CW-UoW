package com.w2120198.csa.cw.resource;

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
    private static final String ADMIN_NAME = "Smart Campus Operations";
    private static final String ADMIN_EMAIL = "smartcampus-ops@westminster.ac.uk";
    private static final String ROOMS_PATH = "/rooms";
    private static final String SENSORS_PATH = "/sensors";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public DiscoveryResponse discover(@Context UriInfo uriInfo) {
        String base = uriInfo.getBaseUri().toString();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        Administrator administrator = new Administrator(ADMIN_NAME, ADMIN_EMAIL);
        Resources resources = new Resources(base + ROOMS_PATH, base + SENSORS_PATH);
        return new DiscoveryResponse(API_NAME, API_VERSION, administrator, resources);
    }

    public static class DiscoveryResponse {

        private String apiName;
        private String version;
        private Administrator administrator;
        private Resources resources;

        public DiscoveryResponse() {
        }

        public DiscoveryResponse(String apiName, String version, Administrator administrator, Resources resources) {
            this.apiName = apiName;
            this.version = version;
            this.administrator = administrator;
            this.resources = resources;
        }

        public String getApiName() {
            return apiName;
        }

        public void setApiName(String apiName) {
            this.apiName = apiName;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public Administrator getAdministrator() {
            return administrator;
        }

        public void setAdministrator(Administrator administrator) {
            this.administrator = administrator;
        }

        public Resources getResources() {
            return resources;
        }

        public void setResources(Resources resources) {
            this.resources = resources;
        }
    }

    public static class Administrator {

        private String name;
        private String email;

        public Administrator() {
        }

        public Administrator(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class Resources {

        private String rooms;
        private String sensors;

        public Resources() {
        }

        public Resources(String rooms, String sensors) {
            this.rooms = rooms;
            this.sensors = sensors;
        }

        public String getRooms() {
            return rooms;
        }

        public void setRooms(String rooms) {
            this.rooms = rooms;
        }

        public String getSensors() {
            return sensors;
        }

        public void setSensors(String sensors) {
            this.sensors = sensors;
        }
    }
}
