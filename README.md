# Configurable
Annotation based configuration library for Java

| Dependency | Explanation |
|:-----------|-------------|
|[lombok](https://projectlombok.org/) | To make the code prettier, and easier to work with |
|[json-io](https://github.com/jdereg/json-io) | For the JSON implementation |
|[SnakeYAML](https://bitbucket.org/asomov/snakeyaml) | For the YAML implementation |
|[toml4j](https://github.com/mwanji/toml4j) | For the TOML implementation

| TODO |
|------|
| Find workaround for silly finals & inlining |
| _maybe_ Comments |


# Build
Build using gradle ```gradlew build```

# Maven
We're on maven
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
<dependency>
    <groupId>com.github.Sauilitired</groupId>
    <artifactId>Configurable</artifactId>
    <version>e5b88d349b</version>
</dependency>
```


Example of a config declaration using lombok for ```@Getter```
```java
@Getter
@Configuration(name = "config", implementation = ConfigurationImplementation.JSON)
public class RandomClass {

  private String loginMessage = "Welcome online {0}!";

  private transient Listeners listeners = new Listeners();

  @Getter
  @ConfigSection
  public class Listeners {

    public boolean asyncPlayerChatEvent = false;

    private boolean blockDecayEvent = false;

  }  
}
```

And this is how you would manage it
```java
final File FOLDER = new File(".");

Config<RandomClass> config = ConfigurationFactory.load(RandomClass.class, FOLDER);

// Get the RandomClass instance
RandomClass instance = config.get();
String loginMessage = instance.getLoginMessage();

// But you can also access it like this
String loginMessage = config.get("loginMessage", String.class);
```

And this is how it would end up
```json
// config.json
{
  "loginMessage": "Welcome online {0}!",
  "Listeners": {
    "@type":"java.util.HashMap",
    "asyncPlayerChatEvent": false,
    "blockDecayEvent": false
  }
}
```


Another example, where the inner class is static (which removes the requirement for the instance field - See JavaDocs)
```java
@Configuration(implementation = ConfigurationImplementation.YAML, requiresAnnotations = true)
public class Tester {

    @ConfigSection
    public static class Sub {
        @ConfigValue
        public static String test = "Hello";
    }
    
}   
```

Would be read as:
```java
try {
    Config<Tester> config = ConfigurationFactory.from(Tester.class);
    config.read(new File("."));
    System.out.println(Sub.test);
} catch (Exception e) {
    e.printStackTrace();
}


OR

ConfigurationFactory.from(Tester.class);
System.out.println(Sub.test);
```

And would ouput (Tester.yml)
```yaml
Sub:
  test: Hello
```
