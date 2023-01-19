package org.joget.marketplace;

import java.util.Map;
import java.util.regex.Pattern;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormValidator;
import org.joget.apps.form.service.FormUtil;
import org.joget.apps.form.model.FormRow;

public class NumberValidator extends FormValidator {
    // Variables

    private Map<String, Object> properties;
    private Element element;
    private final static String MESSAGE_PATH = "messages/NumberValidator";

    public String getName() {
        return "Number Validator";
    }

    public String getDescription() {
        //support i18n
        return AppPluginUtil.getMessage("org.joget.marketplace.NumberValidator.pluginDesc", getClassName(), MESSAGE_PATH);
    }

    public String getVersion() {
        return "7.0.0";
    }

    public String getLabel() {
        //support i18n
        return AppPluginUtil.getMessage("org.joget.marketplace.NumberValidator.pluginLabel", getClassName(), MESSAGE_PATH);
    }

    public String getClassName() {
        return getClass().getName();
    }

    public String getPropertyOptions() {

        //new
        String formDefField = null;
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();

        if (appDef != null) {
            String formJsonUrl = "[CONTEXT_PATH]/web/json/console/app/" + appDef.getId() + "/" + appDef.getVersion() + "/forms/options";
            formDefField = "{\"name\": \"formDefId\",\"label\":\"@@form.marketplace.NumberValidator.form@@\",\"type\":\"selectbox\",\"required\":\"True\",\"control_field\": \"compareAgainst\",\"control_value\": \"formField\",\"options_ajax\":\"" + formJsonUrl + "\"}";
        } else {
            formDefField = "{\"name\": \"formDefId\",\"label\":\"@@form.marketplace.NumberValidator.form@@\",\"type\":\"textfield\",\"required\":\"True\",\"control_field\": \"compareAgainst\",\"control_value\": \"formField\"}";
        }

        Object[] arguments = new Object[]{formDefField};

        return AppUtil.readPluginResource(getClassName(), "/properties/NumberValidator.json", arguments, true, MESSAGE_PATH);
    }

    public String getElementDecoration() {
        String decoration = "";
        String mandatory = (String) getProperty("mandatory");
        if ("true".equals(mandatory)) {
            decoration += " * ";
        }
        if (decoration.trim().length() > 0) {
            decoration = decoration.trim();
        }
        return decoration;
    }

    public boolean validate(Element element, FormData data, String[] values) {

        String fieldId = (String) getProperty("fieldId");
        String formDefId = (String) getProperty("formDefId");
        String customValue = (String) getProperty("customValue");
        String operator = (String) getProperty("operator");
        String customMessage = (String) getProperty("customErrorMessage");
        String emptyValueMessage = (String) getProperty("emptyValueMessage");
        boolean result = true;
        String id = FormUtil.getElementParameterName(element);
        String amount = "";
        
        if (values.length > 0) {
            amount = values[0];
        }

        // amount on configuration
        if (amount.isEmpty()) {
            data.addFormError(id, "Please key in value.");
            return false;
        }
        
        if (Pattern.matches("\\D*", amount)){
            data.addFormError(id, "Please key in numerical value only.");
            return false;
        }
        
        else{
            float amountF = Float.parseFloat(amount);
            
                AppDefinition appDef = AppUtil.getCurrentAppDefinition();
                String tableName = null;
                AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
                String key = element.getPrimaryKeyValue(data);
                String[] fieldValue = data.getRequestParameterValues(fieldId);
                
                if (formDefId != null) {
                    tableName = appService.getFormTableName(appDef, formDefId);
                }

            if (operator != null) {
                if (operator.equals("=")) {
                    result = validateEqual(data, id, values, amountF, customMessage, emptyValueMessage, formDefId, fieldId, customValue, tableName, key, fieldValue);
                }
                
                if (operator.equals("<>")) {
                    result = validateNotEqual(data, id, values, amountF, customMessage, emptyValueMessage, formDefId, fieldId, customValue, tableName, key, fieldValue);
                }
                
                if (operator.equals(">")) {
                    result = validateGreaterThan(data, id, values, amountF, customMessage, emptyValueMessage, formDefId, fieldId, customValue, tableName, key, fieldValue);
                }
               
                if (operator.equals(">=")) {
                    result = validateGreaterThanOrEqual(data, id, values, amountF, customMessage, emptyValueMessage, formDefId, fieldId, customValue, tableName, key, fieldValue);
                }
                
                if (operator.equals("<")) {
                    result = validateLessThan(data, id, values, amountF, customMessage, emptyValueMessage, formDefId, fieldId, customValue, tableName, key, fieldValue);
                }
                
                if (operator.equals("<=")) {
                    result = validateLessThanOrEqual(data, id, values, amountF, customMessage, emptyValueMessage, formDefId, fieldId, customValue, tableName, key, fieldValue);
                }  
            }
            return result;
        }
    }

