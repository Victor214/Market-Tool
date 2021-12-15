/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package userinterfaces;

import classes.Item;
import classes.ThreadRefresh;
import classes.ThreadShutdown;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 *
 * @author Victor
 */
public class MainWindow extends javax.swing.JFrame {

    private HashMap<Integer, Item> itemHash;
    private static SystemTray tray = null;
    private static TrayIcon trayIcon = null;
    private static Boolean minimizedStatus = false;
    
    private HashMap<Integer, Item> getItemsFromFile() {
        try {
            File f = new File("itemlist.txt");
            if (!f.isFile()) {
                f.createNewFile();
                return null;
            }
            
            HashMap<Integer, Item> hash = itemHash;
            Scanner reader = new Scanner(f);
            while (reader.hasNextLine()) {
                String line = reader.nextLine();
                String[] parts = line.split(";");
                
                Integer id = Integer.valueOf(parts[0]);
                Long precoDigitado = Long.valueOf(parts[1]);
                
                if (hash.get(id) != null)
                    continue;

                
                Item item = new Item();
                item.setID(id);
                item.setPrecoDigitado(precoDigitado);
                
                hash.put(item.getID(), item);
            }
            return hash;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }
    
    public static boolean isNumeric(String str) {
        if (str == null) {
            return false;
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (Character.isDigit(str.charAt(i)) == false) {
                return false;
            }
        }
        return true;
    }
    
    
    // Merely used to format prices.
    private static final NavigableMap<Long, String> suffixes = new TreeMap<> ();
    static {
      suffixes.put(1_000L, "k");
      suffixes.put(1_000_000L, "KK");
      suffixes.put(1_000_000_000L, "B");
      suffixes.put(1_000_000_000_000L, "T");
      suffixes.put(1_000_000_000_000_000L, "P");
      suffixes.put(1_000_000_000_000_000_000L, "E");
    }

    public static String format(long value) {
      //Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
      if (value == Long.MIN_VALUE) return format(Long.MIN_VALUE + 1);
      if (value < 0) return "-" + format(-value);
      if (value < 1000) return Long.toString(value); //deal with easy case

      Entry<Long, String> e = suffixes.floorEntry(value);
      Long divideBy = e.getKey();
      String suffix = e.getValue();

      long truncated = value / (divideBy / 10); //the number part of the output times 10
      boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
      return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
    }
    
    public static Item getItemFromOldItem( Item oldItem ) throws IOException {
        InputStream is = null;
        try {
            System.out.println("https://market.ragnahistory.com/market/item/" + oldItem.getID());
            is = new URL("https://market.ragnahistory.com/market/item/" + oldItem.getID()).openStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject obj = new JSONObject(jsonText);
            String itemName = "Nome desconhecido";
            if (!obj.isNull("data"))
                itemName = obj.getJSONObject("data").getString("name");
            JSONArray json;
            json = obj.getJSONArray("itens");
            Long itemPrice = Long.valueOf(0);
            String mapName = "";
            Integer x = null;
            Integer y = null;
            Integer quantidade = 0;
            String loja = "";
            if (json.length() > 0) {
                itemPrice = json.getJSONObject(0).getLong("price");
                mapName = json.getJSONObject(0).getString("map");
                x = json.getJSONObject(0).getInt("x");
                y = json.getJSONObject(0).getInt("y");
                quantidade = json.getJSONObject(0).getInt("qtd");
                loja = json.getJSONObject(0).getString("loja");
            }
            oldItem.setNome(itemName);
            oldItem.setMenorPreco(itemPrice);
            oldItem.setMap(mapName);
            oldItem.setX(x);
            oldItem.setY(y);
            oldItem.setQuantidade(quantidade);
            oldItem.setLoja(loja);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (is != null)
                is.close();
        }
        return oldItem;
    }
    
    private void addItemRows(HashMap<Integer, Item> hash) throws IOException, JSONException {
        for (Item item : hash.values()) {
            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            item = getItemFromOldItem(item);
            String priceText = "N/A";
            if (item.getMenorPreco() != 0)
                priceText = format(item.getMenorPreco());
            model.addRow(new Object[]{item.getID(), item.getNome(), format(item.getPrecoDigitado()), "" + priceText });
        }
    }
    
    public static void sendWindowsNotification(String header, String message) {
        if (minimizedStatus) {
            trayIcon.displayMessage(header, message, MessageType.INFO);
        } else {
             try {
                tray.add(trayIcon);
                trayIcon.displayMessage(header, message, MessageType.INFO);
                tray.remove(trayIcon);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }       
        }
    } 
    
    /**
     * Creates new form MainWindow
     */
    public MainWindow() throws IOException {
        initComponents();
        itemHash = new HashMap<Integer, Item>();
        HashMap<Integer, Item> hash = this.getItemsFromFile();
        if (hash != null)
            this.addItemRows(hash);
        
        jTable1.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {

                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                TableModel model = jTable1.getModel();
                Integer itemID = (Integer) model.getValueAt(row, 0);
                Item item = itemHash.get(itemID);
                if ( item.getMenorPreco() == 0 ) {
                    l.setBackground(new Color(234, 30, 8));
                } else if ( item.getMenorPreco() > item.getPrecoDigitado()) {
                    l.setBackground(new Color(252, 186, 3));
                } else {
                    l.setBackground(new Color(65, 234, 15));
                }
  
                return l;
            }
        });
        
