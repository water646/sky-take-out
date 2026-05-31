package com.sky.utils;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class BloomFilterUtil {

    /**
     * categoryId 布隆过滤器
     */
    private BloomFilter<Long> categoryBloomFilter;

    /**
     * 初始化布隆过滤器
     */
    @PostConstruct
    public void init() {

        // 预计插入1000个元素
        // 误判率1%
        categoryBloomFilter = BloomFilter.create(
                Funnels.longFunnel(),
                1000,
                0.01
        );
    }

    /**
     * 添加 categoryId
     */
    public void addCategoryId(Long categoryId) {
        categoryBloomFilter.put(categoryId);
    }

    /**
     * 判断 categoryId 是否可能存在
     */
    public boolean mightContainCategoryId(Long categoryId) {
        return categoryBloomFilter.mightContain(categoryId);
    }
}
