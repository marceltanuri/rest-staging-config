# rest-staging-config

Hi!

I'm a RESTFull Liferay Portlet for setting up portal's staging remote configuration.

How do I work?
- You give me a POST request with the parameters:
  - groupId: the groupID of the site in Liferay portal
  - target: a valid remote address which is set in the application.properties file.
  - username and password: A valid username and password of user which has Administrator role in the portal.

Then...
- If it is all set with your request, the staging remote addres in the portal configurations will be updated for you with the given parameters.

Addition:
- Also is possible to trigger a reindex action

A curl example for setting staging remote configuration:
``` curl
curl -d "groupId=10182&target=127.0.0.1&username=test@liferay.com&password=test" -X POST http://localhost:8080/rest-staging-config-1.0.0/rest/staging-target
```

A curl example for executing reindex:
``` curl
curl -d "groupId=10182&username=test@liferay.com&password=test" -X POST http://localhost:8080/rest-staging-config-1.0.0/rest/reindex
```

Nice to meet you!
