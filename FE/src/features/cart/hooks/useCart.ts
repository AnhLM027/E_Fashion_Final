// features/cart/useCart.ts
import { useEffect, useState } from "react";
import { cartApi } from "../api/cart.api";
import type { CartItem, CartResponse } from "../types/cart.type";
import { toast } from "sonner";

export function useCart() {
  const [cartId, setCartId] = useState<string | null>(null);
  const [items, setItems] = useState<CartItem[]>([]);
  const [totalPrice, setTotalPrice] = useState(0);
  const [loading, setLoading] = useState(true);

  const fetchCart = async () => {
    setLoading(true);
    const cart: CartResponse = await cartApi.getCart();
    console.log("Fetched cart:", cart);
    setCartId(cart.cartId);
    setItems(cart.items);
    setTotalPrice(cart.totalPrice);
    setLoading(false);
  };

  useEffect(() => {
    fetchCart();
  }, []);

  const updateQuantity = async (productVariantSizeId: string, quantity: number) => {
    if (quantity < 1) return;

    // optimistic UI
    setItems(prev =>
      prev.map(i =>
        i.productVariantSizeId === productVariantSizeId
          ? { ...i, quantity }
          : i
      )
    );

    try {
      await cartApi.updateQuantity(productVariantSizeId, quantity);
      // nếu BE trả lại cart mới → dùng dòng dưới
      // setItems(res.items); setTotalPrice(res.totalPrice);
      await fetchCart();
    } catch (err) {
      toast.error('Cập nhật số lượng thất bại');
      await fetchCart();
    }
  };

  const removeItem = async (productVariantSizeId: string) => {
    // optimistic
    const removed = items.find(i => i.productVariantSizeId === productVariantSizeId);
    setItems(prev => prev.filter(i => i.productVariantSizeId !== productVariantSizeId));

    try {
      await cartApi.removeItem(productVariantSizeId);
      await fetchCart();
    } catch (err) {
      // rollback
      if (removed) setItems(prev => [...prev, removed]);
      toast.error('Xóa sản phẩm thất bại');
    }
  };

  return {
    cartId,
    items,
    totalPrice,
    loading,
    refetch: fetchCart,
    updateQuantity,
    removeItem,
  };
}
