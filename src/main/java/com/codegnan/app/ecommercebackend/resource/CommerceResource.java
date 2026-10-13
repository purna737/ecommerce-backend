package com.codegnan.app.ecommercebackend.resource;

import java.util.List;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.codegnan.app.ecommercebackend.service.CommerceService;
import com.codegnan.app.ecommercebackend.service.AuthenticationService;

@CrossOrigin(origins = { "http://localhost:5173", "http://127.0.0.1:5173" })
@RestController
@RequestMapping("/rest/api/v1")
public class CommerceResource {
	private final CommerceService commerce;
	private final AuthenticationService authenticationService;

	public CommerceResource(CommerceService commerce, AuthenticationService authenticationService) {
		this.commerce = commerce;
		this.authenticationService = authenticationService;
	}

	@GetMapping("/me/cart")
	public ResponseEntity<?> cart(@RequestParam Long userId) {
		return handle(() -> commerce.getCart(userId));
	}

	@PostMapping("/me/cart/items")
	public ResponseEntity<?> addCartItem(@RequestParam Long userId, @RequestBody Map<String, Object> body) {
		if (authenticationService.isAdmin(userId))
			return forbiddenPurchase();
		return handle(() -> commerce.addCartItem(userId, number(body, "productId"), integer(body, "quantity", 1)));
	}

	@PatchMapping("/me/cart/items/{productId}")
	public ResponseEntity<?> setCartItem(@RequestParam Long userId, @PathVariable Long productId,
			@RequestBody Map<String, Object> body) {
		if (authenticationService.isAdmin(userId))
			return forbiddenPurchase();
		return handle(() -> commerce.setCartItem(userId, productId, integer(body, "quantity", 0)));
	}

	@DeleteMapping("/me/cart/items/{productId}")
	public ResponseEntity<?> removeCartItem(@RequestParam Long userId, @PathVariable Long productId) {
		if (authenticationService.isAdmin(userId))
			return forbiddenPurchase();
		return handle(() -> commerce.removeCartItem(userId, productId));
	}

	@DeleteMapping("/me/cart")
	public ResponseEntity<?> clearCart(@RequestParam Long userId) {
		if (authenticationService.isAdmin(userId))
			return forbiddenPurchase();
		return handle(() -> commerce.clearCart(userId));
	}

	@GetMapping("/inventory")
	public ResponseEntity<?> inventory(@RequestParam Long userId) {
		if (!authenticationService.isAdmin(userId))
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin access required."));
		return handle(commerce::inventory);
	}

	@PatchMapping("/inventory/{productId}")
	public ResponseEntity<?> adjustInventory(@RequestParam Long userId, @PathVariable Long productId,
			@RequestBody Map<String, Object> body) {
		if (!authenticationService.isAdmin(userId))
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin access required."));
		return handle(() -> commerce.adjustInventory(productId, integer(body, "delta", 0)));
	}

	@PostMapping("/me/orders")
	public ResponseEntity<?> checkout(@RequestParam Long userId, @RequestBody Map<String, Object> body) {
		if (authenticationService.isAdmin(userId))
			return forbiddenPurchase();
		return handle(() -> commerce.checkout(userId, body));
	}

	@GetMapping("/me/orders")
	public ResponseEntity<?> orders(@RequestParam Long userId) {
		return handle(() -> commerce.orders(userId));
	}

	@GetMapping("/me/orders/{orderId}")
	public ResponseEntity<?> order(@RequestParam Long userId, @PathVariable Long orderId) {
		return handle(() -> commerce.getOrder(userId, orderId));
	}

	@PostMapping("/me/orders/{orderId}/cancellation")
	public ResponseEntity<?> cancelOrder(@RequestParam Long userId, @PathVariable Long orderId) {
		return handle(() -> commerce.cancelOrder(userId, orderId));
	}

	@GetMapping("/products/{productId}/reviews")
	public ResponseEntity<?> reviews(@PathVariable Long productId) {
		return handle(() -> commerce.reviews(productId));
	}

	@PostMapping("/products/{productId}/reviews")
	public ResponseEntity<?> addReview(@RequestParam Long userId, @PathVariable Long productId,
			@RequestBody Map<String, Object> body) {
		return handle(() -> commerce.addReview(userId, productId, body));
	}

	@GetMapping("/admin/dashboard")
	public ResponseEntity<?> adminDashboard(@RequestParam Long userId) {
		if (!authenticationService.isAdmin(userId))
			return forbiddenAdmin();
		return handle(commerce::adminDashboard);
	}

	@GetMapping("/admin/users")
	public ResponseEntity<?> adminUsers(@RequestParam Long userId) {
		if (!authenticationService.isAdmin(userId))
			return forbiddenAdmin();
		return handle(commerce::adminUsers);
	}

	@GetMapping("/admin/orders")
	public ResponseEntity<?> adminOrders(@RequestParam Long userId) {
		if (!authenticationService.isAdmin(userId))
			return forbiddenAdmin();
		return handle(commerce::adminOrders);
	}

	@GetMapping("/admin/feedback")
	public ResponseEntity<?> adminFeedback(@RequestParam Long userId) {
		if (!authenticationService.isAdmin(userId))
			return forbiddenAdmin();
		return handle(commerce::adminFeedback);
	}

	@PostMapping("/feedback")
	public ResponseEntity<?> submitFeedback(@RequestParam Long userId, @RequestBody Map<String, Object> body) {
		return handle(() -> commerce.submitFeedback(userId, body));
	}

	@PatchMapping("/admin/shipments/{shipmentId}")
	public ResponseEntity<?> updateShipment(@RequestParam Long userId, @PathVariable Long shipmentId,
			@RequestBody Map<String, Object> body) {
		if (!authenticationService.isAdmin(userId))
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin access required."));
		return handle(() -> commerce.updateShipment(shipmentId, String.valueOf(body.getOrDefault("status", ""))));
	}

	private ResponseEntity<?> forbiddenPurchase() {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message",
				"Admin accounts can manage the store but cannot place customer orders. Please sign in with a customer account to purchase."));
	}

	private ResponseEntity<?> forbiddenAdmin() {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin access required."));
	}

	private ResponseEntity<?> handle(Work work) {
		try {
			return ResponseEntity.ok(work.run());
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest()
					.body(Map.of("message", ex.getMessage() == null ? "Invalid request" : ex.getMessage()));
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "Request could not be completed."));
		}
	}

	private static Long number(Map<String, Object> m, String key) {
		Object v = m.get(key);
		if (v instanceof Number n)
			return n.longValue();
		try {
			return Long.valueOf(String.valueOf(v));
		} catch (Exception ex) {
			throw new IllegalArgumentException("A valid " + key + " is required.");
		}
	}

	private static int integer(Map<String, Object> m, String key, int fallback) {
		Object v = m.get(key);
		if (v == null)
			return fallback;
		if (v instanceof Number n)
			return n.intValue();
		try {
			return Integer.parseInt(String.valueOf(v));
		} catch (Exception ex) {
			throw new IllegalArgumentException("A valid " + key + " is required.");
		}
	}

	@FunctionalInterface
	private interface Work {
		Object run() throws Exception;
	}
}
