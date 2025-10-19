package com.eduactivity;

import com.eduactivity.ui.LoginFrame;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        // Start only Swing GUI as requested
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new LoginFrame().setVisible(true);
        });
    }
}