    protected boolean validateEqual(FormData data, String id, String[] values, Float amountF, String customMessage, String emptyValueMessage, String formDefId, String fieldId, String customValue, String tableName, String key, String[] fieldValue) {
        boolean result = true;

        if (values != null && values.length > 0) {

            if (customValue != null && !customValue.isEmpty()) {

                Float valueF = Float.parseFloat(customValue);

                if ((Float.compare(amountF, valueF) != 0)) {
                    if (customMessage == null || customMessage.isEmpty()) {
                        data.addFormError(id, "Number Validation Failed");
                    } else {
                        data.addFormError(id, customMessage);
                    }
                    result = false;
                }
            } 
            
            else {
                String comparedAgainstFieldValue;
                
                //grab value from form-field id logic
                if(tableName != null){
                FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
                
                
                //if record does exist
                if (key != null){
                    try{
                    FormRow existingRow = formDataDao.load(formDefId, tableName, key);
                    String existingValue = existingRow.get(fieldId).toString();
                    String primaryKey = formDataDao.findPrimaryKey(formDefId, tableName, fieldId, existingValue);
                    FormRow row = formDataDao.load(formDefId, tableName, primaryKey);
                    comparedAgainstFieldValue = row.get(fieldId).toString(); 
                    }catch (Exception e){
                        comparedAgainstFieldValue = null;
                    }
                    
                    //if the compared field is empty
                    if (comparedAgainstFieldValue == null || comparedAgainstFieldValue.isEmpty()){
                        if(emptyValueMessage == null || emptyValueMessage.isEmpty()){
                             data.addFormError(id, "Value of compared field value is empty");
                        }
                        else{
                            data.addFormError(id, emptyValueMessage);
                        }
                        
                        return false;
                    }
                }
                
                //if record is new
                else {
                    try{
                         comparedAgainstFieldValue = fieldValue[0];
                    }catch (Exception e){
                        //handle exception when record is new, but compared value doesn't exist
                        comparedAgainstFieldValue = null;
                    }
                    //if the compared field is empty           
                    if  (comparedAgainstFieldValue == null || comparedAgainstFieldValue.isEmpty() || fieldValue[0] == null || fieldValue[0].isEmpty()){
                        if(emptyValueMessage == null || emptyValueMessage.isEmpty()){
                             data.addFormError(id, "Value of compared field value is empty");
                        }
                        else{
                            data.addFormError(id, emptyValueMessage);
                        }
                     return false;
                    }
                }
                    
                float comparedAgainstFieldValueFloat = Float.parseFloat(comparedAgainstFieldValue);
                
                if ((Float.compare(amountF, comparedAgainstFieldValueFloat) != 0)) {
                    if (customMessage == null || customMessage.isEmpty()) {
                        data.addFormError(id, "Number Validation Failed");
                    } else {
                        data.addFormError(id, customMessage);
                    }
                    result = false;
                }
                
                } 
            }
        }
        return result;
    }

   
    protected boolean validateNotEqual(FormData data, String id, String[] values, Float amountF, String customMessage, String emptyValueMessage, String formDefId, String fieldId, String customValue, String tableName, String key, String[] fieldValue) {
     boolean result = true;

        if (values != null && values.length > 0) {

            if (customValue != null && !customValue.isEmpty()) {

                Float valueF = Float.parseFloat(customValue);

                if ((Float.compare(amountF, valueF) == 0)) {
                    if (customMessage == null || customMessage.isEmpty()) {
                        data.addFormError(id, "Number Validation Failed");
                    } else {
                        data.addFormError(id, customMessage);
                    }
                    result = false;
                }
            } 
            
            else {
                String comparedAgainstFieldValue;
                
                //grab value from form-field id logic
                if(tableName != null){
                FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
                
                
                //if record does exist
                if (key != null){
                    try{
                    FormRow existingRow = formDataDao.load(formDefId, tableName, key);
                    String existingValue = existingRow.get(fieldId).toString();
                    String primaryKey = formDataDao.findPrimaryKey(formDefId, tableName, fieldId, existingValue);
                    FormRow row = formDataDao.load(formDefId, tableName, primaryKey);
                    comparedAgainstFieldValue = row.get(fieldId).toString(); 
                    }catch (Exception e){
                        comparedAgainstFieldValue = null;
                    }
                    
                    //if the compared field is empty
                    if (comparedAgainstFieldValue == null || comparedAgainstFieldValue.isEmpty()){
                        if(emptyValueMessage == null || emptyValueMessage.isEmpty()){
                             data.addFormError(id, "Value of compared field value is empty");
                        }
                        else{
                            data.addFormError(id, emptyValueMessage);
                        }
                        
                        return false;
                    }
                }
                
                //if record is new
                else {
                    try{
                         comparedAgainstFieldValue = fieldValue[0];
                    }catch (Exception e){
                        //handle exception when record is new, but compared value doesn't exist
                        comparedAgainstFieldValue = null;
                    }
                    //if the compared field is empty           
                    if  (comparedAgainstFieldValue == null || comparedAgainstFieldValue.isEmpty() || fieldValue[0] == null || fieldValue[0].isEmpty()){
                        if(emptyValueMessage == null || emptyValueMessage.isEmpty()){
                             data.addFormError(id, "Value of compared field value is empty");
                        }
                        else{
                            data.addFormError(id, emptyValueMessage);
                        }
                     return false;
                    }
                }    
                float comparedAgainstFieldValueFloat = Float.parseFloat(comparedAgainstFieldValue);
                
                if ((Float.compare(amountF, comparedAgainstFieldValueFloat) == 0)) {
                    if (customMessage == null || customMessage.isEmpty()) {
                        data.addFormError(id, "Number Validation Failed");
                    } else {
                        data.addFormError(id, customMessage);
                    }
                    result = false;
                }
                
                } 
            }
        }
        return result;
    }

     
    protected boolean validateGreaterThan(FormData data, String id, String[] values, Float amountF, String customMessage, String emptyValueMessage, String formDefId, String fieldId, String customValue, String tableName, String key, String[] fieldValue){
     boolean result = true;

        if (values != null && values.length > 0) {

            if (customValue != null && !customValue.isEmpty()) {

                Float valueF = Float.parseFloat(customValue);

                if ((Float.compare(amountF, valueF) <= 0)) {
                    if (customMessage == null || customMessage.isEmpty()) {
                        data.addFormError(id, "Number Validation Failed");
                    } else {
                        data.addFormError(id, customMessage);
                    }
                    result = false;
                }
            } 
            
            else {
                String comparedAgainstFieldValue;
                
                //grab value from form-field id logic
                if(tableName != null){
                FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
                
                
                //if record does exist
                if (key != null){
                    try{
                    FormRow existingRow = formDataDao.load(formDefId, tableName, key);
                    String existingValue = existingRow.get(fieldId).toString();
                    String primaryKey = formDataDao.findPrimaryKey(formDefId, tableName, fieldId, existingValue);
                    FormRow row = formDataDao.load(formDefId, tableName, primaryKey);
                    comparedAgainstFieldValue = row.get(fieldId).toString(); 
                    }catch (Exception e){
                        comparedAgainstFieldValue = null;
                    }
                    
                    //if the compared field is empty
                    if (comparedAgainstFieldValue == null || comparedAgainstFieldValue.isEmpty()){
                        if(emptyValueMessage == null || emptyValueMessage.isEmpty()){
                             data.addFormError(id, "Value of compared field value is empty");
                        }
                        else{
                            data.addFormError(id, emptyValueMessage);
                        }
                     return false;
                    }
                }
                
                //if record is new
                else {
                    try{
                         comparedAgainstFieldValue = fieldValue[0];
                    }catch (Exception e){
                        //handle exception when record is new, but compared value doesn't exist
                        comparedAgainstFieldValue = null;
                    }
                    //if the compared field is empty           
                    if  (comparedAgainstFieldValue == null || comparedAgainstFieldValue.isEmpty() || fieldValue[0] == null || fieldValue[0].isEmpty()){
                        if(emptyValueMessage == null || emptyValueMessage.isEmpty()){
                             data.addFormError(id, "Value of compared field value is empty");
                        }
                        else{
                            data.addFormError(id, emptyValueMessage);
                        }
                        
                        return false;
                    }
                }
                    
                float comparedAgainstFieldValueFloat = Float.parseFloat(comparedAgainstFieldValue);
                
                if ((Float.compare(amountF, comparedAgainstFieldValueFloat) <= 0)) {
                    if (customMessage == null || customMessage.isEmpty()) {
                        data.addFormError(id, "Number Validation Failed");
                    } else {
                        data.addFormError(id, customMessage);
                    }
                    result = false;
                }
                
                } 
            }
        }
        return result;
    }
    
