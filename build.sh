# get WALA
git clone https://github.com/wala/WALA
cd WALA
./gradlew clean build publishToMavenLocal
cd ..

mvn clean install