        // Tray related
        if (SystemTray.isSupported()) {
            tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/resources/icon.png"));
            
            ActionListener exitListener=new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.out.println("Exiting....");
                    System.exit(0);
                }
            };
            
            PopupMenu popup = new PopupMenu();
            
   
            
            MenuItem defaultItem = new MenuItem("Maximizar");
            defaultItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setVisible(true);
                    setExtendedState(JFrame.NORMAL);
                }
            });
            popup.add(defaultItem);
            
            defaultItem = new MenuItem("Sair");
            defaultItem.addActionListener(exitListener);
            popup.add(defaultItem);
            
            trayIcon = new TrayIcon(image, "RagnaHistory - Market Tool", popup);
            trayIcon.setImageAutoSize(true);
            
        } else {
            System.err.println("System tray not supported!");
        }
        
        addWindowStateListener(new WindowStateListener() {
            public void windowStateChanged(WindowEvent e) {
                if(e.getNewState() == ICONIFIED){
                    try {
                        tray.add(trayIcon);
                        setVisible(false);
                        System.out.println("added to SystemTray");
                        minimizedStatus = true;
                        //trayIcon.displayMessage("Hello, World", "notification demo", MessageType.INFO);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
                
                if(e.getNewState() == 7){
                    try{
                        tray.add(trayIcon);
                        setVisible(false);
                        System.out.println("added to SystemTray");
                        minimizedStatus = true;
                    }catch(AWTException ex){
                        System.out.println("unable to add to system tray");
                    }
                }
                
                if(e.getNewState()==MAXIMIZED_BOTH){
                    tray.remove(trayIcon);
                    setVisible(true);
                    System.out.println("Tray icon removed");
                    minimizedStatus = false;
                }
                
                if(e.getNewState()==NORMAL){
                    tray.remove(trayIcon);
                    setVisible(true);
                    System.out.println("Tray icon removed");
                    minimizedStatus = false;
                }
            }
        });

        Thread thread = new Thread(new ThreadRefresh(itemHash, jTable1));
        thread.start();
        
        ThreadShutdown exitThread = new ThreadShutdown(itemHash);
        Runtime.getRuntime().addShutdownHook(exitThread);
    }

    // Function to add elements to table based on given Item bean.
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel8 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabel4 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        itemIDField = new javax.swing.JTextField();
        itemPriceField = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Rag History - Market Tool");
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/resources/logo.png")));

        jTabbedPane1.setForeground(new java.awt.Color(51, 51, 51));

        jPanel1.setForeground(new java.awt.Color(51, 51, 51));

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/logo.png"))); // NOI18N

        jLabel2.setFont(new java.awt.Font("Josefin Sans", 1, 12)); // NOI18N
        jLabel2.setText("<html>Basta preencher o ID do item desejado, o preço máximo (o programa irá procurar itens com custo igual ou menor ao preço informado), e adicionar.</html>");
        jLabel2.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel2.setVerticalTextPosition(javax.swing.SwingConstants.TOP);

        jLabel3.setFont(new java.awt.Font("Josefin Sans", 0, 12)); // NOI18N
        jLabel3.setText("<html>O Market Tool é uma ferramenta de acesso ao mercado do RagnaHistory. Permite a elaboração de listas com preços dos itens. Uma notificação é enviada assim que o item está disponível.</html>");
        jLabel3.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel3.setVerticalTextPosition(javax.swing.SwingConstants.TOP);

        jLabel8.setFont(new java.awt.Font("Josefin Sans", 1, 12)); // NOI18N
        jLabel8.setText("<html>Utilize a aba \"Mercado\" para começar.</html>");
        jLabel8.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel8.setVerticalTextPosition(javax.swing.SwingConstants.TOP);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 288, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 286, Short.MAX_VALUE)
                        .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 38, Short.MAX_VALUE)
                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(30, 30, 30))
        );

        jTabbedPane1.addTab("Inicio", jPanel1);

        jPanel2.setForeground(new java.awt.Color(51, 51, 51));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Nome do Item", "($) Desejado", "($) Venda"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jTable1.getTableHeader().setResizingAllowed(false);
        jTable1.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(jTable1);
        if (jTable1.getColumnModel().getColumnCount() > 0) {
            jTable1.getColumnModel().getColumn(0).setMinWidth(35);
            jTable1.getColumnModel().getColumn(0).setPreferredWidth(35);
            jTable1.getColumnModel().getColumn(0).setMaxWidth(35);
            jTable1.getColumnModel().getColumn(2).setMinWidth(80);
            jTable1.getColumnModel().getColumn(2).setPreferredWidth(80);
            jTable1.getColumnModel().getColumn(2).setMaxWidth(80);
            jTable1.getColumnModel().getColumn(3).setMinWidth(65);
            jTable1.getColumnModel().getColumn(3).setPreferredWidth(65);
            jTable1.getColumnModel().getColumn(3).setMaxWidth(65);
        }

        jLabel4.setFont(new java.awt.Font("Josefin Sans SemiBold", 0, 14)); // NOI18N
        jLabel4.setText("Lista de Itens");

        jLabel5.setFont(new java.awt.Font("Josefin Sans SemiBold", 0, 14)); // NOI18N
        jLabel5.setText("Adicionar Novo Item");

        jLabel6.setFont(new java.awt.Font("Josefin Sans SemiBold", 0, 12)); // NOI18N
        jLabel6.setText("ID :");

        jLabel7.setFont(new java.awt.Font("Josefin Sans SemiBold", 0, 12)); // NOI18N
        jLabel7.setText("Preço Maximo :");

        jButton1.setText("Adicionar");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("Remover Seleção");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setText("Informações Adicionais");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                    .addComponent(jLabel6)
                                    .addGap(88, 88, 88)
                                    .addComponent(itemIDField))
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                    .addComponent(jLabel7)
                                    .addGap(22, 22, 22)
                                    .addComponent(itemPriceField))
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                    .addComponent(jButton2)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 42, Short.MAX_VALUE)
                                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel4)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(itemIDField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(itemPriceField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Mercado", jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // Return if either is empty.
        if (itemIDField.getText().equals("") || itemPriceField.getText().equals("")) {
            JOptionPane.showMessageDialog(null, "Os campos não podem estar vazios.");
            return;
        }
        
        // They are not both integers
        if (!isNumeric(itemIDField.getText()) || !isNumeric(itemPriceField.getText())) {
            JOptionPane.showMessageDialog(null, "Este não é um número válido.");
            return;
        }
            
        
        Integer ID = Integer.valueOf(itemIDField.getText());
        Long price = Long.valueOf(itemPriceField.getText());
        
        if (itemHash.get(ID) != null) {
            JOptionPane.showMessageDialog(null, "Esse item já está presente na lista.");
            return;
        }
        
        
        Item item = new Item();
        item.setID(ID);
        item.setPrecoDigitado(price);
        
        itemHash.put(ID, item);
        
        try {
            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            item = getItemFromOldItem(item);
            String priceText = "N/A";
            if (item.getMenorPreco() != 0)
                priceText = format(item.getMenorPreco());
            model.addRow(new Object[]{item.getID(), item.getNome(), format(item.getPrecoDigitado()), "" + priceText});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        Integer row = jTable1.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(null, "Você deve escolher um item a ser removido.");
            return;
        }
        
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        Integer ID = (Integer) model.getValueAt(row, 0);
        model.removeRow(row);
        itemHash.remove(ID);
        
        JOptionPane.showMessageDialog(null, "Item removido com sucesso!");
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        // TODO add your handling code here:
        Integer row = jTable1.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(null, "Você deve escolher um item para ver informações adicionais.");
            return;
        }
        
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        Integer ID = (Integer) model.getValueAt(row, 0);
        Item item = itemHash.get(ID);
        
        if (item == null) {
            JOptionPane.showMessageDialog(null, "Informações não puderam ser carregadas.");
            return;
        }
        
        if (item.getMenorPreco() == 0) {
            JOptionPane.showMessageDialog(null, "Esta informação só está disponível para itens que estão sendo vendidos em alguma loja.");
            return;            
        }
        
        // If an item was being sold, and is no longer being sold, gotta clear out all properties that were previously marked.
        String resMsg = 
                  "<html>"
                + "<font color=#DD2222><b>Informações</b></font><br>"
                + "<b>ID : </b>" + item.getID() + "<br>"
                + "<b>Nome : </b>" + item.getNome() + "<br>"
                + "<b>Preço Desejado : </b>" + format(item.getPrecoDigitado()) + "<br>"
                + "<b>Menor Preço    : </b>" + format(item.getMenorPreco()) + "<br>"
                + "<b>Qtd : </b>" + item.getQuantidade() + "<br>"
                + "<b>Loja : </b>" + item.getLoja() + "<br>"
                + "<b>Local : </b>" + item.getMap() + " " + item.getX() + "," + item.getY() + "<br>";
                
        JOptionPane.showMessageDialog(null, resMsg);
    }//GEN-LAST:event_jButton3ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new MainWindow().setVisible(true);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField itemIDField;
    private javax.swing.JTextField itemPriceField;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
