package server.utils;

import engine.manager.MagitMsgManager;
import engine.ui.UIManager;
import engine.users.UserManager;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import static server.constants.Constants.INT_PARAMETER_ERROR;

public class ServletUtils {
	public static final String REPOSITORY_DIR = "C:\\magit-ex3";
	private static final String USER_MANAGER_ATTRIBUTE_NAME = "userManager";
	private static final String MSG_MANAGER_ATTRIBUTE_NAME = "msgManager";
	private static final String UI_MANAGER_ATTRIBUTE_NAME = "uiManager";

	/*
	Note how the synchronization is done only on the question and\or creation of the relevant managers and once they exists -
	the actual fetch of them is remained un-synchronized for performance POV
	 */
	private static final Object userManagerLock = new Object();
	private static final Object msgManagerLock = new Object();
	private static final Object uiManagerLock = new Object();

	public static UserManager getUserManager(ServletContext servletContext) {

		synchronized (userManagerLock) {
			if (servletContext.getAttribute(USER_MANAGER_ATTRIBUTE_NAME) == null) {
				servletContext.setAttribute(USER_MANAGER_ATTRIBUTE_NAME, new UserManager());
			}
		}
		return (UserManager) servletContext.getAttribute(USER_MANAGER_ATTRIBUTE_NAME);
	}

	public static MagitMsgManager getMsgManager(ServletContext servletContext) {
		synchronized (msgManagerLock) {
			if (servletContext.getAttribute(MSG_MANAGER_ATTRIBUTE_NAME) == null) {
				servletContext.setAttribute(MSG_MANAGER_ATTRIBUTE_NAME, new MagitMsgManager());
			}
		}
		return (MagitMsgManager) servletContext.getAttribute(MSG_MANAGER_ATTRIBUTE_NAME);
	}

	public static UIManager getUIManager(ServletContext servletContext) {
		synchronized (uiManagerLock) {
			if (servletContext.getAttribute(UI_MANAGER_ATTRIBUTE_NAME) == null) {
				servletContext.setAttribute(UI_MANAGER_ATTRIBUTE_NAME, new UIManager());
			}
		}
		return (UIManager) servletContext.getAttribute(UI_MANAGER_ATTRIBUTE_NAME);
	}

	public static int getIntParameter(HttpServletRequest request, String name) {
		String value = request.getParameter(name);
		if (value != null) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException numberFormatException) {
			}
		}
		return INT_PARAMETER_ERROR;
	}
}
