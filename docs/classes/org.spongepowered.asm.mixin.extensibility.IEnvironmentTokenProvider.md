[< Back](../README.md)
# public interface IEnvironmentTokenProvider IEnvironmentTokenProvider #
>#### Class Overview ####
>Provides a token value into the attached environment
## Fields ##
### public static final int DEFAULT_PRIORITY ###
>#### Field Overview ####
>Default token provider priority
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1000
>
## Methods ##
### public int getPriority () ###
>#### Method Overview ####
>Get the priority for this provider, should return a priority relative to
 {@link #DEFAULT_PRIORITY}.
>
### public Integer getToken (String, MixinEnvironment) ###
>#### Method Overview ####
>Get the value of the specified token in this environment, or return null
 if this provider does not have a value for this token. All tokens are 
 converted to UPPERCASE before being requested from the provider
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The token value, or null if this provider does not have the token
>
>### Parameters ###
>**token**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Token (in upper case) to search for
>
>**env**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Current environment
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)