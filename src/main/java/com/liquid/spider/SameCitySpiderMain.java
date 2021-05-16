package com.liquid.spider;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.enmus.ExcelType;
import com.liquid.spider.constant.CommonConstant;
import com.liquid.spider.constant.SameCityConstant;
import com.liquid.spider.pojo.OrderData;
import com.liquid.spider.utils.*;
import org.apache.poi.ss.usermodel.Workbook;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Actions;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SameCitySpiderMain {

    public static List<OrderData> orderDataList = new ArrayList<>();

    public static int loadedOrderCount = 0;

    public static String accountName = "";

    public static void start() throws Exception {
        System.out.println("=======================同城爬虫开始==========================");
        System.setProperty(CommonPropertiesUtil.get(CommonConstant.WEB_DRIVER), CommonPropertiesUtil.get(CommonConstant.WEB_DRIVER_VALUE));
        //option.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        WebDriver driver = new FirefoxDriver();
        //driver.navigate().to("https://www.tcpjw.com/passport/login?referer=https%3A%2F%2Fwww.tcpjw.com%2F");
        //打开登录页面
        while (true){
            try{
                driver.get(SameCitySystemPropertiesUtil.get(SameCityConstant.LOGIN_URL)); // todo 改成配置
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
        //定位账号输入框
        WebElement accountElement = WebDriverUtil.getElement(driver, By.ByXPath.xpath(SameCitySystemPropertiesUtil.get(SameCityConstant.LOGIN_ACCOUNT_INPUT))); // todo 改成配置
        accountElement.clear(); // 清空账号
        accountElement.sendKeys(SameCityUserPropertiesUtil.get(SameCityConstant.ACCOUNT)); // 设置账号
        TimeUnit.MILLISECONDS.sleep(500);
        WebElement passwordElement = WebDriverUtil.getElement(driver, By.ByXPath.xpath(SameCitySystemPropertiesUtil.get(SameCityConstant.LOGIN_PASSWORD_INPUT))); // todo 改成配置
        passwordElement.clear(); // 清空密码
        passwordElement.sendKeys(SameCityUserPropertiesUtil.get(SameCityConstant.PASSWORD)); // 设置密码

        //定位滑块 ,滑块长度为 40 * 34  ,整个滑块区域为360 * 34 ，因此计算出滑动距离为360-40 = 320
        WebElement scaleElement = WebDriverUtil.getElement(driver, By.ByXPath.xpath(SameCitySystemPropertiesUtil.get(SameCityConstant.SCALE_INPUT))); // todo 改成配置

        Actions action = new Actions(driver);
        // 滑动滑块
        TimeUnit.SECONDS.sleep(1);
        action.dragAndDropBy(scaleElement,320,0).perform();
        TimeUnit.MILLISECONDS.sleep(500);

        //点击登录按钮
        WebElement submitButton = WebDriverUtil.getElement(driver, By.ByXPath.xpath(SameCitySystemPropertiesUtil.get(SameCityConstant.SUBMIT_BUTTON))); // todo 改成配置
        submitButton.click();
        //
        TimeUnit.SECONDS.sleep(5);
        //登录成功
        //获取首页广告遮罩层
        WebElement alertContentElement = WebDriverUtil.isElementPresent(driver, By.cssSelector(SameCitySystemPropertiesUtil.get(SameCityConstant.INDEX_ALTER_CONTENT))); // todo 改成配置
        if(alertContentElement != null){
            //有广告层，关闭广告
            alertContentElement.findElement(By.xpath(SameCitySystemPropertiesUtil.get(SameCityConstant.INDEX_ALTER_CONTENT_CLOSE))).click(); // todo 改成配置
        }

        //点击用户名
        // /html/body/div[1]/div/div[1]/div/div[2]/div/div[1]
        WebElement userElement = WebDriverUtil.getElement(driver, By.ByXPath.xpath(SameCitySystemPropertiesUtil.get(SameCityConstant.USER_ELEMENT))); // todo 改成配置
        accountName = userElement.getText();
        userElement.click();

        TimeUnit.SECONDS.sleep(5);
        //成功跳转到用户数据页面
        //判断是否含有提示信息
        String currentHandles = driver.getWindowHandle();
        Set<String> handles = driver.getWindowHandles();
        for(String handle : handles){
            if(!handle.equals(currentHandles)){
                //切换窗口
                driver.switchTo().window(handle);
                TimeUnit.SECONDS.sleep(1);  // todo 改成配置
                handlerTradingHall(driver);  // 跳转到交易
            }
        }
    }


    private static void handlerTradingHall(WebDriver driver) throws Exception {

        WebElement knowButton = WebDriverUtil.isElementPresent(driver,By.cssSelector(SameCitySystemPropertiesUtil.get(SameCityConstant.TRADING_HALL_KNOW_BUTTON_CSS)));  // todo 改成配置
        if(knowButton!=null){ // 有知道了的提示框，需要关闭提示框
            knowButton.click();
        }
        //点击我的订单（买），从众多菜单中定位其中一个
        List<WebElement> items = WebDriverUtil.getElements(driver,By.cssSelector(SameCitySystemPropertiesUtil.get(SameCityConstant.TRADING_HALL_ANT_MENU_ITEM))); // todo 改成配置
        WebElement buyOrderItem = null;
        for(WebElement item : items){
            String text = item.getText();
            if(text.startsWith(SameCitySystemPropertiesUtil.get(SameCityConstant.TRADING_HALL_ITEM_NAME))){       // todo 改成配置
                buyOrderItem = item;
                break;
            }
        }
        if(buyOrderItem==null){
            System.out.println("==========没有我的订单（买）菜单");
            return;
        }
        // 点击我的订单（买）菜单
        buyOrderItem.click();
        TimeUnit.SECONDS.sleep(2);     // todo 改成配置

        //查询数据

        // 最新订单  xpath /html/body/div[1]/div/div[3]/div[2]/div[1]/div[3]/div[2]/div[2]/div[3]/div[1]/div/div/div/div/div[1]/div[1]
        // 最新订单  已完成交易xpath  /html/body/div[1]/div/div[3]/div[2]/div[1]/div[3]/div[2]/div[2]/div[4]/label[8]/span[2]
        //          数据区 cssSelector .buy-empty-list

        // 历史订单  xpath /html/body/div[1]/div/div[3]/div[2]/div[1]/div[3]/div[2]/div[2]/div[3]/div[1]/div/div/div/div/div[1]/div[2]
        // 历史订单  已完成交易xpath  /html/body/div[1]/div/div[3]/div[2]/div[1]/div[3]/div[2]/div[2]/div[4]/label[2]/span[2]

        // 订单tag 这里选择最新订单
        WebElement orderTagElement = WebDriverUtil.getElement(driver,By.ByXPath.xpath(SameCitySystemPropertiesUtil.get(SameCityConstant.ORDER_XPATH)));
        orderTagElement.click();
        //选择交易完成条件
        WebElement orderFinishElement = WebDriverUtil.getElement(driver,By.ByXPath.xpath(SameCitySystemPropertiesUtil.get(SameCityConstant.ORDER_FINISH_XPATH)));
        orderFinishElement.click();
        TimeUnit.SECONDS.sleep(3);

        //输入统计日期
        List<WebElement> queryTimeElements = driver.findElements(By.cssSelector(".ant-calendar-range-picker-input"));
        queryTimeElements.get(0).click();
        TimeUnit.MILLISECONDS.sleep(100);
        //点击开始日期
        WebElement currentDateElement = driver.findElement(By.cssSelector(".ant-calendar-today"));

        WebElement trDateElement = WebDriverUtil.getElementFromElement(currentDateElement,By.xpath("./.."));
        List<WebElement> tdDateElements = WebDriverUtil.getElementsFromElement(trDateElement, By.tagName("td"));
        WebElement lastDateElement = null;
        for(int i=0; i<tdDateElements.size(); i++){
            if(currentDateElement.getText().equals(tdDateElements.get(i).getText())){
                if(i !=0){ // 当前日不是第一个，则前一天取i-1
                    lastDateElement = tdDateElements.get(i-1);
                }else{
                    // 如果是第一个，则取上一行的最后一个
                    // 获取日期组件
                    WebElement dateComposeElement = WebDriverUtil.getElementFromElement(trDateElement, By.xpath("./.."));
                    List<WebElement> weekElements = WebDriverUtil.getElementsFromElement(dateComposeElement, By.tagName("tr"));
                    WebElement lastWeekElement = null;
                    for(int j = 0; j<weekElements.size() ;j++ ){
                        if(weekElements.get(j).getAttribute("class").equals(trDateElement.getAttribute("class"))){
                            lastWeekElement = weekElements.get(j-1);
                            break;
                        }
                    }
                    List<WebElement> lastWeekDays = WebDriverUtil.getElementsFromElement(lastWeekElement, By.tagName("td"));
                    lastDateElement = lastWeekDays.get(lastWeekDays.size()-1);
                }
            }
        }
        ((JavascriptExecutor)driver).executeScript("arguments[0].click();",lastDateElement);  //  开始时间
        //点击结束日期
        ((JavascriptExecutor)driver).executeScript("arguments[0].click();",currentDateElement);

        //执行查询
        List<WebElement> buttons = driver.findElement(By.cssSelector(".button-area")).findElements(By.ByTagName.tagName("button"));
        WebElement queryButton = null;
        for(WebElement button : buttons){
            if("查询".equals(button.getText())){
                queryButton = button;
            }
        }
        if(queryButton !=null){
            ((JavascriptExecutor)driver).executeScript("arguments[0].click();",queryButton);
        }else{
            System.out.println("没有查询按钮");
            return;
        }

        //加载订单列表
        //todo 临时注释
        loadOrderListData(driver,1);
        System.out.println("=======================================");
        System.out.println("爬虫完成，准备生成excel");
        System.out.println("=======================================");
        //导出excel
        int dataSize = orderDataList.size();
        if(dataSize > 0) {
            List<OrderData> orderDataListSorted = orderDataList.stream().sorted((o1, o2) ->
                    o1.getFinishDate().compareTo(o2.getFinishDate())
            ).collect(Collectors.toList());
            ExportParams exportParams = new ExportParams(null, accountName, ExcelType.XSSF);
            Workbook workbook = ExcelExportUtil.exportExcel(exportParams, OrderData.class, orderDataListSorted);
            String excelName = accountName+"_同城_" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".xlsx";
            BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream("./excels/" + excelName));
            workbook.write(outputStream);
            outputStream.close();
            workbook.close();
            System.out.println("成功爬取"+ dataSize +"条记录，文件存放在excels目录，文件名是："+excelName);
        }else{
            System.out.println("没有数据，不需要导excel");
        }
        System.out.println("=======================================");
        System.out.println("完成！！！");
        System.out.println("=======================================");
    }

    /**
     * 查询订单数据
     * @param driver
     * @throws Exception
     */
    private static void loadOrderListData(WebDriver driver,int currentPage) throws InterruptedException, ParseException {
        System.out.println("正在爬第"+currentPage+"页数据");
        WebElement tableBodyElement = null;
        while (true) { // 等待加载数据完成
            //
            tableBodyElement = WebDriverUtil.getElement(driver, By.ByXPath.xpath(SameCitySystemPropertiesUtil.get(SameCityConstant.ORDER_LIST_TABLE)));
            if(tableBodyElement !=null){
                break;
            }else{
                System.out.println("正在加载第"+currentPage+"页数据");
                TimeUnit.MILLISECONDS.sleep(200);
            }
        }// 判断是否有数据
        TimeUnit.SECONDS.sleep(2);
        List<WebElement> trElements= WebDriverUtil.getElementsFromElement(tableBodyElement, By.tagName("tr"));
        int trSize = trElements.size();
        if(trSize == 0){  // 没有数据
            return;
        }
        for(int i = 1; i<=trSize; i++){
            //判断已统计的订单数量是否大于最大限制
            if(loadedOrderCount >= Integer.valueOf(SameCitySystemPropertiesUtil.get(SameCityConstant.SAME_CITY_MAX_SPIDER_RECORD))){
                return;
            }
            // /html/body/div[1]/div/div[3]/div[2]/div[1]/div[3]/div[2]/div[2]/div[6]/div/div/div/div/div/table/tbody/tr[1]/td[15]/span/button
            // 获取交易完成时间
            WebElement finishDateElement = WebDriverUtil.getElement(driver,By.xpath(SameCitySystemPropertiesUtil.get(SameCityConstant.ORDER_LIST_FINISH_DATE).replace("{1}",i+"")));
            Date finishDate = DateUtil.getDate(finishDateElement.getText().split("\\n")[0]);
            if(DateUtil.sameDay(new Date(),finishDate)) { // 判断订单完成日期是否为今天
                TimeUnit.MILLISECONDS.sleep(200);
                WebElement detailButton = WebDriverUtil.getElement(driver, By.ByXPath.xpath(SameCitySystemPropertiesUtil.get(SameCityConstant.ORDER_DETAIL_BUTTON).replace("{1}", i + "")));

                //detailButton.click();
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", detailButton);
                driver.switchTo().activeElement();

                loadDetailOrderDate(driver);
                //关闭窗口
                WebDriverUtil.getElement(driver, By.ByXPath.xpath(SameCitySystemPropertiesUtil.get(SameCityConstant.ORDER_DETAIL_CLOSE_BUTTON))).click();
                TimeUnit.MILLISECONDS.sleep(100);

                loadedOrderCount++; // 统计已加载的订单数量
            }

        }

        // 判断是否到了最后一页。
        WebElement pageInfoElement = WebDriverUtil.getElement(driver, By.cssSelector(SameCitySystemPropertiesUtil.get(SameCityConstant.PAGE_TOTAL_INFO_CSS)));
        TimeUnit.MILLISECONDS.sleep(500);
        String totalPageText = pageInfoElement.findElements(By.ByTagName.tagName("span")).get(1).getText();
        if(currentPage >= Integer.valueOf(totalPageText)){ // 以到最后一页
            return;
        }

        WebElement nextButtonElement = WebDriverUtil.getElement(driver, By.cssSelector(SameCitySystemPropertiesUtil.get(SameCityConstant.NEXT_PAGE_CSS)))
                .findElement(By.ByTagName.tagName("button"));
        nextButtonElement.click();
        loadOrderListData(driver,++currentPage);
    }

    //加载页面数据
    private static void loadDetailOrderDate(WebDriver driver) {
        OrderData orderData = new OrderData();
        //*[@id="root"]/div/div[3]/div[2]/div[1]/div[3]/div[3]/div[2]/div/div[2]/div[2]/div[2]/table/tbody/tr[1]/td[2]
        //承兑人
        WebElement acceptorElement = WebDriverUtil.getElement(driver,By.ByXPath.xpath(SameCitySystemPropertiesUtil.get(SameCityConstant.ORDER_DETAIL_ACCEPTOR)));
        orderData.setAcceptor(acceptorElement.getText());
        // 票面金额
        WebElement amountElement = WebDriverUtil.getElement(driver,By.ByXPath.xpath(SameCitySystemPropertiesUtil.get(SameCityConstant.ORDER_DETAIL_AMOUNT)));
        orderData.setAmount(amountElement.getText().replace("万",""));
        //到期日期
        WebElement endDateElement = WebDriverUtil.getElement(driver,By.ByXPath.xpath(SameCitySystemPropertiesUtil.get(SameCityConstant.ORDER_DETAIL_END_DATE)));
        String dateElementText = endDateElement.getText();
        orderData.setEndDate(dateElementText.substring(0,10));
        // 剩余天数
        orderData.setRemainderDay(dateElementText.substring(dateElementText.indexOf("剩余")+2,dateElementText.indexOf("天")));
        //票号
        WebElement billNoElement = WebDriverUtil.getElement(driver,By.ByXPath.xpath(SameCitySystemPropertiesUtil.get(SameCityConstant.ORDER_DETAIL_BILL_NO)));
        orderData.setBillNo(billNoElement.getText());

        //年息
        WebElement yearRateElement = WebDriverUtil.getElement(driver,By.ByXPath.xpath(SameCitySystemPropertiesUtil.get(SameCityConstant.ORDER_DETAIL_YEAR_RATE)));
        String yearRateText = yearRateElement.getText();
        orderData.setYearRate(yearRateText.substring(yearRateText.indexOf("年息")+2));

        //订单号
        WebElement orderNoElement = WebDriverUtil.getElement(driver, By.ByXPath.xpath(SameCitySystemPropertiesUtil.get(SameCityConstant.ORDER_NO_XPATH)));
        String orderNo = orderNoElement.getText().split("：")[1].replace("操作记录","");
        orderData.setOrderNo(orderNo);
        //支付方式
        WebElement payTypeElement = WebDriverUtil.getElement(driver, By.ByXPath.xpath(SameCitySystemPropertiesUtil.get(SameCityConstant.PAY_TYPE_XPATH)));
        orderData.setPayType(payTypeElement.getText());

        //支付完成时间
        String finishTime = WebDriverUtil.getElement(driver, By.xpath(SameCitySystemPropertiesUtil.get(SameCityConstant.ORDER_FINISH_TIME))).getText();
        orderData.setFinishDate(new SimpleDateFormat("yyyy-MM-dd").format(new Date())+" "+finishTime);

        WebElement amountTbodyElement = WebDriverUtil.getElement(driver, By.ByXPath.xpath(SameCitySystemPropertiesUtil.get(SameCityConstant.AMOUNT_TBODY_XPATH)));

        List<WebElement> amountTableTrElements = WebDriverUtil.getElementsFromElement(amountTbodyElement, By.tagName("tr"));

        for(WebElement trElement : amountTableTrElements){
            List<WebElement> tds = WebDriverUtil.getElementsFromElement(trElement, By.ByTagName.tagName("td"));
            if(tds.get(0).getText().contains("平台服务费")){
                orderData.setServiceCharge(tds.get(1).getText().replace("个同城豆",""));
            }else if(tds.get(0).getText().contains("实付金额")){
                DecimalFormat usFormat = new DecimalFormat("###,###.00");
                BigDecimal amount = new BigDecimal(tds.get(1).getText().replace("万元", "")).multiply(new BigDecimal(10000)).setScale(2, BigDecimal.ROUND_HALF_UP);
                orderData.setPayAmount("¥ "+ usFormat.format(amount));
            }
        }

       /* //平台服务费
        WebElement serviceChargeElement = WebDriverUtil.getElement(driver,By.ByXPath.xpath(SameCitySystemPropertiesUtil.get(SameCityConstant.ORDER_DETAIL_SERVICE_CHARGE)));
        orderData.setServiceCharge(serviceChargeElement.getText().replace("个同城豆",""));
        //实付金额
        WebElement payAmountElement = WebDriverUtil.getElement(driver,By.ByXPath.xpath(SameCitySystemPropertiesUtil.get(SameCityConstant.ORDER_DETAIL_PAY_AMOUNT)));
        DecimalFormat usFormat = new DecimalFormat("###,###.00");
        BigDecimal amount = new BigDecimal(payAmountElement.getText().replace("万元", "")).multiply(new BigDecimal(10000)).setScale(2, BigDecimal.ROUND_HALF_UP);
        orderData.setPayAmount("¥ "+ usFormat.format(amount));*/

        orderDataList.add(orderData);
    }


}
