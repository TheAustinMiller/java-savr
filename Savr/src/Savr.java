import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
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
        categoryField = new JComboBox<>(new String[]{"Food", "Entertainment", "Housing", "Transportation", "Savings", "Other"});
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
                        new String[]{"Food", "Entertainment", "Housing", "Transportation", "Savings", "Other"});
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
     * Creates a basic panel for graphs
     */
    private void createGraphsPanel() {
        graphsPanel = new JPanel(new BorderLayout());

        // Create a placeholder for the chart
        JPanel chartPanel = new JPanel();
        chartPanel.setBackground(Color.WHITE);
        chartPanel.setBorder(BorderFactory.createEtchedBorder());

        JLabel placeholder = new JLabel("Graphs will be displayed here", SwingConstants.CENTER);
        placeholder.setFont(new Font("Arial", Font.BOLD, 16));
        chartPanel.add(placeholder);

        // Add components to panel
        graphsPanel.add(new JLabel("Financial Graphs", SwingConstants.CENTER), BorderLayout.NORTH);
        graphsPanel.add(chartPanel, BorderLayout.CENTER);
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
