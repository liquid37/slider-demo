package com.liquid.spider;

import java.io.IOException;
import java.util.Scanner;

public class SpiderMain {

    public static void main(String[] args) throws Exception {
        System.out.println("******************************************");
        System.out.println("进入爬虫系统");
        System.out.println("请选择爬虫的网站");
        System.out.println("1.同城");
        System.out.println("2.深度");
        System.out.println("按数字 + 回车进行选择，其他为退出");
        Scanner sc = new Scanner(System.in);
        int spiderSystem = sc.nextInt();
        if(spiderSystem == 1){
            SameCitySpiderMain.start();
        }else if(spiderSystem == 2){
            DeepSpiderMain.start();
        }else{
            System.exit(0);
        }
    }
}
