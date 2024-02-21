package com.shopme.wallet;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.shopme.ControllerHelper;
import com.shopme.common.entity.Customer;
import com.shopme.common.entity.Wallet;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Controller
public class WalletController {

    @Autowired
    private WalletService walletService;
    @Autowired
    private ControllerHelper controllerHelper;

    @GetMapping("/walletCreate")
    public String createWallet(HttpServletRequest request){
        Customer customer=controllerHelper.getAuthenticatedCustomer(request);
        walletService.createWallet(customer);
        return "redirect:/wallet";
    }

    @GetMapping("/wallet")
    public String getWallet(Model model,HttpServletRequest request){
        Customer customer = controllerHelper.getAuthenticatedCustomer(request);
        if(walletService.findWalletByCustomer(customer)==null){
            walletService.createWallet(customer);
        }
        Wallet wallet = walletService.findWalletByCustomer(customer);
        System.out.println("Wallet id :"+wallet.getId());
        model.addAttribute("wallet",wallet);
        return "wallet/wallet";
    }

    @PostMapping("/walletDeposit")
    @ResponseBody
    public String depositFunds(HttpServletRequest request, @RequestBody Map<String,Object> data) throws RazorpayException {

        System.out.println("The order function executed");
        System.out.println(data);

        double amt = Double.parseDouble(data.get("amount").toString());

        System.out.println(amt);

        double razorpayTotal = amt*100;

        var client =new RazorpayClient("rzp_test_KYtqpMJ35eljqL","j0ZbwMlpUd6NEW91gKzCqaMu");
        JSONObject ob = new JSONObject();
        ob.put("amount",razorpayTotal);
        ob.put("currency","INR");
//        ob.put("reciept","txn_1213");
//        ob.put("receipt",);
        com.razorpay.Order order = client.orders.create(ob);
        System.out.println(order);

        return order.toString();

    }

    @GetMapping("/onRazorpaySuccess")
    public String successAchanadaodjhagfudafikajsfasdhjfahf0afhadlfha(HttpServletRequest request,@RequestParam ("amount") Double amount){
        Customer  customer= controllerHelper.getAuthenticatedCustomer(request);

        System.out.println(amount);


        walletService.depositFunds(customer,amount);
        return "redirect:/wallet";
    }





//
//    @GetMapping("/walletWithdraw")
//    public String withdrawFunds(HttpServletRequest request, Double amount) {
//        Customer customer = controllerHelper.getAuthenticatedCustomer(request);
//        walletService.withdrawFunds(customer, amount);
//        return "wallet/wallet";
//    }

    @GetMapping("/walletBalance")
    public String checkWalletBalance(HttpServletRequest request) {
        Customer customer = controllerHelper.getAuthenticatedCustomer(request);
        double balance = walletService.checkWalletBalance(customer);
        System.out.println(balance);
        request.setAttribute("balance", balance);
        return "wallet/wallet";
    }

    private String generateOrderId() {
        return "ORD_" + UUID.randomUUID().toString();
    }

    private void createRazorpayOrder(String orderId, Double amount) {
        try {
            RazorpayClient razorpay = new RazorpayClient("rzp_test_KYtqpMJ35eljqL", "j0ZbwMlpUd6NEW91gKzCqaMu");
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount * 100); // Razorpay accepts amount in paisa
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", orderId);
            Order order = razorpay.orders.create(orderRequest);
            System.out.println(order.toJson());
        } catch (RazorpayException e) {
            e.printStackTrace();
        }
    }



//    @PostMapping("/razorpayCallback")
//    public String razorpayCallback(HttpServletRequest request, @RequestBody String razorpayPaymentId) {
//        // Handle Razorpay callback response here
//        Customer customer = controllerHelper.getAuthenticatedCustomer(request);
//        // Update wallet balance based on Razorpay payment response
//        // You need to implement this method
//        return "redirect:/wallet";
//    }


}