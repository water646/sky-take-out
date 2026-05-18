package com.sky.service;

import com.sky.controller.admin.DishController;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;
import io.swagger.models.auth.In;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface DishService {

    public void saveWithFlavor(DishDTO dishDTO);

    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    public void deleteBatch(List<Long> ids);

    public void startOrStop(Integer status , Long id);

    public DishVO getByIdWithFlavor(Long id);

    public void updateWithFlavor(DishDTO dishDTO);

    public List<Dish> getByCategoryId(Long categoryId);

    List<DishVO> listWithFlavor(Dish dish);

}