    protected boolean validateGreaterThanOrEqual(FormData data, String id, String[] values, Float amountF, String customMessage, String emptyValueMessage, String formDefId, String fieldId, String customValue, String tableName, String key, String[] fieldValue) {
     boolean result = true;

        if (values != null && values.length > 0) {

            if (customValue != null && !customValue.isEmpty()) {

                Float valueF = Float.parseFloat(customValue);

                if ((Float.compare(amountF, valueF) < 0)) {
                    if (customMessage == null || customMessage.isEmpty()) {
                        data.addFormError(id, "Number Validation Failed");
                    } else {
                        data.addFormError(id, customMessage);
                    }
                    result = false;
                }
            } 
            
            else {
                String comparedAgainstFieldValue;
                
                //grab value from form-field id logic
                if(tableName != null){
                FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
                
                
                //if record does exist
                if (key != null){
                    try{
                    FormRow existingRow = formDataDao.load(formDefId, tableName, key);
                    String existingValue = existingRow.get(fieldId).toString();
                    String primaryKey = formDataDao.findPrimaryKey(formDefId, tableName, fieldId, existingValue);
                    FormRow row = formDataDao.load(formDefId, tableName, primaryKey);
                    comparedAgainstFieldValue = row.get(fieldId).toString(); 
                    }catch (Exception e){
                        comparedAgainstFieldValue = null;
                    }
                    
                    //if the compared field is empty
                    if (comparedAgainstFieldValue == null || comparedAgainstFieldValue.isEmpty()){
                        if(emptyValueMessage == null || emptyValueMessage.isEmpty()){
                             data.addFormError(id, "Value of compared field value is empty");
                        }
                        else{
                            data.addFormError(id, emptyValueMessage);
                        }
                        
                        return false;
                    }
                }
                
                //if record is new
                else {
                    try{
                         comparedAgainstFieldValue = fieldValue[0];
                    }catch (Exception e){
                        //handle exception when record is new, but compared value doesn't exist
                        comparedAgainstFieldValue = null;
                    }
                    //if the compared field is empty           
                    if  (comparedAgainstFieldValue == null || comparedAgainstFieldValue.isEmpty() || fieldValue[0] == null || fieldValue[0].isEmpty()){
                        if(emptyValueMessage == null || emptyValueMessage.isEmpty()){
                             data.addFormError(id, "Value of compared field value is empty");
                        }
                        else{
                            data.addFormError(id, emptyValueMessage);
                        }
                     return false;
                    }
                }    
                float comparedAgainstFieldValueFloat = Float.parseFloat(comparedAgainstFieldValue);
                
                if ((Float.compare(amountF, comparedAgainstFieldValueFloat) < 0)) {
                    if (customMessage == null || customMessage.isEmpty()) {
                        data.addFormError(id, "Number Validation Failed");
                    } else {
                        data.addFormError(id, customMessage);
                    }
                    result = false;
                }
                
                } 
            }
        }
        return result;
    }

