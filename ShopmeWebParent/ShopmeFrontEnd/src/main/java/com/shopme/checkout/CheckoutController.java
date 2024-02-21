package com.shopme.checkout;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.shopme.ControllerHelper;
import com.shopme.Utility;
import com.shopme.address.AddressService;
import com.shopme.common.entity.*;
import com.shopme.common.entity.order.Order;
import com.shopme.common.entity.order.PaymentMethod;
import com.shopme.coupon.CouponService;
import com.shopme.customer.CustomerService;
import com.shopme.order.OrderService;
import com.shopme.setting.CurrencySettingBag;
import com.shopme.setting.EmailSettingBag;
import com.shopme.setting.PaymentSettingBag;
import com.shopme.setting.SettingService;
import com.shopme.shipping.ShippingRateService;
import com.shopme.shoppingcart.ShoppingCartService;
import com.shopme.wallet.WalletService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class CheckoutController {

	@Autowired private CheckoutService checkoutService;
	@Autowired private ControllerHelper controllerHelper;
	@Autowired private AddressService addressService;
	@Autowired private ShippingRateService shipService;
	@Autowired private ShoppingCartService cartService;
	@Autowired private OrderService orderService;
	@Autowired private SettingService settingService;
	@Autowired private CouponService couponService;
	@Autowired private CustomerService customerService;
	@Autowired private WalletService walletService;
	@GetMapping("/checkout")
	public String showCheckoutPage(Model model, HttpServletRequest request) {
		Customer customer = controllerHelper.getAuthenticatedCustomer(request);
		
		Address defaultAddress = addressService.getDefaultAddress(customer);
		ShippingRate shippingRate = null;
		
		if (defaultAddress != null) {
			model.addAttribute("shippingAddress", defaultAddress.toString());
			shippingRate = shipService.getShippingRateForAddress(defaultAddress);
		} else {
			model.addAttribute("shippingAddress", customer.toString());
			shippingRate = shipService.getShippingRateForCustomer(customer);
		}
		
		if (shippingRate == null) {
			return "redirect:/cart";
		}
		
		List<CartItem> cartItems = cartService.listCartItems(customer);
		CheckoutInfo checkoutInfo = checkoutService.prepareCheckout(cartItems, shippingRate);

		String currencyCode = settingService.getCurrencyCode();
		PaymentSettingBag paymentSettings = settingService.getPaymentSettings();

		Wallet wallet = walletService.findWalletByCustomer(customer);


		model.addAttribute("currencyCode", currencyCode);
		model.addAttribute("customer", customer);
		model.addAttribute("coupon",new Coupon());
		model.addAttribute("checkoutInfo", checkoutInfo);
		model.addAttribute("cartItems", cartItems);
		model.addAttribute("wallet",wallet);
		return "checkout/checkout";
	}

	private Customer getAuthenticatedCustomer(HttpServletRequest request) {
		String email = Utility.getEmailOfAuthenticatedCustomer(request);
		return customerService.getCustomerByEmail(email);
	}

	@PostMapping("/place_order")
	public String placeOrder(HttpServletRequest request)
			throws UnsupportedEncodingException, MessagingException {
		String paymentType = request.getParameter("paymentMethod");
		PaymentMethod paymentMethod = PaymentMethod.valueOf(paymentType);

		Customer customer = controllerHelper.getAuthenticatedCustomer(request);

		Address defaultAddress = addressService.getDefaultAddress(customer);
		ShippingRate shippingRate = null;

		if (defaultAddress != null) {
			shippingRate = shipService.getShippingRateForAddress(defaultAddress);
		} else {
			shippingRate = shipService.getShippingRateForCustomer(customer);
		}

		List<CartItem> cartItems = cartService.listCartItems(customer);
		CheckoutInfo checkoutInfo = checkoutService.prepareCheckout(cartItems, shippingRate);

		Order createdOrder = orderService.createOrder(customer, defaultAddress, cartItems, paymentMethod, checkoutInfo);
		cartService.deleteByCustomer(customer);
		sendOrderConfirmationEmail(request, createdOrder);

		return "checkout/order_completed";
	}

	private void sendOrderConfirmationEmail(HttpServletRequest request, Order order)
			throws UnsupportedEncodingException, MessagingException {
		EmailSettingBag emailSettings = settingService.getEmailSettings();
		JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettings);
		mailSender.setDefaultEncoding("utf-8");

		String toAddress = order.getCustomer().getEmail();
		String subject = emailSettings.getOrderConfirmationSubject();
		String content = emailSettings.getOrderConfirmationContent();

		subject = subject.replace("[[orderId]]", String.valueOf(order.getId()));

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		helper.setFrom(emailSettings.getFromAddress(), emailSettings.getSenderName());
		helper.setTo(toAddress);
		helper.setSubject(subject);

		DateFormat dateFormatter =  new SimpleDateFormat("HH:mm:ss E, dd MMM yyyy");
		String orderTime = dateFormatter.format(order.getOrderTime());

		CurrencySettingBag currencySettings = settingService.getCurrencySettings();
		String totalAmount = Utility.formatCurrency(order.getTotal(), currencySettings);

		content = content.replace("[[name]]", order.getCustomer().getFullName());
		content = content.replace("[[orderId]]", String.valueOf(order.getId()));
		content = content.replace("[[orderTime]]", orderTime);
		content = content.replace("[[shippingAddress]]", order.getShippingAddress());
		content = content.replace("[[total]]", totalAmount);
		content = content.replace("[[paymentMethod]]", order.getPaymentMethod().toString());

		helper.setText(content, true);
		mailSender.send(message);
	}

	@PostMapping("/create_order")
	@ResponseBody
	public String create(@RequestBody Map<String,Object> data) throws RazorpayException {
		System.out.println("The order function executed");
		System.out.println(data);

		Double amtDouble = Double.parseDouble(data.get("amount").toString());
		int amt = (int) (amtDouble * 100 / 100);
			var client =new RazorpayClient("rzp_test_KYtqpMJ35eljqL","j0ZbwMlpUd6NEW91gKzCqaMu");
		JSONObject ob = new JSONObject();
		ob.put("amount",amt);
		ob.put("currency","INR");
//        ob.put("reciept","txn_1213");
//        ob.put("receipt",);
		com.razorpay.Order order = client.orders.create(ob);
		System.out.println(order);

		return order.toString();
	}

	@PostMapping("/apply-coupon")
	public String applyCoupon(@ModelAttribute("coupon") Coupon coupon,
							  RedirectAttributes redirectAttributes,
							  HttpServletRequest request,
							  Model model) {
		Optional<Coupon> optionalCoupon = couponService.findCouponByCode(coupon.getCode());

//        String checkoutInfo = request.getParameter("checkoutInfo");
		Customer customer = getAuthenticatedCustomer(request);

		Address defaultAddress = addressService.getDefaultAddress(customer);
		ShippingRate shippingRate = null;

		if (defaultAddress != null) {
			shippingRate = shipService.getShippingRateForAddress(defaultAddress);
		} else {
			shippingRate = shipService.getShippingRateForCustomer(customer);
		}

		List<CartItem> cartItems = cartService.listCartItems(customer);
		CheckoutInfo checkoutInfo = checkoutService.prepareCheckout(cartItems, shippingRate);

		String currencyCode = settingService.getCurrencyCode();
		PaymentSettingBag paymentSettings = settingService.getPaymentSettings();
		Wallet wallet = walletService.findWalletByCustomer(customer);

		model.addAttribute("currencyCode", currencyCode);
		model.addAttribute("customer", customer);

		model.addAttribute("coupon",new Coupon());
		model.addAttribute("cartItems", cartItems);
		model.addAttribute("wallet",wallet);

		System.out.println(checkoutInfo.getPaymentTotal());

		if (optionalCoupon.isPresent()) {
			System.out.println("inside the coupon");
			System.out.println(checkoutInfo.getPaymentTotal());

			int discountPercentage = optionalCoupon.get().getDiscount();
			System.out.println(discountPercentage);
			float updatedPayment = checkoutInfo.getPaymentTotal()-((discountPercentage*checkoutInfo.getPaymentTotal())/100);

			checkoutInfo.setPaymentTotal(updatedPayment);
			checkoutInfo.setRazorPayTotal(updatedPayment*100);

			System.out.println(updatedPayment);

			redirectAttributes.addFlashAttribute("successMessage", "Coupon applied successfully!");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "Invalid coupon code!");
		}
		model.addAttribute("checkoutInfo", checkoutInfo);

		return "checkout/checkout";
	}
//for wallet updating balance
	@PostMapping("/updateBalanceWallet")
	@ResponseBody
	public Wallet updateWallet(Long walletId,Double amountToReduce){
		Wallet wallet = walletService.getWalletById(walletId);

		System.out.println("Inside update wallet method"+walletId);
		Double balance = wallet.getBalance();


		if(amountToReduce<=balance){
			walletService.reduceBalance(walletId,amountToReduce);
		}else {
			return null;
		}
		return wallet;
	}


}
