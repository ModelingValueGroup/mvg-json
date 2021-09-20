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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class TestProtocolHandler extends ProtocolHandler {
    public static final String PING_MESSAGE_KEY           = "ping";
    public static final String GET_PING_COUNT_MESSAGE_KEY = "getPingCount";
    public static final String PING_COUNT_MESSAGE_KEY     = "pingCount";
    public static final String GET_MAGIC_MESSAGE_KEY      = "getMagic";
    public static final String MAGIC_MESSAGE_KEY          = "magic";

    public static TestProtocolHandler of(String host, int port) throws IOException {
        Socket       socket = new Socket(host, port);
        OutputStream out    = socket.getOutputStream();
        InputStream  in     = socket.getInputStream();
        return new TestProtocolHandler("TPH:" + socket.getLocalPort() + "=>" + host + ":" + port, in, out);
    }

    private final Map<String, Long> pingCountMap = new HashMap<>();
    private       Thread            pinger;

    public TestProtocolHandler(String id, InputStream in, OutputStream out) {
        super(id, in, out);

        add(MessageHandler.of(PING_MESSAGE_KEY, m -> pingCountMap.compute(m.senderUuid(), (k, old) -> (old == null ? 0 : old) + 1)));
        add(MessageHandler.of(GET_PING_COUNT_MESSAGE_KEY, PING_COUNT_MESSAGE_KEY, m -> send(PING_COUNT_MESSAGE_KEY, pingCountMap)));
        add(MessageHandler.of(GET_MAGIC_MESSAGE_KEY, MAGIC_MESSAGE_KEY, m -> send(MAGIC_MESSAGE_KEY, Map.of("magicNumber", 4711L))));
    }

    public void ping() {
        send(PING_MESSAGE_KEY, null);
    }

    @Override
    protected void peerEnter(Message m) {
        super.peerEnter(m);
        pingCountMap.put(m.senderUuid(), 0L);
    }

    @Override
    protected void peerLeave(Message m) {
        super.peerLeave(m);
        pingCountMap.remove(m.senderUuid());
    }

    public void startPinger() {
        pinger = new Thread("pinger") {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    ping();
                }
            }
        };
        pinger.setDaemon(true);
        pinger.start();
    }

    @Override
    protected void shutdownDone() {
        if (pinger != null) {
            pinger.interrupt();
            pinger = null;
        }
    }

    //======================================================
    public Map<String, Long> getMyPingCountMap() {
        return pingCountMap;
    }

    public Map<String, Map<String, Long>> getPingCountMapMap() {
        //noinspection unchecked
        return getMulti(GET_PING_COUNT_MESSAGE_KEY, PING_COUNT_MESSAGE_KEY, json -> (Map<String, Long>) json);
    }

    public Map<String, Long> getMagicMap() {
        return getMulti(GET_MAGIC_MESSAGE_KEY, MAGIC_MESSAGE_KEY, json -> (Long) ((Map<?, ?>) json).get("magicNumber"));
    }

    //======================================================
    public long getMyPingCount(String peerUuid) {
        return getMyPingCountMap().get(peerUuid);
    }

    public Map<String, Long> getPingCountMap(String peerUuid) {
        return getPingCountMapMap().get(peerUuid);
    }

    public long getMagic(String peerUuid) {
        return getMagicMap().get(peerUuid);
    }

    //======================================================
    public long getMyPingCount() {
        return getSingle(getMyPingCountMap());
    }

    public long getPingCount() {
        Map<String, Map<String, Long>> x = getPingCountMapMap();
        return getSingle(x.values().stream().findFirst().orElseThrow());
    }

    public long getMagic() {
        return getSingle(getMagicMap());
    }
}
