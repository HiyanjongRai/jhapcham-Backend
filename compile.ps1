$env:JAVA_HOME = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.3\jbr"
$env:Path = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin;C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.3\jbr\bin;" + $env:Path
mvn clean compile
