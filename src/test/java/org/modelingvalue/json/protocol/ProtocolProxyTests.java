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

import org.junit.jupiter.api.RepeatedTest;
import org.modelingvalue.syncproxy.Main;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class ProtocolProxyTests {
    private static final char[] SEPARATORS = {'@', '|', '\n', '\0', '^'};
    private static       int    separatorNumber;

    private static char nextSeparator() {
        char sep = SEPARATORS[separatorNumber++];
        if (separatorNumber == SEPARATORS.length) {
            separatorNumber = 0;
        }
        return sep;
    }

    @RepeatedTest(20)
    public void socketTest() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            char sep   = nextSeparator();
            Main proxy = new Main(0, sep, false);
            try {
                TestProtocolHandler ph1 = TestProtocolHandler.of("localhost", proxy.getPort(), sep);
                TestProtocolHandler ph2 = TestProtocolHandler.of("localhost", proxy.getPort(), sep);
                TestProtocolHandler ph3 = TestProtocolHandler.of("localhost", proxy.getPort(), sep);

                int initialDelay = 200;
                int delay        = 20;

                Thread.sleep(initialDelay);
                Map<String, String> m = ph1.getPeerMap();
                //m.keySet().stream().sorted().forEach(k -> System.err.printf("  - %-20s - %s\n", k, m.get(k)));
                assertEquals(2, m.size());
                assertEquals(2, m.keySet().stream().distinct().count());
                assertEquals(2, m.values().stream().distinct().count());

                Thread.sleep(delay);
                ph1.ping();
                ph2.ping();
                ph2.ping();
                ph3.ping();
                ph3.ping();
                ph3.ping();

                Thread.sleep(delay);
                assertEquals(2, ph1.getMyPingCount(ph2.getUUID()));
                assertEquals(3, ph1.getMyPingCount(ph3.getUUID()));
                assertEquals(1, ph2.getMyPingCount(ph1.getUUID()));
                assertEquals(3, ph2.getMyPingCount(ph3.getUUID()));
                assertEquals(1, ph3.getMyPingCount(ph1.getUUID()));
                assertEquals(2, ph3.getMyPingCount(ph2.getUUID()));


                Thread.sleep(delay);
                assertEquals(4711, ph1.getMagic(ph2.getUUID()));
                assertEquals(4711, ph1.getMagic(ph3.getUUID()));

                Thread.sleep(delay);
                ph1.throwIfProblems();
                ph2.throwIfProblems();
                ph3.throwIfProblems();

                Thread.sleep(delay);
            } finally {
                proxy.close();
            }
        });
    }
}
