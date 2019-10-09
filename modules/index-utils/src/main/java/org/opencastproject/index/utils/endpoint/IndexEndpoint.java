/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.index.utils.endpoint;

import static org.opencastproject.util.doc.rest.RestParameter.Type.BOOLEAN;

import org.opencastproject.adminui.index.AdminUISearchIndex;
import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.index.event.Event;
import org.opencastproject.external.index.ExternalIndex;
import org.opencastproject.index.utils.IndexUtils;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.impl.WorkflowServiceIndex;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/")
@RestService(name = "indexutils", title = "Index Utils", notes = {}, abstractText = "Provides utility functionality "
        + "for the admin ui and api indices")
public class IndexEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(IndexEndpoint.class);

  /* OSGi service references */
  private ExternalIndex externalIndex;
  private AdminUISearchIndex adminUIIndex;
  private WorkflowServiceIndex workflowIndex;
  private IndexUtils indexUtils;

  void setExternalIndex(ExternalIndex externalIndex) {
    this.externalIndex = externalIndex;
  }

  void setAdminUIIndex(AdminUISearchIndex adminUIIndex) {
    this.adminUIIndex = adminUIIndex;
  }

  void setWorkflowIndex(WorkflowServiceIndex workflowIndex) {
    this.workflowIndex = workflowIndex;
  }

  void setIndexUtils(IndexUtils indexUtils) {
    this.indexUtils = indexUtils;
  }

  @GET
  @Path("adminui/events/{eventId}")
  @RestQuery(name = "geteventadminui", description = "Get an event from the admin UI index.",
          returnDescription = "The event.", pathParameters = {
          @RestParameter(name = "eventId", isRequired = true, description = "The id of the event.",
                  type = RestParameter.Type.STRING) }, responses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "The event in the admin ui index."),
          @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED,
                  description = "If the current user is not admin") })
  public Response getEventFromAdminUIIndex(@PathParam("eventId") String id) throws SearchIndexException {
    Event e = indexUtils.getEventFromIndex(adminUIIndex, id);
    return Response.ok(e.toJSON()).build();
  }

  @GET
  @Path("api/events/{eventId}")
  @RestQuery(name = "geteventapi", description = "Get an event from the external api index.",
          returnDescription = "The event.", pathParameters = {
          @RestParameter(name = "eventId", isRequired = true, description = "The id of the event.",
                  type = RestParameter.Type.STRING) }, responses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "The event in the api index."),
          @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED,
                  description = "If the current user is not admin") })
  public Response getEventFromApiIndex(@PathParam("eventId") String id) throws SearchIndexException {
    Event e = indexUtils.getEventFromIndex(externalIndex, id);
    return Response.ok(e.toJSON()).build();
  }

  @GET
  @Path("api/events/emptyacls")
  @RestQuery(name = "getemptyaclsapi", description = "Get events with empty acls in api index",
             returnDescription = "The events with empty or broken acls in the external api index.",
             responses = {
               @RestResponse(responseCode = HttpServletResponse.SC_OK,
                             description = "The events with empty or broken acls in the external api index.")
             })
  public Response getEventsWithEmptyAclinApiIndex() throws SearchIndexException {
    JSONObject result = indexUtils.getEventsWithEmptyAclInIndex(externalIndex);
    return Response.ok(result.toJSONString()).build();
  }

  @GET
  @Path("adminui/events/emptyacls")
  @RestQuery(name = "getemptyaclsadminui", description = "Get events with empty acls in admin ui index",
             returnDescription = "The events with empty or broken acls in the admin ui index.",
             responses = {
               @RestResponse(responseCode = HttpServletResponse.SC_OK,
                             description = "The events with empty or broken acls in the admin ui index.")
             })
  public Response getEventsWithEmptyAclInAdminUiIndex() throws SearchIndexException {
    JSONObject result = indexUtils.getEventsWithEmptyAclInIndex(adminUIIndex);
    return Response.ok(result.toJSONString()).build();
  }

  @POST
  @Path("adminui/events/emptyacls")
  @RestQuery(name = "fixemptyaclsadminui", description = "Fix events with empty acl in admin ui index",
          returnDescription = "The events that were fixed and the ones that weren't.",
          restParameters = {
            @RestParameter(name = "fixBrokenAcls", isRequired = false,
                    description = "Whether to also try to fix broken acls", type = BOOLEAN) },
          responses = {
            @RestResponse(responseCode = HttpServletResponse.SC_OK,
                    description = "The events that were fixed and the ones that weren't.")})
  public Response fixEventsWithEmptyAclInAdminUi(@FormParam("fixBrokenAcls") final boolean fixBrokenAcls)
          throws SearchIndexException {
    JSONObject result = indexUtils.fixEventsWithEmptyAclInIndex(adminUIIndex, fixBrokenAcls);
    return Response.ok(result.toJSONString()).build();
  }

  @POST
  @Path("api/events/emptyacls")
  @RestQuery(name = "fixemptyaclsapi", description = "Fix events with empty acl in api index",
          returnDescription = "The events that were fixed and the ones that weren't.",
          restParameters = {
                  @RestParameter(name = "fixBrokenAcls", isRequired = false,
                          description = "Whether to also try to fix broken acls", type = BOOLEAN) },
          responses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK,
                          description = "The events that were fixed and the ones that weren't.")})
  public Response fixEventsWithEmptyAclInApi(@FormParam("fixBrokenAcls") final boolean fixBrokenAcls)
          throws SearchIndexException {
    JSONObject result = indexUtils.fixEventsWithEmptyAclInIndex(externalIndex, fixBrokenAcls);
    return Response.ok(result.toJSONString()).build();
  }

  @DELETE
  @Path("workflows/{workflowId}")
  @RestQuery(name = "deleteworkflow", description = "Delete a workflow from the workflow index.",
          returnDescription = "Ok if the workflow has been deleted.", pathParameters = {
          @RestParameter(name = "workflowId", isRequired = true, description = "The id of the workflow to delete.",
                  type = RestParameter.Type.STRING) }, responses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "The workflow has been deleted."),
          @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED,
                  description = "If the current user is not admin") })
  public Response deleteWorkflowFromWorkflowIndex(@PathParam("workflowId") String id)
          throws WorkflowDatabaseException, NotFoundException {
    workflowIndex.remove(Long.parseLong(id));
    logger.info("Removed workflow " + id + " from workflow index.");
    return Response.ok().build();
  }

  @DELETE
  @Path("adminui/events/{eventId}")
  @RestQuery(name = "deleteeventadminui", description = "Delete an event from the admin UI index.",
          returnDescription = "Ok if the event has been deleted.", pathParameters = {
          @RestParameter(name = "eventId", isRequired = true, description = "The id of the event to delete.",
                  type = RestParameter.Type.STRING) }, responses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "The event has been deleted."),
          @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED,
                  description = "If the current user is not admin") })
  public Response deleteEventFromAdminUIIndex(@PathParam("eventId") String id) throws SearchIndexException {
    indexUtils.deleteEventFromIndex(adminUIIndex, id);
    return Response.ok().build();
  }

  @DELETE
  @Path("api/events/{eventId}")
  @RestQuery(name = "deleteeventapi", description = "Delete an event from the external api index.",
          returnDescription = "Ok if the event has been deleted.", pathParameters = {
          @RestParameter(name = "eventId", isRequired = true, description = "The id of the event to delete.",
                  type = RestParameter.Type.STRING) }, responses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "The event has been deleted."),
          @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED,
                  description = "If the current user is not admin") })
  public Response deleteEventFromAPIIndex(@PathParam("eventId") String id) throws SearchIndexException {
    indexUtils.deleteEventFromIndex(externalIndex, id);
    return Response.ok().build();
  }
}
