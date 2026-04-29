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
  * Premium Shift Management Panel with standardized footer.
  */
 public class ShiftPanel extends JPanel {
 
     private UIHelper.StyledTablePanel tablePanel;
     private JLabel statusLbl;
 
     private static final String[] COLUMNS = { "ID", "Shift Name", "Start", "End", "Break (mins)", "Grace (mins)", "Off Day 1", "Off Day 2", "Work Hrs" };
 
     public ShiftPanel() {
         setBackground(UIHelper.BG_MAIN);
         setLayout(new MigLayout("ins 24, wrap, gap 0, fill", "[grow]", "[] 16 [grow] 8 []"));
         buildUI();
         loadData();
     }
 
     private void buildUI() {
         JPanel toolbar = new JPanel(new MigLayout("ins 0, gap 12", "[] push [] 8 [] 8 [] 8 []"));
         toolbar.setOpaque(false);
         JLabel title = new JLabel("Shift Management"); title.setFont(UIHelper.FNT_TITLE); title.setForeground(UIHelper.PRIMARY);
         JButton addBtn = UIHelper.makeButton("Add Shift", UIHelper.SUCCESS, "plus.svg");
         JButton editBtn = UIHelper.makeButton("Edit", UIHelper.PRIMARY, "edit.svg");
         JButton deleteBtn = UIHelper.makeButton("Delete", UIHelper.DANGER, "trash.svg");
         JButton refreshBtn = UIHelper.makeButton("Refresh", new Color(0x334155), "sync.svg");
         addBtn.addActionListener(e -> openForm(-1));
         editBtn.addActionListener(e -> { Object id = tablePanel.getSelectedValue(); if (id == null) { UIHelper.showInfo(this, "Select a shift first."); return; } openForm((int) id); });
         deleteBtn.addActionListener(e -> deleteSelected()); refreshBtn.addActionListener(e -> loadData());
         toolbar.add(title); toolbar.add(addBtn, "right"); toolbar.add(editBtn); toolbar.add(deleteBtn); toolbar.add(refreshBtn);
         add(toolbar, "growx");
 
         tablePanel = new UIHelper.StyledTablePanel(COLUMNS);
         tablePanel.setBorder(UIHelper.createCardBorder());
         tablePanel.getTable().getColumnModel().getColumn(0).setMaxWidth(60);
         add(tablePanel, "grow, push, wmin 0");
 
         statusLbl = UIHelper.createSummaryLabel("Analyzing shift rotations...");
         add(statusLbl, "growx, gaptop 8");
     }
 
     private void loadData() {
         tablePanel.clearRows();
         SwingWorker<List<Map<String, Object>>, Void> w = new SwingWorker<>() {
             @Override protected List<Map<String, Object>> doInBackground() throws Exception { return DatabaseManager.getInstance().fetchAll("SELECT id, shift_name, start_time, end_time, break_mins, grace_mins, weekly_off1, weekly_off2, work_hours FROM shifts ORDER BY shift_name"); }
             @Override protected void done() {
                 try {
                     List<Map<String, Object>> res = get();
                     for (Map<String, Object> r : res) tablePanel.addRow(new Object[] { r.get("id"), r.get("shift_name"), r.get("start_time"), r.get("end_time"), r.get("break_mins"), r.get("grace_mins"), r.get("weekly_off1"), r.get("weekly_off2"), r.get("work_hours") });
                     statusLbl.setText("Total Configured Shifts: " + res.size() + " | Active Rotations: " + res.size());
                 } catch (Exception ignored) {}
             }
         };
         w.execute();
     }
     private void openForm(int id) { JDialog dlg = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), id < 0 ? "Create New Shift" : "Modify Shift Settings", true); dlg.setSize(440, 420); UIHelper.centerWindow(dlg, 440, 420); JPanel p = new JPanel(new MigLayout("ins 24, wrap, gap 12", "[][grow]", "[]")); p.setBackground(Color.WHITE); JTextField name = new JTextField(14), start = new JTextField("09:00", 14), end = new JTextField("18:00", 14), breakM = new JTextField("30", 14), grace = new JTextField("5", 14); String[] days = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "None" }; JComboBox<String> off1 = new JComboBox<>(days), off2 = new JComboBox<>(days); off2.setSelectedItem("None"); if (id >= 0) { try { Map<String, Object> r = DatabaseManager.getInstance().fetchOne("SELECT * FROM shifts WHERE id=?", id); if (r != null) { name.setText((String) r.get("shift_name")); start.setText(r.get("start_time").toString()); end.setText(r.get("end_time").toString()); breakM.setText(String.valueOf(r.get("break_mins"))); grace.setText(String.valueOf(r.get("grace_mins"))); off1.setSelectedItem(r.get("weekly_off1")); off2.setSelectedItem(r.get("weekly_off2")); } } catch (Exception ignored) {} } p.add(new JLabel("Shift Title")); p.add(name, "growx"); p.add(new JLabel("Start Time")); p.add(start, "growx"); p.add(new JLabel("End Time")); p.add(end, "growx"); p.add(new JLabel("Break (min)")); p.add(breakM, "growx"); p.add(new JLabel("Grace Period")); p.add(grace, "growx"); p.add(new JLabel("Weekly Off 1")); p.add(off1, "growx"); p.add(new JLabel("Weekly Off 2")); p.add(off2, "growx"); JButton save = UIHelper.makeButton("Apply Changes", UIHelper.SUCCESS); save.addActionListener(e -> { try { int bm = Integer.parseInt(breakM.getText().trim()), gm = Integer.parseInt(grace.getText().trim()); String n = name.getText().trim(); if (id < 0) DatabaseManager.getInstance().execute("INSERT INTO shifts (shift_name, start_time, end_time, break_mins, grace_mins, weekly_off1, weekly_off2) VALUES(?,?,?,?,?,?,?)", n, start.getText().trim(), end.getText().trim(), bm, gm, off1.getSelectedItem(), off2.getSelectedItem()); else DatabaseManager.getInstance().execute("UPDATE shifts SET shift_name=?, start_time=?, end_time=?, break_mins=?, grace_mins=?, weekly_off1=?, weekly_off2=? WHERE id=?", n, start.getText().trim(), end.getText().trim(), bm, gm, off1.getSelectedItem(), off2.getSelectedItem(), id); dlg.dispose(); loadData(); } catch (Exception ex) { UIHelper.showError(dlg, "Validation Error: " + ex.getMessage()); } }); p.add(save, "skip, growx, gaptop 12"); dlg.setContentPane(p); dlg.setVisible(true); }
     private void deleteSelected() { Object idVal = tablePanel.getSelectedValue(); if (idVal == null) { UIHelper.showInfo(this, "Select a shift record first."); return; } int id = (int) idVal; if (!UIHelper.confirm(this, "Confirm Action", "Delete this shift schedule permanently?")) return; try { DatabaseManager.getInstance().execute("DELETE FROM shifts WHERE id=?", id); loadData(); } catch (SQLException ex) { UIHelper.showError(this, "Operation Failed: " + ex.getMessage()); } }
 }
