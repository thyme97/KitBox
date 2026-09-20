package com.kitbox;

import com.kitbox.ui.MainWindow;
import com.kitbox.ui.UiTheme;

import javax.swing.SwingUtilities;

/**
 * KitBox 工具箱入口。
 */
public final class KitBoxApp {

    private KitBoxApp() {
    }

    public static void main(String[] args) {
        AppContext.init();
        UiTheme.apply(AppContext.config);
        SwingUtilities.invokeLater(() -> new MainWindow().display());
    }
}
