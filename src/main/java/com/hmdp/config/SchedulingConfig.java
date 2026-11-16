package com.hmdp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 开启定时任务（超时关单、对账、归档）。
 * 单独建一个类，是为了不改动 HmDianPingApplication 这个启动类。
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
