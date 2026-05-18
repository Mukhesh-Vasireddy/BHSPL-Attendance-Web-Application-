package com.bhspl.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.bhspl.db.DatabaseManager;
import com.bhspl.util.UIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

public class EmployeePanel extends JPanel {

    private UIHelper.StyledTablePanel tablePanel;
    private JTextField searchField;
    private JLabel statusLbl;

    private static final String[] COLUMNS = {
            "Emp ID", "Name", "Department", "Designation", "Shift", "Phone", "Status", "Action"
    };

    public EmployeePanel() {
        setBackground(UIHelper.BG_MAIN);
        setLayout(new MigLayout("ins 24, wrap, gap 0, fill", "[grow]", "[] 16 [grow] 8 []"));
        buildUI();
        loadData("");
    }

    private void buildUI() {
        // Toolbar
        JPanel toolbar = new JPanel(new MigLayout("ins 0, gap 12, wrap", "[] 8 [grow]"));
        toolbar.setOpaque(false);

        searchField = new JTextField(20);
        searchField.setFont(UIHelper.FNT_MEDIUM);
        searchField.putClientProperty("JTextField.placeholderText", "Search name or ID...");

        JButton searchBtn = UIHelper.makeButton("Search", UIHelper.PRIMARY);
        JButton refreshBtn = UIHelper.makeButton("Refresh", new Color(0x334155));

        searchBtn.addActionListener(e -> loadData(searchField.getText().trim()));
        searchField.addActionListener(e -> loadData(searchField.getText().trim()));
        refreshBtn.addActionListener(e -> loadData(searchField.getText().trim()));

        toolbar.add(searchField, "growx");
        toolbar.add(searchBtn);
        toolbar.add(refreshBtn);

        add(toolbar, "growx");

        // Responsive toolbar
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = getWidth();
                String layout;
                String cols;
                if (w < 600) {
                    layout = "ins 0, gap 12, wrap 1";
                    cols = "[grow, fill]";
                } else {
                    layout = "ins 0, gap 12, wrap";
                    cols = "[] 8 [] push []";
                }
                toolbar.setLayout(new MigLayout(layout, cols));
                toolbar.revalidate();
            }
        });

        tablePanel = new UIHelper.StyledTablePanel(COLUMNS);
        tablePanel.setBorder(UIHelper.createCardBorder());

        // Add Edit Icon to Action Column
        tablePanel.getTable().getColumn("Action").setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            private final FlatSVGIcon icon = new FlatSVGIcon("icons/edit.svg", 16, 16);
            {
                icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> UIHelper.PRIMARY));
                setHorizontalAlignment(SwingConstants.CENTER);
            }

            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, "", s, f, r, c);
                setIcon(icon);
                setToolTipText("Edit Employee Details");
                return this;
            }
        });

        tablePanel.getTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = tablePanel.getTable().columnAtPoint(e.getPoint());
                if (col == tablePanel.getTable().getColumnModel().getColumnIndex("Action") || e.getClickCount() == 2) {
                    Object id = tablePanel.getSelectedValue();
                    if (id != null)
                        openForm((String) id);
                }
            }
        });
        add(tablePanel, "grow, push, wmin 0");

        // Status bar
        statusLbl = new JLabel("Showing all active employees");
        statusLbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        statusLbl.setForeground(UIHelper.TEXT_LIGHT);
        add(statusLbl, "gapleft 4");
    }

    private void loadData(String search) {
        tablePanel.clearRows();
        SwingWorker<List<Map<String, Object>>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Map<String, Object>> doInBackground() throws Exception {
                DatabaseManager db = DatabaseManager.getInstance();
                String sql = "SELECT emp_id, emp_name, department, designation, shift, phone, status " +
                        "FROM employees WHERE emp_name LIKE ? OR emp_id LIKE ? " +
                        "ORDER BY CASE WHEN emp_name LIKE ? THEN 0 WHEN emp_id LIKE ? THEN 1 ELSE 2 END, emp_name";
                String like = "%" + search + "%";
                String startsWith = search + "%";
                return db.fetchAll(sql, like, like, startsWith, startsWith);
            }

            @Override
            protected void done() {
                try {
                    List<Map<String, Object>> rows = get();
                    for (Map<String, Object> r : rows) {
                        tablePanel.addRow(new Object[] {
                                r.get("emp_id"), r.get("emp_name"), r.get("department"),
                                r.get("designation"), r.get("shift"), r.get("phone"), r.get("status"),
                                "Edit"
                        });
                    }
                    statusLbl.setText("Found " + rows.size() + " employees");
                } catch (Exception ignored) {
                }
            }
        };
        worker.execute();
    }

    private void openForm(String empId) {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        new EmployeeForm(parent, empId, () -> loadData(searchField.getText().trim()));
    }
}
