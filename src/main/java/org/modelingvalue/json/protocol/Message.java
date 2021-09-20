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

public interface Message {
    String keyword();

    String senderUuid();

    long senderMessageNumber();

    long receiverMessageNumber();

    Object json();

    class MessageImpl implements Message {
        private final String keyWord;
        private final String senderUuid;
        private final long   senderMessageNumber;
        private final long   receiverMessageNumber;
        private final Object json;

        public MessageImpl(String keyWord, String senderUuid, long senderMessageNumber, long receiverMessageNumber, Object json) {
            this.keyWord               = keyWord;
            this.senderUuid            = senderUuid;
            this.senderMessageNumber   = senderMessageNumber;
            this.receiverMessageNumber = receiverMessageNumber;
            this.json                  = json;
        }

        @Override
        public String keyword() {
            return keyWord;
        }

        @Override
        public String senderUuid() {
            return senderUuid;
        }

        @Override
        public long senderMessageNumber() {
            return senderMessageNumber;
        }

        @Override
        public long receiverMessageNumber() {
            return receiverMessageNumber;
        }

        @Override
        public Object json() {
            return json;
        }
    }
}
