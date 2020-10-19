FROM openjdk:11 as build
RUN apt-get update
RUN apt-get install -y ant nsis makeself
COPY . /usr/src/tray
WORKDIR /usr/src/tray
RUN ant makeself

FROM openjdk:11-jre as install
RUN apt-get update
RUN apt-get install -y libglib2.0-bin
COPY --from=build /usr/src/tray/out/*.run /tmp
RUN find /tmp -iname '*.run' -exec {} \;
WORKDIR /opt/qz-tray
ENTRYPOINT ["/opt/qz-tray/qz-tray"]
CMD ["--headless"]