#!/bin/bash

APP_HOME=$(dirname $(dirname $(readlink -f $0)))
APP_NAME=${project.artifactId}

# ${project.description} installer for Ubuntu >= 14.04
if [ `id -u` -ne 0 ]; then
    echo ""
    echo "Only root can start $APP_NAME"
    echo ""
    exit 1
fi

useradd -d /var/kurento/ kurento

SYSTEMD=$(pidof systemd && echo "systemd" || echo "other")

# Install binaries
mkdir -p /var/lib/kurento
chown kurento /var/lib/kurento
install -o kurento -g root $APP_HOME/files/$APP_NAME.jar /var/lib/kurento/
install -o kurento -g root $APP_HOME/sysfiles/$APP_NAME.conf /var/lib/kurento/
install -o kurento -g root $APP_HOME/files/keystore.jks /var/lib/kurento/
sudo rm /etc/init.d/$APP_NAME
sudo ln -s /var/lib/kurento/$APP_NAME.jar /etc/init.d/$APP_NAME
mkdir -p /etc/kurento/
install -o kurento -g root $APP_HOME/files/$APP_NAME.conf.json /etc/kurento/
install -o kurento -g root $APP_HOME/files/$APP_NAME.properties /etc/kurento/
install -o kurento -g root $APP_HOME/sysfiles/$APP_NAME-log4j.properties /etc/kurento/

mkdir -p /var/log/kurento
chown kurento /var/log/kurento


if [[ "$SYSTEMD" != "other" ]]; then
	install -o root -g root $APP_HOME/sysfiles/systemd.service /etc/systemd/system/$APP_NAME.service

	sudo systemctl daemon-reload

	# enable at startup
	systemctl enable $APP_NAME

	# start service
	systemctl restart $APP_NAME
else
	# Create defaults
	mkdir -p /etc/default
	cat > /etc/default/$APP_NAME <<-EOF
		# Defaults for $APP_NAME initscript
		# sourced by /etc/init.d/$APP_NAME
		# installed at /etc/default/$APP_NAME by the maintainer scripts

		#
		# This is a POSIX shell fragment
		#

		# Comment next line to disable $APP_NAME daemon
		START_DAEMON=true

		# Whom the daemons should run as
		DAEMON_USER=kurento
	EOF

	update-rc.d $APP_NAME defaults
	service $APP_NAME restart
fi