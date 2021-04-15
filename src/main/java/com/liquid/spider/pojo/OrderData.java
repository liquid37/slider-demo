package com.liquid.spider.pojo;

import cn.afterturn.easypoi.excel.annotation.Excel;
import lombok.Data;

import java.io.Serializable;

@Data
public class OrderData implements Serializable {

    @Excel(name = "承兑人")
    private String acceptor;

    @Excel(name="票面金额(万)")
    private String amount;

    @Excel(name="到期日")
    private String endDate;

    @Excel(name="剩余天数(天)")
    private String remainderDay;

    @Excel(name="票号")
    private String billNo;

    @Excel(name="年息")
    private String yearRate;

    @Excel(name="服务费(同城豆)")
    private String serviceCharge;

    @Excel(name="实付金额(元)")
    private String payAmount;

    @Excel(name="支付方式")
    private String payType;

    @Excel(name="交易完成时间")
    private String finishDate;

    @Excel(name="订单号")
    private String orderNo;





}
