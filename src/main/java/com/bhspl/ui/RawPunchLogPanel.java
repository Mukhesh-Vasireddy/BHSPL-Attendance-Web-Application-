package com.bhspl.ui;
 
 import com.bhspl.db.DatabaseManager;
 import com.bhspl.util.UIHelper;
 import net.miginfocom.swing.MigLayout;
 
 import javax.swing.*;
 import java.awt.*;
 import java.sql.SQLException;
 import java.time.LocalDate;
 import java.util.List;
 import java.util.Map;
 
 /**
  * Premium Raw Punch Logs Panel with standardized footer.
  */
 public class RawPunchLogPanel extends JPanel {
 
     private UIHelper.StyledTablePanel tablePanel;
     private JTextField dateField;
     private JComboBox<String> deptFilter;
     private JLabel statusLbl;
 
     private static final String[] COLUMNS = {
             "Emp ID", "Name", "Department", "Date", "Punch In", "Punch Out", "Total Hours", "Status"
     };
 
     public RawPunchLogPanel() {
         setBackground(UIHelper.BG_MAIN);
         setLayout(new MigLayout("ins 24, wrap, gap 0, fill", "[grow]", "[] 16 [grow] 8 []"));
         buildUI();
         loadData();
     }
 
     private void buildUI() {
         JPanel toolbar = new JPanel(new MigLayout("ins 0, gap 12", "[] 8 [] 15 [] 8 [] push []"));
         toolbar.setOpaque(false);
         dateField = new JTextField(LocalDate.now().toString(), 10);
         deptFilter = new JComboBox<>(new String[] { "All" }); loadDepartments();
         JButton filterBtn = UIHelper.makeButton("Filter Logs", UIHelper.PRIMARY, "search.svg");
         JButton refreshBtn = UIHelper.makeButton("Refresh", new Color(0x334155), "sync.svg");
         filterBtn.addActionListener(e -> loadData());
         refreshBtn.addActionListener(e -> loadData());
         toolbar.add(new JLabel("Date:")); toolbar.add(dateField);
         toolbar.add(new JLabel("Dept:")); toolbar.add(deptFilter, "w 150!");
         toolbar.add(filterBtn); toolbar.add(refreshBtn, "right");
         add(toolbar, "growx");
 
         tablePanel = new UIHelper.StyledTablePanel(COLUMNS);
         tablePanel.setBorder(UIHelper.createCardBorder());
         add(tablePanel, "grow, push, wmin 0");
 
         statusLbl = UIHelper.createSummaryLabel("Processing raw punch sequences...");
         add(statusLbl, "growx, gaptop 8");
     }
 
     private void loadDepartments() {
         try { List<Map<String, Object>> rows = DatabaseManager.getInstance().query("SELECT dept_name FROM departments WHERE status='Active'"); for (Map<String, Object> r : rows) deptFilter.addItem((String) r.get("dept_name")); } catch (Exception ignored) {}
     }
 
     private void loadData() {
         tablePanel.clearRows();
         String date = dateField.getText().trim(); String dept = (String) deptFilter.getSelectedItem();
         SwingWorker<List<Map<String, Object>>, Void> worker = new SwingWorker<>() {
             @Override protected List<Map<String, Object>> doInBackground() throws Exception {
                 String sql = "SELECT p.emp_id, e.emp_name, e.department, p.punch_date, p.punch_in, p.punch_out, p.duration, p.status " +
                         "FROM raw_punches p JOIN employees e ON p.emp_id = e.emp_id WHERE p.punch_date = ?";
                 if (!"All".equals(dept)) sql += " AND e.department = '" + dept + "'";
                 sql += " ORDER BY e.emp_name ASC";
                 return DatabaseManager.getInstance().fetchAll(sql, date);
             }
             @Override protected void done() {
                 try {
                     List<Map<String, Object>> rows = get();
                     int p=0, a=0;
                     for (Map<String, Object> r : rows) {
                         tablePanel.addRow(new Object[] { r.get("emp_id"), r.get("emp_name"), r.get("department"), r.get("punch_date"), r.get("punch_in"), r.get("punch_out"), r.get("duration"), r.get("status") });
                         if ("Present".equalsIgnoreCase(String.valueOf(r.get("status")))) p++; else a++;
                     }
                     statusLbl.setText("Total Punch Logs: " + rows.size() + " | Present: " + p + " | Absent: " + a);
                 } catch (Exception ignored) {}
             }
         };
         worker.execute();
     }
 }
