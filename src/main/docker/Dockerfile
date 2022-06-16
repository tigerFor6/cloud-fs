FROM hub.kuangheng.com/jdk/mini-jdk8:1
#VOLUME /tmp
ENV LANG C.UTF-8
ENV TZ=Asia/Shanghai
ADD cloud-fs.jar fsApp.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/fsApp.jar"]