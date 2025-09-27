package org.acme;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST endpoint for cache management operations
 */
@Path("/api/cache")
public class CacheResource {

    @Inject
    CachePopulator cachePopulator;

    @POST
    @Path("/populate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response populateCache() {
        cachePopulator.populateCacheWithTestData();
        return Response.ok("{\"message\": \"Cache populated with test data\"}").build();
    }

    @POST
    @Path("/clear")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearCache() {
        cachePopulator.clearCache();
        return Response.ok("{\"message\": \"Cache cleared\"}").build();
    }

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCacheStatus() {
        return Response.ok("{\"status\": \"Cache service is running\"}").build();
    }
}
