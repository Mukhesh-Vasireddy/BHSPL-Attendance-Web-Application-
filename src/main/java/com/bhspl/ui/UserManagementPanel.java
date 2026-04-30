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
  * Full-featured System User Management Panel.
  * Handles creation, password resets, and status toggling of administrative users.
  */
 public class UserManagementPanel extends JPanel {
     private UIHelper.StyledTablePanel tablePanel;
     private static final String[] COLUMNS = {"ID", "Username", "Role", "Emp ID", "Status", "Last Login"};
 
     public UserManagementPanel() {
         setLayout(new MigLayout("ins 24, fill, wrap", "[grow]", "[] 20 [grow]"));
         setBackground(UIHelper.BG_MAIN);
         buildUI();
         loadData();
     }
 
     private void buildUI() {
         // Header Section
         JPanel header = new JPanel(new MigLayout("ins 0, gap 15", "[] push [] 8 [] 8 []"));
         header.setOpaque(false);
 
         JLabel title = new JLabel("System User Management");
         title.setFont(new Font("Segoe UI", Font.BOLD, 22));
         title.setForeground(UIHelper.PRIMARY);
         header.add(title);
 
         // Action Buttons
         JButton addBtn = UIHelper.makeButton("Add User", UIHelper.SUCCESS, "plus.svg");
         JButton resetBtn = UIHelper.makeButton("Reset Password", UIHelper.PRIMARY, "edit.svg");
         JButton toggleBtn = UIHelper.makeButton("Toggle Status", new Color(0x334155), "eye.svg");
 
         addBtn.addActionListener(e -> openAddUser());
         resetBtn.addActionListener(e -> resetPassword());
         toggleBtn.addActionListener(e -> toggleUserStatus());
 
         header.add(addBtn);
         header.add(resetBtn);
         header.add(toggleBtn);
         add(header, "growx, gapbottom 10");
 
         // Table Section
         tablePanel = new UIHelper.StyledTablePanel(COLUMNS);
         tablePanel.getTable().getColumnModel().getColumn(0).setMaxWidth(60);
         add(tablePanel, "grow, push");
     }
 
     public void loadData() {
         tablePanel.clearRows();
         SwingWorker<List<Map<String, Object>>, Void> worker = new SwingWorker<>() {
             @Override
             protected List<Map<String, Object>> doInBackground() throws Exception {
                 return DatabaseManager.getInstance().fetchAll(
                     "SELECT id, username, role, emp_id, status, last_login FROM users ORDER BY username");
             }
 
             @Override
             protected void done() {
                 try {
                     for (Map<String, Object> r : get()) {
                         tablePanel.addRow(new Object[]{
                             r.get("id"), r.get("username"), r.get("role"), 
                             r.get("emp_id"), r.get("status"), r.get("last_login")
                         });
                     }
                 } catch (Exception ignored) {}
             }
         };
         worker.execute();
     }
 
     private void openAddUser() {
         JDialog dlg = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Create System User", true);
         dlg.setSize(400, 320);
         UIHelper.centerWindow(dlg, 400, 320);
 
         JPanel p = new JPanel(new MigLayout("ins 24, wrap, gap 12", "[][grow]", "[]"));
         p.setBackground(Color.WHITE);
 
         JTextField uname = new JTextField(14);
         JPasswordField pass = new JPasswordField(14);
         JComboBox<String> role = new JComboBox<>(new String[]{"Admin", "Operator", "HR"});
         JTextField empId = new JTextField(14);
 
         Object[][] rows = {{"Username", uname}, {"Password", pass}, {"System Role", role}, {"Employee ID", empId}};
         for (Object[] row : rows) {
             p.add(new JLabel((String) row[0]));
             p.add((Component) row[1], "growx");
         }
 
         JButton save = UIHelper.makeButton("Create Account", UIHelper.SUCCESS, "check.svg");
         save.addActionListener(e -> {
             try {
                 String u = uname.getText().trim();
                 if (u.isEmpty()) { UIHelper.showWarning(dlg, "Username is required."); return; }
                 
                 String ph = DatabaseManager.getInstance().hashPw(new String(pass.getPassword()));
                 DatabaseManager.getInstance().execute(
                     "INSERT INTO users (username, password_hash, role, emp_id) VALUES(?,?,?,?)",
                     u, ph, role.getSelectedItem(),
                     empId.getText().trim().isEmpty() ? null : empId.getText().trim());
                 dlg.dispose(); 
                 loadData();
             } catch (SQLException ex) { UIHelper.showError(dlg, "Error: " + ex.getMessage()); }
         });
         
         p.add(save, "skip, growx, gaptop 12");
         dlg.setContentPane(p);
         dlg.setVisible(true);
     }
 
     private void resetPassword() {
         Object idVal = tablePanel.getSelectedValue();
         if (idVal == null) { UIHelper.showInfo(this, "Please select a user first."); return; }
         
         int id = (int) idVal;
         String newPass = JOptionPane.showInputDialog(this, "Enter new password for the user:");
         if (newPass == null || newPass.trim().isEmpty()) return;
         try {
             String ph = DatabaseManager.getInstance().hashPw(newPass.trim());
             DatabaseManager.getInstance().execute("UPDATE users SET password_hash=? WHERE id=?", ph, id);
             UIHelper.showSuccess(this, "Password reset successfully.");
         } catch (SQLException ex) { UIHelper.showError(this, "Operation Failed: " + ex.getMessage()); }
     }
 
     private void toggleUserStatus() {
         Object idVal = tablePanel.getSelectedValue();
         if (idVal == null) { UIHelper.showInfo(this, "Please select a user first."); return; }
         
         int id = (int) idVal;
         int rowIdx = tablePanel.getTable().getSelectedRow();
         String current = (String) tablePanel.getTable().getValueAt(rowIdx, 4);
         String newStatus = "Active".equals(current) ? "Inactive" : "Active";
         
         try {
             DatabaseManager.getInstance().execute("UPDATE users SET status=? WHERE id=?", newStatus, id);
             loadData();
         } catch (SQLException ex) { UIHelper.showError(this, "Error: " + ex.getMessage()); }
     }
 }
