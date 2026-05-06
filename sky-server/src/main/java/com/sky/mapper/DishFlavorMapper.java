package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    //批量插入菜品口味
    public void insertBatch(List<DishFlavor> dishFlavors);

    @Delete("delete from dish_flavor where dish_id = #{dishId}")
    public void deleteByDishId(Long dishId);

    public void deleteByDishIds(List<Long> dishIds);

    @Select("select * from dish_flavor where dish_id = #{dishId}")
    public List<DishFlavor> selectByDishId(Long dishId);

}
