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

package org.opencastproject.vitallivestream.impl.endpoint;

import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.vitallivestream.api.VitalLivestreamService;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The REST endpoint for the {@link VitalLivestreamService} service
 */
@Component(
    property = {
        "service.description=Vital Livestream REST Endpoint",
        "opencast.service.type=org.opencastproject.vitallivestream",
        "opencast.service.path=/vitallivestream",
        "opencast.service.jobproducer=false"
    },
    immediate = true,
    service = VitalLivestreamRestEndpoint.class
)
@Path("/")
@RestService(
    name = "VitalLivestreamServiceEndpoint",
    title = "Vital Livestream Service Endpoint",
    abstractText = "This is the service for the Vital Livestream",
    notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the "
            + "underlying service is not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was "
            + "not anticipated. In other words, there is a bug! You should file an error report "
            + "with your server logs from the time when the error occurred: "
            + "<a href=\"https://github.com/opencast/opencast/issues\">Opencast Issue Tracker</a>"
    }
)
public class VitalLivestreamRestEndpoint {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(VitalLivestreamRestEndpoint.class);

  /** The rest docs */
  protected String docs;

  /** The service */
  protected VitalLivestreamService vitalLivestreamService;

  private static final Gson gson = new Gson();

  /**
   * CHannels
   *
   * @return The Hello World statement
   * @throws Exception
   */
  @GET
  @Path("availablechannels")
  @RestQuery(
          name = "availablechannels",
          description = "Get available channels",
          responses = {
                  @RestResponse(
                          responseCode = HttpServletResponse.SC_OK,
                          description = "The available channels."
                  ),
                  @RestResponse(
                          responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                          description = "The underlying service could not output something."
                  )
          },
          returnDescription = "All clear."
  )
  public Response availableChannels() throws Exception {
    logger.info("REST call for Available Channels");

    return Response.ok().entity(
            gson.toJson(vitalLivestreamService.getAvailableChannels())
    ).build();
  }

  /**
   * Livestreams
   *
   * @return The Hello World statement
   * @throws Exception
   */
  @GET
  @Path("vitallivestream")
  @RestQuery(
          name = "vitallivestream",
          description = "Get livestreams",
          responses = {
                  @RestResponse(
                          responseCode = HttpServletResponse.SC_OK,
                          description = "The livestreams."
                  ),
                  @RestResponse(
                          responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                          description = "The underlying service could not output something."
                  )
          },
          returnDescription = "All clear."
  )
  public Response getVitalLivestreams() throws Exception {
    logger.info("REST call for Livestreams");

    return Response.ok().entity(
            gson.toJson(vitalLivestreamService.getLivestreams())
    ).build();
  }

  /**
   * Livestream by Id
   *
   * @return The Hello World statement
   * @throws Exception
   */
  @GET
  @Path("vitallivestream/{channelId}")
  @RestQuery(
          name = "vitallivestreamWithId",
          description = "Get livestream by id",
          pathParameters = {
                  @RestParameter(
                          name = "channelId",
                          description = "Id of the livestream",
                          isRequired = true,
                          type = RestParameter.Type.STRING
                  )
          },
          responses = {
                  @RestResponse(
                          responseCode = HttpServletResponse.SC_OK,
                          description = "The livestream."
                  ),
                  @RestResponse(
                          responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                          description = "The underlying service could not output something."
                  )
          },
          returnDescription = "All clear."
  )
  public Response getVitalLivestream(@PathParam("channelId") String channelId) throws Exception {
    logger.info("REST call for Livestreams");

    return Response.ok().entity(
            gson.toJson(vitalLivestreamService.getLivestreamByChannel(channelId))
    ).build();
  }

  /**
   * Simple example service call
   *
   * @return The Hello World statement
   * @throws Exception
   */
  @PUT
  @Path("vitallivestream")
  @RestQuery(
      name = "vitallivestream",
      description = "Adds a livestream",
      restParameters = {
          @RestParameter(
                  name = "livestream",
                  description = "JSON with livestream",
                  isRequired = true,
                  type = RestParameter.Type.STRING
          ),
      },
      responses = {
          @RestResponse(
              responseCode = HttpServletResponse.SC_OK,
              description = "Livestream was added."
          ),
          @RestResponse(
              responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              description = "The underlying service could not output something."
          )
      },
      returnDescription = "All clear."
  )
  public Response updateVitalLivestream(@FormParam("livestream") String liveStreamJSON) throws Exception {
    logger.info("REST call for Vital Livestream");

    // Parse
    try {
      VitalLivestreamService.JsonVitalLiveStream liveStream;
      try {
        liveStream = gson.fromJson(liveStreamJSON, VitalLivestreamService.JsonVitalLiveStream.class);
      } catch (JsonSyntaxException e) {
        throw new IllegalArgumentException(e);
      }
      if (liveStream == null) {
        throw new IllegalArgumentException("JSON is empty");
      }
      if (liveStream != null
          && liveStream.getViewer() == null) {
        throw new IllegalArgumentException("Viewer is missing or malformed");
      }
      if (liveStream != null
          && liveStream.getId() == null) {
        throw new IllegalArgumentException("Event is missing or missing channel id");
      }

      vitalLivestreamService.updateLivestream(liveStream);

    } catch (IllegalArgumentException e) {
      logger.debug("Received invalid JSON", e);
      return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity("Invalid JSON").build();
    }

    return Response.ok().build();
  }

  /**
   * Simple example service call
   *
   * @return The Hello World statement
   * @throws Exception
   */
  @DELETE
  @Path("vitallivestream")
  @RestQuery(
          name = "deleteVitalLivestream",
          description = "Deletes a livestream",
          restParameters = {
                  @RestParameter(
                          name = "livestream",
                          description = "JSON with livestream",
                          isRequired = true,
                          type = RestParameter.Type.STRING
                  ),
          },
          responses = {
                  @RestResponse(
                          responseCode = HttpServletResponse.SC_OK,
                          description = "Livestream was deleted."
                  ),
                  @RestResponse(
                          responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                          description = "The underlying service could not output something."
                  )
          },
          returnDescription = "All clear."
  )
  public Response deleteVitalLivestream(@FormParam("livestream") String liveStreamJSON) throws Exception {
    logger.info("REST call for Vital Livestream");

    // Parse
    try {
      VitalLivestreamService.JsonVitalLiveStream liveStream;
      try {
        liveStream = gson.fromJson(liveStreamJSON, VitalLivestreamService.JsonVitalLiveStream.class);
      } catch (JsonSyntaxException e) {
        throw new IllegalArgumentException(e);
      }
      if (liveStream == null) {
        throw new IllegalArgumentException("JSON is empty");
      }
      if (liveStream != null
          && liveStream.getId() == null) {
        throw new IllegalArgumentException("Channel is missing or missing channel id");
      }

      vitalLivestreamService.deleteLivestream(liveStream);

    } catch (IllegalArgumentException e) {
      logger.debug("Received invalid JSON", e);
      return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity("Invalid JSON").build();
    }

    return Response.ok().build();
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocs() {
    return docs;
  }

  @Reference(name = "vitallivestream-service")
  public void setVitalLivestreamService(VitalLivestreamService service) {
    this.vitalLivestreamService = service;
  }
}
