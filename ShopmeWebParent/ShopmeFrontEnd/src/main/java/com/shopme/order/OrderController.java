package com.shopme.order;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.shopme.ControllerHelper;
import com.shopme.common.entity.Customer;
import com.shopme.common.entity.order.Order;
import com.shopme.common.entity.order.OrderDetail;
import com.shopme.common.entity.product.Product;
import com.shopme.review.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.lowagie.text.FontFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Controller
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private ControllerHelper controllerHelper;
    @Autowired
    private ReviewService reviewService;

    @GetMapping("/orders")
    public String listFirstPage(Model model, HttpServletRequest request) {
        return listOrdersByPage(model, request, 1, "orderTime", "desc", null);
    }

    @GetMapping("/orders/page/{pageNum}")
    public String listOrdersByPage(Model model, HttpServletRequest request,
                                   @PathVariable(name = "pageNum") int pageNum,
                                   String sortField, String sortDir, String keyword) {
        Customer customer = controllerHelper.getAuthenticatedCustomer(request);

        Page<Order> page = orderService.listForCustomerByPage(customer, pageNum, sortField, sortDir, keyword);
        List<Order> listOrders = page.getContent();

        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("totalItems", page.getTotalElements());
        model.addAttribute("currentPage", pageNum);
        model.addAttribute("listOrders", listOrders);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("keyword", keyword);
        model.addAttribute("moduleURL", "/orders");
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");

        long startCount = (pageNum - 1) * OrderService.ORDERS_PER_PAGE + 1;
        model.addAttribute("startCount", startCount);

        long endCount = startCount + OrderService.ORDERS_PER_PAGE - 1;
        if (endCount > page.getTotalElements()) {
            endCount = page.getTotalElements();
        }

        model.addAttribute("endCount", endCount);

        return "orders/orders_customer";
    }

    @GetMapping("/orders/detail/{id}")
    public String viewOrderDetails(Model model,
                                   @PathVariable(name = "id") Integer id, HttpServletRequest request) {
        Customer customer = controllerHelper.getAuthenticatedCustomer(request);
        Order order = orderService.getOrder(id, customer);

        setProductReviewableStatus(customer, order);

        model.addAttribute("order", order);

        return "orders/order_details_modal";
    }

    private void setProductReviewableStatus(Customer customer, Order order) {
        Iterator<OrderDetail> iterator = order.getOrderDetails().iterator();

        while(iterator.hasNext()) {
            OrderDetail orderDetail = iterator.next();
            Product product = orderDetail.getProduct();
            Integer productId = product.getId();

            boolean didCustomerReviewProduct = reviewService.didCustomerReviewProduct(customer, productId);
            product.setReviewedByCustomer(didCustomerReviewProduct);

            if (!didCustomerReviewProduct) {
                boolean canCustomerReviewProduct = reviewService.canCustomerReviewProduct(customer, productId);
                product.setCustomerCanReview(canCustomerReviewProduct);
            }

        }
    }

    @GetMapping("/orders/invoice/pdf/{id}")
    public void exportInvoiceToPDF(HttpServletResponse response, @PathVariable(name = "id") Integer id) throws IOException {
        try {
            Order order = orderService.getOrderById(id);

            if (order != null) {
                // Set response headers
                response.setContentType("application/pdf");
                String headerKey = "Content-Disposition";
                String headerValue = "attachment; filename=invoice_" + order.getId() + ".pdf";
                response.setHeader(headerKey, headerValue);

                // Create PDF document
                Document document = new Document();
                PdfWriter.getInstance(document, response.getOutputStream());

                // Open the document
                document.open();

                // Add content to the document
                addInvoiceContent(document, order);

                // Close the document
                document.close();
            } else {
                // Handle case when order is not found
                response.getWriter().println("Order with ID " + id + " not found");
            }
        } catch (IOException | DocumentException e) {
            // Handle document processing or response writing errors
            e.printStackTrace();
            // You might want to log the error and provide a meaningful response to the client
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void addInvoiceContent(Document document, Order order) throws DocumentException {


        // Add invoice details to the document
        Paragraph title = new Paragraph("Invoice Details", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16));
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(Chunk.NEWLINE);

        // Buyer information
        Paragraph buyerInfo = new Paragraph("Buyer: " + order.getFirstName(), FontFactory.getFont(FontFactory.HELVETICA, 12));
        document.add(buyerInfo);
        document.add(Chunk.NEWLINE);

        // Shipping information
        Paragraph shippingInfo = new Paragraph("Shipping Address: " + order.getShippingAddress(), FontFactory.getFont(FontFactory.HELVETICA, 12));
        document.add(shippingInfo);
        document.add(Chunk.NEWLINE);

        // Order details
        Paragraph orderId = new Paragraph("Order ID: " + order.getId(), FontFactory.getFont(FontFactory.HELVETICA, 12));
        document.add(orderId);
        document.add(Chunk.NEWLINE);

        // Product details
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        PdfPCell cell1 = new PdfPCell(new Paragraph("Product", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        PdfPCell cell2 = new PdfPCell(new Paragraph("Quantity", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        PdfPCell cell3 = new PdfPCell(new Paragraph("Unit Price", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        PdfPCell cell4 = new PdfPCell(new Paragraph("Subtotal", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));

        table.addCell(cell1);
        table.addCell(cell2);
        table.addCell(cell3);
        table.addCell(cell4);

        for (OrderDetail detail : order.getOrderDetails()) {
            table.addCell(detail.getProduct().getShortName());
            table.addCell(String.valueOf(detail.getQuantity()));
            table.addCell(String.valueOf(detail.getUnitPrice()));
            table.addCell(String.valueOf(detail.getSubtotal()));
        }

        document.add(table);
        document.add(Chunk.NEWLINE);

        // Order time
        Paragraph orderTime = new Paragraph("Order Time: " + order.getOrderTime(), FontFactory.getFont(FontFactory.HELVETICA, 12));
        document.add(orderTime);
        document.add(Chunk.NEWLINE);

        // Totals
        Paragraph productTotal = new Paragraph("Product Total: " + order.getProductCost(), FontFactory.getFont(FontFactory.HELVETICA, 12));
        document.add(productTotal);
        document.add(Chunk.NEWLINE);

        Paragraph shippingTotal = new Paragraph("Shipping Total: " + order.getShippingCost(), FontFactory.getFont(FontFactory.HELVETICA, 12));
        document.add(shippingTotal);
        document.add(Chunk.NEWLINE);

        Paragraph paymentTotal = new Paragraph("Payment Total: " + order.getTotal(), FontFactory.getFont(FontFactory.HELVETICA, 12));
        document.add(paymentTotal);

        Paragraph copyright = new Paragraph("Copyright © 2024 Gamezone. All rights reserved.", FontFactory.getFont(FontFactory.HELVETICA, 10));
        copyright.setAlignment(Element.ALIGN_CENTER);
        document.add(copyright);
    }
}
