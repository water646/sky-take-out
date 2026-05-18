package com.sky.service.impl;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    public List<ShoppingCart> showShoppingCart();

    public void cleanShoppingCart();
}
