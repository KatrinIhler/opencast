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

package org.opencastproject.vitallivestream.impl;

import static org.easymock.EasyMock.anyString;

import org.opencastproject.vitalchat.api.VitalChat;
import org.opencastproject.vitallivestream.api.VitalLivestreamService.JsonVitalLiveStream;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test class for Hello World Tutorial
 */
public class VitalLivestreamServiceTest {

  private VitalLivestreamServiceImpl service;

  /**
   * Setup for the Hello World Service
   */
  @Before
  public void setUp() {
//    service = new VitalLivestreamServiceImpl();

    service = new VitalLivestreamServiceImpl() {
//      @Override
//      protected JobBarrier.Result waitForStatus(Job... jobs) throws IllegalStateException, IllegalArgumentException {
//        JobBarrier.Result result = EasyMock.createNiceMock(JobBarrier.Result.class);
//        EasyMock.expect(result.isSuccess()).andReturn(true).anyTimes();
//        EasyMock.replay(result);
//        return result;
//      }
    };
    VitalChat vitalChatService = EasyMock.createMock(VitalChat.class);
    EasyMock.expect(vitalChatService.createChat(anyString())).andReturn(true).anyTimes();
    EasyMock.expect(vitalChatService.deleteChat(anyString())).andReturn(true).anyTimes();

    EasyMock.replay(vitalChatService);

    service.setVitalChatService(vitalChatService);
  }

  @Test
  public void testUpdateLivestream() throws Exception {
    List<JsonVitalLiveStream> livestreams = new ArrayList<>();
    JsonVitalLiveStream livestream = new JsonVitalLiveStream("MyId");
    livestreams.add(livestream);

    service.updateLivestream(livestream);

    Assert.assertEquals(livestreams, service.getLivestreams());
  }

  @Test
  public void testUpdateLivestreamSameEventID() throws Exception {
    JsonVitalLiveStream livestream1 = new JsonVitalLiveStream("MyId");
    JsonVitalLiveStream livestream2 = new JsonVitalLiveStream("MyId");

    service.updateLivestream(livestream1);
    service.updateLivestream(livestream2);

    Assert.assertEquals(1, service.getLivestreams().size());
  }

  @Test
  public void testDeleteLivestream() throws Exception {
    JsonVitalLiveStream livestream1 = new JsonVitalLiveStream("MyId1");
    JsonVitalLiveStream livestream2 = new JsonVitalLiveStream("MyId2");
    JsonVitalLiveStream livestream3 = new JsonVitalLiveStream("MyId3");
    List<JsonVitalLiveStream> livestreams = new ArrayList<>();
    livestreams.add(livestream1);
    livestreams.add(livestream3);

    service.updateLivestream(livestream1);
    service.updateLivestream(livestream2);
    service.updateLivestream(livestream3);

    Boolean deleted = service.deleteLivestream(livestream2);

    Assert.assertEquals(true, deleted);
    Assert.assertEquals(livestreams, service.getLivestreams());
  }

  @Test
  public void testDeleteLivestreamThatDoesntExist() throws Exception {
    JsonVitalLiveStream livestream = new JsonVitalLiveStream("MyId1");

    Boolean deleted = service.deleteLivestream(livestream);

    Assert.assertEquals(false, deleted);
  }

  @Test
  public void testGetLivestreamByChannel() throws Exception {
    String getChannelId = "MyIdGET";
    JsonVitalLiveStream livestream1 = new JsonVitalLiveStream("MyId1");
    JsonVitalLiveStream livestream2 = new JsonVitalLiveStream(getChannelId);
    JsonVitalLiveStream livestream3 = new JsonVitalLiveStream("MyId3");

    service.updateLivestream(livestream1);
    service.updateLivestream(livestream2);
    service.updateLivestream(livestream3);

    JsonVitalLiveStream gottenLivestream = service.getLivestreamByChannel(getChannelId);

    Assert.assertEquals(livestream2, gottenLivestream);
  }

  @Test
  public void testGetLivestreamByChannelThatDoesntExist() throws Exception {
    String getChannelId = "MyIdDoesNotExist";
    JsonVitalLiveStream livestream1 = new JsonVitalLiveStream("MyId1");
    JsonVitalLiveStream livestream2 = new JsonVitalLiveStream("MyId2");

    service.updateLivestream(livestream1);
    service.updateLivestream(livestream2);

    JsonVitalLiveStream gottenLivestream = service.getLivestreamByChannel(getChannelId);

    Assert.assertEquals(null, gottenLivestream);
  }

  @Test
  public void testGetLivestreamByChannelWhenNoLivestreamsExist() throws Exception {
    String getChannelId = "MyIdDoesNotExist";

    JsonVitalLiveStream gottenLivestream = service.getLivestreamByChannel(getChannelId);

    Assert.assertEquals(null, gottenLivestream);
  }

}
