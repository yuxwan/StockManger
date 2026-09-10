package com.luckyun.stock.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luckyun.stock.entity.Product;
import com.luckyun.stock.service.OperationLogService;
import com.luckyun.stock.service.ProductService;
import com.luckyun.stock.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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

    @GetMapping("/search")
    public ResponseEntity<Page<Product>> search(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int pageSize) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getName, keyword)
                    .or().like(Product::getBarcode, keyword)
                    .or().like(Product::getSpec, keyword);
        }
        wrapper.orderByDesc(Product::getId);
        return ResponseEntity.ok(productService.page(new Page<>(page, pageSize), wrapper));
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
    public ResponseEntity<?> getByBarcode(@PathVariable String barcode) {
        Product product = productService.lambdaQuery()
                .eq(Product::getBarcode, barcode).one();
        if (product == null) return ResponseEntity.badRequest().body(Map.of("msg", "商品不存在"));
        return ResponseEntity.ok(product);
    }

    @PostMapping
    @SaCheckPermission("products:add")
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
    @SaCheckPermission("products:edit")
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
    @SaCheckPermission("products:delete")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Product product = productService.getById(id);
        productService.removeById(id);
        if (product != null) {
            operationLogService.log("DELETE_PRODUCT", "PRODUCT", id, product.getName(),
                    "删除商品", getOperatorName());
        }
        return ResponseEntity.ok().build();
    }

    /** 入库 */
    @PostMapping("/{id}/stock-in")
    @SaCheckPermission("products:stock-in")
    public ResponseEntity<?> stockIn(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int quantity = toInt(body.get("quantity"));
        if (quantity <= 0) return ResponseEntity.badRequest().body(Map.of("msg", "入库数量必须大于0"));
        String remark = body.get("remark") == null ? "" : String.valueOf(body.get("remark")).trim();
        return adjustStock(id, quantity, remark);
    }

    /** 出库 */
    @PostMapping("/{id}/stock-out")
    @SaCheckPermission("products:stock-out")
    public ResponseEntity<?> stockOut(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int quantity = toInt(body.get("quantity"));
        if (quantity <= 0) return ResponseEntity.badRequest().body(Map.of("msg", "出库数量必须大于0"));
        String remark = body.get("remark") == null ? "" : String.valueOf(body.get("remark")).trim();
        return adjustStock(id, -quantity, remark);
    }

    private int toInt(Object v) {
        if (v instanceof Number) return ((Number) v).intValue();
        if (v != null) {
            try { return Integer.parseInt(String.valueOf(v).trim()); } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private ResponseEntity<?> adjustStock(Long id, int delta, String remark) {
        Product product = productService.getById(id);
        if (product == null) return ResponseEntity.badRequest().body(Map.of("msg", "商品不存在"));
        int newStock = product.getStock() + delta;
        if (newStock < 0) return ResponseEntity.badRequest().body(Map.of("msg", "库存不足"));
        String logType = delta > 0 ? "STOCK_IN" : "STOCK_OUT";
        String action = delta > 0 ? "入库" : "出库";
        int qty = Math.abs(delta);
        String detail = action + " " + qty + " " + (product.getUnit() != null ? product.getUnit() : "件")
                + "（库存：" + product.getStock() + " → " + newStock + "）";
        if (remark != null && !remark.isEmpty()) {
            detail += "，备注：" + remark;
        }
        operationLogService.log(logType, "PRODUCT", id, product.getName(),
                detail,
                getOperatorName());
        product.setStock(newStock);
        productService.updateById(product);
        return ResponseEntity.ok(product);
    }
}
