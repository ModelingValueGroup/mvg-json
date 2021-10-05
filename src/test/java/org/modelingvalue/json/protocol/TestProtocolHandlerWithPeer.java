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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

class TestProtocolHandlerWithPeer extends TestProtocolHandler {
    public static TestProtocolHandlerWithPeer createPipedWithPeer() {
        try {
            PipedInputStream  in      = new PipedInputStream();
            PipedInputStream  inPeer  = new PipedInputStream();
            PipedOutputStream out     = new PipedOutputStream(inPeer);
            PipedOutputStream outPeer = new PipedOutputStream(in);
            return new TestProtocolHandlerWithPeer(in, out, inPeer, outPeer);
        } catch (IOException e) {
            throw new Error("problem during creation", e);
        }
    }

    public final TestProtocolHandler peer;

    public TestProtocolHandlerWithPeer(InputStream in, OutputStream out, InputStream inPeer, OutputStream outPeer) {
        super("test", in, out);
        peer = new TestProtocolHandler("peer", inPeer, outPeer);
        waitForSinglePeer();
        peer.waitForSinglePeer();
    }

    public void startPingerOnPeer() {
        peer.startPinger();
    }

    public boolean isShutdown() {
        return super.isShutdown() && peer.isShutdown();
    }

}
