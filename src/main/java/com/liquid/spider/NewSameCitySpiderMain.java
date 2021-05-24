package com.liquid.spider;

import com.liquid.spider.constant.CommonConstant;
import com.liquid.spider.constant.SameCityConstant;
import com.liquid.spider.pojo.OrderData;
import com.liquid.spider.utils.CommonPropertiesUtil;
import com.liquid.spider.utils.SameCitySystemPropertiesUtil;
import com.liquid.spider.utils.SameCityUserPropertiesUtil;
import com.liquid.spider.utils.WebDriverUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NewSameCitySpiderMain {

    public static List<OrderData> orderDataList = new ArrayList<>();

    public static int loadedOrderCount = 0;

    public static String accountName = "";

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=======================同城爬虫开始==========================");
        System.setProperty(CommonPropertiesUtil.get(CommonConstant.WEB_DRIVER), CommonPropertiesUtil.get(CommonConstant.WEB_DRIVER_VALUE));
        //option.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        WebDriver driver = new FirefoxDriver();
        while (true){
            try{
                driver.get(SameCitySystemPropertiesUtil.get(SameCityConstant.LOGIN_URL));
                //解决使用selenium-java被检测导致滑块验证失败
                ((JavascriptExecutor) driver).executeScript("Object.defineProperties(navigator,{ webdriver:{ get: () => false } })");
            }
            catch (Exception e)
            {
                e.printStackTrace();
                continue;
            }
            break;
        }
        WebElement bodyElm = driver.findElement(By.ByTagName.tagName("body"));

        List<WebElement> inputElements = bodyElm.findElements(By.tagName("input"));
        System.out.println(inputElements.size());

    }
}
