package com.kitbox.ui.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/**
 * 可换行的 FlowLayout：当容器宽度不足时组件折到下一行，
 * 且容器首选高度按折行后的实际行数计算（原生 FlowLayout 的首选高度只算一行，
 * 放在 BorderLayout.NORTH 里会导致折行后的第二行被裁剪不可见）。
 */
public class WrapLayout extends FlowLayout {

    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= getHgap() + 1;
        return minimum;
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            // 容器尚未布局时按单行计算；已布局时按当前宽度折行
            int targetWidth = target.getSize().width;
            if (targetWidth == 0) {
                targetWidth = Integer.MAX_VALUE;
            }
            Insets insets = target.getInsets();
            int maxWidth = targetWidth - (insets.left + insets.right + getHgap() * 2);

            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;
            int count = target.getComponentCount();
            for (int i = 0; i < count; i++) {
                Component c = target.getComponent(i);
                if (!c.isVisible()) {
                    continue;
                }
                Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                if (rowWidth > 0 && rowWidth + getHgap() + d.width > maxWidth) {
                    // 折行
                    dim.width = Math.max(dim.width, rowWidth);
                    dim.height += rowHeight + getVgap();
                    rowWidth = 0;
                    rowHeight = 0;
                }
                rowWidth += (rowWidth > 0 ? getHgap() : 0) + d.width;
                rowHeight = Math.max(rowHeight, d.height);
            }
            dim.width = Math.max(dim.width, rowWidth);
            dim.height += rowHeight;
            dim.width += insets.left + insets.right + getHgap() * 2;
            dim.height += insets.top + insets.bottom + getVgap() * 2;
            return dim;
        }
    }
}
