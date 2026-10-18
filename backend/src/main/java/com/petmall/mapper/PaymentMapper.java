package com.petmall.mapper;

import com.petmall.entity.Payment;
import java.util.List;

public interface PaymentMapper {
    Payment selectById(Long id);
    Payment selectByOrderId(Long orderId);
    Payment selectByPaymentNo(String paymentNo);
    int insert(Payment payment);
    int update(Payment payment);
}



