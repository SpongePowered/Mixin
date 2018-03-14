[< Back](../README.md)
# public PrettyPrinter PrettyPrinter #
>#### Class Overview ####
>Prints information in a pretty box
## Fields ##
### protected int width ###
>#### Field Overview ####
>Box with (adapts to contents)
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected int wrapWidth ###
>#### Field Overview ####
>Wrap width used when an explicit wrap width is not specified
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected int kvKeyWidth ###
>#### Field Overview ####
>Key/value key width
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
### protected String kvFormat ###
>#### Field Overview ####
>No description provided
>
>**default**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;null
>
## Constructors ##
### public PrettyPrinter () ###
>#### Constructor Overview ####
>No description provided
>
### public PrettyPrinter (int) ###
>#### Constructor Overview ####
>No description provided
>
## Methods ##
### public PrettyPrinter wrapTo (int) ###
>#### Method Overview ####
>Set the wrap width (default 80 columns)
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**wrapWidth**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;new width (in characters) to wrap to
>
### public int wrapTo () ###
>#### Method Overview ####
>Get the current wrap width
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;the current wrap width
>
### public PrettyPrinter table () ###
>#### Method Overview ####
>Begin a new table with no header and adaptive column widths
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
### public PrettyPrinter table (String[]) ###
>#### Method Overview ####
>Begin a new table with the specified headers and adaptive column widths
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**titles**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Column titles
>
### public PrettyPrinter table (Object[]) ###
>#### Method Overview ####
>Begin a new table with the specified format. The format is specified as a
 sequence of values with {@link String}s defining column titles,
 {@link Integer}s defining column widths, and {@link Alignment}s defining
 column alignments. Widths and alignment specifiers should follow the
 relevant column title. Specify a <em>negative</em> value to specify the
 <em>maximum</em> width for a column (values will be truncated).
 
 <p>For example, to specify a table with two columns of width 10:</p>
 
 <code>printer.table("Column 1", 10, "Column 2", 10);</code>
 
 <p>A table with a column 30 characters wide and a right-aligned column 20
 characters wide:</p>
 
 <code>printer.table("Column 1", 30, "Column 2", 20, Alignment.RIGHT);
 </code>
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**format**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;format string, see description
>
### public PrettyPrinter spacing (int) ###
>#### Method Overview ####
>Set the column spacing for the current table. Default = 2
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**spacing**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Column spacing in characters
>
### public PrettyPrinter th () ###
>#### Method Overview ####
>Print the current table header. The table header is automatically printed
 before the first row if not explicitly specified by calling this method.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
