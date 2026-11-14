package cz.fit.tsm.faces;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import jakarta.faces.context.FacesContext;

final class Messages {

	private Messages() {
	}

	static String get(String key) {
		FacesContext context = FacesContext.getCurrentInstance();
		ResourceBundle bundle = context.getApplication().getResourceBundle(context, "msg");
		try {
			return bundle.getString(key);
		}
		catch (MissingResourceException ex) {
			return key;
		}
	}
}
