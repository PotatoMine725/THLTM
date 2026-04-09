package com.wifichat.ui;

/**
 * Client-local alias for {@link com.wifichat.shared.ui.RoundedBorder}.
 * Extends the shared border so all existing client references compile unchanged.
 */
public class RoundedBorder extends com.wifichat.shared.ui.RoundedBorder {
    public RoundedBorder(java.awt.Color borderColor, int radius, int thickness) {
        super(borderColor, radius, thickness);
    }
}
