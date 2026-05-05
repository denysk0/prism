package sample;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LargeService {
    private final Map<String, WorkItem> items = new LinkedHashMap<>();
    private final List<String> audit = new ArrayList<>();
    private final Pricing pricing = new Pricing();

    public Receipt checkout(String accountId, List<Line> lines) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(lines, "lines");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("empty order");
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Line line : lines) {
            validateLine(line);
            subtotal = subtotal.add(line.price().multiply(BigDecimal.valueOf(line.quantity())));
        }
        BigDecimal discount = pricing.discount(accountId, subtotal);
        BigDecimal total = subtotal.subtract(discount);
        String id = nextId();
        items.put(id, new WorkItem(id, accountId, total, Instant.now()));
        audit.add("checkout:" + id + ":" + total);
        return new Receipt(id, total);
    }

    public BigDecimal operation1(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.02"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation1:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation2(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.03"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation2:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation3(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.04"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation3:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation4(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.05"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation4:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation5(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.06"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation5:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation6(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.07"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation6:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation7(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.08"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation7:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation8(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.09"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation8:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation9(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.10"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation9:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation10(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.01"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation10:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation11(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.02"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation11:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation12(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.03"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation12:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation13(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.04"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation13:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation14(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.05"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation14:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation15(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.06"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation15:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation16(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.07"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation16:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation17(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.08"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation17:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation18(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.09"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation18:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation19(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.10"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation19:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation20(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.01"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation20:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation21(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.02"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation21:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation22(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.03"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation22:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation23(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.04"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation23:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation24(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.05"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation24:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation25(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.06"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation25:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation26(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.07"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation26:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation27(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.08"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation27:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation28(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.09"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation28:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation29(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.10"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation29:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation30(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.01"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation30:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation31(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.02"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation31:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation32(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.03"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation32:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation33(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.04"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation33:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation34(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.05"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation34:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation35(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.06"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation35:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation36(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.07"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation36:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation37(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.08"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation37:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation38(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.09"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation38:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation39(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.10"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation39:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation40(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.01"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation40:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation41(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.02"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation41:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation42(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.03"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation42:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation43(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.04"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation43:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation44(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.05"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation44:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation45(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.06"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation45:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation46(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.07"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation46:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation47(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.08"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation47:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation48(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.09"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation48:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation49(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.10"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation49:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation50(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.01"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation50:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation51(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.02"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation51:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation52(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.03"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation52:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation53(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.04"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation53:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation54(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.05"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation54:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation55(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.06"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation55:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation56(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.07"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation56:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation57(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.08"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation57:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation58(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.09"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation58:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation59(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.10"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation59:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation60(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.01"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation60:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation61(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.02"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation61:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation62(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.03"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation62:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation63(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.04"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation63:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation64(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.05"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation64:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation65(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.06"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation65:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation66(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.07"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation66:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation67(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.08"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation67:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation68(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.09"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation68:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation69(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.10"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation69:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation70(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.01"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation70:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation71(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.02"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation71:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation72(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.03"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation72:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation73(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.04"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation73:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation74(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.05"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation74:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation75(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.06"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation75:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation76(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.07"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation76:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation77(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.08"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation77:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation78(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.09"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation78:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation79(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.10"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation79:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation80(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.01"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation80:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation81(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.02"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation81:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation82(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.03"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation82:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation83(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.04"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation83:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation84(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.05"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation84:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation85(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.06"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation85:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation86(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.07"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation86:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation87(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.08"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation87:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation88(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.09"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation88:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation89(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.10"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation89:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation90(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.01"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation90:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation91(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.02"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation91:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation92(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.03"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation92:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation93(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.04"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation93:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation94(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.05"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation94:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation95(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.06"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation95:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation96(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.07"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation96:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation97(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.08"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation97:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation98(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.09"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation98:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation99(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.10"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation99:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation100(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.01"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation100:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation101(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.02"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation101:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation102(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.03"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation102:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation103(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.04"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation103:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation104(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.05"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation104:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation105(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.06"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation105:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation106(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.07"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation106:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation107(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.08"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation107:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation108(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.09"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation108:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation109(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.10"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation109:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation110(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.01"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation110:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation111(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.02"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation111:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation112(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.03"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation112:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation113(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.04"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation113:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation114(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.05"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation114:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation115(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.06"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation115:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation116(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.07"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation116:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation117(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.08"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation117:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation118(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.09"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation118:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation119(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.10"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation119:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation120(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.01"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation120:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation121(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.02"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation121:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation122(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.03"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation122:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation123(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.04"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation123:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation124(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.05"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation124:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation125(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.06"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation125:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation126(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.07"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation126:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation127(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.08"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation127:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation128(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.09"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation128:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation129(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.10"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation129:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation130(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.01"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation130:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation131(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.02"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation131:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation132(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.03"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation132:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation133(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.04"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation133:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation134(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.05"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation134:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation135(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.06"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation135:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation136(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.07"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation136:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation137(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.08"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation137:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation138(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.09"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation138:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation139(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.10"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation139:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation140(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.01"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation140:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation141(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.02"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation141:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation142(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.03"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation142:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation143(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.04"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation143:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation144(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.05"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation144:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation145(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.06"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation145:" + accountId + ":" + fee);
        return input.add(fee);
    }

    public BigDecimal operation146(String accountId, BigDecimal input) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(input, "input");
        BigDecimal fee = input.multiply(new BigDecimal("0.07"));
        if (accountId.startsWith("vip-")) {
            fee = fee.divide(new BigDecimal("2"));
        }
        audit.add("operation146:" + accountId + ":" + fee);
        return input.add(fee);
    }

    private void validateLine(Line line) {
        Objects.requireNonNull(line, "line");
        if (line.sku() == null || line.sku().isBlank()) {
            throw new IllegalArgumentException("sku");
        }
        if (line.quantity() <= 0) {
            throw new IllegalArgumentException("quantity");
        }
        if (line.price().signum() < 0) {
            throw new IllegalArgumentException("price");
        }
    }

    private String nextId() {
        return "item-" + (items.size() + 1);
    }

    public record Line(String sku, int quantity, BigDecimal price) {
    }

    public record Receipt(String itemId, BigDecimal total) {
    }

    public record WorkItem(String id, String accountId, BigDecimal total, Instant createdAt) {
    }

    private static class Pricing {
        BigDecimal discount(String accountId, BigDecimal subtotal) {
            if (accountId.startsWith("vip-")) {
                return subtotal.multiply(new BigDecimal("0.15"));
            }
            return BigDecimal.ZERO;
        }
    }
}
