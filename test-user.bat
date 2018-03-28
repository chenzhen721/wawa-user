@echo off

cd wawa-user

echo ================git pull=======================
@call git pull

sleep 5

@call mvn clean package -U -P test -Dmaven.test.skip=true

if %ERRORLEVEL% EQU 0 (
        echo ================SUCCESS=======================
        call:pubApi 120.79.52.5 "publish to 120.79.52.5"
        echo ================SUCCESS=======================
        sleep 15
) else (
        COLOR C
	    echo -------         !! FAILD !!      -------------
        pause
)

exit

:pubApi
scp target/wawa-user.war webserver@%~1:~/test-wawa-user
ssh mlsty@%~1  "source /etc/profile;cd ~/test-wawa-user;rm -rf bak.webapp && mv -f webapp bak.webapp;unzip wawa-user.war -d webapp;./restart.sh"

