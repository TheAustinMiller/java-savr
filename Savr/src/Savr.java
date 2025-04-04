import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;

/**
 * Basic tabbed interface for Savr Finance App
 */
public class Savr extends JFrame {
    // Main components
    private JTabbedPane tabbedPane;
    private JPanel addTransactionPanel;
    private JPanel viewTransactionsPanel;
    private JPanel graphsPanel;

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
        formPanel.add(new JTextField());

        formPanel.add(new JLabel("Date:"));
        formPanel.add(new JTextField(LocalDate.now().toString()));

        formPanel.add(new JLabel("Category:"));
        formPanel.add(new JComboBox<>(new String[]{"Food", "Transport", "Housing", "Other"}));

        formPanel.add(new JLabel("Payment Method:"));
        formPanel.add(new JComboBox<>(new String[]{"Cash", "Credit Card", "Debit Card"}));

        formPanel.add(new JLabel("Type:"));
        formPanel.add(new JComboBox<>(new String[]{"Expense", "Income"}));

        JButton addButton = new JButton("Add Transaction");

        // Add components to panel
        addTransactionPanel.add(new JLabel("Add Transaction", SwingConstants.CENTER), BorderLayout.NORTH);
        addTransactionPanel.add(formPanel, BorderLayout.CENTER);
        addTransactionPanel.add(addButton, BorderLayout.SOUTH);
    }

    /**
     * Creates a basic panel for viewing transactions
     */
    private void createViewTransactionsPanel() {
        viewTransactionsPanel = new JPanel(new BorderLayout());

        // Create simple table
        String[] columns = {"ID", "Date", "Amount", "Category", "Type"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);
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