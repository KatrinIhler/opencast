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

import org.opencastproject.vitallivestream.api.VitalLivestreamService;
import org.opencastproject.vitallivestream.impl.VitalLivestreamServiceImpl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

/**
 * Test class for Hello World Tutorial
 */
public class VitalLivestreamRestEndpointTest {

  private VitalLivestreamRestEndpoint rest;

  /**
   * Setup for the Hello World Rest Service
   */
  @Before
  public void setUp() {
    VitalLivestreamService service = new VitalLivestreamServiceImpl();
    rest = new VitalLivestreamRestEndpoint();
    rest.setVitalLivestreamService(service);
  }

  @Test
  public void testUpdateVitalLivestream() throws Exception {
    String legalJson = "{ 'id': 'MyId', 'viewer': 'http://nope.com' }";
    Response response = rest.updateVitalLivestream(legalJson);
    Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testUpdateVitalLivestreamCompleteJson() throws Exception {
    JsonObject completeJson = new JsonObject();
    completeJson.addProperty("id", "MyId");
    completeJson.addProperty("viewer", "https://api.medunigraz.at/video/live/viewer/<eventid>/");
    completeJson.addProperty("title", "lorem ipsum");
    completeJson.addProperty("description",
            "Lorem <strong>ipsum dolor</strong> sit "
                + "amet, <a href=\"https://www.medunigraz.at/\">consetetur</a> sadipscing elitr, ...");
    completeJson.addProperty("unrestricted", true);

    JsonObject previews = new JsonObject();
    JsonArray presenter = new JsonArray();
    presenter.add("https://1.asd.medunigraz.at/livestream/<eventid>/<stream1id>.jpg");
    presenter.add("https://2.asd.medunigraz.at/livestream/<eventid>/<stream1id>.jpg");
    JsonArray slides = new JsonArray();
    slides.add("https://1.asd.medunigraz.at/livestream/<eventid>/<stream1id>.jpg");
    slides.add("https://2.asd.medunigraz.at/livestream/<eventid>/<stream1id>.jpg");
    previews.add("presenter", presenter);
    previews.add("slides", slides);

    completeJson.add("previews", previews);

    String payload = new Gson().toJson(completeJson);
    Response responseUpdate = rest.updateVitalLivestream(payload);
    Assert.assertEquals(Response.Status.OK.getStatusCode(), responseUpdate.getStatus());

    JsonArray completeJsonArray = new JsonArray();
    completeJsonArray.add(completeJson);
    Response responseGet = rest.getVitalLivestreams();
    Object responseArray = new Gson().fromJson((String) responseGet.getEntity(), JsonArray.class);
    Assert.assertTrue(completeJsonArray.equals(responseArray));
  }

  @Test
  public void testUpdateVitalLivestreamIllegalJson() throws Exception {
    String illegalJson = "{ 'id': 'MyId, 'viewer': 'http://nope.com' }";
    Response response = rest.updateVitalLivestream(illegalJson);
    Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testDeleteVitalLivestream() throws Exception {
    String legalJson = "{ 'id': 'MyId' }";
    Response response = rest.deleteVitalLivestream(legalJson);
    Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testDeleteVitalLivestreamIllegalJson() throws Exception {
    String legalJson = "{ 'id': 'MyId }";
    Response response = rest.deleteVitalLivestream(legalJson);
    Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
}
