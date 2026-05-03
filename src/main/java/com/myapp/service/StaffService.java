package com.myapp.service;

import com.myapp.dao.StaffDAO;
import com.myapp.dao.UserDAO;
import com.myapp.model.Staff;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public class StaffService {

    private StaffDAO staffDAO = new StaffDAO();
    private UserDAO userDAO = new UserDAO();

    public List<Staff> findAll() {
        return staffDAO.findAll();
    }

    public Staff findById(int id) {
        return staffDAO.findById(id);
    }

    public boolean updateStatus(int id, String status) {
        return staffDAO.updateStatus(id, status);
    }

    public int getTotalCount() {
        return staffDAO.getTotalCount();
    }

    public int getActiveCount() {
        return staffDAO.getActiveCount();
    }

    public int getOnBreakCount() {
        return staffDAO.getOnBreakCount();
    }

    public boolean updateName(int id, String name) {
        return staffDAO.updateName(id, name);
    }

    public boolean updateEmail(int id, String email) {
        return staffDAO.updateEmail(id, email);
    }

    public boolean updateRole(int id, String role) {
        return staffDAO.updateRole(id, role);
    }

    public boolean updateShift(int id, LocalTime start, LocalTime end) {
        return staffDAO.updateShift(id, start, end);
    }

    public boolean delete(int id) {
        return staffDAO.delete(id);
    }

    public boolean insert(String name, String email, String password, String role, LocalTime start, LocalTime end) {
        return staffDAO.insert(name, email, password, role, start, end);
    }

    public boolean existsByName(String name) {
        return staffDAO.existsByName(name);
    }

    public boolean existsByEmail(String email) {
        return staffDAO.existsByEmail(email);
    }

    public List<Staff> findActiveStaff() {
        return staffDAO.findActiveStaff();
    }

    public Optional<String> findPasswordHashByStaffId(int staffId) {
        return userDAO.findPasswordHashByStaffId(staffId);
    }

    public boolean updatePasswordByStaffId(int staffId, String password) {
        return userDAO.updatePasswordByStaffId(staffId, password);
    }
}