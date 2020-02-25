# cassandra-sstable-generator

![Instaclustr](https://circleci.com/gh/instaclustr/cassandra-sstable-generator.svg?style=svg)

CLI tool generating Cassandra SSTables

This tool is simply generating SSTables programmatically. It uses Cassandra's `CQLSSTableWriter`. 
After the generation of SSTables is finished, you can load them by `sstableloader` tool as usually.

The project consists of these modules:

* api - impl is coded against this module
* impl - the implementation of your population logic, depends on `api`
* generator - the implementation of whole generator CLI application
* cassandra-3 - the implementation of SSTable generator using internals of Cassandra 3 artifact
* cassandra- 4 - the implementation of SSTable generator using internals of Cassandra 4 artifact 

## Build 

`mvn clean instal`

## Run

Let's guide you through an example. We want to generate a SSTable by Cassandra 3 API so we can load it 
to Cassandra afterwards. The components you need to have on a class path are as follows:

* generator jar
* cassandra-3 module jar
* jar with the implementation of your generation logic

```
java \
  -cp /path/to/impl-1.0.jar:/path/to/generator-1.0.jar:/path/to/cassandra-3.jar \
  com.instaclustr.sstable.generator.LoaderApplication \
  _command_ \
  _arguments_
```

The concrete example of the invocation would be:

```
java \
    -cp impl/target/impl-1.0.jar:generator/target/sstable-generator-1.0.jar:cassandra-3/target/cassandra-3-1.0.jar \
    com.instaclustr.sstable.generator.LoaderApplication \
    fixed \
    --keyspace mykeyspace \
    --table mytable \
    --output-dir=/tmp/output \
    --schema cassandra-3/src/test/resources/cassandra/cql/table.cql \
    --threads 2
```

No `command` executes default command - `help`:

```
Usage: <main class> [-V] COMMAND
  -V, --version   print version information and exit
Commands:
  csv     tool for bulk-loading of data from csv
  random  tool for bulk-loading of random data
  fixed   tool for bulk-loading of fixed data
```

### `random` command

By this command, you are expected to provide data which represents a row in random fashion.

### `csv` command

`csv` command has same arguments as `random` but `--file` is mandatory. There is supposed to be CSV file which 
is representing rows. Each row will be parsed into list of strings passed to `RowMapper` implementation where you 
have to map them to list of objects for Cassandra INSERT statement as values.

### `fixed` command 

By `fixed` command, we will generate SSTable by using the exact list of "rows" with columns. This 
will be obvious from the documentation which follows. 

## Row generation

In order to generate data for all three cases above. you have to implement interface 
`com.instaclustr.cassandra.bulkloader.RowMapper` in `api` module. This implementation should 
be placed in `impl` (or its equivalent) and it should be on a class path.

## RowMapper interface

```
public interface RowMapper {

    /**
     * Maps list of strings from whatever input representing
     * a row to list of objects to insert into Cassandra.
     *<p>
     * This method e.g. called upon generation from CSV file.
     *<p>
     * @param row where values are consisting of list of strings
     * @return list of objects to put to insert statement
     */
    List<Object> map(final List<String> row);

    /**
     * Used when we do not want to generate data randomly but we have exact list of what to insert.
     *
     * @return list of rows to be created containing list of cells
     */
    List<List<Object>> get();

    /**
     * Logically same as {@link #map(List)} but all data per row
     * needs to be generated inside of the method. The number
     * of items in the returned list has to match number of columns
     * in a row. Each such object represents value which will be
     * passed to Cassandra INSERT statement.
     * <p>
     * This method is called repeatedly. Number of calls
     * is equal to paramter `--numberOfRecords`.
     *<p>
     * This method is called upon "random" generation.
     * @return list of objects to put to insert statement
     */
    List<Object> random();

    /**
     * @return string representation of INSERT INTO statement. Question marks in VALUES are not
     * meant to be replaced.
     * <p>
     * For example: 'INSERT INTO keyspace.table (field1, field2, field3) VALUES (?, ?, ?)'
     */
    String insertStatement();
}
```

The implementation of `RowMapper` you are supposed to place on the class path would look like this:

```
public class RowMapper1 implements RowMapper {


    public static final String KEYSPACE = "mykeyspace";
    public static final String TABLE = "mytable";

    public static final UUID UUID_1 = UUID.randomUUID();
    public static final UUID UUID_2 = UUID.randomUUID();
    public static final UUID UUID_3 = UUID.randomUUID();

    @Override
    public List<Object> map(final List<String> row) {
        return null;
    }

    @Override
    public Stream<List<Object>> get() {
        return Stream.of(
            new ArrayList<Object>() {{
                add(UUID_1);
                add("John");
                add("Doe");
            }},
            new ArrayList<Object>() {{
                add(UUID_2);
                add("Marry");
                add("Poppins");
            }},
            new ArrayList<Object>() {{
                add(UUID_3);
                add("Jim");
                add("Jack");
            }});
    }

    @Override
    public List<Object> random() {
        return null;
    }

    @Override
    public String insertStatement() {
        return format("INSERT INTO %s.%s (id, name, surname) VALUES (?, ?, ?);", KEYSPACE, TABLE);
    }
}
```

## SPI mechanism

There is Java SPI mechanism for implementation discovery so it means that besides implementing API,
you have to change `impl/src/main/resources/META-INF/services/com.instaclustr.sstable.generator.RowMapper` 
file containing FQCN of your implemenation on one line.

Once `impl` jar is placed on the class path, it will be automatically discovered by `generator` module so 
you do not need to use any command-line arguments. Mere putting of that JAR on the class path does the job.

The same mechanism works for `cassandra-3/4` jar. In case you want to generate jars by `CQLSSTableWriter` 
for Cassandra 3, just put that jar on the class path. If you want to generate "Cassandra 4 SSTables", place 
respective `cassandra-4.jar` on the class path as shown above.

This in practice means that you need to compile only `impl` module which contains one class so the compilation 
and JAR building will take literally few seconds (less the 1 sec here). The command line arguments and all will look 
just same.