### public PrettyPrinter tr (Object[]) ###
>#### Method Overview ####
>Print a table row with the specified values. If more columns are
 specified than exist in the table, then the table is automatically
 expanded.
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**args**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;column values
>
### public PrettyPrinter add () ###
>#### Method Overview ####
>Adds a blank line to the output
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
### public PrettyPrinter add (String) ###
>#### Method Overview ####
>Adds a string line to the output
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**string**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;format string
>
### public PrettyPrinter add (String, Object[]) ###
>#### Method Overview ####
>Adds a formatted line to the output
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**format**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;format string
>
>**args**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;arguments
>
### public PrettyPrinter add (Object[]) ###
>#### Method Overview ####
>Add elements of the array to the output, one per line
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**array**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Array of objects to print
>
### public PrettyPrinter add (Object[], String) ###
>#### Method Overview ####
>Add elements of the array to the output, one per line
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**array**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Array of objects to print
>
>**format**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Format for each row
>
### public PrettyPrinter addIndexed (Object[]) ###
>#### Method Overview ####
>Add elements of the array to the output, one per line, with array indices
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**array**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Array of objects to print
>
### public PrettyPrinter addWithIndices (Collection) ###
>#### Method Overview ####
>Add elements of the collection to the output, one per line, with indices
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**c**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Collection of objects to print
>
### public PrettyPrinter add (PrettyPrinter.IPrettyPrintable) ###
>#### Method Overview ####
>Adds a pretty-printable object to the output, the object is responsible
 for adding its own representation to this printer
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**printable**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;object to add
>
### public PrettyPrinter add (Throwable) ###
>#### Method Overview ####
>Print a formatted representation of the specified throwable with the
 default indent (4)
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**th**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Throwable to print
>
### public PrettyPrinter add (Throwable, int) ###
>#### Method Overview ####
>Print a formatted representation of the specified throwable with the
 specified indent
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**th**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Throwable to print
>
>**indent**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Indent size for stacktrace lines
>
### public PrettyPrinter add (StackTraceElement[], int) ###
>#### Method Overview ####
>Print a formatted representation of the specified stack trace with the
 specified indent
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**stackTrace**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;stack trace to print
>
>**indent**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Indent size for stacktrace lines
>
### public PrettyPrinter add (Object) ###
>#### Method Overview ####
>Adds the specified object to the output
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**object**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;object to add
>
### public PrettyPrinter add (Object, int) ###
>#### Method Overview ####
>Adds the specified object to the output
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**object**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;object to add
>
>**indent**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;indent amount
>
### public PrettyPrinter addWrapped (String, Object[]) ###
>#### Method Overview ####
>Adds a formatted line to the output, and attempts to wrap the line
 content to the current wrap width
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**format**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;format string
>
>**args**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;arguments
>
### public PrettyPrinter addWrapped (int, String, Object[]) ###
>#### Method Overview ####
>Adds a formatted line to the output, and attempts to wrap the line
 content to the specified width
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**width**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;wrap width to use for this content
>
>**format**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;format string
>
>**args**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;arguments
>
### public PrettyPrinter kv (String, String, Object[]) ###
>#### Method Overview ####
>Add a formatted key/value pair to the output
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Key
>
>**format**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Value format
>
>**args**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Value args
>
### public PrettyPrinter kv (String, Object) ###
>#### Method Overview ####
>Add a key/value pair to the output
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**key**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Key
>
>**value**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Value
>
### public PrettyPrinter kvWidth (int) ###
>#### Method Overview ####
>Set the minimum key display width
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent
>
>### Parameters ###
>**width**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;width to set
>
### public PrettyPrinter add (Map) ###
>#### Method Overview ####
>Add all values of the specified map to this printer as key/value pairs
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent
>
>### Parameters ###
>**map**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Map with entries to add
>
### public PrettyPrinter hr () ###
>#### Method Overview ####
>Adds a horizontal rule to the output
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
### public PrettyPrinter hr (char) ###
>#### Method Overview ####
>Adds a horizontal rule of the specified char to the output
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**ruleChar**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;character to use for the horizontal rule
>
### public PrettyPrinter centre () ###
>#### Method Overview ####
>Centre the last line added
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
### public PrettyPrinter trace () ###
>#### Method Overview ####
>Outputs this printer to stderr and to a logger decorated with the calling
 class name with level {@link Level#DEBUG}
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
### public PrettyPrinter trace (Level) ###
>#### Method Overview ####
>Outputs this printer to stderr and to a logger decorated with the calling
 class name at the specified level
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**level**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Log level to write messages
>
### public PrettyPrinter trace (String, Level) ###
>#### Method Overview ####
>Outputs this printer to stderr and to a logger decorated with specified
 name with the specified level
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**logger**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Logger name to write to
>
>**level**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Log level to write messages
>
### public PrettyPrinter trace (Logger) ###
>#### Method Overview ####
>Outputs this printer to stderr and to the supplied logger with level
 {@link Level#DEBUG}
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**logger**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Logger to write to
>
### public PrettyPrinter trace (Logger, Level) ###
>#### Method Overview ####
>Outputs this printer to stderr and to the supplied logger with the
 specified level
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**logger**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Logger to write to
>
>**level**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Log level to write messages
>
### public PrettyPrinter trace (PrintStream, Level) ###
>#### Method Overview ####
>Outputs this printer to the specified stream and to a logger decorated
 with the calling class name with the specified level
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**stream**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Output stream to print to
>
>**level**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Log level to write messages
>
### public PrettyPrinter trace (PrintStream, String, Level) ###
>#### Method Overview ####
>Outputs this printer to the specified stream and to a logger with the
 specified name at the specified level
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**stream**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Output stream to print to
>
>**logger**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Logger name to write to
>
>**level**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Log level to write messages
>
### public PrettyPrinter trace (PrintStream, Logger) ###
>#### Method Overview ####
>Outputs this printer to the specified stream and to the supplied logger
 with level {@link Level#DEBUG}
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**stream**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Output stream to print to
>
>**logger**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Logger to write to
>
### public PrettyPrinter trace (PrintStream, Logger, Level) ###
>#### Method Overview ####
>Outputs this printer to the specified stream and to the supplied logger
 with at the specified level
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**stream**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Output stream to print to
>
>**logger**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Logger to write to
>
>**level**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Log level to write messages
>
### public PrettyPrinter print () ###
>#### Method Overview ####
>Print this printer to stderr
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
### public PrettyPrinter print (PrintStream) ###
>#### Method Overview ####
>Print this printer to the specified output
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**stream**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;stream to print to
>
### public PrettyPrinter log (Logger) ###
>#### Method Overview ####
>Write this printer to the specified logger at {@link Level#INFO}
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**logger**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;logger to log to
>
### public PrettyPrinter log (Logger, Level) ###
>#### Method Overview ####
>Write this printer to the specified logger
>
>**returns**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fluent interface
>
>### Parameters ###
>**logger**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;logger to log to
>
>**level**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;log level
>
### public static void dumpStack () ###
>#### Method Overview ####
>Convenience method, alternative to using <tt>Thread.dumpStack</tt> which
 prints to stderr in pretty-printed format.
>
### public static void print (Throwable) ###
>#### Method Overview ####
>Convenience methods, pretty-prints the specified throwable to stderr
>
>### Parameters ###
>**th**<br />
>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Throwable to log
>

---
Powered by [MDDocs](https://github.com/VRCube/MDDocs)