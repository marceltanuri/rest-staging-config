package br.com.mtanuri.liferay.rest.stagingconfig;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.User;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;

@Component
public class Authentication {

	private static final String ADMINISTRATOR_ROLE = "Administrator";

	@Value("${auth.username}")
	private String authUsername;

	@Value("${auth.password}")
	private String authPassword;

	private static Log _log = LogFactoryUtil.getLog(Authentication.class);

	public boolean isAuthenticated(HttpServletRequest request) {

		String username = request.getParameter("username");
		String password = request.getParameter("password");
		String groupId = request.getParameter("groupId");

		if (username != null && password != null && !username.isEmpty() && !password.isEmpty() & groupId == null
				|| !groupId.isEmpty()) {

			try {
				
				Group group = GroupLocalServiceUtil.getGroup(Long.valueOf(groupId));
				String authType = CompanyLocalServiceUtil.getCompany(group.getCompanyId()).getAuthType();
				long authenticatedUser = UserLocalServiceUtil.authenticateForBasic(group.getCompanyId(), authType,
						username, password);
				
				if (authenticatedUser > 0) {
					User user = UserLocalServiceUtil.getUser(authenticatedUser);
					for (Role role : user.getRoles()) {
						
						if (role.getType() == 1 && role.getName().equals(ADMINISTRATOR_ROLE)) {
							return true;
						}
					}
				}
				
			} catch (Exception e) {
				_log.info("Authentication error: " + e.getMessage());
				return false;
			}
		}
		return false;
	}
}
