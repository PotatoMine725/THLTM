import os

file_path = r"d:\Code\Java\test\client\src\main\java\com\wifichat\ui\MainFrame.java"
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix composer border
content = content.replace("""        composer.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppTheme.BORDER_SUBTLE, 12, 1),
                new EmptyBorder(10, 10, 10, 10)
        ));""", """        composer.setBorder(new EmptyBorder(8, 10, 8, 10));""")

# Fix composerScroll
content = content.replace("""        JScrollPane composerScroll = new JScrollPane(composer);
        composerScroll.setBorder(BorderFactory.createEmptyBorder());
        composerScroll.getVerticalScrollBar().setUnitIncrement(14);""", """        JScrollPane composerScroll = new JScrollPane(composer);
        composerScroll.setBorder(BorderFactory.createEmptyBorder());
        composerScroll.getVerticalScrollBar().setUnitIncrement(14);
        composerScroll.putClientProperty("JComponent.roundRect", true);
        composerScroll.setBackground(AppTheme.WINDOW_BG);
        composerScroll.getViewport().setBackground(AppTheme.WINDOW_BG);""")

# Remove FocusAdapter
content = content.replace("""        composer.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                composer.setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(AppTheme.INPUT_FOCUS_BORDER, 12, 1),
                        new EmptyBorder(10, 10, 10, 10)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                composer.setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(AppTheme.BORDER_SUBTLE, 12, 1),
                        new EmptyBorder(10, 10, 10, 10)
                ));
            }
        });""", "")

# Remove unused createCardPanel
content = content.replace("""    private JPanel createCardPanel(int radius) {
        JPanel panel = new JPanel();
        panel.setBackground(AppTheme.CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppTheme.BORDER_SUBTLE, radius, 1),
                new EmptyBorder(10, 10, 10, 10)
        ));
        return panel;
    }""", "")

# Use \r\n for Windows
content = content.replace('\r\n', '\n').replace('\n', '\r\n')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
