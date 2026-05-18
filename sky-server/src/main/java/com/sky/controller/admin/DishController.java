package com.sky.controller.admin;

import com.github.pagehelper.Page;
import com.sky.annotation.AddTen;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.mapper.DishMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.service.impl.DishServiceImpl;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/dish")
@Slf4j
@Api(tags="菜品相关接口")
public class DishController {

    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO){

        dishService.saveWithFlavor(dishDTO);

        //清理redis缓存数据
        cleanCache("*dish_"+dishDTO.getCategoryId());

        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("分页查询菜品")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){

        PageResult dishPage = dishService.pageQuery(dishPageQueryDTO);

        return Result.success(dishPage);
    }

    @DeleteMapping
    @ApiOperation("删除菜品")
    public Result delete (@RequestParam List<Long> ids){

        dishService.deleteBatch(ids);

        //删除redis所有缓存
        cleanCache("*dish_*");

        return Result.success();
    }

    @PostMapping("/status/{status}")
    @ApiOperation("菜品起售停售")     //id参数在query参数中
    public Result startOrStop(@PathVariable("status") Integer status,Long id){
        dishService.startOrStop(status,id);

        //删除redis所有缓存
        cleanCache("*dish_*");

        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据Id查询菜品，用于填充修改菜品时的初始文本")
    public Result<DishVO> getById(@PathVariable("id") Long id){
        DishVO dishVO = dishService.getByIdWithFlavor(id);

        return Result.success(dishVO);
    }

    @PutMapping
    @ApiOperation("修改菜品数据")
    public Result update(@RequestBody DishDTO dishDTO){
        dishService.updateWithFlavor(dishDTO);

        //删除redis所有缓存
        cleanCache("dish_*");

        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> list(Long categoryId){
//        System.out.println(categoryId);
        List<Dish> dishes = dishService.getByCategoryId(categoryId);

        return Result.success(dishes);
    }


    //私有方法，清理缓存数据
    private void cleanCache(String pattern){
        Set<String> keys = redisTemplate.keys(pattern);
        log.info("进入删除redis");
        log.info(keys.toString());
        redisTemplate.delete(keys);
    }

}
