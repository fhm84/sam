package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.persons.Person;
import de.halbmann.sam.api.entity.persons.PersonCredit;
import de.halbmann.sam.api.entity.persons.PersonFilterRequest;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * REST API for the person catalogue: everyone credited on sheets as composer or arranger. A person
 * is a shared canonical entry, not a per-sheet copy. Band members are persons too; their personal
 * data is in {@link MembersResource}.
 */
@Path("persons")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface PersonsResource {

    /**
     * Returns a paginated list of persons matching the given filter criteria.
     *
     * @param filterRequest name filter and pagination parameters
     * @return paginated list of matching persons
     */
    @GET
    PaginatedResponse<Person> findPersons(@BeanParam PersonFilterRequest filterRequest);

    /**
     * Creates a new person.
     *
     * @param person the person to create
     * @return the persisted person including its generated ID
     */
    @POST
    Person add(Person person);

    /**
     * Loads a single person by their ID.
     *
     * @param personId the person ID
     * @return the person
     */
    @GET
    @Path("{personId}")
    Person load(@PathParam("personId") String personId);

    /**
     * Lists the sheets that credit the person as composer or arranger, sorted by title.
     *
     * @param personId the person ID
     * @return the person's credits
     */
    @GET
    @Path("{personId}/credits")
    List<PersonCredit> credits(@PathParam("personId") String personId);

    /**
     * Updates an existing person.
     *
     * @param personId the person ID
     * @param person   the updated person data
     */
    @PUT
    @Path("{personId}")
    void update(@PathParam("personId") String personId, Person person);

    /**
     * Merges a duplicate into the person to keep: the duplicate's credits move to the kept person
     * and the duplicate is deleted. If only the duplicate has a member profile, its record is the
     * one that stays and takes over the kept person's name, years and IPI, so the profile, its
     * seats and login stay intact. Two members can't be merged (409).
     *
     * @param personId the duplicate
     * @param targetId the person to keep
     * @return the remaining person
     */
    @POST
    @Path("{personId}/merge-into/{targetId}")
    @Consumes(MediaType.WILDCARD)
    Person mergeInto(@PathParam("personId") String personId, @PathParam("targetId") String targetId);

    /**
     * Deletes a person that is neither credited on a sheet nor a member.
     *
     * @param personId the person ID
     */
    @DELETE
    @Path("{personId}")
    void delete(@PathParam("personId") String personId);
}
