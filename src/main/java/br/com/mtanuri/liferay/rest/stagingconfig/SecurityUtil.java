package br.com.mtanuri.liferay.rest.stagingconfig;

import java.util.Arrays;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

	@Value("${target.hosts}")
	private String validTargetHosts;

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

		if (param != null && !param.isEmpty()) {
			return StringEscapeUtils.escapeHtml4(param).replace(" ", "");
		}

		return param;
	}

	public boolean isValidTargetHost(String host) {
		if (this.validTargetHosts != null && !this.validTargetHosts.isEmpty()) {
			if(Arrays.asList(this.validTargetHosts.split(",")).contains(host)) {
				return true;
			}
		}
		return false;
	}

}
