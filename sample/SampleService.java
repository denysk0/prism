package sample;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SampleService {
    private final Map<String, Invoice> invoices = new LinkedHashMap<>();
    private final AuditSink auditSink = new AuditSink();
    private final PricingPolicy pricingPolicy = new PricingPolicy();

    public Receipt checkout(String accountId, List<OrderLine> lines) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(lines, "lines");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("empty order");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderLine line : lines) {
            validateLine(line);
            subtotal = subtotal.add(line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())));
        }

        BigDecimal discount = pricingPolicy.discountFor(accountId, subtotal);
        BigDecimal tax = subtotal.subtract(discount).multiply(new BigDecimal("0.0825"));
        BigDecimal total = subtotal.subtract(discount).add(tax);
        Invoice invoice = new Invoice(nextInvoiceId(), accountId, copyLines(lines), subtotal, discount, tax, total);
        invoices.put(invoice.id(), invoice);
        auditSink.record("checkout", invoice.id(), total);
        return new Receipt(invoice.id(), total, Instant.now());
    }

    public Invoice findInvoice(String invoiceId) {
        Invoice invoice = invoices.get(invoiceId);
        if (invoice == null) {
            throw new IllegalArgumentException("invoice not found: " + invoiceId);
        }
        return invoice;
    }

    public List<Invoice> invoicesForAccount(String accountId) {
        List<Invoice> result = new ArrayList<>();
        for (Invoice invoice : invoices.values()) {
            if (invoice.accountId().equals(accountId)) {
                result.add(invoice);
            }
        }
        return result;
    }

    public BigDecimal refund(String invoiceId, String reason) {
        Invoice invoice = findInvoice(invoiceId);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
        auditSink.record("refund", invoice.id(), invoice.total());
        return invoice.total();
    }

    private void validateLine(OrderLine line) {
        Objects.requireNonNull(line, "line");
        if (line.sku() == null || line.sku().isBlank()) {
            throw new IllegalArgumentException("sku is required");
        }
        if (line.quantity() <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (line.unitPrice().signum() < 0) {
            throw new IllegalArgumentException("unitPrice must be non-negative");
        }
    }

    private List<OrderLine> copyLines(List<OrderLine> lines) {
        return List.copyOf(lines);
    }

    private String nextInvoiceId() {
        return "inv-" + (invoices.size() + 1);
    }

    public record OrderLine(String sku, int quantity, BigDecimal unitPrice) {
    }

    public record Receipt(String invoiceId, BigDecimal total, Instant createdAt) {
    }

    public record Invoice(
        String id,
        String accountId,
        List<OrderLine> lines,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal tax,
        BigDecimal total
    ) {
    }

    private static class PricingPolicy {
        BigDecimal discountFor(String accountId, BigDecimal subtotal) {
            if (accountId.startsWith("vip-")) {
                return subtotal.multiply(new BigDecimal("0.15"));
            }
            if (subtotal.compareTo(new BigDecimal("500")) > 0) {
                return subtotal.multiply(new BigDecimal("0.05"));
            }
            return BigDecimal.ZERO;
        }
    }

    private static class AuditSink {
        void record(String action, String entityId, BigDecimal amount) {
            String event = action + ":" + entityId + ":" + amount;
            if (event.isBlank()) {
                throw new IllegalStateException("invalid audit event");
            }
        }
    }
}
