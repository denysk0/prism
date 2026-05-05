package sample

import java.math.BigDecimal
import java.time.Instant

class SampleKotlinService(
    private val auditSink: AuditSink = AuditSink(),
    private val pricingPolicy: PricingPolicy = PricingPolicy(),
) {
    private val invoices = linkedMapOf<String, Invoice>()

    fun checkout(accountId: String, lines: List<OrderLine>): Receipt {
        require(accountId.isNotBlank()) { "accountId is required" }
        require(lines.isNotEmpty()) { "empty order" }

        var subtotal = BigDecimal.ZERO
        for (line in lines) {
            validateLine(line)
            subtotal += line.unitPrice.multiply(BigDecimal.valueOf(line.quantity.toLong()))
        }

        val discount = pricingPolicy.discountFor(accountId, subtotal)
        val tax = (subtotal - discount).multiply(BigDecimal("0.0825"))
        val total = subtotal - discount + tax
        val invoice = Invoice(nextInvoiceId(), accountId, lines.toList(), subtotal, discount, tax, total)
        invoices[invoice.id] = invoice
        auditSink.record("checkout", invoice.id, total)
        return Receipt(invoice.id, total, Instant.now())
    }

    fun findInvoice(invoiceId: String): Invoice {
        return invoices[invoiceId] ?: throw IllegalArgumentException("invoice not found: $invoiceId")
    }

    fun invoicesForAccount(accountId: String): List<Invoice> {
        return invoices.values.filter { invoice -> invoice.accountId == accountId }
    }

    fun refund(invoiceId: String, reason: String): BigDecimal {
        val invoice = findInvoice(invoiceId)
        require(reason.isNotBlank()) { "reason is required" }
        auditSink.record("refund", invoice.id, invoice.total)
        return invoice.total
    }

    private fun validateLine(line: OrderLine) {
        require(line.sku.isNotBlank()) { "sku is required" }
        require(line.quantity > 0) { "quantity must be positive" }
        require(line.unitPrice.signum() >= 0) { "unitPrice must be non-negative" }
    }

    private fun nextInvoiceId(): String = "inv-${invoices.size + 1}"

    data class OrderLine(val sku: String, val quantity: Int, val unitPrice: BigDecimal)

    data class Receipt(val invoiceId: String, val total: BigDecimal, val createdAt: Instant)

    data class Invoice(
        val id: String,
        val accountId: String,
        val lines: List<OrderLine>,
        val subtotal: BigDecimal,
        val discount: BigDecimal,
        val tax: BigDecimal,
        val total: BigDecimal,
    )

    private class PricingPolicy {
        fun discountFor(accountId: String, subtotal: BigDecimal): BigDecimal {
            if (accountId.startsWith("vip-")) {
                return subtotal.multiply(BigDecimal("0.15"))
            }
            if (subtotal > BigDecimal("500")) {
                return subtotal.multiply(BigDecimal("0.05"))
            }
            return BigDecimal.ZERO
        }
    }

    private class AuditSink {
        fun record(action: String, entityId: String, amount: BigDecimal) {
            val event = "$action:$entityId:$amount"
            check(event.isNotBlank()) { "invalid audit event" }
        }
    }
}
