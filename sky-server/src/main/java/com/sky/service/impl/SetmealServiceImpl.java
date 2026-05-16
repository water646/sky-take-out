package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.vo.DishItemVO;
import com.sky.entity.Category;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private CategoryMapper categoryMapper;


    //插入套餐表
    @Transactional
    public void saveWithSetmealDish(SetmealDTO setmealDTO){
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.save(setmeal);

        //插入套餐-菜品表
        List<SetmealDish> setmealDishList = setmealDTO.getSetmealDishes();
        setmealDishList.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmeal.getId());
        });

        setmealDishMapper.insertBatch(setmealDishList);
    }

    //分页查询套餐
    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO){
        //拦截mybatis并分页
        Integer pageNum =  setmealPageQueryDTO.getPage();
        Integer pageSize = setmealPageQueryDTO.getPageSize();
        PageHelper.startPage(pageNum,pageSize);

        Page<SetmealVO> pages = setmealMapper.pageQuery(setmealPageQueryDTO);
        PageResult pageResult = new PageResult();
        pageResult.setTotal(pages.getTotal());
        pageResult.setRecords(pages.getResult());

        return pageResult;
    }

    //批量删除并检查能否删除,还要删除套餐-菜品表的数据
    @Transactional
    public void deleteBatch(List<Long> ids){
        //检查套餐是否起售中
        for(Long id:ids){
            Setmeal setmeal = setmealMapper.getById(id);
            if(setmeal.getStatus()== StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }

        setmealMapper.deleteBatch(ids);
        setmealDishMapper.deleteBySetmealIds(ids);
    }

    //获取setmealVO
    public SetmealVO getSetmealVOById(Long id){
        Setmeal setmeal = setmealMapper.getById(id);

        Long categoryId = setmeal.getCategoryId();
        Category category = categoryMapper.getById(categoryId);

        List<SetmealDish> setmealDishList = setmealDishMapper.getBySetmealId(id);

        //自己拼SetmealVO
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal,setmealVO);
        setmealVO.setCategoryName(category.getName());
        setmealVO.setSetmealDishes(setmealDishList);

        return setmealVO;
    }

    //修改套餐
    @Transactional
    public void update(SetmealDTO setmealDTO){
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.update(setmeal);

        //更新套餐-菜品关系表
        List<Long> idList = new ArrayList<>();
        idList.add(setmeal.getId());
        List<SetmealDish> setmealDishList = setmealDTO.getSetmealDishes();
        setmealDishList.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmeal.getId());
        });

        setmealDishMapper.deleteBySetmealIds(idList);
        setmealDishMapper.insertBatch(setmealDTO.getSetmealDishes());

    }

    //起售停售套餐
    public void startOrStop(Integer status,Long setmealId){

        Setmeal setmeal = Setmeal.builder()
                .id(setmealId)
                .status(status)
                .build();

        setmealMapper.update(setmeal);

    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }
}
