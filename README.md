# rest-staging-config

Hi!

I'm a RESTFull Liferay Portlet for setting up portal's staging remote configuration.

How am I work?
- You give me a POST request with the parameters:
  - groupId: the groupID of the site in Liferay portal
  - target: the new remote address to be set up.
  - username: A valid username which is set in the auth.properties file
  - password: A valid password which is set in the auth.properties file

Then...
- If it is all set with your request, the staging remote addres in the portal configurations will be updated for you with the given parameters.

A curl example:
```curl -d "groupId=10182&target=new-target.com&username=admin&password=admin123" -X POST http://localhost:8080/rest-staging-config-1.0.0/rest/staging-target```

Nice to meet you!
