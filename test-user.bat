@echo off

cd star-user

echo ================git pull=======================
@call git pull

sleep 5

@call mvn clean package -U -P test -Dmaven.test.skip=true

if %ERRORLEVEL% EQU 0 (
        echo ================SUCCESS=======================
        call:pubApi 192.168.31.249 "publish to 192.168.31.249"
        echo ================SUCCESS=======================
        sleep 15
) else (
        COLOR C
	    echo -------         !! FAILD !!      -------------
        pause
)

exit

:pubApi
scp target/star-user.war webserver@%~1:~/star-user
ssh webserver@%~1  "source /etc/profile;cd ~/star-user;rm -rf bak.webapp && mv -f webapp bak.webapp;unzip star-user.war -d webapp;./restart.sh"

