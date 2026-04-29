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
 
 /**
  * Premium Employee Management Panel with standardized footer and full CRUD support.
  */
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
         // Toolbar Area
         JPanel toolbar = new JPanel(new MigLayout("ins 0, gap 12", "[] 8 [grow] [] 8 [] 8 []"));
         toolbar.setOpaque(false);
 
         searchField = new JTextField(20);
         searchField.setFont(UIHelper.FNT_MEDIUM);
         searchField.putClientProperty("JTextField.placeholderText", "Search name or ID...");
 
         JButton searchBtn  = UIHelper.makeButton("Search", UIHelper.PRIMARY, "search.svg");
         JButton addBtn     = UIHelper.makeButton("Add Employee", UIHelper.SUCCESS, "plus.svg");
         JButton refreshBtn = UIHelper.makeButton("Refresh", new Color(0x334155), "sync.svg");
 
         searchBtn.addActionListener(e -> loadData(searchField.getText().trim()));
         searchField.addActionListener(e -> loadData(searchField.getText().trim()));
         addBtn.addActionListener(e -> openForm(null));
         refreshBtn.addActionListener(e -> loadData(searchField.getText().trim()));
 
         toolbar.add(new JLabel("Quick Search:"));
         toolbar.add(searchField, "growx");
         toolbar.add(searchBtn);
         toolbar.add(addBtn);
         toolbar.add(refreshBtn);
         add(toolbar, "growx");
 
         // Responsive Toolbar Adjustments
         addComponentListener(new java.awt.event.ComponentAdapter() {
             @Override
             public void componentResized(java.awt.event.ComponentEvent e) {
                 int w = getWidth();
                 if (w < 800) {
                     toolbar.setLayout(new MigLayout("ins 0, gap 8, wrap 2", "[] [grow]"));
                 } else {
                     toolbar.setLayout(new MigLayout("ins 0, gap 12", "[] 8 [grow] [] 8 [] 8 []"));
                 }
                 toolbar.revalidate();
             }
         });
 
         tablePanel = new UIHelper.StyledTablePanel(COLUMNS);
         tablePanel.setBorder(UIHelper.createCardBorder());
 
         // Actions Column Styling (Edit Icon)
         tablePanel.getTable().getColumn("Action").setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
             private final FlatSVGIcon icon = new FlatSVGIcon("icons/edit.svg", 16, 16);
             { icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> UIHelper.PRIMARY)); setHorizontalAlignment(SwingConstants.CENTER); }
             @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                 super.getTableCellRendererComponent(t, "", s, f, r, c); setIcon(icon); setToolTipText("Edit Details"); return this;
             }
         });
 
         tablePanel.getTable().addMouseListener(new MouseAdapter() {
             @Override public void mouseClicked(MouseEvent e) {
                 int col = tablePanel.getTable().columnAtPoint(e.getPoint());
                 if (col == tablePanel.getTable().getColumnModel().getColumnIndex("Action") || e.getClickCount() == 2) {
                     Object id = tablePanel.getSelectedValue();
                     if (id != null) openForm((String) id);
                 }
             }
         });
         add(tablePanel, "grow, push, wmin 0");
 
         // Standardized Indigo Summary Footer
         statusLbl = UIHelper.createSummaryLabel("Initializing directory...");
         add(statusLbl, "growx, gaptop 8");
     }
 
     private void loadData(String search) {
         tablePanel.clearRows();
         SwingWorker<List<Map<String, Object>>, Void> worker = new SwingWorker<>() {
             @Override
             protected List<Map<String, Object>> doInBackground() throws Exception {
                 DatabaseManager db = DatabaseManager.getInstance();
                 String sql = "SELECT emp_id, emp_name, department, designation, shift, phone, status FROM employees WHERE emp_name LIKE ? OR emp_id LIKE ? ORDER BY emp_name";
                 String like = "%" + search + "%";
                 return db.fetchAll(sql, like, like);
             }
 
             @Override
             protected void done() {
                 try {
                     List<Map<String, Object>> rows = get();
                     int active = 0;
                     for (Map<String, Object> r : rows) {
                         tablePanel.addRow(new Object[] { r.get("emp_id"), r.get("emp_name"), r.get("department"), r.get("designation"), r.get("shift"), r.get("phone"), r.get("status"), "Edit" });
                         if ("Active".equalsIgnoreCase(String.valueOf(r.get("status")))) active++;
                     }
                     statusLbl.setText("Total Employees: " + rows.size() + " | Active: " + active + " | Inactive: " + (rows.size() - active));
                 } catch (Exception ignored) {}
             }
         };
         worker.execute();
     }
 
     private void openForm(String empId) {
         JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
         new EmployeeForm(parent, empId, () -> loadData(searchField.getText().trim()));
     }
 }
