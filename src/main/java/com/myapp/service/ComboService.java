package com.myapp.service;

import com.myapp.dao.ComboDAO;
import com.myapp.model.Combo;
import java.sql.Date;
import java.util.List;

public class ComboService {

    private ComboDAO comboDAO = new ComboDAO();

    public List<Combo> findAll() {
        return comboDAO.findAll();
    }

    public List<Combo> findByName(String name) {
        return comboDAO.findByName(name);
    }

    public List<String> searchByName(String query) {
        return comboDAO.searchByName(query);
    }

    public List<String> searchMenuItems(String query) {
        return comboDAO.searchMenuItems("");
    }

    public double getMenuItemPrice(String name) {
        return comboDAO.getMenuItemPrice(name);
    }

    public boolean updateStatus(int id, String status) {
        return comboDAO.updateStatus(id, status);
    }

    public boolean update(int id, String name, String inclusions, double promoPrice, double originalPrice, Date validUntil) {
        return comboDAO.update(id, name, inclusions, promoPrice, originalPrice, validUntil);
    }

    public boolean insert(String name, String inclusions, double promoPrice, double originalPrice, Date validUntil) {
        return comboDAO.insert(name, inclusions, promoPrice, originalPrice, validUntil);
    }

    public boolean delete(int id) {
        return comboDAO.delete(id);
    }

    public int getTotalCount() {
        return comboDAO.getTotalCount();
    }

    public int getActiveCount() {
        return comboDAO.getActiveCount();
    }
}