package com.liquid.spider;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.enmus.ExcelType;
import com.liquid.spider.pojo.OrderData;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestMain {

    /*public static void main(String[] args) throws IOException {
        OrderData orderData = new OrderData();
        orderData.setPayAmount(new BigDecimal("312111.3232").setScale(2,BigDecimal.ROUND_HALF_UP));
        orderData.setServiceCharge("2");
        orderData.setYearRate("3");
        orderData.setBillNo("4");
        orderData.setAmount("5");
        orderData.setRemainderDay("6");
        orderData.setEndDate("7");
        orderData.setAcceptor("8");

        List<OrderData> dataList = new ArrayList<>();
        dataList.add(orderData);

        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams("aaa","sheet1", ExcelType.XSSF), OrderData.class, dataList);
        FileOutputStream outputStream = new FileOutputStream("./excels/a.xlsx");
        workbook.write(outputStream);
        outputStream.close();
        workbook.close();
    }*/

    /*public static void main(String[] args) {

        DecimalFormat usFormat = new DecimalFormat("###,###.00");
        String format = usFormat.format(new BigDecimal("312111.3262").setScale(2, BigDecimal.ROUND_HALF_UP));
        System.out.println("¥ "+format);
    }*/

    /*public static void main(String[] args) {
        OrderData orderData1 = new OrderData();
        orderData1.setFinishDate("2021-03-24 17:31:15");
        OrderData orderData2 = new OrderData();
        orderData2.setFinishDate("2021-03-24 16:23:33");
        OrderData orderData3 = new OrderData();
        orderData3.setFinishDate("2021-03-24 12:04:29");
        OrderData orderData4 = new OrderData();
        orderData4.setFinishDate("2021-03-24 11:45:42");
        OrderData orderData5 = new OrderData();
        orderData5.setFinishDate("2021-03-24 10:47:19");
        OrderData orderData6 = new OrderData();
        orderData6.setFinishDate("2021-03-24 10:07:09");

        List<OrderData> orderDataList = Arrays.asList(orderData1,orderData2,orderData3,orderData4,orderData5,orderData6);
        List<OrderData> orderDatas = orderDataList.stream().sorted((o1, o2) -> o1.getFinishDate().compareTo(o2.getFinishDate())).collect(Collectors.toList());
        orderDatas.stream().forEach(System.out ::println);
    }*/

    public static void main(String[] args) {
        // 需要取整数和小数的字符串
        String str = "需要提取的字符串1.111";
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
        System.out.println(str);
    }

}
