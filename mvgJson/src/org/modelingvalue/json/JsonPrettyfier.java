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
