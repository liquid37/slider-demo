package com.liquid.spider.utils;

import java.io.*;
import java.util.Properties;

public class SameCitySystemPropertiesUtil {

    private static Properties sameCityProperties = new Properties();

    static{
        try {
            //
            //InputStream sameCityResource = new BufferedInputStream(new FileInputStream("./config/same_city.properties"));
            InputStream sameCityResource = SameCitySystemPropertiesUtil.class.getResourceAsStream("/config/same_city.properties");
            sameCityProperties.load(sameCityResource);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String get(String key){
        return sameCityProperties.getProperty(key);
    }


}
