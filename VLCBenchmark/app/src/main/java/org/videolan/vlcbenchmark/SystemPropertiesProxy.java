package org.videolan.vlcbenchmark;

import java.lang.reflect.Method;

/**
 *  SystemPropertiesProxy gets
 */
public class SystemPropertiesProxy {

    /**
     * This class cannot be instantiated
     */
    private SystemPropertiesProxy() {
    }

    /**
     * Get the value for the given key.
     *
     * @return an empty string if the key isn't found
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    public static String get(String key) throws IllegalArgumentException {
        String ret;
        try {
            Class<?> SystemProperties = Class.forName("android.os.SystemProperties");

            /* Parameters Types */
            Class[] paramTypes = { String.class };
            Method get = SystemProperties.getMethod("get", paramTypes);

            /* Parameters */
            Object[] params = { key };
            ret = (String) get.invoke(SystemProperties, params);
        } catch (IllegalArgumentException iAE) {
            throw iAE;
        } catch (Exception e) {
            ret = "";
        }
        return ret;
    }
}
