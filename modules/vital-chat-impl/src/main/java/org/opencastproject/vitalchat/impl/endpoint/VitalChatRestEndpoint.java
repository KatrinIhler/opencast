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

package org.opencastproject.vitalchat.impl.endpoint;

import static org.opencastproject.vitalchat.api.VitalChat.websocketAddress;

import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.vitalchat.api.VitalChat;

import com.google.gson.Gson;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The REST endpoint for the {@link VitalChat} service
 */
@Component(
        property = {
                "service.description=Vital Chat REST Endpoint",
                "opencast.service.type=org.opencastproject.vitalchat",
                "opencast.service.path=/vitalchat",
                "opencast.service.jobproducer=false"
        },
        immediate = true,
        service = VitalChatRestEndpoint.class
)
@Path("/")
@RestService(
        name = "VitalChatServiceEndpoint",
        title = "Vital Chat Service Endpoint",
        abstractText = "This is a tutorial service.",
        notes = {
                "Creates a websocket at " + websocketAddress + ". Access chats by connecting to " + websocketAddress
                + "/{chat-id}",
                "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
                "If the service is down or not working it will return a status 503, this means the the "
                        + "underlying service is not working and is either restarting or has failed",
                "A status code 500 means a general failure has occurred which is not recoverable and was "
                        + "not anticipated. In other words, there is a bug! You should file an error report "
                        + "with your server logs from the time when the error occurred: "
                        + "<a href=\"https://github.com/opencast/opencast/issues\">Opencast Issue Tracker</a>"
        }
)
public class VitalChatRestEndpoint {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(VitalChatRestEndpoint.class);

  /** The rest docs */
  protected String docs;

  /** The service */
  protected VitalChat vitalChatService;

  /** JSON parse utility */
  private static final Gson gson = new Gson();

  @GET
  @Path("getChats")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(
          name = "getChats",
          description = "Returns a list of chat ids",
          responses = {
                  @RestResponse(
                          responseCode = HttpServletResponse.SC_OK,
                          description = "Vital Chats"
                  ),
                  @RestResponse(
                          responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                          description = "The underlying service could not output something."
                  )
          },
        returnDescription = ""
  )
  public Response getVitalChats() throws Exception {
    return Response.ok().entity(
            gson.toJson(vitalChatService.getChats())
    ).build();
  }

//  @GET
//  @Path("vitalchat/{id}")
//  @Produces(MediaType.TEXT_PLAIN)
//  @RestQuery(
//          name = "vitalchat",
//          description = "Returns a JSON with all chat messages",
//          pathParameters = {
//                  @RestParameter(
//                          name = "id",
//                          description = "chat id",
//                          isRequired = true,
//                          type = RestParameter.Type.TEXT
//                  )
//          },
//          responses = {
//                  @RestResponse(
//                          responseCode = HttpServletResponse.SC_OK,
//                          description = "JSON with chat messages"
//                  ),
//                  @RestResponse(
//                          responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
//                          description = "The underlying service could not output something."
//                  )
//          },
//        returnDescription = ""
//  )
//  public Response getVitalChat(@PathParam("id") String id) throws Exception {
//    logger.info("REST call for Vital Chat ID");
//    return Response.ok().build();
//  }

  @POST
  @Path("createChat/{id}")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(
          name = "createChat",
          description = "Creates a new chat with id",
          pathParameters = {
                  @RestParameter(
                          name = "id",
                          description = "chat id",
                          isRequired = true,
                          type = RestParameter.Type.STRING
                  )
          },
          responses = {
                  @RestResponse(
                          responseCode = HttpServletResponse.SC_OK,
                          description = "Chat created"
                  ),
                  @RestResponse(
                          responseCode = HttpServletResponse.SC_CONFLICT,
                          description = "Could not create chat, chat probably already exists"
                  )
          },
      returnDescription = ""
  )
  public Response addVitalChatMessage(@PathParam("id") String id) throws Exception {
    if (vitalChatService.createChat(id)) {
      return Response.ok().build();
    } else {
      return Response.serverError().status(Response.Status.CONFLICT).build();
    }
  }

  @DELETE
  @Path("deleteChat/{id}")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(
          name = "deleteChat",
          description = "Removes a chat",
          pathParameters = {
                  @RestParameter(
                          name = "id",
                          description = "chat id",
                          isRequired = true,
                          type = RestParameter.Type.STRING
                  )
          },
          responses = {
                  @RestResponse(
                          responseCode = HttpServletResponse.SC_OK,
                          description = "Chat deleted"
                  ),
                  @RestResponse(
                          responseCode = HttpServletResponse.SC_NOT_FOUND,
                          description = "Chat could not be deleted, probably already has been deleted"
                  )
          },
      returnDescription = ""
  )
  public Response deleteVitalChat(@PathParam("id") String id) throws Exception {
    if (vitalChatService.deleteChat(id)) {
      return Response.ok().build();
    } else {
      return Response.serverError().status(Response.Status.NOT_FOUND).build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocs() {
    return docs;
  }

  @Reference(name = "vitalchat-service")
  public void setVitalChatService(VitalChat service) {
    this.vitalChatService = service;
  }
}
