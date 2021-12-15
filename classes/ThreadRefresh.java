/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package classes;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import userinterfaces.MainWindow;

/**
 *
 * @author Victor
 */
public class ThreadRefresh implements Runnable {
    private HashMap<Integer, Item> hash;
    private javax.swing.JTable jTable1;
    
    public ThreadRefresh(HashMap<Integer, Item> hash, javax.swing.JTable jTable1) {
        this.hash = hash;
        this.jTable1 = jTable1;
    }
    
    @Override
    public void run() {
        while (true) {
            SwingUtilities.invokeLater(new Runnable()  {
                public void run() {
                    try {
                        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
                        for (int row = 0; row < jTable1.getRowCount(); row++) {
                            Integer id = (Integer) model.getValueAt(row, 0);
                            Item item = hash.get(id);
                            Boolean wasGreen = (item.getMenorPreco() != 0 && item.getMenorPreco() < item.getPrecoDigitado());
                            item = MainWindow.getItemFromOldItem(item);
                            Boolean isGreen = (item.getMenorPreco() != 0 && item.getMenorPreco() < item.getPrecoDigitado());
                            if (!wasGreen && isGreen) {
                                MainWindow.sendWindowsNotification(item.getNome(), item.getNome() + " está sendo vendido por " + MainWindow.format(item.getMenorPreco()) + "!");
                            }
                            
                            String priceText = "N/A";
                            if (item.getMenorPreco() != 0)
                                priceText = MainWindow.format(item.getMenorPreco());
                            model.setValueAt(priceText, row, 3);
                            jTable1.repaint();
                        }  
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            
            try {
                Thread.sleep(30000);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
