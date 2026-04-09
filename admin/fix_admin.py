import os
import re

file_path = r"d:\Code\Java\test\admin\src\main\java\com\wifichat\admin\ui\AdminFrame.java"
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix layout split
content = content.replace("""        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(380);
        split.setResizeWeight(0.32);""", """        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(380);
        split.setResizeWeight(0.32);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setDividerSize(2);""")

# Fix sidebar
content = content.replace("""    private JPanel buildSidebar() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 6));
        panel.setBackground(new Color(36, 39, 45));""", """    private JPanel buildSidebar() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 6));
        panel.setBackground(Color.decode("#2B2D31"));""")

content = content.replace("""        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);

        JLabel adminLabel = new JLabel(session.displayName + " (ADMIN)");
        adminLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        adminLabel.setForeground(new Color(232, 238, 248));""", """        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);

        JLabel adminLabel = new JLabel(session.displayName + " (ADMIN)");
        adminLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        adminLabel.setForeground(Color.WHITE);""")

# Fix main pane
content = content.replace("""    private JPanel buildMainPane() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(new EmptyBorder(10, 6, 10, 10));""", """    private JPanel buildMainPane() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(Color.decode("#313338"));""")

# Fix window bg
content = content.replace("""        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }""", """        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(Color.decode("#313338"));
    }""")

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
