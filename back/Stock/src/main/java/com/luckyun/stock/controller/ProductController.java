package com.luckyun.stock.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.luckyun.stock.entity.Product;
import com.luckyun.stock.service.OperationLogService;
import com.luckyun.stock.service.ProductService;
import com.luckyun.stock.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@SaCheckLogin
public class ProductController {

    private final ProductService productService;
    private final OperationLogService operationLogService;
    private final UserService userService;

    private String getOperatorName() {
        long userId = StpUtil.getLoginIdAsLong();
        var user = userService.getById(userId);
        return user != null ? (user.getNickname() != null ? user.getNickname() : user.getUsername()) : "未知";
    }

    @GetMapping
    public ResponseEntity<List<Product>> list() {
        return ResponseEntity.ok(productService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> get(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<Product> getByBarcode(@PathVariable String barcode) {
        Product product = productService.lambdaQuery()
                .eq(Product::getBarcode, barcode).one();
        return ResponseEntity.ok(product);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Product product) {
        // 条码重复时累加库存
        if (product.getBarcode() != null && !product.getBarcode().isEmpty()) {
            Product exist = productService.lambdaQuery()
                    .eq(Product::getBarcode, product.getBarcode()).one();
            if (exist != null) {
                int added = product.getStock();
                int oldStock = exist.getStock();
                exist.setStock(oldStock + added);
                productService.updateById(exist);
                operationLogService.log("STOCK_IN", "PRODUCT", exist.getId(), exist.getName(),
                        "入库 " + added + " " + (exist.getUnit() != null ? exist.getUnit() : "件")
                                + "（库存：" + oldStock + " → " + (oldStock + added) + "）",
                        getOperatorName());
                Map<String, Object> result = new HashMap<>();
                result.put("type", "stock_in");
                result.put("added", added);
                result.put("product", exist);
                return ResponseEntity.ok(result);
            }
        }

        Product saved = productService.createProduct(product);
        operationLogService.log("CREATE_PRODUCT", "PRODUCT", saved.getId(), saved.getName(),
                "新增商品", getOperatorName());
        Map<String, Object> result = new HashMap<>();
        result.put("type", "create");
        result.put("product", saved);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @RequestBody Product product) {
        Product old = productService.getById(id);
        product.setId(id);
        productService.updateById(product);
        if (old != null) {
            operationLogService.log("UPDATE_PRODUCT", "PRODUCT", id, product.getName(),
                    "编辑商品（原名称：" + old.getName() + "）", getOperatorName());
        }
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Product product = productService.getById(id);
        productService.removeById(id);
        if (product != null) {
            operationLogService.log("DELETE_PRODUCT", "PRODUCT", id, product.getName(),
                    "删除商品", getOperatorName());
        }
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<Product> adjustStock(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        int delta = body.get("delta");
        if (delta == 0) return ResponseEntity.badRequest().build();
        Product product = productService.getById(id);
        if (product == null) return ResponseEntity.notFound().build();
        int newStock = product.getStock() + delta;
        if (newStock < 0) return ResponseEntity.badRequest().build();
        String logType = delta > 0 ? "STOCK_IN" : "STOCK_OUT";
        String action = delta > 0 ? "入库" : "出库";
        int qty = Math.abs(delta);
        operationLogService.log(logType, "PRODUCT", id, product.getName(),
                action + " " + qty + " " + (product.getUnit() != null ? product.getUnit() : "件")
                        + "（库存：" + product.getStock() + " → " + newStock + "）",
                getOperatorName());
        product.setStock(newStock);
        productService.updateById(product);
        return ResponseEntity.ok(product);
    }
}
