/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package classes;

import java.io.PrintWriter;
import java.util.HashMap;

/**
 *
 * @author Victor
 */
public class ThreadShutdown extends Thread {
    private HashMap<Integer, Item> itemHash;
    
    public ThreadShutdown(HashMap<Integer, Item> itemHash) {
        this.itemHash = itemHash;
    }
    
    @Override
    public void run() {
        try {
            PrintWriter pw = new PrintWriter("itemlist.txt");
            for (Item item : itemHash.values()) {
                pw.write("" + item.getID() + ";" + item.getPrecoDigitado() + "\n");
            }
            pw.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
