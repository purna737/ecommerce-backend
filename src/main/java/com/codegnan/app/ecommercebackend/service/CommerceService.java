package com.codegnan.app.ecommercebackend.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommerceService {
    private final JdbcTemplate jdbc;

    public CommerceService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Map<String, Object> getCart(Long userId) {
        Long cartId = ensureCart(userId);
        List<Map<String, Object>> items = jdbc.query(
            "SELECT ci.product_id AS productId, p.name, p.brand, p.price, ci.quantity, (p.price * ci.quantity) AS lineTotal, COALESCE(i.quantity,0) AS stock " +
            "FROM cart_items ci JOIN products p ON p.product_id=ci.product_id LEFT JOIN inventory i ON i.product_id=p.product_id WHERE ci.cart_id=? ORDER BY ci.cart_item_id",
            (rs, n) -> mapCartItem(rs), cartId);
        BigDecimal subtotal = items.stream().map(i -> (BigDecimal)i.get("lineTotal")).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String,Object> result = new LinkedHashMap<>(); result.put("cartId", cartId); result.put("items", items); result.put("subtotal", subtotal); result.put("itemCount", items.stream().mapToInt(i -> ((Number)i.get("quantity")).intValue()).sum());
        return result;
    }

    @Transactional
    public Map<String,Object> addCartItem(Long userId, Long productId, int quantity) {
        if (quantity < 1 || quantity > 99) throw new IllegalArgumentException("Quantity must be between 1 and 99.");
        List<Long> activeProduct = jdbc.query("SELECT product_id FROM products WHERE product_id=? AND UPPER(status) IN ('ACTIVE','PUBLISHED')", (rs,n)->rs.getLong(1), productId);
        if (activeProduct.isEmpty()) throw new IllegalArgumentException("Product was not found or is not active.");
        Long cartId = ensureCart(userId);
        List<Integer> existing = jdbc.query("SELECT quantity FROM cart_items WHERE cart_id=? AND product_id=?", (rs,n)->rs.getInt(1), cartId, productId);
        if (existing.isEmpty()) jdbc.update("INSERT INTO cart_items(cart_id, product_id, quantity, created_at, updated_at) VALUES(?,?,?,?,?)", cartId, productId, quantity, LocalDateTime.now(), LocalDateTime.now());
        else jdbc.update("UPDATE cart_items SET quantity=?, updated_at=? WHERE cart_id=? AND product_id=?", Math.min(99, existing.get(0)+quantity), LocalDateTime.now(), cartId, productId);
        jdbc.update("UPDATE carts SET updated_at=?, version=version+1 WHERE cart_id=?", LocalDateTime.now(), cartId);
        return getCart(userId);
    }

    @Transactional
    public Map<String,Object> setCartItem(Long userId, Long productId, int quantity) {
        Long cartId = ensureCart(userId);
        if (quantity <= 0) jdbc.update("DELETE FROM cart_items WHERE cart_id=? AND product_id=?", cartId, productId);
        else {
            if (quantity > 99) throw new IllegalArgumentException("Maximum quantity per product is 99.");
            int changed = jdbc.update("UPDATE cart_items SET quantity=?, updated_at=? WHERE cart_id=? AND product_id=?", quantity, LocalDateTime.now(), cartId, productId);
            if (changed == 0) throw new IllegalArgumentException("Product is not in the cart.");
        }
        jdbc.update("UPDATE carts SET updated_at=?, version=version+1 WHERE cart_id=?", LocalDateTime.now(), cartId);
        return getCart(userId);
    }

    @Transactional
    public Map<String,Object> removeCartItem(Long userId, Long productId) { return setCartItem(userId, productId, 0); }
    @Transactional
    public Map<String,Object> clearCart(Long userId) { Long id=ensureCart(userId); jdbc.update("DELETE FROM cart_items WHERE cart_id=?", id); jdbc.update("UPDATE carts SET updated_at=?, version=version+1 WHERE cart_id=?", LocalDateTime.now(), id); return getCart(userId); }

    public List<Map<String,Object>> inventory() {
        return jdbc.query("SELECT i.product_id AS productId,p.name,CAST(p.product_id AS CHAR) AS sku,i.quantity,i.reorder_level AS reorderLevel,i.updated_at AS updatedAt FROM inventory i JOIN products p ON p.product_id=i.product_id ORDER BY p.name", (rs,n)->{
            Map<String,Object> m=new LinkedHashMap<>(); m.put("productId",rs.getLong("productId")); m.put("name",rs.getString("name")); m.put("sku",rs.getString("sku")); m.put("quantity",rs.getInt("quantity")); m.put("reorderLevel",rs.getInt("reorderLevel")); m.put("updatedAt",rs.getTimestamp("updatedAt").toLocalDateTime()); return m;
        });
    }

    @Transactional
    public Map<String,Object> adjustInventory(Long productId, int delta) {
        if (delta == 0) throw new IllegalArgumentException("Stock adjustment cannot be zero.");
        int changed=jdbc.update("UPDATE inventory SET quantity=quantity+?, updated_at=?, version=version+1 WHERE product_id=? AND quantity+?>=0", delta, LocalDateTime.now(), productId, delta);
        if(changed==0) throw new IllegalArgumentException("Product inventory not found or adjustment would make stock negative.");
        return inventory().stream().filter(i->((Number)i.get("productId")).longValue()==productId).findFirst().orElseThrow();
    }

    @Transactional
    public Map<String,Object> checkout(Long userId, Map<String,Object> payload) {
        Long addressId = number(payload.get("addressId"), "addressId");
        List<Long> ownedAddress=jdbc.query("SELECT address_id FROM addresses WHERE address_id=? AND user_id=? AND active=1",(rs,n)->rs.getLong(1),addressId,userId);
        if(ownedAddress.isEmpty()) throw new IllegalArgumentException("Select an active address that belongs to your account.");
        Object rawItems=payload.get("items");
        if (!(rawItems instanceof List<?> requested) || requested.isEmpty()) throw new IllegalArgumentException("Cart is empty.");
        String method=String.valueOf(payload.getOrDefault("paymentMethod","MOCK" )).trim().toUpperCase();
        if (!List.of("MOCK", "MOCK_DECLINE", "UPI", "CARD", "COD").contains(method)) throw new IllegalArgumentException("Unsupported simulated payment method.");
        boolean declined = method.equals("MOCK_DECLINE");
        boolean cashOnDelivery = method.equals("COD");
        String paymentStatus = declined ? "FAILED" : (cashOnDelivery ? "PENDING" : "SUCCEEDED");
        Map<Long,Integer> requestedQuantities=new LinkedHashMap<>();
        for(Object obj:requested){
            if(!(obj instanceof Map<?,?> line)) throw new IllegalArgumentException("Invalid cart item.");
            Long requestedProductId=number(line.get("productId"),"productId"); int requestedQty=number(line.get("quantity"),"quantity").intValue();
            if(requestedQty<1 || requestedQty>99) throw new IllegalArgumentException("Quantity must be between 1 and 99.");
            int merged=requestedQuantities.getOrDefault(requestedProductId,0)+requestedQty;
            if(merged>99) throw new IllegalArgumentException("Maximum quantity per product is 99.");
            requestedQuantities.put(requestedProductId,merged);
        }
        List<Map<String,Object>> normalized=new ArrayList<>(); BigDecimal subtotal=BigDecimal.ZERO;
        for(Map.Entry<Long,Integer> requestedLine:requestedQuantities.entrySet()){
            Long productId=requestedLine.getKey(); int qty=requestedLine.getValue();
            List<Map<String,Object>> productRows=jdbc.query("SELECT product_id,name,price,status FROM products WHERE product_id=?",(rs,n)->{Map<String,Object> m=new LinkedHashMap<>();m.put("product_id",rs.getLong("product_id"));m.put("name",rs.getString("name"));m.put("price",rs.getBigDecimal("price"));m.put("status",rs.getString("status"));return m;},productId);
            if(productRows.isEmpty()) throw new IllegalArgumentException("Product not found: "+productId);
            Map<String,Object> p=productRows.get(0);
            String status=String.valueOf(p.get("status")); if(!status.equalsIgnoreCase("ACTIVE") && !status.equalsIgnoreCase("PUBLISHED")) throw new IllegalArgumentException("A selected product is not active.");
            List<Integer> stocks=jdbc.query("SELECT quantity FROM inventory WHERE product_id=? FOR UPDATE",(rs,n)->rs.getInt(1),productId);
            if(stocks.isEmpty()) throw new IllegalArgumentException("Inventory has not been configured for product "+productId+".");
            if(!declined && stocks.get(0)<qty) throw new IllegalArgumentException("Insufficient stock for "+p.get("name")+". Available: "+stocks.get(0));
            BigDecimal price=(BigDecimal)p.get("price"); BigDecimal lineTotal=price.multiply(BigDecimal.valueOf(qty)); subtotal=subtotal.add(lineTotal);
            Map<String,Object> n=new LinkedHashMap<>(); n.put("productId",productId); n.put("name",p.get("name")); n.put("quantity",qty); n.put("unitPrice",price); n.put("lineTotal",lineTotal); normalized.add(n);
        }
        BigDecimal shipping=BigDecimal.ZERO, tax=BigDecimal.ZERO, total=subtotal.add(shipping).add(tax); LocalDateTime now=LocalDateTime.now();
        String initialStatus=declined?"PAYMENT_FAILED":"CONFIRMED";
        jdbc.update("INSERT INTO orders(user_id,address_id,status,subtotal,shipping_fee,tax_total,total_amount,payment_status,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?)",userId,addressId,initialStatus,subtotal,shipping,tax,total,paymentStatus,now,now);
        Long orderId=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        Map<String,Object> a=jdbc.queryForMap("SELECT label,recipient_name,phone,line1,line2,city,state,postal_code,country_code FROM addresses WHERE address_id=?",addressId);
        jdbc.update("INSERT INTO order_addresses(order_id,label,recipient_name,phone,line1,line2,city,state,postal_code,country_code,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)",orderId,a.get("label"),a.get("recipient_name"),a.get("phone"),a.get("line1"),a.get("line2"),a.get("city"),a.get("state"),a.get("postal_code"),a.get("country_code"),now);
        for(Map<String,Object> line:normalized){
            jdbc.update("INSERT INTO order_items(order_id,product_id,product_name,quantity,unit_price,line_total,created_at) VALUES(?,?,?,?,?,?,?)",orderId,line.get("productId"),line.get("name"),line.get("quantity"),line.get("unitPrice"),line.get("lineTotal"),now);
            if(!declined) { int updated=jdbc.update("UPDATE inventory SET quantity=quantity-?,updated_at=?,version=version+1 WHERE product_id=? AND quantity>=?",line.get("quantity"),now,line.get("productId"),line.get("quantity")); if(updated==0) throw new IllegalArgumentException("Stock changed during checkout. Please retry."); }
        }
        jdbc.update("INSERT INTO payments(order_id,provider,method,status,amount,currency,failure_reason,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?)",orderId,"MOCK",method,paymentStatus,total,"INR",declined?"Simulated payment decline":(cashOnDelivery?"Payment due on delivery":null),now,now);
        jdbc.update("INSERT INTO order_status_history(order_id,old_status,new_status,changed_by_user_id,changed_at,note) VALUES(?,?,?,?,?,?)",orderId,null,initialStatus,userId,now,declined?"Mock payment declined":"Order created with simulated payment");
        if(!declined){ jdbc.update("INSERT INTO shipments(order_id,status,carrier,tracking_number,created_at,updated_at) VALUES(?,?,?,?,?,?)",orderId,"PREPARING","LOCAL","SHIP-"+orderId,now,now); Long cartId=ensureCart(userId); jdbc.update("DELETE FROM cart_items WHERE cart_id=?",cartId); jdbc.update("UPDATE carts SET updated_at=?,version=version+1 WHERE cart_id=?",now,cartId); }
        return getOrder(userId,orderId);
    }

    public List<Map<String,Object>> orders(Long userId) {
        return jdbc.query("SELECT order_id AS orderId,status,subtotal,shipping_fee AS shippingFee,tax_total AS taxTotal,total_amount AS totalAmount,payment_status AS paymentStatus,created_at AS createdAt FROM orders WHERE user_id=? ORDER BY order_id DESC",(rs,n)->orderRow(rs),userId);
    }
    public Map<String,Object> getOrder(Long userId, Long orderId) {
        List<Map<String,Object>> rows=jdbc.query("SELECT order_id AS orderId,status,subtotal,shipping_fee AS shippingFee,tax_total AS taxTotal,total_amount AS totalAmount,payment_status AS paymentStatus,created_at AS createdAt FROM orders WHERE user_id=? AND order_id=?",(rs,n)->orderRow(rs),userId,orderId);
        if(rows.isEmpty()) throw new IllegalArgumentException("Order not found for this customer.");
        Map<String,Object> order=rows.get(0);
        order.put("items",jdbc.query("SELECT product_id AS productId,product_name AS productName,quantity,unit_price AS unitPrice,line_total AS lineTotal FROM order_items WHERE order_id=?",(rs,n)->{Map<String,Object> m=new LinkedHashMap<>();m.put("productId",rs.getLong("productId"));m.put("productName",rs.getString("productName"));m.put("quantity",rs.getInt("quantity"));m.put("unitPrice",rs.getBigDecimal("unitPrice"));m.put("lineTotal",rs.getBigDecimal("lineTotal"));return m;},orderId));
        order.put("shipments",jdbc.query("SELECT shipment_id AS shipmentId,status,carrier,tracking_number AS trackingNumber,created_at AS createdAt,updated_at AS updatedAt FROM shipments WHERE order_id=? ORDER BY shipment_id DESC",(rs,n)->{Map<String,Object> m=new LinkedHashMap<>();m.put("shipmentId",rs.getLong("shipmentId"));m.put("status",rs.getString("status"));m.put("carrier",rs.getString("carrier"));m.put("trackingNumber",rs.getString("trackingNumber"));m.put("createdAt",rs.getTimestamp("createdAt").toLocalDateTime());m.put("updatedAt",rs.getTimestamp("updatedAt").toLocalDateTime());return m;},orderId));
        return order;
    }


    @Transactional
    public Map<String,Object> cancelOrder(Long userId, Long orderId) {
        List<Map<String,Object>> found=jdbc.query("SELECT status,total_amount AS totalAmount,payment_status AS paymentStatus FROM orders WHERE order_id=? AND user_id=? FOR UPDATE",(rs,n)->{Map<String,Object> m=new LinkedHashMap<>();m.put("status",rs.getString("status"));m.put("totalAmount",rs.getBigDecimal("totalAmount"));m.put("paymentStatus",rs.getString("paymentStatus"));return m;},orderId,userId);
        if(found.isEmpty()) throw new IllegalArgumentException("Order not found for this customer.");
        Map<String,Object> order=found.get(0); if(!"CONFIRMED".equals(order.get("status"))) throw new IllegalArgumentException("Only confirmed orders can be cancelled.");
        LocalDateTime now=LocalDateTime.now();
        jdbc.update("UPDATE orders SET status='CANCELLED',updated_at=? WHERE order_id=?",now,orderId);
        if("SUCCEEDED".equals(order.get("paymentStatus"))) {
            List<Long> paymentIds=jdbc.query("SELECT payment_id FROM payments WHERE order_id=? AND status='SUCCEEDED' ORDER BY payment_id DESC LIMIT 1",(rs,n)->rs.getLong(1),orderId);
            if(!paymentIds.isEmpty()) {
                jdbc.update("INSERT INTO refunds(payment_id,refund_key,amount,status,reason,completed_at,created_at,updated_at) VALUES(?,?,?,'SUCCEEDED','CUSTOMER_CANCELLATION',?,?,?)",paymentIds.get(0),"CANCEL-"+orderId,order.get("totalAmount"),now,now,now);
                jdbc.update("UPDATE orders SET payment_status='REFUNDED' WHERE order_id=?",orderId);
            }
        }
        List<Map<String,Object>> lines=jdbc.query("SELECT product_id,quantity FROM order_items WHERE order_id=?",(rs,n)->{Map<String,Object> m=new LinkedHashMap<>();m.put("productId",rs.getLong(1));m.put("quantity",rs.getInt(2));return m;},orderId);
        for(Map<String,Object> line:lines) jdbc.update("UPDATE inventory SET quantity=quantity+?,updated_at=?,version=version+1 WHERE product_id=?",line.get("quantity"),now,line.get("productId"));
        jdbc.update("INSERT INTO order_status_history(order_id,old_status,new_status,changed_by_user_id,changed_at,note) VALUES(?,?,?,?,?,?)",orderId,"CONFIRMED","CANCELLED",userId,now,"Customer cancellation; stock restored");
        jdbc.update("UPDATE shipments SET status='CANCELLED',updated_at=? WHERE order_id=? AND status='PREPARING'",now,orderId);
        return getOrder(userId,orderId);
    }

    public List<Map<String,Object>> reviews(Long productId) {
        return jdbc.query("SELECT r.review_id AS reviewId,r.rating,r.title,r.comment,r.created_at AS createdAt,u.first_name AS firstName FROM product_reviews r JOIN users u ON u.user_id=r.user_id WHERE r.product_id=? AND r.status='PUBLISHED' ORDER BY r.created_at DESC",(rs,n)->{Map<String,Object> m=new LinkedHashMap<>();m.put("reviewId",rs.getLong("reviewId"));m.put("rating",rs.getInt("rating"));m.put("title",rs.getString("title"));m.put("comment",rs.getString("comment"));m.put("createdAt",rs.getTimestamp("createdAt").toLocalDateTime());m.put("firstName",rs.getString("firstName"));return m;},productId);
    }
    @Transactional
    public Map<String,Object> addReview(Long userId, Long productId, Map<String,Object> payload) {
        int rating=number(payload.get("rating"),"rating").intValue(); if(rating<1||rating>5) throw new IllegalArgumentException("Rating must be between 1 and 5.");
        Integer purchased=jdbc.queryForObject("SELECT COUNT(*) FROM order_items oi JOIN orders o ON o.order_id=oi.order_id WHERE o.user_id=? AND oi.product_id=? AND o.status='DELIVERED'",Integer.class,userId,productId);
        if(purchased==null||purchased==0) throw new IllegalArgumentException("Only customers with a delivered order can review this product.");
        String title=String.valueOf(payload.getOrDefault("title","Review")).trim(); String comment=String.valueOf(payload.getOrDefault("comment","")).trim(); if(comment.isBlank()) throw new IllegalArgumentException("Review comment is required.");
        Integer duplicate=jdbc.queryForObject("SELECT COUNT(*) FROM product_reviews WHERE product_id=? AND user_id=?",Integer.class,productId,userId);
        if(duplicate!=null && duplicate>0) throw new IllegalArgumentException("You have already reviewed this product.");
        LocalDateTime now=LocalDateTime.now(); jdbc.update("INSERT INTO product_reviews(product_id,user_id,rating,title,comment,status,created_at,updated_at) VALUES(?,?,?,?,?,'PUBLISHED',?,?)",productId,userId,rating,title,comment,now,now);
        return jdbc.queryForMap("SELECT review_id AS reviewId,product_id AS productId,user_id AS userId,rating,title,comment,status,created_at AS createdAt FROM product_reviews WHERE product_id=? AND user_id=?",productId,userId);
    }

    @Transactional
    public Map<String,Object> updateShipment(Long shipmentId, String status) {
        String normalized=status.trim().toUpperCase();
        if(!List.of("PREPARING","SHIPPED","IN_TRANSIT","DELIVERED","CANCELLED").contains(normalized)) throw new IllegalArgumentException("Unsupported shipment status.");
        int count=jdbc.update("UPDATE shipments SET status=?,updated_at=? WHERE shipment_id=?",normalized,LocalDateTime.now(),shipmentId); if(count==0) throw new IllegalArgumentException("Shipment not found.");
        Long orderId=jdbc.queryForObject("SELECT order_id FROM shipments WHERE shipment_id=?",Long.class,shipmentId);
        if(normalized.equals("DELIVERED")) jdbc.update("UPDATE orders SET status='DELIVERED',updated_at=? WHERE order_id=?",LocalDateTime.now(),orderId);
        else if(normalized.equals("SHIPPED")||normalized.equals("IN_TRANSIT")) jdbc.update("UPDATE orders SET status='SHIPPED',updated_at=? WHERE order_id=? AND status<>'DELIVERED'",LocalDateTime.now(),orderId);
        return jdbc.queryForMap("SELECT shipment_id AS shipmentId,order_id AS orderId,status,carrier,tracking_number AS trackingNumber,updated_at AS updatedAt FROM shipments WHERE shipment_id=?",shipmentId);
    }


    public Map<String,Object> adminDashboard() {
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("totalUsers", jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class));
        out.put("activeUsers", jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE active=1", Long.class));
        out.put("totalProducts", jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE UPPER(status) IN ('ACTIVE','PUBLISHED')", Long.class));
        out.put("totalOrders", jdbc.queryForObject("SELECT COUNT(*) FROM orders", Long.class));
        out.put("pendingOrders", jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE status IN ('CONFIRMED','PROCESSING','PAYMENT_PENDING')", Long.class));
        out.put("paidRevenue", jdbc.queryForObject("SELECT COALESCE(SUM(total_amount),0) FROM orders WHERE payment_status='SUCCEEDED'", BigDecimal.class));
        out.put("pendingPayments", jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE payment_status='PENDING'", Long.class));
        out.put("feedbackCount", jdbc.queryForObject("SELECT COUNT(*) FROM customer_feedback", Long.class));
        out.put("recentOrders", adminOrders());
        return out;
    }

    public List<Map<String,Object>> adminUsers() {
        return jdbc.query("SELECT u.user_id AS userId,u.first_name AS firstName,u.last_name AS lastName,u.email,u.phone,u.active,u.created_at AS createdAt,COALESCE(GROUP_CONCAT(r.role_name ORDER BY r.role_name SEPARATOR ', '),'CUSTOMER') AS roles,(SELECT COUNT(*) FROM orders o WHERE o.user_id=u.user_id) AS orderCount FROM users u LEFT JOIN user_roles ur ON ur.user_id=u.user_id LEFT JOIN roles r ON r.role_id=ur.role_id GROUP BY u.user_id ORDER BY u.created_at DESC",
          (rs,n)->{Map<String,Object> m=new LinkedHashMap<>();m.put("userId",rs.getLong("userId"));m.put("firstName",rs.getString("firstName"));m.put("lastName",rs.getString("lastName"));m.put("email",rs.getString("email"));m.put("phone",rs.getString("phone"));m.put("active",rs.getBoolean("active"));m.put("createdAt",rs.getTimestamp("createdAt").toLocalDateTime());m.put("roles",rs.getString("roles"));m.put("orderCount",rs.getLong("orderCount"));return m;});
    }

    public List<Map<String,Object>> adminOrders() {
        return jdbc.query("SELECT o.order_id AS orderId,o.user_id AS userId,o.status,o.subtotal,o.shipping_fee AS shippingFee,o.tax_total AS taxTotal,o.total_amount AS totalAmount,o.payment_status AS paymentStatus,o.created_at AS createdAt,u.first_name AS firstName,u.last_name AS lastName,u.email,s.shipment_id AS shipmentId,s.status AS shipmentStatus,s.tracking_number AS trackingNumber FROM orders o JOIN users u ON u.user_id=o.user_id LEFT JOIN shipments s ON s.order_id=o.order_id ORDER BY o.created_at DESC LIMIT 300",
          (rs,n)->{Map<String,Object> m=new LinkedHashMap<>();m.put("orderId",rs.getLong("orderId"));m.put("userId",rs.getLong("userId"));m.put("status",rs.getString("status"));m.put("subtotal",rs.getBigDecimal("subtotal"));m.put("shippingFee",rs.getBigDecimal("shippingFee"));m.put("taxTotal",rs.getBigDecimal("taxTotal"));m.put("totalAmount",rs.getBigDecimal("totalAmount"));m.put("paymentStatus",rs.getString("paymentStatus"));m.put("createdAt",rs.getTimestamp("createdAt").toLocalDateTime());m.put("customerName",(rs.getString("firstName")+" "+rs.getString("lastName")).trim());m.put("email",rs.getString("email"));m.put("shipmentId",(Object)rs.getObject("shipmentId"));m.put("shipmentStatus",rs.getString("shipmentStatus"));m.put("trackingNumber",rs.getString("trackingNumber"));return m;});
    }

    public List<Map<String,Object>> adminFeedback() {
        return jdbc.query("SELECT f.feedback_id AS feedbackId,f.user_id AS userId,u.first_name AS firstName,u.last_name AS lastName,u.email,f.rating,f.category,f.message,f.status,f.created_at AS createdAt FROM customer_feedback f JOIN users u ON u.user_id=f.user_id ORDER BY f.created_at DESC",
          (rs,n)->{Map<String,Object> m=new LinkedHashMap<>();m.put("feedbackId",rs.getLong("feedbackId"));m.put("userId",rs.getLong("userId"));m.put("customerName",(rs.getString("firstName")+" "+rs.getString("lastName")).trim());m.put("email",rs.getString("email"));m.put("rating",rs.getInt("rating"));m.put("category",rs.getString("category"));m.put("message",rs.getString("message"));m.put("status",rs.getString("status"));m.put("createdAt",rs.getTimestamp("createdAt").toLocalDateTime());return m;});
    }

    @Transactional
    public Map<String,Object> submitFeedback(Long userId, Map<String,Object> body) {
        int rating=number(body.get("rating"),"rating").intValue();
        if(rating<1||rating>5) throw new IllegalArgumentException("Rating must be between 1 and 5.");
        String category=String.valueOf(body.getOrDefault("category","GENERAL")).trim().toUpperCase();
        if(!List.of("GENERAL","PRODUCT","DELIVERY","PAYMENT","SUPPORT").contains(category)) throw new IllegalArgumentException("Choose a valid feedback category.");
        String message=String.valueOf(body.getOrDefault("message","")).trim();
        if(message.length()<5||message.length()>2000) throw new IllegalArgumentException("Feedback must be between 5 and 2000 characters.");
        LocalDateTime now=LocalDateTime.now();
        jdbc.update("INSERT INTO customer_feedback(user_id,rating,category,message,status,created_at,updated_at) VALUES(?,?,?,?,'NEW',?,?)",userId,rating,category,message,now,now);
        return jdbc.queryForMap("SELECT feedback_id AS feedbackId,user_id AS userId,rating,category,message,status,created_at AS createdAt FROM customer_feedback WHERE feedback_id=LAST_INSERT_ID()");
    }

    private Long ensureCart(Long userId) {
        Integer userCount=jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE user_id=?",Integer.class,userId); if(userCount==null||userCount==0) throw new IllegalArgumentException("Customer not found.");
        List<Long> found=jdbc.query("SELECT cart_id FROM carts WHERE user_id=?",(rs,n)->rs.getLong(1),userId);
        if(!found.isEmpty()) return found.get(0);
        jdbc.update("INSERT INTO carts(user_id,version,created_at,updated_at) VALUES(?,0,?,?)",userId,LocalDateTime.now(),LocalDateTime.now());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
    }
    private static Long number(Object value,String name) { if(value instanceof Number n) return n.longValue(); try{return Long.valueOf(String.valueOf(value));}catch(Exception ex){throw new IllegalArgumentException("A valid "+name+" is required.");} }
    private static Map<String,Object> mapCartItem(ResultSet rs) throws SQLException { Map<String,Object> m=new LinkedHashMap<>();m.put("productId",rs.getLong("productId"));m.put("name",rs.getString("name"));m.put("brand",rs.getString("brand"));m.put("price",rs.getBigDecimal("price"));m.put("quantity",rs.getInt("quantity"));m.put("lineTotal",rs.getBigDecimal("lineTotal"));m.put("stock",rs.getInt("stock"));return m; }
    private static Map<String,Object> orderRow(ResultSet rs) throws SQLException { Map<String,Object> m=new LinkedHashMap<>();m.put("orderId",rs.getLong("orderId"));m.put("status",rs.getString("status"));m.put("subtotal",rs.getBigDecimal("subtotal"));m.put("shippingFee",rs.getBigDecimal("shippingFee"));m.put("taxTotal",rs.getBigDecimal("taxTotal"));m.put("totalAmount",rs.getBigDecimal("totalAmount"));m.put("paymentStatus",rs.getString("paymentStatus"));m.put("createdAt",rs.getTimestamp("createdAt").toLocalDateTime());return m; }
}
