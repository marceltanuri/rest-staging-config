package br.com.mtanuri.liferay.rest.stagingconfig;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Authentication {
	
	 @Value("${auth.username}")
	 private String authUsername;
	 
	 @Value("${auth.password}")
	 private String authPassword;

	public boolean isAuthenticated(HttpServletRequest request) {

		String user = request.getParameter("username");
		String pass = request.getParameter("password");

		if (user != null && user.equals(authUsername) && pass != null
				&& pass.equals(authPassword)) {
			return true;
		}

		return false;
	}

}
