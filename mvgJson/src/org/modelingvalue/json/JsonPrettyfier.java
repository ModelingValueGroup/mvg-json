//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2019 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
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

import java.util.HashMap;
import java.util.Map;

public class JsonPrettyfier {
    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static String terse(String json) {
        return new JsonPrettyfier(json).get();
    }

    public static String pretty(String json) {
        return pretty(json, "  ", "\n");
    }

    public static String pretty(String json, String indent, String eol) {
        return new JsonPrettyfier(terse(json), indent, eol).get();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    private final String               indentString;
    private final String               eolString;
    private final char[]               charArray;
    private final StringBuilder        b           = new StringBuilder();
    private final Map<Integer, String> indentCache = new HashMap<>();
    private final boolean              indentEnabled;
    private       int                  indent;
    private       boolean              inQuote;

    private JsonPrettyfier(String json) {
        this(json, null, null);
    }

    private JsonPrettyfier(String json, String indentString, String eolString) {
        this.indentString = indentString;
        this.eolString = eolString;
        charArray = json.toCharArray();
        indentEnabled = indentString != null;
        if ((indentString == null) != (eolString == null)) {
            throw new Error("Json: indent and eol must both be set or not set");
        }
    }


    private String get() {
        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            if (inQuote) {
                switch (c) {
                case '\\':
                    b.append(c);
                    c = charArray[++i];
                    b.append(c);
                    break;
                case '"':
                    b.append(c);
                    inQuote = false;
                    break;
                default:
                    b.append(c);
                }
            } else {
                switch (c) {
                case '"':
                    b.append(c);
                    inQuote = true;
                    break;
                case '{':
                case '[':
                    b.append(c);
                    appendIndent(+1);
                    break;
                case '}':
                case ']':
                    appendIndent(-1);
                    b.append(c);
                    break;
                case ',':
                    b.append(c);
                    appendIndent(0);
                    break;
                case ':':
                    b.append(": ");
                    break;
                case ' ':
                case '\r':
                case '\n':
                case '\t':
                    break;
                default:
                    b.append(c);
                }
            }
        }
        return b.toString();
    }

    private void appendIndent(int move) {
        if (indentEnabled) {
            indent += move;
            String toAppend = indentCache.computeIfAbsent(indent, this::makeIndent);
            String toMatch  = indentCache.computeIfAbsent(indent + 1, this::makeIndent);

            // do not indent if we just had an indent (of one higher)
            int matchLength = toMatch.length();
            int preLength   = b.length() - matchLength;
            if (preLength < 0 || !b.substring(preLength).equals(toMatch)) {
                b.append(toAppend);
            } else {
                b.setLength(b.length() - indentString.length());
            }
        }
    }

    private String makeIndent(int i) {
        return eolString + indentString.repeat(i);
    }
}
