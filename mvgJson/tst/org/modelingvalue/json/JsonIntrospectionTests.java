package org.modelingvalue.json;

import static org.modelingvalue.json.Json.*;

import org.junit.jupiter.api.*;

public class JsonIntrospectionTests {
    public static class ProbeA {
        int aap01 = 1;
        private                int aap02 = 2;
        protected              int aap03 = 3;
        public                 int aap04 = 4;
        //
        static                 int aap05 = 5;
        static private         int aap06 = 6;
        static protected       int aap07 = 7;
        static public          int aap08 = 8;
        //
        final                  int aap09 = 9;
        final private          int aap10 = 10;
        final protected        int aap11 = 11;
        final public           int aap12 = 12;
        //
        final static           int aap13 = 13;
        final static private   int aap14 = 14;
        final static protected int aap15 = 15;
        final static public    int aap16 = 15;

        int aap99 = 99;
    }

    public static class ProbeB extends ProbeA {
        int aap01 = 101;
        private                int aap02 = 102;
        protected              int aap03 = 103;
        public                 int aap04 = 104;
        //
        static                 int aap05 = 105;
        static private         int aap06 = 106;
        static protected       int aap07 = 107;
        static public          int aap08 = 108;
        //
        final                  int aap09 = 109;
        final private          int aap10 = 1010;
        final protected        int aap11 = 1011;
        final public           int aap12 = 1012;
        //
        final static           int aap13 = 1013;
        final static private   int aap14 = 1014;
        final static protected int aap15 = 1015;
        final static public    int aap16 = 1015;

        ProbeA probea = new ProbeA();

        String getName() {
            return "xyzzy";
        }
    }

    @RepeatedTest(1)
    public void listsToJson() {
        Assertions.assertEquals("{" +
                        "\"aap01\":101," +
                        "\"aap02\":102," +
                        "\"aap03\":103," +
                        "\"aap04\":104," +
                        //05
                        //06
                        //07
                        //08
                        "\"aap09\":109," +
                        "\"aap10\":1010," +
                        "\"aap11\":1011," +
                        "\"aap12\":1012," +
                        //13
                        //14
                        //15
                        //16
                        "\"aap99\":99," +
                        "\"getName\":\"xyzzy\"," +
                        "\"probea\":{" +
                        /**/"\"aap01\":1," +
                        /**/"\"aap02\":2," +
                        /**/"\"aap03\":3," +
                        /**/"\"aap04\":4," +
                        /**/"\"aap09\":9," +
                        /**/"\"aap10\":10," +
                        /**/"\"aap11\":11," +
                        /**/"\"aap12\":12," +
                        /**/"\"aap99\":99" +
                        "}" +
                        "}",
                toJson(new ProbeB()));
    }
}
