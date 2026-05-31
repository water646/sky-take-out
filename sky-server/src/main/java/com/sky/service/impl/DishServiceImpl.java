package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.annotation.AddTen;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.controller.admin.DishController;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.StatusNotExistException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    //新增菜品和对应的口味
    @Transactional  //将要操作两张表，必须让这个事务不可分割。如果出现异常，操作全部撤销
    public void saveWithFlavor(DishDTO dishDTO){

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);

        //向菜品表插入1条数据
        dishMapper.insert(dish);

        //mapper.xml里面进行了主键回填，数据库的id被赋值给dish了
        Long dishId = dish.getId();

        //向菜品口味表插入n条数据
        List<DishFlavor> dishFlavors = dishDTO.getFlavors();
        if(dishFlavors!=null&&dishFlavors.size()>0){
            dishFlavors.forEach(d->{
                d.setDishId(dishId);
            });

            dishFlavorMapper.insertBatch(dishFlavors);
        }

    }

    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO){
        int pageSize = dishPageQueryDTO.getPageSize();
        int pageNum =  dishPageQueryDTO.getPage();

        PageHelper.startPage(pageNum,pageSize);

        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);

        PageResult pageResult = new PageResult();
        pageResult.setTotal(page.getTotal());
        pageResult.setRecords(page.getResult());

        return pageResult;
    }

    @Transactional
    public void deleteBatch(List<Long> ids){
        //判断当前菜品能否删除--是否起售中
        for(Long id:ids){
            Dish dish = dishMapper.getById(id);
            if(dish.getStatus()== StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        //判断当前菜品能否删除--是否被套餐包含
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealIds!=null&&setmealIds.size()>0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }


        //优化批量删除
        dishMapper.deleteByIds(ids);
        dishFlavorMapper.deleteByDishIds(ids);
    }

    //起售停售菜品
    public void startOrStop(Integer status , Long id){
        //新增：检查目标菜品状态是否合法
        if(status!=StatusConstant.ENABLE&&status!=StatusConstant.DISABLE){
            throw new StatusNotExistException(MessageConstant.DISH_STATUS_ERROR);
        }

        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();

        dishMapper.update(dish);
    }

    //获取修改菜品的初始文本
    public DishVO getByIdWithFlavor(Long id){
        Dish dish = dishMapper.getById(id);

        List<DishFlavor> flavor = dishFlavorMapper.selectByDishId(id);

        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(flavor);

        return dishVO;
    }

    public void updateWithFlavor(DishDTO dishDTO){
        //更新菜品信息
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.update(dish);

        //先删除原有口味数据，再插入新的
        dishFlavorMapper.deleteByDishId(dish.getId());

        List<DishFlavor> dishFlavors = dishDTO.getFlavors();
        if(dishFlavors!=null&&dishFlavors.size()>0){
            dishFlavors.forEach(d->{
                d.setDishId(dish.getId());
            });
        dishFlavorMapper.insertBatch(dishFlavors);
        }
    }

    public List<Dish> getByCategoryId(Long categoryId){
        List<Dish> dishList = dishMapper.getByCategoryId(categoryId);

        return  dishList;
    }

    public List<DishVO> listWithFlavor(Dish dish) {

        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.selectByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }


}
