import java.time.LocalDate;

/**
 * Simple Transaction class to help with data handling
 */
public class Transaction {
    private int id;
    private double amount;
    private LocalDate date;
    private String category;
    private String paymentMethod;
    private boolean isIncome;
    private boolean recurring;

    public Transaction(int id, double amount, LocalDate date, String category,
                       String paymentMethod, boolean isIncome, boolean recurring) {
        this.id = id;
        this.amount = amount;
        this.date = date;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.isIncome = isIncome;
        this.recurring = recurring;
    }

    // Getters and setters
    public int getId() { return id; }
    public double getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getCategory() { return category; }
    public String getPaymentMethod() { return paymentMethod; }
    public boolean isIncome() { return isIncome; }
    public boolean isRecurring() { return recurring; }

    @Override
    public String toString() {
        return String.format("Transaction #%d: $%.2f on %s (%s)",
                id, amount, date, isIncome ? "Income" : "Expense");
    }
}
