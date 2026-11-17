package cz.fit.tsm.faces.common;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

public final class Messages {

	private Messages() {
	}

	public static String get(String key) {
		FacesContext context = FacesContext.getCurrentInstance();
		ResourceBundle bundle = context.getApplication().getResourceBundle(context, "msg");
		try {
			return bundle.getString(key);
		}
		catch (MissingResourceException ex) {
			return key;
		}
	}

	public static void error(String detail) {
		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(FacesMessage.SEVERITY_ERROR, detail, null));
	}

	public static void errorKey(String key) {
		error(get(key));
	}

	public static void info(String detail) {
		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(FacesMessage.SEVERITY_INFO, detail, null));
	}

	public static void infoKey(String key) {
		info(get(key));
	}
}