    protected boolean validateLessThan(FormData data, String id, String[] values, Float amountF, String customMessage, String emptyValueMessage, String formDefId, String fieldId, String customValue, String tableName, String key, String[] fieldValue) {
     boolean result = true;

        if (values != null && values.length > 0) {

            if (customValue != null && !customValue.isEmpty()) {

                Float valueF = Float.parseFloat(customValue);

                if ((Float.compare(amountF, valueF) >= 0)) {
                    if (customMessage == null || customMessage.isEmpty()) {
                        data.addFormError(id, "Number Validation Failed");
                    } else {
                        data.addFormError(id, customMessage);
                    }
                    result = false;
                }
            } 
            
            else {
                String comparedAgainstFieldValue;
                
                //grab value from form-field id logic
                if(tableName != null){
                FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
                
                
                //if record does exist
                if (key != null){
                    try{
                    FormRow existingRow = formDataDao.load(formDefId, tableName, key);
                    String existingValue = existingRow.get(fieldId).toString();
                    String primaryKey = formDataDao.findPrimaryKey(formDefId, tableName, fieldId, existingValue);
                    FormRow row = formDataDao.load(formDefId, tableName, primaryKey);
                    comparedAgainstFieldValue = row.get(fieldId).toString(); 
                    }catch (Exception e){
                        comparedAgainstFieldValue = null;
                    }
                    
                    //if the compared field is empty
                    if (comparedAgainstFieldValue == null || comparedAgainstFieldValue.isEmpty()){
                        if(emptyValueMessage == null || emptyValueMessage.isEmpty()){
                             data.addFormError(id, "Value of compared field value is empty");
                        }
                        else{
                            data.addFormError(id, emptyValueMessage);
                        }
                        
                        return false;
                    }
                }
                
                //if record is new
                else {
                    try{
                         comparedAgainstFieldValue = fieldValue[0];
                    }catch (Exception e){
                        //handle exception when record is new, but compared value doesn't exist
                        comparedAgainstFieldValue = null;
                    }
                    //if the compared field is empty           
                    if  (comparedAgainstFieldValue == null || comparedAgainstFieldValue.isEmpty() || fieldValue[0] == null || fieldValue[0].isEmpty()){
                        if(emptyValueMessage == null || emptyValueMessage.isEmpty()){
                             data.addFormError(id, "Value of compared field value is empty");
                        }
                        else{
                            data.addFormError(id, emptyValueMessage);
                        }
                     return false;
                    }
                    }
                    
                float comparedAgainstFieldValueFloat = Float.parseFloat(comparedAgainstFieldValue);
                
                if ((Float.compare(amountF, comparedAgainstFieldValueFloat) >= 0)) {
                    if (customMessage == null || customMessage.isEmpty()) {
                        data.addFormError(id, "Number Validation Failed");
                    } else {
                        data.addFormError(id, customMessage);
                    }
                    result = false;
                }
                
                } 
            }
        }
        return result;
    }
    
