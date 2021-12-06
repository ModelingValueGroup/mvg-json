//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2021 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
//                                                                                                                     ~
// Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in      ~
// compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0  ~
// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on ~
// an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the  ~
// specific language governing permissions and limitations under the License.                                          ~
//                                                                                                                     ~
// Maintainers:                                                                                                        ~
//     Wim Bast, Tom Brus, Ronald Krijgsheld                                                                           ~
// Contributors:                                                                                                       ~
//     Arjan Kok, Carel Bast                                                                                           ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.json.protocol;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("unchecked")
public class ProtocolTests {
    private TestProtocolHandlerWithPeer tph;

    @BeforeEach
    public void beforeEach() {
        assertNull(tph);
        tph = TestProtocolHandlerWithPeer.createPipedWithPeer('\n');
    }

    @AfterEach
    public void afterEach() throws IOException, InterruptedException {
        assertNotNull(tph);
        if (!tph.isShutdown()) {
            tph.shutdown();
            long t0 = System.currentTimeMillis();
            while (!tph.isShutdown() && System.currentTimeMillis() < t0 + 30_000) {
                //noinspection BusyWait
                Thread.sleep(1);
            }
            assertTrue(tph.isShutdown());
        }
        tph = null;
    }

    @Test
    public void onePingTest() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            assertEquals(0L, tph.getPingCount());
            tph.ping();
            assertEquals(1L, tph.getPingCount());
            tph.ping();
            assertEquals(2L, tph.getPingCount());
            tph.ping();
            assertEquals(3L, tph.getPingCount());
        });
    }

    @Test
    public void manyPingTest() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            for (int i = 0; i < 100; i++) {
                tph.ping();
            }
            assertEquals(100L, tph.getPingCount());
        });
    }

    @Test
    public void seqNumberTest() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            Message message = tph.sendAndReceiveSingle("getPingCount", "pingCount", null);
            assertEquals(1, message.senderMessageNumber());
            assertEquals(1, message.receiverMessageNumber());
            assertEquals(0L, ((Map<String, Long>) message.json()).get(tph.getUUID()));

            for (int i = 0; i < 100; i++) {
                tph.ping();
            }

            message = tph.sendAndReceiveSingle("getPingCount", "pingCount", null);
            assertEquals(2, message.senderMessageNumber());
            assertEquals(2, message.receiverMessageNumber());
            assertEquals(100L, ((Map<String, Long>) message.json()).get(tph.getUUID()));

            message = tph.sendAndReceiveSingle("getPingCount", "pingCount", null);
            assertEquals(3, message.senderMessageNumber());
            assertEquals(3, message.receiverMessageNumber());
            assertEquals(100L, ((Map<String, Long>) message.json()).get(tph.getUUID()));
        });
    }

    @Test
    public void unknownTest() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            tph.send("unknown", "@@@");
            Thread.sleep(50);
            Message unknown = tph.peer.getLastMessageSingle("unknown");
            assertNotNull(unknown);
            assertEquals(unknown.keyword(), "unknown");
            assertEquals(unknown.json(), "@@@");
        });
    }

    @Test
    public void magicTest() {
        //noinspection CodeBlock2Expr
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            assertEquals(4711L, tph.getMagic());
        });
    }

    @Test
    public void shutdownTest() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            tph.ping();
            tph.ping();
            tph.ping();
            tph.ping();
            Thread.sleep(50);
            tph.shutdown();
            Thread.sleep(50);
            Assertions.assertTrue(tph.isShutdown());
        });
    }

    @Test
    public void pingerTest() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            tph.waitForSinglePeer();
            assertEquals(0, tph.getMyPingCount());
            tph.startPingerOnPeer();
            Thread.sleep(500);
            long myPingCount    = tph.getMyPingCount();
            long MIN_PING_COUNT = 40;
            if (myPingCount < MIN_PING_COUNT) {
                fail("ping count is " + myPingCount + " but it should be above " + MIN_PING_COUNT);
            } else {
                System.err.println("ping count is " + myPingCount + " correctly above " + MIN_PING_COUNT);
            }
        });
    }
}
