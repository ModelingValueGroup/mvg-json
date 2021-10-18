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

import org.modelingvalue.json.*;
import org.modelingvalue.json.protocol.Message.*;

import java.io.*;
import java.net.*;
import java.rmi.*;
import java.util.*;
import java.util.Map.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

public class ProtocolHandler {
    public static final boolean TRACE                    = true;
    public static final String  REMOTE_ERROR_MESSAGE_KEY = "$remote_error";
    public static final String  PEER_ENTER_MESSAGE_KEY   = "$peer_enter";
    public static final String  PEER_LEAVE_MESSAGE_KEY   = "$peer_leave";
    public static final char    PROTOCOL_SEPARATOR       = ':';
    public static final char    PROTOCOL_TERMINATOR      = '\n';

    private final String                            uuid                      = UUID.randomUUID().toString();
    private final Map<String, MessageHandler>       handlerMap                = new HashMap<>();
    private final AtomicLong                        nextSendMessageNumber     = new AtomicLong();
    private final AtomicLong                        nextReceivedMessageNumber = new AtomicLong();
    private final List<Throwable>                   problems                  = new ArrayList<>();
    private final Map<String, String>               peerMap                   = new HashMap<>();
    private final Map<String, Map<String, Message>> lastMessagesMap           = new HashMap<>();
    private final String                            name;
    private final BufferedWriter                    out;
    private final IncomingMessagesThread            inThread;

    public static ProtocolHandler of(String host, int port) throws IOException {
        Socket          socket          = new Socket(host, port);
        OutputStream    out             = socket.getOutputStream();
        InputStream     in              = socket.getInputStream();
        ProtocolHandler protocolHandler = new ProtocolHandler("PH:" + socket.getRemoteSocketAddress().toString(), in, out);
        protocolHandler.start();
        return protocolHandler;
    }

    public ProtocolHandler(String name, InputStream in, OutputStream out) {
        this.name = name;
        this.out  = new BufferedWriter(new OutputStreamWriter(out));
        add(MessageHandler.of(REMOTE_ERROR_MESSAGE_KEY, m -> addProblem(new RemoteException(((Map<?, ?>) m.json()).get("message").toString()))));
        add(MessageHandler.of(PEER_ENTER_MESSAGE_KEY, this::peerEnter));
        add(MessageHandler.of(PEER_LEAVE_MESSAGE_KEY, this::peerLeave));
        inThread = new IncomingMessagesThread(name, in);
        send_peer_enter();
    }

    public void start() {
        inThread.start();
    }

    protected void peerLeave(Message m) {
        synchronized (peerMap) {
            peerMap.remove(m.senderUuid());
        }
    }

    protected void peerEnter(Message m) {
        synchronized (peerMap) {
            peerMap.put(m.senderUuid(), (String) m.json());
        }
    }

    public String getUUID() {
        return uuid;
    }

    public void shutdown() throws IOException {
        send_peer_leave();
        inThread.shutdown();
        out.close();
    }

    protected void shutdownDone() {
    }

    protected boolean isShutdown() {
        return !inThread.isAlive();
    }

    public void add(MessageHandler h) {
        String keyword = h.requestKey();
        synchronized (handlerMap) {
            if (handlerMap.containsKey(keyword)) {
                throw new Error("ProtocolHandler: cannot add MessageHandler: keyword is already mapped to a handler: '" + keyword + "'");
            }
            handlerMap.put(keyword, h);
        }
    }

    public void remove(String keyword) {
        synchronized (handlerMap) {
            if (!handlerMap.containsKey(keyword)) {
                throw new Error("ProtocolHandler: cannot remove MessageHandler: keyword is not mapped to a handler: '" + keyword + "'");
            }
            handlerMap.remove(keyword);
        }
    }

    public MessageHandler getMessageHandler(String keyword) {
        synchronized (handlerMap) {
            return handlerMap.get(keyword);
        }
    }

    public Map<String, String> getPeerMap() {
        synchronized (peerMap) {
            return new HashMap<>(peerMap);
        }
    }

    private void addProblem(Throwable problem) {
        synchronized (problems) {
            if (TRACE) {
                System.err.println("addProblem: " + (problems.size() + 1) + ": " + problem.getMessage() + " [" + Thread.currentThread().getName() + " - " + System.identityHashCode(this) + "]");
            }
            problems.add(problem);
        }
    }

    public void throwIfProblems() {
        synchronized (problems) {
            if (TRACE) {
                System.err.println("throwIfProblems: " + problems.size() + " [" + Thread.currentThread().getName() + " - " + System.identityHashCode(this) + "]");
            }
            if (!problems.isEmpty()) {
                throw new Error("problem in ProtocolHandler", problems.remove(0));
            }
        }
    }

