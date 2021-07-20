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
        System.out.println("=======================深度爬虫开始==========================");
        System.setProperty(CommonPropertiesUtil.get(CommonConstant.WEB_DRIVER), CommonPropertiesUtil.get(CommonConstant.WEB_DRIVER_VALUE));
        //option.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        FirefoxDriver driver = new FirefoxDriver();
        //driver.navigate().to("https://www.tcpjw.com/passport/login?referer=https%3A%2F%2Fwww.tcpjw.com%2F");
        //打开登录页面
        while (true) {
            try {
                driver.get(DeepSystemPropertiesUtil.get(DeepConstant.LOGIN_URL)); // todo 改成配置
                //解决使用selenium-java被检测导致滑块验证失败
                ((JavascriptExecutor) driver).executeScript("Object.defineProperties(navigator,{ webdriver:{ get: () => false } })");
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            break;
        }

        //定位账号输入框
        WebElement accountElement = WebDriverUtil.getElement(driver, By.ByXPath.xpath(DeepSystemPropertiesUtil.get(DeepConstant.LOGIN_ACCOUNT_INPUT))); // todo 改成配置
        accountElement.clear(); // 清空账号
        accountElement.sendKeys(DeepUserPropertiesUtil.get(DeepConstant.ACCOUNT)); // 设置账号
        TimeUnit.MILLISECONDS.sleep(500);
        WebElement passwordElement = WebDriverUtil.getElement(driver, By.ByXPath.xpath(DeepSystemPropertiesUtil.get(DeepConstant.LOGIN_PASSWORD_INPUT))); // todo 改成配置
        passwordElement.clear(); // 清空密码
        passwordElement.sendKeys(DeepUserPropertiesUtil.get(DeepConstant.PASSWORD)); // 设置密码

        //定位滑块 ,滑块长度为 40 * 34  ,整个滑块区域为360 * 34 ，因此计算出滑动距离为360-40 = 320
        WebElement scaleElement = WebDriverUtil.getElement(driver, By.ByXPath.xpath(DeepSystemPropertiesUtil.get(DeepConstant.SCALE_INPUT))); // todo 改成配置

        Actions action = new Actions(driver);
        // 滑动滑块
        TimeUnit.SECONDS.sleep(1);
        action.dragAndDropBy(scaleElement, 320, 0).perform();
        TimeUnit.MILLISECONDS.sleep(500);


        //点击登录按钮
        WebElement submitButton = WebDriverUtil.getElement(driver, By.ByXPath.xpath(DeepSystemPropertiesUtil.get(DeepConstant.SUBMIT_BUTTON))); // todo 改成配置
        submitButton.click();
        //
        TimeUnit.SECONDS.sleep(5);

        String loginUserInfo = driver.getLocalStorage().getItem("F_user");
        System.out.println("f_user: " + loginUserInfo);
        DeepUserTokenInfo tokenInfo = JSONObject.parseObject(loginUserInfo, DeepUserTokenInfo.class);
        token = tokenInfo.getToken();
        // 查询当日我的订单
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost httpPost = new HttpPost("https://sdpjw.cn/order/api/v1/orderList/buyer");

        // 响应模型
        CloseableHttpResponse response = null;
        try {
            // 由客户端执行(发送)Get请求
            httpPost.addHeader("token",token);
            httpPost.addHeader("Content-Type", "application/json");
            DeepOrderLIstQO listQueryQO = new DeepOrderLIstQO();
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            listQueryQO.setStartTime(format.format(new Date()));
            listQueryQO.setEndTime(format.format(new Date()));
            HttpEntity entity = new StringEntity(JSONObject.toJSONString(listQueryQO), "UTF-8");
            httpPost.setEntity(entity);
            response = httpClient.execute(httpPost);
            // 从响应模型中获取响应实体
            HttpEntity responseEntity = response.getEntity();
            String listResultJson = EntityUtils.toString(responseEntity);
            JSONObject listJson = JSONObject.parseObject(listResultJson);
            JSONObject data = (JSONObject)listJson.get("data");
            JSONArray list = (JSONArray)data.get("list");
            if(list.isEmpty()){
                System.out.println("没有数据，不需要导excel");
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
            //导出excel
            if(dataSize > 0) {
                List<DeepOrderData> orderDataListSorted = NewDeepSpiderMain.orderDataList.stream().sorted(Comparator.comparing(DeepOrderData::getFinishDate)
                ).collect(Collectors.toList());
                Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(null, accountName, ExcelType.XSSF), DeepOrderData.class, orderDataListSorted);
                String excelName = tokenInfo.getCorpName()+"_深度_" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".xlsx";
                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream("./excels/" + excelName));
                workbook.write(outputStream);
                outputStream.close();
                workbook.close();
                System.out.println("成功爬取"+ dataSize +"条记录，文件存放在excels目录，文件名是："+excelName);
            }else{
                System.out.println("没有数据，不需要导excel");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // 释放资源
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
                .amount(formatAmount(orderData.getString("draftAmt")))  // 以分为单位。需要转成万
                .deposit(orderData.getString("sellerReceivableAmt")) // 保证金
                .endDate(orderData.getString("expiryDate")+"(剩"+orderData.getString("discountDays")+"天)")
                .payAmount(formatAmount(orderData.getString("sellerReceivableAmt")))  // 以分为单位。需要转成万
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
        amount = amount / 1000000; // 除1W
        return amount +"万";
    }

    public static void main(String[] args) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.format(new Date());
        System.out.println(format.format(new Date()));
    }
}
