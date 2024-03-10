//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//  (C) Copyright 2018-2024 Modeling Value Group B.V. (http://modelingvalue.org)                                         ~
//                                                                                                                       ~
//  Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in       ~
//  compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0   ~
//  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on  ~
//  an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the   ~
//  specific language governing permissions and limitations under the License.                                           ~
//                                                                                                                       ~
//  Maintainers:                                                                                                         ~
//      Wim Bast, Tom Brus                                                                                               ~
//                                                                                                                       ~
//  Contributors:                                                                                                        ~
//      Ronald Krijgsheld ✝, Arjan Kok, Carel Bast                                                                       ~
// --------------------------------------------------------------------------------------------------------------------- ~
//  In Memory of Ronald Krijgsheld, 1972 - 2023                                                                          ~
//      Ronald was suddenly and unexpectedly taken from us. He was not only our long-term colleague and team member      ~
//      but also our friend. "He will live on in many of the lines of code you see below."                               ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.json.protocol;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.modelingvalue.json.TestUtil;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unchecked")
public class ProtocolTests {
    private TestProtocolHandlerWithPeer tph;

    @BeforeEach
    public void beforeEach() {
        assertNull(tph);
        tph = TestProtocolHandlerWithPeer.createPipedWithPeer('\n');
    }

    @AfterEach
    public void afterEach() throws Throwable {
        assertNotNull(tph);
        if (!tph.isShutdown()) {
            tph.shutdown();
            TestUtil.assertEventually("correctly shutdown after test", () -> assertTrue(tph.isShutdown()));
        }
        tph = null;
    }

    @RepeatedTest(4)
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

    @RepeatedTest(4)
    public void manyPingTest() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            for (int i = 0; i < 100; i++) {
                tph.ping();
            }
            assertEquals(100L, tph.getPingCount());
        });
    }

    @RepeatedTest(4)
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

    @RepeatedTest(4)
    public void unknownTest() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            tph.send("unknown", "@@@");
            TestUtil.assertEventually("unknown is handled", () -> assertDoesNotThrow(() -> {
                Message unknown = tph.peer.getLastMessageSingle("unknown");
                assertNotNull(unknown);
                assertEquals(unknown.keyword(), "unknown");
                assertEquals(unknown.json(), "@@@");
            }));
        });
    }

    @RepeatedTest(4)
    public void magicTest() {
        //noinspection CodeBlock2Expr
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            assertEquals(4711L, tph.getMagic());
        });
    }

    @RepeatedTest(4)
    public void shutdownTest() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            tph.ping();
            tph.ping();
            tph.ping();
            tph.ping();
            Thread.sleep(50);

            tph.shutdown();
            TestUtil.assertEventually("shutdown done", () -> assertTrue(tph.isShutdown()));
        });
    }

    @RepeatedTest(4)
    public void pingerTest() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            TestUtil.waitForSinglePeer(tph);
            assertEquals(0, tph.getMyPingCount());
            tph.start100PingerOnPeer();

            int  target = 25;
            long t0     = System.currentTimeMillis();
            TestUtil.assertEventually("ping count at or above " + target, () -> assertTrue(target <= tph.getMyPingCount(), "ping count did not get over " + target + " in time"));
            long dt = System.currentTimeMillis() - t0;
            if (TestUtil.VERBOSE_TESTS) {
                System.err.println("info: it took " + dt + " ms to get to a ping count of " + target);
            }
        });
    }
}
