//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2022 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
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

package org.modelingvalue.json;

import org.junit.jupiter.api.function.Executable;
import org.modelingvalue.json.protocol.ProtocolHandler;
import org.opentest4j.AssertionFailedError;

import java.time.Duration;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;

@SuppressWarnings("BusyWait")
public class TestUtil {
    public static void assertEventually(String name, Executable executable) throws Throwable {
        assertEventually(name, ofSeconds(5), ofMillis(50), executable);
    }

    public static void assertEventually(String name, Duration max, Duration keep, Executable executable) throws Throwable {
        int  n  = 0;
        long t0 = System.currentTimeMillis();
        do {
            try {
                executable.execute();
                break;
            } catch (AssertionFailedError failure) {
                n++;
                Thread.sleep(1);
            }
        } while (System.currentTimeMillis() - t0 < max.toMillis());

        System.err.printf("assertEventually: it took %5d ms and %4d failures for: %s\n", System.currentTimeMillis() - t0, n, name);

        long t1 = System.currentTimeMillis();
        do {
            executable.execute();
            Thread.sleep(1);
        } while (System.currentTimeMillis() - t1 < keep.toMillis());
    }

    public static void waitForSinglePeer(ProtocolHandler ph) {
        waitForMultiPeer(1, ph);
    }

    public static void waitForMultiPeer(int n, ProtocolHandler ph) {
        try {
            while (ph.getPeerMap().size() != n) {
                //noinspection BusyWait
                Thread.sleep(1);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
