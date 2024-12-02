package org.joget.marketplace;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;

public class NumberComparisonValidator {

    /**
     * Validates the number using the provided operator.
     *
     * @param data              The form data
     * @param id                The element id
     * @param amountF           The number to validate (parsed from user input)
     * @param operator          The comparison operator (e.g., "=", "<>", ">", ">=", "<", "<=")
     * @param customValue       If provided, this custom value is used for comparison instead of a form field value.
     * @param customMessage     Custom error message to display if the validation fails.
     * @param emptyValueMessage Error message to display if the compared field is empty.
     * @param formDefId         The form definition id (used to load the compared value from the database).
     * @param fieldId           The field id to compare against.
     * @param tableName         The table name (if available).
     * @param key               The primary key value of the record.
     * @param fieldValue        The current field value from the request parameters.
     * @return                  true if the validation passes; false otherwise.
     */
    public static boolean validateComparison(FormData data, String id, float amountF, String operator,
                                             String customValue, String customMessage, String emptyValueMessage,
                                             String formDefId, String fieldId, String tableName, String key,
                                             String[] fieldValue) {
        if (customValue != null && !customValue.isEmpty()) {
            try {
                float valueF = Float.parseFloat(customValue);
                return checkComparison(data, id, amountF, valueF, operator, customMessage);
            } catch (NumberFormatException e) {
                data.addFormError(id, "Custom value is not a valid number.");
                return false;
            }
        } else {
            Float comparedValue = getComparedFieldValue(data, id, formDefId, fieldId, tableName, key, fieldValue, emptyValueMessage);
            if (comparedValue == null) {
                return false;
            }
            return checkComparison(data, id, amountF, comparedValue, operator, customMessage);
        }
    }

    /**
     * Retrieves and parses the compared field value either from the database (if editing an existing record)
     * or from the request parameters (if a new record).
     */
    private static Float getComparedFieldValue(FormData data, String id, String formDefId, String fieldId, String tableName,
                                               String key, String[] fieldValue, String emptyValueMessage) {
        String comparedValueStr = null;
        if (tableName != null) {
            FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
            if (key != null) {
                try {
                    FormRow existingRow = formDataDao.load(formDefId, tableName, key);
                    String existingValue = existingRow.get(fieldId).toString();
                    String primaryKey = formDataDao.findPrimaryKey(formDefId, tableName, fieldId, existingValue);
                    FormRow row = formDataDao.load(formDefId, tableName, primaryKey);
                    comparedValueStr = row.get(fieldId).toString();
                } catch (Exception e) {
                    comparedValueStr = null;
                }
            } else {
                try {
                    comparedValueStr = fieldValue[0];
                } catch (Exception e) {
                    comparedValueStr = null;
                }
            }
        }
        if (comparedValueStr == null || comparedValueStr.isEmpty()) {
            String msg = (emptyValueMessage == null || emptyValueMessage.isEmpty())
                    ? "Value of compared field value is empty" : emptyValueMessage;
            data.addFormError(id, msg);
            return null;
        }
        try {
            return Float.parseFloat(comparedValueStr);
        } catch (NumberFormatException e) {
            data.addFormError(id, "Compared field value is not a valid number.");
            return null;
        }
    }

    /**
     * Compares two float numbers based on the operator.
     * If the condition is not met, an error is added.
     */
    private static boolean checkComparison(FormData data, String id, float amountF, float reference,
                                           String operator, String customMessage) {
        boolean valid = false;
        switch (operator) {
            case "=":
                valid = (Float.compare(amountF, reference) == 0);
                break;
            case "<>":
                valid = (Float.compare(amountF, reference) != 0);
                break;
            case ">":
                valid = (Float.compare(amountF, reference) > 0);
                break;
            case ">=":
                valid = (Float.compare(amountF, reference) >= 0);
                break;
            case "<":
                valid = (Float.compare(amountF, reference) < 0);
                break;
            case "<=":
                valid = (Float.compare(amountF, reference) <= 0);
                break;
            default:
                break;
        }
        if (!valid) {
            String errorMsg = (customMessage == null || customMessage.isEmpty())
                    ? "Number Validation Failed" : customMessage;
            data.addFormError(id, errorMsg);
        }
        return valid;
    }
}