    protected <T> Map<String, T> getMulti(String keyword, String anwerKeyWord, Function<Object, T> f) {
        return sendAndReceiveMulti(keyword, anwerKeyWord, null)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Entry::getKey, e -> f.apply(e.getValue().json())));
    }

    protected static <T> T getSingle(Map<String, T> map) {
        int size = map.size();
        if (size != 1) {
            throw new Error("should have exactly 1 peer but have " + size);
        }
        return map.values().stream().findFirst().get();
    }

    private void send_remote_error(Throwable problem) {
        send(REMOTE_ERROR_MESSAGE_KEY, problem);
    }

    private void send_peer_enter() {
        send(PEER_ENTER_MESSAGE_KEY, name);
    }

    private void send_peer_leave() {
        send(PEER_LEAVE_MESSAGE_KEY, name);
    }

    public void send(String keyword, Object payload) {
        synchronized (out) {
            try {
                String msg = keyword + PROTOCOL_SEPARATOR
                             + uuid + PROTOCOL_SEPARATOR
                             + nextSendMessageNumber.getAndIncrement() + PROTOCOL_SEPARATOR
                             + ToJson.toJson(payload) + PROTOCOL_TERMINATOR;
                if (TRACE) {
                    System.err.printf(">>>%-50s>>>    SEND: %s", Thread.currentThread().getName(), String.format("%" + (20 - msg.indexOf(PROTOCOL_SEPARATOR)) + "s%s", "", msg));
                }
                out.write(msg);
                out.flush();
            } catch (IOException e) {
                throw new Error("problem during send()", e);
            }
        }
    }

    private void received(Message message) {
        synchronized (lastMessagesMap) {
            Map<String, Message> peer2messageMap = lastMessagesMap.computeIfAbsent(message.keyword(), k -> new HashMap<>());
            peer2messageMap.put(message.senderUuid(), message);
        }

        MessageHandler h = getMessageHandler(message.keyword());
        if (h != null) {
            h.handle(message);
        }
    }

    public Map<String, Message> getLastMessageMulti(String keyword) {
        synchronized (lastMessagesMap) {
            return lastMessagesMap.computeIfAbsent(keyword, k -> new HashMap<>());
        }
    }

    public Message getLastMessage(String keyword, String peerUuid) {
        return getLastMessageMulti(keyword).get(peerUuid);
    }

    public Message getLastMessageSingle(String keyword) {
        if (getPeerMap().size() != 1) {
            throw new Error("this is not a P2P connection");
        }
        return getLastMessageMulti(keyword).values().stream().findFirst().orElseThrow();
    }

    public Message sendAndReceiveSingle(String keyword, String answerKeyword, Object payload) {
        if (getPeerMap().size() != 1) {
            throw new Error("this is not a P2P connection");
        }
        Map<String, Message> m = sendAndReceiveMulti(keyword, answerKeyword, payload);
        if (m.size() != 1) {
            throw new Error("this is not a P2P connection");
        }
        return m.values().stream().findFirst().orElseThrow();
    }

    public Map<String, Message> sendAndReceiveMulti(String keyword, String answerKeyword, Object payload) {
        if (answerKeyword == null || answerKeyword.length() == 0) {
            throw new Error("answerKeyword should not be empty");
        }
        AnswerMessageHandler h = new AnswerMessageHandler(answerKeyword);
        add(h);
        send(keyword, payload);
        return h.awaitAnswer();
    }

    public void waitForSinglePeer() {
        waitForMultiPeer(1);
    }

    public void waitForMultiPeer(int n) {
        try {
            while (peerMap.size() != n) {
                //noinspection BusyWait
                Thread.sleep(1);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class AnswerMessageHandler implements MessageHandler {
        private final String                                  answerKeyword;
        private final CompletableFuture<Map<String, Message>> syncer    = new CompletableFuture<>();
        private       Map<String, Message>                    answerMap = new HashMap<>();

        public AnswerMessageHandler(String answerKeyword) {
            this.answerKeyword = answerKeyword;
        }

        @Override
        public String requestKey() {
            return answerKeyword;
        }

        @Override
        public String answerKey() {
            return null;
        }

        @Override
        public void handle(Message message) {
            Map<String, String> newPeerMap = getPeerMap();
            answerMap = answerMap.entrySet().stream().filter(e -> newPeerMap.containsKey(e.getKey())).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            answerMap.put(message.senderUuid(), message);
            if (answerMap.size() == newPeerMap.size()) {
                remove(answerKeyword);
                syncer.complete(answerMap);
            }
        }

        public Map<String, Message> awaitAnswer() {
            try {
                return syncer.get();
            } catch (Exception e) {
                throw new Error("problem during wait for answer", e);
            }
        }
    }

    private class IncomingMessagesThread extends Thread {
        private final BufferedReader in;
        private       boolean        stop;

        public IncomingMessagesThread(String id, InputStream in) {
            super("ProtocolHandler-" + id);
            this.in = new BufferedReader(new InputStreamReader(in));
            setDaemon(true);
        }

        public void shutdown() throws IOException {
            //System.err.println("shutdown... " + getName());
            stop = true;
            interrupt();
            in.close();
            //System.err.println("shutdown!!! " + getName());
        }

        @Override
        public void run() {
            try {
                while (!stop) {
                    try {
                        String line = in.readLine();
                        if (TRACE) {
                            System.err.printf(">>>%-50s>>>    READ: %s\n", getName(), line == null ? "<EOF>" : String.format("%" + (20 - line.indexOf(PROTOCOL_SEPARATOR)) + "s%s", "", line));
                        }
                        stop = line == null;
                        if (!stop) {
                            String[] split = line.split("" + PROTOCOL_SEPARATOR, 4);
                            if (split.length != 4) {
                                send_remote_error(new IOException("non protocol line: " + line));
                            } else {
                                received(new MessageImpl(split[0], split[1], Long.parseLong(split[2]), nextReceivedMessageNumber.getAndIncrement(), FromJson.fromJson(split[3])));
                            }
                        }
                    } catch (InterruptedIOException e) {
                        if (TRACE) {
                            System.err.printf(">>>%-50s>>>    READ: <interrupted!>\n", getName());
                        }
                        stop = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                        send_remote_error(e);
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
                try {
                    send_remote_error(e);
                } catch (Throwable e2) {
                    // out of solutions....
                    e2.printStackTrace();
                }
            }
            shutdownDone();
        }
    }
}
