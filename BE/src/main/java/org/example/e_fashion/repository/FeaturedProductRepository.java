package org.example.e_fashion.repository;

import org.example.e_fashion.dto.projection.FeaturedProductProjection;
import org.example.e_fashion.entity.ProductEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeaturedProductRepository extends Repository<ProductEntity, String> {

    // ===== NEW =====
    @Query(value = """
        SELECT 
            p.id as productId,
            p.name as name,
            p.slug as slug,

            p.thumbnail_url as thumbnail,

            MIN(vs.original_price) as minPrice,
            MAX(vs.original_price) as maxPrice,

            EXISTS (
                SELECT 1 
                FROM product_variant_sizes vs2
                JOIN product_variants v3 ON v3.id = vs2.variant_id
                WHERE v3.product_id = p.id
                  AND vs2.sale_price > 0
                  AND vs2.sale_price < vs2.original_price
            ) as isOnSale,

            NULL as totalSold

        FROM products p
        JOIN product_variants v ON v.product_id = p.id
        JOIN product_variant_sizes vs ON vs.variant_id = v.id

        WHERE p.is_active = 1
          AND p.deleted_at IS NULL

        GROUP BY p.id
        ORDER BY p.created_at DESC
        LIMIT 5
        """, nativeQuery = true)
    List<FeaturedProductProjection> findNewProducts();



    // ===== SALE =====
    @Query(value = """
        SELECT
           p.id as productId,
           p.name as name,
           p.slug as slug,
       
           p.thumbnail_url as thumbnail,
       
           MIN(vs.sale_price) as minPrice,
           MAX(vs.original_price) as maxPrice,
       
           MAX(vs.original_price - vs.sale_price) as maxDiscount,
       
           1 as isOnSale,
           NULL as totalSold
       
       FROM products p
       JOIN product_variants v ON v.product_id = p.id
       JOIN product_variant_sizes vs ON vs.variant_id = v.id
       
       WHERE vs.sale_price > 0
         AND vs.sale_price < vs.original_price
         AND p.is_active = 1
       
       GROUP BY p.id
       
       ORDER BY maxDiscount DESC
       LIMIT 5;
        """, nativeQuery = true)
    List<FeaturedProductProjection> findSaleProducts();



    // ===== BEST SELLERS =====
    @Query(value = """
        SELECT 
            p.id as productId,
            p.name as name,
            p.slug as slug,

            p.thumbnail_url as thumbnail,

            MIN(vs.original_price) as minPrice,
            MAX(vs.original_price) as maxPrice,

            FALSE as isOnSale,

            SUM(oi.quantity) as totalSold

        FROM products p
        JOIN product_variants v ON v.product_id = p.id
        JOIN product_variant_sizes vs ON vs.variant_id = v.id
        JOIN order_items oi ON oi.product_variant_size_id = vs.id
        JOIN orders o ON o.id = oi.order_id

        WHERE o.payment_status = 'PAID'

        GROUP BY p.id
        ORDER BY totalSold DESC
        LIMIT 5
        """, nativeQuery = true)
    List<FeaturedProductProjection> findBestSellerProducts();
}