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

import org.junit.jupiter.api.RepeatedTest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonRandomTests {
    @RepeatedTest(10)
    public void oneBigObjectToJson() {
        long   t0;
        long   dt;
        String json1;
        Object copy1;
        String json2;
        Object copy2;
        long   chars;
        do {
            Object original = randomObject(14);
            t0    = System.currentTimeMillis();
            json1 = Json.toJson(original);
            copy1 = Json.fromJson(json1);
            json2 = Json.toJson(copy1);
            copy2 = Json.fromJson(json2);
            chars = json1.length();
            dt    = System.currentTimeMillis() - t0;

            assertEquals(copy1, copy2);
            assertEquals(json1, json2);
        } while (dt < 100); // the random object can be so small that the time is too low for proper measuring... so just try again

        double mbps = (1000.0 * chars) / (1024.0 * 1024.0 * dt);
        System.err.printf("oneBigObjectToJson-test:        %7.2f Mb/s json (handling %5.1f Mb)\n", mbps, chars / (1024.0 * 1024.0));
        assertTrue(2.0 < mbps, "<2 Mb/s (you either have a very slow machine or some change has impacted the performance)");
    }

    @RepeatedTest(4)
    public void manySmallObjectsToJson() {
        long t0    = System.currentTimeMillis();
        long chars = 0;
        int  i     = 0;
        while (System.currentTimeMillis() < t0 + 5_000) {
            Object original = randomObject(5);
            String json1    = Json.toJson(original);
            Object copy1    = Json.fromJson(json1);
            String json2    = Json.toJson(copy1);
            Object copy2    = Json.fromJson(json2);

            assertEquals(copy1, copy2);
            assertEquals(json1, json2);
            chars += json1.length();
            i++;
        }
        long   dt   = System.currentTimeMillis() - t0;
        double mbps = (1000.0 * chars) / (1024.0 * 1024.0 * dt);
        System.err.printf("manySmallObjectsToJson-test:    %7.2f Mb/s json (in %6d runs in %d ms)\n", mbps, i, dt);
        assertTrue(1.0 < mbps, "<1 Mb/s (you either have a very slow machine or some change has impacted the performance)");
    }

    private static final Random random = new Random(4711);

    public static Object randomObject(int depth) {
        if (((double) depth) < random.nextDouble()) {
            switch (random.nextInt(9)) {
            case 0: // boolean
                return random.nextBoolean();
            case 1: // char
                return (char) random.nextInt(Character.MAX_VALUE + 1);
            case 2: // byte
                return (byte) random.nextInt(Byte.MAX_VALUE + 1);
            case 3: // short
                return (short) random.nextInt(Short.MAX_VALUE + 1);
            case 4: // int
                return random.nextInt();
            case 5: // long
                return random.nextLong();
            case 6: // float
                return random.nextFloat();
            case 7:  // double
                return random.nextDouble();
            case 8: // String
                return randomString();
            }
        } else if (random.nextBoolean()) {
            Map<String, Object> m = new HashMap<>();
            for (int i = 0; i < random.nextInt(10); i++) {
                m.put(randomString(), randomObject(depth - 1));
            }
            return m;
        } else {
            List<Object> l = new ArrayList<>();
            for (int i = 0; i < random.nextInt(10); i++) {
                l.add(randomObject(depth - 1));
            }
            return l;
        }
        throw new RuntimeException("huh?");
    }

    private static String randomString() {
        byte[] array = new byte[random.nextInt(4)];
        random.nextBytes(array);
        return new String(array, StandardCharsets.UTF_8);
    }

}
