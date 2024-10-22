package org.acme.opentelemetry;

import com.google.common.util.concurrent.AtomicDouble;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.logging.Logger;

@Path("/")
public class TracedResource {

    private static final Logger LOG = Logger.getLogger(TracedResource.class);

    @Context
    UriInfo uriInfo;

    @Inject
    MeterRegistry registry;

    private AtomicDouble xValue = new AtomicDouble(0.0);

    @PostConstruct
    public void init() {
        Gauge.builder("service.xvalue", xValue, AtomicDouble::get)
                .baseUnit("units")
                .description("Current value of X in the service")
                .tag("service", "TracedResource")
                .register(registry);
    }

    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        LOG.info("hello");
        return "hello";
    }

    @GET
    @Path("/chain")
    @Produces(MediaType.TEXT_PLAIN)
    public String chain() {
        ResourceClient resourceClient = RestClientBuilder.newBuilder()
                .baseUri(uriInfo.getBaseUri())
                .build(ResourceClient.class);
        return "chain -> " + resourceClient.hello();
    }

    @GET
    @Path("/metrics/set")
    @Produces(MediaType.APPLICATION_JSON)
    public String setMetric(@QueryParam("value") Double value) {
        if (value == null) {
            LOG.warn("Attempted to set metric without providing a value.");
            return "{\"status\":\"failure\", \"message\":\"Value parameter is missing\"}";
        }
        xValue.set(value);
        LOG.infof("Metric 'xvalue' set to: %f", value);
        return "{\"status\":\"success\", \"xvalue\":" + value + "}";
    }
}
