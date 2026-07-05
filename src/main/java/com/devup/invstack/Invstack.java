package com.devup.invstack;

import vista.Login;

/**
 * 
 */
public class Invstack {

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new Login().setVisible(true));
    }
}
