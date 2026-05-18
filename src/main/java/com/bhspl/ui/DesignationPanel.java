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
  * Premium Designation Master panel with inline edit actions.
  */
 public class DesignationPanel extends JPanel {
     private UIHelper.StyledTablePanel tablePanel;
     private JTextField searchField;
     private static final String[] COLUMNS = {"ID", "Designation Name", "Level", "Description", "Status", "Actions"};
 
     public DesignationPanel() {
         setLayout(new MigLayout("ins 24, fill, wrap", "[grow]", "[] 20 [] 15 [grow]"));
         setBackground(UIHelper.BG_MAIN);
         buildUI();
         loadData();
     }
 
     private void buildUI() {
         // Header
         JPanel header = new JPanel(new MigLayout("ins 0", "[] 10 [] push"));
         header.setOpaque(false);
         
         JLabel iconLbl = new JLabel(new com.formdev.flatlaf.extras.FlatSVGIcon("icons/reports.svg", 22, 22));
         header.add(iconLbl);
         
         JLabel title = new JLabel("Designation Master");
         title.setFont(new Font("Segoe UI", Font.BOLD, 22));
         title.setForeground(new Color(0x1E293B));
         header.add(title);
         add(header, "growx");
 
         // Toolbar
         JPanel toolbar = new JPanel(new MigLayout("ins 0, gap 12", "[] 8 [] push [] 8 []"));
         toolbar.setOpaque(false);
         
         JButton addBtn     = UIHelper.makeButton("Add New Designation", UIHelper.SUCCESS, "plus.svg");
         JButton deleteBtn  = UIHelper.makeButton("Delete Selected", UIHelper.DANGER, "trash.svg");
         JButton refreshBtn = UIHelper.makeButton("Refresh", new Color(0x334155), "sync.svg");
 
         searchField = new JTextField(15);
         searchField.putClientProperty("JTextField.placeholderText", "Search designations...");
         JButton searchBtn = UIHelper.makeButton("Search", UIHelper.PRIMARY, "search.svg");
 
         addBtn.addActionListener(e -> openForm(null));
         deleteBtn.addActionListener(e -> deleteSelected());
         refreshBtn.addActionListener(e -> loadData());
         searchBtn.addActionListener(e -> loadData());
 
         toolbar.add(addBtn);
         toolbar.add(deleteBtn);
         toolbar.add(refreshBtn);
         toolbar.add(new JLabel("Search:"), "gapleft 20");
         toolbar.add(searchField, "w 200!");
         toolbar.add(searchBtn);
         
         add(toolbar, "growx");
 
         tablePanel = new UIHelper.StyledTablePanel(COLUMNS);
         styleTable();
         add(tablePanel, "grow, push, wmin 0");
     }
 
     private void styleTable() {
         JTable table = tablePanel.getTable();
         table.getColumnModel().getColumn(0).setMaxWidth(50);
         table.getColumnModel().getColumn(2).setMaxWidth(80); // Level
         table.getColumnModel().getColumn(4).setMaxWidth(120); // Status
         table.getColumnModel().getColumn(5).setMaxWidth(80);  // Actions
 
         // Status Column Renderer (Green for Active)
         table.getColumnModel().getColumn(4).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
             @Override
             public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                 super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                 if ("Active".equalsIgnoreCase(String.valueOf(value))) {
                     setForeground(new Color(0x15803D));
                     setFont(getFont().deriveFont(Font.BOLD));
                 } else {
                     setForeground(new Color(0xDC2626));
                 }
                 setHorizontalAlignment(JLabel.CENTER);
                 return this;
             }
         });
 
         // Actions Column Renderer (Only Pencil Icon)
         table.getColumnModel().getColumn(5).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
             private final com.formdev.flatlaf.extras.FlatSVGIcon editIcon = new com.formdev.flatlaf.extras.FlatSVGIcon("icons/edit.svg", 16, 16);
             {
                 editIcon.setColorFilter(new com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter(c -> UIHelper.PRIMARY));
                 setHorizontalAlignment(JLabel.CENTER);
             }
             @Override
             public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                 super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
                 setIcon(editIcon);
                 return this;
             }
         });
 
         // Mouse click listener for inline edit
         table.addMouseListener(new java.awt.event.MouseAdapter() {
             @Override
             public void mouseClicked(java.awt.event.MouseEvent e) {
                 int row = table.rowAtPoint(e.getPoint());
                 int col = table.columnAtPoint(e.getPoint());
                 if (row >= 0 && col == 5) {
                     Integer id = (Integer) table.getValueAt(row, 0);
                     openForm(id);
                 }
             }
         });
         
         table.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
             @Override
             public void mouseMoved(java.awt.event.MouseEvent e) {
                 int col = table.columnAtPoint(e.getPoint());
                 if (col == 5) table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                 else table.setCursor(Cursor.getDefaultCursor());
             }
         });
     }
 
     private void loadData() {
         tablePanel.clearRows();
         String filter = searchField.getText().trim();
         try {
             StringBuilder sql = new StringBuilder("SELECT * FROM designations");
             if (!filter.isEmpty()) {
                 sql.append(" WHERE desig_name LIKE '%").append(filter).append("%'");
                 sql.append(" ORDER BY CASE WHEN desig_name LIKE '").append(filter).append("%' THEN 0 ELSE 1 END, level_order ASC, desig_name ASC");
             } else {
                 sql.append(" ORDER BY level_order ASC, desig_name ASC");
             }
             
             List<Map<String, Object>> rows = DatabaseManager.getInstance().query(sql.toString());
             for (Map<String, Object> r : rows) {
                 tablePanel.addRow(new Object[]{
                     r.get("id"),
                     r.get("desig_name"),
                     r.get("level_order"),
                     r.get("description"),
                     r.get("status"),
                     "Edit"
                 });
             }
         } catch (SQLException e) {
             e.printStackTrace();
         }
     }
 
     private void openForm(Integer id) {
         new DesigForm((JFrame) SwingUtilities.getWindowAncestor(this), id, this::loadData);
     }
 
     private void deleteSelected() {
         deleteSelected(null);
     }
 
     private void deleteSelected(Object targetId) {
         Object id = (targetId != null) ? targetId : tablePanel.getSelectedValue();
         if (id == null) {
             UIHelper.showError(this, "Select a designation to delete.");
             return;
         }
 
         if (!UIHelper.confirm(this, "Confirm Delete", "Permanently delete this designation?"))
             return;
 
         try {
             DatabaseManager.getInstance().execute("DELETE FROM designations WHERE id=?", id);
             loadData();
         } catch (SQLException ex) {
             UIHelper.showError(this, "Cannot delete designation. It might be assigned to employees.");
         }
     }
 }
