package com.liquid.spider.pojo;

import lombok.Data;

@Data
public class DeepOrderLIstQO {

    private Integer draftStatus = 7;

    private Integer current = 1;

    private Integer size = 100;

    private String startTime;

    private String endTime;

    private Integer history = 0;
}
