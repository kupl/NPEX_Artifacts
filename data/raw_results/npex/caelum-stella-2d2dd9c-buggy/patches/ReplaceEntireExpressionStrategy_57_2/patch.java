package br.com.caelum.stella.faces.validation;

import java.util.List;
import java.util.ResourceBundle;

import javax.faces.application.FacesMessage;
import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import br.com.caelum.stella.ResourceBundleMessageProducer;
import br.com.caelum.stella.ValidationMessage;
import br.com.caelum.stella.validation.CNPJValidator;
import br.com.caelum.stella.validation.InvalidStateException;

/**
 * Caso ocorra algum erro de validação, todas as mensagens serão enfileiradas no
 * FacesContext e associadas ao elemento inválido.
 * 
 * @author Leonardo Bessa
 */
@FacesValidator(StellaCNPJValidator.VALIDATOR_ID)
public class StellaCNPJValidator implements Validator, StateHolder {

    /**
     * Identificador do Validador JSF.
     */
    public static final String VALIDATOR_ID = "StellaCNPJValidator";

    private final ResourceBundleFinder resourceBundleFinder = new ResourceBundleFinder();

    private boolean formatted;
    private boolean transientValue = false;

    /**
     * Atribui se a regra de validação deve considerar, ou não, a cadeia no
     * formato do documento.
     * 
     * @param formatted
     *            caso seja <code>true</code> o validador considera que a cadeia
     *            está formatada; caso contrário, considera que a cadeia contém
     *            apenas dígitos numéricos.
     */
    public void setFormatted(boolean formatted) {
        this.formatted = formatted;
    }

public void validate(javax.faces.context.FacesContext facesContext, javax.faces.component.UIComponent uiComponent, java.lang.Object value) throws javax.faces.validator.ValidatorException {
    java.util.ResourceBundle bundle = resourceBundleFinder.getForCurrentLocale(facesContext);
    br.com.caelum.stella.ResourceBundleMessageProducer producer = new br.com.caelum.stella.ResourceBundleMessageProducer(bundle);
    br.com.caelum.stella.validation.CNPJValidator validator = new br.com.caelum.stella.validation.CNPJValidator(producer, formatted);
    try {
        /* NPEX_PATCH_BEGINS */
        validator.assertValid(value != null ? value.toString() : "");
    } catch (br.com.caelum.stella.validation.InvalidStateException e) {
        java.util.List<br.com.caelum.stella.ValidationMessage> messages = e.getInvalidMessages();
        java.lang.String firstErrorMessage = messages.remove(0).getMessage();
        registerAllMessages(facesContext, uiComponent, messages);
        throw new javax.faces.validator.ValidatorException(new javax.faces.application.FacesMessage(firstErrorMessage));
    }
}

    private void registerAllMessages(FacesContext facesContext, UIComponent uiComponent,
            List<ValidationMessage> messages) {
        for (ValidationMessage message : messages) {
            String componentId = uiComponent.getClientId(facesContext);
            facesContext.addMessage(componentId, new FacesMessage(message.getMessage()));
        }
    }

    public void restoreState(FacesContext ctx, Object state) {
        this.formatted = (Boolean) state;
    }

    public Object saveState(FacesContext ctx) {
        return formatted;
    }

    public boolean isTransient() {
        return transientValue;
    }

    public void setTransient(boolean transientValue) {
        this.transientValue = transientValue;
    }
}
