#!/bin/sh

# Kurento Room Demo installer for Ubuntu 14.04
if [ `id -u` -ne 0 ]; then
    echo ""
    echo "Only root can start Kurento Room Demo server"
    echo ""
    exit 1
fi

KROOMDEMO_HOME=$(dirname $(dirname $(readlink -f $0)))

# Create directories
mkdir -p /etc/kurento/
mkdir -p /var/lib/kurento
mkdir -p /var/log/kurento-media-server && chown nobody /var/log/kurento-media-server

# Install binary & config
install -o root -g root -m 755 $KROOMDEMO_HOME/bin/start.sh /usr/bin/kroomdemo
install -o root -g root $KROOMDEMO_HOME/lib/kroomdemo.jar /var/lib/kurento/kroomdemo.jar
install -o root -g root $KROOMDEMO_HOME/config/kroomdemo.conf.json /etc/kurento/kroomdemo.conf.json
install -o root -g root $KROOMDEMO_HOME/config/kroomdemo-log4j.properties /etc/kurento/kroomdemo-log4j.properties

DIST=$(lsb_release -i | awk '{print $3}')
[ -z "$DIST" ] && { echo "Unable to get distribution information"; exit 1; } 
case "$DIST" in
    Ubuntu)
        mkdir -p /etc/default
        echo "# Defaults for KROOMDEMO initscript" > /etc/default/kroomdemo
        echo "# sourced by /etc/init.d/kroomdemo" >> /etc/default/kroomdemo
        echo "# installed at /etc/default/kroomdemo by the maintainer scripts" >> /etc/default/kroomdemo
        echo "" >> /etc/default/kroomdemo
        echo "#" >> /etc/default/kroomdemo
        echo "# This is a POSIX shell fragment" >> /etc/default/kroomdemo
        echo "#" >> /etc/default/kroomdemo  
        echo "" >> /etc/default/kroomdemo
        echo "# Commment next line to disable KROOMDEMO daemon" >> /etc/default/kroomdemo
        echo "START_DAEMON=true" >> /etc/default/kroomdemo
        echo "" >> /etc/default/kroomdemo
        echo "# Whom the daemons should run as" >> /etc/default/kroomdemo
        echo "DAEMON_USER=nobody" >> /etc/default/kroomdemo
        
        install -o root -g root -m 755 $KROOMDEMO_HOME/support-files/kroomdemo.sh /etc/init.d/kroomdemo
        update-rc.d kroomdemo defaults
        /etc/init.d/kroomdemo restart
        ;;
    CentOS)
        install -o root -g root -m  644 $KROOMDEMO_HOME/support-files/kroomdemo.service /usr/lib/systemd/system/kroomdemo.service
        systemctl daemon-reload
        systemctl enable kroomdemo.service
        systemctl restart kroomdemo
        ;;
esac
