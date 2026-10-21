package ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * UITheme - Consistent styling helper matching the Hotel Management System theme.
 * Replicates the modern, clean layout, colors, and typography used across team modules.
 */
public class UITheme {

    // Fonts
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_INPUT = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_TABLE = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_TABLE_HEADER = new Font("Segoe UI", Font.BOLD, 13);

    // Color Palette
    public static final Color COLOR_PRIMARY = new Color(22, 101, 116);      // Teal Accent (Add button)
    public static final Color COLOR_NAVY = new Color(30, 58, 95);           // Dark Navy (Update / Header)
    public static final Color COLOR_DANGER = new Color(201, 74, 74);        // Red (Cancel / Deactivate)
    public static final Color COLOR_TEXT_MUTED = new Color(110, 115, 125);  // Subtitle Grey
    public static final Color COLOR_CARD_BG = new Color(250, 251, 253);     // Card background
    public static final Color COLOR_BORDER = new Color(220, 226, 232);      // Subtle line border

    /**
     * Creates the top title block with main title and subtitle.
     */
    public static JPanel createHeader(String titleText, String subtitleText) {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel lblTitle = new JLabel(titleText);
        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(new Color(33, 37, 41));

        JLabel lblSub = new JLabel(subtitleText);
        lblSub.setFont(FONT_SUBTITLE);
        lblSub.setForeground(COLOR_TEXT_MUTED);

        headerPanel.add(lblTitle);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(lblSub);

        return headerPanel;
    }

    /**
     * Styles a form container card.
     */
    public static Border createCardBorder(String title) {
        Border line = BorderFactory.createLineBorder(COLOR_BORDER, 1);
        Border titled = BorderFactory.createTitledBorder(line, title, 0, 0, FONT_SECTION, new Color(50, 60, 75));
        return BorderFactory.createCompoundBorder(titled, new EmptyBorder(10, 15, 15, 15));
    }

    /**
     * Styles buttons to match the application aesthetic.
     */
    public static JButton createButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 24, 34));
        return btn;
    }

    /**
     * Applies the dark navy header and clean padding to JTable.
     */
    public static void styleTable(JTable table) {
        table.setFont(FONT_TABLE);
        table.setRowHeight(28);
        table.setSelectionBackground(new Color(220, 235, 248));
        table.setSelectionForeground(Color.BLACK);
        table.setShowGrid(true);
        table.setGridColor(new Color(235, 238, 242));

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_TABLE_HEADER);
        header.setBackground(COLOR_NAVY);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(header.getWidth(), 32));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        if (table.getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        }
    }
}
