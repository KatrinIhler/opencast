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

//  @Test
//  public void testGetVitalLivestreams() throws Exception {
//    String legalJson = "{ 'id': 'MyId', 'viewer': 'http://nope.com' }";
//    String legalJson2 = "{ 'id': 'MyId2', 'viewer': 'http://nope.com' }";
//    String returnJson = "[" + legalJson + "," + legalJson2 + "]";
//
//    Response response = rest.updateVitalLivestream(legalJson);
//    Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
//    Response response2 = rest.updateVitalLivestream(legalJson2);
//    Assert.assertEquals(Response.Status.OK.getStatusCode(), response2.getStatus());
//
//    Response response3 = rest.getVitalLivestreams();
//    Assert.assertEquals(Response.Status.OK.getStatusCode(), response3.getStatus());
//
//    Map<String, Object> rightMap = gson.fromJson(returnJson, channelListType);
//    Map<String, Object> leftMap = gson.fromJson((JsonElement) response3.getEntity(), channelListType);
//    MapDifference<String, Object> difference = Maps.difference(leftMap, rightMap);
//    Assert.assertEquals(returnJson, response3.getEntity());
//  }
}
