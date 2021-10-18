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

import org.junit.jupiter.api.*;
import org.modelingvalue.syncproxy.*;

import java.io.*;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
public class ProtocolTests {
    public static final int                         TEST_PORT = 25430;
    private final       TestProtocolHandlerWithPeer tph       = TestProtocolHandlerWithPeer.createPipedWithPeer();

    @AfterEach
    public void afterEach() throws IOException, InterruptedException {
        if (!tph.isShutdown()) {
            tph.shutdown();
            long t0 = System.currentTimeMillis();
            while (!tph.isShutdown() && System.currentTimeMillis() < t0 + 30_000) {
                //noinspection BusyWait
                Thread.sleep(1);
            }
            assertTrue(tph.isShutdown());
        }
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
            System.err.println("pinger at " + tph.getMyPingCount());
            assertTrue(40 < tph.getMyPingCount());
        });
    }

    @Test
    public void socketTest() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            Main proxy = new Main(TEST_PORT);

            TestProtocolHandler ph1 = TestProtocolHandler.of("localhost", TEST_PORT);
            TestProtocolHandler ph2 = TestProtocolHandler.of("localhost", TEST_PORT);
            TestProtocolHandler ph3 = TestProtocolHandler.of("localhost", TEST_PORT);

            Thread.sleep(1000);
            Map<String, String> m = ph1.getPeerMap();
            m.keySet().stream().sorted().forEach(k -> System.err.printf("  - %-20s - %s\n", k, m.get(k)));
            assertEquals(2, m.size());
            assertEquals(2, m.keySet().stream().distinct().count());
            assertEquals(2, m.values().stream().distinct().count());

            Thread.sleep(10);
            ph1.ping();
            ph2.ping();
            ph2.ping();
            ph3.ping();
            ph3.ping();
            ph3.ping();

            Thread.sleep(10);
            assertEquals(2, ph1.getMyPingCount(ph2.getUUID()));
            assertEquals(3, ph1.getMyPingCount(ph3.getUUID()));
            assertEquals(1, ph2.getMyPingCount(ph1.getUUID()));
            assertEquals(3, ph2.getMyPingCount(ph3.getUUID()));
            assertEquals(1, ph3.getMyPingCount(ph1.getUUID()));
            assertEquals(2, ph3.getMyPingCount(ph2.getUUID()));


            Thread.sleep(10);
            assertEquals(4711, ph1.getMagic(ph2.getUUID()));
            assertEquals(4711, ph1.getMagic(ph3.getUUID()));

            Thread.sleep(100);
            ph1.throwIfProblems();
            ph2.throwIfProblems();
            ph3.throwIfProblems();

            Thread.sleep(10);
            proxy.close();
        });
    }
}
