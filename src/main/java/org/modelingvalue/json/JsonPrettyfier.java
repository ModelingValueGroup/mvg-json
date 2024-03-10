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

package org.modelingvalue.json;

import java.util.HashMap;
import java.util.Map;

public class JsonPrettyfier {
    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static String pretty(String json) {
        return prettify(json, "  ", "\n", " ", false);
    }

    public static String prettyStrict(String json) {
        return prettify(json, "  ", "\n", " ", true);
    }

    public static String terse(String json) {
        return prettify(json, null, null, null, true);
    }

    public static String prettify(String json, String indent, String eol, String afterColon, boolean strict) {
        return new JsonPrettyfier(json, indent, eol, afterColon, strict).get();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    private final String               indentString;
    private final String               eolString;
    private final String               colon;
    private final boolean              strict;
    private final char[]               charArray;
    private final StringBuilder        b           = new StringBuilder();
    private final Map<Integer, String> indentCache = new HashMap<>();
    private final boolean              indentEnabled;
    private       int                  indent;

    private JsonPrettyfier(String json, String indentString, String eolString, String afterColon, boolean strict) {
        this.indentString = indentString;
        this.eolString    = eolString;
        this.colon        = ":" + (afterColon == null ? "" : afterColon);
        this.strict       = strict;
        charArray         = json.toCharArray();
        indentEnabled     = indentString != null;
        if ((indentString == null) != (eolString == null)) {
            throw new RuntimeException("Json: indent and eol must both be set or not set");
        }
    }


    private String get() {
        boolean inQuote    = false;
        boolean inOneLiner = false;
        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            if (!inQuote) {
                switch (c) {
                case '"':
                    b.append(c);
                    inQuote = true;
                    break;
                case '{':
                case '[':
                    b.append(c);
                    inOneLiner = !strict && isOneLiner(i + 1);
                    if (!inOneLiner) {
                        appendIndent(+1);
                    }
                    break;
                case '}':
                case ']':
                    if (!inOneLiner) {
                        appendIndent(-1);
                    }
                    inOneLiner = false;
                    b.append(c);
                    break;
                case ',':
                    b.append(c);
                    appendIndent(0);
                    break;
                case ':':
                    b.append(colon);
                    break;
                case ' ':
                case '\r':
                case '\n':
                case '\t':
                    break;
                default:
                    b.append(c);
                }
            } else {
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
            }
        }
        return b.toString();
    }

    private boolean isOneLiner(int i) {
        boolean inQuote = false;
        for (; i < charArray.length; i++) {
            char c = charArray[i];
            if (!inQuote) {
                switch (c) {
                case '"':
                    inQuote = true;
                    break;
                case '{':
                case '[':
                case ',':
                    return false;
                case '}':
                case ']':
                    return true;
                }
            } else {
                switch (c) {
                case '\\':
                    ++i;
                    break;
                case '"':
                    inQuote = false;
                    break;
                }
            }
        }
        return false;
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
