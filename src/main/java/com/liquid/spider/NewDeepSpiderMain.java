package com.liquid.spider;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.enmus.ExcelType;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.liquid.spider.constant.CommonConstant;
import com.liquid.spider.constant.DeepConstant;
import com.liquid.spider.pojo.DeepOrderData;
import com.liquid.spider.pojo.DeepOrderLIstQO;
import com.liquid.spider.pojo.DeepUserTokenInfo;
import com.liquid.spider.utils.CommonPropertiesUtil;
import com.liquid.spider.utils.DeepSystemPropertiesUtil;
import com.liquid.spider.utils.DeepUserPropertiesUtil;
import com.liquid.spider.utils.WebDriverUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Actions;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class NewDeepSpiderMain {

    public static List<DeepOrderData> orderDataList = new ArrayList<>();

    public static int loadedOrderCount = 0;

    public static String accountName ="";

    public static String token;

    public static void start() throws Exception {
        System.out.println("=======================??????????????????==========================");
        System.setProperty(CommonPropertiesUtil.get(CommonConstant.WEB_DRIVER), CommonPropertiesUtil.get(CommonConstant.WEB_DRIVER_VALUE));
        //option.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        FirefoxDriver driver = new FirefoxDriver();
        //driver.navigate().to("https://www.tcpjw.com/passport/login?referer=https%3A%2F%2Fwww.tcpjw.com%2F");
        //??????????????????
        while (true) {
            try {
                driver.get(DeepSystemPropertiesUtil.get(DeepConstant.LOGIN_URL)); // todo ????????????
                //????????????selenium-java?????????????????????????????????
                ((JavascriptExecutor) driver).executeScript("Object.defineProperties(navigator,{ webdriver:{ get: () => false } })");
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            break;
        }

        //?????????????????????
        WebElement accountElement = WebDriverUtil.getElement(driver, By.ByXPath.xpath(DeepSystemPropertiesUtil.get(DeepConstant.LOGIN_ACCOUNT_INPUT))); // todo ????????????
        accountElement.clear(); // ????????????
        accountElement.sendKeys(DeepUserPropertiesUtil.get(DeepConstant.ACCOUNT)); // ????????????
        TimeUnit.MILLISECONDS.sleep(500);
        WebElement passwordElement = WebDriverUtil.getElement(driver, By.ByXPath.xpath(DeepSystemPropertiesUtil.get(DeepConstant.LOGIN_PASSWORD_INPUT))); // todo ????????????
        passwordElement.clear(); // ????????????
        passwordElement.sendKeys(DeepUserPropertiesUtil.get(DeepConstant.PASSWORD)); // ????????????

        //???????????? ,??????????????? 40 * 34  ,?????????????????????380 * 34 ?????????????????????????????????380-40 = 340
        WebElement scaleElement = WebDriverUtil.getElement(driver, By.ByXPath.xpath(DeepSystemPropertiesUtil.get(DeepConstant.SCALE_INPUT))); // todo ????????????

        Actions action = new Actions(driver);
        // ????????????
        TimeUnit.SECONDS.sleep(1);
        action.dragAndDropBy(scaleElement, 340, 0).perform();
        TimeUnit.MILLISECONDS.sleep(500);


        //??????????????????
        WebElement submitButton = WebDriverUtil.getElement(driver, By.ByXPath.xpath(DeepSystemPropertiesUtil.get(DeepConstant.SUBMIT_BUTTON))); // todo ????????????
        submitButton.click();
        //1

        TimeUnit.SECONDS.sleep(5);

        String loginUserInfo = driver.getLocalStorage().getItem("F_user");
        System.out.println("f_user: " + loginUserInfo);
        DeepUserTokenInfo tokenInfo = JSONObject.parseObject(loginUserInfo, DeepUserTokenInfo.class);
        token = tokenInfo.getToken();
        // ????????????????????????
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost httpPost = new HttpPost("https://sdpjw.cn/order/api/v1/orderList/buyer");

        // ????????????
        CloseableHttpResponse response = null;
        try {
            // ??????????????????(??????)Get??????
            httpPost.addHeader("token",token);
            httpPost.addHeader("Content-Type", "application/json");
            DeepOrderLIstQO listQueryQO = new DeepOrderLIstQO();
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            Date startDate = format.parse("2021-11-30");
            //listQueryQO.setStartTime(format.format(new Date()));
            //listQueryQO.setEndTime(format.format(new Date()));
            listQueryQO.setStartTime("2021-11-30");
            listQueryQO.setEndTime("2021-11-30");
            HttpEntity entity = new StringEntity(JSONObject.toJSONString(listQueryQO), "UTF-8");
            httpPost.setEntity(entity);
            response = httpClient.execute(httpPost);
            // ????????????????????????????????????
            HttpEntity responseEntity = response.getEntity();
            String listResultJson = EntityUtils.toString(responseEntity);
            JSONObject listJson = JSONObject.parseObject(listResultJson);
            JSONObject data = (JSONObject)listJson.get("data");
            JSONArray list = (JSONArray)data.get("list");
            if(list.isEmpty()){
                System.out.println("???????????????????????????excel");
            }else{
                list.stream().forEach(d ->{
                    try {
                        TimeUnit.SECONDS.sleep(1);
                        JSONObject order = (JSONObject)d;
                        DeepOrderData deepOrderData = readOrderData(httpClient, order.getString("id"));
                        String finishTime = readFinishTime(httpClient, order.getString("id"));
                        deepOrderData.setFinishDate(finishTime);
                        orderDataList.add(deepOrderData);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            System.out.println("=====================================================");
            int dataSize = orderDataList.size();
            //??????excel
            if(dataSize > 0) {
                List<DeepOrderData> orderDataListSorted = NewDeepSpiderMain.orderDataList.stream().sorted(Comparator.comparing(DeepOrderData::getFinishDate)
                ).collect(Collectors.toList());
                Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(null, accountName, ExcelType.XSSF), DeepOrderData.class, orderDataListSorted);
                String excelName = tokenInfo.getCorpName()+"_??????_" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".xlsx";
                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream("./excels/" + excelName));
                workbook.write(outputStream);
                outputStream.close();
                workbook.close();
                System.out.println("????????????"+ dataSize +"???????????????????????????excels????????????????????????"+excelName);
            }else{
                System.out.println("???????????????????????????excel");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // ????????????
                if (httpClient != null) {
                    httpClient.close();
                }
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static DeepOrderData readOrderData(HttpClient client, String id) throws IOException {
        HttpGet httpGet = new HttpGet("https://sdpjw.cn/order/api/v1/draft/buyer/"+id);
        httpGet.addHeader("token",token);
        httpGet.addHeader("Content-Type", "application/json");
        HttpResponse response = client.execute(httpGet);
        HttpEntity httpEntity = response.getEntity();
        String order = EntityUtils.toString(httpEntity);
        JSONObject dataJson = JSONObject.parseObject(order);
        JSONObject orderData = (JSONObject)dataJson.get("data");

        DeepOrderData deepOrderData = DeepOrderData.builder()
                .acceptor(orderData.getString("acceptance"))
                .billNo(orderData.getString("draftNo"))
                .amount(formatAmount(orderData.getString("draftAmt")))  // ?????????????????????????????????
                .deposit(formatAmount(orderData.getString("sellerReceivableAmt"))) // ?????????
                .endDate(orderData.getString("expiryDate")+"(???"+orderData.getString("discountDays")+"???)")
                .payAmount(formatAmount(orderData.getString("sellerReceivableAmt")))  // ?????????????????????????????????
                .yearRate(orderData.getDouble("annualInterest")/10000+"%")
                .build();

        return deepOrderData;
    }

    public static String readFinishTime(HttpClient client, String id)throws IOException{
        HttpGet httpGet = new HttpGet("https://sdpjw.cn/order/api/v1/getOrderTiming?draftId="+id);
        httpGet.addHeader("token",token);
        httpGet.addHeader("Content-Type", "application/json");
        HttpResponse response = client.execute(httpGet);
        HttpEntity httpEntity = response.getEntity();
        String order = EntityUtils.toString(httpEntity);
        JSONObject dataJson = JSONObject.parseObject(order);
        JSONObject orderData = (JSONObject)dataJson.get("data");
        return orderData.getString("signinTime");
    }

    public static String formatAmount(String amountStr){
        amountStr = amountStr.toUpperCase();
        double amount = 0;
        if (amountStr.contains("E")) {
            String[] amountSplit = amountStr.split("E");
            String pow = amountSplit[1];
            double p = Math.pow(10, new Double(pow));
            amount =  new Double(amountSplit[0]) * p;
        }else{
            amount = new Double(amountStr);
        }
        amount = amount / 1000000; // ???1W
        return amount +"???";
    }

    public static void main(String[] args) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.format(new Date());
        System.out.println(format.format(new Date()));
    }
}
