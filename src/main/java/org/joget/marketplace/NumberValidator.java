package org.joget.marketplace;

import java.util.Map;
import java.util.regex.Pattern;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormValidator;
import org.joget.apps.form.service.FormUtil;

public class NumberValidator extends FormValidator {

    private Map<String, Object> properties;
    private Element element;
    private final static String MESSAGE_PATH = "messages/NumberValidator";

    @Override
    public String getName() {
        return "Number Validator";
    }

    @Override
    public String getDescription() {
        // Support i18n
        return AppPluginUtil.getMessage("org.joget.marketplace.NumberValidator.pluginDesc", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getVersion() {
        return "7.0-SNAPSHOT";
    }

    @Override
    public String getLabel() {
        // Support i18n
        return AppPluginUtil.getMessage("org.joget.marketplace.NumberValidator.pluginLabel", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    public String getPropertyOptions() {
        String formDefField;
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

    @Override
    public String getElementDecoration() {
        String mandatory = (String) getProperty("mandatory");
        return ("true".equals(mandatory)) ? "*" : "";
    }

    @Override
    public boolean validate(Element element, FormData data, String[] values) {
        String fieldId = (String) getProperty("fieldId");
        String formDefId = (String) getProperty("formDefId");
        String customValue = (String) getProperty("customValue");
        String operator = (String) getProperty("operator");
        String customMessage = (String) getProperty("customErrorMessage");
        String emptyValueMessage = (String) getProperty("emptyValueMessage");
        String id = FormUtil.getElementParameterName(element);
        String amount = (values.length > 0) ? values[0] : "";

        // Validate the input value
        if (amount.isEmpty()) {
            data.addFormError(id, "Please key in value.");
            return false;
        }
        if (Pattern.matches("\\D*", amount)) {
            data.addFormError(id, "Please key in numerical value only.");
            return false;
        }
        float amountF = Float.parseFloat(amount);

        // Get additional form context
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String tableName = null;
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        String key = element.getPrimaryKeyValue(data);
        String[] fieldValue = data.getRequestParameterValues(fieldId);
        if (formDefId != null) {
            tableName = appService.getFormTableName(appDef, formDefId);
        }

        // Delegate the comparison logic to the helper class
        if (operator != null) {
            return NumberComparisonValidator.validateComparison(data, id, amountF, operator,
                    customValue, customMessage, emptyValueMessage,
                    formDefId, fieldId, tableName, key, fieldValue);
        }
        return true;
    }
}
