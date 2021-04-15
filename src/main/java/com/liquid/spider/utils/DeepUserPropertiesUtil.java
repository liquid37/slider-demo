package com.liquid.spider.utils;

import java.io.*;
import java.util.Properties;

public class DeepUserPropertiesUtil {

    private static Properties deepProperties = new Properties();

    static{
        try {
            InputStream sameCityResource = new BufferedInputStream(new FileInputStream("./config/deep.properties"));
            deepProperties.load(sameCityResource);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String get(String key){
        return deepProperties.getProperty(key);
    }

}
