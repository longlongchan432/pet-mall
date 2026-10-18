package com.petmall.controller;

import com.petmall.common.Result;
import com.petmall.entity.Order;
import com.petmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    public Result<Order> createOrder(@RequestBody Order order, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        order.setUserId(userId);
        try {
            Order createdOrder = orderService.createOrder(order);
            return Result.success(createdOrder);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<Order> getOrderById(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer role = (Integer) request.getAttribute("role");
        Order order = orderService.getOrderById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (role != 2 && !order.getUserId().equals(userId) && !order.getMerchantId().equals(userId)) {
            return Result.error(403, "无权限访问");
        }
        return Result.success(order);
    }

    @GetMapping("/list")
    public Result<List<Order>> getUserOrders(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Order> orders = orderService.getUserOrders(userId);
        return Result.success(orders);
    }

    @PostMapping("/pay")
    public Result<String> payOrder(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long orderId = Long.valueOf(params.get("orderId").toString());
        Integer paymentMethod = Integer.valueOf(params.get("paymentMethod").toString());
        String password = params.get("password") != null ? params.get("password").toString() : null;
        Long couponUserId = params.get("couponUserId") != null ? Long.valueOf(params.get("couponUserId").toString()) : null;
        Order order = orderService.getOrderById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            return Result.error("订单不存在或无权限");
        }
        try {
            if (orderService.payOrder(orderId, paymentMethod, password, couponUserId)) {
                return Result.success("支付成功");
            }
            return Result.error("支付失败");
        } catch (Exception e) {
            return Result.error(e.getMessage() != null ? e.getMessage() : "支付失败");
        }
    }

    @PostMapping("/receive")
    public Result<String> receiveOrder(@RequestBody Map<String, Long> params, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long orderId = params.get("orderId");
        Order order = orderService.getOrderById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            return Result.error("订单不存在或无权限");
        }
        if (orderService.receiveOrder(orderId)) {
            return Result.success("确认收货成功");
        }
        return Result.error("确认收货失败");
    }

    @PostMapping("/cancel")
    public Result<String> cancelOrder(@RequestBody Map<String, Long> params, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long orderId = params.get("orderId");
        Order order = orderService.getOrderById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            return Result.error("订单不存在或无权限");
        }
        if (orderService.cancelOrder(orderId)) {
            return Result.success("取消订单成功");
        }
        return Result.error("取消订单失败");
    }

    @PostMapping("/refund")
    public Result<String> refundOrder(@RequestBody Map<String, Long> params, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long orderId = params.get("orderId");
        Order order = orderService.getOrderById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            return Result.error("订单不存在或无权限");
        }
        if (orderService.refundOrder(orderId)) {
            return Result.success("退款申请成功");
        }
        return Result.error("退款申请失败");
    }

    // 商家接口
    @GetMapping("/merchant/list")
    public Result<List<Order>> getMerchantOrders(@RequestParam(required = false) Integer status,
                                                 HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role != 1) {
            return Result.error(403, "无权限访问");
        }
        Long merchantId = (Long) request.getAttribute("userId");
        List<Order> orders = orderService.getMerchantOrders(merchantId);
        if (status != null) {
            orders = orders.stream()
                    .filter(o -> o.getStatus().equals(status))
                    .collect(java.util.stream.Collectors.toList());
        }
        return Result.success(orders);
    }

    @PostMapping("/merchant/ship")
    public Result<String> shipOrder(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role != 1) {
            return Result.error(403, "无权限访问");
        }
        Long merchantId = (Long) request.getAttribute("userId");
        Long orderId = Long.valueOf(params.get("orderId").toString());
        String logisticsNo = params.get("logisticsNo").toString();
        String logisticsCompany = params.get("logisticsCompany").toString();
        Order order = orderService.getOrderById(orderId);
        if (order == null || !order.getMerchantId().equals(merchantId)) {
            return Result.error("订单不存在或无权限");
        }
        if (orderService.shipOrder(orderId, logisticsNo, logisticsCompany)) {
            return Result.success("发货成功");
        }
        return Result.error("发货失败");
    }

    @PostMapping("/merchant/refund-process")
    public Result<String> processRefund(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role != 1) {
            return Result.error(403, "无权限访问");
        }
        Long merchantId = (Long) request.getAttribute("userId");
        Long orderId = Long.valueOf(params.get("orderId").toString());
        Boolean agree = Boolean.valueOf(params.get("agree").toString());
        Order order = orderService.getOrderById(orderId);
        if (order == null || !order.getMerchantId().equals(merchantId)) {
            return Result.error("订单不存在或无权限");
        }
        if (orderService.processRefund(orderId, agree)) {
            return Result.success(agree ? "已同意退款" : "已拒绝退款");
        }
        return Result.error("退款处理失败");
    }

    @GetMapping("/track/{id}")
    public Result<List<Map<String, Object>>> getOrderTrack(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer role = (Integer) request.getAttribute("role");
        Order order = orderService.getOrderById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (role != 2 && !order.getUserId().equals(userId) && !order.getMerchantId().equals(userId)) {
            return Result.error(403, "无权限访问");
        }
        List<Map<String, Object>> tracks = new ArrayList<>();
        if (order.getShipTime() == null) {
            return Result.success(tracks);
        }
        // ponytail: 轨迹按发货时间推算（种子单号为假号，真实快递API接不上）；接快递100只需替换本段
        long shipMs = order.getShipTime().getTime();
        long now = System.currentTimeMillis();
        String company = order.getLogisticsCompany() == null ? "快递" : order.getLogisticsCompany();
        long[] offsets = {0L, 6 * 3600_000L, 24 * 3600_000L, 48 * 3600_000L};
        String[] descs = {
            company + "已揽收，物流单号：" + order.getLogisticsNo(),
            "快件已发出，正在运往目的地",
            "快件已到达【本地转运中心】",
            "快件正在派送中，请保持电话畅通"
        };
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        boolean signed = order.getReceiveTime() != null;
        for (int i = 0; i < offsets.length; i++) {
            long t = shipMs + offsets[i];
            if (signed || now >= t) {
                tracks.add(trackNode(fmt.format(new Date(t)), descs[i]));
            }
        }
        if (signed) {
            tracks.add(trackNode(fmt.format(order.getReceiveTime()), "快件已签收，感谢使用" + company));
        }
        Collections.reverse(tracks);
        return Result.success(tracks);
    }

    private Map<String, Object> trackNode(String time, String description) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("time", time);
        node.put("description", description);
        return node;
    }

    // 管理员接口
    @GetMapping("/admin/list")
    public Result<List<Order>> getAllOrders(@RequestParam(required = false) Integer status,
                                            HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role != 2) {
            return Result.error(403, "无权限访问");
        }
        List<Order> orders = orderService.getAllOrders(status);
        return Result.success(orders);
    }
}



