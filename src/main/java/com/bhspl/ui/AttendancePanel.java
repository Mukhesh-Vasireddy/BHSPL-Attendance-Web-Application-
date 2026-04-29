package com.bhspl.ui;
 
 import com.bhspl.db.DatabaseManager;
 import com.bhspl.util.CSVExporter;
 import com.bhspl.util.ExcelExporter;
 import com.bhspl.util.UIHelper;
 import net.miginfocom.swing.MigLayout;
 
 import javax.swing.*;
 import java.awt.*;
 import java.time.LocalDate;
 import java.time.LocalDateTime;
 import java.time.LocalTime;
 import java.util.List;
 import java.util.Map;
 
 /**
  * Premium Attendance Tracking Panel with real-time status summary.
  */
 public class AttendancePanel extends JPanel {
 
     private UIHelper.StyledTablePanel tablePanel;
     private JTextField dateField;
     private JTextField empSearchField;
     private JLabel statusLbl;
 
     private static final String[] COLUMNS = {
             "Emp ID", "Name", "Date", "In Time", "Out Time", "Work Hrs", "OT Hrs", "Status", "Remarks"
     };
 
     public AttendancePanel() {
         setBackground(UIHelper.BG_MAIN);
         setLayout(new MigLayout("ins 24, wrap, gap 0, fill", "[grow]", "[] 16 [grow] 8 []"));
         buildUI();
         loadData();
         Timer autoRefresh = new Timer(5000, e -> loadData());
         autoRefresh.start();
     }
 
     private void buildUI() {
         JPanel filterBar = new JPanel(new MigLayout("ins 0, gap 12, wrap", "[] 8 [grow] [] 8 [grow]"));
         filterBar.setOpaque(false);
         dateField = new JTextField(LocalDate.now().toString(), 10);
         empSearchField = new JTextField(14);
         empSearchField.putClientProperty("JTextField.placeholderText", "Name or ID...");
 
         JButton loadBtn = UIHelper.makeButton("Load Data", UIHelper.PRIMARY, "attendance.svg");
         JButton manualBtn = UIHelper.makeButton("Manual Punch", UIHelper.SUCCESS, "plus.svg");
         JButton todayBtn = UIHelper.makeButton("Today", new Color(0x334155), "refresh.svg");
         JButton excelBtn = UIHelper.makeButton("Export Excel", new Color(0x15803D), "leaves_card.svg");
         JButton csvBtn = UIHelper.makeButton("Export CSV", new Color(0x0d9488), "backup.svg");
 
         loadBtn.addActionListener(e -> loadData());
         empSearchField.addActionListener(e -> loadData());
         dateField.addActionListener(e -> loadData());
         manualBtn.addActionListener(e -> openManualPunch());
         todayBtn.addActionListener(e -> { dateField.setText(LocalDate.now().toString()); loadData(); });
         excelBtn.addActionListener(e -> exportToExcel());
         csvBtn.addActionListener(e -> exportToCSV());
 
         filterBar.add(new JLabel("Date:")); filterBar.add(dateField, "growx");
         filterBar.add(new JLabel("Employee:")); filterBar.add(empSearchField, "growx");
         filterBar.add(loadBtn, "growx"); filterBar.add(manualBtn, "growx"); filterBar.add(todayBtn, "growx"); filterBar.add(excelBtn, "growx"); filterBar.add(csvBtn, "growx");
         add(filterBar, "growx");
 
         addComponentListener(new java.awt.event.ComponentAdapter() {
             @Override public void componentResized(java.awt.event.ComponentEvent e) {
                 int w = getWidth();
                 if (w < 800) { filterBar.setLayout(new MigLayout("ins 0, gap 12, wrap 2", "[] 8 [grow]")); }
                 else { filterBar.setLayout(new MigLayout("ins 0, gap 12, wrap 10", "[] 8 [120!] 24 [] 8 [grow] [] [] [] [] []")); }
                 filterBar.revalidate();
             }
         });
 
         tablePanel = new UIHelper.StyledTablePanel(COLUMNS);
         tablePanel.setBorder(UIHelper.createCardBorder());
         int[] colW = { 80, 180, 110, 90, 90, 90, 80, 100, 200 };
         for (int i = 0; i < colW.length; i++) tablePanel.getTable().getColumnModel().getColumn(i).setPreferredWidth(colW[i]);
         add(tablePanel, "grow, push, wmin 0");
 
         statusLbl = UIHelper.createSummaryLabel("Calculating daily attendance metrics...");
         add(statusLbl, "growx, gaptop 8");
     }
 
     private void loadData() {
         String date = dateField.getText().trim(); String search = empSearchField.getText().trim(); if (date.isEmpty()) return;
         SwingWorker<List<Object[]>, Void> worker = new SwingWorker<>() {
             @Override protected List<Object[]> doInBackground() throws Exception {
                 String sql = "SELECT e.emp_id, e.emp_name, ? AS punch_date, DATE_FORMAT(MIN(a.in_time),'%H:%i') AS in_time, DATE_FORMAT(MAX(a.out_time),'%H:%i') AS out_time, SUM(a.work_hours) as work_hours, SUM(a.overtime) as overtime, " +
                         "COALESCE((CASE WHEN SUM(CASE WHEN a.status='Half Day' THEN 1 ELSE 0 END) > 0 THEN 'Half Day' WHEN SUM(CASE WHEN a.status='Late' THEN 1 ELSE 0 END) > 0 THEN 'Late' WHEN SUM(CASE WHEN a.status='OD' OR a.status='On Duty' THEN 1 ELSE 0 END) > 0 THEN 'On Duty' WHEN SUM(CASE WHEN a.status='Present' THEN 1 ELSE 0 END) > 0 THEN 'Present' WHEN MAX(a.status) IS NOT NULL THEN MAX(a.status) END), (SELECT UPPER(leave_type) FROM leaves l WHERE l.emp_id = e.emp_id AND l.status='Approved' AND ? BETWEEN l.from_date AND l.to_date LIMIT 1), (SELECT UPPER(holiday_name) FROM holidays h WHERE h.holiday_date = ? LIMIT 1), 'Absent') as status, GROUP_CONCAT(a.remarks SEPARATOR '; ') as remarks " +
                         "FROM employees e LEFT JOIN attendance a ON e.emp_id=a.emp_id AND a.punch_date=? WHERE e.status='Active' AND (e.emp_id LIKE ? OR e.emp_name LIKE ?) GROUP BY e.emp_id, e.emp_name ORDER BY e.emp_name ASC";
                 String like = "%" + search + "%";
                 List<Map<String, Object>> data = DatabaseManager.getInstance().fetchAll(sql, date, date, date, date, like, like);
                 List<Object[]> rows = new java.util.ArrayList<>();
                 for (Map<String, Object> r : data) { rows.add(new Object[] { r.get("emp_id"), r.get("emp_name"), r.get("punch_date"), r.get("in_time"), r.get("out_time"), com.bhspl.util.AttendanceCalculator.formatDuration(DatabaseManager.dbl(r, "work_hours")), com.bhspl.util.AttendanceCalculator.formatDuration(DatabaseManager.dbl(r, "overtime")), r.get("status"), r.get("remarks") }); }
                 return rows;
             }
             @Override protected void done() {
                 try {
                     List<Object[]> newRows = get();
                     if (isDifferent(newRows)) {
                         tablePanel.clearRows(); int p=0, a=0, l=0, hd=0, od=0;
                         for (Object[] row : newRows) {
                             tablePanel.addRow(row); String s = String.valueOf(row[7]);
                             if ("Present".equalsIgnoreCase(s)) p++; else if ("Absent".equalsIgnoreCase(s)) a++; else if ("Late".equalsIgnoreCase(s)) l++; else if ("Half Day".equalsIgnoreCase(s)) hd++; else if ("On Duty".equalsIgnoreCase(s)) od++;
                         }
                         statusLbl.setText("Total Records: " + newRows.size() + " | Present: " + p + " | Absent: " + a + " | Late: " + l + " | HD: " + hd + " | OD: " + od);
                     }
                 } catch (Exception ignored) {}
             }
         };
         worker.execute();
     }
     private boolean isDifferent(List<Object[]> nr) { if (nr.size() != tablePanel.getModel().getRowCount()) return true; for (int i = 0; i < nr.size(); i++) { for (int j = 0; j < 8; j++) { Object v1 = nr.get(i)[j], v2 = tablePanel.getModel().getValueAt(i, j); if (v1 == null && v2 == null) continue; if (v1 == null || v2 == null || !v1.toString().equals(v2.toString())) return true; } } return false; }
     private void openManualPunch() { JDialog dlg = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Manual Punch Entry", true); dlg.setSize(400, 360); UIHelper.centerWindow(dlg, 400, 360); JPanel p = new JPanel(new MigLayout("ins 24, wrap, gap 12", "[][grow]", "[]")); p.setBackground(Color.WHITE); JTextField empId = new JTextField(12), date = new JTextField(LocalDate.now().toString(), 12), inTime = new JTextField("09:00", 12), outTime = new JTextField("18:00", 12); JComboBox<String> status = new JComboBox<>(new String[] { "Present", "Absent", "Half Day", "Late" }); p.add(new JLabel("Emp ID")); p.add(empId, "growx"); p.add(new JLabel("Date")); p.add(date, "growx"); p.add(new JLabel("In Time")); p.add(inTime, "growx"); p.add(new JLabel("Out Time")); p.add(outTime, "growx"); p.add(new JLabel("Status")); p.add(status, "growx"); JButton save = UIHelper.makeButton("Save Entry", UIHelper.SUCCESS, "check.svg"); save.addActionListener(e -> { try { DatabaseManager db = DatabaseManager.getInstance(); String eid = empId.getText().trim(), ds = date.getText().trim(); Map<String, Object> emp = db.fetchOne("SELECT shift FROM employees WHERE emp_id=?", eid); Map<String, Object> sInfo = db.fetchOne("SELECT * FROM shifts WHERE shift_name=?", (emp != null ? DatabaseManager.str(emp, "shift") : "General")); LocalDateTime inDt = LocalDateTime.of(LocalDate.parse(ds), LocalTime.parse(inTime.getText().trim())); LocalDateTime outDt = LocalDateTime.of(LocalDate.parse(ds), LocalTime.parse(outTime.getText().trim())); com.bhspl.util.AttendanceCalculator.Metrics m = com.bhspl.util.AttendanceCalculator.calculate(inDt, outDt, sInfo); db.execute("INSERT INTO attendance (emp_id, punch_date, in_time, out_time, work_hours, overtime, status, late_mins, punch_type) VALUES (?,?,?,?,?,?,?,?,'Manual') ON DUPLICATE KEY UPDATE in_time=VALUES(in_time), out_time=VALUES(out_time), work_hours=VALUES(work_hours), overtime=VALUES(overtime), status=VALUES(status), late_mins=VALUES(late_mins)", eid, ds, inDt, outDt, m.workHours, m.overtime, status.getSelectedItem().toString(), m.lateMins); UIHelper.showSuccess(dlg, "Punch saved successfully."); dlg.dispose(); loadData(); } catch (Exception ex) { UIHelper.showError(dlg, "Error: " + ex.getMessage()); } }); p.add(save, "skip, growx, gaptop 12"); dlg.setContentPane(p); dlg.setVisible(true); }
     private void exportToExcel() { if (tablePanel.getTable().getRowCount() == 0) { UIHelper.showInfo(this, "No data to export."); return; } JFileChooser fc = new JFileChooser(); fc.setSelectedFile(new java.io.File("Attendance_Report_" + dateField.getText() + ".xlsx")); if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) { try { ExcelExporter.exportTable(tablePanel.getTable(), fc.getSelectedFile().getAbsolutePath()); UIHelper.showSuccess(this, "Export successful!"); } catch (Exception ex) { UIHelper.showError(this, "Export Failed: " + ex.getMessage()); } } }
     private void exportToCSV() { if (tablePanel.getTable().getRowCount() == 0) { UIHelper.showInfo(this, "No data to export."); return; } JFileChooser fc = new JFileChooser(); fc.setSelectedFile(new java.io.File("Attendance_Report_" + dateField.getText() + ".csv")); if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) { try { CSVExporter.exportTable(tablePanel.getTable(), fc.getSelectedFile().getAbsolutePath()); UIHelper.showSuccess(this, "Export successful!"); } catch (Exception ex) { UIHelper.showError(this, "Export Failed: " + ex.getMessage()); } } }
 }
