package com.bhspl.ui;
 
 import com.bhspl.db.DatabaseManager;
 import com.bhspl.util.UIHelper;
 import net.miginfocom.swing.MigLayout;
 
 import javax.swing.*;
 import java.awt.*;
 import java.sql.SQLException;
 import java.util.List;
 import java.util.Map;
 
 /**
  * Weekly Off Master panel with standardized footer and functional actions.
  */
 public class WeeklyOffPanel extends JPanel {
     private UIHelper.StyledTablePanel tablePanel;
     private JComboBox<String> deptCombo;
     private JLabel statusLbl;
     private static final String[] COLUMNS = {"ID", "Emp ID", "Name", "Dept", "Off Day 1", "Off Day 2", "From", "To", "Remarks"};
 
     public WeeklyOffPanel() {
         setLayout(new MigLayout("ins 24, fill, wrap", "[grow]", "[] 15 [] 15 [] 15 [grow] 8 []"));
         setBackground(UIHelper.BG_MAIN);
         buildUI();
         loadData();
     }
 
     private void buildUI() {
         JLabel title = new JLabel("Weekly Off Master");
         title.setFont(new Font("Segoe UI", Font.BOLD, 22));
         title.setForeground(new Color(0x1E293B));
         add(title, "gapbottom 5");
 
         JPanel actions = new JPanel(new MigLayout("ins 0, gap 10", "[] [] [] [] []"));
         actions.setOpaque(false);
         
         JButton assignBtn = UIHelper.makeButton("Assign Off", UIHelper.SUCCESS, "plus.svg");
         JButton editBtn   = UIHelper.makeButton("Edit", UIHelper.PRIMARY, "edit.svg");
         JButton deleteBtn = UIHelper.makeButton("Delete", UIHelper.DANGER, "trash.svg");
         JButton bulkBtn   = UIHelper.makeButton("Bulk Assign", UIHelper.WARNING, "employees.svg");
         JButton refreshBtn = UIHelper.makeButton("Refresh", new Color(0x334155), "sync.svg");
 
         assignBtn.addActionListener(e -> showAssignOffDialog(null));
         editBtn.addActionListener(e -> {
             Object id = tablePanel.getSelectedValue();
             if (id == null) { UIHelper.showInfo(this, "Select a record to edit."); return; }
             showAssignOffDialog((Integer) id);
         });
         deleteBtn.addActionListener(e -> deleteSelected());
         refreshBtn.addActionListener(e -> loadData());
 
         actions.add(assignBtn);
         actions.add(editBtn);
         actions.add(deleteBtn);
         actions.add(bulkBtn);
         actions.add(refreshBtn);
         add(actions, "growx");
 
         JPanel filterArea = new JPanel(new MigLayout("ins 0, gap 10", "[] [] []"));
         filterArea.setOpaque(false);
         filterArea.add(new JLabel("Dept:"));
         deptCombo = new JComboBox<>(new String[]{"All"}); loadDepartments();
         filterArea.add(deptCombo, "w 150!");
         JButton filterBtn = UIHelper.makeButton("Filter", UIHelper.PRIMARY);
         filterBtn.addActionListener(e -> loadData());
         filterArea.add(filterBtn);
         add(filterArea, "growx");
 
         tablePanel = new UIHelper.StyledTablePanel(COLUMNS);
         tablePanel.setBorder(UIHelper.createCardBorder());
         add(tablePanel, "grow, push, wmin 0");
 
         statusLbl = UIHelper.createSummaryLabel("Analyzing weekly off assignments...");
         add(statusLbl, "growx, gaptop 8");
     }
 
     private void loadDepartments() {
         try { List<Map<String, Object>> depts = DatabaseManager.getInstance().query("SELECT dept_name FROM departments"); for (Map<String, Object> d : depts) deptCombo.addItem((String) d.get("dept_name")); } catch (SQLException e) { e.printStackTrace(); }
     }
 
     private void loadData() {
         tablePanel.clearRows();
         try {
             String sql = "SELECT w.*, e.emp_name, e.department as dept_name FROM weekly_offs w JOIN employees e ON w.emp_id = e.emp_id";
             String selectedDept = (String) deptCombo.getSelectedItem();
             if (selectedDept != null && !"All".equals(selectedDept)) sql += " WHERE e.department = '" + selectedDept + "'";
             List<Map<String, Object>> rows = DatabaseManager.getInstance().query(sql);
             java.util.Set<String> uniqueEmps = new java.util.HashSet<>();
             for (Map<String, Object> r : rows) {
                 tablePanel.addRow(new Object[]{ r.get("id"), r.get("emp_id"), r.get("emp_name"), r.get("dept_name"), r.get("off_day1"), r.get("off_day2"), r.get("effective_from"), r.get("effective_to"), r.get("remarks") });
                 uniqueEmps.add(String.valueOf(r.get("emp_id")));
             }
             statusLbl.setText("Total Assignments: " + rows.size() + " | Employees Covered: " + uniqueEmps.size());
         } catch (SQLException e) { e.printStackTrace(); }
     }
 
     private void deleteSelected() {
         Object idVal = tablePanel.getSelectedValue();
         if (idVal == null) { UIHelper.showInfo(this, "Select a record first."); return; }
         int id = (int) idVal;
         if (!UIHelper.confirm(this, "Confirm Delete", "Permanently delete this weekly off assignment?")) return;
         try {
             DatabaseManager.getInstance().execute("DELETE FROM weekly_offs WHERE id=?", id);
             loadData();
             UIHelper.showSuccess(this, "Record deleted successfully.");
         } catch (SQLException ex) { UIHelper.showError(this, "Error: " + ex.getMessage()); }
     }
 
     private void showAssignOffDialog(Integer id) {
         JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), id == null ? "Assign Weekly Off" : "Edit Weekly Off", true);
         dialog.setLayout(new MigLayout("ins 20, wrap 2, gapy 15", "[shrink] 15 [grow, fill]", "[]"));
         
         JComboBox<String> empCombo = new JComboBox<>(); loadEmployeesIntoCombo(empCombo);
         String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday", "None"};
         JComboBox<String> day1Combo = new JComboBox<>(days), day2Combo = new JComboBox<>(days);
         day1Combo.setSelectedItem("Sunday"); day2Combo.setSelectedItem("None");
         JTextField fromDate = new JTextField(java.time.LocalDate.now().toString()), toDate = new JTextField("2099-12-31"), remarks = new JTextField();
         
         if (id != null) {
             try {
                 Map<String, Object> data = DatabaseManager.getInstance().fetchOne("SELECT * FROM weekly_offs WHERE id=?", id);
                 if (data != null) {
                     String empId = (String) data.get("emp_id");
                     for (int i=0; i<empCombo.getItemCount(); i++) if (empCombo.getItemAt(i).startsWith(empId)) empCombo.setSelectedIndex(i);
                     day1Combo.setSelectedItem(data.get("off_day1"));
                     day2Combo.setSelectedItem(data.get("off_day2"));
                     fromDate.setText(data.get("effective_from").toString());
                     toDate.setText(data.get("effective_to").toString());
                     remarks.setText(DatabaseManager.str(data, "remarks"));
                 }
             } catch (SQLException ignored) {}
         }
 
         dialog.add(new JLabel("Select Employee:")); dialog.add(empCombo);
         dialog.add(new JLabel("Off Day 1:")); dialog.add(day1Combo);
         dialog.add(new JLabel("Off Day 2:")); dialog.add(day2Combo);
         dialog.add(new JLabel("Effective From:")); dialog.add(fromDate);
         dialog.add(new JLabel("Effective To:")); dialog.add(toDate);
         dialog.add(new JLabel("Remarks:")); dialog.add(remarks);
         
         JButton saveBtn = UIHelper.makeButton("Save Assignment", UIHelper.SUCCESS);
         saveBtn.addActionListener(e -> {
             String empStr = (String) empCombo.getSelectedItem(); if (empStr == null) return; String empId = empStr.split(" - ")[0];
             try {
                 if (id == null) {
                     DatabaseManager.getInstance().execute("INSERT INTO weekly_offs (emp_id, off_day1, off_day2, effective_from, effective_to, remarks) VALUES (?,?,?,?,?,?)", empId, day1Combo.getSelectedItem(), day2Combo.getSelectedItem(), fromDate.getText(), toDate.getText(), remarks.getText());
                 } else {
                     DatabaseManager.getInstance().execute("UPDATE weekly_offs SET emp_id=?, off_day1=?, off_day2=?, effective_from=?, effective_to=?, remarks=? WHERE id=?", empId, day1Combo.getSelectedItem(), day2Combo.getSelectedItem(), fromDate.getText(), toDate.getText(), remarks.getText(), id);
                 }
                 dialog.dispose(); loadData(); UIHelper.showSuccess(this, "Weekly off saved successfully!");
             } catch (SQLException ex) { UIHelper.showError(dialog, "Error: " + ex.getMessage()); }
         });
         dialog.add(saveBtn, "span 2, growx, gaptop 10"); dialog.pack(); dialog.setSize(450, dialog.getHeight()); dialog.setLocationRelativeTo(this); dialog.setVisible(true);
     }
 
     private void loadEmployeesIntoCombo(JComboBox<String> combo) {
         try { List<Map<String, Object>> emps = DatabaseManager.getInstance().query("SELECT emp_id, emp_name FROM employees ORDER BY emp_name"); for (Map<String, Object> e : emps) combo.addItem(e.get("emp_id") + " - " + e.get("emp_name")); } catch (SQLException e) { e.printStackTrace(); }
     }
 }
