* Set next release-version in pom.xml
* Edit README.md with new version
* git commit -a -m "Releasing xxx"
* git tag xxx
* If JAVA_HOME is not set, set lit like this: export JAVA_HOME=$(/usr/libexec/java_home)
* mvn clean deploy
* Set next develop-version in pom.xml
* git commit -a -m "Preparing for next version"
* git push
* git push --tags
