/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package classes;

/**
 *
 * @author Victor
 */
public class Item {
    private Integer ID;
    private String nome;
    private Long precoDigitado;
    private Long menorPreco;
    private String map;
    private Integer x;
    private Integer y;
    private Integer quantidade;
    private String loja;
    

    public Integer getID() {
        return ID;
    }

    public void setID(Integer ID) {
        this.ID = ID;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Long getPrecoDigitado() {
        return precoDigitado;
    }

    public void setPrecoDigitado(Long precoDigitado) {
        this.precoDigitado = precoDigitado;
    }

    public Long getMenorPreco() {
        return menorPreco;
    }

    public void setMenorPreco(Long menorPreco) {
        this.menorPreco = menorPreco;
    }

    public String getMap() {
        return map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public String getLoja() {
        return loja;
    }

    public void setLoja(String loja) {
        this.loja = loja;
    }
    
    
    
}
