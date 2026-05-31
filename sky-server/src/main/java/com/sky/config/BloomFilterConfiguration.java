package com.sky.config;

import com.sky.mapper.CategoryMapper;
import com.sky.utils.BloomFilterUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class BloomFilterConfiguration implements CommandLineRunner {

    @Autowired
    private BloomFilterUtil bloomFilterUtil;

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public void run(String... args) throws Exception {
        List<Long> ids = categoryMapper.getAllCategoryIds();
        ids.forEach(bloomFilterUtil::addCategoryId);
        log.info("布隆过滤器初始化完成");
    }
}