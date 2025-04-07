import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Basic tabbed interface for Savr Finance App
 */
public class Savr extends JFrame {
    // Main components
    private JTabbedPane tabbedPane;
    private JButton addButton;
    private JPanel addTransactionPanel;
    private JPanel viewTransactionsPanel;
    private JPanel graphsPanel;
    private JTextField amountField;
    private JTextField dateField;
    private JComboBox categoryField;
    private JComboBox paymentField;
    private JComboBox typeField;

    // Database manager reference
    private DatabaseManager dbManager;

    public Savr() {
        // Basic frame setup
        setTitle("Savr - Personal Finance Manager");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Initialize database manager
        dbManager = new DatabaseManager();

        // Create tabbed pane
        tabbedPane = new JTabbedPane();

        // Create simple panels
        createAddTransactionPanel();
        createViewTransactionsPanel();
        createGraphsPanel();

        // Add panels to tabbed pane
        tabbedPane.addTab("Add Transaction", addTransactionPanel);
        tabbedPane.addTab("View Transactions", viewTransactionsPanel);
        tabbedPane.addTab("Graphs", graphsPanel);

        // Add tabbed pane to frame
        getContentPane().add(tabbedPane);

        // Close database on window close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dbManager.closeConnection();
            }
        });
    }

    /**
     * Creates a basic panel for adding transactions
     */
    private void createAddTransactionPanel() {
        addTransactionPanel = new JPanel(new BorderLayout());

        // Simple form with basic fields
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Add form components
        formPanel.add(new JLabel("Amount:"));
        amountField = new JTextField();
        formPanel.add(amountField);

        formPanel.add(new JLabel("Date:"));
        dateField = new JTextField(LocalDate.now().toString());
        formPanel.add(dateField);

        formPanel.add(new JLabel("Category:"));
        categoryField = new JComboBox<>(new String[]{"Food", "Entertainment", "Housing", "Transportation", "Golfing", "Savings", "Other"});
        formPanel.add(categoryField);

        formPanel.add(new JLabel("Payment Method:"));
        paymentField = new JComboBox<>(new String[]{"Cash", "Credit Card", "Debit Card"});
        formPanel.add(paymentField);

        formPanel.add(new JLabel("Type:"));
        typeField = new JComboBox<>(new String[]{"Expense", "Income"});
        formPanel.add(typeField);

        addButton = new JButton("Add Transaction");

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                double amount = Double.parseDouble(amountField.getText());
                LocalDate date = LocalDate.parse(dateField.getText());
                String category = categoryField.getSelectedItem().toString();
                String payment = paymentField.getSelectedItem().toString();
                boolean isIncome = typeField.getSelectedItem().toString().equals("Income");

                dbManager.addTransaction(amount, date, category, payment, isIncome, false);
                refreshTransactionTable();
            }
        });

        // Add components to panel
        addTransactionPanel.add(new JLabel("Add Transaction", SwingConstants.CENTER), BorderLayout.NORTH);
        addTransactionPanel.add(formPanel, BorderLayout.CENTER);
        addTransactionPanel.add(addButton, BorderLayout.SOUTH);
    }

    /**
     * Creates a panel for viewing transactions with editable rows
     */
    private void createViewTransactionsPanel() {
        viewTransactionsPanel = new JPanel(new BorderLayout());

        // Add ID to columns (but we'll hide it later)
        String[] columns = {"ID", "Amount", "Date", "Category", "Type", "Edit"};
        ArrayList<Transaction> transactions = (ArrayList<Transaction>) dbManager.getAllTransactions();

        // Now include the ID in the data array
        Object[][] data = new Object[transactions.size()][6];
        for (int i = 0; i < transactions.size(); i++) {
            Transaction t = transactions.get(i);
            data[i][0] = t.getId(); // Assuming you have a getId() method in Transaction
            data[i][1] = t.getAmount();
            data[i][2] = t.getDate();
            data[i][3] = t.getCategory();
            data[i][4] = t.getPaymentMethod();
            data[i][5] = "Edit";
        }

        // Create model with override to make last column cells clickable
        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Only Edit column is editable
            }
        };

        JTable table = new JTable(model);

        class ButtonRenderer extends JButton implements TableCellRenderer {
            public ButtonRenderer() {
                setOpaque(true);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                setText("Edit");
                return this;
            }
        }

        class ButtonEditor extends DefaultCellEditor {
            private JButton button;
            private String label;
            private boolean isPushed;
            private JTable table;
            private int row;

            public ButtonEditor(JCheckBox checkBox) {
                super(checkBox);
                button = new JButton();
                button.setOpaque(true);
                button.addActionListener(e -> fireEditingStopped());
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                                                         boolean isSelected, int row, int column) {
                this.table = table;
                this.row = row;
                button.setText("Edit");
                isPushed = true;
                return button;
            }

            @Override
            public Object getCellEditorValue() {
                if (isPushed) {
                    // Get transaction ID from the hidden column
                    int transactionId = (int) table.getValueAt(row, 0);

                    // Get transaction data for this row (adjusted column indices)
                    double amount = (double) table.getValueAt(row, 1);
                    LocalDate date = (LocalDate) table.getValueAt(row, 2);
                    String category = (String) table.getValueAt(row, 3);
                    String paymentMethod = (String) table.getValueAt(row, 4);

                    // Create and show an edit dialog with the transaction ID
                    showEditDialog(transactionId, amount, date, category, paymentMethod, row);
                }
                isPushed = false;
                return "Edit";
            }

            // Update the method signature to include transactionId
            private void showEditDialog(int transactionId, double amount, LocalDate date, String category,
                                        String paymentMethod, int rowIndex) {
                // Create a modal dialog for editing
                JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(table),
                        "Edit Transaction", true);
                dialog.setLayout(new BorderLayout());
                dialog.setSize(400, 300);
                dialog.setLocationRelativeTo(table);

                // Create form panel with current transaction values
                JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));
                formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

                // Add form components with current values
                formPanel.add(new JLabel("Amount:"));
                JTextField amountField = new JTextField(String.valueOf(amount));
                formPanel.add(amountField);

                formPanel.add(new JLabel("Date:"));
                JTextField dateField = new JTextField(date.toString());
                formPanel.add(dateField);

                formPanel.add(new JLabel("Category:"));
                JComboBox<String> categoryField = new JComboBox<>(
                        new String[]{"Food", "Entertainment", "Housing", "Transportation", "Golfing", "Savings", "Other"});
                categoryField.setSelectedItem(category);
                formPanel.add(categoryField);

                formPanel.add(new JLabel("Payment Method:"));
                JComboBox<String> paymentField = new JComboBox<>(
                        new String[]{"Cash", "Credit Card", "Debit Card"});
                paymentField.setSelectedItem(paymentMethod);
                formPanel.add(paymentField);

                // Create buttons panel
                JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton saveButton = new JButton("Save");
                JButton deleteButton = new JButton("Delete");
                JButton cancelButton = new JButton("Cancel");

                // Style the delete button to stand out as a destructive action
                deleteButton.setBackground(new Color(220, 53, 69));
                deleteButton.setForeground(Color.WHITE);
                deleteButton.setFocusPainted(false);

                saveButton.addActionListener(e -> {
                    try {
                        // Get updated values
                        double newAmount = Double.parseDouble(amountField.getText());
                        LocalDate newDate = LocalDate.parse(dateField.getText());
                        String newCategory = categoryField.getSelectedItem().toString();
                        String newPayment = paymentField.getSelectedItem().toString();

                        // Update the database using the transaction ID
                        dbManager.updateTransaction(transactionId, newAmount, newDate, newCategory, newPayment, false, false);

                        // Update table model - make sure the row still exists
                        if (rowIndex < table.getModel().getRowCount()) {
                            table.setValueAt(newAmount, rowIndex, 1);
                            table.setValueAt(newDate, rowIndex, 2);
                            table.setValueAt(newCategory, rowIndex, 3);
                            table.setValueAt(newPayment, rowIndex, 4);
                        }

                        dialog.dispose();

                        // Refresh the table view
                        refreshTransactionTable();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(dialog, "Please enter valid values: " + ex.getMessage(),
                                "Input Error", JOptionPane.ERROR_MESSAGE);
                    }
                });

                deleteButton.addActionListener(e -> {
                    // Show a confirmation dialog before deleting
                    int result = JOptionPane.showConfirmDialog(
                            dialog,
                            "Are you sure you want to delete this transaction? This action cannot be undone.",
                            "Confirm Delete",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                    );

                    if (result == JOptionPane.YES_OPTION) {
                        // Delete from database using the transaction ID
                        dbManager.deleteTransaction(transactionId);

                        // Close the dialog first before manipulating the table
                        dialog.dispose();

                        // Remove from table model safely
                        try {
                            if (rowIndex < table.getModel().getRowCount()) {
                                ((DefaultTableModel)table.getModel()).removeRow(rowIndex);
                            }
                        } catch (Exception ex) {
                            // Just in case there's an issue with removal
                            System.out.println("Error removing row: " + ex.getMessage());
                        }

                        // Refresh the table (this would be a method you need to add)
                        refreshTransactionTable();

                        // Show confirmation message
                        JOptionPane.showMessageDialog(
                                table,
                                "Transaction has been deleted successfully.",
                                "Transaction Deleted",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                });

                cancelButton.addActionListener(e -> dialog.dispose());

                buttonsPanel.add(deleteButton);
                buttonsPanel.add(saveButton);
                buttonsPanel.add(cancelButton);

                // Add components to dialog
                dialog.add(new JLabel("Edit Transaction", SwingConstants.CENTER), BorderLayout.NORTH);
                dialog.add(formPanel, BorderLayout.CENTER);
                dialog.add(buttonsPanel, BorderLayout.SOUTH);

                dialog.setVisible(true);
            }

            @Override
            public boolean stopCellEditing() {
                isPushed = false;
                return super.stopCellEditing();
            }
        }

        // Hide the ID column but keep the data in the model
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);

        // Add renderer and editor for Edit column (now at index 5)
        table.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(new ButtonEditor(new JCheckBox()));

        JScrollPane scrollPane = new JScrollPane(table);

        // Add filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Filter by:"));
        filterPanel.add(new JComboBox<>(new String[]{"All", "Income", "Expense"}));
        filterPanel.add(new JButton("Apply Filter"));

        // Add components to panel
        viewTransactionsPanel.add(new JLabel("View Transactions", SwingConstants.CENTER), BorderLayout.NORTH);
        viewTransactionsPanel.add(filterPanel, BorderLayout.SOUTH);
        viewTransactionsPanel.add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Refreshes the transaction table with the latest data from the database
     */
    private void refreshTransactionTable() {
        // Get updated transactions from the database
        ArrayList<Transaction> transactions = (ArrayList<Transaction>) dbManager.getAllTransactions();

        // Create a new model with the updated data
        DefaultTableModel model = (DefaultTableModel) ((JTable)((JScrollPane)viewTransactionsPanel.getComponent(2)).getViewport().getView()).getModel();

        // Clear existing data
        model.setRowCount(0);

        // Add updated data
        for (Transaction t : transactions) {
            model.addRow(new Object[]{
                    t.getId(),
                    t.getAmount(),
                    t.getDate(),
                    t.getCategory(),
                    t.getPaymentMethod(),
                    "Edit"
            });
        }
    }


    /**
     * Creates a panel for financial graphs based on transaction data
     */
    private void createGraphsPanel() {
        graphsPanel = new JPanel(new BorderLayout());

        // Create a tabbed pane for different graphs
        JTabbedPane graphsTabbedPane = new JTabbedPane();

        // Create expense by category chart
        JPanel categoryChartPanel = createCategoryPieChart();
        graphsTabbedPane.addTab("Expenses by Category", categoryChartPanel);

        // Create payment method chart (Credit vs Debit vs Cash)
        JPanel paymentMethodPanel = createPaymentMethodChart();
        graphsTabbedPane.addTab("Payment Methods", paymentMethodPanel);

        // Create spending trend chart
        JPanel spendingTrendPanel = createSpendingTrendChart();
        graphsTabbedPane.addTab("Monthly Trend", spendingTrendPanel);

        // Add refresh button
        JButton refreshButton = new JButton("Refresh Graphs");
        refreshButton.addActionListener(e -> {
            graphsTabbedPane.setComponentAt(0, createCategoryPieChart());
            graphsTabbedPane.setComponentAt(1, createPaymentMethodChart());
            graphsTabbedPane.setComponentAt(2, createSpendingTrendChart());
            graphsTabbedPane.repaint();
        });

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlPanel.add(refreshButton);

        // Add components to panel
        graphsPanel.add(new JLabel("Financial Graphs", SwingConstants.CENTER), BorderLayout.NORTH);
        graphsPanel.add(graphsTabbedPane, BorderLayout.CENTER);
        graphsPanel.add(controlPanel, BorderLayout.SOUTH);
    }

    /**
     * Creates a pie chart showing expenses by category
     */
    private JPanel createCategoryPieChart() {
        // Fetch transactions data from database and organize by category
        ArrayList<Transaction> transactions = (ArrayList<Transaction>) dbManager.getAllTransactions();

        // Calculate totals by category (only expenses)
        final java.util.Map<String, Double> categoryTotals = new java.util.HashMap<>();
        for (Transaction t : transactions) {
            // Skip income transactions
            if (t.isIncome()) {
                continue;
            }

            String category = t.getCategory();
            double amount = t.getAmount();

            if (categoryTotals.containsKey(category)) {
                categoryTotals.put(category, categoryTotals.get(category) + amount);
            } else {
                categoryTotals.put(category, amount);
            }
        }

        // Create colors for each category
        java.util.Map<String, Color> categoryColors = new java.util.HashMap<>();
        categoryColors.put("Food", new Color(255, 99, 132));
        categoryColors.put("Entertainment", new Color(54, 162, 235));
        categoryColors.put("Housing", new Color(255, 206, 86));
        categoryColors.put("Transportation", new Color(75, 192, 192));
        categoryColors.put("Savings", new Color(153, 102, 255));
        categoryColors.put("Other", new Color(255, 159, 64));

        // Create pie chart panel
        JPanel pieChartPanel = new JPanel(new BorderLayout());

        // Create custom pie chart
        JPanel chartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();
                int x = width / 2;
                int y = height / 2;
                int radius = Math.min(width, height) / 3;

                // Calculate total for percentages
                double total = categoryTotals.values().stream().mapToDouble(Double::doubleValue).sum();

                // Draw pie slices
                double currentAngle = 0;
                for (String category : categoryTotals.keySet()) {
                    double value = categoryTotals.get(category);
                    double sliceAngle = (value / total) * 360;

                    // Get color for this category
                    Color color = categoryColors.getOrDefault(category, new Color(100, 100, 100));

                    g2d.setColor(color);
                    g2d.fillArc(x - radius, y - radius, radius * 2, radius * 2,
                            (int) currentAngle, (int) sliceAngle);

                    // Calculate text position
                    double middleAngle = Math.toRadians(currentAngle + sliceAngle / 2);
                    int labelX = (int) (x + (radius * 1.3) * Math.cos(middleAngle));
                    int labelY = (int) (y + (radius * 1.3) * Math.sin(middleAngle));

                    // Draw percentage
                    String percent = String.format("%.1f%%", (value / total * 100));
                    g2d.setColor(Color.BLACK);
                    g2d.drawString(category + ": " + percent, labelX, labelY);

                    currentAngle += sliceAngle;
                }

                // If no data available
                if (categoryTotals.isEmpty()) {
                    g2d.setColor(Color.GRAY);
                    g2d.drawString("No expense data available", x - 70, y);
                }
            }
        };

        // Create legend panel
        JPanel legendPanel = new JPanel(new GridLayout(0, 1));
        legendPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (String category : categoryColors.keySet()) {
            if (categoryTotals.containsKey(category)) {
                JPanel legendItem = new JPanel(new FlowLayout(FlowLayout.LEFT));

                JPanel colorBox = new JPanel();
                colorBox.setBackground(categoryColors.get(category));
                colorBox.setPreferredSize(new Dimension(15, 15));

                double amount = categoryTotals.getOrDefault(category, 0.0);
                JLabel categoryLabel = new JLabel(category + ": $" + String.format("%.2f", amount));

                legendItem.add(colorBox);
                legendItem.add(categoryLabel);
                legendPanel.add(legendItem);
            }
        }

        pieChartPanel.add(chartPanel, BorderLayout.CENTER);
        pieChartPanel.add(legendPanel, BorderLayout.EAST);

        return pieChartPanel;
    }

    /**
     * Creates a bar chart comparing spending by payment method (Credit vs Debit vs Cash)
     */
    private JPanel createPaymentMethodChart() {
        // Create panel for payment method chart
        JPanel paymentMethodChartPanel = new JPanel(new BorderLayout());

        // Fetch transactions data from database
        ArrayList<Transaction> transactions = (ArrayList<Transaction>) dbManager.getAllTransactions();

        // Calculate totals by payment method (only considering expenses)
        // Use wrapper objects to hold mutable values
        final double[] creditCardTotal = {0};
        final double[] debitCardTotal = {0};
        final double[] cashTotal = {0};

        for (Transaction t : transactions) {
            // Skip income transactions if you want to only show expenses
            if (t.isIncome()) {
                continue;
            }

            String paymentMethod = t.getPaymentMethod();
            double amount = t.getAmount();

            if (paymentMethod.equals("Credit Card")) {
                creditCardTotal[0] += amount;
            } else if (paymentMethod.equals("Debit Card")) {
                debitCardTotal[0] += amount;
            } else if (paymentMethod.equals("Cash")) {
                cashTotal[0] += amount;
            }
        }

        // Create custom bar chart
        JPanel chartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();
                int barWidth = 80;
                int bottomMargin = 50;
                int leftMargin = 60;

                // Draw axes
                g2d.setColor(Color.BLACK);
                g2d.drawLine(leftMargin, height - bottomMargin, width - 20, height - bottomMargin); // X-axis
                g2d.drawLine(leftMargin, 20, leftMargin, height - bottomMargin); // Y-axis

                // Calculate scale based on maximum value
                double maxValue = Math.max(Math.max(creditCardTotal[0], debitCardTotal[0]), cashTotal[0]);
                if (maxValue == 0) maxValue = 100; // Default if no data
                double scale = (height - bottomMargin - 40) / maxValue;

                // Bar spacing
                int spacing = 40;
                int startX = leftMargin + 60;

                // Draw Credit Card bar
                int barHeight1 = (int) (creditCardTotal[0] * scale);
                g2d.setColor(new Color(255, 99, 132)); // Pink-red for credit card
                g2d.fillRect(startX, height - bottomMargin - barHeight1, barWidth, barHeight1);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(startX, height - bottomMargin - barHeight1, barWidth, barHeight1);

                // Draw Credit Card label and value
                g2d.drawString("Credit Card", startX + barWidth/2 - 30, height - bottomMargin + 20);
                g2d.drawString("$" + String.format("%.2f", creditCardTotal[0]), startX + barWidth/2 - 30,
                        height - bottomMargin - barHeight1 - 5);

                // Draw Debit Card bar
                int x2 = startX + barWidth + spacing;
                int barHeight2 = (int) (debitCardTotal[0] * scale);
                g2d.setColor(new Color(54, 162, 235)); // Blue for debit card
                g2d.fillRect(x2, height - bottomMargin - barHeight2, barWidth, barHeight2);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x2, height - bottomMargin - barHeight2, barWidth, barHeight2);

                // Draw Debit Card label and value
                g2d.drawString("Debit Card", x2 + barWidth/2 - 28, height - bottomMargin + 20);
                g2d.drawString("$" + String.format("%.2f", debitCardTotal[0]), x2 + barWidth/2 - 30,
                        height - bottomMargin - barHeight2 - 5);

                // Draw Cash bar
                int x3 = x2 + barWidth + spacing;
                int barHeight3 = (int) (cashTotal[0] * scale);
                g2d.setColor(new Color(75, 192, 192)); // Green-blue for cash
                g2d.fillRect(x3, height - bottomMargin - barHeight3, barWidth, barHeight3);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x3, height - bottomMargin - barHeight3, barWidth, barHeight3);

                // Draw Cash label and value
                g2d.drawString("Cash", x3 + barWidth/2 - 15, height - bottomMargin + 20);
                g2d.drawString("$" + String.format("%.2f", cashTotal[0]), x3 + barWidth/2 - 30,
                        height - bottomMargin - barHeight3 - 5);

                // Draw Y-axis labels
                g2d.setColor(Color.BLACK);
                int numYLabels = 5;
                for (int i = 0; i <= numYLabels; i++) {
                    double value = maxValue * i / numYLabels;
                    int y = height - bottomMargin - (int)(value * scale);
                    g2d.drawLine(leftMargin - 5, y, leftMargin, y);
                    g2d.drawString(String.format("$%.0f", value), leftMargin - 50, y + 5);
                }

                // Draw title
                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                g2d.drawString("Expenses by Payment Method", width/2 - 120, 20);

                // Calculate and display total spending
                double totalSpending = creditCardTotal[0] + debitCardTotal[0] + cashTotal[0];
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                g2d.setColor(Color.BLACK);
                g2d.drawString("Total Spending: $" + String.format("%.2f", totalSpending), width/2 - 80, height - 15);
            }
        };

        paymentMethodChartPanel.add(chartPanel, BorderLayout.CENTER);

        return paymentMethodChartPanel;
    }

    /**
     * Creates a line chart showing spending trends over time
     */
    private JPanel createSpendingTrendChart() {
        // Fetch transactions data from database
        ArrayList<Transaction> transactions = (ArrayList<Transaction>) dbManager.getAllTransactions();

        // Group transactions by month
        final java.util.Map<String, Double> monthlyExpenses = new java.util.TreeMap<>();
        final java.util.Map<String, Double> monthlyIncome = new java.util.TreeMap<>();

        for (Transaction t : transactions) {
            LocalDate date = t.getDate();
            String month = date.getYear() + "-" + String.format("%02d", date.getMonthValue());

            if (t.isIncome()) {
                if (monthlyIncome.containsKey(month)) {
                    monthlyIncome.put(month, monthlyIncome.get(month) + t.getAmount());
                } else {
                    monthlyIncome.put(month, t.getAmount());
                }
            } else {
                if (monthlyExpenses.containsKey(month)) {
                    monthlyExpenses.put(month, monthlyExpenses.get(month) + t.getAmount());
                } else {
                    monthlyExpenses.put(month, t.getAmount());
                }
            }
        }

        // Ensure we have same set of months for both maps
        java.util.Set<String> allMonths = new java.util.HashSet<>();
        allMonths.addAll(monthlyExpenses.keySet());
        allMonths.addAll(monthlyIncome.keySet());

        for (String month : allMonths) {
            if (!monthlyExpenses.containsKey(month)) {
                monthlyExpenses.put(month, 0.0);
            }
            if (!monthlyIncome.containsKey(month)) {
                monthlyIncome.put(month, 0.0);
            }
        }

        // Create line chart panel
        JPanel lineChartPanel = new JPanel(new BorderLayout());

        // Create custom line chart
        JPanel chartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();
                int bottomMargin = 70;
                int leftMargin = 60;
                int rightMargin = 40;
                int topMargin = 40;

                // Available chart area
                int chartWidth = width - leftMargin - rightMargin;
                int chartHeight = height - bottomMargin - topMargin;

                // Draw axes
                g2d.setColor(Color.BLACK);
                g2d.drawLine(leftMargin, height - bottomMargin, width - rightMargin, height - bottomMargin); // X-axis
                g2d.drawLine(leftMargin, topMargin, leftMargin, height - bottomMargin); // Y-axis

                // If no data available
                if (monthlyExpenses.isEmpty() && monthlyIncome.isEmpty()) {
                    g2d.setColor(Color.GRAY);
                    g2d.drawString("No data available for trend analysis", width/2 - 100, height/2);
                    return;
                }

                // Sort months
                java.util.List<String> sortedMonths = new java.util.ArrayList<>(monthlyExpenses.keySet());
                java.util.Collections.sort(sortedMonths);

                // Find maximum value for scale
                double maxValue = 0;
                for (String month : sortedMonths) {
                    maxValue = Math.max(maxValue, Math.max(monthlyExpenses.get(month), monthlyIncome.get(month)));
                }
                maxValue = maxValue == 0 ? 100 : maxValue * 1.1; // Add 10% margin

                // Calculate scale
                double xScale = (double) chartWidth / (sortedMonths.size() - 1 > 0 ? sortedMonths.size() - 1 : 1);
                double yScale = (double) chartHeight / maxValue;

                // Draw X-axis labels (months)
                g2d.setColor(Color.BLACK);
                for (int i = 0; i < sortedMonths.size(); i++) {
                    int x = leftMargin + (int)(i * xScale);
                    g2d.drawLine(x, height - bottomMargin, x, height - bottomMargin + 5);

                    // Rotate month labels for better readability
                    AffineTransform originalTransform = g2d.getTransform();
                    g2d.rotate(Math.PI / 4, x, height - bottomMargin + 10);
                    g2d.drawString(sortedMonths.get(i), x, height - bottomMargin + 10);
                    g2d.setTransform(originalTransform);
                }

                // Draw Y-axis labels
                g2d.setColor(Color.BLACK);
                int numYLabels = 5;
                for (int i = 0; i <= numYLabels; i++) {
                    double value = maxValue * i / numYLabels;
                    int y = height - bottomMargin - (int)(value * yScale);
                    g2d.drawLine(leftMargin - 5, y, leftMargin, y);
                    g2d.drawString(String.format("$%.0f", value), leftMargin - 50, y + 5);
                }

                // Draw expenses line
                g2d.setColor(new Color(255, 99, 132));
                g2d.setStroke(new BasicStroke(2f));

                int prevX = 0, prevY = 0;
                for (int i = 0; i < sortedMonths.size(); i++) {
                    String month = sortedMonths.get(i);
                    double value = monthlyExpenses.get(month);

                    int x = leftMargin + (int)(i * xScale);
                    int y = height - bottomMargin - (int)(value * yScale);

                    // Draw point
                    g2d.fillOval(x - 4, y - 4, 8, 8);

                    // Draw connecting line (skip first point)
                    if (i > 0) {
                        g2d.drawLine(prevX, prevY, x, y);
                    }

                    prevX = x;
                    prevY = y;
                }

                // Draw income line
                g2d.setColor(new Color(75, 192, 192));
                g2d.setStroke(new BasicStroke(2f));

                prevX = 0;
                prevY = 0;
                for (int i = 0; i < sortedMonths.size(); i++) {
                    String month = sortedMonths.get(i);
                    double value = monthlyIncome.get(month);

                    int x = leftMargin + (int)(i * xScale);
                    int y = height - bottomMargin - (int)(value * yScale);

                    // Draw point
                    g2d.fillOval(x - 4, y - 4, 8, 8);

                    // Draw connecting line (skip first point)
                    if (i > 0) {
                        g2d.drawLine(prevX, prevY, x, y);
                    }

                    prevX = x;
                    prevY = y;
                }

                // Draw title
                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                g2d.drawString("Monthly Financial Trend", width/2 - 100, 20);

                // Draw legend
                int legendX = width - rightMargin - 180;
                int legendY = topMargin + 10;

                // Expenses legend
                g2d.setColor(new Color(255, 99, 132));
                g2d.fillRect(legendX, legendY, 15, 15);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(legendX, legendY, 15, 15);
                g2d.drawString("Expenses", legendX + 20, legendY + 12);

                // Income legend
                g2d.setColor(new Color(75, 192, 192));
                g2d.fillRect(legendX, legendY + 25, 15, 15);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(legendX, legendY + 25, 15, 15);
                g2d.drawString("Income", legendX + 20, legendY + 37);
            }
        };

        lineChartPanel.add(chartPanel, BorderLayout.CENTER);

        return lineChartPanel;
    }

    /**
     * Main method to launch the application
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Savr().setVisible(true);
            }
        });
    }
}
