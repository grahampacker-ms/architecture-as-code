package org.finos.calm.resources;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.finos.calm.domain.Domain;
import org.finos.calm.domain.ValueWrapper;
import org.finos.calm.domain.exception.DomainAlreadyExistsException;
import org.finos.calm.store.DomainStore;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * REST resource for managing domains.
 */
@Path("/calm/controls/domains")
public class DomainSchemaResource {

    private final DomainStore store;

    /**
     * Constructor for DomainSchemaResource.
     *
     * @param store the DomainStore instance
     */
    @Inject
    public DomainSchemaResource(DomainStore store) {
        this.store = store;
    }

    /**
     * Retrieves the list of domains.
     *
     * @return a Response containing the list of domains
     */
    @GET
    @Produces("application/json")
    public Response getDomains() {
        return Response.ok(new ValueWrapper<>(store.getDomains())).build();
    }

    /**
     * Creates a new domain if it does not already exist and is of the correct structure
     * @param domain the domain to create
     * @return a Response indicating the result of the operation
     * @throws URISyntaxException if the URI syntax is incorrect
     */
    @POST
    @Produces("application/json")
    @Consumes("application/json")
    public Response createDomain(@Valid Domain domain) throws URISyntaxException {
        String domainName = domain.getName();

        try {
            domain = store.createDomain(domainName);
        } catch (DomainAlreadyExistsException e) {
            return Response.status(Response.Status.CONFLICT).entity("{\"error\":\"Domain already exists\"}").build();
        }
        return Response.created(new URI("/calm/domains/" + domainName)).build();
    }

}
