package com.sky.service;


import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {

    public void saveWithSetmealDish(SetmealDTO setmealDTO);

    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO);

    public void deleteBatch(List<Long> ids);

    public SetmealVO getSetmealVOById(Long id);

    public void update(SetmealDTO setmealDTO);

    public void startOrStop(Integer status,Long setmealId);

}
