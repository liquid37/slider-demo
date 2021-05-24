package com.liquid.spider;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.enmus.ExcelType;
import com.liquid.spider.constant.CommonConstant;
import com.liquid.spider.constant.DeepConstant;
import com.liquid.spider.pojo.DeepOrderData;
import com.liquid.spider.utils.CommonPropertiesUtil;
import com.liquid.spider.utils.DeepSystemPropertiesUtil;
import com.liquid.spider.utils.DeepUserPropertiesUtil;
import com.liquid.spider.utils.WebDriverUtil;
import org.apache.commons.lang3.time.DateFormatUtils;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DeepSpiderMain {

    public static List<DeepOrderData> orderDataList = new ArrayList<>();

    public static int loadedOrderCount = 0;

    public static String accountName ="";

    public static void start() throws Exception {
        System.out.println("=======================深度爬虫开始==========================");
        System.setProperty(CommonPropertiesUtil.get(CommonConstant.WEB_DRIVER), CommonPropertiesUtil.get(CommonConstant.WEB_DRIVER_VALUE));
        //option.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        WebDriver driver = new FirefoxDriver();
        //driver.navigate().to("https://www.tcpjw.com/passport/login?referer=https%3A%2F%2Fwww.tcpjw.com%2F");
        //打开登录页面
        while (true){
            try{
                driver.get(DeepSystemPropertiesUtil.get(DeepConstant.LOGIN_URL)); // todo 改成配置
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
        action.dragAndDropBy(scaleElement,320,0).perform();
        TimeUnit.MILLISECONDS.sleep(500);

        //点击登录按钮
        WebElement submitButton = WebDriverUtil.getElement(driver, By.ByXPath.xpath(DeepSystemPropertiesUtil.get(DeepConstant.SUBMIT_BUTTON))); // todo 改成配置
        submitButton.click();
        //
        TimeUnit.SECONDS.sleep(5);
        //登录成功
        //获取首页广告遮罩层
        WebElement alertContentElement = WebDriverUtil.isElementPresent(driver, By.cssSelector(DeepSystemPropertiesUtil.get(DeepConstant.INDEX_ALTER_CONTENT))); // todo 改成配置
        if(alertContentElement != null){
            //有广告层，关闭广告
            //alertContentElement.findElement(By.xpath(DeepPropertiesUtil.get(DeepConstant.INDEX_ALTER_CONTENT_CLOSE))).click(); // todo 改成配置
            WebDriverUtil.getElement(driver,By.ByXPath.xpath(DeepSystemPropertiesUtil.get(DeepConstant.INDEX_ALTER_CONTENT_CLOSE))).click();
        }

        TimeUnit.MILLISECONDS.sleep(200);
        //关闭”知道了“按钮
        //WebElement knowButtonElement = driver.findElement(By.cssSelector(".driver-close-only-btn"));
        //if (knowButtonElement != null) {
            //点解知道了
        //    ((JavascriptExecutor)driver).executeScript("arguments[0].click();",knowButtonElement);
        //}
        //关闭风险设置的提示方式
        WebElement alterKnowElement = driver.findElement(By.cssSelector(".confirm"));
        if(alterKnowElement!=null){
            if("知道了".equals(alterKnowElement.getText())){
                ((JavascriptExecutor)driver).executeScript("arguments[0].click();",alterKnowElement);
            }
        }

        //点击用户名
        WebElement userBackendElement = driver.findElement(By.ByXPath.xpath("/html/body/div[1]/div[3]/div[1]/div/div[2]/div[1]/ul/li/div/a"));
        accountName = userBackendElement.getText();
        ((JavascriptExecutor)driver).executeScript("arguments[0].click();",userBackendElement);

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
                handlerUserBackend(driver);  // 跳转到交易
            }
        }

    }

    public static void handlerUserBackend(WebDriver driver) throws Exception {
        List<WebElement> itemElements = driver.findElements(By.cssSelector(".el-menu-item"));
        WebElement buyOrderItem = null;
        for(WebElement item : itemElements){
            String text = item.getText();
            if(text.startsWith(DeepSystemPropertiesUtil.get(DeepConstant.TRADING_HALL_ITEM_NAME))){       // todo 改成配置
                buyOrderItem = item;
                break;
            }
        }
        if(buyOrderItem==null){
            System.out.println("==========没有我的订单(接单方)菜单");
            return;
        }
        // 点击我的订单（买）菜单
        ((JavascriptExecutor)driver).executeScript("arguments[0].click();",buyOrderItem);
        TimeUnit.SECONDS.sleep(2);     // todo 改成配置

        //查询数据

        // 最新订单  xpath /html/body/div[1]/section/section/main/div/div[1]/ul/li[1]
        // 最新订单  已完成交易xpath  /html/body/div[1]/section/section/main/div/div[2]/div/div[1]/div/div/label[8]/span
        //          数据区 cssSelector .buy-empty-list

        // 历史订单  xpath /html/body/div[1]/section/section/main/div/div[1]/ul/li[2]
        // 历史订单  已完成交易xpath  /html/body/div[1]/section/section/main/div/div[2]/div/div[2]/div/div/label[2]/span

        // 订单tag 这里选择最新订单
        WebElement orderTagElement = WebDriverUtil.getElement(driver,By.ByXPath.xpath(DeepSystemPropertiesUtil.get(DeepConstant.ORDER_XPATH)));
        orderTagElement.click();

        //选择交易完成条件
        WebElement orderFinishElement = WebDriverUtil.getElement(driver,By.ByXPath.xpath(DeepSystemPropertiesUtil.get(DeepConstant.ORDER_FINISH_XPATH)));
        orderFinishElement.click();
        TimeUnit.SECONDS.sleep(2);

        //输入日期
        List<WebElement> queryTimeElements = WebDriverUtil.getElements(driver,By.cssSelector(DeepSystemPropertiesUtil.get(DeepConstant.TIME_COMPONENT)));
        queryTimeElements.get(0).click();
        TimeUnit.MILLISECONDS.sleep(100);
        //点击开始日期
        WebElement dateElement = WebDriverUtil.getElement(driver,By.cssSelector(DeepSystemPropertiesUtil.get(DeepConstant.TODAY_CSS)));
        ((JavascriptExecutor)driver).executeScript("arguments[0].click();",dateElement);
        //点击结束日期
        ((JavascriptExecutor)driver).executeScript("arguments[0].click();",dateElement);

        //执行查询
        List<WebElement> buttons = WebDriverUtil.getElements(driver,By.cssSelector(DeepSystemPropertiesUtil.get(DeepConstant.SUBMIT_CSS)));
        WebElement queryButton = null;
        for(WebElement button : buttons){
            if("查询".equals(button.getText())){
                queryButton = button;
            }
        }
        if(queryButton !=null){
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", queryButton);
        }else{
            System.out.println("没有查询按钮");
            return;
        }
        //等待执行结果
        TimeUnit.SECONDS.sleep(3);

        //加载订单列表
        loadOrderListData(driver,1);

        System.out.println("=====================================================");
        int dataSize = orderDataList.size();
        //导出excel
        if(dataSize > 0) {
            List<DeepOrderData> orderDataListSorted = DeepSpiderMain.orderDataList.stream().sorted((o1, o2) ->
                    o1.getFinishDate().compareTo(o2.getFinishDate())
            ).collect(Collectors.toList());
            Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(null, accountName,ExcelType.XSSF), DeepOrderData.class, orderDataListSorted);
            String excelName = accountName+"_深度_" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".xlsx";
            BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream("./excels/" + excelName));
            workbook.write(outputStream);
            outputStream.close();
            workbook.close();
            System.out.println("成功爬取"+ dataSize +"条记录，文件存放在excels目录，文件名是："+excelName);
        }else{
            System.out.println("没有数据，不需要导excel");
        }

        System.out.println("=======================================");
        System.out.println("深度网站爬虫完成");
        System.out.println("=======================================");

    }

    private static void loadOrderListData(WebDriver driver, int currentPage) throws InterruptedException {
        System.out.println("正在爬第"+currentPage+"页数据");
        WebElement tableBodyElement = null;
        while (true) { // 等待加载数据完成
            //
            tableBodyElement = WebDriverUtil.getElement(driver, By.ByXPath.xpath(DeepSystemPropertiesUtil.get(DeepConstant.ORDER_LIST_TABLE)));
            if(tableBodyElement !=null){
                break;
            }else{
                System.out.println("正在加载第"+currentPage+"页数据");
                TimeUnit.MILLISECONDS.sleep(200);
            }
        }// 判断是否有数据

        List<WebElement> trElements= WebDriverUtil.getElementsFromElement(tableBodyElement, By.tagName("tr"));
        int trSize = trElements.size();
        if(trSize == 0){  // 没有数据
            return;
        }
        //System.out.println("当前页有"+trSize+"条记录");

        for(int i = 0; i<trSize; i++){
            //判断已统计的订单数量是否大于最大限制
            if(loadedOrderCount >= Integer.valueOf(DeepSystemPropertiesUtil.get(DeepConstant.DEEP_MAX_SPIDER_RECORD))){
                return;
            }
            WebElement trElement = trElements.get(i);
            List<WebElement> tdElements = WebDriverUtil.getElementsFromElement(trElement,By.tagName("td"));
            //获取最后一个td,详情按钮在最后一个td
            WebElement detailTdElement = tdElements.get(tdElements.size() - 1);
            WebElement btnsElements = WebDriverUtil.getElementFromElement(detailTdElement, By.cssSelector(".btns"));
            WebElement detailButtonElement = WebDriverUtil.getElementFromElement(btnsElements,By.cssSelector(".detail"));

            ((JavascriptExecutor)driver).executeScript("arguments[0].click();",detailButtonElement);
            TimeUnit.SECONDS.sleep(Integer.valueOf(DeepUserPropertiesUtil.get(DeepConstant.SLEEP_SECOND)));
            loadDetailOrderDate(driver);

            loadedOrderCount++; // 统计已加载的订单数量

        }

        WebElement nextButtonElement = WebDriverUtil.getElement(driver, By.cssSelector(DeepSystemPropertiesUtil.get(DeepConstant.NEXT_PAGE_CSS)));

        if(nextButtonElement.isEnabled()){  // 判断能否点下一页
            ((JavascriptExecutor)driver).executeScript("arguments[0].click();",nextButtonElement);
            TimeUnit.MILLISECONDS.sleep(500);
            loadOrderListData(driver,++currentPage);
        }
    }



    private static void loadDetailOrderDate(WebDriver driver) throws InterruptedException {
        DeepOrderData orderData = new DeepOrderData();
        //*[@id="root"]/div/div[3]/div[2]/div[1]/div[3]/div[3]/div[2]/div/div[2]/div[2]/div[2]/table/tbody/tr[1]/td[2]

        //driver.findElements(By.cssSelector("div .detail-border")).get(0).findElements(By.cssSelector(".el-row")).get(0).findElements(By.tagName("div"))
        List<WebElement> trs = new ArrayList<>();
        WebDriverUtil.getElements(driver,By.cssSelector("div .detail-border")).forEach(b->{
            List<WebElement> rows =  WebDriverUtil.getElementsFromElement(b,By.cssSelector(".el-row"));
            trs.addAll(rows);
        });
        //List<WebElement> trs = WebDriverUtil.getElements(driver, By.cssSelector(".el-row"));
        for(WebElement tr : trs){
            List<WebElement> tds =  WebDriverUtil.getElementsFromElement(tr,By.tagName("div"));
            if(tds.get(0).getText().contains("票号")){
                orderData.setBillNo(tds.get(1).getText());
            }else if(tds.get(0).getText().contains("票面金额")){
                orderData.setAmount(tds.get(1).getText().replace("万",""));
            }else if(tds.get(0).getText().contains("到期日")){
                String dateElementText = tds.get(1).getText();
                orderData.setEndDate(dateElementText);
            }else if(tds.get(0).getText().contains("承兑人")){
                orderData.setAcceptor(tds.get(1).getText());
            }else if(tds.get(0).getText().contains("成交价")){
                String yearRateText = tds.get(1).getText();
                orderData.setYearRate(yearRateText.substring(yearRateText.indexOf("年息")+2));
            }else if(tds.get(0).getText().contains("实付金额")){
                DecimalFormat usFormat = new DecimalFormat("###,###.00");
                BigDecimal amount = new BigDecimal(tds.get(1).getText().replace("万", "")).multiply(new BigDecimal(10000)).setScale(2, BigDecimal.ROUND_HALF_UP);
                orderData.setPayAmount("¥ "+ usFormat.format(amount));
            }else if(tds.get(0).getText().contains("保证金")){
                String depositText = tds.get(1).getText();
                String deposit = getAmount(depositText);
                orderData.setDeposit("¥ "+ deposit);
            }
        }
        //支付完成时间
        //获取订单状态栏，
        List<WebElement> orderStatusElements = WebDriverUtil.getElements(driver, By.cssSelector(DeepSystemPropertiesUtil.get(DeepConstant.ORDER_STATUS_CSS)));
        // 订单完成了只会是最后一个节点，因此只取最后一个即可
        WebElement orderFinishElement = orderStatusElements.get(orderStatusElements.size()-1);
        orderData.setFinishDate(DateFormatUtils.format(new Date(),"yyyy")+"-"+orderFinishElement.findElements(By.ByTagName.tagName("p")).get(0).getText());

        orderDataList.add(orderData);
        WebElement windowCloseButton = WebDriverUtil.getElement(driver, By.cssSelector(DeepSystemPropertiesUtil.get(DeepConstant.DIALOG_CLOSE_CSS)));
        ((JavascriptExecutor)driver).executeScript("arguments[0].click();",windowCloseButton);
        TimeUnit.SECONDS.sleep(Integer.valueOf(DeepUserPropertiesUtil.get(DeepConstant.SLEEP_SECOND)));
    }

    private static String getAmount(String str){
        // 需要取整数和小数的字符串
        // 控制正则表达式的匹配行为的参数(小数)
        Pattern p = Pattern.compile("(\\d+\\.\\d+)");
        //Matcher类的构造方法也是私有的,不能随意创建,只能通过Pattern.matcher(CharSequence input)方法得到该类的实例.
        Matcher m = p.matcher(str);
        //m.find用来判断该字符串中是否含有与"(\\d+\\.\\d+)"相匹配的子串
        if (m.find()) {
            //如果有相匹配的,则判断是否为null操作
            //group()中的参数：0表示匹配整个正则，1表示匹配第一个括号的正则,2表示匹配第二个正则,在这只有一个括号,即1和0是一样的
            str = m.group(1) == null ? "" : m.group(1);
        } else {
            //如果匹配不到小数，就进行整数匹配
            p = Pattern.compile("(\\d+)");
            m = p.matcher(str);
            if (m.find()) {
                //如果有整数相匹配
                str = m.group(1) == null ? "" : m.group(1);
            } else {
                //如果没有小数和整数相匹配,即字符串中没有整数和小数，就设为空
                str = "";
            }
        }
        return str;
    }
}
