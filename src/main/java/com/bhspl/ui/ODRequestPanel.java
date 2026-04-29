package com.bhspl.ui;
 
 import com.bhspl.db.DatabaseManager;
 import com.bhspl.util.UIHelper;
 import net.miginfocom.swing.MigLayout;
 
 import javax.swing.*;
 import javax.swing.table.DefaultTableCellRenderer;
 import java.awt.*;
 import java.sql.SQLException;
 import java.time.LocalDate;
 import java.time.format.DateTimeFormatter;
 import java.util.List;
 import java.util.Map;
 
 /**
  * Professional OD (On Duty) Requests panel with standardized footer.
  */
 public class ODRequestPanel extends JPanel {
 
     private UIHelper.StyledTablePanel tablePanel;
     private JComboBox<String> statusFilter, deptFilter;
     private JTextField monthFilter;
     private JLabel statusLbl;
     
     private static final String[] COLUMNS = { "ID", "Emp ID", "Name", "Dept", "From Date", "To Date", "Days", "Type", "Time", "Location", "Purpose", "Status", "Applied On" };
 
     public ODRequestPanel() {
         setLayout(new MigLayout("ins 24, fill, wrap", "[grow]", "[] 10 [] 15 [] 15 [] 15 [grow] 8 []"));
         setBackground(UIHelper.BG_MAIN);
         buildUI();
         loadData();
     }
 
     private void buildUI() {
         JPanel header = new JPanel(new MigLayout("ins 0", "[] 10 [] push"));
         header.setOpaque(false);
         JLabel printIcon = new JLabel(new com.formdev.flatlaf.extras.FlatSVGIcon("icons/reports.svg", 18, 18));
         header.add(printIcon);
         JLabel title = new JLabel("OD (On Duty) Requests");
         title.setFont(new Font("Segoe UI", Font.BOLD, 22));
         title.setForeground(new Color(0x1E293B));
         header.add(title);
         add(header, "growx");
 
         JPanel infoBox = new JPanel(new MigLayout("ins 12", "[] 10 [grow]"));
         infoBox.setBackground(new Color(0xDCFCE7)); infoBox.setBorder(BorderFactory.createLineBorder(new Color(0xBBF7D0)));
         JLabel infoIcon = new JLabel("📌"); infoBox.add(infoIcon);
         JLabel infoText = new JLabel("OD = Employee is On Duty outside office / field work / client visit. Attendance marked Present. Leave balance not deducted.");
         infoText.setFont(new Font("Segoe UI", Font.PLAIN, 13)); infoText.setForeground(new Color(0x166534));
         infoBox.add(infoText);
         add(infoBox, "growx");
 
         JPanel actions = new JPanel(new MigLayout("ins 0, gap 10"));
         actions.setOpaque(false);
         JButton applyBtn = UIHelper.makeButton("Apply OD", UIHelper.SUCCESS, "plus.svg");
         JButton editBtn = UIHelper.makeButton("Edit", UIHelper.PRIMARY, "edit.svg");
         JButton deleteBtn = UIHelper.makeButton("Delete", UIHelper.DANGER, "trash.svg");
         JButton approveBtn = UIHelper.makeButton("Approve", new Color(0x15803D), "check.svg");
         JButton rejectBtn = UIHelper.makeButton("Reject", new Color(0xB91C1C), "x.svg");
         JButton refreshBtn = UIHelper.makeButton("Refresh", new Color(0x475569), "sync.svg");
 
         applyBtn.addActionListener(e -> openForm(null));
         editBtn.addActionListener(e -> { Object id = tablePanel.getSelectedValue(); if (id != null) openForm((Integer) id); else UIHelper.showError(this, "Select a request to edit."); });
         deleteBtn.addActionListener(e -> deleteSelected());
         approveBtn.addActionListener(e -> updateStatus("Approved"));
         rejectBtn.addActionListener(e -> updateStatus("Rejected"));
         refreshBtn.addActionListener(e -> loadData());
 
         actions.add(applyBtn); actions.add(editBtn); actions.add(deleteBtn); actions.add(approveBtn); actions.add(rejectBtn); actions.add(refreshBtn);
         add(actions, "growx");
 
         JPanel filters = new JPanel(new MigLayout("ins 0, gap 12", "[] 5 [] 15 [] 5 [] 15 [] 5 [] 10 []"));
         filters.setOpaque(false);
         filters.add(new JLabel("Status:")); statusFilter = new JComboBox<>(new String[]{"All", "Pending", "Approved", "Rejected"}); filters.add(statusFilter);
         filters.add(new JLabel("Dept:")); deptFilter = new JComboBox<>(new String[]{"All"}); loadDepartments(); filters.add(deptFilter, "w 160!");
         filters.add(new JLabel("Month:")); monthFilter = new JTextField(LocalDate.now().format(DateTimeFormatter.ofPattern("MM-yyyy"))); filters.add(monthFilter, "w 110!");
         JButton filterBtn = UIHelper.makeButton("Filter", UIHelper.PRIMARY, "search.svg"); filterBtn.addActionListener(e -> loadData()); filters.add(filterBtn);
         add(filters, "growx");
 
         tablePanel = new UIHelper.StyledTablePanel(COLUMNS);
         styleTable();
         add(tablePanel, "grow, push, wmin 0");
 
         statusLbl = UIHelper.createSummaryLabel("Analyzing OD requests...");
         add(statusLbl, "growx, gaptop 8");
     }
 
     private void styleTable() {
         JTable table = tablePanel.getTable();
         table.getColumnModel().getColumn(0).setMaxWidth(50); table.getColumnModel().getColumn(6).setMaxWidth(60);
         table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
             @Override public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                 Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                 String status = String.valueOf(t.getValueAt(row, 11));
                 if (!sel) {
                     if ("Approved".equals(status)) setForeground(new Color(0x15803D));
                     else if ("Rejected".equals(status)) setForeground(new Color(0xB91C1C));
                     else if ("Pending".equals(status)) setForeground(new Color(0xCA8A04));
                     else setForeground(new Color(0x1E293B));
                 }
                 setHorizontalAlignment(col == 2 || col == 3 || col == 9 || col == 10 ? SwingConstants.LEFT : SwingConstants.CENTER);
                 setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5)); return c;
             }
         });
     }
 
     private void loadDepartments() {
         try { List<Map<String, Object>> rows = DatabaseManager.getInstance().query("SELECT dept_name FROM departments WHERE status='Active'"); for (Map<String, Object> r : rows) deptFilter.addItem((String) r.get("dept_name")); } catch (Exception ignored) {}
     }
 
     private void loadData() {
         tablePanel.clearRows();
         try {
             StringBuilder sql = new StringBuilder("SELECT o.*, e.emp_name, e.department FROM od_requests o JOIN employees e ON o.emp_id = e.emp_id WHERE 1=1");
             String st = (String) statusFilter.getSelectedItem(); if (!"All".equals(st)) sql.append(" AND o.status='").append(st).append("'");
             String dpt = (String) deptFilter.getSelectedItem(); if (!"All".equals(dpt)) sql.append(" AND e.department='").append(dpt).append("'");
             String month = monthFilter.getText().trim(); if (month.matches("\\d{2}-\\d{4}")) { sql.append(" AND DATE_FORMAT(o.od_from, '%m-%Y')='").append(month).append("'"); }
             sql.append(" ORDER BY o.applied_on DESC");
             List<Map<String, Object>> rows = DatabaseManager.getInstance().query(sql.toString());
             int p=0, a=0, r=0;
             for (Map<String, Object> row : rows) {
                 String timeStr = "Full Day"; if (row.get("from_time") != null && row.get("to_time") != null) { timeStr = row.get("from_time").toString().substring(0, 5) + " - " + row.get("to_time").toString().substring(0, 5); }
                 tablePanel.addRow(new Object[]{ row.get("id"), row.get("emp_id"), row.get("emp_name"), row.get("department"), fmtDate(row.get("od_from")), fmtDate(row.get("od_to")), row.get("od_days"), row.get("od_type"), timeStr, row.get("location") != null ? row.get("location") : "N/A", row.get("purpose") != null ? row.get("purpose") : row.get("reason"), row.get("status"), fmtDate(row.get("applied_on")) });
                 String s = String.valueOf(row.get("status")); if ("Pending".equalsIgnoreCase(s)) p++; else if ("Approved".equalsIgnoreCase(s)) a++; else if ("Rejected".equalsIgnoreCase(s)) r++;
             }
             statusLbl.setText("Total OD Requests: " + rows.size() + " | Pending: " + p + " | Approved: " + a + " | Rejected: " + r);
         } catch (SQLException e) { e.printStackTrace(); }
     }
     private String fmtDate(Object v) { if (v == null) return "-"; if (v instanceof java.sql.Date) return DateTimeFormatter.ofPattern("dd-MM-yyyy").format(((java.sql.Date) v).toLocalDate()); if (v instanceof java.sql.Timestamp) return DateTimeFormatter.ofPattern("dd-MM-yyyy").format(((java.sql.Timestamp) v).toLocalDateTime().toLocalDate()); return v.toString(); }
     private void openForm(Integer id) { new ODForm((JFrame) SwingUtilities.getWindowAncestor(this), id, this::loadData); }
     private void updateStatus(String newStatus) {
         Object id = tablePanel.getSelectedValue(); if (id == null) { UIHelper.showError(this, "Select a request first."); return; }
         if (!UIHelper.confirm(this, "Confirm " + newStatus, "Mark OD request #" + id + " as " + newStatus + "?")) return;
         try {
             DatabaseManager db = DatabaseManager.getInstance(); db.execute("UPDATE od_requests SET status=? WHERE id=?", newStatus, id);
             if ("Approved".equals(newStatus)) {
                 Map<String, Object> req = db.queryOne("SELECT * FROM od_requests WHERE id=?", id);
                 if (req != null) {
                     String eid = (String) req.get("emp_id"); LocalDate start = LocalDate.parse(req.get("od_from").toString()); LocalDate end = LocalDate.parse(req.get("od_to").toString());
                     Map<String, Object> emp = db.fetchOne("SELECT s.work_hours FROM employees e JOIN shifts s ON e.shift=s.shift_name WHERE e.emp_id=?", eid);
                     double wh = emp != null ? DatabaseManager.dbl(emp, "work_hours") : 8.0;
                     for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) { db.execute("INSERT INTO attendance (emp_id, punch_date, status, work_hours, remarks) VALUES (?,?,?,?,'OD Approved') ON DUPLICATE KEY UPDATE status=?, work_hours=?, remarks='OD Approved'", eid, d.toString(), "OD", wh, "OD", wh); }
                 }
             }
             UIHelper.showSuccess(this, "Request " + newStatus + " successfully."); loadData();
         } catch (Exception ex) { UIHelper.showError(this, "Error: " + ex.getMessage()); }
     }
     private void deleteSelected() { Object id = tablePanel.getSelectedValue(); if (id == null) { UIHelper.showError(this, "Select a request to delete."); return; } if (!UIHelper.confirm(this, "Confirm Delete", "Permanently delete OD request #" + id + "?")) return; try { DatabaseManager.getInstance().execute("DELETE FROM od_requests WHERE id=?", id); loadData(); } catch (SQLException ex) { UIHelper.showError(this, "Error: " + ex.getMessage()); } }
 }
