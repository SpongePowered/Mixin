[< Back](../README.md)
# ConstraintParser.Constraint #
>#### Class Overview ####
>A constraint. Constraints are parsed from string expressions which are
 always of the form:
 
 <blockquote><pre>&lt;token&gt;(&lt;constraint&gt;)</pre></blockquote>
 
 <p><b>token</b> is normalised to uppercase and must be provided by the
 environment.</p>
 
 <p><b>constraint</b> is an integer range specified in one of the
 following formats:

 <dl>
   <dt><pre>()</pre></dt>
   <dd>The token value must be present in the environment, but can have
     any value</dd>
   <dt><pre>(1234)</pre></dt>
   <dd>The token value must be <em>exactly equal to </em> <code>1234
   </code></dd>
   <dt><pre>(1234+)
(1234-)
(1234&gt;)
</pre></dt>
   <dd>All of these variants mean the same thing, and can be read as "1234
     or greater"</dd>
   <dt><pre>(&lt;1234)</pre></dt>
   <dd><em>Less than</em> 123</dd>
   <dt><pre>(&lt;=1234)</pre></dt>
   <dd><em>Less than or equal to</em> 1234 (equivalent to <code>1234&lt;
     </code>)</dd>
   <dt><pre>(&gt;1234)</pre></dt>
   <dd><em>Greater than</em> 1234</dd>
   <dt><pre>(&gt;=1234)</pre></dt>
   <dd><em>Greater than or equal to</em> 1234 (equivalent to <code>1234
     &gt;</code>)</dd>
   <dt><pre>(1234-1300)</pre></dt>
   <dd>Value must be <em>between</em> 1234 and 1300 (inclusive)</dd> 
   <dt><pre>(1234+10)</pre></dt>
   <dd>Value must be <em>between</em> 1234 and 1234+10 (1234-1244
     inclusive)</dd>
 </dl>
 
 <p>All whitespace is ignored in constraint declarations. The following
 declarations are equivalent:</p>
 
 <blockquote><pre>token(123-456)
token   (   123 - 456   )</pre></blockquote>

 <p>Multiple constraints should be separated by semicolon (<code>;</code>)
 and are conjoined by an implied logical <code>AND</code> operator. That
 is: all constraints must pass for the constraint to be considered valid.
 </p>
## Fields ##
### public static final Constraint NONE ###
>#### Field Overview ####
>No description provided
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Methods ##
### public String getToken () ###
>#### Method Overview ####
>No description provided
>
### public int getMin () ###
>#### Method Overview ####
>No description provided
>
### public int getMax () ###
>#### Method Overview ####
>No description provided
>
### public void check (ITokenProvider) ###
>#### Method Overview ####
>Checks the current token against the environment and throws a
 {@link ConstraintViolationException} if the constraint is invalid
>
>### Parameters ###
>**environment**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;environment to fetch constraints
>
>### Throws ###
>**ConstraintViolationException**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;if constraint is not valid
>
### public String getRangeHumanReadable () ###
>#### Method Overview ####
>Gets a human-readable description of the range expressed by this
 constraint
>
### public String toString () ###
>#### Method Overview ####
>No description provided
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)