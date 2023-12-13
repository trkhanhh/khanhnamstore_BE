package com.web.api;
import com.web.config.Environment;
import com.web.constants.LogUtils;
import com.web.constants.RequestType;
import com.web.dto.request.PaymentDto;
import com.web.dto.request.ProductSizeRequest;
import com.web.dto.response.ResponsePayment;
import com.web.entity.ProductSize;
import com.web.entity.Voucher;
import com.web.exception.MessageException;
import com.web.models.PaymentResponse;
import com.web.models.QueryStatusTransactionResponse;
import com.web.processor.CreateOrderMoMo;
import com.web.processor.QueryTransactionStatus;
import com.web.repository.ProductSizeRepository;
import com.web.servive.VoucherService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class MomoApi {

    @Getter
    @Setter
    @AllArgsConstructor
    private class CheckPaymentResponse{
        private String message;
        private boolean success;
    }
    @Autowired
    private ProductSizeRepository productSizeRepository;

    @Autowired
    private VoucherService voucherService;

    @PostMapping("/urlpayment")
    public ResponsePayment getUrlPayment(@RequestBody PaymentDto paymentDto){
        LogUtils.init();
        Double totalAmount = 0D;
        for(ProductSizeRequest p : paymentDto.getListProductSize()){
            if(p.getIdProductSize() == null){
                throw new MessageException("id product size require");
            }
            Optional<ProductSize> productSize = productSizeRepository.findById(p.getIdProductSize());
            if(productSize.isEmpty()){
                throw new MessageException("product size: "+p.getIdProductSize()+" not found");
            }
            if(productSize.get().getQuantity() < p.getQuantity()){
                throw new MessageException("product size: "+p.getIdProductSize()+" not enough quantity");
            }
            totalAmount += productSize.get().getProductColor().getProduct().getPrice() * p.getQuantity();
        }
        if(paymentDto.getCodeVoucher() != null){
            Optional<Voucher> voucher = voucherService.findByCode(paymentDto.getCodeVoucher(), totalAmount);
            if(voucher.isPresent()){
                totalAmount = totalAmount - voucher.get().getDiscount();
            }
        }
        totalAmount += 20000;
        //tai sao lai cong 20k vao day
        Long td = Math.round(totalAmount);
        String orderId = String.valueOf(System.currentTimeMillis());
        String requestId = String.valueOf(System.currentTimeMillis());
        Environment environment = Environment.selectEnv("dev");
        PaymentResponse captureATMMoMoResponse = null;
        try {
            captureATMMoMoResponse = CreateOrderMoMo.process(environment, orderId, requestId, Long.toString(td), paymentDto.getContent(), paymentDto.getReturnUrl(), paymentDto.getNotifyUrl(), "", RequestType.PAY_WITH_ATM, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("url ====: "+captureATMMoMoResponse.getPayUrl());
        ResponsePayment responsePayment = new ResponsePayment(captureATMMoMoResponse.getPayUrl(),orderId,requestId);
        return responsePayment;
    }


    @GetMapping("/checkPayment")
    public ResponseEntity<?> checkPayment(@RequestParam("orderId") String orderId, @RequestParam("requestId") String requestId) throws Exception {
        LogUtils.init();

        Environment environment = Environment.selectEnv("dev");
        QueryStatusTransactionResponse queryStatusTransactionResponse = QueryTransactionStatus.process(environment, orderId, requestId);
        System.out.println("qqqq-----------------------------------------------------------"+queryStatusTransactionResponse.getMessage());
        System.out.println("qqqq-----------------------------------------------------------"+queryStatusTransactionResponse.getResultCode());

        if(queryStatusTransactionResponse.getResultCode() == 0){
            return new ResponseEntity<>(new CheckPaymentResponse("Thanh toán thành công",true), HttpStatus.OK);
        }
        return new ResponseEntity<>(new CheckPaymentResponse("Đơn hàng chưa được thanh toán",false), HttpStatus.OK);
    }
}
