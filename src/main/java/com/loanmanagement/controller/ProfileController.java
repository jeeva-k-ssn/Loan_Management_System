package com.loanmanagement.controller;

import com.loanmanagement.database.DatabaseConnection;
import com.loanmanagement.model.User;
import com.loanmanagement.navigation.NavigationManager;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;
import java.text.NumberFormat;
import java.util.*;

/** Customer profile. Database reads are performed on a worker thread. */
public class ProfileController {
    @FXML private TextField nameField,emailField,phoneField; @FXML private TextArea addressField;
    @FXML private Label customerIdLabel,applicationsLabel,pendingLabel,approvedLabel,rejectedLabel,activeLoansLabel,closedLoansLabel,borrowedLabel,paidLabel,outstandingLabel,messageLabel;
    @FXML private ListView<String> activityList;
    private User currentUser; private int targetCustomerId,targetUserId;
    public void setCurrentUser(User user){currentUser=user;targetCustomerId=0;loadProfile();}
    public void setCustomer(User viewer,int customerId){currentUser=viewer;targetCustomerId=customerId;loadProfile();}
    private void loadProfile(){
        if(currentUser==null||(!role("CUSTOMER")&&!role("ADMIN"))){showError("Access denied","Your account cannot open customer profiles.");return;}
        if(targetCustomerId==0&&!role("CUSTOMER")){showError("Profile unavailable","Select a customer before opening a profile.");return;}
        Task<ProfileData> task=new Task<>(){protected ProfileData call() throws SQLException{return fetchProfile();}};
        task.setOnSucceeded(e->render(task.getValue())); task.setOnFailed(e->showError("Profile unavailable","Unable to load your profile right now."));
        start(task,"loanflow-profile-load");
    }
    private ProfileData fetchProfile() throws SQLException{
        try(Connection c=DatabaseConnection.getConnection()){
            String sql="SELECT c.CUSTOMER_ID,c.USER_ID,u.FULL_NAME,u.EMAIL,c.PHONE,c.ADDRESS FROM LMS_CUSTOMER c JOIN USERS u ON u.USER_ID=c.USER_ID WHERE "+(targetCustomerId>0?"c.CUSTOMER_ID=?":"c.USER_ID=?");
            int lookup=targetCustomerId>0?targetCustomerId:currentUser.getUserId(); int cid,uid; String name,email,phone,address;
            try(PreparedStatement p=c.prepareStatement(sql)){p.setInt(1,lookup);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("profile not found");cid=r.getInt(1);uid=r.getInt(2);name=r.getString(3);email=r.getString(4);phone=r.getString(5);address=r.getString(6);}}
            int apps=count(c,"SELECT COUNT(*) FROM LOAN_APPLICATION WHERE CUSTOMER_ID=?",cid),pending=count(c,"SELECT COUNT(*) FROM LOAN_APPLICATION WHERE CUSTOMER_ID=? AND UPPER(STATUS)='PENDING'",cid),approved=count(c,"SELECT COUNT(*) FROM LOAN_APPLICATION WHERE CUSTOMER_ID=? AND UPPER(STATUS)='APPROVED'",cid),rejected=count(c,"SELECT COUNT(*) FROM LOAN_APPLICATION WHERE CUSTOMER_ID=? AND UPPER(STATUS)='REJECTED'",cid),active=count(c,"SELECT COUNT(*) FROM LOAN WHERE CUSTOMER_ID=? AND STATUS='ACTIVE'",cid),closed=count(c,"SELECT COUNT(*) FROM LOAN WHERE CUSTOMER_ID=? AND STATUS='CLOSED'",cid);
            double borrowed=amount(c,"SELECT NVL(SUM(LOAN_AMOUNT),0) FROM LOAN WHERE CUSTOMER_ID=?",cid),paid=amount(c,"SELECT NVL(SUM(p.AMOUNT),0) FROM PAYMENT p JOIN LOAN l ON l.LOAN_ID=p.LOAN_ID WHERE l.CUSTOMER_ID=? AND p.PAYMENT_STATUS='PAID'",cid),repayable=amount(c,"SELECT NVL(SUM(EMI_AMOUNT*TENURE_MONTHS),0) FROM LOAN WHERE CUSTOMER_ID=?",cid);
            var activity=new ArrayList<String>(); String a="SELECT activity FROM (SELECT 'Application #'||APPLICATION_ID||' · '||STATUS activity_date, 'Application #'||APPLICATION_ID||' · '||STATUS activity FROM LOAN_APPLICATION WHERE CUSTOMER_ID=? UNION ALL SELECT 'Payment #'||p.PAYMENT_ID||' · Loan #'||l.LOAN_ID activity_date,'Payment #'||p.PAYMENT_ID||' · Loan #'||l.LOAN_ID activity FROM PAYMENT p JOIN LOAN l ON l.LOAN_ID=p.LOAN_ID WHERE l.CUSTOMER_ID=?) ORDER BY activity_date DESC FETCH FIRST 8 ROWS ONLY";
            try(PreparedStatement p=c.prepareStatement(a)){p.setInt(1,cid);p.setInt(2,cid);try(ResultSet r=p.executeQuery()){while(r.next())activity.add(r.getString(1));}}
            return new ProfileData(cid,uid,name,email,phone,address,apps,pending,approved,rejected,active,closed,borrowed,paid,Math.max(0,repayable-paid),activity);
        }
    }
    private void render(ProfileData d){targetCustomerId=d.cid;targetUserId=d.uid;customerIdLabel.setText(String.valueOf(d.cid));nameField.setText(nullToEmpty(d.name));emailField.setText(nullToEmpty(d.email));phoneField.setText(nullToEmpty(d.phone));addressField.setText(nullToEmpty(d.address));applicationsLabel.setText(String.valueOf(d.apps));pendingLabel.setText(String.valueOf(d.pending));approvedLabel.setText(String.valueOf(d.approved));rejectedLabel.setText(String.valueOf(d.rejected));activeLoansLabel.setText(String.valueOf(d.active));closedLoansLabel.setText(String.valueOf(d.closed));borrowedLabel.setText(money(d.borrowed));paidLabel.setText(money(d.paid));outstandingLabel.setText(money(d.outstanding));activityList.setItems(FXCollections.observableArrayList(d.activity));}
    @FXML private void save(){String n=nameField.getText().trim(),e=emailField.getText().trim(),p=phoneField.getText().trim(),a=addressField.getText().trim();if(n.length()<3||!e.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")||(!p.isEmpty()&&!p.matches("[0-9+() -]{7,20}"))){messageLabel.setText("Enter a valid name, email, and phone number.");return;}Task<Boolean> task=new Task<>(){protected Boolean call() throws SQLException{try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("UPDATE USERS SET FULL_NAME=?,EMAIL=? WHERE USER_ID=?")){s.setString(1,n);s.setString(2,e);s.setInt(3,targetUserId);s.executeUpdate();try(PreparedStatement q=c.prepareStatement("UPDATE LMS_CUSTOMER SET FULL_NAME=?,EMAIL=?,PHONE=?,ADDRESS=? WHERE USER_ID=?")){q.setString(1,n);q.setString(2,e);q.setString(3,p.isEmpty()?null:p);q.setString(4,a.isEmpty()?null:a);q.setInt(5,targetUserId);q.executeUpdate();}return true;}}};task.setOnSucceeded(x->{messageLabel.setText("Profile updated successfully.");loadProfile();});task.setOnFailed(x->messageLabel.setText("Unable to update profile. Please try again."));start(task,"loanflow-profile-save");}
    @FXML private void refresh(){loadProfile();}
    @FXML private void back(){try{String r=role("ADMIN")?"/fxml/admin.fxml":"/fxml/dashboard.fxml";NavigationManager.navigate((javafx.stage.Stage)nameField.getScene().getWindow(),r,"LoanFlow - Dashboard",c->{if(role("ADMIN"))((AdminController)c).setCurrentUser(currentUser);else ((DashboardController)c).setCurrentUser(currentUser);});}catch(Exception e){showError("Navigation error","Unable to return to the previous page.");}}
    private int count(Connection c,String s,int id)throws SQLException{try(PreparedStatement p=c.prepareStatement(s)){p.setInt(1,id);try(ResultSet r=p.executeQuery()){return r.next()?r.getInt(1):0;}}} private double amount(Connection c,String s,int id)throws SQLException{try(PreparedStatement p=c.prepareStatement(s)){p.setInt(1,id);try(ResultSet r=p.executeQuery()){return r.next()?r.getDouble(1):0;}}}
    private boolean role(String r){return currentUser!=null&&r.equalsIgnoreCase(currentUser.getRole());} private void start(Task<?> t,String n){Thread x=new Thread(t,n);x.setDaemon(true);x.start();} private String nullToEmpty(String s){return s==null?"":s;} private String money(double v){return NumberFormat.getCurrencyInstance(new Locale("en","IN")).format(v);} private void showError(String t,String m){Alert a=new Alert(Alert.AlertType.ERROR);a.setTitle(t);a.setHeaderText(null);a.setContentText(m);a.showAndWait();}
    private static final class ProfileData{final int cid,uid,apps,pending,approved,rejected,active,closed;final String name,email,phone,address;final double borrowed,paid,outstanding;final List<String> activity;ProfileData(int c,int u,String n,String e,String p,String a,int ap,int pe,int ok,int re,int ac,int cl,double b,double pa,double o,List<String> l){cid=c;uid=u;name=n;email=e;phone=p;address=a;apps=ap;pending=pe;approved=ok;rejected=re;active=ac;closed=cl;borrowed=b;paid=pa;outstanding=o;activity=l;}}
}
