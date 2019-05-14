package br.com.mtanuri.liferay.rest.stagingconfig;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

	public String getIPAddress(HttpServletRequest request) {
		String ipAddress = request.getHeader("X-FORWARDED-FOR");
		if (ipAddress == null) {
			ipAddress = request.getRemoteAddr();
		}
		return ipAddress;
	}

	public String getRequestParameters(HttpServletRequest request) {
		StringBuilder sb = new StringBuilder();
		
		@SuppressWarnings("unchecked")
		Enumeration<String> parameterNames = request.getParameterNames();
		
		while (parameterNames.hasMoreElements()) {
			String parameterName = parameterNames.nextElement();
			sb.append(parameterName + ": " + request.getParameter(parameterName) + "; ");
		}
		return "{" + sb.toString() + "}";
	}
	
	public String preventXSS(String param) {
		
		if(param!=null && !param.isEmpty()) {
			return StringEscapeUtils.escapeHtml4(param).replace(" ", "");
		}
		
		return param;
	}

}
