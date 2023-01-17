//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2023 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
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

import java.util.function.Consumer;

public interface MessageHandler {
    String requestKey();

    String answerKey();

    void handle(Message message);

    static MessageHandler of(String key, Consumer<Message> h) {
        return new MessageHandler() {
            @Override
            public String requestKey() {
                return key;
            }

            @Override
            public String answerKey() {
                return null;
            }

            @Override
            public void handle(Message message) {
                h.accept(message);
            }
        };
    }

    static MessageHandler of(String key, String answerKey, Consumer<Message> h) {
        return new MessageHandler() {
            @Override
            public String requestKey() {
                return key;
            }

            @Override
            public String answerKey() {
                return answerKey;
            }

            @Override
            public void handle(Message message) {
                h.accept(message);
            }
        };
    }
}
