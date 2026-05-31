package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.mapper.DishMapper;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.utils.BloomFilterUtil;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private BloomFilterUtil bloomFilterUtil;

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {

        //新增：用布隆过滤器，如果是不可能出现的分类id，说明是缓存穿透，直接滚蛋
        if(!bloomFilterUtil.mightContainCategoryId(categoryId)) {
            return Result.error("分类id不存在");
        }

        //查询redis中是否存在菜品数据，如果有就走redis
        String key = "dish_"+categoryId;
        List<DishVO> list =(List<DishVO>) redisTemplate.opsForValue().get(key);

        //修改：redis查到空集也算命中，防止缓存穿透
        if(list!=null){
            return Result.success(list);
        }

        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品

        //如果redis中没有，就在mysql查询，把结果缓存进redis
        list = dishService.listWithFlavor(dish);

        //修改：如果mysql都查不到，说明是缓存穿透，设置2分钟过期的空值缓存
        if(list.size()==0){
            redisTemplate.opsForValue().set(key,list,2, TimeUnit.MINUTES);
        }
        else{
            redisTemplate.opsForValue().set(key, list);
        }

        return Result.success(list);
    }

}
