# get WALA
git clone https://github.com/ijcon40/WALA
cd WALA
git checkout java11-fix
./gradle clean build publishToMavenLocal -x test -x javadoc
cd ..

mvn clean install