    protected boolean validateLessThanOrEqual(FormData data, String id, String[] values, Float amountF, String customMessage, String emptyValueMessage, String formDefId, String fieldId, String customValue, String tableName, String key, String[] fieldValue) {
     boolean result = true;

        if (values != null && values.length > 0) {

            if (customValue != null && !customValue.isEmpty()) {

                Float valueF = Float.parseFloat(customValue);

                if ((Float.compare(amountF, valueF) > 0)) {
                    if (customMessage == null || customMessage.isEmpty()) {
                        data.addFormError(id, "Number Validation Failed");
                    } else {
                        data.addFormError(id, customMessage);
                    }
                    result = false;
                }
            } 
            
            else {
                String comparedAgainstFieldValue;
                
                //grab value from form-field id logic
                if(tableName != null){
                FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
                
                
                //if record does exist
                if (key != null){
                    try{
                    FormRow existingRow = formDataDao.load(formDefId, tableName, key);
                    String existingValue = existingRow.get(fieldId).toString();
                    String primaryKey = formDataDao.findPrimaryKey(formDefId, tableName, fieldId, existingValue);
                    FormRow row = formDataDao.load(formDefId, tableName, primaryKey);
                    comparedAgainstFieldValue = row.get(fieldId).toString(); 
                    }catch (Exception e){
                        comparedAgainstFieldValue = null;
                    }
                    
                    //if the compared field is empty
                    if (comparedAgainstFieldValue == null || comparedAgainstFieldValue.isEmpty()){
                        if(emptyValueMessage == null || emptyValueMessage.isEmpty()){
                             data.addFormError(id, "Value of compared field value is empty");
                        }
                        else{
                            data.addFormError(id, emptyValueMessage);
                        }
                        
                        return false;
                    }
                }
                
                //if record is new
                else {
                    try{
                         comparedAgainstFieldValue = fieldValue[0];
                    }catch (Exception e){
                        //handle exception when record is new, but compared value doesn't exist
                        comparedAgainstFieldValue = null;
                    }
                    //if the compared field is empty           
                    if  (comparedAgainstFieldValue == null || comparedAgainstFieldValue.isEmpty() || fieldValue[0] == null || fieldValue[0].isEmpty()){
                        if(emptyValueMessage == null || emptyValueMessage.isEmpty()){
                             data.addFormError(id, "Value of compared field value is empty");
                        }
                        else{
                            data.addFormError(id, emptyValueMessage);
                        }
                     return false;
                    }
                    }
                    
                float comparedAgainstFieldValueFloat = Float.parseFloat(comparedAgainstFieldValue);
                
                if ((Float.compare(amountF, comparedAgainstFieldValueFloat) > 0)) {
                    if (customMessage == null || customMessage.isEmpty()) {
                        data.addFormError(id, "Number Validation Failed");
                    } else {
                        data.addFormError(id, customMessage);
                    }
                    result = false;
                }
                
                } 
            }
        }
        return result;
    }
     
}
