package com.alkacode.vips.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Grade ASCII de posicoes de uma GUI (letras = slots fixos com papel proprio, '0' = slot
 * de lista dinamica repetida, '_'/espaco = nunca preenchido - o fill() de fundo cuida do
 * resto) - ver gui-layouts.yml. Mesmo mecanismo de posicionamento do AlkaFish.
 */
public record GuiLayout(int rows, String[] layout) {
    public List<Integer> findSlots(char c) {
        List<Integer> slots = new ArrayList<>();
        for (int row = 0; row < layout.length; row++) {
            String line = layout[row];
            for (int col = 0; col < line.length() && col < 9; col++) {
                if (line.charAt(col) == c) {
                    slots.add(row * 9 + col);
                }
            }
        }
        return slots;
    }

    public int firstSlot(char c) {
        List<Integer> slots = findSlots(c);
        return slots.isEmpty() ? -1 : slots.get(0);
    }
}
