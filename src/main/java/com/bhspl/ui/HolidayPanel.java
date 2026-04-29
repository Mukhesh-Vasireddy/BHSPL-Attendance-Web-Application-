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
 import java.time.format.TextStyle;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 
 /**
  * Enhanced Holiday Master panel with standardized footer.
  */
 public class HolidayPanel extends JPanel {
 
     private UIHelper.StyledTablePanel tablePanel;
     private String currentFilter = null;
     private JLabel statusLbl;
 
     private static final String[] COLUMNS = { "ID", "Date", "Day", "Holiday Name", "Type" };
     private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
 
     private static final Color CLR_NATIONAL = new Color(0x991B1B);
     private static final Color CLR_PUBLIC = new Color(0x1D4ED8);
     private static final Color CLR_FESTIVAL = new Color(0xEA580C);
     private static final Color CLR_OPTIONAL = new Color(0xCA8A04);
     private static final Color CLR_COMPANY = new Color(0x059669);
     private static final Color CLR_REGIONAL = new Color(0x4F46E5);
 
     public HolidayPanel() {
         setBackground(UIHelper.BG_MAIN);
         setLayout(new MigLayout("ins 24, wrap, gap 0, fill", "[grow]", "[] 16 [grow] 8 []"));
         buildUI();
         loadData();
     }
 
     private void buildUI() {
         JPanel toolbar = new JPanel(new MigLayout("ins 0, wrap, gap 0", "[grow]", "[] 12 []"));
         toolbar.setOpaque(false);
         JPanel topRow = new JPanel(new MigLayout("ins 0, gap 12", "push [] 8 [] 8 [] 8 [] 8 []"));
         topRow.setOpaque(false);
         JButton addBtn = UIHelper.makeButton("Add Holiday", UIHelper.SUCCESS, "plus.svg");
         JButton editBtn = UIHelper.makeButton("Edit", UIHelper.PRIMARY, "edit.svg");
         JButton deleteBtn = UIHelper.makeButton("Delete", UIHelper.DANGER, "trash.svg");
         JButton loadDefBtn = UIHelper.makeButton("Load Defaults", new Color(0xF59E0B), "backup.svg");
         JButton refreshBtn = UIHelper.makeButton("Refresh", new Color(0x4B5563), "sync.svg");
         addBtn.addActionListener(e -> openForm(-1));
         editBtn.addActionListener(e -> { Object idVal = tablePanel.getSelectedValue(); if (idVal == null) { UIHelper.showInfo(this, "Select a holiday record to edit."); return; } openForm((int) idVal); });
         deleteBtn.addActionListener(e -> deleteSelected()); loadDefBtn.addActionListener(e -> loadDefaults()); refreshBtn.addActionListener(e -> loadData());
         topRow.add(addBtn); topRow.add(editBtn); topRow.add(deleteBtn); topRow.add(loadDefBtn); topRow.add(refreshBtn);
         toolbar.add(topRow, "growx");
 
         JPanel filterRow = new JPanel(new MigLayout("ins 0, gap 8", "[] 8 [] 8 [] 8 [] 8 [] 8 [] 8 []"));
         filterRow.setOpaque(false);
         filterRow.add(createFilterBtn("All Holidays", new Color(0x64748b)));
         filterRow.add(createFilterBtn("National Holiday", CLR_NATIONAL));
         filterRow.add(createFilterBtn("Public Holiday", CLR_PUBLIC));
         filterRow.add(createFilterBtn("Festival Holiday", CLR_FESTIVAL));
         filterRow.add(createFilterBtn("Optional Holiday", CLR_OPTIONAL));
         filterRow.add(createFilterBtn("Company Holiday", CLR_COMPANY));
         filterRow.add(createFilterBtn("Regional Holiday", CLR_REGIONAL));
         toolbar.add(filterRow, "growx");
         add(toolbar, "growx");
 
         tablePanel = new UIHelper.StyledTablePanel(COLUMNS);
         tablePanel.setBorder(UIHelper.createCardBorder());
         tablePanel.getTable().getColumnModel().getColumn(0).setMaxWidth(60);
         styleHolidayTable();
         add(tablePanel, "grow, push, wmin 0");
 
         statusLbl = UIHelper.createSummaryLabel("Analyzing holiday schedule...");
         add(statusLbl, "growx, gaptop 8");
     }
 
     private JButton createFilterBtn(String text, Color bg) {
         JButton btn = UIHelper.makeButton(text, bg); btn.setFont(new Font("Segoe UI", Font.BOLD, 10)); btn.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
         btn.addActionListener(e -> { currentFilter = (text.equals("All Holidays") || text.equals(currentFilter)) ? null : text; loadData(); }); return btn;
     }
 
     private void styleHolidayTable() {
         JTable table = tablePanel.getTable();
         table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
             @Override public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                 Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col); String type = t.getValueAt(row, 4).toString();
                 if (!sel) {
                     if (type.contains("National")) c.setBackground(new Color(0xFFF1F2));
                     else if (type.contains("Festival")) c.setBackground(new Color(0xFFFBEB));
                     else if (type.contains("Public")) c.setBackground(new Color(0xEFF6FF));
                     else c.setBackground(row % 2 == 0 ? UIHelper.TREE_ODD : Color.WHITE);
                 }
                 setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10)); setHorizontalAlignment(col == 3 ? SwingConstants.LEFT : SwingConstants.CENTER); return c;
             }
         });
     }
 
     private void loadData() {
         tablePanel.clearRows();
         SwingWorker<List<Map<String, Object>>, Void> w = new SwingWorker<>() {
             @Override protected List<Map<String, Object>> doInBackground() throws Exception {
                 String sql = "SELECT id, holiday_date, holiday_name, holiday_type FROM holidays ";
                 if (currentFilter != null) sql += " WHERE holiday_type = '" + currentFilter + "'";
                 sql += " ORDER BY holiday_date";
                 return DatabaseManager.getInstance().fetchAll(sql);
             }
             @Override protected void done() {
                 try {
                     List<Map<String, Object>> res = get(); int nat = 0, pub = 0, fest = 0;
                     for (Map<String, Object> r : res) {
                         LocalDate d = LocalDate.parse(r.get("holiday_date").toString());
                         tablePanel.addRow(new Object[] { r.get("id"), d.format(DISPLAY_FMT), d.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH), r.get("holiday_name"), r.get("holiday_type") });
                         String type = String.valueOf(r.get("holiday_type"));
                         if (type.contains("National")) nat++; else if (type.contains("Public")) pub++; else if (type.contains("Festival")) fest++;
                     }
                     statusLbl.setText("Total Holidays: " + res.size() + " | National: " + nat + " | Public: " + pub + " | Festival: " + fest);
                 } catch (Exception ignored) {}
             }
         };
         w.execute();
     }
     private void openForm(int id) { new HolidayForm((JFrame) SwingUtilities.getWindowAncestor(this), id < 0 ? null : id, this::loadData); }
     private void deleteSelected() { Object idVal = tablePanel.getSelectedValue(); if (idVal == null) { UIHelper.showInfo(this, "Select a holiday record first."); return; } int id = (int) idVal; if (!UIHelper.confirm(this, "Confirm Delete", "Permanently delete this holiday record?")) return; try { DatabaseManager.getInstance().execute("DELETE FROM holidays WHERE id=?", id); loadData(); } catch (SQLException ex) { UIHelper.showError(this, "Error: " + ex.getMessage()); } }
     private void loadDefaults() {
         if (!UIHelper.confirm(this, "Load Defaults", "Load common 2026 holidays into the master list?")) return;
         Object[][] defaults = { {"2026-01-01", "New Year's Day", "National Holiday"}, {"2026-01-26", "Republic Day", "National Holiday"}, {"2026-03-25", "Holi", "Festival Holiday"}, {"2026-04-14", "Dr. Ambedkar Jayanti", "National Holiday"}, {"2026-04-18", "Good Friday", "Festival Holiday"}, {"2026-05-01", "Labour Day", "National Holiday"}, {"2026-08-15", "Independence Day", "National Holiday"}, {"2026-08-27", "Janmashtami", "Festival Holiday"}, {"2026-10-02", "Gandhi Jayanti", "National Holiday"}, {"2026-10-24", "Dussehra", "Festival Holiday"}, {"2026-11-01", "Diwali", "Festival Holiday"}, {"2026-11-05", "Bhai Dooj", "Festival Holiday"}, {"2026-12-25", "Christmas Day", "Public Holiday"} };
         try { for (Object[] h : defaults) DatabaseManager.getInstance().execute("INSERT IGNORE INTO holidays (holiday_date, holiday_name, holiday_type) VALUES (?,?,?)", h[0], h[1], h[2]); loadData(); } catch (SQLException ex) { UIHelper.showError(this, "Error loading defaults: " + ex.getMessage()); }
     }
 }
