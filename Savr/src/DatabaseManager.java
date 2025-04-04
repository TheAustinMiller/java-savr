import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates and manages the local database and the transactions table
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:C:\\Users\\ajthe\\IdeaProjects\\Savr\\Savr-db";
    private Connection connection;

    /**
     * Initializes the database connection
     */
    public DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);

            createTransactionsTable();

            System.out.println("Database connection established successfully.");
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates the transactions table (if not already made)
     */
    private void createTransactionsTable() throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS transactions (" +
                "transaction_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "amount DECIMAL(10,2) NOT NULL, " +
                "transaction_date DATE NOT NULL, " +
                "category VARCHAR(50), " +
                "payment_method VARCHAR(50), " +
                "is_income BOOLEAN DEFAULT FALSE, " +
                "recurring BOOLEAN DEFAULT FALSE, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSQL);
            System.out.println("Transactions table checked/created successfully.");
        }
    }

    /**
     * Adds a new transaction to the database
     *
     * @param amount Transaction amount
     * @param date Transaction date
     * @param category Category of the transaction
     * @param paymentMethod Method of payment
     * @param isIncome Whether this is income (true) or expense (false)
     * @param recurring Whether this is a recurring transaction
     * @return The ID of the newly created transaction
     */
    public int addTransaction(double amount, LocalDate date, String category,
                              String paymentMethod, boolean isIncome, boolean recurring) {

        String insertSQL = "INSERT INTO transactions (amount, transaction_date, category, " +
                "payment_method, is_income, recurring) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL,
                Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setDouble(1, amount);
            pstmt.setDate(2, Date.valueOf(date));
            pstmt.setString(3, category);
            pstmt.setString(4, paymentMethod);
            pstmt.setBoolean(5, isIncome);
            pstmt.setBoolean(6, recurring);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating transaction failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating transaction failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error adding transaction: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Retrieves all transactions from the database
     *
     * @return List of Transaction objects
     */
    public List<Transaction> getAllTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        String selectSQL = "SELECT * FROM transactions ORDER BY transaction_date DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {

            while (rs.next()) {
                Transaction transaction = new Transaction(
                        rs.getInt("transaction_id"),
                        rs.getDouble("amount"),
                        rs.getDate("transaction_date").toLocalDate(),
                        rs.getString("category"),
                        rs.getString("payment_method"),
                        rs.getBoolean("is_income"),
                        rs.getBoolean("recurring")
                );
                transactions.add(transaction);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving transactions: " + e.getMessage());
            e.printStackTrace();
        }

        return transactions;
    }

    /**
     * Retrieves transactions filtered by date range
     *
     * @param startDate Beginning of date range
     * @param endDate End of date range
     * @return List of Transaction objects in the date range
     */
    public List<Transaction> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Transaction> transactions = new ArrayList<>();
        String selectSQL = "SELECT * FROM transactions WHERE transaction_date BETWEEN ? AND ? " +
                "ORDER BY transaction_date DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
            pstmt.setDate(1, Date.valueOf(startDate));
            pstmt.setDate(2, Date.valueOf(endDate));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Transaction transaction = new Transaction(
                            rs.getInt("transaction_id"),
                            rs.getDouble("amount"),
                            rs.getDate("transaction_date").toLocalDate(),
                            rs.getString("category"),
                            rs.getString("payment_method"),
                            rs.getBoolean("is_income"),
                            rs.getBoolean("recurring")
                    );
                    transactions.add(transaction);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving transactions by date range: " + e.getMessage());
            e.printStackTrace();
        }

        return transactions;
    }

    /**
     * Updates an existing transaction
     *
     * @param transactionId ID of the transaction to update
     * @param amount Updated amount
     * @param date Updated date
     * @param category Updated category
     * @param paymentMethod Updated payment method
     * @param isIncome Updated income status
     * @param recurring Updated recurring status
     * @return true if update was successful, false otherwise
     */
    public boolean updateTransaction(int transactionId, double amount, LocalDate date,
                                     String category, String paymentMethod,
                                     boolean isIncome, boolean recurring) {

        String updateSQL = "UPDATE transactions SET amount = ?, transaction_date = ?, " +
                "category = ?, payment_method = ?, " +
                "is_income = ?, recurring = ? " +
                "WHERE transaction_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {
            pstmt.setDouble(1, amount);
            pstmt.setDate(2, Date.valueOf(date));
            pstmt.setString(3, category);
            pstmt.setString(4, paymentMethod);
            pstmt.setBoolean(5, isIncome);
            pstmt.setBoolean(6, recurring);
            pstmt.setInt(7, transactionId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating transaction: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a transaction from the database
     *
     * @param transactionId ID of the transaction to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteTransaction(int transactionId) {
        String deleteSQL = "DELETE FROM transactions WHERE transaction_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(deleteSQL)) {
            pstmt.setInt(1, transactionId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting transaction: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Close the database connection
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Simple Transaction class to help with data handling
     */
    public static class Transaction {
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
}